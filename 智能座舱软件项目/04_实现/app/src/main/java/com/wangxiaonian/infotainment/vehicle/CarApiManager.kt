/**
 * Car API 管理器
 * 负责管理与 CarService 的连接和车辆属性访问
 * 
 * @author 王小年联盟
 * @version 1.0
 * @trace SRS-001, SRS-002, DD-系统框架-001
 */
@Singleton
class CarApiManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: Logger
) {
    private var car: Car? = null
    private var carPropertyManager: CarPropertyManager? = null
    private var carInfoManager: CarInfoManager? = null
    
    private val _connectionState = MutableStateFlow<CarConnectionState>(CarConnectionState.Disconnected)
    val connectionState: StateFlow<CarConnectionState> = _connectionState.asStateFlow()
    
    private val callback = object : Car.CarServiceLifecycleListener {
        override fun onLifecycleChanged(car: Car, ready: Boolean) {
            if (ready) {
                this@CarApiManager.car = car
                initializeManagers()
                _connectionState.value = CarConnectionState.Connected
                logger.i("CarService connected successfully")
            } else {
                _connectionState.value = CarConnectionState.Disconnected
                logger.w("CarService disconnected")
            }
        }
    }
    
    /**
     * 建立与 CarService 的连接
     */
    fun connect() {
        try {
            car = Car.createCar(context, null, callback)
            car?.connect()
            _connectionState.value = CarConnectionState.Connecting
        } catch (e: Exception) {
            logger.e("Failed to connect to CarService", e)
            _connectionState.value = CarConnectionState.Error(e.message ?: "Unknown error")
        }
    }
    
    /**
     * 断开与 CarService 的连接
     */
    fun disconnect() {
        carPropertyManager = null
        carInfoManager = null
        car?.disconnect()
        car = null
        _connectionState.value = CarConnectionState.Disconnected
    }
    
    private fun initializeManagers() {
        car?.let { carInstance ->
            carPropertyManager = carInstance.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager
            carInfoManager = carInstance.getCarManager(Car.INFO_SERVICE) as? CarInfoManager
        }
    }
    
    /**
     * 获取 CarPropertyManager
     */
    fun getPropertyManager(): CarPropertyManager? = carPropertyManager
    
    /**
     * 获取 CarInfoManager
     */
    fun getInfoManager(): CarInfoManager? = carInfoManager
    
    /**
     * 检查 CarService 是否已连接
     */
    fun isConnected(): Boolean = car?.isConnected == true
}

/**
 * CarService 连接状态
 */
sealed class CarConnectionState {
    object Disconnected : CarConnectionState()
    object Connecting : CarConnectionState()
    object Connected : CarConnectionState()
    data class Error(val message: String) : CarConnectionState()
}
