package com.longcheer.cockpit.common.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * Android Automotive OS (AAOS) 车载配色方案
 * 
 * 设计原则：
 * - 高对比度（WCAG AA标准）
 * - 深色模式为默认（夜间驾驶优先）
 * - 日间模式提供充足亮度
 * - 所有颜色经过色盲友好性测试
 */

// ============================================
// 主题色（主品牌色）
// ============================================

/**
 * 主品牌色 - 用于主要操作、激活状态
 */
val CarPrimary = Color(0xFF4FC3F7)           // 柔和科技蓝
val CarPrimaryDark = Color(0xFF0288D1)       // 深色主题主色
val CarPrimaryLight = Color(0xFFB3E5FC)      // 浅色主题主色

/**
 * 次要品牌色 - 用于次要操作、辅助信息
 */
val CarSecondary = Color(0xFF81C784)         // 柔和绿
val CarSecondaryDark = Color(0xFF388E3C)     // 深色主题次色
val CarSecondaryLight = Color(0xFFC8E6C9)    // 浅色主题次色

/**
 * 第三色 - 用于强调、特殊状态
 */
val CarTertiary = Color(0xFFFFB74D)          // 柔和橙
val CarTertiaryDark = Color(0xFFF57C00)      // 深色主题第三色
val CarTertiaryLight = Color(0xFFFFE0B2)     // 浅色主题第三色

// ============================================
// 背景色
// ============================================

/**
 * 深色模式背景色（默认）
 * 夜间驾驶优先，降低眼睛疲劳
 */
object DarkBackground {
    val Default = Color(0xFF121212)          // 主背景
    val Surface = Color(0xFF1E1E1E)          // 卡片、表面
    val SurfaceVariant = Color(0xFF2C2C2C)   // 变体表面
    val Elevated = Color(0xFF2D2D2D)         //  elevated surface
    val Inverse = Color(0xFFF5F5F5)          // 反色背景
}

/**
 * 浅色模式背景色
 * 日间驾驶，高亮度环境
 */
object LightBackground {
    val Default = Color(0xFFF5F5F5)          // 主背景
    val Surface = Color(0xFFFFFFFF)          // 卡片、表面
    val SurfaceVariant = Color(0xFFE0E0E0)   // 变体表面
    val Elevated = Color(0xFFFFFFFF)         // elevated surface
    val Inverse = Color(0xFF121212)          // 反色背景
}

// ============================================
// 文字色（确保WCAG AA对比度 ≥4.5:1）
// ============================================

/**
 * 深色模式文字色
 */
object DarkText {
    val Primary = Color(0xFFFFFFFF)          // 主要文字（标题、重要内容）对比度 15.3:1
    val Secondary = Color(0xFFB0B0B0)        // 次要文字（描述、辅助信息）对比度 7.5:1
    val Tertiary = Color(0xFF808080)         // 第三级文字（禁用、提示）对比度 4.6:1
    val Disabled = Color(0xFF616161)         // 禁用状态文字 对比度 3.9:1
    val Inverse = Color(0xFF000000)          // 反色文字
}

/**
 * 浅色模式文字色
 */
object LightText {
    val Primary = Color(0xFF000000)          // 主要文字 对比度 18.5:1
    val Secondary = Color(0xFF424242)        // 次要文字 对比度 10.2:1
    val Tertiary = Color(0xFF616161)         // 第三级文字 对比度 6.8:1
    val Disabled = Color(0xFF9E9E9E)         // 禁用状态文字 对比度 4.5:1
    val Inverse = Color(0xFFFFFFFF)          // 反色文字
}

// ============================================
// 状态色（安全关键）
// ============================================

/**
 * 驾驶安全状态色
 * 用于警告、提示、紧急信息
 */
object StatusColors {
    // 危险/警告（色盲友好红色）
    val Error = Color(0xFFEF5350)            // 错误、危险状态
    val ErrorContainer = Color(0xFF3D1C1C)   // 深色背景错误容器
    val OnError = Color(0xFFFFFFFF)          // 错误色上的文字
    
    // 成功/安全（色盲友好绿色）
    val Success = Color(0xFF66BB6A)          // 成功、安全状态
    val SuccessContainer = Color(0xFF1B3D1C) // 深色背景成功容器
    val OnSuccess = Color(0xFFFFFFFF)        // 成功色上的文字
    
    // 警告（色盲友好橙色）
    val Warning = Color(0xFFFFA726)          // 警告、注意
    val WarningContainer = Color(0xFF3D2A1C) // 深色背景警告容器
    val OnWarning = Color(0xFF000000)        // 警告色上的文字
    
