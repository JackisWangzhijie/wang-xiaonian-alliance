package com.longcheer.cockpit.message.domain.usecase

import com.longcheer.cockpit.message.domain.model.MessageFilter
import com.longcheer.cockpit.message.domain.model.MessageStatistics
import com.longcheer.cockpit.message.domain.repository.IMessageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 获取消息统计用例
 * 封装获取消息统计信息的业务逻辑
 */
class GetMessageStatisticsUseCase @Inject constructor(
    private val messageRepository: IMessageRepository
) {
    /**
     * 获取指定时间范围的统计信息
     * @param startTime 开始时间戳
     * @param endTime 结束时间戳
     * @return 包含统计信息的Result
     */
    suspend operator fun invoke(
        startTime: Long,
        endTime: Long
    ): Result<MessageStatistics> = withContext(Dispatchers.IO) {
        messageRepository.getMessageStatistics(startTime, endTime)
    }
    
    /**
     * 获取最近N天的统计信息
     * @param days 天数
     * @return 包含统计信息的Result
     */
    suspend fun forLastDays(days: Int): Result<MessageStatistics> = withContext(Dispatchers.IO) {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - (days * 24 * 60 * 60 * 1000)
        messageRepository.getMessageStatistics(startTime, endTime)
    }
    
    /**
     * 获取今日统计
     * @return 包含今日统计信息的Result
     */
    suspend fun forToday(): Result<MessageStatistics> = forLastDays(1)
}
