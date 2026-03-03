package com.longcheer.cockpit.fwk.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * 车辆专用按钮
 * 大尺寸、高对比度，适合驾驶环境使用
 * 需求追溯: REQ-FWK-FUN-015
 *
 * @param onClick 点击回调
 * @param modifier 修饰符
 * @param enabled 是否启用
 * @param variant 按钮变体
 * @param size 按钮尺寸
 * @param content 按钮内容
 */
@Composable
fun VehicleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: VehicleButtonVariant = VehicleButtonVariant.PRIMARY,
    size: VehicleButtonSize = VehicleButtonSize.LARGE,
    content: @Composable RowScope.() -> Unit
) {
    val theme = LocalVehicleWidgetTheme.current

    val (backgroundColor, contentColor) = when (variant) {
        VehicleButtonVariant.PRIMARY -> theme.primaryColor to theme.onPrimaryColor
        VehicleButtonVariant.SECONDARY -> theme.secondaryColor to theme.onPrimaryColor
        VehicleButtonVariant.DANGER -> theme.errorColor to Color.White
    }

    val buttonHeight = when (size) {
        VehicleButtonSize.SMALL -> 56.dp
        VehicleButtonSize.MEDIUM -> 72.dp
        VehicleButtonSize.LARGE -> 88.dp
    }

    Button(
        onClick = onClick,
        modifier = modifier
            .height(buttonHeight)
            .defaultMinSize(minWidth = theme.dimensions.buttonMinWidth),
        enabled = enabled,
        shape = RoundedCornerShape(theme.shapes.buttonCornerRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor,
            disabledContainerColor = theme.surfaceColor.copy(alpha = 0.5f),
            disabledContentColor = theme.onBackgroundColor.copy(alpha = 0.3f)
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            content()
        }
    }
}

/**
 * 车辆图标按钮
 */
@Composable
fun VehicleIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: VehicleButtonVariant = VehicleButtonVariant.PRIMARY,
    size: VehicleButtonSize = VehicleButtonSize.LARGE,
    contentDescription: String? = null
) {
    VehicleButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        variant = variant,
        size = size
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(when (size) {
                VehicleButtonSize.SMALL -> 24.dp
                VehicleButtonSize.MEDIUM -> 32.dp
                VehicleButtonSize.LARGE -> 40.dp
            })
        )
    }
}

/**
 * 车辆文字按钮
 */
@Composable
fun VehicleTextButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: VehicleButtonVariant = VehicleButtonVariant.PRIMARY,
    size: VehicleButtonSize = VehicleButtonSize.LARGE
) {
    val theme = LocalVehicleWidgetTheme.current

    VehicleButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        variant = variant,
        size = size
    ) {
        Text(
            text = text,
            style = when (size) {
                VehicleButtonSize.SMALL -> theme.typography.buttonText.copy(fontSize = 16.sp)
                VehicleButtonSize.MEDIUM -> theme.typography.buttonText.copy(fontSize = 18.sp)
                VehicleButtonSize.LARGE -> theme.typography.buttonText
            }
        )
    }
}

/**
 * 车辆图标+文字按钮
 */
@Composable
fun VehicleIconTextButton(
    onClick: () -> Unit,
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: VehicleButtonVariant = VehicleButtonVariant.PRIMARY,
    size: VehicleButtonSize = VehicleButtonSize.LARGE,
    contentDescription: String? = null
) {
    val theme = LocalVehicleWidgetTheme.current

    VehicleButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        variant = variant,
        size = size
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(when (size) {
                VehicleButtonSize.SMALL -> 24.dp
                VehicleButtonSize.MEDIUM -> 28.dp
                VehicleButtonSize.LARGE -> 32.dp
            })
        )
        Text(
            text = text,
            style = when (size) {
                VehicleButtonSize.SMALL -> theme.typography.buttonText.copy(fontSize = 16.sp)
                VehicleButtonSize.MEDIUM -> theme.typography.buttonText.copy(fontSize = 18.sp)
                VehicleButtonSize.LARGE -> theme.typography.buttonText
            }
        )
    }
}

/**
 * 按钮变体枚举
 */
enum class VehicleButtonVariant {
    PRIMARY,    // 主色
    SECONDARY,  // 次要色
    DANGER      // 危险/警告
}

/**
 * 按钮尺寸枚举
 */
enum class VehicleButtonSize {
    SMALL,      // 小尺寸 (56dp)
    MEDIUM,     // 中尺寸 (72dp)
    LARGE       // 大尺寸 (88dp)
}
