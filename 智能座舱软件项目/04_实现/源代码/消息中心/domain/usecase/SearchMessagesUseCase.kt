package com.longcheer.cockpit.message.domain.usecase

import com.longcheer.cockpit.message.domain.model.MessageFilter
import com.longcheer.cockpit.message.domain.model.Message
import com.longcheer.cockpit.message.domain.repository.IMessageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 搜索消息用例
 * 封装搜索消息的业务逻辑
 */
class SearchMessagesUseCase @Inject constructor(
    private val messageRepository: IMessageRepository
) {
    /**
     * 搜索消息
     * @param keyword 关键词
     * @param filter 基础筛选条件
     * @return 符合条件的消息列表Flow
     */
    operator fun invoke(
        keyword: String,
        filter: MessageFilter = MessageFilter()
    ): Flow<List<Message>> {
        // 如果关键词为空，返回普通消息列表
        if (keyword.isBlank()) {
            return messageRepository.getMessages(filter)
        }
        return messageRepository.searchMessages(keyword, filter)
    }
    
    /**
     * 快速搜索（不带筛选条件）
     * @param keyword 关键词
     * @return 符合条件的消息列表Flow
     */
    fun quickSearch(keyword: String): Flow<List<Message>> {
        return invoke(keyword, MessageFilter())
    }
}
