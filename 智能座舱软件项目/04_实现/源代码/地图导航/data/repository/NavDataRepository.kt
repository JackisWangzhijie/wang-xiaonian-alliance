package com.longcheer.cockpit.nav.data.repository

import com.longcheer.cockpit.nav.data.local.database.dao.*
import com.longcheer.cockpit.nav.data.local.database.entity.*
import com.longcheer.cockpit.nav.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 导航数据仓库
 * 负责导航历史、收藏等数据的持久化操作
 * 
 * @author 龙旗智能导航团队
 * @version 1.0.0
 */
@Singleton
class NavDataRepository @Inject constructor(
    private val navDatabase: NavDatabase
) {
    private val historyDao = navDatabase.navHistoryDao()
    private val favoriteDao = navDatabase.navFavoriteDao()
    
    // ==================== 导航历史 ====================
    
    /**
     * 保存导航历史
     */
    suspend fun saveNavHistory(route: Route, userId: Long = 0): Long {
        val entity = NavHistoryEntity(
            userId = userId,
            startName = route.startPoint.name.ifEmpty { "当前位置" },
            startLat = route.startPoint.latitude,
            startLng = route.startPoint.longitude,
            endName = route.endPoint.name,
            endLat = route.endPoint.latitude,
            endLng = route.endPoint.longitude,
            endAddress = route.endPoint.address,
            distance = route.distance,
            duration = route.duration,
            routeType = route.strategy.ordinal,
            startTime = System.currentTimeMillis()
        )
        return historyDao.insert(entity)
    }
    
    /**
     * 获取导航历史列表
     */
    suspend fun getNavHistory(userId: Long = 0, limit: Int = 100): List<NavHistory> {
        return historyDao.getHistoryList(userId, limit).map { it.toNavHistory() }
    }
    
    /**
     * 获取导航历史流
     */
    fun getNavHistoryFlow(userId: Long = 0, limit: Int = 100): Flow<List<NavHistory>> {
        return historyDao.getHistoryListFlow(userId, limit).map { list ->
            list.map { it.toNavHistory() }
        }
    }
    
    /**
     * 根据目的地查找历史记录
     */
    suspend fun findHistoryByDestination(userId: Long, lat: Double, lng: Double): NavHistory? {
        return historyDao.findByDestination(userId, lat, lng)?.toNavHistory()
    }
    
    /**
     * 设置历史记录收藏状态
     */
    suspend fun setFavorite(historyId: Long, isFavorite: Boolean) {
        historyDao.setFavorite(historyId, isFavorite)
    }
    
    /**
     * 完成导航
     */
    suspend fun completeNavigation(historyId: Long) {
        historyDao.completeNavigation(historyId, true, System.currentTimeMillis())
    }
    
    /**
     * 删除历史记录
     */
    suspend fun deleteHistory(historyId: Long) {
        historyDao.softDelete(historyId)
    }
    
    /**
     * 清理过期历史记录
     */
    suspend fun cleanupOldHistory(daysToKeep: Int = 30) {
        val cutoffTime = System.currentTimeMillis() - daysToKeep * 24 * 60 * 60 * 1000
        historyDao.deleteBefore(cutoffTime)
    }
    
    // ==================== 收藏管理 ====================
    
    /**
     * 添加收藏
     */
    suspend fun addFavorite(favorite: NavFavorite, userId: Long = 0): Long {
        val entity = NavFavoriteEntity(
            userId = userId,
            favType = favorite.type.ordinal,
            name = favorite.name,
            address = favorite.address,
            lat = favorite.location.latitude,
            lng = favorite.location.longitude,
            poiId = favorite.poiId,
            phone = favorite.phone,
            category = favorite.category
        )
        return favoriteDao.insert(entity)
    }
    
    /**
     * 获取收藏列表
     */
    suspend fun getFavorites(userId: Long = 0): List<NavFavorite> {
        return favoriteDao.getFavorites(userId).map { it.toNavFavorite() }
    }
    
    /**
     * 获取收藏列表流
     */
    fun getFavoritesFlow(userId: Long = 0): Flow<List<NavFavorite>> {
        return favoriteDao.getFavoritesFlow(userId).map { list ->
            list.map { it.toNavFavorite() }
        }
    }
    
    /**
     * 获取家地址
     */
    suspend fun getHomeAddress(userId: Long = 0): NavFavorite? {
        return favoriteDao.getHomeAddress(userId)?.toNavFavorite()
    }
    
    /**
     * 获取公司地址
     */
    suspend fun getCompanyAddress(userId: Long = 0): NavFavorite? {
        return favoriteDao.getCompanyAddress(userId)?.toNavFavorite()
    }
    
    /**
     * 搜索收藏
     */
    suspend fun searchFavorites(userId: Long, keyword: String): List<NavFavorite> {
        return favoriteDao.searchFavorites(userId, keyword).map { it.toNavFavorite() }
    }
    
    /**
     * 删除收藏
     */
    suspend fun deleteFavorite(favoriteId: Long) {
        favoriteDao.deleteById(favoriteId)
    }
    
    /**
     * 更新收藏排序
     */
    suspend fun updateSortOrder(favoriteId: Long, order: Int) {
        favoriteDao.updateSortOrder(favoriteId, order)
    }
    
    // ==================== 扩展函数：实体转模型 ====================
    
    private fun NavHistoryEntity.toNavHistory(): NavHistory {
        return NavHistory(
            id = id,
            startName = startName,
            startLat = startLat,
            startLng = startLng,
            endName = endName,
            endLat = endLat,
            endLng = endLng,
            endAddress = endAddress,
            distance = distance,
            duration = duration,
            startTime = startTime,
            isCompleted = isCompleted,
            isFavorite = isFavorite
        )
    }
    
    private fun NavFavoriteEntity.toNavFavorite(): NavFavorite {
        return NavFavorite(
            id = id,
            type = FavoriteType.values().getOrElse(favType) { FavoriteType.CUSTOM },
            name = name,
            address = address,
            location = LatLng(lat, lng),
            poiId = poiId,
            phone = phone,
            category = category
        )
    }
}