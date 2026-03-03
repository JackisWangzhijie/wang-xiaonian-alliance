package com.longcheer.cockpit.drv.e2e

import com.longcheer.cockpit.drv.model.*
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

/**
 * E2E保护处理器
 * ASIL等级: ASIL B
 * 
 * 实现基于AUTOSAR E2E Profile 2的端到端保护
 * - CRC8校验 (SAE J1850)
 * - 4位序列号(0-15)
 * - 100ms超时检测
 */
class E2EProtectionHandler {

    companion object {
        private const val TAG = "E2EProtectionHandler"
        
        /** 最大序列号值 */
        const val MAX_COUNTER = 15
        
        /** 超时阈值 (ms) */
        const val TIMEOUT_MS = 100L
        
        /** CRC8多项式 (SAE J1850) */
        const val CRC_POLYNOMIAL = 0x1D
        
        /** 初始CRC值 */
        const val CRC_INITIAL: Byte = 0xFF.toByte()
        
        /** Data ID映射 */
        val DATA_ID_MAP = mapOf(
            SignalType.SPEED to 0x0001,
            SignalType.GEAR to 0x0002,
            SignalType.PARKING_BRAKE to 0x0003
        )
    }

    // 每个信号源的序列号追踪
    private val counterMap = ConcurrentHashMap<String, Byte>()
    
    // 每个信号源的时间戳追踪
    private val timestampMap = ConcurrentHashMap<String, Long>()
    
    // 每个信号源的状态
    private val statusMap = ConcurrentHashMap<String, E2EStatus>()
    
    // 连续错误计数
    private val errorCountMap = ConcurrentHashMap<String, Int>()
    
    // 最大允许连续错误数
    private val MAX_CONSECUTIVE_ERRORS = 3

    /**
     * E2E验证入口
     * 
     * @param signal 车辆信号
     * @return E2E校验结果
     */
    fun verifyE2E(signal: VehicleSignal): E2EResult {
        val signalId = signal.type.name
        
        try {
            // 1. 超时检查
            if (!checkTimeout(signalId, signal.timestamp)) {
                updateStatus(signalId, E2EStatus.TIMEOUT)
                incrementErrorCount(signalId)
                return E2EResult(E2EStatus.TIMEOUT, "Signal timeout: elapsed=${System.currentTimeMillis() - (timestampMap[signalId] ?: 0)}ms")
            }
            
            // 2. CRC校验
            if (!verifyCRC(signal)) {
                updateStatus(signalId, E2EStatus.CRC_ERROR)
                incrementErrorCount(signalId)
                return E2EResult(E2EStatus.CRC_ERROR, "CRC verification failed for ${signal.type}")
            }
            
            // 3. 序列号检查
            if (!checkCounter(signalId, signal.e2eCounter)) {
                updateStatus(signalId, E2EStatus.COUNTER_ERROR)
                incrementErrorCount(signalId)
                val expected = getExpectedCounter(signalId)
                return E2EResult(E2EStatus.COUNTER_ERROR, 
                    "Counter sequence error: expected=$expected, received=${signal.e2eCounter}")
            }
            
            // 所有校验通过
            updateStatus(signalId, E2EStatus.VALID)
            resetErrorCount(signalId)
            updateState(signalId, signal.e2eCounter, signal.timestamp)
            
            return E2EResult(E2EStatus.VALID, "E2E check passed for ${signal.type}")
            
        } catch (e: Exception) {
            updateStatus(signalId, E2EStatus.CRC_ERROR)
            return E2EResult(E2EStatus.CRC_ERROR, "E2E verification exception: ${e.message}")
        }
    }

    /**
     * CRC8校验
     * 
     * @param signal 车辆信号
     * @return 校验是否通过
     */
    private fun verifyCRC(signal: VehicleSignal): Boolean {
        // 构造待校验数据
        val data = when (signal.type) {
            SignalType.SPEED -> {
                ByteBuffer.allocate(4).putInt(signal.speed).array()
            }
            SignalType.GEAR -> {
                byteArrayOf(signal.gear.ordinal.toByte())
            }
            SignalType.PARKING_BRAKE -> {
                byteArrayOf(if (signal.parkingBrake) 1 else 0)
            }
            else -> byteArrayOf()
        }
        
        // 获取Data ID
        val dataId = DATA_ID_MAP[signal.type] ?: 0x0000
        val dataIdBytes = byteArrayOf(
            (dataId shr 8).toByte(),
            dataId.toByte()
        )
        
        // 添加序列号到数据 (Profile 2格式)
        val dataWithCounter = dataIdBytes + data + signal.e2eCounter
        
        // 计算CRC
        val calculatedCrc = calculateCRC8(dataWithCounter)
        
        return calculatedCrc == signal.e2eCrc
    }

    /**
     * 计算CRC8 (SAE J1850标准)
     * 
     * 多项式: 0x1D (x^8 + x^4 + x^3 + x^2 + 1)
     * 
     * @param data 输入数据
     * @return CRC8校验值
     */
    fun calculateCRC8(data: ByteArray): Byte {
        var crc: Byte = CRC_INITIAL
        
        for (byte in data) {
            crc = crc.xor(byte)
            for (i in 0 until 8) {
                crc = if ((crc.toInt() and 0x80) != 0) {
                    ((crc.toInt() shl 1) xor CRC_POLYNOMIAL).toByte()
                } else {
                    (crc.toInt() shl 1).toByte()
                }
            }
        }
        
        // 最终异或
        return crc.xor(0xFF.toByte())
    }

