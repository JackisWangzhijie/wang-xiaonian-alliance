package com.longcheer.cockpit.drv.monitor

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.os.ServiceManager
import android.util.Log
import com.longcheer.cockpit.drv.*
import com.longcheer.cockpit.drv.e2e.E2EProtectionHandler
import com.longcheer.cockpit.drv.model.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * 车辆信号监听器
 * ASIL等级: ASIL B
 * 
 * 职责:
 * - 订阅车辆服务信号
 * - 执行E2E保护校验
 * - 信号有效性验证
 * - 车速监测特定处理
 */
class VehicleSignalMonitor(private val context: Context) {

    companion object {
        private const val TAG = "VehicleSignalMonitor"
        
        /** 车速信号CAN ID */
        const val CAN_ID_SPEED = 0x130
        
        /** 挡位信号CAN ID */
        const val CAN_ID_GEAR = 0x131
        
        /** 驻车制动信号CAN ID */
        const val CAN_ID_PARKING_BRAKE = 0x132
    }

    // 车辆服务接口
    private var vehicleService: IVehicleService? = null
    
    // E2E处理器
    private lateinit var e2eHandler: E2EProtectionHandler
    
    // 信号回调
    private val signalCallbacks = CopyOnWriteArrayList<(VehicleSignal) -> Unit>()
    
    // 错误回调
    private var errorCallback: ((String, E2EStatus) -> Unit)? = null
    
    // 信号缓存用于冗余校验
    private var lastValidSignal: VehicleSignal? = null
    private val signalLock = Object()
    
    // 运行状态
    private var isRunning = false
    
