package com.longcheer.cockpit.nav.business.manager

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import com.longcheer.cockpit.nav.business.engine.ArRenderEngine
import com.longcheer.cockpit.nav.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AR导航管理器
 * 负责AR实景导航的相机、渲染和虚实融合
 * 
 * @author 龙旗智能导航团队
 * @version 1.0.0
 * @since 1.0.0
 */
@Singleton
class ARManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationManager: LocationManager
) {
    companion object {
        private const val TAG = "ARManager"
        private const val AR_FOV = 60f // AR视场角
    }
    
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    
    private var arRenderEngine: ArRenderEngine? = null
    private var cameraSurface: Surface? = null
    
    // AR状态
    private val _arState = MutableStateFlow<ArState>(ArState.Idle)
    val arState: StateFlow<ArState> = _arState.asStateFlow()
    
    // AR导航信息
    private val _arNavInfo = MutableStateFlow<ArNavInfo?>(null)
    val arNavInfo: StateFlow<ArNavInfo?> = _arNavInfo.asStateFlow()
    
    // 相机帧数据
    private val _cameraFrame = MutableSharedFlow<CameraFrame>()
    val cameraFrame: SharedFlow<CameraFrame> = _cameraFrame.asSharedFlow()
    
    private var isSensorRegistered = false
    
    /**
     * 初始化AR引擎
     * @param surface 相机预览Surface
     * @param width 预览宽度
     * @param height 预览高度
     */
    fun initAR(surface: Surface, width: Int, height: Int) {
        cameraSurface = surface
        
        arRenderEngine = ArRenderEngine().apply {
            init(width, height)
            setCameraSurface(surface)
        }
        
        _arState.value = ArState.Ready
    }
    
    /**
     * 开始AR导航
     * @param route 导航路线
     */
    fun startARNavigation(route: Route) {
        if (_arState.value != ArState.Ready) return
        
        arRenderEngine?.startNavigation(route)
        startSensorFusion()
        
        _arState.value = ArState.Navigating
    }
    
    /**
     * 停止AR导航
     */
    fun stopARNavigation() {
        arRenderEngine?.stopNavigation()
        stopSensorFusion()
        _arState.value = ArState.Ready
    }
    
    /**
     * 设置AR指引样式
     */
    fun setARGuideStyle(style: ArGuideStyle) {
        arRenderEngine?.setGuideStyle(style)
    }
    
    /**
     * 渲染一帧AR画面
     * @param cameraFrame 相机帧数据
     */
    fun renderFrame(cameraFrame: CameraFrame) {
        arRenderEngine?.renderFrame(cameraFrame)
    }
    
    /**
     * 更新相机帧
     */
    fun updateCameraFrame(frame: CameraFrame) {
        _cameraFrame.tryEmit(frame)
    }
    
    /**
     * 获取AR指引位置（屏幕坐标）
     */
    fun worldToScreen(worldPos: Vector3): PointF? {
        return arRenderEngine?.worldToScreen(worldPos)
    }
    
    /**
     * 设置相机参数
     */
    fun setCameraParameters(fov: Float, aspectRatio: Float) {
        arRenderEngine?.setCameraParameters(fov, aspectRatio)
    }
    
    private fun startSensorFusion() {
        if (isSensorRegistered) return
        
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)?.let { sensor ->
            sensorManager.registerListener(
                sensorListener,
                sensor,
                SensorManager.SENSOR_DELAY_GAME
            )
            isSensorRegistered = true
        }
        
        // 同时监听位置变化
        coroutineScope.launch {
            locationManager.locationFlow.filterNotNull().collect { location ->
                updateLocationInAR(location)
            }
        }
    }
    
    private fun stopSensorFusion() {
        if (isSensorRegistered) {
            sensorManager.unregisterListener(sensorListener)
            isSensorRegistered = false
        }
    }
    
    private fun updateLocationInAR(location: Location) {
        arRenderEngine?.updateLocation(location)
    }
    
    private val sensorListener = object : SensorEventListener {
        private val rotationMatrix = FloatArray(9)
        private val orientation = FloatArray(3)
        
        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_ROTATION_VECTOR -> {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    
                    // 更新设备方向到AR引擎
                    arRenderEngine?.updateDeviceOrientation(rotationMatrix, orientation)
                }
            }
        }
        
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }
    
    /**
     * 检查AR可用性
     */
    fun checkARAvailability(): ARAvailability {
        val hasCamera = context.packageManager.hasSystemFeature(
            android.content.pm.PackageManager.FEATURE_CAMERA
        )
        val hasSensors = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null
        
        return when {
            !hasCamera -> ARAvailability.NO_CAMERA
            !hasSensors -> ARAvailability.NO_SENSORS
            else -> ARAvailability.AVAILABLE
        }
    }
    
    /**
     * 释放资源
     */
    fun release() {
        stopARNavigation()
        coroutineScope.cancel()
        arRenderEngine?.release()
        arRenderEngine = null
        cameraSurface = null
        _arState.value = ArState.Idle
    }
}

/**
 * 相机帧数据
 */
data class CameraFrame(
    val data: ByteArray? = null,
    val textureId: Int = 0,
    val width: Int = 0,
    val height: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val format: Int = android.graphics.ImageFormat.NV21
)

/**
 * AR可用性状态
 */
enum class ARAvailability {
    AVAILABLE,      // 可用
    NO_CAMERA,      // 无相机
    NO_SENSORS,     // 无传感器
    UNSUPPORTED     // 不支持
}