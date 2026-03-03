package com.longcheer.cockpit.message.data.mapper

import com.longcheer.cockpit.message.data.local.entity.*
import com.longcheer.cockpit.message.domain.model.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 消息实体与领域模型映射器
 * 负责在数据库实体（Entity）与领域模型（Domain Model）之间转换
 * 
 * 遵循单一职责原则：仅处理数据转换，不包含业务逻辑
 */
@Singleton
class MessageMapper @Inject constructor() {
    
    // ==================== Message 映射 ====================
    
    /**
     * 将消息关联实体转换为领域模型
     * 
     * @param entity 包含消息、分类、附件的关联实体
     * @return 领域模型Message
     */
    fun mapToDomain(entity: MessageWithCategory): Message {
        return Message(
            id = entity.message.msgId,
            sourceApp = entity.message.sourceApp,
            sourceName = entity.message.sourceName ?: "",
            category = MessageCategory.values().getOrElse(entity.message.categoryId) { MessageCategory.OTHER },
            priority = MessagePriority.fromLevel(entity.message.priority),
            title = entity.message.title,
            content = entity.message.content ?: "",
            contentType = ContentType.fromValue(entity.message.contentType),
            actionType = ActionType.fromValue(entity.message.actionType),
            actionData = entity.message.actionData,
            iconUrl = entity.message.iconUrl,
            attachments = entity.attachments.map { mapAttachmentToDomain(it) },
            userId = entity.message.userId.toString(),
            isRead = entity.message.isRead,
            isDeleted = entity.message.isDeleted,
            readTime = entity.message.readTime,
            expireTime = entity.message.expireTime,
            createTime = entity.message.createTime,
            updateTime = entity.message.updateTime
        )
    }
    
    /**
     * 将领域模型转换为消息实体
     * 
     * @param domain 领域模型Message
     * @return 数据库实体MessageEntity
     */
    fun mapToEntity(domain: Message): MessageEntity {
        return MessageEntity(
            msgId = domain.id,
            sourceApp = domain.sourceApp,
            sourceName = domain.sourceName.takeIf { it.isNotBlank() },
            categoryId = domain.category.ordinal,
            priority = domain.priority.level,
            title = domain.title,
            content = domain.content.takeIf { it.isNotBlank() },
            contentType = domain.contentType.value,
            actionType = domain.actionType.value,
            actionData = domain.actionData,
            iconUrl = domain.iconUrl,
            userId = domain.userId.toLongOrNull() ?: 0,
            isRead = domain.isRead,
            isDeleted = domain.isDeleted,
            readTime = domain.readTime,
            expireTime = domain.expireTime,
            createTime = domain.createTime,
            updateTime = domain.updateTime
        )
    }
    
    /**
     * 批量映射领域模型列表到实体列表
     */
    fun mapToEntityList(domains: List<Message>): List<MessageEntity> {
        return domains.map { mapToEntity(it) }
    }
    
    /**
     * 批量映射关联实体列表到领域模型列表
     */
    fun mapToDomainList(entities: List<MessageWithCategory>): List<Message> {
        return entities.map { mapToDomain(it) }
    }
    
    // ==================== Attachment 映射 ====================
    
    /**
     * 将附件实体转换为领域模型
     * 
     * @param entity 附件实体
     * @return 领域模型Attachment
     */
    fun mapAttachmentToDomain(entity: MessageAttachmentEntity): Attachment {
        return Attachment(
            id = entity.id.toString(),
            messageId = entity.msgId.toString(),
            fileName = entity.fileName,
            filePath = entity.filePath,
            fileType = FileType.fromValue(entity.fileType),
            fileSize = entity.fileSize,
            mimeType = entity.mimeType ?: "",
            thumbnailPath = entity.thumbnail,
            createTime = entity.createTime
        )
    }
    
    /**
     * 将领域模型转换为附件实体
     * 
     * @param domain 附件领域模型
     * @return 附件实体
     */
    fun mapAttachmentToEntity(domain: Attachment): MessageAttachmentEntity {
        return MessageAttachmentEntity(
            msgId = domain.messageId.toLongOrNull() ?: 0,
            fileName = domain.fileName,
            filePath = domain.filePath,
            fileType = domain.fileType.value,
            fileSize = domain.fileSize,
            mimeType = domain.mimeType.takeIf { it.isNotBlank() },
            thumbnail = domain.thumbnailPath
        )
    }
    
    /**
     * 将附件实体列表映射为领域模型列表
     */
    fun mapAttachmentToDomainList(entities: List<MessageAttachmentEntity>): List<Attachment> {
        return entities.map { mapAttachmentToDomain(it) }
    }
    
    // ==================== Category 映射 ====================
    
    /**
     * 将分类实体转换为领域模型（使用MessageCategory枚举）
     * 
     * @param entity 分类实体
     * @return 领域模型MessageCategory
     */
    fun mapCategoryToDomain(entity: MessageCategoryEntity): MessageCategory {
        return MessageCategory.values().getOrElse(entity.id) { MessageCategory.OTHER }
    }
    
    /**
     * 将领域模型枚举转换为分类实体
     * 
     * @param domain 分类枚举
     * @return 分类实体
     */
    fun mapCategoryToEntity(domain: MessageCategory): MessageCategoryEntity {
        return MessageCategoryEntity(
            catCode = domain.code,
            catName = domain.displayName,
            sortOrder = domain.ordinal
        )
    }
}
