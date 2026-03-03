package com.longcheer.cockpit.message.data.local.entity

import androidx.room.*

/**
 * 消息分类实体
 * 对应数据库表: message_category
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
    val catNameEn: String? = null,
    
    @ColumnInfo(name = "parent_id")
    val parentId: Int = 0,
    
    @ColumnInfo(name = "icon")
    val icon: String? = null,
    
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,
    
    @ColumnInfo(name = "is_system")
    val isSystem: Boolean = false,
    
    @ColumnInfo(name = "is_visible")
    val isVisible: Boolean = true,
    
    @ColumnInfo(name = "create_time")
    val createTime: Long = System.currentTimeMillis()
)
