package com.longcheer.cockpit.drv.model

import android.os.Parcel
import android.os.Parcelable

/**
 * 限制状态数据类
 * ASIL等级: ASIL B
 */
data class RestrictionStatus(
    val state: RestrictionState = RestrictionState.NORMAL,
    val isRestricted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val triggerReason: String = "",
    val vehicleSpeed: Int = 0,
    val gearPosition: GearPosition = GearPosition.PARK,
    val parkingBrakeOn: Boolean = true
) : Parcelable {
    
    constructor(parcel: Parcel) : this(
        state = RestrictionState.values()[parcel.readInt()],
        isRestricted = parcel.readByte() != 0.toByte(),
        timestamp = parcel.readLong(),
        triggerReason = parcel.readString() ?: "",
        vehicleSpeed = parcel.readInt(),
        gearPosition = GearPosition.values()[parcel.readInt()],
        parkingBrakeOn = parcel.readByte() != 0.toByte()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(state.ordinal)
        parcel.writeByte(if (isRestricted) 1 else 0)
        parcel.writeLong(timestamp)
        parcel.writeString(triggerReason)
        parcel.writeInt(vehicleSpeed)
        parcel.writeInt(gearPosition.ordinal)
        parcel.writeByte(if (parkingBrakeOn) 1 else 0)
    }

    override fun describeContents(): Int = 0

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<RestrictionStatus> {
            override fun createFromParcel(parcel: Parcel): RestrictionStatus = RestrictionStatus(parcel)
            override fun newArray(size: Int): Array<RestrictionStatus?> = arrayOfNulls(size)
        }
    }
}

/**
 * 限制状态枚举
 */
enum class RestrictionState {
    NORMAL,      // 正常状态，无限制
    RESTRICTED,  // 限制状态，应用受限
    RECOVERING,  // 恢复中状态
    FAULT        // 故障状态（安全状态）
}

/**
 * 限制类型枚举
 */
enum class RestrictionType {
    NONE,            // 无限制
    VIDEO_BLOCKED,   // 视频禁止
    GAME_BLOCKED,    // 游戏禁止
    BROWSER_LIMITED, // 浏览器限制
    FULL_RESTRICTED  // 完全限制
}

/**
 * 行为控制枚举
 */
enum class BehaviorControl {
    PAUSE,              // 暂停
    RESUME,             // 恢复
    RETURN_HOME,        // 返回HOME
    LIMIT_INTERACTION,  // 限制交互
    GRAYSCALE           // 灰度显示
}

/**
 * 挡位位置枚举
 */
enum class GearPosition(val value: Int) {
    PARK(0),
    REVERSE(1),
    NEUTRAL(2),
    DRIVE(3),
    UNKNOWN(-1);

    companion object {
        fun fromInt(value: Int): GearPosition = values().find { it.value == value } ?: UNKNOWN
    }
}

/**
 * 信号类型枚举
 */
enum class SignalType {
    SPEED,
    GEAR,
    PARKING_BRAKE,
    COMBINED
}

/**
 * 应用类别枚举
 */
enum class AppCategory {
    NAVIGATION,
    MUSIC,
    VIDEO,
    GAME,
    BROWSER,
    COMMUNICATION,
    VEHICLE_CONTROL,
    SYSTEM,
    OTHER
}

/**
 * E2E状态枚举
 */
enum class E2EStatus {
    VALID,         // 校验通过
    CRC_ERROR,     // CRC错误
    COUNTER_ERROR, // 序列号错误
    TIMEOUT,       // 超时
    NO_DATA        // 无数据
}

/**
 * E2E校验结果
 */
data class E2EResult(
    val status: E2EStatus,
    val message: String
)

/**
 * E2E状态信息
 */
data class E2EStatusInfo(
    val signalId: String,
    val lastCounter: Byte,
    val lastTimestamp: Long,
    val isAlive: Boolean,
    val status: E2EStatus
)

/**
 * 车辆信号数据类
 */
data class VehicleSignal(
    val type: SignalType,
    val speed: Int = 0,
    val gear: GearPosition = GearPosition.PARK,
    val parkingBrake: Boolean = true,
    val e2eCounter: Byte = 0,
    val e2eCrc: Byte = 0,
    val timestamp: Long = 0
)

/**
 * 应用信息数据类
 */
data class AppInfo(
    val packageName: String,
    val appName: String,
    val category: AppCategory,
    val isRunning: Boolean = false
)

/**
 * 白名单条目数据类
 */
data class WhitelistEntry(
    val packageName: String,
    val appName: String,
    val category: String,
    val addedTime: Long,
    val reason: String,
    val signatureHash: String
) : Parcelable {
    
    constructor(parcel: Parcel) : this(
        packageName = parcel.readString() ?: "",
        appName = parcel.readString() ?: "",
        category = parcel.readString() ?: "",
        addedTime = parcel.readLong(),
        reason = parcel.readString() ?: "",
        signatureHash = parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(packageName)
        parcel.writeString(appName)
        parcel.writeString(category)
        parcel.writeLong(addedTime)
        parcel.writeString(reason)
        parcel.writeString(signatureHash)
    }

    override fun describeContents(): Int = 0

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<WhitelistEntry> {
            override fun createFromParcel(parcel: Parcel): WhitelistEntry = WhitelistEntry(parcel)
            override fun newArray(size: Int): Array<WhitelistEntry?> = arrayOfNulls(size)
        }
    }
}

/**
 * 安全事件数据类
 */
data class SafetyEvent(
    val eventId: String,
    val eventType: SafetyEventType,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val data: Map<String, String> = emptyMap()
)

/**
 * 安全事件类型
 */
enum class SafetyEventType {
    E2E_CRC_ERROR,
    E2E_COUNTER_ERROR,
    E2E_TIMEOUT,
    WATCHDOG_TIMEOUT,
    REDUNDANCY_MISMATCH,
    STATE_TRANSITION,
    RESTRICTION_TRIGGERED,
    RESTRICTION_RECOVERED,
    SAFE_STATE_ENTERED,
    FAULT_DETECTED
}
