package com.longcheer.cockpit.fwk.launcher

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.longcheer.cockpit.fwk.model.AppCategory
import com.longcheer.cockpit.fwk.model.AppInfo
import com.longcheer.cockpit.fwk.repository.IAppRepository
import com.longcheer.cockpit.fwk.repository.PreferenceRepository
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
 * Launcher ViewModel
 * 管理Launcher界面状态和业务逻辑
 * 需求追溯: REQ-FWK-FUN-014
 */
@HiltViewModel
class LauncherViewModel @Inject constructor(
    private val appRepository: IAppRepository,
    private val preferenceRepository: PreferenceRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // ========== UI状态 ==========
    private val _uiState = MutableStateFlow(LauncherUiState())
    val uiState: StateFlow<LauncherUiState> = _uiState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LauncherUiState()
        )

    // ========== 一次性事件 ==========
    private val _events = MutableSharedFlow<LauncherEvent>()
    val events: SharedFlow<LauncherEvent> = _events.asSharedFlow()

    // ========== 应用列表 ==========
    private val _appList = MutableStateFlow<List<AppInfo>>(emptyList())
    val appList: StateFlow<List<AppInfo>> = _appList.asStateFlow()

    // ========== 筛选后的应用列表 ==========
    private val _filteredApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val filteredApps: StateFlow<List<AppInfo>> = _filteredApps.asStateFlow()

    // ========== 当前选中的应用分类 ==========
    private val _selectedCategory = MutableStateFlow<AppCategory?>(null)
    val selectedCategory: StateFlow<AppCategory?> = _selectedCategory.asStateFlow()

    // ========== 快捷入口 ==========
    private val _shortcuts = MutableStateFlow<List<ShortcutInfo>>(emptyList())
    val shortcuts: StateFlow<List<ShortcutInfo>> = _shortcuts.asStateFlow()

    // ========== 应用分类列表 ==========
    private val _categories = MutableStateFlow<List<AppCategoryInfo>>(emptyList())
    val categories: StateFlow<List<AppCategoryInfo>> = _categories.asStateFlow()

    init {
        loadApps()
        loadShortcuts()
        loadCategories()
    }

    /**
     * 处理用户意图
     */
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

    /**
     * 加载应用列表
     */
    private fun loadApps() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val apps = appRepository.getAllApps()
                _appList.value = apps
                _filteredApps.value = apps
                _uiState.update { it.copy(isLoading = false, isEmpty = apps.isEmpty()) }
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

    /**
     * 加载快捷入口
     */
    private fun loadShortcuts() {
        viewModelScope.launch {
            try {
                val frequentApps = appRepository.getFrequentlyUsedApps(4)
                _shortcuts.value = frequentApps.map { app ->
                    ShortcutInfo(
                        id = app.appId,
                        name = app.appName,
                        iconPath = app.iconPath,
                        action = { launchApp(app.appId) }
                    )
                }
            } catch (e: Exception) {
                // 静默处理，快捷入口不是关键功能
            }
        }
    }

    /**
     * 加载应用分类
     */
    private fun loadCategories() {
        viewModelScope.launch {
            val allApps = appRepository.getAllApps()
            val categories = AppCategory.entries.map { category ->
                AppCategoryInfo(
                    category = category,
                    appCount = allApps.count { it.categoryId == category.ordinal },
                    isSelected = false
                )
            }.filter { it.appCount > 0 }
            _categories.value = categories
        }
    }

    /**
     * 启动应用
     */
    private fun launchApp(appId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = appRepository.launchApp(appId)

            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        lastLaunchedApp = appId
                    )
                }
                _events.emit(LauncherEvent.NavigateToApp(appId))
            }.onFailure { error ->
                val errorMessage = when (error) {
                    is com.longcheer.cockpit.cockpit.fwk.model.AppRestrictedException -> "行驶中无法使用该应用"
                    else -> "启动失败: ${error.message}"
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = errorMessage
                    )
                }
                _events.emit(LauncherEvent.ShowToast(errorMessage))
            }
        }
    }

    /**
     * 按分类筛选应用
     */
    private fun filterByCategory(category: AppCategory?) {
        _selectedCategory.value = category
        viewModelScope.launch {
            val apps = if (category == null) {
                _appList.value
            } else {
                _appList.value.filter { it.categoryId == category.ordinal }
            }
            _filteredApps.value = apps

            // 更新分类选中状态
            _categories.update { list ->
                list.map {
                    it.copy(isSelected = it.category == category)
                }
            }
        }
    }

    /**
     * 搜索应用
     */
    private fun searchApps(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) {
                _filteredApps.value = _appList.value
            } else {
                val results = appRepository.searchApps(query)
                _filteredApps.value = results
            }
        }
    }

    /**
     * 重新排序应用
     */
    private fun reorderApps(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val currentList = _appList.value.toMutableList()
            if (fromIndex in currentList.indices && toIndex in currentList.indices) {
                val movedApp = currentList.removeAt(fromIndex)
                currentList.add(toIndex, movedApp)

                _appList.value = currentList
                _filteredApps.value = currentList
                appRepository.updateAppOrder(currentList)
            }
        }
    }

    /**
     * 进入编辑模式
     */
    private fun enterEditMode() {
        _uiState.update { it.copy(isEditMode = true) }
    }

    /**
     * 退出编辑模式
     */
    private fun exitEditMode() {
        _uiState.update { it.copy(isEditMode = false) }
    }

    /**
     * 清除错误信息
     */
    private fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

/**
 * Launcher UI状态
 */
data class LauncherUiState(
    val isLoading: Boolean = false,
    val isEmpty: Boolean = false,
    val isEditMode: Boolean = false,
    val lastLaunchedApp: String? = null,
    val errorMessage: String? = null
)

/**
 * 一次性事件
 */
sealed class LauncherEvent {
    data class ShowToast(val message: String) : LauncherEvent()
    data class NavigateToApp(val appId: String) : LauncherEvent()
    data class ShowPermissionDialog(val permission: String) : LauncherEvent()
    object RequestEditMode : LauncherEvent()
}

/**
 * 用户意图
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

/**
 * 快捷入口信息
 */
data class ShortcutInfo(
    val id: String,
    val name: String,
    val iconPath: String?,
    val action: () -> Unit
)

/**
 * 应用分类信息
 */
data class AppCategoryInfo(
    val category: AppCategory,
    val appCount: Int = 0,
    val isSelected: Boolean = false
)
