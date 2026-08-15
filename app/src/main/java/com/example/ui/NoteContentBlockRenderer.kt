package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.video.videoFrameMillis
import com.example.R
import com.example.data.model.BlockRenderContext
import com.example.data.model.ColumnAlignment
import com.example.data.model.NoteContentBlock
import com.example.util.RichTextParser

@Composable
fun NoteContentBlock.RenderContent(
    context: BlockRenderContext,
    modifier: Modifier = Modifier
) {
    when (this) {
        is NoteContentBlock.TextBlock -> renderTextBlock(context, modifier)
        is NoteContentBlock.ChecklistItemBlock -> renderChecklistItemBlock(context, modifier)
        is NoteContentBlock.ImageBlock -> renderImageBlock(context, modifier)
        is NoteContentBlock.VideoBlock -> renderVideoBlock(context, modifier)
        is NoteContentBlock.AudioBlock -> renderAudioBlock(context, modifier)
        is NoteContentBlock.DrawingBlock -> renderDrawingBlock(context, modifier)
        is NoteContentBlock.VoiceBlock -> renderVoiceBlock(context, modifier)
        is NoteContentBlock.FileBlock -> renderFileBlock(context, modifier)
        is NoteContentBlock.BookmarkBlock -> renderBookmarkBlock(context, modifier)
        is NoteContentBlock.TableBlock -> renderTableBlock(context, modifier)
        is NoteContentBlock.HorizontalRuleBlock -> renderHorizontalRuleBlock(modifier)
        is NoteContentBlock.CollapsibleBlock -> renderCollapsibleBlock(context, modifier)
    }
}

private const val MEDIA_CARD_SHAPE_SIZE = 12

@Composable
private fun MediaCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MEDIA_CARD_SHAPE_SIZE.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Box { content() }
    }
}

