package com.longcheer.cockpit.fwk.launcher

import android.graphics.drawable.Drawable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.longcheer.cockpit.fwk.model.AppInfo

/**
 * 应用网格屏幕
 * 需求追溯: REQ-FWK-FUN-014
 *
 * @param apps 应用列表
 * @param onAppClick 应用点击回调
 * @param onAppLongClick 应用长按回调
 * @param isEditMode 是否编辑模式
 * @param modifier 修饰符
 */
@Composable
fun AppGridScreen(
    apps: List<AppInfo>,
    onAppClick: (String) -> Unit,
    onAppLongClick: (String) -> Unit = {},
    isEditMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(
            items = apps,
            key = { it.appId }
        ) { app ->
            AppCard(
                app = app,
                onClick = { onAppClick(app.appId) },
                onLongClick = { onAppLongClick(app.appId) },
                isEditMode = isEditMode
            )
        }
    }
}

/**
 * 应用卡片组件
 * 需求追溯: REQ-FWK-FUN-014
 *
 * @param app 应用信息
 * @param onClick 点击回调
 * @param onLongClick 长按回调
 * @param isEditMode 是否编辑模式
 * @param modifier 修饰符
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppCard(
    app: AppInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    isEditMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed && !isEditMode) 0.95f else 1f,
        label = "scale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(100.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                onClickLabel = "打开${app.appName}",
                onLongClickLabel = "编辑${app.appName}"
            )
            .padding(4.dp)
    ) {
        // 应用图标
        AppIcon(
            iconPath = app.iconPath,
            packageName = app.appId,
            modifier = Modifier
                .size(72.dp)
                .scale(scale)
                .alpha(if (app.isEnabled) 1f else 0.5f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 应用名称
        Text(
            text = app.appName,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = if (app.isEnabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            },
            modifier = Modifier.fillMaxWidth()
        )

        // 白名单标记
        if (app.isInWhitelist) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "白名单应用",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(12.dp)
            )
        }

        // 编辑模式指示器
        if (isEditMode) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

/**
 * 应用图标组件
 *
 * @param iconPath 图标路径
 * @param packageName 应用包名
 * @param modifier 修饰符
 */
@Composable
fun AppIcon(
    iconPath: String?,
    packageName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (iconPath != null) {
                // 使用Coil加载图标
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(iconPath)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // 从PackageManager加载默认图标
                val iconDrawable = remember(packageName) {
                    try {
                        context.packageManager.getApplicationIcon(packageName)
                    } catch (e: Exception) {
                        null
                    }
                }

                if (iconDrawable != null) {
                    IconFromDrawable(
                        drawable = iconDrawable,
                        modifier = Modifier.fillMaxSize(0.6f)
                    )
                } else {
                    // 默认图标
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 从Drawable加载图标
 */
@Composable
fun IconFromDrawable(
    drawable: Drawable,
    modifier: Modifier = Modifier
) {
    val bitmap = remember(drawable) {
        drawable.toBitmap()
    }
    Image(
        painter = BitmapPainter(bitmap.asImageBitmap()),
        contentDescription = null,
        modifier = modifier
    )
}

/**
 * 空状态视图
 */
@Composable
fun EmptyAppGridState(
    message: String = "暂无应用",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Apps,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}
