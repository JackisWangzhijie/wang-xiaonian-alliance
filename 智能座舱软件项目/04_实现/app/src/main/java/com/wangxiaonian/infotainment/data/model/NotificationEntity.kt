/**
 * 通知数据实体
 * 对应数据库表结构
 * 
 * @author 王小年联盟
 * @version 1.0
 */
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val title: String,
    val content: String,
    val summary: String? = null, // TTS 朗读摘要
    
    val category: NotificationCategory,
    val priority: NotificationPriority,
    
    val source: String, // 来源应用/系统
    val iconRes: Int? = null,
    
    val isRead: Boolean = false,
    val isDismissed: Boolean = false,
    
    val actionType: ActionType? = null,
    val actionData: String? = null,
    
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null
)

/**
 * 通知类别
 */
enum class NotificationCategory {
    VEHICLE,    // 车辆相关 (故障、保养等)
    NAVIGATION, // 导航相关
    MEDIA,      // 媒体相关
    COMMUNICATION, // 通讯相关
    SYSTEM,     // 系统相关
    GENERAL     // 一般通知
}

/**
 * 通知优先级
 */
enum class NotificationPriority(val level: Int) {
    CRITICAL(4), // 紧急 (立即朗读)
    HIGH(3),     // 高 (震动 + 提示音)
    NORMAL(2),   // 正常 (提示音)
    LOW(1)       // 低 (静默)
}

/**
 * 操作类型
 */
enum class ActionType {
    OPEN_APP,
    NAVIGATE,
    CALL,
    REPLY,
    DISMISS,
    CUSTOM
}
