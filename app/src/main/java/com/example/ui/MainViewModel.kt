package com.example.ui

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.FirebaseAiService
import com.example.ai.RecapResult
import com.example.data.local.AppDatabase
import com.example.data.local.LocalRecapEntity
import com.example.data.model.RecapItem
import com.example.data.model.SrtCue
import com.example.data.model.VoiceProfile
import com.example.data.repository.RecapRepository
import com.example.util.MyanmarSpeechEngine
import com.example.util.SpeechState
import com.example.util.SrtParser
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID

sealed interface ActionState {
    data object Idle : ActionState
    data class Loading(val message: String) : ActionState
    data class Success(val message: String) : ActionState
    data class Error(val message: String) : ActionState
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val aiService = FirebaseAiService()
    private val repository = RecapRepository(application)
    private val localDb = AppDatabase.getDatabase(application)
    val speechEngine = MyanmarSpeechEngine(application)

    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState: StateFlow<ActionState> = _actionState.asStateFlow()

    // Active selected video and SRT
    var selectedVideoUri by mutableStateOf<Uri?>(null)
        private set
    var rawSrtText by mutableStateOf("")
        private set
    var parsedCues by mutableStateOf<List<SrtCue>>(emptyList())
        private set

    // Current active Recap
    var activeRecap by mutableStateOf<RecapItem?>(null)
        private set

    // Voice preferences
    var activeVoiceProfile by mutableStateOf(VoiceProfile.MALE_NARRATOR)
        private set
    var voicePitch by mutableStateOf(1.0f)
        private set
    var voiceSpeed by mutableStateOf(1.0f)
        private set

    // Dark mode setting
    var isDarkMode by mutableStateOf(true)
        private set

    // Text translation tab
    var inputTranslateText by mutableStateOf("")
    var translatedMyanmarResult by mutableStateOf("")

    // Speech state from engine
    val speechState: StateFlow<SpeechState> = speechEngine.speechState

