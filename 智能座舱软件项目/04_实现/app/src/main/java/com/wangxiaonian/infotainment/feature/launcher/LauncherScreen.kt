package com.wangxiaonian.infotainment.feature.launcher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wangxiaonian.infotainment.presentation.component.CarCard
import com.wangxiaonian.infotainment.presentation.component.CarListItem
import com.wangxiaonian.infotainment.presentation.component.DrivingRestrictionOverlay

/**
 * 桌面启动器界面
 *
 * @author 王小年联盟
 * @version 1.0
 */
@Composable
fun LauncherScreen(
    viewModel: LauncherViewModel,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToNavigation: () -> Unit,
    onNavigateToHvac: () -> Unit,
    onNavigateToMedia: () -> Unit
) {
    val apps by viewModel.apps.collectAsState()
    val recentApps by viewModel.recentApps.collectAsState()
    val restrictionState by viewModel.restrictionState.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()

    val isRestricted = restrictionState != com.wangxiaonian.infotainment.vehicle.RestrictionState.Full

    Scaffold(
        topBar = {
            LauncherTopBar(
                unreadCount = unreadCount,
                onNotificationsClick = onNavigateToNotifications,
                onSettingsClick = onNavigateToSettings
            )
        }
    ) { paddingValues ->
        DrivingRestrictionOverlay(isRestricted = false) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                // 最近使用
                if (recentApps.isNotEmpty()) {
                    Text(
                        text = "最近使用",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        recentApps.take(4).forEach { app ->
                            RecentAppItem(
                                app = app,
                                onClick = { viewModel.launchApp(app) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }

                // 应用网格
                Text(
                    text = "应用",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = viewModel.getAppsForCurrentState(),
                        key = { it.packageName }
                    ) { app ->
                        AppGridItem(
                            app = app,
                            onClick = { viewModel.launchApp(app) },
                            enabled = !isRestricted || app.category == AppCategory.NAVIGATION
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LauncherTopBar(
    unreadCount: Int,
    onNotificationsClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "智能座舱",
            style = MaterialTheme.typography.headlineMedium
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 通知按钮
            BadgedBox(
                badge = {
                    if (unreadCount > 0) {
                        Badge { Text(unreadCount.toString()) }
                    }
                }
            ) {
                IconButton(onClick = onNotificationsClick) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "通知",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // 设置按钮
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "设置",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun RecentAppItem(
    app: CarAppInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CarCard(
        onClick = onClick,
        modifier = modifier
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 这里应该显示应用图标
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.medium
                    )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = app.label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AppGridItem(
    app: CarAppInfo,
    onClick: () -> Unit,
    enabled: Boolean
) {
    CarCard(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.aspectRatio(1f)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 应用图标占位
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        if (enabled) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    )
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = app.label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
