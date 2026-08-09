package com.example.data.model

import androidx.compose.ui.text.style.TextAlign
import com.example.util.RichTextParser

data class BlockRenderContext(
    val content: String,
    val noteId: Int,
    val onDeleteBlock: (NoteContentBlock) -> Unit,
    val onNavigateToMediaViewer: (String, String) -> Unit,
    val onNavigateToDrawing: (Int, String?) -> Unit,
    val onUrlClicked: (url: String, rawOffset: Int) -> Unit,
    val onEditTable: ((NoteContentBlock.TableBlock) -> Unit)? = null,
    val onChecklistToggle: ((globalIndex: Int, isChecked: Boolean) -> Unit)? = null,
    val onOpenBlockMore: ((NoteContentBlock) -> Unit)? = null
)

sealed interface NoteContentBlock {
    data class TextBlock(
        val parseResult: RichTextParser.ParseResult,
        val rawStart: Int,
        val textAlign: TextAlign? = null
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

    data class ImageBlock(
        val src: String,
        val linkUrl: String? = null,
        val caption: String? = null,
        val align: String? = null
    ) : NoteContentBlock
    data class VideoBlock(val src: String) : NoteContentBlock
    data class AudioBlock(val src: String) : NoteContentBlock
    data class DrawingBlock(val jsonPath: String, val previewPath: String) : NoteContentBlock
    data class VoiceBlock(val path: String) : NoteContentBlock
    data class FileBlock(val name: String, val path: String) : NoteContentBlock

    data class TableBlock(
        val headers: List<String>,
        val rows: List<List<String>>,
        val columnAlignment: List<ColumnAlignment> = emptyList(),
        val cellAlignment: List<List<ColumnAlignment>> = emptyList()
    ) : NoteContentBlock

    data object HorizontalRuleBlock : NoteContentBlock

    data class CollapsibleBlock(
        val summary: String,
        val content: String,
        val isExpanded: Boolean = false
    ) : NoteContentBlock
}

enum class ColumnAlignment { Start, Center, End }
