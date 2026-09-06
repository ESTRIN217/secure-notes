package com.example.ui

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface OpenTab {
    val key: String
    val lastAccess: Long

    data class Note(
        val noteId: Int,
        override val lastAccess: Long = 0L
    ) : OpenTab {
        override val key: String get() = "note:$noteId"
    }

    data class Media(
        val type: String,
        val src: String,
        val sourceNoteId: Int? = null,
        override val lastAccess: Long = 0L
    ) : OpenTab {
        override val key: String get() = "media:$type:$src"
    }

    data class Pdf(
        val uri: String,
        val label: String,
        override val lastAccess: Long = 0L
    ) : OpenTab {
        override val key: String get() = "pdf:$uri"
    }

    data class TextFile(
        val uri: String,
        val label: String,
        override val lastAccess: Long = 0L
    ) : OpenTab {
        override val key: String get() = "text:$uri"
    }
}

fun OpenTab.withAccess(now: Long): OpenTab = when (this) {
    is OpenTab.Note -> copy(lastAccess = now)
    is OpenTab.Media -> copy(lastAccess = now)
    is OpenTab.Pdf -> copy(lastAccess = now)
    is OpenTab.TextFile -> copy(lastAccess = now)
}

class OpenNoteTabs {
    private val _tabs = MutableStateFlow<List<OpenTab>>(emptyList())
    val tabs: StateFlow<List<OpenTab>> = _tabs.asStateFlow()

    private val _selectedIndex = MutableStateFlow(0)
    val selectedIndex: StateFlow<Int> = _selectedIndex.asStateFlow()

    val selectedTab: OpenTab?
        get() = _tabs.value.getOrNull(_selectedIndex.value)

    fun openNote(noteId: Int, now: Long = System.currentTimeMillis()) {
        if (noteId == 0 && hasDraft()) {
            selectTab(draftIndex(), now)
            return
        }
        openTab(OpenTab.Note(noteId), now)
    }

    fun openMedia(type: String, src: String, sourceNoteId: Int? = null, now: Long = System.currentTimeMillis()) {
        openTab(OpenTab.Media(type, src, sourceNoteId), now)
    }

    fun openPdf(uri: String, label: String, now: Long = System.currentTimeMillis()) {
        openTab(OpenTab.Pdf(uri, label), now)
    }

    fun openText(uri: String, label: String, now: Long = System.currentTimeMillis()) {
        openTab(OpenTab.TextFile(uri, label), now)
    }

    fun openTab(tab: OpenTab, now: Long = System.currentTimeMillis()) {
        val existing = _tabs.value.indexOfFirst { it.key == tab.key }
        if (existing >= 0) {
            selectTab(existing, now)
            return
        }
        appendTab(tab.withAccess(now))
    }

    fun selectTab(index: Int, now: Long = System.currentTimeMillis()) {
        if (index !in _tabs.value.indices) return
        _selectedIndex.value = index
        touch(index, now)
    }

    fun selectTabByKey(key: String, now: Long = System.currentTimeMillis()) {
        val index = _tabs.value.indexOfFirst { it.key == key }
        if (index >= 0) selectTab(index, now)
    }

    fun closeTab(index: Int): OpenTab? {
        if (index !in _tabs.value.indices) return selectedTab
        val remaining = _tabs.value.filterIndexed { i, _ -> i != index }
        _tabs.value = remaining
        if (remaining.isEmpty()) {
            _selectedIndex.value = 0
            return null
        }
        _selectedIndex.value = index.coerceAtMost(remaining.lastIndex)
        return remaining[_selectedIndex.value]
    }

    fun closeTabByKey(key: String): OpenTab? {
        val index = _tabs.value.indexOfFirst { it.key == key }
        if (index < 0) return selectedTab
        return closeTab(index)
    }

    fun closeOthers(index: Int): OpenTab? {
        if (index !in _tabs.value.indices) return selectedTab
        val kept = _tabs.value[index]
        _tabs.value = listOf(kept)
        _selectedIndex.value = 0
        return kept
    }

    fun closeAll() {
        _tabs.value = emptyList()
        _selectedIndex.value = 0
    }

    fun promoteDraft(newId: Int, now: Long = System.currentTimeMillis()) {
        val draft = draftIndex()
        if (draft < 0 || newId <= 0) return
        if (_tabs.value.any { it.key == "note:$newId" }) {
            closeTab(draft)
            return
        }
        _tabs.value = _tabs.value.mapIndexed { i, tab ->
            if (i == draft) OpenTab.Note(newId, now) else tab
        }
    }

