package com.longcheer.cockpit.message.domain.usecase

import com.longcheer.cockpit.message.domain.model.IConfigRepository
import com.longcheer.cockpit.message.domain.repository.IMessageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 清理过期消息用例
 * 需求追溯: REQ-MSG-FUN-002 (30天历史)
 * 
 * 封装清理过期消息的业务逻辑
 */
class CleanupExpiredMessagesUseCase @Inject constructor(
    private val messageRepository: IMessageRepository,
    private val configRepository: IConfigRepository
) {
    companion object {
        const val DEFAULT_RETENTION_DAYS = 30
        const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L
    }
    
    /**
     * 执行清理
     * @return 包含清理数量的Result
     */
    suspend operator fun invoke(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            // 1. 获取保留天数配置
            val retentionDays = configRepository.getMessageRetentionDays()
                .coerceAtLeast(7)  // 至少保留7天
            
            // 2. 计算截止时间
            val cutoffTime = System.currentTimeMillis() - (retentionDays * MILLIS_PER_DAY)
            
            // 3. 执行清理
            val result = messageRepository.cleanupExpiredMessages(cutoffTime)
            
            result.getOrThrow()
        }
    }
    
    /**
     * 按指定天数清理
     * @param retentionDays 保留天数
     * @return 包含清理数量的Result
     */
    suspend fun cleanupWithRetentionDays(retentionDays: Int): Result<Int> = withContext(Dispatchers.IO) {
        val cutoffTime = System.currentTimeMillis() - (retentionDays * MILLIS_PER_DAY)
        messageRepository.cleanupExpiredMessages(cutoffTime)
    }
    
    /**
     * 清理指定时间之前的数据
     * @param beforeTime 时间戳
     * @return 包含清理数量的Result
     */
    suspend fun cleanupBefore(beforeTime: Long): Result<Int> = withContext(Dispatchers.IO) {
        messageRepository.cleanupExpiredMessages(beforeTime)
    }
}
