# 地图导航模块详细设计文档
## Detailed Design Document - Map Navigation Module

**项目名称**: 2024年智能座舱软件主交互开发  
**文档版本**: V1.0  
**编制日期**: 2024-06-20  
**编制单位**: 上海龙旗智能科技有限公司  
**客户单位**: 奇瑞汽车股份有限公司  
**模块名称**: 地图导航模块 (NAV)  
**符合标准**: ASPICE 3.1, ISO 26262, ISO/SAE 21434

---

## 文档控制信息

### 版本历史
| 版本 | 日期 | 作者 | 变更描述 | 审批 |
|------|------|------|----------|------|
| V0.1 | 2024-06-18 | 导航模块工程师 | 初稿编制 | - |
| V0.5 | 2024-06-19 | 架构师 | 架构评审后修订 | 项目经理 |
| V1.0 | 2024-06-20 | 导航模块工程师 | 基线版本 | 项目总监 |

### 参考文档
1. 《HLD_概要设计文档_V1.0.md》
2. 《SRS_智能座舱主交互系统_V1.0.md》
3. 《数据库设计文档_V1.0.md》
4. 《高德地图车机版SDK开发文档_V8.0》

---

## 目录

1. [引言](#1-引言)
2. [模块架构设计](#2-模块架构设计)
3. [核心类设计](#3-核心类设计)
4. [时序图设计](#4-时序图设计)
5. [高德SDK集成设计](#5-高德sdk集成设计)
6. [3D渲染架构](#6-3d渲染架构)
7. [数据库设计](#7-数据库设计)
8. [接口设计](#8-接口设计)
9. [安全设计](#9-安全设计)
10. [需求追溯矩阵](#10-需求追溯矩阵)

---

## 1. 引言

### 1.1 目的
本文档定义地图导航模块的详细设计，基于HLD概要设计和SRS需求规格，指导开发团队实现地图导航功能。

### 1.2 范围
覆盖以下核心功能：
- 实时路况显示
- 多路线规划
- 车道级导航
- AR实景导航
- 离线地图
- 3D地图交互

### 1.3 设计约束
- 平台：Android Automotive 12 (API 31)
- 地图引擎：高德车机版SDK V8.0+
- 渲染引擎：OpenGL ES 3.2 / Vulkan 1.1
- ASIL等级：QM（导航显示）/ ASIL A（关键提示）

---

## 2. 模块架构设计

### 2.1 模块整体架构

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         地图导航模块 (NAV)                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                         应用层 (App Layer)                           │   │
│  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐                │   │
│  │  │  地图主界面   │ │   AR导航页    │ │  搜索页      │                │   │
│  │  │ MapActivity  │ │ ArNavActivity│ │SearchActivity│                │   │
│  │  └──────────────┘ └──────────────┘ └──────────────┘                │   │
│  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐                │   │
│  │  │  路线规划页   │ │   设置页      │ │  收藏夹      │                │   │
│  │  │ RouteActivity│ │SettingActivity│ │FavActivity   │                │   │
│  │  └──────────────┘ └──────────────┘ └──────────────┘                │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                        │
│                                    ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                        业务逻辑层 (Business Layer)                   │   │
│  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐                │   │
│  │  │   地图管理器  │ │   导航管理器  │ │   搜索管理器  │                │   │
│  │  │ MapManager   │ │NavManager    │ │SearchManager │                │   │
│  │  └──────────────┘ └──────────────┘ └──────────────┘                │   │
│  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐                │   │
│  │  │   位置管理器  │ │   路况管理器  │ │   AR管理器   │                │   │
│  │  │LocationMgr   │ │TrafficMgr    │ │ARManager     │                │   │
│  │  └──────────────┘ └──────────────┘ └──────────────┘                │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                        │
│                                    ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                         数据层 (Data Layer)                          │   │
│  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐                │   │
│  │  │   地图数据    │ │   导航数据    │ │   缓存数据    │                │   │
│  │  │ MapDataRepo  │ │NavDataRepo   │ │ CacheManager│                │   │
│  │  └──────────────┘ └──────────────┘ └──────────────┘                │   │
│  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐                │   │
│  │  │   离线地图    │ │   收藏数据    │ │   设置数据    │                │   │
│  │  │OfflineMapRepo│ │FavoriteRepo  │ │SettingsRepo  │                │   │
│  │  └──────────────┘ └──────────────┘ └──────────────┘                │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                        │
│                                    ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                       SDK适配层 (SDK Layer)                          │   │
│  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐                │   │
│  │  │  高德地图SDK  │ │   定位SDK    │ │   AR引擎     │                │   │
│  │  │ AMap SDK     │ │Location SDK  │ │ AR Engine   │                │   │
│  │  └──────────────┘ └──────────────┘ └──────────────┘                │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 组件关系图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            组件关系图                                        │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌─────────────────┐                                                       │
│   │  MapActivity    │                                                       │
│   │  (地图主界面)    │                                                       │
│   └────────┬────────┘                                                       │
│            │ uses                                                           │
│            ▼                                                                │
│   ┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐      │
│   │  MapViewModel   │────>│   MapManager    │────>│   MapDataRepo   │      │
│   │  (MVVM架构)      │     │  (地图管理)      │     │  (地图数据)      │      │
│   └─────────────────┘     └────────┬────────┘     └────────┬────────┘      │
│                                    │                        │              │
│                                    ▼                        ▼              │
│                          ┌─────────────────┐     ┌─────────────────┐      │
│                          │   AMapService   │     │  Database/Cache │      │
│                          │  (高德SDK封装)   │     │  (数据存储)      │      │
│                          └────────┬────────┘     └─────────────────┘      │
│                                   │                                        │
│                                   ▼                                        │
│                          ┌─────────────────┐                              │
│                          │  高德地图SDK     │                              │
│                          │  (AMap SDK)     │                              │
│                          └─────────────────┘                              │
│                                                                             │
│   ═══════════════════════════════════════════════════════════════════     │
│                                                                             │
│   ┌─────────────────┐                                                       │
│   │  NavActivity    │                                                       │
│   │  (导航界面)      │                                                       │
│   └────────┬────────┘                                                       │
│            │ uses                                                           │
│            ▼                                                                │
│   ┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐      │
│   │  NavViewModel   │────>│  NavManager     │────>│  NavDataRepo    │      │
│   │  (导航VM)        │     │  (导航管理)      │     │  (导航数据)      │      │
│   └─────────────────┘     └────────┬────────┘     └────────┬────────┘      │
│                                    │                        │              │
│                    ┌───────────────┼───────────────┐        │              │
│                    │               │               │        ▼              │
│                    ▼               ▼               ▼   ┌─────────────────┐ │
│            ┌─────────────┐ ┌─────────────┐ ┌──────────┐ │  nav_history    │ │
│            │RouteService │ │GuideService │ │TTSMgr    │ │  nav_favorite   │ │
│            │(路线规划)    │ │(语音引导)    │ │(语音播报) │ │  (数据库表)      │ │
│            └─────────────┘ └─────────────┘ └──────────┘ └─────────────────┘ │
│                                                                             │
│   ═══════════════════════════════════════════════════════════════════     │
│                                                                             │
│   ┌─────────────────┐                                                       │
│   │ ArNavActivity   │                                                       │
│   │  (AR导航界面)    │                                                       │
│   └────────┬────────┘                                                       │
│            │ uses                                                           │
│            ▼                                                                │
│   ┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐      │
│   │  ARViewModel    │────>│  ARManager      │────>│  AREngine       │      │
│   │  (AR VM)         │     │  (AR管理)        │     │  (AR渲染引擎)    │      │
│   └─────────────────┘     └────────┬────────┘     └────────┬────────┘      │
│                                    │                        │              │
│                                    ▼                        ▼              │
│                          ┌─────────────────┐     ┌─────────────────┐      │
│                          │  CameraService  │     │  OpenGL/Vulkan  │      │
│                          │  (相机服务)      │     │  (图形渲染)      │      │
│                          └─────────────────┘     └─────────────────┘      │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.3 模块间交互关系

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           模块间交互关系                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌──────────────┐                                                          │
│   │  地图导航模块  │                                                          │
│   │   (NAV)      │                                                          │
│   └──────┬───────┘                                                          │
│          │                                                                  │
│   ┌──────┼──────┬────────┬────────┬────────┬────────┐                       │
│   │      │      │        │        │        │        │                       │
│   ▼      ▼      ▼        ▼        ▼        ▼        ▼                       │
│ ┌────┐ ┌────┐ ┌────┐  ┌────┐  ┌────┐  ┌────┐  ┌────┐                      │
│ │AI  │ │MSG │ │SVC │  │VSVC│  │DSVC│  │MKT │  │LCH │                      │
│ │语音 │ │消息 │ │系统 │  │车辆 │  │数据 │  │市场 │  │桌面 │                      │
│ └──┬─┘ └──┬─┘ └─┬──┘  └─┬──┘  └─┬──┘  └─┬──┘  └─┬──┘                      │
│    │      │     │       │       │       │       │                         │
│    ▼      ▼     ▼       ▼       ▼       ▼       ▼                         │
│ ┌─────────────────────────────────────────────────────┐                    │
│ │ 交互类型说明:                                        │                    │
│ │ • AI: 语音搜索、语音导航控制                          │                    │
│ │ • MSG: 导航消息推送(P0/P1优先级)                      │                    │
│ │ • VSVC: 车速/挡位/位置信息获取                        │                    │
│ │ • DSVC: 导航历史/收藏数据持久化                       │                    │
│ │ • MKT: 充电桩/加油站POI数据                          │                    │
│ │ • LCH: 导航Widget桌面显示                            │                    │
│ └─────────────────────────────────────────────────────┘                    │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. 核心类设计

### 3.1 包结构定义

```
com.longcheer.cockpit.nav/
├── ui/                          # UI层
│   ├── activity/               # Activity页面
│   │   ├── MapActivity.kt
│   │   ├── NavigationActivity.kt
│   │   ├── ArNavigationActivity.kt
│   │   ├── SearchActivity.kt
│   │   ├── RoutePlanningActivity.kt
│   │   └── FavoriteActivity.kt
│   ├── fragment/               # Fragment组件
│   │   ├── MapFragment.kt
│   │   ├── RouteOverviewFragment.kt
│   │   └── PoiDetailFragment.kt
│   ├── viewmodel/              # ViewModel
│   │   ├── MapViewModel.kt
│   │   ├── NavigationViewModel.kt
│   │   └── SearchViewModel.kt
│   ├── view/                   # 自定义View
│   │   ├── MapView.kt
│   │   ├── CompassView.kt
│   │   ├── SpeedPanelView.kt
│   │   └── LaneGuidanceView.kt
│   └── adapter/                # RecyclerView适配器
│       ├── PoiListAdapter.kt
│       ├── RouteListAdapter.kt
│       └── FavoriteAdapter.kt
├── business/                    # 业务逻辑层
│   ├── manager/                # 管理器
│   │   ├── MapManager.kt
│   │   ├── NavigationManager.kt
│   │   ├── SearchManager.kt
│   │   ├── LocationManager.kt
│   │   ├── TrafficManager.kt
│   │   ├── ArManager.kt
│   │   └── OfflineMapManager.kt
│   ├── service/                # 服务类
│   │   ├── RouteService.kt
│   │   ├── GuideService.kt
│   │   ├── GeocodeService.kt
│   │   └── TtsService.kt
│   └── engine/                 # 引擎类
│       ├── MapRenderEngine.kt
│       ├── ArRenderEngine.kt
│       └── LocationFusionEngine.kt
├── data/                        # 数据层
│   ├── repository/             # 数据仓库
│   │   ├── MapDataRepository.kt
│   │   ├── NavDataRepository.kt
│   │   ├── SearchDataRepository.kt
│   │   ├── FavoriteRepository.kt
│   │   └── OfflineMapRepository.kt
│   ├── local/                  # 本地数据源
│   │   ├── database/           # 数据库
│   │   │   ├── NavDatabase.kt
│   │   │   ├── dao/
│   │   │   └── entity/
│   │   ├── cache/              # 缓存
│   │   │   ├── MapCache.kt
│   │   │   └── TileCache.kt
│   │   └── prefs/              # SharedPreferences
│   │       └── NavSettings.kt
│   └── remote/                 # 远程数据源
│       ├── api/                # API接口
│       │   ├── AMapApi.kt
│       │   └── CloudApi.kt
│       └── model/              # 数据模型
│           ├── request/
│           └── response/
├── sdk/                         # SDK适配层
│   ├── amap/                   # 高德SDK封装
│   │   ├── AMapService.kt
│   │   ├── AMapMapAdapter.kt
│   │   ├── AMapNavAdapter.kt
│   │   ├── AMapSearchAdapter.kt
│   │   └── callback/
│   ├── location/               # 定位SDK
│   │   └── LocationProvider.kt
│   └── ar/                     # AR引擎
│       └── ArEngine.kt
├── model/                       # 领域模型
│   ├── MapModel.kt
│   ├── NavigationModel.kt
│   ├── PoiModel.kt
│   ├── RouteModel.kt
│   ├── TrafficModel.kt
│   └── ArModel.kt
├── utils/                       # 工具类
│   ├── MapUtils.kt
│   ├── CoordinateUtils.kt
│   └── DistanceUtils.kt
└── di/                          # 依赖注入
    └── NavModule.kt
```

### 3.2 核心类定义

#### 3.2.1 MapManager - 地图管理器

```kotlin
/**
 * 地图管理器
 * 负责地图显示、图层管理、手势交互等核心功能
 * 
 * @author 导航模块团队
 * @since 1.0.0
 */
@Singleton
class MapManager @Inject constructor(
    private val context: Context,
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

    // 地图状态
    private val _mapState = MutableStateFlow<MapState>(MapState.Idle)
    val mapState: StateFlow<MapState> = _mapState.asStateFlow()

    // 当前位置
    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    // 地图配置
    private val _mapConfig = MutableStateFlow(MapConfig())
    val mapConfig: StateFlow<MapConfig> = _mapConfig.asStateFlow()

    /**
     * 初始化地图
     * @param mapView 地图视图
     * @param config 地图配置
     */
    fun initMap(mapView: MapView, config: MapConfig = MapConfig()) {
        _mapConfig.value = config
        aMapService.initMap(mapView, config.toAMapOptions())
        setupMapListeners()
        setupLocationTracking()
    }

    /**
     * 设置地图类型
     * @param type 地图类型
     */
    fun setMapType(type: MapType) {
        aMapService.setMapType(type.toAMapType())
        _mapConfig.update { it.copy(mapType = type) }
    }

    /**
     * 显示实时路况
     * @param enable 是否启用
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
     * @param enable 是否启用3D
     */
    fun set3DMode(enable: Boolean) {
        aMapService.set3DMode(enable)
        _mapConfig.update { it.copy(is3DMode = enable) }
    }

    /**
     * 移动到当前位置
     * @param animate 是否动画移动
     */
    fun moveToCurrentLocation(animate: Boolean = true) {
        val location = _currentLocation.value ?: return
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(
            LatLng(location.latitude, location.longitude),
            DEFAULT_ZOOM
        )
        if (animate) {
            aMapService.animateCamera(cameraUpdate)
        } else {
            aMapService.moveCamera(cameraUpdate)
        }
    }

    /**
     * 搜索POI
     * @param keyword 关键词
     * @param location 中心位置
     * @param radius 搜索半径(米)
     * @return 搜索结果Flow
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
     * @param marker 标记数据
     * @return 标记ID
     */
    fun addMarker(marker: MapMarker): String {
        return aMapService.addMarker(marker.toAMapMarker())
    }

    /**
     * 清除所有标记
     */
    fun clearMarkers() {
        aMapService.clearMarkers()
    }

    /**
     * 释放资源
     */
    fun release() {
        trafficManager.stopTrafficUpdate()
        aMapService.release()
    }

    private fun setupMapListeners() {
        aMapService.setOnMapClickListener { latLng ->
            // 处理地图点击
        }
        aMapService.setOnMarkerClickListener { marker ->
            // 处理标记点击
            true
        }
    }

    private fun setupLocationTracking() {
        locationManager.locationFlow
            .onEach { location ->
                _currentLocation.value = location
                updateLocationMarker(location)
            }
            .launchIn(CoroutineScope(Dispatchers.Main))
    }

    private fun updateLocationMarker(location: Location) {
        aMapService.updateLocationMarker(
            LatLng(location.latitude, location.longitude),
            location.bearing
        )
    }
}

/**
 * 地图状态
 */
sealed class MapState {
    object Idle : MapState()
    object Loading : MapState()
    object Searching : MapState()
    object Navigating : MapState()
    data class Error(val message: String?) : MapState()
}

/**
 * 地图配置
 */
data class MapConfig(
    val mapType: MapType = MapType.NORMAL,
    val showTraffic: Boolean = true,
    val is3DMode: Boolean = false,
    val showBuildings: Boolean = true,
    val compassEnabled: Boolean = true,
    val zoomControlsEnabled: Boolean = false,
    val scaleControlsEnabled: Boolean = true
)

/**
 * 地图类型
 */
enum class MapType {
    NORMAL,     // 标准地图
    SATELLITE,  // 卫星地图
    NIGHT,      // 夜间地图
    NAVI        // 导航地图
}
```

#### 3.2.2 NavigationManager - 导航管理器

```kotlin
/**
 * 导航管理器
 * 负责路线规划、导航引导、语音播报等导航核心功能
 * 
 * @author 导航模块团队
 * @since 1.0.0
 */
@Singleton
class NavigationManager @Inject constructor(
    private val context: Context,
    private val aMapNavAdapter: AMapNavAdapter,
    private val routeService: RouteService,
    private val guideService: GuideService,
    private val ttsService: TtsService,
    private val navDataRepo: NavDataRepository,
    private val messageService: IMessageService
) {
    companion object {
        private const val TAG = "NavigationManager"
        private const val ROUTE_PLAN_TIMEOUT = 5000L // 5秒
        private const val REPLAN_THRESHOLD = 50 // 偏离路线50米重新规划
    }

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
     * 开始导航
     * @param route 选择的路线
     * @param mode 导航模式
     * @return 是否成功启动
     */
    fun startNavigation(
        route: Route = _currentRoute.value!!,
        mode: NavMode = NavMode.NORMAL
    ): Boolean {
        if (_navState.value != NavState.Planned && 
            _navState.value != NavState.Paused) {
            return false
        }

        val success = aMapNavAdapter.startNavigation(route.toAMapRoute(), mode)
        if (success) {
            _navState.value = NavState.Navigating
            guideService.startGuidance()
            
            // 发送导航开始消息
            messageService.sendMessage(createNavStartMessage(route))
            
            // 保存导航历史
            saveNavHistory(route)
        }
        return success
    }

    /**
     * 停止导航
     * @param reason 停止原因
     */
    fun stopNavigation(reason: StopReason = StopReason.USER) {
        aMapNavAdapter.stopNavigation()
        guideService.stopGuidance()
        _navState.value = NavState.Idle
        _navInfo.value = null
        _laneInfo.value = null
        
        // 发送导航结束消息
        messageService.sendMessage(createNavEndMessage(reason))
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
     * @param routeIndex 路线索引
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
     * @param preference 导航偏好设置
     */
    fun setNavPreference(preference: NavPreference) {
        routeService.setPreference(preference)
        aMapNavAdapter.setNavPreference(preference.toAMapPreference())
    }

    private fun setupNavListeners() {
        // 导航信息回调
        aMapNavAdapter.setOnNavInfoCallback { info ->
            _navInfo.value = info.toNavInfo()
            
            // 播报语音
            if (info.isKeyPoint) {
                ttsService.speak(info.guideText)
            }
            
            // 发送关键导航消息
            if (info.alertType != AlertType.NONE) {
                messageService.sendMessage(createNavAlertMessage(info))
            }
        }

        // 车道信息回调
        aMapNavAdapter.setOnLaneInfoCallback { laneInfo ->
            _laneInfo.value = laneInfo.toLaneInfo()
        }

        // 偏航回调
        aMapNavAdapter.setOnOffRouteCallback { distance ->
            if (distance > REPLAN_THRESHOLD) {
                // 自动重新规划
                autoReplanRoute()
            }
        }

        // 到达目的地回调
        aMapNavAdapter.setOnArrivedCallback {
            stopNavigation(StopReason.ARRIVED)
        }
    }

    private fun autoReplanRoute() {
        val currentLoc = _navInfo.value?.currentLocation ?: return
        val end = _currentRoute.value?.endPoint ?: return
        
        CoroutineScope(Dispatchers.IO).launch {
            planRoute(
                start = NavPoint(currentLoc.latitude, currentLoc.longitude),
                end = end,
                strategy = RouteStrategy.FAST
            )
        }
    }

    private fun saveNavHistory(route: Route) {
        CoroutineScope(Dispatchers.IO).launch {
            navDataRepo.saveNavHistory(route.toNavHistoryEntity())
        }
    }

    private fun createNavStartMessage(route: Route): Message {
        return Message(
            id = UUID.randomUUID().toString(),
            appId = "navigation",
            priority = MessagePriority.P1,
            title = "导航开始",
            content = "前往 ${route.endPoint.name}",
            timestamp = System.currentTimeMillis()
        )
    }

    private fun createNavEndMessage(reason: StopReason): Message {
        return Message(
            id = UUID.randomUUID().toString(),
            appId = "navigation",
            priority = MessagePriority.P2,
            title = when (reason) {
                StopReason.ARRIVED -> "已到达目的地"
                StopReason.USER -> "导航已结束"
                StopReason.ERROR -> "导航异常结束"
            },
            content = "",
            timestamp = System.currentTimeMillis()
        )
    }

    private fun createNavAlertMessage(info: NavInfo): Message {
        return Message(
            id = UUID.randomUUID().toString(),
            appId = "navigation",
            priority = MessagePriority.P1,
            title = "导航提示",
            content = info.guideText,
            timestamp = System.currentTimeMillis()
        )
    }

    fun release() {
        stopNavigation()
        aMapNavAdapter.release()
        ttsService.release()
    }
}

/**
 * 导航状态
 */
sealed class NavState {
    object Idle : NavState()
    object Planning : NavState()
    object Planned : NavState()
    object Navigating : NavState()
    object Paused : NavState()
    data class Error(val message: String?) : NavState()
}

/**
 * 路线策略
 */
enum class RouteStrategy {
    FAST,       // 最快路线
    SHORT,      // 最短路线
    AVOID_JAM,  // 躲避拥堵
    SAVE_MONEY, // 省钱路线
    HIGHWAY,    // 高速优先
    NO_HIGHWAY  // 不走高速
}

/**
 * 导航模式
 */
enum class NavMode {
    NORMAL,     // 普通导航
    AR,         // AR导航
    SIMULATE    // 模拟导航
}

/**
 * 停止原因
 */
enum class StopReason {
    USER,       // 用户停止
    ARRIVED,    // 到达目的地
    ERROR       // 错误
}
```

#### 3.2.3 ARManager - AR导航管理器

```kotlin
/**
 * AR导航管理器
 * 负责AR实景导航的相机、渲染和虚实融合
 * 
 * @author 导航模块团队
 * @since 1.0.0
 */
@Singleton
class ARManager @Inject constructor(
    private val context: Context,
    private val arEngine: ArEngine,
    private val locationManager: LocationManager,
    private val sensorManager: SensorManager
) {
    companion object {
        private const val TAG = "ARManager"
        private const val AR_FOV = 60f // AR视场角
    }

    // AR状态
    private val _arState = MutableStateFlow<ArState>(ArState.Idle)
    val arState: StateFlow<ArState> = _arState.asStateFlow()

    // AR导航信息
    private val _arNavInfo = MutableStateFlow<ArNavInfo?>(null)
    val arNavInfo: StateFlow<ArNavInfo?> = _arNavInfo.asStateFlow()

    // 相机预览Surface
    private var cameraSurface: Surface? = null

    /**
     * 初始化AR引擎
     * @param surface 相机预览Surface
     * @param width 预览宽度
     * @param height 预览高度
     */
    fun initAR(surface: Surface, width: Int, height: Int) {
        cameraSurface = surface
        arEngine.init(context, width, height)
        arEngine.setCameraSurface(surface)
        setupAREngineCallbacks()
        _arState.value = ArState.Ready
    }

    /**
     * 开始AR导航
     * @param route 导航路线
     */
    fun startARNavigation(route: Route) {
        if (_arState.value != ArState.Ready) return
        
        arEngine.startNavigation(route.toArRoute())
        startSensorFusion()
        _arState.value = ArState.Navigating
    }

    /**
     * 停止AR导航
     */
    fun stopARNavigation() {
        arEngine.stopNavigation()
        stopSensorFusion()
        _arState.value = ArState.Ready
    }

    /**
     * 设置AR指引样式
     * @param style AR指引样式
     */
    fun setARGuideStyle(style: ArGuideStyle) {
        arEngine.setGuideStyle(style.toEngineStyle())
    }

    /**
     * 渲染一帧AR画面
     * @param cameraFrame 相机帧数据
     * @param projectionMatrix 投影矩阵
     */
    fun renderFrame(cameraFrame: CameraFrame, projectionMatrix: FloatArray) {
        arEngine.renderFrame(cameraFrame, projectionMatrix)
    }

    /**
     * 获取AR指引位置（屏幕坐标）
     * @param worldPos 世界坐标
     * @return 屏幕坐标，null表示不在视野内
     */
    fun worldToScreen(worldPos: Vector3): PointF? {
        return arEngine.worldToScreen(worldPos)
    }

    private fun setupAREngineCallbacks() {
        arEngine.setOnGuideUpdateListener { guide ->
            _arNavInfo.value = guide.toArNavInfo()
        }
    }

    private fun startSensorFusion() {
        // 启动传感器融合，提高定位精度
        sensorManager.registerListener(
            sensorListener,
            Sensor.TYPE_ROTATION_VECTOR,
            SensorManager.SENSOR_DELAY_GAME
        )
    }

    private fun stopSensorFusion() {
        sensorManager.unregisterListener(sensorListener)
    }

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                arEngine.updateDeviceOrientation(rotationMatrix)
            }
        }
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    fun release() {
        stopARNavigation()
        arEngine.release()
        cameraSurface = null
        _arState.value = ArState.Idle
    }
}

/**
 * AR状态
 */
sealed class ArState {
    object Idle : ArState()
    object Initializing : ArState()
    object Ready : ArState()
    object Navigating : ArState()
    object Error : ArState()
}

/**
 * AR导航信息
 */
data class ArNavInfo(
    val turnType: TurnType,
    val distance: Int, // 距离转向点距离(米)
    val guideText: String,
    val arrowPosition: PointF?, // 指引箭头屏幕位置
    val laneInfo: ArLaneInfo?,
    val safetyAlert: SafetyAlert?
)

/**
 * AR指引样式
 */
enum class ArGuideStyle {
    ARROW,      // 箭头指引
    GUIDE_LINE, // 引导线
    COMBINED    // 组合指引
}
```

#### 3.2.4 OfflineMapManager - 离线地图管理器

```kotlin
/**
 * 离线地图管理器
 * 负责离线地图的下载、更新和管理
 * 
 * @author 导航模块团队
 * @since 1.0.0
 */
@Singleton
class OfflineMapManager @Inject constructor(
    private val context: Context,
    private val aMapService: AMapService,
    private val offlineMapRepo: OfflineMapRepository
) {
    companion object {
        private const val TAG = "OfflineMapManager"
        private const val MAX_DOWNLOAD_TASKS = 3
    }

    // 离线地图状态
    private val _offlineState = MutableStateFlow<OfflineState>(OfflineState.Idle)
    val offlineState: StateFlow<OfflineState> = _offlineState.asStateFlow()

    // 下载任务列表
    private val _downloadTasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val downloadTasks: StateFlow<List<DownloadTask>> = _downloadTasks.asStateFlow()

    // 已下载城市列表
    private val _downloadedCities = MutableStateFlow<List<OfflineCity>>(emptyList())
    val downloadedCities: StateFlow<List<OfflineCity>> = _downloadedCities.asStateFlow()

    /**
     * 初始化离线地图
     */
    fun init() {
        refreshDownloadedCities()
    }

    /**
     * 获取支持离线地图的城市列表
     * @return 城市列表
     */
    suspend fun getOfflineCityList(): List<OfflineCity> = 
        offlineMapRepo.getOfflineCityList()

    /**
     * 搜索城市
     * @param keyword 关键词
     * @return 匹配的城市列表
     */
    suspend fun searchCity(keyword: String): List<OfflineCity> =
        offlineMapRepo.searchCity(keyword)

    /**
     * 开始下载城市离线地图
     * @param cityCode 城市编码
     * @return 下载任务ID
     */
    fun startDownload(cityCode: String): String {
        val taskId = UUID.randomUUID().toString()
        val task = DownloadTask(
            id = taskId,
            cityCode = cityCode,
            state = DownloadState.PENDING,
            progress = 0
        )
        
        _downloadTasks.update { it + task }
        
        // 启动下载
        aMapService.startOfflineDownload(cityCode, object : OfflineMapDownloadListener {
            override fun onDownload(progress: Int) {
                updateTaskProgress(taskId, progress)
            }
            
            override fun onComplete() {
                updateTaskState(taskId, DownloadState.COMPLETED)
                refreshDownloadedCities()
            }
            
            override fun onError(errorCode: Int) {
                updateTaskState(taskId, DownloadState.ERROR)
            }
        })
        
        return taskId
    }

    /**
     * 暂停下载
     * @param taskId 任务ID
     */
    fun pauseDownload(taskId: String) {
        aMapService.pauseOfflineDownload(taskId)
        updateTaskState(taskId, DownloadState.PAUSED)
    }

    /**
     * 恢复下载
     * @param taskId 任务ID
     */
    fun resumeDownload(taskId: String) {
        aMapService.resumeOfflineDownload(taskId)
        updateTaskState(taskId, DownloadState.DOWNLOADING)
    }

    /**
     * 删除离线地图
     * @param cityCode 城市编码
     */
    fun removeOfflineMap(cityCode: String) {
        aMapService.removeOfflineMap(cityCode)
        offlineMapRepo.removeOfflineMap(cityCode)
        refreshDownloadedCities()
    }

    /**
     * 检查城市离线地图是否存在
     * @param cityCode 城市编码
     * @return 是否存在
     */
    fun hasOfflineMap(cityCode: String): Boolean {
        return _downloadedCities.value.any { it.code == cityCode }
    }

    /**
     * 获取离线地图总大小
     * @return 总大小(字节)
     */
    fun getTotalOfflineSize(): Long {
        return _downloadedCities.value.sumOf { it.size }
    }

    /**
     * 更新所有离线地图
     */
    fun updateAll() {
        _downloadedCities.value.forEach { city ->
            if (city.hasUpdate) {
                startDownload(city.code)
            }
        }
    }

    private fun updateTaskProgress(taskId: String, progress: Int) {
        _downloadTasks.update { tasks ->
            tasks.map { task ->
                if (task.id == taskId) {
                    task.copy(progress = progress, state = DownloadState.DOWNLOADING)
                } else task
            }
        }
    }

    private fun updateTaskState(taskId: String, state: DownloadState) {
        _downloadTasks.update { tasks ->
            tasks.map { task ->
                if (task.id == taskId) task.copy(state = state) else task
            }
        }
    }

    private fun refreshDownloadedCities() {
        CoroutineScope(Dispatchers.IO).launch {
            val cities = offlineMapRepo.getDownloadedCities()
            _downloadedCities.value = cities
        }
    }
}

/**
 * 下载任务
 */
data class DownloadTask(
    val id: String,
    val cityCode: String,
    val state: DownloadState,
    val progress: Int
)

/**
 * 下载状态
 */
enum class DownloadState {
    PENDING,     // 等待中
    DOWNLOADING, // 下载中
    PAUSED,      // 已暂停
    COMPLETED,   // 已完成
    ERROR        // 错误
}

/**
 * 离线城市
 */
data class OfflineCity(
    val code: String,
    val name: String,
    val size: Long,
    val version: String,
    val hasUpdate: Boolean = false
)
```

### 3.3 数据模型定义

#### 3.3.1 导航相关模型

```kotlin
/**
 * 导航点
 */
data class NavPoint(
    val latitude: Double,
    val longitude: Double,
    val name: String = "",
    val address: String = "",
    val poiId: String? = null
)

/**
 * 路线
 */
data class Route(
    val id: String,
    val startPoint: NavPoint,
    val endPoint: NavPoint,
    val waypoints: List<NavPoint> = emptyList(),
    val distance: Int, // 总距离(米)
    val duration: Int, // 总时长(秒)
    val tollCost: Int, // 过路费(元)
    val trafficLights: Int, // 红绿灯数量
    val steps: List<RouteStep>,
    val bounds: LatLngBounds,
    val trafficStatus: TrafficStatus,
    val strategy: RouteStrategy
)

/**
 * 路线步骤
 */
data class RouteStep(
    val index: Int,
    val instruction: String,
    val distance: Int,
    val duration: Int,
    val roadName: String,
    val action: TurnAction,
    val polyline: List<LatLng>,
    val trafficConditions: List<TrafficCondition>
)

/**
 * 路线规划结果
 */
data class RoutePlanResult(
    val recommendedRoute: Route,
    val alternativeRoutes: List<Route>,
    val queryTime: Long
)

/**
 * 导航信息
 */
data class NavInfo(
    val currentLocation: Location,
    val currentSpeed: Float,
    val remainingDistance: Int,
    val remainingTime: Int,
    val nextTurnDistance: Int,
    val nextTurnType: TurnType,
    val guideText: String,
    val isKeyPoint: Boolean,
    val alertType: AlertType,
    val currentRoadName: String,
    val nextRoadName: String
)

/**
 * 车道信息
 */
data class LaneInfo(
    val laneCount: Int,
    val lanes: List<Lane>,
    val recommendedLanes: List<Int>
)

/**
 * 车道
 */
data class Lane(
    val index: Int,
    val type: LaneType,
    val directions: List<TurnDirection>,
    val isRecommended: Boolean
)

/**
 * 转向类型
 */
enum class TurnType {
    STRAIGHT,
    LEFT,
    RIGHT,
    UTURN,
    LEFT_FRONT,
    RIGHT_FRONT,
    LEFT_BACK,
    RIGHT_BACK,
    ROUNDABOUT_ENTER,
    ROUNDABOUT_EXIT,
    DESTINATION
}

/**
 * 车道类型
 */
enum class LaneType {
    NORMAL,
    BUS,
    BIKE,
    VARIABLE,
    TURN
}

/**
 * 转向方向
 */
enum class TurnDirection {
    STRAIGHT,
    LEFT,
    SLIGHT_LEFT,
    RIGHT,
    SLIGHT_RIGHT,
    UTURN
}

/**
 * 告警类型
 */
enum class AlertType {
    NONE,
    SPEED_CAMERA,
    TRAFFIC_LIGHT,
    TOLL_GATE,
    SERVICE_AREA,
    EMERGENCY_LANE,
    CONGESTION
}

/**
 * 路况状态
 */
enum class TrafficStatus {
    UNKNOWN,
    SMOOTH,     // 畅通
    SLOW,       // 缓行
    CONGESTED,  // 拥堵
    BLOCKED     // 严重拥堵
}
```

#### 3.3.2 POI相关模型

```kotlin
/**
 * POI(兴趣点)
 */
data class Poi(
    val id: String,
    val name: String,
    val type: PoiType,
    val location: LatLng,
    val address: String,
    val phone: String? = null,
    val distance: Int = 0,
    val rating: Float = 0f,
    val price: Int = 0,
    val businessHours: String? = null,
    val photos: List<String> = emptyList(),
    val isOpen: Boolean? = null
)

/**
 * POI类型
 */
enum class PoiType {
    GAS_STATION,    // 加油站
    CHARGING_STATION, // 充电站
    RESTAURANT,     // 餐厅
    HOTEL,          // 酒店
    PARKING,        // 停车场
    TOILET,         // 卫生间
    SHOPPING,       // 购物
    ENTERTAINMENT,  // 娱乐
    SCENIC,         // 景点
    SERVICE_AREA,   // 服务区
    HOSPITAL,       // 医院
    BANK,           // 银行
    OTHER
}

/**
 * POI搜索参数
 */
data class PoiSearchParam(
    val keyword: String = "",
    val category: PoiType? = null,
    val location: LatLng? = null,
    val radius: Int = 5000,
    val city: String? = null,
    val sortRule: SortRule = SortRule.DISTANCE,
    val page: Int = 1,
    val pageSize: Int = 20
)

/**
 * 排序规则
 */
enum class SortRule {
    DISTANCE,   // 距离优先
    RATING,     // 评分优先
    PRICE_ASC,  // 价格从低到高
    PRICE_DESC  // 价格从高到低
}
```

---

## 4. 时序图设计

### 4.1 路线规划时序图

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│     User    │     │ RoutePlanning│     │NavigationMgr│     │ RouteService│     │  高德SDK    │
│  (用户)      │     │  Activity   │     │             │     │             │     │             │
└──────┬──────┘     └──────┬──────┘     └──────┬──────┘     └──────┬──────┘     └──────┬──────┘
       │                   │                   │                   │                   │
       │ 1: 输入起点终点    │                   │                   │                   │
       │──────────────────>│                   │                   │                   │
       │                   │                   │                   │                   │
       │                   │ 2: planRoute()    │                   │                   │
       │                   │──────────────────>│                   │                   │
       │                   │                   │                   │                   │
       │                   │                   │ 3: planRoute()    │                   │
       │                   │                   │──────────────────>│                   │
       │                   │                   │                   │                   │
       │                   │                   │                   │ 4: 构建请求参数    │
       │                   │                   │                   │──────────────────>│
       │                   │                   │                   │                   │
       │                   │                   │                   │ 5: 返回路线数据    │
       │                   │                   │                   │<──────────────────│
       │                   │                   │                   │                   │
       │                   │                   │ 6: RoutePlanResult│                   │
       │                   │                   │<──────────────────│                   │
       │                   │                   │                   │                   │
       │                   │ 7: 显示路线列表    │                   │                   │
       │                   │<──────────────────│                   │                   │
       │                   │                   │                   │                   │
       │ 8: 选择路线        │                   │                   │                   │
       │──────────────────>│                   │                   │                   │
       │                   │                   │                   │                   │
       │                   │ 9: selectRoute()  │                   │                   │
       │                   │──────────────────>│                   │                   │
       │                   │                   │                   │                   │
       │                   │ 10: 路线详情页面   │                   │                   │
       │                   │<──────────────────│                   │                   │
       │                   │                   │                   │                   │
       │ 11: 点击开始导航   │                   │                   │                   │
       │──────────────────────────────────────────────────────────>│                   │
       │                   │                   │                   │                   │
       │                   │                   │                   │                   │
       ▼                   ▼                   ▼                   ▼                   ▼

[说明]
1. 用户在路线规划页面输入起点和终点
2. Activity调用NavigationManager的planRoute方法
3. NavigationManager调用RouteService进行路线规划
4. RouteService构建请求参数并调用高德SDK
5. 高德SDK返回路线数据
6. RouteService解析并返回RoutePlanResult
7. Activity显示多条路线供用户选择
8. 用户选择期望的路线
9. Activity通知NavigationManager选中路线
10. 显示路线详情页面
11. 用户点击开始导航，切换到导航页面
```

### 4.2 导航过程时序图

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   定位SDK    │     │ NavigationMgr│     │ 高德导航SDK  │     │ GuideService│     │ TtsService  │
│             │     │             │     │             │     │             │             │
└──────┬──────┘     └──────┬──────┘     └──────┬──────┘     └──────┬──────┘     └──────┬──────┘
       │                   │                   │                   │                   │
       │ 1: 位置更新(10Hz)  │                   │                   │                   │
       │──────────────────>│                   │                   │                   │
       │                   │                   │                   │                   │
       │                   │ 2: 更新位置        │                   │                   │
       │                   │──────────────────>│                   │                   │
       │                   │                   │                   │                   │
       │                   │ 3: 导航信息回调    │                   │                   │
       │                   │<──────────────────│                   │                   │
       │                   │                   │                   │                   │
       │                   │ 4: 检查是否关键引导点│                   │                   │
       │                   │──────────────────>│                   │                   │
       │                   │                   │                   │                   │
       │                   │ 5: 生成引导文本    │                   │                   │
       │                   │<──────────────────│                   │                   │
       │                   │                   │                   │                   │
       │                   │ 6: speak()        │                   │                   │
       │                   │──────────────────────────────────────────────────────────>│
       │                   │                   │                   │                   │
       │                   │ 7: 播报语音        │                   │                   │
       │                   │<──────────────────────────────────────────────────────────│
       │                   │                   │                   │                   │
       │                   │ 8: 更新UI          │                   │                   │
       │                   │───┐               │                   │                   │
       │                   │   │               │                   │                   │
       │                   │<──┘               │                   │                   │
       │                   │                   │                   │                   │
       │                   │ 9: 发送导航消息(P1)│                   │                   │
       │                   │───┐               │                   │                   │
       │                   │   │               │                   │                   │
       │                   │<──┘               │                   │                   │
       │                   │                   │                   │                   │
       │                   │                   │                   │                   │
       ▼                   ▼                   ▼                   ▼                   ▼

[说明]
1. 定位SDK以10Hz频率更新位置信息
2. NavigationManager将位置更新传递给高德导航SDK
3. 高德导航SDK回调导航信息（包含转向、距离等）
4. NavigationManager检查是否为关键引导点
5. GuideService生成语音引导文本
6. NavigationManager调用TtsService播报
7. TTS完成语音播报
8. NavigationManager更新导航UI状态
9. 关键导航消息以P1优先级发送给消息中心

循环执行1-9，直到到达目的地
```

### 4.3 AR导航时序图

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Camera    │     │  ARManager  │     │   ArEngine  │     │   OpenGL    │     │   高德SDK    │
│  (相机)      │     │             │     │             │     │  (渲染)      │             │
└──────┬──────┘     └──────┬──────┘     └──────┬──────┘     └──────┬──────┘     └──────┬──────┘
       │                   │                   │                   │                   │
       │ 1: 初始化相机      │                   │                   │                   │
       │──────────────────>│                   │                   │                   │
       │                   │                   │                   │                   │
       │                   │ 2: init()         │                   │                   │
       │                   │──────────────────>│                   │                   │
       │                   │                   │                   │                   │
       │                   │                   │ 3: 初始化OpenGL上下文│                   │
       │                   │                   │──────────────────>│                   │
       │                   │                   │                   │                   │
       │                   │                   │ 4: 加载AR资源       │                   │
       │                   │                   │───┐               │                   │
       │                   │                   │   │               │                   │
       │                   │                   │<──┘               │                   │
       │                   │                   │                   │                   │
       │ 5: 相机帧数据(30fps)│                   │                   │                   │
       │──────────────────>│                   │                   │                   │
       │                   │                   │                   │                   │
       │                   │ 6: renderFrame()  │                   │                   │
       │                   │──────────────────>│                   │                   │
       │                   │                   │                   │                   │
       │                   │                   │ 7: 虚实融合计算     │                   │
       │                   │                   │───┐               │                   │
       │                   │                   │   │               │                   │
       │                   │                   │<──┘               │                   │
       │                   │                   │                   │                   │
       │                   │                   │ 8: 渲染AR指引      │                   │
       │                   │                   │──────────────────>│                   │
       │                   │                   │                   │                   │
       │                   │                   │ 9: 获取导航数据     │                   │
       │                   │                   │──────────────────────────────────────>│
       │                   │                   │                   │                   │
       │                   │                   │ 10: 返回导航数据    │                   │
       │                   │                   │<──────────────────────────────────────│
       │                   │                   │                   │                   │
       │                   │                   │ 11: 更新指引位置    │                   │
       │                   │                   │───┐               │                   │
       │                   │                   │   │               │                   │
       │                   │                   │<──┘               │                   │
       │                   │                   │                   │                   │
       │                   │ 12: 更新AR导航信息 │                   │                   │
       │                   │<──────────────────│                   │                   │
       │                   │                   │                   │                   │
       │                   │                   │                   │                   │
       ▼                   ▼                   ▼                   ▼                   ▼

[说明]
1. 初始化相机，设置预览参数
2. 初始化ARManager和ArEngine
3. ArEngine初始化OpenGL渲染上下文
4. 加载AR指引资源（箭头、引导线等）
5. 相机以30fps输出帧数据
6. ARManager调用ArEngine渲染
7. ArEngine进行虚实融合计算（图像识别、位置匹配）
8. 使用OpenGL渲染AR指引元素
9. ArEngine获取实时导航数据
10. 高德SDK返回导航数据
11. 根据导航数据更新AR指引位置
12. ARManager更新AR导航信息到UI层

循环执行5-12，实现实时AR导航
```

### 4.4 离线地图下载时序图

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│     User    │     │OfflineMapMgr│     │OfflineMapRepo│     │ 高德离线SDK  │     │   Database  │
│  (用户)      │     │             │     │             │     │             │     │             │
└──────┬──────┘     └──────┬──────┘     └──────┬──────┘     └──────┬──────┘     └──────┬──────┘
       │                   │                   │                   │                   │
       │ 1: 浏览城市列表    │                   │                   │                   │
       │──────────────────>│                   │                   │                   │
       │                   │                   │                   │                   │
       │                   │ 2: getOfflineCityList()              │                   │
       │                   │──────────────────>│                   │                   │
       │                   │                   │                   │                   │
       │                   │                   │ 3: 获取城市列表    │                   │
       │                   │                   │──────────────────>│                   │
       │                   │                   │                   │                   │
       │                   │                   │ 4: 返回城市数据    │                   │
       │                   │                   │<──────────────────│                   │
       │                   │                   │                   │                   │
       │ 5: 显示城市列表    │                   │                   │                   │
       │<──────────────────│                   │                   │                   │
       │                   │                   │                   │                   │
       │ 6: 选择城市下载    │                   │                   │                   │
       │──────────────────>│                   │                   │                   │
       │                   │                   │                   │                   │
       │                   │ 7: startDownload()│                   │                   │
       │                   │──────────────────>│                   │                   │
       │                   │                   │                   │                   │
       │                   │                   │ 8: 开始下载        │                   │
       │                   │                   │──────────────────>│                   │
       │                   │                   │                   │                   │
       │                   │ 9: 进度更新回调    │                   │                   │
       │                   │<──────────────────────────────────────│                   │
       │                   │                   │                   │                   │
       │ 10: 更新下载进度   │                   │                   │                   │
       │<──────────────────│                   │                   │                   │
       │                   │                   │                   │                   │
       │                   │                   │                   │ 11: 下载完成       │
       │                   │                   │                   │<──────────────────│
       │                   │                   │                   │                   │
       │                   │                   │ 12: 保存下载记录   │                   │
       │                   │                   │──────────────────────────────────────>│
       │                   │                   │                   │                   │
       │                   │                   │ 13: 保存成功       │                   │
       │                   │                   │<──────────────────────────────────────│
       │                   │                   │                   │                   │
       │                   │ 14: 刷新已下载列表 │                   │                   │
       │                   │<──────────────────│                   │                   │
       │                   │                   │                   │                   │
       ▼                   ▼                   ▼                   ▼                   ▼

[说明]
1. 用户进入离线地图页面，浏览可下载城市
2. 调用OfflineMapManager获取城市列表
3. OfflineMapRepository从高德SDK获取城市数据
4. 返回城市列表（包含城市名称、大小、版本等）
5. 显示城市列表给用户
6. 用户选择需要下载的城市
7. 调用startDownload开始下载
8. OfflineMapRepository调用高德离线SDK开始下载
9. SDK回调下载进度
10. 实时更新下载进度UI
11. 高德SDK通知下载完成
12. 保存下载记录到数据库
13. 数据库保存成功
14. 刷新已下载城市列表
```

---

## 5. 高德SDK集成设计

### 5.1 SDK架构集成

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         高德SDK集成架构                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                      应用层 (Application)                            │   │
│  │  ┌─────────────────────────────────────────────────────────────┐   │   │
│  │  │                 地图导航模块 (NAV)                           │   │   │
│  │  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐       │   │   │
│  │  │  │MapManager│ │NavManager│ │SearchMgr │ │ ARManager│       │   │   │
│  │  │  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘       │   │   │
│  │  └───────┼───────────┼───────────┼───────────┼───────────────┘   │   │
│  └──────────┼───────────┼───────────┼───────────┼───────────────────┘   │
│             │           │           │           │                        │
│  ┌──────────┼───────────┼───────────┼───────────┼───────────────────┐   │
│  │          ▼           ▼           ▼           ▼                   │   │
│  │  ┌─────────────────────────────────────────────────────────────┐ │   │
│  │  │              SDK适配层 (SDK Adapter Layer)                   │ │   │
│  │  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐         │ │   │
│  │  │  │AMapMapAdapter│ │AMapNavAdapter│ │AMapSearchAdap│         │ │   │
│  │  │  │              │ │              │ │ter           │         │ │   │
│  │  │  └──────┬───────┘ └──────┬───────┘ └──────┬───────┘         │ │   │
│  │  └─────────┼────────────────┼────────────────┼─────────────────┘ │   │
│  └────────────┼────────────────┼────────────────┼───────────────────┘   │
│               │                │                │                        │
│  ┌────────────┼────────────────┼────────────────┼───────────────────┐   │
│  │            ▼                ▼                ▼                   │   │
│  │  ┌─────────────────────────────────────────────────────────────┐ │   │
│  │  │                 高德地图车机版SDK V8.0+                      │ │   │
│  │  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐         │ │   │
│  │  │  │   AMap地图    │ │  AMap导航     │ │  AMap搜索     │         │ │   │
│  │  │  │   (地图显示)  │ │  (路线规划)   │ │  (POI搜索)    │         │ │   │
│  │  │  └──────────────┘ └──────────────┘ └──────────────┘         │ │   │
│  │  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐         │ │   │
│  │  │  │AMapLocation  │ │AMapOfflineMap│ │  AMapAR      │         │ │   │
│  │  │  │  (定位服务)   │ │  (离线地图)   │ │  (AR导航)     │         │ │   │
│  │  │  └──────────────┘ └──────────────┘ └──────────────┘         │ │   │
│  │  └─────────────────────────────────────────────────────────────┘ │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 5.2 SDK适配器设计

#### 5.2.1 AMapService - 高德地图服务封装

```kotlin
/**
 * 高德地图服务
 * 封装高德地图SDK核心功能
 */
@Singleton
class AMapService @Inject constructor(
    private val context: Context
) {
    private var aMap: AMap? = null
    private var mapView: MapView? = null

    /**
     * 初始化地图
     */
    fun initMap(mapView: MapView, options: AMapOptions) {
        this.mapView = mapView
        mapView.map.let { map ->
            this.aMap = map
            map.setMapType(options.mapType)
            map.isTrafficEnabled = options.showTraffic
            map.uiSettings.apply {
                isCompassEnabled = options.compassEnabled
                isZoomControlsEnabled = options.zoomControlsEnabled
                isScaleControlsEnabled = options.scaleControlsEnabled
            }
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
     * 添加标记
     */
    fun addMarker(options: MarkerOptions): String? {
        return aMap?.addMarker(options)?.id
    }

    /**
     * 清除所有标记
     */
    fun clearMarkers() {
        aMap?.clear()
    }

    /**
     * 相机移动
     */
    fun moveCamera(update: CameraUpdate) {
        aMap?.moveCamera(update)
    }

    /**
     * 相机动画
     */
    fun animateCamera(update: CameraUpdate) {
        aMap?.animateCamera(update)
    }

    /**
     * 更新位置标记
     */
    fun updateLocationMarker(latLng: LatLng, bearing: Float) {
        // 实现位置标记更新
    }

    /**
     * 释放资源
     */
    fun release() {
        mapView?.onDestroy()
        aMap = null
        mapView = null
    }
}

/**
 * 高德地图选项
 */
data class AMapOptions(
    val mapType: Int = AMap.MAP_TYPE_NORMAL,
    val showTraffic: Boolean = true,
    val compassEnabled: Boolean = true,
    val zoomControlsEnabled: Boolean = false,
    val scaleControlsEnabled: Boolean = true
)
```

#### 5.2.2 AMapNavAdapter - 高德导航适配器

```kotlin
/**
 * 高德导航适配器
 * 封装高德导航SDK功能
 */
@Singleton
class AMapNavAdapter @Inject constructor(
    private val context: Context
) {
    private var naviInstance: AMapNavi? = null
    private var naviListener: AMapNaviListener? = null

    /**
     * 初始化导航
     */
    fun init() {
        naviInstance = AMapNavi.getInstance(context)
        setupNaviListener()
    }

    /**
     * 开始导航
     */
    fun startNavigation(route: AMapNaviRoute, mode: NavMode): Boolean {
        naviInstance?.let { navi ->
            // 设置导航模式
            when (mode) {
                NavMode.GPS -> navi.startNavi(AMapNavi.GPSNaviMode)
                NavMode.EMULATOR -> navi.startNavi(AMapNavi.EmulatorNaviMode)
                else -> return false
            }
            return true
        }
        return false
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
     * 设置导航监听
     */
    private fun setupNaviListener() {
        naviListener = object : AMapNaviListener {
            override fun onInitNaviFailure() {}
            override fun onInitNaviSuccess() {}
            override fun onStartNavi(type: Int) {}
            override fun onTrafficStatusUpdate() {}
            override fun onLocationChange(location: AMapNaviLocation) {
                // 位置更新回调
            }
            override fun onGetNavigationText(type: Int, text: String) {
                // 导航文本回调
            }
            override fun onGetNavigationText(s: String) {}
            override fun onEndEmulatorNavi() {}
            override fun onArriveDestination() {
                // 到达目的地
            }
            override fun onCalculateRouteSuccess(response: AMapCalcRouteResult?) {
                // 路线规划成功
            }
            override fun onCalculateRouteFailure(response: AMapCalcRouteResult?) {
                // 路线规划失败
            }
            override fun onReCalculateRouteForYaw() {
                // 偏航重新规划
            }
            override fun onReCalculateRouteForTrafficJam() {
                // 拥堵重新规划
            }
            override fun onArrivedWayPoint(wayID: Int) {}
            override fun onGpsOpenStatus(enabled: Boolean) {}
            override fun updateNaviInfo(info: NaviInfo?) {
                // 导航信息更新
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
            override fun showLaneInfo(laneInfo: Array<AMapLaneInfo>?, backupLane: ByteArray?, suggestedLane: ByteArray?) {
                // 显示车道信息
            }
            override fun hideLaneInfo() {}
            override fun onCalculateRouteSuccess(routeIds: IntArray?) {}
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

    fun setOnNavInfoCallback(callback: (NaviInfo) -> Unit) {
        // 设置导航信息回调
    }

    fun setOnLaneInfoCallback(callback: (LaneInfo) -> Unit) {
        // 设置车道信息回调
    }

    fun setOnOffRouteCallback(callback: (Int) -> Unit) {
        // 设置偏航回调
    }

    fun setOnArrivedCallback(callback: () -> Unit) {
        // 设置到达回调
    }

    fun release() {
        naviInstance?.removeAMapNaviListener(naviListener)
        naviInstance?.destroy()
        naviInstance = null
    }
}
```

### 5.3 SDK配置

```kotlin
/**
 * 高德SDK配置
 */
object AMapConfig {
    // API Key（实际项目中从配置文件读取）
    const val API_KEY = "YOUR_AMAP_API_KEY"
    
    // 定位配置
    const val LOCATION_INTERVAL = 1000L // 定位间隔(ms)
    const val LOCATION_PRIORITY = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy
    
    // 导航配置
    const val NAVI_EMULATOR_SPEED = 60 // 模拟导航速度(km/h)
    const val NAVI_REPLAN_DISTANCE = 50 // 偏航重新规划阈值(米)
    
    // 地图配置
    const val DEFAULT_ZOOM = 15f
    const val MIN_ZOOM = 3f
    const val MAX_ZOOM = 20f
    const val TILE_CACHE_SIZE = 100 * 1024 * 1024L // 100MB
    
    // AR配置
    const val AR_FOV = 60f
    const val AR_GUIDE_DISTANCE = 50f // AR指引显示距离(米)
}
```

---

## 6. 3D渲染架构

### 6.1 3D渲染架构图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         3D渲染架构                                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                         渲染层 (Render Layer)                        │   │
│  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐                │   │
│  │  │   MapView    │ │   ARView     │ │  LaneView    │                │   │
│  │  │  (地图视图)   │ │  (AR视图)    │ │  (车道视图)   │                │   │
│  │  └──────┬───────┘ └──────┬───────┘ └──────┬───────┘                │   │
│  └─────────┼────────────────┼────────────────┼────────────────────────┘   │
│            │                │                │                             │
│  ┌─────────┼────────────────┼────────────────┼────────────────────────┐   │
│  │         ▼                ▼                ▼                        │   │
│  │  ┌─────────────────────────────────────────────────────────────┐   │   │
│  │  │                    渲染引擎 (Render Engine)                  │   │   │
│  │  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐         │   │   │
│  │  │  │ OpenGL ES    │ │   Vulkan     │ │   Surface    │         │   │   │
│  │  │  │   (移动)     │ │  (高性能)    │ │  (相机预览)  │         │   │   │
│  │  │  └──────────────┘ └──────────────┘ └──────────────┘         │   │   │
│  │  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐         │   │   │
│  │  │  │ Shader管理   │ │  纹理管理    │ │  缓冲区管理  │         │   │   │
│  │  │  │ShaderManager │ │TextureManager│ │BufferManager │         │   │   │
│  │  │  └──────────────┘ └──────────────┘ └──────────────┘         │   │   │
│  │  └─────────────────────────────────────────────────────────────┘   │   │
│  └────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    资源层 (Resource Layer)                           │   │
│  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐                │   │
│  │  │   3D模型      │ │   纹理资源    │ │   着色器      │                │   │
│  │  │  (*.obj)     │ │  (*.png)     │ │  (*.glsl)    │                │   │
│  │  │ 建筑/地标    │ │ 贴图/图标    │ │ 顶点/片段    │                │   │
│  │  └──────────────┘ └──────────────┘ └──────────────┘                │   │
│  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐                │   │
│  │  │   AR资源      │ │   字体资源    │ │   特效资源    │                │   │
│  │  │ 指引箭头/线  │ │ 文字渲染     │ │ 粒子效果     │                │   │
│  │  └──────────────┘ └──────────────┘ └──────────────┘                │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.2 3D地图渲染流程

```kotlin
/**
 * 3D地图渲染引擎
 */
class MapRenderEngine(context: Context) : GLSurfaceView.Renderer {
    
    private val shaderManager = ShaderManager()
    private val textureManager = TextureManager(context)
    private val modelManager = ModelManager()
    
    // MVP矩阵
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    
    // 相机参数
    private var cameraPosition = Vector3(0f, 1000f, 0f) // 俯视角度
    private var cameraTarget = Vector3(0f, 0f, 0f)
    private var cameraUp = Vector3(0f, 0f, -1f)
    
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // 初始化OpenGL
        GLES30.glClearColor(0.9f, 0.9f, 0.9f, 1.0f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        
        // 初始化着色器
        shaderManager.loadShader("building", R.raw.building_vertex, R.raw.building_fragment)
        shaderManager.loadShader("road", R.raw.road_vertex, R.raw.road_fragment)
        
        // 加载资源
        loadResources()
    }
    
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        
        // 设置透视投影
        val ratio = width.toFloat() / height.toFloat()
        Matrix.perspectiveM(projectionMatrix, 0, 60f, ratio, 1f, 10000f)
    }
    
    override fun onDrawFrame(gl: GL10?) {
        // 清屏
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        
        // 更新视图矩阵
        Matrix.setLookAtM(viewMatrix, 0,
            cameraPosition.x, cameraPosition.y, cameraPosition.z,
            cameraTarget.x, cameraTarget.y, cameraTarget.z,
            cameraUp.x, cameraUp.y, cameraUp.z
        )
        
        // 渲染3D建筑
        renderBuildings()
        
        // 渲染道路
        renderRoads()
        
        // 渲染POI标记
        renderPoiMarkers()
    }
    
    private fun renderBuildings() {
        val shader = shaderManager.getShader("building")
        shader.use()
        
        // 设置 uniforms
        shader.setMat4("uProjection", projectionMatrix)
        shader.setMat4("uView", viewMatrix)
        shader.setVec3("uLightPos", Vector3(1000f, 2000f, 1000f))
        
        // 渲染建筑模型
        modelManager.getBuildings().forEach { building ->
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, building.x, 0f, building.z)
            Matrix.scaleM(modelMatrix, 0, 1f, building.height, 1f)
            
            Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0)
            Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)
            
            shader.setMat4("uMVP", mvpMatrix)
            shader.setMat4("uModel", modelMatrix)
            
            building.mesh.draw()
        }
    }
    
    private fun renderRoads() {
        val shader = shaderManager.getShader("road")
        shader.use()
        
        shader.setMat4("uProjection", projectionMatrix)
        shader.setMat4("uView", viewMatrix)
        
        // 渲染道路
        // ...
    }
    
    private fun renderPoiMarkers() {
        // 渲染POI标记
        // ...
    }
    
    private fun loadResources() {
        // 加载建筑模型
        modelManager.loadBuildingModels()
        // 加载纹理
        textureManager.loadTextures()
    }
    
    /**
     * 设置相机位置
     */
    fun setCameraPosition(lat: Double, lng: Double, tilt: Float, bearing: Float) {
        // 将经纬度转换为世界坐标
        val worldPos = CoordinateUtils.latLngToWorld(lat, lng)
        
        // 根据倾斜角和方位角计算相机位置
        val tiltRad = Math.toRadians(tilt.toDouble())
        val bearingRad = Math.toRadians(bearing.toDouble())
        
        val distance = 1000f // 相机距离
        val height = (distance * Math.sin(tiltRad)).toFloat()
        val groundDist = (distance * Math.cos(tiltRad)).toFloat()
        
        cameraPosition.x = worldPos.x + (groundDist * Math.sin(bearingRad)).toFloat()
        cameraPosition.y = height
        cameraPosition.z = worldPos.z + (groundDist * Math.cos(bearingRad)).toFloat()
        
        cameraTarget.x = worldPos.x
        cameraTarget.y = 0f
        cameraTarget.z = worldPos.z
    }
}
```

### 6.3 AR渲染引擎

```kotlin
/**
 * AR渲染引擎
 */
class ArRenderEngine(context: Context) : GLSurfaceView.Renderer {
    
    private val shaderManager = ShaderManager()
    private val guideArrowModel = GuideArrowModel()
    private val guideLineModel = GuideLineModel()
    
    // 相机预览纹理
    private var cameraTextureId: Int = 0
    
    // 导航数据
    private var navGuide: ArNavGuide? = null
    
    // 投影矩阵
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        
        // 创建相机预览纹理
        cameraTextureId = createExternalTexture()
        
        // 加载AR着色器
        shaderManager.loadShader("camera", R.raw.camera_vertex, R.raw.camera_fragment)
        shaderManager.loadShader("guide", R.raw.guide_vertex, R.raw.guide_fragment)
        
        // 初始化AR模型
        guideArrowModel.init()
        guideLineModel.init()
    }
    
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        
        // AR投影矩阵
        val fov = 60f
        val aspect = width.toFloat() / height.toFloat()
        Matrix.perspectiveM(projectionMatrix, 0, fov, aspect, 0.1f, 1000f)
    }
    
    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        
        // 1. 渲染相机预览背景
        renderCameraPreview()
        
        // 2. 渲染AR指引
        navGuide?.let { guide ->
            renderARGuide(guide)
        }
    }
    
    private fun renderCameraPreview() {
        val shader = shaderManager.getShader("camera")
        shader.use()
        
        // 绑定相机纹理
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId)
        shader.setInt("uCameraTexture", 0)
        
        // 绘制全屏四边形
        drawFullscreenQuad()
    }
    
    private fun renderARGuide(guide: ArNavGuide) {
        val shader = shaderManager.getShader("guide")
        shader.use()
        
        // 设置混合模式（实现半透明效果）
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        
        shader.setMat4("uProjection", projectionMatrix)
        shader.setMat4("uView", viewMatrix)
        
        // 根据引导类型渲染不同元素
        when (guide.type) {
            ArGuideType.ARROW -> {
                renderGuideArrow(guide)
            }
            ArGuideType.LINE -> {
                renderGuideLine(guide)
            }
            ArGuideType.COMBINED -> {
                renderGuideLine(guide)
                renderGuideArrow(guide)
            }
        }
        
        GLES30.glDisable(GLES30.GL_BLEND)
    }
    
    private fun renderGuideArrow(guide: ArNavGuide) {
        Matrix.setIdentityM(modelMatrix, 0)
        
        // 设置箭头位置和旋转
        Matrix.translateM(modelMatrix, 0, guide.position.x, guide.position.y, guide.position.z)
        Matrix.rotateM(modelMatrix, 0, guide.rotation.y, 0f, 1f, 0f)
        
        // 根据距离调整大小
        val scale = calculateScaleByDistance(guide.distance)
        Matrix.scaleM(modelMatrix, 0, scale, scale, scale)
        
        val shader = shaderManager.getShader("guide")
        shader.setMat4("uModel", modelMatrix)
        shader.setVec3("uColor", Vector3(0.0f, 0.6f, 1.0f)) // 蓝色箭头
        
        guideArrowModel.draw()
    }
    
    private fun renderGuideLine(guide: ArNavGuide) {
        // 渲染引导线
        val shader = shaderManager.getShader("guide")
        
        guide.linePoints.forEachIndexed { index, point ->
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, point.x, point.y, point.z)
            
            val scale = 1f - (index.toFloat() / guide.linePoints.size) * 0.5f
            Matrix.scaleM(modelMatrix, 0, scale, scale, scale)
            
            shader.setMat4("uModel", modelMatrix)
            shader.setVec3("uColor", Vector3(0.0f, 0.8f, 1.0f))
            
            guideLineModel.draw()
        }
    }
    
    /**
     * 更新导航指引数据
     */
    fun updateNavGuide(guide: ArNavGuide) {
        this.navGuide = guide
    }
    
    /**
     * 更新设备姿态
     */
    fun updateDeviceOrientation(rotationMatrix: FloatArray) {
        // 将旋转矩阵转换为视图矩阵
        System.arraycopy(rotationMatrix, 0, viewMatrix, 0, 16)
        Matrix.invertM(viewMatrix, 0, viewMatrix, 0)
    }
    
    private fun calculateScaleByDistance(distance: Float): Float {
        // 根据距离动态调整AR元素大小
        return when {
            distance < 10f -> 1.0f
            distance < 50f -> 0.8f
            distance < 100f -> 0.6f
            else -> 0.4f
        }
    }
    
    private fun createExternalTexture(): Int {
        val textures = IntArray(1)
        GLES30.glGenTextures(1, textures, 0)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0])
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        return textures[0]
    }
    
    private fun drawFullscreenQuad() {
        // 绘制全屏四边形
    }
    
    fun release() {
        guideArrowModel.release()
        guideLineModel.release()
        GLES30.glDeleteTextures(1, intArrayOf(cameraTextureId), 0)
    }
}

/**
 * AR导航指引
 */
data class ArNavGuide(
    val type: ArGuideType,
    val position: Vector3,
    val rotation: Vector3,
    val distance: Float,
    val linePoints: List<Vector3> = emptyList()
)

enum class ArGuideType {
    ARROW,  // 箭头指引
    LINE,   // 引导线
    COMBINED // 组合指引
}
```

---

## 7. 数据库设计

### 7.1 导航相关数据库表

```kotlin
/**
 * 导航数据库
 */
@Database(
    entities = [
        NavHistoryEntity::class,
        NavFavoriteEntity::class,
        OfflineMapEntity::class,
        NavSettingsEntity::class
    ],
    version = 1
)
abstract class NavDatabase : RoomDatabase() {
    abstract fun navHistoryDao(): NavHistoryDao
    abstract fun navFavoriteDao(): NavFavoriteDao
    abstract fun offlineMapDao(): OfflineMapDao
    abstract fun navSettingsDao(): NavSettingsDao
}

/**
 * 导航历史记录实体
 */
@Entity(tableName = "nav_history")
data class NavHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "user_id")
    val userId: Long = 0,
    
    @ColumnInfo(name = "start_name")
    val startName: String,
    
    @ColumnInfo(name = "start_lat")
    val startLat: Double,
    
    @ColumnInfo(name = "start_lng")
    val startLng: Double,
    
    @ColumnInfo(name = "end_name")
    val endName: String,
    
    @ColumnInfo(name = "end_lat")
    val endLat: Double,
    
    @ColumnInfo(name = "end_lng")
    val endLng: Double,
    
    @ColumnInfo(name = "end_address")
    val endAddress: String? = null,
    
    @ColumnInfo(name = "distance")
    val distance: Int = 0,
    
    @ColumnInfo(name = "duration")
    val duration: Int = 0,
    
    @ColumnInfo(name = "route_type")
    val routeType: Int = 0,
    
    @ColumnInfo(name = "start_time")
    val startTime: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "end_time")
    val endTime: Long? = null,
    
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,
    
    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,
    
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false
)

/**
 * 导航收藏实体
 */
@Entity(
    tableName = "nav_favorite",
    indices = [Index(value = ["user_id", "fav_type", "name"], unique = true)]
)
data class NavFavoriteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "user_id")
    val userId: Long = 0,
    
    @ColumnInfo(name = "fav_type")
    val favType: Int = 2, // 0=家, 1=公司, 2=常用, 3=自定义
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "address")
    val address: String? = null,
    
    @ColumnInfo(name = "lat")
    val lat: Double,
    
    @ColumnInfo(name = "lng")
    val lng: Double,
    
    @ColumnInfo(name = "poi_id")
    val poiId: String? = null,
    
    @ColumnInfo(name = "phone")
    val phone: String? = null,
    
    @ColumnInfo(name = "category")
    val category: String? = null,
    
    @ColumnInfo(name = "icon")
    val icon: String? = null,
    
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,
    
    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false,
    
    @ColumnInfo(name = "create_time")
    val createTime: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "update_time")
    val updateTime: Long = System.currentTimeMillis()
)

/**
 * 离线地图实体
 */
@Entity(tableName = "offline_map")
data class OfflineMapEntity(
    @PrimaryKey
    @ColumnInfo(name = "city_code")
    val cityCode: String,
    
    @ColumnInfo(name = "city_name")
    val cityName: String,
    
    @ColumnInfo(name = "size")
    val size: Long = 0,
    
    @ColumnInfo(name = "version")
    val version: String,
    
    @ColumnInfo(name = "local_path")
    val localPath: String? = null,
    
    @ColumnInfo(name = "download_time")
    val downloadTime: Long? = null,
    
    @ColumnInfo(name = "has_update")
    val hasUpdate: Boolean = false,
    
    @ColumnInfo(name = "latest_version")
    val latestVersion: String? = null
)

/**
 * 导航设置实体
 */
@Entity(
    tableName = "nav_settings",
    indices = [Index(value = ["user_id", "setting_key"], unique = true)]
)
data class NavSettingsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "user_id")
    val userId: Long = 0,
    
    @ColumnInfo(name = "setting_key")
    val settingKey: String,
    
    @ColumnInfo(name = "setting_value")
    val settingValue: String,
    
    @ColumnInfo(name = "setting_type")
    val settingType: Int = 0, // 0=字符串, 1=整数, 2=浮点, 3=布尔
    
    @ColumnInfo(name = "update_time")
    val updateTime: Long = System.currentTimeMillis()
)

/**
 * 导航历史DAO
 */
@Dao
interface NavHistoryDao {
    @Query("SELECT * FROM nav_history WHERE user_id = :userId AND is_deleted = 0 ORDER BY start_time DESC LIMIT :limit")
    suspend fun getHistoryList(userId: Long, limit: Int = 100): List<NavHistoryEntity>
    
    @Query("SELECT * FROM nav_history WHERE id = :id")
    suspend fun getHistoryById(id: Long): NavHistoryEntity?
    
    @Insert
    suspend fun insert(history: NavHistoryEntity): Long
    
    @Update
    suspend fun update(history: NavHistoryEntity)
    
    @Query("UPDATE nav_history SET is_deleted = 1 WHERE id = :id")
    suspend fun softDelete(id: Long)
    
    @Query("DELETE FROM nav_history WHERE start_time < :beforeTime")
    suspend fun deleteBefore(beforeTime: Long)
    
    @Query("SELECT * FROM nav_history WHERE user_id = :userId AND is_favorite = 1 AND is_deleted = 0 ORDER BY start_time DESC")
    suspend fun getFavoriteHistory(userId: Long): List<NavHistoryEntity>
}

/**
 * 导航收藏DAO
 */
@Dao
interface NavFavoriteDao {
    @Query("SELECT * FROM nav_favorite WHERE user_id = :userId ORDER BY fav_type ASC, sort_order ASC")
    suspend fun getFavorites(userId: Long): List<NavFavoriteEntity>
    
    @Query("SELECT * FROM nav_favorite WHERE user_id = :userId AND fav_type = :type")
    suspend fun getFavoritesByType(userId: Long, type: Int): List<NavFavoriteEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(favorite: NavFavoriteEntity): Long
    
    @Delete
    suspend fun delete(favorite: NavFavoriteEntity)
    
    @Query("SELECT * FROM nav_favorite WHERE is_synced = 0")
    suspend fun getUnsynced(): List<NavFavoriteEntity>
    
    @Query("UPDATE nav_favorite SET is_synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)
}
```

### 7.2 数据仓库实现

```kotlin
/**
 * 导航数据仓库
 */
class NavDataRepository @Inject constructor(
    private val navDatabase: NavDatabase
) {
    private val historyDao = navDatabase.navHistoryDao()
    private val favoriteDao = navDatabase.navFavoriteDao()
    
    /**
     * 保存导航历史
     */
    suspend fun saveNavHistory(route: Route, userId: Long = 0): Long {
        val entity = NavHistoryEntity(
            userId = userId,
            startName = route.startPoint.name,
            startLat = route.startPoint.latitude,
            startLng = route.startPoint.longitude,
            endName = route.endPoint.name,
            endLat = route.endPoint.latitude,
            endLng = route.endPoint.longitude,
            distance = route.distance,
            duration = route.duration,
            routeType = route.strategy.ordinal
        )
        return historyDao.insert(entity)
    }
    
    /**
     * 获取导航历史列表
     */
    suspend fun getNavHistory(userId: Long = 0, limit: Int = 100): List<NavHistory> {
        return historyDao.getHistoryList(userId, limit).map { it.toNavHistory() }
    }
    
    /**
     * 添加收藏
     */
    suspend fun addFavorite(favorite: NavFavorite, userId: Long = 0): Long {
        val entity = NavFavoriteEntity(
            userId = userId,
            favType = favorite.type.ordinal,
            name = favorite.name,
            address = favorite.address,
            lat = favorite.location.latitude,
            lng = favorite.location.longitude,
            poiId = favorite.poiId,
            phone = favorite.phone,
            category = favorite.category
        )
        return favoriteDao.insert(entity)
    }
    
    /**
     * 获取收藏列表
     */
    suspend fun getFavorites(userId: Long = 0): List<NavFavorite> {
        return favoriteDao.getFavorites(userId).map { it.toNavFavorite() }
    }
    
    /**
     * 清理过期历史记录
     */
    suspend fun cleanupOldHistory(daysToKeep: Int = 30) {
        val cutoffTime = System.currentTimeMillis() - daysToKeep * 24 * 60 * 60 * 1000
        historyDao.deleteBefore(cutoffTime)
    }
}
```

---

## 8. 接口设计

### 8.1 对外服务接口

```kotlin
/**
 * 导航服务接口
 * 提供给其他模块使用
 */
interface INavigationService : IInterface {
    companion object {
        const val DESCRIPTOR = "com.longcheer.cockpit.nav.INavigationService"
        const val TRANSACTION_START_NAVIGATION = IBinder.FIRST_CALL_TRANSACTION
        const val TRANSACTION_STOP_NAVIGATION = IBinder.FIRST_CALL_TRANSACTION + 1
        const val TRANSACTION_GET_CURRENT_LOCATION = IBinder.FIRST_CALL_TRANSACTION + 2
        const val TRANSACTION_SEARCH_LOCATION = IBinder.FIRST_CALL_TRANSACTION + 3
        const val TRANSACTION_REGISTER_LISTENER = IBinder.FIRST_CALL_TRANSACTION + 4
    }
    
    /**
     * 启动导航
     */
    fun startNavigation(destination: NavDestination, strategy: Int): String
    
    /**
     * 停止导航
     */
    fun stopNavigation(sessionId: String)
    
    /**
     * 获取当前位置
     */
    fun getCurrentLocation(): Location?
    
    /**
     * 搜索地点
     */
    fun searchLocation(keyword: String, callback: ISearchCallback)
    
    /**
     * 注册导航监听
     */
    fun registerNavigationListener(listener: INavigationListener)
}

/**
 * 导航监听接口
 */
interface INavigationListener : IInterface {
    fun onNavStatusChanged(status: Int)
    fun onNavInfoUpdated(info: NavInfo)
    fun onRouteCalculated(routes: List<Route>)
}

/**
 * 搜索回调接口
 */
interface ISearchCallback : IInterface {
    fun onResult(results: List<Poi>)
    fun onError(errorCode: Int, errorMsg: String)
}
```

### 8.2 AIDL定义

```aidl
// INavigationService.aidl
package com.longcheer.cockpit.nav;

import com.longcheer.cockpit.nav.NavDestination;
import com.longcheer.cockpit.nav.ISearchCallback;
import com.longcheer.cockpit.nav.INavigationListener;

interface INavigationService {
    String startNavigation(in NavDestination destination, int strategy);
    void stopNavigation(String sessionId);
    Location getCurrentLocation();
    void searchLocation(String keyword, ISearchCallback callback);
    void registerNavigationListener(INavigationListener listener);
}

// NavDestination.aidl
package com.longcheer.cockpit.nav;

parcelable NavDestination {
    double latitude;
    double longitude;
    String name;
    String address;
    String poiId;
}
```

---

## 9. 安全设计

### 9.1 数据安全

| 数据类型 | 安全策略 | 实现方式 |
|----------|----------|----------|
| 导航历史 | 本地存储加密 | AES-256加密敏感字段 |
| 用户收藏 | 访问权限控制 | 用户隔离，仅本人可访问 |
| 位置数据 | 传输加密 | HTTPS/TLS 1.3 |
| 离线地图 | 完整性校验 | SHA-256校验 |

### 9.2 功能安全

| 功能 | ASIL等级 | 安全机制 |
|------|----------|----------|
| 导航关键提示 | ASIL A | 消息优先级P1，强制显示 |
| 车道级指引 | QM | 视觉高亮显示 |
| AR实景导航 | QM | 辅助功能，不影响主驾驶 |

---

## 10. 需求追溯矩阵

### 10.1 功能需求追溯

| 需求ID | 需求描述 | 设计元素 | 实现类 | 测试用例 |
|--------|----------|----------|--------|----------|
| REQ-NAV-FUN-011 | 实时路况显示 | 3.2.1 MapManager.showTraffic() | MapManager.kt | TC-NAV-001 |
| REQ-NAV-FUN-011 | 多路线规划 | 3.2.2 NavigationManager.planRoute() | NavigationManager.kt | TC-NAV-002 |
| REQ-NAV-FUN-011 | 车道级导航 | 3.2.2 NavigationManager._laneInfo | NavigationManager.kt | TC-NAV-003 |
| REQ-NAV-FUN-011 | AR实景导航 | 3.2.3 ARManager | ARManager.kt | TC-NAV-004 |
| REQ-NAV-FUN-011 | 离线地图 | 3.2.4 OfflineMapManager | OfflineMapManager.kt | TC-NAV-005 |
| REQ-NAV-FUN-012 | 3D交互功能 | 6.2 MapRenderEngine | MapRenderEngine.kt | TC-NAV-006 |
| REQ-NAV-FUN-013 | 高德SDK集成 | 5.1 SDK集成架构 | AMapService.kt | TC-NAV-007 |
| REQ-PER-004 | 路线规划≤2s | 4.2 时序图优化 | RouteService.kt | TC-PER-001 |
| REQ-PER-005 | 地图缩放≤50ms | 6.2 GPU加速渲染 | MapRenderEngine.kt | TC-PER-002 |

### 10.2 接口需求追溯

| 需求ID | 接口定义 | 实现文件 | 调用方 |
|--------|----------|----------|--------|
| REQ-INT-SW-004 | INavigationService | INavigationService.aidl | MSG, AI, LCH |
| REQ-NAV-FUN-011 | 高德SDK API | AMapService.kt | NAV内部 |

### 10.3 性能需求追溯

| 需求ID | 性能指标 | 设计优化 | 验证方法 |
|--------|----------|----------|----------|
| REQ-PER-004 | 路线规划≤2s | 异步计算+缓存 | 性能测试 |
| REQ-PER-005 | 地图缩放≤50ms | GPU渲染+瓦片缓存 | 帧率监控 |
| REQ-PER-007 | 导航响应≤200ms | 消息队列优化 | 延迟测试 |

---

## 附录A: 开发规范

### A.1 代码规范
- 使用Kotlin语言开发
- 遵循Kotlin官方代码风格
- 使用MVVM架构模式
- 使用Hilt进行依赖注入

### A.2 命名规范
- 类名：PascalCase
- 函数名：camelCase
- 常量名：SCREAMING_SNAKE_CASE
- 包名：全小写，使用反向域名

### A.3 注释规范
- 类注释：包含作者、版本、功能描述
- 函数注释：参数、返回值、异常说明
- 复杂逻辑：行内注释说明

---

**文档结束**

*本地图导航模块详细设计文档符合ASPICE Level 3要求，建立了从需求到设计的完整追溯链。*

**编制**: 上海龙旗智能科技有限公司  
**审核**: [待填写]  
**批准**: [待填写]  
**日期**: 2024-06-20
