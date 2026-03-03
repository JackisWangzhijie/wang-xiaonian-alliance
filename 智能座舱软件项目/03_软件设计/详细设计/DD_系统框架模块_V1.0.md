# 系统框架模块详细设计文档
## Detailed Design Document - System Framework Module

**项目名称**: 2024年智能座舱软件主交互开发  
**文档版本**: V1.0  
**编制日期**: 2024-06-20  
**编制单位**: 上海龙旗智能科技有限公司  
**客户单位**: 奇瑞汽车股份有限公司  
**模块名称**: 系统框架模块 (System Framework Module)  
**ASIL等级**: QM  
**符合标准**: ASPICE 3.1, ISO 26262

---

## 文档控制信息

### 版本历史
| 版本 | 日期 | 作者 | 变更描述 | 审批 |
|------|------|------|----------|------|
| V0.1 | 2024-06-18 | 软件工程师 | 初稿编制 | - |
| V0.5 | 2024-06-19 | 系统架构师 | 架构评审后修订 | - |
| V1.0 | 2024-06-20 | 软件工程师 | 基线版本 | 项目总监 |

### 参考文档
1. 《HLD_概要设计文档_V1.0.md》
2. 《SRS_智能座舱主交互系统_V1.0.md》
3. 《数据库设计文档_V1.0.md》
4. 《Android Automotive开发规范》
5. 《Jetpack Compose设计指南》

---

## 目录

