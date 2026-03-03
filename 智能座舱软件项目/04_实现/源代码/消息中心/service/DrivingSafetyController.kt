package com.longcheer.cockpit.message.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.longcheer.cockpit.message.domain.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.PriorityQueue
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 驾驶安全控制器
 * ASIL等级: ASIL A
 * 需求追溯: REQ-MSG-FUN-004
 * 
 * 职责：
 * 1. 监控驾驶状态变化
 * 2. 根据驾驶状态控制消息显示策略
 * 3. 确保驾驶安全，防止弹窗干扰驾驶
 */
@Singleton
class DrivingSafetyController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val popupManager: MessagePopupManager,
    private val voiceAnnouncer: VoiceAnnouncer
) {
    private val safetyScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineName("SafetyController")
    )

    /**
     * 当前驾驶状态
     */
    private val _drivingState = MutableStateFlow(DrivingState.STOPPED)
    val drivingState: StateFlow<DrivingState> = _drivingState.asStateFlow()

    /**
     * 待处理消息队列（按优先级排序）
     */
    private val pendingQueue = PriorityQueue<QueuedMessage>(
        compareByDescending { it.message.priority.level }
    )

    init {
        // 监听驾驶状态变化
        startDrivingStateMonitoring()
    }

    /**
     * 检查并显示消息（带驾驶安全控制）
     * 根据当前驾驶状态决定是否显示弹窗或使用通知替代
     * 
     * @param message 要显示的消息
     */
    fun checkAndShow(message: Message) {
        val currentState = _drivingState.value

        when {
            // 停车状态 - 正常显示
            !currentState.isDriving() -> {
                popupManager.show(
                    message,
                    PopupConfig(
                        position = PopupPosition.TOP_SAFE,
                        size = PopupSize.NORMAL,
                        durationMs = 5000,
                        autoDismiss = true,
                        showIcon = true,
                        voiceAnnouncement = false
                    )
                )
            }
            // 行驶状态 - 只允许P0/P1消息显示弹窗
            message.priority.canShowWhileDriving() -> {
                showSafePopup(message, currentState)
            }
            // 其他消息 - 仅静默通知
            else -> {
                popupManager.showNotification(message)
            }
        }
    }

    /**
     * 获取当前驾驶状态下的安全弹窗配置
     * 
     * @return 安全的弹窗配置
     */
    fun getSafePopupConfig(): PopupConfig {
        val currentState = _drivingState.value
        return PopupConfig.drivingSafe(currentState)
    }

    /**
     * 更新驾驶状态
     * 当状态从行驶变为停车时，处理队列中的消息
     * 
     * @param state 新的驾驶状态
     */
    fun updateDrivingState(state: DrivingState) {
        val oldState = _drivingState.value
        _drivingState.value = state

        // 从行驶变为停车，处理队列中的消息
        if (oldState.isDriving() && !state.isDriving()) {
            processPendingQueue()
        }
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        safetyScope.cancel()
    }

    private fun startDrivingStateMonitoring() {
        // 这里应该连接车辆服务获取真实数据
        // 目前使用模拟数据
        safetyScope.launch {
            // 模拟驾驶状态监听
            // 实际实现应通过IVehicleService监听车速和挡位
        }
    }

    private fun showSafePopup(message: Message, state: DrivingState) {
        val config = PopupConfig(
            position = PopupPosition.BOTTOM_SAFE,
            size = PopupSize.COMPACT,
            durationMs = if (state == DrivingState.DRIVING_FAST) 3000L else 5000L,
            autoDismiss = true,
            showIcon = true,
            voiceAnnouncement = true
        )

        popupManager.show(message, config)

        // P0消息语音播报
        if (message.priority == MessagePriority.P0_EMERGENCY) {
            voiceAnnouncer.announce("${message.title}，${message.content}")
        }
    }

    private fun processPendingQueue() {
        while (pendingQueue.isNotEmpty()) {
            val queued = pendingQueue.poll() ?: break
            popupManager.show(queued.message, queued.config)
        }
    }

    /**
     * 队列中的消息数据类
     */
    private data class QueuedMessage(
        val message: Message,
        val config: PopupConfig
    )
}
