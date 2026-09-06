package com.example.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DecryptedNote
import com.example.R
import com.example.ui.viewmodel.AiViewModel
import com.example.ui.viewmodel.NotesViewModel

sealed interface PendingTabAction {
    data class Select(val targetId: Int) : PendingTabAction
    data class Close(val targetId: Int) : PendingTabAction
    data class CloseOthers(val keepId: Int) : PendingTabAction
    data object CloseAll : PendingTabAction
    data object Back : PendingTabAction
}

@Composable
fun NoteEditorTabsHost(
    noteId: Int,
    tabsManager: OpenNoteTabs,
    viewModel: NotesViewModel,
    aiViewModel: AiViewModel,
    onSelectNote: (Int) -> Unit,
    onBack: () -> Unit,
    onNavigateToDrawing: (Int, String?) -> Unit,
    onNavigateToMediaViewer: (String, String) -> Unit,
    onNavigateToAiChat: (Int) -> Unit,
    onNavigateToNote: (Int) -> Unit
) {
    val tabs by tabsManager.tabs.collectAsStateWithLifecycle()
    val selectedIndex by tabsManager.selectedIndex.collectAsStateWithLifecycle()
    val notes by viewModel.notesList.collectAsStateWithLifecycle()
    val guard = remember(noteId) { DraftGuard() }
    var pending by remember(noteId) { mutableStateOf<PendingTabAction?>(null) }
    PruneMissingTabs(tabs, notes, tabsManager, noteId, onSelectNote, onBack)
    key(noteId) {
        NoteEditorScreen(
            noteId = noteId,
            viewModel = viewModel,
            aiViewModel = aiViewModel,
            onBack = { requestLeaveForBack(tabs, selectedIndex, guard, { pending = PendingTabAction.Back }, onBack) },
            onNavigateToDrawing = onNavigateToDrawing,
            onNavigateToMediaViewer = onNavigateToMediaViewer,
            onNavigateToAiChat = onNavigateToAiChat,
            onNavigateToNote = onNavigateToNote,
            tabBarContent = {
                OpenNoteTabBar(
                    tabs = tabs,
                    selectedIndex = selectedIndex,
                    titleFor = { id -> tabTitle(id, notes) },
                    onSelect = { i -> gateOrRun(tabs, selectedIndex, guard, PendingTabAction.Select(tabs[i].noteId), { pending = PendingTabAction.Select(tabs[i].noteId) }) { selectTabById(tabsManager, tabs[i].noteId); onSelectNote(tabs[i].noteId) } },
                    onClose = { i -> gateOrRun(tabs, selectedIndex, guard, PendingTabAction.Close(tabs[i].noteId), { pending = PendingTabAction.Close(tabs[i].noteId) }) { val next = closeTabById(tabsManager, tabs[i].noteId); if (next != null) onSelectNote(next) else onBack() } },
                    onCloseOthers = { i -> gateOrRun(tabs, selectedIndex, guard, PendingTabAction.CloseOthers(tabs[i].noteId), { pending = PendingTabAction.CloseOthers(tabs[i].noteId) }) { closeOthersById(tabsManager, tabs[i].noteId); onSelectNote(tabs[i].noteId) } },
                    onCloseAll = { gateOrRun(tabs, selectedIndex, guard, PendingTabAction.CloseAll, { pending = PendingTabAction.CloseAll }) { tabsManager.closeAll(); onBack() } }
                )
            },
            draftGuard = guard
        )
    }
    DraftGuardDialog(
        visible = pending != null && guard.hasUnsaved && isDraftTab(tabs, selectedIndex),
        onSave = { saveAndResume(guard, tabsManager, pending, noteId, onSelectNote, onBack) { pending = null } },
        onDiscard = { discardAndResume(guard, tabsManager, pending, noteId, onSelectNote, onBack) { pending = null } },
        onCancel = { pending = null }
    )
}

@Composable
private fun PruneMissingTabs(
    tabs: List<OpenTab>,
    notes: List<DecryptedNote>,
    tabsManager: OpenNoteTabs,
    noteId: Int,
    onSelectNote: (Int) -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(tabs, notes) {
        if (notes.isEmpty() || tabs.isEmpty()) return@LaunchedEffect
        tabsManager.pruneMissing(notes.map { it.note.id }.toSet())
        val remaining = tabsManager.tabs.value
        if (remaining.none { it.noteId == noteId }) {
            val fallback = remaining.getOrNull(tabsManager.selectedIndex.value)?.noteId
            if (fallback != null) onSelectNote(fallback) else onBack()
        }
    }
}

@Composable
private fun tabTitle(noteId: Int, notes: List<DecryptedNote>): String {
    if (noteId == 0) return stringResource(R.string.tabs_new_note)
    val title = notes.find { it.note.id == noteId }?.title?.trim()
    if (!title.isNullOrEmpty()) return title
    return stringResource(R.string.untitled_note)
}

