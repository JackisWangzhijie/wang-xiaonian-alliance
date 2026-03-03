package com.wangxiaonian.infotainment.feature.hvac

import com.wangxiaonian.infotainment.vehicle.VehiclePropertyHelper
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * HvacViewModel 单元测试
 *
 * @author 王小年联盟
 * @version 1.0
 */
@ExperimentalCoroutinesApi
class HvacViewModelTest {

    private lateinit var viewModel: HvacViewModel
    private lateinit var vehiclePropertyHelper: VehiclePropertyHelper

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        vehiclePropertyHelper = mockk(relaxed = true)
        viewModel = HvacViewModel(vehiclePropertyHelper)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should have default values`() = runTest {
        // Then
        val state = viewModel.hvacState.first()
        assertEquals(22.0f, state.driverTemp, 0.1f)
        assertEquals(22.0f, state.passengerTemp, 0.1f)
        assertEquals(3, state.fanSpeed)
        assertTrue(state.isAcOn)
        assertFalse(state.isRecirculationOn)
        assertEquals(FanDirection.FACE, state.fanDirection)
    }

    @Test
    fun `setDriverTemperature should update state`() = runTest {
        // When
        viewModel.setDriverTemperature(25.0f)
        advanceUntilIdle()

        // Then
        val state = viewModel.hvacState.first()
        assertEquals(25.0f, state.driverTemp, 0.1f)
    }

    @Test
    fun `setPassengerTemperature should update state`() = runTest {
        // When
        viewModel.setPassengerTemperature(20.0f)
        advanceUntilIdle()

        // Then
        val state = viewModel.hvacState.first()
        assertEquals(20.0f, state.passengerTemp, 0.1f)
    }

    @Test
    fun `setFanSpeed should update state within valid range`() = runTest {
        // When - 设置有效值
        viewModel.setFanSpeed(5)
        advanceUntilIdle()

        // Then
        var state = viewModel.hvacState.first()
        assertEquals(5, state.fanSpeed)

        // When - 设置超过最大值
        viewModel.setFanSpeed(10)
        advanceUntilIdle()

        // Then - 应该被限制在 7
        state = viewModel.hvacState.first()
        assertEquals(7, state.fanSpeed)

        // When - 设置小于最小值
        viewModel.setFanSpeed(-1)
        advanceUntilIdle()

        // Then - 应该被限制在 0
        state = viewModel.hvacState.first()
        assertEquals(0, state.fanSpeed)
    }

    @Test
    fun `toggleAc should toggle AC state`() = runTest {
        // Given - 初始状态 AC 是开启的
        var state = viewModel.hvacState.first()
        assertTrue(state.isAcOn)

        // When
        viewModel.toggleAc()
        advanceUntilIdle()

        // Then
        state = viewModel.hvacState.first()
        assertFalse(state.isAcOn)

        // When - 再次切换
        viewModel.toggleAc()
        advanceUntilIdle()

        // Then
        state = viewModel.hvacState.first()
        assertTrue(state.isAcOn)
    }

    @Test
    fun `toggleRecirculation should toggle recirculation state`() = runTest {
        // Given - 初始状态是外循环
        var state = viewModel.hvacState.first()
        assertFalse(state.isRecirculationOn)

        // When
        viewModel.toggleRecirculation()
        advanceUntilIdle()

        // Then
        state = viewModel.hvacState.first()
        assertTrue(state.isRecirculationOn)
    }

    @Test
    fun `setFanDirection should update state`() = runTest {
        // Given - 初始是 FACE
        var state = viewModel.hvacState.first()
        assertEquals(FanDirection.FACE, state.fanDirection)

        // When
        viewModel.setFanDirection(FanDirection.FLOOR)
        advanceUntilIdle()

        // Then
        state = viewModel.hvacState.first()
        assertEquals(FanDirection.FLOOR, state.fanDirection)

        // When - 切换到除霜
        viewModel.setFanDirection(FanDirection.DEFROST)
        advanceUntilIdle()

        // Then
        state = viewModel.hvacState.first()
        assertEquals(FanDirection.DEFROST, state.fanDirection)
    }

    @Test
    fun `toggleDriverSeatHeat should cycle through levels`() = runTest {
        // Given - 初始是 0
        var state = viewModel.hvacState.first()
        assertEquals(0, state.driverSeatHeatLevel)

        // When - 第一次点击
        viewModel.toggleDriverSeatHeat()
        advanceUntilIdle()

        // Then - 应该变成 1
        state = viewModel.hvacState.first()
        assertEquals(1, state.driverSeatHeatLevel)

        // When - 连续点击 3 次
        viewModel.toggleDriverSeatHeat()
        viewModel.toggleDriverSeatHeat()
        viewModel.toggleDriverSeatHeat()
        advanceUntilIdle()

        // Then - 应该循环回 0
        state = viewModel.hvacState.first()
        assertEquals(0, state.driverSeatHeatLevel)
    }

    @Test
    fun `togglePassengerSeatHeat should cycle through levels`() = runTest {
        // Given - 初始是 0
        var state = viewModel.hvacState.first()
        assertEquals(0, state.passengerSeatHeatLevel)

        // When - 点击 2 次
        viewModel.togglePassengerSeatHeat()
        viewModel.togglePassengerSeatHeat()
        advanceUntilIdle()

        // Then - 应该是 2
        state = viewModel.hvacState.first()
        assertEquals(2, state.passengerSeatHeatLevel)
    }

    @Test
    fun `HvacState should hold all values correctly`() {
        // Given
        val state = HvacState(
            driverTemp = 24.0f,
            passengerTemp = 23.0f,
            fanSpeed = 4,
            isAcOn = false,
            isRecirculationOn = true,
            fanDirection = FanDirection.FLOOR_DEFROST,
            driverSeatHeatLevel = 2,
            passengerSeatHeatLevel = 1,
            isAutoMode = true
        )

        // Then
        assertEquals(24.0f, state.driverTemp, 0.1f)
        assertEquals(23.0f, state.passengerTemp, 0.1f)
        assertEquals(4, state.fanSpeed)
        assertFalse(state.isAcOn)
        assertTrue(state.isRecirculationOn)
        assertEquals(FanDirection.FLOOR_DEFROST, state.fanDirection)
        assertEquals(2, state.driverSeatHeatLevel)
        assertEquals(1, state.passengerSeatHeatLevel)
        assertTrue(state.isAutoMode)
    }

    @Test
    fun `FanDirection enum should have all directions`() {
        // Given & Then
        val directions = FanDirection.values()
        assertEquals(5, directions.size)
        assertTrue(directions.contains(FanDirection.FACE))
        assertTrue(directions.contains(FanDirection.FLOOR))
        assertTrue(directions.contains(FanDirection.DEFROST))
        assertTrue(directions.contains(FanDirection.FACE_FLOOR))
        assertTrue(directions.contains(FanDirection.FLOOR_DEFROST))
    }
}
