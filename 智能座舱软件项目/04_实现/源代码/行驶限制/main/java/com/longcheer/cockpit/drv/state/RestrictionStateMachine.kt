package com.longcheer.cockpit.drv.state

import android.util.Log
import com.longcheer.cockpit.drv.model.RestrictionState

/**
 * 行驶限制状态机
 * ASIL等级: ASIL B
 * 
 * 状态转换规则:
 * NORMAL -> RESTRICTED (满足限制条件)
 * RESTRICTED -> RECOVERING (满足恢复条件，延迟3s)
 * RECOVERING -> NORMAL (恢复完成)
 * ANY -> FAULT (故障/异常)
 * FAULT -> NORMAL (故障恢复)
 */
class RestrictionStateMachine {

    companion object {
        private const val TAG = "RestrictionStateMachine"
    }

    @Volatile
    private var currentState: RestrictionState = RestrictionState.NORMAL
    
    @Volatile
    private var previousState: RestrictionState = RestrictionState.NORMAL
    
    // 状态进入时间戳
    private var stateEnterTime: Long = System.currentTimeMillis()
    
    // 状态转换锁
    private val stateLock = Object()
    
    // 状态转换监听
    private val stateChangeListeners = mutableListOf<OnStateChangeListener>()

    /**
     * 状态转换监听接口
     */
    interface OnStateChangeListener {
        fun onStateChanged(from: RestrictionState, to: RestrictionState, timestamp: Long)
    }

    /**
     * 状态转换
     * ASIL B: 原子性保证
     * 
     * @param newState 目标状态
     * @return 是否转换成功
     */
    fun transitionTo(newState: RestrictionState): Boolean {
        synchronized(stateLock) {
            val fromState = currentState
            
            if (!canTransition(fromState, newState)) {
                Log.w(TAG, "Invalid state transition: $fromState -> $newState")
                return false
            }
            
            previousState = fromState
            currentState = newState
            stateEnterTime = System.currentTimeMillis()
            
            onStateChanged(fromState, newState)
            
            Log.i(TAG, "State transition: $fromState -> $newState")
            return true
        }
    }

    /**
     * 强制状态转换 (用于故障处理)
     * 
     * @param newState 目标状态
     */
    fun forceTransitionTo(newState: RestrictionState) {
        synchronized(stateLock) {
            val fromState = currentState
            previousState = fromState
            currentState = newState
            stateEnterTime = System.currentTimeMillis()
            
            onStateChanged(fromState, newState)
            
            Log.w(TAG, "Forced state transition: $fromState -> $newState")
        }
    }

    /**
     * 检查状态转换是否允许
     * 
     * @param from 当前状态
     * @param to 目标状态
     * @return 是否允许转换
     */
    private fun canTransition(from: RestrictionState, to: RestrictionState): Boolean {
        return when (from) {
            RestrictionState.NORMAL -> 
                to == RestrictionState.RESTRICTED || to == RestrictionState.FAULT
            RestrictionState.RESTRICTED -> 
                to == RestrictionState.RECOVERING || to == RestrictionState.FAULT
            RestrictionState.RECOVERING -> 
                to == RestrictionState.NORMAL || to == RestrictionState.FAULT
            RestrictionState.FAULT -> 
                to == RestrictionState.NORMAL // 故障恢复
        }
    }

    /**
     * 状态变化回调
     */
    private fun onStateChanged(from: RestrictionState, to: RestrictionState) {
        val timestamp = System.currentTimeMillis()
        
        // 通知监听者
        stateChangeListeners.forEach { listener ->
            try {
                listener.onStateChanged(from, to, timestamp)
            } catch (e: Exception) {
                Log.e(TAG, "Error notifying state change listener", e)
            }
        }
    }

    /**
     * 获取当前状态
     */
    fun getCurrentState(): RestrictionState = currentState
    
    /**
     * 获取之前状态
     */
    fun getPreviousState(): RestrictionState = previousState
    
    /**
     * 获取状态持续时间
     */
    fun getStateDuration(): Long {
        return System.currentTimeMillis() - stateEnterTime
    }
    
    /**
     * 添加状态变化监听
     */
    fun addStateChangeListener(listener: OnStateChangeListener) {
        synchronized(stateLock) {
            stateChangeListeners.add(listener)
        }
    }
    
    /**
     * 移除状态变化监听
     */
    fun removeStateChangeListener(listener: OnStateChangeListener) {
        synchronized(stateLock) {
            stateChangeListeners.remove(listener)
        }
    }
    
    /**
     * 检查是否处于限制状态
     */
    fun isRestricted(): Boolean {
        return currentState == RestrictionState.RESTRICTED || 
               currentState == RestrictionState.FAULT
    }
    
    /**
     * 检查是否处于故障状态
     */
    fun isFault(): Boolean {
        return currentState == RestrictionState.FAULT
    }
    
    /**
     * 检查是否处于恢复中状态
     */
    fun isRecovering(): Boolean {
        return currentState == RestrictionState.RECOVERING
    }
    
    /**
     * 检查是否处于正常状态
     */
    fun isNormal(): Boolean {
        return currentState == RestrictionState.NORMAL
    }
    
    /**
     * 获取状态机信息
     */
    fun getStateMachineInfo(): StateMachineInfo {
        return StateMachineInfo(
            currentState = currentState,
            previousState = previousState,
            stateDuration = getStateDuration(),
            stateEnterTime = stateEnterTime
        )
    }
    
    /**
     * 重置状态机
     */
    fun reset() {
        synchronized(stateLock) {
            previousState = currentState
            currentState = RestrictionState.NORMAL
            stateEnterTime = System.currentTimeMillis()
        }
    }
}

/**
 * 状态机信息数据类
 */
data class StateMachineInfo(
    val currentState: RestrictionState,
    val previousState: RestrictionState,
    val stateDuration: Long,
    val stateEnterTime: Long
)
