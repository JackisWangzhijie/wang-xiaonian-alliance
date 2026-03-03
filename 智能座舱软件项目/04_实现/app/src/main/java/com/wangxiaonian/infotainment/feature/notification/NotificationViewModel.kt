/**
 * 消息中心 ViewModel
 * 
 * @author 王小年联盟
 * @version 1.0
 */
@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val repository: NotificationRepository,
    private val ttsManager: TtsManager,
    private val restrictionManager: RestrictionManager
) : ViewModel() {
    
    // 通知列表
    val notifications: StateFlow<List<NotificationEntity>> = repository
        .getNotificationsForCurrentState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // 未读数量
    val unreadCount: StateFlow<Int> = repository
        .getUnreadCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )
    
    // 当前限制状态
    val restrictionState: StateFlow<RestrictionState> = restrictionManager
        .restrictionState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RestrictionState.Full
        )
    
    /**
     * 标记通知已读
     */
    fun markAsRead(notificationId: Long) {
        viewModelScope.launch {
            repository.markAsRead(notificationId)
        }
    }
    
    /**
     * 清除通知
     */
    fun dismissNotification(notificationId: Long) {
        viewModelScope.launch {
            repository.dismissNotification(notificationId)
        }
    }
    
    /**
     * 朗读通知 (TTS)
     */
    fun speakNotification(notification: NotificationEntity) {
        // 驾驶中只朗读摘要
        val textToSpeak = when (restrictionManager.restrictionState.value) {
            RestrictionState.Critical, RestrictionState.Limited -> 
                notification.summary ?: "新通知来自${notification.source}"
            else -> "${notification.title}，${notification.content}"
        }
        
        ttsManager.speak(textToSpeak)
    }
    
    /**
     * 处理通知点击
     */
    fun onNotificationClick(notification: NotificationEntity) {
        if (!restrictionManager.isOperationAllowed(RestrictedOperation.OPEN_APP)) {
            // 驾驶中不能打开应用，只朗读
            speakNotification(notification)
            return
        }
        
        // 执行对应操作
        when (notification.actionType) {
            ActionType.OPEN_APP -> {
                // 导航到对应应用
            }
            ActionType.NAVIGATE -> {
                // 启动导航
            }
            ActionType.CALL -> {
                // 拨打电话
            }
            else -> {
                markAsRead(notification.id)
            }
        }
    }
}

/**
 * TTS 管理器
 */
@Singleton
class TtsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false
    
    init {
        textToSpeech = TextToSpeech(context) { status ->
            isInitialized = status == TextToSpeech.SUCCESS
        }
    }
    
    fun speak(text: String) {
        if (!isInitialized) return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            @Suppress("DEPRECATION")
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null)
        }
    }
    
    fun stop() {
        textToSpeech?.stop()
    }
    
    fun shutdown() {
        textToSpeech?.shutdown()
    }
}
