package com.longcheer.cockpit.fwk.repository

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import com.longcheer.cockpit.fwk.data.DockDao
import com.longcheer.cockpit.fwk.model.DockActionType
import com.longcheer.cockpit.fwk.model.DockConfig
import com.longcheer.cockpit.fwk.model.DockItem
import com.longcheer.cockpit.fwk.model.DockSlotType
import com.longcheer.cockpit.fwk.model.RecentAppInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dock数据仓库实现
 * 需求追溯: REQ-FWK-FUN-016
 */
@Singleton
class DockRepository @Inject constructor(
    private val dockDao: DockDao,
    private val appRepository: IAppRepository,
    @ApplicationContext private val context: Context
) : IDockRepository {

    private val activityManager: ActivityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    // 最近应用队列，使用ArrayDeque实现LRU
    private val recentAppsQueue = ArrayDeque<RecentAppInfo>(10)
    private val recentAppsMutex = Mutex()

    override suspend fun getDockConfig(): List<DockConfig> = withContext(Dispatchers.IO) {
        val config = dockDao.getAllConfig().sortedBy { it.slotIndex }
        if (config.isEmpty()) {
            // 返回默认配置
            getDefaultDockConfig()
        } else {
            config
        }
    }

    override fun observeDockConfig(): Flow<List<DockConfig>> = dockDao.observeAllConfig()

    override suspend fun updateDockConfig(config: List<DockConfig>) {
        dockDao.updateConfig(config)
    }

    override suspend fun getRecentApps(limit: Int): List<RecentAppInfo> =
        recentAppsMutex.withLock {
            recentAppsQueue.take(limit)
        }

    override suspend fun addToRecentApps(appId: String, appName: String, iconPath: String?) {
        recentAppsMutex.withLock {
            val recentApp = RecentAppInfo(
                appId = appId,
                appName = appName,
                iconPath = iconPath,
                lastUsedTime = System.currentTimeMillis(),
                isRunning = isAppRunning(appId)
            )

            // 移除已存在的相同应用
            recentAppsQueue.removeAll { it.appId == appId }
            // 添加到队首
            recentAppsQueue.addFirst(recentApp)
            // 限制队列大小
            while (recentAppsQueue.size > 10) {
                recentAppsQueue.removeLast()
            }
        }
    }

    override suspend fun clearRecentApps() {
        recentAppsMutex.withLock {
            recentAppsQueue.clear()
        }
    }

    override suspend fun handleHomeAction() {
        withContext(Dispatchers.Main) {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(intent)
        }
    }

    override suspend fun handleRecentAppsAction() {
        withContext(Dispatchers.Main) {
            // 显示最近任务
            val intent = Intent("android.intent.action.RECENT_TASKS").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            kotlin.runCatching {
                context.startActivity(intent)
            }
        }
    }

    override suspend fun handleVoiceAssistantAction() {
        withContext(Dispatchers.Main) {
            val intent = Intent(Intent.ACTION_VOICE_COMMAND).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            kotlin.runCatching {
                context.startActivity(intent)
            }
        }
    }

    private fun isAppRunning(appId: String): Boolean {
        val runningApps = activityManager.runningAppProcesses
        return runningApps?.any { it.processName == appId } ?: false
    }

    /**
     * 获取默认Dock配置
     */
    private fun getDefaultDockConfig(): List<DockConfig> {
        return listOf(
            DockConfig(
                slotIndex = 0,
                slotType = DockSlotType.FIXED_APP,
                appId = "com.autonavi.amapauto",  // 高德地图
                actionType = DockActionType.LAUNCH_APP
            ),
            DockConfig(
                slotIndex = 1,
                slotType = DockSlotType.FIXED_APP,
                appId = "com.tencent.qqmusiccar",  // QQ音乐
                actionType = DockActionType.LAUNCH_APP
            ),
            DockConfig(
                slotIndex = 2,
                slotType = DockSlotType.FIXED_APP,
                appId = "com.android.dialer",  // 电话
                actionType = DockActionType.LAUNCH_APP
            ),
            DockConfig(
                slotIndex = 3,
                slotType = DockSlotType.FIXED_APP,
                appId = "com.android.settings",  // 设置
                actionType = DockActionType.LAUNCH_APP
            )
        )
    }

    /**
     * 将DockConfig列表转换为DockItem列表
     */
    suspend fun getDockItems(): List<DockItem> {
        val configs = getDockConfig()
        return configs.mapNotNull { config ->
            when (config.slotType) {
                DockSlotType.FIXED_APP -> {
                    config.appId?.let { appId ->
                        val app = appRepository.getAppById(appId)
                        app?.let {
                            DockItem.FixedAppItem(
                                appId = it.appId,
                                name = it.appName,
                                iconPath = it.iconPath,
                                isEnabled = it.isEnabled
                            )
                        }
                    }
                }
                DockSlotType.HOME_BUTTON -> DockItem.HomeButtonItem()
                DockSlotType.QUICK_ACTION -> {
                    DockItem.QuickActionItem(
                        appId = config.appId ?: "",
                        name = "快捷操作",
                        iconPath = config.iconResource,
                        actionType = config.actionType
                    )
                }
                else -> null
            }
        }
    }
}
