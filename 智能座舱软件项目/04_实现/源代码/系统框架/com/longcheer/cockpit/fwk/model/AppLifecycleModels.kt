package com.longcheer.cockpit.fwk.model

import kotlinx.serialization.Serializable

/**
 * 应用生命周期状态
 * 需求追溯: REQ-DRV-FUN-009
 */
enum class AppLifecycleState {
    STOPPED,      // 已停止
    RUNNING,      // 运行中
    PAUSED,       // 已暂停（行驶限制）
    BACKGROUND,   // 后台运行
    RESTRICTED    // 受限状态
}

/**
 * 应用状态快照
 * 用于保存和恢复应用状态
 */
@Serializable
data class AppStateSnapshot(
    val appId: String,
    val timestamp: Long,
    val savedState: Map<String, String>? = null,
    val lifecycleState: AppLifecycleState = AppLifecycleState.STOPPED
)

/**
 * 行驶限制状态
 * 需求追溯: REQ-DRV-FUN-007
 */
data class DrivingRestrictionStatus(
    val isRestricted: Boolean,        // 是否受限
    val vehicleSpeed: Int,            // 车速 km/h
    val gearPosition: GearPosition,   // 挡位
    val isParkingBrakeOn: Boolean,    // 驻车制动状态
    val restrictionLevel: RestrictionLevel = RestrictionLevel.NONE
) {
    companion object {
        val NOT_RESTRICTED = DrivingRestrictionStatus(
            isRestricted = false,
            vehicleSpeed = 0,
            gearPosition = GearPosition.P,
            isParkingBrakeOn = true,
            restrictionLevel = RestrictionLevel.NONE
        )
    }
}

/**
 * 挡位枚举
 */
enum class GearPosition {
    P,    // 驻车挡
    R,    // 倒车挡
    N,    // 空挡
    D,    // 前进挡
    S,    // 运动挡
    L,    // 低速挡
    UNKNOWN
}

/**
 * 限制等级
 */
enum class RestrictionLevel {
    NONE,       // 无限制
    LIGHT,      // 轻度限制（仅警告）
    MODERATE,   // 中度限制（禁止视频）
    SEVERE      // 重度限制（仅允许白名单）
}

/**
 * 应用限制异常
 */
class AppRestrictedException : Exception("应用在当前行驶状态下被限制使用")

/**
 * 应用未找到异常
 */
class AppNotFoundException : Exception("应用未找到")

/**
 * 行驶限制监听器
 */
interface DrivingRestrictionListener {
    fun onRestrictionChanged(status: DrivingRestrictionStatus)
}
