package com.longcheer.cockpit.nav.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 导航历史记录实体
 */
@Entity(
    tableName = "nav_history",
    indices = [Index(value = ["user_id", "start_time"])]
)
data class NavHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "user_id")
    val userId: Long = 0,
    
    @ColumnInfo(name = "start_name")
    val startName: String,
    
    @ColumnInfo(name = "start_lat")
    val startLat: Double,
    
    @ColumnInfo(name = "start_lng")
    val startLng: Double,
    
    @ColumnInfo(name = "end_name")
    val endName: String,
    
    @ColumnInfo(name = "end_lat")
    val endLat: Double,
    
    @ColumnInfo(name = "end_lng")
    val endLng: Double,
    
    @ColumnInfo(name = "end_address")
    val endAddress: String? = null,
    
    @ColumnInfo(name = "distance")
    val distance: Int = 0,
    
    @ColumnInfo(name = "duration")
    val duration: Int = 0,
    
    @ColumnInfo(name = "route_type")
    val routeType: Int = 0,
    
    @ColumnInfo(name = "start_time")
    val startTime: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "end_time")
    val endTime: Long? = null,
    
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,
    
    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,
    
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false
)

/**
 * 导航收藏实体
 */
@Entity(
    tableName = "nav_favorite",
    indices = [
        Index(value = ["user_id", "fav_type", "name"]),
        Index(value = ["user_id", "lat", "lng"])
    ]
)
data class NavFavoriteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "user_id")
    val userId: Long = 0,
    
    @ColumnInfo(name = "fav_type")
    val favType: Int = 2, // 0=家, 1=公司, 2=常用, 3=自定义
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "address")
    val address: String? = null,
    
    @ColumnInfo(name = "lat")
    val lat: Double,
    
    @ColumnInfo(name = "lng")
    val lng: Double,
    
    @ColumnInfo(name = "poi_id")
    val poiId: String? = null,
    
    @ColumnInfo(name = "phone")
    val phone: String? = null,
    
    @ColumnInfo(name = "category")
    val category: String? = null,
    
    @ColumnInfo(name = "icon")
    val icon: String? = null,
    
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,
    
    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false,
    
    @ColumnInfo(name = "create_time")
    val createTime: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "update_time")
    val updateTime: Long = System.currentTimeMillis()
)

/**
 * 离线地图实体
 */
@Entity(
    tableName = "offline_map",
    indices = [Index(value = ["city_name"])]
)
data class OfflineMapEntity(
    @PrimaryKey
    @ColumnInfo(name = "city_code")
    val cityCode: String,
    
    @ColumnInfo(name = "city_name")
    val cityName: String,
    
    @ColumnInfo(name = "size")
    val size: Long = 0,
    
    @ColumnInfo(name = "version")
    val version: String,
    
    @ColumnInfo(name = "local_path")
    val localPath: String? = null,
    
    @ColumnInfo(name = "download_time")
    val downloadTime: Long? = null,
    
    @ColumnInfo(name = "has_update")
    val hasUpdate: Boolean = false,
    
    @ColumnInfo(name = "latest_version")
    val latestVersion: String? = null
)

/**
 * 导航设置实体
 */
@Entity(
    tableName = "nav_settings",
    indices = [Index(value = ["user_id", "setting_key"], unique = true)]
)
data class NavSettingsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "user_id")
    val userId: Long = 0,
    
    @ColumnInfo(name = "setting_key")
    val settingKey: String,
    
    @ColumnInfo(name = "setting_value")
    val settingValue: String,
    
    @ColumnInfo(name = "setting_type")
    val settingType: Int = 0, // 0=字符串, 1=整数, 2=浮点, 3=布尔
    
    @ColumnInfo(name = "update_time")
    val updateTime: Long = System.currentTimeMillis()
)

/**
 * 搜索历史实体
 */
@Entity(
    tableName = "search_history",
    indices = [Index(value = ["user_id", "search_time"])]
)
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "user_id")
    val userId: Long = 0,
    
    @ColumnInfo(name = "keyword")
    val keyword: String,
    
    @ColumnInfo(name = "poi_id")
    val poiId: String? = null,
    
    @ColumnInfo(name = "poi_name")
    val poiName: String? = null,
    
    @ColumnInfo(name = "lat")
    val lat: Double? = null,
    
    @ColumnInfo(name = "lng")
    val lng: Double? = null,
    
    @ColumnInfo(name = "search_time")
    val searchTime: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "search_count")
    val searchCount: Int = 1
)

/**
 * 路况缓存实体
 */
@Entity(
    tableName = "traffic_cache",
    indices = [Index(value = ["road_id", "timestamp"])]
)
data class TrafficCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "road_id")
    val roadId: String,
    
    @ColumnInfo(name = "status")
    val status: Int, // 0=畅通, 1=缓行, 2=拥堵, 3=严重拥堵
    
    @ColumnInfo(name = "speed")
    val speed: Int, // 平均速度(km/h)
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "expire_time")
    val expireTime: Long = System.currentTimeMillis() + 5 * 60 * 1000 // 5分钟过期
)

/**
 * 地图瓦片缓存实体
 */
@Entity(
    tableName = "tile_cache",
    indices = [Index(value = ["x", "y", "zoom", "map_type"], unique = true)]
)
data class TileCacheEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "x")
    val x: Int,
    
    @ColumnInfo(name = "y")
    val y: Int,
    
    @ColumnInfo(name = "zoom")
    val zoom: Int,
    
    @ColumnInfo(name = "map_type")
    val mapType: Int,
    
    @ColumnInfo(name = "file_path")
    val filePath: String,
    
    @ColumnInfo(name = "file_size")
    val fileSize: Long,
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "access_count")
    val accessCount: Int = 1
)