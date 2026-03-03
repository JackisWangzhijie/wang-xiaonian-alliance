package com.longcheer.cockpit.nav.data.repository

import com.longcheer.cockpit.nav.data.local.database.NavDatabase
import com.longcheer.cockpit.nav.data.local.database.entity.OfflineMapEntity
import com.longcheer.cockpit.nav.model.OfflineCity
import com.longcheer.cockpit.nav.model.OfflineState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 离线地图数据仓库
 * 管理离线地图的下载、更新和存储
 * 
 * @author 龙旗智能导航团队
 * @version 1.0.0
 */
@Singleton
class OfflineMapRepository @Inject constructor(
    private val navDatabase: NavDatabase
) {
    private val offlineMapDao = navDatabase.offlineMapDao()
    
    private val _offlineState = MutableStateFlow<OfflineState>(OfflineState.Idle)
    val offlineState: StateFlow<OfflineState> = _offlineState
    
    /**
     * 获取已下载的离线地图列表
     */
    suspend fun getDownloadedCities(): List<OfflineCity> {
        return offlineMapDao.getAll().map { it.toOfflineCity() }
    }
    
    /**
     * 获取已下载的离线地图流
     */
    fun getDownloadedCitiesFlow(): Flow<List<OfflineCity>> {
        return offlineMapDao.getAllFlow().map { list ->
            list.map { it.toOfflineCity() }
        }
    }
    
    /**
     * 保存离线地图记录
     */
    suspend fun saveOfflineMap(city: OfflineCity, localPath: String) {
        val entity = OfflineMapEntity(
            cityCode = city.code,
            cityName = city.name,
            size = city.size,
            version = city.version,
            localPath = localPath,
            downloadTime = System.currentTimeMillis(),
            hasUpdate = false
        )
        offlineMapDao.insert(entity)
    }
    
    /**
     * 删除离线地图记录
     */
    suspend fun removeOfflineMap(cityCode: String) {
        offlineMapDao.deleteByCityCode(cityCode)
    }
    
    /**
     * 更新离线地图版本
     */
    suspend fun updateOfflineMapVersion(cityCode: String, newVersion: String, newSize: Long) {
        val existing = offlineMapDao.getByCityCode(cityCode) ?: return
        val updated = existing.copy(
            hasUpdate = true,
            latestVersion = newVersion,
            size = newSize
        )
        offlineMapDao.update(updated)
    }
    
    /**
     * 获取需要更新的城市列表
     */
    suspend fun getUpdatableCities(): List<OfflineCity> {
        return offlineMapDao.getUpdatableCities().map { it.toOfflineCity() }
    }
    
    /**
     * 获取离线地图总大小
     */
    suspend fun getTotalSize(): Long {
        return offlineMapDao.getTotalSize() ?: 0L
    }
    
    /**
     * 搜索离线地图
     */
    suspend fun searchByName(keyword: String): List<OfflineCity> {
        return offlineMapDao.searchByName(keyword).map { it.toOfflineCity() }
    }
    
    /**
     * 检查城市是否已下载
     */
    suspend fun isCityDownloaded(cityCode: String): Boolean {
        return offlineMapDao.getByCityCode(cityCode) != null
    }
    
    /**
     * 设置下载状态
     */
    fun setOfflineState(state: OfflineState) {
        _offlineState.value = state
    }
    
    /**
     * 获取支持离线地图的城市列表（从高德SDK获取）
     * 注：实际实现需要通过AMapOfflineMapManager获取
     */
    suspend fun getOfflineCityList(): List<OfflineCity> {
        // TODO: 调用高德离线地图SDK获取
        return emptyList()
    }
    
    /**
     * 搜索城市
     */
    suspend fun searchCity(keyword: String): List<OfflineCity> {
        // TODO: 调用高德离线地图SDK搜索
        return emptyList()
    }
    
    private fun OfflineMapEntity.toOfflineCity(): OfflineCity {
        return OfflineCity(
            code = cityCode,
            name = cityName,
            size = size,
            version = version,
            hasUpdate = hasUpdate
        )
    }
}