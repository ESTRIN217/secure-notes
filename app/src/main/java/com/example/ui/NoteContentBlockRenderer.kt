package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.BlockRenderContext
import com.example.data.model.ColumnAlignment
import com.example.data.model.NoteContentBlock

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
        is NoteContentBlock.TableBlock -> renderTableBlock(context, modifier)
        is NoteContentBlock.HorizontalRuleBlock -> renderHorizontalRuleBlock(modifier)
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
private fun DeleteOverlay(onDelete: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(28.dp).align(Alignment.TopEnd)
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
                style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                onClick = { offset ->
                    annotatedString.getStringAnnotations("URL", offset, offset)
                        .firstOrNull()?.let { annotation ->
                            val rawOffset = rawStart + parseResult.transformedToOriginal(offset)
                            context.onUrlClicked(annotation.item, rawOffset)
                        }
                }
            )
            DeleteOverlay { context.onDeleteBlock(block) }
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
        Row(verticalAlignment = Alignment.CenterVertically) {
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
        DeleteOverlay { context.onDeleteBlock(block) }
    }
}

@Composable
private fun NoteContentBlock.ImageBlock.renderImageBlock(
    context: BlockRenderContext,
    modifier: Modifier
) {
    val block = this
    MediaCard(modifier = modifier.heightIn(max = 200.dp).wrapContentHeight()) {
        AsyncImage(
            model = src,
            contentDescription = stringResource(R.string.attachment_image),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(MEDIA_CARD_SHAPE_SIZE.dp))
                .clickable { context.onNavigateToMediaViewer("image", src) },
            contentScale = ContentScale.FillWidth
        )
        DeleteOverlay { context.onDeleteBlock(block) }
    }
}

@Composable
private fun NoteContentBlock.VideoBlock.renderVideoBlock(
    context: BlockRenderContext,
    modifier: Modifier
) {
    val block = this
    MediaCard(modifier = modifier.height(200.dp)) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                    .clickable { context.onNavigateToMediaViewer("video", src) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = stringResource(id = R.string.cd_play_video),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }
            DeleteOverlay { context.onDeleteBlock(block) }
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
        DeleteOverlay { context.onDeleteBlock(block) }
    }
}

@Composable
private fun NoteContentBlock.DrawingBlock.renderDrawingBlock(
    context: BlockRenderContext,
    modifier: Modifier
) {
    val block = this
    MediaCard(modifier = modifier.heightIn(max = 200.dp).wrapContentHeight()) {
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
        DeleteOverlay { context.onDeleteBlock(block) }
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
        DeleteOverlay { context.onDeleteBlock(block) }
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
        DeleteOverlay { context.onDeleteBlock(block) }
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
                        val align = columnAlignment.getOrNull(colIndex) ?: ColumnAlignment.Start
                        Box(
                            modifier = Modifier
                                .weight(1f)
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
