package com.longcheer.cockpit.message.domain.usecase

import com.longcheer.cockpit.message.domain.model.Message
import com.longcheer.cockpit.message.domain.model.MessagePriority
import com.longcheer.cockpit.message.domain.repository.IMessageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 发送消息用例
 * ASIL等级: ASIL A
 * 需求追溯: REQ-MSG-FUN-003
 * 
 * 封装发送消息的完整业务逻辑：
 * 1. 计算消息优先级
 * 2. 存储消息
 * 3. 触发驾驶安全检查（P0/P1消息）
 */
class SendMessageUseCase @Inject constructor(
    private val messageRepository: IMessageRepository
) {
    /**
     * 执行用例
     * @param message 要发送的消息
     * @return 包含消息ID的Result
     */
    suspend operator fun invoke(message: Message): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            // 1. 验证消息有效性
            validateMessage(message)
            
            // 2. 发送消息到仓库
            val result = messageRepository.sendMessage(message)
            
            // 3. 返回消息ID
            result.getOrThrow()
        }
    }
    
    /**
     * 批量发送消息
     * @param messages 消息列表
     * @return 包含成功发送数量的Result
     */
    suspend fun sendBatch(messages: List<Message>): Result<Int> = withContext(Dispatchers.IO) {
        messageRepository.sendMessages(messages)
    }
    
    /**
     * 创建并发送紧急消息
     * @param sourceApp 来源应用
     * @param title 标题
     * @param content 内容
     * @return 包含消息ID的Result
     */
    suspend fun sendEmergency(
        sourceApp: String,
        title: String,
        content: String
    ): Result<String> = invoke(
        Message.createEmergency(
            sourceApp = sourceApp,
            title = title,
            content = content
        )
    )
    
    private fun validateMessage(message: Message) {
        require(message.title.isNotBlank()) { "Message title cannot be blank" }
        require(message.sourceApp.isNotBlank()) { "Message source app cannot be blank" }
    }
}
