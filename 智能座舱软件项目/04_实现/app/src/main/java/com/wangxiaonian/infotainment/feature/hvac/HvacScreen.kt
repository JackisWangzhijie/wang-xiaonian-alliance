package com.wangxiaonian.infotainment.feature.hvac

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.wangxiaonian.infotainment.presentation.component.CarCard

/**
 * HVAC 空调控制界面
 *
 * @author 王小年联盟
 * @version 1.0
 */
@Composable
fun HvacScreen(
    viewModel: HvacViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.hvacState.collectAsState()

    Scaffold(
        topBar = {
            HvacTopBar(onBack = onBack)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // 温度显示区
            TemperatureDisplay(
                driverTemp = state.driverTemp,
                passengerTemp = state.passengerTemp,
                onDriverTempChange = { viewModel.setDriverTemperature(it) },
                onPassengerTempChange = { viewModel.setPassengerTemperature(it) }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 控制按钮区
            ControlButtons(
                isAcOn = state.isAcOn,
                isRecirculationOn = state.isRecirculationOn,
                fanDirection = state.fanDirection,
                onAcToggle = { viewModel.toggleAc() },
                onRecirculationToggle = { viewModel.toggleRecirculation() },
                onFanDirectionChange = { viewModel.setFanDirection(it) }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 风扇速度
            FanSpeedControl(
                speed = state.fanSpeed,
                onSpeedChange = { viewModel.setFanSpeed(it) }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 座椅加热
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SeatHeatControl(
                    title = "驾驶员",
                    level = state.driverSeatHeatLevel,
                    onToggle = { viewModel.toggleDriverSeatHeat() }
                )

                SeatHeatControl(
                    title = "副驾驶员",
                    level = state.passengerSeatHeatLevel,
                    onToggle = { viewModel.togglePassengerSeatHeat() }
                )
            }
        }
    }
}

@Composable
private fun HvacTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = "空调控制",
            style = MaterialTheme.typography.headlineMedium
        )
    }
}

@Composable
private fun TemperatureDisplay(
    driverTemp: Float,
    passengerTemp: Float,
    onDriverTempChange: (Float) -> Unit,
    onPassengerTempChange: (Float) -> Unit
) {
    CarCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // 驾驶员侧温度
            TemperatureControl(
                label = "驾驶员",
                temperature = driverTemp,
                onIncrease = { onDriverTempChange(driverTemp + 0.5f) },
                onDecrease = { onDriverTempChange(driverTemp - 0.5f) }
            )

            // 分隔线
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(120.dp)
                    .background(MaterialTheme.colorScheme.outline)
            )

            // 副驾驶员侧温度
            TemperatureControl(
                label = "副驾驶员",
                temperature = passengerTemp,
                onIncrease = { onPassengerTempChange(passengerTemp + 0.5f) },
                onDecrease = { onPassengerTempChange(passengerTemp - 0.5f) }
            )
        }
    }
}

@Composable
private fun TemperatureControl(
    label: String,
    temperature: Float,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        IconButton(
            onClick = onIncrease,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "升温",
                modifier = Modifier.size(32.dp)
            )
        }

        Text(
            text = "${temperature.toInt()}°C",
            style = MaterialTheme.typography.displaySmall
        )

        IconButton(
            onClick = onDecrease,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "降温",
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun ControlButtons(
    isAcOn: Boolean,
    isRecirculationOn: Boolean,
    fanDirection: FanDirection,
    onAcToggle: () -> Unit,
    onRecirculationToggle: () -> Unit,
    onFanDirectionChange: (FanDirection) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        HvacControlButton(
            icon = Icons.Default.AcUnit,
            label = "AC",
            isActive = isAcOn,
            onClick = onAcToggle
        )

        HvacControlButton(
            icon = Icons.Default.Sync,
            label = "内循环",
            isActive = isRecirculationOn,
            onClick = onRecirculationToggle
        )

        HvacControlButton(
            icon = Icons.Default.Face,
            label = "面部",
            isActive = fanDirection == FanDirection.FACE,
            onClick = { onFanDirectionChange(FanDirection.FACE) }
        )

        HvacControlButton(
            icon = Icons.Default.Air,
            label = "脚部",
            isActive = fanDirection == FanDirection.FLOOR,
            onClick = { onFanDirectionChange(FanDirection.FLOOR) }
        )

        HvacControlButton(
            icon = Icons.Default.Waves,
            label = "除霜",
            isActive = fanDirection == FanDirection.DEFROST,
            onClick = { onFanDirectionChange(FanDirection.DEFROST) }
        )
    }
}

@Composable
private fun HvacControlButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier.size(76.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (isActive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isActive) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FanSpeedControl(
    speed: Int,
    onSpeedChange: (Int) -> Unit
) {
    CarCard {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "风量",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Air,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in 1..7) {
                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .height(48.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(
                                    if (i <= speed) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { onSpeedChange(i) }
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.Air,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

@Composable
private fun SeatHeatControl(
    title: String,
    level: Int,
    onToggle: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledIconButton(
            onClick = onToggle,
            modifier = Modifier.size(76.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = when (level) {
                    0 -> MaterialTheme.colorScheme.surfaceVariant
                    1 -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
                    2 -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)
                    else -> MaterialTheme.colorScheme.tertiary
                }
            )
        ) {
            Icon(
                imageVector = Icons.Default.EventSeat,
                contentDescription = title,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge
        )

        if (level > 0) {
            Text(
                text = "${level}档",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}
