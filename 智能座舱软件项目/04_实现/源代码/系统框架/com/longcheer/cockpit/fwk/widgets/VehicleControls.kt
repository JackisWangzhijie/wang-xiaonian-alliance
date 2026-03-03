package com.longcheer.cockpit.fwk.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 车辆滑块控件
 * 需求追溯: REQ-FWK-FUN-015
 *
 * @param value 当前值
 * @param onValueChange 值变化回调
 * @param modifier 修饰符
 * @param valueRange 值范围
 * @param enabled 是否启用
 * @param steps 步数
 * @param label 标签
 * @param theme 车辆主题
 */
@Composable
fun VehicleSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true,
    steps: Int = 0,
    label: String? = null,
    theme: VehicleWidgetTheme = LocalVehicleWidgetTheme.current
) {
    Column(modifier = modifier) {
        label?.let {
            Text(
                text = it,
                style = theme.typography.controlLabel,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = theme.primaryColor,
                activeTrackColor = theme.primaryColor,
                inactiveTrackColor = theme.surfaceColor,
                disabledThumbColor = theme.onBackgroundColor.copy(alpha = 0.3f),
                disabledActiveTrackColor = theme.onBackgroundColor.copy(alpha = 0.2f),
                disabledInactiveTrackColor = theme.onBackgroundColor.copy(alpha = 0.1f)
            ),
            modifier = Modifier.height(48.dp)  // 增大触控区域
        )
    }
}

/**
 * 车辆开关控件
 * 需求追溯: REQ-FWK-FUN-015
 *
 * @param checked 是否选中
 * @param onCheckedChange 状态变化回调
 * @param modifier 修饰符
 * @param enabled 是否启用
 * @param label 标签
 * @param theme 车辆主题
 */
@Composable
fun VehicleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: String? = null,
    theme: VehicleWidgetTheme = LocalVehicleWidgetTheme.current
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)  // 确保足够的触控区域
    ) {
        label?.let {
            Text(
                text = it,
                style = theme.typography.controlLabel,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = theme.primaryColor,
                checkedTrackColor = theme.primaryColor.copy(alpha = 0.5f),
                uncheckedThumbColor = theme.onBackgroundColor.copy(alpha = 0.6f),
                uncheckedTrackColor = theme.surfaceColor
            )
        )
    }
}

/**
 * 车辆文本控件
 * 需求追溯: REQ-FWK-FUN-015
 *
 * @param text 文本内容
 * @param style 文本样式类型
 * @param modifier 修饰符
 * @param theme 车辆主题
 */
@Composable
fun VehicleText(
    text: String,
    style: VehicleTextStyle = VehicleTextStyle.BODY,
    modifier: Modifier = Modifier,
    theme: VehicleWidgetTheme = LocalVehicleWidgetTheme.current
) {
    val textStyle = when (style) {
        VehicleTextStyle.SPEED -> theme.typography.speedDisplay
        VehicleTextStyle.GAUGE_LABEL -> theme.typography.gaugeLabel
        VehicleTextStyle.CONTROL_LABEL -> theme.typography.controlLabel
        VehicleTextStyle.BUTTON -> theme.typography.buttonText
        VehicleTextStyle.INFO -> theme.typography.infoText
        VehicleTextStyle.BODY -> MaterialTheme.typography.bodyLarge
    }

    Text(
        text = text,
        style = textStyle,
        color = theme.onBackgroundColor,
        modifier = modifier
    )
}

/**
 * 车辆文本样式枚举
 */
enum class VehicleTextStyle {
    SPEED,          // 车速显示
    GAUGE_LABEL,    // 仪表标签
    CONTROL_LABEL,  // 控件标签
    BUTTON,         // 按钮文字
    INFO,           // 信息文字
    BODY            // 正文
}
