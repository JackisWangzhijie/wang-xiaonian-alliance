package com.longcheer.cockpit.message.data.local.database

import androidx.room.TypeConverter
import java.util.*

/**
 * Room数据库类型转换器
 * 用于实体类与数据库字段之间的类型转换
 */
class MessageConverters {
    
    /**
     * 将时间戳转换为Date对象
     */
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }
    
    /**
     * 将Date对象转换为时间戳
     */
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
    
    /**
     * 将逗号分隔的字符串转换为字符串列表
     */
    @TypeConverter
    fun fromStringList(value: String?): List<String>? {
        return value?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
    }
    
    /**
     * 将字符串列表转换为逗号分隔的字符串
     */
    @TypeConverter
    fun toStringList(list: List<String>?): String? {
        return list?.joinToString(",")
    }
    
    /**
     * 将逗号分隔的数字字符串转换为整数列表
     */
    @TypeConverter
    fun fromIntList(value: String?): List<Int>? {
        return value?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.mapNotNull { it.toIntOrNull() }
    }
    
    /**
     * 将整数列表转换为逗号分隔的数字字符串
     */
    @TypeConverter
    fun toIntList(list: List<Int>?): String? {
        return list?.joinToString(",")
    }
}
