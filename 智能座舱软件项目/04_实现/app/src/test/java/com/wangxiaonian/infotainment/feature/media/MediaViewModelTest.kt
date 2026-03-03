package com.wangxiaonian.infotainment.feature.media

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
 * MediaViewModel 单元测试
 *
 * @author 王小年联盟
 * @version 1.0
 */
@ExperimentalCoroutinesApi
class MediaViewModelTest {

    private lateinit var viewModel: MediaViewModel
    private lateinit var mediaRepository: MediaRepository

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mediaRepository = mockk(relaxed = true)
        viewModel = MediaViewModel(mediaRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should have default values`() = runTest {
        // Then
        val state = viewModel.mediaState.first()
        assertFalse(state.isPlaying)
        assertNull(state.currentItem)
        assertEquals(PlayMode.SEQUENCE, state.playMode)
        assertEquals(50, state.volume)
        assertEquals(MediaSourceType.LOCAL, state.sourceType)
    }

    @Test
    fun `setVolume should update state within valid range`() = runTest {
        // When - 设置有效值
        viewModel.setVolume(75)
        advanceUntilIdle()

        // Then
        var state = viewModel.mediaState.first()
        assertEquals(75, state.volume)

        // When - 设置超过最大值
        viewModel.setVolume(150)
        advanceUntilIdle()

        // Then - 应该被限制在 100
        state = viewModel.mediaState.first()
        assertEquals(100, state.volume)

        // When - 设置小于最小值
        viewModel.setVolume(-10)
        advanceUntilIdle()

        // Then - 应该被限制在 0
        state = viewModel.mediaState.first()
        assertEquals(0, state.volume)
    }

    @Test
    fun `setPlayMode should update state`() = runTest {
        // Given - 初始是 SEQUENCE
        var state = viewModel.mediaState.first()
        assertEquals(PlayMode.SEQUENCE, state.playMode)

        // When
        viewModel.setPlayMode(PlayMode.SHUFFLE)
        
        // Then
        state = viewModel.mediaState.first()
        assertEquals(PlayMode.SHUFFLE, state.playMode)

        // When - 切换到单曲循环
        viewModel.setPlayMode(PlayMode.REPEAT_ONE)
        
        // Then
        state = viewModel.mediaState.first()
        assertEquals(PlayMode.REPEAT_ONE, state.playMode)
    }

    @Test
    fun `MediaItem should hold correct data`() {
        // Given
        val item = MediaItem(
            id = "1",
            title = "Test Song",
            artist = "Test Artist",
            album = "Test Album",
            duration = 180000L,
            coverUrl = "https://example.com/cover.jpg",
            sourceType = MediaSourceType.LOCAL,
            mediaUri = "file:///music/test.mp3"
        )

        // Then
        assertEquals("1", item.id)
        assertEquals("Test Song", item.title)
        assertEquals("Test Artist", item.artist)
        assertEquals("Test Album", item.album)
        assertEquals(180000L, item.duration)
        assertEquals("https://example.com/cover.jpg", item.coverUrl)
        assertEquals(MediaSourceType.LOCAL, item.sourceType)
        assertEquals("file:///music/test.mp3", item.mediaUri)
    }

    @Test
    fun `MediaState should hold all values correctly`() {
        // Given
        val item = MediaItem(
            id = "1",
            title = "Song",
            artist = "Artist",
            sourceType = MediaSourceType.BLUETOOTH
        )
        
        val state = MediaState(
            isPlaying = true,
            currentItem = item,
            playMode = PlayMode.REPEAT_ALL,
            volume = 80,
            progress = 0.5f,
            duration = 200000L,
            currentPosition = 100000L,
            sourceType = MediaSourceType.BLUETOOTH
        )

        // Then
        assertTrue(state.isPlaying)
        assertEquals(item, state.currentItem)
        assertEquals(PlayMode.REPEAT_ALL, state.playMode)
        assertEquals(80, state.volume)
        assertEquals(0.5f, state.progress, 0.01f)
        assertEquals(200000L, state.duration)
        assertEquals(100000L, state.currentPosition)
        assertEquals(MediaSourceType.BLUETOOTH, state.sourceType)
    }

    @Test
    fun `PlayMode enum should have all modes`() {
        // Given & Then
        val modes = PlayMode.values()
        assertEquals(4, modes.size)
        assertTrue(modes.contains(PlayMode.SEQUENCE))
        assertTrue(modes.contains(PlayMode.SHUFFLE))
        assertTrue(modes.contains(PlayMode.REPEAT_ONE))
        assertTrue(modes.contains(PlayMode.REPEAT_ALL))
    }

    @Test
    fun `MediaSourceType enum should have all types`() {
        // Given & Then
        val types = MediaSourceType.values()
        assertEquals(5, types.size)
        assertTrue(types.contains(MediaSourceType.LOCAL))
        assertTrue(types.contains(MediaSourceType.USB))
        assertTrue(types.contains(MediaSourceType.BLUETOOTH))
        assertTrue(types.contains(MediaSourceType.ONLINE))
        assertTrue(types.contains(MediaSourceType.RADIO))
    }

    @Test
    fun `selectItem should update current item and set playing`() = runTest {
        // Given
        val item = MediaItem(
            id = "1",
            title = "Selected Song",
            artist = "Artist",
            sourceType = MediaSourceType.LOCAL
        )
        every { mediaRepository.play(any()) } just Runs

        // When
        viewModel.selectItem(item)
        advanceUntilIdle()

        // Then
        val state = viewModel.mediaState.first()
        assertEquals(item, state.currentItem)
        assertTrue(state.isPlaying)
        verify { mediaRepository.play(item) }
    }
}
