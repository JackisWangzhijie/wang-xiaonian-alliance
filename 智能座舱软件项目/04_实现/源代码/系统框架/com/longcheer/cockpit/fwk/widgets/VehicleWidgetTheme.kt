package com.longcheer.cockpit.fwk.widgets

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 控件主题配置
 * 需求追溯: REQ-FWK-FUN-015
 */
@Immutable
data class VehicleWidgetTheme(
    // 颜色
    val primaryColor: Color,
    val secondaryColor: Color,
    val backgroundColor: Color,
    val surfaceColor: Color,
    val onPrimaryColor: Color,
    val onBackgroundColor: Color,
    val warningColor: Color,
    val errorColor: Color,
    val successColor: Color,

    // 字体样式
    val typography: VehicleTypography,

    // 形状定义
    val shapes: VehicleShapes,

    // 尺寸定义
    val dimensions: VehicleDimensions
)

/**
 * 车辆专用字体样式
 */
@Immutable
data class VehicleTypography(
    val speedDisplay: TextStyle,      // 车速显示字体
    val gaugeLabel: TextStyle,        // 仪表标签字体
    val controlLabel: TextStyle,      // 控件标签字体
    val buttonText: TextStyle,        // 按钮文字字体
    val infoText: TextStyle           // 信息文字字体
)

/**
 * 车辆专用形状
 */
@Immutable
data class VehicleShapes(
    val buttonCornerRadius: Dp,       // 按钮圆角
    val cardCornerRadius: Dp,         // 卡片圆角
    val gaugeCornerRadius: Dp,        // 仪表圆角
    val small: CornerBasedShape,
    val medium: CornerBasedShape,
    val large: CornerBasedShape
)

/**
 * 车辆专用尺寸
 */
@Immutable
data class VehicleDimensions(
    val touchTargetSize: Dp = 88.dp,              // 最小触控目标尺寸
    val buttonHeight: Dp = 80.dp,                 // 按钮高度
    val buttonMinWidth: Dp = 120.dp,              // 按钮最小宽度
    val iconSize: Dp = 64.dp,                     // 图标尺寸
    val iconSizeSmall: Dp = 48.dp,                // 小图标尺寸
    val iconSizeLarge: Dp = 96.dp,                // 大图标尺寸
    val spacingSmall: Dp = 8.dp,
    val spacingMedium: Dp = 16.dp,
    val spacingLarge: Dp = 24.dp,
    val appGridColumnCount: Int = 4               // 应用网格列数
)

/**
 * 默认车辆主题
 */
val DefaultVehicleWidgetTheme = VehicleWidgetTheme(
    primaryColor = Color(0xFF2196F3),
    secondaryColor = Color(0xFF03A9F4),
    backgroundColor = Color(0xFF121212),
    surfaceColor = Color(0xFF1E1E1E),
    onPrimaryColor = Color.White,
    onBackgroundColor = Color.White,
    warningColor = Color(0xFFFF9800),
    errorColor = Color(0xFFF44336),
    successColor = Color(0xFF4CAF50),
    typography = VehicleTypography(
        speedDisplay = TextStyle(
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-2).sp
        ),
        gaugeLabel = TextStyle(
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.sp
        ),
        controlLabel = TextStyle(
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.sp
        ),
        buttonText = TextStyle(
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.sp
        ),
        infoText = TextStyle(
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.sp
        )
    ),
    shapes = VehicleShapes(
        buttonCornerRadius = 16.dp,
        cardCornerRadius = 20.dp,
        gaugeCornerRadius = 100.dp,
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(24.dp)
    ),
    dimensions = VehicleDimensions()
)

/**
 * 运动风格主题
 */
val SportVehicleWidgetTheme = DefaultVehicleWidgetTheme.copy(
    primaryColor = Color(0xFFFF5722),
    secondaryColor = Color(0xFFFF9800),
    typography = DefaultVehicleWidgetTheme.typography.copy(
        speedDisplay = DefaultVehicleWidgetTheme.typography.speedDisplay.copy(
            color = Color(0xFFFF5722)
        )
    )
)

/**
 * 优雅风格主题
 */
val ElegantVehicleWidgetTheme = DefaultVehicleWidgetTheme.copy(
    primaryColor = Color(0xFF9C27B0),
    secondaryColor = Color(0xFFE1BEE7),
    shapes = DefaultVehicleWidgetTheme.shapes.copy(
        buttonCornerRadius = 24.dp,
        cardCornerRadius = 28.dp
    )
)

/**
 * CompositionLocal提供车辆主题
 */
val LocalVehicleWidgetTheme = staticCompositionLocalOf {
    DefaultVehicleWidgetTheme
}

/**
 * 车辆主题包装器
 */
@Composable
fun VehicleWidgetTheme(
    theme: VehicleWidgetTheme = DefaultVehicleWidgetTheme,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalVehicleWidgetTheme provides theme,
        content = content
    )
}

/**
 * 获取当前车辆主题的便捷方法
 */
val vehicleWidgetTheme: VehicleWidgetTheme
    @Composable
    get() = LocalVehicleWidgetTheme.current
