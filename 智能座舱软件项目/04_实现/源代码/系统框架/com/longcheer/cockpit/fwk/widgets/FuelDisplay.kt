package com.longcheer.cockpit.fwk.widgets

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.ElectricCar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 油量/电量显示控件
 * 需求追溯: REQ-FWK-FUN-015
 *
 * @param level 油量/电量百分比 (0.0 - 1.0)
 * @param range 续航里程(km)
 * @param type 能源类型
 * @param modifier 修饰符
 * @param theme 车辆主题
 */
@Composable
fun FuelDisplay(
    level: Float,  // 0.0 - 1.0
    range: Int,    // 续航里程(km)
    type: EnergyType = EnergyType.GASOLINE,
    modifier: Modifier = Modifier,
    theme: VehicleWidgetTheme = LocalVehicleWidgetTheme.current
) {
    val animatedLevel by animateFloatAsState(
        targetValue = level.coerceIn(0f, 1f),
        animationSpec = androidx.compose.animation.core.tween(500),
        label = "fuel_level"
    )

    // 根据油量选择颜色
    val indicatorColor = when {
        level < 0.15f -> theme.errorColor
        level < 0.3f -> theme.warningColor
        else -> when (type) {
            EnergyType.ELECTRIC -> Color(0xFF4CAF50)
            else -> theme.primaryColor
        }
    }

    Card(
        modifier = modifier
            .width(280.dp)
            .height(120.dp),
        shape = RoundedCornerShape(theme.shapes.cardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = theme.surfaceColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 油量/电量图标
            val icon = when (type) {
                EnergyType.GASOLINE -> Icons.Default.LocalGasStation
                EnergyType.ELECTRIC -> Icons.Default.BatteryFull
                EnergyType.HYBRID -> Icons.Default.ElectricCar
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = indicatorColor
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 油量/电量百分比
                Text(
                    text = "${(animatedLevel * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineMedium,
                    color = theme.onBackgroundColor
                )

                // 续航里程
                Text(
                    text = "剩余里程: ${range}km",
                    style = MaterialTheme.typography.bodyMedium,
                    color = theme.onBackgroundColor.copy(alpha = 0.7f)
                )

                // 进度条
                LinearProgressIndicator(
                    progress = { animatedLevel },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = indicatorColor,
                    trackColor = theme.backgroundColor
                )
            }
        }
    }
}

/**
 * 能源类型枚举
 */
enum class EnergyType {
    GASOLINE,    // 汽油
    ELECTRIC,    // 电动
    HYBRID       // 混动
}
