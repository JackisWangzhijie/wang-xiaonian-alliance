/**
 * 消息中心模块 - 领域模型包
 * ASIL等级: ASIL A (P0消息相关)
 * 
 * @author 智能座舱开发团队
 * @version 1.0
 * @since 2024-06-20
 */
package com.longcheer.cockpit.message.domain.model

import java.util.UUID

/**
 * 消息实体 - 领域层核心对象
 * ASIL等级: ASIL A (P0消息)
 * 需求追溯: REQ-MSG-FUN-001, REQ-MSG-FUN-003
 * 
 * @property id 消息唯一标识 (UUID)
 * @property sourceApp 来源应用ID
 * @property sourceName 来源应用名称
 * @property category 消息分类
 * @property priority 消息优先级
 * @property title 消息标题
 * @property content 消息内容
 * @property contentType 内容类型
 * @property actionType 动作类型
 * @property actionData 动作参数 (JSON)
 * @property iconUrl 图标URL
 * @property attachments 附件列表
 * @property userId 接收用户ID
 * @property isRead 是否已读
 * @property isDeleted 是否删除
 * @property readTime 阅读时间戳
 * @property expireTime 过期时间戳
 * @property createTime 创建时间戳
 * @property updateTime 更新时间戳
 */
data class Message(
    val id: String = UUID.randomUUID().toString(),
    val sourceApp: String,
    val sourceName: String = "",
    val category: MessageCategory = MessageCategory.OTHER,
    val priority: MessagePriority = MessagePriority.P2_MEDIUM,
    val title: String,
    val content: String = "",
    val contentType: ContentType = ContentType.TEXT,
    val actionType: ActionType = ActionType.NONE,
    val actionData: String? = null,
    val iconUrl: String? = null,
    val attachments: List<Attachment> = emptyList(),
    val userId: String = "0",
    val isRead: Boolean = false,
    val isDeleted: Boolean = false,
    val readTime: Long? = null,
    val expireTime: Long? = null,
    val createTime: Long = System.currentTimeMillis(),
    val updateTime: Long = System.currentTimeMillis()
) {
    
    /**
     * 判断消息是否过期
     * @return true表示已过期
     */
    fun isExpired(): Boolean {
        return expireTime != null && System.currentTimeMillis() > expireTime
    }
    
    /**
     * 判断是否为紧急消息 (需要立即显示)
     * @return true表示紧急消息
     */
    fun isUrgent(): Boolean {
        return priority == MessagePriority.P0_EMERGENCY
    }
    
    /**
     * 判断驾驶时是否可以显示
     * @return true表示驾驶时可显示
     */
    fun canShowWhileDriving(): Boolean {
        return priority == MessagePriority.P0_EMERGENCY || 
               priority == MessagePriority.P1_HIGH
    }
    
    /**
     * 获取消息摘要（用于日志和显示）
     */
    fun summary(): String = "Message[id=$id, source=$sourceApp, priority=$priority, title=$title]"
    
    companion object {
        /**
         * 创建紧急消息（P0级别）
         */
        fun createEmergency(
            sourceApp: String,
            title: String,
            content: String,
            sourceName: String = ""
        ): Message = Message(
            sourceApp = sourceApp,
            sourceName = sourceName,
            priority = MessagePriority.P0_EMERGENCY,
            category = MessageCategory.SECURITY,
            title = title,
            content = content
        )
        
        /**
         * 创建导航消息
         */
        fun createNavigation(
            title: String,
            content: String,
            actionData: String? = null
        ): Message = Message(
            sourceApp = "com.autonavi.amapauto",
            sourceName = "高德地图",
            priority = MessagePriority.P1_HIGH,
            category = MessageCategory.NAVIGATION,
            title = title,
            content = content,
            actionType = ActionType.NAVIGATE,
            actionData = actionData
        )
    }
}

/**
 * 消息优先级枚举
 * ASIL等级: ASIL A
 * 需求追溯: REQ-MSG-FUN-003
 * 
 * @property level 优先级等级数字（越小优先级越高）
 * @property displayTimeoutMs 显示超时时间（毫秒）
 */
