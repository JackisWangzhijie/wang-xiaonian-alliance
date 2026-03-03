package com.longcheer.cockpit.fwk.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.longcheer.cockpit.fwk.model.AppInfo
import kotlinx.coroutines.flow.Flow

/**
 * 应用数据访问对象
 * 需求追溯: REQ-FWK-FUN-014
 */
@Dao
interface AppDao {
    @Query("SELECT * FROM application WHERE is_enabled = 1 ORDER BY sort_order, app_name")
    suspend fun getAllApps(): List<AppInfo>

    @Query("SELECT * FROM application WHERE is_enabled = 1 ORDER BY sort_order, app_name")
    fun observeAllApps(): Flow<List<AppInfo>>

    @Query("SELECT * FROM application WHERE app_id = :appId")
    suspend fun getAppById(appId: String): AppInfo?

    @Query("SELECT * FROM application WHERE category_id = :categoryId AND is_enabled = 1 ORDER BY sort_order")
    suspend fun getAppsByCategory(categoryId: Int): List<AppInfo>

    @Query("SELECT * FROM application ORDER BY launch_count DESC, last_launch DESC LIMIT :limit")
    suspend fun getFrequentlyUsedApps(limit: Int): List<AppInfo>

    @Query("SELECT * FROM application WHERE app_name LIKE '%' || :query || '%' OR app_name_en LIKE '%' || :query || '%'")
    suspend fun searchApps(query: String): List<AppInfo>

    @Query("UPDATE application SET is_enabled = :isEnabled WHERE app_id = :appId")
    suspend fun updateAppStatus(appId: String, isEnabled: Boolean)

    @Query("UPDATE application SET launch_count = launch_count + 1, last_launch = :timestamp WHERE app_id = :appId")
    suspend fun incrementLaunchCount(appId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE application SET sort_order = :order WHERE app_id = :appId")
    suspend fun updateSortOrder(appId: String, order: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: AppInfo)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<AppInfo>)

    @Update
    suspend fun updateApp(app: AppInfo)

    @Delete
    suspend fun deleteApp(app: AppInfo)

    @Query("DELETE FROM application WHERE app_id = :appId")
    suspend fun deleteAppById(appId: String)

    @Query("SELECT COUNT(*) FROM application WHERE is_enabled = 1")
    suspend fun getEnabledAppCount(): Int
}
