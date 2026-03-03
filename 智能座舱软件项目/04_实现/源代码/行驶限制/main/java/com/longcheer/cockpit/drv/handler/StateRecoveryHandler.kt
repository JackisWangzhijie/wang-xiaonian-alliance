package com.longcheer.cockpit.drv.handler

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 状态恢复处理器
 * ASIL等级: ASIL B
 * 
 * 职责:
 * - 管理恢复延迟定时器
 * - 恢复条件验证
 * - 恢复执行协调
 */
class StateRecoveryHandler {

    companion object {
        private const val TAG = "StateRecoveryHandler"
        
        /** 默认恢复延迟: 3秒 */
        const val DEFAULT_RECOVERY_DELAY_MS = 3000L
        
        /** 最小恢复延迟: 1秒 */
        const val MIN_RECOVERY_DELAY_MS = 1000L
        
        /** 最大恢复延迟: 10秒 */
        const val MAX_RECOVERY_DELAY_MS = 10000L
    }

    // 恢复延迟时间
    private var recoveryDelayMs: Long = DEFAULT_RECOVERY_DELAY_MS
    
    // 调度器
    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "StateRecoveryHandler-Thread").apply { isDaemon = true }
    }
    
    // 恢复定时器任务
    private var recoveryTask: java.util.concurrent.ScheduledFuture<*>? = null
    
    // 恢复条件检查回调
    private var conditionChecker: (() -> Boolean)? = null
    
    // 恢复执行回调
    private var recoveryExecutor: (() -> Unit)? = null
    
    // 恢复状态
    private val isRecoveryScheduled = AtomicBoolean(false)
    private val isRecoveryInProgress = AtomicBoolean(false)
    
    // 待恢复应用列表
    private val pendingApps = CopyOnWriteArrayList<String>()
    
    // 稳定状态记录
    private var stableStateStartTime: Long = 0
    private var lastVehicleSpeed: Int = -1
    private var lastGearPosition: Int = -1
    private var lastParkingBrake: Boolean = false
    
    // 主线程Handler用于UI回调
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // 恢复监听
    private val recoveryListeners = mutableListOf<RecoveryListener>()

    /**
     * 恢复监听接口
     */
    interface RecoveryListener {
        fun onRecoveryScheduled(delayMs: Long)
        fun onRecoveryStarted()
        fun onRecoveryCompleted()
        fun onRecoveryCancelled(reason: String)
    }

    /**
     * 初始化恢复处理器
     * 
     * @param delayMs 恢复延迟时间
     * @param conditionChecker 恢复条件检查回调
     * @param recoveryExecutor 恢复执行回调
     */
    fun initialize(
        delayMs: Long = DEFAULT_RECOVERY_DELAY_MS,
        conditionChecker: () -> Boolean,
        recoveryExecutor: () -> Unit
    ) {
        this.recoveryDelayMs = delayMs.coerceIn(MIN_RECOVERY_DELAY_MS, MAX_RECOVERY_DELAY_MS)
        this.conditionChecker = conditionChecker
        this.recoveryExecutor = recoveryExecutor
        
        Log.i(TAG, "StateRecoveryHandler initialized with delay=${recoveryDelayMs}ms")
    }

    /**
     * 检查是否可以恢复
     * 
     * 恢复条件:
     * - 车速 = 0 km/h
     * - 挡位 = P挡
     * - 驻车制动 = 启用
     * - 状态稳定超过3秒
     * 
     * @param vehicleSpeed 当前车速
     * @param gearPosition 当前挡位 (0=P, 1=R, 2=N, 3=D)
     * @param parkingBrake 驻车制动状态
     * @return 是否可以恢复
     */
    fun canRecover(
        vehicleSpeed: Int,
        gearPosition: Int,
        parkingBrake: Boolean
    ): Boolean {
        // 基础条件检查
        if (vehicleSpeed != 0) {
            resetStableState()
            return false
        }
        
        if (gearPosition != 0) { // P挡
            resetStableState()
            return false
        }
        
        if (!parkingBrake) {
            resetStableState()
            return false
        }
        
        // 检查状态稳定性
        if (!isStateStable(vehicleSpeed, gearPosition, parkingBrake)) {
            return false
        }
        
        val stableDuration = SystemClock.elapsedRealtime() - stableStateStartTime
        return stableDuration >= recoveryDelayMs
    }

    /**
     * 检查状态是否稳定
     */
    private fun isStateStable(
        vehicleSpeed: Int,
        gearPosition: Int,
        parkingBrake: Boolean
    ): Boolean {
        if (vehicleSpeed != lastVehicleSpeed ||
            gearPosition != lastGearPosition ||
            parkingBrake != lastParkingBrake) {
            
            // 状态变化，重置稳定时间
            lastVehicleSpeed = vehicleSpeed
            lastGearPosition = gearPosition
            lastParkingBrake = parkingBrake
            stableStateStartTime = SystemClock.elapsedRealtime()
            
            return false
        }
        
        return stableStateStartTime > 0
    }

    /**
     * 重置稳定状态
     */
    private fun resetStableState() {
        stableStateStartTime = 0
        lastVehicleSpeed = -1
        lastGearPosition = -1
        lastParkingBrake = false
    }

    /**
     * 调度恢复
     * 
     * @param onRecovery 恢复回调
     */
    fun scheduleRecovery(onRecovery: () -> Unit) {
        if (isRecoveryScheduled.get()) {
            Log.d(TAG, "Recovery already scheduled")
            return
        }
        
        if (isRecoveryInProgress.get()) {
            Log.d(TAG, "Recovery already in progress")
            return
        }
        
        isRecoveryScheduled.set(true)
        
        // 通知监听者
        recoveryListeners.forEach { it.onRecoveryScheduled(recoveryDelayMs) }
        
        Log.i(TAG, "Recovery scheduled in ${recoveryDelayMs}ms")
        
        // 启动定时器
        recoveryTask = executor.schedule({
            executeRecovery(onRecovery)
        }, recoveryDelayMs, TimeUnit.MILLISECONDS)
    }

    /**
     * 执行恢复
     */
    private fun executeRecovery(onRecovery: () -> Unit) {
        if (isRecoveryInProgress.get()) {
            return
        }
        
        isRecoveryInProgress.set(true)
        isRecoveryScheduled.set(false)
        
        // 检查恢复条件
        val condition = conditionChecker?.invoke() ?: false
        
        if (!condition) {
            Log.w(TAG, "Recovery condition not met, cancelling recovery")
            isRecoveryInProgress.set(false)
            mainHandler.post {
                recoveryListeners.forEach { it.onRecoveryCancelled("Condition not met") }
            }
            return
        }
        
        Log.i(TAG, "Executing recovery...")
        
        mainHandler.post {
            recoveryListeners.forEach { it.onRecoveryStarted() }
        }
        
        val startTime = SystemClock.elapsedRealtime()
        
        try {
            // 执行恢复
            recoveryExecutor?.invoke()
            onRecovery()
            
            val elapsed = SystemClock.elapsedRealtime() - startTime
            Log.i(TAG, "Recovery completed in ${elapsed}ms")
            
            // 检查延迟要求
            if (elapsed > 500) {
                Log.w(TAG, "Recovery took ${elapsed}ms, exceeds 500ms requirement")
            }
            
            mainHandler.post {
                recoveryListeners.forEach { it.onRecoveryCompleted() }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during recovery", e)
            mainHandler.post {
                recoveryListeners.forEach { it.onRecoveryCancelled("Exception: ${e.message}") }
            }
        } finally {
            isRecoveryInProgress.set(false)
            pendingApps.clear()
        }
    }

    /**
     * 取消恢复
     */
    fun cancelRecovery(reason: String = "User cancelled") {
        if (!isRecoveryScheduled.get()) {
            return
        }
        
        recoveryTask?.cancel(false)
        recoveryTask = null
        isRecoveryScheduled.set(false)
        
        Log.i(TAG, "Recovery cancelled: $reason")
        
        mainHandler.post {
            recoveryListeners.forEach { it.onRecoveryCancelled(reason) }
        }
    }

    /**
     * 添加待恢复应用
     */
    fun addPendingApp(packageName: String) {
        if (!pendingApps.contains(packageName)) {
            pendingApps.add(packageName)
        }
    }

    /**
     * 移除待恢复应用
     */
    fun removePendingApp(packageName: String) {
        pendingApps.remove(packageName)
    }

    /**
     * 获取待恢复应用列表
     */
    fun getPendingApps(): List<String> {
        return pendingApps.toList()
    }

    /**
     * 添加恢复监听
     */
    fun addRecoveryListener(listener: RecoveryListener) {
        recoveryListeners.add(listener)
    }

    /**
     * 移除恢复监听
     */
    fun removeRecoveryListener(listener: RecoveryListener) {
        recoveryListeners.remove(listener)
    }

    /**
     * 获取恢复处理器状态
     */
    fun getStatus(): RecoveryHandlerStatus {
        return RecoveryHandlerStatus(
            isScheduled = isRecoveryScheduled.get(),
            isInProgress = isRecoveryInProgress.get(),
            pendingAppsCount = pendingApps.size,
            stableStateDuration = if (stableStateStartTime > 0) {
                SystemClock.elapsedRealtime() - stableStateStartTime
            } else 0,
            recoveryDelayMs = recoveryDelayMs
        )
    }

    /**
     * 释放资源
     */
    fun release() {
        cancelRecovery("Handler released")
        executor.shutdown()
        recoveryListeners.clear()
    }
}

/**
 * 恢复处理器状态
 */
data class RecoveryHandlerStatus(
    val isScheduled: Boolean,
    val isInProgress: Boolean,
    val pendingAppsCount: Int,
    val stableStateDuration: Long,
    val recoveryDelayMs: Long
)