enum class MessagePriority(
    val level: Int, 
    val displayTimeoutMs: Long,
    val displayName: String
) {
    /** 紧急: 安全告警，100ms内必须显示 */
    P0_EMERGENCY(0, 100, "紧急"),
    
    /** 高: 导航提示、来电，200ms内显示 */
    P1_HIGH(1, 200, "高"),
    
    /** 中: 普通通知，500ms内显示 */
    P2_MEDIUM(2, 500, "中"),
    
    /** 低: 资讯推送，1s内显示 */
    P3_LOW(3, 1000, "低");

    companion object {
        /**
         * 根据等级数字获取优先级
         * @param level 等级数字
         * @return 对应的优先级，默认返回P2_MEDIUM
         */
        fun fromLevel(level: Int): MessagePriority {
            return values().find { it.level == level } ?: P2_MEDIUM
        }
        
        /**
         * 获取驾驶时允许的优先级列表
         */
        fun drivingAllowed(): List<MessagePriority> = listOf(P0_EMERGENCY, P1_HIGH)
    }
}

/**
 * 消息分类枚举
 * 需求追溯: REQ-MSG-FUN-001
 * 
 * @property code 分类代码
 * @property displayName 显示名称
 */
enum class MessageCategory(
    val code: String, 
    val displayName: String
) {
    NAVIGATION("NAV", "导航"),
    PHONE("PHONE", "电话"),
    VEHICLE("CAR", "车辆"),
    SYSTEM("SYS", "系统"),
    MEDIA("MEDIA", "媒体"),
    SECURITY("SEC", "安全"),
    RECOMMENDATION("REC", "推荐"),
    SOCIAL("SOC", "社交"),
    OTHER("OTHER", "其他");

    companion object {
        /**
         * 根据代码获取分类
         * @param code 分类代码
         * @return 对应的分类，默认返回OTHER
         */
        fun fromCode(code: String): MessageCategory {
            return values().find { it.code.equals(code, ignoreCase = true) } ?: OTHER
        }
        
        /**
         * 获取系统预定义分类列表
         */
        fun systemCategories(): List<MessageCategory> = values().toList()
    }
}

/**
 * 内容类型枚举
 * 
 * @property value 类型值
 */
enum class ContentType(val value: Int) {
    TEXT(0),           // 纯文本
    RICH_TEXT(1),      // 富文本
    IMAGE_TEXT(2),     // 图文
    MULTIMEDIA(3);     // 多媒体

    companion object {
        /**
         * 根据值获取内容类型
         * @param value 类型值
         * @return 对应的内容类型，默认返回TEXT
         */
        fun fromValue(value: Int): ContentType {
            return values().find { it.value == value } ?: TEXT
        }
    }
}

/**
 * 动作类型枚举
 * 
 * @property value 类型值
 */
enum class ActionType(val value: Int) {
    NONE(0),           // 无动作
    NAVIGATE(1),       // 页面跳转
    POPUP(2),          // 弹窗显示
    EXTERNAL(3),       // 外部链接
    DISMISS(4);        // 仅消失

    companion object {
        /**
         * 根据值获取动作类型
         * @param value 类型值
         * @return 对应的动作类型，默认返回NONE
         */
        fun fromValue(value: Int): ActionType {
            return values().find { it.value == value } ?: NONE
        }
    }
}

/**
 * 附件实体
 * 
 * @property id 附件唯一标识
 * @property messageId 关联消息ID
 * @property fileName 文件名
 * @property filePath 文件路径
 * @property fileType 文件类型
 * @property fileSize 文件大小（字节）
 * @property mimeType MIME类型
 * @property thumbnailPath 缩略图路径
 * @property createTime 创建时间戳
 */
data class Attachment(
    val id: String = UUID.randomUUID().toString(),
    val messageId: String,
    val fileName: String,
    val filePath: String,
    val fileType: FileType = FileType.OTHER,
    val fileSize: Long = 0,
    val mimeType: String = "",
    val thumbnailPath: String? = null,
    val createTime: Long = System.currentTimeMillis()
)

/**
 * 文件类型枚举
 * 
 * @property value 类型值
 */
enum class FileType(val value: Int) {
    IMAGE(0),
    VIDEO(1),
    AUDIO(2),
    OTHER(3);

    companion object {
        /**
         * 根据值获取文件类型
         * @param value 类型值
         * @return 对应的文件类型，默认返回OTHER
         */
        fun fromValue(value: Int): FileType {
            return values().find { it.value == value } ?: OTHER
        }
    }
}
