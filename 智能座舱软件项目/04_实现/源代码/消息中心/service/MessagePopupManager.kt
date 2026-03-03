package com.longcheer.cockpit.message.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.longcheer.cockpit.message.domain.model.Message
import com.longcheer.cockpit.message.domain.model.MessagePriority
import com.longcheer.cockpit.message.domain.model.PopupConfig
import com.longcheer.cockpit.message.domain.model.PopupPosition
import com.longcheer.cockpit.message.domain.model.PopupSize
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.PriorityQueue
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 消息弹窗管理器
 * ASIL等级: ASIL A
 * 需求追溯: REQ-MSG-FUN-004
 * 
 * 职责：
 * 1. 管理消息弹窗的显示和关闭
 * 2. 维护弹窗队列，按优先级排序
 * 3. 防止弹窗重叠和冲突
 */
@Singleton
class MessagePopupManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val MESSAGE_CHANNEL_ID = "cockpit_messages"
        const val POPUP_QUEUE_MAX_SIZE = 10
    }

    private val handler = Handler(Looper.getMainLooper())

    /**
     * 弹窗队列（按优先级排序）
     */
    private val popupQueue = PriorityQueue<PopupItem>(
        compareByDescending { it.message.priority.level }
    )

    /**
     * 当前显示的弹窗
     */
    private var currentPopup: Any? = null  // 实际应为PopupWindow或Compose弹窗

    /**
     * 显示消息弹窗
     * 如果当前有弹窗显示，将消息加入队列
     * 
     * @param message 要显示的消息
     * @param config 弹窗配置
     */
    @Synchronized
    fun show(message: Message, config: PopupConfig) {
        // 限制队列大小
        if (popupQueue.size >= POPUP_QUEUE_MAX_SIZE) {
            // 移除优先级最低的消息
            popupQueue.poll()
        }

        // 添加到队列
        popupQueue.offer(PopupItem(message, config))

        // 如果当前没有弹窗，立即显示
        if (currentPopup == null) {
            showNext()
        }
    }

    /**
     * 显示通知（非弹窗形式）
     * 用于低优先级消息或驾驶状态下的静默通知
     * 
     * @param message 要显示的消息
     */
    fun showNotification(message: Message) {
        val notification = createNotification(message)
        NotificationManagerCompat.from(context).notify(
            message.id.hashCode(),
            notification
        )
    }

    /**
     * 关闭当前弹窗
     * 显示队列中的下一个弹窗
     */
    @Synchronized
    fun dismissCurrent() {
        // 关闭当前弹窗
        currentPopup = null

        // 延迟显示下一个，避免闪烁
        handler.postDelayed({ showNext() }, 100)
    }

    /**
     * 清空弹窗队列
     */
    @Synchronized
    fun clearQueue() {
        popupQueue.clear()
    }

    /**
     * 获取当前队列大小
     */
    fun getQueueSize(): Int = popupQueue.size

    private fun showNext() {
        val item = popupQueue.poll() ?: return

        // 创建并显示弹窗
        // 实际实现应使用Compose或原生PopupWindow
        currentPopup = item

        // 设置自动关闭
        if (item.config.autoDismiss) {
            handler.postDelayed({ dismissCurrent() }, item.config.durationMs)
        }
    }

    private fun createNotification(message: Message): android.app.Notification {
        return NotificationCompat.Builder(context, MESSAGE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)  // 应使用实际图标资源
            .setContentTitle(message.title)
            .setContentText(message.content)
            .setPriority(getNotificationPriority(message.priority))
            .setAutoCancel(true)
            .build()
    }

    private fun getNotificationPriority(priority: MessagePriority): Int {
        return when (priority) {
            MessagePriority.P0_EMERGENCY -> NotificationCompat.PRIORITY_HIGH
            MessagePriority.P1_HIGH -> NotificationCompat.PRIORITY_DEFAULT
            MessagePriority.P2_MEDIUM -> NotificationCompat.PRIORITY_LOW
            MessagePriority.P3_LOW -> NotificationCompat.PRIORITY_MIN
        }
    }

    /**
     * 弹窗项数据类
     */
    private data class PopupItem(
        val message: Message,
        val config: PopupConfig
    )
}