    // 调度器
    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "VehicleSignalMonitor-Thread").apply { isDaemon = true }
    }

    /**
     * 初始化信号监听
     * 
     * @param callback 信号回调
     */
    fun initialize(callback: (VehicleSignal) -> Unit) {
        this.signalCallbacks.add(callback)
        this.e2eHandler = E2EProtectionHandler()
        
        bindVehicleService()
    }

    /**
     * 绑定车辆服务
     */
    private fun bindVehicleService() {
        try {
            // 通过ServiceManager获取系统服务
            val binder = ServiceManager.getService("vehicle")
            if (binder != null) {
                vehicleService = IVehicleService.Stub.asInterface(binder)
                registerListeners()
                isRunning = true
                Log.i(TAG, "Vehicle service bound successfully")
            } else {
                Log.e(TAG, "Failed to get vehicle service")
                // 尝试备用绑定方式
                bindVehicleServiceAlternative()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error binding vehicle service", e)
            bindVehicleServiceAlternative()
        }
    }

    /**
     * 备用绑定方式 (通过Context)
     */
    private fun bindVehicleServiceAlternative() {
        val intent = Intent().apply {
            setClassName("com.android.car", "com.android.car.VehicleService")
        }
        
        try {
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            Log.e(TAG, "Alternative binding also failed", e)
        }
    }

    /**
     * 服务连接回调
     */
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            vehicleService = IVehicleService.Stub.asInterface(service)
            registerListeners()
            isRunning = true
            Log.i(TAG, "Vehicle service connected via alternative binding")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            vehicleService = null
            isRunning = false
            Log.w(TAG, "Vehicle service disconnected")
        }
    }

    /**
     * 注册车辆信号监听
     */
    private fun registerListeners() {
        val service = vehicleService ?: return
        
        try {
            // 注册车速监听 (100Hz = 10ms周期)
            service.registerSpeedListener(speedListener, 10)
            
            // 注册挡位监听 (20Hz = 50ms周期)
            service.registerGearListener(gearListener)
            
            // 注册驻车制动监听 (10Hz = 100ms周期)
            service.registerParkingBrakeListener(parkingBrakeListener)
            
            Log.i(TAG, "All vehicle signal listeners registered")
            
        } catch (e: RemoteException) {
            Log.e(TAG, "Error registering listeners", e)
        }
    }

    /**
     * 车速监听回调
     */
    private val speedListener = object : ISpeedListener.Stub() {
        override fun onSpeedChanged(speed: Int, e2eData: E2EData) {
            val signal = VehicleSignal(
                type = SignalType.SPEED,
                speed = speed,
                e2eCounter = e2eData.counter,
                e2eCrc = e2eData.crc,
                timestamp = e2eData.timestamp
            )
            processSignal(signal)
        }
    }

    /**
     * 挡位监听回调
     */
    private val gearListener = object : IGearListener.Stub() {
        override fun onGearChanged(gear: Int, e2eData: E2EData) {
            val signal = VehicleSignal(
                type = SignalType.GEAR,
                gear = GearPosition.fromInt(gear),
                e2eCounter = e2eData.counter,
                e2eCrc = e2eData.crc,
                timestamp = e2eData.timestamp
            )
            processSignal(signal)
        }
    }

    /**
     * 驻车制动监听回调
     */
    private val parkingBrakeListener = object : IParkingBrakeListener.Stub() {
        override fun onParkingBrakeChanged(isOn: Boolean, e2eData: E2EData) {
            val signal = VehicleSignal(
                type = SignalType.PARKING_BRAKE,
                parkingBrake = isOn,
                e2eCounter = e2eData.counter,
                e2eCrc = e2eData.crc,
                timestamp = e2eData.timestamp
            )
            processSignal(signal)
        }
    }

    /**
     * 处理车辆信号
     * ASIL B: 完整的E2E保护
     * 
     * @param signal 车辆信号
     */
    private fun processSignal(signal: VehicleSignal) {
        // 1. E2E完整性校验
        val e2eResult = e2eHandler.verifyE2E(signal)
        
        when (e2eResult.status) {
            E2EStatus.VALID -> {
                synchronized(signalLock) {
                    lastValidSignal = signal
                }
                // 通知所有监听者
                signalCallbacks.forEach { callback ->
                    try {
                        callback(signal)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in signal callback", e)
                    }
                }
            }
            E2EStatus.COUNTER_ERROR -> {
                handleE2EError(signal.type.name, E2EStatus.COUNTER_ERROR, 
                    "Counter sequence error: ${signal.type}")
            }
            E2EStatus.CRC_ERROR -> {
                handleE2EError(signal.type.name, E2EStatus.CRC_ERROR, 
                    "CRC mismatch: ${signal.type}")
            }
            E2EStatus.TIMEOUT -> {
                handleE2EError(signal.type.name, E2EStatus.TIMEOUT, 
                    "Signal timeout: ${signal.type}")
            }
            else -> {
                Log.w(TAG, "Unknown E2E status: ${e2eResult.status}")
            }
        }
    }

    /**
     * 处理E2E错误
     */
    private fun handleE2EError(signalId: String, status: E2EStatus, message: String) {
        Log.e(TAG, message)
        errorCallback?.invoke(signalId, status)
        
        // 检查连续错误次数
        if (e2eHandler.isConsecutiveErrorLimitReached(signalId)) {
            Log.e(TAG, "Consecutive E2E errors limit reached for $signalId")
            // 通知上层进入安全状态
            errorCallback?.invoke(signalId, E2EStatus.CRC_ERROR)
        }
    }

    /**
     * 设置错误回调
     */
    fun setErrorCallback(callback: (String, E2EStatus) -> Unit) {
        this.errorCallback = callback
    }

    /**
     * 获取最后有效信号
     */
    fun getLastValidSignal(): VehicleSignal? {
        return synchronized(signalLock) { lastValidSignal }
    }

    /**
     * 获取组合信号
     */
    fun getCombinedSignal(): VehicleSignal? {
        return synchronized(signalLock) { lastValidSignal }
    }

    /**
     * 获取E2E状态
     */
    fun getE2EStatus(): Map<String, E2EStatusInfo> {
        return e2eHandler.getE2EStatus()
    }

    /**
     * 重置E2E状态
     */
    fun resetE2E() {
        e2eHandler.resetAll()
    }

    /**
     * 停止监听
     */
    fun stop() {
        isRunning = false
        
        try {
            vehicleService?.let { service ->
                service.unregisterSpeedListener(speedListener)
                service.unregisterGearListener(gearListener)
                service.unregisterParkingBrakeListener(parkingBrakeListener)
            }
        } catch (e: RemoteException) {
            Log.e(TAG, "Error unregistering listeners", e)
        }
        
        try {
            context.unbindService(serviceConnection)
        } catch (e: Exception) {
            // 忽略未绑定的错误
        }
        
        executor.shutdown()
        signalCallbacks.clear()
        
        Log.i(TAG, "Vehicle signal monitor stopped")
    }

    /**
     * 是否运行中
     */
    fun isRunning(): Boolean = isRunning
}

/**
 * 车速监测器 (专门用于车速监测)
 */
class VehicleSpeedMonitor(context: Context) : VehicleSignalMonitor(context) {
    
    companion object {
        private const val TAG = "VehicleSpeedMonitor"
    }
    
    private val speedCallbacks = CopyOnWriteArrayList<(Int) -> Unit>()
    
    /**
     * 注册车速回调
     */
    fun registerSpeedCallback(callback: (Int) -> Unit) {
        speedCallbacks.add(callback)
    }
    
    /**
     * 注销车速回调
     */
    fun unregisterSpeedCallback(callback: (Int) -> Unit) {
        speedCallbacks.remove(callback)
    }
}

/**
 * 挡位监测器 (专门用于挡位监测)
 */
class GearPositionMonitor(context: Context) : VehicleSignalMonitor(context) {
    
    companion object {
        private const val TAG = "GearPositionMonitor"
    }
    
    private val gearCallbacks = CopyOnWriteArrayList<(GearPosition) -> Unit>()
    
    /**
     * 注册挡位回调
     */
    fun registerGearCallback(callback: (GearPosition) -> Unit) {
        gearCallbacks.add(callback)
    }
    
    /**
     * 注销挡位回调
     */
    fun unregisterGearCallback(callback: (GearPosition) -> Unit) {
        gearCallbacks.remove(callback)
    }
}
