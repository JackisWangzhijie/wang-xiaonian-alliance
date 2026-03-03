package com.longcheer.cockpit.message.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.longcheer.cockpit.message.data.local.dao.MessageDao
import com.longcheer.cockpit.message.data.local.dao.MessageCategoryDao
import com.longcheer.cockpit.message.data.local.entity.MessageEntity
import com.longcheer.cockpit.message.data.local.entity.MessageCategoryEntity
import com.longcheer.cockpit.message.data.local.entity.MessageAttachmentEntity

/**
 * Room数据库配置
 * 
 * 数据库版本: 1
 * 包含实体: MessageEntity, MessageCategoryEntity, MessageAttachmentEntity
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
    
    /**
     * 获取消息DAO
     */
    abstract fun messageDao(): MessageDao
    
    /**
     * 获取分类DAO
     */
    abstract fun categoryDao(): MessageCategoryDao
    
    companion object {
        const val DATABASE_NAME = "cockpit_message.db"
        
        /**
         * 数据库创建回调
         * 初始化默认分类数据
         */
        val CALLBACK = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // 初始化默认分类数据
                initDefaultCategories(db)
            }
        }
        
        private fun initDefaultCategories(db: SupportSQLiteDatabase) {
            val categories = listOf(
                Triple("NAV", "导航", 1),
                Triple("PHONE", "电话", 2),
                Triple("CAR", "车辆", 3),
                Triple("SYS", "系统", 4),
                Triple("MEDIA", "媒体", 5),
                Triple("SEC", "安全", 6),
                Triple("REC", "推荐", 7),
                Triple("SOC", "社交", 8),
                Triple("OTHER", "其他", 9)
            )
            
            categories.forEach { (code, name, order) ->
                db.execSQL(
                    """
                    INSERT INTO message_category (cat_code, cat_name, sort_order, is_system) 
                    VALUES (?, ?, ?, 1)
                    """.trimIndent(),
                    arrayOf(code, name, order)
                )
            }
        }
    }
}
