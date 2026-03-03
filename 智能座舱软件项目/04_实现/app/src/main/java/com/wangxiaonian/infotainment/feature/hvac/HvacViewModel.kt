package com.wangxiaonian.infotainment.feature.hvac

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wangxiaonian.infotainment.vehicle.VehiclePropertyHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * HVAC 空调控制 ViewModel
 *
 * @author 王小年联盟
 * @version 1.0
 */
@HiltViewModel
class HvacViewModel @Inject constructor(
    private val vehiclePropertyHelper: VehiclePropertyHelper
) : ViewModel() {

    private val _hvacState = MutableStateFlow(HvacState())
    val hvacState: StateFlow<HvacState> = _hvacState

    init {
        loadHvacState()
    }

    private fun loadHvacState() {
        viewModelScope.launch {
            // TODO: 从车辆属性读取当前 HVAC 状态
            _hvacState.value = HvacState(
                driverTemp = 22.0f,
                passengerTemp = 22.0f,
                fanSpeed = 3,
                isAcOn = true,
                isRecirculationOn = false,
                fanDirection = FanDirection.FACE
            )
        }
    }

    /**
     * 设置驾驶员侧温度
     */
    fun setDriverTemperature(temp: Float) {
        viewModelScope.launch {
            _hvacState.value = _hvacState.value.copy(driverTemp = temp)
            // TODO: 写入车辆属性 HVAC_TEMPERATURE_SET (驾驶员侧)
        }
    }

    /**
     * 设置副驾驶员侧温度
     */
    fun setPassengerTemperature(temp: Float) {
        viewModelScope.launch {
            _hvacState.value = _hvacState.value.copy(passengerTemp = temp)
            // TODO: 写入车辆属性 HVAC_TEMPERATURE_SET (副驾驶员侧)
        }
    }

    /**
     * 设置风扇速度
     */
    fun setFanSpeed(speed: Int) {
        viewModelScope.launch {
            val newSpeed = speed.coerceIn(0, 7)
            _hvacState.value = _hvacState.value.copy(fanSpeed = newSpeed)
            // TODO: 写入车辆属性 HVAC_FAN_SPEED
        }
    }

    /**
     * 开关 AC
     */
    fun toggleAc() {
        viewModelScope.launch {
            val newState = !_hvacState.value.isAcOn
            _hvacState.value = _hvacState.value.copy(isAcOn = newState)
            // TODO: 写入车辆属性 HVAC_AC_ON
        }
    }

    /**
     * 切换内/外循环
     */
    fun toggleRecirculation() {
        viewModelScope.launch {
            val newState = !_hvacState.value.isRecirculationOn
            _hvacState.value = _hvacState.value.copy(isRecirculationOn = newState)
            // TODO: 写入车辆属性 HVAC_RECIRCULATION_ON
        }
    }

    /**
     * 设置风向
     */
    fun setFanDirection(direction: FanDirection) {
        viewModelScope.launch {
            _hvacState.value = _hvacState.value.copy(fanDirection = direction)
            // TODO: 写入车辆属性 HVAC_FAN_DIRECTION
        }
    }

    /**
     * 开关座椅加热（驾驶员）
     */
    fun toggleDriverSeatHeat() {
        viewModelScope.launch {
            val newLevel = if (_hvacState.value.driverSeatHeatLevel < 3) {
                _hvacState.value.driverSeatHeatLevel + 1
            } else 0
            _hvacState.value = _hvacState.value.copy(driverSeatHeatLevel = newLevel)
        }
    }

    /**
     * 开关座椅加热（副驾驶员）
     */
    fun togglePassengerSeatHeat() {
        viewModelScope.launch {
            val newLevel = if (_hvacState.value.passengerSeatHeatLevel < 3) {
                _hvacState.value.passengerSeatHeatLevel + 1
            } else 0
            _hvacState.value = _hvacState.value.copy(passengerSeatHeatLevel = newLevel)
        }
    }
}

/**
 * HVAC 状态
 */
data class HvacState(
    val driverTemp: Float = 22.0f,
    val passengerTemp: Float = 22.0f,
    val fanSpeed: Int = 1,
    val isAcOn: Boolean = false,
    val isRecirculationOn: Boolean = false,
    val fanDirection: FanDirection = FanDirection.FACE,
    val driverSeatHeatLevel: Int = 0, // 0-3
    val passengerSeatHeatLevel: Int = 0, // 0-3
    val isAutoMode: Boolean = false
)

/**
 * 风向枚举
 */
enum class FanDirection {
    FACE,       // 面部
    FLOOR,      // 脚部
    DEFROST,    // 除霜
    FACE_FLOOR, // 面部+脚部
    FLOOR_DEFROST // 脚部+除霜
}