    fun pruneMissing(existingIds: Set<Int>, fileExists: (String) -> Boolean) {
        if (_tabs.value.isEmpty()) return
        val kept = _tabs.value.filter { tab -> isTabAlive(tab, existingIds, fileExists) }
        if (kept.size == _tabs.value.size) return
        _tabs.value = kept
        _selectedIndex.value = _selectedIndex.value.coerceAtMost(kept.lastIndex.coerceAtLeast(0))
    }

    fun restore(tabs: List<OpenTab>, selected: Int) {
        val now = System.currentTimeMillis()
        _tabs.value = tabs.filter { it !is OpenTab.Note || it.noteId > 0 }
            .distinctBy { it.key }
            .take(MAX_TABS)
            .map { it.withAccess(now) }
        _selectedIndex.value = selected.coerceIn(0, _tabs.value.lastIndex.coerceAtLeast(0))
    }

    private fun isTabAlive(tab: OpenTab, existingIds: Set<Int>, fileExists: (String) -> Boolean): Boolean {
        return when (tab) {
            is OpenTab.Note -> tab.noteId == 0 || tab.noteId in existingIds
            is OpenTab.Media -> fileExists(tab.src)
            is OpenTab.Pdf -> fileExists(tab.uri)
            is OpenTab.TextFile -> fileExists(tab.uri)
        }
    }

    private fun hasDraft(): Boolean = _tabs.value.any { it is OpenTab.Note && it.noteId == 0 }

    private fun draftIndex(): Int = _tabs.value.indexOfFirst { it is OpenTab.Note && it.noteId == 0 }

    private fun appendTab(tab: OpenTab) {
        val grown = _tabs.value + tab
        _tabs.value = evictIfNeeded(grown, _selectedIndex.value)
        _selectedIndex.value = _tabs.value.indexOfFirst { it.key == tab.key }
    }

    private fun evictIfNeeded(all: List<OpenTab>, protectedIndex: Int): List<OpenTab> {
        if (all.size <= MAX_TABS) return all
        val victim = all.withIndex()
            .filter { (i, tab) -> i != protectedIndex && !tab.isDraft }
            .minByOrNull { (_, tab) -> tab.lastAccess }
            ?: return all.takeLast(MAX_TABS)
        return all.filterIndexed { i, _ -> i != victim.index }
    }

    private fun touch(index: Int, now: Long) {
        _tabs.value = _tabs.value.mapIndexed { i, tab ->
            if (i == index) tab.withAccess(now) else tab
        }
    }

    companion object {
        const val MAX_TABS = 10
    }
}

val OpenTab.isDraft: Boolean
    get() = this is OpenTab.Note && noteId == 0

val TabsSaver: Saver<OpenNoteTabs, Any> = listSaver(
    save = { manager ->
        val out = mutableListOf<Any?>()
        manager.tabs.value.forEach { tab -> out.addAll(tab.encode()) }
        out.add(manager.selectedIndex.value)
        out
    },
    restore = { saved ->
        OpenNoteTabs().apply {
            val selected = (saved.lastOrNull() as? Int) ?: 0
            restore(decodeTabs(saved.dropLast(1)), selected)
        }
    }
)

private fun OpenTab.encode(): List<Any?> = when (this) {
    is OpenTab.Note -> listOf("n", noteId)
    is OpenTab.Media -> listOf("m", type, src, sourceNoteId ?: -1)
    is OpenTab.Pdf -> listOf("p", uri, label)
    is OpenTab.TextFile -> listOf("t", uri, label)
}

private fun decodeTabs(saved: List<Any?>): List<OpenTab> {
    val tabs = mutableListOf<OpenTab>()
    var i = 0
    while (i < saved.size) {
        when (saved.getOrNull(i) as? String) {
            "n" -> {
                (saved.getOrNull(i + 1) as? Int)?.let { tabs.add(OpenTab.Note(it)) }
                i += 2
            }
            "m" -> {
                val type = saved.getOrNull(i + 1) as? String
                val src = saved.getOrNull(i + 2) as? String
                val source = (saved.getOrNull(i + 3) as? Int)?.takeIf { it >= 0 }
                if (type != null && src != null) tabs.add(OpenTab.Media(type, src, source))
                i += 4
            }
            "p" -> {
                val uri = saved.getOrNull(i + 1) as? String
                val label = saved.getOrNull(i + 2) as? String
                if (uri != null && label != null) tabs.add(OpenTab.Pdf(uri, label))
                i += 3
            }
            "t" -> {
                val uri = saved.getOrNull(i + 1) as? String
                val label = saved.getOrNull(i + 2) as? String
                if (uri != null && label != null) tabs.add(OpenTab.TextFile(uri, label))
                i += 3
            }
            else -> i += 1
        }
    }
    return tabs
}
