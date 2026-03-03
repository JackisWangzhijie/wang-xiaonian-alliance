package com.longcheer.cockpit.message.data.local.dao

import androidx.room.*
import com.longcheer.cockpit.message.data.local.entity.MessageEntity
import com.longcheer.cockpit.message.data.local.entity.MessageWithCategory
import com.longcheer.cockpit.message.data.local.entity.MessageStatisticsRow
import kotlinx.coroutines.flow.Flow

/**
 * 消息数据访问对象
 * 提供消息的增删改查操作
 */
@Dao
interface MessageDao {
    
    // ==================== 查询操作 ====================
    
    /**
     * 根据筛选条件查询消息列表
     * 支持分页、排序和多条件筛选
     * 
     * @return Flow<List<MessageWithCategory>> 可观察的消息列表流
     */
    @Query(
        """
        SELECT m.*, c.* FROM message m
        LEFT JOIN message_category c ON m.category_id = c.id
        WHERE (:userId IS NULL OR m.user_id = :userId)
        AND (:sourceApp IS NULL OR m.source_app = :sourceApp)
        AND (:categoryId IS NULL OR m.category_id = :categoryId)
        AND (:priority IS NULL OR m.priority = :priority)
        AND (:isRead IS NULL OR m.is_read = :isRead)
        AND (:startTime IS NULL OR m.create_time >= :startTime)
        AND (:endTime IS NULL OR m.create_time <= :endTime)
        AND m.is_deleted = 0
        ORDER BY 
            CASE WHEN :sortBy = 'TIME' THEN m.create_time END DESC,
            CASE WHEN :sortBy = 'PRIORITY' THEN m.priority END ASC,
            CASE WHEN :sortBy = 'CATEGORY' THEN m.category_id END ASC
        LIMIT :limit OFFSET :offset
        """
    )
    fun getMessages(
        userId: Long? = null,
        sourceApp: String? = null,
        categoryId: Int? = null,
        priority: Int? = null,
        isRead: Boolean? = null,
        startTime: Long? = null,
        endTime: Long? = null,
        sortBy: String = "TIME",
        limit: Int = 20,
        offset: Int = 0
    ): Flow<List<MessageWithCategory>>
    
    /**
     * 根据ID查询单条消息
     * 
     * @param msgId 消息ID
     * @return 消息关联实体，不存在时返回null
     */
    @Query(
        """
        SELECT m.*, c.* FROM message m
        LEFT JOIN message_category c ON m.category_id = c.id
        WHERE m.msg_id = :msgId AND m.is_deleted = 0
        """
    )
    suspend fun getMessageById(msgId: String): MessageWithCategory?
    
    /**
     * 获取未读消息数量
     * 使用Flow实现实时更新
     * 
     * @param categoryId 分类ID筛选（null表示全部）
     * @return 未读数量的Flow流
     */
    @Query(
        """
        SELECT COUNT(*) FROM message 
        WHERE is_read = 0 AND is_deleted = 0
        AND (:categoryId IS NULL OR category_id = :categoryId)
        """
    )
    fun getUnreadCount(categoryId: Int? = null): Flow<Int>
    
    /**
     * 关键词搜索消息
     * 搜索标题和内容字段
     * 
     * @param keyword 关键词
     * @return 匹配的消息列表Flow
     */
    @Query(
        """
        SELECT m.*, c.* FROM message m
        LEFT JOIN message_category c ON m.category_id = c.id
        WHERE (m.title LIKE '%' || :keyword || '%' 
               OR m.content LIKE '%' || :keyword || '%')
        AND m.is_deleted = 0
        ORDER BY m.create_time DESC
        """
    )
    fun searchMessages(keyword: String): Flow<List<MessageWithCategory>>
    
    /**
     * 获取消息统计信息
     * 
     * @param startTime 开始时间戳
     * @param endTime 结束时间戳
     * @return 统计行数据列表
     */
    @Query(
        """
        SELECT 
            COUNT(*) as total,
            SUM(CASE WHEN is_read = 1 THEN 1 ELSE 0 END) as read_count,
            category_id,
            priority,
            source_app
        FROM message
        WHERE create_time BETWEEN :startTime AND :endTime
        AND is_deleted = 0
        GROUP BY category_id, priority, source_app
        """
    )
    suspend fun getStatistics(startTime: Long, endTime: Long): List<MessageStatisticsRow>
    
