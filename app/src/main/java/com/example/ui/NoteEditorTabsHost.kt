package com.example.ui

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.estrin217.codetools.CodeTools
import com.estrin217.codetools.ui.CodeEditorViewModel
import com.estrin217.codetools.ui.CodeToolsApp
import com.estrin217.pdfviewer.ui.PdfViewerScreen
import com.estrin217.pdfviewer.ui.PdfViewerViewModel
import com.example.R
import com.estrin217.visormedia.ui.MediaViewerScreen
import com.example.data.model.DecryptedNote
import com.example.ui.viewmodel.AiViewModel
import com.example.ui.viewmodel.NotesViewModel
import java.io.File

sealed interface PendingTabAction {
    data class Select(val targetKey: String) : PendingTabAction
    data class Close(val targetKey: String) : PendingTabAction
    data class CloseOthers(val keepKey: String) : PendingTabAction
    data object CloseAll : PendingTabAction
    data object Back : PendingTabAction
}

@Composable
fun NoteEditorTabsHost(
    anchorNoteId: Int,
    tabsManager: OpenNoteTabs,
    viewModel: NotesViewModel,
    aiViewModel: AiViewModel,
    onSelectNote: (Int) -> Unit,
    onBack: () -> Unit,
    onNavigateToDrawing: (Int, String?) -> Unit,
    onNavigateToAiChat: (Int) -> Unit,
    onNavigateToNote: (Int) -> Unit
) {
    val tabs by tabsManager.tabs.collectAsStateWithLifecycle()
    val selectedIndex by tabsManager.selectedIndex.collectAsStateWithLifecycle()
    val notes by viewModel.notesList.collectAsStateWithLifecycle()
    val selected = tabs.getOrNull(selectedIndex)
    val noteTabId = (selected as? OpenTab.Note)?.noteId
    val guard = remember(noteTabId) { DraftGuard() }
    var pending by remember { mutableStateOf<PendingTabAction?>(null) }
    val importedKeys = remember { mutableSetOf<String>() }
    PruneMissingTabs(tabs, notes, tabsManager, selected?.key, onSelectNote, onBack)
    if (selected == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    TabContent(
        tab = selected,
        anchorNoteId = anchorNoteId,
        tabsManager = tabsManager,
        viewModel = viewModel,
        aiViewModel = aiViewModel,
        notes = notes,
        tabs = tabs,
        selectedIndex = selectedIndex,
        guard = guard,
        importedKeys = importedKeys,
        onSelectNote = onSelectNote,
        onBack = onBack,
        onNavigateToDrawing = onNavigateToDrawing,
        onNavigateToAiChat = onNavigateToAiChat,
        onNavigateToNote = onNavigateToNote,
        setPending = { pending = it }
    )
    DraftGuardDialog(
        visible = pending != null && guard.hasUnsaved,
        onSave = { saveAndResume(guard, tabsManager, pending, onSelectNote, onBack) { pending = null } },
        onDiscard = { discardAndResume(guard, tabsManager, pending, onSelectNote, onBack) { pending = null } },
        onCancel = { pending = null }
    )
}

@Composable
private fun TabContent(
    tab: OpenTab,
    anchorNoteId: Int,
    tabsManager: OpenNoteTabs,
    viewModel: NotesViewModel,
    aiViewModel: AiViewModel,
    notes: List<DecryptedNote>,
    tabs: List<OpenTab>,
    selectedIndex: Int,
    guard: DraftGuard,
    importedKeys: MutableSet<String>,
    onSelectNote: (Int) -> Unit,
    onBack: () -> Unit,
    onNavigateToDrawing: (Int, String?) -> Unit,
    onNavigateToAiChat: (Int) -> Unit,
    onNavigateToNote: (Int) -> Unit,
    setPending: (PendingTabAction) -> Unit
) {
    val tabBar: @Composable () -> Unit = {
        OpenNoteTabBar(
            tabs = tabs,
            selectedIndex = selectedIndex,
            titleFor = { tabTitle(it, notes) },
            onSelect = { i -> runTabAction(tabsManager, PendingTabAction.Select(tabs[i].key), onSelectNote, onBack) },
            onClose = { i -> gateOrRun(tabsManager, tabs, PendingTabAction.Close(tabs[i].key), guard, selectedIndex, setPending, onSelectNote, onBack) },
            onCloseOthers = { i -> gateOrRun(tabsManager, tabs, PendingTabAction.CloseOthers(tabs[i].key), guard, selectedIndex, setPending, onSelectNote, onBack) },
            onCloseAll = { gateOrRun(tabsManager, tabs, PendingTabAction.CloseAll, guard, selectedIndex, setPending, onSelectNote, onBack) }
        )
    }
    when (tab) {
        is OpenTab.Note -> key(tab.noteId) {
            NoteEditorScreen(
                noteId = tab.noteId,
                viewModel = viewModel,
                aiViewModel = aiViewModel,
                onBack = { gateOrRun(tabsManager, tabs, PendingTabAction.Back, guard, selectedIndex, setPending, onSelectNote, onBack) },
                onNavigateToDrawing = onNavigateToDrawing,
                onNavigateToMediaViewer = { type, src -> tabsManager.openMedia(type, src, anchorNoteId) },
                onNavigateToAiChat = onNavigateToAiChat,
                onNavigateToNote = onNavigateToNote,
                onOpenPdfTab = { uri, label -> tabsManager.openPdf(uri.toString(), label) },
                onOpenTextTab = { uri, label -> tabsManager.openText(uri.toString(), label) },
                tabBarContent = tabBar,
                draftGuard = guard
            )
        }
        is OpenTab.Media -> key(tab.key) {
            MediaViewerScreen(
                type = tab.type,
                src = tab.src,
                onBack = { hostOnBack(tabsManager, onBack) },
                tabBarContent = tabBar
            )
        }
        is OpenTab.Pdf -> key(tab.key) {
            PdfTabContent(tab, tabBar, importedKeys) { hostOnBack(tabsManager, onBack) }
        }
        is OpenTab.TextFile -> key(tab.key) {
            TextTabContent(tab, tabBar, importedKeys) { hostOnBack(tabsManager, onBack) }
        }
    }
}

@Composable
private fun PdfTabContent(
    tab: OpenTab.Pdf,
    tabBar: @Composable () -> Unit,
    importedKeys: MutableSet<String>,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val pdfViewModel: PdfViewerViewModel = viewModel(key = "tab-${tab.key}")
    val uri = remember(tab.uri) { runCatching { Uri.parse(tab.uri) }.getOrNull() }
    BackHandler { onClose() }
    LaunchedEffect(tab.uri) {
        if (uri == null || !importedKeys.add(tab.key)) return@LaunchedEffect
        runCatching { pdfViewModel.openUri(context, uri) }
            .onFailure { Log.e("NoteEditorTabsHost", "openPdf failed", it) }
    }
    if (uri == null) {
        TabLoadError(tab.label, onClose)
        return
    }
    PdfViewerScreen(viewModel = pdfViewModel, topTabs = tabBar, onClose = onClose)
}

@Composable
private fun TextTabContent(
    tab: OpenTab.TextFile,
    tabBar: @Composable () -> Unit,
    importedKeys: MutableSet<String>,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val codeViewModel: CodeEditorViewModel = viewModel(
        key = "tab-${tab.key}",
        factory = CodeTools.viewModelFactory(appContext)
    )
    val uri = remember(tab.uri) { runCatching { Uri.parse(tab.uri) }.getOrNull() }
    BackHandler { onClose() }
    LaunchedEffect(tab.uri) {
        if (uri == null || !importedKeys.add(tab.key)) return@LaunchedEffect
        runCatching { codeViewModel.handleIntent(context, Intent(Intent.ACTION_VIEW, uri)) }
            .onFailure { Log.e("NoteEditorTabsHost", "openText failed", it) }
    }
    if (uri == null) {
        TabLoadError(tab.label, onClose)
        return
    }
    CodeToolsApp(viewModel = codeViewModel, topTabs = tabBar)
}

@Composable
private fun TabLoadError(label: String, onClose: () -> Unit) {
    BackHandler { onClose() }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.tabs_file_load_error, label))
    }
}

