/**
 * 车辆属性助手
 * 封装 CarPropertyManager 的常用操作
 * 
 * @author 王小年联盟
 * @version 1.0
 * @trace SRS-003, DD-系统框架-002
 */
@Singleton
class VehiclePropertyHelper @Inject constructor(
    private val carApiManager: CarApiManager,
    private val logger: Logger,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val propertyCache = ConcurrentHashMap<Int, Any>()
    
    /**
     * 获取车辆属性值
     * @param propertyId 车辆属性 ID
     * @return 属性值，如果获取失败返回 null
     */
    suspend fun <T> getProperty(propertyId: Int): T? = withContext(ioDispatcher) {
        try {
            carApiManager.getPropertyManager()?.let { manager ->
                @Suppress("UNCHECKED_CAST")
                manager.getProperty<T>(propertyId)?.value as? T
            }
        } catch (e: Exception) {
            logger.e("Failed to get property $propertyId", e)
            null
        }
    }
    
    /**
     * 设置车辆属性值
     * @param propertyId 车辆属性 ID
     * @param value 要设置的值
     * @return 是否设置成功
     */
    suspend fun <T> setProperty(propertyId: Int, value: T): Boolean = withContext(ioDispatcher) {
        try {
            carApiManager.getPropertyManager()?.let { manager ->
                manager.setProperty(propertyId, value)
                true
            } ?: false
        } catch (e: Exception) {
            logger.e("Failed to set property $propertyId", e)
            false
        }
    }
    
    /**
     * 监听车辆属性变化
     * @param propertyIds 要监听的属性 ID 列表
     * @return 属性变化的 Flow
     */
    fun observeProperties(vararg propertyIds: Int): Flow<VehiclePropertyEvent> = callbackFlow {
        val manager = carApiManager.getPropertyManager()
        
        if (manager == null) {
            close(IllegalStateException("CarPropertyManager not available"))
            return@callbackFlow
        }
        
        val callback = object : CarPropertyManager.CarPropertyEventCallback {
            override fun onChangeEvent(event: CarPropertyValue<*>) {
                trySend(VehiclePropertyEvent.Changed(event.propertyId, event.value))
                    .onFailure { logger.w("Failed to send property change event") }
            }
            
            override fun onErrorEvent(propertyId: Int, zone: Int) {
                trySend(VehiclePropertyEvent.Error(propertyId, zone))
                    .onFailure { logger.w("Failed to send property error event") }
            }
        }
        
        // 注册监听
        propertyIds.forEach { propertyId ->
            try {
                manager.registerCallback(callback, propertyId)
            } catch (e: Exception) {
                logger.e("Failed to register callback for property $propertyId", e)
            }
        }
        
        awaitClose {
            propertyIds.forEach { propertyId ->
                try {
                    manager.unregisterCallback(callback, propertyId)
                } catch (e: Exception) {
                    logger.e("Failed to unregister callback for property $propertyId", e)
                }
            }
        }
    }.flowOn(ioDispatcher)
    
    /**
     * 获取当前车速 (km/h)
     */
    suspend fun getSpeed(): Float? = getProperty<Float>(VehiclePropertyIds.PERF_VEHICLE_SPEED)
        ?.let { it * 3.6f } // m/s to km/h
    
    /**
     * 获取当前挡位
     */
    suspend fun getGear(): Int? = getProperty<Int>(VehiclePropertyIds.GEAR_SELECTION)
    
    /**
     * 检查是否处于停车状态
     */
    suspend fun isParked(): Boolean {
        val gear = getGear()
        return gear == VehicleGear.GEAR_PARK
    }
    
    /**
     * 监听车速变化
     */
    fun observeSpeed(): Flow<Float> = observeProperties(VehiclePropertyIds.PERF_VEHICLE_SPEED)
        .filterIsInstance<VehiclePropertyEvent.Changed>()
        .map { (it.value as? Float)?.times(3.6f) ?: 0f }
        .distinctUntilChanged()
    
    /**
     * 监听挡位变化
     */
    fun observeGear(): Flow<Int> = observeProperties(VehiclePropertyIds.GEAR_SELECTION)
        .filterIsInstance<VehiclePropertyEvent.Changed>()
        .map { it.value as? Int ?: VehicleGear.GEAR_PARK }
        .distinctUntilChanged()
}

/**
 * 车辆属性事件
 */
sealed class VehiclePropertyEvent {
    data class Changed(val propertyId: Int, val value: Any?) : VehiclePropertyEvent()
    data class Error(val propertyId: Int, val zone: Int) : VehiclePropertyEvent()
}

/**
 * 常用车辆属性 ID
 */
object VehiclePropertyIds {
    const val PERF_VEHICLE_SPEED = 291504647 // 车速
    const val GEAR_SELECTION = 289408000 // 挡位
    const val PARKING_BRAKE_ON = 286261505 // 手刹
    const val IGNITION_STATE = 289408009 // 点火状态
    const val HVAC_TEMPERATURE_SET = 356517131 // 空调温度
    const val HVAC_FAN_SPEED = 356517120 // 风扇速度
    const val FUEL_LEVEL = 291504903 // 油量
    const val BATTERY_LEVEL = 291504904 // 电量
    const val ODOMETER = 291504648 // 里程
}

/**
 * 挡位常量
 */
object VehicleGear {
    const val GEAR_PARK = 4
    const val GEAR_REVERSE = 2
    const val GEAR_NEUTRAL = 3
    const val GEAR_DRIVE = 6
}
