package com.estrin217.visormedia.ui

import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.estrin217.visormedia.R
import com.estrin217.visormedia.model.MediaItem
import com.estrin217.visormedia.util.VideoUrlHelper
import kotlinx.coroutines.launch
import java.io.File

/**
 * Fullscreen Media Viewer with smooth horizontal swiping (HorizontalPager).
 * Allows swiping left / right (forward / backward) across images, videos, and audio.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaViewerScreen(
    items: List<MediaItem>,
    initialIndex: Int = 0,
    onBack: () -> Unit,
  tabBarContent: @Composable () -> Unit = {}
) {
    if (items.isEmpty()) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val safeInitial = initialIndex.coerceIn(0, items.size - 1)

    val pagerState = rememberPagerState(
        initialPage = safeInitial,
        pageCount = { items.size }
    )

    val currentItem = items.getOrElse(pagerState.currentPage) { items.first() }
    val isWebVideo = VideoUrlHelper.isWebVideoUrl(currentItem.uri)

    // Image zoom, pan and rotation state for active page
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var rotationAngle by remember { mutableFloatStateOf(0f) }

    // Reset zoom and rotation whenever user swipes to another page
    LaunchedEffect(pagerState.currentPage) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
        rotationAngle = 0f
    }

    BackHandler { onBack() }

    Scaffold(
      modifier = Modifier
        .statusBarsPadding()
        .navigationBarsPadding(),
        topBar = {
          Column {
            tabBarContent()
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = when (currentItem.type.lowercase()) {
                                    "image", "foto", "imagen" -> stringResource(R.string.attachment_image)
                                    "audio", "música", "musica" -> stringResource(R.string.attachment_audio)
                                    else -> stringResource(R.string.attachment_video)
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            if (items.size > 1) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = Color.White.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "${pagerState.currentPage + 1}/${items.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        if (currentItem.title.isNotBlank()) {
                            Text(
                                text = currentItem.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 1
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    val isCurrentImage = currentItem.type in listOf("image", "foto", "imagen")
                    if (isCurrentImage) {
                        // Rotate button
                        IconButton(onClick = {
                            rotationAngle = (rotationAngle + 90f) % 360f
                        }) {
                            Icon(
                                imageVector = Icons.Default.RotateRight,
                                contentDescription = stringResource(R.string.rotate_image),
                                tint = Color.White
                            )
                        }
                        // Reset zoom button
                        if (scale != 1f || offsetX != 0f || offsetY != 0f || rotationAngle != 0f) {
                            IconButton(onClick = {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                                rotationAngle = 0f
                            }) {
                                Icon(
                                    imageVector = Icons.Default.FilterCenterFocus,
                                    contentDescription = stringResource(R.string.reset_zoom),
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    // Share option
                    IconButton(onClick = {
                        try {
                            val src = currentItem.uri
                            if (src.startsWith("http://") || src.startsWith("https://")) {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    this.type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, src)
                                    putExtra(Intent.EXTRA_SUBJECT, currentItem.title.ifEmpty { "Medio compartido" })
                                }
                                context.startActivity(Intent.createChooser(shareIntent, null))
                            } else {
                                val uri = if (src.startsWith("content://")) {
                                    Uri.parse(src)
                                } else {
                                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(src))
                                }
                                val shareMime = when (currentItem.type.lowercase()) {
                                    "image", "foto", "imagen" -> "image/*"
                                    "audio", "música", "musica" -> "audio/*"
                                    else -> "video/*"
                                }
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    this.type = shareMime
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, null))
                            }
                        } catch (e: Exception) {
                            Log.e("MediaViewer", "Share failed", e)
                            Toast.makeText(context, context.getString(R.string.toast_share_error), Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = stringResource(R.string.option_share),
                            tint = Color.White
                        )
                    }

                    // Save to device / gallery option
                    if (!isWebVideo && !currentItem.uri.startsWith("http://") && !currentItem.uri.startsWith("https://")) {
                        IconButton(onClick = {
                            try {
                                val src = currentItem.uri
                                val srcUri = if (src.startsWith("content://")) Uri.parse(src) else Uri.fromFile(File(src))
                                val isImage = currentItem.type in listOf("image", "foto", "imagen")
                                val isAudio = currentItem.type in listOf("audio", "música", "musica")
                                val ext = if (src.startsWith("content://")) {
                                    val mime = context.contentResolver.getType(srcUri)
                                    when {
                                        mime?.startsWith("video") == true -> "mp4"
                                        mime?.startsWith("audio") == true -> "m4a"
                                        else -> "png"
                                    }
                                } else {
                                    File(src).extension.ifEmpty {
                                        when {
                                            isImage -> "png"
                                            isAudio -> "m4a"
                                            else -> "mp4"
                                        }
                                    }
                                }
                                val fileName = "visor_media_${System.currentTimeMillis()}.$ext"
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
                                    Environment.DIRECTORY_MUSIC + "/VisorMedia"
                                } else {
                                    Environment.DIRECTORY_PICTURES + "/VisorMedia"
                                }
                                val values = android.content.ContentValues().apply {
                                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                                    put(MediaStore.MediaColumns.RELATIVE_PATH, relPath)
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
                                Log.e("MediaViewer", "Save to gallery failed", e)
                                Toast.makeText(context, context.getString(R.string.toast_save_error), Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(
                                Icons.Default.SaveAlt,
                                contentDescription = stringResource(R.string.option_save),
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F0E17)
                )
            )
          }
        },
        containerColor = Color.Black
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            // HorizontalPager enables sliding left / right between images, audios, and videos
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = scale <= 1.05f
            ) { page ->
                val item = items[page]
                val isCurrentPage = pagerState.currentPage == page

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        item.type in listOf("image", "foto", "imagen") -> {
                            if (isCurrentPage) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(item.uri)
                                        .build(),
                                    contentDescription = stringResource(R.string.attachment_image),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer(
                                            scaleX = scale,
                                            scaleY = scale,
                                            translationX = offsetX,
                                            translationY = offsetY,
                                            rotationZ = rotationAngle
                                        )
                                        .pointerInput(Unit) {
                                            detectTransformGestures { _, pan, zoom, _ ->
                                                scale = (scale * zoom).coerceIn(0.7f, 5f)
                                                offsetX += pan.x
                                                offsetY += pan.y
                                            }
                                        }
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onDoubleTap = {
                                                    if (scale > 1.2f) {
                                                        scale = 1f
                                                        offsetX = 0f
                                                        offsetY = 0f
                                                    } else {
                                                        scale = 2.5f
                                                    }
                                                }
                                            )
                                        },
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(item.uri)
                                        .build(),
                                    contentDescription = stringResource(R.string.attachment_image),
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }

                        item.type in listOf("audio", "música", "musica") -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(110.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF6750A4).copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = stringResource(R.string.attachment_audio),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(56.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(28.dp))

                                if (item.title.isNotBlank()) {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                if (isCurrentPage) {
                                    AudioPlayerWidget(
                                        path = item.uri,
                                        modifier = Modifier.fillMaxWidth(),
                                        isActive = isCurrentPage
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = item.uri,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.5f),
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                            }
                        }

                        VideoUrlHelper.isYouTubeUrl(item.uri) -> {
                            if (isCurrentPage) {
                                InAppYouTubePlayer(
                                    videoUrl = item.uri,
                                    modifier = Modifier.fillMaxSize(),
                                    isActive = isCurrentPage
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (item.thumbnailUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(item.thumbnailUrl)
                                                .build(),
                                            contentDescription = item.title,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.PlayCircle,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(64.dp)
                                    )
                                }
                            }
                        }

                        else -> {
                            if (isCurrentPage) {
                                Media3VideoPlayer(
                                    videoUri = item.uri,
                                    modifier = Modifier.fillMaxSize(),
                                    autoPlay = isCurrentPage,
                                    isActive = isCurrentPage
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (item.thumbnailUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(item.thumbnailUrl)
                                                .build(),
                                            contentDescription = item.title,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.PlayCircle,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(64.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Navigation Pill with Previous/Next controls and page position indicator
            if (items.size > 1) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 20.dp, start = 16.dp, end = 16.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Surface(
                        color = Color(0xCC1A1A24),
                        shape = RoundedCornerShape(28.dp),
                        tonalElevation = 6.dp,
                        shadowElevation = 8.dp,
                        modifier = Modifier.wrapContentSize()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            // Previous button (desplazar a la izquierda / atrás)
                            IconButton(
                                onClick = {
                                    if (pagerState.currentPage > 0) {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                        }
                                    }
                                },
                                enabled = pagerState.currentPage > 0,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronLeft,
                                    contentDescription = stringResource(R.string.media_nav_previous),
                                    tint = if (pagerState.currentPage > 0) Color.White else Color.White.copy(alpha = 0.25f),
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // Counter and swipe hint
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Text(
                                    text = stringResource(
                                        R.string.media_item_counter,
                                        pagerState.currentPage + 1,
                                        items.size
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = stringResource(R.string.media_swipe_hint),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }

                            // Next button (desplazar a la derecha / adelante)
                            IconButton(
                                onClick = {
                                    if (pagerState.currentPage < items.size - 1) {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                        }
                                    }
                                },
                                enabled = pagerState.currentPage < items.size - 1,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = stringResource(R.string.media_nav_next),
                                    tint = if (pagerState.currentPage < items.size - 1) Color.White else Color.White.copy(alpha = 0.25f),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Backward compatibility overload accepting single item arguments.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaViewerScreen(
    type: String,
    src: String,
    title: String = "",
    onBack: () -> Unit,
    tabBarContent: @Composable () -> Unit = {}
) {
    MediaViewerScreen(
        items = listOf(
            MediaItem(
                id = "single_viewer_item",
                title = title,
                subtitle = "",
                type = type,
                uri = src,
                thumbnailUrl = if (type == "image") src else "",
                isSample = false
            )
        ),
        initialIndex = 0,
        onBack = onBack,
        tabBarContent = tabBarContent
    )
}
