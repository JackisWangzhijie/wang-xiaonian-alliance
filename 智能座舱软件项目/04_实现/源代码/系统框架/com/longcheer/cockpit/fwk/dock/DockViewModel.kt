package com.longcheer.cockpit.fwk.dock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.longcheer.cockpit.fwk.model.DockConfig
import com.longcheer.cockpit.fwk.model.DockItem
import com.longcheer.cockpit.fwk.model.DockSlotType
import com.longcheer.cockpit.fwk.repository.DockRepository
import com.longcheer.cockpit.fwk.repository.IAppRepository
import com.longcheer.cockpit.fwk.repository.IDockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Dock ViewModel
 * 管理Dock栏状态和业务逻辑
 * 需求追溯: REQ-FWK-FUN-016
 */
@HiltViewModel
class DockViewModel @Inject constructor(
    private val dockRepository: IDockRepository,
    private val appRepository: IAppRepository
) : ViewModel() {

    // ========== UI状态 ==========
    private val _uiState = MutableStateFlow(DockUiState())
    val uiState: StateFlow<DockUiState> = _uiState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DockUiState()
        )

    // ========== 一次性事件 ==========
    private val _events = MutableSharedFlow<DockEvent>()
    val events: SharedFlow<DockEvent> = _events.asSharedFlow()

    // ========== 固定应用列表 ==========
    private val _fixedApps = MutableStateFlow<List<DockItem.FixedAppItem>>(emptyList())
    val fixedApps: StateFlow<List<DockItem.FixedAppItem>> = _fixedApps.asStateFlow()

    // ========== 最近应用列表 ==========
    private val _recentApps = MutableStateFlow<List<DockItem.RecentAppItem>>(emptyList())
    val recentApps: StateFlow<List<DockItem.RecentAppItem>> = _recentApps.asStateFlow()

    init {
        loadDockConfig()
        loadRecentApps()
    }

    /**
     * 处理用户意图
     */
    fun onIntent(intent: DockIntent) {
        when (intent) {
            is DockIntent.OnHomeClick -> onHomeClick()
            is DockIntent.OnRecentAppsClick -> onRecentAppsClick()
            is DockIntent.OnAppClick -> onAppClick(intent.appId)
            is DockIntent.OnFixedAppClick -> onFixedAppClick(intent.appId)
            is DockIntent.UpdateFixedApps -> updateFixedApps(intent.apps)
            is DockIntent.ToggleExpand -> toggleExpand()
        }
    }

    /**
     * 加载Dock配置
     */
    private fun loadDockConfig() {
        viewModelScope.launch {
            try {
                val config = dockRepository.getDockConfig()
                val fixedItems = config
                    .filter { it.slotType == DockSlotType.FIXED_APP }
                    .mapNotNull { config ->
                        config.appId?.let { appId ->
                            val app = appRepository.getAppById(appId)
                            app?.let {
                                DockItem.FixedAppItem(
                                    appId = it.appId,
                                    name = it.appName,
                                    iconPath = it.iconPath,
                                    isEnabled = it.isEnabled
                                )
                            }
                        }
                    }
                _fixedApps.value = fixedItems
            } catch (e: Exception) {
                // 使用默认配置
                _fixedApps.value = getDefaultFixedApps()
            }
        }
    }

    /**
     * 加载最近应用
     */
    private fun loadRecentApps() {
        viewModelScope.launch {
            try {
                val recent = dockRepository.getRecentApps(4)
                _recentApps.value = recent.map {
                    DockItem.RecentAppItem(
                        appId = it.appId,
                        name = it.appName,
                        iconPath = it.iconPath,
                        isRunning = it.isRunning
                    )
                }
            } catch (e: Exception) {
                _recentApps.value = emptyList()
            }
        }
    }

    /**
     * Home键点击
     */
    private fun onHomeClick() {
        viewModelScope.launch {
            dockRepository.handleHomeAction()
            _events.emit(DockEvent.GoHome)
        }
    }

    /**
     * 最近应用按钮点击
     */
    private fun onRecentAppsClick() {
        viewModelScope.launch {
            dockRepository.handleRecentAppsAction()
            _events.emit(DockEvent.ShowRecentApps)
        }
    }

    /**
     * 应用点击（通用）
     */
    private fun onAppClick(appId: String) {
        viewModelScope.launch {
            val app = appRepository.getAppById(appId)
            app?.let {
                // 添加到最近应用
                (dockRepository as? DockRepository)?.addToRecentApps(
                    appId = it.appId,
                    appName = it.appName,
                    iconPath = it.iconPath
                )

                // 启动应用
                val result = appRepository.launchApp(appId)
                result.onSuccess {
                    _events.emit(DockEvent.LaunchApp(appId))
                    // 刷新最近应用列表
                    loadRecentApps()
                }.onFailure { error ->
                    _events.emit(DockEvent.ShowError(error.message ?: "启动失败"))
                }
            }
        }
    }

    /**
     * 固定应用点击
     */
    private fun onFixedAppClick(appId: String) {
        onAppClick(appId)
    }

    /**
     * 更新固定应用列表
     */
    private fun updateFixedApps(apps: List<DockItem.FixedAppItem>) {
        viewModelScope.launch {
            _fixedApps.value = apps
            // 保存到数据库
            val config = apps.mapIndexed { index, app ->
                DockConfig(
                    slotIndex = index,
                    slotType = DockSlotType.FIXED_APP,
                    appId = app.appId,
                    actionType = com.longcheer.cockpit.fwk.model.DockActionType.LAUNCH_APP
                )
            }
            dockRepository.updateDockConfig(config)
        }
    }

    /**
     * 切换展开/收起状态
     */
    private fun toggleExpand() {
        _uiState.update { it.copy(isExpanded = !it.isExpanded) }
    }

    /**
     * 获取默认固定应用
     */
    private suspend fun getDefaultFixedApps(): List<DockItem.FixedAppItem> {
        val defaultAppIds = listOf(
            "com.autonavi.amapauto",    // 高德地图
            "com.tencent.qqmusiccar",   // QQ音乐
            "com.android.dialer",       // 电话
            "com.android.settings"      // 设置
        )

        return defaultAppIds.mapNotNull { appId ->
            appRepository.getAppById(appId)?.let { app ->
                DockItem.FixedAppItem(
                    appId = app.appId,
                    name = app.appName,
                    iconPath = app.iconPath,
                    isEnabled = app.isEnabled
                )
            }
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
 * Dock一次性事件
 */
sealed class DockEvent {
    object GoHome : DockEvent()
    object ShowRecentApps : DockEvent()
    data class LaunchApp(val appId: String) : DockEvent()
    data class ShowError(val message: String) : DockEvent()
}

/**
 * Dock用户意图
 */
sealed class DockIntent {
    object OnHomeClick : DockIntent()
    object OnRecentAppsClick : DockIntent()
    data class OnAppClick(val appId: String) : DockIntent()
    data class OnFixedAppClick(val appId: String) : DockIntent()
    data class UpdateFixedApps(val apps: List<DockItem.FixedAppItem>) : DockIntent()
    object ToggleExpand : DockIntent()
}
