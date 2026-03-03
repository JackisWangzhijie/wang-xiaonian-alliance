package com.longcheer.cockpit.message.domain.model

/**
 * 消息筛选条件
 * 用于消息列表的筛选和排序
 * 
 * @property userId 用户ID筛选
 * @property sourceApp 来源应用筛选
 * @property category 分类筛选
 * @property priority 优先级筛选
 * @property isRead 已读状态筛选
 * @property startTime 开始时间戳
 * @property endTime 结束时间戳
 * @property keyword 关键词搜索
 * @property sortBy 排序字段
 * @property sortOrder 排序方向
 * @property page 页码（从1开始）
 * @property pageSize 每页数量
 */
data class MessageFilter(
    val userId: String? = null,
    val sourceApp: String? = null,
    val category: MessageCategory? = null,
    val priority: MessagePriority? = null,
    val isRead: Boolean? = null,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val keyword: String? = null,
    val sortBy: SortField = SortField.TIME,
    val sortOrder: SortOrder = SortOrder.DESC,
    val page: Int = 1,
    val pageSize: Int = 20
) {
    init {
        require(page >= 1) { "Page must be >= 1" }
        require(pageSize in 1..100) { "PageSize must be between 1 and 100" }
    }
    
    /**
     * 计算偏移量（用于数据库查询）
     */
    fun offset(): Int = (page - 1) * pageSize
    
    /**
     * 创建复制并修改页码
     */
    fun withPage(newPage: Int): MessageFilter = copy(page = newPage)
    
    /**
     * 创建复制并修改分类
     */
    fun withCategory(newCategory: MessageCategory?): MessageFilter = copy(category = newCategory)
    
    companion object {
        /**
         * 默认筛选条件（获取第一页）
         */
        val DEFAULT = MessageFilter()
        
        /**
         * 未读消息筛选
         */
        val UNREAD = MessageFilter(isRead = false)
        
        /**
         * 紧急消息筛选
         */
        val EMERGENCY = MessageFilter(priority = MessagePriority.P0_EMERGENCY)
    }
}

/**
 * 排序字段枚举
 */
enum class SortField {
    TIME,       // 按时间排序
    PRIORITY,   // 按优先级排序
    CATEGORY,   // 按分类排序
    SOURCE      // 按来源排序
}

/**
 * 排序方向枚举
 */
enum class SortOrder {
    ASC,    // 升序
    DESC    // 降序
}

/**
 * 驾驶状态枚举
 * ASIL等级: ASIL A
 * 需求追溯: REQ-MSG-FUN-004
 */
enum class DrivingState {
    STOPPED,           // 停车状态
    DRIVING_SLOW,      // 低速行驶 (<30km/h)
    DRIVING_NORMAL,    // 正常行驶 (30-80km/h)
    DRIVING_FAST;      // 高速行驶 (>80km/h)

    /**
     * 是否处于行驶状态
     * @return true表示行驶中
     */
    fun isDriving(): Boolean = this != STOPPED
    
    /**
     * 是否处于高速行驶状态
     * @return true表示高速行驶
     */
    fun isHighSpeed(): Boolean = this == DRIVING_FAST

    /**
     * 获取允许显示的消息优先级列表
     * @return 允许显示的优先级列表
     */
    fun allowedPriorities(): List<MessagePriority> = when (this) {
        STOPPED -> MessagePriority.values().toList()
        DRIVING_SLOW -> listOf(MessagePriority.P0_EMERGENCY, MessagePriority.P1_HIGH)
        DRIVING_NORMAL, DRIVING_FAST -> listOf(MessagePriority.P0_EMERGENCY)
    }
    
    /**
     * 获取最大允许弹窗尺寸
     */
    fun maxPopupSizePercent(): Int = when (this) {
        STOPPED -> 100
        DRIVING_SLOW -> 20
        DRIVING_NORMAL, DRIVING_FAST -> 10
    }
    