1. [引言](#1-引言)
2. [模块架构设计](#2-模块架构设计)
3. [核心类设计](#3-核心类设计)
4. [UI组件设计](#4-ui组件设计)
5. [状态管理设计](#5-状态管理设计)
6. [应用生命周期管理](#6-应用生命周期管理)
7. [需求追溯矩阵](#7-需求追溯矩阵)
8. [接口定义](#8-接口定义)
9. [数据持久化设计](#9-数据持久化设计)
10. [性能与安全设计](#10-性能与安全设计)

---

## 1. 引言

### 1.1 目的
本文档基于HLD概要设计和SRS需求，定义系统框架模块的详细设计，为开发团队提供具体的技术实现指导。

### 1.2 范围
本文档涵盖以下子模块的详细设计：
- **Launcher模块**: 桌面管理、应用图标网格、快捷入口
- **控件库模块**: 基础UI控件、车辆专用控件
- **Dock栏模块**: 固定入口、最近应用、Home键

### 1.3 设计约束
- 开发语言: Kotlin 1.7+
- UI框架: Jetpack Compose 1.3+
- 架构模式: MVVM + Repository
- 最低API: Android 12 (API 31)
- 目标平台: Android Automotive

---

## 2. 模块架构设计

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         系统框架模块 (FWK)                               │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                      Presentation Layer                          │   │
│  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐            │   │
│  │  │   Launcher   │ │   Widget     │ │    Dock      │            │   │
│  │  │     UI       │ │   Library    │ │     Bar      │            │   │
│  │  │  (Compose)   │ │   (Compose)  │ │   (Compose)  │            │   │
│  │  └──────────────┘ └──────────────┘ └──────────────┘            │   │
│  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐            │   │
│  │  │ AppGridScreen│ │VehicleWidgets│ │RecentAppsView│            │   │
│  │  │ShortcutPanel │ │ControlButtons│ │HomeButtonView│            │   │
│  │  └──────────────┘ └──────────────┘ └──────────────┘            │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                              │                                          │
│  ┌───────────────────────────┼───────────────────────────────────┐    │
│  │                      ViewModel Layer                             │    │
│  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐            │    │
│  │  │LauncherViewMo│ │WidgetLibViewM│ │  DockViewMod │            │    │
│  │  │    del       │ │    odel      │ │     el       │            │    │
│  │  │- App列表管理 │ │- 控件状态管理│ │- 最近应用管理│            │    │
│  │  │- 快捷入口管理│ │- 主题管理   │ │- 固定入口管理│            │    │
│  │  │- 布局管理   │ │- 动画管理   │ │- Home键处理 │            │    │
│  │  └──────────────┘ └──────────────┘ └──────────────┘            │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                              │                                          │
│  ┌───────────────────────────┼───────────────────────────────────┐    │
│  │                      Repository Layer                            │    │
│  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐            │    │
│  │  │ AppRepository│ │WidgetReposito│ │ DockReposito │            │    │
│  │  │              │ │      ry      │ │      ry      │            │    │
│  │  │- 应用数据获取│ │- 控件配置获取│ │- Dock配置管理│            │    │
│  │  │- 应用状态管理│ │- 主题数据管理│ │- 最近应用数据│            │    │
│  │  │- 应用启动控制│ │- 控件事件处理│ │- 快捷方式管理│            │    │
│  │  └──────────────┘ └──────────────┘ └──────────────┘            │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                              │                                          │
│  ┌───────────────────────────┼───────────────────────────────────┐    │
│  │                      Data Source Layer                           │    │
│  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐            │    │
│  │  │   Room DB    │ │DataStore     │ │ System APIs  │            │    │
│  │  │(application) │ │(Preferences) │ │(PackageMgr)  │            │    │
│  │  └──────────────┘ └──────────────┘ └──────────────┘            │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.2 模块划分

#### 2.2.1 Launcher模块

| 组件 | 职责 | 对应文件 |
|------|------|----------|
| AppGridScreen | 应用图标网格主界面 | launcher/AppGridScreen.kt |
| AppIconItem | 单个应用图标组件 | launcher/AppIconItem.kt |
| ShortcutPanel | 快捷入口面板 | launcher/ShortcutPanel.kt |
| LauncherViewModel | Launcher业务逻辑 | launcher/LauncherViewModel.kt |
| AppRepository | 应用数据管理 | repository/AppRepository.kt |

#### 2.2.2 控件库模块

| 组件 | 职责 | 对应文件 |
|------|------|----------|
| VehicleButton | 车辆专用按钮 | widgets/VehicleButton.kt |
| VehicleText | 车辆专用文本 | widgets/VehicleText.kt |
| SpeedDisplay | 车速显示控件 | widgets/SpeedDisplay.kt |
| FuelDisplay | 油量/电量显示 | widgets/FuelDisplay.kt |
| TirePressureWidget | 胎压显示控件 | widgets/TirePressureWidget.kt |
| VehicleSlider | 车辆专用滑块 | widgets/VehicleSlider.kt |
| WidgetTheme | 控件主题管理 | widgets/WidgetTheme.kt |

#### 2.2.3 Dock栏模块

| 组件 | 职责 | 对应文件 |
|------|------|----------|
| DockBar | Dock栏主组件 | dock/DockBar.kt |
| FixedAppSlot | 固定应用入口 | dock/FixedAppSlot.kt |
| RecentAppsView | 最近应用区域 | dock/RecentAppsView.kt |
| HomeButton | Home键组件 | dock/HomeButton.kt |
| DockViewModel | Dock业务逻辑 | dock/DockViewModel.kt |

### 2.3 模块依赖关系

```
                    ┌─────────────────┐
                    │   应用层模块     │
                    │ NAV/MSG/AI/SET  │
                    └────────┬────────┘
                             │ 使用
                             ▼
┌──────────────────────────────────────────────────────┐
│                   系统框架模块 (FWK)                   │
│  ┌─────────────────────────────────────────────────┐ │
│  │  Launcher  │  Widget Lib  │      Dock          │ │
│  └────────────┴──────────────┴─────────────────────┘ │
│                    │           │                      │
│                    ▼           ▼                      │
│           ┌─────────────────────────┐                │
│           │    Repository Layer     │                │
│           └────────────┬────────────┘                │
│                        │                             │
│           ┌────────────┴────────────┐                │
│           ▼                         ▼                │
│  ┌─────────────────┐    ┌─────────────────┐         │
│  │   Room Database │    │  Service APIs   │         │
│  └─────────────────┘    └─────────────────┘         │
└──────────────────────────────────────────────────────┘
           │                          │
           ▼                          ▼
┌─────────────────┐          ┌─────────────────┐
│   Vehicle SVC   │          │  Message SVC    │
└─────────────────┘          └─────────────────┘
```

---

## 3. 核心类设计

### 3.1 数据模型类

#### 3.1.1 应用信息模型

```kotlin
/**
 * 应用信息数据类
 * 对应数据库表: application
 * 需求追溯: REQ-FWK-FUN-014
 */
@Entity(tableName = "application")
data class AppInfo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "app_id")
    val appId: String,                    // 应用包名 (唯一标识)
    
    @ColumnInfo(name = "app_name")
    val appName: String,                  // 应用显示名称
    
    @ColumnInfo(name = "app_name_en")
    val appNameEn: String?,               // 英文名称
    
    @ColumnInfo(name = "category_id")
    val categoryId: Int,                  // 应用分类ID
    
    @ColumnInfo(name = "icon_path")
    val iconPath: String?,                // 图标路径
    
    @ColumnInfo(name = "version_code")
    val versionCode: Int = 1,             // 版本号
    
    @ColumnInfo(name = "version_name")
    val versionName: String = "1.0.0",    // 版本名称
    
    @ColumnInfo(name = "is_system")
    val isSystem: Boolean = false,        // 是否系统应用
    
    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean = true,        // 是否启用
    
    @ColumnInfo(name = "is_in_whitelist")
    val isInWhitelist: Boolean = false,   // 是否在白名单
    
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,               // 显示排序
    
    @ColumnInfo(name = "launch_count")
    val launchCount: Int = 0,             // 启动次数
    
    @ColumnInfo(name = "last_launch")
    val lastLaunch: Long? = null,         // 最后启动时间
    
    @ColumnInfo(name = "create_time")
    val createTime: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "update_time")
    val updateTime: Long = System.currentTimeMillis()
)

/**
 * 应用分类枚举
 */
enum class AppCategory(
    val code: String,
    @StringRes val nameResId: Int,
    val icon: Int
) {
    NAVIGATION("NAV", R.string.category_nav, R.drawable.ic_nav),
    MUSIC("MUSIC", R.string.category_music, R.drawable.ic_music),
    VIDEO("VIDEO", R.string.category_video, R.drawable.ic_video),
    PHONE("PHONE", R.string.category_phone, R.drawable.ic_phone),
    VEHICLE("CAR", R.string.category_vehicle, R.drawable.ic_car),
    LIFE("LIFE", R.string.category_life, R.drawable.ic_life),
    GAME("GAME", R.string.category_game, R.drawable.ic_game),
    TOOL("TOOL", R.string.category_tool, R.drawable.ic_tool);
    
    companion object {
        fun fromCode(code: String): AppCategory = 
            values().find { it.code == code } ?: TOOL
    }
}
```

#### 3.1.2 Dock配置模型

```kotlin
/**
 * Dock栏配置数据类
 * 对应数据库表: dock_config
 * 需求追溯: REQ-FWK-FUN-016
 */
@Entity(tableName = "dock_config")
data class DockConfig(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "slot_index")
    val slotIndex: Int,                   // 槽位索引 (0-3固定槽位)
    
    @ColumnInfo(name = "slot_type")
    val slotType: DockSlotType,           // 槽位类型
    
    @ColumnInfo(name = "app_id")
    val appId: String?,                   // 应用ID (固定应用时使用)
    
    @ColumnInfo(name = "icon_resource")
    val iconResource: String?,            // 图标资源
    
    @ColumnInfo(name = "action_type")
    val actionType: DockActionType,       // 动作类型
    
    @ColumnInfo(name = "action_data")
    val actionData: String?               // 动作参数(JSON)
)

/**
 * Dock槽位类型
 */
enum class DockSlotType {
    FIXED_APP,        // 固定应用
    RECENT_APP,       // 最近应用
    HOME_BUTTON,      // Home键
    QUICK_ACTION      // 快捷操作
}

/**
 * Dock动作类型
 */
enum class DockActionType {
    LAUNCH_APP,       // 启动应用
    GO_HOME,          // 返回Home
    SHOW_RECENTS,     // 显示最近任务
    QUICK_SETTINGS,   // 快捷设置
    VOICE_ASSISTANT   // 语音助手
}

/**
 * 最近应用信息
 */
data class RecentAppInfo(
    val appId: String,
    val appName: String,
    val iconPath: String?,
    val lastUsedTime: Long,
    val isRunning: Boolean = false
)
```

#### 3.1.3 控件主题模型

```kotlin
/**
 * 控件主题配置
 * 需求追溯: REQ-FWK-FUN-015
 */
@Immutable
data class VehicleWidgetTheme(
    val primaryColor: Color,              // 主色调
    val secondaryColor: Color,            // 次要色
    val backgroundColor: Color,           // 背景色
    val surfaceColor: Color,              // 表面色
    val onPrimaryColor: Color,            // 主色上的文字色
    val onBackgroundColor: Color,         // 背景上的文字色
    val warningColor: Color,              // 警告色
    val errorColor: Color,                // 错误色
    val successColor: Color,              // 成功色
    val typography: VehicleTypography,    // 字体样式
    val shapes: VehicleShapes,            // 形状定义
    val dimensions: VehicleDimensions     // 尺寸定义
)

/**
 * 车辆专用字体样式
 */
data class VehicleTypography(
    val speedDisplay: TextStyle,          // 车速显示字体
    val gaugeLabel: TextStyle,            // 仪表标签字体
    val controlLabel: TextStyle,          // 控件标签字体
    val buttonText: TextStyle,            // 按钮文字字体
    val infoText: TextStyle               // 信息文字字体
)

/**
 * 车辆专用形状
 */
data class VehicleShapes(
    val buttonCornerRadius: Dp,           // 按钮圆角
    val cardCornerRadius: Dp,             // 卡片圆角
    val gaugeCornerRadius: Dp,            // 仪表圆角
    val small: CornerBasedShape,
    val medium: CornerBasedShape,
    val large: CornerBasedShape
)

/**
 * 车辆专用尺寸
 */
@Immutable
data class VehicleDimensions(
    val touchTargetSize: Dp = 88.dp,      // 最小触控目标尺寸
    val buttonHeight: Dp = 80.dp,         // 按钮高度
    val iconSize: Dp = 64.dp,             // 图标尺寸
    val spacingSmall: Dp = 8.dp,
    val spacingMedium: Dp = 16.dp,
    val spacingLarge: Dp = 24.dp,
    val appGridColumnCount: Int = 4       // 应用网格列数
)
```

### 3.2 Repository层类

#### 3.2.1 应用数据仓库

```kotlin
/**
 * 应用数据仓库接口
 */
interface IAppRepository {
    // 应用查询
    suspend fun getAllApps(): List<AppInfo>
    suspend fun getAppsByCategory(category: AppCategory): List<AppInfo>
    suspend fun getAppById(appId: String): AppInfo?
    suspend fun getFrequentlyUsedApps(limit: Int = 8): List<AppInfo>
    suspend fun searchApps(query: String): List<AppInfo>
    
    // 应用状态管理
    suspend fun updateAppStatus(appId: String, isEnabled: Boolean)
    suspend fun incrementLaunchCount(appId: String)
    suspend fun updateAppOrder(apps: List<AppInfo>)
    
    // 应用启动
    suspend fun launchApp(appId: String): Result<Unit>
    suspend fun isAppRestricted(appId: String): Boolean
    
    // 应用安装/卸载监听
    fun observeAppChanges(): Flow<List<AppInfo>>
}

/**
 * 应用数据仓库实现
 */
@Singleton
class AppRepository @Inject constructor(
    private val appDao: AppDao,
    private val packageManager: PackageManager,
    private val vehicleService: IVehicleService,
    private val context: Context
) : IAppRepository {
    
    private val appChangesFlow = MutableStateFlow<List<AppInfo>>(emptyList())
    
    init {
        // 监听应用安装/卸载事件
        observePackageChanges()
    }
    
    override suspend fun getAllApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        appDao.getAllApps().sortedBy { it.sortOrder }
    }
    
    override suspend fun getAppsByCategory(category: AppCategory): List<AppInfo> = 
        withContext(Dispatchers.IO) {
            appDao.getAppsByCategory(category.code)
        }
    
    override suspend fun getAppById(appId: String): AppInfo? = withContext(Dispatchers.IO) {
        appDao.getAppById(appId)
    }
    
    override suspend fun getFrequentlyUsedApps(limit: Int): List<AppInfo> = 
        withContext(Dispatchers.IO) {
            appDao.getFrequentlyUsedApps(limit)
        }
    
    override suspend fun searchApps(query: String): List<AppInfo> = 
        withContext(Dispatchers.IO) {
            appDao.searchApps("%$query%")
        }
    
    override suspend fun updateAppStatus(appId: String, isEnabled: Boolean) {
        appDao.updateAppStatus(appId, isEnabled)
        refreshAppList()
    }
    
    override suspend fun incrementLaunchCount(appId: String) {
        appDao.incrementLaunchCount(appId, System.currentTimeMillis())
    }
    
    override suspend fun updateAppOrder(apps: List<AppInfo>) {
        apps.forEachIndexed { index, app ->
            appDao.updateSortOrder(app.appId, index)
        }
    }
    
    override suspend fun launchApp(appId: String): Result<Unit> = 
        withContext(Dispatchers.Main) {
            try {
                // 检查行驶限制
                if (isAppRestricted(appId)) {
                    return@withContext Result.failure(AppRestrictedException())
                }
                
                val intent = context.packageManager.getLaunchIntentForPackage(appId)
                    ?: return@withContext Result.failure(AppNotFoundException())
                
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                
                // 更新启动次数
                incrementLaunchCount(appId)
                
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    
    override suspend fun isAppRestricted(appId: String): Boolean {
        val restrictionStatus = vehicleService.getDrivingRestrictionStatus()
        val app = getAppById(appId) ?: return false
        
        return when {
            app.isInWhitelist -> false
            app.categoryId == AppCategory.VIDEO.ordinal && restrictionStatus.isRestricted -> true
            app.categoryId == AppCategory.GAME.ordinal && restrictionStatus.isRestricted -> true
            else -> false
        }
    }
    
    override fun observeAppChanges(): Flow<List<AppInfo>> = appChangesFlow
    
    private fun observePackageChanges() {
        // 注册应用安装/卸载广播接收器
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        }
        // 实现监听逻辑...
    }
    
    private suspend fun refreshAppList() {
        appChangesFlow.value = getAllApps()
    }
}
```

#### 3.2.2 Dock数据仓库

```kotlin
/**
 * Dock数据仓库接口
 */
interface IDockRepository {
    suspend fun getDockConfig(): List<DockConfig>
    suspend fun updateDockConfig(config: List<DockConfig>)
    suspend fun getRecentApps(limit: Int = 4): List<RecentAppInfo>
    suspend fun addToRecentApps(appId: String)
    suspend fun clearRecentApps()
    suspend fun handleHomeAction()
    suspend fun handleRecentAppsAction()
}

/**
 * Dock数据仓库实现
 */
@Singleton
class DockRepository @Inject constructor(
    private val dockDao: DockDao,
    private val appRepository: IAppRepository,
    private val activityManager: ActivityManager,
    private val context: Context
) : IDockRepository {
    
    private val recentAppsQueue = ArrayDeque<RecentAppInfo>(10)
    
    override suspend fun getDockConfig(): List<DockConfig> = withContext(Dispatchers.IO) {
        dockDao.getAllConfig().sortedBy { it.slotIndex }
    }
    
    override suspend fun updateDockConfig(config: List<DockConfig>) {
        dockDao.updateConfig(config)
    }
    
    override suspend fun getRecentApps(limit: Int): List<RecentAppInfo> {
        return recentAppsQueue.take(limit)
    }
    
    override suspend fun addToRecentApps(appId: String) {
        val app = appRepository.getAppById(appId) ?: return
        val recentApp = RecentAppInfo(
            appId = app.appId,
            appName = app.appName,
            iconPath = app.iconPath,
            lastUsedTime = System.currentTimeMillis(),
            isRunning = isAppRunning(appId)
        )
        
        // 移除已存在的相同应用
        recentAppsQueue.removeAll { it.appId == appId }
        // 添加到队首
        recentAppsQueue.addFirst(recentApp)
        // 限制队列大小
        while (recentAppsQueue.size > 10) {
            recentAppsQueue.removeLast()
        }
    }
    
    override suspend fun clearRecentApps() {
        recentAppsQueue.clear()
    }
    
    override suspend fun handleHomeAction() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
    
    override suspend fun handleRecentAppsAction() {
        // 显示最近任务
        val intent = Intent("android.intent.action.RECENT_TASKS").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
    
    private fun isAppRunning(appId: String): Boolean {
        val runningApps = activityManager.runningAppProcesses
        return runningApps?.any { it.processName == appId } ?: false
    }
}
```

### 3.3 ViewModel层类

#### 3.3.1 Launcher ViewModel

```kotlin
/**
 * Launcher ViewModel
 * 管理Launcher界面状态和业务逻辑
 */
@HiltViewModel
class LauncherViewModel @Inject constructor(
    private val appRepository: IAppRepository,
    private val preferenceRepository: IPreferenceRepository
) : ViewModel() {
    
    // UI状态
    private val _uiState = MutableStateFlow(LauncherUiState())
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()
    
    // 应用列表
    private val _appList = MutableStateFlow<List<AppInfo>>(emptyList())
    val appList: StateFlow<List<AppInfo>> = _appList.asStateFlow()
    
    // 快捷入口
    private val _shortcuts = MutableStateFlow<List<ShortcutInfo>>(emptyList())
    val shortcuts: StateFlow<List<ShortcutInfo>> = _shortcuts.asStateFlow()
    
    // 当前选中的应用分类
    private val _selectedCategory = MutableStateFlow<AppCategory?>(null)
    val selectedCategory: StateFlow<AppCategory?> = _selectedCategory.asStateFlow()
    
    init {
        loadApps()
        loadShortcuts()
    }
    
    private fun loadApps() {
        viewModelScope.launch {
            val apps = appRepository.getAllApps()
            _appList.value = apps
        }
    }
    
    private fun loadShortcuts() {
        viewModelScope.launch {
            // 加载常用应用作为快捷入口
            val frequentApps = appRepository.getFrequentlyUsedApps(4)
            _shortcuts.value = frequentApps.map { app ->
                ShortcutInfo(
                    id = app.appId,
                    name = app.appName,
                    iconPath = app.iconPath,
                    action = { launchApp(app.appId) }
                )
            }
        }
    }
    
    fun launchApp(appId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val result = appRepository.launchApp(appId)
            
            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    lastLaunchedApp = appId
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = when (error) {
                        is AppRestrictedException -> "行驶中无法使用该应用"
                        is AppNotFoundException -> "应用未找到"
                        else -> "启动失败: ${error.message}"
                    }
                )
            }
        }
    }
    
    fun filterByCategory(category: AppCategory?) {
        _selectedCategory.value = category
        viewModelScope.launch {
            val apps = if (category == null) {
                appRepository.getAllApps()
            } else {
                appRepository.getAppsByCategory(category)
            }
            _appList.value = apps
        }
    }
    
    fun searchApps(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                loadApps()
            } else {
                val results = appRepository.searchApps(query)
                _appList.value = results
            }
        }
    }
    
    fun reorderApps(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val currentList = _appList.value.toMutableList()
            val movedApp = currentList.removeAt(fromIndex)
            currentList.add(toIndex, movedApp)
            
            _appList.value = currentList
            appRepository.updateAppOrder(currentList)
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

/**
 * Launcher UI状态
 */
data class LauncherUiState(
    val isLoading: Boolean = false,
    val lastLaunchedApp: String? = null,
    val errorMessage: String? = null,
    val isEditMode: Boolean = false
)

/**
 * 快捷入口信息
 */
data class ShortcutInfo(
    val id: String,
    val name: String,
    val iconPath: String?,
    val action: () -> Unit
)
```

#### 3.3.2 Dock ViewModel

```kotlin
/**
 * Dock ViewModel
 * 管理Dock栏状态和业务逻辑
 */
@HiltViewModel
class DockViewModel @Inject constructor(
    private val dockRepository: IDockRepository,
    private val appRepository: IAppRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DockUiState())
    val uiState: StateFlow<DockUiState> = _uiState.asStateFlow()
    
    private val _fixedApps = MutableStateFlow<List<DockItem>>(emptyList())
    val fixedApps: StateFlow<List<DockItem>> = _fixedApps.asStateFlow()
    
    private val _recentApps = MutableStateFlow<List<DockItem>>(emptyList())
    val recentApps: StateFlow<List<DockItem>> = _recentApps.asStateFlow()
    
    init {
        loadDockConfig()
        loadRecentApps()
    }
    
    private fun loadDockConfig() {
        viewModelScope.launch {
            val config = dockRepository.getDockConfig()
            val fixedItems = config
                .filter { it.slotType == DockSlotType.FIXED_APP }
                .mapNotNull { config ->
                    config.appId?.let { appId ->
                        val app = appRepository.getAppById(appId)
                        app?.let {
                            DockItem.AppItem(
                                appId = it.appId,
                                name = it.appName,
                                iconPath = it.iconPath,
                                isEnabled = it.isEnabled
                            )
                        }
                    }
                }
            _fixedApps.value = fixedItems
        }
    }
    
    private fun loadRecentApps() {
        viewModelScope.launch {
            val recent = dockRepository.getRecentApps(4)
            _recentApps.value = recent.map {
                DockItem.RecentAppItem(
                    appId = it.appId,
                    name = it.appName,
                    iconPath = it.iconPath,
                    isRunning = it.isRunning
                )
            }
        }
    }
    
    fun onHomeClick() {
        viewModelScope.launch {
            dockRepository.handleHomeAction()
        }
    }
    
    fun onRecentAppsClick() {
        viewModelScope.launch {
            dockRepository.handleRecentAppsAction()
        }
    }
    
    fun onAppClick(appId: String) {
        viewModelScope.launch {
            appRepository.launchApp(appId)
            dockRepository.addToRecentApps(appId)
            loadRecentApps()
        }
    }
    
    fun updateFixedApps(apps: List<DockItem.AppItem>) {
        viewModelScope.launch {
            _fixedApps.value = apps
            // 保存到数据库
            val config = apps.mapIndexed { index, app ->
                DockConfig(
                    slotIndex = index,
                    slotType = DockSlotType.FIXED_APP,
                    appId = app.appId,
                    actionType = DockActionType.LAUNCH_APP
                )
            }
            dockRepository.updateDockConfig(config)
        }
    }
}

/**
 * Dock UI状态
 */
data class DockUiState(
    val isVisible: Boolean = true,
    val isExpanded: Boolean = false
)

/**
 * Dock项密封类
 */
sealed class DockItem {
    abstract val appId: String
    abstract val name: String
    abstract val iconPath: String?
    
    data class AppItem(
        override val appId: String,
        override val name: String,
        override val iconPath: String?,
        val isEnabled: Boolean = true
    ) : DockItem()
    
    data class RecentAppItem(
        override val appId: String,
        override val name: String,
        override val iconPath: String?,
        val isRunning: Boolean = false
    ) : DockItem()
    
    data class QuickActionItem(
        override val appId: String,
        override val name: String,
        override val iconPath: String?,
        val actionType: DockActionType
    ) : DockItem()
}
```

---

## 4. UI组件设计

### 4.1 Jetpack Compose组件设计

#### 4.1.1 应用图标网格 (AppGrid)

```kotlin
/**
 * 应用网格屏幕
 * 需求追溯: REQ-FWK-FUN-014
 */
@Composable
fun AppGridScreen(
    viewModel: LauncherViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val apps by viewModel.appList.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    
    Column(modifier = modifier.fillMaxSize()) {
        // 分类筛选栏
        CategoryFilterBar(
            selectedCategory = selectedCategory,
            onCategorySelected = { viewModel.filterByCategory(it) }
        )
        
        // 应用网格
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(apps, key = { it.appId }) { app ->
                AppIconItem(
                    app = app,
                    onClick = { viewModel.launchApp(app.appId) },
                    onLongClick = { /* 进入编辑模式 */ }
                )
            }
        }
        
        // 快捷入口面板
        ShortcutPanel(
            shortcuts = viewModel.shortcuts.collectAsState().value,
            modifier = Modifier.padding(16.dp)
        )
        
        // 加载和错误状态
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
        
        uiState.errorMessage?.let { error ->
            ErrorSnackbar(
                message = error,
                onDismiss = { viewModel.clearError() }
            )
        }
    }
}

/**
 * 应用图标项
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIconItem(
    app: AppInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(120.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(8.dp)
    ) {
        // 应用图标
        AppIcon(
            iconPath = app.iconPath,
            packageName = app.appId,
            modifier = Modifier
                .size(80.dp)
                .scale(if (isPressed) 0.95f else 1f)
                .alpha(if (app.isEnabled) 1f else 0.5f)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 应用名称
        Text(
            text = app.appName,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = if (app.isEnabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            }
        )
        
        // 白名单标记
        if (app.isInWhitelist) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "白名单应用",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * 应用图标加载组件
 */
@Composable
fun AppIcon(
    iconPath: String?,
    packageName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (iconPath != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(iconPath)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // 默认图标
            Icon(
                imageVector = Icons.Default.Apps,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

#### 4.1.2 Dock栏组件

```kotlin
/**
 * Dock栏主组件
 * 需求追溯: REQ-FWK-FUN-016
 */
@Composable
fun DockBar(
    viewModel: DockViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val fixedApps by viewModel.fixedApps.collectAsState()
    val recentApps by viewModel.recentApps.collectAsState()
    
    if (!uiState.isVisible) return
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 固定应用区域
            FixedAppsArea(
                apps = fixedApps,
                onAppClick = { viewModel.onAppClick(it) }
            )
            
            // 分隔线
            VerticalDivider(
                modifier = Modifier
                    .height(64.dp)
                    .padding(horizontal = 16.dp)
            )
            
            // 最近应用区域
            RecentAppsArea(
                apps = recentApps,
                onAppClick = { viewModel.onAppClick(it) }
            )
            
            // 分隔线
            VerticalDivider(
                modifier = Modifier
                    .height(64.dp)
                    .padding(horizontal = 16.dp)
            )
            
            // Home键
            HomeButton(
                onClick = { viewModel.onHomeClick() }
            )
        }
    }
}

/**
 * 固定应用区域
 */
@Composable
fun FixedAppsArea(
    apps: List<DockItem>,
    onAppClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
    ) {
        apps.forEach { app ->
            DockAppButton(
                app = app,
                onClick = { onAppClick(app.appId) }
            )
        }
    }
}

/**
 * 最近应用区域
 */
@Composable
fun RecentAppsArea(
    apps: List<DockItem>,
    onAppClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
    ) {
        apps.forEach { app ->
            val isRunning = (app as? DockItem.RecentAppItem)?.isRunning ?: false
            
            DockAppButton(
                app = app,
                onClick = { onAppClick(app.appId) },
                showRunningIndicator = isRunning
            )
        }
    }
}

/**
 * Dock应用按钮
 */
@Composable
fun DockAppButton(
    app: DockItem,
    onClick: () -> Unit,
    showRunningIndicator: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable(onClick = onClick)
        ) {
            Box {
                AppIcon(
                    iconPath = app.iconPath,
                    packageName = app.appId,
                    modifier = Modifier.size(64.dp)
                )
                
                // 运行中指示器
                if (showRunningIndicator) {
                    RunningIndicator(
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = app.name,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Home按钮
 */
@Composable
fun HomeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier.size(72.dp),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Icon(
            imageVector = Icons.Default.Home,
            contentDescription = "Home",
            modifier = Modifier.size(40.dp)
        )
    }
}

/**
 * 运行中指示器
 */
@Composable
fun RunningIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
    )
}
```

### 4.2 车辆专用控件

#### 4.2.1 车速显示控件

```kotlin
/**
 * 车速显示控件
 * 需求追溯: REQ-FWK-FUN-015
 */
@Composable
fun SpeedDisplay(
    speed: Int,
    unit: SpeedUnit = SpeedUnit.KMH,
    maxSpeed: Int = 240,
    modifier: Modifier = Modifier,
    theme: VehicleWidgetTheme = LocalVehicleWidgetTheme.current
) {
    val animatedSpeed by animateIntAsState(
        targetValue = speed,
        animationSpec = tween(300)
    )
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(200.dp)
            .clip(CircleShape)
            .background(theme.backgroundColor)
    ) {
        // 背景进度环
        CircularProgressIndicator(
            progress = { animatedSpeed / maxSpeed.toFloat() },
            modifier = Modifier.fillMaxSize(),
            color = when {
                speed > 120 -> theme.errorColor
                speed > 80 -> theme.warningColor
                else -> theme.primaryColor
            },
            strokeWidth = 12.dp,
            trackColor = theme.surfaceColor
        )
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 速度值
            Text(
                text = animatedSpeed.toString(),
                style = theme.typography.speedDisplay,
                color = theme.onBackgroundColor
            )
            
            // 单位
            Text(
                text = unit.displayName,
                style = theme.typography.infoText,
                color = theme.onBackgroundColor.copy(alpha = 0.7f)
            )
        }
    }
}

enum class SpeedUnit(val displayName: String) {
    KMH("km/h"),
    MPH("mph")
}

/**
 * 油量/电量显示控件
 */
@Composable
fun FuelDisplay(
    level: Float,  // 0.0 - 1.0
    range: Int,    // 续航里程(km)
    type: EnergyType = EnergyType.GASOLINE,
    modifier: Modifier = Modifier,
    theme: VehicleWidgetTheme = LocalVehicleWidgetTheme.current
) {
    val animatedLevel by animateFloatAsState(
        targetValue = level,
        animationSpec = tween(500)
    )
    
    Card(
        modifier = modifier
            .width(280.dp)
            .height(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = theme.surfaceColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 油量/电量图标
            Icon(
                imageVector = when (type) {
                    EnergyType.GASOLINE -> Icons.Default.LocalGasStation
                    EnergyType.ELECTRIC -> Icons.Default.BatteryFull
                    EnergyType.HYBRID -> Icons.Default.ElectricCar
                },
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = when {
                    level < 0.15f -> theme.errorColor
                    level < 0.3f -> theme.warningColor
                    else -> theme.primaryColor
                }
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                // 油量/电量百分比
                Text(
                    text = "${(animatedLevel * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineMedium,
                    color = theme.onBackgroundColor
                )
                
                // 续航里程
                Text(
                    text = "剩余里程: ${range}km",
                    style = MaterialTheme.typography.bodyMedium,
                    color = theme.onBackgroundColor.copy(alpha = 0.7f)
                )
                
                // 进度条
                LinearProgressIndicator(
                    progress = { animatedLevel },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = when {
                        level < 0.15f -> theme.errorColor
                        level < 0.3f -> theme.warningColor
                        else -> theme.primaryColor
                    }
                )
            }
        }
    }
}

enum class EnergyType {
    GASOLINE, ELECTRIC, HYBRID
}
```

#### 4.2.2 车辆专用按钮

```kotlin
/**
 * 车辆专用按钮
 * 大尺寸、高对比度，适合驾驶环境使用
 * 需求追溯: REQ-FWK-FUN-015
 */
@Composable
fun VehicleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: VehicleButtonVariant = VehicleButtonVariant.PRIMARY,
    size: VehicleButtonSize = VehicleButtonSize.LARGE,
    content: @Composable RowScope.() -> Unit
) {
    val theme = LocalVehicleWidgetTheme.current
    
    val (backgroundColor, contentColor) = when (variant) {
        VehicleButtonVariant.PRIMARY -> theme.primaryColor to theme.onPrimaryColor
        VehicleButtonVariant.SECONDARY -> theme.secondaryColor to theme.onPrimaryColor
        VehicleButtonVariant.DANGER -> theme.errorColor to Color.White
    }
    
    val buttonHeight = when (size) {
        VehicleButtonSize.SMALL -> 56.dp
        VehicleButtonSize.MEDIUM -> 72.dp
        VehicleButtonSize.LARGE -> 88.dp
    }
    
    Button(
        onClick = onClick,
        modifier = modifier
            .height(buttonHeight)
            .minimumInteractiveComponentSize(),
        enabled = enabled,
        shape = RoundedCornerShape(theme.shapes.buttonCornerRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor,
            disabledContainerColor = theme.surfaceColor.copy(alpha = 0.5f),
            disabledContentColor = theme.onBackgroundColor.copy(alpha = 0.3f)
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            content()
        }
    }
}

enum class VehicleButtonVariant {
    PRIMARY, SECONDARY, DANGER
}

enum class VehicleButtonSize {
    SMALL, MEDIUM, LARGE
}

/**
 * 车辆开关控件
 */
@Composable
fun VehicleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null
) {
    val theme = LocalVehicleWidgetTheme.current
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        label?.let {
            Text(
                text = it,
                style = theme.typography.controlLabel,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = theme.primaryColor,
                checkedTrackColor = theme.primaryColor.copy(alpha = 0.5f)
            )
        )
    }
}

/**
 * 车辆滑块控件
 */
@Composable
fun VehicleSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true,
    steps: Int = 0,
    label: String? = null
) {
    val theme = LocalVehicleWidgetTheme.current
    
    Column(modifier = modifier) {
        label?.let {
            Text(
                text = it,
                style = theme.typography.controlLabel,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = theme.primaryColor,
                activeTrackColor = theme.primaryColor,
                inactiveTrackColor = theme.surfaceColor
            ),
            modifier = Modifier.height(48.dp)  // 增大触控区域
        )
    }
}
```

---

## 5. 状态管理设计

### 5.1 状态管理架构

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         状态管理架构                                     │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                     UI Layer (Composable)                        │   │
│  │              仅负责UI渲染，无业务逻辑                            │   │
│  │              通过ViewModel观察状态变化                           │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                              │                                          │
│                              ▼                                          │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                   ViewModel Layer                                │   │
│  │  ┌─────────────────────────────────────────────────────────┐   │   │
│  │  │                   UiState (StateFlow)                    │   │   │
│  │  │  不可变数据类，包含UI所有状态                             │   │   │
│  │  │  data class LauncherUiState(...)                         │   │   │
│  │  └─────────────────────────────────────────────────────────┘   │   │
│  │                              │                                  │   │
│  │                              ▼                                  │   │
│  │  ┌─────────────────────────────────────────────────────────┐   │   │
│  │  │                   Event (SharedFlow)                     │   │   │
│  │  │  一次性事件，如Toast、导航                                │   │   │
│  │  │  sealed class LauncherEvent {...}                        │   │   │
│  │  └─────────────────────────────────────────────────────────┘   │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                              │                                          │
│                              ▼                                          │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                 Repository Layer                                 │   │
│  │              数据源抽象，业务逻辑处理                            │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                              │                                          │
│                              ▼                                          │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                  Data Source Layer                               │   │
│  │         Room / DataStore / Remote API / System API              │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 5.2 状态流转设计

```kotlin
/**
 * 状态流转管理
 * 遵循单向数据流原则
 */

