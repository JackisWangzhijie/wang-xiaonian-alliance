package com.longcheer.cockpit.message.data.local.entity

import androidx.room.*

/**
 * 消息表实体
 * 对应数据库表: message
 * 
 * 索引设计：
 * - msg_id: 唯一索引，快速查找特定消息
 * - user_id + is_deleted + create_time: 复合索引，优化列表查询
 * - category_id: 分类查询索引
 * - priority + is_read: 优先级和已读状态查询索引
 * - source_app: 来源应用查询索引
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
    val sourceName: String? = null,
    
    @ColumnInfo(name = "category_id")
    val categoryId: Int,
    
    @ColumnInfo(name = "priority")
    val priority: Int,  // 0=P0, 1=P1, 2=P2, 3=P3
    
    @ColumnInfo(name = "title")
    val title: String,
    
    @ColumnInfo(name = "content")
    val content: String? = null,
    
    @ColumnInfo(name = "content_type")
    val contentType: Int = 0,  // 0=文本, 1=富文本, 2=图文, 3=多媒体
    
    @ColumnInfo(name = "action_type")
    val actionType: Int = 0,  // 0=无, 1=跳转, 2=弹窗, 3=外部链接
    
    @ColumnInfo(name = "action_data")
    val actionData: String? = null,
    
    @ColumnInfo(name = "icon_url")
    val iconUrl: String? = null,
    
    @ColumnInfo(name = "user_id")
    val userId: Long = 0,
    
    @ColumnInfo(name = "is_read")
    val isRead: Boolean = false,
    
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,
    
    @ColumnInfo(name = "read_time")
    val readTime: Long? = null,
    
    @ColumnInfo(name = "expire_time")
    val expireTime: Long? = null,
    
    @ColumnInfo(name = "create_time")
    val createTime: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "update_time")
    val updateTime: Long = System.currentTimeMillis()
)
