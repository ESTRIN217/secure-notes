package com.example.ui

import android.content.ContentValues
import android.media.MediaPlayer
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import java.io.File
import kotlinx.coroutines.delay

private val AUDIO_CORNER = RoundedCornerShape(10.dp)
private val AUDIO_SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

@Composable
fun EditableAudioBlock(
    src: String,
    isActive: Boolean,
    onActivate: () -> Unit,
    onDelete: () -> Unit,
    onReplace: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var isPreparing by remember { mutableStateOf(false) }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var isUserDragging by remember { mutableFloatStateOf(-1f) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }

    fun releaseCurrent() {
        val mp = player
        player = null
        isPlaying = false
        isPreparing = false
        if (mp != null) {
            try {
                if (mp.isPlaying) mp.stop()
            } catch (_: Exception) {
            }
            try {
                mp.release()
            } catch (_: Exception) {
            }
        }
    }

    DisposableEffect(src) {
        onDispose { releaseCurrent() }
    }

    LaunchedEffect(src) {
        releaseCurrent()
        positionMs = 0L
        durationMs = 0L
    }

    fun preparePlayer() {
        releaseCurrent()
        isPreparing = true
        try {
            val mp = if (src.startsWith("content://")) {
                MediaPlayer().apply { setDataSource(context, Uri.parse(src)) }
            } else {
                try {
                    MediaPlayer().apply { setDataSource(src) }
                } catch (_: Exception) {
                    MediaPlayer().apply { setDataSource(context, Uri.parse(src)) }
                }
            }
            mp.setOnPreparedListener { p ->
                isPreparing = false
                durationMs = p.duration.toLong().coerceAtLeast(0L)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    try {
                        p.playbackParams = p.playbackParams.setSpeed(playbackSpeed)
                    } catch (_: Exception) {
                    }
                }
                p.start()
                positionMs = 0L
                isPlaying = true
            }
            mp.setOnCompletionListener {
                isPlaying = false
                positionMs = durationMs
            }
            mp.setOnErrorListener { _, _, _ ->
                isPreparing = false
                isPlaying = false
                Toast.makeText(context, context.getString(R.string.toast_audio_play_error), Toast.LENGTH_SHORT).show()
                true
            }
            mp.prepareAsync()
            player = mp
        } catch (_: Exception) {
            isPreparing = false
            Toast.makeText(context, context.getString(R.string.toast_audio_play_error), Toast.LENGTH_SHORT).show()
        }
    }

    val togglePlay: () -> Unit = {
        val mp = player
        if (mp != null && !isPreparing) {
            if (isPlaying) {
                try {
                    mp.pause()
                    positionMs = mp.currentPosition.toLong()
                } catch (_: Exception) {
                }
                isPlaying = false
            } else {
                try {
                    if (durationMs > 0L && positionMs >= durationMs) positionMs = 0L
                    mp.seekTo(positionMs.toInt())
                    mp.start()
                    isPlaying = true
                } catch (_: Exception) {
                }
            }
        } else if (!isPreparing) {
            preparePlayer()
        }
    }

    LaunchedEffect(isPlaying, player) {
        while (isPlaying) {
            val p = player ?: break
            positionMs = try {
                p.currentPosition.toLong()
            } catch (_: Exception) {
                break
            }
            delay(250)
        }
    }

    fun applySpeed(speed: Float) {
        playbackSpeed = speed
        player?.let { p ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                try {
                    p.playbackParams = p.playbackParams.setSpeed(speed)
                } catch (_: Exception) {
                }
            }
        }
    }

    fun saveAudio() {
        try {
            val srcUri = if (src.startsWith("content://")) Uri.parse(src) else Uri.fromFile(File(src))
            val mime = context.contentResolver.getType(srcUri) ?: "audio/mpeg"
            val fileExt = File(src).extension
            val ext = fileExt.ifEmpty {
                when {
                    mime.contains("3gpp") || mime.contains("3gp") -> "3gp"
                    mime.contains("ogg") -> "ogg"
                    mime.contains("wav") -> "wav"
                    mime.contains("aac") || mime.contains("m4a") -> "m4a"
                    else -> "mp3"
                }
            }
            val displayName = "secure_notes_${System.currentTimeMillis()}.$ext"
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Audio.Media.MIME_TYPE, mime)
                put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/SecureNotes")
            }
            val uri = context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    context.contentResolver.openInputStream(srcUri)?.use { input ->
                        input.copyTo(out)
                    }
                }
                Toast.makeText(context, context.getString(R.string.toast_audio_saved), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, context.getString(R.string.toast_audio_save_error), Toast.LENGTH_SHORT).show()
            }
        } catch (_: Exception) {
            Toast.makeText(context, context.getString(R.string.toast_audio_save_error), Toast.LENGTH_SHORT).show()
        }
    }

    val maxRange = durationMs.toFloat().coerceAtLeast(1f)
    val currentPositionMs = if (isUserDragging > 0f) isUserDragging.toLong() else positionMs
    val sliderValue = currentPositionMs.toFloat().coerceIn(0f, maxRange)

    Column(modifier = modifier.fillMaxWidth()) {
        if (src.isBlank()) {
            AudioPlaceholder(onClick = onReplace)
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AUDIO_CORNER)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .then(
                        if (isActive) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, AUDIO_CORNER)
                        else Modifier
                    )
                    .clickable { onActivate() }
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = {
                    onActivate()
                    togglePlay()
                }) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                        contentDescription = stringResource(R.string.cd_play_pause_audio),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${formatTime(currentPositionMs)} / ${formatTime(durationMs)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = sliderValue,
                        onValueChange = { newValue ->
                            isUserDragging = newValue
                            onActivate()
                        },
                        onValueChangeFinished = {
                            if (isUserDragging > 0f) {
                                player?.seekTo(isUserDragging.toInt())
                                positionMs = isUserDragging.toLong()
                                isUserDragging = -1f
                            }
                        },
                        valueRange = 0f..maxRange,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                IconButton(onClick = {
                    onActivate()
                    showMoreMenu = true
                }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.cd_block_more),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showMoreMenu) {
        BlockOptionsSheet(
            title = stringResource(R.string.block_audio),
            onDismiss = { showMoreMenu = false },
            actions = listOf(
                BlockSheetAction(
                    label = stringResource(R.string.block_audio_replace),
                    icon = Icons.Default.Edit,
                    onClick = {
                        showMoreMenu = false
                        onReplace()
                    }
                )
            ),
            contentAfterActions = {
                BlockOptionsSheetDivider()
                menuMas(
                    guardar = {
                        showMoreMenu = false
                        saveAudio()
                    },
                    velocidadDeReproducion = {
                        showMoreMenu = false
                        showSpeedSheet = true
                    },
                    eliminar = {
                        showMoreMenu = false
                        onDelete()
                    }
                )
            }
        )
    }

    if (showSpeedSheet) {
        BlockOptionsSheet(
            title = stringResource(R.string.block_audio_speed),
            onDismiss = { showSpeedSheet = false },
            actions = AUDIO_SPEEDS.map { speed ->
                BlockSheetAction(
                    label = audioSpeedLabel(speed),
                    icon = Icons.Default.PlayCircle,
                    toggle = speed == playbackSpeed,
                    onClick = {
                        showSpeedSheet = false
                        applySpeed(speed)
                    }
                )
            }
        )
    }
}

@Composable
private fun AudioPlaceholder(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(AUDIO_CORNER)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = stringResource(R.string.block_audio_replace),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun menuMas(
    guardar: () -> Unit,
    velocidadDeReproducion: () -> Unit,
    eliminar: () -> Unit = {}
) {
    AudioSheetRow(label = R.string.block_audio_save, icon = Icons.Default.Download, onClick = guardar)
    AudioSheetRow(label = R.string.block_audio_speed, icon = Icons.Default.PlayCircle, onClick = velocidadDeReproducion)
    AudioSheetRow(label = R.string.btn_delete, icon = Icons.Default.Delete, danger = true, onClick = eliminar)
}

@Composable
private fun AudioSheetRow(
    label: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val contentColor = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    val iconTint = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor,
            modifier = Modifier
                .weight(1f)
                .padding(start = 20.dp, end = 8.dp)
        )
    }
}

fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

private fun audioSpeedLabel(speed: Float): String {
    val text = if (speed % 1f == 0f) speed.toInt().toString() else speed.toString().trimEnd('0').trimEnd('.')
    return "$text×"
}
