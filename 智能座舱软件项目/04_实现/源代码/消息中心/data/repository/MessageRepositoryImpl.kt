package com.longcheer.cockpit.message.data.repository

import com.longcheer.cockpit.message.data.local.dao.MessageCategoryDao
import com.longcheer.cockpit.message.data.local.dao.MessageDao
import com.longcheer.cockpit.message.data.mapper.MessageMapper
import com.longcheer.cockpit.message.domain.model.*
import com.longcheer.cockpit.message.domain.repository.IMessageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 消息仓库实现类
 * 
 * 职责：
 * 1. 协调本地数据源（Room数据库）
 * 2. 处理数据映射（Entity与Domain Model之间转换）
 * 3. 实现仓库接口定义的所有操作
 * 
 * 设计原则：
 * - 单一职责：只处理数据访问和转换
 * - 依赖倒置：依赖于抽象接口（DAO）而非具体实现
 */
@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao,
    private val categoryDao: MessageCategoryDao,
    private val messageMapper: MessageMapper
) : IMessageRepository {

    override fun getMessages(filter: MessageFilter): Flow<List<Message>> {
        val offset = filter.offset()
        
        return messageDao.getMessages(
            userId = filter.userId?.toLongOrNull(),
            sourceApp = filter.sourceApp,
            categoryId = filter.category?.ordinal,
            priority = filter.priority?.level,
            isRead = filter.isRead,
            startTime = filter.startTime,
            endTime = filter.endTime,
            sortBy = filter.sortBy.name,
            limit = filter.pageSize,
            offset = offset
        ).map { entities ->
            entities.map { messageMapper.mapToDomain(it) }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun getMessageById(messageId: String): Result<Message> = withContext(Dispatchers.IO) {
        runCatching {
            val entity = messageDao.getMessageById(messageId)
                ?: throw NoSuchElementException("Message not found: $messageId")
            messageMapper.mapToDomain(entity)
        }
    }

    override suspend fun sendMessage(message: Message): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val entity = messageMapper.mapToEntity(message)
            val rowId = messageDao.insertMessage(entity)
            
            // 插入附件
            message.attachments.forEach { attachment ->
                val attachmentEntity = attachment.copy(messageId = rowId.toString())
                    .let { messageMapper.mapAttachmentToEntity(it) }
                messageDao.insertAttachment(attachmentEntity)
            }
            
            message.id
        }
    }

    override suspend fun sendMessages(messages: List<Message>): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val entities = messages.map { messageMapper.mapToEntity(it) }
            messageDao.insertMessages(entities).size
        }
    }

    override suspend fun markAsRead(messageId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val affected = messageDao.markAsRead(messageId)
            if (affected == 0) {
                throw NoSuchElementException("Message not found: $messageId")
            }
        }
    }

    override suspend fun markAsReadBatch(messageIds: List<String>): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            messageDao.markAsReadBatch(messageIds)
        }
    }

    override suspend fun markCategoryAsRead(category: MessageCategory): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            messageDao.markCategoryAsRead(category.ordinal)
        }
    }

    override suspend fun deleteMessage(messageId: String, softDelete: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (softDelete) {
                val affected = messageDao.softDelete(messageId)
                if (affected == 0) {
                    throw NoSuchElementException("Message not found: $messageId")
                }
            } else {
                // 硬删除需要先查询实体
                val entity = messageDao.getMessageById(messageId)?.message
                    ?: throw NoSuchElementException("Message not found: $messageId")
                messageDao.deleteMessage(entity)
            }
        }
    }

    override suspend fun deleteMessages(messageIds: List<String>): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            messageDao.deleteMessages(messageIds)
        }
    }

    override fun getUnreadCount(category: MessageCategory?): Flow<Int> {
        return messageDao.getUnreadCount(category?.ordinal)
            .flowOn(Dispatchers.IO)
    }

    override suspend fun cleanupExpiredMessages(beforeTime: Long): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            messageDao.cleanupExpired(beforeTime)
        }
    }

    override fun searchMessages(
        keyword: String,
        filter: MessageFilter
    ): Flow<List<Message>> {
        return messageDao.searchMessages(keyword)
            .map { entities ->
                entities.map { messageMapper.mapToDomain(it) }
                    .filter { message ->
                        // 应用额外的筛选条件
                        (filter.category == null || message.category == filter.category) &&
                        (filter.priority == null || message.priority == filter.priority) &&
                        (filter.startTime == null || message.createTime >= filter.startTime) &&
                        (filter.endTime == null || message.createTime <= filter.endTime)
                    }
            }.flowOn(Dispatchers.IO)
    }

    override suspend fun getMessageStatistics(
        startTime: Long,
        endTime: Long
    ): Result<MessageStatistics> = withContext(Dispatchers.IO) {
        runCatching {
            val rows = messageDao.getStatistics(startTime, endTime)
            
            val totalCount = rows.sumOf { it.total }
            val readCount = rows.sumOf { it.readCount }
            
            MessageStatistics(
                totalCount = totalCount,
                readCount = readCount,
                unreadCount = totalCount - readCount,
                byCategory = rows.groupBy {
                    MessageCategory.values().getOrNull(it.categoryId) ?: MessageCategory.OTHER
                }.mapValues { it.value.sumOf { row -> row.total } },
                byPriority = rows.groupBy {
                    MessagePriority.fromLevel(it.priority)
                }.mapValues { it.value.sumOf { row -> row.total } },
                bySource = rows.groupBy { it.sourceApp }
                    .mapValues { it.value.sumOf { row -> row.total } }
            )
        }
    }
}