// ==================== 状态定义 ====================

/**
 * Launcher界面状态
 * 不可变数据类，状态变更必须创建新实例
 */
data class LauncherUiState(
    val isLoading: Boolean = false,                    // 加载状态
    val apps: List<AppInfo> = emptyList(),            // 应用列表
    val filteredApps: List<AppInfo> = emptyList(),    // 筛选后的应用
    val shortcuts: List<ShortcutInfo> = emptyList(),  // 快捷入口
    val selectedCategory: AppCategory? = null,        // 当前选中分类
    val isEditMode: Boolean = false,                  // 是否编辑模式
    val errorMessage: String? = null,                 // 错误信息
    val lastOperation: OperationResult? = null        // 最后操作结果
) {
    val isEmpty: Boolean get() = apps.isEmpty()
    val hasError: Boolean get() = errorMessage != null
}

/**
 * 一次性事件
 * 使用SharedFlow，确保每个事件只被消费一次
 */
sealed class LauncherEvent {
    data class ShowToast(val message: String) : LauncherEvent()
    data class NavigateToApp(val appId: String) : LauncherEvent()
    data class ShowPermissionDialog(val permission: String) : LauncherEvent()
    object RequestEditMode : LauncherEvent()
}

/**
 * 操作结果
 */
sealed class OperationResult {
    data class Success(val message: String? = null) : OperationResult()
    data class Error(val exception: Throwable) : OperationResult()
}

