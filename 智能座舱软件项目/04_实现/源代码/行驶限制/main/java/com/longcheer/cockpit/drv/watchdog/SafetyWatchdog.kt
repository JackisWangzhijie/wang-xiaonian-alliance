package com.longcheer.cockpit.drv.watchdog

import android.os.SystemClock
import android.util.Log
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * 安全看门狗
 * ASIL等级: ASIL B
 * 
 * 实现软硬件结合看门狗监控:
 * - 软件看门狗: 检测软件死循环/阻塞
 * - 硬件看门狗: 检测系统级故障
 * 
 * @param timeoutMs 超时阈值 (毫秒)
 * @param onTimeout 超时回调
 */
class SafetyWatchdog(
    private val timeoutMs: Long,
    private val onTimeout: () -> Unit
) {
    companion object {
        private const val TAG = "SafetyWatchdog"
        
        /** 默认看门狗超时: 500ms */
        const val DEFAULT_TIMEOUT_MS = 500L
        
        /** 监控周期: 超时时间的一半 */
        const val MONITOR_INTERVAL_RATIO = 2
    }

    // 调度器
    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "SafetyWatchdog-Thread").apply { isDaemon = true }
    }
    
    // 最后喂狗时间
    private val lastFeedTime = AtomicLong(SystemClock.elapsedRealtime())
    
    // 运行状态
    private val isRunning = AtomicBoolean(false)
    
    // 超时计数
    private val timeoutCount = AtomicLong(0)
    
    // 最大允许超时次数 (连续)
    private val maxTimeoutCount = 2
    
    // 任务监控映射
    private val taskFeedTimes = ConcurrentHashMap<String, AtomicLong>()

    /**
     * 初始化并启动监控
     */
    init {
        startMonitoring()
    }

    /**
     * 启动监控
     */
    private fun startMonitoring() {
        if (isRunning.get()) return
        
        isRunning.set(true)
        val interval = timeoutMs / MONITOR_INTERVAL_RATIO
        
        executor.scheduleAtFixedRate(
            { checkTimeout() },
            interval, interval, TimeUnit.MILLISECONDS
        )
        
        Log.i(TAG, "Watchdog started with timeout=$timeoutMs ms, interval=$interval ms")
    }

    /**
     * 喂狗操作
     * 应在主循环中定期调用 (建议100ms周期)
     */
    fun feed() {
        lastFeedTime.set(SystemClock.elapsedRealtime())
        timeoutCount.set(0)
    }

    /**
     * 任务级喂狗
     * 
     * @param taskId 任务ID
     */
    fun feedTask(taskId: String) {
        taskFeedTimes.getOrPut(taskId) { AtomicLong(SystemClock.elapsedRealtime()) }
            .set(SystemClock.elapsedRealtime())
    }

    /**
     * 检查超时
     */
    private fun checkTimeout() {
        if (!isRunning.get()) return
        
        val elapsed = SystemClock.elapsedRealtime() - lastFeedTime.get()
        
        if (elapsed > timeoutMs) {
            val count = timeoutCount.incrementAndGet()
            Log.e(TAG, "Watchdog timeout! Elapsed: $elapsed ms, count: $count")
            
            if (count >= maxTimeoutCount) {
                handleTimeout()
            }
        }
        
        // 检查任务级超时
        checkTaskTimeouts()
    }

    /**
     * 检查任务超时
     */
    private fun checkTaskTimeouts() {
        val now = SystemClock.elapsedRealtime()
        
        taskFeedTimes.forEach { (taskId, feedTime) ->
            val elapsed = now - feedTime.get()
            if (elapsed > timeoutMs) {
                Log.w(TAG, "Task watchdog timeout: $taskId, elapsed: $elapsed ms")
            }
        }
    }

    /**
     * 处理超时
     */
    private fun handleTimeout() {
        Log.e(TAG, "Watchdog timeout limit reached, triggering safe state")
        
        try {
            // 执行超时回调
            onTimeout()
            
            // 触发硬件看门狗复位
            triggerHardwareWatchdog()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in timeout handler", e)
            // 强制触发硬件复位
            forceSystemReset()
        }
    }

    /**
     * 触发硬件看门狗
     * 通过HAL接口触发硬件看门狗
     */
    private fun triggerHardwareWatchdog() {
        try {
            // 通过系统属性或HAL接口触发硬件看门狗
            // 实际实现依赖于硬件平台
            val process = Runtime.getRuntime().exec("echo 1 > /dev/watchdog")
            process.waitFor()
            
            Log.w(TAG, "Hardware watchdog triggered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to trigger hardware watchdog", e)
        }
    }

    /**
     * 强制系统复位 (最后手段)
     */
    private fun forceSystemReset() {
        try {
            Runtime.getRuntime().exec("reboot")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to force system reset", e)
        }
    }

    /**
     * 停止看门狗
     */
    fun stop() {
        isRunning.set(false)
        executor.shutdown()
        try {
            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executor.shutdownNow()
        }
        Log.i(TAG, "Watchdog stopped")
    }

    /**
     * 获取看门狗状态
     */
    fun getStatus(): WatchdogStatus {
        return WatchdogStatus(
            isRunning = isRunning.get(),
            lastFeedTime = lastFeedTime.get(),
            elapsedSinceLastFeed = SystemClock.elapsedRealtime() - lastFeedTime.get(),
            timeoutCount = timeoutCount.get(),
            timeoutThreshold = timeoutMs,
            monitoredTasks = taskFeedTimes.keys.toList()
        )
    }

    /**
     * 注册监控任务
     */
    fun registerTask(taskId: String) {
        taskFeedTimes[taskId] = AtomicLong(SystemClock.elapsedRealtime())
        Log.d(TAG, "Registered task for monitoring: $taskId")
    }

    /**
     * 注销监控任务
     */
    fun unregisterTask(taskId: String) {
        taskFeedTimes.remove(taskId)
        Log.d(TAG, "Unregistered task from monitoring: $taskId")
    }
}

/**
 * 看门狗状态数据类
 */
data class WatchdogStatus(
    val isRunning: Boolean,
    val lastFeedTime: Long,
    val elapsedSinceLastFeed: Long,
    val timeoutCount: Long,
    val timeoutThreshold: Long,
    val monitoredTasks: List<String>
)

import java.util.concurrent.ConcurrentHashMap