private fun hostOnBack(tabsManager: OpenNoteTabs, onBack: () -> Unit) {
    val selected = tabsManager.selectedTab
    if (selected != null && selected !is OpenTab.Note) {
        if (tabsManager.closeTabByKey(selected.key) != null) return
    }
    onBack()
}

@Composable
private fun PruneMissingTabs(
    tabs: List<OpenTab>,
    notes: List<DecryptedNote>,
    tabsManager: OpenNoteTabs,
    selectedKey: String?,
    onSelectNote: (Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(tabs, notes) {
        if (notes.isEmpty() || tabs.isEmpty()) return@LaunchedEffect
        val ids = notes.map { it.note.id }.toSet()
        tabsManager.pruneMissing(ids) { ref -> tabFileExists(ref, context) }
        val current = tabsManager.selectedTab
        if (current == null) {
            onBack()
            return@LaunchedEffect
        }
        if (current.key != selectedKey && current is OpenTab.Note) onSelectNote(current.noteId)
    }
}

private fun tabFileExists(ref: String, context: android.content.Context): Boolean {
    if (ref.startsWith("content://")) return hasContentUri(ref, context)
    return runCatching { File(ref).exists() }.getOrDefault(false)
}

private fun hasContentUri(ref: String, context: android.content.Context): Boolean {
    return runCatching {
        context.contentResolver.openInputStream(Uri.parse(ref))?.close()
        true
    }.getOrDefault(false)
}

@Composable
private fun tabTitle(tab: OpenTab, notes: List<DecryptedNote>): String {
    when (tab) {
        is OpenTab.Note -> {
            if (tab.noteId == 0) return stringResource(R.string.tabs_new_note)
            val title = notes.find { it.note.id == tab.noteId }?.title?.trim()
            if (!title.isNullOrEmpty()) return title
            return stringResource(R.string.untitled_note)
        }
        is OpenTab.Media -> return tab.src.substringAfterLast('/').substringAfterLast('\\').ifBlank { tab.type }
        is OpenTab.Pdf -> return tab.label
        is OpenTab.TextFile -> return tab.label
    }
}

private fun gateOrRun(
    tabsManager: OpenNoteTabs,
    tabs: List<OpenTab>,
    action: PendingTabAction,
    guard: DraftGuard,
    selectedIndex: Int,
    setPending: (PendingTabAction) -> Unit,
    onSelectNote: (Int) -> Unit,
    onBack: () -> Unit
) {
    if (!shouldDefer(action, tabs, selectedIndex, guard)) {
        runTabAction(tabsManager, action, onSelectNote, onBack)
        return
    }
    setPending(action)
}

private fun shouldDefer(
    action: PendingTabAction,
    tabs: List<OpenTab>,
    selectedIndex: Int,
    guard: DraftGuard
): Boolean {
    if (!guard.hasUnsaved) return false
    return when (action) {
        is PendingTabAction.Select -> false
        is PendingTabAction.Close -> tabs.find { it.key == action.targetKey }?.isDraft == true
        is PendingTabAction.CloseOthers -> tabs.any { it.key != action.keepKey && it.isDraft }
        PendingTabAction.CloseAll -> tabs.any { it.isDraft }
        PendingTabAction.Back -> tabs.getOrNull(selectedIndex)?.isDraft == true
    }
}

private fun runTabAction(
    tabsManager: OpenNoteTabs,
    action: PendingTabAction,
    onSelectNote: (Int) -> Unit,
    onBack: () -> Unit
) {
    resumePending(action, tabsManager, null, false, onSelectNote, onBack)
}

private fun saveAndResume(
    guard: DraftGuard,
    tabsManager: OpenNoteTabs,
    pending: PendingTabAction?,
    onSelectNote: (Int) -> Unit,
    onBack: () -> Unit,
    clear: () -> Unit
) {
    guard.saveAction { newId ->
        tabsManager.promoteDraft(newId)
        clear()
        resumePending(pending, tabsManager, newId, false, onSelectNote, onBack)
    }
}

private fun discardAndResume(
    guard: DraftGuard,
    tabsManager: OpenNoteTabs,
    pending: PendingTabAction?,
    onSelectNote: (Int) -> Unit,
    onBack: () -> Unit,
    clear: () -> Unit
) {
    guard.discardAction()
    clear()
    resumePending(pending, tabsManager, null, true, onSelectNote, onBack)
}

private fun resumePending(
    pending: PendingTabAction?,
    tabsManager: OpenNoteTabs,
    savedNewId: Int?,
    discardedDraft: Boolean,
    onSelectNote: (Int) -> Unit,
    onBack: () -> Unit
) {
    val draftKey = OpenTab.Note(0).key
    fun resolve(key: String) = if (key == draftKey && savedNewId != null) "note:$savedNewId" else key
    when (pending) {
        is PendingTabAction.Select -> {
            tabsManager.selectTabByKey(resolve(pending.targetKey))
            syncScreen(tabsManager.selectedTab, onSelectNote, onBack)
        }
        is PendingTabAction.Close -> {
            val next = tabsManager.closeTabByKey(resolve(pending.targetKey))
            syncScreen(next, onSelectNote, onBack)
        }
        is PendingTabAction.CloseOthers -> {
            val index = tabsManager.tabs.value.indexOfFirst { it.key == resolve(pending.keepKey) }
            if (index >= 0) tabsManager.closeOthers(index)
            syncScreen(tabsManager.selectedTab, onSelectNote, onBack)
        }
        PendingTabAction.CloseAll -> {
            tabsManager.closeAll()
            onBack()
        }
        PendingTabAction.Back -> {
            if (discardedDraft) tabsManager.closeTabByKey(draftKey)
            onBack()
        }
        null -> onBack()
    }
}

private fun syncScreen(tab: OpenTab?, onSelectNote: (Int) -> Unit, onBack: () -> Unit) {
    if (tab == null) {
        onBack()
        return
    }
    if (tab is OpenTab.Note) onSelectNote(tab.noteId)
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
