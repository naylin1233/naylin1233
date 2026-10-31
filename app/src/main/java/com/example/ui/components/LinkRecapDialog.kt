package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun LinkRecapDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSubmitLink: (url: String, platform: String) -> Unit
) {
    var urlText by remember { mutableStateOf("") }
    var selectedPlatform by remember { mutableStateOf("YouTube") }
    var inputError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "YouTube & TikTok Video Recap",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "ဗီဒီယိုလင့်ခ်ကို ကူးယူထည့်သွင်းပြီး အချိန်တိုအတွင်း မြန်မာဘာသာပြန်နှင့် မြန်မာအသံထွက် Recap ရယူပါ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Platform selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = selectedPlatform == "YouTube",
                        onClick = { selectedPlatform = "YouTube" },
                        label = { Text("YouTube") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedPlatform == "TikTok",
                        onClick = { selectedPlatform = "TikTok" },
                        label = { Text("TikTok") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = urlText,
                    onValueChange = {
                        urlText = it
                        inputError = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("link_url_input"),
                    label = { Text("$selectedPlatform Video URL") },
                    placeholder = {
                        Text(
                            if (selectedPlatform == "YouTube") "https://youtube.com/watch?v=... သို့မဟုတ် youtu.be/..."
                            else "https://www.tiktok.com/@.../video/..."
                        )
                    },
                    singleLine = true,
                    isError = inputError != null,
                    supportingText = inputError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    shape = RoundedCornerShape(12.dp)
                )

                // Quick sample test buttons for easy demo
                Text(
                    text = "ဥပမာလင့်ခ် ရွေးရန် (Quick Sample):",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            selectedPlatform = "YouTube"
                            urlText = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("YouTube ဥပမာ", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = {
                            selectedPlatform = "TikTok"
                            urlText = "https://www.tiktok.com/@tech_recap/video/739123456789"
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("TikTok ဥပမာ", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (urlText.isBlank()) {
                        inputError = "ကျေးဇူးပြု၍ Video Link ထည့်သွင်းပါ"
                    } else {
                        onSubmitLink(urlText.trim(), selectedPlatform)
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("submit_link_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Recap ပြုလုပ်မည်")
                }
            }
        },
        dismissButton = {
            if (!isLoading) {
                OutlinedButton(onClick = onDismiss) {
                    Text("မလုပ်တော့ပါ")
                }
            }
        }
    )
}
