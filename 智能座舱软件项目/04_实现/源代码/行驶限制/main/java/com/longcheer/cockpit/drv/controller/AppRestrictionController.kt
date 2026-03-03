package com.longcheer.cockpit.drv.controller

import android.app.ActivityManager
import android.app.IActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.RemoteException
import android.os.ServiceManager
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.view.WindowManager
import com.longcheer.cockpit.drv.model.AppCategory
import com.longcheer.cockpit.drv.model.BehaviorControl
import java.util.concurrent.ConcurrentHashMap

/**
 * 应用限制控制器
 * ASIL等级: ASIL A
 * 
 * 职责:
 * - 执行应用行为控制
 * - 暂停/恢复应用
 * - 限制应用交互
 */
class AppRestrictionController(private val context: Context) {

    companion object {
        private const val TAG = "AppRestrictionController"
        
        /** 限制触发最大延迟 */
        const val MAX_RESTRICTION_DELAY_MS = 200L
        
        /** 恢复最大延迟 */
        const val MAX_RECOVERY_DELAY_MS = 500L
    }

    // ActivityManager
    private val activityManager: IActivityManager = IActivityManager.Stub.asInterface(
        ServiceManager.getService(Context.ACTIVITY_SERVICE)
    )
    
    // 被暂停的应用状态缓存
    private val pausedApps = ConcurrentHashMap<String, AppState>()
    
    // 被限制交互的应用
    private val limitedInteractionApps = ConcurrentHashMap<String, Long>()
    
    // 灰度模式应用
    private val grayscaleApps = ConcurrentHashMap<String, Boolean>()

    /**
     * 应用状态数据类
     */
    data class AppState(
        val packageName: String,
        val pausedTime: Long,
        val taskId: Int = -1,
        val lifecycleState: String = ""
    )

