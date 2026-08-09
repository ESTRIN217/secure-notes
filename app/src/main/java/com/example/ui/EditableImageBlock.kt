package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.Image
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.example.R

private val IMAGE_CORNER = RoundedCornerShape(8.dp)
private const val IMAGE_ALIGN_CENTER = "center"
private const val IMAGE_ALIGN_LEFT = "left"
private const val IMAGE_ALIGN_RIGHT = "right"

@Composable
fun EditableImageBlock(
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
    modifier: Modifier = Modifier
) {
    var showAlignSheet by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        val widthFraction = if (alignment == IMAGE_ALIGN_CENTER) 1f else 0.5f
        val horizontalAlign = when (alignment) {
            IMAGE_ALIGN_LEFT -> Alignment.CenterStart
            IMAGE_ALIGN_RIGHT -> Alignment.CenterEnd
            else -> Alignment.Center
        }
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = horizontalAlign
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(widthFraction)
                    .clip(IMAGE_CORNER)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .then(
                        if (isActive) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, IMAGE_CORNER)
                        else Modifier
                    )
                    .clickable { onOpen() }
            ) {
                if (src.isBlank()) {
                    ImagePlaceholder(onClick = onReplace)
                } else {
                    SubcomposeAsyncImage(
                        model = src,
                        contentDescription = stringResource(R.string.block_image_open),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 480.dp),
                        contentScale = ContentScale.FillWidth
                    )
                }
                if (isActive) {
                    ImageActionsOverlay(
                        onReplace = onReplace,
                        onAlign = { showAlignSheet = true },
                        onDelete = onDelete
                    )
                }
            }
        }

        EditableImageCaption(
            caption = caption,
            isActive = isActive,
            onActivate = onActivate,
            onCaptionChange = onCaptionChange,
            onDelete = onDelete,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showAlignSheet) {
        val currentAlign = when (alignment) {
            IMAGE_ALIGN_LEFT -> IMAGE_ALIGN_LEFT
            IMAGE_ALIGN_RIGHT -> IMAGE_ALIGN_RIGHT
            else -> IMAGE_ALIGN_CENTER
        }
        BlockOptionsSheet(
            title = stringResource(R.string.block_image_align),
            onDismiss = { showAlignSheet = false },
            actions = listOf(
                BlockSheetAction(
                    label = stringResource(R.string.block_align_center),
                    icon = Icons.Default.FormatAlignCenter,
                    toggle = currentAlign == IMAGE_ALIGN_CENTER,
                    onClick = { showAlignSheet = false; onAlignmentChange(IMAGE_ALIGN_CENTER) }
                ),
                BlockSheetAction(
                    label = stringResource(R.string.block_align_left),
                    icon = Icons.Default.FormatAlignLeft,
                    toggle = currentAlign == IMAGE_ALIGN_LEFT,
                    onClick = { showAlignSheet = false; onAlignmentChange(IMAGE_ALIGN_LEFT) }
                ),
                BlockSheetAction(
                    label = stringResource(R.string.block_align_right),
                    icon = Icons.Default.FormatAlignRight,
                    toggle = currentAlign == IMAGE_ALIGN_RIGHT,
                    onClick = { showAlignSheet = false; onAlignmentChange(IMAGE_ALIGN_RIGHT) }
                )
            )
        )
    }
}

@Composable
private fun ImagePlaceholder(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = stringResource(R.string.block_image_replace),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.block_image_replace),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ImageActionsOverlay(
    onReplace: () -> Unit,
    onAlign: () -> Unit,
    onDelete: () -> Unit
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
            IconButton(onClick = onReplace, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.block_image_replace),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onAlign, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.FormatAlignCenter,
                    contentDescription = stringResource(R.string.block_image_align),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.btn_delete),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun EditableImageCaption(
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
                text = stringResource(R.string.block_image_caption_hint),
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
