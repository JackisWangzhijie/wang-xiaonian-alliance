package com.wangxiaonian.infotainment.data.repository

import com.wangxiaonian.infotainment.data.local.NotificationDao
import com.wangxiaonian.infotainment.data.model.*
import com.wangxiaonian.infotainment.vehicle.RestrictionManager
import com.wangxiaonian.infotainment.vehicle.RestrictedOperation
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * NotificationRepository 单元测试
 *
 * @author 王小年联盟
 * @version 1.0
 */
@ExperimentalCoroutinesApi
class NotificationRepositoryTest {

    private lateinit var repository: NotificationRepository
    private lateinit var notificationDao: NotificationDao
    private lateinit var carApiManager: com.wangxiaonian.infotainment.vehicle.CarApiManager
    private lateinit var restrictionManager: RestrictionManager
    private lateinit var logger: com.wangxiaonian.infotainment.vehicle.Logger

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        notificationDao = mockk(relaxed = true)
        carApiManager = mockk(relaxed = true)
        restrictionManager = mockk(relaxed = true)
        logger = mockk(relaxed = true)

        // 默认允许所有操作
        every { restrictionManager.isOperationAllowed(any()) } returns true

        repository = NotificationRepository(
            notificationDao = notificationDao,
            carApiManager = carApiManager,
            restrictionManager = restrictionManager,
            logger = logger,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `getAllNotifications should return flow from dao`() = runTest {
        // Given
        val notifications = listOf(
            createTestNotification(1, "Test 1"),
            createTestNotification(2, "Test 2")
        )
        every { notificationDao.getAllNotifications() } returns flowOf(notifications)

        // When
        val result = repository.getAllNotifications().first()

        // Then
        assertEquals(2, result.size)
        assertEquals("Test 1", result[0].title)
    }

    @Test
    fun `getUnreadNotifications should return only unread notifications`() = runTest {
        // Given
        val unreadNotifications = listOf(
            createTestNotification(1, "Unread 1", isRead = false),
            createTestNotification(2, "Unread 2", isRead = false)
        )
        every { notificationDao.getUnreadNotifications() } returns flowOf(unreadNotifications)

        // When
        val result = repository.getUnreadNotifications().first()

        // Then
        assertEquals(2, result.size)
        assertFalse(result[0].isRead)
    }

    @Test
    fun `getCriticalNotifications should return only critical priority notifications`() = runTest {
        // Given
        val criticalNotifications = listOf(
            createTestNotification(1, "Critical", priority = NotificationPriority.CRITICAL)
        )
        every { 
            notificationDao.getNotificationsByPriority(NotificationPriority.CRITICAL) 
        } returns flowOf(criticalNotifications)

        // When
        val result = repository.getCriticalNotifications().first()

        // Then
        assertEquals(1, result.size)
        assertEquals(NotificationPriority.CRITICAL, result[0].priority)
    }

    @Test
    fun `insertNotification should return id when successful`() = runTest {
        // Given
        val notification = createTestNotification(0, "New Notification")
        every { notificationDao.insertNotification(notification) } returns 1L

        // When
        val result = repository.insertNotification(notification)

        // Then
        assertEquals(1L, result)
        verify { notificationDao.insertNotification(notification) }
    }

    @Test
    fun `insertNotification should return -1 when exception occurs`() = runTest {
        // Given
        val notification = createTestNotification(0, "New Notification")
        every { notificationDao.insertNotification(any()) } throws RuntimeException("DB Error")

        // When
        val result = repository.insertNotification(notification)

        // Then
        assertEquals(-1L, result)
        verify { logger.e(any(), any()) }
    }

    @Test
    fun `markAsRead should call dao when operation allowed`() = runTest {
        // Given
        every { restrictionManager.isOperationAllowed(RestrictedOperation.VIEW_NOTIFICATION) } returns true
        every { notificationDao.markAsRead(1L) } just Runs

        // When
        repository.markAsRead(1L)
        advanceUntilIdle()

        // Then
        verify { notificationDao.markAsRead(1L) }
    }

    @Test
    fun `markAsRead should not call dao when operation not allowed`() = runTest {
        // Given
        every { restrictionManager.isOperationAllowed(RestrictedOperation.VIEW_NOTIFICATION) } returns false

        // When
        repository.markAsRead(1L)
        advanceUntilIdle()

        // Then
        verify(exactly = 0) { notificationDao.markAsRead(any()) }
        verify { logger.w(any()) }
    }

    @Test
    fun `dismissNotification should call dao when operation allowed`() = runTest {
        // Given
        every { restrictionManager.isOperationAllowed(RestrictedOperation.DISMISS_NOTIFICATION) } returns true
        every { notificationDao.deleteNotification(1L) } just Runs

        // When
        repository.dismissNotification(1L)
        advanceUntilIdle()

        // Then
        verify { notificationDao.deleteNotification(1L) }
    }

    @Test
    fun `dismissNotification should not call dao when operation not allowed`() = runTest {
        // Given
        every { restrictionManager.isOperationAllowed(RestrictedOperation.DISMISS_NOTIFICATION) } returns false

        // When
        repository.dismissNotification(1L)
        advanceUntilIdle()

        // Then
        verify(exactly = 0) { notificationDao.deleteNotification(any()) }
    }

    @Test
    fun `clearReadNotifications should return count of deleted notifications`() = runTest {
        // Given
        every { notificationDao.deleteReadNotifications() } returns 5

        // When
        repository.clearReadNotifications()
        advanceUntilIdle()

        // Then
        verify { notificationDao.deleteReadNotifications() }
    }

    @Test
    fun `getUnreadCount should return flow from dao`() = runTest {
        // Given
        every { notificationDao.getUnreadCount() } returns flowOf(3)

        // When
        val result = repository.getUnreadCount().first()

        // Then
        assertEquals(3, result)
    }

    private fun createTestNotification(
        id: Long,
        title: String,
        isRead: Boolean = false,
        priority: NotificationPriority = NotificationPriority.NORMAL
    ): NotificationEntity {
        return NotificationEntity(
            id = id,
            title = title,
            content = "Test content",
            category = NotificationCategory.GENERAL,
            priority = priority,
            source = "Test",
            isRead = isRead
        )
    }
}
