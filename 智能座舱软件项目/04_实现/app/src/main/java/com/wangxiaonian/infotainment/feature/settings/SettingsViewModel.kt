package com.wangxiaonian.infotainment.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 设置页面 ViewModel
 *
 * @author 王小年联盟
 * @version 1.0
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferenceRepository: PreferenceRepository
) : ViewModel() {

    private val _settingsState = MutableStateFlow(SettingsState())
    val settingsState: StateFlow<SettingsState> = _settingsState

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _settingsState.value = SettingsState(
                isDarkTheme = preferenceRepository.isDarkTheme(),
                isAutoTheme = preferenceRepository.isAutoTheme(),
                ttsEnabled = preferenceRepository.isTtsEnabled(),
                notificationSoundEnabled = preferenceRepository.isNotificationSoundEnabled(),
                drivingRestrictionEnabled = preferenceRepository.isDrivingRestrictionEnabled()
            )
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            preferenceRepository.setDarkTheme(enabled)
            _settingsState.value = _settingsState.value.copy(isDarkTheme = enabled)
        }
    }

    fun setAutoTheme(enabled: Boolean) {
        viewModelScope.launch {
            preferenceRepository.setAutoTheme(enabled)
            _settingsState.value = _settingsState.value.copy(isAutoTheme = enabled)
        }
    }

    fun setTtsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferenceRepository.setTtsEnabled(enabled)
            _settingsState.value = _settingsState.value.copy(ttsEnabled = enabled)
        }
    }

    fun setNotificationSound(enabled: Boolean) {
        viewModelScope.launch {
            preferenceRepository.setNotificationSoundEnabled(enabled)
            _settingsState.value = _settingsState.value.copy(notificationSoundEnabled = enabled)
        }
    }

    fun setDrivingRestriction(enabled: Boolean) {
        viewModelScope.launch {
            preferenceRepository.setDrivingRestrictionEnabled(enabled)
            _settingsState.value = _settingsState.value.copy(drivingRestrictionEnabled = enabled)
        }
    }
}

/**
 * 设置状态
 */
data class SettingsState(
    val isDarkTheme: Boolean = true,
    val isAutoTheme: Boolean = true,
    val ttsEnabled: Boolean = true,
    val notificationSoundEnabled: Boolean = true,
    val drivingRestrictionEnabled: Boolean = true
)

/**
 * 偏好设置 Repository
 */
class PreferenceRepository @Inject constructor() {
    // TODO: 使用 DataStore 实现持久化
    
    suspend fun isDarkTheme(): Boolean = true
    suspend fun setDarkTheme(enabled: Boolean) {}
    
    suspend fun isAutoTheme(): Boolean = true
    suspend fun setAutoTheme(enabled: Boolean) {}
    
    suspend fun isTtsEnabled(): Boolean = true
    suspend fun setTtsEnabled(enabled: Boolean) {}
    
    suspend fun isNotificationSoundEnabled(): Boolean = true
    suspend fun setNotificationSoundEnabled(enabled: Boolean) {}
    
    suspend fun isDrivingRestrictionEnabled(): Boolean = true
    suspend fun setDrivingRestrictionEnabled(enabled: Boolean) {}
}
