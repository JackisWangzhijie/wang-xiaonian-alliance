package com.longcheer.cockpit.message.data.local.entity

import androidx.room.*

/**
 * 消息附件实体
 * 对应数据库表: message_attachment
 * 
 * 外键约束：当关联的消息被删除时，级联删除附件
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
    val fileType: Int = 3,  // 0=图片, 1=视频, 2=音频, 3=其他
    
    @ColumnInfo(name = "file_size")
    val fileSize: Long = 0,
    
    @ColumnInfo(name = "mime_type")
    val mimeType: String? = null,
    
    @ColumnInfo(name = "thumbnail")
    val thumbnail: String? = null,
    
    @ColumnInfo(name = "create_time")
    val createTime: Long = System.currentTimeMillis()
)
