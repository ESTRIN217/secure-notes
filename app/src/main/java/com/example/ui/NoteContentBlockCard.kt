package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.data.model.BlockRenderContext
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
    val context = BlockRenderContext(
        content = content,
        noteId = noteId,
        onDeleteBlock = onDeleteBlock,
        onNavigateToMediaViewer = onNavigateToMediaViewer,
        onNavigateToDrawing = onNavigateToDrawing,
        onUrlClicked = onUrlClicked
    )
    block.render(context, modifier)
}
