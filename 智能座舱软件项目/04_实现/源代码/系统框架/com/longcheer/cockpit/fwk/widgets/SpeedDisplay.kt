package com.longcheer.cockpit.fwk.widgets

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 车速显示控件
 * 需求追溯: REQ-FWK-FUN-015
 *
 * @param speed 当前车速
 * @param unit 速度单位
 * @param maxSpeed 最大车速（用于计算进度）
 * @param modifier 修饰符
 * @param theme 车辆主题
 */
@Composable
fun SpeedDisplay(
    speed: Int,
    unit: SpeedUnit = SpeedUnit.KMH,
    maxSpeed: Int = 240,
    modifier: Modifier = Modifier,
    theme: VehicleWidgetTheme = LocalVehicleWidgetTheme.current
) {
    val animatedSpeed by animateFloatAsState(
        targetValue = speed.toFloat(),
        animationSpec = androidx.compose.animation.core.tween(300),
        label = "speed"
    )

    val progress = (animatedSpeed / maxSpeed).coerceIn(0f, 1f)

    // 根据速度选择颜色
    val progressColor = when {
        speed > 120 -> theme.errorColor
        speed > 80 -> theme.warningColor
        else -> theme.primaryColor
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(200.dp)
            .clip(CircleShape)
            .background(theme.surfaceColor)
    ) {
        // 背景进度环
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxSize()
                .progressSemantics(progress),
            color = progressColor,
            trackColor = theme.backgroundColor,
            strokeWidth = 12.dp,
            strokeCap = StrokeCap.Round
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 速度值
            Text(
                text = animatedSpeed.toInt().toString(),
                style = theme.typography.speedDisplay,
                color = theme.onBackgroundColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 单位
            Text(
                text = unit.displayName,
                style = theme.typography.infoText,
                color = theme.onBackgroundColor.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 速度单位枚举
 */
enum class SpeedUnit(val displayName: String) {
    KMH("km/h"),
    MPH("mph")
}