@Composable
private fun MediaActionsOverlay(
    onDelete: () -> Unit,
    onMore: (() -> Unit)? = null
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (onMore != null) {
                IconButton(
                    onClick = onMore,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(id = R.string.cd_block_more),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(id = R.string.cd_remove),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun NoteContentBlock.TextBlock.renderTextBlock(
    context: BlockRenderContext,
    modifier: Modifier
) {
    val block = this
    if (annotatedString.isNotEmpty()) {
        Box(modifier = modifier) {
            @Suppress("DEPRECATION")
            ClickableText(
                text = annotatedString,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = textAlign ?: TextAlign.Start
                ),
                onClick = { offset ->
                    annotatedString.getStringAnnotations("URL", offset, offset)
                        .firstOrNull()?.let { annotation ->
                            val rawOffset = rawStart + parseResult.transformedToOriginal(offset)
                            context.onUrlClicked(annotation.item, rawOffset)
                        }
                }
            )
        }
    }
}

@Composable
private fun NoteContentBlock.ChecklistItemBlock.renderChecklistItemBlock(
    context: BlockRenderContext,
    modifier: Modifier
) {
    val block = this
    Box(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable {
                context.onChecklistToggle?.invoke(globalIndex, isChecked)
            }
        ) {
            Icon(
                imageVector = if (isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                contentDescription = if (isChecked) stringResource(id = R.string.cd_checked) else stringResource(id = R.string.cd_unchecked),
                tint = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp).size(24.dp)
            )
            @Suppress("DEPRECATION")
            ClickableText(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = if (isChecked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (isChecked) TextDecoration.LineThrough else null
                ),
                onClick = { offset ->
                    text.getStringAnnotations("URL", offset, offset)
                        .firstOrNull()?.let { annotation ->
                            val rawOffset = rawStart + parseResult.transformedToOriginal(offset)
                            context.onUrlClicked(annotation.item, rawOffset)
                        }
                }
            )
        }
    }
}

@Composable
private fun NoteContentBlock.ImageBlock.renderImageBlock(
    context: BlockRenderContext,
    modifier: Modifier
) {
    val block = this
    val isFullWidth = align == null || align == "center"
    val widthFraction = if (isFullWidth) 1f else 0.5f
    val horizontalAlign = when (align) {
        "left" -> Alignment.CenterStart
        "right" -> Alignment.CenterEnd
        else -> Alignment.Center
    }
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = horizontalAlign
    ) {
        MediaCard(modifier = Modifier.fillMaxWidth(widthFraction).heightIn(max = 200.dp).wrapContentHeight()) {
            Column {
                SubcomposeAsyncImage(
                    model = src,
                    contentDescription = stringResource(R.string.attachment_image),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(MEDIA_CARD_SHAPE_SIZE.dp))
                        .clickable {
                            if (linkUrl != null) {
                                context.onUrlClicked(linkUrl, 0)
                            } else {
                                context.onNavigateToMediaViewer("image", src)
                            }
                        },
                    contentScale = ContentScale.FillWidth,
                    loading = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = stringResource(R.string.note_image_load_error),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = stringResource(R.string.note_image_load_error),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                )
                if (!caption.isNullOrBlank()) {
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
            MediaActionsOverlay(
                onDelete = { context.onDeleteBlock(block) },
                onMore = context.onOpenBlockMore?.let { { it(block) } }
            )
        }
    }
}

@Composable
private fun NoteContentBlock.VideoBlock.renderVideoBlock(
    context: BlockRenderContext,
    modifier: Modifier
) {
    val block = this
    val isFullWidth = align == null || align == "center"
    val widthFraction = if (isFullWidth) 1f else 0.5f
    val horizontalAlign = when (align) {
        "left" -> Alignment.CenterStart
        "right" -> Alignment.CenterEnd
        else -> Alignment.Center
    }
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = horizontalAlign
    ) {
        MediaCard(modifier = Modifier.fillMaxWidth(widthFraction)) {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                        .clickable { context.onNavigateToMediaViewer("video", src) },
                    contentAlignment = Alignment.Center
                ) {
                    val contextForThumb = LocalContext.current
                    val youTubeThumb = if (com.example.util.VideoUrlHelper.isYouTubeUrl(src)) {
                        com.example.util.VideoUrlHelper.youTubeThumbnail(src)
                    } else {
                        null
                    }
                    if (youTubeThumb != null) {
                        SubcomposeAsyncImage(
                            model = youTubeThumb,
                            contentDescription = stringResource(id = R.string.cd_play_video),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            loading = {},
                            error = {}
                        )
                    } else if (!com.example.util.VideoUrlHelper.isWebVideoUrl(src)) {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(contextForThumb)
                                .data(src)
                                .videoFrameMillis(0)
                                .build(),
                            contentDescription = stringResource(id = R.string.cd_play_video),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            loading = {},
                            error = {}
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = stringResource(id = R.string.cd_play_video),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
                            .padding(8.dp)
                    )
                }
                MediaActionsOverlay(
                    onDelete = { context.onDeleteBlock(block) },
                    onMore = context.onOpenBlockMore?.let { { it(block) } }
                )
            }
            if (!caption.isNullOrBlank()) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun NoteContentBlock.AudioBlock.renderAudioBlock(
    context: BlockRenderContext,
    modifier: Modifier
) {
    val block = this
    MediaCard(modifier = modifier.wrapContentHeight()) {
        AudioPlayerWidget(path = src, modifier = Modifier.fillMaxWidth().padding(12.dp))
        MediaActionsOverlay(
            onDelete = { context.onDeleteBlock(block) },
            onMore = context.onOpenBlockMore?.let { { it(block) } }
        )
    }
}

@Composable
private fun NoteContentBlock.DrawingBlock.renderDrawingBlock(
    context: BlockRenderContext,
    modifier: Modifier
) {
    val block = this
    val isFullWidth = align == null || align == "center"
    val widthFraction = if (isFullWidth) 1f else 0.5f
    val horizontalAlign = when (align) {
        "left" -> Alignment.CenterStart
        "right" -> Alignment.CenterEnd
        else -> Alignment.Center
    }
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = horizontalAlign
    ) {
        MediaCard(modifier = Modifier.fillMaxWidth(widthFraction).heightIn(max = 200.dp).wrapContentHeight()) {
            Column {
                if (strokes != null) {
                    DrawingStrokesView(
                        strokes = strokes,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(MEDIA_CARD_SHAPE_SIZE.dp))
                            .background(Color.White)
                            .border(1.dp, Color.LightGray, RoundedCornerShape(MEDIA_CARD_SHAPE_SIZE.dp))
                    )
                } else {
                    AsyncImage(
                        model = previewPath,
                        contentDescription = stringResource(R.string.attachment_drawing),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(MEDIA_CARD_SHAPE_SIZE.dp))
                            .background(Color.White)
                            .border(1.dp, Color.LightGray, RoundedCornerShape(MEDIA_CARD_SHAPE_SIZE.dp))
                            .clickable { context.onNavigateToDrawing(context.noteId, jsonPath) },
                        contentScale = ContentScale.Fit
                    )
                }
                if (!caption.isNullOrBlank()) {
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
            MediaActionsOverlay(
                onDelete = { context.onDeleteBlock(block) },
                onMore = context.onOpenBlockMore?.let { { it(block) } }
            )
        }
    }
}

