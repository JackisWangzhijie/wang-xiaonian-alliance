/**
 * 智能座舱 - 地图导航模块
 * 数据模型定义
 * 
 * @author 龙旗智能导航团队
 * @version 1.0.0
 */

package com.longcheer.cockpit.nav.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 经纬度坐标
 */
@Parcelize
data class LatLng(
    val latitude: Double,
    val longitude: Double
) : Parcelable {
    companion object {
        val EMPTY = LatLng(0.0, 0.0)
    }
}

/**
 * 导航点
 * @property latitude 纬度
 * @property longitude 经度
 * @property name 地点名称
 * @property address 详细地址
 * @property poiId POI唯一标识
 */
@Parcelize
data class NavPoint(
    val latitude: Double,
    val longitude: Double,
    val name: String = "",
    val address: String = "",
    val poiId: String? = null
) : Parcelable {
    fun toLatLng(): LatLng = LatLng(latitude, longitude)
    
    companion object {
        fun fromLatLng(latLng: LatLng, name: String = "", address: String = ""): NavPoint {
            return NavPoint(latLng.latitude, latLng.longitude, name, address)
        }
    }
}

/**
 * 路线
 * @property id 路线唯一标识
 * @property startPoint 起点
 * @property endPoint 终点
 * @property waypoints 途经点列表
 * @property distance 总距离(米)
 * @property duration 总时长(秒)
 * @property tollCost 过路费(元)
 * @property trafficLights 红绿灯数量
 * @property steps 路线步骤列表
 * @property bounds 路线边界
 * @property trafficStatus 路况状态
 * @property strategy 路线策略
 */
@Parcelize
data class Route(
    val id: String,
    val startPoint: NavPoint,
    val endPoint: NavPoint,
    val waypoints: List<NavPoint> = emptyList(),
    val distance: Int,
    val duration: Int,
    val tollCost: Int,
    val trafficLights: Int,
    val steps: List<RouteStep>,
    val bounds: LatLngBounds,
    val trafficStatus: TrafficStatus,
    val strategy: RouteStrategy
) : Parcelable

/**
 * 路线步骤
 * @property index 步骤索引
 * @property instruction 导航指令文本
 * @property distance 步骤距离(米)
 * @property duration 步骤时长(秒)
 * @property roadName 道路名称
 * @property action 转向动作
 * @property polyline 路径点列表
 * @property trafficConditions 路况条件列表
 */
@Parcelize
data class RouteStep(
    val index: Int,
    val instruction: String,
    val distance: Int,
    val duration: Int,
    val roadName: String,
    val action: TurnAction,
    val polyline: List<LatLng>,
    val trafficConditions: List<TrafficCondition>
) : Parcelable

/**
 * 路线规划结果
 * @property recommendedRoute 推荐路线
 * @property alternativeRoutes 备选路线列表
 * @property queryTime 查询耗时(毫秒)
 */
@Parcelize
data class RoutePlanResult(
    val recommendedRoute: Route,
    val alternativeRoutes: List<Route>,
    val queryTime: Long
) : Parcelable

/**
 * 导航信息
 * @property currentLocation 当前位置
 * @property currentSpeed 当前速度(km/h)
 * @property remainingDistance 剩余距离(米)
 * @property remainingTime 剩余时间(秒)
 * @property nextTurnDistance 下个转向距离(米)
 * @property nextTurnType 下个转向类型
 * @property guideText 引导文本
 * @property isKeyPoint 是否关键引导点
 * @property alertType 告警类型
 * @property currentRoadName 当前道路名称
 * @property nextRoadName 下条道路名称
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
 * 位置信息
 * @property latitude 纬度
 * @property longitude 经度
 * @property altitude 海拔(米)
 * @property accuracy 精度(米)
 * @property bearing 方位角
 * @property speed 速度(m/s)
 * @property timestamp 时间戳
 */
data class Location(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val accuracy: Float = 0f,
    val bearing: Float = 0f,
    val speed: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 车道信息
 * @property laneCount 车道总数
 * @property lanes 车道列表
 * @property recommendedLanes 推荐车道索引列表
 */
data class LaneInfo(
    val laneCount: Int,
    val lanes: List<Lane>,
    val recommendedLanes: List<Int>
)

/**
 * 车道
 * @property index 车道索引
 * @property type 车道类型
 * @property directions 转向方向列表
 * @property isRecommended 是否为推荐车道
 */
@Parcelize
data class Lane(
    val index: Int,
    val type: LaneType,
    val directions: List<TurnDirection>,
    val isRecommended: Boolean
) : Parcelable

/**
 * POI(兴趣点)
 * @property id POI唯一标识
 * @property name POI名称
 * @property type POI类型
 * @property location 位置坐标
 * @property address 详细地址
 * @property phone 联系电话
 * @property distance 距离(米)
 * @property rating 评分(0-5)
 * @property price 人均消费(元)
 * @property businessHours 营业时间
 * @property photos 图片URL列表
 * @property isOpen 是否营业中
 */
@Parcelize
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
) : Parcelable

