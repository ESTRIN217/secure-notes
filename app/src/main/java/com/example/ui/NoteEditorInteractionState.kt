package com.example.ui

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.TextFieldValue
import com.example.data.model.DataBlock
import com.example.data.model.TextSegment
import com.example.ui.editor.PendingTextSelection
import com.example.ui.editor.PendingTextSelectionState
import com.example.util.RichTextConverter

internal class NoteEditorInteractionState {
    val contentValue = mutableStateOf(TextFieldValue(""))
    val toolbarActiveBlockIndex = mutableIntStateOf(0)
    val toolbarActiveCursorOffset = mutableIntStateOf(0)
    val activeSelection = mutableStateOf(IntRange(0, 0))
    val pendingTypingStyle = mutableStateOf<TextSegment?>(null)
    val pendingFocusBlockIndex = mutableIntStateOf(-1)
    val pendingSelection = mutableStateOf(PendingTextSelectionState())
    val toolbarParseResult = mutableStateOf(RichTextConverter.parseResultFor(emptyList()))

    fun setContentValue(value: TextFieldValue, noteId: Int, blockIndex: Int, blocks: List<DataBlock>) {
        contentValue.value = value
        activeSelection.value = value.selection.start..value.selection.end
        publishPendingSelection(noteId, blockIndex, blocks, value.selection.start..value.selection.end)
    }

    fun publishPendingSelection(noteId: Int, blockIndex: Int, blocks: List<DataBlock>, range: IntRange) {
        val block = blocks.getOrNull(blockIndex)
        pendingSelection.value = block?.let {
            PendingTextSelectionState(PendingTextSelection(noteId, blockIndex, it, range))
        } ?: PendingTextSelectionState()
    }
}
