package com.example.ui.editor

import com.example.data.model.DataBlock
import com.example.util.RichTextConverter

data class SlashCommandSelection(
    val blocks: List<DataBlock>,
    val activeBlockIndex: Int,
    val pendingFocusBlockIndex: Int
)

fun selectSlashCommand(
    blocks: List<DataBlock>,
    activeBlockIndex: Int,
    pendingFocusBlockIndex: Int,
    selectedBlock: DataBlock
): SlashCommandSelection {
    val currentIndex = activeBlockIndex.coerceIn(0, blocks.size)
    val isSlashPlaceholder = hasSlashPlaceholder(blocks, currentIndex)
    val insertion = insertionIndex(currentIndex, isSlashPlaceholder, blocks.size)
    val updatedBlocks = blocks.toMutableList()
    if (isSlashPlaceholder) {
        updatedBlocks[currentIndex] = selectedBlock
    } else {
        updatedBlocks.add(insertion, selectedBlock)
    }
    return SlashCommandSelection(
        blocks = updatedBlocks,
        activeBlockIndex = insertion.coerceAtMost(updatedBlocks.size - 1),
        pendingFocusBlockIndex = if (isSlashPlaceholder) pendingFocusBlockIndex else insertion
    )
}

private fun hasSlashPlaceholder(blocks: List<DataBlock>, index: Int): Boolean =
    blocks.getOrNull(index)
        ?.ensureSegments()
        ?.let(RichTextConverter::segmentsToPlainText)
        ?.startsWith("/") == true
