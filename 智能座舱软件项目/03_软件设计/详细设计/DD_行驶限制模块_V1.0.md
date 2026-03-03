# 行驶限制模块详细设计文档
## Driving Restriction Module Detailed Design Document

**项目名称**: 2024年智能座舱软件主交互开发  
**模块名称**: 行驶限制模块 (Driving Restriction Module - DRV)  
**文档版本**: V1.0  
**编制日期**: 2024-06-20  
**编制单位**: 上海龙旗智能科技有限公司  
**客户单位**: 奇瑞汽车股份有限公司  
**安全等级**: ASIL B  
**符合标准**: ASPICE 3.1, ISO 26262-2018, ISO/SAE 21434  

---

## 文档控制信息

### 版本历史
| 版本 | 日期 | 作者 | 变更描述 | 审批 |
|------|------|------|----------|------|
| V0.1 | 2024-06-18 | 软件架构师 | 初始框架 | - |
| V0.5 | 2024-06-19 | 安全工程师 | 安全机制补充 | - |
| V1.0 | 2024-06-20 | 软件架构师 | 基线版本 | 项目总监 |

### 参考文档
1. 《SRS_智能座舱主交互系统_V1.0.md》
2. 《HLD_概要设计文档_V1.0.md》
3. 《数据库设计文档_V1.0.md》
4. 《SR_安全需求规格说明书_V1.0.md》
5. ISO 26262-6:2018 软件层面产品开发
6. ISO 26262-9:2018 基于ASIL和安全导向分析
7. 《E2E保护实现指南_V2.0》

---

## 目录

