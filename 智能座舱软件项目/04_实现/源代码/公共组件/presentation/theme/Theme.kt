package com.longcheer.cockpit.common.presentation.theme

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Android Automotive OS (AAOS) Material3 主题配置
 * 
 * 核心特性：
 * - 深色模式默认（夜间驾驶优先）
 * - 圆角设计适合触摸操作
 * - 高对比度确保可读性
 * - 支持日间/夜间自动切换
 */

// ============================================
// 车载专用形状定义
// ============================================

/**
 * 车载UI形状规范
 * - 大圆角适合触摸目标
 * - 统一圆角尺寸减少认知负担
 */
val CarShapes = Shapes(
    // 小圆角 - 小按钮、标签
    small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    // 中圆角 - 卡片、列表项
    medium = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    // 大圆角 - 大卡片、对话框
    large = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    // 超大圆角 - 底部面板、全屏模态
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
)

// ============================================
// 车载专用尺寸规范
// ============================================

/**
 * 触摸目标尺寸规范
 * 符合 AAOS 人机交互规范
 */
object CarDimensions {
    /**
     * 最小触摸区域（Google Automotive 推荐）
     */
    const val MIN_TOUCH_TARGET_DP = 76
    
    /**
     * 推荐触摸区域
     */
    const val RECOMMENDED_TOUCH_TARGET_DP = 88
    
    /**
     * 大按钮触摸区域
     */
    const val LARGE_TOUCH_TARGET_DP = 104
    
    /**
     * 触摸区域对应的 Padding
     */
    val minTouchTarget = androidx.compose.ui.unit.Dp.MIN_TOUCH_TARGET_DP.dp
    val recommendedTouchTarget = RECOMMENDED_TOUCH_TARGET_DP.dp
    val largeTouchTarget = LARGE_TOUCH_TARGET_DP.dp
    
    /**
     * 间距规范
     */
    val spacingXS = 4.dp
    val spacingS = 8.dp
    val spacingM = 16.dp
    val spacingL = 24.dp
    val spacingXL = 32.dp
    val spacingXXL = 48.dp
    
    /**
     * 卡片内边距
     */
    val cardPaddingHorizontal = 24.dp
    val cardPaddingVertical = 20.dp
    
    /**
     * 列表项高度
     */
    val listItemHeightMin = 88.dp
    val listItemHeightRecommended = 104.dp
    
    /**
     * 图标尺寸
     */
    val iconSmall = 24.dp
    val iconMedium = 32.dp
    val iconLarge = 48.dp
    val iconXLarge = 64.dp
}

// ============================================
// 主题模式定义
// ============================================

/**
 * 车载主题模式
 */
enum class CarThemeMode {
    AUTO,       // 跟随系统/光感
    DARK,       // 强制深色
    LIGHT       // 强制浅色
}

// ============================================
// Composition Locals
// ============================================

/**
 * 提供当前主题模式
 */
val LocalCarThemeMode = staticCompositionLocalOf { CarThemeMode.AUTO }

/**
 * 提供是否是驾驶模式
 */
val LocalDrivingMode = staticCompositionLocalOf { false }

/**
 * 提供当前车速（影响UI元素大小）
 */
val LocalVehicleSpeed = staticCompositionLocalOf { 0 }

// ============================================
// 主题 Composable
// ============================================

/**
 * 智能座舱主题
 * 
 * @param themeMode 主题模式，默认跟随系统
 * @param isDriving 是否处于驾驶状态，影响字号和触摸目标
 * @param vehicleSpeed 当前车速，用于动态调整UI
 * @param content 内容
 */
@Composable
fun CarCockpitTheme(
    themeMode: CarThemeMode = CarThemeMode.AUTO,
    isDriving: Boolean = false,
    vehicleSpeed: Int = 0,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    
    // 确定当前是否为深色模式
    val isDarkTheme = when (themeMode) {
        CarThemeMode.DARK -> true
        CarThemeMode.LIGHT -> false
        CarThemeMode.AUTO -> isSystemInDarkTheme() || isCarNightMode(context)
    }
    
    // 选择对应的颜色方案
    val colorScheme = if (isDarkTheme) DarkCarColorScheme else LightCarColorScheme
    
    // 驾驶模式下可能需要调整字体大小
    val typography = if (isDriving || vehicleSpeed > 0) {
        // 驾驶模式下使用更大的字号
        CarTypography.copy(
            bodyLarge = CarTypography.bodyLarge.copy(fontSize = androidx.compose.ui.unit.sp(28)),
            bodyMedium = CarTypography.bodyMedium.copy(fontSize = androidx.compose.ui.unit.sp(24)),
            labelLarge = CarTypography.labelLarge.copy(fontSize = androidx.compose.ui.unit.sp(28))
        )
    } else {
        CarTypography
    }
    
    CompositionLocalProvider(
        LocalCarThemeMode provides themeMode,
        LocalDrivingMode provides isDriving,
        LocalVehicleSpeed provides vehicleSpeed
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = CarShapes,
            content = content
        )
    }
}

/**
 * 检查车辆是否处于夜间模式
 * 通过 UiModeManager 或光感传感器判断
 */
private fun isCarNightMode(context: Context): Boolean {
    val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
    return if (uiModeManager != null) {
        // 在 Automotive 环境中检查夜间模式
        uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES
    } else {
        // 回退到系统配置
        val currentNightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        currentNightMode == Configuration.UI_MODE_NIGHT_YES
    }
}

// ============================================
// 主题扩展属性
// ============================================

/**
 * 获取当前是否处于深色模式
 */
val isDarkMode: Boolean
    @Composable
    @ReadOnlyComposable
    get() = !MaterialTheme.colorScheme.background.equals(LightBackground.Default)

/**
 * 获取当前是否处于驾驶模式
 */
val isInDrivingMode: Boolean
    @Composable
    @ReadOnlyComposable
    get() = LocalDrivingMode.current

/**
 * 获取当前车速
 */
val currentVehicleSpeed: Int
    @Composable
    @ReadOnlyComposable
    get() = LocalVehicleSpeed.current

// ============================================
// 预览主题（用于Compose Preview）
// ============================================

/**
 * 深色主题预览
 */
@Composable
fun CarCockpitDarkThemePreview(
    content: @Composable () -> Unit
) {
    CarCockpitTheme(themeMode = CarThemeMode.DARK, content = content)
}

/**
 * 浅色主题预览
 */
@Composable
fun CarCockpitLightThemePreview(
    content: @Composable () -> Unit
) {
    CarCockpitTheme(themeMode = CarThemeMode.LIGHT, content = content)
}

/**
 * 驾驶模式预览（深色+大字体）
 */
@Composable
fun CarCockpitDrivingThemePreview(
    content: @Composable () -> Unit
) {
    CarCockpitTheme(
        themeMode = CarThemeMode.DARK,
        isDriving = true,
        vehicleSpeed = 60,
        content = content
    )
}
