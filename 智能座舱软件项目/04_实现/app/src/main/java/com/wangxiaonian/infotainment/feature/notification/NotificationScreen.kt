package com.wangxiaonian.infotainment.feature.notification

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wangxiaonian.infotainment.data.model.NotificationEntity
import com.wangxiaonian.infotainment.data.model.NotificationPriority
import com.wangxiaonian.infotainment.presentation.component.CarCard
import com.wangxiaonian.infotainment.presentation.component.CarListItem

/**
 * 消息中心界面
 *
 * @author 王小年联盟
 * @version 1.0
 */
@Composable
fun NotificationScreen(
    viewModel: NotificationViewModel,
    onBack: () -> Unit
) {
    val notifications by viewModel.notifications.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()
    val restrictionState by viewModel.restrictionState.collectAsState()

    val isRestricted = restrictionState != com.wangxiaonian.infotainment.vehicle.RestrictionState.Full

    Scaffold(
        topBar = {
            NotificationTopBar(
                unreadCount = unreadCount,
                onBack = onBack,
                onClearAll = { /* TODO: 清除所有 */ }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            if (isRestricted) {
                // 驾驶中限制提示
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "行驶中仅显示紧急通知",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            if (notifications.isEmpty()) {
                // 空状态
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "暂无通知",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = notifications,
                        key = { it.id }
                    ) { notification ->
                        NotificationCard(
                            notification = notification,
                            onClick = { viewModel.onNotificationClick(notification) },
                            onDismiss = { viewModel.dismissNotification(notification.id) },
                            onSpeak = { viewModel.speakNotification(notification) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationTopBar(
    unreadCount: Int,
    onBack: () -> Unit,
    onClearAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = "消息中心",
                style = MaterialTheme.typography.headlineMedium
            )

            if (unreadCount > 0) {
                Spacer(modifier = Modifier.width(12.dp))
                Badge {
                    Text(unreadCount.toString())
                }
            }
        }

        IconButton(onClick = onClearAll) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "清除全部",
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun NotificationCard(
    notification: NotificationEntity,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    onSpeak: () -> Unit
) {
    val priorityColor = when (notification.priority) {
        NotificationPriority.CRITICAL -> MaterialTheme.colorScheme.error
        NotificationPriority.HIGH -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    CarCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // 优先级指示器
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(60.dp)
                    .background(priorityColor, shape = MaterialTheme.shapes.small)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // 内容区
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (!notification.isRead) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = notification.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = notification.source,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 操作按钮
            Column {
                IconButton(onClick = onSpeak) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "朗读",
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "清除",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
