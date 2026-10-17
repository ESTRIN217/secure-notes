package com.example

import com.example.data.model.BlockType
import com.example.data.model.DataBlock
import com.example.ui.editor.selectSlashCommand
import org.junit.Assert.assertEquals
import org.junit.Test

class SlashCommandSelectionTest {

    @Test
    fun `slash placeholder is replaced without requesting focus again`() {
        val placeholder = DataBlock(BlockType.TEXT, "/image")
        val command = DataBlock(BlockType.IMAGE, "image.png")

        val result = selectSlashCommand(listOf(placeholder), 0, 7, command)

        assertEquals(listOf(command), result.blocks)
        assertEquals(0, result.activeBlockIndex)
        assertEquals(7, result.pendingFocusBlockIndex)
    }

    @Test
    fun `non slash text stays and command is inserted immediately after`() {
        val blocks = listOf(
            DataBlock(BlockType.TEXT, "before"),
            DataBlock(BlockType.TEXT, "ordinary text"),
            DataBlock(BlockType.TEXT, "after")
        )
        val command = DataBlock(BlockType.PAGE)

        val result = selectSlashCommand(blocks, 1, -1, command)

        assertEquals(listOf(blocks[0], blocks[1], command, blocks[2]), result.blocks)
        assertEquals(2, result.activeBlockIndex)
        assertEquals(2, result.pendingFocusBlockIndex)
    }

    @Test
    fun `empty active block is not treated as a slash placeholder`() {
        val empty = DataBlock(BlockType.TEXT)
        val command = DataBlock(BlockType.TABLE)

        val result = selectSlashCommand(listOf(empty), 0, -1, command)

        assertEquals(listOf(empty, command), result.blocks)
        assertEquals(1, result.activeBlockIndex)
        assertEquals(1, result.pendingFocusBlockIndex)
    }

    @Test
    fun `command added to an empty document becomes its first active block`() {
        val command = DataBlock(BlockType.TEXT, "created")

        val result = selectSlashCommand(emptyList(), 0, -1, command)

        assertEquals(listOf(command), result.blocks)
        assertEquals(0, result.activeBlockIndex)
        assertEquals(0, result.pendingFocusBlockIndex)
    }
}
