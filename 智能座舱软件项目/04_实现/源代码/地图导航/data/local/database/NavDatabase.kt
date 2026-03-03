package com.longcheer.cockpit.nav.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.longcheer.cockpit.nav.data.local.database.dao.*
import com.longcheer.cockpit.nav.data.local.database.entity.*

/**
 * 导航模块数据库
 * 包含导航历史、收藏、离线地图、设置等数据表
 * 
 * @author 龙旗智能导航团队
 * @version 1.0.0
 */
@Database(
    entities = [
        NavHistoryEntity::class,
        NavFavoriteEntity::class,
        OfflineMapEntity::class,
        NavSettingsEntity::class,
        SearchHistoryEntity::class,
        TrafficCacheEntity::class,
        TileCacheEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class NavDatabase : RoomDatabase() {
    
    abstract fun navHistoryDao(): NavHistoryDao
    abstract fun navFavoriteDao(): NavFavoriteDao
    abstract fun offlineMapDao(): OfflineMapDao
    abstract fun navSettingsDao(): NavSettingsDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun trafficCacheDao(): TrafficCacheDao
    abstract fun tileCacheDao(): TileCacheDao
    
    companion object {
        private const val DATABASE_NAME = "navigation.db"
        
        @Volatile
        private var INSTANCE: NavDatabase? = null
        
        fun getInstance(context: Context): NavDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }
        
        private fun buildDatabase(context: Context): NavDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                NavDatabase::class.java,
                DATABASE_NAME
            )
            .fallbackToDestructiveMigration()
            .build()
        }
    }
}