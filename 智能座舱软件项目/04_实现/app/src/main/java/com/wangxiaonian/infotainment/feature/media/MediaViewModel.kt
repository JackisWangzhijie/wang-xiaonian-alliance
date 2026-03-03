package com.wangxiaonian.infotainment.feature.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 媒体娱乐 ViewModel
 *
 * @author 王小年联盟
 * @version 1.0
 */
@HiltViewModel
class MediaViewModel @Inject constructor(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _mediaState = MutableStateFlow(MediaState())
    val mediaState: StateFlow<MediaState> = _mediaState

    private val _playlist = MutableStateFlow<List<MediaItem>>(emptyList())
    val playlist: StateFlow<List<MediaItem>> = _playlist

    init {
        loadPlaylist()
    }

    private fun loadPlaylist() {
        viewModelScope.launch {
            // TODO: 从媒体库加载播放列表
            _playlist.value = emptyList()
        }
    }

    /**
     * 播放/暂停
     */
    fun playPause() {
        viewModelScope.launch {
            val currentState = _mediaState.value
            if (currentState.isPlaying) {
                mediaRepository.pause()
                _mediaState.value = currentState.copy(isPlaying = false)
            } else {
                currentState.currentItem?.let { mediaRepository.play(it) }
                _mediaState.value = currentState.copy(isPlaying = true)
            }
        }
    }

    /**
     * 下一首
     */
    fun next() {
        viewModelScope.launch {
            val currentIndex = _playlist.value.indexOfFirst { 
                it.id == _mediaState.value.currentItem?.id 
            }
            val nextIndex = if (currentIndex < _playlist.value.size - 1) {
                currentIndex + 1
            } else 0

            _playlist.value.getOrNull(nextIndex)?.let { nextItem ->
                _mediaState.value = _mediaState.value.copy(
                    currentItem = nextItem,
                    isPlaying = true
                )
                mediaRepository.play(nextItem)
            }
        }
    }

    /**
     * 上一首
     */
    fun previous() {
        viewModelScope.launch {
            val currentIndex = _playlist.value.indexOfFirst { 
                it.id == _mediaState.value.currentItem?.id 
            }
            val prevIndex = if (currentIndex > 0) {
                currentIndex - 1
            } else _playlist.value.size - 1

            _playlist.value.getOrNull(prevIndex)?.let { prevItem ->
                _mediaState.value = _mediaState.value.copy(
                    currentItem = prevItem,
                    isPlaying = true
                )
                mediaRepository.play(prevItem)
            }
        }
    }

    /**
     * 选择播放项
     */
    fun selectItem(item: MediaItem) {
        viewModelScope.launch {
            _mediaState.value = _mediaState.value.copy(
                currentItem = item,
                isPlaying = true
            )
            mediaRepository.play(item)
        }
    }

    /**
     * 设置播放模式
     */
    fun setPlayMode(mode: PlayMode) {
        _mediaState.value = _mediaState.value.copy(playMode = mode)
    }

    /**
     * 设置音量
     */
    fun setVolume(volume: Int) {
        viewModelScope.launch {
            val newVolume = volume.coerceIn(0, 100)
            _mediaState.value = _mediaState.value.copy(volume = newVolume)
            mediaRepository.setVolume(newVolume)
        }
    }

    /**
     * 搜索媒体
     */
    fun search(query: String) {
        viewModelScope.launch {
            // TODO: 搜索本地和在线媒体
        }
    }
}

/**
 * 媒体状态
 */
data class MediaState(
    val isPlaying: Boolean = false,
    val currentItem: MediaItem? = null,
    val playMode: PlayMode = PlayMode.SEQUENCE,
    val volume: Int = 50,
    val progress: Float = 0f,
    val duration: Long = 0L,
    val currentPosition: Long = 0L,
    val sourceType: MediaSourceType = MediaSourceType.LOCAL
)

/**
 * 媒体项
 */
data class MediaItem(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val duration: Long = 0L,
    val coverUrl: String? = null,
    val sourceType: MediaSourceType = MediaSourceType.LOCAL,
    val mediaUri: String = ""
)

/**
 * 播放模式
 */
enum class PlayMode {
    SEQUENCE,   // 顺序播放
    SHUFFLE,    // 随机播放
    REPEAT_ONE, // 单曲循环
    REPEAT_ALL  // 列表循环
}

/**
 * 媒体源类型
 */
enum class MediaSourceType {
    LOCAL,      // 本地音乐
    USB,        // USB设备
    BLUETOOTH,  // 蓝牙音乐
    ONLINE,     // 在线音乐
    RADIO       // 收音机
}

/**
 * 媒体 Repository
 */
class MediaRepository @Inject constructor() {
    
    suspend fun play(item: MediaItem) {
        // TODO: 调用 MediaSession 播放
    }
    
    suspend fun pause() {
        // TODO: 暂停播放
    }
    
    suspend fun stop() {
        // TODO: 停止播放
    }
    
    suspend fun setVolume(volume: Int) {
        // TODO: 设置音量
    }
    
    suspend fun seekTo(position: Long) {
        // TODO: 跳转进度
    }
    
    suspend fun getPlaylist(): List<MediaItem> {
        // TODO: 获取播放列表
        return emptyList()
    }
}
