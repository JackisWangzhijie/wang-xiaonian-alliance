package com.longcheer.cockpit.message.domain.usecase

import com.longcheer.cockpit.message.domain.model.MessageCategory
import com.longcheer.cockpit.message.domain.repository.IMessageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 删除消息用例
 * 封装删除消息的业务逻辑
 */
class DeleteMessageUseCase @Inject constructor(
    private val messageRepository: IMessageRepository
) {
    /**
     * 删除单条消息
     * @param messageId 消息ID
     * @param softDelete true表示软删除，false表示硬删除
     * @return 操作结果的Result
     */
    suspend operator fun invoke(
        messageId: String, 
        softDelete: Boolean = true
    ): Result<Unit> = withContext(Dispatchers.IO) {
        messageRepository.deleteMessage(messageId, softDelete)
    }
    
    /**
     * 批量删除消息
     * @param messageIds 消息ID列表
     * @return 包含成功删除数量的Result
     */
    suspend fun deleteBatch(messageIds: List<String>): Result<Int> = withContext(Dispatchers.IO) {
        messageRepository.deleteMessages(messageIds)
    }
}
