package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.NoteContentBlock

@Composable
fun NoteContentBlockCard(
    block: NoteContentBlock,
    content: String,
    noteId: Int,
    onDeleteBlock: (NoteContentBlock) -> Unit,
    onNavigateToMediaViewer: (String, String) -> Unit,
    onNavigateToDrawing: (Int, String?) -> Unit,
    onUrlClicked: (url: String, rawOffset: Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val deleteIcon: @Composable () -> Unit = {
        Box(modifier = Modifier.fillMaxSize()) {
            IconButton(
                onClick = { onDeleteBlock(block) },
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.TopEnd)
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

    when (block) {
        is NoteContentBlock.TextBlock -> {
            if (block.annotatedString.isNotEmpty()) {
                Box(modifier = modifier) {
                    @Suppress("DEPRECATION")
                    ClickableText(
                        text = block.annotatedString,
                        style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        onClick = { offset ->
                            block.annotatedString.getStringAnnotations("URL", offset, offset)
                                .firstOrNull()?.let { annotation ->
                                    val rawOffset = block.rawStart + block.parseResult.transformedToSource[offset.coerceIn(0, block.parseResult.transformedToSource.lastIndex)]
                                    onUrlClicked(annotation.item, rawOffset)
                                }
                        }
                    )
                    deleteIcon()
                }
            }
        }
        is NoteContentBlock.ChecklistItemBlock -> {
            Box(modifier = modifier) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (block.isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                        contentDescription = if (block.isChecked) stringResource(id = R.string.cd_checked) else stringResource(id = R.string.cd_unchecked),
                        tint = if (block.isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp).size(24.dp)
                    )
                    @Suppress("DEPRECATION")
                    ClickableText(
                        text = block.text,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = if (block.isChecked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (block.isChecked) TextDecoration.LineThrough else null
                        ),
                        onClick = { offset ->
                            block.text.getStringAnnotations("URL", offset, offset)
                                .firstOrNull()?.let { annotation ->
                                    val rawOffset = block.rawStart + block.parseResult.transformedToSource[offset.coerceIn(0, block.parseResult.transformedToSource.lastIndex)]
                                    onUrlClicked(annotation.item, rawOffset)
                                }
                        }
                    )
                }
                deleteIcon()
            }
        }
        is NoteContentBlock.ImageBlock -> {
            Card(
                modifier = modifier.fillMaxWidth().heightIn(max = 200.dp).wrapContentHeight(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Box {
                    AsyncImage(
                        model = block.src,
                        contentDescription = stringResource(R.string.attachment_image),
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onNavigateToMediaViewer("image", block.src) },
                        contentScale = ContentScale.FillWidth
                    )
                    deleteIcon()
                }
            }
        }
        is NoteContentBlock.VideoBlock -> {
            Card(
                modifier = modifier.fillMaxWidth().height(200.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)).clickable { onNavigateToMediaViewer("video", block.src) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = stringResource(id = R.string.cd_play_video),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    deleteIcon()
                }
            }
        }
        is NoteContentBlock.AudioBlock -> {
            Card(
                modifier = modifier.fillMaxWidth().wrapContentHeight(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Box {
                    AudioPlayerWidget(path = block.src, modifier = Modifier.fillMaxWidth().padding(12.dp))
                    deleteIcon()
                }
            }
        }
        is NoteContentBlock.DrawingBlock -> {
            Card(
                modifier = modifier.fillMaxWidth().heightIn(max = 200.dp).wrapContentHeight(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Box {
                    AsyncImage(
                        model = block.previewPath,
                        contentDescription = stringResource(R.string.attachment_drawing),
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White)
                            .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                            .clickable { onNavigateToDrawing(noteId, block.jsonPath) },
                        contentScale = ContentScale.Fit
                    )
                    deleteIcon()
                }
            }
        }
        is NoteContentBlock.VoiceBlock -> {
            Card(
                modifier = modifier.fillMaxWidth().wrapContentHeight(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Box {
                    AudioPlayerWidget(path = block.path, modifier = Modifier.fillMaxWidth().padding(12.dp))
                    deleteIcon()
                }
            }
        }
        is NoteContentBlock.FileBlock -> {
            Card(
                modifier = modifier.fillMaxWidth().wrapContentHeight(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = stringResource(R.string.attachment_file),
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = block.name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    deleteIcon()
                }
            }
        }
    }
}
