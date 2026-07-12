package com.example.data.model

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
import com.example.util.RichTextParser

data class BlockRenderContext(
    val content: String,
    val noteId: Int,
    val onDeleteBlock: (NoteContentBlock) -> Unit,
    val onNavigateToMediaViewer: (String, String) -> Unit,
    val onNavigateToDrawing: (Int, String?) -> Unit,
    val onUrlClicked: (url: String, rawOffset: Int) -> Unit
)

sealed interface NoteContentBlock {
    @Composable
    fun render(context: BlockRenderContext, modifier: Modifier = Modifier)

    data class TextBlock(
        val parseResult: RichTextParser.ParseResult,
        val rawStart: Int
    ) : NoteContentBlock {
        val annotatedString: androidx.compose.ui.text.AnnotatedString get() = parseResult.text

        @Composable
        override fun render(context: BlockRenderContext, modifier: Modifier) {
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
                    DeleteOverlay { context.onDeleteBlock(this@TextBlock) }
                }
            }
        }
    }

    data class ChecklistItemBlock(
        val isChecked: Boolean,
        val parseResult: RichTextParser.ParseResult,
        val rawStart: Int,
        val globalIndex: Int
    ) : NoteContentBlock {
        val text: androidx.compose.ui.text.AnnotatedString get() = parseResult.text

        @Composable
        override fun render(context: BlockRenderContext, modifier: Modifier) {
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
                DeleteOverlay { context.onDeleteBlock(this@ChecklistItemBlock) }
            }
        }
    }

    data class ImageBlock(val src: String) : NoteContentBlock {
        @Composable
        override fun render(context: BlockRenderContext, modifier: Modifier) {
            Card(
                modifier = modifier.fillMaxWidth().heightIn(max = 200.dp).wrapContentHeight(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Box {
                    AsyncImage(
                        model = src,
                        contentDescription = stringResource(R.string.attachment_image),
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .clickable { context.onNavigateToMediaViewer("image", src) },
                        contentScale = ContentScale.FillWidth
                    )
                    DeleteOverlay { context.onDeleteBlock(this@ImageBlock) }
                }
            }
        }
    }

    data class VideoBlock(val src: String) : NoteContentBlock {
        @Composable
        override fun render(context: BlockRenderContext, modifier: Modifier) {
            Card(
                modifier = modifier.fillMaxWidth().height(200.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier.fillMaxSize()
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
                    DeleteOverlay { context.onDeleteBlock(this@VideoBlock) }
                }
            }
        }
    }

    data class AudioBlock(val src: String) : NoteContentBlock {
        @Composable
        override fun render(context: BlockRenderContext, modifier: Modifier) {
            Card(
                modifier = modifier.fillMaxWidth().wrapContentHeight(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Box {
                    com.example.ui.AudioPlayerWidget(path = src, modifier = Modifier.fillMaxWidth().padding(12.dp))
                    DeleteOverlay { context.onDeleteBlock(this@AudioBlock) }
                }
            }
        }
    }

    data class DrawingBlock(val jsonPath: String, val previewPath: String) : NoteContentBlock {
        @Composable
        override fun render(context: BlockRenderContext, modifier: Modifier) {
            Card(
                modifier = modifier.fillMaxWidth().heightIn(max = 200.dp).wrapContentHeight(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Box {
                    AsyncImage(
                        model = previewPath,
                        contentDescription = stringResource(R.string.attachment_drawing),
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White)
                            .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                            .clickable { context.onNavigateToDrawing(context.noteId, jsonPath) },
                        contentScale = ContentScale.Fit
                    )
                    DeleteOverlay { context.onDeleteBlock(this@DrawingBlock) }
                }
            }
        }
    }

    data class VoiceBlock(val path: String) : NoteContentBlock {
        @Composable
        override fun render(context: BlockRenderContext, modifier: Modifier) {
            Card(
                modifier = modifier.fillMaxWidth().wrapContentHeight(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Box {
                    com.example.ui.AudioPlayerWidget(path = path, modifier = Modifier.fillMaxWidth().padding(12.dp))
                    DeleteOverlay { context.onDeleteBlock(this@VoiceBlock) }
                }
            }
        }
    }

    data class FileBlock(val name: String, val path: String) : NoteContentBlock {
        @Composable
        override fun render(context: BlockRenderContext, modifier: Modifier) {
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
                        Text(text = name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    }
                    DeleteOverlay { context.onDeleteBlock(this@FileBlock) }
                }
            }
        }
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
