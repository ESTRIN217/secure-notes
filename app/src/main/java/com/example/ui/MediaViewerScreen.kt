package com.example.ui

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.example.R
import androidx.compose.foundation.BorderStroke
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaViewerScreen(
    type: String,
    src: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isWebVideo = com.example.util.VideoUrlHelper.isWebVideoUrl(src)
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    BackHandler { onBack() }

    Scaffold(
        topBar = {
          TopAppBar(
            title = {
              Text(
                            text = when (type) {
                                "image" -> stringResource(R.string.attachment_image)
                                "audio" -> stringResource(R.string.attachment_audio)
                                else -> stringResource(R.string.attachment_video)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
            },
            navigationIcon = {
              IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
            },
            actions = {
              if (!isWebVideo) {
                            IconButton(onClick = {
                                try {
                                    val uri = if (src.startsWith("content://")) {
                                        Uri.parse(src)
                                    } else {
                                        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(src))
                                    }
                                    val shareMime = when (type) {
                                        "image" -> "image/*"
                                        "audio" -> "audio/*"
                                        else -> "video/*"
                                    }
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        this.type = shareMime
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, null))
                                } catch (e: Exception) {
                                    Toast.makeText(context, context.getString(R.string.toast_share_error), Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Default.Share, contentDescription = stringResource(R.string.option_share))
                            }
                            IconButton(onClick = {
                                try {
                                    val srcUri = if (src.startsWith("content://")) Uri.parse(src) else Uri.fromFile(File(src))
                                    val isImage = type == "image"
                                    val isAudio = type == "audio"
                                    val ext = if (src.startsWith("content://")) {
                                        val mime = context.contentResolver.getType(srcUri)
                                        when {
                                            mime?.startsWith("video") == true -> "mp4"
                                            mime?.startsWith("audio") == true -> "m4a"
                                            else -> "png"
                                        }
                                    } else {
                                        File(src).extension.ifEmpty { when { isImage -> "png"; isAudio -> "m4a"; else -> "mp4" } }
                                    }
                                    val fileName = "secure_notes_${System.currentTimeMillis()}.$ext"
                                    val mimeType = when {
                                        isImage -> "image/png"
                                        isAudio -> "audio/mp4"
                                        else -> "video/mp4"
                                    }
                                    val collection = when {
                                        isImage -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                                        isAudio -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                                        else -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                                    }
                                    val relPath = if (isAudio) {
                                        Environment.DIRECTORY_MUSIC + "/SecureNotes"
                                    } else {
                                        Environment.DIRECTORY_PICTURES + "/SecureNotes"
                                    }
                                    val values = android.content.ContentValues().apply {
                                        put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                                        put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
                                        put(MediaStore.Audio.Media.RELATIVE_PATH, relPath)
                                    }
                                    val uri = context.contentResolver.insert(collection, values)
                                    if (uri != null) {
                                        context.contentResolver.openOutputStream(uri)?.use { out ->
                                            context.contentResolver.openInputStream(srcUri)?.use { input ->
                                                input.copyTo(out)
                                            }
                                        }
                                        Toast.makeText(context, context.getString(R.string.toast_saved_to_gallery), Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, context.getString(R.string.toast_save_error), Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Default.SaveAlt, contentDescription = stringResource(R.string.option_save))
                            }
              }
            }
          )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (type == "image") {
                AsyncImage(
                    model = src,
                    contentDescription = stringResource(R.string.attachment_image),
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        )
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.5f, 5f)
                                offsetX += pan.x
                                offsetY += pan.y
                            }
                        },
                    contentScale = ContentScale.Fit
                )
            } else if (type == "audio") {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = stringResource(R.string.attachment_audio),
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    AudioPlayerWidget(path = src, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = src,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            } else if (isWebVideo) {
                val youTubeThumb = com.example.util.VideoUrlHelper.youTubeThumbnail(src)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(src)))
                            } catch (e: Exception) {
                                Toast.makeText(context, context.getString(R.string.toast_cannot_open_url), Toast.LENGTH_SHORT).show()
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (youTubeThumb.isNotBlank()) {
                            AsyncImage(
                                model = youTubeThumb,
                                contentDescription = stringResource(R.string.attachment_video),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = stringResource(R.string.cd_play_video),
                            tint = Color.White,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(
                            if (com.example.util.VideoUrlHelper.isYouTubeUrl(src)) R.string.video_open_youtube
                            else R.string.video_open_external
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = src,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            } else {
                AndroidView(
                    factory = { ctx ->
                        try {
                            android.widget.VideoView(ctx).apply {
                                if (src.startsWith("content://")) {
                                    setVideoURI(Uri.parse(src))
                                } else {
                                    setVideoPath(src)
                                }
                                val mc = android.widget.MediaController(ctx)
                                setMediaController(mc)
                                start()
                            }
                        } catch (e: Exception) {
                            Log.e("MediaViewer", "VideoView creation failed", e)
                            android.widget.TextView(ctx).apply {
                                text = context.getString(R.string.toast_video_play_error)
                                textAlignment = android.view.View.TEXT_ALIGNMENT_CENTER
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                )
            }
        }
    }
}
