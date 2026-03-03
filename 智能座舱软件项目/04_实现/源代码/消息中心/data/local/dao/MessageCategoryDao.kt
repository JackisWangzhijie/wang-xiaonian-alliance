package com.longcheer.cockpit.message.data.local.dao

import androidx.room.*
import com.longcheer.cockpit.message.data.local.entity.MessageCategoryEntity

/**
 * 消息分类数据访问对象
 */
@Dao
interface MessageCategoryDao {
    
    /**
     * 获取所有可见分类
     * 按排序顺序排列
     * 
     * @return 分类实体列表
     */
    @Query("SELECT * FROM message_category WHERE is_visible = 1 ORDER BY sort_order")
    suspend fun getAllCategories(): List<MessageCategoryEntity>
    
    /**
     * 根据ID获取分类
     * 
     * @param id 分类ID
     * @return 分类实体，不存在时返回null
     */
    @Query("SELECT * FROM message_category WHERE id = :id")
    suspend fun getCategoryById(id: Int): MessageCategoryEntity?
    
    /**
     * 根据代码获取分类
     * 
     * @param code 分类代码
     * @return 分类实体，不存在时返回null
     */
    @Query("SELECT * FROM message_category WHERE cat_code = :code")
    suspend fun getCategoryByCode(code: String): MessageCategoryEntity?
    
    /**
     * 插入分类
     * 
     * @param category 分类实体
     * @return 插入的行ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: MessageCategoryEntity): Long
    
    /**
     * 批量插入分类
     * 
     * @param categories 分类实体列表
     * @return 插入的行ID列表
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<MessageCategoryEntity>): List<Long>
    
    /**
     * 更新分类
     * 
     * @param category 分类实体
     * @return 受影响的行数
     */
    @Update
    suspend fun updateCategory(category: MessageCategoryEntity): Int
    
    /**
     * 删除分类
     * 
     * @param category 分类实体
     * @return 删除的行数
     */
    @Delete
    suspend fun deleteCategory(category: MessageCategoryEntity): Int
    
    /**
     * 根据ID删除分类
     * 
     * @param id 分类ID
     * @return 删除的行数
     */
    @Query("DELETE FROM message_category WHERE id = :id")
    suspend fun deleteCategoryById(id: Int): Int
}
