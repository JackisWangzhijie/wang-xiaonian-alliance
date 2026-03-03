package com.longcheer.cockpit.fwk.repository

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.core.content.pm.PackageInfoCompat
import com.longcheer.cockpit.fwk.data.AppDao
import com.longcheer.cockpit.fwk.model.AppCategory
import com.longcheer.cockpit.fwk.model.AppInfo
import com.longcheer.cockpit.fwk.model.AppRestrictedException
import com.longcheer.cockpit.fwk.service.IVehicleService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 应用数据仓库实现
 * 需求追溯: REQ-FWK-FUN-014
 */
@Singleton
class AppRepository @Inject constructor(
    private val appDao: AppDao,
    private val vehicleService: IVehicleService,
    @ApplicationContext private val context: Context
) : IAppRepository {

    private val packageManager: PackageManager = context.packageManager
    private val activityManager: ActivityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    private val _appChangesFlow = MutableStateFlow<List<AppInfo>>(emptyList())

    init {
        observePackageChanges()
    }

    override suspend fun getAllApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        appDao.getAllApps().sortedBy { it.sortOrder }
    }

    override fun observeAllApps(): Flow<List<AppInfo>> = appDao.observeAllApps()

    override suspend fun getAppsByCategory(category: AppCategory): List<AppInfo> =
        withContext(Dispatchers.IO) {
            appDao.getAppsByCategory(category.ordinal)
        }

    override suspend fun getAppById(appId: String): AppInfo? = withContext(Dispatchers.IO) {
        appDao.getAppById(appId)
    }

    override suspend fun getFrequentlyUsedApps(limit: Int): List<AppInfo> =
        withContext(Dispatchers.IO) {
            appDao.getFrequentlyUsedApps(limit)
        }

    override suspend fun searchApps(query: String): List<AppInfo> =
        withContext(Dispatchers.IO) {
            if (query.isBlank()) {
                getAllApps()
            } else {
                appDao.searchApps(query)
            }
        }

    override suspend fun updateAppStatus(appId: String, isEnabled: Boolean) {
        appDao.updateAppStatus(appId, isEnabled)
        refreshAppList()
    }

    override suspend fun incrementLaunchCount(appId: String) {
        appDao.incrementLaunchCount(appId)
    }

    override suspend fun updateAppOrder(apps: List<AppInfo>) {
        apps.forEachIndexed { index, app ->
            appDao.updateSortOrder(app.appId, index)
        }
    }

    override suspend fun launchApp(appId: String): Result<Unit> =
        withContext(Dispatchers.Main) {
            try {
                // 检查行驶限制
                if (isAppRestricted(appId)) {
                    return@withContext Result.failure(AppRestrictedException())
                }

                val intent = context.packageManager.getLaunchIntentForPackage(appId)
                    ?: return@withContext Result.failure(Exception("应用未找到: $appId"))

                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                context.startActivity(intent)

                // 更新启动次数
                incrementLaunchCount(appId)

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun isAppRestricted(appId: String): Boolean {
        val restrictionStatus = vehicleService.getDrivingRestrictionStatus()
        val app = getAppById(appId) ?: return false

        return when {
            app.isInWhitelist -> false
            app.categoryId == AppCategory.VIDEO.ordinal && restrictionStatus.isRestricted -> true
            app.categoryId == AppCategory.GAME.ordinal && restrictionStatus.isRestricted -> true
            else -> false
        }
    }

    override fun observeAppChanges(): Flow<List<AppInfo>> = _appChangesFlow.asStateFlow()

    private fun observePackageChanges() {
        // 注册应用安装/卸载广播接收器
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }
        // 实际实现中需要注册广播接收器
    }

    private suspend fun refreshAppList() {
        _appChangesFlow.value = getAllApps()
    }

    /**
     * 从系统PackageManager同步应用列表到数据库
     */
    suspend fun syncAppsFromSystem() = withContext(Dispatchers.IO) {
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { appInfo ->
                // 过滤掉系统组件和不可启动的应用
                appInfo.enabled &&
                packageManager.getLaunchIntentForPackage(appInfo.packageName) != null
            }
            .mapIndexed { index, appInfo ->
                val packageInfo = packageManager.getPackageInfo(appInfo.packageName, 0)
                AppInfo(
                    appId = appInfo.packageName,
                    appName = packageManager.getApplicationLabel(appInfo).toString(),
                    categoryId = AppCategory.TOOL.ordinal, // 默认分类
                    sortOrder = index,
                    isSystem = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                )
            }

        appDao.insertApps(installedApps)
    }
}
