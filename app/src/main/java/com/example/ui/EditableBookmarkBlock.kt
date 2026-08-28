package com.example.ui

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Description
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.R
import com.example.util.BookmarkMetadataFetcher
import com.example.util.JsonColorizer

private val BOOKMARK_CORNER = RoundedCornerShape(10.dp)

@Composable
fun EditableBookmarkBlock(
    url: String,
    title: String,
    description: String,
    favicon: String,
    caption: String,
    color: String?,
    isFetching: Boolean,
    isActive: Boolean,
    onActivate: () -> Unit,
    onOpen: () -> Unit,
    onCaptionChange: (String) -> Unit,
    onConvertToMention: () -> Unit = {},
    onReplace: () -> Unit,
    onRefresh: () -> Unit,
    onColorChange: (String) -> Unit = {},
    onDelete: () -> Unit,
    onInsertBelow: () -> Unit = {},
    onDuplicate: () -> Unit = {},
    onMoveTo: () -> Unit = {},
    showCaption: Boolean = false,
    onShowCaptionChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMoreMenu by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }
    val cardColor = JsonColorizer.parseColor(color)
    val host = hostOf(url)
    val displayTitle = title.ifBlank { host.ifBlank { url } }

    Column(modifier = modifier.fillMaxWidth()
    .padding(bottom = 5.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(BOOKMARK_CORNER)
                .background(cardColor ?: MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .then(
                    if (isActive) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, BOOKMARK_CORNER)
                    else Modifier
                )
                .clickable { if (url.isBlank()) onReplace() else onOpen() }
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            if (url.isBlank()) {
                BookmarkPlaceholder()
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isFetching) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else if (favicon.isNotBlank()) {
                            AsyncImage(
                                model = favicon,
                                contentDescription = stringResource(R.string.block_bookmark_open),
                                modifier = Modifier.size(28.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = stringResource(R.string.block_bookmark_open),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (description.isNotBlank() && !isFetching) {
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else if (isFetching) {
                            Text(
                                text = stringResource(R.string.block_bookmark_fetching),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = host.ifBlank { url },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = {
                            onActivate()
                            showMoreMenu = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.cd_block_more),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (showCaption) {
            EditableBookmarkCaption(
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
            title = stringResource(R.string.block_bookmark),
            onDismiss = { showMoreMenu = false },
            actions = listOf(
                BlockSheetAction(
                    label = stringResource(R.string.block_bookmark_convert_mention),
                    icon = Icons.Default.AlternateEmail,
                    onClick = {
                        showMoreMenu = false
                        onConvertToMention()
                    }
                ),
                BlockSheetAction(
                    label = stringResource(R.string.block_bookmark_color),
                    icon = Icons.Default.Palette,
                    onClick = {
                        showMoreMenu = false
                        showColorDialog = true
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
                    label = stringResource(R.string.block_bookmark_description),
                    icon = Icons.Default.Description,
                    toggle = showCaption,
                    onClick = {
                        showMoreMenu = false
                        onShowCaptionChange(!showCaption)
                    }
                ),
                BlockSheetAction(
                    label = stringResource(R.string.block_bookmark_replace),
                    icon = Icons.Default.Edit,
                    onClick = {
                        showMoreMenu = false
                        onReplace()
                    }
                ),
                BlockSheetAction(
                    label = stringResource(R.string.block_bookmark_refresh),
                    icon = Icons.Default.Refresh,
                    onClick = {
                        showMoreMenu = false
                        onRefresh()
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

    if (showColorDialog) {
        ColorSelectionDialog(
            title = stringResource(R.string.block_bookmark_color),
            onDismiss = { showColorDialog = false },
            onColorSelected = { hex ->
                showColorDialog = false
                onColorChange(hex)
            }
        )
    }
}

@Composable
private fun BookmarkPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Link,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = stringResource(R.string.block_bookmark_url_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EditableBookmarkCaption(
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
                text = stringResource(R.string.block_bookmark_caption_hint),
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

private fun hostOf(url: String): String = BookmarkMetadataFetcher.hostOf(url).orEmpty()
