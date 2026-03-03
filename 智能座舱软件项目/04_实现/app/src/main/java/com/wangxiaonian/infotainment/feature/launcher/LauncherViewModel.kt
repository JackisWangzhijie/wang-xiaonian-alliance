/**
 * Launcher ViewModel
 * 管理桌面启动器状态和导航
 * 
 * @author 王小年联盟
 * @version 1.0
 */
@HiltViewModel
class LauncherViewModel @Inject constructor(
    private val restrictionManager: RestrictionManager,
    private val packageManager: PackageManager,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    // 应用列表
    private val _apps = MutableStateFlow<List<CarAppInfo>>(emptyList())
    val apps: StateFlow<List<CarAppInfo>> = _apps.asStateFlow()
    
    // 当前限制状态
    val restrictionState: StateFlow<RestrictionState> = restrictionManager
        .restrictionState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = RestrictionState.Full
        )
    
    // 最近使用的应用
    private val _recentApps = MutableStateFlow<List<CarAppInfo>>(emptyList())
    val recentApps: StateFlow<List<CarAppInfo>> = _recentApps.asStateFlow()
    
    init {
        loadApps()
    }
    
    /**
     * 加载已安装的车载应用
     */
    private fun loadApps() {
        viewModelScope.launch {
            val carApps = getInstalledCarApps()
            _apps.value = carApps.sortedBy { it.label }
        }
    }
    
    /**
     * 获取已安装的车载应用列表
     */
    private fun getInstalledCarApps(): List<CarAppInfo> {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_APP_CAR_LAUNCHER)
        }
        
        return packageManager.queryIntentActivities(intent, 0)
            .map { resolveInfo ->
                CarAppInfo(
                    packageName = resolveInfo.activityInfo.packageName,
                    activityName = resolveInfo.activityInfo.name,
                    label = resolveInfo.loadLabel(packageManager).toString(),
                    icon = resolveInfo.loadIcon(packageManager),
                    category = resolveInfo.getCarAppCategory()
                )
            }
            .filter { it.packageName != context.packageName } // 排除自己
    }
    
    /**
     * 启动应用
     */
    fun launchApp(app: CarAppInfo) {
        // 检查行驶限制
        if (!restrictionManager.isOperationAllowed(RestrictedOperation.OPEN_APP)) {
            // 驾驶中不能打开新应用
            return
        }
        
        val intent = Intent(Intent.ACTION_MAIN).apply {
            setClassName(app.packageName, app.activityName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        try {
            context.startActivity(intent)
            addToRecentApps(app)
        } catch (e: Exception) {
            // 启动失败
        }
    }
    
    /**
     * 添加到最近应用
     */
    private fun addToRecentApps(app: CarAppInfo) {
        val current = _recentApps.value.toMutableList()
        current.removeAll { it.packageName == app.packageName }
        current.add(0, app)
        _recentApps.value = current.take(4) // 只保留4个
    }
    
    /**
     * 根据驾驶状态过滤可显示的应用
     */
    fun getAppsForCurrentState(): List<CarAppInfo> {
        return when (restrictionState.value) {
            RestrictionState.Critical -> {
                // 危险状态只显示导航和电话
                _apps.value.filter { 
                    it.category == AppCategory.NAVIGATION || 
                    it.category == AppCategory.COMMUNICATION 
                }
            }
            RestrictionState.Limited -> {
                // 行驶中限制娱乐应用
                _apps.value.filter { 
                    it.category != AppCategory.ENTERTAINMENT 
                }
            }
            else -> _apps.value
        }
    }
}

/**
 * 车载应用信息
 */
data class CarAppInfo(
    val packageName: String,
    val activityName: String,
    val label: String,
    val icon: Drawable,
    val category: AppCategory
)

/**
 * 应用分类
 */
enum class AppCategory {
    NAVIGATION,     // 导航
    COMMUNICATION,  // 通讯
    MEDIA,          // 媒体
    ENTERTAINMENT,  // 娱乐
    SETTINGS,       // 设置
    SYSTEM,         // 系统
    OTHER           // 其他
}

/**
 * 获取应用分类
 */
private fun ResolveInfo.getCarAppCategory(): AppCategory {
    return when {
        activityInfo.packageName.contains("maps") ||
        activityInfo.packageName.contains("navigation") -> AppCategory.NAVIGATION
        
        activityInfo.packageName.contains("phone") ||
        activityInfo.packageName.contains("dialer") ||
        activityInfo.packageName.contains("contact") -> AppCategory.COMMUNICATION
        
        activityInfo.packageName.contains("music") ||
        activityInfo.packageName.contains("media") ||
        activityInfo.packageName.contains("radio") -> AppCategory.MEDIA
        
        activityInfo.packageName.contains("video") ||
        activityInfo.packageName.contains("game") -> AppCategory.ENTERTAINMENT
        
        activityInfo.packageName.contains("setting") -> AppCategory.SETTINGS
        
        activityInfo.packageName.contains("system") -> AppCategory.SYSTEM
        
        else -> AppCategory.OTHER
    }
}
