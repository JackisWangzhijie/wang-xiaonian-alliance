# 消息中心模块详细设计文档
## Detailed Design Document - Message Center Module

**项目名称**: 2024年智能座舱软件主交互开发  
**文档版本**: V1.0  
**编制日期**: 2024-06-20  
**编制单位**: 上海龙旗智能科技有限公司  
**客户单位**: 奇瑞汽车股份有限公司  
**ASPICE等级**: Level 3  
**符合标准**: ASPICE 3.1, ISO 26262, ISO/SAE 21434  
**ASIL等级**: ASIL A (P0消息弹窗相关)

---

## 文档控制信息

### 版本历史
| 版本 | 日期 | 作者 | 变更描述 | 审批 |
|------|------|------|----------|------|
| V0.1 | 2024-06-18 | 详细设计工程师 | 初稿编制 | - |
| V0.5 | 2024-06-19 | 系统架构师 | 架构评审后修订 | - |
| V1.0 | 2024-06-20 | 详细设计工程师 | 基线版本 | 项目总监 |

### 参考文档
1. 《SRS_智能座舱主交互系统_V1.0.md》
2. 《HLD_概要设计文档_V1.0.md》
3. 《数据库设计文档_V1.0.md》
4. 《消息中心模块软件需求规格说明书》

---

## 目录