// ==================== ViewModel实现 ====================

@HiltViewModel
class LauncherViewModel @Inject constructor(
    private val appRepository: IAppRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    // UI状态 - StateFlow保证状态一致性
    private val _uiState = MutableStateFlow(LauncherUiState())
    val uiState: StateFlow<LauncherUiState> = _uiState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LauncherUiState()
        )
    
    // 一次性事件 - SharedFlow保证事件不丢失
    private val _events = MutableSharedFlow<LauncherEvent>()
    val events: SharedFlow<LauncherEvent> = _events.asSharedFlow()
    
    // 用户意图处理
    fun onIntent(intent: LauncherIntent) {
        when (intent) {
            is LauncherIntent.LoadApps -> loadApps()
            is LauncherIntent.LaunchApp -> launchApp(intent.appId)
            is LauncherIntent.FilterByCategory -> filterByCategory(intent.category)
            is LauncherIntent.SearchApps -> searchApps(intent.query)
            is LauncherIntent.ReorderApps -> reorderApps(intent.fromIndex, intent.toIndex)
            is LauncherIntent.EnterEditMode -> enterEditMode()
            is LauncherIntent.ExitEditMode -> exitEditMode()
            is LauncherIntent.ClearError -> clearError()
        }
    }
    
    private fun loadApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val apps = appRepository.getAllApps()
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        apps = apps,
                        filteredApps = apps
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "加载应用失败: ${e.message}"
                    )
                }
            }
        }
    }
    
    private fun launchApp(appId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            appRepository.launchApp(appId)
                .onSuccess {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            lastOperation = OperationResult.Success()
                        )
                    }
                    _events.emit(LauncherEvent.NavigateToApp(appId))
                }
                .onFailure { error ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message,
                            lastOperation = OperationResult.Error(error)
                        )
                    }
                    _events.emit(LauncherEvent.ShowToast(error.message ?: "启动失败"))
                }
        }
    }
    
    private fun filterByCategory(category: AppCategory?) {
        _uiState.update { currentState ->
            val filtered = if (category == null) {
                currentState.apps
            } else {
                currentState.apps.filter { it.categoryId == category.ordinal }
            }
            currentState.copy(
                selectedCategory = category,
                filteredApps = filtered
            )
        }
    }
    
    private fun searchApps(query: String) {
        viewModelScope.launch {
            val results = if (query.isBlank()) {
                _uiState.value.apps
            } else {
                appRepository.searchApps(query)
            }
            _uiState.update { it.copy(filteredApps = results) }
        }
    }
    
    private fun reorderApps(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val currentList = _uiState.value.apps.toMutableList()
            val movedApp = currentList.removeAt(fromIndex)
            currentList.add(toIndex, movedApp)
            
            _uiState.update { it.copy(apps = currentList) }
            appRepository.updateAppOrder(currentList)
        }
    }
    
    private fun enterEditMode() {
        _uiState.update { it.copy(isEditMode = true) }
    }
    
    private fun exitEditMode() {
        _uiState.update { it.copy(isEditMode = false) }
    }
    
    private fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

