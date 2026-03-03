package com.longcheer.cockpit.nav.data.repository

import com.longcheer.cockpit.nav.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 地图数据仓库
 * 负责地图相关的数据获取，包括POI搜索、地理编码等
 * 
 * @author 龙旗智能导航团队
 * @version 1.0.0
 */
@Singleton
class MapDataRepository @Inject constructor(
    private val searchHistoryDao: com.longcheer.cockpit.nav.data.local.database.dao.SearchHistoryDao
) {
    
    /**
     * 搜索POI
     * @param keyword 关键词
     * @param location 中心位置
     * @param radius 搜索半径(米)
     * @return 搜索结果
     */
    suspend fun searchPoi(
        keyword: String,
        location: LatLng? = null,
        radius: Int = 5000
    ): List<Poi> {
        // TODO: 调用高德搜索SDK
        return emptyList()
    }
    
    /**
     * 搜索POI（带Flow）
     */
    fun searchPoiFlow(param: PoiSearchParam): Flow<Result<List<Poi>>> = flow {
        try {
            val results = searchPoi(param.keyword, param.location, param.radius)
            emit(Result.success(results))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    /**
     * 根据类别搜索附近POI
     */
    suspend fun searchNearbyPoi(
        category: PoiType,
        location: LatLng,
        radius: Int = 5000
    ): List<Poi> {
        // TODO: 调用高德搜索SDK
        return emptyList()
    }
    
    /**
     * 获取POI详情
     */
    suspend fun getPoiDetail(poiId: String): Poi? {
        // TODO: 调用高德搜索SDK
        return null
    }
    
    /**
     * 地理编码（地址转坐标）
     */
    suspend fun geocode(address: String): LatLng? {
        // TODO: 调用高德地理编码SDK
        return null
    }
    
    /**
     * 逆地理编码（坐标转地址）
     */
    suspend fun reverseGeocode(latLng: LatLng): String? {
        // TODO: 调用高德逆地理编码SDK
        return null
    }
    
    /**
     * 获取输入提示
     */
    suspend fun getInputTips(keyword: String, city: String? = null): List<String> {
        // TODO: 调用高德输入提示SDK
        return emptyList()
    }
    
    /**
     * 保存搜索历史
     */
    suspend fun saveSearchHistory(keyword: String, poi: Poi? = null, userId: Long = 0) {
        val entity = com.longcheer.cockpit.nav.data.local.database.entity.SearchHistoryEntity(
            userId = userId,
            keyword = keyword,
            poiId = poi?.id,
            poiName = poi?.name,
            lat = poi?.location?.latitude,
            lng = poi?.location?.longitude
        )
        searchHistoryDao.insert(entity)
    }
    
    /**
     * 获取搜索历史
     */
    suspend fun getSearchHistory(userId: Long = 0, limit: Int = 20): List<String> {
        return searchHistoryDao.getRecentSearches(userId, limit).map { it.keyword }
    }
    
    /**
     * 清除搜索历史
     */
    suspend fun clearSearchHistory(userId: Long = 0) {
        searchHistoryDao.deleteAll(userId)
    }
}