    /**
     * 序列号检查
     * 允许小范围丢包但不允许乱序
     * 
     * @param signalId 信号ID
     * @param receivedCounter 接收到的序列号
     * @return 序列号是否有效
     */
    private fun checkCounter(signalId: String, receivedCounter: Byte): Boolean {
        val lastCounter = counterMap[signalId]
        
        // 首次接收，直接接受
        if (lastCounter == null) {
            return true
        }
        
        // 计算期望序列号
        val expectedCounter = ((lastCounter.toInt() + 1) and MAX_COUNTER).toByte()
        
        // 计算差值 (处理回绕)
        val diff = ((receivedCounter.toInt() - lastCounter.toInt() + MAX_COUNTER + 1) 
                    and MAX_COUNTER)
        
        // 检查是否是期望的序列号（允许最多丢2个包，diff范围1-3）
        return diff in 1..3
    }

    /**
     * 获取期望的序列号
     */
    private fun getExpectedCounter(signalId: String): Byte {
        val lastCounter = counterMap[signalId] ?: return 0
        return ((lastCounter.toInt() + 1) and MAX_COUNTER).toByte()
    }

    /**
     * 超时检查
     * 
     * @param signalId 信号ID
     * @param timestamp 信号时间戳
     * @return 是否未超时
     */
    private fun checkTimeout(signalId: String, timestamp: Long): Boolean {
        val lastTimestamp = timestampMap[signalId]
        
        // 首次接收，直接接受
        if (lastTimestamp == null) {
            return true
        }
        
        val elapsed = timestamp - lastTimestamp
        return elapsed <= TIMEOUT_MS
    }

    /**
     * 更新状态
     */
    private fun updateState(signalId: String, counter: Byte, timestamp: Long) {
        counterMap[signalId] = counter
        timestampMap[signalId] = timestamp
    }

    /**
     * 更新E2E状态
     */
    private fun updateStatus(signalId: String, status: E2EStatus) {
        statusMap[signalId] = status
    }

    /**
     * 增加错误计数
     */
    private fun incrementErrorCount(signalId: String) {
        val count = (errorCountMap[signalId] ?: 0) + 1
        errorCountMap[signalId] = count
    }

    /**
     * 重置错误计数
     */
    private fun resetErrorCount(signalId: String) {
        errorCountMap[signalId] = 0
    }

    /**
     * 检查是否超过最大连续错误
     */
    fun isConsecutiveErrorLimitReached(signalId: String): Boolean {
        return (errorCountMap[signalId] ?: 0) >= MAX_CONSECUTIVE_ERRORS
    }

    /**
     * 获取E2E状态信息
     * 
     * @return E2E状态信息映射
     */
    fun getE2EStatus(): Map<String, E2EStatusInfo> {
        return SignalType.values().associate { type ->
            val signalId = type.name
            val lastCounter = counterMap[signalId] ?: 0
            val lastTimestamp = timestampMap[signalId] ?: 0
            val elapsed = System.currentTimeMillis() - lastTimestamp
            
            signalId to E2EStatusInfo(
                signalId = signalId,
                lastCounter = lastCounter,
                lastTimestamp = lastTimestamp,
                isAlive = elapsed < TIMEOUT_MS * 2,
                status = statusMap[signalId] ?: E2EStatus.NO_DATA
            )
        }
    }

    /**
     * 重置指定信号的E2E状态
     */
    fun resetSignal(signalId: String) {
        counterMap.remove(signalId)
        timestampMap.remove(signalId)
        statusMap.remove(signalId)
        errorCountMap.remove(signalId)
    }

    /**
     * 重置所有E2E状态
     */
    fun resetAll() {
        counterMap.clear()
        timestampMap.clear()
        statusMap.clear()
        errorCountMap.clear()
    }

    /**
     * 获取序列号 (用于发送端)
     */
    fun getNextCounter(signalId: String): Byte {
        val current = counterMap[signalId] ?: (-1).toByte()
        return ((current.toInt() + 1) and MAX_COUNTER).toByte()
    }

    /**
     * 计算E2E保护数据 (用于发送端)
     */
    fun calculateE2E(signalType: SignalType, payload: ByteArray): Pair<Byte, Byte> {
        val signalId = signalType.name
        val counter = getNextCounter(signalId)
        
        // 获取Data ID
        val dataId = DATA_ID_MAP[signalType] ?: 0x0000
        val dataIdBytes = byteArrayOf(
            (dataId shr 8).toByte(),
            dataId.toByte()
        )
        
        // 计算CRC
        val dataWithCounter = dataIdBytes + payload + counter
        val crc = calculateCRC8(dataWithCounter)
        
        // 更新状态
        counterMap[signalId] = counter
        timestampMap[signalId] = System.currentTimeMillis()
        
        return Pair(counter, crc)
    }
}
