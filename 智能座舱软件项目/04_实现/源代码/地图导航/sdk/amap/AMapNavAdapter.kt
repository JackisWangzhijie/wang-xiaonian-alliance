package com.longcheer.cockpit.nav.sdk.amap

import android.content.Context
import com.amap.api.navi.AMapNavi
import com.amap.api.navi.AMapNaviListener
import com.amap.api.navi.enums.NaviType
import com.amap.api.navi.model.*
import com.longcheer.cockpit.nav.model.NavMode
import com.longcheer.cockpit.nav.model.NavPreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 高德导航适配器
 * 封装高德导航SDK功能
 * 
 * @author 龙旗智能导航团队
 * @version 1.0.0
 */
@Singleton
class AMapNavAdapter @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "AMapNavAdapter"
    }
    
    private var naviInstance: AMapNavi? = null
    private var naviListener: AMapNaviListener? = null
    
    // 回调
    private var onNavInfoCallback: ((NaviInfo) -> Unit)? = null
    private var onLaneInfoCallback: ((AMapNaviLaneInfo) -> Unit)? = null
    private var onOffRouteCallback: ((Int) -> Unit)? = null
    private var onArrivedCallback: (() -> Unit)? = null
    private var onRouteCalculatedCallback: ((IntArray?) -> Unit)? = null
    
    /**
     * 初始化导航
     */
    fun init() {
        naviInstance = AMapNavi.getInstance(context.applicationContext)
        setupNaviListener()
    }
    
    /**
     * 开始导航
     */
    fun startNavigation(route: AMapNaviPath, mode: NavMode): Boolean {
        val navi = naviInstance ?: return false
        
        return when (mode) {
            NavMode.NORMAL, NavMode.AR -> {
                navi.selectRouteId(route.pathId)
                navi.startNavi(NaviType.GPS)
            }
            NavMode.SIMULATE -> {
                navi.selectRouteId(route.pathId)
                navi.startNavi(NaviType.EMULATOR)
            }
            else -> false
        }
    }
    
    /**
     * 停止导航
     */
    fun stopNavigation() {
        naviInstance?.stopNavi()
    }
    
    /**
     * 暂停导航
     */
    fun pauseNavigation() {
        naviInstance?.pauseNavi()
    }
    
    /**
     * 恢复导航
     */
    fun resumeNavigation() {
        naviInstance?.resumeNavi()
    }
    
    /**
     * 切换路线
     */
    fun switchRoute(routeId: Int) {
        naviInstance?.selectRouteId(routeId)
    }
    
    /**
     * 设置导航偏好
     */
    fun setNavPreference(preference: NavPreference) {
        naviInstance?.apply {
            if (preference.avoidCongestion) setAvoidCongestionEnabled(true)
            if (preference.avoidHighway) setAvoidHighwayEnabled(true)
            if (preference.avoidToll) setAvoidCostEnabled(true)
            if (preference.priorityHighway) setPreferHighwayEnabled(true)
        }
    }
    
    /**
     * 规划路线
     */
    fun calculateRoute(
        start: com.amap.api.navi.model.NaviLatLng,
        end: com.amap.api.navi.model.NaviLatLng,
        waypoints: List<com.amap.api.navi.model.NaviLatLng> = emptyList()
    ): Boolean {
        return naviInstance?.calculateDriveRoute(
            waypoints.toMutableList(),
            end,
            waypoints.toMutableList(),
            AMapNavi.DrivingDefault
        ) ?: false
    }
    
    /**
     * 设置导航信息回调
     */
    fun setOnNavInfoCallback(callback: (NaviInfo) -> Unit) {
        onNavInfoCallback = callback
    }
    
    /**
     * 设置车道信息回调
     */
    fun setOnLaneInfoCallback(callback: (AMapNaviLaneInfo) -> Unit) {
        onLaneInfoCallback = callback
    }
    
    /**
     * 设置偏航回调
     */
    fun setOnOffRouteCallback(callback: (Int) -> Unit) {
        onOffRouteCallback = callback
    }
    
    /**
     * 设置到达回调
     */
    fun setOnArrivedCallback(callback: () -> Unit) {
        onArrivedCallback = callback
    }
    
    /**
     * 设置路线规划完成回调
     */
    fun setOnRouteCalculatedCallback(callback: (IntArray?) -> Unit) {
        onRouteCalculatedCallback = callback
    }
    
    /**
     * 获取导航路径
     */
    fun getNaviPaths(): Map<Int, AMapNaviPath>? {
        return naviInstance?.naviPaths
    }
    
    /**
     * 获取当前导航信息
     */
    fun getNaviInfo(): NaviInfo? {
        return naviInstance?.naviInfo
    }
    
    private fun setupNaviListener() {
        naviListener = object : AMapNaviListener {
            override fun onInitNaviFailure() {}
            
            override fun onInitNaviSuccess() {}
            
            override fun onStartNavi(type: Int) {}
            
            override fun onTrafficStatusUpdate() {}
            
            override fun onLocationChange(location: AMapNaviLocation?) {}
            
            override fun onGetNavigationText(type: Int, text: String?) {
                text?.let { 
                    // TTS播报
                }
            }
            
            override fun onGetNavigationText(s: String?) {}
            
            override fun onEndEmulatorNavi() {}
            
            override fun onArriveDestination() {
                onArrivedCallback?.invoke()
            }
            
            override fun onCalculateRouteSuccess(response: AMapCalcRouteResult?) {
                onRouteCalculatedCallback?.invoke(response?.routeid)
            }
            
            override fun onCalculateRouteFailure(response: AMapCalcRouteResult?) {}
            
            override fun onReCalculateRouteForYaw() {
                onOffRouteCallback?.invoke(100) // 偏航距离
            }
            
            override fun onReCalculateRouteForTrafficJam() {}
            
            override fun onArrivedWayPoint(wayID: Int) {}
            
            override fun onGpsOpenStatus(enabled: Boolean) {}
            
            override fun updateNaviInfo(info: NaviInfo?) {
                info?.let { onNavInfoCallback?.invoke(it) }
            }
            
            override fun updateAimlessModeStatistics(aimlessModeStat: AimLessModeStat?) {}
            
            override fun updateAimlessModeCongestionInfo(aimlessModeCongestionResult: AimLessModeCongestionResult?) {}
            
            override fun onPlayRing(type: Int) {}
            
            override fun onNaviInfoUpdate(naviInfo: NaviInfo?) {}
            
            override fun onNaviInfoUpdated(naviInfo: NaviInfo?) {}
            
            override fun updateCameraInfo(list: MutableList<AMapNaviCameraInfo>?) {}
            
            override fun onServiceAreaUpdate(list: MutableList<AMapServiceAreaInfo>?) {}
            
            override fun showCross(showCross: AMapNaviCross?) {
                // 显示路口放大图
            }
            
            override fun hideCross() {}
            
            override fun showModeCross(showCross: AMapNaviGuide?) {}
            
            override fun hideModeCross() {}
            
            override fun showLaneInfo(
                laneInfo: Array<AMapLaneInfo>?,
                backupLane: ByteArray?,
                suggestedLane: ByteArray?
            ) {
                laneInfo?.let { onLaneInfoCallback?.invoke(AMapNaviLaneInfo()) }
            }
            
            override fun hideLaneInfo() {}
            
            override fun onCalculateRouteSuccess(routeIds: IntArray?) {
                onRouteCalculatedCallback?.invoke(routeIds)
            }
            
            override fun notifyParallelRoad(parellelRoadType: Int) {}
            
            override fun OnUpdateTrafficFacility(list: MutableList<AMapNaviTrafficFacilityInfo>?) {}
            
            override fun OnUpdateTrafficFacility(trafficFacilityInfo: AMapNaviTrafficFacilityInfo?) {}
            
            override fun OnUpdateTrafficFacility(p0: AMapNaviTrafficFacilityInfo?) {}
            
            override fun updateAimlessModeStatistics(p0: Int, p1: Int, p2: Int, p3: Int, p4: Int) {}
            
            override fun updateAimlessModeCongestionInfo(p0: AimLessModeCongestionResult?) {}
            
            override fun onGetNavigationText(p0: Int, p1: String?) {}
            
            override fun onGetNavigationText(p0: String?) {}
        }
        
        naviInstance?.addAMapNaviListener(naviListener)
    }
    
    fun release() {
        naviInstance?.removeAMapNaviListener(naviListener)
        naviInstance?.destroy()
        naviInstance = null
    }
}