package com.longcheer.cockpit.fwk.service

import com.longcheer.cockpit.fwk.model.DrivingRestrictionListener
import com.longcheer.cockpit.fwk.model.DrivingRestrictionStatus
import com.longcheer.cockpit.fwk.model.GearPosition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 车辆服务接口
 * 用于获取行驶限制状态和车辆信息
 * 需求追溯: REQ-DRV-FUN-007
 */
interface IVehicleService {
    fun getDrivingRestrictionStatus(): DrivingRestrictionStatus
    fun observeDrivingRestrictionStatus(): Flow<DrivingRestrictionStatus>
    fun registerDrivingRestrictionListener(listener: DrivingRestrictionListener)
    fun unregisterDrivingRestrictionListener(listener: DrivingRestrictionListener)

    // 车辆基本信息
    fun getVehicleSpeed(): Int
    fun getGearPosition(): GearPosition
    fun isParkingBrakeOn(): Boolean
}

/**
 * 车辆服务模拟实现
 * 实际项目中应通过AIDL或HIDL与车辆服务通信
 */
@Singleton
class VehicleServiceImpl @Inject constructor() : IVehicleService {

    private val _restrictionStatus = MutableStateFlow(DrivingRestrictionStatus.NOT_RESTRICTED)
    private val restrictionListeners = mutableListOf<DrivingRestrictionListener>()

    override fun getDrivingRestrictionStatus(): DrivingRestrictionStatus {
        return _restrictionStatus.value
    }

    override fun observeDrivingRestrictionStatus(): Flow<DrivingRestrictionStatus> {
        return _restrictionStatus.asStateFlow()
    }

    override fun registerDrivingRestrictionListener(listener: DrivingRestrictionListener) {
        if (!restrictionListeners.contains(listener)) {
            restrictionListeners.add(listener)
        }
    }

    override fun unregisterDrivingRestrictionListener(listener: DrivingRestrictionListener) {
        restrictionListeners.remove(listener)
    }

    override fun getVehicleSpeed(): Int {
        return _restrictionStatus.value.vehicleSpeed
    }

    override fun getGearPosition(): GearPosition {
        return _restrictionStatus.value.gearPosition
    }

    override fun isParkingBrakeOn(): Boolean {
        return _restrictionStatus.value.isParkingBrakeOn
    }

    /**
     * 更新行驶限制状态（用于模拟和测试）
     */
    fun updateRestrictionStatus(status: DrivingRestrictionStatus) {
        _restrictionStatus.value = status
        restrictionListeners.forEach { it.onRestrictionChanged(status) }
    }

    /**
     * 模拟车速变化
     */
    fun simulateSpeedChange(speed: Int) {
        val currentStatus = _restrictionStatus.value
        val newStatus = currentStatus.copy(
            vehicleSpeed = speed,
            isRestricted = speed > 0 && currentStatus.gearPosition != GearPosition.P
        )
        updateRestrictionStatus(newStatus)
    }

    /**
     * 模拟挡位变化
     */
    fun simulateGearChange(gear: GearPosition) {
        val currentStatus = _restrictionStatus.value
        val newStatus = currentStatus.copy(
            gearPosition = gear,
            isRestricted = currentStatus.vehicleSpeed > 0 && gear != GearPosition.P
        )
        updateRestrictionStatus(newStatus)
    }
}
