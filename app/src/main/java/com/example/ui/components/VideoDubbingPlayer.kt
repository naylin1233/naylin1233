package com.example.ui.components

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.SrtCue
import com.example.util.MyanmarSpeechEngine
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun VideoDubbingPlayer(
    videoUri: Uri?,
    cues: List<SrtCue>,
    voiceScript: String,
    speechEngine: MyanmarSpeechEngine?,
    modifier: Modifier = Modifier
) {
    var isPlaying by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableIntStateOf(0) }
    var durationMs by remember { mutableIntStateOf(0) }
    var isMutedVideo by remember { mutableStateOf(false) }
    var isDubbingVoiceActive by remember { mutableStateOf(true) }
    var showSubtitles by remember { mutableStateOf(true) }
    var activeSubtitleText by remember { mutableStateOf("") }
    var lastSpokenCueIndex by remember { mutableIntStateOf(-1) }

    var videoViewInstance by remember { mutableStateOf<VideoView?>(null) }

    // Video playback ticker & Subtitle sync
    LaunchedEffect(isPlaying, videoViewInstance) {
        while (isPlaying) {
            val pos = videoViewInstance?.currentPosition ?: 0
            val dur = videoViewInstance?.duration ?: 0
            currentPositionMs = pos
            if (dur > 0) durationMs = dur

            // Determine active subtitle cue
            val activeCue = cues.find { pos >= it.startTimeMs && pos <= it.endTimeMs }
            if (activeCue != null) {
                val text = if (activeCue.translatedMyanmarText.isNotBlank()) {
                    activeCue.translatedMyanmarText
                } else {
                    activeCue.originalText
                }
                activeSubtitleText = text

                // Voice dubbing trigger on cue change
                if (isDubbingVoiceActive && activeCue.index != lastSpokenCueIndex) {
                    lastSpokenCueIndex = activeCue.index
                    speechEngine?.speak(text, startFromBeginning = true)
                }
            } else {
                activeSubtitleText = ""
            }

            delay(200)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            speechEngine?.stop()
            videoViewInstance?.stopPlayback()
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Video / Canvas Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (videoUri != null) {
                    AndroidView(
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                setVideoURI(videoUri)
                                setOnPreparedListener { mp ->
                                    durationMs = mp.duration
                                    if (isMutedVideo) {
                                        mp.setVolume(0f, 0f)
                                    }
                                }
                                setOnCompletionListener {
                                    isPlaying = false
                                    speechEngine?.stop()
                                }
                                videoViewInstance = this
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Audio-only preview visualization with waveform card
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "ဗီဒီယို မရွေးချယ်ထားပါက အသံသီးသန့် ဖွင့်ပါမည်",
                            color = Color.LightGray,
                            fontSize = 13.sp
                        )
                    }
                }

                // Subtitle Overlay
                if (showSubtitles && activeSubtitleText.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp, start = 16.dp, end = 16.dp)
                            .background(Color(0xCC000000), shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = activeSubtitleText,
                            color = Color.Yellow,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Controls Row
            Column(modifier = Modifier.padding(12.dp)) {
                // Time progress & Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTimeMs(currentPositionMs.toLong()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = if (durationMs > 0) currentPositionMs.toFloat() / durationMs.toFloat() else 0f,
                        onValueChange = { fraction ->
                            val seekTo = (fraction * durationMs).toInt()
                            currentPositionMs = seekTo
                            videoViewInstance?.seekTo(seekTo)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(
                        text = formatTimeMs(durationMs.toLong()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Buttons Toolbar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rewind 10s
                    IconButton(
                        onClick = {
                            val newPos = (currentPositionMs - 10000).coerceAtLeast(0)
                            videoViewInstance?.seekTo(newPos)
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Replay10, contentDescription = "Rewind 10s")
                    }

                    // Play / Pause Main Button
                    IconButton(
                        onClick = {
                            if (isPlaying) {
                                videoViewInstance?.pause()
                                speechEngine?.pause()
                                isPlaying = false
                            } else {
                                if (videoUri != null) {
                                    videoViewInstance?.start()
                                } else if (voiceScript.isNotBlank()) {
                                    speechEngine?.speak(voiceScript)
                                }
                                isPlaying = true
                            }
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .testTag("play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Forward 10s
                    IconButton(
                        onClick = {
                            val newPos = (currentPositionMs + 10000).coerceAtMost(durationMs)
                            videoViewInstance?.seekTo(newPos)
                        }
                    ) {
                        Icon(imageVector = Icons.Default.Forward10, contentDescription = "Forward 10s")
                    }

                    // Toggle Subtitles
                    IconButton(
                        onClick = { showSubtitles = !showSubtitles },
                        modifier = Modifier.clip(CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ClosedCaption,
                            contentDescription = "Toggle Subtitles",
                            tint = if (showSubtitles) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Toggle Voice Dubbing
                    IconButton(
                        onClick = {
                            isDubbingVoiceActive = !isDubbingVoiceActive
                            if (!isDubbingVoiceActive) speechEngine?.stop()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = "Toggle Voice Dubbing",
                            tint = if (isDubbingVoiceActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Toggle Video Mute
                    IconButton(
                        onClick = {
                            isMutedVideo = !isMutedVideo
                            // Mute video audio to hear only the Myanmar dubbing
                        }
                    ) {
                        Icon(
                            imageVector = if (isMutedVideo) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Toggle Video Audio",
                            tint = if (isMutedVideo) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun formatTimeMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
