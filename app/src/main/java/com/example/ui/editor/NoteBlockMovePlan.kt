package com.example.ui.editor

import com.example.data.model.DataBlock
import com.example.data.model.createRawContent
import com.example.data.model.parseNoteContentAndAttachments
import com.example.util.RichTextConverter

internal data class NoteBlockMovePlan(
    val sourceBlocks: List<DataBlock>,
    val destinationContent: String,
    val movedBlock: DataBlock
)

internal fun prepareNoteBlockMove(
    sourceBlocks: List<DataBlock>,
    sourceIndex: Int,
    destinationContent: String
): NoteBlockMovePlan? {
    val movedBlock = sourceBlocks.getOrNull(sourceIndex) ?: return null
    val (text, attachments) = parseNoteContentAndAttachments(destinationContent)
    val destinationBlocks = (
        RichTextConverter.contentToBlocks(text) ?: DataBlock.migrateLegacyContent(text)
    ).let { DataBlock.migrateToSegments(it).first }.toMutableList()
    destinationBlocks.add(movedBlock)
    val remainingSourceBlocks = sourceBlocks.toMutableList().also { it.removeAt(sourceIndex) }
    return NoteBlockMovePlan(
        sourceBlocks = remainingSourceBlocks,
        destinationContent = createRawContent(DataBlock.serialize(destinationBlocks), attachments),
        movedBlock = movedBlock
    )
}
