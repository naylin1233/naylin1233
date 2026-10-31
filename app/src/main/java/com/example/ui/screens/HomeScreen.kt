package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.model.RecapItem
import com.example.ui.ActionState
import com.example.ui.MainViewModel
import com.example.ui.auth.AuthManager
import com.example.ui.components.LinkRecapDialog
import com.example.ui.components.SrtViewerEditor
import com.example.ui.components.VideoDubbingPlayer
import com.example.ui.components.VoiceSettingsDialog
import com.example.ui.components.copyToClipboard
import com.example.util.SpeechState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showVoiceSettingsDialog by remember { mutableStateOf(false) }
    var showLinkRecapDialog by remember { mutableStateOf(false) }

    val actionState by viewModel.actionState.collectAsState()
    val savedRecaps by viewModel.savedRecaps.collectAsState()
    val speechState by viewModel.speechState.collectAsState()

    // Video & SRT File Pickers (no limits on size or duration)
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setVideoUri(uri)
        }
    }

    val srtPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.loadSrtFromUri(uri)
        }
    }

    // ActionState notifications
    LaunchedEffect(actionState) {
        when (val state = actionState) {
            is ActionState.Success -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.clearActionState()
            }
            is ActionState.Error -> {
                snackbarHostState.showSnackbar("အမှား: ${state.message}")
                viewModel.clearActionState()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground_img),
                            contentDescription = "Logo",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Recap MM",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "မြန်မာအသံနှင့် စာတန်းထိုး Video Recap",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Dark Mode Toggle
                    IconButton(
                        onClick = { viewModel.toggleDarkMode() },
                        modifier = Modifier.testTag("dark_mode_toggle")
                    ) {
                        Icon(
                            imageVector = if (viewModel.isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Dark Mode"
                        )
                    }

                    // Voice Settings Dialog
                    IconButton(
                        onClick = { showVoiceSettingsDialog = true },
                        modifier = Modifier.testTag("voice_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = "Voice Settings",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }

                    // Sign Out Button
                    IconButton(
                        onClick = {
                            AuthManager.signOut(
                                context = context,
                                credentialManager = credentialManager,
                                onSignOutComplete = onSignOut,
                                scope = coroutineScope
                            )
                        },
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign Out"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Selection
            PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Video & SRT Recap") },
                    icon = { Icon(Icons.Default.Videocam, contentDescription = null) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("ဘာသာပြန်နှင့် အသံ") },
                    icon = { Icon(Icons.Default.Translate, contentDescription = null) }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("သိမ်းဆည်းမှု (${savedRecaps.size})") },
                    icon = { Icon(Icons.Default.Bookmark, contentDescription = null) }
                )
            }

            // Global Loading Indicator Banner
            if (actionState is ActionState.Loading) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = (actionState as ActionState.Loading).message,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Tab Content
            when (selectedTabIndex) {
                0 -> VideoRecapTab(
                    viewModel = viewModel,
                    onPickVideo = { videoPickerLauncher.launch("video/*") },
                    onPickSrt = { srtPickerLauncher.launch("*/*") },
                    onOpenLinkRecap = { showLinkRecapDialog = true }
                )
                1 -> TranslationSpeechTab(viewModel = viewModel)
                2 -> SavedRecapsTab(
                    viewModel = viewModel,
                    savedRecaps = savedRecaps,
                    onSelectRecap = { item ->
                        viewModel.selectSavedRecap(item)
                        selectedTabIndex = 0
                    }
                )
            }
        }
    }

    // Voice Settings Modal
    if (showVoiceSettingsDialog) {
        VoiceSettingsDialog(
            currentProfile = viewModel.activeVoiceProfile,
            currentPitch = viewModel.voicePitch,
            currentSpeed = viewModel.voiceSpeed,
            speechEngine = viewModel.speechEngine,
            onDismiss = { showVoiceSettingsDialog = false },
            onSave = { profile, pitch, speed ->
                viewModel.updateVoiceSettings(profile, pitch, speed)
                showVoiceSettingsDialog = false
            }
        )
    }

    // Link Recap Modal
    if (showLinkRecapDialog) {
        LinkRecapDialog(
            isLoading = actionState is ActionState.Loading,
            onDismiss = { showLinkRecapDialog = false },
            onSubmitLink = { url, platform ->
                viewModel.recapFromUrl(url, platform)
                showLinkRecapDialog = false
            }
        )
    }
}

