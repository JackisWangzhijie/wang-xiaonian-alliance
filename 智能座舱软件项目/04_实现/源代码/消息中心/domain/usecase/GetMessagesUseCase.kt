package com.longcheer.cockpit.message.domain.usecase

import com.longcheer.cockpit.message.domain.model.Message
import com.longcheer.cockpit.message.domain.model.MessageCategory
import com.longcheer.cockpit.message.domain.model.MessageFilter
import com.longcheer.cockpit.message.domain.repository.IMessageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * 获取消息列表用例
 * 封装获取消息列表的业务逻辑
 */
class GetMessagesUseCase @Inject constructor(
    private val messageRepository: IMessageRepository
) {
    /**
     * 执行用例
     * @param filter 消息筛选条件
     * @return 消息列表的Flow流
     */
    operator fun invoke(filter: MessageFilter = MessageFilter()): Flow<List<Message>> {
        return messageRepository.getMessages(filter)
    }
}
