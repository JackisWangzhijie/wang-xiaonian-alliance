package com.longcheer.cockpit.fwk.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.longcheer.cockpit.fwk.model.AppInfo
import com.longcheer.cockpit.fwk.model.DockConfig

/**
 * 应用数据库
 * 需求追溯: REQ-FWK-FUN-014, REQ-FWK-FUN-016
 */
@Database(
    entities = [AppInfo::class, DockConfig::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun dockDao(): DockDao

    companion object {
        private const val DATABASE_NAME = "cockpit_framework.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