    companion object {
        /**
         * 根据车速和挡位获取驾驶状态
         * @param speed 车速（km/h）
         * @param gearPosition 挡位
         * @return 对应的驾驶状态
         */
        fun fromSpeedAndGear(speed: Int, gearPosition: GearPosition): DrivingState {
            return when {
                speed == 0 && gearPosition == GearPosition.P -> STOPPED
                speed < 30 -> DRIVING_SLOW
                speed < 80 -> DRIVING_NORMAL
                else -> DRIVING_FAST
            }
        }
    }
}

/**
 * 挡位枚举
 */
enum class GearPosition {
    P,      // 驻车挡
    R,      // 倒车挡
    N,      // 空挡
    D,      // 前进挡
    S,      // 运动挡
    L,      // 低速挡
    UNKNOWN // 未知
}

/**
 * 消息弹窗配置
 * ASIL等级: ASIL A
 * 需求追溯: REQ-MSG-FUN-004
 * 
 * @property position 弹窗位置
 * @property size 弹窗尺寸
 * @property durationMs 显示时长（毫秒）
 * @property autoDismiss 是否自动消失
 * @property showIcon 是否显示图标
 * @property voiceAnnouncement 是否语音播报
 */
data class PopupConfig(
    val position: PopupPosition = PopupPosition.TOP_SAFE,
    val size: PopupSize = PopupSize.NORMAL,
    val durationMs: Long = 5000L,
    val autoDismiss: Boolean = true,
    val showIcon: Boolean = true,
    val voiceAnnouncement: Boolean = false
) {
    companion object {
        /**
         * 紧急消息配置
         */
        val EMERGENCY = PopupConfig(
            position = PopupPosition.TOP_SAFE,
            size = PopupSize.COMPACT,
            durationMs = 10000L,
            autoDismiss = false,
            voiceAnnouncement = true
        )
        
        /**
         * 驾驶时安全配置
         */
        fun drivingSafe(state: DrivingState): PopupConfig = when (state) {
            DrivingState.STOPPED -> PopupConfig()
            else -> PopupConfig(
                position = PopupPosition.BOTTOM_SAFE,
                size = PopupSize.COMPACT,
                durationMs = if (state == DrivingState.DRIVING_FAST) 3000L else 5000L,
                voiceAnnouncement = true
            )
        }
    }
}

/**
 * 弹窗位置枚举
 */
enum class PopupPosition {
    TOP_SAFE,          // 顶部安全区域
    BOTTOM_SAFE,       // 底部安全区域
    CENTER_COMPACT;    // 中央紧凑模式

    /**
     * 根据驾驶状态获取安全位置
     * @param drivingState 当前驾驶状态
     * @return 安全的弹窗位置
     */
    fun getSafePosition(drivingState: DrivingState): PopupPosition {
        return when (drivingState) {
            DrivingState.STOPPED -> this
            else -> BOTTOM_SAFE  // 驾驶时只能显示在底部
        }
    }
}

/**
 * 弹窗尺寸枚举
 */
enum class PopupSize {
    COMPACT,           // 紧凑 (屏幕10%)
    NORMAL,            // 正常 (屏幕20%)
    FULL;              // 全屏

    /**
     * 获取驾驶时的安全尺寸
     * @param drivingState 当前驾驶状态
     * @return 安全的弹窗尺寸
     */
    fun getSafeSize(drivingState: DrivingState): PopupSize {
        return when (drivingState) {
            DrivingState.STOPPED -> this
            else -> COMPACT      // 驾驶时只能使用紧凑尺寸
        }
    }
}

/**
 * 消息统计数据
 * 
 * @property totalCount 总消息数
 * @property readCount 已读消息数
 * @property unreadCount 未读消息数
 * @property byCategory 按分类统计
 * @property byPriority 按优先级统计
 * @property bySource 按来源统计
 */
