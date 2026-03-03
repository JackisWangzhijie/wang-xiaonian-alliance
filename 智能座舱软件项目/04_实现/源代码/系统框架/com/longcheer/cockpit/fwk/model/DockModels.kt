package com.longcheer.cockpit.fwk.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Dock栏配置数据类
 * 对应数据库表: dock_config
 * 需求追溯: REQ-FWK-FUN-016
 */
@Entity(tableName = "dock_config")
data class DockConfig(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "slot_index")
    val slotIndex: Int,                   // 槽位索引 (0-3固定槽位)

    @ColumnInfo(name = "slot_type")
    val slotType: DockSlotType,           // 槽位类型

    @ColumnInfo(name = "app_id")
    val appId: String?,                   // 应用ID (固定应用时使用)

    @ColumnInfo(name = "icon_resource")
    val iconResource: String?,            // 图标资源

    @ColumnInfo(name = "action_type")
    val actionType: DockActionType,       // 动作类型

    @ColumnInfo(name = "action_data")
    val actionData: String?               // 动作参数(JSON)
)

/**
 * Dock槽位类型
 */
enum class DockSlotType {
    FIXED_APP,        // 固定应用
    RECENT_APP,       // 最近应用
    HOME_BUTTON,      // Home键
    QUICK_ACTION      // 快捷操作
}

/**
 * Dock动作类型
 */
enum class DockActionType {
    LAUNCH_APP,       // 启动应用
    GO_HOME,          // 返回Home
    SHOW_RECENTS,     // 显示最近任务
    QUICK_SETTINGS,   // 快捷设置
    VOICE_ASSISTANT   // 语音助手
}

/**
 * 最近应用信息
 */
data class RecentAppInfo(
    val appId: String,
    val appName: String,
    val iconPath: String?,
    val lastUsedTime: Long,
    val isRunning: Boolean = false
)

/**
 * Dock项密封类
 * 用于UI层展示
 */
sealed class DockItem {
    abstract val appId: String
    abstract val name: String
    abstract val iconPath: String?

    data class FixedAppItem(
        override val appId: String,
        override val name: String,
        override val iconPath: String?,
        val isEnabled: Boolean = true
    ) : DockItem()

    data class RecentAppItem(
        override val appId: String,
        override val name: String,
        override val iconPath: String?,
        val isRunning: Boolean = false
    ) : DockItem()

    data class QuickActionItem(
        override val appId: String,
        override val name: String,
        override val iconPath: String?,
        val actionType: DockActionType
    ) : DockItem()

    data class HomeButtonItem(
        override val appId: String = "home",
        override val name: String = "Home",
        override val iconPath: String? = null
    ) : DockItem()
}
