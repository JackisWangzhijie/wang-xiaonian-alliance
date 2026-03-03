package com.longcheer.cockpit.fwk.repository

import com.longcheer.cockpit.fwk.model.AppCategory
import com.longcheer.cockpit.fwk.model.AppInfo
import kotlinx.coroutines.flow.Flow

/**
 * 应用数据仓库接口
 * 需求追溯: REQ-FWK-FUN-014
 */
interface IAppRepository {
    // 应用查询
    suspend fun getAllApps(): List<AppInfo>
    fun observeAllApps(): Flow<List<AppInfo>>
    suspend fun getAppsByCategory(category: AppCategory): List<AppInfo>
    suspend fun getAppById(appId: String): AppInfo?
    suspend fun getFrequentlyUsedApps(limit: Int = 8): List<AppInfo>
    suspend fun searchApps(query: String): List<AppInfo>

    // 应用状态管理
    suspend fun updateAppStatus(appId: String, isEnabled: Boolean)
    suspend fun incrementLaunchCount(appId: String)
    suspend fun updateAppOrder(apps: List<AppInfo>)

    // 应用启动
    suspend fun launchApp(appId: String): Result<Unit>
    suspend fun isAppRestricted(appId: String): Boolean

    // 应用安装/卸载监听
    fun observeAppChanges(): Flow<List<AppInfo>>
}