1. [引言](#1-引言)
2. [模块架构设计](#2-模块架构设计)
3. [核心类设计](#3-核心类设计)
4. [时序图设计](#4-时序图设计)
5. [接口实现细节](#5-接口实现细节)
6. [数据库访问层设计](#6-数据库访问层设计)
7. [UI层设计](#7-ui层设计)
8. [安全设计](#8-安全设计)
9. [性能设计](#9-性能设计)
10. [需求追溯矩阵](#10-需求追溯矩阵)

---

## 1. 引言

### 1.1 目的
本文档基于HLD概要设计和SRS需求，定义消息中心模块的详细设计，为编码实现提供技术规范。

### 1.2 范围
涵盖消息中心模块的类设计、接口实现、数据库访问、时序逻辑、安全机制和性能优化策略。

### 1.3 设计约束
- 编程语言: Kotlin 1.7+ (主), Java 11 (兼容)
- 架构模式: MVVM + Clean Architecture
- UI框架: Jetpack Compose 1.3+
- 数据库: SQLite 3.36+ with Room
- ASIL等级: ASIL A (消息弹窗、P0优先级处理)

---

## 2. 模块架构设计

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              应用层 (Presentation)                          │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐             │
│  │ MessageListPage │  │ MessagePopup    │  │ NotificationBar │             │
│  │ (消息列表页)     │  │ (消息弹窗)       │  │ (通知栏)        │             │
│  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘             │
│           │                    │                    │                      │
│           └────────────────────┼────────────────────┘                      │
│                                │                                           │
│  ┌─────────────────────────────┴─────────────────────────────┐             │
│  │                    MessageViewModel                         │             │
│  │              (消息状态管理 - Compose State)                  │             │
│  └─────────────────────────────┬─────────────────────────────┘             │
└────────────────────────────────┼───────────────────────────────────────────┘
                                 │
┌────────────────────────────────┼───────────────────────────────────────────┐
│                           领域层 (Domain)                                   │
│  ┌─────────────────────────────┼─────────────────────────────┐             │
│  │                       MessageUseCases                        │             │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐          │             │
│  │  │GetMessages  │ │SendMessage  │ │MarkAsRead   │          │             │
│  │  │UseCase      │ │UseCase      │ │UseCase      │          │             │
│  │  └─────────────┘ └─────────────┘ └─────────────┘          │             │
│  └─────────────────────────────┬─────────────────────────────┘             │
│                                │                                           │
│  ┌─────────────────────────────┼─────────────────────────────┐             │
│  │                      MessageRepository                       │             │
│  │              (仓库接口定义 - 抽象数据访问)                     │             │
│  │  ┌─────────────────────────────────────────────────────┐   │             │
│  │  │  interface IMessageRepository {                     │   │             │
│  │  │    fun getMessages(filter: MessageFilter): Flow     │   │             │
│  │  │    suspend fun sendMessage(msg: Message): Result    │   │             │
│  │  │    suspend fun markAsRead(id: String): Result       │   │             │
│  │  │  }                                                  │   │             │
│  │  └─────────────────────────────────────────────────────┘   │             │
│  └─────────────────────────────┬─────────────────────────────┘             │
└────────────────────────────────┼───────────────────────────────────────────┘
                                 │
┌────────────────────────────────┼───────────────────────────────────────────┐
│                           数据层 (Data)                                     │
│  ┌─────────────────────────────┼─────────────────────────────┐             │
│  │                   MessageRepositoryImpl                      │             │
│  │              (仓库实现 - 协调本地和远程数据源)                  │             │
│  └──────────────┬──────────────┴──────────────┬──────────────┘             │
│                 │                             │                            │
│  ┌──────────────┴──────────────┐ ┌────────────┴─────────────┐              │
│  │    LocalDataSource          │ │    RemoteDataSource      │              │
│  │  ┌───────────────────────┐  │ │  ┌──────────────────┐    │              │
│  │  │ MessageDao (Room)     │  │ │  │ MessageApi       │    │              │
│  │  │ MessageCategoryDao    │  │ │  │ CloudSyncService │    │              │
│  │  │ AttachmentDao         │  │ │  │ PushService      │    │              │
│  │  └───────────────────────┘  │ │  └──────────────────┘    │              │
│  └─────────────────────────────┘ └──────────────────────────┘              │
└────────────────────────────────────────────────────────────────────────────┘
                                 │
┌────────────────────────────────┼───────────────────────────────────────────┐
│                           服务层 (Service)                                  │
│  ┌─────────────────────────────┼─────────────────────────────┐             │
│  │                    MessageService (SystemService)            │             │
│  │  ┌─────────────────────────────────────────────────────┐   │             │
│  │  │  - 消息路由分发                                      │   │             │
│  │  │  - 优先级管理 (ASIL A)                               │   │             │
│  │  │  - 弹窗控制 (ASIL A)                                 │   │             │
│  │  │  - 驾驶安全检测                                      │   │             │
│  │  └─────────────────────────────────────────────────────┘   │             │
│  └─────────────────────────────┬─────────────────────────────┘             │
│                                │                                           │
│  ┌─────────────────────────────┼─────────────────────────────┐             │
│  │              DrivingRestrictionMonitor (ASIL A)              │             │
│  │  ┌─────────────────────────────────────────────────────┐   │             │
│  │  │  - 车速监听 (通过IVehicleService)                     │   │             │
│  │  │  - 挡位监听                                          │   │             │
│  │  │  - 驾驶模式判断                                       │   │             │
│  │  │  - 弹窗安全策略                                       │   │             │
│  │  └─────────────────────────────────────────────────────┘   │             │
│  └────────────────────────────────────────────────────────────┘             │
└────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 组件关系图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           消息中心模块组件关系图                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌──────────────┐         ┌──────────────┐         ┌──────────────┐       │
│   │ MessageInput │         │  Message     │         │ MessageOutput│       │
│   │  (消息输入)   │────────▶│  Router      │────────▶│  (消息输出)   │       │
│   └──────────────┘         │  (消息路由)   │         └──────────────┘       │
│          │                 └──────┬───────┘                │               │
│          │                        │                        │               │
│          ▼                        ▼                        ▼               │
│   ┌──────────────┐         ┌──────────────┐         ┌──────────────┐       │
│   │ - 导航应用    │         │ - 优先级计算  │         │ - 弹窗显示    │       │
│   │ - 电话应用    │         │ - 去重处理   │         │ - 通知栏     │       │
│   │ - 车辆系统    │         │ - 分类存储   │         │ - 语音播报    │       │
│   │ - 系统应用    │         │ - 时间戳排序  │         │ - 历史记录    │       │
│   │ - 第三方应用  │         │              │         │ - 智能推荐    │       │
│   └──────────────┘         └──────────────┘         └──────────────┘       │
│                                                                             │
│   ┌──────────────────────────────────────────────────────────────────┐     │
│   │                      优先级队列 (Priority Queue)                   │     │
│   │  ┌────────────────────────────────────────────────────────────┐  │     │
│   │  │  P0紧急队列 │ P1高优先级队列 │ P2中优先级队列 │ P3低优先级队列 │  │     │
│   │  │  (立即处理) │   (队列显示)   │   (静默通知)   │   (延迟推送)   │  │     │
│   │  └────────────────────────────────────────────────────────────┘  │     │
│   └──────────────────────────────────────────────────────────────────┘     │
│                                                                             │
│   ┌──────────────────────────────────────────────────────────────────┐     │
│   │                      驾驶安全控制器 (ASIL A)                       │     │
│   │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌──────────┐ │     │
│   │  │ 车速检测    │  │ 弹窗位置    │  │ 语音替代    │  │ 自动过滤 │ │     │
│   │  │ >0km/h     │  │ 安全区域    │  │ P0/P1消息   │  │ P2/P3消息│ │     │
│   │  └─────────────┘  └─────────────┘  └─────────────┘  └──────────┘ │     │
│   └──────────────────────────────────────────────────────────────────┘     │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. 核心类设计

### 3.1 领域模型 (Domain Model)

```kotlin
/**
 * 消息实体 - 领域层核心对象
 * ASIL等级: ASIL A (P0消息)
 * 需求追溯: REQ-MSG-FUN-001, REQ-MSG-FUN-003
 */
data class Message(
    val id: String,                          // 消息唯一标识 (UUID)
    val sourceApp: String,                   // 来源应用ID
    val sourceName: String,                  // 来源应用名称
    val category: MessageCategory,           // 消息分类
    val priority: MessagePriority,           // 消息优先级
    val title: String,                       // 消息标题
    val content: String,                     // 消息内容
    val contentType: ContentType,            // 内容类型
    val actionType: ActionType,              // 动作类型
    val actionData: String?,                 // 动作参数 (JSON)
    val iconUrl: String?,                    // 图标URL
    val attachments: List<Attachment>,       // 附件列表
    val userId: String,                      // 接收用户ID
    val isRead: Boolean,                     // 是否已读
    val isDeleted: Boolean,                  // 是否删除
    val readTime: Long?,                     // 阅读时间戳
    val expireTime: Long?,                   // 过期时间戳
    val createTime: Long,                    // 创建时间戳
    val updateTime: Long                     // 更新时间戳
) {
    /**
     * 判断消息是否过期
     */
    fun isExpired(): Boolean {
        return expireTime != null && System.currentTimeMillis() > expireTime
    }

    /**
     * 判断是否为紧急消息 (需要立即显示)
     */
    fun isUrgent(): Boolean {
        return priority == MessagePriority.P0_EMERGENCY
    }

    /**
     * 判断驾驶时是否可以显示
     */
    fun canShowWhileDriving(): Boolean {
        return priority == MessagePriority.P0_EMERGENCY || 
               priority == MessagePriority.P1_HIGH
    }
}

/**
 * 消息优先级枚举
 * ASIL等级: ASIL A
 * 需求追溯: REQ-MSG-FUN-003
 */
enum class MessagePriority(val level: Int, val displayTimeoutMs: Long) {
    P0_EMERGENCY(0, 100),    // 紧急: 安全告警，100ms内必须显示
    P1_HIGH(1, 200),         // 高: 导航提示、来电，200ms内显示
    P2_MEDIUM(2, 500),       // 中: 普通通知，500ms内显示
    P3_LOW(3, 1000);         // 低: 资讯推送，1s内显示

    companion object {
        fun fromLevel(level: Int): MessagePriority {
            return values().find { it.level == level } ?: P2_MEDIUM
        }
    }
}

/**
 * 消息分类
 */
enum class MessageCategory(val code: String, val displayName: String) {
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
        fun fromCode(code: String): MessageCategory {
            return values().find { it.code == code } ?: OTHER
        }
    }
}

/**
 * 内容类型
 */
enum class ContentType(val value: Int) {
    TEXT(0),           // 纯文本
    RICH_TEXT(1),      // 富文本
    IMAGE_TEXT(2),     // 图文
    MULTIMEDIA(3);     // 多媒体

    companion object {
        fun fromValue(value: Int): ContentType {
            return values().find { it.value == value } ?: TEXT
        }
    }
}

/**
 * 动作类型
 */
enum class ActionType(val value: Int) {
    NONE(0),           // 无动作
    NAVIGATE(1),       // 页面跳转
    POPUP(2),          // 弹窗显示
    EXTERNAL(3),       // 外部链接
    DISMISS(4);        // 仅消失

    companion object {
        fun fromValue(value: Int): ActionType {
            return values().find { it.value == value } ?: NONE
        }
    }
}

/**
 * 附件实体
 */
data class Attachment(
    val id: String,
    val messageId: String,
    val fileName: String,
    val filePath: String,
    val fileType: FileType,
    val fileSize: Long,
    val mimeType: String,
    val thumbnailPath: String?,
    val createTime: Long
)

enum class FileType(val value: Int) {
    IMAGE(0),
    VIDEO(1),
    AUDIO(2),
    OTHER(3);

    companion object {
        fun fromValue(value: Int): FileType {
            return values().find { it.value == value } ?: OTHER
        }
    }
}

/**
 * 消息筛选条件
 */
data class MessageFilter(
    val userId: String? = null,              // 用户ID筛选
    val sourceApp: String? = null,           // 来源应用筛选
    val category: MessageCategory? = null,   // 分类筛选
    val priority: MessagePriority? = null,   // 优先级筛选
    val isRead: Boolean? = null,             // 已读状态筛选
    val startTime: Long? = null,             // 开始时间
    val endTime: Long? = null,               // 结束时间
    val keyword: String? = null,             // 关键词搜索
    val sortBy: SortField = SortField.TIME,  // 排序字段
    val sortOrder: SortOrder = SortOrder.DESC, // 排序方向
    val page: Int = 1,                       // 页码
    val pageSize: Int = 20                   // 每页数量
)

enum class SortField {
    TIME, PRIORITY, CATEGORY, SOURCE
}

enum class SortOrder {
    ASC, DESC
}

/**
 * 驾驶状态
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
     */
    fun isDriving(): Boolean {
        return this != STOPPED
    }

    /**
     * 获取允许显示的消息优先级
     */
    fun allowedPriorities(): List<MessagePriority> {
        return when (this) {
            STOPPED -> MessagePriority.values().toList()
            DRIVING_SLOW -> listOf(MessagePriority.P0_EMERGENCY, MessagePriority.P1_HIGH)
            DRIVING_NORMAL, DRIVING_FAST -> listOf(MessagePriority.P0_EMERGENCY)
        }
    }
}

/**
 * 消息弹窗配置
 * ASIL等级: ASIL A
 * 需求追溯: REQ-MSG-FUN-004
 */
data class PopupConfig(
    val position: PopupPosition,             // 弹窗位置
    val size: PopupSize,                     // 弹窗尺寸
    val durationMs: Long,                    // 显示时长
    val autoDismiss: Boolean,                // 自动消失
    val showIcon: Boolean,                   // 显示图标
    val voiceAnnouncement: Boolean           // 语音播报
)

enum class PopupPosition {
    TOP_SAFE,          // 顶部安全区域
    BOTTOM_SAFE,       // 底部安全区域
    CENTER_COMPACT;    // 中央紧凑模式

    /**
     * 根据驾驶状态获取安全位置
     */
    fun getSafePosition(drivingState: DrivingState): PopupPosition {
        return when (drivingState) {
            DrivingState.STOPPED -> this
            else -> BOTTOM_SAFE  // 驾驶时只能显示在底部
        }
    }
}

enum class PopupSize {
    COMPACT,           // 紧凑 (屏幕10%)
    NORMAL,            // 正常 (屏幕20%)
    FULL;              // 全屏

    /**
     * 获取驾驶时的安全尺寸
     */
    fun getSafeSize(drivingState: DrivingState): PopupSize {
        return when (drivingState) {
            DrivingState.STOPPED -> this
            else -> COMPACT      // 驾驶时只能使用紧凑尺寸
        }
    }
}
```

### 3.2 仓库接口 (Repository Interface)

```kotlin
/**
 * 消息仓库接口
 * 需求追溯: REQ-MSG-FUN-001, REQ-MSG-FUN-002
 */
interface IMessageRepository {
    
    /**
     * 获取消息列表 (Flow实时更新)
     * @param filter 筛选条件
     * @return 消息列表流
     */
    fun getMessages(filter: MessageFilter = MessageFilter()): Flow<List<Message>>
    
    /**
     * 获取单条消息
     * @param messageId 消息ID
     */
    suspend fun getMessageById(messageId: String): Result<Message>
    
    /**
     * 发送消息
     * @param message 消息对象
     * @return 发送结果
     */
    suspend fun sendMessage(message: Message): Result<String>
    
    /**
     * 批量发送消息
     * @param messages 消息列表
     */
    suspend fun sendMessages(messages: List<Message>): Result<Int>
    
    /**
     * 标记消息为已读
     * @param messageId 消息ID
     */
    suspend fun markAsRead(messageId: String): Result<Unit>
    
    /**
     * 批量标记已读
     * @param messageIds 消息ID列表
     */
    suspend fun markAsReadBatch(messageIds: List<String>): Result<Int>
    
    /**
     * 标记分类下所有消息为已读
     * @param category 分类
     */
    suspend fun markCategoryAsRead(category: MessageCategory): Result<Int>
    
    /**
     * 删除消息
     * @param messageId 消息ID
     * @param softDelete 软删除标记
     */
    suspend fun deleteMessage(messageId: String, softDelete: Boolean = true): Result<Unit>
    
    /**
     * 批量删除消息
     */
    suspend fun deleteMessages(messageIds: List<String>): Result<Int>
    
    /**
     * 获取未读消息数量
     * @param category 分类 (null表示全部)
     */
    fun getUnreadCount(category: MessageCategory? = null): Flow<Int>
    
    /**
     * 清理过期消息
     * @param beforeTime 清理此时间之前的消息
     */
    suspend fun cleanupExpiredMessages(beforeTime: Long): Result<Int>
    
    /**
     * 搜索消息
     * @param keyword 关键词
     * @param filter 基础筛选条件
     */
    fun searchMessages(keyword: String, filter: MessageFilter = MessageFilter()): Flow<List<Message>>
    
    /**
     * 获取消息统计
     */
    suspend fun getMessageStatistics(startTime: Long, endTime: Long): Result<MessageStatistics>
}

/**
 * 消息统计数据
 */
data class MessageStatistics(
    val totalCount: Int,
    val readCount: Int,
    val unreadCount: Int,
    val byCategory: Map<MessageCategory, Int>,
    val byPriority: Map<MessagePriority, Int>,
    val bySource: Map<String, Int>
)
```

### 3.3 用例类 (Use Cases)

```kotlin
/**
 * 获取消息列表用例
 */
class GetMessagesUseCase @Inject constructor(
    private val messageRepository: IMessageRepository
) {
    operator fun invoke(filter: MessageFilter = MessageFilter()): Flow<List<Message>> {
        return messageRepository.getMessages(filter)
    }
}

/**
 * 发送消息用例
 * ASIL等级: ASIL A
 * 需求追溯: REQ-MSG-FUN-003
 */
class SendMessageUseCase @Inject constructor(
    private val messageRepository: IMessageRepository,
    private val priorityCalculator: PriorityCalculator,
    private val drivingSafetyController: DrivingSafetyController
) {
    suspend operator fun invoke(message: Message): Result<String> {
        // 1. 计算消息优先级
        val calculatedPriority = priorityCalculator.calculate(message)
        val finalMessage = message.copy(priority = calculatedPriority)
        
        // 2. 发送消息
        val result = messageRepository.sendMessage(finalMessage)
        
        // 3. 如果是P0/P1消息，检查驾驶安全
        if (finalMessage.priority.canShowWhileDriving()) {
            drivingSafetyController.checkAndShow(finalMessage)
        }
        
        return result
    }
}

/**
 * 标记已读用例
 */
class MarkAsReadUseCase @Inject constructor(
    private val messageRepository: IMessageRepository
) {
    suspend operator fun invoke(messageId: String): Result<Unit> {
        return messageRepository.markAsRead(messageId)
    }
}

/**
 * 删除消息用例
 */
class DeleteMessageUseCase @Inject constructor(
    private val messageRepository: IMessageRepository
) {
    suspend operator fun invoke(messageId: String, softDelete: Boolean = true): Result<Unit> {
        return messageRepository.deleteMessage(messageId, softDelete)
    }
}

/**
 * 获取未读数量用例
 */
class GetUnreadCountUseCase @Inject constructor(
    private val messageRepository: IMessageRepository
) {
    operator fun invoke(category: MessageCategory? = null): Flow<Int> {
        return messageRepository.getUnreadCount(category)
    }
}

/**
 * 清理过期消息用例
 * 需求追溯: REQ-MSG-FUN-002 (30天历史)
 */
class CleanupExpiredMessagesUseCase @Inject constructor(
    private val messageRepository: IMessageRepository,
    private val configRepository: IConfigRepository
) {
    suspend operator fun invoke(): Result<Int> {
        val retentionDays = configRepository.getMessageRetentionDays()
        val cutoffTime = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000)
        return messageRepository.cleanupExpiredMessages(cutoffTime)
    }
}

/**
 * 获取智能推荐用例
 * 需求追溯: REQ-MSG-FUN-006
 */
class GetSmartRecommendationsUseCase @Inject constructor(
    private val messageRepository: IMessageRepository,
    private val userBehaviorAnalyzer: UserBehaviorAnalyzer,
    private val sceneDetector: SceneDetector
) {
    suspend operator fun invoke(): Result<List<RecommendedMessage>> {
        // 1. 检测当前场景
        val currentScene = sceneDetector.detectCurrentScene()
        
        // 2. 分析用户行为
        val userPreferences = userBehaviorAnalyzer.analyze()
        
        // 3. 生成推荐
        return generateRecommendations(currentScene, userPreferences)
    }
    
    private suspend fun generateRecommendations(
        scene: Scene,
        preferences: UserPreferences
    ): Result<List<RecommendedMessage>> {
        // 实现推荐算法
        return Result.success(emptyList())
    }
}

/**
 * 推荐消息数据类
 */
data class RecommendedMessage(
    val message: Message,
    val relevanceScore: Float,           // 相关度分数 (0-1)
    val recommendationType: RecommendationType,
    val reason: String                   // 推荐理由
)

enum class RecommendationType {
    FREQUENT_APP,        // 常用应用
    SCENE_BASED,         // 场景化推荐
    TIME_BASED,          // 时间相关
    LOCATION_BASED,      // 位置相关
    BEHAVIOR_BASED       // 行为分析
}
```

### 3.4 服务层类 (Service Layer)

```kotlin
/**
 * 消息服务 - 系统服务
 * 作为AIDL接口的实现
 * ASIL等级: ASIL A
 * 需求追溯: REQ-MSG-FUN-001~006
 */
class MessageService : Service() {
    
    @Inject
    lateinit var messageRepository: IMessageRepository
    
    @Inject
    lateinit var drivingSafetyController: DrivingSafetyController
    
    @Inject
    lateinit var priorityCalculator: PriorityCalculator
    
    @Inject
    lateinit var popupManager: MessagePopupManager
    
    private val binder = object : IMessageService.Stub() {
        
        override fun sendMessage(msg: MessageParcel): String {
            return runBlocking {
                val message = msg.toDomainModel()
                val result = messageRepository.sendMessage(message)
                result.getOrNull() ?: ""
            }
        }
        
        override fun getMessages(filter: MessageFilterParcel): List<MessageParcel> {
            return runBlocking {
                messageRepository.getMessages(filter.toDomainModel())
                    .first()
                    .map { it.toParcel() }
            }
        }
        
        override fun setAppMessagePriority(appId: String, priority: Int) {
            priorityCalculator.setAppPriority(appId, MessagePriority.fromLevel(priority))
        }
        
        override fun markAsRead(messageId: String) {
            runBlocking {
                messageRepository.markAsRead(messageId)
            }
        }
        
        override fun deleteMessage(messageId: String) {
            runBlocking {
                messageRepository.deleteMessage(messageId)
            }
        }
        
        override fun registerMessageListener(listener: IMessageListener) {
            // 注册消息监听回调
        }
    }
    
    override fun onBind(intent: Intent): IBinder = binder
}

/**
 * 优先级计算器
 * ASIL等级: ASIL A
 * 需求追溯: REQ-MSG-FUN-003
 */
class PriorityCalculator @Inject constructor(
    private val configRepository: IConfigRepository
) {
    private val appPriorities = ConcurrentHashMap<String, MessagePriority>()
    
    /**
     * 计算消息优先级
     * 根据消息内容、来源应用、当前场景综合判断
     */
    fun calculate(message: Message): MessagePriority {
        // 1. 检查是否有预定义优先级
        val appPriority = appPriorities[message.sourceApp]
        
        // 2. 根据内容类型调整
        val contentPriority = when {
            isEmergencyContent(message) -> MessagePriority.P0_EMERGENCY
            isHighPriorityContent(message) -> MessagePriority.P1_HIGH
            else -> appPriority ?: message.priority
        }
        
        // 3. 根据驾驶状态调整
        return adjustPriorityForDriving(contentPriority)
    }
    
    /**
     * 设置应用消息优先级
     */
    fun setAppPriority(appId: String, priority: MessagePriority) {
        appPriorities[appId] = priority
    }
    
    private fun isEmergencyContent(message: Message): Boolean {
        val emergencyKeywords = listOf("碰撞", "故障", "告警", "紧急", "危险")
        return emergencyKeywords.any { 
            message.title.contains(it) || message.content.contains(it) 
        }
    }
    
    private fun isHighPriorityContent(message: Message): Boolean {
        val highPriorityCategories = listOf(
            MessageCategory.VEHICLE,
            MessageCategory.SECURITY
        )
        return message.category in highPriorityCategories
    }
    
    private fun adjustPriorityForDriving(priority: MessagePriority): MessagePriority {
        // 驾驶时的优先级调整逻辑
        return priority
    }
}

/**
 * 驾驶安全控制器
 * ASIL等级: ASIL A
 * 需求追溯: REQ-MSG-FUN-004
 */
class DrivingSafetyController @Inject constructor(
    private val vehicleService: IVehicleService,
    private val popupManager: MessagePopupManager,
    private val voiceAnnouncer: VoiceAnnouncer
) {
    private val _drivingState = MutableStateFlow(DrivingState.STOPPED)
    val drivingState: StateFlow<DrivingState> = _drivingState.asStateFlow()
    
    private val safetyScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    init {
        // 监听车辆状态变化
        safetyScope.launch {
            monitorDrivingState()
        }
    }
    
    /**
     * 检查并显示消息 (带驾驶安全控制)
     */
    fun checkAndShow(message: Message) {
        val currentState = _drivingState.value
        
        when {
            // 停车状态 - 正常显示
            !currentState.isDriving() -> {
                popupManager.show(message, PopupConfig(
                    position = PopupPosition.TOP_SAFE,
                    size = PopupSize.NORMAL,
                    durationMs = 5000,
                    autoDismiss = true,
                    showIcon = true,
                    voiceAnnouncement = false
                ))
            }
            // 行驶状态 - 只允许P0/P1消息
            message.priority.canShowWhileDriving() -> {
                showSafePopup(message, currentState)
            }
            // 其他消息 - 仅静默通知
            else -> {
                popupManager.showNotification(message)
            }
        }
    }
    
    /**
     * 获取安全的弹窗配置
     */
    fun getSafePopupConfig(): PopupConfig {
        val currentState = _drivingState.value
        return PopupConfig(
            position = PopupPosition.BOTTOM_SAFE.getSafePosition(currentState),
            size = PopupSize.COMPACT.getSafeSize(currentState),
            durationMs = if (currentState == DrivingState.DRIVING_FAST) 3000 else 5000,
            autoDismiss = true,
            showIcon = true,
            voiceAnnouncement = currentState.isDriving()  // 驾驶时语音播报
        )
    }
    
    private suspend fun monitorDrivingState() {
        // 通过VehicleService监听车速和挡位
        vehicleService.registerSpeedListener(object : SpeedListener {
            override fun onSpeedChanged(speed: Int) {
                updateDrivingState(speed, vehicleService.getGearPosition())
            }
        }, 100)  // 100ms间隔
    }
    
    private fun updateDrivingState(speed: Int, gear: GearPosition) {
        val newState = when {
            speed == 0 && gear == GearPosition.P -> DrivingState.STOPPED
            speed < 30 -> DrivingState.DRIVING_SLOW
            speed < 80 -> DrivingState.DRIVING_NORMAL
            else -> DrivingState.DRIVING_FAST
        }
        _drivingState.value = newState
    }
    
    private fun showSafePopup(message: Message, state: DrivingState) {
        val config = PopupConfig(
            position = PopupPosition.BOTTOM_SAFE,
            size = PopupSize.COMPACT,
            durationMs = 3000,
            autoDismiss = true,
            showIcon = true,
            voiceAnnouncement = true
        )
        
        popupManager.show(message, config)
        
        // P0消息语音播报
        if (message.priority == MessagePriority.P0_EMERGENCY) {
            voiceAnnouncer.announce(message.title + message.content)
        }
    }
}

/**
 * 消息弹窗管理器
 * ASIL等级: ASIL A
 * 需求追溯: REQ-MSG-FUN-004
 */
class MessagePopupManager @Inject constructor(
    private val context: Context,
    private val windowManager: WindowManager
) {
    private var currentPopup: PopupWindow? = null
    private val popupQueue = PriorityQueue<MessagePopupItem>(
        compareByDescending { it.message.priority.level }
    )
    private val handler = Handler(Looper.getMainLooper())
    
    /**
     * 显示消息弹窗
     */
    @Synchronized
    fun show(message: Message, config: PopupConfig) {
        // 添加到队列
        popupQueue.offer(MessagePopupItem(message, config))
        
        // 如果当前没有弹窗，立即显示
        if (currentPopup == null) {
            showNext()
        }
    }
    
    /**
     * 显示通知 (非弹窗形式)
     */
    fun showNotification(message: Message) {
        // 发送到通知栏
        val notification = createNotification(message)
        NotificationManagerCompat.from(context).notify(
            message.id.hashCode(), 
            notification
        )
    }
    
    /**
     * 关闭当前弹窗
     */
    @Synchronized
    fun dismissCurrent() {
        currentPopup?.dismiss()
        currentPopup = null
        
        // 显示队列中的下一个
        handler.postDelayed({ showNext() }, 100)
    }
    
    private fun showNext() {
        val item = popupQueue.poll() ?: return
        
        val popupView = createPopupView(item.message, item.config)
        val params = createLayoutParams(item.config)
        
        val popup = PopupWindow(
            popupView,
            params.width,
            params.height,
            false
        ).apply {
            isOutsideTouchable = true
            setOnDismissListener {
                currentPopup = null
                showNext()
            }
        }
        
        currentPopup = popup
        
        // 显示弹窗
        val anchorView = getAnchorView(item.config.position)
        popup.showAtLocation(anchorView, getGravity(item.config.position), 0, 0)
        
        // 自动消失
        if (item.config.autoDismiss) {
            handler.postDelayed({ dismissCurrent() }, item.config.durationMs)
        }
    }
    
    private fun createPopupView(message: Message, config: PopupConfig): View {
        // 使用Compose创建弹窗视图
        return ComposeView(context).apply {
            setContent {
                MessagePopupCard(
                    message = message,
                    config = config,
                    onDismiss = { dismissCurrent() },
                    onClick = { handleMessageClick(message) }
                )
            }
        }
    }
    
    private fun createLayoutParams(config: PopupConfig): WindowManager.LayoutParams {
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        
        return WindowManager.LayoutParams().apply {
            width = when (config.size) {
                PopupSize.COMPACT -> (screenWidth * 0.9).toInt()
                PopupSize.NORMAL -> (screenWidth * 0.9).toInt()
                PopupSize.FULL -> screenWidth
            }
            height = when (config.size) {
                PopupSize.COMPACT -> (screenHeight * 0.1).toInt()  // ≤10%屏幕
                PopupSize.NORMAL -> (screenHeight * 0.2).toInt()
                PopupSize.FULL -> screenHeight
            }
            
            gravity = when (config.position) {
                PopupPosition.TOP_SAFE -> Gravity.TOP
                PopupPosition.BOTTOM_SAFE -> Gravity.BOTTOM
                PopupPosition.CENTER_COMPACT -> Gravity.CENTER
            }
        }
    }
    
    private fun createNotification(message: Message): Notification {
        // 创建通知
        return NotificationCompat.Builder(context, MESSAGE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(message.title)
            .setContentText(message.content)
            .setPriority(getNotificationPriority(message.priority))
            .setAutoCancel(true)
            .build()
    }
    
    private fun getNotificationPriority(priority: MessagePriority): Int {
        return when (priority) {
            MessagePriority.P0_EMERGENCY -> NotificationCompat.PRIORITY_HIGH
            MessagePriority.P1_HIGH -> NotificationCompat.PRIORITY_DEFAULT
            MessagePriority.P2_MEDIUM -> NotificationCompat.PRIORITY_LOW
            MessagePriority.P3_LOW -> NotificationCompat.PRIORITY_MIN
        }
    }
    
    private fun handleMessageClick(message: Message) {
        // 处理消息点击
        when (message.actionType) {
            ActionType.NAVIGATE -> navigateToPage(message.actionData)
            ActionType.EXTERNAL -> openExternalApp(message.actionData)
            else -> dismissCurrent()
        }
    }
    
    private fun navigateToPage(actionData: String?) {
        // 页面导航逻辑
    }
    
    private fun openExternalApp(actionData: String?) {
        // 打开外部应用逻辑
    }
    
    private fun getAnchorView(position: PopupPosition): View {
        // 获取锚点视图
        return (context as Activity).window.decorView.rootView
    }
    
    private fun getGravity(position: PopupPosition): Int {
        return when (position) {
            PopupPosition.TOP_SAFE -> Gravity.TOP
            PopupPosition.BOTTOM_SAFE -> Gravity.BOTTOM
            PopupPosition.CENTER_COMPACT -> Gravity.CENTER
        }
    }
}

private data class MessagePopupItem(
    val message: Message,
    val config: PopupConfig
)

/**
 * 语音播报器
 */
class VoiceAnnouncer @Inject constructor(
    private val ttsService: ITtsService
) {
    fun announce(text: String) {
        ttsService.speak(text)
    }
}

/**
 * 用户行为分析器
 * 需求追溯: REQ-MSG-FUN-006
 */
class UserBehaviorAnalyzer @Inject constructor(
    private val messageRepository: IMessageRepository,
    private val appUsageRepository: IAppUsageRepository
) {
    /**
     * 分析用户行为偏好
     */
    suspend fun analyze(): UserPreferences {
        // 1. 获取历史消息交互数据
        val messageStats = messageRepository.getMessageStatistics(
            System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000,
            System.currentTimeMillis()
        ).getOrNull()
        
        // 2. 获取应用使用数据
        val appUsage = appUsageRepository.getRecentUsage(30)
        
        // 3. 分析偏好
        return UserPreferences(
            preferredCategories = analyzePreferredCategories(messageStats),
            preferredApps = analyzePreferredApps(appUsage),
            activeTimeRanges = analyzeActiveTimeRanges(),
            interactionPatterns = analyzeInteractionPatterns()
        )
    }
    
    private fun analyzePreferredCategories(stats: MessageStatistics?): List<MessageCategory> {
        return stats?.byCategory?.entries
            ?.sortedByDescending { it.value }
            ?.map { it.key }
            ?: emptyList()
    }
    
    private fun analyzePreferredApps(usage: List<AppUsage>): List<String> {
        return usage.sortedByDescending { it.duration }
            .map { it.appId }
            .take(10)
    }
    
    private fun analyzeActiveTimeRanges(): List<TimeRange> {
        // 分析活跃时间段
        return emptyList()
    }
    
    private fun analyzeInteractionPatterns(): InteractionPatterns {
        // 分析交互模式
        return InteractionPatterns()
    }
}

data class UserPreferences(
    val preferredCategories: List<MessageCategory>,
    val preferredApps: List<String>,
    val activeTimeRanges: List<TimeRange>,
    val interactionPatterns: InteractionPatterns
)

data class TimeRange(val startHour: Int, val endHour: Int)
data class InteractionPatterns(
    val clickThroughRate: Float = 0f,
    val averageResponseTime: Long = 0L
)
data class AppUsage(val appId: String, val duration: Long, val launchCount: Int)

/**
 * 场景检测器
 * 需求追溯: REQ-MSG-FUN-006
 */
class SceneDetector @Inject constructor(
    private val vehicleService: IVehicleService,
    private val navigationService: INavigationService,
    private val locationService: ILocationService
) {
    /**
     * 检测当前场景
     */
    fun detectCurrentScene(): Scene {
        val speed = vehicleService.getVehicleSpeed()
        val gear = vehicleService.getGearPosition()
        val location = locationService.getCurrentLocation()
        val navStatus = navigationService.getNavigationStatus()
        
        return when {
            navStatus.isNavigating -> Scene.NAVIGATING
            speed > 0 -> Scene.DRIVING
            isNearGasStation(location) -> Scene.NEAR_GAS_STATION
            isNearParking(location) -> Scene.NEAR_PARKING
            isNearChargingStation(location) -> Scene.NEAR_CHARGING
            else -> Scene.IDLE
        }
    }
    
    private fun isNearGasStation(location: Location): Boolean {
        // 检测是否靠近加油站
        return false
    }
    
    private fun isNearParking(location: Location): Boolean {
        // 检测是否靠近停车场
        return false
    }
    
    private fun isNearChargingStation(location: Location): Boolean {
        // 检测是否靠近充电站
        return false
    }
}

enum class Scene {
    IDLE,               // 空闲
    DRIVING,            // 驾驶中
    NAVIGATING,         // 导航中
    NEAR_GAS_STATION,   // 靠近加油站
    NEAR_PARKING,       // 靠近停车场
    NEAR_CHARGING       // 靠近充电站
}
```

### 3.5 ViewModel层

```kotlin
/**
 * 消息中心ViewModel
 * 管理UI状态和业务逻辑协调
 */
@HiltViewModel
class MessageCenterViewModel @Inject constructor(
    private val getMessagesUseCase: GetMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val markAsReadUseCase: MarkAsReadUseCase,
    private val deleteMessageUseCase: DeleteMessageUseCase,
    private val getUnreadCountUseCase: GetUnreadCountUseCase,
    private val getSmartRecommendationsUseCase: GetSmartRecommendationsUseCase,
    private val drivingSafetyController: DrivingSafetyController
) : ViewModel() {
    
    // UI状态
    private val _uiState = MutableStateFlow(MessageUiState())
    val uiState: StateFlow<MessageUiState> = _uiState.asStateFlow()
    
    // 当前筛选条件
    private val _currentFilter = MutableStateFlow(MessageFilter())
    
    // 驾驶状态
    val drivingState: StateFlow<DrivingState> = drivingSafetyController.drivingState
    
    init {
        // 加载消息列表
        loadMessages()
        
        // 加载未读数量
        loadUnreadCount()
        
        // 加载智能推荐
        loadRecommendations()
    }
    
    /**
     * 加载消息列表
     */
    private fun loadMessages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            getMessagesUseCase(_currentFilter.value)
                .catch { e ->
                    _uiState.update { 
                        it.copy(isLoading = false, error = e.message) 
                    }
                }
                .collect { messages ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            messages = messages,
                            error = null
                        ) 
                    }
                }
        }
    }
    
    /**
     * 更新筛选条件
     */
    fun updateFilter(filter: MessageFilter) {
        _currentFilter.value = filter
        loadMessages()
    }
    
    /**
     * 标记消息已读
     */
    fun markAsRead(messageId: String) {
        viewModelScope.launch {
            markAsReadUseCase(messageId)
                .onSuccess {
                    // 刷新列表
                    loadMessages()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }
    
    /**
     * 删除消息
     */
    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            deleteMessageUseCase(messageId)
                .onSuccess {
                    loadMessages()
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
        }
    }
    
    /**
     * 发送消息 (用于测试)
     */
    fun sendMessage(message: Message) {
        viewModelScope.launch {
            sendMessageUseCase(message)
                .onSuccess {
                    loadMessages()
                }
        }
    }
    
    /**
     * 加载未读数量
     */
    private fun loadUnreadCount() {
        viewModelScope.launch {
            getUnreadCountUseCase()
                .collect { count ->
                    _uiState.update { it.copy(unreadCount = count) }
                }
        }
    }
    
    /**
     * 加载智能推荐
     */
    private fun loadRecommendations() {
        viewModelScope.launch {
            getSmartRecommendationsUseCase()
                .onSuccess { recommendations ->
                    _uiState.update { 
                        it.copy(recommendations = recommendations) 
                    }
                }
        }
    }
    
    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * 消息UI状态
 */
data class MessageUiState(
    val isLoading: Boolean = false,
    val messages: List<Message> = emptyList(),
    val unreadCount: Int = 0,
    val recommendations: List<RecommendedMessage> = emptyList(),
    val selectedCategory: MessageCategory? = null,
    val error: String? = null
)
```

---

## 4. 时序图设计

### 4.1 消息接收与显示流程

```
┌─────────┐     ┌─────────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────┐
│ 应用    │     │ MessageRouter│     │ DrivingSafety│     │ MessagePopup│     │ 用户     │
│(Source) │     │             │     │ Controller   │     │ Manager     │     │(User)    │
└────┬────┘     └──────┬──────┘     └──────┬───────┘     └──────┬──────┘     └────┬─────┘
     │                 │                   │                    │                 │
     │ 1. sendMessage  │                   │                    │                 │
     │────────────────>│                   │                    │                 │
     │                 │                   │                    │                 │
     │                 │ 2. calculatePriority                   │                 │
     │                 │──────────────────>│                    │                 │
     │                 │                   │                    │                 │
     │                 │ 3. return Priority│                    │                 │
     │                 │<──────────────────│                    │                 │
     │                 │                   │                    │                 │
     │                 │ 4. checkDrivingState                  │                 │
     │                 │──────────────────>│                    │                 │
     │                 │                   │                    │                 │
     │                 │ 5. return State   │                    │                 │
     │                 │<──────────────────│                    │                 │
     │                 │                   │                    │                 │
     │                 │ 6. showMessage (with safe config)      │                 │
     │                 │───────────────────────────────────────>│                 │
     │                 │                   │                    │                 │
     │                 │                   │ 7. renderPopup     │                 │
     │                 │                   │                    │────────────────>│
     │                 │                   │                    │                 │
     │                 │                   │ 8. userInteraction │                 │
     │                 │                   │                    │<────────────────│
     │                 │                   │                    │                 │
     │                 │ 9. dismiss        │                    │                 │
     │                 │<───────────────────────────────────────│                 │
     │                 │                   │                    │                 │
```

**流程说明**:
1. 应用调用`sendMessage`发送消息
2. `MessageRouter`调用`PriorityCalculator`计算优先级
3. 返回计算后的优先级
4. 检查当前驾驶状态
5. 返回驾驶状态信息
6. 根据驾驶状态调用安全的显示方法
7. 渲染弹窗显示给用户
8. 用户与弹窗交互
9. 弹窗关闭，流程结束

### 4.2 消息存储与查询流程

```
┌─────────┐     ┌─────────────┐     ┌──────────────┐     ┌─────────────┐     ┌──────────┐
│  UI层   │     │  ViewModel  │     │  UseCase     │     │ Repository  │     │  DAO     │
│(Compose)│     │             │     │              │     │             │     │(Room)    │
└────┬────┘     └──────┬──────┘     └──────┬───────┘     └──────┬──────┘     └────┬─────┘
     │                 │                   │                    │                 │
     │ 1. 用户操作      │                   │                    │                 │
     │────────────────>│                   │                    │                 │
     │                 │                   │                    │                 │
     │                 │ 2. updateFilter   │                    │                 │
     │                 │──────────────────>│                    │                 │
     │                 │                   │                    │                 │
     │                 │                   │ 3. getMessages(filter)
     │                 │                   │───────────────────>│                 │
     │                 │                   │                    │                 │
     │                 │                   │                    │ 4. query DB    │
     │                 │                   │                    │────────────────>│
     │                 │                   │                    │                 │
     │                 │                   │                    │ 5. return Flow │
     │                 │                   │                    │<────────────────│
     │                 │                   │                    │                 │
     │                 │                   │ 6. return Flow     │                 │
     │                 │                   │<───────────────────│                 │
     │                 │                   │                    │                 │
     │                 │ 7. collect Flow   │                    │                 │
     │                 │                   │                    │                 │
     │ 8. update UI    │                   │                    │                 │
     │<────────────────│                   │                    │                 │
     │                 │                   │                    │                 │
```

**流程说明**:
1. 用户在UI层进行操作（如切换分类）
2. ViewModel更新筛选条件
3. UseCase调用Repository获取消息
4. Repository通过DAO查询数据库
5. 返回Flow数据流
6. Flow逐层返回
7. ViewModel收集Flow数据
8. 更新UI状态，Compose自动重组

### 4.3 驾驶安全控制流程

```
┌─────────────┐     ┌───────────────┐     ┌──────────────┐     ┌─────────────┐
│ Vehicle HAL │     │ VehicleService│     │ DrivingSafety│     │ PopupManager│
│             │     │               │     │ Controller   │             │
└──────┬──────┘     └───────┬───────┘     └──────┬───────┘     └──────┬──────┘
       │                    │                    │                    │
       │ 1. CAN信号(车速/挡位)│                    │                    │
       │───────────────────>│                    │                    │
       │                    │                    │                    │
       │                    │ 2. parseSignal     │                    │
       │                    │───────────────────>│                    │
       │                    │                    │                    │
       │                    │ 3. stateChanged    │                    │
       │                    │───────────────────>│                    │
       │                    │                    │                    │
       │                    │                    │ 4. updateDrivingState
       │                    │                    │                    │
       │                    │                    │ 5. checkQueue       │
       │                    │                    │───────────────────>│
       │                    │                    │                    │
       │                    │                    │ 6. applySafeMode    │
       │                    │                    │<───────────────────│
       │                    │                    │                    │
       │                    │                    │ 7. adjustDisplay    │
       │                    │                    │───────────────────>│
       │                    │                    │                    │
```

**流程说明**:
1. Vehicle HAL接收CAN总线信号
2. VehicleService解析信号
3. 驾驶状态变化通知
4. 更新驾驶状态
5. 检查消息队列
6. 应用安全模式
7. 调整显示策略

---

## 5. 接口实现细节

### 5.1 AIDL接口定义

```aidl
// IMessageService.aidl
package com.longcheer.cockpit.message;

import com.longcheer.cockpit.message.MessageParcel;
import com.longcheer.cockpit.message.MessageFilterParcel;
import com.longcheer.cockpit.message.IMessageListener;

/**
 * 消息服务AIDL接口
 * ASIL等级: ASIL A
 */
interface IMessageService {
    
    /**
     * 发送消息
     * @param msg 消息对象
     * @return 消息ID
     */
    String sendMessage(in MessageParcel msg);
    
    /**
     * 获取消息列表
     * @param filter 筛选条件
     * @return 消息列表
     */
    List<MessageParcel> getMessages(in MessageFilterParcel filter);
    
    /**
     * 设置应用消息优先级
     * @param appId 应用ID
     * @param priority 优先级(0=P0, 1=P1, 2=P2, 3=P3)
     */
    void setAppMessagePriority(String appId, int priority);
    
    /**
     * 标记消息已读
     * @param messageId 消息ID
     */
    void markAsRead(String messageId);
    
    /**
     * 删除消息
     * @param messageId 消息ID
     */
    void deleteMessage(String messageId);
    
    /**
     * 注册消息监听
     * @param listener 监听器
     */
    void registerMessageListener(IMessageListener listener);
    
    /**
     * 注销消息监听
     * @param listener 监听器
     */
    void unregisterMessageListener(IMessageListener listener);
    
    /**
     * 获取未读数量
     * @param category 分类代码 (null表示全部)
     * @return 未读数量
     */
    int getUnreadCount(String category);
    
    /**
     * 清理过期消息
     * @param beforeTime 此时间之前的消息将被清理
     * @return 清理数量
     */
    int cleanupMessages(long beforeTime);
}

// IMessageListener.aidl
package com.longcheer.cockpit.message;

import com.longcheer.cockpit.message.MessageParcel;

/**
 * 消息监听回调
 */
interface IMessageListener {
    
    /**
     * 新消息到达
     * @param message 消息对象
     */
    void onNewMessage(in MessageParcel message);
    
    /**
     * 消息已读
     * @param messageId 消息ID
     */
    void onMessageRead(String messageId);
    
    /**
     * 消息删除
     * @param messageId 消息ID
     */
    void onMessageDeleted(String messageId);
}

// MessageParcel.aidl (parcelable定义)
package com.longcheer.cockpit.message;

parcelable MessageParcel {
    String id;
    String sourceApp;
    String sourceName;
    String category;
    int priority;
    String title;
    String content;
    int contentType;
    int actionType;
    String actionData;
    String iconUrl;
    long createTime;
    boolean isRead;
}

// MessageFilterParcel.aidl
package com.longcheer.cockpit.message;

parcelable MessageFilterParcel {
    String userId;
    String sourceApp;
    String category;
    int priority;
    boolean unreadOnly;
    long startTime;
    long endTime;
    String keyword;
    int page;
    int pageSize;
}
```

### 5.2 接口实现代码

```kotlin
/**
 * AIDL接口实现类
 */
class MessageServiceImpl @Inject constructor(
    private val messageRepository: IMessageRepository,
    private val priorityCalculator: PriorityCalculator,
    private val drivingSafetyController: DrivingSafetyController,
    private val popupManager: MessagePopupManager
) : IMessageService.Stub() {
    
    private val listeners = RemoteCallbackList<IMessageListener>()
    
    override fun sendMessage(msg: MessageParcel?): String {
        requireNotNull(msg) { "Message cannot be null" }
        
        return runBlocking(Dispatchers.IO) {
            try {
                // 转换为领域模型
                val message = msg.toDomainModel()
                
                // 计算优先级
                val priority = priorityCalculator.calculate(message)
                val finalMessage = message.copy(priority = priority)
                
                // 存储消息
                val result = messageRepository.sendMessage(finalMessage)
                
                result.fold(
                    onSuccess = { messageId ->
                        // 通知监听器
                        notifyNewMessage(finalMessage.toParcel())
                        
                        // 检查驾驶安全并显示
                        if (finalMessage.priority.canShowWhileDriving()) {
                            drivingSafetyController.checkAndShow(finalMessage)
                        } else {
                            popupManager.showNotification(finalMessage)
                        }
                        
                        messageId
                    },
                    onFailure = {
                        throw RemoteException("Failed to send message: ${it.message}")
                    }
                )
            } catch (e: Exception) {
                throw RemoteException("Send message error: ${e.message}")
            }
        }
    }
    
    override fun getMessages(filter: MessageFilterParcel?): List<MessageParcel> {
        return runBlocking(Dispatchers.IO) {
            val domainFilter = filter?.toDomainModel() ?: MessageFilter()
            
            messageRepository.getMessages(domainFilter)
                .first()
                .map { it.toParcel() }
        }
    }
    
    override fun setAppMessagePriority(appId: String?, priority: Int) {
        requireNotNull(appId) { "AppId cannot be null" }
        priorityCalculator.setAppPriority(
            appId, 
            MessagePriority.fromLevel(priority)
        )
    }
    
    override fun markAsRead(messageId: String?) {
        requireNotNull(messageId) { "MessageId cannot be null" }
        
        runBlocking(Dispatchers.IO) {
            messageRepository.markAsRead(messageId)
                .onSuccess {
                    notifyMessageRead(messageId)
                }
        }
    }
    
    override fun deleteMessage(messageId: String?) {
        requireNotNull(messageId) { "MessageId cannot be null" }
        
        runBlocking(Dispatchers.IO) {
            messageRepository.deleteMessage(messageId)
                .onSuccess {
                    notifyMessageDeleted(messageId)
                }
        }
    }
    
    override fun registerMessageListener(listener: IMessageListener?) {
        listener?.let { listeners.register(it) }
    }
    
    override fun unregisterMessageListener(listener: IMessageListener?) {
        listener?.let { listeners.unregister(it) }
    }
    
    override fun getUnreadCount(category: String?): Int {
        return runBlocking(Dispatchers.IO) {
            val cat = category?.let { MessageCategory.fromCode(it) }
            messageRepository.getUnreadCount(cat).first()
        }
    }
    
    override fun cleanupMessages(beforeTime: Long): Int {
        return runBlocking(Dispatchers.IO) {
            messageRepository.cleanupExpiredMessages(beforeTime)
                .getOrDefault(0)
        }
    }
    
    private fun notifyNewMessage(message: MessageParcel) {
        val n = listeners.beginBroadcast()
        for (i in 0 until n) {
            try {
                listeners.getBroadcastItem(i).onNewMessage(message)
            } catch (e: RemoteException) {
                // Ignore
            }
        }
        listeners.finishBroadcast()
    }
    
    private fun notifyMessageRead(messageId: String) {
        val n = listeners.beginBroadcast()
        for (i in 0 until n) {
            try {
                listeners.getBroadcastItem(i).onMessageRead(messageId)
            } catch (e: RemoteException) {
                // Ignore
            }
        }
        listeners.finishBroadcast()
    }
    
    private fun notifyMessageDeleted(messageId: String) {
        val n = listeners.beginBroadcast()
        for (i in 0 until n) {
            try {
                listeners.getBroadcastItem(i).onMessageDeleted(messageId)
            } catch (e: RemoteException) {
                // Ignore
            }
        }
        listeners.finishBroadcast()
    }
}

/**
 * 扩展函数：Parcel与Domain Model转换
 */
fun MessageParcel.toDomainModel(): Message {
    return Message(
        id = id ?: UUID.randomUUID().toString(),
        sourceApp = sourceApp ?: "",
        sourceName = sourceName ?: "",
        category = MessageCategory.fromCode(category ?: "OTHER"),
        priority = MessagePriority.fromLevel(priority),
        title = title ?: "",
        content = content ?: "",
        contentType = ContentType.fromValue(contentType),
        actionType = ActionType.fromValue(actionType),
        actionData = actionData,
        iconUrl = iconUrl,
        attachments = emptyList(),
        userId = "0",  // 广播消息
        isRead = false,
        isDeleted = false,
        readTime = null,
        expireTime = null,
        createTime = createTime.takeIf { it > 0 } ?: System.currentTimeMillis(),
        updateTime = System.currentTimeMillis()
    )
}

fun Message.toParcel(): MessageParcel {
    return MessageParcel().apply {
        id = this@toParcel.id
        sourceApp = this@toParcel.sourceApp
        sourceName = this@toParcel.sourceName
        category = this@toParcel.category.code
        priority = this@toParcel.priority.level
        title = this@toParcel.title
        content = this@toParcel.content
        contentType = this@toParcel.contentType.value
        actionType = this@toParcel.actionType.value
        actionData = this@toParcel.actionData
        iconUrl = this@toParcel.iconUrl
        createTime = this@toParcel.createTime
        isRead = this@toParcel.isRead
    }
}

fun MessageFilterParcel.toDomainModel(): MessageFilter {
    return MessageFilter(
        userId = userId,
        sourceApp = sourceApp,
        category = category?.let { MessageCategory.fromCode(it) },
        priority = priority.takeIf { it >= 0 }?.let { MessagePriority.fromLevel(it) },
        isRead = if (unreadOnly) false else null,
        startTime = startTime.takeIf { it > 0 },
        endTime = endTime.takeIf { it > 0 },
        keyword = keyword,
        page = page.coerceAtLeast(1),
        pageSize = pageSize.coerceIn(1, 100)
    )
}
```

### 5.3 模块间接口矩阵

| 接口名称 | 提供者 | 消费者 | 调用方式 | 同步/异步 | ASIL等级 |
|----------|--------|--------|----------|-----------|----------|
| IMessageService | Message模块 | 所有应用 | AIDL/Binder | 同步 | ASIL A |
| IVehicleService | Vehicle服务 | Message模块 | AIDL | 异步 | ASIL B |
| IDrivingRestriction | DRV模块 | Message模块 | 回调 | 异步 | ASIL B |
| ITtsService | AI模块 | Message模块 | AIDL | 异步 | QM |
| INavigationService | NAV模块 | Message模块 | AIDL | 异步 | QM |

---

## 6. 数据库访问层设计

### 6.1 Entity定义

```kotlin
/**
 * 消息表实体
 * 对应数据库表: message
 */
@Entity(
    tableName = "message",
    indices = [
        Index(value = ["msg_id"], unique = true),
        Index(value = ["user_id", "is_deleted", "create_time"]),
        Index(value = ["category_id"]),
        Index(value = ["priority", "is_read"]),
        Index(value = ["source_app"])
    ]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    
    @ColumnInfo(name = "msg_id")
    val msgId: String,
    
    @ColumnInfo(name = "source_app")
    val sourceApp: String,
    
    @ColumnInfo(name = "source_name")
    val sourceName: String?,
    
    @ColumnInfo(name = "category_id")
    val categoryId: Int,
    
    @ColumnInfo(name = "priority")
    val priority: Int,  // 0=P0, 1=P1, 2=P2, 3=P3
    
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "content")
    val content: String?,
    
    @ColumnInfo(name = "content_type")
    val contentType: Int,  // 0=文本, 1=富文本, 2=图文, 3=多媒体
    
    @ColumnInfo(name = "action_type")
    val actionType: Int,  // 0=无, 1=跳转, 2=弹窗, 3=外部链接
    
    @ColumnInfo(name = "action_data")
    val actionData: String?,
    
    @ColumnInfo(name = "icon_url")
    val iconUrl: String?,
    
    @ColumnInfo(name = "user_id")
    val userId: Long,
    
    @ColumnInfo(name = "is_read")
    val isRead: Boolean = false,
    
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,
    
    @ColumnInfo(name = "read_time")
    val readTime: Long?,
    
    @ColumnInfo(name = "expire_time")
    val expireTime: Long?,
    
    @ColumnInfo(name = "create_time")
    val createTime: Long,
    
    @ColumnInfo(name = "update_time")
    val updateTime: Long
)

/**
 * 消息分类实体
 */
@Entity(tableName = "message_category")
data class MessageCategoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,
    
    @ColumnInfo(name = "cat_code")
    val catCode: String,
    
    @ColumnInfo(name = "cat_name")
    val catName: String,
    
    @ColumnInfo(name = "cat_name_en")
    val catNameEn: String?,
    
    @ColumnInfo(name = "parent_id")
    val parentId: Int = 0,
    
    @ColumnInfo(name = "icon")
    val icon: String?,
    
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,
    
    @ColumnInfo(name = "is_system")
    val isSystem: Boolean = false,
    
    @ColumnInfo(name = "is_visible")
    val isVisible: Boolean = true,
    
    @ColumnInfo(name = "create_time")
    val createTime: Long = System.currentTimeMillis()
)

/**
 * 消息附件实体
 */
@Entity(
    tableName = "message_attachment",
    foreignKeys = [
        ForeignKey(
            entity = MessageEntity::class,
            parentColumns = ["id"],
            childColumns = ["msg_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["msg_id"])]
)
data class MessageAttachmentEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    
    @ColumnInfo(name = "msg_id")
    val msgId: Long,
    
    @ColumnInfo(name = "file_name")
    val fileName: String,
    
    @ColumnInfo(name = "file_path")
    val filePath: String,
    
    @ColumnInfo(name = "file_type")
    val fileType: Int,  // 0=图片, 1=视频, 2=音频, 3=其他
    
    @ColumnInfo(name = "file_size")
    val fileSize: Long,
    
    @ColumnInfo(name = "mime_type")
    val mimeType: String?,
    
    @ColumnInfo(name = "thumbnail")
    val thumbnail: String?,
    
    @ColumnInfo(name = "create_time")
    val createTime: Long = System.currentTimeMillis()
)

/**
 * 消息与分类关联实体 (用于联合查询)
 */
data class MessageWithCategory(
    @Embedded
    val message: MessageEntity,
    
    @Relation(
        parentColumn = "category_id",
        entityColumn = "id"
    )
    val category: MessageCategoryEntity?,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "msg_id"
    )
    val attachments: List<MessageAttachmentEntity>
)
```

### 6.2 DAO接口

```kotlin
/**
 * 消息数据访问对象
 */
@Dao
interface MessageDao {
    
    // ==================== 查询操作 ====================
    
    /**
     * 根据筛选条件查询消息 (Flow实时监控)
     */
    @Query("""
        SELECT m.*, c.* FROM message m
        LEFT JOIN message_category c ON m.category_id = c.id
        WHERE (:userId IS NULL OR m.user_id = :userId)
        AND (:sourceApp IS NULL OR m.source_app = :sourceApp)
        AND (:categoryId IS NULL OR m.category_id = :categoryId)
        AND (:priority IS NULL OR m.priority = :priority)
        AND (:isRead IS NULL OR m.is_read = :isRead)
        AND (:startTime IS NULL OR m.create_time >= :startTime)
        AND (:endTime IS NULL OR m.create_time <= :endTime)
        AND m.is_deleted = 0
        ORDER BY 
            CASE WHEN :sortBy = 'TIME' THEN m.create_time END DESC,
            CASE WHEN :sortBy = 'PRIORITY' THEN m.priority END ASC,
            CASE WHEN :sortBy = 'CATEGORY' THEN m.category_id END ASC
        LIMIT :limit OFFSET :offset
    """)
    fun getMessages(
        userId: Long? = null,
        sourceApp: String? = null,
        categoryId: Int? = null,
        priority: Int? = null,
        isRead: Boolean? = null,
        startTime: Long? = null,
        endTime: Long? = null,
        sortBy: String = "TIME",
        limit: Int = 20,
        offset: Int = 0
    ): Flow<List<MessageWithCategory>>
    
    /**
     * 根据ID查询消息
     */
    @Query("""
        SELECT m.*, c.* FROM message m
        LEFT JOIN message_category c ON m.category_id = c.id
        WHERE m.msg_id = :msgId
    """)
    suspend fun getMessageById(msgId: String): MessageWithCategory?
    
    /**
     * 获取未读消息数量
     */
    @Query("""
        SELECT COUNT(*) FROM message 
        WHERE is_read = 0 AND is_deleted = 0
        AND (:categoryId IS NULL OR category_id = :categoryId)
    """)
    fun getUnreadCount(categoryId: Int? = null): Flow<Int>
    
    /**
     * 关键词搜索
     */
    @Query("""
        SELECT m.*, c.* FROM message m
        LEFT JOIN message_category c ON m.category_id = c.id
        WHERE (m.title LIKE '%' || :keyword || '%' 
               OR m.content LIKE '%' || :keyword || '%')
        AND m.is_deleted = 0
        ORDER BY m.create_time DESC
    """)
    fun searchMessages(keyword: String): Flow<List<MessageWithCategory>>
    
    /**
     * 获取消息统计
     */
    @Query("""
        SELECT 
            COUNT(*) as total,
            SUM(CASE WHEN is_read = 1 THEN 1 ELSE 0 END) as read_count,
            category_id,
            priority,
            source_app
        FROM message
        WHERE create_time BETWEEN :startTime AND :endTime
        AND is_deleted = 0
        GROUP BY category_id, priority, source_app
    """)
    suspend fun getStatistics(
        startTime: Long, 
        endTime: Long
    ): List<MessageStatisticsRow>
    
    // ==================== 插入操作 ====================
    
    /**
     * 插入消息
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long
    
    /**
     * 批量插入消息
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>): List<Long>
    
    /**
     * 插入附件
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: MessageAttachmentEntity): Long
    
    // ==================== 更新操作 ====================
    
    /**
     * 标记消息已读
     */
    @Query("""
        UPDATE message 
        SET is_read = 1, read_time = :readTime, update_time = :updateTime
        WHERE msg_id = :msgId
    """)
    suspend fun markAsRead(
        msgId: String, 
        readTime: Long = System.currentTimeMillis(),
        updateTime: Long = System.currentTimeMillis()
    ): Int
    
    /**
     * 批量标记已读
     */
    @Query("""
        UPDATE message 
        SET is_read = 1, read_time = :readTime, update_time = :updateTime
        WHERE msg_id IN (:msgIds)
    """)
    suspend fun markAsReadBatch(
        msgIds: List<String>,
        readTime: Long = System.currentTimeMillis(),
        updateTime: Long = System.currentTimeMillis()
    ): Int
    
    /**
     * 按分类标记已读
     */
    @Query("""
        UPDATE message 
        SET is_read = 1, read_time = :readTime, update_time = :updateTime
        WHERE category_id = :categoryId AND is_deleted = 0
    """)
    suspend fun markCategoryAsRead(
        categoryId: Int,
        readTime: Long = System.currentTimeMillis(),
        updateTime: Long = System.currentTimeMillis()
    ): Int
    
    // ==================== 删除操作 ====================
    
    /**
     * 软删除消息
     */
    @Query("""
        UPDATE message 
        SET is_deleted = 1, update_time = :updateTime
        WHERE msg_id = :msgId
    """)
    suspend fun softDelete(msgId: String, updateTime: Long = System.currentTimeMillis()): Int
    
    /**
     * 硬删除消息
     */
    @Delete
    suspend fun deleteMessage(message: MessageEntity): Int
    
    /**
     * 批量删除
     */
    @Query("DELETE FROM message WHERE msg_id IN (:msgIds)")
    suspend fun deleteMessages(msgIds: List<String>): Int
    
    /**
     * 清理过期消息
     */
    @Query("DELETE FROM message WHERE create_time < :beforeTime")
    suspend fun cleanupExpired(beforeTime: Long): Int
    
    /**
     * 清理已删除消息 (物理删除)
     */
    @Query("DELETE FROM message WHERE is_deleted = 1")
    suspend fun cleanupDeleted(): Int
}

/**
 * 消息统计行
 */
data class MessageStatisticsRow(
    val total: Int,
    val readCount: Int,
    val categoryId: Int,
    val priority: Int,
    val sourceApp: String
)

/**
 * 分类DAO
 */
@Dao
interface MessageCategoryDao {
    
    @Query("SELECT * FROM message_category WHERE is_visible = 1 ORDER BY sort_order")
    suspend fun getAllCategories(): List<MessageCategoryEntity>
    
    @Query("SELECT * FROM message_category WHERE cat_code = :code")
    suspend fun getCategoryByCode(code: String): MessageCategoryEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: MessageCategoryEntity): Long
}
```

### 6.3 Repository实现

```kotlin
/**
 * 消息仓库实现
 */
class MessageRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao,
    private val categoryDao: MessageCategoryDao,
    private val messageMapper: MessageMapper,
    private val appDatabase: AppDatabase
) : IMessageRepository {
    
    override fun getMessages(filter: MessageFilter): Flow<List<Message>> {
        val offset = (filter.page - 1) * filter.pageSize
        
        return messageDao.getMessages(
            userId = filter.userId?.toLongOrNull(),
            sourceApp = filter.sourceApp,
            categoryId = filter.category?.ordinal,
            priority = filter.priority?.level,
            isRead = filter.isRead,
            startTime = filter.startTime,
            endTime = filter.endTime,
            sortBy = filter.sortBy.name,
            limit = filter.pageSize,
            offset = offset
        ).map { entities ->
            entities.map { messageMapper.mapToDomain(it) }
        }.flowOn(Dispatchers.IO)
    }
    
    override suspend fun getMessageById(messageId: String): Result<Message> {
        return runCatching {
            val entity = messageDao.getMessageById(messageId)
                ?: throw NoSuchElementException("Message not found: $messageId")
            messageMapper.mapToDomain(entity)
        }
    }
    
    override suspend fun sendMessage(message: Message): Result<String> {
        return runCatching {
            val entity = messageMapper.mapToEntity(message)
            val rowId = messageDao.insertMessage(entity)
            
            // 插入附件
            message.attachments.forEach { attachment ->
                val attachmentEntity = messageMapper.mapAttachmentToEntity(
                    attachment.copy(messageId = rowId.toString())
                )
                messageDao.insertAttachment(attachmentEntity)
            }
            
            message.id
        }
    }
    
    override suspend fun sendMessages(messages: List<Message>): Result<Int> {
        return runCatching {
            val entities = messages.map { messageMapper.mapToEntity(it) }
            messageDao.insertMessages(entities).size
        }
    }
    
    override suspend fun markAsRead(messageId: String): Result<Unit> {
        return runCatching {
            val affected = messageDao.markAsRead(messageId)
            if (affected == 0) {
                throw NoSuchElementException("Message not found: $messageId")
            }
        }
    }
    
    override suspend fun markAsReadBatch(messageIds: List<String>): Result<Int> {
        return runCatching {
            messageDao.markAsReadBatch(messageIds)
        }
    }
    
    override suspend fun markCategoryAsRead(category: MessageCategory): Result<Int> {
        return runCatching {
            messageDao.markCategoryAsRead(category.ordinal)
        }
    }
    
    override suspend fun deleteMessage(messageId: String, softDelete: Boolean): Result<Unit> {
        return runCatching {
            if (softDelete) {
                messageDao.softDelete(messageId)
            } else {
                // 硬删除需要先查询实体
                val entity = messageDao.getMessageById(messageId)?.message
                    ?: throw NoSuchElementException("Message not found: $messageId")
                messageDao.deleteMessage(entity)
            }
        }
    }
    
    override suspend fun deleteMessages(messageIds: List<String>): Result<Int> {
        return runCatching {
            messageDao.deleteMessages(messageIds)
        }
    }
    
    override fun getUnreadCount(category: MessageCategory?): Flow<Int> {
        return messageDao.getUnreadCount(category?.ordinal)
            .flowOn(Dispatchers.IO)
    }
    
    override suspend fun cleanupExpiredMessages(beforeTime: Long): Result<Int> {
        return runCatching {
            messageDao.cleanupExpired(beforeTime)
        }
    }
    
    override fun searchMessages(
        keyword: String, 
        filter: MessageFilter
    ): Flow<List<Message>> {
        return messageDao.searchMessages(keyword)
            .map { entities ->
                entities.map { messageMapper.mapToDomain(it) }
                    .filter { message ->
                        // 应用额外的筛选条件
                        (filter.category == null || message.category == filter.category) &&
                        (filter.priority == null || message.priority == filter.priority) &&
                        (filter.startTime == null || message.createTime >= filter.startTime) &&
                        (filter.endTime == null || message.createTime <= filter.endTime)
                    }
            }.flowOn(Dispatchers.IO)
    }
    
    override suspend fun getMessageStatistics(
        startTime: Long, 
        endTime: Long
    ): Result<MessageStatistics> {
        return runCatching {
            val rows = messageDao.getStatistics(startTime, endTime)
            
            val totalCount = rows.sumOf { it.total }
            val readCount = rows.sumOf { it.readCount }
            
            MessageStatistics(
                totalCount = totalCount,
                readCount = readCount,
                unreadCount = totalCount - readCount,
                byCategory = rows.groupBy { 
                    MessageCategory.values().getOrNull(it.categoryId) ?: MessageCategory.OTHER
                }.mapValues { it.value.sumOf { row -> row.total } },
                byPriority = rows.groupBy { 
                    MessagePriority.fromLevel(it.priority)
                }.mapValues { it.value.sumOf { row -> row.total } },
                bySource = rows.groupBy { it.sourceApp }
                    .mapValues { it.value.sumOf { row -> row.total } }
            )
        }
    }
}

/**
 * Entity与Domain Model映射器
 */
class MessageMapper @Inject constructor() {
    
    fun mapToDomain(entity: MessageWithCategory): Message {
        return Message(
            id = entity.message.msgId,
            sourceApp = entity.message.sourceApp,
            sourceName = entity.message.sourceName ?: "",
            category = MessageCategory.values().getOrElse(entity.message.categoryId) { MessageCategory.OTHER },
            priority = MessagePriority.fromLevel(entity.message.priority),
            title = entity.message.title,
            content = entity.message.content ?: "",
            contentType = ContentType.fromValue(entity.message.contentType),
            actionType = ActionType.fromValue(entity.message.actionType),
            actionData = entity.message.actionData,
            iconUrl = entity.message.iconUrl,
            attachments = entity.attachments.map { mapAttachmentToDomain(it) },
            userId = entity.message.userId.toString(),
            isRead = entity.message.isRead,
            isDeleted = entity.message.isDeleted,
            readTime = entity.message.readTime,
            expireTime = entity.message.expireTime,
            createTime = entity.message.createTime,
            updateTime = entity.message.updateTime
        )
    }
    
    fun mapToEntity(domain: Message): MessageEntity {
        return MessageEntity(
            msgId = domain.id,
            sourceApp = domain.sourceApp,
            sourceName = domain.sourceName,
            categoryId = domain.category.ordinal,
            priority = domain.priority.level,
            title = domain.title,
            content = domain.content,
            contentType = domain.contentType.value,
            actionType = domain.actionType.value,
            actionData = domain.actionData,
            iconUrl = domain.iconUrl,
            userId = domain.userId.toLongOrNull() ?: 0,
            isRead = domain.isRead,
            isDeleted = domain.isDeleted,
            readTime = domain.readTime,
            expireTime = domain.expireTime,
            createTime = domain.createTime,
            updateTime = domain.updateTime
        )
    }
    
    fun mapAttachmentToDomain(entity: MessageAttachmentEntity): Attachment {
        return Attachment(
            id = entity.id.toString(),
            messageId = entity.msgId.toString(),
            fileName = entity.fileName,
            filePath = entity.filePath,
            fileType = FileType.fromValue(entity.fileType),
            fileSize = entity.fileSize,
            mimeType = entity.mimeType ?: "",
            thumbnailPath = entity.thumbnail,
            createTime = entity.createTime
        )
    }
    
    fun mapAttachmentToEntity(domain: Attachment): MessageAttachmentEntity {
        return MessageAttachmentEntity(
            msgId = domain.messageId.toLongOrNull() ?: 0,
            fileName = domain.fileName,
            filePath = domain.filePath,
            fileType = domain.fileType.value,
            fileSize = domain.fileSize,
            mimeType = domain.mimeType,
            thumbnail = domain.thumbnailPath
        )
    }
}
```

### 6.4 数据库配置

```kotlin
/**
 * Room数据库配置
 */
@Database(
    entities = [
        MessageEntity::class,
        MessageCategoryEntity::class,
        MessageAttachmentEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(MessageConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun categoryDao(): MessageCategoryDao
}

/**
 * 类型转换器
 */
class MessageConverters {
    
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }
    
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
    
    @TypeConverter
    fun fromStringList(value: String?): List<String>? {
        return value?.split(",")?.map { it.trim() }
    }
    
    @TypeConverter
    fun toStringList(list: List<String>?): String? {
        return list?.joinToString(",")
    }
}

/**
 * 数据库模块 (Hilt DI)
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "cockpit_main.db"
        )
            .enableMultiInstanceInvalidation()
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // 初始化默认分类数据
                    db.execSQL("""
                        INSERT INTO message_category (cat_code, cat_name, sort_order, is_system) 
                        VALUES 
                            ('NAV', '导航', 1, 1),
                            ('PHONE', '电话', 2, 1),
                            ('CAR', '车辆', 3, 1),
                            ('SYS', '系统', 4, 1),
                            ('MEDIA', '媒体', 5, 1),
                            ('SEC', '安全', 6, 1),
                            ('REC', '推荐', 7, 1),
                            ('SOC', '社交', 8, 1),
                            ('OTHER', '其他', 9, 1)
                    """)
                }
            })
            .build()
    }
    
    @Provides
    fun provideMessageDao(database: AppDatabase): MessageDao {
        return database.messageDao()
    }
    
    @Provides
    fun provideCategoryDao(database: AppDatabase): MessageCategoryDao {
        return database.categoryDao()
    }
}
```

---

## 7. UI层设计

### 7.1 Compose UI组件

```kotlin
/**
 * 消息列表页面
 */
@Composable
fun MessageListPage(
    viewModel: MessageCenterViewModel = hiltViewModel(),
    onMessageClick: (Message) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val drivingState by viewModel.drivingState.collectAsState()
    
    Scaffold(
        topBar = {
            MessageTopBar(
                unreadCount = uiState.unreadCount,
                drivingState = drivingState
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 分类筛选栏
            CategoryFilterBar(
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = { category ->
                    viewModel.updateFilter(MessageFilter(category = category))
                }
            )
            
            // 消息列表
            if (uiState.isLoading) {
                LoadingIndicator()
            } else {
                MessageList(
                    messages = uiState.messages,
                    onMessageClick = onMessageClick,
                    onMessageRead = { viewModel.markAsRead(it) },
                    onMessageDelete = { viewModel.deleteMessage(it) }
                )
            }
            
            // 智能推荐区
            if (uiState.recommendations.isNotEmpty()) {
                SmartRecommendations(
                    recommendations = uiState.recommendations
                )
            }
        }
    }
    
    // 错误提示
    uiState.error?.let { error ->
        ErrorSnackbar(
            message = error,
            onDismiss = { viewModel.clearError() }
        )
    }
}

/**
 * 消息弹窗卡片
 * ASIL等级: ASIL A
 */
@Composable
fun MessagePopupCard(
    message: Message,
    config: PopupConfig,
    onDismiss: () -> Unit,
    onClick: () -> Unit
) {
    val backgroundColor = when (message.priority) {
        MessagePriority.P0_EMERGENCY -> Color(0xFFE53935)  // 红色警告
        MessagePriority.P1_HIGH -> Color(0xFFFB8C00)       // 橙色高优
        MessagePriority.P2_MEDIUM -> Color(0xFF1E88E5)     // 蓝色普通
        MessagePriority.P3_LOW -> Color(0xFF757575)        // 灰色低优
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        backgroundColor = backgroundColor,
        elevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            if (config.showIcon) {
                MessageIcon(message = message)
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            // 内容
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = message.title,
                    style = MaterialTheme.typography.subtitle1,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (message.content.isNotBlank()) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.body2,
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // 关闭按钮
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = Color.White
                )
            }
        }
    }
}

/**
 * 消息列表项
 */
@Composable
fun MessageListItem(
    message: Message,
    onClick: () -> Unit,
    onRead: () -> Unit,
    onDelete: () -> Unit
) {
    val unreadIndicator = if (!message.isRead) 4.dp else 0.dp
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        elevation = if (!message.isRead) 4.dp else 1.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .drawBehind {
                    if (!message.isRead) {
                        drawCircle(
                            color = Color(0xFF1E88E5),
                            radius = unreadIndicator.toPx(),
                            center = Offset(0f, size.height / 2)
                        )
                    }
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 来源图标
            AsyncImage(
                model = message.iconUrl,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                placeholder = painterResource(R.drawable.ic_message_default)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = message.sourceName,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.primary
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // 优先级标签
                    PriorityBadge(priority = message.priority)
                }
                
                Text(
                    text = message.title,
                    style = MaterialTheme.typography.subtitle2,
                    fontWeight = if (!message.isRead) FontWeight.Bold else FontWeight.Normal
                )
                
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = formatTimestamp(message.createTime),
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
                )
            }
            
            // 操作菜单
            MessageItemMenu(
                onRead = onRead,
                onDelete = onDelete,
                isRead = message.isRead
            )
        }
    }
}

/**
 * 优先级徽章
 */
@Composable
fun PriorityBadge(priority: MessagePriority) {
    val (text, color) = when (priority) {
        MessagePriority.P0_EMERGENCY -> "紧急" to Color(0xFFE53935)
        MessagePriority.P1_HIGH -> "高" to Color(0xFFFB8C00)
        MessagePriority.P2_MEDIUM -> "中" to Color(0xFF1E88E5)
        MessagePriority.P3_LOW -> "低" to Color(0xFF757575)
    }
    
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.caption,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
```

---

## 8. 安全设计

### 8.1 ASIL A安全机制

```kotlin
/**
 * ASIL A级安全监控器
 * 需求追溯: REQ-SAF-004
 */
class MessageSafetyMonitor @Inject constructor(
    private val vehicleService: IVehicleService
) {
    private val safetyScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineName("SafetyMonitor")
    )
    
    /**
     * 验证弹窗位置安全性
     */
    fun verifyPopupPosition(position: PopupPosition, drivingState: DrivingState): Boolean {
        return when (drivingState) {
            DrivingState.STOPPED -> true
            else -> position == PopupPosition.BOTTOM_SAFE
        }
    }
    
    /**
     * 验证弹窗尺寸安全性
     */
    fun verifyPopupSize(size: PopupSize, drivingState: DrivingState): Boolean {
        return when (drivingState) {
            DrivingState.STOPPED -> true
            else -> size == PopupSize.COMPACT
        }
    }
    
    /**
     * E2E保护 (End-to-End Protection)
     * 对P0级消息进行E2E校验
     */
    fun calculateE2EChecksum(message: Message): Int {
        val data = "${message.id}${message.priority}${message.title}${message.timestamp}"
        return CRC32().apply { update(data.toByteArray()) }.value.toInt()
    }
    
    fun verifyE2E(message: Message, expectedChecksum: Int): Boolean {
        return calculateE2EChecksum(message) == expectedChecksum
    }
}
```

### 8.2 数据安全

```kotlin
/**
 * 敏感数据加密存储
 */
class SecureStorage @Inject constructor(
    private val context: Context
) {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    
    /**
     * 加密存储消息内容
     */
    fun encryptMessageContent(content: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val encrypted = cipher.doFinal(content.toByteArray())
        return Base64.encodeToString(encrypted, Base64.DEFAULT)
    }
    
    private fun getSecretKey(): SecretKey {
        // 从Android Keystore获取或创建密钥
        return keyStore.getKey("message_key", null) as? SecretKey 
            ?: generateKey()
    }
    
    private fun generateKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                "message_key",
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return keyGenerator.generateKey()
    }
}
```

---

## 9. 性能设计

### 9.1 性能优化策略

```kotlin
/**
 * 性能优化配置
 */
object MessagePerformanceConfig {
    
    // 消息缓存配置
    const val MESSAGE_CACHE_SIZE = 100              // 内存缓存消息数
    const val MESSAGE_CACHE_EXPIRY_MINUTES = 5L     // 缓存过期时间
    
    // 数据库查询配置
    const val DEFAULT_PAGE_SIZE = 20                // 默认分页大小
    const val MAX_PAGE_SIZE = 100                   // 最大分页大小
    const val QUERY_TIMEOUT_MS = 500L               // 查询超时时间
    
    // 弹窗性能配置
    const val POPUP_DISPLAY_DELAY_MS = 100L         // P0消息显示延迟
    const val POPUP_QUEUE_MAX_SIZE = 10             // 弹窗队列最大值
    
    // 清理配置
    const val CLEANUP_BATCH_SIZE = 100              // 批量清理大小
    const val CLEANUP_INTERVAL_HOURS = 24L          // 清理间隔
}

/**
 * 消息缓存管理器
 */
class MessageCacheManager @Inject constructor() {
    private val cache = LruCache<String, Message>(MESSAGE_CACHE_SIZE)
    
    fun get(messageId: String): Message? = cache.get(messageId)
    
    fun put(message: Message) {
        cache.put(message.id, message)
    }
    
    fun invalidate(messageId: String) {
        cache.remove(messageId)
    }
    
    fun clear() {
        cache.evictAll()
    }
}
```

### 9.2 性能指标

| 指标项 | 目标值 | 最大允许值 | 测试方法 |
|--------|--------|-----------|----------|
| 消息发送延迟 | ≤100ms | ≤200ms | 单元测试 |
| 消息查询响应 | ≤50ms | ≤100ms | 性能测试 |
| 弹窗显示延迟(P0) | ≤100ms | ≤150ms | 自动化测试 |
| 弹窗显示延迟(P1) | ≤200ms | ≤300ms | 自动化测试 |
| 历史记录查询(30天) | ≤500ms | ≤1000ms | 压力测试 |
| 内存占用 | ≤50MB | ≤100MB | 内存分析 |

---

## 10. 需求追溯矩阵

### 10.1 SRS需求追溯

| SRS需求ID | 需求描述 | 设计元素 | 实现文件 | 测试用例 |
|-----------|----------|----------|----------|----------|
| REQ-MSG-FUN-001-QM | 消息统一管理（分类、排序、筛选） | MessageRepository, MessageFilter | MessageRepositoryImpl.kt, MessageDao.kt | TC-MSG-001 |
| REQ-MSG-FUN-002-QM | 历史记录查询（30天） | cleanupExpiredMessages, MessageFilter | CleanupExpiredMessagesUseCase.kt | TC-MSG-002 |
| REQ-MSG-FUN-003-ASIL_A | 消息优先级智能管理 | PriorityCalculator, MessagePriority | PriorityCalculator.kt | TC-MSG-003-ASIL_A |
| REQ-MSG-FUN-004-ASIL_A | 消息弹窗（驾驶安全） | DrivingSafetyController, MessagePopupManager | DrivingSafetyController.kt, MessagePopupManager.kt | TC-MSG-004-ASIL_A |
| REQ-MSG-FUN-005-QM | 活动资讯与通知管理 | Message.category=RECOMMENDATION | MessageRouter.kt | TC-MSG-005 |
| REQ-MSG-FUN-006-QM | 智能推荐管理 | UserBehaviorAnalyzer, SceneDetector | GetSmartRecommendationsUseCase.kt | TC-MSG-006 |
| REQ-PER-003-ASIL_A | 消息弹窗响应时间≤100ms | PopupConfig.displayTimeoutMs | MessagePopupManager.kt | TC-PER-003-ASIL_A |
| REQ-SAF-004-ASIL_A | 防止弹窗遮挡关键驾驶信息 | verifyPopupPosition, verifyPopupSize | MessageSafetyMonitor.kt | TC-SAF-004-ASIL_A |

### 10.2 架构追溯矩阵

| HLD组件 | DD实现类 | 接口定义 | 依赖服务 |
|---------|----------|----------|----------|
| FWK-004 消息管理 | MessageService | IMessageService.aidl | IVehicleService, ITtsService |
| APP-002 消息中心 | MessageCenterViewModel | MessageUiState | MessageRepository |
| SVC-004 数据服务 | MessageRepositoryImpl | IMessageRepository | MessageDao, AppDatabase |

### 10.3 数据库追溯矩阵

| 数据库表 | 实体类 | DAO接口 | 对应需求 |
|----------|--------|---------|----------|
| message | MessageEntity | MessageDao | REQ-MSG-FUN-001~006 |
| message_category | MessageCategoryEntity | MessageCategoryDao | REQ-MSG-FUN-001 |
| message_attachment | MessageAttachmentEntity | MessageDao | REQ-MSG-FUN-005 |

---

## 附录

### A. 文件结构

```
com/longcheer/cockpit/message/
├── data/
│   ├── local/
│   │   ├── dao/
│   │   │   ├── MessageDao.kt
│   │   │   └── MessageCategoryDao.kt
│   │   ├── entity/
│   │   │   ├── MessageEntity.kt
│   │   │   ├── MessageCategoryEntity.kt
│   │   │   └── MessageAttachmentEntity.kt
│   │   └── database/
│   │       ├── AppDatabase.kt
│   │       └── MessageConverters.kt
│   ├── mapper/
│   │   └── MessageMapper.kt
│   └── repository/
│       └── MessageRepositoryImpl.kt
├── domain/
│   ├── model/
│   │   ├── Message.kt
│   │   ├── MessagePriority.kt
│   │   ├── MessageCategory.kt
│   │   └── ...
│   ├── repository/
│   │   └── IMessageRepository.kt
│   └── usecase/
│       ├── GetMessagesUseCase.kt
│       ├── SendMessageUseCase.kt
│       ├── MarkAsReadUseCase.kt
│       └── ...
├── service/
│   ├── MessageService.kt
│   ├── PriorityCalculator.kt
│   ├── DrivingSafetyController.kt
│   ├── MessagePopupManager.kt
│   ├── VoiceAnnouncer.kt
│   ├── UserBehaviorAnalyzer.kt
│   └── SceneDetector.kt
├── presentation/
│   ├── viewmodel/
│   │   └── MessageCenterViewModel.kt
│   └── ui/
│       ├── MessageListPage.kt
│       ├── MessagePopupCard.kt
│       ├── MessageListItem.kt
│       └── ...
└── aidl/
    ├── IMessageService.aidl
    ├── IMessageListener.aidl
    ├── MessageParcel.aidl
    └── MessageFilterParcel.aidl
```

### B. 依赖注入配置

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class MessageModule {
    
    @Binds
    abstract fun bindMessageRepository(
        impl: MessageRepositoryImpl
    ): IMessageRepository
}
```

### C. 配置文件示例

```xml
<!-- AndroidManifest.xml -->
<service
    android:name=".service.MessageService"
    android:enabled="true"
    android:exported="true"
    android:process=":message">
    <intent-filter>
        <action android:name="com.longcheer.cockpit.message.IMessageService"/>
    </intent-filter>
</service>
```

---

**文档结束**

*本详细设计文档符合ASPICE Level 3要求，建立了从概要设计到详细设计的完整追溯链。*

**编制**: 详细设计工程师  
**审核**: 系统架构师  
**批准**: 项目总监  
**日期**: 2024-06-20