/**
 * 用户意图密封类
 * 明确定义用户可以对系统发出的所有操作
 */
sealed class LauncherIntent {
    object LoadApps : LauncherIntent()
    data class LaunchApp(val appId: String) : LauncherIntent()
    data class FilterByCategory(val category: AppCategory?) : LauncherIntent()
    data class SearchApps(val query: String) : LauncherIntent()
    data class ReorderApps(val fromIndex: Int, val toIndex: Int) : LauncherIntent()
    object EnterEditMode : LauncherIntent()
    object ExitEditMode : LauncherIntent()
    object ClearError : LauncherIntent()
}
```

### 5.3 Compose状态订阅

```kotlin
/**
 * UI层状态订阅示例
 */
@Composable
fun LauncherScreen(
    viewModel: LauncherViewModel = hiltViewModel()
) {
    // 订阅UI状态
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // 订阅一次性事件
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LauncherEvent.ShowToast -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is LauncherEvent.NavigateToApp -> {
                    // 导航处理
                }
                // ...
            }
        }
    }
    
    // 界面渲染
    LauncherContent(
        uiState = uiState,
        onIntent = { viewModel.onIntent(it) }
    )
}

@Composable
fun LauncherContent(
    uiState: LauncherUiState,
    onIntent: (LauncherIntent) -> Unit
) {
    // 根据状态渲染UI
    Box(modifier = Modifier.fillMaxSize()) {
        // 应用网格
        AppGrid(
            apps = uiState.filteredApps,
            onAppClick = { onIntent(LauncherIntent.LaunchApp(it)) },
            isEditMode = uiState.isEditMode
        )
        
        // 加载状态
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }
        
        // 空状态
        if (uiState.isEmpty && !uiState.isLoading) {
            EmptyState()
        }
    }
}
```

---

## 6. 应用生命周期管理

### 6.1 生命周期架构

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     应用生命周期管理架构                                 │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    AppLifecycleManager                           │   │
│  │                     (生命周期管理器)                              │   │
│  │  - 监控应用状态变化                                              │   │
│  │  - 处理应用启动/暂停/恢复/停止                                   │   │
│  │  - 行驶限制控制                                                  │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                              │                                          │
│          ┌───────────────────┼───────────────────┐                      │
│          │                   │                   │                      │
│          ▼                   ▼                   ▼                      │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐                │
│  │   AppState   │   │ AppRestrictor│   │ AppRecovery  │                │
│  │   Tracker    │   │              │   │   Manager    │                │
│  │              │   │              │   │              │                │
│  │ 应用状态跟踪  │   │ 行驶限制控制  │   │ 状态恢复管理  │                │
│  └──────────────┘   └──────────────┘   └──────────────┘                │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 6.2 生命周期管理器

```kotlin
/**
 * 应用生命周期管理器
 * 负责管理系统中所有应用的生命周期
 */
