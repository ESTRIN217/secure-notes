package com.example.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.video.videoFrameMillis
import com.example.R
import com.example.util.VideoUrlHelper

private val VIDEO_CORNER = RoundedCornerShape(8.dp)
private const val VIDEO_ALIGN_CENTER = "center"
private const val VIDEO_ALIGN_LEFT = "left"
private const val VIDEO_ALIGN_RIGHT = "right"

@Composable
fun EditableVideoBlock(
    src: String,
    caption: String,
    alignment: String,
    isActive: Boolean,
    onActivate: () -> Unit,
    onOpen: () -> Unit,
    onCaptionChange: (String) -> Unit,
    onReplace: () -> Unit,
    onDelete: () -> Unit,
    onAlignmentChange: (String) -> Unit,
    onConvertTo: () -> Unit = {},
    onInsertBelow: () -> Unit = {},
    onDuplicate: () -> Unit = {},
    onMoveTo: () -> Unit = {},
    showCaption: Boolean = false,
    onShowCaptionChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMoreMenu by remember { mutableStateOf(false) }
    var showAlignSheet by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val isWebVideo = VideoUrlHelper.isWebVideoUrl(src)
    val youTubeThumb = if (VideoUrlHelper.isYouTubeUrl(src)) VideoUrlHelper.youTubeThumbnail(src) else null

    val handleOpen: () -> Unit = {
        if (isWebVideo) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(src)))
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.toast_cannot_open_url), Toast.LENGTH_SHORT).show()
            }
        } else {
            onOpen()
        }
    }

    Column(modifier = modifier.fillMaxWidth()
    .padding(bottom = 5.dp)) {
        val widthFraction = if (alignment == VIDEO_ALIGN_CENTER) 1f else 0.5f
        val horizontalAlign = when (alignment) {
            VIDEO_ALIGN_LEFT -> Alignment.CenterStart
            VIDEO_ALIGN_RIGHT -> Alignment.CenterEnd
            else -> Alignment.Center
        }
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = horizontalAlign
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(widthFraction)
                    .aspectRatio(16f / 9f)
                    .clip(VIDEO_CORNER)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .then(
                        if (isActive) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, VIDEO_CORNER)
                        else Modifier
                    )
                    .clickable { handleOpen() }
            ) {
                if (src.isBlank()) {
                    VideoPlaceholder(onClick = onReplace)
                } else if (youTubeThumb != null) {
                    SubcomposeAsyncImage(
                        model = youTubeThumb,
                        contentDescription = stringResource(R.string.block_video_open),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = { VideoLoadingBox() },
                        error = { VideoLoadingBox() }
                    )
                } else if (isWebVideo) {
                    VideoWebPlaceholder()
                } else {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(src)
                            .videoFrameMillis(0)
                            .build(),
                        contentDescription = stringResource(R.string.block_video_open),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = { VideoLoadingBox() },
                        error = { VideoErrorBox(onReplace = onReplace) }
                    )
                }

                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = stringResource(id = R.string.cd_play_video),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(56.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
                        .padding(8.dp)
                )

                VideoActionsOverlay(
                    onMore = { showMoreMenu = true }
                )
            }
        }

        if (showCaption) {
            EditableVideoCaption(
                caption = caption,
                isActive = isActive,
                onActivate = onActivate,
                onCaptionChange = onCaptionChange,
                onDelete = onDelete,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showMoreMenu) {
        BlockOptionsSheet(
            title = stringResource(R.string.block_video),
            onDismiss = { showMoreMenu = false },
            actions = listOf(
                BlockSheetAction(
                    label = stringResource(R.string.block_convert_title),
                    icon = Icons.Default.SwapHoriz,
                    onClick = {
                        showMoreMenu = false
                        onConvertTo()
                    }
                ),
                BlockSheetAction(
                    label = stringResource(R.string.block_insert_below),
                    icon = Icons.Default.ArrowDownward,
                    onClick = {
                        showMoreMenu = false
                        onInsertBelow()
                    }
                ),
                BlockSheetAction(
                    label = stringResource(R.string.block_video_replace),
                    icon = Icons.Default.Edit,
                    onClick = {
                        showMoreMenu = false
                        onReplace()
                    }
                ),
                BlockSheetAction(
                    label = stringResource(R.string.block_video_description),
                    icon = Icons.Default.Description,
                    toggle = showCaption,
                    onClick = {
                        showMoreMenu = false
                        onShowCaptionChange(!showCaption)
                    }
                ),
                BlockSheetAction(
                    label = stringResource(R.string.block_video_align),
                    icon = Icons.Default.FormatAlignCenter,
                    onClick = {
                        showMoreMenu = false
                        showAlignSheet = true
                    }
                ),
                BlockSheetAction(
                    label = stringResource(R.string.block_duplicate),
                    icon = Icons.Default.ContentCopy,
                    onClick = {
                        showMoreMenu = false
                        onDuplicate()
                    }
                ),
                BlockSheetAction(
                    label = stringResource(R.string.block_move_to),
                    icon = Icons.AutoMirrored.Filled.DriveFileMove,
                    onClick = {
                        showMoreMenu = false
                        onMoveTo()
                    }
                ),
                BlockSheetAction(
                    label = stringResource(R.string.btn_delete),
                    icon = Icons.Default.Delete,
                    danger = true,
                    onClick = {
                        showMoreMenu = false
                        onDelete()
                    }
                )
            )
        )
    }

    if (showAlignSheet) {
        val currentAlign = when (alignment) {
            VIDEO_ALIGN_LEFT -> VIDEO_ALIGN_LEFT
            VIDEO_ALIGN_RIGHT -> VIDEO_ALIGN_RIGHT
            else -> VIDEO_ALIGN_CENTER
        }
        BlockOptionsSheet(
            title = stringResource(R.string.block_video_align),
            onDismiss = { showAlignSheet = false },
            actions = listOf(
                BlockSheetAction(
                    label = stringResource(R.string.block_align_center),
                    icon = Icons.Default.FormatAlignCenter,
                    toggle = currentAlign == VIDEO_ALIGN_CENTER,
                    onClick = { showAlignSheet = false; onAlignmentChange(VIDEO_ALIGN_CENTER) }
                ),
                BlockSheetAction(
                    label = stringResource(R.string.block_align_left),
                    icon = Icons.AutoMirrored.Filled.FormatAlignLeft,
                    toggle = currentAlign == VIDEO_ALIGN_LEFT,
                    onClick = { showAlignSheet = false; onAlignmentChange(VIDEO_ALIGN_LEFT) }
                ),
                BlockSheetAction(
                    label = stringResource(R.string.block_align_right),
                    icon = Icons.AutoMirrored.Filled.FormatAlignRight,
                    toggle = currentAlign == VIDEO_ALIGN_RIGHT,
                    onClick = { showAlignSheet = false; onAlignmentChange(VIDEO_ALIGN_RIGHT) }
                )
            )
        )
    }
}