    /**
     * 根据消息ID列表查询消息
     * 用于批量操作后刷新数据
     */
    @Query(
        """
        SELECT m.*, c.* FROM message m
        LEFT JOIN message_category c ON m.category_id = c.id
        WHERE m.msg_id IN (:msgIds) AND m.is_deleted = 0
        """
    )
    suspend fun getMessagesByIds(msgIds: List<String>): List<MessageWithCategory>
    
    // ==================== 插入操作 ====================
    
    /**
     * 插入单条消息
     * 如果ID冲突则替换
     * 
     * @param message 消息实体
     * @return 插入的行ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long
    
    /**
     * 批量插入消息
     * 
     * @param messages 消息实体列表
     * @return 插入的行ID列表
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>): List<Long>
    
    /**
     * 插入附件
     * 
     * @param attachment 附件实体
     * @return 插入的行ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: com.longcheer.cockpit.message.data.local.entity.MessageAttachmentEntity): Long
    
    // ==================== 更新操作 ====================
    
    /**
     * 标记单条消息已读
     * 
     * @param msgId 消息ID
     * @param readTime 阅读时间戳
     * @param updateTime 更新时间戳
     * @return 受影响的行数
     */
    @Query(
        """
        UPDATE message 
        SET is_read = 1, read_time = :readTime, update_time = :updateTime
        WHERE msg_id = :msgId
        """
    )
    suspend fun markAsRead(
        msgId: String, 
        readTime: Long = System.currentTimeMillis(),
        updateTime: Long = System.currentTimeMillis()
    ): Int
    
    /**
     * 批量标记消息已读
     * 
     * @param msgIds 消息ID列表
     * @param readTime 阅读时间戳
     * @param updateTime 更新时间戳
     * @return 受影响的行数
     */
    @Query(
        """
        UPDATE message 
        SET is_read = 1, read_time = :readTime, update_time = :updateTime
        WHERE msg_id IN (:msgIds)
        """
    )
    suspend fun markAsReadBatch(
        msgIds: List<String>,
        readTime: Long = System.currentTimeMillis(),
        updateTime: Long = System.currentTimeMillis()
    ): Int
    
    /**
     * 按分类标记所有消息已读
     * 
     * @param categoryId 分类ID
     * @param readTime 阅读时间戳
     * @param updateTime 更新时间戳
     * @return 受影响的行数
     */
    @Query(
        """
        UPDATE message 
        SET is_read = 1, read_time = :readTime, update_time = :updateTime
        WHERE category_id = :categoryId AND is_deleted = 0
        """
    )
    suspend fun markCategoryAsRead(
        categoryId: Int,
        readTime: Long = System.currentTimeMillis(),
        updateTime: Long = System.currentTimeMillis()
    ): Int
    
    /**
     * 更新消息内容
     * 
     * @param message 消息实体
     * @return 受影响的行数
     */
    @Update
    suspend fun updateMessage(message: MessageEntity): Int
    
    // ==================== 删除操作 ====================
    
    /**
     * 软删除消息（标记删除）
     * 
     * @param msgId 消息ID
     * @param updateTime 更新时间戳
     * @return 受影响的行数
     */
    @Query(
        """
        UPDATE message 
        SET is_deleted = 1, update_time = :updateTime
        WHERE msg_id = :msgId
        """
    )
    suspend fun softDelete(
        msgId: String, 
        updateTime: Long = System.currentTimeMillis()
    ): Int
    
    /**
     * 硬删除单条消息
     * 
     * @param message 消息实体
     * @return 受影响的行数
     */
    @Delete
    suspend fun deleteMessage(message: MessageEntity): Int
    
    /**
     * 根据ID列表批量删除消息
     * 
     * @param msgIds 消息ID列表
     * @return 删除的行数
     */
    @Query("DELETE FROM message WHERE msg_id IN (:msgIds)")
    suspend fun deleteMessages(msgIds: List<String>): Int
    
    /**
     * 清理过期消息
     * 
     * @param beforeTime 此时间戳之前的消息将被删除
     * @return 删除的行数
     */
    @Query("DELETE FROM message WHERE create_time < :beforeTime")
    suspend fun cleanupExpired(beforeTime: Long): Int
    
    /**
     * 清理已删除的消息（物理删除）
     * 
     * @return 删除的行数
     */
    @Query("DELETE FROM message WHERE is_deleted = 1")
    suspend fun cleanupDeleted(): Int
    
    /**
     * 获取已删除消息数量
     * 
     * @return 已删除消息数量
     */
    @Query("SELECT COUNT(*) FROM message WHERE is_deleted = 1")
    suspend fun getDeletedCount(): Int
}
