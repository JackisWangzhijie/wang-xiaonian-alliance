package com.longcheer.cockpit.message.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.RemoteException
import com.longcheer.cockpit.message.aidl.IMessageListener
import com.longcheer.cockpit.message.aidl.IMessageService
import com.longcheer.cockpit.message.aidl.MessageFilterParcel
import com.longcheer.cockpit.message.aidl.MessageParcel
import com.longcheer.cockpit.message.domain.model.MessageCategory
import com.longcheer.cockpit.message.domain.model.MessagePriority
import com.longcheer.cockpit.message.domain.repository.IMessageRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import android.os.RemoteCallbackList
import javax.inject.Inject

/**
 * 消息服务 - Android系统服务
 * 
 * 作为AIDL接口的实现，提供跨进程消息服务
 * ASIL等级: ASIL A
 * 需求追溯: REQ-MSG-FUN-001~006
 * 
 * 功能：
 * 1. 消息发送和查询
 * 2. 消息状态管理（已读、删除）
 * 3. 优先级管理
 * 4. 消息监听回调
 */
@AndroidEntryPoint
class MessageService : Service() {

    @Inject
    lateinit var messageRepository: IMessageRepository

    @Inject
    lateinit var priorityCalculator: PriorityCalculator

    @Inject
    lateinit var drivingSafetyController: DrivingSafetyController

    @Inject
    lateinit var popupManager: MessagePopupManager

    private val binder = MessageServiceBinder()

    /**
     * 远程监听器列表
     * 使用RemoteCallbackList管理跨进程回调
     */
    private val listeners = RemoteCallbackList<IMessageListener>()

    override fun onCreate() {
        super.onCreate()
        // 服务初始化
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        listeners.kill()
        super.onDestroy()
    }

    /**
     * AIDL接口实现类
     */
    inner class MessageServiceBinder : IMessageService.Stub() {

        override fun sendMessage(msg: MessageParcel?): String {
            requireNotNull(msg) { "Message cannot be null" }

            return runBlocking(Dispatchers.IO) {
                try {
                    // 转换为领域模型
                    val message = msg.toDomainModel()

                    // 计算优先级
                    val priority = priorityCalculator.calculate(message)
                    val finalMessage = message.copy(priority = priority)

                    // 存储消息
                    val result = messageRepository.sendMessage(finalMessage)

                    result.fold(
                        onSuccess = { messageId ->
                            // 通知监听器
                            notifyNewMessage(finalMessage.toParcel())

                            // 检查驾驶安全并显示
                            if (finalMessage.priority.canShowWhileDriving()) {
                                drivingSafetyController.checkAndShow(finalMessage)
                            } else {
                                popupManager.showNotification(finalMessage)
                            }

                            messageId
                        },
                        onFailure = {
                            throw RemoteException("Failed to send message: ${it.message}")
                        }
                    )
                } catch (e: Exception) {
                    throw RemoteException("Send message error: ${e.message}")
                }
            }
        }

        override fun getMessages(filter: MessageFilterParcel?): List<MessageParcel> {
            return runBlocking(Dispatchers.IO) {
                val domainFilter = filter?.toDomainModel() ?: com.longcheer.cockpit.message.domain.model.MessageFilter()

                messageRepository.getMessages(domainFilter)
                    .first()
                    .map { it.toParcel() }
            }
        }

        override fun setAppMessagePriority(appId: String?, priority: Int) {
            requireNotNull(appId) { "AppId cannot be null" }
            priorityCalculator.setAppPriority(
                appId,
                MessagePriority.fromLevel(priority)
            )
        }

        override fun markAsRead(messageId: String?) {
            requireNotNull(messageId) { "MessageId cannot be null" }

            runBlocking(Dispatchers.IO) {
                messageRepository.markAsRead(messageId)
                    .onSuccess {
                        notifyMessageRead(messageId)
                    }
            }
        }

        override fun deleteMessage(messageId: String?) {
            requireNotNull(messageId) { "MessageId cannot be null" }

            runBlocking(Dispatchers.IO) {
                messageRepository.deleteMessage(messageId)
                    .onSuccess {
                        notifyMessageDeleted(messageId)
                    }
            }
        }

        override fun registerMessageListener(listener: IMessageListener?) {
            listener?.let { listeners.register(it) }
        }

        override fun unregisterMessageListener(listener: IMessageListener?) {
            listener?.let { listeners.unregister(it) }
        }

        override fun getUnreadCount(category: String?): Int {
            return runBlocking(Dispatchers.IO) {
                val cat = category?.let { MessageCategory.fromCode(it) }
                messageRepository.getUnreadCount(cat).first()
            }
        }

        override fun cleanupMessages(beforeTime: Long): Int {
            return runBlocking(Dispatchers.IO) {
                messageRepository.cleanupExpiredMessages(beforeTime)
                    .getOrDefault(0)
            }
        }
    }

    /**
     * 通知所有监听器有新消息
     */
    private fun notifyNewMessage(message: MessageParcel) {
        val n = listeners.beginBroadcast()
        for (i in 0 until n) {
            try {
                listeners.getBroadcastItem(i).onNewMessage(message)
            } catch (e: RemoteException) {
                // 忽略远程异常
            }
        }
        listeners.finishBroadcast()
    }

    /**
     * 通知所有监听器消息已读
     */
    private fun notifyMessageRead(messageId: String) {
        val n = listeners.beginBroadcast()
        for (i in 0 until n) {
            try {
                listeners.getBroadcastItem(i).onMessageRead(messageId)
            } catch (e: RemoteException) {
                // 忽略远程异常
            }
        }
        listeners.finishBroadcast()
    }

    /**
     * 通知所有监听器消息已删除
     */
    private fun notifyMessageDeleted(messageId: String) {
        val n = listeners.beginBroadcast()
        for (i in 0 until n) {
            try {
                listeners.getBroadcastItem(i).onMessageDeleted(messageId)
            } catch (e: RemoteException) {
                // 忽略远程异常
            }
        }
        listeners.finishBroadcast()
    }
}