    // Observed recaps from Firestore, with fallback mapping
    val savedRecaps: StateFlow<List<RecapItem>> = repository.observeRecaps()
        .catch { err ->
            Log.w("MainViewModel", "Firestore stream error, loading local offline recaps", err)
            // Emit from local DB if Firestore is offline
            localDb.recapDao().getAllRecaps().map { entities -> entities.map { it.toRecapItem() } }.collect {
                emit(it)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    init {
        // Pre-load default samples so user can test immediately
        loadDefaultSample()
    }

    private fun loadDefaultSample() {
        val sampleSrt = """
            1
            00:00:01,000 --> 00:00:05,200
            Artificial Intelligence is revolutionizing science and technology today.

            2
            00:00:05,300 --> 00:00:09,800
            With modern language models, computers can understand, translate, and speak human languages naturally.

            3
            00:00:10,000 --> 00:00:15,500
            In this video, we explore how Myanmar audio and subtitle recaps make content accessible to everyone.
        """.trimIndent()
        setSrtContent(sampleSrt)
    }

    fun toggleDarkMode() {
        isDarkMode = !isDarkMode
    }

    fun setVideoUri(uri: Uri?) {
        selectedVideoUri = uri
    }

    fun setSrtContent(content: String) {
        rawSrtText = content
        parsedCues = SrtParser.parse(content)
    }

    fun loadSrtFromUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val content = reader.readText()
                reader.close()
                setSrtContent(content)
                _actionState.value = ActionState.Success("SRT ဖိုင်ကို အောင်မြင်စွာ ထည့်သွင်းပြီးပါပြီ")
            } catch (e: Exception) {
                _actionState.value = ActionState.Error("SRT ဖိုင် ဖတ်၍မရပါ: ${e.localizedMessage}")
            }
        }
    }

    fun updateVoiceSettings(profile: VoiceProfile, pitch: Float, speed: Float) {
        activeVoiceProfile = profile
        voicePitch = pitch
        voiceSpeed = speed
        speechEngine.setVoiceProfile(profile)
        speechEngine.setCustomPitchAndSpeed(pitch, speed)
    }

    fun generateVideoOrSrtRecap(customText: String? = null) {
        val content = customText ?: rawSrtText
        if (content.isBlank()) {
            _actionState.value = ActionState.Error("Recap ပြုလုပ်ရန် စာသား သို့မဟုတ် SRT ဖိုင် ထည့်သွင်းပါ")
            return
        }

        viewModelScope.launch {
            _actionState.value = ActionState.Loading("မြန်မာအသံနှင့် စာတန်းထိုး Recap ပြုလုပ်နေပါသည်...")
            val result = aiService.generateRecap(content, sourceType = if (selectedVideoUri != null) "video" else "srt")

            result.onSuccess { recap ->
                val newCues = if (recap.srtSubtitles.isNotBlank()) {
                    SrtParser.parse(recap.srtSubtitles)
                } else {
                    parsedCues
                }
                parsedCues = newCues

                val recapItem = RecapItem(
                    id = UUID.randomUUID().toString(),
                    userId = Firebase.auth.currentUser?.uid ?: "",
                    title = recap.title,
                    sourceType = if (selectedVideoUri != null) "video" else "srt",
                    originalText = content,
                    myanmarSummary = recap.summary,
                    myanmarScript = recap.voiceScript,
                    subtitlesSrt = recap.srtSubtitles,
                    voiceTone = activeVoiceProfile.id,
                    speechSpeed = voiceSpeed.toDouble(),
                    speechPitch = voicePitch.toDouble(),
                    durationSeconds = newCues.lastOrNull()?.let { (it.endTimeMs / 1000).toInt() } ?: 60
                )
                activeRecap = recapItem

                // Save locally for offline use
                localDb.recapDao().insertRecap(LocalRecapEntity.fromRecapItem(recapItem))

                // Save to Firestore if authenticated
                if (Firebase.auth.currentUser != null) {
                    repository.saveRecap(recapItem)
                }

                _actionState.value = ActionState.Success("Recap အောင်မြင်စွာ ဖန်တီးပြီးပါပြီ!")
            }.onFailure { err ->
                _actionState.value = ActionState.Error(err.localizedMessage ?: "Recap မအောင်မြင်ပါ")
            }
        }
    }

    fun recapFromUrl(url: String, platform: String) {
        viewModelScope.launch {
            _actionState.value = ActionState.Loading("$platform လင့်ခ်မှ အချက်အလက်များ ရှာဖွေပြီး မြန်မာ Recap ပြုလုပ်နေပါသည်...")
            val result = aiService.recapSocialLink(url, platform)

            result.onSuccess { recap ->
                val cues = if (recap.srtSubtitles.isNotBlank()) SrtParser.parse(recap.srtSubtitles) else emptyList()
                parsedCues = cues

                val recapItem = RecapItem(
                    id = UUID.randomUUID().toString(),
                    userId = Firebase.auth.currentUser?.uid ?: "",
                    title = recap.title,
                    sourceType = if (platform.lowercase().contains("youtube")) "youtube" else "tiktok",
                    sourceUrl = url,
                    originalText = url,
                    myanmarSummary = recap.summary,
                    myanmarScript = recap.voiceScript,
                    subtitlesSrt = recap.srtSubtitles,
                    voiceTone = activeVoiceProfile.id,
                    speechSpeed = voiceSpeed.toDouble(),
                    speechPitch = voicePitch.toDouble(),
                    durationSeconds = cues.lastOrNull()?.let { (it.endTimeMs / 1000).toInt() } ?: 60
                )
                activeRecap = recapItem

                // Cache locally
                localDb.recapDao().insertRecap(LocalRecapEntity.fromRecapItem(recapItem))

                if (Firebase.auth.currentUser != null) {
                    repository.saveRecap(recapItem)
                }

                _actionState.value = ActionState.Success("$platform Recap အောင်မြင်ပါသည်!")
            }.onFailure { err ->
                _actionState.value = ActionState.Error(err.localizedMessage ?: "Link recap မအောင်မြင်ပါ")
            }
        }
    }

    fun translateInputText(fastMode: Boolean = false) {
        if (inputTranslateText.isBlank()) return
        viewModelScope.launch {
            _actionState.value = ActionState.Loading("မြန်မာဘာသာပြန်ဆိုနေပါသည်...")
            val result = aiService.translateText(inputTranslateText, fastMode = fastMode)
            result.onSuccess { translated ->
                translatedMyanmarResult = translated
                _actionState.value = ActionState.Success("ဘာသာပြန်ဆိုပြီးပါပြီ")
            }.onFailure { err ->
                _actionState.value = ActionState.Error(err.localizedMessage ?: "ဘာသာပြန်ခြင်း မအောင်မြင်ပါ")
            }
        }
    }

    fun translateCurrentSrt() {
        if (rawSrtText.isBlank()) {
            _actionState.value = ActionState.Error("SRT စာသား ထည့်သွင်းထားခြင်း မရှိပါ")
            return
        }

        viewModelScope.launch {
            _actionState.value = ActionState.Loading("SRT ဖိုင်ကို အချိန်ကိုက် မြန်မာဘာသာပြန်နေပါသည်...")
            val result = aiService.translateSrtSubtitles(rawSrtText)
            result.onSuccess { translatedSrt ->
                rawSrtText = translatedSrt
                parsedCues = SrtParser.parse(translatedSrt)
                _actionState.value = ActionState.Success("SRT ဖိုင် အားလုံးကို မြန်မာဘာသာပြန်ပြီးပါပြီ!")
            }.onFailure { err ->
                _actionState.value = ActionState.Error(err.localizedMessage ?: "SRT ဘာသာပြန်ခြင်း မအောင်မြင်ပါ")
            }
        }
    }

    fun playActiveScriptAudio() {
        val script = activeRecap?.myanmarScript ?: activeRecap?.myanmarSummary ?: translatedMyanmarResult
        if (script.isNotBlank()) {
            speechEngine.speak(script)
        }
    }

    fun stopAudio() {
        speechEngine.stop()
    }

    fun selectSavedRecap(item: RecapItem) {
        activeRecap = item
        if (!item.subtitlesSrt.isNullOrBlank()) {
            parsedCues = SrtParser.parse(item.subtitlesSrt)
        }
        val profile = VoiceProfile.fromId(item.voiceTone)
        updateVoiceSettings(profile, item.speechPitch.toFloat(), item.speechSpeed.toFloat())
    }

    fun deleteSavedRecap(item: RecapItem) {
        viewModelScope.launch {
            localDb.recapDao().deleteRecap(item.id)
            if (Firebase.auth.currentUser != null) {
                repository.deleteRecap(item.id)
            }
            if (activeRecap?.id == item.id) {
                activeRecap = null
            }
        }
    }

    fun clearActionState() {
        _actionState.value = ActionState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        speechEngine.release()
    }
}