private fun isDraftTab(tabs: List<OpenTab>, selectedIndex: Int): Boolean {
    return tabs.getOrNull(selectedIndex)?.noteId == 0
}

private fun gateOrRun(
    tabs: List<OpenTab>,
    selectedIndex: Int,
    guard: DraftGuard,
    action: PendingTabAction,
    defer: (PendingTabAction) -> Unit,
    run: () -> Unit
) {
    if (!isDraftTab(tabs, selectedIndex) || !guard.hasUnsaved) {
        run()
        return
    }
    defer(action)
}

private fun requestLeaveForBack(
    tabs: List<OpenTab>,
    selectedIndex: Int,
    guard: DraftGuard,
    setPending: () -> Unit,
    onBack: () -> Unit
) {
    if (!isDraftTab(tabs, selectedIndex) || !guard.hasUnsaved) {
        onBack()
        return
    }
    setPending()
}

private fun selectTabById(tabsManager: OpenNoteTabs, targetId: Int) {
    val index = tabsManager.tabs.value.indexOfFirst { it.noteId == targetId }
    if (index >= 0) tabsManager.selectTab(index)
}

private fun closeTabById(tabsManager: OpenNoteTabs, targetId: Int): Int? {
    val index = tabsManager.tabs.value.indexOfFirst { it.noteId == targetId }
    if (index < 0) return tabsManager.selectedTab?.noteId
    return tabsManager.closeTab(index)
}

private fun saveAndResume(
    guard: DraftGuard,
    tabsManager: OpenNoteTabs,
    pending: PendingTabAction?,
    noteId: Int,
    onSelectNote: (Int) -> Unit,
    onBack: () -> Unit,
    clear: () -> Unit
) {
    guard.saveAction { newId ->
        tabsManager.promoteDraft(newId)
        clear()
        resumeAfterSave(tabsManager, pending, newId, noteId, onSelectNote, onBack)
    }
}

private fun resumeAfterSave(
    tabsManager: OpenNoteTabs,
    pending: PendingTabAction?,
    newId: Int,
    noteId: Int,
    onSelectNote: (Int) -> Unit,
    onBack: () -> Unit
) {
    when (pending) {
        is PendingTabAction.Select -> {
            selectTabById(tabsManager, pending.targetId)
            onSelectNote(pending.targetId)
        }
        is PendingTabAction.Close -> {
            val closedId = if (pending.targetId == noteId) newId else pending.targetId
            val next = closeTabById(tabsManager, closedId)
            if (next != null) onSelectNote(next) else onBack()
        }
        is PendingTabAction.CloseOthers -> {
            closeOthersById(tabsManager, pending.keepId)
            onSelectNote(pending.keepId)
        }
        PendingTabAction.CloseAll -> {
            tabsManager.closeAll()
            onBack()
        }
        PendingTabAction.Back, null -> onBack()
    }
}

private fun discardAndResume(
    guard: DraftGuard,
    tabsManager: OpenNoteTabs,
    pending: PendingTabAction?,
    noteId: Int,
    onSelectNote: (Int) -> Unit,
    onBack: () -> Unit,
    clear: () -> Unit
) {
    guard.discardAction()
    clear()
    when (pending) {
        is PendingTabAction.Select -> {
            selectTabById(tabsManager, pending.targetId)
            onSelectNote(pending.targetId)
        }
        is PendingTabAction.Close -> {
            val next = closeTabById(tabsManager, pending.targetId)
            if (next != null) onSelectNote(next) else onBack()
        }
        is PendingTabAction.CloseOthers -> {
            closeOthersById(tabsManager, pending.keepId)
            onSelectNote(pending.keepId)
        }
        PendingTabAction.CloseAll -> {
            tabsManager.closeAll()
            onBack()
        }
        PendingTabAction.Back -> {
            closeTabById(tabsManager, noteId)
            onBack()
        }
        null -> onBack()
    }
}

private fun closeOthersById(tabsManager: OpenNoteTabs, keepId: Int) {
    val index = tabsManager.tabs.value.indexOfFirst { it.noteId == keepId }
    if (index >= 0) tabsManager.closeOthers(index)
}

@Composable
private fun DraftGuardDialog(
    visible: Boolean,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onCancel: () -> Unit
) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.tabs_draft_dialog_title)) },
        text = { Text(stringResource(R.string.tabs_draft_dialog_message)) },
        confirmButton = {
            TextButton(onClick = onSave) { Text(stringResource(R.string.tabs_draft_save)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDiscard) { Text(stringResource(R.string.tabs_draft_discard)) }
                TextButton(onClick = onCancel) { Text(stringResource(R.string.btn_cancel)) }
            }
        }
    )
}
