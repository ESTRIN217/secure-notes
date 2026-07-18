package com.example.data.model

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
    data class TextBlock(
        val parseResult: RichTextParser.ParseResult,
        val rawStart: Int
    ) : NoteContentBlock {
        val annotatedString: androidx.compose.ui.text.AnnotatedString get() = parseResult.text
    }

    data class ChecklistItemBlock(
        val isChecked: Boolean,
        val parseResult: RichTextParser.ParseResult,
        val rawStart: Int,
        val globalIndex: Int
    ) : NoteContentBlock {
        val text: androidx.compose.ui.text.AnnotatedString get() = parseResult.text
    }

    data class ImageBlock(val src: String) : NoteContentBlock
    data class VideoBlock(val src: String) : NoteContentBlock
    data class AudioBlock(val src: String) : NoteContentBlock
    data class DrawingBlock(val jsonPath: String, val previewPath: String) : NoteContentBlock
    data class VoiceBlock(val path: String) : NoteContentBlock
    data class FileBlock(val name: String, val path: String) : NoteContentBlock
}
