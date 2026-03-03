package com.longcheer.cockpit.drv.manager

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.util.Log
import com.longcheer.cockpit.drv.model.AppCategory
import com.longcheer.cockpit.drv.model.WhitelistEntry
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * 白名单管理器
 * ASIL等级: QM (非安全相关，但需防篡改)
 * 
 * 职责:
 * - 管理应用白名单
 * - 验证应用签名
 * - 白名单缓存
 */
class WhitelistManager(private val context: Context) {

    companion object {
        private const val TAG = "WhitelistManager"
        private const val PREFS_NAME = "drv_whitelist"
        private const val KEY_WHITELIST = "whitelist_entries"
        
        /** 内置白名单应用 */
        val BUILTIN_WHITELIST = setOf(
            // 导航类
            "com.autonavi.amapauto",
            "com.baidu.BaiduMap",
            "com.google.android.apps.maps",
            // 音乐类
            "com.tencent.qqmusic",
            "com.netease.cloudmusic",
            "com.spotify.music",
            // 通讯类
            "com.android.bluetooth",
            "com.google.android.dialer",
            // 车辆控制类
            "com.longcheer.vehicle",
            "com.longcheer.aircondition",
            // 系统类
            "com.android.launcher",
            "com.android.settings"
        )
    }

    // SharedPreferences
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // 白名单缓存 (包名 -> 白名单条目)
    private val whitelistCache = ConcurrentHashMap<String, WhitelistEntry>()
    
    // 类别映射 (包名 -> 类别)
    private val categoryMap = ConcurrentHashMap<String, AppCategory>()
    
    // 初始化标记
    private var initialized = false

    /**
     * 初始化白名单管理器
     */
    fun initialize() {
        if (initialized) return
        
        // 加载内置白名单
        loadBuiltinWhitelist()
        
        // 加载持久化白名单
        loadPersistedWhitelist()
        
        initialized = true
        Log.i(TAG, "WhitelistManager initialized with ${whitelistCache.size} entries")
    }

    /**
     * 加载内置白名单
     */
    private fun loadBuiltinWhitelist() {
        BUILTIN_WHITELIST.forEach { packageName ->
            val category = detectCategory(packageName)
            val entry = WhitelistEntry(
                packageName = packageName,
                appName = getAppName(packageName),
                category = category.name,
                addedTime = System.currentTimeMillis(),
                reason = "Built-in whitelist",
                signatureHash = getAppSignatureHash(packageName) ?: ""
            )
            whitelistCache[packageName] = entry
            categoryMap[packageName] = category
        }
    }

    /**
     * 加载持久化白名单
     */
    private fun loadPersistedWhitelist() {
        val entriesJson = prefs.getStringSet(KEY_WHITELIST, emptySet()) ?: return
        
        entriesJson.forEach { json ->
            try {
                val entry = parseWhitelistEntry(json)
                whitelistCache[entry.packageName] = entry
                categoryMap[entry.packageName] = AppCategory.valueOf(entry.category)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse whitelist entry: $json", e)
            }
        }
    }

    /**
     * 保存白名单到持久化存储
     */
    private fun persistWhitelist() {
        val entriesJson = whitelistCache.values.map { entry ->
            serializeWhitelistEntry(entry)
        }.toSet()
        
        prefs.edit().putStringSet(KEY_WHITELIST, entriesJson).apply()
    }

    /**
     * 查询应用是否在白名单中
     * 
     * @param packageName 应用包名
     * @return 是否在白名单中
     */
    fun isAllowed(packageName: String): Boolean {
        if (!initialized) initialize()
        
        val entry = whitelistCache[packageName] ?: return false
        
        // 验证签名是否变化 (防篡改)
        val currentHash = getAppSignatureHash(packageName)
        if (currentHash != null && currentHash != entry.signatureHash) {
            Log.w(TAG, "Signature mismatch for $packageName, removing from whitelist")
            removeFromWhitelist(packageName)
            return false
        }
        
        return true
    }

    /**
     * 获取应用类别
     */
    fun getAppCategory(packageName: String): AppCategory {
        return categoryMap[packageName] ?: detectCategory(packageName)
    }

    /**
     * 添加到白名单
     * 
     * @param packageName 应用包名
     * @param reason 添加原因
     * @return 是否添加成功
     */
    fun addToWhitelist(packageName: String, reason: String): Boolean {
        try {
            val category = detectCategory(packageName)
            val entry = WhitelistEntry(
                packageName = packageName,
                appName = getAppName(packageName),
                category = category.name,
                addedTime = System.currentTimeMillis(),
                reason = reason,
                signatureHash = getAppSignatureHash(packageName) ?: ""
            )
            
            whitelistCache[packageName] = entry
            categoryMap[packageName] = category
            persistWhitelist()
            
            Log.i(TAG, "Added $packageName to whitelist")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add $packageName to whitelist", e)
            return false
        }
    }

