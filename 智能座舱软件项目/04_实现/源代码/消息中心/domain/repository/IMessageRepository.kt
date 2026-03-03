package com.longcheer.cockpit.message.domain.repository

import com.longcheer.cockpit.message.domain.model.Message
import com.longcheer.cockpit.message.domain.model.MessageCategory
import com.longcheer.cockpit.message.domain.model.MessageFilter
import com.longcheer.cockpit.message.domain.model.MessageStatistics
import kotlinx.coroutines.flow.Flow

/**
 * 消息仓库接口
 * 定义消息数据访问的抽象接口，遵循依赖倒置原则
 * 需求追溯: REQ-MSG-FUN-001, REQ-MSG-FUN-002
 * 
 * 实现类应处理本地数据库和远程数据源的协调
 * 所有操作都应支持协程和Flow响应式编程
 */
interface IMessageRepository {
    
    /**
     * 获取消息列表（Flow实时更新）
     * 返回的数据流会自动更新当数据库中的数据发生变化时
     * 
     * @param filter 筛选条件，默认为空筛选
     * @return 消息列表的Flow流
     */
    fun getMessages(filter: MessageFilter = MessageFilter()): Flow<List<Message>>
    
    /**
     * 根据ID获取单条消息
     * 
     * @param messageId 消息ID
     * @return 包含消息的Result，失败时包含异常信息
     */
    suspend fun getMessageById(messageId: String): Result<Message>
    
    /**
     * 发送/存储消息
     * 
     * @param message 消息对象
     * @return 包含消息ID的Result，失败时包含异常信息
     */
    suspend fun sendMessage(message: Message): Result<String>
    
    /**
     * 批量发送消息
     * 
     * @param messages 消息列表
     * @return 包含成功插入数量的Result
     */
    suspend fun sendMessages(messages: List<Message>): Result<Int>
    
    /**
     * 标记消息为已读
     * 
     * @param messageId 消息ID
     * @return 操作结果的Result
     */
    suspend fun markAsRead(messageId: String): Result<Unit>
    
    /**
     * 批量标记消息为已读
     * 
     * @param messageIds 消息ID列表
     * @return 包含成功更新数量的Result
     */
    suspend fun markAsReadBatch(messageIds: List<String>): Result<Int>
    
    /**
     * 标记指定分类下所有消息为已读
     * 
     * @param category 消息分类
     * @return 包含成功更新数量的Result
     */
    suspend fun markCategoryAsRead(category: MessageCategory): Result<Int>
    
    /**
     * 删除消息
     * 
     * @param messageId 消息ID
     * @param softDelete true表示软删除（标记删除），false表示硬删除
     * @return 操作结果的Result
     */
    suspend fun deleteMessage(messageId: String, softDelete: Boolean = true): Result<Unit>
    
    /**
     * 批量删除消息
     * 
     * @param messageIds 消息ID列表
     * @return 包含成功删除数量的Result
     */
    suspend fun deleteMessages(messageIds: List<String>): Result<Int>
    
    /**
     * 获取未读消息数量（Flow实时更新）
     * 
     * @param category 分类筛选（null表示全部）
     * @return 未读数量的Flow流
     */
    fun getUnreadCount(category: MessageCategory? = null): Flow<Int>
    
    /**
     * 清理过期消息
     * 
     * @param beforeTime 清理此时间戳之前的消息
     * @return 包含清理数量的Result
     */
    suspend fun cleanupExpiredMessages(beforeTime: Long): Result<Int>
    
    /**
     * 搜索消息
     * 
     * @param keyword 关键词
     * @param filter 基础筛选条件
     * @return 符合条件的消息列表Flow
     */
    fun searchMessages(keyword: String, filter: MessageFilter = MessageFilter()): Flow<List<Message>>
    
    /**
     * 获取消息统计信息
     * 
     * @param startTime 开始时间戳
     * @param endTime 结束时间戳
     * @return 包含统计信息的Result
     */
    suspend fun getMessageStatistics(startTime: Long, endTime: Long): Result<MessageStatistics>
}

/**
 * 配置仓库接口
 * 用于获取系统配置信息
 */
interface IConfigRepository {
    /**
     * 获取消息保留天数
     * @return 消息保留天数（默认30天）
     */
    suspend fun getMessageRetentionDays(): Int
    
    /**
     * 获取最大消息数量限制
     * @return 最大消息数量
     */
    suspend fun getMaxMessageCount(): Int
    
    /**
     * 获取是否启用智能推荐
     * @return true表示启用
     */
    suspend fun isSmartRecommendationEnabled(): Boolean
}

/**
 * 应用使用数据仓库接口
 */
interface IAppUsageRepository {
    /**
     * 获取最近的应用使用数据
     * 
     * @param days 查询天数
     * @return 应用使用数据列表
     */
    suspend fun getRecentUsage(days: Int): List<com.longcheer.cockpit.message.domain.model.AppUsage>
}
