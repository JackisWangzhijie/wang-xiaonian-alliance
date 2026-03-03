package com.longcheer.cockpit.nav.business.manager

import com.longcheer.cockpit.nav.model.TrafficStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 路况管理器
 * 负责实时路况数据的获取和更新
 * 
 * @author 龙旗智能导航团队
 * @version 1.0.0
 * @since 1.0.0
 */
@Singleton
class TrafficManager @Inject constructor() {
    
    companion object {
        private const val TAG = "TrafficManager"
        private const val TRAFFIC_UPDATE_INTERVAL = 60_000L // 路况更新间隔(60秒)
    }
    
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var trafficJob: Job? = null
    
    // 路况状态流
    private val _trafficStatus = MutableStateFlow<TrafficStatus>(TrafficStatus.UNKNOWN)
    val trafficStatus: StateFlow<TrafficStatus> = _trafficStatus.asStateFlow()
    
    // 路况数据
    private val _trafficData = MutableStateFlow<Map<String, TrafficSegment>>(emptyMap())
    val trafficData: StateFlow<Map<String, TrafficSegment>> = _trafficData.asStateFlow()
    
    /**
     * 开始路况更新
     */
    fun startTrafficUpdate() {
        if (trafficJob?.isActive == true) return
        
        trafficJob = coroutineScope.launch {
            while (isActive) {
                updateTrafficData()
                delay(TRAFFIC_UPDATE_INTERVAL)
            }
        }
    }
    
    /**
     * 停止路况更新
     */
    fun stopTrafficUpdate() {
        trafficJob?.cancel()
        trafficJob = null
    }
    
    /**
     * 立即更新路况
     */
    fun refreshTraffic() {
        coroutineScope.launch {
            updateTrafficData()
        }
    }
    
    /**
     * 获取指定路段的路况
     */
    fun getTrafficStatus(roadId: String): TrafficStatus {
        return _trafficData.value[roadId]?.status ?: TrafficStatus.UNKNOWN
    }
    
    /**
     * 设置路况数据
     */
    fun setTrafficData(segments: List<TrafficSegment>) {
        val dataMap = segments.associateBy { it.roadId }
        _trafficData.value = dataMap
        
        // 计算整体路况
        _trafficStatus.value = calculateOverallStatus(segments)
    }
    
    private suspend fun updateTrafficData() {
        try {
            // TODO: 从服务器获取路况数据
            // val data = trafficApi.getTrafficData()
            // setTrafficData(data)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun calculateOverallStatus(segments: List<TrafficSegment>): TrafficStatus {
        if (segments.isEmpty()) return TrafficStatus.UNKNOWN
        
        val congestedCount = segments.count { 
            it.status == TrafficStatus.CONGESTED || it.status == TrafficStatus.BLOCKED 
        }
        val slowCount = segments.count { it.status == TrafficStatus.SLOW }
        
        return when {
            congestedCount > segments.size * 0.3 -> TrafficStatus.CONGESTED
            congestedCount > segments.size * 0.1 || slowCount > segments.size * 0.3 -> TrafficStatus.SLOW
            else -> TrafficStatus.SMOOTH
        }
    }
    
    fun release() {
        stopTrafficUpdate()
        coroutineScope.cancel()
    }
}

/**
 * 路况路段数据
 */
data class TrafficSegment(
    val roadId: String,
    val roadName: String,
    val startLat: Double,
    val startLng: Double,
    val endLat: Double,
    val endLng: Double,
    val status: TrafficStatus,
    val speed: Int, // 平均速度(km/h)
    val congestionLevel: Int = 0 // 拥堵等级
)