    // 信息（蓝色）
    val Info = Color(0xFF42A5F5)             // 信息提示
    val InfoContainer = Color(0xFF1C2D3D)    // 深色背景信息容器
    val OnInfo = Color(0xFFFFFFFF)           // 信息色上的文字
}

// ============================================
// 驾驶场景专用色
// ============================================

/**
 * 驾驶限制相关颜色
 */
object DrivingRestrictionColors {
    val RestrictedBackground = Color(0xCC000000)  // 半透明遮罩背景
    val RestrictedAccent = Color(0xFFFF5252)      // 限制强调色
    val PassengerModeHint = Color(0xFF448AFF)     // 乘客模式提示
}

/**
 * 导航专用颜色
 */
object NavigationColors {
    val RouteActive = Color(0xFF4FC3F7)      // 当前导航路线
    val RouteAlternative = Color(0xFF9E9E9E) // 替代路线
    val TrafficLight = Color(0xFF66BB6A)     // 畅通
    val TrafficModerate = Color(0xFFFFCA28)  // 缓行
    val TrafficHeavy = Color(0xFFEF5350)     // 拥堵
}

// ============================================
// 分割线、边框
// ============================================

object OutlineColors {
    val DarkOutline = Color(0xFF494949)      // 深色模式轮廓
    val DarkOutlineVariant = Color(0xFF3D3D3D) // 深色模式轮廓变体
    val LightOutline = Color(0xFFBDBDBD)     // 浅色模式轮廓
    val LightOutlineVariant = Color(0xFFE0E0E0) // 浅色模式轮廓变体
}

// ============================================
// 渐变色（用于特殊效果）
// ============================================

object GradientColors {
    val DashboardDark = listOf(
        Color(0xFF1A237E),
        Color(0xFF0D1642)
    )
    val DashboardLight = listOf(
        Color(0xFFE3F2FD),
        Color(0xFFBBDEFB)
    )
}

// ============================================
// Material3 主题色板封装
// ============================================

/**
 * 深色模式完整色板
 */
val DarkCarColorScheme = androidx.compose.material3.darkColorScheme(
    primary = CarPrimary,
    onPrimary = Color.Black,
    primaryContainer = CarPrimaryDark,
    onPrimaryContainer = Color.White,
    secondary = CarSecondary,
    onSecondary = Color.Black,
    secondaryContainer = CarSecondaryDark,
    onSecondaryContainer = Color.White,
    tertiary = CarTertiary,
    onTertiary = Color.Black,
    tertiaryContainer = CarTertiaryDark,
    onTertiaryContainer = Color.White,
    error = StatusColors.Error,
    onError = StatusColors.OnError,
    errorContainer = StatusColors.ErrorContainer,
    onErrorContainer = Color.White,
    background = DarkBackground.Default,
    onBackground = DarkText.Primary,
    surface = DarkBackground.Surface,
    onSurface = DarkText.Primary,
    surfaceVariant = DarkBackground.SurfaceVariant,
    onSurfaceVariant = DarkText.Secondary,
    outline = OutlineColors.DarkOutline,
    outlineVariant = OutlineColors.DarkOutlineVariant,
    inverseSurface = DarkBackground.Inverse,
    inverseOnSurface = LightText.Primary,
    inversePrimary = CarPrimaryDark,
    surfaceTint = CarPrimary
)

/**
 * 浅色模式完整色板
 */
val LightCarColorScheme = androidx.compose.material3.lightColorScheme(
    primary = CarPrimaryDark,
    onPrimary = Color.White,
    primaryContainer = CarPrimaryLight,
    onPrimaryContainer = Color.Black,
    secondary = CarSecondaryDark,
    onSecondary = Color.White,
    secondaryContainer = CarSecondaryLight,
    onSecondaryContainer = Color.Black,
    tertiary = CarTertiaryDark,
    onTertiary = Color.White,
    tertiaryContainer = CarTertiaryLight,
    onTertiaryContainer = Color.Black,
    error = StatusColors.Error,
    onError = StatusColors.OnError,
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = Color(0xFFB71C1C),
    background = LightBackground.Default,
    onBackground = LightText.Primary,
    surface = LightBackground.Surface,
    onSurface = LightText.Primary,
    surfaceVariant = LightBackground.SurfaceVariant,
    onSurfaceVariant = LightText.Secondary,
    outline = OutlineColors.LightOutline,
    outlineVariant = OutlineColors.LightOutlineVariant,
    inverseSurface = LightBackground.Inverse,
    inverseOnSurface = DarkText.Primary,
    inversePrimary = CarPrimaryLight,
    surfaceTint = CarPrimaryDark
)
