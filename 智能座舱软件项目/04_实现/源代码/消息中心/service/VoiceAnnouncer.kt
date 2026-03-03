package com.longcheer.cockpit.message.service

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context

/**
 * 语音播报器
 * 
 * 使用Android TTS服务进行消息语音播报
 * 适用于驾驶场景下的消息提醒
 */
@Singleton
class VoiceAnnouncer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "VoiceAnnouncer"
    }

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val pendingUtterances = mutableListOf<String>()

    init {
        initTts()
    }

    /**
     * 播报文本
     * 
     * @param text 要播报的文本
     */
    fun announce(text: String) {
        if (text.isBlank()) return

        if (!isInitialized) {
            pendingUtterances.add(text)
            return
        }

        speak(text)
    }

    /**
     * 停止播报
     */
    fun stop() {
        tts?.stop()
    }

    /**
     * 释放资源
     */
    fun shutdown() {
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    private fun initTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                // 设置语言
                tts?.language = Locale.CHINESE
                // 设置语速
                tts?.setSpeechRate(1.0f)
                // 设置音调
                tts?.setPitch(1.0f)

                // 处理待播报队列
                pendingUtterances.forEach { speak(it) }
                pendingUtterances.clear()
            } else {
                Log.e(TAG, "TTS initialization failed")
            }
        }
    }

    private fun speak(text: String) {
        val utteranceId = UUID.randomUUID().toString()
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            tts?.speak(text, TextToSpeech.QUEUE_ADD, null, utteranceId)
        } else {
            @Suppress("DEPRECATION")
            val params = HashMap<String, String>()
            params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = utteranceId
            @Suppress("DEPRECATION")
            tts?.speak(text, TextToSpeech.QUEUE_ADD, params)
        }
    }
}
