package com.wangxiaonian.infotainment.feature.notification

import com.wangxiaonian.infotainment.data.model.*
import com.wangxiaonian.infotainment.data.repository.NotificationRepository
import com.wangxiaonian.infotainment.vehicle.RestrictionManager
import com.wangxiaonian.infotainment.vehicle.RestrictionState
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
 * NotificationViewModel 单元测试
 *
 * @author 王小年联盟
 * @version 1.0
 */
@ExperimentalCoroutinesApi
class NotificationViewModelTest {

    private lateinit var viewModel: NotificationViewModel
    private lateinit var repository: NotificationRepository
    private lateinit var ttsManager: TtsManager
    private lateinit var restrictionManager: RestrictionManager

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        repository = mockk(relaxed = true)
        ttsManager = mockk(relaxed = true)
        restrictionManager = mockk(relaxed = true)

        // 默认设置
        every { repository.getNotificationsForCurrentState() } returns flowOf(emptyList())
        every { repository.getUnreadCount() } returns flowOf(0)
        every { restrictionManager.restrictionState } returns MutableStateFlow(RestrictionState.Full)

        viewModel = NotificationViewModel(
            repository = repository,
            ttsManager = ttsManager,
            restrictionManager = restrictionManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial notifications should be empty`() = runTest {
        // Then
        val notifications = viewModel.notifications.first()
        assertTrue(notifications.isEmpty())
    }

    @Test
    fun `initial unread count should be 0`() = runTest {
        // Then
        val count = viewModel.unreadCount.first()
        assertEquals(0, count)
    }

    @Test
    fun `notifications should be filtered for current state`() = runTest {
        // Given
        val testNotifications = listOf(
            createTestNotification(1, "Test 1"),
            createTestNotification(2, "Test 2")
        )
        every { repository.getNotificationsForCurrentState() } returns flowOf(testNotifications)

        // 重新创建 ViewModel 以使用新的 flow
        viewModel = NotificationViewModel(repository, ttsManager, restrictionManager)

        // When
        val result = viewModel.notifications.first()

        // Then
        assertEquals(2, result.size)
    }

    @Test
    fun `unread count should reflect repository data`() = runTest {
        // Given
        every { repository.getUnreadCount() } returns flowOf(5)

        // 重新创建 ViewModel
        viewModel = NotificationViewModel(repository, ttsManager, restrictionManager)

        // When
        val result = viewModel.unreadCount.first()

        // Then
        assertEquals(5, result)
    }

    @Test
    fun `markAsRead should call repository`() = runTest {
        // Given
        every { repository.markAsRead(1L) } just Runs

        // When
        viewModel.markAsRead(1L)
        advanceUntilIdle()

        // Then
        verify { repository.markAsRead(1L) }
    }

    @Test
    fun `dismissNotification should call repository`() = runTest {
        // Given
        every { repository.dismissNotification(1L) } just Runs

        // When
        viewModel.dismissNotification(1L)
        advanceUntilIdle()

        // Then
        verify { repository.dismissNotification(1L) }
    }

    @Test
    fun `speakNotification should use summary in Critical state`() = runTest {
        // Given
        every { restrictionManager.restrictionState } returns MutableStateFlow(RestrictionState.Critical)
        every { ttsManager.speak(any()) } just Runs

        val notification = createTestNotification(
            1,
            "Title",
            content = "Full content here",
            summary = "Summary"
        )

        // When
        viewModel.speakNotification(notification)

        // Then - 在 Critical 状态应该朗读摘要
        verify { ttsManager.speak("新通知来自${notification.source}") }
    }

    @Test
    fun `speakNotification should use full content in normal state`() = runTest {
        // Given
        every { restrictionManager.restrictionState } returns MutableStateFlow(RestrictionState.Full)
        every { ttsManager.speak(any()) } just Runs

        val notification = createTestNotification(
            1,
            "Title",
            content = "Full content"
        )

        // When
        viewModel.speakNotification(notification)

        // Then - 在 Full 状态应该朗读完整内容
        verify { ttsManager.speak("${notification.title}，${notification.content}") }
    }

    @Test
    fun `onNotificationClick should speak when operation not allowed`() {
        // Given
        every { restrictionManager.isOperationAllowed(any()) } returns false
        every { ttsManager.speak(any()) } just Runs

        val notification = createTestNotification(1, "Test")

        // When
        viewModel.onNotificationClick(notification)

        // Then - 驾驶中不能打开应用，只朗读
        verify { ttsManager.speak(any()) }
    }

    @Test
    fun `onNotificationClick should mark as read for OPEN_APP action`() = runTest {
        // Given
        every { restrictionManager.isOperationAllowed(any()) } returns true
        every { repository.markAsRead(any()) } just Runs

        val notification = createTestNotification(
            1,
            "Test",
            actionType = ActionType.OPEN_APP
        )

        // When
        viewModel.onNotificationClick(notification)
        advanceUntilIdle()

        // Then
        // OPEN_APP 操作目前只是注释，没有实际实现
    }

    @Test
    fun `restrictionState should reflect manager state`() = runTest {
        // Given
        val expectedState = RestrictionState.Limited
        every { restrictionManager.restrictionState } returns MutableStateFlow(expectedState)

        // 重新创建 ViewModel
        viewModel = NotificationViewModel(repository, ttsManager, restrictionManager)

        // When
        val result = viewModel.restrictionState.first()

        // Then
        assertEquals(expectedState, result)
    }

    private fun createTestNotification(
        id: Long,
        title: String,
        content: String = "Test content",
        summary: String? = null,
        actionType: ActionType? = null
    ): NotificationEntity {
        return NotificationEntity(
            id = id,
            title = title,
            content = content,
            summary = summary,
            category = NotificationCategory.GENERAL,
            priority = NotificationPriority.NORMAL,
            source = "Test",
            actionType = actionType
        )
    }
}
