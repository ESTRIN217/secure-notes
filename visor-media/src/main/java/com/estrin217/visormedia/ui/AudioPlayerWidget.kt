package com.estrin217.visormedia.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.estrin217.visormedia.R
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale

@OptIn(UnstableApi::class)
@Composable
fun AudioPlayerWidget(
    path: String,
    modifier: Modifier = Modifier,
    isActive: Boolean = true
) {
    val context = LocalContext.current
    var isPlayingAudio by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var bufferedPosition by remember { mutableLongStateOf(0L) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderProgress by remember { mutableFloatStateOf(0f) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var retryTrigger by remember { mutableIntStateOf(0) }

    val parsedUri = remember(path) {
        if (path.startsWith("http://") || path.startsWith("https://") ||
            path.startsWith("content://") || path.startsWith("file://") ||
            path.startsWith("asset://")
        ) {
            Uri.parse(path)
        } else {
            Uri.fromFile(File(path))
        }
    }

    val exoPlayer = remember(parsedUri, retryTrigger) {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 VisorMedia/1.0")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(25000)
            .setReadTimeoutMs(25000)

        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)

        ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                setMediaItem(Media3Item.fromUri(parsedUri))
                prepare()
                playWhenReady = false
            }
    }

    // Attach ExoPlayer listener and lifecycle cleanup
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                isPlayingAudio = isPlaying
            }

            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> {
                        isBuffering = true
                        errorMessage = null
                    }
                    Player.STATE_READY -> {
                        isBuffering = false
                        errorMessage = null
                        val dur = exoPlayer.duration
                        if (dur > 0) {
                            duration = dur
                        }
                    }
                    Player.STATE_ENDED -> {
                        isBuffering = false
                        isPlayingAudio = false
                        currentPosition = 0L
                        sliderProgress = 0f
                    }
                    Player.STATE_IDLE -> {
                        isBuffering = false
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e("AudioPlayerWidget", "ExoPlayer error: ${error.message}", error)
                isBuffering = false
                isPlayingAudio = false
                val cause = error.cause
                val msg = when {
                    cause is HttpDataSource.InvalidResponseCodeException -> {
                        if (cause.responseCode == 403) {
                            context.getString(R.string.audio_error_403)
                        } else {
                            context.getString(R.string.audio_error_http, cause.responseCode)
                        }
                    }
                    error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ||
                    error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> {
                        context.getString(R.string.audio_error_network)
                    }
                    else -> context.getString(R.string.audio_error_generic)
                }
                errorMessage = msg
            }
        }

        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    LaunchedEffect(isActive) {
        if (!isActive && exoPlayer.isPlaying) {
            exoPlayer.pause()
        }
    }

    // Progress polling loop
    LaunchedEffect(isPlayingAudio, isBuffering) {
        while (true) {
            try {
                val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                val dur = exoPlayer.duration.coerceAtLeast(0L)
                val buf = exoPlayer.bufferedPosition.coerceAtLeast(0L)
                currentPosition = pos
                bufferedPosition = buf
                if (dur > 0) {
                    duration = dur
                    if (!isDraggingSlider) {
                        sliderProgress = (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                    }
                }
            } catch (e: Exception) {
                Log.e("AudioPlayerWidget", "Error polling playback position", e)
            }
            delay(250)
        }
    }

    fun togglePlayPause() {
        if (errorMessage != null) {
            // Retry playback
            errorMessage = null
            retryTrigger++
            return
        }

        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                exoPlayer.seekTo(0L)
            }
            exoPlayer.play()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E1E2E).copy(alpha = 0.85f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Waveform visualization animation or error state
        if (errorMessage != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = stringResource(R.string.cd_alert),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            errorMessage = null
                            retryTrigger++
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(stringResource(R.string.retry_action), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "audioWave")
                val barCount = 18
                for (i in 0 until barCount) {
                    val animatedHeight by infiniteTransition.animateFloat(
                        initialValue = 6f,
                        targetValue = if (isPlayingAudio && !isBuffering) ((i * 7) % 24 + 10).toFloat() else 6f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 350 + (i * 45) % 400, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "bar_$i"
                    )
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(animatedHeight.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (isPlayingAudio && !isBuffering) MaterialTheme.colorScheme.primary
                                else Color.White.copy(alpha = 0.25f)
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Progress Slider
        Slider(
            value = sliderProgress,
            onValueChange = { newProgress ->
                isDraggingSlider = true
                sliderProgress = newProgress
            },
            onValueChangeFinished = {
                isDraggingSlider = false
                if (duration > 0) {
                    val targetMs = (sliderProgress * duration).toLong()
                    exoPlayer.seekTo(targetMs)
                    currentPosition = targetMs
                }
            },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Time labels & buffering indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatDuration(currentPosition),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f)
            )

            if (isBuffering) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = context.getString(R.string.loading_audio),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = if (duration > 0) formatDuration(duration) else "--:--",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Control buttons
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Rewind 10s
            IconButton(
                onClick = {
                    val newPos = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                    exoPlayer.seekTo(newPos)
                    currentPosition = newPos
                },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Replay10,
                    contentDescription = stringResource(R.string.cd_rewind_10s),
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Play / Pause / Loading central button
            FilledIconButton(
                onClick = { togglePlayPause() },
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Icon(
                        imageVector = if (isPlayingAudio) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = stringResource(id = R.string.cd_play_pause_audio),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Forward 10s
            IconButton(
                onClick = {
                    val targetDuration = if (duration > 0) duration else Long.MAX_VALUE
                    val newPos = (exoPlayer.currentPosition + 10000L).coerceAtMost(targetDuration)
                    exoPlayer.seekTo(newPos)
                    currentPosition = newPos
                },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Forward10,
                    contentDescription = stringResource(R.string.cd_forward_10s),
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