@Singleton
class AppLifecycleManager @Inject constructor(
    private val activityManager: ActivityManager,
    private val vehicleService: IVehicleService,
    private val appRepository: IAppRepository,
    @ApplicationContext private val context: Context
) {
    private val appStates = ConcurrentHashMap<String, AppLifecycleState>()
    private val stateFlow = MutableStateFlow<Map<String, AppLifecycleState>>(emptyMap())
    
    // 当前行驶限制状态
    private var currentRestrictionStatus: DrivingRestrictionStatus? = null
    
    init {
        // 监听行驶限制状态
        vehicleService.registerDrivingRestrictionListener(
            object : DrivingRestrictionListener {
                override fun onRestrictionChanged(status: DrivingRestrictionStatus) {
                    handleRestrictionChange(status)
                }
            }
        )
    }
    
    /**
     * 获取应用当前状态
     */
    fun getAppState(appId: String): AppLifecycleState {
        return appStates[appId] ?: AppLifecycleState.STOPPED
    }
    
    /**
     * 观察应用状态变化
     */
    fun observeAppState(appId: String): Flow<AppLifecycleState> {
        return stateFlow.map { it[appId] ?: AppLifecycleState.STOPPED }
    }
    
    /**
     * 启动应用
     */
    suspend fun startApp(appId: String): Result<Unit> {
        // 检查行驶限制
        if (isAppRestricted(appId)) {
            return Result.failure(AppRestrictedException())
        }
        
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(appId)
                ?: return Result.failure(AppNotFoundException())
            
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            
            updateAppState(appId, AppLifecycleState.RUNNING)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 暂停应用（行驶限制时）
     */
    suspend fun pauseApp(appId: String): Result<Unit> {
        return try {
            // 发送暂停广播
            val intent = Intent("com.longcheer.action.PAUSE_APP").apply {
                setPackage(appId)
                putExtra("reason", "driving_restriction")
            }
            context.sendBroadcast(intent)
            
            // 保存应用状态
            saveAppState(appId)
            
            updateAppState(appId, AppLifecycleState.PAUSED)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 恢复应用（停车后）
     */
    suspend fun resumeApp(appId: String): Result<Unit> {
        return try {
            // 发送恢复广播
            val intent = Intent("com.longcheer.action.RESUME_APP").apply {
                setPackage(appId)
            }
            context.sendBroadcast(intent)
            
            // 恢复应用状态
            restoreAppState(appId)
            
            updateAppState(appId, AppLifecycleState.RUNNING)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 停止应用
     */
    suspend fun stopApp(appId: String): Result<Unit> {
        return try {
            activityManager.killBackgroundProcesses(appId)
            updateAppState(appId, AppLifecycleState.STOPPED)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 返回Home
     */
    fun goHome() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
    
    /**
     * 处理行驶限制状态变化
     */
    private fun handleRestrictionChange(status: DrivingRestrictionStatus) {
        currentRestrictionStatus = status
        
        if (status.isRestricted) {
            // 进入限制模式，暂停受限应用
            applyDrivingRestrictions()
        } else {
            // 解除限制，恢复应用
            releaseDrivingRestrictions()
        }
    }
    
    /**
     * 应用行驶限制
     */
    private fun applyDrivingRestrictions() {
        CoroutineScope(Dispatchers.Default).launch {
            appStates.forEach { (appId, state) ->
                if (state == AppLifecycleState.RUNNING && isAppRestricted(appId)) {
                    pauseApp(appId)
                }
            }
        }
    }
    
    /**
     * 解除行驶限制
     */
    private fun releaseDrivingRestrictions() {
        CoroutineScope(Dispatchers.Default).launch {
            appStates.forEach { (appId, state) ->
                if (state == AppLifecycleState.PAUSED) {
                    resumeApp(appId)
                }
            }
        }
    }
    
    /**
     * 检查应用是否受限
     */
    private suspend fun isAppRestricted(appId: String): Boolean {
        val status = currentRestrictionStatus ?: return false
        if (!status.isRestricted) return false
        
        val app = appRepository.getAppById(appId) ?: return false
        if (app.isInWhitelist) return false
        
        return when (app.categoryId) {
            AppCategory.VIDEO.ordinal -> true
            AppCategory.GAME.ordinal -> true
            else -> false
        }
    }
    
    private fun updateAppState(appId: String, state: AppLifecycleState) {
        appStates[appId] = state
        stateFlow.value = appStates.toMap()
    }
    
    private suspend fun saveAppState(appId: String) {
        // 保存应用到本地存储，用于恢复
        context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey("app_state_$appId")] = 
                Json.encodeToString(AppStateSnapshot(appId, System.currentTimeMillis()))
        }
    }
    
    private suspend fun restoreAppState(appId: String) {
        // 从本地存储恢复应用状态
        context.dataStore.data.map { preferences ->
            preferences[stringPreferencesKey("app_state_$appId")]
        }.firstOrNull()?.let { savedState ->
            // 解析并恢复状态
        }
    }
}

/**
 * 应用生命周期状态
 */
enum class AppLifecycleState {
    STOPPED,      // 已停止
    RUNNING,      // 运行中
    PAUSED,       // 已暂停（行驶限制）
    BACKGROUND,   // 后台运行
    RESTRICTED    // 受限状态
}

/**
 * 应用状态快照
 */
@Serializable
data class AppStateSnapshot(
    val appId: String,
    val timestamp: Long,
    val savedState: Map<String, String>? = null
)

/**
 * 应用限制异常
 */
class AppRestrictedException : Exception("应用在当前行驶状态下被限制使用")

/**
 * 应用未找到异常
 */
class AppNotFoundException : Exception("应用未找到")
```

### 6.3 生命周期感知组件

```kotlin
/**
 * 生命周期感知的应用容器
 */
@Composable
fun LifecycleAwareAppContainer(
    appId: String,
    content: @Composable () -> Unit
) {
    val lifecycleManager = LocalAppLifecycleManager.current
    val appState by lifecycleManager.observeAppState(appId)
        .collectAsState(initial = AppLifecycleState.STOPPED)
    
    // 根据状态渲染
    when (appState) {
        AppLifecycleState.RUNNING -> {
            content()
        }
        AppLifecycleState.PAUSED -> {
            PausedOverlay(
                onResume = { lifecycleManager.resumeApp(appId) }
            )
        }
        AppLifecycleState.RESTRICTED -> {
            RestrictedOverlay()
        }
        else -> {
            // 应用未运行
        }
    }
}

/**
 * 暂停覆盖层
 */
@Composable
fun PausedOverlay(onResume: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.8f),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "应用已暂停",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "停车后可继续",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * 限制覆盖层
 */
@Composable
fun RestrictedOverlay() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.9f),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "行驶中无法使用",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "请在停车后使用此功能",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
```

---

## 7. 需求追溯矩阵

### 7.1 SRS需求追溯

| SRS需求ID | 需求描述 | 设计元素 | 实现文件 | 验证方法 |
|-----------|----------|----------|----------|----------|
| REQ-FWK-FUN-014 | Launcher桌面管理 | Launcher模块架构 | launcher/* | 功能测试 |
| REQ-FWK-FUN-014 | 应用图标网格 | AppGridScreen, AppIconItem | AppGridScreen.kt | UI测试 |
| REQ-FWK-FUN-014 | 快捷入口 | ShortcutPanel | ShortcutPanel.kt | 功能测试 |
| REQ-FWK-FUN-015 | 控件库模块 | WidgetLibrary模块 | widgets/* | 功能测试 |
| REQ-FWK-FUN-015 | 基础控件Button | VehicleButton | VehicleButton.kt | UI测试 |
| REQ-FWK-FUN-015 | 基础控件Text | VehicleText | VehicleText.kt | UI测试 |
| REQ-FWK-FUN-015 | 车速显示控件 | SpeedDisplay | SpeedDisplay.kt | 集成测试 |
| REQ-FWK-FUN-015 | 油量/电量显示 | FuelDisplay | FuelDisplay.kt | 集成测试 |
| REQ-FWK-FUN-015 | 胎压显示控件 | TirePressureWidget | TirePressureWidget.kt | 集成测试 |
| REQ-FWK-FUN-016 | Dock栏 | DockBar模块 | dock/* | 功能测试 |
| REQ-FWK-FUN-016 | 固定应用入口 | FixedAppsArea | DockBar.kt | 功能测试 |
| REQ-FWK-FUN-016 | 最近应用显示 | RecentAppsArea | DockBar.kt | 功能测试 |
| REQ-FWK-FUN-016 | Home键功能 | HomeButton | DockBar.kt | 功能测试 |
| REQ-DRV-FUN-007 | 行驶限制 | AppLifecycleManager | AppLifecycleManager.kt | 安全测试 |
| REQ-DRV-FUN-009 | 应用行为控制 | pauseApp/resumeApp | AppLifecycleManager.kt | 功能测试 |
| REQ-PER-001 | 应用启动时间≤1000ms | 优化启动流程 | AppRepository.kt | 性能测试 |
| REQ-PER-002 | 界面切换≤300ms | Compose优化 | 各Screen文件 | 性能测试 |

### 7.2 架构追溯矩阵

| HLD模块ID | HLD模块名称 | 详细设计元素 | 代码实现 | 测试用例 |
|-----------|-------------|--------------|----------|----------|
| FWK-002 | Launcher(LCH) | Launcher模块 | launcher包 | TC-LCH-001~010 |
| FWK-003 | 控件库(UI) | WidgetLibrary模块 | widgets包 | TC-UI-001~015 |
| FWK-002 | Dock栏 | Dock模块 | dock包 | TC-DCK-001~008 |

### 7.3 数据库追溯矩阵

| 数据库表 | 对应实体类 | 使用模块 | 操作类型 |
|----------|------------|----------|----------|
| application | AppInfo | Launcher, Dock | CRUD |
| dock_config | DockConfig | Dock | CRUD |
| app_status | AppLifecycleState | AppLifecycleManager | Update |
| user_settings | UserPreference | WidgetTheme | Read |

---

## 8. 接口定义

### 8.1 内部接口

#### 8.1.1 应用仓库接口

```kotlin
interface IAppRepository {
    suspend fun getAllApps(): List<AppInfo>
    suspend fun getAppById(appId: String): AppInfo?
    suspend fun launchApp(appId: String): Result<Unit>
    suspend fun isAppRestricted(appId: String): Boolean
    fun observeAppChanges(): Flow<List<AppInfo>>
}
```

#### 8.1.2 Dock仓库接口

```kotlin
interface IDockRepository {
    suspend fun getDockConfig(): List<DockConfig>
    suspend fun getRecentApps(limit: Int = 4): List<RecentAppInfo>
    suspend fun handleHomeAction()
}
```

#### 8.1.3 生命周期管理接口

```kotlin
interface IAppLifecycleManager {
    fun getAppState(appId: String): AppLifecycleState
    fun observeAppState(appId: String): Flow<AppLifecycleState>
    suspend fun startApp(appId: String): Result<Unit>
    suspend fun pauseApp(appId: String): Result<Unit>
    suspend fun resumeApp(appId: String): Result<Unit>
}
```

### 8.2 外部服务接口

```kotlin
/**
 * 车辆服务接口 (来自HLD)
 * 用于获取行驶限制状态
 */
interface IVehicleService {
    fun getDrivingRestrictionStatus(): DrivingRestrictionStatus
    fun registerDrivingRestrictionListener(listener: DrivingRestrictionListener)
}
```

---

## 9. 数据持久化设计

### 9.1 Room数据库设计

```kotlin
/**
 * 应用数据库
 */
@Database(
    entities = [AppInfo::class, DockConfig::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun dockDao(): DockDao
}

/**
 * 应用数据访问对象
 */
@Dao
interface AppDao {
    @Query("SELECT * FROM application WHERE is_enabled = 1 ORDER BY sort_order")
    suspend fun getAllApps(): List<AppInfo>
    
    @Query("SELECT * FROM application WHERE app_id = :appId")
    suspend fun getAppById(appId: String): AppInfo?
    
    @Query("SELECT * FROM application WHERE category_id = :categoryId AND is_enabled = 1")
    suspend fun getAppsByCategory(categoryId: Int): List<AppInfo>
    
    @Query("SELECT * FROM application ORDER BY launch_count DESC LIMIT :limit")
    suspend fun getFrequentlyUsedApps(limit: Int): List<AppInfo>
    
    @Query("SELECT * FROM application WHERE app_name LIKE :query OR app_name_en LIKE :query")
    suspend fun searchApps(query: String): List<AppInfo>
    
    @Query("UPDATE application SET is_enabled = :isEnabled WHERE app_id = :appId")
    suspend fun updateAppStatus(appId: String, isEnabled: Boolean)
    
    @Query("UPDATE application SET launch_count = launch_count + 1, last_launch = :timestamp WHERE app_id = :appId")
    suspend fun incrementLaunchCount(appId: String, timestamp: Long)
    
    @Query("UPDATE application SET sort_order = :order WHERE app_id = :appId")
    suspend fun updateSortOrder(appId: String, order: Int)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: AppInfo)
    
    @Delete
    suspend fun deleteApp(app: AppInfo)
}

/**
 * Dock数据访问对象
 */
@Dao
interface DockDao {
    @Query("SELECT * FROM dock_config ORDER BY slot_index")
    suspend fun getAllConfig(): List<DockConfig>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateConfig(config: List<DockConfig>)
}
```

### 9.2 DataStore偏好设置

```kotlin
/**
 * 用户偏好数据存储
 */
class PreferenceRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val Context.dataStore by preferencesDataStore(name = "fwk_preferences")
    
    // 主题设置
    val themeFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.THEME] ?: "dark"
        }
    
    suspend fun setTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = theme
        }
    }
    
    // 应用网格列数
    val gridColumnsFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.GRID_COLUMNS] ?: 4
        }
    
    // Launcher布局配置
    val launcherLayoutFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.LAUNCHER_LAYOUT] ?: "grid"
        }
    
    private object PreferencesKeys {
        val THEME = stringPreferencesKey("theme")
        val GRID_COLUMNS = intPreferencesKey("grid_columns")
        val LAUNCHER_LAYOUT = stringPreferencesKey("launcher_layout")
    }
}
```

---

## 10. 性能与安全设计

### 10.1 性能设计

| 性能指标 | 设计要求 | 实现方案 |
|----------|----------|----------|
| 应用启动 | ≤800ms | 延迟加载、图标缓存、预加载 |
| 界面切换 | ≤150ms | Compose智能重组、动画优化 |
| 应用列表加载 | ≤200ms | 数据库索引、分页加载、内存缓存 |
| 内存占用 | ≤100MB | 图片压缩、资源复用、及时释放 |
| 帧率 | 60fps | Lazy列表优化、减少重组次数 |

### 10.2 性能优化实现

```kotlin
/**
 * 性能优化：应用图标缓存
 */
