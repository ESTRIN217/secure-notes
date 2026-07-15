package com.example.ui

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.util.Log
import com.example.R

@Composable
fun AudioPlayerWidget(path: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var isPlayingAudio by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    var disposed by remember { mutableStateOf(false) }

    DisposableEffect(path) {
        onDispose {
            disposed = true
            val mp = mediaPlayer
            if (mp != null) {
                mediaPlayer = null
                try {
                    if (mp.isPlaying) {
                        mp.stop()
                    }
                } catch (e: Exception) {
                    Log.e("AudioPlayerWidget", "stop failed", e)
                }
                try {
                    mp.release()
                } catch (e: Exception) {
                    Log.e("AudioPlayerWidget", "release failed", e)
                }
            }
        }
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = {
                if (isPlayingAudio) {
                    mediaPlayer?.let { mp ->
                        try {
                            if (mp.isPlaying) {
                                mp.stop()
                            }
                        } catch (e: Exception) {
                            Log.e("AudioPlayerWidget", "stop failed", e)
                        }
                        try {
                            mp.release()
                        } catch (e: Exception) {
                            Log.e("AudioPlayerWidget", "release failed", e)
                        }
                    }
                    mediaPlayer = null
                    isPlayingAudio = false
                } else {
                    try {
                        val mp = MediaPlayer().apply {
                            setDataSource(context, Uri.parse(path))
                            prepare()
                            start()
                            setOnCompletionListener {
                                if (disposed) return@setOnCompletionListener
                                isPlayingAudio = false
                                try {
                                    release()
                                } catch (e: Exception) {
                                    Log.e("AudioPlayerWidget", "release on completion failed", e)
                                }
                                mediaPlayer = null
                            }
                        }
                        mediaPlayer = mp
                        isPlayingAudio = true
                    } catch (e: Exception) {
                        try {
                            val mp = MediaPlayer().apply {
                                setDataSource(path)
                                prepare()
                                start()
                                setOnCompletionListener {
                                    if (disposed) return@setOnCompletionListener
                                    isPlayingAudio = false
                                    try {
                                        release()
                                    } catch (e: Exception) {
                                        Log.e("AudioPlayerWidget", "release on completion 2 failed", e)
                                    }
                                    mediaPlayer = null
                                }
                            }
                            mediaPlayer = mp
                            isPlayingAudio = true
                        } catch (e2: Exception) {
                            Toast.makeText(context, context.getString(R.string.toast_audio_play_error), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = if (isPlayingAudio) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = stringResource(id = R.string.cd_play_pause_audio),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}