@Composable
private fun NoteContentBlock.VoiceBlock.renderVoiceBlock(
    context: BlockRenderContext,
    modifier: Modifier
) {
    val block = this
    MediaCard(modifier = modifier.wrapContentHeight()) {
        AudioPlayerWidget(path = path, modifier = Modifier.fillMaxWidth().padding(12.dp))
        MediaActionsOverlay(
            onDelete = { context.onDeleteBlock(block) },
            onMore = context.onOpenBlockMore?.let { { it(block) } }
        )
    }
}

@Composable
private fun NoteContentBlock.FileBlock.renderFileBlock(
    context: BlockRenderContext,
    modifier: Modifier
) {
    val block = this
    MediaCard(modifier = modifier.wrapContentHeight()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = stringResource(R.string.attachment_file),
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        }
        MediaActionsOverlay(
            onDelete = { context.onDeleteBlock(block) },
            onMore = context.onOpenBlockMore?.let { { it(block) } }
        )
    }
}

@Composable
private fun NoteContentBlock.BookmarkBlock.renderBookmarkBlock(
    context: BlockRenderContext,
    modifier: Modifier
) {
    val block = this
    val host = try {
        android.net.Uri.parse(url).host?.removePrefix("www.").orEmpty()
    } catch (e: Exception) {
        ""
    }
    MediaCard(modifier = modifier.wrapContentHeight()) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { context.onUrlClicked(url, 0) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!favicon.isNullOrBlank()) {
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
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title?.takeIf { it.isNotBlank() } ?: host.ifBlank { url },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!description.isNullOrBlank()) {
                        Text(
                            text = description,
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
            }
            if (!caption.isNullOrBlank()) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
        MediaActionsOverlay(
            onDelete = { context.onDeleteBlock(block) },
            onMore = context.onOpenBlockMore?.let { { it(block) } }
        )
    }
}

@Composable
private fun NoteContentBlock.TableBlock.renderTableBlock(
    context: BlockRenderContext,
    modifier: Modifier
) {
    val block = this
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .clickable {
                    context.onEditTable?.invoke(block)
                }
        ) {
            if (headers.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                ) {
                    headers.forEachIndexed { colIndex, header ->
                        val align = columnAlignment.getOrNull(colIndex) ?: ColumnAlignment.Start
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            contentAlignment = when (align) {
                                ColumnAlignment.Center -> Alignment.Center
                                ColumnAlignment.End -> Alignment.CenterEnd
                                else -> Alignment.CenterStart
                            }
                        ) {
                            Text(
                                text = header,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            }

            rows.forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (rowIndex % 2 == 1)
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                            else Color.Transparent
                        )
                ) {
                    val cellCount = maxOf(row.size, if (headers.isNotEmpty()) headers.size else row.size)
                    for (colIndex in 0 until cellCount) {
                        val cell = row.getOrElse(colIndex) { "" }
                        val cellAlign = cellAlignment.getOrNull(rowIndex)?.getOrNull(colIndex)
                        val align = cellAlign ?: columnAlignment.getOrNull(colIndex) ?: ColumnAlignment.Start
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = when (align) {
                                ColumnAlignment.Center -> Alignment.Center
                                ColumnAlignment.End -> Alignment.CenterEnd
                                else -> Alignment.CenterStart
                            }
                        ) {
                            Text(
                                text = cell,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                if (rowIndex < rows.lastIndex) {
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
private fun renderHorizontalRuleBlock(modifier: Modifier) {
    HorizontalDivider(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun NoteContentBlock.CollapsibleBlock.renderCollapsibleBlock(
    context: BlockRenderContext,
    modifier: Modifier
) {
    val block = this
    var expanded by remember { mutableStateOf(isExpanded) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = stringResource(if (expanded) R.string.cd_collapse else R.string.cd_expand),
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(if (expanded) 180f else 0f),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Box(modifier = Modifier.padding(start = 32.dp, end = 12.dp, bottom = 10.dp)) {
                    if (content.isNotBlank()) {
                        @Suppress("DEPRECATION")
                        ClickableText(
                            text = RichTextParser.parse(content, hideTags = true),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            onClick = { }
                        )
                    }
                }
            }
        }
    }
}
