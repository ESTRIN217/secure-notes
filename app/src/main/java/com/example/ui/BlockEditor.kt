package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.model.Attachment
import com.example.data.model.NoteContentBlock
import com.example.util.BlockRange
import com.example.util.parseEditorBlockRanges
import com.example.util.removeMediaFromContent
import com.example.util.removeAttachmentFromContent
import com.example.util.toggleNthChecklistItem

@Composable
fun BlockEditor(
    rawContent: String,
    onRawContentChange: (String) -> Unit,
    attachments: List<Attachment>,
    noteId: Int,
    onNavigateToMediaViewer: (String, String) -> Unit,
    onNavigateToDrawing: (Int, String?) -> Unit,
    pendingTagInsert: MutableState<String?>,
    pendingInsert: MutableState<String?> = remember { mutableStateOf(null) },
    onActiveBlockChange: (Int) -> Unit = {},
    onActiveCursorChange: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val blockRanges by remember(rawContent) {
        mutableStateOf(parseEditorBlockRanges(rawContent))
    }
    var activeBlockIndex by remember { mutableIntStateOf(-1) }
    var activeCursorOffset by remember { mutableIntStateOf(0) }

    LaunchedEffect(pendingTagInsert.value) {
        val tag = pendingTagInsert.value ?: return@LaunchedEffect
        if (activeBlockIndex == -1) return@LaunchedEffect

        val (block, range) = blockRanges.getOrNull(activeBlockIndex) ?: return@LaunchedEffect
        if (block !is NoteContentBlock.TextBlock) return@LaunchedEffect

        val blockRaw = rawContent.substring(range)
        val insertPos = activeCursorOffset.coerceIn(0, blockRaw.length)
        val newBlockRaw = blockRaw.substring(0, insertPos) + tag + blockRaw.substring(insertPos)
        val newContent = rawContent.replaceRange(range, newBlockRaw)
        onRawContentChange(newContent)
        pendingTagInsert.value = null
    }

    LaunchedEffect(pendingInsert.value) {
        val insert = pendingInsert.value ?: return@LaunchedEffect
        if (activeBlockIndex == -1 || activeBlockIndex >= blockRanges.size) {
            onRawContentChange(rawContent + "\n" + insert)
            pendingInsert.value = null
            return@LaunchedEffect
        }
        val (block, range) = blockRanges.getOrNull(activeBlockIndex) ?: run {
            onRawContentChange(rawContent + "\n" + insert)
            pendingInsert.value = null
            return@LaunchedEffect
        }
        if (block !is NoteContentBlock.TextBlock) {
            onRawContentChange(rawContent + "\n" + insert)
            pendingInsert.value = null
            return@LaunchedEffect
        }
        val blockRaw = rawContent.substring(range)
        val insertPos = activeCursorOffset.coerceIn(0, blockRaw.length)
        val newBlockRaw = blockRaw.substring(0, insertPos) + insert + blockRaw.substring(insertPos)
        val newContent = rawContent.replaceRange(range, newBlockRaw)
        onRawContentChange(newContent)
        pendingInsert.value = null
    }

    val attachmentBlocks = remember(attachments) {
        attachments.mapNotNull { att ->
            when (att.type) {
                "drawing" -> NoteContentBlock.DrawingBlock(jsonPath = att.path, previewPath = att.name)
                "voice" -> NoteContentBlock.VoiceBlock(path = att.path)
                "file" -> NoteContentBlock.FileBlock(name = att.name.ifEmpty { att.path.substringAfterLast('/') }, path = att.path)
                else -> null
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        blockRanges.forEachIndexed { index, (block, range) ->
            when (block) {
                is NoteContentBlock.TextBlock -> {
                    val rawSubstring = rawContent.substring(range.first, range.last + 1)
                    EditableTextBlock(
                        rawText = rawSubstring,
                        onChange = { newRawText ->
                            val newContent = rawContent.replaceRange(range, newRawText)
                            onRawContentChange(newContent)
                        },
                        onFocusChange = { focused ->
                            if (focused) {
                                activeBlockIndex = index
                                onActiveBlockChange(index)
                            } else if (activeBlockIndex == index) {
                                activeBlockIndex = -1
                                onActiveBlockChange(-1)
                            }
                        },
                        onCursorChange = { cursor ->
                            activeCursorOffset = cursor
                            onActiveCursorChange(cursor)
                        }
                    )
                }
                else -> {
                    NoteContentBlockCard(
                        block = block,
                        content = rawContent,
                        noteId = noteId,
                        onDeleteBlock = { deletedBlock ->
                            val newContent = when (deletedBlock) {
                                is NoteContentBlock.ImageBlock ->
                                    removeMediaFromContent(rawContent, deletedBlock.src, "image")
                                is NoteContentBlock.VideoBlock ->
                                    removeMediaFromContent(rawContent, deletedBlock.src, "video")
                                is NoteContentBlock.AudioBlock ->
                                    removeMediaFromContent(rawContent, deletedBlock.src, "audio")
                                is NoteContentBlock.TableBlock,
                                is NoteContentBlock.HorizontalRuleBlock ->
                                    rawContent.replaceRange(range, "")
                                is NoteContentBlock.DrawingBlock -> {
                                    val target = attachments.find { it.type == "drawing" && it.path == deletedBlock.jsonPath }
                                    if (target != null) {
                                        removeAttachmentFromContent(rawContent, target)
                                    } else rawContent
                                }
                                is NoteContentBlock.VoiceBlock -> {
                                    val target = attachments.find { it.type == "voice" && it.path == deletedBlock.path }
                                    if (target != null) {
                                        removeAttachmentFromContent(rawContent, target)
                                    } else rawContent
                                }
                                is NoteContentBlock.FileBlock -> {
                                    val target = attachments.find { it.type != "drawing" && it.type != "voice" && it.name == deletedBlock.name }
                                    if (target != null) {
                                        removeAttachmentFromContent(rawContent, target)
                                    } else rawContent
                                }
                                else -> rawContent
                            }
                            if (newContent != rawContent) {
                                onRawContentChange(newContent)
                            }
                        },
                        onNavigateToMediaViewer = onNavigateToMediaViewer,
                        onNavigateToDrawing = onNavigateToDrawing,
                        onUrlClicked = { _, _ -> },
                        onChecklistToggle = { globalIndex, _ ->
                            val newText = toggleNthChecklistItem(rawContent, globalIndex)
                            if (newText != rawContent) {
                                onRawContentChange(newText)
                            }
                        }
                    )
                }
            }
        }
        attachmentBlocks.forEach { block ->
            NoteContentBlockCard(
                block = block,
                content = rawContent,
                noteId = noteId,
                onDeleteBlock = { deletedBlock ->
                    val target = when (deletedBlock) {
                        is NoteContentBlock.DrawingBlock -> attachments.find { it.type == "drawing" && it.path == deletedBlock.jsonPath }
                        is NoteContentBlock.VoiceBlock -> attachments.find { it.type == "voice" && it.path == deletedBlock.path }
                        is NoteContentBlock.FileBlock -> attachments.find { it.type != "drawing" && it.type != "voice" && it.name == deletedBlock.name }
                        else -> null
                    }
                    if (target != null) {
                        val newContent = removeAttachmentFromContent(rawContent, target)
                        if (newContent != rawContent) {
                            onRawContentChange(newContent)
                        }
                    }
                },
                onNavigateToMediaViewer = onNavigateToMediaViewer,
                onNavigateToDrawing = onNavigateToDrawing,
                onUrlClicked = { _, _ -> }
            )
        }
    }
}
