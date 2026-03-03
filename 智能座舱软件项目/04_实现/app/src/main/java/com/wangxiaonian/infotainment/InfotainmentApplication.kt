/**
 * Application 类
 * 应用入口
 * 
 * @author 王小年联盟
 * @version 1.0
 */
@HiltAndroidApp
class InfotainmentApplication : Application() {
    
    @Inject lateinit var carApiManager: CarApiManager
    @Inject lateinit var restrictionManager: RestrictionManager
    @Inject lateinit var logger: Logger
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override fun onCreate() {
        super.onCreate()
        
        // 初始化日志
        Logger.init(this)
        logger.i("InfotainmentApplication started")
        
        // 连接 CarService
        connectToCarService()
        
        // 启动行驶限制监控
        restrictionManager.startMonitoring(applicationScope)
    }
    
    private fun connectToCarService() {
        try {
            carApiManager.connect()
        } catch (e: Exception) {
            logger.e("Failed to connect to CarService on startup", e)
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
        restrictionManager.stopMonitoring()
        carApiManager.disconnect()
        applicationScope.cancel()
    }
}

/**
 * 简单日志工具
 */
object Logger {
    private const val TAG = "Infotainment"
    
    fun init(context: Context) {
        // 可以在这里初始化更复杂的日志系统
    }
    
    fun d(message: String) {
        Log.d(TAG, message)
    }
    
    fun i(message: String) {
        Log.i(TAG, message)
    }
    
    fun w(message: String) {
        Log.w(TAG, message)
    }
    
    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
    }
}

@Singleton
class Logger @Inject constructor() {
    fun d(message: String) = Logger.d(message)
    fun i(message: String) = Logger.i(message)
    fun w(message: String) = Logger.w(message)
    fun e(message: String, throwable: Throwable? = null) = Logger.e(message, throwable)
}
