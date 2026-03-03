package com.longcheer.cockpit.nav.business.manager

import android.content.Context
import com.longcheer.cockpit.nav.business.service.GuideService
import com.longcheer.cockpit.nav.business.service.RouteService
import com.longcheer.cockpit.nav.data.repository.NavDataRepository
import com.longcheer.cockpit.nav.model.*
import com.longcheer.cockpit.nav.sdk.amap.AMapNavAdapter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 导航管理器
 * 负责路线规划、导航引导、语音播报等导航核心功能
 * 
 * @author 龙旗智能导航团队
 * @version 1.0.0
 * @since 1.0.0
 */
@Singleton
class NavigationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val aMapNavAdapter: AMapNavAdapter,
    private val routeService: RouteService,
    private val guideService: GuideService,
    private val navDataRepo: NavDataRepository,
    private val locationManager: LocationManager
) {
    companion object {
        private const val TAG = "NavigationManager"
        private const val ROUTE_PLAN_TIMEOUT = 5000L // 5秒
        private const val REPLAN_THRESHOLD = 50 // 偏离路线50米重新规划
    }
    
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // 导航状态
    private val _navState = MutableStateFlow<NavState>(NavState.Idle)
    val navState: StateFlow<NavState> = _navState.asStateFlow()
    
    // 当前路线
    private val _currentRoute = MutableStateFlow<Route?>(null)
    val currentRoute: StateFlow<Route?> = _currentRoute.asStateFlow()
    
    // 备选路线列表
    private val _alternativeRoutes = MutableStateFlow<List<Route>>(emptyList())
    val alternativeRoutes: StateFlow<List<Route>> = _alternativeRoutes.asStateFlow()
    
    // 导航信息
    private val _navInfo = MutableStateFlow<NavInfo?>(null)
    val navInfo: StateFlow<NavInfo?> = _navInfo.asStateFlow()
    
    // 车道信息
    private val _laneInfo = MutableStateFlow<LaneInfo?>(null)
    val laneInfo: StateFlow<LaneInfo?> = _laneInfo.asStateFlow()
    
    // 导航偏好
    private val _navPreference = MutableStateFlow(NavPreference())
    val navPreference: StateFlow<NavPreference> = _navPreference.asStateFlow()
    
    private var currentHistoryId: Long? = null
    
    /**
     * 初始化导航
     */
    fun initNavigation() {
        aMapNavAdapter.init()
        setupNavListeners()
    }
    
    /**
     * 规划路线
     * @param start 起点
     * @param end 终点
     * @param waypoints 途经点
     * @param strategy 路线策略
     * @return 规划结果
     */
    suspend fun planRoute(
        start: NavPoint,
        end: NavPoint,
        waypoints: List<NavPoint> = emptyList(),
        strategy: RouteStrategy = RouteStrategy.FAST
    ): Result<RoutePlanResult> = withContext(Dispatchers.IO) {
        try {
            _navState.value = NavState.Planning
            
            val result = routeService.planRoute(
                start = start,
                end = end,
                waypoints = waypoints,
                strategy = strategy,
                alternatives = true
            )
            
            result.fold(
                onSuccess = { planResult ->
                    _currentRoute.value = planResult.recommendedRoute
                    _alternativeRoutes.value = planResult.alternativeRoutes
                    _navState.value = NavState.Planned
                    Result.success(planResult)
                },
                onFailure = { e ->
                    _navState.value = NavState.Error(e.message)
                    Result.failure(e)
                }
            )
        } catch (e: Exception) {
            _navState.value = NavState.Error(e.message)
            Result.failure(e)
        }
    }
    
    /**
     * 快速规划路线（从当前位置）
     */
    suspend fun quickPlanRoute(
        destination: NavPoint,
        strategy: RouteStrategy = RouteStrategy.FAST
    ): Result<RoutePlanResult> {
        val currentLoc = locationManager.getLastLocation()
            ?: return Result.failure(Exception("无法获取当前位置"))
        
        val startPoint = NavPoint(
            latitude = currentLoc.latitude,
            longitude = currentLoc.longitude,
            name = "当前位置"
        )
        
        return planRoute(startPoint, destination, emptyList(), strategy)
    }
    
    /**
     * 开始导航
     */
    fun startNavigation(
        route: Route = _currentRoute.value!!,
        mode: NavMode = NavMode.NORMAL
    ): Boolean {
        if (_navState.value != NavState.Planned && 
            _navState.value != NavState.Paused) {
            return false
        }
        
        // TODO: 转换为高德路线并启动导航
        // val success = aMapNavAdapter.startNavigation(route.toAMapRoute(), mode)
        val success = true // 模拟成功
        
        if (success) {
            _navState.value = NavState.Navigating
            guideService.startGuidance()
            
            // 保存导航历史
            coroutineScope.launch(Dispatchers.IO) {
                currentHistoryId = navDataRepo.saveNavHistory(route)
            }
        }
        
        return success
    }
    
    /**
     * 停止导航
     */
    fun stopNavigation(reason: StopReason = StopReason.USER) {
        aMapNavAdapter.stopNavigation()
        guideService.stopGuidance()
        
        // 标记导航完成
        if (reason == StopReason.ARRIVED) {
            coroutineScope.launch(Dispatchers.IO) {
                currentHistoryId?.let { navDataRepo.completeNavigation(it) }
            }
        }
        
        _navState.value = NavState.Idle
        _navInfo.value = null
        _laneInfo.value = null
        currentHistoryId = null
    }
    
    /**
     * 暂停导航
     */
    fun pauseNavigation() {
        if (_navState.value == NavState.Navigating) {
            aMapNavAdapter.pauseNavigation()
            guideService.pauseGuidance()
            _navState.value = NavState.Paused
        }
    }
    
    /**
     * 恢复导航
     */
    fun resumeNavigation() {
        if (_navState.value == NavState.Paused) {
            aMapNavAdapter.resumeNavigation()
            guideService.resumeGuidance()
            _navState.value = NavState.Navigating
        }
    }
    
    /**
     * 切换路线
     */
    fun switchRoute(routeIndex: Int) {
        val routes = _alternativeRoutes.value
        if (routeIndex >= 0 && routeIndex < routes.size) {
            val newRoute = routes[routeIndex]
            _currentRoute.value = newRoute
            aMapNavAdapter.switchRoute(routeIndex)
        }
    }
    
    /**
     * 设置导航偏好
     */
    fun setNavPreference(preference: NavPreference) {
        _navPreference.value = preference
        routeService.setPreference(preference)
        aMapNavAdapter.setNavPreference(preference)
    }
    
    /**
     * 重新规划路线
     */
    fun replanRoute() {
        val currentRoute = _currentRoute.value ?: return
        val currentLoc = _navInfo.value?.currentLocation ?: return
        
        coroutineScope.launch(Dispatchers.IO) {
            planRoute(
                start = NavPoint(currentLoc.latitude, currentLoc.longitude),
                end = currentRoute.endPoint,
                waypoints = currentRoute.waypoints,
                strategy = currentRoute.strategy
            )
        }
    }
    
    /**
     * 添加途经点
     */
    fun addWaypoint(waypoint: NavPoint) {
        val currentRoute = _currentRoute.value ?: return
        val newWaypoints = currentRoute.waypoints + waypoint
        
        coroutineScope.launch(Dispatchers.IO) {
            planRoute(
                start = currentRoute.startPoint,
                end = currentRoute.endPoint,
                waypoints = newWaypoints,
                strategy = currentRoute.strategy
            )
        }
    }
    
    /**
     * 移除途经点
     */
    fun removeWaypoint(index: Int) {
        val currentRoute = _currentRoute.value ?: return
        if (index < 0 || index >= currentRoute.waypoints.size) return
        
        val newWaypoints = currentRoute.waypoints.filterIndexed { i, _ -> i != index }
        
        coroutineScope.launch(Dispatchers.IO) {
            planRoute(
                start = currentRoute.startPoint,
                end = currentRoute.endPoint,
                waypoints = newWaypoints,
                strategy = currentRoute.strategy
            )
        }
    }
    
    private fun setupNavListeners() {
        // 导航信息回调
        aMapNavAdapter.setOnNavInfoCallback { info ->
            // TODO: 转换为NavInfo
            // _navInfo.value = info.toNavInfo()
        }
        
        // 车道信息回调
        aMapNavAdapter.setOnLaneInfoCallback { laneInfo ->
            // TODO: 转换为LaneInfo
            // _laneInfo.value = laneInfo.toLaneInfo()
        }
        
        // 偏航回调
        aMapNavAdapter.setOnOffRouteCallback { distance ->
            if (distance > REPLAN_THRESHOLD) {
                autoReplanRoute()
            }
        }
        
        // 到达目的地回调
        aMapNavAdapter.setOnArrivedCallback {
            stopNavigation(StopReason.ARRIVED)
        }
    }
    
    private fun autoReplanRoute() {
        if (_navState.value != NavState.Navigating) return
        
        val currentLoc = _navInfo.value?.currentLocation ?: return
        val end = _currentRoute.value?.endPoint ?: return
        
        coroutineScope.launch(Dispatchers.IO) {
            planRoute(
                start = NavPoint(currentLoc.latitude, currentLoc.longitude),
                end = end,
                strategy = RouteStrategy.FAST
            )
        }
    }
    
    /**
     * 释放资源
     */
    fun release() {
        stopNavigation()
        coroutineScope.cancel()
        aMapNavAdapter.release()
        guideService.release()
    }
}