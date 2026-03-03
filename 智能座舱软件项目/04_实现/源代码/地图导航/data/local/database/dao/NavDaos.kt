package com.longcheer.cockpit.nav.data.local.database.dao

import androidx.room.*
import com.longcheer.cockpit.nav.data.local.database.entity.*
import kotlinx.coroutines.flow.Flow

/**
 * 导航历史DAO
 */
@Dao
interface NavHistoryDao {
    
    @Query("""
        SELECT * FROM nav_history 
        WHERE user_id = :userId AND is_deleted = 0 
        ORDER BY start_time DESC 
        LIMIT :limit
    """)
    suspend fun getHistoryList(userId: Long, limit: Int = 100): List<NavHistoryEntity>
    
    @Query("""
        SELECT * FROM nav_history 
        WHERE user_id = :userId AND is_deleted = 0 
        ORDER BY start_time DESC 
        LIMIT :limit
    """)
    fun getHistoryListFlow(userId: Long, limit: Int = 100): Flow<List<NavHistoryEntity>>
    
    @Query("SELECT * FROM nav_history WHERE id = :id")
    suspend fun getHistoryById(id: Long): NavHistoryEntity?
    
    @Insert
    suspend fun insert(history: NavHistoryEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(historyList: List<NavHistoryEntity>)
    
    @Update
    suspend fun update(history: NavHistoryEntity)
    
    @Query("UPDATE nav_history SET is_deleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)
    
    @Query("DELETE FROM nav_history WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("DELETE FROM nav_history WHERE start_time < :beforeTime")
    suspend fun deleteBefore(beforeTime: Long)
    
    @Query("DELETE FROM nav_history WHERE user_id = :userId AND is_deleted = 1")
    suspend fun cleanDeleted(userId: Long)
    
    @Query("""
        SELECT * FROM nav_history 
        WHERE user_id = :userId AND is_favorite = 1 AND is_deleted = 0 
        ORDER BY start_time DESC
    """)
    suspend fun getFavoriteHistory(userId: Long): List<NavHistoryEntity>
    
    @Query("""
        SELECT * FROM nav_history 
        WHERE user_id = :userId AND end_lat = :lat AND end_lng = :lng 
        AND is_deleted = 0 
        ORDER BY start_time DESC LIMIT 1
    """)
    suspend fun findByDestination(userId: Long, lat: Double, lng: Double): NavHistoryEntity?
    
    @Query("UPDATE nav_history SET is_favorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)
    
    @Query("""
        UPDATE nav_history 
        SET is_completed = :isCompleted, end_time = :endTime 
        WHERE id = :id
    """)
    suspend fun completeNavigation(id: Long, isCompleted: Boolean, endTime: Long)
    
    @Query("SELECT COUNT(*) FROM nav_history WHERE user_id = :userId AND is_deleted = 0")
    suspend fun getCount(userId: Long): Int
    
    @Query("SELECT * FROM nav_history WHERE user_id = :userId AND is_deleted = 0 ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomHistory(userId: Long, limit: Int): List<NavHistoryEntity>
}

/**
 * 导航收藏DAO
 */
@Dao
interface NavFavoriteDao {
    
    @Query("""
        SELECT * FROM nav_favorite 
        WHERE user_id = :userId 
        ORDER BY fav_type ASC, sort_order ASC, create_time DESC
    """)
    suspend fun getFavorites(userId: Long): List<NavFavoriteEntity>
    
    @Query("""
        SELECT * FROM nav_favorite 
        WHERE user_id = :userId 
        ORDER BY fav_type ASC, sort_order ASC, create_time DESC
    """)
    fun getFavoritesFlow(userId: Long): Flow<List<NavFavoriteEntity>>
    
    @Query("SELECT * FROM nav_favorite WHERE user_id = :userId AND fav_type = :type")
    suspend fun getFavoritesByType(userId: Long, type: Int): List<NavFavoriteEntity>
    
    @Query("SELECT * FROM nav_favorite WHERE id = :id")
    suspend fun getFavoriteById(id: Long): NavFavoriteEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: NavFavoriteEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(favorites: List<NavFavoriteEntity>)
    
    @Update
    suspend fun update(favorite: NavFavoriteEntity)
    
    @Delete
    suspend fun delete(favorite: NavFavoriteEntity)
    
    @Query("DELETE FROM nav_favorite WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("SELECT * FROM nav_favorite WHERE is_synced = 0")
    suspend fun getUnsynced(): List<NavFavoriteEntity>
    
    @Query("UPDATE nav_favorite SET is_synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)
    
    @Query("SELECT * FROM nav_favorite WHERE user_id = :userId AND fav_type = 0 LIMIT 1")
    suspend fun getHomeAddress(userId: Long): NavFavoriteEntity?
    
    @Query("SELECT * FROM nav_favorite WHERE user_id = :userId AND fav_type = 1 LIMIT 1")
    suspend fun getCompanyAddress(userId: Long): NavFavoriteEntity?
    
    @Query("""
        SELECT * FROM nav_favorite 
        WHERE user_id = :userId 
        AND (name LIKE '%' || :keyword || '%' OR address LIKE '%' || :keyword || '%')
    """)
    suspend fun searchFavorites(userId: Long, keyword: String): List<NavFavoriteEntity>
    
    @Query("UPDATE nav_favorite SET sort_order = :order WHERE id = :id")
    suspend fun updateSortOrder(id: Long, order: Int)
    
    @Query("SELECT COUNT(*) FROM nav_favorite WHERE user_id = :userId")
    suspend fun getCount(userId: Long): Int
}

/**
 * 离线地图DAO
 */
@Dao
interface OfflineMapDao {
    
    @Query("SELECT * FROM offline_map ORDER BY city_name")
    suspend fun getAll(): List<OfflineMapEntity>
    
    @Query("SELECT * FROM offline_map ORDER BY city_name")
    fun getAllFlow(): Flow<List<OfflineMapEntity>>
    
    @Query("SELECT * FROM offline_map WHERE city_code = :cityCode")
    suspend fun getByCityCode(cityCode: String): OfflineMapEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: OfflineMapEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<OfflineMapEntity>)
    
    @Update
    suspend fun update(entity: OfflineMapEntity)
    
    @Delete
    suspend fun delete(entity: OfflineMapEntity)
    
    @Query("DELETE FROM offline_map WHERE city_code = :cityCode")
    suspend fun deleteByCityCode(cityCode: String)
    
    @Query("SELECT * FROM offline_map WHERE has_update = 1")
    suspend fun getUpdatableCities(): List<OfflineMapEntity>
    
    @Query("SELECT SUM(size) FROM offline_map")
    suspend fun getTotalSize(): Long?
    
    @Query("SELECT COUNT(*) FROM offline_map")
    suspend fun getCount(): Int
    
    @Query("UPDATE offline_map SET has_update = :hasUpdate, latest_version = :latestVersion WHERE city_code = :cityCode")
    suspend fun setUpdateStatus(cityCode: String, hasUpdate: Boolean, latestVersion: String?)
    
    @Query("SELECT * FROM offline_map WHERE city_name LIKE '%' || :keyword || '%'")
    suspend fun searchByName(keyword: String): List<OfflineMapEntity>
}

/**
 * 导航设置DAO
 */
@Dao
interface NavSettingsDao {
    
    @Query("SELECT * FROM nav_settings WHERE user_id = :userId AND setting_key = :key")
    suspend fun getSetting(userId: Long, key: String): NavSettingsEntity?
    
    @Query("SELECT * FROM nav_settings WHERE user_id = :userId")
    suspend fun getAllSettings(userId: Long): List<NavSettingsEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(setting: NavSettingsEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(settings: List<NavSettingsEntity>)
    
    @Update
    suspend fun update(setting: NavSettingsEntity)
    
    @Query("DELETE FROM nav_settings WHERE user_id = :userId AND setting_key = :key")
    suspend fun delete(userId: Long, key: String)
    
    @Query("DELETE FROM nav_settings WHERE user_id = :userId")
    suspend fun deleteAll(userId: Long)
    
    @Query("SELECT setting_value FROM nav_settings WHERE user_id = :userId AND setting_key = :key")
    suspend fun getString(userId: Long, key: String, defaultValue: String = ""): String?
    
    @Query("SELECT setting_value FROM nav_settings WHERE user_id = :userId AND setting_key = :key")
    suspend fun getInt(userId: Long, key: String, defaultValue: Int = 0): Int?
}

/**
 * 搜索历史DAO
 */
@Dao
interface SearchHistoryDao {
    
    @Query("""
        SELECT * FROM search_history 
        WHERE user_id = :userId 
        ORDER BY search_count DESC, search_time DESC 
        LIMIT :limit
    """)
    suspend fun getRecentSearches(userId: Long, limit: Int = 20): List<SearchHistoryEntity>
    
    @Query("""
        SELECT * FROM search_history 
        WHERE user_id = :userId 
        ORDER BY search_count DESC, search_time DESC 
        LIMIT :limit
    """)
    fun getRecentSearchesFlow(userId: Long, limit: Int = 20): Flow<List<SearchHistoryEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(search: SearchHistoryEntity): Long
    
    @Query("""
        UPDATE search_history 
        SET search_count = search_count + 1, search_time = :time 
        WHERE id = :id
    """)
    suspend fun incrementCount(id: Long, time: Long = System.currentTimeMillis())
    
    @Query("DELETE FROM search_history WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("DELETE FROM search_history WHERE user_id = :userId")
    suspend fun deleteAll(userId: Long)
    
    @Query("""
        DELETE FROM search_history 
        WHERE user_id = :userId 
        AND id NOT IN (
            SELECT id FROM search_history 
            WHERE user_id = :userId 
            ORDER BY search_count DESC, search_time DESC 
            LIMIT :keepCount
        )
    """)
    suspend fun trimHistory(userId: Long, keepCount: Int = 50)
    
    @Query("""
        SELECT * FROM search_history 
        WHERE user_id = :userId AND keyword LIKE '%' || :keyword || '%'
        ORDER BY search_count DESC 
        LIMIT :limit
    """)
    suspend fun searchHistory(userId: Long, keyword: String, limit: Int = 10): List<SearchHistoryEntity>
}

/**
 * 路况缓存DAO
 */
@Dao
interface TrafficCacheDao {
    
    @Query("SELECT * FROM traffic_cache WHERE road_id = :roadId")
    suspend fun getByRoadId(roadId: String): TrafficCacheEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cache: TrafficCacheEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(caches: List<TrafficCacheEntity>)
    
    @Query("DELETE FROM traffic_cache WHERE expire_time < :currentTime")
    suspend fun deleteExpired(currentTime: Long = System.currentTimeMillis())
    
    @Query("DELETE FROM traffic_cache")
    suspend fun clearAll()
    
    @Query("SELECT * FROM traffic_cache WHERE timestamp > :since")
    suspend fun getRecent(since: Long): List<TrafficCacheEntity>
}

/**
 * 地图瓦片缓存DAO
 */
@Dao
interface TileCacheDao {
    
    @Query("SELECT * FROM tile_cache WHERE x = :x AND y = :y AND zoom = :zoom AND map_type = :mapType")
    suspend fun getTile(x: Int, y: Int, zoom: Int, mapType: Int): TileCacheEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tile: TileCacheEntity)
    
    @Query("""
        UPDATE tile_cache 
        SET access_count = access_count + 1, timestamp = :time 
        WHERE id = :id
    """)
    suspend fun updateAccess(id: Long, time: Long = System.currentTimeMillis())
    
    @Query("SELECT * FROM tile_cache ORDER BY timestamp ASC LIMIT :count")
    suspend fun getOldestTiles(count: Int): List<TileCacheEntity>
    
    @Query("DELETE FROM tile_cache WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    @Query("SELECT SUM(file_size) FROM tile_cache")
    suspend fun getTotalSize(): Long?
    
    @Query("SELECT COUNT(*) FROM tile_cache")
    suspend fun getCount(): Int
    
    @Query("DELETE FROM tile_cache")
    suspend fun clearAll()
}