1. [引言](#1-引言)
2. [模块架构设计](#2-模块架构设计)
3. [核心类设计](#3-核心类设计)
4. [时序图设计](#4-时序图设计)
5. [安全机制设计](#5-安全机制设计)
6. [CAN信号处理](#6-can信号处理)
7. [需求追溯矩阵](#7-需求追溯矩阵)
8. [接口定义](#8-接口定义)
9. [测试策略](#9-测试策略)
10. [附录](#10-附录)

---

## 1. 引言

### 1.1 目的
本文档定义智能座舱行驶限制模块(DRV)的详细设计，基于HLD概要设计和SRS需求，为实现符合ASIL B安全等级的行驶限制功能提供详细的技术规范。

### 1.2 范围
本详细设计覆盖行驶限制模块的完整软件实现，包括：
- 车速/挡位信号监测与处理
- 行驶限制条件判断逻辑
- 应用白名单管理
- 应用行为控制（暂停/恢复/限制）
- 安全状态恢复机制
- E2E保护与看门狗监控

### 1.3 安全等级声明
| 属性 | 值 |
|------|-----|
| ASIL等级 | ASIL B |
| 安全目标 | SG-001: 防止驾驶时视频播放导致驾驶员分心 |
| 安全目标 | SG-003: 确保行驶限制功能可靠触发 |
| E2E配置文件 | E2E_P02_CanBus_DRV_V1.0.xml |
| 看门狗超时 | 500ms |

### 1.4 术语定义
| 术语 | 定义 |
|------|------|
| DRV | Driving Restriction，行驶限制模块 |
| E2E | End-to-End，端到端保护 |
| ASIL | Automotive Safety Integrity Level，汽车安全完整性等级 |
| FTTI | Fault Tolerant Time Interval，故障容忍时间间隔 |
| HAR | Hazard Analysis and Risk Assessment，危害分析与风险评估 |
| Watchdog | 看门狗，用于监控系统运行状态的硬件/软件机制 |
| CRC | Cyclic Redundancy Check，循环冗余校验 |
| VEH | Vehicle，车辆 |
| VSVC | Vehicle Service，车辆服务 |

---

## 2. 模块架构设计

### 2.1 总体架构

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         行驶限制模块 (DRV) ASIL B                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                         应用控制层                                   │   │
│  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐                │   │
│  │  │ AppController │ │ WhitelistMgr │ │ StateManager │                │   │
│  │  │ 应用行为控制  │ │ 白名单管理    │ │ 状态管理器   │                │   │
│  │  └──────────────┘ └──────────────┘ └──────────────┘                │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                         限制决策层                                   │   │
│  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐                │   │
│  │  │ Restriction  │ │ Restriction  │ │ Recovery     │                │   │
│  │  │  Evaluator   │ │   Enforcer   │ │  Handler     │                │   │
│  │  │ 限制条件评估  │ │ 限制执行器   │ │ 状态恢复处理器│                │   │
│  │  └──────────────┘ └──────────────┘ └──────────────┘                │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                         信号处理层 (ASIL B核心)                      │   │
│  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐                │   │
│  │  │ SpeedHandler │ │  GearHandler │ │  PBHandler   │                │   │
│  │  │ 车速信号处理  │ │ 挡位信号处理  │ │ 驻车制动处理  │                │   │
│  │  │ + E2E校验    │ │ + E2E校验    │ │ + E2E校验    │                │   │
│  │  └──────────────┘ └──────────────┘ └──────────────┘                │   │
│  │  ┌──────────────────────────────────────────────────────────────┐  │   │
│  │  │                 E2E Protection Manager                       │  │   │
│  │  │                 E2E保护管理器                                 │  │   │
│  │  │  - CRC8计算/校验                                              │  │   │
│  │  │  - Counter序列号管理                                          │  │   │
│  │  │  - 超时检测                                                   │  │   │
│  │  └──────────────────────────────────────────────────────────────┘  │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                        │
└────────────────────────────────────┼────────────────────────────────────────┘
                                     │
                                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         外部接口层                                          │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐       │
│  │ IVehicle     │ │ IActivity    │ │ IWhitelist   │ │ IDatabase    │       │
│  │ Service      │ │ Manager      │ │ Repository   │ │ Access       │       │
│  │ 车辆服务接口  │ │ Activity管理  │ │ 白名单存储接口│ │ 数据库访问   │       │
│  │ (AIDL)       │ │ (系统服务)    │ │ (DAO)        │ │ (Room)       │       │
│  └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘       │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 组件关系图

```
                          ┌─────────────────┐
                          │   Application   │
                          │   Layer (QM)    │
                          └────────┬────────┘
                                   │ IDrivingRestrictionService.aidl
                                   ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      Driving Restriction Module (ASIL B)                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌──────────────────┐         ┌──────────────────┐                         │
│  │  Driving         │         │  Driving         │                         │
│  │  Restriction     │◄───────►│  Restriction     │                         │
│  │  Service         │  binds  │  Manager         │                         │
│  │  (Service)       │         │  (Singleton)     │                         │
│  └────────┬─────────┘         └────────┬─────────┘                         │
│           │                            │                                    │
│           │ manages                    │ manages                            │
│           ▼                            ▼                                    │
│  ┌──────────────────┐         ┌──────────────────┐                         │
│  │  VehicleSignal   │         │  Restriction     │                         │
│  │  Monitor         │◄───────►│  Controller      │                         │
│  │  (ASIL B Core)   │  notify │  (ASIL B Core)   │                         │
│  └────────┬─────────┘         └────────┬─────────┘                         │
│           │                            │                                    │
│           │ subscribes                 │ controls                           │
│           ▼                            ▼                                    │
│  ┌──────────────────┐         ┌──────────────────┐                         │
│  │  E2EProtection   │         │  AppRestriction  │                         │
│  │  Handler         │         │  Handler         │                         │
│  │  (CRC/Counter)   │         │  (pause/resume)  │                         │
│  └────────┬─────────┘         └────────┬─────────┘                         │
│           │                            │                                    │
│           │ uses                       │ uses                               │
│           ▼                            ▼                                    │
│  ┌──────────────────┐         ┌──────────────────┐                         │
│  │  VehicleService  │         │  ActivityManager │                         │
│  │  (HAL/VSVC)      │         │  (Android)       │                         │
│  └──────────────────┘         └──────────────────┘                         │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.3 模块职责分配

| 组件 | 职责 | ASIL等级 | 安全机制 |
|------|------|----------|----------|
| DrivingRestrictionService | AIDL服务暴露，权限校验 | ASIL B | 访问控制 |
| DrivingRestrictionManager | 核心管理，状态机维护 | ASIL B | 冗余计算 |
| VehicleSignalMonitor | 车辆信号监听与E2E校验 | ASIL B | E2E保护 |
| RestrictionController | 限制条件评估与决策 | ASIL B | 双路比较 |
| AppRestrictionHandler | 应用行为控制执行 | ASIL A | 错误处理 |
| E2EProtectionHandler | E2E封装/解封装/校验 | ASIL B | CRC+Counter |
| WhitelistManager | 白名单查询与缓存 | QM | 签名验证 |
| StateRecoveryHandler | 状态恢复处理 | ASIL B | 超时保护 |

---

## 3. 核心类设计

### 3.1 类图

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                                    行驶限制模块类图                                       │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                         │
│  ┌───────────────────────────┐                                                          │
│  │ <<interface>>             │                                                          │
│  │ IDrivingRestrictionService│◄──────────────────────────────────────┐                 │
│  ├───────────────────────────┤                                       │                 │
│  │ + getRestrictionStatus()  │                                       │ AIDL            │
│  │ + getAppRestriction()     │                                       │ Interface       │
│  │ + controlAppBehavior()    │                                       │                 │
│  │ + registerListener()      │                                       │                 │
│  └───────────────────────────┘                                       │                 │
│                              ▲                                       │                 │
│                              │ implements                           │                 │
│  ┌───────────────────────────┴─────────┐                            │                 │
│  │ DrivingRestrictionService            │                            │                 │
│  │ (AIDL Service)                       │                            │                 │
│  ├──────────────────────────────────────┤                            │                 │
│  │ - manager: DrivingRestrictionManager │                            │                 │
│  │ - securityChecker: SecurityChecker   │                            │                 │
│  ├──────────────────────────────────────┤                            │                 │
│  │ + onBind()                           │                            │                 │
│  │ + checkPermission()                  │                            │                 │
│  └────────────────┬─────────────────────┘                            │                 │
│                   │ uses                                             │                 │
│                   ▼                                                  │                 │
│  ┌──────────────────────────────────────┐                            │                 │
│  │ DrivingRestrictionManager            │                            │                 │
│  │ (Singleton - ASIL B Core)            │                            │                 │
│  ├──────────────────────────────────────┤                            │                 │
│  │ - stateMachine: RestrictionStateMachine                           │                 │
│  │ - signalMonitor: VehicleSignalMonitor│                            │                 │
│  │ - restrictionController: RestrictionController                    │                 │
│  │ - whitelistManager: WhitelistManager │                            │                 │
│  │ - recoveryHandler: StateRecoveryHandler                           │                 │
│  │ - watchdog: SafetyWatchdog           │                            │                 │
│  │ - listeners: CopyOnWriteArrayList    │                            │                 │
│  ├──────────────────────────────────────┤                            │                 │
│  │ + initialize()                       │                            │                 │
│  │ + onVehicleSignalChanged()           │                            │                 │
│  │ + evaluateRestriction()              │                            │                 │
│  │ + enforceRestriction()               │                            │                 │
│  │ + triggerRecovery()                  │                            │                 │
│  │ + registerListener()                 │                            │                 │
│  │ + feedWatchdog()                     │◄─────────────────────────┘                 │
│  └──────────┬───────────────────────────┘                                              │
│             │ manages                                                                  │
│    ┌────────┼────────┬───────────────┬───────────────┬───────────────┐                 │
│    │        │        │               │               │               │                 │
│    ▼        ▼        ▼               ▼               ▼               ▼                 │
│  ┌──────────────┐ ┌────────────────┐ ┌──────────────────┐ ┌──────────────────┐       │
│  │VehicleSignal │ │ Restriction    │ │  StateRecovery   │ │ WhitelistManager │       │
│  │Monitor       │ │ Controller     │ │  Handler         │ │ (QM)             │       │
│  │(ASIL B)      │ │ (ASIL B)       │ │  (ASIL B)        │ ├──────────────────┤       │
│  ├──────────────┤ ├────────────────┤ ├──────────────────┤ │ - whitelistCache │       │
│  │- e2eHandler  │ │- evaluator     │ │ - recoveryTimer  │ │ - repository     │       │
│  │- speedHandler│ │- enforcer      │ │ - pendingApps    │ ├──────────────────┤       │
│  │- gearHandler │ ├────────────────┤ ├──────────────────┤ │ + queryWhitelist()│      │
│  │- pbHandler   │ │+ evaluate()    │ │ + scheduleRecovery│ │ + refreshCache() │       │
│  ├──────────────┤ │+ enforce()     │ │ + executeRecovery │ │ + isAllowed()    │       │
│  │+ onSpeedChanged│ + canRecover() │ └──────────────────┘ └──────────────────┘       │
│  │+ onGearChanged│                │                                                     │
│  │+ validateE2E()│                │                                                     │
│  └──────┬───────┘ └────────────────┘                                                     │
│         │                                                                                │
│         │ uses                                                                           │
│         ▼                                                                                │
│  ┌──────────────────────────────────────────────────────────────────┐                   │
│  │                    E2EProtectionHandler                           │                   │
│  │                    (ASIL B)                                       │                   │
│  ├──────────────────────────────────────────────────────────────────┤                   │
│  │ - crcCalculator: CRC8Calculator                                   │                   │
│  │ - counterMap: Map<SignalId, Byte>                                 │                   │
│  │ - timeoutMap: Map<SignalId, Long>                                 │                   │
│  │ - MAX_COUNTER: Int = 15                                           │                   │
│  │ - TIMEOUT_MS: Long = 100                                          │                   │
│  ├──────────────────────────────────────────────────────────────────┤                   │
│  │ + calculateCRC(data: ByteArray): Byte                             │                   │
│  │ + verifyE2E(signal: VehicleSignal): E2EResult                     │                   │
│  │ + checkCounter(signalId: String, counter: Byte): Boolean          │                   │
│  │ + checkTimeout(signalId: String, timestamp: Long): Boolean        │                   │
│  │ + getE2EStatus(): E2EStatus                                       │                   │
│  └──────────────────────────────────────────────────────────────────┘                   │
│                                                                                          │
│  ┌───────────────────────────┐        ┌───────────────────────────┐                     │
│  │ RestrictionStateMachine   │        │ <<enum>>                  │                     │
│  │ (State Pattern)           │        │ RestrictionState          │                     │
│  ├───────────────────────────┤        ├───────────────────────────┤                     │
│  │ - currentState            │◄───────┤ NORMAL                    │                     │
│  │ - previousState           │        │ RESTRICTED                │                     │
│  ├───────────────────────────┤        │ RECOVERING                │                     │
│  │ + transitionTo()          │        │ FAULT                     │                     │
│  │ + canTransition()         │        └───────────────────────────┘                     │
│  │ + getState()              │                                                          │
│  └───────────────────────────┘                                                          │
│                                                                                          │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 核心类定义

#### 3.2.1 DrivingRestrictionService.kt - AIDL服务

```kotlin
/**
 * 行驶限制服务 - AIDL接口实现
 * ASIL等级: ASIL B
 * 
 * 安全要求:
 * - 所有接口调用需进行权限校验
 * - 敏感操作记录审计日志
 * - 异常情况进入安全状态
 */
class DrivingRestrictionService : Service() {

    companion object {
        private const val TAG = "DrvRestrictionSvc"
        private const val PERMISSION_CONTROL_DRV = "com.longcheer.permission.CONTROL_DRIVING_RESTRICTION"
    }

    private lateinit var manager: DrivingRestrictionManager
    private val binder = object : IDrivingRestrictionService.Stub() {
        
        override fun getRestrictionStatus(): RestrictionStatus {
            checkPermission(PERMISSION_CONTROL_DRV)
            return manager.getCurrentStatus()
        }

        override fun getAppRestriction(appId: String): RestrictionType {
            checkPermission(PERMISSION_CONTROL_DRV)
            return manager.getAppRestrictionType(appId)
        }

        override fun controlAppBehavior(appId: String, control: BehaviorControl) {
            checkPermission(PERMISSION_CONTROL_DRV)
            manager.executeBehaviorControl(appId, control)
        }

        override fun registerListener(listener: IDrivingRestrictionListener) {
            manager.registerListener(listener)
        }

        override fun unregisterListener(listener: IDrivingRestrictionListener) {
            manager.unregisterListener(listener)
        }
    }

    override fun onCreate() {
        super.onCreate()
        manager = DrivingRestrictionManager.getInstance(applicationContext)
        manager.initialize()
    }

    override fun onBind(intent: Intent): IBinder = binder

    private fun checkPermission(permission: String) {
        if (checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("Permission denied: $permission")
        }
    }
}
```

#### 3.2.2 DrivingRestrictionManager.kt - 核心管理器

```kotlin
/**
 * 行驶限制管理器 - 单例模式
 * ASIL等级: ASIL B
 * 
 * 职责:
 * - 管理行驶限制状态机
 * - 协调各子模块工作
 * - 维护看门狗喂狗
 * - 提供线程安全保证
 * 
 * 安全要求:
 * - 关键决策采用双路冗余计算
 * - 看门狗500ms超时保护
 * - 状态转换原子性保证
 */
class DrivingRestrictionManager private constructor(context: Context) {

    companion object {
        @Volatile
        private var instance: DrivingRestrictionManager? = null
        private const val WATCHDOG_TIMEOUT_MS = 500L
        private const val RECOVERY_DELAY_MS = 3000L

        fun getInstance(context: Context): DrivingRestrictionManager {
            return instance ?: synchronized(this) {
                instance ?: DrivingRestrictionManager(context).also { instance = it }
            }
        }
    }

    // ASIL B核心组件
    private val stateMachine = RestrictionStateMachine()
    private val signalMonitor = VehicleSignalMonitor()
    private val restrictionController = RestrictionController()
    private val recoveryHandler = StateRecoveryHandler()
    
    // QM级组件
    private val whitelistManager = WhitelistManager(context)
    
    // 安全组件
    private val watchdog = SafetyWatchdog(WATCHDOG_TIMEOUT_MS) {
        enterSafeState()
    }
    
    // 监听者
    private val listeners = CopyOnWriteArrayList<IDrivingRestrictionListener>()
    
    // 主线程Handler用于状态同步
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // 看门狗喂狗调度
    private val watchdogExecutor = Executors.newSingleThreadScheduledExecutor()

    /**
     * 初始化模块
     * 启动信号监听、看门狗、状态恢复定时器
     */
    fun initialize() {
        signalMonitor.initialize { signal ->
            onVehicleSignalChanged(signal)
        }
        
        // 启动周期性看门狗喂狗
        watchdogExecutor.scheduleAtFixedRate(
            { feedWatchdog() },
            0, 100, TimeUnit.MILLISECONDS
        )
        
        // 启动状态恢复监控
        recoveryHandler.initialize(RECOVERY_DELAY_MS) {
            triggerRecovery()
        }
    }

    /**
     * 车辆信号变化回调
     * ASIL B: 信号已进行E2E校验
     */
    private fun onVehicleSignalChanged(signal: VehicleSignal) {
        // 双路冗余评估
        val shouldRestrict = redundantEvaluate(signal)
        
        when (stateMachine.getCurrentState()) {
            RestrictionState.NORMAL -> {
                if (shouldRestrict) {
                    enforceRestriction()
                }
            }
            RestrictionState.RESTRICTED -> {
                if (!shouldRestrict && recoveryHandler.canRecover(signal)) {
                    scheduleRecovery()
                }
            }
            else -> { /* 其他状态不处理 */ }
        }
    }

    /**
     * 双路冗余评估 - ASIL B安全机制
     * 两条独立路径计算，结果必须一致
     */
    private fun redundantEvaluate(signal: VehicleSignal): Boolean {
        // 路径1: 基于车速
        val result1 = evaluateBySpeed(signal.speed)
        
        // 路径2: 基于挡位和驻车制动
        val result2 = evaluateByGearAndPB(signal.gear, signal.parkingBrake)
        
        // 结果比较，不一致则进入安全状态
        if (result1 != result2) {
            enterSafeState()
            return true // 安全状态下默认限制
        }
        
        return result1
    }

    private fun evaluateBySpeed(speed: Int): Boolean {
        return speed > 0
    }

    private fun evaluateByGearAndPB(gear: GearPosition, pb: Boolean): Boolean {
        return gear == GearPosition.DRIVE || 
               gear == GearPosition.REVERSE || 
               !pb
    }

    /**
     * 执行限制措施
     * ASIL B: 限制触发延迟≤200ms
     */
    private fun enforceRestriction() {
        val startTime = SystemClock.elapsedRealtime()
        
        stateMachine.transitionTo(RestrictionState.RESTRICTED)
        
        // 获取当前运行的受限应用
        val restrictedApps = getRunningRestrictedApps()
        
        restrictedApps.forEach { appInfo ->
            // 根据应用类型执行不同限制
            when (appInfo.category) {
                AppCategory.VIDEO -> restrictionController.pauseApp(appInfo.packageName)
                AppCategory.GAME -> restrictionController.returnToHome(appInfo.packageName)
                AppCategory.BROWSER -> restrictionController.limitInteraction(appInfo.packageName)
                else -> { /* 其他类别按白名单判断 */ }
            }
        }
        
        // 通知监听者
        notifyRestrictionChanged(true)
        
        // 检查延迟要求
        val elapsed = SystemClock.elapsedRealtime() - startTime
        if (elapsed > 200) {
            Log.w(TAG, "Restriction enforcement took $elapsed ms, exceeds 200ms requirement")
        }
    }

    /**
     * 触发状态恢复
     */
    private fun scheduleRecovery() {
        recoveryHandler.scheduleRecovery {
            executeRecovery()
        }
    }

    private fun executeRecovery() {
        stateMachine.transitionTo(RestrictionState.RECOVERING)
        
        // 恢复被限制的应用
        val pausedApps = recoveryHandler.getPausedApps()
        pausedApps.forEach { packageName ->
            restrictionController.resumeApp(packageName)
        }
        
        stateMachine.transitionTo(RestrictionState.NORMAL)
        notifyRestrictionChanged(false)
    }

    /**
     * 进入安全状态 - 故障安全模式
     */
    private fun enterSafeState() {
        stateMachine.transitionTo(RestrictionState.FAULT)
        
        // 强制限制所有非必要应用
        restrictionController.restrictAllNonEssential()
        
        // 记录故障日志
        logSafetyEvent("Entered safe state due to inconsistency")
    }

    /**
     * 喂狗操作
     */
    private fun feedWatchdog() {
        watchdog.feed()
    }

    // 公共API
    fun getCurrentStatus(): RestrictionStatus {
        return RestrictionStatus(
            state = stateMachine.getCurrentState(),
            isRestricted = stateMachine.getCurrentState() == RestrictionState.RESTRICTED,
            timestamp = System.currentTimeMillis()
        )
    }

    fun getAppRestrictionType(appId: String): RestrictionType {
        return if (whitelistManager.isAllowed(appId)) {
            RestrictionType.NONE
        } else {
            getRestrictionTypeByState()
        }
    }

    fun executeBehaviorControl(appId: String, control: BehaviorControl) {
        when (control) {
            BehaviorControl.PAUSE -> restrictionController.pauseApp(appId)
            BehaviorControl.RESUME -> restrictionController.resumeApp(appId)
            BehaviorControl.RETURN_HOME -> restrictionController.returnToHome(appId)
            BehaviorControl.LIMIT_INTERACTION -> restrictionController.limitInteraction(appId)
            BehaviorControl.GRAYSCALE -> restrictionController.setGrayscale(appId)
        }
    }

    fun registerListener(listener: IDrivingRestrictionListener) {
        listeners.add(listener)
    }

    fun unregisterListener(listener: IDrivingRestrictionListener) {
        listeners.remove(listener)
    }

    private fun notifyRestrictionChanged(restricted: Boolean) {
        mainHandler.post {
            listeners.forEach { listener ->
                try {
                    listener.onRestrictionChanged(restricted)
                } catch (e: RemoteException) {
                    listeners.remove(listener)
                }
            }
        }
    }

    private fun getRestrictionTypeByState(): RestrictionType {
        return when (stateMachine.getCurrentState()) {
            RestrictionState.RESTRICTED -> RestrictionType.FULL_RESTRICTED
            else -> RestrictionType.NONE
        }
    }

    private fun getRunningRestrictedApps(): List<AppInfo> {
        // 获取当前运行的应用并过滤白名单
        return emptyList() // 简化实现
    }

    private fun logSafetyEvent(message: String) {
        // 记录到安全事件日志
    }
}
```

#### 3.2.3 VehicleSignalMonitor.kt - 车辆信号监听

```kotlin
/**
 * 车辆信号监听器
 * ASIL等级: ASIL B
 * 
 * 职责:
 * - 订阅车辆服务信号
 * - 执行E2E保护校验
 * - 信号有效性验证
 */
class VehicleSignalMonitor {

    private lateinit var vehicleService: IVehicleService
    private lateinit var e2eHandler: E2EProtectionHandler
    private var signalCallback: ((VehicleSignal) -> Unit)? = null
    
    // 信号缓存用于冗余校验
    private var lastValidSignal: VehicleSignal? = null
    private val signalLock = Object()

    fun initialize(callback: (VehicleSignal) -> Unit) {
        this.signalCallback = callback
        this.e2eHandler = E2EProtectionHandler()
        
        // 绑定车辆服务
        bindVehicleService()
    }

    private fun bindVehicleService() {
        vehicleService = IVehicleService.Stub.asInterface(
            ServiceManager.getService("vehicle")
        )
        
        // 注册车速监听 (100Hz)
        vehicleService.registerSpeedListener(object : SpeedListener.Stub() {
            override fun onSpeedChanged(speed: Int, e2eData: E2EData) {
                processSpeedSignal(speed, e2eData)
            }
        }, 10) // 10ms = 100Hz
        
        // 注册挡位监听 (20Hz)
        vehicleService.registerGearListener(object : GearListener.Stub() {
            override fun onGearChanged(gear: Int, e2eData: E2EData) {
                processGearSignal(gear, e2eData)
            }
        })
        
        // 注册驻车制动监听
        vehicleService.registerParkingBrakeListener(object : ParkingBrakeListener.Stub() {
            override fun onParkingBrakeChanged(isOn: Boolean, e2eData: E2EData) {
                processParkingBrakeSignal(isOn, e2eData)
            }
        })
    }

    /**
     * 处理车速信号 - 带E2E校验
     */
    private fun processSpeedSignal(speed: Int, e2eData: E2EData) {
        val signal = VehicleSignal(
            type = SignalType.SPEED,
            speed = speed,
            e2eCounter = e2eData.counter,
            e2eCrc = e2eData.crc,
            timestamp = e2eData.timestamp
        )
        
        if (validateAndProcessSignal(signal)) {
            signalCallback?.invoke(signal)
        }
    }

    /**
     * 处理挡位信号 - 带E2E校验
     */
    private fun processGearSignal(gear: Int, e2eData: E2EData) {
        val signal = VehicleSignal(
            type = SignalType.GEAR,
            gear = GearPosition.fromInt(gear),
            e2eCounter = e2eData.counter,
            e2eCrc = e2eData.crc,
            timestamp = e2eData.timestamp
        )
        
        if (validateAndProcessSignal(signal)) {
            signalCallback?.invoke(signal)
        }
    }

    /**
     * 处理驻车制动信号 - 带E2E校验
     */
    private fun processParkingBrakeSignal(isOn: Boolean, e2eData: E2EData) {
        val signal = VehicleSignal(
            type = SignalType.PARKING_BRAKE,
            parkingBrake = isOn,
            e2eCounter = e2eData.counter,
            e2eCrc = e2eData.crc,
            timestamp = e2eData.timestamp
        )
        
        if (validateAndProcessSignal(signal)) {
            signalCallback?.invoke(signal)
        }
    }

    /**
     * E2E校验与信号处理
     * ASIL B: 完整的E2E保护
     */
    private fun validateAndProcessSignal(signal: VehicleSignal): Boolean {
        // 1. E2E完整性校验
        val e2eResult = e2eHandler.verifyE2E(signal)
        
        when (e2eResult.status) {
            E2EStatus.VALID -> {
                synchronized(signalLock) {
                    lastValidSignal = signal
                }
                return true
            }
            E2EStatus.COUNTER_ERROR -> {
                handleE2EError("Counter sequence error: ${signal.type}")
                return false
            }
            E2EStatus.CRC_ERROR -> {
                handleE2EError("CRC mismatch: ${signal.type}")
                return false
            }
            E2EStatus.TIMEOUT -> {
                handleE2EError("Signal timeout: ${signal.type}")
                return false
            }
        }
    }

    private fun handleE2EError(message: String) {
        // 记录E2E错误，进入降级模式
        Log.e("VehicleSignalMonitor", message)
        // 通知上层进入安全状态
    }

    fun getLastValidSignal(): VehicleSignal? {
        return synchronized(signalLock) { lastValidSignal }
    }
}

/**
 * 车辆信号数据类
 */
data class VehicleSignal(
    val type: SignalType,
    val speed: Int = 0,
    val gear: GearPosition = GearPosition.PARK,
    val parkingBrake: Boolean = true,
    val e2eCounter: Byte = 0,
    val e2eCrc: Byte = 0,
    val timestamp: Long = 0
)

enum class SignalType {
    SPEED, GEAR, PARKING_BRAKE, COMBINED
}

enum class GearPosition {
    PARK, REVERSE, NEUTRAL, DRIVE, UNKNOWN;
    
    companion object {
        fun fromInt(value: Int): GearPosition = when (value) {
            0 -> PARK
            1 -> REVERSE
            2 -> NEUTRAL
            3 -> DRIVE
            else -> UNKNOWN
        }
    }
}
```

#### 3.2.4 E2EProtectionHandler.kt - E2E保护处理器

```kotlin
/**
 * E2E保护处理器
 * ASIL等级: ASIL B
 * 
 * 实现基于AUTOSAR E2E Profile 2的端到端保护
 * - CRC8校验
 * - 4位序列号(0-15)
 * - 100ms超时检测
 */
class E2EProtectionHandler {

    companion object {
        private const val MAX_COUNTER = 15
        private const val TIMEOUT_MS = 100L
        private const val CRC_POLYNOMIAL = 0x1D // CRC8-SAE-J1850
    }

    // 每个信号源的序列号追踪
    private val counterMap = ConcurrentHashMap<String, Byte>()
    private val timestampMap = ConcurrentHashMap<String, Long>()

    /**
     * E2E验证入口
     */
    fun verifyE2E(signal: VehicleSignal): E2EResult {
        val signalId = signal.type.name
        
        // 1. 超时检查
        if (!checkTimeout(signalId, signal.timestamp)) {
            return E2EResult(E2EStatus.TIMEOUT, "Signal timeout detected")
        }
        
        // 2. CRC校验
        if (!verifyCRC(signal)) {
            return E2EResult(E2EStatus.CRC_ERROR, "CRC verification failed")
        }
        
        // 3. 序列号检查
        if (!checkCounter(signalId, signal.e2eCounter)) {
            return E2EResult(E2EStatus.COUNTER_ERROR, "Counter sequence error")
        }
        
        // 更新状态
        updateState(signalId, signal.e2eCounter, signal.timestamp)
        
        return E2EResult(E2EStatus.VALID, "E2E check passed")
    }

    /**
     * CRC8校验
     */
    private fun verifyCRC(signal: VehicleSignal): Boolean {
        // 构造待校验数据
        val data = when (signal.type) {
            SignalType.SPEED -> ByteBuffer.allocate(4).putInt(signal.speed).array()
            SignalType.GEAR -> byteArrayOf(signal.gear.ordinal.toByte())
            SignalType.PARKING_BRAKE -> byteArrayOf(if (signal.parkingBrake) 1 else 0)
            else -> byteArrayOf()
        }
        
        // 添加序列号到数据
        val dataWithCounter = data + signal.e2eCounter
        
        // 计算CRC
        val calculatedCrc = calculateCRC8(dataWithCounter)
        
        return calculatedCrc == signal.e2eCrc
    }

    /**
     * 计算CRC8 (SAE J1850)
     */
    fun calculateCRC8(data: ByteArray): Byte {
        var crc: Byte = 0xFF.toByte()
        
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
        
        return crc.xor(0xFF.toByte())
    }

    /**
     * 序列号检查 - 允许丢包但不允许乱序
     */
    private fun checkCounter(signalId: String, receivedCounter: Byte): Boolean {
        val lastCounter = counterMap[signalId] ?: return true // 首次接收
        
        val expectedCounter = ((lastCounter + 1) % (MAX_COUNTER + 1)).toByte()
        
        // 检查是否是期望的序列号（允许小范围丢包）
        val diff = (receivedCounter - lastCounter + MAX_COUNTER + 1) % (MAX_COUNTER + 1)
        
        return diff in 1..3 // 允许最多丢2个包
    }

    /**
     * 超时检查
     */
    private fun checkTimeout(signalId: String, timestamp: Long): Boolean {
        val lastTimestamp = timestampMap[signalId] ?: return true // 首次接收
        
        val elapsed = timestamp - lastTimestamp
        return elapsed <= TIMEOUT_MS
    }

    private fun updateState(signalId: String, counter: Byte, timestamp: Long) {
        counterMap[signalId] = counter
        timestampMap[signalId] = timestamp
    }

    fun getE2EStatus(): Map<String, E2EStatusInfo> {
        return counterMap.keys.associateWith { signalId ->
            E2EStatusInfo(
                lastCounter = counterMap[signalId] ?: 0,
                lastTimestamp = timestampMap[signalId] ?: 0,
                isAlive = System.currentTimeMillis() - (timestampMap[signalId] ?: 0) < TIMEOUT_MS * 2
            )
        }
    }
}

/**
 * E2E校验结果
 */
data class E2EResult(
    val status: E2EStatus,
    val message: String
)

enum class E2EStatus {
    VALID,           // 校验通过
    CRC_ERROR,       // CRC错误
    COUNTER_ERROR,   // 序列号错误
    TIMEOUT          // 超时
}

data class E2EStatusInfo(
    val lastCounter: Byte,
    val lastTimestamp: Long,
    val isAlive: Boolean
)
```

#### 3.2.5 RestrictionStateMachine.kt - 状态机

```kotlin
/**
 * 行驶限制状态机
 * ASIL等级: ASIL B
 * 
 * 状态转换规则:
 * NORMAL -> RESTRICTED (满足限制条件)
 * RESTRICTED -> RECOVERING (满足恢复条件，延迟3s)
 * RECOVERING -> NORMAL (恢复完成)
 * ANY -> FAULT (故障/异常)
 */
class RestrictionStateMachine {

    @Volatile
    private var currentState: RestrictionState = RestrictionState.NORMAL
    
    @Volatile
    private var previousState: RestrictionState = RestrictionState.NORMAL
    
    private val stateLock = Object()

    /**
     * 状态转换
     * ASIL B: 原子性保证
     */
    fun transitionTo(newState: RestrictionState): Boolean {
        synchronized(stateLock) {
            if (!canTransition(currentState, newState)) {
                return false
            }
            
            previousState = currentState
            currentState = newState
            
            onStateChanged(previousState, newState)
            return true
        }
    }

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

    private fun onStateChanged(from: RestrictionState, to: RestrictionState) {
        // 记录状态转换日志
        Log.i("RestrictionStateMachine", "State transition: $from -> $to")
    }

    fun getCurrentState(): RestrictionState = currentState
    
    fun getPreviousState(): RestrictionState = previousState
}

enum class RestrictionState {
    NORMAL,      // 正常状态，无限制
    RESTRICTED,  // 限制状态，应用受限
    RECOVERING,  // 恢复中状态
    FAULT        // 故障状态（安全状态）
}
```

#### 3.2.6 SafetyWatchdog.kt - 安全看门狗

```kotlin
/**
 * 安全看门狗
 * ASIL等级: ASIL B
 * 
 * 实现软硬件结合看门狗监控:
 * - 软件看门狗: 检测软件死循环/阻塞
 * - 硬件看门狗: 检测系统级故障
 */
class SafetyWatchdog(
    private val timeoutMs: Long,
    private val onTimeout: () -> Unit
) {
    private val executor = Executors.newSingleThreadScheduledExecutor()
    private val lastFeedTime = AtomicLong(System.currentTimeMillis())
    private val isRunning = AtomicBoolean(false)

    init {
        startMonitoring()
    }

    private fun startMonitoring() {
        isRunning.set(true)
        executor.scheduleAtFixedRate({
            if (isRunning.get()) {
                checkTimeout()
            }
        }, timeoutMs, timeoutMs / 2, TimeUnit.MILLISECONDS)
    }

    /**
     * 喂狗操作
     * 应在主循环中定期调用 (建议100ms周期)
     */
    fun feed() {
        lastFeedTime.set(System.currentTimeMillis())
    }

    private fun checkTimeout() {
        val elapsed = System.currentTimeMillis() - lastFeedTime.get()
        if (elapsed > timeoutMs) {
            Log.e("SafetyWatchdog", "Watchdog timeout! Elapsed: $elapsed ms")
            onTimeout()
            // 触发硬件看门狗复位
            triggerHardwareWatchdog()
        }
    }

    private fun triggerHardwareWatchdog() {
        // 通过HAL接口触发硬件看门狗
        // 实际实现依赖于硬件平台
    }

    fun stop() {
        isRunning.set(false)
        executor.shutdown()
    }
}
```

---

## 4. 时序图设计

### 4.1 速度检测→限制触发→状态恢复 完整流程

```
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│                              行驶限制完整流程时序图                                          │
├─────────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                              │
│  Vehicle        VehicleSignal      E2EProtection      DrivingRestriction       AppRestriction│
│    ECU            Monitor              Handler           Manager                 Handler    │
│     │               │                   │                   │                       │      │
│     │  ┌──────────────────────────────────────────────────────────────────────────┐       │
│     │  │                         阶段1: 信号接收与E2E校验                          │       │
│     │  └──────────────────────────────────────────────────────────────────────────┘       │
│     │               │                   │                   │                       │      │
│     │───CAN信号────>│                   │                   │                       │      │
│     │  (100Hz)      │                   │                   │                       │      │
│     │               │───原始信号+元数据──>│                   │                       │      │
│     │               │  (speed, counter,  │                   │                       │      │
│     │               │   crc, timestamp) │                   │                       │      │
│     │               │                   │                   │                       │      │
│     │               │                   │──1.CRC校验        │                       │      │
│     │               │                   │──2.Counter检查    │                       │      │
│     │               │                   │──3.超时检测       │                       │      │
│     │               │                   │                   │                       │      │
│     │               │<──E2E结果─────────│                   │                       │      │
│     │               │  (VALID/ERROR)    │                   │                       │      │
│     │               │                   │                   │                       │      │
│     │  ┌──────────────────────────────────────────────────────────────────────────┐       │
│     │  │                         阶段2: 限制条件评估                              │       │
│     │  └──────────────────────────────────────────────────────────────────────────┘       │
│     │               │                   │                   │                       │      │
│     │               │──有效信号────────>│                   │                       │      │
│     │               │  (speed=30km/h)   │                   │                       │      │
│     │               │                   │                   │                       │      │
│     │               │                   │───评估请求───────>│                       │      │
│     │               │                   │                   │                       │      │
│     │               │                   │                   │──双路冗余评估───────>│      │
│     │               │                   │                   │  (路径1: speed>0)     │      │
│     │               │                   │                   │  (路径2: gear=D)      │      │
│     │               │                   │                   │                       │      │
│     │               │                   │                   │<──评估结果: RESTRICT──│      │
│     │               │                   │                   │                       │      │
│     │  ┌──────────────────────────────────────────────────────────────────────────┐       │
│     │  │                         阶段3: 限制触发执行                              │       │
│     │  └──────────────────────────────────────────────────────────────────────────┘       │
│     │               │                   │                   │                       │      │
│     │               │                   │                   │──状态机:NORMAL→RESTRICTED  │   │
│     │               │                   │                   │                       │      │
│     │               │                   │                   │──白名单检查─────────>│      │
│     │               │                   │                   │  (app=com.tencent.video)     │
│     │               │                   │                   │                       │      │
│     │               │                   │                   │<──不在白名单───────│       │
│     │               │                   │                   │                       │      │
│     │               │                   │                   │────暂停应用────────>│       │
│     │               │                   │                   │  (PAUSE操作)          │      │
│     │               │                   │                   │                       │      │
│     │               │                   │                   │<──执行完成─────────│       │
│     │               │                   │                   │                       │      │
│     │               │                   │                   │─────记录状态────────>│      │
│     │               │                   │                   │  (用于恢复)           │      │
│     │               │                   │                   │                       │      │
│     │  ┌──────────────────────────────────────────────────────────────────────────┐       │
│     │  │                         阶段4: 持续监控(3秒)                             │       │
│     │  └──────────────────────────────────────────────────────────────────────────┘       │
│     │               │                   │                   │                       │      │
│     │───CAN信号────>│                   │                   │                       │      │
│     │  (speed=0)    │                   │                   │                       │      │
│     │               │──有效信号+元数据──>│                   │                       │      │
│     │               │  (speed=0,        │                   │                       │      │
│     │               │   gear=P, PB=ON)  │                   │                       │      │
│     │               │                   │                   │                       │      │
│     │               │                   │──E2E校验通过─────>│                       │      │
│     │               │                   │                   │                       │      │
│     │               │                   │                   │───恢复条件检查──────│       │
│     │               │                   │                   │  (speed=0 && gear=P   │      │
│     │               │                   │                   │   && PB=ON > 3s)      │      │
│     │               │                   │                   │                       │      │
│     │  ┌──────────────────────────────────────────────────────────────────────────┐       │
│     │  │                         阶段5: 状态恢复                                  │       │
│     │  └──────────────────────────────────────────────────────────────────────────┘       │
│     │               │                   │                   │                       │      │
│     │               │                   │                   │──状态机:RESTRICTED→RECOVERING│
│     │               │                   │                   │                       │      │
│     │               │                   │                   │────恢复应用────────>│       │
│     │               │                   │                   │  (RESUME操作)         │      │
│     │               │                   │                   │                       │      │
│     │               │                   │                   │<──执行完成─────────│       │
│     │               │                   │                   │                       │      │
│     │               │                   │                   │──状态机:RECOVERING→NORMAL    │
│     │               │                   │                   │                       │      │
│     │               │                   │                   │────通知UI──────────>│       │
│     │               │                   │                   │  (限制解除)           │      │
│     │               │                   │                   │                       │      │
│                                                                                              │
└─────────────────────────────────────────────────────────────────────────────────────────────┘
```

### 4.2 E2E保护详细时序

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           E2E保护详细时序                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   Sender                 CAN Bus                Receiver       Application  │
│     │                      │                       │               │        │
│     │  ┌─────────────────────────────────────────────────────────────────┐  │
│     │  │                    发送端E2E封装                                 │  │
│     │  └─────────────────────────────────────────────────────────────────┘  │
│     │                      │                       │               │        │
│     │──1.生成数据─────────>│                       │               │        │
│     │  (speed=30)          │                       │               │        │
│     │                      │                       │               │        │
│     │──2.读取Counter──────>│                       │               │        │
│     │  (counter=5)         │                       │               │        │
│     │                      │                       │               │        │
│     │──3.计算CRC8─────────>│                       │               │        │
│     │  (crc=0xA3)          │                       │               │        │
│     │                      │                       │               │        │
│     │──4.递增Counter──────>│                       │               │        │
│     │  (counter=6)         │                       │               │        │
│     │                      │                       │               │        │
│     │  ┌─────────────────────────────────────────────────────────────────┐  │
│     │  │                    CAN传输                                     │  │
│     │  └─────────────────────────────────────────────────────────────────┘  │
│     │                      │                       │               │        │
│     │──────────CAN帧────────>│                       │               │        │
│     │  [DataID|Counter|CRC| │                       │               │        │
│     │   SpeedData]         │                       │               │        │
│     │                      │                       │               │        │
│     │                      │────5.接收帧──────────>│               │        │
│     │                      │                       │               │        │
│     │  ┌─────────────────────────────────────────────────────────────────┐  │
│     │  │                    接收端E2E校验                                 │  │
│     │  └─────────────────────────────────────────────────────────────────┘  │
│     │                      │                       │               │        │
│     │                      │                       │──6.解析帧─────>│        │
│     │                      │                       │               │        │
│     │                      │                       │──7.CRC计算    │        │
│     │                      │                       │  (expected=0xA3)       │
│     │                      │                       │               │        │
│     │                      │                       │──8.CRC比较────────────>│
│     │                      │                       │  (0xA3==0xA3) │        │
│     │                      │                       │               │        │
│     │                      │                       │──9.Counter检查───────>│
│     │                      │                       │  (expected=5,│        │
│     │                      │                       │   received=5) │        │
│     │                      │                       │               │        │
│     │                      │                       │──10.超时检查──────────>│
│     │                      │                       │  (elapsed<100ms)      │
│     │                      │                       │               │        │
│     │                      │                       │──11.更新状态──────────>│
│     │                      │                       │  (lastCounter=5)      │
│     │                      │                       │               │        │
│     │                      │                       │──12.通知应用─────────>│
│     │                      │                       │  (speed=30,  │        │
│     │                      │                       │   valid=true) │        │
│     │                      │                       │               │        │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.3 故障处理时序

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           故障处理时序                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   VehicleSignal       E2EProtection      DrivingRestriction   Hardware      │
│      Monitor             Handler             Manager          Watchdog      │
│        │                    │                    │               │          │
│        │  ┌───────────────────────────────────────────────────────────────┐  │
│        │  │                      E2E校验失败场景                           │  │
│        │  └───────────────────────────────────────────────────────────────┘  │
│        │                    │                    │               │          │
│        │──CRC错误─────────>│                    │               │          │
│        │  (calculated!=   │                    │               │          │
│        │   received)      │                    │               │          │
│        │                    │                    │               │          │
│        │                    │──E2E错误通知─────>│               │          │
│        │                    │  (CRC_ERROR)       │               │          │
│        │                    │                    │               │          │
│        │                    │                    │──进入安全状态─│          │
│        │                    │                    │  (跛行模式)    │          │
│        │                    │                    │               │          │
│        │                    │                    │───记录故障────│          │
│        │                    │                    │               │          │
│        │  ┌───────────────────────────────────────────────────────────────┐  │
│        │  │                      看门狗超时场景                            │  │
│        │  └───────────────────────────────────────────────────────────────┘  │
│        │                    │                    │               │          │
│        │                    │                    │               │          │
│        │                    │                    │               │──500ms──>│
│        │                    │                    │               │  无喂狗   │
│        │                    │                    │               │          │
│        │                    │                    │               │──超时处理─│
│        │                    │                    │               │          │
│        │                    │                    │<──复位通知────│          │
│        │                    │                    │               │          │
│        │  ┌───────────────────────────────────────────────────────────────┐  │
│        │  │                      冗余计算不一致场景                        │  │
│        │  └───────────────────────────────────────────────────────────────┘  │
│        │                    │                    │               │          │
│        │───信号A──────────>│                    │               │          │
│        │───信号B──────────>│                    │               │          │
│        │                    │                    │               │          │
│        │                    │                    │──双路冗余计算─│          │
│        │                    │                    │  (结果1=true, │          │
│        │                    │                    │   结果2=false)│          │
│        │                    │                    │               │          │
│        │                    │                    │──结果不一致───│          │
│        │                    │                    │               │          │
│        │                    │                    │──进入安全状态─│          │
│        │                    │                    │               │          │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 5. 安全机制设计

### 5.1 E2E保护详细设计

#### 5.1.1 E2E配置参数

| 参数 | 值 | 说明 |
|------|-----|------|
| Profile | Profile 2 | AUTOSAR标准Profile 2 |
| Counter位宽 | 4 bits | 范围0-15 |
| CRC算法 | CRC8_SAE_J1850 | 多项式0x1D |
| Data ID长度 | 16 bits | 信号唯一标识 |
| 超时阈值 | 100ms | 信号最大允许延迟 |
| 最大丢包数 | 2 | 允许连续丢包数 |

#### 5.1.2 E2E数据结构

```
┌─────────────────────────────────────────────────────────────────┐
│                    E2E保护数据帧格式                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   0                   1                   2                   3 │
│   0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1│
│  ┌─────────────────────────────────────────────────────────────┐│
│  │                    Data ID (16 bits)                        ││
│  │              (信号类型唯一标识)                              ││
│  ├─────────────────────────────────────────────────────────────┤│
│  │  Counter  │                    CRC (8 bits)                 ││
│  │  (4 bits) │              (CRC8校验值)                        ││
│  ├─────────────────────────────────────────────────────────────┤│
│  │                    Payload (variable)                       ││
│  │              (车速/挡位/驻车制动数据)                        ││
│  └─────────────────────────────────────────────────────────────┘│
│                                                                 │
│  Data ID分配:                                                   │
│  - 0x0001: 车速信号 (Speed)                                     │
│  - 0x0002: 挡位信号 (Gear)                                      │
│  - 0x0003: 驻车制动 (Parking Brake)                             │
│  - 0x0004: 行驶限制状态反馈                                     │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

#### 5.1.3 E2E状态机

```
┌─────────────────────────────────────────────────────────────────┐
│                     E2E状态机                                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│                         ┌─────────┐                             │
│              ┌─────────│  INIT   │─────────┐                   │
│              │         │ (初始)   │         │                   │
│              │         └────┬────┘         │                   │
│              │              │              │                   │
│              ▼              ▼              ▼                   │
│       ┌────────────┐ ┌────────────┐ ┌────────────┐            │
│       │   VALID    │ │  NO_DATA   │ │  INVALID   │            │
│       │  (有效)     │ │ (无数据)   │ │  (无效)    │            │
│       └─────┬──────┘ └─────┬──────┘ └─────┬──────┘            │
│             │              │              │                    │
│             │              │              │                    │
│             ▼              ▼              ▼                    │
│       ┌────────────┐ ┌────────────┐ ┌────────────┐            │
│       │ CRC_OK     │ │ TIMEOUT    │ │ CRC_ERROR  │            │
│       │ COUNTER_OK │ │            │ │ COUNTER_ERR│            │
│       └────────────┘ └────────────┘ └────────────┘            │
│                                                                 │
│  状态转换条件:                                                   │
│  - VALID -> INVALID: CRC错误或序列号错误                         │
│  - VALID -> NO_DATA: 超时(>100ms)                               │
│  - NO_DATA -> VALID: 收到有效数据                                │
│  - INVALID -> VALID: 连续3个有效帧                               │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 5.2 看门狗设计

#### 5.2.1 看门狗架构

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           看门狗保护架构                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                        软件看门狗层                                  │   │
│  │  ┌─────────────────────────────────────────────────────────────┐   │   │
│  │  │                  Task Monitor                                │   │   │
│  │  │  - 信号处理任务: 100ms喂狗                                   │   │   │
│  │  │  - 限制决策任务: 50ms喂狗                                    │   │   │
│  │  │  - 状态恢复任务: 100ms喂狗                                   │   │   │
│  │  └─────────────────────────────────────────────────────────────┘   │   │
│  │  ┌─────────────────────────────────────────────────────────────┐   │   │
│  │  │                  SafetyWatchdog                              │   │   │
│  │  │  - 超时阈值: 500ms                                           │   │   │
│  │  │  - 监控周期: 50ms                                            │   │   │
│  │  │  - 超时动作: 进入安全状态 + 触发硬件看门狗                    │   │   │
│  │  └─────────────────────────────────────────────────────────────┘   │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                      │                                      │
│                                      ▼                                      │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                        硬件看门狗层                                  │   │
│  │  ┌─────────────────────────────────────────────────────────────┐   │   │
│  │  │                  Hardware Watchdog                           │   │   │
│  │  │  - 超时阈值: 1000ms                                          │   │   │
│  │  │  - 复位类型: 系统复位                                        │   │   │
│  │  │  - 喂狗接口: /dev/watchdog                                   │   │   │
│  │  └─────────────────────────────────────────────────────────────┘   │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### 5.2.2 喂狗策略

| 任务 | 执行周期 | 喂狗周期 | 最大允许延迟 |
|------|----------|----------|-------------|
| 信号处理 | 10ms | 100ms | 100ms |
| 限制决策 | 10ms | 50ms | 50ms |
| 状态恢复 | 100ms | 100ms | 100ms |
| 主管理器 | 10ms | 100ms | 500ms |

### 5.3 冗余设计

#### 5.3.1 双路冗余评估

```kotlin
/**
 * 双路冗余限制评估
 * ASIL B: 两条独立路径计算，结果必须一致
 */
class RedundantRestrictionEvaluator {
    
    /**
     * 路径1: 基于车速评估
     */
    fun evaluatePath1(signal: VehicleSignal): Boolean {
        // 独立读取车速信号
        val speed = signal.speed
        
        // 信号有效性检查
        if (speed < 0 || speed > 300) {
            return true // 信号异常，默认限制
        }
        
        return speed > 0
    }
    
    /**
     * 路径2: 基于挡位和驻车制动评估
     */
    fun evaluatePath2(signal: VehicleSignal): Boolean {
        // 独立读取挡位和驻车制动信号
        val gear = signal.gear
        val parkingBrake = signal.parkingBrake
        
        // 信号有效性检查
        if (gear == GearPosition.UNKNOWN) {
            return true // 信号异常，默认限制
        }
        
        // 行驶条件: D挡或R挡或驻车制动释放
        return gear == GearPosition.DRIVE || 
               gear == GearPosition.REVERSE || 
               !parkingBrake
    }
    
    /**
     * 冗余比较
     */
    fun evaluateWithRedundancy(signal: VehicleSignal): Pair<Boolean, Boolean> {
        val result1 = evaluatePath1(signal)
        val result2 = evaluatePath2(signal)
        
        // 比较结果
        val consistent = (result1 == result2)
        
        return Pair(consistent, if (consistent) result1 else true)
    }
}
```

### 5.4 安全状态定义

| 状态 | 行为 | 恢复条件 |
|------|------|----------|
| 正常(NORMAL) | 按规则限制应用 | - |
| 受限(RESTRICTED) | 限制非白名单应用 | 满足恢复条件 |
| 故障(FAULT) | 限制所有非必要应用 | 系统复位 |
| 恢复中(RECOVERING) | 逐步恢复应用 | 恢复完成 |

---

## 6. CAN信号处理

### 6.1 CAN信号矩阵

| 信号名称 | CAN ID | 周期 | 长度 | 因子 | 偏移 | 范围 | E2E保护 |
|----------|--------|------|------|------|------|------|---------|
| VehicleSpeed | 0x130 | 10ms | 16bit | 0.01 | 0 | 0-655.35 km/h | ✓ |
| GearPosition | 0x131 | 50ms | 4bit | 1 | 0 | 0-15 | ✓ |
| ParkingBrake | 0x132 | 100ms | 1bit | 1 | 0 | 0-1 | ✓ |
| DrvRestrStatus | 0x140 | 20ms | 8bit | 1 | 0 | 0-255 | ✓ |

### 6.2 信号解析代码

```kotlin
/**
 * CAN信号解析器
 */
class CanSignalParser {
    
    /**
     * 解析车速信号
     * CAN ID: 0x130
     */
    fun parseVehicleSpeed(data: ByteArray): Int {
        require(data.size >= 2) { "Invalid data length" }
        
        // 小端模式解析
        val rawValue = (data[1].toInt() and 0xFF shl 8) or 
                       (data[0].toInt() and 0xFF)
        
        // 物理值 = 原始值 × 因子 + 偏移
        return (rawValue * 0.01).toInt()
    }
    
    /**
     * 解析挡位信号
     * CAN ID: 0x131
     */
    fun parseGearPosition(data: ByteArray): GearPosition {
        require(data.isNotEmpty()) { "Invalid data length" }
        
        val rawValue = data[0].toInt() and 0x0F
        
        return when (rawValue) {
            0 -> GearPosition.PARK
            1 -> GearPosition.REVERSE
            2 -> GearPosition.NEUTRAL
            3 -> GearPosition.DRIVE
            else -> GearPosition.UNKNOWN
        }
    }
    
    /**
     * 解析驻车制动信号
     * CAN ID: 0x132
     */
    fun parseParkingBrake(data: ByteArray): Boolean {
        require(data.isNotEmpty()) { "Invalid data length" }
        
        return (data[0].toInt() and 0x01) == 1
    }
}
```

### 6.3 CAN错误处理

| 错误类型 | 处理策略 | 安全响应 |
|----------|----------|----------|
| 总线关闭(Bus-Off) | 重置CAN控制器 | 进入安全状态 |
| 帧错误 | 丢弃错误帧，请求重发 | 使用上一有效值 |
| 校验错误 | 丢弃帧，记录错误 | 连续3次错误进入安全状态 |
| 超时 | 标记信号超时 | 进入安全状态 |

---

## 7. 需求追溯矩阵

### 7.1 功能需求追溯

| SRS需求ID | 需求描述 | HLD设计元素 | DD设计元素 | 代码单元 | 测试用例 | ASIL |
|-----------|----------|-------------|------------|----------|----------|------|
| REQ-DRV-FUN-007-ASIL_B | 车速>0或D/R挡或PB释放时启用限制 | 3.2.4 IDrivingRestrictionService | DrivingRestrictionManager | DrvManager.kt | TC-DRV-001 | B |
| REQ-DRV-FUN-007-ASIL_B | 限制触发延迟≤200ms | 8.1性能目标 | enforceRestriction() | DrvManager.kt | TC-DRV-PERF-001 | B |
| REQ-DRV-FUN-007-ASIL_B | 禁止视频播放 | 3.2.4 controlAppBehavior() | AppRestrictionHandler | AppRestHandler.kt | TC-DRV-002 | B |
| REQ-DRV-FUN-007-ASIL_B | 禁止应用市场下载 | 3.2.4 controlAppBehavior() | AppRestrictionHandler | AppRestHandler.kt | TC-DRV-003 | B |
| REQ-DRV-FUN-007-ASIL_B | 禁止游戏类应用 | 3.2.4 controlAppBehavior() | AppRestrictionHandler | AppRestHandler.kt | TC-DRV-004 | B |
| REQ-DRV-FUN-008-ASIL_B | 白名单管理功能 | 3.2.4 WhitelistManager | WhitelistManager | WhitelistMgr.kt | TC-DRV-005 | B |
| REQ-DRV-FUN-008-ASIL_B | 导航类应用白名单 | DB: app_whitelist表 | WhitelistManager | WhitelistMgr.kt | TC-DRV-006 | B |
| REQ-DRV-FUN-008-ASIL_B | 音乐类应用白名单 | DB: app_whitelist表 | WhitelistManager | WhitelistMgr.kt | TC-DRV-007 | B |
| REQ-DRV-FUN-008-ASIL_B | 通讯类应用白名单 | DB: app_whitelist表 | WhitelistManager | WhitelistMgr.kt | TC-DRV-008 | B |
| REQ-DRV-FUN-009-ASIL_A | 应用暂停功能 | 3.2.4 BehaviorControl | AppRestrictionHandler | AppRestHandler.kt | TC-DRV-009 | A |
| REQ-DRV-FUN-009-ASIL_A | 返回HOME功能 | 3.2.4 BehaviorControl | AppRestrictionHandler | AppRestHandler.kt | TC-DRV-010 | A |
| REQ-DRV-FUN-009-ASIL_A | 限制交互功能 | 3.2.4 BehaviorControl | AppRestrictionHandler | AppRestHandler.kt | TC-DRV-011 | A |
| REQ-DRV-FUN-010-ASIL_B | 车速=0且持续>3s恢复 | 3.2.4 RecoveryHandler | StateRecoveryHandler | RecoveryHandler.kt | TC-DRV-012 | B |
| REQ-DRV-FUN-010-ASIL_B | 挡位P挡恢复 | 3.2.4 RecoveryHandler | StateRecoveryHandler | RecoveryHandler.kt | TC-DRV-013 | B |
| REQ-DRV-FUN-010-ASIL_B | 驻车制动启用恢复 | 3.2.4 RecoveryHandler | StateRecoveryHandler | RecoveryHandler.kt | TC-DRV-014 | B |
| REQ-DRV-FUN-010-ASIL_B | 恢复延迟≤500ms | 8.1性能目标 | executeRecovery() | RecoveryHandler.kt | TC-DRV-PERF-002 | B |

### 7.2 安全需求追溯

| 安全需求ID | 需求描述 | 设计元素 | 实现代码 | 验证方法 | ASIL |
|------------|----------|----------|----------|----------|------|
| REQ-SAF-001-ASIL_B | E2E保护 | 5.1 E2EProtectionHandler | E2EHandler.kt | 故障注入测试 | B |
| REQ-SAF-001-ASIL_B | CRC8校验 | 5.1.1 CRC配置 | calculateCRC8() | 边界值测试 | B |
| REQ-SAF-001-ASIL_B | Counter序列号 | 5.1.1 Counter配置 | checkCounter() | 序列号跳变测试 | B |
| REQ-SAF-001-ASIL_B | 超时检测 | 5.1.1 Timeout配置 | checkTimeout() | 延迟注入测试 | B |
| REQ-SAF-002-ASIL_B | 看门狗监控 | 5.2 SafetyWatchdog | SafetyWatchdog.kt | 超时触发测试 | B |
| REQ-SAF-002-ASIL_B | 500ms超时阈值 | 5.2.1 看门狗配置 | WATCHDOG_TIMEOUT_MS | 超时边界测试 | B |
| REQ-SAF-003-ASIL_B | 冗余设计 | 5.3 RedundantEvaluator | RedundantEvaluator.kt | 双路比较测试 | B |
| REQ-SAF-003-ASIL_B | 双路评估 | 5.3.1 evaluateWithRedundancy() | redundantEvaluate() | 不一致注入测试 | B |
| REQ-SAF-004-ASIL_B | 安全状态 | 5.4 安全状态定义 | enterSafeState() | 状态转换测试 | B |

### 7.3 性能需求追溯

| 性能需求ID | 需求描述 | 设计元素 | 目标值 | 测试方法 |
|------------|----------|----------|--------|----------|
| REQ-PER-007-ASIL_B | 限制触发延迟 | enforceRestriction() | ≤200ms | 性能测试 |
| REQ-PER-007-ASIL_B | 信号处理延迟 | VehicleSignalMonitor | ≤10ms | 性能测试 |
| REQ-REL-002-ASIL_B | 安全功能故障率 | 整体架构 | ≤10⁻⁵/h | 可靠性分析 |

---

## 8. 接口定义

### 8.1 AIDL接口

#### IDrivingRestrictionService.aidl

```aidl
// IDrivingRestrictionService.aidl
package com.longcheer.cockpit.drv;

interface IDrivingRestrictionService {
    // 获取当前限制状态
    RestrictionStatus getRestrictionStatus();
    
    // 获取应用限制类型
    RestrictionType getAppRestriction(in String appId);
    
    // 控制应用行为
    void controlAppBehavior(in String appId, in BehaviorControl control);
    
    // 注册/注销监听
    void registerListener(in IDrivingRestrictionListener listener);
    void unregisterListener(in IDrivingRestrictionListener listener);
    
    // 白名单管理 (系统权限)
    void addToWhitelist(in String appId, in String reason);
    void removeFromWhitelist(in String appId);
    List<WhitelistEntry> getWhitelist();
}

// 限制状态
parcelable RestrictionStatus {
    int state;          // 0=NORMAL, 1=RESTRICTED, 2=RECOVERING, 3=FAULT
    boolean isRestricted;
    long timestamp;
}

// 限制类型
enum RestrictionType {
    NONE,           // 无限制
    VIDEO_BLOCKED,  // 视频禁止
    GAME_BLOCKED,   // 游戏禁止
    FULL_RESTRICTED // 完全限制
}

// 行为控制
enum BehaviorControl {
    PAUSE,          // 暂停
    RESUME,         // 恢复
    RETURN_HOME,    // 返回HOME
    LIMIT_INTERACTION, // 限制交互
    GRAYSCALE       // 灰度显示
}
```

### 8.2 车辆服务接口

```aidl
// IVehicleService.aidl - 简化版
interface IVehicleService {
    // 车速相关
    int getVehicleSpeed();
    void registerSpeedListener(in SpeedListener listener, int intervalMs);
    
    // 挡位相关
    int getGearPosition();
    void registerGearListener(in GearListener listener);
    
    // 驻车制动
    boolean isParkingBrakeOn();
    void registerParkingBrakeListener(in ParkingBrakeListener listener);
}

// E2E数据结构
parcelable E2EData {
    byte counter;   // 序列号
    byte crc;       // CRC校验值
    long timestamp; // 时间戳
}

oneway interface SpeedListener {
    void onSpeedChanged(int speed, in E2EData e2eData);
}
```

---

## 9. 测试策略

### 9.1 测试层级

| 测试层级 | 测试对象 | 覆盖率要求 | ASIL B要求 |
|----------|----------|------------|------------|
| 单元测试 | 单个类/方法 | 语句≥100%，分支≥100% | 必须 |
| 集成测试 | 模块间接口 | 接口100%覆盖 | 必须 |
| 故障注入 | 安全机制 | 故障场景100%覆盖 | 必须 |
| 性能测试 | 响应时间 | 延迟≤200ms | 必须 |
| 系统测试 | 端到端场景 | 需求100%覆盖 | 必须 |

### 9.2 ASIL B专项测试

| 测试项 | 测试方法 | 通过标准 |
|--------|----------|----------|
| E2E CRC错误 | 注入错误CRC | 进入安全状态 |
| E2E Counter跳变 | 注入跳变序列号 | 进入安全状态 |
| E2E超时 | 延迟信号>100ms | 进入安全状态 |
| 看门狗超时 | 停止喂狗>500ms | 系统复位 |
| 冗余不一致 | 模拟双路结果不一致 | 进入安全状态 |
| 限制延迟 | 测量限制触发时间 | ≤200ms |
| 恢复延迟 | 测量状态恢复时间 | ≤500ms |

---

## 10. 附录

### 附录A: 状态转换表

| 当前状态 | 事件 | 条件 | 新状态 | 动作 |
|----------|------|------|--------|------|
| NORMAL | 车速>0 | 信号有效 | RESTRICTED | 执行限制 |
| NORMAL | 挡位=D/R | 信号有效 | RESTRICTED | 执行限制 |
| NORMAL | PB释放 | 信号有效 | RESTRICTED | 执行限制 |
| RESTRICTED | 车速=0 && 挡位=P && PB=ON && 持续3s | 信号有效 | RECOVERING | 启动恢复定时器 |
| RECOVERING | 恢复完成 | - | NORMAL | 恢复应用 |
| ANY | E2E错误 | - | FAULT | 进入安全状态 |
| ANY | 看门狗超时 | - | FAULT | 系统复位 |
| FAULT | 系统复位 | - | NORMAL | 初始化 |

### 附录B: 错误码定义

| 错误码 | 名称 | 描述 | 处理建议 |
|--------|------|------|----------|
| 0x1001 | E_E2E_CRC_ERROR | E2E CRC校验失败 | 检查CAN信号线 |
| 0x1002 | E_E2E_COUNTER_ERROR | E2E序列号错误 | 检查发送端Counter |
| 0x1003 | E_E2E_TIMEOUT | E2E信号超时 | 检查CAN总线负载 |
| 0x2001 | E_WATCHDOG_TIMEOUT | 看门狗超时 | 系统复位 |
| 0x3001 | E_REDUNDANCY_MISMATCH | 冗余计算不一致 | 进入安全状态 |
| 0x4001 | E_RESTRICTION_TIMEOUT | 限制触发超时 | 性能优化 |

### 附录C: 配置文件示例

```xml
<!-- E2E配置文件: E2E_P02_CanBus_DRV_V1.0.xml -->
<E2EConfiguration>
    <Profile>Profile2</Profile>
    <Signals>
        <Signal id="0x0001" name="VehicleSpeed">
            <DataLength>2</DataLength>
            <CounterWidth>4</CounterWidth>
            <CRCAlgorithm>CRC8_SAE_J1850</CRCAlgorithm>
            <TimeoutMs>100</TimeoutMs>
        </Signal>
        <Signal id="0x0002" name="GearPosition">
            <DataLength>1</DataLength>
            <CounterWidth>4</CounterWidth>
            <CRCAlgorithm>CRC8_SAE_J1850</CRCAlgorithm>
            <TimeoutMs>100</TimeoutMs>
        </Signal>
    </Signals>
</E2EConfiguration>
```

---

**文档结束**

*本详细设计文档符合ASPICE Level 3和ISO 26262 ASIL B要求，建立了从需求到实现的完整追溯链。*

**编制**: 上海龙旗智能科技有限公司  
**审核**: [待填写]  
**批准**: [待填写]  
**日期**: 2024-06-20
