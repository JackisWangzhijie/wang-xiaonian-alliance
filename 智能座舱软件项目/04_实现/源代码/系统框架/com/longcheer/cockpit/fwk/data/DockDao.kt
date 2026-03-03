package com.longcheer.cockpit.fwk.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.longcheer.cockpit.fwk.model.DockConfig
import kotlinx.coroutines.flow.Flow

/**
 * Dock数据访问对象
 * 需求追溯: REQ-FWK-FUN-016
 */
@Dao
interface DockDao {
    @Query("SELECT * FROM dock_config ORDER BY slot_index")
    suspend fun getAllConfig(): List<DockConfig>

    @Query("SELECT * FROM dock_config ORDER BY slot_index")
    fun observeAllConfig(): Flow<List<DockConfig>>

    @Query("SELECT * FROM dock_config WHERE slot_type = :slotType ORDER BY slot_index")
    suspend fun getConfigByType(slotType: String): List<DockConfig>

    @Query("SELECT * FROM dock_config WHERE slot_index = :index")
    suspend fun getConfigByIndex(index: Int): DockConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: DockConfig)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateConfig(config: List<DockConfig>)

    @Query("DELETE FROM dock_config WHERE slot_index = :index")
    suspend fun deleteConfigByIndex(index: Int)

    @Query("DELETE FROM dock_config")
    suspend fun deleteAllConfig()
}
