package com.longcheer.cockpit.message.domain.usecase

import com.longcheer.cockpit.message.domain.model.MessageCategory
import com.longcheer.cockpit.message.domain.repository.IMessageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 标记消息已读用例
 * 封装标记消息已读的业务逻辑
 */
class MarkAsReadUseCase @Inject constructor(
    private val messageRepository: IMessageRepository
) {
    /**
     * 标记单条消息已读
     * @param messageId 消息ID
     * @return 操作结果的Result
     */
    suspend operator fun invoke(messageId: String): Result<Unit> = withContext(Dispatchers.IO) {
        messageRepository.markAsRead(messageId)
    }
    
    /**
     * 批量标记消息已读
     * @param messageIds 消息ID列表
     * @return 包含成功更新数量的Result
     */
    suspend fun markBatch(messageIds: List<String>): Result<Int> = withContext(Dispatchers.IO) {
        messageRepository.markAsReadBatch(messageIds)
    }
    
    /**
     * 标记指定分类下所有消息已读
     * @param category 消息分类
     * @return 包含成功更新数量的Result
     */
    suspend fun markCategoryAsRead(category: MessageCategory): Result<Int> = withContext(Dispatchers.IO) {
        messageRepository.markCategoryAsRead(category)
    }
}