    /**
     * 从白名单移除
     * 
     * @param packageName 应用包名
     * @return 是否移除成功
     */
    fun removeFromWhitelist(packageName: String): Boolean {
        if (!whitelistCache.containsKey(packageName)) {
            return false
        }
        
        whitelistCache.remove(packageName)
        categoryMap.remove(packageName)
        persistWhitelist()
        
        Log.i(TAG, "Removed $packageName from whitelist")
        return true
    }

    /**
     * 获取白名单列表
     */
    fun getWhitelist(): List<WhitelistEntry> {
        if (!initialized) initialize()
        return whitelistCache.values.toList()
    }

    /**
     * 获取白名单包名列表
     */
    fun getWhitelistPackages(): Set<String> {
        if (!initialized) initialize()
        return whitelistCache.keys.toSet()
    }

    /**
     * 刷新白名单缓存
     */
    fun refreshCache() {
        whitelistCache.clear()
        categoryMap.clear()
        loadBuiltinWhitelist()
        loadPersistedWhitelist()
        Log.i(TAG, "Whitelist cache refreshed")
    }

    /**
     * 检测应用类别
     */
    private fun detectCategory(packageName: String): AppCategory {
        return when {
            packageName.contains("map") || 
            packageName.contains("nav") || 
            packageName.contains("location") -> AppCategory.NAVIGATION
            
            packageName.contains("music") || 
            packageName.contains("audio") || 
            packageName.contains("media") -> AppCategory.MUSIC
            
            packageName.contains("video") || 
            packageName.contains("player") ||
            packageName.contains("tencent") -> AppCategory.VIDEO
            
            packageName.contains("game") || 
            packageName.contains("play") -> AppCategory.GAME
            
            packageName.contains("browser") || 
            packageName.contains("web") -> AppCategory.BROWSER
            
            packageName.contains("bluetooth") || 
            packageName.contains("phone") || 
            packageName.contains("dialer") -> AppCategory.COMMUNICATION
            
            packageName.contains("vehicle") || 
            packageName.contains("car") || 
            packageName.contains("air") -> AppCategory.VEHICLE_CONTROL
            
            packageName.contains("launcher") || 
            packageName.contains("settings") || 
            packageName.contains("system") -> AppCategory.SYSTEM
            
            else -> AppCategory.OTHER
        }
    }

    /**
     * 获取应用名称
     */
    private fun getAppName(packageName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    /**
     * 获取应用签名哈希
     */
    private fun getAppSignatureHash(packageName: String): String? {
        return try {
            val pm = context.packageManager
            val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pm.getPackageInfo(packageName, 
                    PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            }
            
            val signatures = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }
            
            signatures?.firstOrNull()?.toCharsString()?.let { signature ->
                val md = MessageDigest.getInstance("SHA-256")
                md.update(signature.toByteArray())
                md.digest().joinToString("") { "%02x".format(it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get signature for $packageName", e)
            null
        }
    }

    /**
     * 序列化白名单条目
     */
    private fun serializeWhitelistEntry(entry: WhitelistEntry): String {
        return "${entry.packageName}|${entry.appName}|${entry.category}|${entry.addedTime}|${entry.reason}|${entry.signatureHash}"
    }

    /**
     * 解析白名单条目
     */
    private fun parseWhitelistEntry(json: String): WhitelistEntry {
        val parts = json.split("|")
        return WhitelistEntry(
            packageName = parts.getOrElse(0) { "" },
            appName = parts.getOrElse(1) { "" },
            category = parts.getOrElse(2) { "" },
            addedTime = parts.getOrElse(3) { "0" }.toLongOrNull() ?: 0,
            reason = parts.getOrElse(4) { "" },
            signatureHash = parts.getOrElse(5) { "" }
        )
    }

    /**
     * 清空白名单 (仅保留内置)
     */
    fun clearWhitelist() {
        whitelistCache.clear()
        categoryMap.clear()
        persistWhitelist()
        loadBuiltinWhitelist()
        Log.i(TAG, "Whitelist cleared, restored built-in entries")
    }

    /**
     * 获取白名单统计信息
     */
    fun getStatistics(): WhitelistStatistics {
        val byCategory = whitelistCache.values.groupBy { it.category }
        return WhitelistStatistics(
            totalCount = whitelistCache.size,
            builtinCount = BUILTIN_WHITELIST.size,
            customCount = whitelistCache.size - BUILTIN_WHITELIST.size,
            categoryDistribution = byCategory.mapValues { it.value.size }
        )
    }
}

/**
 * 白名单统计信息
 */
data class WhitelistStatistics(
    val totalCount: Int,
    val builtinCount: Int,
    val customCount: Int,
    val categoryDistribution: Map<String, Int>
)
