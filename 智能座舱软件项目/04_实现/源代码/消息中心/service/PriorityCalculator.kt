package com.longcheer.cockpit.message.service

import com.longcheer.cockpit.message.domain.model.Message
import com.longcheer.cockpit.message.domain.model.MessageCategory
import com.longcheer.cockpit.message.domain.model.MessagePriority
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 优先级计算器
 * ASIL等级: ASIL A
 * 需求追溯: REQ-MSG-FUN-003
 * 
 * 根据消息内容、来源应用、当前场景综合计算消息优先级
 */
@Singleton
class PriorityCalculator @Inject constructor() {

    /**
     * 应用优先级映射表
     * 键：应用ID，值：优先级
     */
    private val appPriorities = ConcurrentHashMap<String, MessagePriority>()

    /**
     * 紧急关键词列表
     */
    private val emergencyKeywords = listOf(
        "碰撞", "故障", "告警", "紧急", "危险", "异常",
        "collision", "fault", "alert", "emergency", "danger"
    )

    /**
     * 高优先级关键词列表
     */
    private val highPriorityKeywords = listOf(
        "来电", "导航", "转弯", "限速", "超速", "电量低",
        "call", "navigation", "turn", "speed limit", "overspeed", "low battery"
    )

    /**
     * 计算消息优先级
     * 综合考虑预定义优先级、内容分析和来源分类
     * 
     * @param message 消息对象
     * @return 计算后的优先级
     */
    fun calculate(message: Message): MessagePriority {
        // 1. 检查内容是否包含紧急关键词（最高优先级）
        if (isEmergencyContent(message)) {
            return MessagePriority.P0_EMERGENCY
        }

        // 2. 检查是否有预定义的应用优先级
        val appPriority = appPriorities[message.sourceApp]
        if (appPriority != null && appPriority <= MessagePriority.P1_HIGH) {
            return appPriority
        }

        // 3. 根据分类判断
        val categoryPriority = getPriorityByCategory(message.category)
        if (categoryPriority <= MessagePriority.P1_HIGH) {
            return categoryPriority
        }

        // 4. 检查是否包含高优先级关键词
        if (isHighPriorityContent(message)) {
            return MessagePriority.P1_HIGH
        }

        // 5. 使用预定义或默认优先级
        return appPriority ?: message.priority
    }

    /**
     * 设置应用消息优先级
     * 
     * @param appId 应用ID
     * @param priority 优先级
     */
    fun setAppPriority(appId: String, priority: MessagePriority) {
        appPriorities[appId] = priority
    }

    /**
     * 获取应用的优先级
     * 
     * @param appId 应用ID
     * @return 优先级，未设置时返回null
     */
    fun getAppPriority(appId: String): MessagePriority? {
        return appPriorities[appId]
    }

    /**
     * 移除应用的优先级设置
     * 
     * @param appId 应用ID
     */
    fun removeAppPriority(appId: String) {
        appPriorities.remove(appId)
    }

    /**
     * 清空所有应用优先级设置
     */
    fun clearAllPriorities() {
        appPriorities.clear()
    }

    /**
     * 检查是否为紧急内容
     * 
     * @param message 消息对象
     * @return true表示包含紧急关键词
     */
    private fun isEmergencyContent(message: Message): Boolean {
        val text = "${message.title} ${message.content}"
        return emergencyKeywords.any { keyword ->
            text.contains(keyword, ignoreCase = true)
        }
    }

    /**
     * 检查是否为高优先级内容
     * 
     * @param message 消息对象
     * @return true表示包含高优先级关键词
     */
    private fun isHighPriorityContent(message: Message): Boolean {
        val text = "${message.title} ${message.content}"
        return highPriorityKeywords.any { keyword ->
            text.contains(keyword, ignoreCase = true)
        }
    }

    /**
     * 根据分类获取优先级
     * 
     * @param category 消息分类
     * @return 该分类对应的优先级
     */
    private fun getPriorityByCategory(category: MessageCategory): MessagePriority {
        return when (category) {
            MessageCategory.SECURITY -> MessagePriority.P0_EMERGENCY
            MessageCategory.VEHICLE -> MessagePriority.P1_HIGH
            MessageCategory.NAVIGATION -> MessagePriority.P1_HIGH
            MessageCategory.PHONE -> MessagePriority.P1_HIGH
            MessageCategory.SYSTEM -> MessagePriority.P2_MEDIUM
            MessageCategory.MEDIA -> MessagePriority.P3_LOW
            MessageCategory.RECOMMENDATION -> MessagePriority.P3_LOW
            MessageCategory.SOCIAL -> MessagePriority.P3_LOW
            MessageCategory.OTHER -> MessagePriority.P2_MEDIUM
        }
    }
}
