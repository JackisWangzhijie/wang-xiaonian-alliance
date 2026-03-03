package com.longcheer.cockpit.fwk.lifecycle

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.longcheer.cockpit.fwk.model.AppCategory
import com.longcheer.cockpit.fwk.model.AppLifecycleState
import com.longcheer.cockpit.cockpit.fwk.model.AppRestrictedException
import com.longcheer.cockpit.fwk.model.AppStateSnapshot
import com.longcheer.cockpit.fwk.model.DrivingRestrictionListener
import com.longcheer.cockpit.fwk.model.DrivingRestrictionStatus
import com.longcheer.cockpit.fwk.model.GearPosition
import com.longcheer.cockpit.fwk.repository.IAppRepository
import com.longcheer.cockpit.fwk.service.IVehicleService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 应用生命周期管理器
 * 负责管理系统中所有应用的生命周期
 * 需求追溯: REQ-DRV-FUN-007, REQ-DRV-FUN-009
 */
@Singleton
class AppLifecycleManager @Inject constructor(
    private val activityManager: ActivityManager,
    private val vehicleService: IVehicleService,
    private val appRepository: IAppRepository,
    @ApplicationContext private val context: Context
) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_lifecycle")

    // 应用状态映射表
    private val appStates = ConcurrentHashMap<String, AppLifecycleState>()
    private val _stateFlow = MutableStateFlow<Map<String, AppLifecycleState>>(emptyMap())

    // 当前行驶限制状态
    private var currentRestrictionStatus: DrivingRestrictionStatus = DrivingRestrictionStatus.NOT_RESTRICTED

    init {
        // 监听行驶限制状态变化
        vehicleService.registerDrivingRestrictionListener(object : DrivingRestrictionListener {
            override fun onRestrictionChanged(status: DrivingRestrictionStatus) {
                handleRestrictionChange(status)
            }
        })
    }

    /**
     * 获取应用当前状态
     */
    fun getAppState(appId: String): AppLifecycleState {
        return appStates[appId] ?: AppLifecycleState.STOPPED
    }

    /**
     * 观察应用状态变化
     */
    fun observeAppState(appId: String): Flow<AppLifecycleState> {
        return _stateFlow.map { it[appId] ?: AppLifecycleState.STOPPED }
    }

    /**
     * 观察所有应用状态
     */
    fun observeAllAppStates(): Flow<Map<String, AppLifecycleState>> = _stateFlow.asStateFlow()

    /**
     * 启动应用
     */
    suspend fun startApp(appId: String): Result<Unit> {
        // 检查行驶限制
        if (isAppRestricted(appId)) {
            return Result.failure(AppRestrictedException())
        }

        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(appId)
                ?: return Result.failure(Exception("应用未找到: $appId"))

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            context.startActivity(intent)

            updateAppState(appId, AppLifecycleState.RUNNING)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 暂停应用（行驶限制时）
     */
    suspend fun pauseApp(appId: String): Result<Unit> {
        return try {
            // 发送暂停广播到应用
            val intent = Intent(ACTION_PAUSE_APP).apply {
                setPackage(appId)
                putExtra("reason", "driving_restriction")
                putExtra("timestamp", System.currentTimeMillis())
            }
            context.sendBroadcast(intent)

            // 保存应用状态
            saveAppState(appId)

            updateAppState(appId, AppLifecycleState.PAUSED)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 恢复应用（停车后）
     */
    suspend fun resumeApp(appId: String): Result<Unit> {
        return try {
            // 发送恢复广播到应用
            val intent = Intent(ACTION_RESUME_APP).apply {
                setPackage(appId)
                putExtra("timestamp", System.currentTimeMillis())
            }
            context.sendBroadcast(intent)

            // 恢复应用状态
            restoreAppState(appId)

            updateAppState(appId, AppLifecycleState.RUNNING)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 停止应用
     */
    suspend fun stopApp(appId: String): Result<Unit> {
        return try {
            activityManager.killBackgroundProcesses(appId)
            updateAppState(appId, AppLifecycleState.STOPPED)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 返回Home
     */
    fun goHome() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
    }

    /**
     * 处理行驶限制状态变化
     */
    private fun handleRestrictionChange(status: DrivingRestrictionStatus) {
        currentRestrictionStatus = status

        CoroutineScope(Dispatchers.Default).launch {
            if (status.isRestricted) {
                // 进入限制模式，暂停受限应用
                applyDrivingRestrictions()
            } else {
                // 解除限制，恢复应用
                releaseDrivingRestrictions()
            }
        }
    }

    /**
     * 应用行驶限制
     */
    private suspend fun applyDrivingRestrictions() {
        appStates.forEach { (appId, state) ->
            if (state == AppLifecycleState.RUNNING) {
                val isRestricted = checkAppRestrictionInternal(appId)
                if (isRestricted) {
                    pauseApp(appId)
                }
            }
        }
    }

    /**
     * 解除行驶限制
     */
    private suspend fun releaseDrivingRestrictions() {
        appStates.forEach { (appId, state) ->
            if (state == AppLifecycleState.PAUSED || state == AppLifecycleState.RESTRICTED) {
                resumeApp(appId)
            }
        }
    }

    /**
     * 检查应用是否受限（公开方法）
     */
    suspend fun isAppRestricted(appId: String): Boolean {
        return checkAppRestrictionInternal(appId)
    }

    /**
     * 内部方法：检查应用是否受限
     */
    private suspend fun checkAppRestrictionInternal(appId: String): Boolean {
        if (!currentRestrictionStatus.isRestricted) return false

        val app = appRepository.getAppById(appId) ?: return false
        if (app.isInWhitelist) return false

        return when (app.categoryId) {
            AppCategory.VIDEO.ordinal -> true
            AppCategory.GAME.ordinal -> true
            else -> false
        }
    }

    /**
     * 更新应用状态
     */
    private fun updateAppState(appId: String, state: AppLifecycleState) {
        appStates[appId] = state
        _stateFlow.value = appStates.toMap()
    }

    /**
     * 保存应用状态
     */
    private suspend fun saveAppState(appId: String) {
        val snapshot = AppStateSnapshot(
            appId = appId,
            timestamp = System.currentTimeMillis(),
            lifecycleState = AppLifecycleState.PAUSED
        )
        context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey("app_state_$appId")] = Json.encodeToString(snapshot)
        }
    }

    /**
     * 恢复应用状态
     */
    private suspend fun restoreAppState(appId: String) {
        // 从DataStore中获取保存的状态
        context.dataStore.data.map { preferences ->
            preferences[stringPreferencesKey("app_state_$appId")]
        }.collect { savedState ->
            savedState?.let {
                // 解析并恢复状态
                kotlin.runCatching {
                    Json.decodeFromString<AppStateSnapshot>(it)
                }
            }
        }
    }

    companion object {
        const val ACTION_PAUSE_APP = "com.longcheer.action.PAUSE_APP"
        const val ACTION_RESUME_APP = "com.longcheer.action.RESUME_APP"
    }
}
