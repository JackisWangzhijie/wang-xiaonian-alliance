/**
 * 消息中心 Repository
 * 处理通知数据的存储、检索和同步
 * 
 * @author 王小年联盟
 * @version 1.0
 * @trace SRS-消息中心-001, DD-消息中心-001
 */
@Singleton
class NotificationRepository @Inject constructor(
    private val notificationDao: NotificationDao,
    private val carApiManager: CarApiManager,
    private val restrictionManager: RestrictionManager,
    private val logger: Logger,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    /**
     * 获取所有通知 Flow
     */
    fun getAllNotifications(): Flow<List<NotificationEntity>> = 
        notificationDao.getAllNotifications()
            .flowOn(ioDispatcher)
    
    /**
     * 获取未读通知
     */
    fun getUnreadNotifications(): Flow<List<NotificationEntity>> =
        notificationDao.getUnreadNotifications()
            .flowOn(ioDispatcher)
    
    /**
     * 获取紧急通知
     */
    fun getCriticalNotifications(): Flow<List<NotificationEntity>> =
        notificationDao.getNotificationsByPriority(NotificationPriority.CRITICAL)
            .flowOn(ioDispatcher)
    
    /**
     * 插入新通知
     */
    suspend fun insertNotification(notification: NotificationEntity): Long = 
        withContext(ioDispatcher) {
            try {
                val id = notificationDao.insertNotification(notification)
                logger.i("Notification inserted: id=$id, title=${notification.title}")
                id
            } catch (e: Exception) {
                logger.e("Failed to insert notification", e)
                -1
            }
        }
    
    /**
     * 标记通知为已读
     */
    suspend fun markAsRead(notificationId: Long) = withContext(ioDispatcher) {
        // 检查行驶限制
        if (!restrictionManager.isOperationAllowed(RestrictedOperation.VIEW_NOTIFICATION)) {
            logger.w("Cannot mark notification as read while driving")
            return@withContext
        }
        
        notificationDao.markAsRead(notificationId)
        logger.d("Notification marked as read: $notificationId")
    }
    
    /**
     * 清除通知
     */
    suspend fun dismissNotification(notificationId: Long) = withContext(ioDispatcher) {
        // 检查行驶限制
        if (!restrictionManager.isOperationAllowed(RestrictedOperation.DISMISS_NOTIFICATION)) {
            logger.w("Cannot dismiss notification while driving")
            return@withContext
        }
        
        notificationDao.deleteNotification(notificationId)
        logger.d("Notification dismissed: $notificationId")
    }
    
    /**
     * 清除所有已读通知
     */
    suspend fun clearReadNotifications() = withContext(ioDispatcher) {
        val count = notificationDao.deleteReadNotifications()
        logger.i("Cleared $count read notifications")
    }
    
    /**
     * 根据驾驶状态过滤通知
     */
    fun getNotificationsForCurrentState(): Flow<List<NotificationEntity>> {
        return combine(
            notificationDao.getAllNotifications(),
            restrictionManager.restrictionState
        ) { notifications, state ->
            when (state) {
                RestrictionState.Critical -> notifications.filter { 
                    it.priority == NotificationPriority.CRITICAL && it.category == NotificationCategory.VEHICLE
                }
                RestrictionState.Limited -> notifications.filter {
                    it.priority <= NotificationPriority.HIGH
                }
                else -> notifications
            }
        }.flowOn(ioDispatcher)
    }
    
    /**
     * 获取未读通知数量
     */
    fun getUnreadCount(): Flow<Int> = notificationDao.getUnreadCount()
}
