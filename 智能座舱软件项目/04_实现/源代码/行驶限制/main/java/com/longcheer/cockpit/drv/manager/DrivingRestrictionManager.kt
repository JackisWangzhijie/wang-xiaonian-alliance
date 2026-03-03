package com.longcheer.cockpit.drv.manager

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.RemoteException
import android.os.SystemClock
import android.util.Log
import com.longcheer.cockpit.drv.IDrivingRestrictionListener
import com.longcheer.cockpit.drv.model.*
import com.longcheer.cockpit.drv.monitor.VehicleSignalMonitor
import com.longcheer.cockpit.drv.controller.AppRestrictionController
import com.longcheer.cockpit.drv.e2e.E2EProtectionHandler
import com.longcheer.cockpit.drv.handler.StateRecoveryHandler
import com.longcheer.cockpit.drv.state.RestrictionStateMachine
import com.longcheer.cockpit.drv.watchdog.SafetyWatchdog
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * 行驶限制管理器 - 单例模式
 * ASIL等级: ASIL B
 * 
 * 职责:
 * - 管理行驶限制状态机
 * - 协调各子模块工作
 * - 维护看门狗喂狗
 * - 提供线程安全保证
 * 
 * 安全要求:
 * - 关键决策采用双路冗余计算
 * - 看门狗500ms超时保护
 * - 状态转换原子性保证
 */
class DrivingRestrictionManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "DrivingRestrictionManager"
        
        /** 看门狗超时: 500ms */
        const val WATCHDOG_TIMEOUT_MS = 500L
        
        /** 恢复延迟: 3秒 */
        const val RECOVERY_DELAY_MS = 3000L
        
        /** 限制触发最大延迟: 200ms */
        const val MAX_RESTRICTION_DELAY_MS = 200L
        
        /** 喂狗周期: 100ms */
        const val WATCHDOG_FEED_PERIOD_MS = 100L

        @Volatile
        private var instance: DrivingRestrictionManager? = null

        fun getInstance(context: Context): DrivingRestrictionManager {
            return instance ?: synchronized(this) {
                instance ?: DrivingRestrictionManager(context.applicationContext).also { 
                    instance = it 
                }
            }
        }
    }

    // ASIL B核心组件
    private val stateMachine = RestrictionStateMachine()
    private val signalMonitor = VehicleSignalMonitor(context)
    private val e2eHandler = E2EProtectionHandler()
    
    // 限制控制器
    private val restrictionController = AppRestrictionController(context)
    
    // 状态恢复处理器
    private val recoveryHandler = StateRecoveryHandler()
    
    // QM级组件
    private val whitelistManager = WhitelistManager(context)
    
    // 安全组件
    private val watchdog = SafetyWatchdog(WATCHDOG_TIMEOUT_MS) {
        enterSafeState("Watchdog timeout")
    }
    
    // 监听者
    private val listeners = CopyOnWriteArrayList<IDrivingRestrictionListener>()
    
    // 主线程Handler用于状态同步
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // 看门狗喂狗调度
    private val watchdogExecutor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "WatchdogFeeder-Thread").apply { isDaemon = true }
    }
    
    // 当前车辆信号缓存
    private var currentVehicleSignal: VehicleSignal? = null
    private val signalLock = Object()
    
    // 冗余计算错误计数
    private val redundancyErrorCount = AtomicInteger(0)
    private val MAX_REDUNDANCY_ERRORS = 3
    
    // 初始化标记
    private var initialized = false

    /**
     * 初始化模块
     * 启动信号监听、看门狗、状态恢复定时器
     */
    fun initialize() {
        if (initialized) return
        
        Log.i(TAG, "Initializing DrivingRestrictionManager...")
        
        // 初始化白名单管理器
        whitelistManager.initialize()
        
        // 初始化信号监听
        signalMonitor.initialize { signal ->
            onVehicleSignalChanged(signal)
        }
        
        // 设置E2E错误回调
        signalMonitor.setErrorCallback { signalId, status ->
            handleE2EError(signalId, status)
        }
        
        // 初始化状态恢复处理器
        recoveryHandler.initialize(
            delayMs = RECOVERY_DELAY_MS,
            conditionChecker = { checkRecoveryConditions() },
            recoveryExecutor = { executeRecoveryInternal() }
        )
        
        // 启动周期性看门狗喂狗
        watchdogExecutor.scheduleAtFixedRate(
            { feedWatchdog() },
            0, WATCHDOG_FEED_PERIOD_MS, TimeUnit.MILLISECONDS
        )
        
        // 注册看门狗监控任务
        watchdog.registerTask("SignalMonitor")
        watchdog.registerTask("RestrictionEvaluator")
        
        initialized = true
        
        Log.i(TAG, "DrivingRestrictionManager initialized successfully")
    }

    /**
     * 车辆信号变化回调
     * ASIL B: 信号已进行E2E校验
     */
    private fun onVehicleSignalChanged(signal: VehicleSignal) {
        // 更新当前信号缓存
        synchronized(signalLock) {
            currentVehicleSignal = signal
        }
        
        // 任务级喂狗
        watchdog.feedTask("SignalMonitor")
        
        // 双路冗余评估
        val evaluationResult = redundantEvaluate(signal)
        
        when (stateMachine.getCurrentState()) {
            RestrictionState.NORMAL -> {
                if (evaluationResult.shouldRestrict) {
                    enforceRestriction(evaluationResult.reason)
                }
            }
            RestrictionState.RESTRICTED -> {
                if (!evaluationResult.shouldRestrict) {
                    // 检查恢复条件
                    if (canRecover(signal)) {
                        scheduleRecovery()
                    }
                }
            }
            RestrictionState.RECOVERING -> {
                // 恢复中状态，检查是否满足限制条件
                if (evaluationResult.shouldRestrict) {
                    // 取消恢复，重新进入限制状态
                    recoveryHandler.cancelRecovery("Restriction condition met during recovery")
                    stateMachine.transitionTo(RestrictionState.RESTRICTED)
                }
            }
            RestrictionState.FAULT -> {
                // 故障状态，需要手动恢复或系统复位
                Log.w(TAG, "In FAULT state, ignoring signal changes")
            }
        }
    }

    /**
     * 双路冗余评估 - ASIL B安全机制
     * 两条独立路径计算，结果必须一致
     * 
     * @param signal 车辆信号
     * @return 评估结果
     */
    private fun redundantEvaluate(signal: VehicleSignal): EvaluationResult {
        // 路径1: 基于车速
        val result1 = evaluateBySpeed(signal.speed)
        
        // 路径2: 基于挡位和驻车制动
        val result2 = evaluateByGearAndPB(signal.gear, signal.parkingBrake)
        
        // 结果比较
        if (result1.shouldRestrict != result2.shouldRestrict) {
            // 冗余计算不一致，增加错误计数
            val errors = redundancyErrorCount.incrementAndGet()
            Log.e(TAG, "Redundancy mismatch: path1=$result1, path2=$result2, errors=$errors")
            
            if (errors >= MAX_REDUNDANCY_ERRORS) {
                enterSafeState("Redundancy check failed $MAX_REDUNDANCY_ERRORS times")
                return EvaluationResult(true, "Redundancy failure - safe state")
            }
            
            // 不一致时默认限制 (安全侧)
            return EvaluationResult(true, "Redundancy mismatch - default restrict")
        }
        
        // 重置错误计数
        redundancyErrorCount.set(0)
        
        // 构建原因描述
        val reason = if (result1.shouldRestrict) result1.reason else result2.reason
        
        return EvaluationResult(result1.shouldRestrict, reason)
    }

    /**
     * 评估结果数据类
     */
    data class EvaluationResult(
        val shouldRestrict: Boolean,
        val reason: String
    )

    /**
     * 路径1: 基于车速评估
     */
    private fun evaluateBySpeed(speed: Int): EvaluationResult {
        // 信号有效性检查
        if (speed < 0 || speed > 300) {
            return EvaluationResult(true, "Invalid speed signal: $speed")
        }
        
        return EvaluationResult(
            shouldRestrict = speed > 0,
            reason = "Speed = $speed km/h > 0"
        )
    }

    /**
     * 路径2: 基于挡位和驻车制动评估
     */
    private fun evaluateByGearAndPB(gear: GearPosition, parkingBrake: Boolean): EvaluationResult {
        // 信号有效性检查
        if (gear == GearPosition.UNKNOWN) {
            return EvaluationResult(true, "Invalid gear signal: UNKNOWN")
        }
        
        // 行驶条件: D挡或R挡或驻车制动释放
        val shouldRestrict = gear == GearPosition.DRIVE || 
                             gear == GearPosition.REVERSE || 
                             !parkingBrake
        
        val reason = when {
            gear == GearPosition.DRIVE -> "Gear = DRIVE"
            gear == GearPosition.REVERSE -> "Gear = REVERSE"
            !parkingBrake -> "Parking brake released"
            else -> "Vehicle stationary"
        }
        
        return EvaluationResult(shouldRestrict, reason)
    }

    /**
     * 执行限制措施
     * ASIL B: 限制触发延迟≤200ms
     * 
     * @param reason 限制原因
     */
    private fun enforceRestriction(reason: String) {
        val startTime = SystemClock.elapsedRealtime()
        
        // 状态机转换
        if (!stateMachine.transitionTo(RestrictionState.RESTRICTED)) {
            Log.w(TAG, "Failed to transition to RESTRICTED state")
            return
        }
        
        // 获取当前运行的受限应用
        val restrictedApps = getRunningRestrictedApps()
        
        restrictedApps.forEach { appInfo ->
            // 根据应用类型执行不同限制
            when (appInfo.category) {
                AppCategory.VIDEO -> restrictionController.pauseApp(appInfo.packageName)
                AppCategory.GAME -> restrictionController.returnToHome(appInfo.packageName)
                AppCategory.BROWSER -> restrictionController.limitInteraction(appInfo.packageName)
                else -> {
                    // 其他类别按白名单判断
                    if (!whitelistManager.isAllowed(appInfo.packageName)) {
                        restrictionController.limitInteraction(appInfo.packageName)
                    }
                }
            }
            
            // 添加到恢复列表
            recoveryHandler.addPendingApp(appInfo.packageName)
        }
        
        // 通知监听者
        notifyRestrictionChanged(true, reason)
        
        // 检查延迟要求
        val elapsed = SystemClock.elapsedRealtime() - startTime
        if (elapsed > MAX_RESTRICTION_DELAY_MS) {
            Log.w(TAG, "Restriction enforcement took ${elapsed}ms, exceeds ${MAX_RESTRICTION_DELAY_MS}ms requirement")
        } else {
            Log.i(TAG, "Restriction enforced in ${elapsed}ms, reason: $reason")
        }
    }

    /**
     * 检查恢复条件
     */
    private fun checkRecoveryConditions(): Boolean {
        val signal = synchronized(signalLock) { currentVehicleSignal } ?: return false
        
        return recoveryHandler.canRecover(
            vehicleSpeed = signal.speed,
            gearPosition = signal.gear.ordinal,
            parkingBrake = signal.parkingBrake
        )
    }

    /**
     * 检查是否可以恢复
     */
    private fun canRecover(signal: VehicleSignal): Boolean {
        return recoveryHandler.canRecover(
            vehicleSpeed = signal.speed,
            gearPosition = signal.gear.ordinal,
            parkingBrake = signal.parkingBrake
        )
    }

    /**
     * 调度恢复
     */
    private fun scheduleRecovery() {
        recoveryHandler.scheduleRecovery {
            // 恢复完成回调
            executeRecovery()
        }
    }

    /**
     * 执行恢复
     */
    private fun executeRecovery() {
        if (!stateMachine.transitionTo(RestrictionState.RECOVERING)) {
            Log.w(TAG, "Failed to transition to RECOVERING state")
            return
        }
        
        executeRecoveryInternal()
    }

    /**
     * 执行恢复内部逻辑
     */
    private fun executeRecoveryInternal() {
        val startTime = SystemClock.elapsedRealtime()
        
        // 恢复被限制的应用
        val pausedApps = recoveryHandler.getPendingApps()
        pausedApps.forEach { packageName ->
            restrictionController.resumeApp(packageName)
        }
        
        // 状态机转换到正常
        stateMachine.transitionTo(RestrictionState.NORMAL)
        
        // 清空恢复列表
        recoveryHandler.getPendingApps().forEach { recoveryHandler.removePendingApp(it) }
        
        // 通知监听者
        notifyRestrictionChanged(false, "Recovery completed")
        
        val elapsed = SystemClock.elapsedRealtime() - startTime
        Log.i(TAG, "Recovery executed in ${elapsed}ms")
    }

    /**
     * 进入安全状态 - 故障安全模式
     * 
     * @param reason 进入安全状态的原因
     */
    private fun enterSafeState(reason: String) {
        Log.e(TAG, "Entering safe state: $reason")
        
        // 状态机转换
        stateMachine.forceTransitionTo(RestrictionState.FAULT)
        
        // 强制限制所有非必要应用
        restrictionController.restrictAllNonEssential()
        
        // 记录故障日志
        logSafetyEvent(SafetyEventType.SAFE_STATE_ENTERED, "Entered safe state: $reason")
        
        // 通知监听者
        mainHandler.post {
            listeners.forEach { listener ->
                try {
                    listener.onEnterSafeState(0x2001) // 安全状态错误码
                } catch (e: RemoteException) {
                    listeners.remove(listener)
                }
            }
        }
    }

    /**
     * 处理E2E错误
     */
    private fun handleE2EError(signalId: String, status: E2EStatus) {
        Log.e(TAG, "E2E error: $signalId, status=$status")
        
        when (status) {
            E2EStatus.CRC_ERROR -> logSafetyEvent(SafetyEventType.E2E_CRC_ERROR, "CRC error: $signalId")
            E2EStatus.COUNTER_ERROR -> logSafetyEvent(SafetyEventType.E2E_COUNTER_ERROR, "Counter error: $signalId")
            E2EStatus.TIMEOUT -> logSafetyEvent(SafetyEventType.E2E_TIMEOUT, "Timeout: $signalId")
            else -> {}
        }
        
        // 连续E2E错误达到阈值，进入安全状态
        if (e2eHandler.isConsecutiveErrorLimitReached(signalId)) {
            enterSafeState("E2E consecutive errors for $signalId")
        }
    }

    /**
     * 喂狗操作
     */
    private fun feedWatchdog() {
        watchdog.feed()
    }

    /**
     * 记录安全事件
     */
    private fun logSafetyEvent(eventType: SafetyEventType, message: String) {
        Log.w(TAG, "Safety event: $eventType - $message")
        // 可扩展为写入安全日志文件或上报
    }

    /**
     * 通知限制状态变化
     */
    private fun notifyRestrictionChanged(restricted: Boolean, reason: String) {
        mainHandler.post {
            listeners.forEach { listener ->
                try {
                    listener.onRestrictionChanged(restricted, stateMachine.getCurrentState().ordinal)
                } catch (e: RemoteException) {
                    listeners.remove(listener)
                }
            }
        }
    }

    /**
     * 获取运行中的受限应用
     */
    private fun getRunningRestrictedApps(): List<AppInfo> {
        // 简化实现，实际应查询AMS获取运行中的应用
        // 并根据白名单过滤
        return emptyList()
    }

    // ==================== 公共API ====================

    /**
     * 获取当前限制状态
     */
    fun getCurrentStatus(): RestrictionStatus {
        val signal = synchronized(signalLock) { currentVehicleSignal }
        
        return RestrictionStatus(
            state = stateMachine.getCurrentState(),
            isRestricted = stateMachine.isRestricted(),
            timestamp = System.currentTimeMillis(),
            triggerReason = "",
            vehicleSpeed = signal?.speed ?: 0,
            gearPosition = signal?.gear ?: GearPosition.PARK,
            parkingBrakeOn = signal?.parkingBrake ?: true
        )
    }

    /**
     * 获取应用限制类型
     */
    fun getAppRestrictionType(appId: String): RestrictionType {
        return if (whitelistManager.isAllowed(appId)) {
            RestrictionType.NONE
        } else {
            getRestrictionTypeByState()
        }
    }

    /**
     * 执行应用行为控制
     */
    fun executeBehaviorControl(appId: String, control: BehaviorControl) {
        restrictionController.executeControl(appId, control)
    }

    /**
     * 注册监听
     */
    fun registerListener(listener: IDrivingRestrictionListener) {
        listeners.add(listener)
    }

    /**
     * 注销监听
     */
    fun unregisterListener(listener: IDrivingRestrictionListener) {
        listeners.remove(listener)
    }

    /**
     * 添加到白名单
     */
    fun addToWhitelist(appId: String, reason: String): Boolean {
        return whitelistManager.addToWhitelist(appId, reason)
    }

    /**
     * 从白名单移除
     */
    fun removeFromWhitelist(appId: String): Boolean {
        return whitelistManager.removeFromWhitelist(appId)
    }

    /**
     * 获取白名单
     */
    fun getWhitelist(): List<WhitelistEntry> {
        return whitelistManager.getWhitelist()
    }

    /**
     * 获取E2E状态
     */
    fun getE2EStatus(): Map<String, E2EStatusInfo> {
        return signalMonitor.getE2EStatus()
    }

    /**
     * 强制进入安全状态 (紧急调试用)
     */
    fun forceEnterSafeState() {
        enterSafeState("Manual trigger")
    }

    /**
     * 根据状态获取限制类型
     */
    private fun getRestrictionTypeByState(): RestrictionType {
        return when (stateMachine.getCurrentState()) {
            RestrictionState.RESTRICTED -> RestrictionType.FULL_RESTRICTED
            RestrictionState.FAULT -> RestrictionType.FULL_RESTRICTED
            else -> RestrictionType.NONE
        }
    }

    /**
     * 获取管理器状态信息
     */
    fun getManagerStatus(): ManagerStatus {
        return ManagerStatus(
            initialized = initialized,
            currentState = stateMachine.getCurrentState(),
            isRestricted = stateMachine.isRestricted(),
            watchdogStatus = watchdog.getStatus(),
            recoveryStatus = recoveryHandler.getStatus(),
            e2eStatus = signalMonitor.getE2EStatus()
        )
    }

    /**
     * 释放资源
     */
    fun release() {
        watchdog.stop()
        watchdogExecutor.shutdown()
        signalMonitor.stop()
        recoveryHandler.release()
        listeners.clear()
        instance = null
        initialized = false
        Log.i(TAG, "DrivingRestrictionManager released")
    }
}

/**
 * 管理器状态数据类
 */
data class ManagerStatus(
    val initialized: Boolean,
    val currentState: RestrictionState,
    val isRestricted: Boolean,
    val watchdogStatus: WatchdogStatus,
    val recoveryStatus: RecoveryHandlerStatus,
    val e2eStatus: Map<String, E2EStatusInfo>
)

import com.longcheer.cockpit.drv.watchdog.WatchdogStatus
import com.longcheer.cockpit.drv.handler.RecoveryHandlerStatus