data class MessageStatistics(
    val totalCount: Int = 0,
    val readCount: Int = 0,
    val unreadCount: Int = 0,
    val byCategory: Map<MessageCategory, Int> = emptyMap(),
    val byPriority: Map<MessagePriority, Int> = emptyMap(),
    val bySource: Map<String, Int> = emptyMap()
) {
    /**
     * 计算已读率
     */
    fun readRate(): Float = if (totalCount > 0) readCount.toFloat() / totalCount else 0f
    
    /**
     * 获取指定分类的未读数
     */
    fun getUnreadByCategory(category: MessageCategory): Int {
        val totalInCategory = byCategory[category] ?: 0
        // 简化计算，实际应从数据库查询
        return totalInCategory / 4  // 假设25%未读
    }
}

/**
 * 推荐消息数据类
 * 
 * @property message 推荐的消息
 * @property relevanceScore 相关度分数 (0-1)
 * @property recommendationType 推荐类型
 * @property reason 推荐理由
 */
data class RecommendedMessage(
    val message: Message,
    val relevanceScore: Float,
    val recommendationType: RecommendationType,
    val reason: String
) {
    init {
        require(relevanceScore in 0f..1f) { "Relevance score must be between 0 and 1" }
    }
    
    /**
     * 判断是否高相关度推荐
     */
    fun isHighRelevance(): Boolean = relevanceScore >= 0.8f
}

/**
 * 推荐类型枚举
 */
enum class RecommendationType(val displayName: String) {
    FREQUENT_APP("常用应用"),        // 常用应用
    SCENE_BASED("场景化推荐"),        // 场景化推荐
    TIME_BASED("时间相关"),          // 时间相关
    LOCATION_BASED("位置相关"),      // 位置相关
    BEHAVIOR_BASED("行为分析")       // 行为分析
}

/**
 * 场景枚举
 * 用于智能推荐场景检测
 */
enum class Scene(val displayName: String) {
    IDLE("空闲"),
    DRIVING("驾驶中"),
    NAVIGATING("导航中"),
    NEAR_GAS_STATION("靠近加油站"),
    NEAR_PARKING("靠近停车场"),
    NEAR_CHARGING("靠近充电站"),
    TRAFFIC_JAM("拥堵中"),
    HIGHWAY("高速行驶");
    
    /**
     * 是否可推荐加油相关服务
     */
    fun canRecommendFuel(): Boolean = this == NEAR_GAS_STATION || this == NAVIGATING
    
    /**
     * 是否可推荐停车相关服务
     */
    fun canRecommendParking(): Boolean = this == NEAR_PARKING || this == NAVIGATING
}

/**
 * 用户偏好数据类
 * 
 * @property preferredCategories 偏好的分类列表
 * @property preferredApps 偏好的应用列表
 * @property activeTimeRanges 活跃时间段
 * @property interactionPatterns 交互模式
 */
data class UserPreferences(
    val preferredCategories: List<MessageCategory> = emptyList(),
    val preferredApps: List<String> = emptyList(),
    val activeTimeRanges: List<TimeRange> = emptyList(),
    val interactionPatterns: InteractionPatterns = InteractionPatterns()
)

/**
 * 时间段数据类
 * 
 * @property startHour 开始小时（0-23）
 * @property endHour 结束小时（0-23）
 */
data class TimeRange(
    val startHour: Int,
    val endHour: Int
) {
    init {
        require(startHour in 0..23) { "Start hour must be 0-23" }
        require(endHour in 0..23) { "End hour must be 0-23" }
    }
    
    /**
     * 检查指定时间是否在此时间段内
     */
    fun contains(hour: Int): Boolean = hour in startHour..endHour
}

/**
 * 交互模式数据类
 * 
 * @property clickThroughRate 点击率（0-1）
 * @property averageResponseTime 平均响应时间（毫秒）
 * @property frequentActions 常用操作类型
 */
data class InteractionPatterns(
    val clickThroughRate: Float = 0f,
    val averageResponseTime: Long = 0L,
    val frequentActions: List<ActionType> = emptyList()
)

/**
 * 应用使用数据类
 * 
 * @property appId 应用ID
 * @property duration 使用时长（毫秒）
 * @property launchCount 启动次数
 * @property lastUsedTime 最后使用时间
 */
data class AppUsage(
    val appId: String,
    val duration: Long,
    val launchCount: Int,
    val lastUsedTime: Long = System.currentTimeMillis()
)