@Composable
private fun VideoPlaceholder(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = stringResource(R.string.block_video_replace),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.block_video_replace),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun VideoWebPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Videocam,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(40.dp)
        )
    }
}

@Composable
private fun VideoLoadingBox() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun VideoErrorBox(onReplace: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onReplace() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.BrokenImage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.block_video_load_error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.block_video_replace),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun VideoActionsOverlay(
    onMore: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(2.dp)
        ) {
            IconButton(onClick = onMore, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.cd_block_more),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun EditableVideoCaption(
    caption: String,
    isActive: Boolean,
    onActivate: () -> Unit,
    onCaptionChange: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var fieldValue by remember {
        mutableStateOf(TextFieldValue(text = caption, selection = TextRange(caption.length)))
    }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(isActive) {
        if (!isActive) isFocused = false
    }

    if (!isFocused && caption != fieldValue.text) {
        fieldValue = TextFieldValue(text = caption, selection = TextRange(caption.length))
    }

    val hintColor = if (isActive) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0f)
    }

    Box(modifier = modifier) {
        BasicTextField(
            value = fieldValue,
            onValueChange = { newValue ->
                fieldValue = newValue
                onCaptionChange(newValue.text)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                    if (focusState.isFocused) onActivate()
                }
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyUp && event.key == Key.Backspace) {
                        if (fieldValue.text.isEmpty()) {
                            onDelete()
                            true
                        } else false
                    } else false
                },
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = if (isActive) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            ),
            singleLine = false
        )
        if (fieldValue.text.isEmpty()) {
            Text(
                text = stringResource(R.string.block_video_caption_hint),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = hintColor,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
        }
    }
}
