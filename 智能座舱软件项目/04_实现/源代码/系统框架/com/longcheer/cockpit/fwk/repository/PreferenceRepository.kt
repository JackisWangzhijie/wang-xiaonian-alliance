package com.longcheer.cockpit.fwk.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户偏好数据存储
 * 需求追溯: REQ-FWK-FUN-015
 */
@Singleton
class PreferenceRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fwk_preferences")

    // ========== 主题设置 ==========
    val themeFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.THEME] ?: THEME_DARK
        }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = theme
        }
    }

    // ========== 应用网格设置 ==========
    val gridColumnsFlow: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.GRID_COLUMNS] ?: DEFAULT_GRID_COLUMNS
        }

    suspend fun setGridColumns(columns: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GRID_COLUMNS] = columns
        }
    }

    // ========== Launcher布局配置 ==========
    val launcherLayoutFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.LAUNCHER_LAYOUT] ?: LAYOUT_GRID
        }

    suspend fun setLauncherLayout(layout: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAUNCHER_LAYOUT] = layout
        }
    }

    // ========== 控件主题设置 ==========
    val widgetThemeFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.WIDGET_THEME] ?: WIDGET_THEME_DEFAULT
        }

    suspend fun setWidgetTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WIDGET_THEME] = theme
        }
    }

    // ========== Dock设置 ==========
    val dockVisibleFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.DOCK_VISIBLE]?.toBoolean() ?: true
        }

    suspend fun setDockVisible(visible: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DOCK_VISIBLE] = visible.toString()
        }
    }

    private object PreferencesKeys {
        val THEME = stringPreferencesKey("theme")
        val GRID_COLUMNS = intPreferencesKey("grid_columns")
        val LAUNCHER_LAYOUT = stringPreferencesKey("launcher_layout")
        val WIDGET_THEME = stringPreferencesKey("widget_theme")
        val DOCK_VISIBLE = stringPreferencesKey("dock_visible")
    }

    companion object {
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val THEME_AUTO = "auto"

        const val LAYOUT_GRID = "grid"
        const val LAYOUT_LIST = "list"

        const val WIDGET_THEME_DEFAULT = "default"
        const val WIDGET_THEME_SPORT = "sport"
        const val WIDGET_THEME_ELEGANT = "elegant"

        const val DEFAULT_GRID_COLUMNS = 4
    }
}
