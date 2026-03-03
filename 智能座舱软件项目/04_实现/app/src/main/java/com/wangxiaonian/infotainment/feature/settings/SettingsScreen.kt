package com.wangxiaonian.infotainment.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wangxiaonian.infotainment.presentation.component.CarCard

/**
 * 设置页面
 *
 * @author 王小年联盟
 * @version 1.0
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.settingsState.collectAsState()

    Scaffold(
        topBar = {
            SettingsTopBar(onBack = onBack)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 显示设置
            SettingsSection(title = "显示") {
                SettingsSwitchItem(
                    icon = Icons.Default.DarkMode,
                    title = "深色模式",
                    subtitle = "使用深色主题保护夜间视力",
                    checked = state.isDarkTheme,
                    onCheckedChange = { viewModel.setDarkTheme(it) },
                    enabled = !state.isAutoTheme
                )

                SettingsSwitchItem(
                    icon = Icons.Default.DarkMode,
                    title = "自动切换",
                    subtitle = "根据环境光自动切换主题",
                    checked = state.isAutoTheme,
                    onCheckedChange = { viewModel.setAutoTheme(it) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 声音设置
            SettingsSection(title = "声音") {
                SettingsSwitchItem(
                    icon = Icons.Default.VolumeUp,
                    title = "语音播报",
                    subtitle = "使用语音播报通知内容",
                    checked = state.ttsEnabled,
                    onCheckedChange = { viewModel.setTtsEnabled(it) }
                )

                SettingsSwitchItem(
                    icon = Icons.Default.Notifications,
                    title = "通知提示音",
                    subtitle = "收到通知时播放提示音",
                    checked = state.notificationSoundEnabled,
                    onCheckedChange = { viewModel.setNotificationSound(it) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 安全设置
            SettingsSection(title = "安全") {
                SettingsSwitchItem(
                    icon = Icons.Default.Notifications,
                    title = "驾驶限制",
                    subtitle = "行驶中限制复杂操作",
                    checked = state.drivingRestrictionEnabled,
                    onCheckedChange = { viewModel.setDrivingRestriction(it) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 关于
            CarCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "智能座舱系统",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "版本 1.0.0",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = "设置",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(bottom = 12.dp)
    )

    CarCard {
        Column {
            content()
        }
    }
}

@Composable
private fun SettingsSwitchItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = if (enabled) MaterialTheme.colorScheme.primary 
                   else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(20.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface 
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}