/**
 * POI搜索参数
 * @property keyword 搜索关键词
 * @property category POI类别
 * @property location 中心位置
 * @property radius 搜索半径(米)
 * @property city 城市名称
 * @property sortRule 排序规则
 * @property page 页码
 * @property pageSize 每页数量
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
 * 地图标记
 * @property id 标记ID
 * @property position 位置坐标
 * @property title 标题
 * @property snippet 描述
 * @property icon 图标资源
 * @property draggable 是否可拖拽
 */
data class MapMarker(
    val id: String,
    val position: LatLng,
    val title: String = "",
    val snippet: String = "",
    val icon: Int? = null,
    val draggable: Boolean = false
)

/**
 * 坐标边界
 * @property northeast 东北角坐标
 * @property southwest 西南角坐标
 */
@Parcelize
data class LatLngBounds(
    val northeast: LatLng,
    val southwest: LatLng
) : Parcelable

/**
 * AR导航信息
 * @property turnType 转向类型
 * @property distance 距离转向点距离(米)
 * @property guideText 引导文本
 * @property arrowPosition 指引箭头屏幕位置
 * @property laneInfo AR车道信息
 * @property safetyAlert 安全告警
 */
data class ArNavInfo(
    val turnType: TurnType,
    val distance: Int,
    val guideText: String,
    val arrowPosition: PointF?,
    val laneInfo: ArLaneInfo?,
    val safetyAlert: SafetyAlert?
)

/**
 * 3D向量
 */
data class Vector3(
    var x: Float = 0f,
    var y: Float = 0f,
    var z: Float = 0f
)

/**
 * 2D点
 */
data class PointF(
    val x: Float,
    val y: Float
)

/**
 * 路况条件
 */
@Parcelize
data class TrafficCondition(
    val startIndex: Int,
    val endIndex: Int,
    val status: TrafficStatus
) : Parcelable

/**
 * AR车道信息
 */
data class ArLaneInfo(
    val laneCount: Int,
    val currentLane: Int,
    val recommendedLanes: List<Int>
)

/**
 * 安全告警
 */
enum class SafetyAlert {
    NONE,
    PEDESTRIAN,
    VEHICLE,
    LANE_DEPARTURE,
    SPEED_LIMIT
}

// ==================== 枚举类型定义 ====================

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
 * 地图类型
 */
enum class MapType {
    NORMAL,     // 标准地图
    SATELLITE,  // 卫星地图
    NIGHT,      // 夜间地图
    NAVI        // 导航地图
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
    val scaleControlsEnabled: Boolean = true,
    val minZoom: Float = 3f,
    val maxZoom: Float = 20f,
    val defaultZoom: Float = 15f
)

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
 * 转向动作
 */
enum class TurnAction {
    STRAIGHT,
    TURN_LEFT,
    TURN_RIGHT,
    UTURN,
    SLIGHT_LEFT,
    SLIGHT_RIGHT,
    ENTER_ROUNDABOUT,
    EXIT_ROUNDABOUT,
    MERGE,
    DESTINATION
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
 * 排序规则
 */
enum class SortRule {
    DISTANCE,   // 距离优先
    RATING,     // 评分优先
    PRICE_ASC,  // 价格从低到高
    PRICE_DESC  // 价格从高到低
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
 * AR指引样式
 */
enum class ArGuideStyle {
    ARROW,      // 箭头指引
    GUIDE_LINE, // 引导线
    COMBINED    // 组合指引
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

/**
 * AR指引类型
 */
enum class ArGuideType {
    ARROW,  // 箭头指引
    LINE,   // 引导线
    COMBINED // 组合指引
}

/**
 * 离线状态
 */
sealed class OfflineState {
    object Idle : OfflineState()
    object Loading : OfflineState()
    data class Downloading(val progress: Int) : OfflineState()
    object Completed : OfflineState()
    data class Error(val message: String) : OfflineState()
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

/**
 * 导航偏好设置
 */
data class NavPreference(
    val avoidHighway: Boolean = false,
    val avoidToll: Boolean = false,
    val avoidCongestion: Boolean = true,
    val priorityHighway: Boolean = false,
    val multiRoute: Boolean = true,
    val voiceGuide: Boolean = true,
    val voiceType: VoiceType = VoiceType.NORMAL,
    val nightMode: NightMode = NightMode.AUTO
)

/**
 * 语音类型
 */
enum class VoiceType {
    NORMAL,
    SWEET,
    STRICT
}

/**
 * 夜间模式
 */
enum class NightMode {
    AUTO,
    DAY,
    NIGHT
}

/**
 * 导航收藏
 */
data class NavFavorite(
    val id: Long = 0,
    val type: FavoriteType,
    val name: String,
    val address: String?,
    val location: LatLng,
    val poiId: String?,
    val phone: String?,
    val category: String?
)

/**
 * 收藏类型
 */
enum class FavoriteType {
    HOME,       // 家
    COMPANY,    // 公司
    FREQUENT,   // 常用
    CUSTOM      // 自定义
}

/**
 * 导航历史
 */
data class NavHistory(
    val id: Long = 0,
    val startName: String,
    val startLat: Double,
    val startLng: Double,
    val endName: String,
    val endLat: Double,
    val endLng: Double,
    val endAddress: String?,
    val distance: Int,
    val duration: Int,
    val startTime: Long,
    val isCompleted: Boolean,
    val isFavorite: Boolean
)