@Singleton
class IconCache @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val memoryCache = LruCache<String, Drawable>(100)
    
    fun getIcon(appId: String): Drawable? {
        // 1. 内存缓存
        memoryCache.get(appId)?.let { return it }
        
        // 2. 磁盘缓存
        // 3. 从PackageManager加载
        
        return null
    }
}

/**
 * 性能优化：Compose优化
 */
@Composable
fun OptimizedAppList(apps: List<AppInfo>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        // 使用key优化列表性能
        content = {
            items(
                items = apps,
                key = { it.appId }  // 稳定的key
            ) { app ->
                // 使用remember减少不必要的计算
                val icon = remember(app.appId) {
                    loadIcon(app.appId)
                }
                AppIconItem(app = app)
            }
        }
    )
}
```

### 10.3 安全设计

| 安全要求 | 设计措施 | 实现位置 |
|----------|----------|----------|
| 应用签名验证 | 安装时校验签名 | AppRepository |
| 行驶限制E2E | 信任车辆服务数据 | AppLifecycleManager |
| 敏感数据加密 | AES-256加密存储 | PreferenceRepository |
| 权限控制 | 最小权限原则 | AndroidManifest |

---

## 附录

### A. 命名规范

| 类型 | 命名规范 | 示例 |
|------|----------|------|
| 类名 | PascalCase | LauncherViewModel |
| 函数名 | camelCase | launchApp |
| 变量名 | camelCase | appList |
| 常量名 | UPPER_SNAKE_CASE | MAX_APP_COUNT |
| Compose函数 | PascalCase | AppGridScreen |
| 资源文件 | snake_case | app_icon_item.xml |

### B. 代码组织

```
com.longcheer.cockpit.fwk/
├── launcher/
│   ├── LauncherViewModel.kt
│   ├── AppGridScreen.kt
│   ├── AppIconItem.kt
│   └── ShortcutPanel.kt
├── dock/
│   ├── DockViewModel.kt
│   ├── DockBar.kt
│   ├── FixedAppSlot.kt
│   └── HomeButton.kt
├── widgets/
│   ├── VehicleButton.kt
│   ├── VehicleText.kt
│   ├── SpeedDisplay.kt
│   ├── FuelDisplay.kt
│   └── WidgetTheme.kt
├── lifecycle/
│   ├── AppLifecycleManager.kt
│   ├── AppState.kt
│   └── LifecycleAwareAppContainer.kt
├── repository/
│   ├── AppRepository.kt
│   ├── DockRepository.kt
│   └── PreferenceRepository.kt
├── data/
│   ├── AppDatabase.kt
│   ├── AppDao.kt
│   ├── AppInfo.kt
│   └── DockConfig.kt
└── di/
    └── FrameworkModule.kt
```

### C. 相关文档索引

1. 《HLD_概要设计文档_V1.0.md》
2. 《SRS_智能座舱主交互系统_V1.0.md》
3. 《数据库设计文档_V1.0.md》
4. 《系统框架模块测试用例》
5. 《系统框架模块集成测试计划》

---

**文档结束**

*本详细设计文档符合ASPICE Level 3要求，建立了从需求到代码实现的完整追溯链。*

**编制**: 上海龙旗智能科技有限公司  
**审核**: [待填写]  
**批准**: [待填写]  
**日期**: 2024-06-20
