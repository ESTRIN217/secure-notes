package com.estrin217.visormedia.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.estrin217.visormedia.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenUrlDialog(
    onDismiss: () -> Unit,
    onOpen: (type: String, url: String, title: String) -> Unit
) {
    var urlText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("image") }
    var titleText by remember { mutableStateOf("") }
    val sampleOceans = stringResource(R.string.sample_oceans)
    val sampleHls = stringResource(R.string.sample_hls)
    val sampleYoutube = stringResource(R.string.sample_youtube)
    val sampleAudio = stringResource(R.string.sample_audio)
    val defaultWebPhoto = stringResource(R.string.default_web_photo)
    val defaultWebAudio = stringResource(R.string.default_web_audio)
    val defaultWebVideo = stringResource(R.string.default_web_video)

    // Auto-detect media type when URL changes
    LaunchedEffect(urlText) {
        val lower = urlText.lowercase().trim()
        when {
            lower.endsWith(".mp3") || lower.endsWith(".m4a") || lower.endsWith(".wav") || lower.endsWith(".ogg") || lower.endsWith(".aac") -> {
                selectedType = "audio"
            }
            lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".webm") || lower.endsWith(".m3u8") || lower.endsWith(".mov") || lower.contains("youtube.com") || lower.contains("youtu.be") -> {
                selectedType = "video"
            }
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp") || lower.endsWith(".gif") -> {
                selectedType = "image"
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.url_dialog_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.url_type_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedType == "image",
                        onClick = { selectedType = "image" },
                        label = { Text(stringResource(R.string.type_photo)) },
                        leadingIcon = {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedType == "audio",
                        onClick = { selectedType = "audio" },
                        label = { Text(stringResource(R.string.attachment_audio)) },
                        leadingIcon = {
                            Icon(Icons.Default.Audiotrack, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedType == "video",
                        onClick = { selectedType = "video" },
                        label = { Text(stringResource(R.string.attachment_video)) },
                        leadingIcon = {
                            Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    label = { Text(stringResource(R.string.url_dialog_hint)) },
                    placeholder = { Text(stringResource(R.string.url_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text(stringResource(R.string.url_title_label)) },
                    placeholder = { Text(stringResource(R.string.url_title_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.preset_samples),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AssistChip(
                            onClick = {
                                urlText = "https://vjs.zencdn.net/v/oceans.mp4"
                                titleText = sampleOceans
                                selectedType = "video"
                            },
                            label = { Text(stringResource(R.string.chip_exoplayer_mp4), style = MaterialTheme.typography.labelSmall) }
                        )
                        AssistChip(
                            onClick = {
                                urlText = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
                                titleText = sampleHls
                                selectedType = "video"
                            },
                            label = { Text(stringResource(R.string.chip_exoplayer_hls), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AssistChip(
                            onClick = {
                                urlText = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
                                titleText = sampleYoutube
                                selectedType = "video"
                            },
                            label = { Text(stringResource(R.string.chip_youtube), style = MaterialTheme.typography.labelSmall) }
                        )
                        AssistChip(
                            onClick = {
                                urlText = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3"
                                titleText = sampleAudio
                                selectedType = "audio"
                            },
                            label = { Text(stringResource(R.string.chip_audio_mp3), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (urlText.isNotBlank()) {
                        val finalTitle = titleText.ifBlank {
                            when (selectedType) {
                                "image" -> defaultWebPhoto
                                "audio" -> defaultWebAudio
                                else -> defaultWebVideo
                            }
                        }
                        onOpen(selectedType, urlText.trim(), finalTitle)
                    }
                },
                enabled = urlText.isNotBlank()
            ) {
                Text(stringResource(R.string.open))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
