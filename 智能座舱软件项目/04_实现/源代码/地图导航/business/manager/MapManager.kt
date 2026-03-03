package com.longcheer.cockpit.nav.business.manager

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.CameraPosition
import com.longcheer.cockpit.nav.data.repository.MapDataRepository
import com.longcheer.cockpit.nav.model.*
import com.longcheer.cockpit.nav.sdk.amap.AMapOptions
import com.longcheer.cockpit.nav.sdk.amap.AMapService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 地图管理器
 * 负责地图显示、图层管理、手势交互等核心功能
 * 
 * @author 龙旗智能导航团队
 * @version 1.0.0
 * @since 1.0.0
 */
@Singleton
class MapManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val aMapService: AMapService,
    private val locationManager: LocationManager,
    private val trafficManager: TrafficManager,
    private val mapDataRepo: MapDataRepository
) {
    companion object {
        private const val TAG = "MapManager"
        private const val DEFAULT_ZOOM = 15f
        private const val MIN_ZOOM = 3f
        private const val MAX_ZOOM = 20f
        private const val TILE_CACHE_SIZE = 100 * 1024 * 1024L // 100MB
    }
    
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // 地图状态
    private val _mapState = MutableStateFlow<MapState>(MapState.Idle)
    val mapState: StateFlow<MapState> = _mapState.asStateFlow()
    
    // 当前位置
    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()
    
    // 地图配置
    private val _mapConfig = MutableStateFlow(MapConfig())
    val mapConfig: StateFlow<MapConfig> = _mapConfig.asStateFlow()
    
    // 当前地图中心点
    private val _mapCenter = MutableStateFlow<LatLng?>(null)
    val mapCenter: StateFlow<LatLng?> = _mapCenter.asStateFlow()
    
    // 缩放级别
    private val _zoomLevel = MutableStateFlow(DEFAULT_ZOOM)
    val zoomLevel: StateFlow<Float> = _zoomLevel.asStateFlow()
    
    // 标记集合
    private val _markers = MutableStateFlow<List<MapMarker>>(emptyList())
    val markers: StateFlow<List<MapMarker>> = _markers.asStateFlow()
    
    /**
     * 初始化地图
     * @param mapView 地图视图
     * @param config 地图配置
     */
    fun initMap(mapView: MapView, config: MapConfig = MapConfig()) {
        _mapConfig.value = config
        
        val options = AMapOptions(
            mapType = config.mapType.toAMapType(),
            showTraffic = config.showTraffic,
            compassEnabled = config.compassEnabled,
            zoomControlsEnabled = config.zoomControlsEnabled,
            scaleControlsEnabled = config.scaleControlsEnabled,
            tiltGesturesEnabled = config.is3DMode,
            rotateGesturesEnabled = true
        )
        
        aMapService.initMap(mapView, options)
        setupMapListeners()
        setupLocationTracking()
        
        _mapState.value = MapState.Idle
    }
    
    /**
     * 设置地图类型
     */
    fun setMapType(type: MapType) {
        aMapService.setMapType(type.toAMapType())
        _mapConfig.update { it.copy(mapType = type) }
    }
    
    /**
     * 显示实时路况
     */
    fun showTraffic(enable: Boolean) {
        aMapService.showTraffic(enable)
        if (enable) {
            trafficManager.startTrafficUpdate()
        } else {
            trafficManager.stopTrafficUpdate()
        }
        _mapConfig.update { it.copy(showTraffic = enable) }
    }
    
    /**
     * 切换3D模式
     */
    fun set3DMode(enable: Boolean) {
        aMapService.set3DMode(enable)
        _mapConfig.update { it.copy(is3DMode = enable) }
    }
    
    /**
     * 显示3D建筑
     */
    fun showBuildings(enable: Boolean) {
        aMapService.showBuildings(enable)
        _mapConfig.update { it.copy(showBuildings = enable) }
    }
    
    /**
     * 移动到当前位置
     */
    fun moveToCurrentLocation(animate: Boolean = true) {
        val location = _currentLocation.value ?: return
        val latLng = LatLng(location.latitude, location.longitude)
        
        if (animate) {
            aMapService.animateCamera(
                CameraUpdateFactory.newLatLngZoom(latLng.toAMapLatLng(), DEFAULT_ZOOM)
            )
        } else {
            aMapService.moveCamera(
                CameraUpdateFactory.newLatLngZoom(latLng.toAMapLatLng(), DEFAULT_ZOOM)
            )
        }
    }
    
    /**
     * 移动地图到指定位置
     */
    fun moveToLocation(latLng: LatLng, zoom: Float = DEFAULT_ZOOM, animate: Boolean = true) {
        aMapService.moveToLocation(latLng, zoom, animate)
    }
    
    /**
     * 搜索POI
     */
    fun searchPoi(
        keyword: String,
        location: LatLng? = null,
        radius: Int = 5000
    ): Flow<Result<List<Poi>>> = flow {
        _mapState.value = MapState.Searching
        try {
            val results = mapDataRepo.searchPoi(keyword, location, radius)
            emit(Result.success(results))
            _mapState.value = MapState.Idle
        } catch (e: Exception) {
            emit(Result.failure(e))
            _mapState.value = MapState.Error(e.message)
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * 添加标记
     */
    fun addMarker(marker: MapMarker): String {
        aMapService.addMarker(marker)
        _markers.update { it + marker }
        return marker.id
    }
    
    /**
     * 移除标记
     */
    fun removeMarker(markerId: String) {
        aMapService.removeMarker(markerId)
        _markers.update { list -> list.filter { it.id != markerId } }
    }
    
    /**
     * 清除所有标记
     */
    fun clearMarkers() {
        aMapService.clearMarkers()
        _markers.value = emptyList()
    }
    
    /**
     * 放大地图
     */
    fun zoomIn() {
        aMapService.animateCamera(CameraUpdateFactory.zoomIn())
    }
    
    /**
     * 缩小地图
     */
    fun zoomOut() {
        aMapService.animateCamera(CameraUpdateFactory.zoomOut())
    }
    
    /**
     * 设置地图倾斜角度
     */
    fun setTilt(tilt: Float) {
        aMapService.animateCamera(CameraUpdateFactory.changeTilt(tilt))
    }
    
    /**
     * 设置地图旋转角度
     */
    fun setBearing(bearing: Float) {
        aMapService.animateCamera(CameraUpdateFactory.changeBearing(bearing))
    }
    
    /**
     * 绘制路线
     */
    fun drawRoute(points: List<LatLng>, color: Int = 0xFF2196F3.toInt()) {
        aMapService.drawPolyline(points, color)
    }
    
    /**
     * 获取当前地图可视区域
     */
    fun getVisibleRegion(): Pair<LatLng, LatLng>? {
        // TODO: 实现获取可视区域
        return null
    }
    
    private fun setupMapListeners() {
        aMapService.setOnMapClickListener { latLng ->
            // 处理地图点击
        }
        
        aMapService.setOnMarkerClickListener { markerId ->
            // 处理标记点击
            true
        }
    }
    
    private fun setupLocationTracking() {
        locationManager.locationFlow
            .filterNotNull()
            .onEach { location ->
                _currentLocation.value = location
                updateLocationMarker(location)
            }
            .launchIn(coroutineScope)
    }
    
    private fun updateLocationMarker(location: Location) {
        aMapService.updateLocationMarker(
            LatLng(location.latitude, location.longitude),
            location.bearing
        )
    }
    
    // 生命周期转发
    fun onResume() {
        aMapService.onResume()
    }
    
    fun onPause() {
        aMapService.onPause()
    }
    
    fun onSaveInstanceState(outState: android.os.Bundle) {
        aMapService.onSaveInstanceState(outState)
    }
    
    /**
     * 释放资源
     */
    fun release() {
        coroutineScope.cancel()
        trafficManager.stopTrafficUpdate()
        aMapService.release()
    }
    
    // 扩展函数
    private fun MapType.toAMapType(): Int {
        return when (this) {
            MapType.NORMAL -> com.amap.api.maps.AMap.MAP_TYPE_NORMAL
            MapType.SATELLITE -> com.amap.api.maps.AMap.MAP_TYPE_SATELLITE
            MapType.NIGHT -> com.amap.api.maps.AMap.MAP_TYPE_NIGHT
            MapType.NAVI -> com.amap.api.maps.AMap.MAP_TYPE_NAVI
        }
    }
    
    private fun LatLng.toAMapLatLng(): com.amap.api.maps.model.LatLng {
        return com.amap.api.maps.model.LatLng(latitude, longitude)
    }
}