package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.data.model.BlockRenderContext
import com.example.data.model.NoteContentBlock

@Composable
fun NoteContentBlockCard(
    block: NoteContentBlock,
    content: String? = null,
    noteId: Int = 0,
    attachments: List<com.example.data.model.Attachment>? = null,
    onDeleteBlock: ((NoteContentBlock) -> Unit)? = null,
    onNavigateToMediaViewer: ((String, String) -> Unit)? = null,
    onNavigateToDrawing: ((Int, String?) -> Unit)? = null,
    onUrlClicked: ((url: String, rawOffset: Int) -> Unit)? = null,
    onChecklistToggle: ((globalIndex: Int, isChecked: Boolean) -> Unit)? = null,
    onOpenBlockMore: ((NoteContentBlock) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = BlockRenderContext(
        content = content ?: "",
        noteId = noteId,
        onDeleteBlock = onDeleteBlock ?: {},
        onNavigateToMediaViewer = onNavigateToMediaViewer ?: { _, _ -> },
        onNavigateToDrawing = onNavigateToDrawing ?: { _, _ -> },
        onUrlClicked = onUrlClicked ?: { _, _ -> },
        onChecklistToggle = onChecklistToggle ?: { _, _ -> },
        onOpenBlockMore = onOpenBlockMore
    )
    block.RenderContent(context, modifier)
}
