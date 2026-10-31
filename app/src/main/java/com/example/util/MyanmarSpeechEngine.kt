package com.example.util

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.model.VoiceProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

sealed interface SpeechState {
    data object Idle : SpeechState
    data class Speaking(
        val currentChunk: Int,
        val totalChunks: Int,
        val currentText: String
    ) : SpeechState
    data object Paused : SpeechState
    data object Completed : SpeechState
    data class Error(val message: String) : SpeechState
}

class MyanmarSpeechEngine(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _speechState = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val speechState: StateFlow<SpeechState> = _speechState.asStateFlow()

    private var currentTextChunks: List<String> = emptyList()
    private var currentChunkIndex = 0
    private var currentPitch = 1.0f
    private var currentRate = 1.0f
    private var activeProfile: VoiceProfile = VoiceProfile.MALE_NARRATOR

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            // Attempt Myanmar locale
            val myanmarLocale = Locale.forLanguageTag("my-MM")
            val result = tts?.setLanguage(myanmarLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to default locale (TTS engine will still synthesize or play phonetically)
                tts?.language = Locale.getDefault()
                Log.w("MyanmarSpeechEngine", "Myanmar locale not fully installed, using fallback locale.")
            }
            applyVoiceSettings()
            setupUtteranceListener()
            Log.d("MyanmarSpeechEngine", "TTS initialized successfully.")
        } else {
            isInitialized = false
            _speechState.value = SpeechState.Error("Text-to-Speech initialization failed on this device.")
        }
    }

    fun setVoiceProfile(profile: VoiceProfile, userPitchOffset: Float = 0f, userSpeedOffset: Float = 0f) {
        activeProfile = profile
        currentPitch = (profile.pitch + userPitchOffset).coerceIn(0.5f, 2.0f)
        currentRate = (profile.speed + userSpeedOffset).coerceIn(0.5f, 2.5f)
        applyVoiceSettings()
    }

    fun setCustomPitchAndSpeed(pitch: Float, speed: Float) {
        currentPitch = pitch.coerceIn(0.5f, 2.0f)
        currentRate = speed.coerceIn(0.5f, 2.5f)
        applyVoiceSettings()
    }

    private fun applyVoiceSettings() {
        tts?.setPitch(currentPitch)
        tts?.setSpeechRate(currentRate)
    }

    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                val idx = utteranceId?.toIntOrNull() ?: currentChunkIndex
                val text = currentTextChunks.getOrNull(idx) ?: ""
                _speechState.value = SpeechState.Speaking(
                    currentChunk = idx + 1,
                    totalChunks = currentTextChunks.size,
                    currentText = text
                )
            }

            override fun onDone(utteranceId: String?) {
                val idx = (utteranceId?.toIntOrNull() ?: currentChunkIndex) + 1
                if (idx < currentTextChunks.size) {
                    currentChunkIndex = idx
                    speakChunk(idx)
                } else {
                    _speechState.value = SpeechState.Completed
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _speechState.value = SpeechState.Error("Audio playback error occurred.")
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                _speechState.value = SpeechState.Error("TTS error code: $errorCode")
            }
        })
    }

    fun speak(text: String, startFromBeginning: Boolean = true) {
        if (!isInitialized || tts == null) {
            _speechState.value = SpeechState.Error("Speech engine is not ready.")
            return
        }

        if (startFromBeginning || currentTextChunks.isEmpty()) {
            currentTextChunks = splitTextIntoChunks(text)
            currentChunkIndex = 0
        }

        if (currentTextChunks.isEmpty()) {
            _speechState.value = SpeechState.Idle
            return
        }

        applyVoiceSettings()
        speakChunk(currentChunkIndex)
    }

    private fun speakChunk(index: Int) {
        if (index >= currentTextChunks.size) {
            _speechState.value = SpeechState.Completed
            return
        }
        val chunk = currentTextChunks[index]
        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, index.toString())
        tts?.speak(chunk, TextToSpeech.QUEUE_FLUSH, params, index.toString())
    }

    fun pause() {
        tts?.stop()
        _speechState.value = SpeechState.Paused
    }

    fun resume() {
        if (currentChunkIndex < currentTextChunks.size) {
            speakChunk(currentChunkIndex)
        }
    }

    fun stop() {
        tts?.stop()
        currentChunkIndex = 0
        _speechState.value = SpeechState.Idle
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }

    private fun splitTextIntoChunks(text: String): List<String> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return emptyList()

        // Split by Myanmar punctuation '။' (section) or '၊' (comma) or newlines, or english periods
        val delimiters = Regex("[။\\n.?!]+")
        val rawParts = trimmed.split(delimiters)
        val chunks = mutableListOf<String>()

        for (part in rawParts) {
            val p = part.trim()
            if (p.isNotEmpty()) {
                if (p.length > 200) {
                    // Split long paragraph into sub-phrases by space or '၊'
                    val subParts = p.split(Regex("[၊,]+"))
                    for (sub in subParts) {
                        val s = sub.trim()
                        if (s.isNotEmpty()) chunks.add(s)
                    }
                } else {
                    chunks.add(p)
                }
            }
        }

        return if (chunks.isEmpty()) listOf(trimmed) else chunks
    }
}
