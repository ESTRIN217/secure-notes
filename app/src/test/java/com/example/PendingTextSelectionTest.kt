package com.example

import com.example.data.model.BlockType
import com.example.data.model.DataBlock
import com.example.ui.editor.PendingSelectionDecision
import com.example.ui.editor.PendingTextSelection
import com.example.ui.editor.PendingTextSelectionState
import com.example.ui.editor.consumePendingTextSelection
import org.junit.Assert.assertEquals
import org.junit.Test

class PendingTextSelectionTest {

    @Test
    fun `selection is applied to its originating note and block`() {
        val origin = textBlock("source")
        val state = pending(noteId = 4, blockIndex = 2, block = origin, range = 3..7)

        val result = consumePendingTextSelection(state, 4, 2, origin)

        assertEquals(PendingSelectionDecision.Apply(3..7), result.decision)
        assertEquals(PendingTextSelectionState(), result.state)
    }

    @Test
    fun `selection from another note is discarded`() {
        val origin = textBlock("same block")
        val state = pending(noteId = 4, blockIndex = 2, block = origin)

        val result = consumePendingTextSelection(state, 5, 2, origin)

        assertEquals(PendingSelectionDecision.Discard, result.decision)
        assertEquals(PendingTextSelectionState(), result.state)
    }

    @Test
    fun `selection from another block index is discarded`() {
        val origin = textBlock("same block")
        val state = pending(noteId = 4, blockIndex = 2, block = origin)

        val result = consumePendingTextSelection(state, 4, 3, origin)

        assertEquals(PendingSelectionDecision.Discard, result.decision)
        assertEquals(PendingTextSelectionState(), result.state)
    }

    @Test
    fun `selection for a changed block snapshot is discarded`() {
        val origin = textBlock("before edit")
        val changed = textBlock("after edit")
        val state = pending(noteId = 4, blockIndex = 2, block = origin)

        val result = consumePendingTextSelection(state, 4, 2, changed)

        assertEquals(PendingSelectionDecision.Discard, result.decision)
        assertEquals(PendingTextSelectionState(), result.state)
    }

    @Test
    fun `empty state has no selection to consume`() {
        val result = consumePendingTextSelection(
            PendingTextSelectionState(),
            4,
            2,
            textBlock("current")
        )

        assertEquals(PendingSelectionDecision.None, result.decision)
        assertEquals(PendingTextSelectionState(), result.state)
    }

    @Test
    fun `consumed selection cannot be applied a second time`() {
        val block = textBlock("source")
        val first = consumePendingTextSelection(
            pending(noteId = 4, blockIndex = 2, block = block, range = 1..4),
            4,
            2,
            block
        )

        val second = consumePendingTextSelection(first.state, 4, 2, block)

        assertEquals(PendingSelectionDecision.Apply(1..4), first.decision)
        assertEquals(PendingSelectionDecision.None, second.decision)
    }

    private fun pending(
        noteId: Int,
        blockIndex: Int,
        block: DataBlock,
        range: IntRange = 0..0
    ) = PendingTextSelectionState(
        PendingTextSelection(noteId, blockIndex, block, range)
    )

    private fun textBlock(content: String) = DataBlock(BlockType.TEXT, content)
}
