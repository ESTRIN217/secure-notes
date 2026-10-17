package com.example

import com.example.data.model.Attachment
import com.example.data.model.BlockType
import com.example.data.model.DataBlock
import com.example.data.model.createRawContent
import com.example.data.model.parseNoteContentAndAttachments
import com.example.ui.editor.prepareNoteBlockMove
import com.example.util.RichTextConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NoteBlockMovePlanTest {

    @Test
    fun `move transfers only the selected block and preserves target data and attachments`() {
        val movedBlock = textBlock("moving").copy(meta = mapOf("marker" to "kept"))
        val source = mutableListOf(textBlock("first"), movedBlock, textBlock("last"))
        val originalSource = source.toList()
        val attachments = listOf(Attachment(type = "image", path = "/target/image.png", name = "image"))
        val targetContent = createRawContent(
            DataBlock.serialize(listOf(textBlock("target"))),
            attachments
        )

        val result = prepareNoteBlockMove(source, 1, targetContent)!!

        assertEquals(listOf("first", "last"), result.sourceBlocks.map(::plainText))
        assertEquals(source, listOf(result.sourceBlocks[0], result.movedBlock, result.sourceBlocks[1]))
        val (targetText, movedAttachments) = parseNoteContentAndAttachments(result.destinationContent)
        val targetBlocks = DataBlock.deserialize(targetText)!!
        assertEquals(listOf("target", "moving"), targetBlocks.map(::plainText))
        assertEquals("kept", targetBlocks.last().meta["marker"])
        assertEquals(attachments, movedAttachments)
        assertEquals(originalSource, source)
    }

    @Test
    fun `move retains legacy target text before the moved block`() {
        val source = listOf(textBlock("moving"))

        val result = prepareNoteBlockMove(source, 0, "legacy target")!!

        val targetText = parseNoteContentAndAttachments(result.destinationContent).first
        val targetBlocks = DataBlock.deserialize(targetText)!!
        assertEquals(listOf("legacy target", "moving"), targetBlocks.map(::plainText))
    }

    @Test
    fun `invalid source index produces no move`() {
        val source = mutableListOf(textBlock("keep"))
        val originalSource = source.toList()

        assertNull(prepareNoteBlockMove(source, 1, "target"))
        assertEquals(originalSource, source)
        assertEquals(listOf("keep"), source.map(::plainText))
    }

    private fun textBlock(content: String) = DataBlock(type = BlockType.TEXT, content = content)

    private fun plainText(block: DataBlock) =
        RichTextConverter.segmentsToPlainText(block.ensureSegments())
}