@Composable
private fun VideoRecapTab(
    viewModel: MainViewModel,
    onPickVideo: () -> Unit,
    onPickSrt: () -> Unit,
    onOpenLinkRecap: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val activeRecap = viewModel.activeRecap

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick Action Row: Pick Video, Pick SRT, Link Recap
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onPickVideo,
                modifier = Modifier
                    .weight(1f)
                    .testTag("pick_video_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (viewModel.selectedVideoUri != null) "Video လဲမည်" else "Video ထည့်မည်", style = MaterialTheme.typography.labelMedium)
            }

            OutlinedButton(
                onClick = onPickSrt,
                modifier = Modifier
                    .weight(1f)
                    .testTag("pick_srt_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("SRT ဖိုင်ထည့်မည်", style = MaterialTheme.typography.labelMedium)
            }
        }

        // YouTube / TikTok Link Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenLinkRecap() },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "YouTube / TikTok လင့်ခ်ဖြင့် Recap ပြုလုပ်မည်",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "အချိန်တိုအတွင်း ရှာဖွေပြီး မြန်မာအသံထွက် Recap ရယူပါ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            }
        }

        // Video & Dubbing Player with Live Subtitle & Speech Sync
        Text(
            text = "Video တွဲဖက်ဖွင့်ပြသမှုနှင့် မြန်မာအသံ/စာတန်းထိုး (Live Player)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        VideoDubbingPlayer(
            videoUri = viewModel.selectedVideoUri,
            cues = viewModel.parsedCues,
            voiceScript = activeRecap?.myanmarScript ?: "",
            speechEngine = viewModel.speechEngine
        )

        // Generate Recap / Translate SRT Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.generateVideoOrSrtRecap() },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("generate_recap_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("မြန်မာ Recap ပြုလုပ်မည်")
            }

            OutlinedButton(
                onClick = { viewModel.translateCurrentSrt() },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("translate_srt_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("SRT မြန်မာပြန်မည်")
            }
        }

        // Recap Result Card (Title, Summary, Myanmar Voice Script, Copy Buttons)
        if (activeRecap != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = activeRecap.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )

                        // Copy full summary
                        IconButton(
                            onClick = {
                                val fullText = "${activeRecap.title}\n\nအကျဉ်းချုပ်:\n${activeRecap.myanmarSummary}\n\nမြန်မာအသံဖတ်ဇာတ်ညွှန်း:\n${activeRecap.myanmarScript}"
                                copyToClipboard(context, "Full Recap", fullText)
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Recap")
                        }
                    }

                    // Summary
                    Text(
                        text = "အကျဉ်းချုပ် (Recap Summary):",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = activeRecap.myanmarSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Voice narration script
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "မြန်မာအသံထွက်ဖတ်ရန် ဇာတ်ညွှန်း (Voiceover Script):",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        Row {
                            IconButton(onClick = { viewModel.playActiveScriptAudio() }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Play Audio", tint = MaterialTheme.colorScheme.secondary)
                            }
                            IconButton(onClick = { viewModel.stopAudio() }) {
                                Icon(Icons.Default.Stop, contentDescription = "Stop Audio")
                            }
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = activeRecap.myanmarScript,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // SRT Subtitles Editor & Cues Viewer
        SrtViewerEditor(
            cues = viewModel.parsedCues,
            speechEngine = viewModel.speechEngine,
            onCueSpoken = {}
        )
    }
}

@Composable
private fun TranslationSpeechTab(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "စာသားများကို မြန်မာဘာသာပြန်နှင့် မြန်မာအသံထွက်ဖတ်ခြင်း",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = viewModel.inputTranslateText,
            onValueChange = { viewModel.inputTranslateText = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .testTag("translate_input_text"),
            placeholder = { Text("ဘာသာပြန်လိုသော စာသား သို့မဟုတ် အသံထွက်ဖတ်လိုသော စာသားများကို ရိုက်ထည့်ပါ...") },
            shape = RoundedCornerShape(12.dp)
        )

        // Translation Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.translateInputText(fastMode = true) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("fast_translate_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("လျင်မြန်စွာ ဘာသာပြန်မည်")
            }

            OutlinedButton(
                onClick = { viewModel.translateInputText(fastMode = false) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Pro ဘာသာပြန်")
            }
        }

        // Translation Result Card
        if (viewModel.translatedMyanmarResult.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "မြန်မာဘာသာပြန် ရလဒ်:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = {
                                copyToClipboard(context, "Myanmar Translated", viewModel.translatedMyanmarResult)
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Translation")
                        }
                    }

                    Text(
                        text = viewModel.translatedMyanmarResult,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Speech Controller Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.speechEngine.speak(viewModel.translatedMyanmarResult) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("speak_translation_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("မြန်မာအသံ ဖတ်မည်")
                        }

                        OutlinedButton(
                            onClick = { viewModel.speechEngine.stop() },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ရပ်မည်")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedRecapsTab(
    viewModel: MainViewModel,
    savedRecaps: List<RecapItem>,
    onSelectRecap: (RecapItem) -> Unit
) {
    val context = LocalContext.current

    if (savedRecaps.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "သိမ်းဆည်းထားသော Recap များ မရှိသေးပါ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Video သို့မဟုတ် SRT များမှ Recap ပြုလုပ်ပြီး သိမ်းဆည်းနိုင်ပါသည်",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(savedRecaps, key = { it.id }) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectRecap(item) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = item.sourceType.uppercase(),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = item.myanmarSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "အသံအမျိုးအစား: ${item.voiceTone}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )

                            Row {
                                IconButton(
                                    onClick = {
                                        copyToClipboard(context, item.title, "${item.title}\n\n${item.myanmarSummary}\n\n${item.myanmarScript}")
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { viewModel.deleteSavedRecap(item) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
