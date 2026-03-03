package com.wangxiaonian.infotainment.feature.media

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wangxiaonian.infotainment.presentation.component.CarCard
import com.wangxiaonian.infotainment.presentation.component.CarListItem

/**
 * 媒体娱乐界面
 *
 * @author 王小年联盟
 * @version 1.0
 */
@Composable
fun MediaScreen(
    viewModel: MediaViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.mediaState.collectAsState()
    val playlist by viewModel.playlist.collectAsState()

    Scaffold(
        topBar = {
            MediaTopBar(
                sourceType = state.sourceType,
                onBack = onBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // 当前播放信息
            NowPlayingCard(
                currentItem = state.currentItem,
                isPlaying = state.isPlaying,
                onPlayPause = { viewModel.playPause() },
                onNext = { viewModel.next() },
                onPrevious = { viewModel.previous() }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 播放控制
            PlaybackControls(
                volume = state.volume,
                playMode = state.playMode,
                onVolumeChange = { viewModel.setVolume(it) },
                onPlayModeChange = { viewModel.setPlayMode(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 播放列表
            Text(
                text = "播放列表",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (playlist.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无播放内容",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(playlist) { item ->
                        MediaListItem(
                            item = item,
                            isPlaying = state.currentItem?.id == item.id && state.isPlaying,
                            onClick = { viewModel.selectItem(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaTopBar(
    sourceType: MediaSourceType,
    onBack: () -> Unit
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
                text = "媒体娱乐",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        // 音源切换
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SourceChip(
                icon = Icons.Default.MusicNote,
                label = "本地",
                isSelected = sourceType == MediaSourceType.LOCAL
            )
            SourceChip(
                icon = Icons.Default.Usb,
                label = "USB",
                isSelected = sourceType == MediaSourceType.USB
            )
            SourceChip(
                icon = Icons.Default.Bluetooth,
                label = "蓝牙",
                isSelected = sourceType == MediaSourceType.BLUETOOTH
            )
            SourceChip(
                icon = Icons.Default.Radio,
                label = "电台",
                isSelected = sourceType == MediaSourceType.RADIO
            )
        }
    }
}

@Composable
private fun SourceChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean
) {
    AssistChip(
        onClick = { },
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun NowPlayingCard(
    currentItem: MediaItem?,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    CarCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 封面图
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (currentItem?.coverUrl != null) {
                    // TODO: 加载封面图片
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        modifier = Modifier
                            .size(60.dp)
                            .align(Alignment.Center),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(24.dp))

            // 歌曲信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = currentItem?.title ?: "未播放",
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = currentItem?.artist ?: "选择一首歌曲",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )

                if (currentItem?.album != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentItem.album,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(24.dp))

            // 播放控制
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledIconButton(
                    onClick = onPrevious,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "上一首",
                        modifier = Modifier.size(36.dp)
                    )
                }

                FilledIconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(88.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause 
                                     else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        modifier = Modifier.size(48.dp)
                    )
                }

                FilledIconButton(
                    onClick = onNext,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "下一首",
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaybackControls(
    volume: Int,
    playMode: PlayMode,
    onVolumeChange: (Int) -> Unit,
    onPlayModeChange: (PlayMode) -> Unit
) {
    CarCard {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            // 音量控制
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeMute,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Slider(
                    value = volume.toFloat(),
                    onValueChange = { onVolumeChange(it.toInt()) },
                    valueRange = 0f..100f,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "$volume%",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.width(60.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 播放模式
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PlayModeButton(
                    icon = Icons.Default.Repeat,
                    label = "顺序",
                    isSelected = playMode == PlayMode.SEQUENCE,
                    onClick = { onPlayModeChange(PlayMode.SEQUENCE) }
                )
                PlayModeButton(
                    icon = Icons.Default.Shuffle,
                    label = "随机",
                    isSelected = playMode == PlayMode.SHUFFLE,
                    onClick = { onPlayModeChange(PlayMode.SHUFFLE) }
                )
                PlayModeButton(
                    icon = Icons.Default.RepeatOne,
                    label = "单曲",
                    isSelected = playMode == PlayMode.REPEAT_ONE,
                    onClick = { onPlayModeChange(PlayMode.REPEAT_ONE) }
                )
                PlayModeButton(
                    icon = Icons.Default.Repeat,
                    label = "循环",
                    isSelected = playMode == PlayMode.REPEAT_ALL,
                    onClick = { onPlayModeChange(PlayMode.REPEAT_ALL) }
                )
            }
        }
    }
}

@Composable
private fun PlayModeButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(28.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MediaListItem(
    item: MediaItem,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    CarCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 播放状态指示
            if (isPlaying) {
                Icon(
                    imageVector = Icons.Default.Equalizer,
                    contentDescription = "正在播放",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
            }

            // 歌曲信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isPlaying) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.artist,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 时长
            Text(
                text = formatDuration(item.duration),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val seconds = durationMs / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%d:%02d".format(minutes, remainingSeconds)
}
