package com.longcheer.cockpit.fwk.repository

import com.longcheer.cockpit.fwk.model.DockConfig
import com.longcheer.cockpit.fwk.model.DockItem
import com.longcheer.cockpit.fwk.model.RecentAppInfo
import kotlinx.coroutines.flow.Flow

/**
 * Dock数据仓库接口
 * 需求追溯: REQ-FWK-FUN-016
 */
interface IDockRepository {
    suspend fun getDockConfig(): List<DockConfig>
    fun observeDockConfig(): Flow<List<DockConfig>>
    suspend fun updateDockConfig(config: List<DockConfig>)

    suspend fun getRecentApps(limit: Int = 4): List<RecentAppInfo>
    suspend fun addToRecentApps(appId: String, appName: String, iconPath: String?)
    suspend fun clearRecentApps()

    suspend fun handleHomeAction()
    suspend fun handleRecentAppsAction()
    suspend fun handleVoiceAssistantAction()
}
