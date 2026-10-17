package com.example.ui.editor

import com.example.data.model.DataBlock

data class PendingTextSelection(
    val noteId: Int,
    val blockIndex: Int,
    val blockSnapshot: DataBlock,
    val range: IntRange
)

data class PendingTextSelectionState(
    val pending: PendingTextSelection? = null
)

sealed interface PendingSelectionDecision {
    data class Apply(val range: IntRange) : PendingSelectionDecision
    data object Discard : PendingSelectionDecision
    data object None : PendingSelectionDecision
}

data class PendingSelectionConsumption(
    val decision: PendingSelectionDecision,
    val state: PendingTextSelectionState
)

fun consumePendingTextSelection(
    state: PendingTextSelectionState,
    currentNoteId: Int,
    currentBlockIndex: Int,
    currentBlock: DataBlock
): PendingSelectionConsumption {
    val pending = state.pending ?: return PendingSelectionConsumption(PendingSelectionDecision.None, state)
    val nextState = PendingTextSelectionState()
    if (pending.noteId != currentNoteId ||
        pending.blockIndex != currentBlockIndex ||
        pending.blockSnapshot != currentBlock
    ) {
        return PendingSelectionConsumption(PendingSelectionDecision.Discard, nextState)
    }
    return PendingSelectionConsumption(PendingSelectionDecision.Apply(pending.range), nextState)
}
