package com.longcheer.cockpit.message.domain.usecase

import com.longcheer.cockpit.message.domain.model.MessageCategory
import com.longcheer.cockpit.message.domain.repository.IMessageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 获取未读消息数量用例
 * 封装获取未读消息数量的业务逻辑
 */
class GetUnreadCountUseCase @Inject constructor(
    private val messageRepository: IMessageRepository
) {
    /**
     * 获取未读数量
     * @param category 分类筛选（null表示全部）
     * @return 未读数量的Flow流
     */
    operator fun invoke(category: MessageCategory? = null): Flow<Int> {
        return messageRepository.getUnreadCount(category)
    }
    
    /**
     * 获取指定分类的未读数量
     * @param category 消息分类
     * @return 未读数量的Flow流
     */
    fun byCategory(category: MessageCategory): Flow<Int> {
        return messageRepository.getUnreadCount(category)
    }
}
