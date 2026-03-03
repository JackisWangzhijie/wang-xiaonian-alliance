package com.longcheer.cockpit.nav.sdk.amap

import android.content.Context
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.*
import com.longcheer.cockpit.nav.model.LatLng
import com.longcheer.cockpit.nav.model.MapMarker
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 高德地图服务
 * 封装高德地图SDK核心功能
 * 
 * @author 龙旗智能导航团队
 * @version 1.0.0
 */
@Singleton
class AMapService @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "AMapService"
        private const val DEFAULT_ZOOM = 15f
        private const val MIN_ZOOM = 3f
        private const val MAX_ZOOM = 20f
    }
    
    private var aMap: AMap? = null
    private var mapView: MapView? = null
    private var locationMarker: Marker? = null
    
    // 标记集合
    private val markers = mutableMapOf<String, Marker>()
    
    /**
     * 初始化地图
     */
    fun initMap(mapView: MapView, options: AMapOptions = AMapOptions()) {
        this.mapView = mapView
        this.aMap = mapView.map
        
        aMap?.apply {
            mapType = options.mapType
            isTrafficEnabled = options.showTraffic
            uiSettings.apply {
                isCompassEnabled = options.compassEnabled
                isZoomControlsEnabled = options.zoomControlsEnabled
                isScaleControlsEnabled = options.scaleControlsEnabled
                isTiltGesturesEnabled = options.tiltGesturesEnabled
                isRotateGesturesEnabled = options.rotateGesturesEnabled
            }
            setMinZoomLevel(MIN_ZOOM)
            setMaxZoomLevel(MAX_ZOOM)
        }
    }
    
    /**
     * 设置地图类型
     */
    fun setMapType(type: Int) {
        aMap?.mapType = type
    }
    
    /**
     * 显示/隐藏实时路况
     */
    fun showTraffic(enable: Boolean) {
        aMap?.isTrafficEnabled = enable
    }
    
    /**
     * 设置3D模式
     */
    fun set3DMode(enable: Boolean) {
        aMap?.uiSettings?.isTiltGesturesEnabled = enable
        if (enable) {
            aMap?.animateCamera(CameraUpdateFactory.changeTilt(60f))
        } else {
            aMap?.animateCamera(CameraUpdateFactory.changeTilt(0f))
        }
    }
    
    /**
     * 显示3D建筑
     */
    fun showBuildings(enable: Boolean) {
        aMap?.showBuildings(enable)
    }
    
    /**
     * 添加标记
     * @param marker 标记数据
     * @return 标记ID
     */
    fun addMarker(marker: MapMarker): String {
        val options = MarkerOptions()
            .position(marker.position.toAMapLatLng())
            .title(marker.title)
            .snippet(marker.snippet)
            .draggable(marker.draggable)
        
        marker.icon?.let { options.icon(BitmapDescriptorFactory.fromResource(it)) }
        
        val amapMarker = aMap?.addMarker(options)
        amapMarker?.let {
            markers[marker.id] = it
        }
        return marker.id
    }
    
    /**
     * 移除标记
     */
    fun removeMarker(markerId: String) {
        markers.remove(markerId)?.remove()
    }
    
    /**
     * 清除所有标记
     */
    fun clearMarkers() {
        markers.values.forEach { it.remove() }
        markers.clear()
    }
    
    /**
     * 移动相机
     */
    fun moveCamera(update: com.amap.api.maps.CameraUpdate) {
        aMap?.moveCamera(update)
    }
    
    /**
     * 相机动画
     */
    fun animateCamera(update: com.amap.api.maps.CameraUpdate) {
        aMap?.animateCamera(update)
    }
    
    /**
     * 移动到指定位置
     */
    fun moveToLocation(latLng: LatLng, zoom: Float = DEFAULT_ZOOM, animate: Boolean = true) {
        val update = CameraUpdateFactory.newLatLngZoom(latLng.toAMapLatLng(), zoom)
        if (animate) {
            animateCamera(update)
        } else {
            moveCamera(update)
        }
    }
    
    /**
     * 更新位置标记
     */
    fun updateLocationMarker(latLng: LatLng, bearing: Float) {
        if (locationMarker == null) {
            val options = MarkerOptions()
                .position(latLng.toAMapLatLng())
                .anchor(0.5f, 0.5f)
                .flat(true)
            // .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_location_marker))
            locationMarker = aMap?.addMarker(options)
        } else {
            locationMarker?.position = latLng.toAMapLatLng()
            locationMarker?.rotateAngle = bearing
        }
    }
    
    /**
     * 设置地图点击监听
     */
    fun setOnMapClickListener(listener: (LatLng) -> Unit) {
        aMap?.setOnMapClickListener { latLng ->
            listener(LatLng(latLng.latitude, latLng.longitude))
        }
    }
    
    /**
     * 设置标记点击监听
     */
    fun setOnMarkerClickListener(listener: (String) -> Boolean) {
        aMap?.setOnMarkerClickListener { marker ->
            listener(marker.id)
        }
    }
    
    /**
     * 绘制路线
     */
    fun drawPolyline(points: List<LatLng>, color: Int, width: Float = 12f): Polyline? {
        if (points.size < 2) return null
        
        val options = PolylineOptions()
            .addAll(points.map { it.toAMapLatLng() })
            .color(color)
            .width(width)
            .geodesic(true)
        
        return aMap?.addPolyline(options)
    }
    
    /**
     * 绘制带路况的路线
     */
    fun drawTrafficPolyline(segments: List<TrafficSegment>): Polyline? {
        if (segments.isEmpty()) return null
        
        val options = PolylineOptions()
        segments.forEach { segment ->
            options.add(segment.start.toAMapLatLng())
            options.color(segment.color)
        }
        options.width(16f)
        
        return aMap?.addPolyline(options)
    }
    
    /**
     * 获取当前地图中心点
     */
    fun getCenterPoint(): LatLng? {
        val target = aMap?.cameraPosition?.target ?: return null
        return LatLng(target.latitude, target.longitude)
    }
    
    /**
     * 获取当前缩放级别
     */
    fun getZoomLevel(): Float {
        return aMap?.cameraPosition?.zoom ?: DEFAULT_ZOOM
    }
    
    /**
     * 释放资源
     */
    fun release() {
        clearMarkers()
        locationMarker = null
        mapView?.onDestroy()
        aMap = null
        mapView = null
    }
    
    // 生命周期转发
    fun onResume() {
        mapView?.onResume()
    }
    
    fun onPause() {
        mapView?.onPause()
    }
    
    fun onSaveInstanceState(outState: android.os.Bundle) {
        mapView?.onSaveInstanceState(outState)
    }
    
    // 扩展函数：坐标转换
    private fun LatLng.toAMapLatLng(): com.amap.api.maps.model.LatLng {
        return com.amap.api.maps.model.LatLng(latitude, longitude)
    }
}

/**
 * 高德地图选项
 */
data class AMapOptions(
    val mapType: Int = com.amap.api.maps.AMap.MAP_TYPE_NORMAL,
    val showTraffic: Boolean = true,
    val compassEnabled: Boolean = true,
    val zoomControlsEnabled: Boolean = false,
    val scaleControlsEnabled: Boolean = true,
    val tiltGesturesEnabled: Boolean = true,
    val rotateGesturesEnabled: Boolean = true
)

/**
 * 路况路段
 */
data class TrafficSegment(
    val start: LatLng,
    val end: LatLng,
    val color: Int,
    val status: Int
)