    /**
     * 暂停应用
     * 
     * @param packageName 应用包名
     * @return 是否成功
     */
    fun pauseApp(packageName: String): Boolean {
        val startTime = SystemClock.elapsedRealtime()
        
        return try {
            // 记录应用状态
            val taskId = getAppTaskId(packageName)
            pausedApps[packageName] = AppState(
                packageName = packageName,
                pausedTime = System.currentTimeMillis(),
                taskId = taskId
            )
            
            // 发送暂停广播给应用
            val intent = Intent("com.longcheer.cockpit.ACTION_PAUSE_APP").apply {
                setPackage(packageName)
                putExtra("reason", "driving_restriction")
            }
            context.sendBroadcast(intent)
            
            // 调用AMS暂停应用
            activityManager.setPackageStoppedState(packageName, true, 
                context.userId)
            
            val elapsed = SystemClock.elapsedRealtime() - startTime
            Log.i(TAG, "Paused app $packageName in ${elapsed}ms")
            
            true
            
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to pause app $packageName", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error pausing app $packageName", e)
            false
        }
    }

    /**
     * 恢复应用
     * 
     * @param packageName 应用包名
     * @return 是否成功
     */
    fun resumeApp(packageName: String): Boolean {
        val startTime = SystemClock.elapsedRealtime()
        
        return try {
            // 从暂停列表移除
            pausedApps.remove(packageName)
            
            // 恢复应用状态
            val intent = Intent("com.longcheer.cockpit.ACTION_RESUME_APP").apply {
                setPackage(packageName)
            }
            context.sendBroadcast(intent)
            
            // 恢复应用运行状态
            activityManager.setPackageStoppedState(packageName, false, 
                context.userId)
            
            val elapsed = SystemClock.elapsedRealtime() - startTime
            Log.i(TAG, "Resumed app $packageName in ${elapsed}ms")
            
            // 检查延迟要求
            if (elapsed > MAX_RECOVERY_DELAY_MS) {
                Log.w(TAG, "Resume took ${elapsed}ms, exceeds ${MAX_RECOVERY_DELAY_MS}ms requirement")
            }
            
            true
            
        } catch (e: RemoteException) {
            Log.e(TAG, "Failed to resume app $packageName", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error resuming app $packageName", e)
            false
        }
    }

    /**
     * 强制应用返回HOME
     * 
     * @param packageName 应用包名
     * @return 是否成功
     */
    fun returnToHome(packageName: String): Boolean {
        return try {
            // 启动HOME Intent
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(homeIntent)
            
            // 将目标应用移至后台
            val taskId = getAppTaskId(packageName)
            if (taskId != -1) {
                activityManager.moveTaskToBack(taskId, null)
            }
            
            Log.i(TAG, "Returned $packageName to home")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to return $packageName to home", e)
            false
        }
    }

    /**
     * 限制应用交互
     * 
     * @param packageName 应用包名
     * @return 是否成功
     */
    fun limitInteraction(packageName: String): Boolean {
        return try {
            limitedInteractionApps[packageName] = System.currentTimeMillis()
            
            // 发送限制交互广播
            val intent = Intent("com.longcheer.cockpit.ACTION_LIMIT_INTERACTION").apply {
                setPackage(packageName)
                putExtra("limited", true)
            }
            context.sendBroadcast(intent)
            
            Log.i(TAG, "Limited interaction for $packageName")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to limit interaction for $packageName", e)
            false
        }
    }

    /**
     * 解除交互限制
     */
    fun unlimitInteraction(packageName: String): Boolean {
        limitedInteractionApps.remove(packageName)
        
        val intent = Intent("com.longcheer.cockpit.ACTION_LIMIT_INTERACTION").apply {
            setPackage(packageName)
            putExtra("limited", false)
        }
        context.sendBroadcast(intent)
        
        return true
    }

    /**
     * 设置灰度显示模式
     * 
     * @param packageName 应用包名
     * @return 是否成功
     */
    fun setGrayscale(packageName: String): Boolean {
        return try {
            grayscaleApps[packageName] = true
            
            // 发送灰度模式广播
            val intent = Intent("com.longcheer.cockpit.ACTION_GRAYSCALE").apply {
                setPackage(packageName)
                putExtra("enabled", true)
            }
            context.sendBroadcast(intent)
            
            Log.i(TAG, "Set grayscale for $packageName")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set grayscale for $packageName", e)
            false
        }
    }

    /**
     * 取消灰度显示
     */
    fun unsetGrayscale(packageName: String): Boolean {
        grayscaleApps.remove(packageName)
        
        val intent = Intent("com.longcheer.cockpit.ACTION_GRAYSCALE").apply {
            setPackage(packageName)
            putExtra("enabled", false)
        }
        context.sendBroadcast(intent)
        
        return true
    }

    /**
     * 根据类别执行限制
     */
    fun restrictByCategory(packageName: String, category: AppCategory): Boolean {
        return when (category) {
            AppCategory.VIDEO -> pauseApp(packageName)
            AppCategory.GAME -> returnToHome(packageName)
            AppCategory.BROWSER -> limitInteraction(packageName)
            else -> {
                // 其他类别按默认处理
                limitInteraction(packageName)
            }
        }
    }

    /**
     * 限制所有非必要应用 (安全状态)
     */
    fun restrictAllNonEssential(): Boolean {
        return try {
            // 获取所有运行中的应用
            val runningApps = getRunningApps()
            
            runningApps.forEach { app ->
                // 保留系统应用和必要服务
                if (!isEssentialApp(app)) {
                    returnToHome(app)
                }
            }
            
            Log.w(TAG, "Restricted all non-essential apps")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restrict all apps", e)
            false
        }
    }

    /**
     * 执行行为控制
     */
    fun executeControl(packageName: String, control: BehaviorControl): Boolean {
        return when (control) {
            BehaviorControl.PAUSE -> pauseApp(packageName)
            BehaviorControl.RESUME -> resumeApp(packageName)
            BehaviorControl.RETURN_HOME -> returnToHome(packageName)
            BehaviorControl.LIMIT_INTERACTION -> limitInteraction(packageName)
            BehaviorControl.GRAYSCALE -> setGrayscale(packageName)
        }
    }

    /**
     * 恢复所有被限制的应用
     */
    fun resumeAllApps(): Boolean {
        var success = true
        
        pausedApps.keys.toList().forEach { packageName ->
            if (!resumeApp(packageName)) {
                success = false
            }
        }
        
        limitedInteractionApps.keys.toList().forEach { packageName ->
            unlimitInteraction(packageName)
        }
        
        grayscaleApps.keys.toList().forEach { packageName ->
            unsetGrayscale(packageName)
        }
        
        return success
    }

    /**
     * 获取被暂停的应用列表
     */
    fun getPausedApps(): List<String> {
        return pausedApps.keys.toList()
    }

    /**
     * 获取应用任务ID
     */
    private fun getAppTaskId(packageName: String): Int {
        return try {
            val tasks = activityManager.getTasks(100)
            tasks?.find { task ->
                task.baseActivity?.packageName == packageName
            }?.id ?: -1
        } catch (e: Exception) {
            -1
        }
    }

    /**
     * 获取运行中的应用列表
     */
    private fun getRunningApps(): List<String> {
        return try {
            val tasks = activityManager.getTasks(100)
            tasks?.mapNotNull { it.baseActivity?.packageName }?.distinct() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 判断是否为必要应用
     */
    private fun isEssentialApp(packageName: String): Boolean {
        val essentialApps = setOf(
            "com.android.systemui",
            "com.android.launcher",
            "com.longcheer.cockpit.drv",
            "com.android.bluetooth"
        )
        return essentialApps.contains(packageName)
    }

    /**
     * 清除状态
     */
    fun clearState() {
        pausedApps.clear()
        limitedInteractionApps.clear()
        grayscaleApps.clear()
    }

    /**
     * 获取控制器状态
     */
    fun getStatus(): ControllerStatus {
        return ControllerStatus(
            pausedAppsCount = pausedApps.size,
            limitedAppsCount = limitedInteractionApps.size,
            grayscaleAppsCount = grayscaleApps.size,
            pausedAppsList = pausedApps.keys.toList()
        )
    }
}

/**
 * 控制器状态数据类
 */
data class ControllerStatus(
    val pausedAppsCount: Int,
    val limitedAppsCount: Int,
    val grayscaleAppsCount: Int,
    val pausedAppsList: List<String>
)
