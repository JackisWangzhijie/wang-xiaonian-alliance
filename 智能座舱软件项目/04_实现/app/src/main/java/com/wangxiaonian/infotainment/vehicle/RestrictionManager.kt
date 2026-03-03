/**
 * 行驶限制管理器
 * 监控驾驶状态并限制行驶中的交互
 * 
 * @author 王小年联盟
 * @version 1.0
 * @trace SRS-安全需求-001, DD-行驶限制-001
 */
@Singleton
class RestrictionManager @Inject constructor(
    private val vehiclePropertyHelper: VehiclePropertyHelper,
    private val logger: Logger,
    @ApplicationContext private val context: Context
) {
    private val _restrictionState = MutableStateFlow<RestrictionState>(RestrictionState.Full)
    val restrictionState: StateFlow<RestrictionState> = _restrictionState.asStateFlow()
    
    private var monitoringJob: Job? = null
    
    /**
     * 开始监控驾驶状态
     */
    fun startMonitoring(scope: CoroutineScope) {
        monitoringJob = scope.launch {
            // 合并多个车辆属性流
            combine(
                vehiclePropertyHelper.observeSpeed(),
                vehiclePropertyHelper.observeGear()
            ) { speed, gear ->
                calculateRestrictionState(speed, gear)
            }.collect { state ->
                if (_restrictionState.value != state) {
                    _restrictionState.value = state
                    logger.i("Driving restriction state changed: $state")
                    // 广播状态变化
                    broadcastRestrictionChange(state)
                }
            }
        }
    }
    
    /**
     * 停止监控
     */
    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
    }
    
    /**
     * 根据车速和挡位计算限制状态
     */
    private fun calculateRestrictionState(speed: Float, gear: Int): RestrictionState {
        return when {
            // 停车状态
            gear == VehicleGear.GEAR_PARK -> RestrictionState.Full
            // 倒车状态 - 限制更强
            gear == VehicleGear.GEAR_REVERSE -> RestrictionState.Critical
            // 行驶中 (车速 > 0)
            speed > DRIVING_SPEED_THRESHOLD -> {
                when {
                    speed > HIGH_SPEED_THRESHOLD -> RestrictionState.Critical
                    speed > MEDIUM_SPEED_THRESHOLD -> RestrictionState.Limited
                    else -> RestrictionState.Partial
                }
            }
            // 低速或空挡
            else -> RestrictionState.Partial
        }
    }
    
    /**
     * 广播限制状态变化
     */
    private fun broadcastRestrictionChange(state: RestrictionState) {
        val intent = Intent(ACTION_RESTRICTION_CHANGED).apply {
            putExtra(EXTRA_RESTRICTION_STATE, state.name)
            setPackage(context.packageName)
        }
        context.sendBroadcast(intent)
    }
    
    /**
     * 检查是否允许特定操作
     */
    fun isOperationAllowed(operation: RestrictedOperation): Boolean {
        return when (_restrictionState.value) {
            RestrictionState.Full -> true
            RestrictionState.Partial -> operation.level <= 1
            RestrictionState.Limited -> operation.level <= 2
            RestrictionState.Critical -> operation.level <= 3
        }
    }
    
    companion object {
        const val ACTION_RESTRICTION_CHANGED = "com.wangxiaonian.infotainment.RESTRICTION_CHANGED"
        const val EXTRA_RESTRICTION_STATE = "restriction_state"
        
        private const val DRIVING_SPEED_THRESHOLD = 5f // km/h
        private const val MEDIUM_SPEED_THRESHOLD = 30f // km/h
        private const val HIGH_SPEED_THRESHOLD = 80f // km/h
    }
}

/**
 * 限制状态
 */
enum class RestrictionState {
    Full,       // 无限制 (停车)
    Partial,    // 部分限制 (低速/空挡)
    Limited,    // 严格限制 (行驶中)
    Critical    // 危险限制 (高速/倒车)
}

/**
 * 受限操作类型
 */
enum class RestrictedOperation(val level: Int) {
    VIEW_NOTIFICATION(1),   // 查看通知
    DISMISS_NOTIFICATION(1), // 清除通知
    OPEN_APP(2),            // 打开应用
    SETTINGS(2),            // 设置操作
    TEXT_INPUT(3),          // 文本输入
    COMPLEX_INTERACTION(3)  // 复杂交互
}
