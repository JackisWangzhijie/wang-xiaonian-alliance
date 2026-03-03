package com.wangxiaonian.infotainment.vehicle

import android.content.Context
import androidx.car.hardware.CarPropertyValue
import androidx.car.hardware.property.CarPropertyManager
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
 * VehiclePropertyHelper 单元测试
 *
 * @author 王小年联盟
 * @version 1.0
 */
@ExperimentalCoroutinesApi
class VehiclePropertyHelperTest {

    private lateinit var vehiclePropertyHelper: VehiclePropertyHelper
    private lateinit var carApiManager: CarApiManager
    private lateinit var carPropertyManager: CarPropertyManager
    private lateinit var logger: Logger

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        carApiManager = mockk(relaxed = true)
        carPropertyManager = mockk(relaxed = true)
        logger = mockk(relaxed = true)

        every { carApiManager.getPropertyManager() } returns carPropertyManager

        vehiclePropertyHelper = VehiclePropertyHelper(
            carApiManager = carApiManager,
            logger = logger,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getProperty should return value when property exists`() = runTest {
        // Given
        val propertyId = VehiclePropertyIds.PERF_VEHICLE_SPEED
        val expectedValue = 30.0f
        val carPropertyValue = mockk<CarPropertyValue<Float>>()
        every { carPropertyValue.value } returns expectedValue
        every { 
            carPropertyManager.getProperty<Float>(propertyId) 
        } returns carPropertyValue

        // When
        val result = vehiclePropertyHelper.getProperty<Float>(propertyId)

        // Then
        assertEquals(expectedValue, result)
    }

    @Test
    fun `getProperty should return null when property manager is null`() = runTest {
        // Given
        every { carApiManager.getPropertyManager() } returns null

        // When
        val result = vehiclePropertyHelper.getProperty<Float>(VehiclePropertyIds.PERF_VEHICLE_SPEED)

        // Then
        assertNull(result)
    }

    @Test
    fun `getProperty should return null when exception occurs`() = runTest {
        // Given
        every { 
            carPropertyManager.getProperty<Float>(any()) 
        } throws RuntimeException("Test exception")

        // When
        val result = vehiclePropertyHelper.getProperty<Float>(VehiclePropertyIds.PERF_VEHICLE_SPEED)

        // Then
        assertNull(result)
        verify { logger.e(any(), any()) }
    }

    @Test
    fun `setProperty should return true when successful`() = runTest {
        // Given
        val propertyId = VehiclePropertyIds.HVAC_TEMPERATURE_SET
        val value = 22.0f
        every { carPropertyManager.setProperty(propertyId, value) } just Runs

        // When
        val result = vehiclePropertyHelper.setProperty(propertyId, value)

        // Then
        assertTrue(result)
        verify { carPropertyManager.setProperty(propertyId, value) }
    }

    @Test
    fun `setProperty should return false when property manager is null`() = runTest {
        // Given
        every { carApiManager.getPropertyManager() } returns null

        // When
        val result = vehiclePropertyHelper.setProperty(VehiclePropertyIds.HVAC_TEMPERATURE_SET, 22.0f)

        // Then
        assertFalse(result)
    }

    @Test
    fun `getSpeed should convert mps to kmh`() = runTest {
        // Given
        val speedMps = 10.0f // m/s
        val expectedKmh = 36.0f // km/h
        val carPropertyValue = mockk<CarPropertyValue<Float>>()
        every { carPropertyValue.value } returns speedMps
        every { 
            carPropertyManager.getProperty<Float>(VehiclePropertyIds.PERF_VEHICLE_SPEED) 
        } returns carPropertyValue

        // When
        val result = vehiclePropertyHelper.getSpeed()

        // Then
        assertEquals(expectedKmh, result, 0.1f)
    }

    @Test
    fun `getGear should return gear value`() = runTest {
        // Given
        val expectedGear = VehicleGear.GEAR_DRIVE
        val carPropertyValue = mockk<CarPropertyValue<Int>>()
        every { carPropertyValue.value } returns expectedGear
        every { 
            carPropertyManager.getProperty<Int>(VehiclePropertyIds.GEAR_SELECTION) 
        } returns carPropertyValue

        // When
        val result = vehiclePropertyHelper.getGear()

        // Then
        assertEquals(expectedGear, result)
    }

    @Test
    fun `isParked should return true when gear is park`() = runTest {
        // Given
        val carPropertyValue = mockk<CarPropertyValue<Int>>()
        every { carPropertyValue.value } returns VehicleGear.GEAR_PARK
        every { 
            carPropertyManager.getProperty<Int>(VehiclePropertyIds.GEAR_SELECTION) 
        } returns carPropertyValue

        // When
        val result = vehiclePropertyHelper.isParked()

        // Then
        assertTrue(result)
    }

    @Test
    fun `isParked should return false when gear is drive`() = runTest {
        // Given
        val carPropertyValue = mockk<CarPropertyValue<Int>>()
        every { carPropertyValue.value } returns VehicleGear.GEAR_DRIVE
        every { 
            carPropertyManager.getProperty<Int>(VehiclePropertyIds.GEAR_SELECTION) 
        } returns carPropertyValue

        // When
        val result = vehiclePropertyHelper.isParked()

        // Then
        assertFalse(result)
    }
}
