package com.wangxiaonian.infotainment.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Material3 主题配置
 *
 * @author 王小年联盟
 * @version 1.0
 */

private val DarkColorScheme = darkColorScheme(
    primary = CarPrimary,
    onPrimary = Color.Black,
    primaryContainer = CarPrimaryDark,
    secondary = CarSecondary,
    onSecondary = Color.Black,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    error = ErrorRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = CarPrimaryDark,
    onPrimary = Color.White,
    primaryContainer = CarPrimary,
    secondary = CarSecondary,
    onSecondary = Color.Black,
    background = LightBackground,
    onBackground = Color.Black,
    surface = LightSurface,
    onSurface = Color.Black,
    error = ErrorRed,
    onError = Color.White
)

val CarShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun CarCockpitTheme(
    darkTheme: Boolean = true, // 车载默认深色模式
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CarTypography,
        shapes = CarShapes,
        content = content
    )
}
