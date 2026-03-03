package com.longcheer.cockpit.fwk.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.longcheer.cockpit.fwk.model.AppCategory

/**
 * 快捷入口面板
 * 需求追溯: REQ-FWK-FUN-014
 *
 * @param shortcuts 快捷入口列表
 * @param onShortcutClick 快捷入口点击回调
 * @param modifier 修饰符
 */
@Composable
fun ShortcutPanel(
    shortcuts: List<ShortcutInfo>,
    onShortcutClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "快捷入口",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.width(16.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(shortcuts) { shortcut ->
                    ShortcutItem(
                        shortcut = shortcut,
                        onClick = { onShortcutClick(shortcut.id) }
                    )
                }
            }
        }
    }
}

/**
 * 快捷入口项
 *
 * @param shortcut 快捷入口信息
 * @param onClick 点击回调
 * @param modifier 修饰符
 */
@Composable
fun ShortcutItem(
    shortcut: ShortcutInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(64.dp)
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        // 图标
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (shortcut.iconPath != null) {
                AsyncImage(
                    model = shortcut.iconPath,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 名称
        Text(
            text = shortcut.name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * 分类筛选栏
 * 需求追溯: REQ-FWK-FUN-014
 *
 * @param categories 分类列表
 * @param selectedCategory 当前选中的分类
 * @param onCategorySelected 分类选择回调
 * @param modifier 修饰符
 */
@Composable
fun CategoryFilterBar(
    categories: List<AppCategoryInfo>,
    selectedCategory: AppCategory?,
    onCategorySelected: (AppCategory?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "全部"选项
        CategoryChip(
            label = "全部",
            isSelected = selectedCategory == null,
            onClick = { onCategorySelected(null) }
        )

        // 各个分类
        categories.forEach { categoryInfo ->
            CategoryChip(
                label = "${categoryInfo.category.displayName} (${categoryInfo.appCount})",
                isSelected = categoryInfo.isSelected,
                onClick = { onCategorySelected(categoryInfo.category) }
            )
        }
    }
}

/**
 * 分类选择芯片
 *
 * @param label 标签文本
 * @param isSelected 是否选中
 * @param onClick 点击回调
 * @param modifier 修饰符
 */
@Composable
fun CategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clickable(onClick = onClick)
            .height(36.dp),
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}
