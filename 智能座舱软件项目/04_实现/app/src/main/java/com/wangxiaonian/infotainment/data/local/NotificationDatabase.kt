/**
 * 消息中心数据库
 * Room 数据库定义
 * 
 * @author 王小年联盟
 * @version 1.0
 */
@Database(
    entities = [NotificationEntity::class],
    version = 1,
    exportSchema = true
)
abstract class NotificationDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao
}

/**
 * 消息中心 DAO
 */
@Dao
interface NotificationDao {
    
    @Query("SELECT * FROM notifications WHERE isDismissed = 0 ORDER BY createdAt DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>
    
    @Query("SELECT * FROM notifications WHERE isRead = 0 AND isDismissed = 0 ORDER BY createdAt DESC")
    fun getUnreadNotifications(): Flow<List<NotificationEntity>>
    
    @Query("SELECT * FROM notifications WHERE priority = :priority AND isDismissed = 0 ORDER BY createdAt DESC")
    fun getNotificationsByPriority(priority: NotificationPriority): Flow<List<NotificationEntity>>
    
    @Query("SELECT * FROM notifications WHERE category = :category AND isDismissed = 0 ORDER BY createdAt DESC")
    fun getNotificationsByCategory(category: NotificationCategory): Flow<List<NotificationEntity>>
    
    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0 AND isDismissed = 0")
    fun getUnreadCount(): Flow<Int>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)
    
    @Update
    suspend fun updateNotification(notification: NotificationEntity)
    
    @Query("UPDATE notifications SET isRead = 1 WHERE id = :notificationId")
    suspend fun markAsRead(notificationId: Long)
    
    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllAsRead()
    
    @Delete
    suspend fun deleteNotification(notification: NotificationEntity)
    
    @Query("DELETE FROM notifications WHERE id = :notificationId")
    suspend fun deleteNotification(notificationId: Long)
    
    @Query("DELETE FROM notifications WHERE isRead = 1")
    suspend fun deleteReadNotifications(): Int
    
    @Query("DELETE FROM notifications WHERE createdAt < :timestamp")
    suspend fun deleteNotificationsBefore(timestamp: Long)
    
    @Query("SELECT * FROM notifications WHERE id = :id LIMIT 1")
    suspend fun getNotificationById(id: Long): NotificationEntity?
}
