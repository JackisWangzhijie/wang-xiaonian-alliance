package com.longcheer.cockpit.nav.business.manager

import android.content.Context
import android.location.Location as AndroidLocation
import android.os.Looper
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import com.longcheer.cockpit.nav.model.Location
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 位置管理器
 * 负责定位服务、位置融合、GPS状态管理
 * 
 * @author 龙旗智能导航团队
 * @version 1.0.0
 * @since 1.0.0
 */
@Singleton
class LocationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "LocationManager"
        private const val LOCATION_INTERVAL = 1000L // 定位间隔(ms)
        private const val LOCATION_TIMEOUT = 10000L // 定位超时(ms)
    }
    
    private var locationClient: AMapLocationClient? = null
    private var lastLocation: Location? = null
    
    // 位置流
    private val _locationFlow = MutableSharedFlow<Location?>(replay = 1)
    val locationFlow: SharedFlow<Location?> = _locationFlow.asSharedFlow()
    
    // GPS状态
    private val _gpsStatus = MutableStateFlow<GpsStatus>(GpsStatus.DISABLED)
    val gpsStatus: StateFlow<GpsStatus> = _gpsStatus.asStateFlow()
    
    // 定位精度
    private val _locationAccuracy = MutableStateFlow<Float>(0f)
    val locationAccuracy: StateFlow<Float> = _locationAccuracy.asStateFlow()
    
    /**
     * 初始化定位
     */
    fun initLocation() {
        try {
            locationClient = AMapLocationClient(context.applicationContext).apply {
                setLocationOption(createLocationOption())
                setLocationListener(locationListener)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 开始定位
     */
    fun startLocation() {
        locationClient?.startLocation()
        _gpsStatus.value = GpsStatus.SEARCHING
    }
    
    /**
     * 停止定位
     */
    fun stopLocation() {
        locationClient?.stopLocation()
    }
    
    /**
     * 获取最后已知位置
     */
    fun getLastLocation(): Location? {
        return lastLocation
    }
    
    /**
     * 请求单次定位
     */
    suspend fun requestSingleLocation(timeout: Long = LOCATION_TIMEOUT): Result<Location> {
        return try {
            val location = locationFlow
                .filterNotNull()
                .timeout(timeout)
                .first()
            Result.success(location)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 检查定位权限
     */
    fun checkLocationPermission(): Boolean {
        // TODO: 检查定位权限
        return true
    }
    
    /**
     * 设置定位模式
     */
    fun setLocationMode(mode: LocationMode) {
        locationClient?.setLocationOption(
            createLocationOption(mode = when (mode) {
                LocationMode.HIGH_ACCURACY -> AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
                LocationMode.BATTERY_SAVING -> AMapLocationClientOption.AMapLocationMode.Battery_Saving
                LocationMode.DEVICE_ONLY -> AMapLocationClientOption.AMapLocationMode.Device_Sensors
            })
        )
    }
    
    /**
     * 模拟位置（用于测试）
     */
    fun setMockLocation(location: Location) {
        lastLocation = location
        _locationFlow.tryEmit(location)
    }
    
    private fun createLocationOption(
        mode: AMapLocationClientOption.AMapLocationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
    ): AMapLocationClientOption {
        return AMapLocationClientOption().apply {
            locationMode = mode
            interval = LOCATION_INTERVAL
            isNeedAddress = true
            isMockEnable = false
            isWifiScan = true
            isLocationCacheEnable = true
        }
    }
    
    private val locationListener = AMapLocationListener { location ->
        if (location != null && location.errorCode == AMapLocation.LOCATION_SUCCESS) {
            val loc = Location(
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = location.altitude,
                accuracy = location.accuracy,
                bearing = location.bearing,
                speed = location.speed,
                timestamp = location.time
            )
            
            lastLocation = loc
            _locationFlow.tryEmit(loc)
            _locationAccuracy.value = location.accuracy
            _gpsStatus.value = if (location.accuracy <= 10f) {
                GpsStatus.ACCURATE
            } else {
                GpsStatus.AVAILABLE
            }
        } else {
            _gpsStatus.value = GpsStatus.ERROR
        }
    }
    
    /**
     * 释放资源
     */
    fun release() {
        stopLocation()
        locationClient?.onDestroy()
        locationClient = null
    }
}

/**
 * GPS状态
 */
enum class GpsStatus {
    DISABLED,   // 未启用
    SEARCHING,  // 搜索中
    AVAILABLE,  // 可用
    ACCURATE,   // 高精度
    ERROR       // 错误
}

/**
 * 定位模式
 */
enum class LocationMode {
    HIGH_ACCURACY,  // 高精度
    BATTERY_SAVING, // 省电模式
    DEVICE_ONLY     // 仅设备
}