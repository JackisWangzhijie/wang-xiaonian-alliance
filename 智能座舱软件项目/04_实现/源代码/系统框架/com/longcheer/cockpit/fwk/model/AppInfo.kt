package com.longcheer.cockpit.fwk.model

import androidx.annotation.StringRes
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 应用信息数据类
 * 对应数据库表: application
 * 需求追溯: REQ-FWK-FUN-014
 */
@Entity(tableName = "application")
data class AppInfo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "app_id")
    val appId: String,                    // 应用包名 (唯一标识)

    @ColumnInfo(name = "app_name")
    val appName: String,                  // 应用显示名称

    @ColumnInfo(name = "app_name_en")
    val appNameEn: String? = null,        // 英文名称

    @ColumnInfo(name = "category_id")
    val categoryId: Int,                  // 应用分类ID

    @ColumnInfo(name = "icon_path")
    val iconPath: String? = null,         // 图标路径

    @ColumnInfo(name = "version_code")
    val versionCode: Int = 1,             // 版本号

    @ColumnInfo(name = "version_name")
    val versionName: String = "1.0.0",    // 版本名称

    @ColumnInfo(name = "is_system")
    val isSystem: Boolean = false,        // 是否系统应用

    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean = true,        // 是否启用

    @ColumnInfo(name = "is_in_whitelist")
    val isInWhitelist: Boolean = false,   // 是否在白名单

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,               // 显示排序

    @ColumnInfo(name = "launch_count")
    val launchCount: Int = 0,             // 启动次数

    @ColumnInfo(name = "last_launch")
    val lastLaunch: Long? = null,         // 最后启动时间

    @ColumnInfo(name = "create_time")
    val createTime: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "update_time")
    val updateTime: Long = System.currentTimeMillis()
)

/**
 * 应用分类枚举
 * 需求追溯: REQ-FWK-FUN-014
 */
enum class AppCategory(
    val code: String,
    val displayName: String,
    val iconRes: String
) {
    NAVIGATION("NAV", "导航", "ic_nav"),
    MUSIC("MUSIC", "音乐", "ic_music"),
    VIDEO("VIDEO", "视频", "ic_video"),
    PHONE("PHONE", "电话", "ic_phone"),
    VEHICLE("CAR", "车辆", "ic_car"),
    LIFE("LIFE", "生活", "ic_life"),
    GAME("GAME", "游戏", "ic_game"),
    TOOL("TOOL", "工具", "ic_tool");

    companion object {
        fun fromCode(code: String): AppCategory =
            entries.find { it.code == code } ?: TOOL

        fun fromOrdinal(ordinal: Int): AppCategory =
            entries.getOrElse(ordinal) { TOOL }
    }
}

/**
 * 应用分类信息
 */
data class AppCategoryInfo(
    val category: AppCategory,
    val appCount: Int = 0,
    val isSelected: Boolean = false
)
