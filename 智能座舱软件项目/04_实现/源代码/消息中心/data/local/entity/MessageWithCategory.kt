package com.longcheer.cockpit.message.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * 消息与分类关联实体（用于联合查询）
 * 包含消息实体、分类实体和附件列表
 */
data class MessageWithCategory(
    @Embedded
    val message: MessageEntity,
    
    @Relation(
        parentColumn = "category_id",
        entityColumn = "id"
    )
    val category: MessageCategoryEntity? = null,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "msg_id"
    )
    val attachments: List<MessageAttachmentEntity> = emptyList()
)

/**
 * 消息统计行数据类
 * 用于DAO统计查询的结果
 */
data class MessageStatisticsRow(
    val total: Int,
    val readCount: Int,
    val categoryId: Int,
    val priority: Int,
    val sourceApp: String
)
