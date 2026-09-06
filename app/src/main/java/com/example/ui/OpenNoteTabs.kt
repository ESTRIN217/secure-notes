package com.example.ui

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class OpenTab(
    val noteId: Int,
    val lastAccess: Long = 0L
)

class OpenNoteTabs {
    private val _tabs = MutableStateFlow<List<OpenTab>>(emptyList())
    val tabs: StateFlow<List<OpenTab>> = _tabs.asStateFlow()

    private val _selectedIndex = MutableStateFlow(0)
    val selectedIndex: StateFlow<Int> = _selectedIndex.asStateFlow()

    val selectedTab: OpenTab?
        get() = _tabs.value.getOrNull(_selectedIndex.value)

    fun openNote(noteId: Int, now: Long = System.currentTimeMillis()) {
        val existing = _tabs.value.indexOfFirst { it.noteId == noteId }
        if (existing >= 0) {
            selectTab(existing, now)
            return
        }
        if (noteId == 0 && hasDraft()) {
            selectTab(draftIndex(), now)
            return
        }
        appendTab(noteId, now)
    }

    fun selectTab(index: Int, now: Long = System.currentTimeMillis()) {
        if (index !in _tabs.value.indices) return
        _selectedIndex.value = index
        touch(index, now)
    }

    fun closeTab(index: Int): Int? {
        if (index !in _tabs.value.indices) return selectedTab?.noteId
        val remaining = _tabs.value.filterIndexed { i, _ -> i != index }
        _tabs.value = remaining
        if (remaining.isEmpty()) {
            _selectedIndex.value = 0
            return null
        }
        _selectedIndex.value = index.coerceAtMost(remaining.lastIndex)
        return remaining[_selectedIndex.value].noteId
    }

    fun closeOthers(index: Int): Int? {
        if (index !in _tabs.value.indices) return selectedTab?.noteId
        val kept = _tabs.value[index]
        _tabs.value = listOf(kept)
        _selectedIndex.value = 0
        return kept.noteId
    }

    fun closeAll() {
        _tabs.value = emptyList()
        _selectedIndex.value = 0
    }

    fun promoteDraft(newId: Int, now: Long = System.currentTimeMillis()) {
        val draft = draftIndex()
        if (draft < 0 || newId <= 0) return
        if (_tabs.value.any { it.noteId == newId }) {
            closeTab(draft)
            return
        }
        _tabs.value = _tabs.value.mapIndexed { i, tab ->
            if (i == draft) tab.copy(noteId = newId, lastAccess = now) else tab
        }
    }

    fun pruneMissing(existingIds: Set<Int>) {
        if (_tabs.value.isEmpty()) return
        val kept = _tabs.value.filter { it.noteId == 0 || it.noteId in existingIds }
        if (kept.size == _tabs.value.size) return
        _tabs.value = kept
        _selectedIndex.value = _selectedIndex.value.coerceAtMost(kept.lastIndex.coerceAtLeast(0))
    }

    fun restore(noteIds: List<Int>, selected: Int) {
        if (noteIds.isEmpty()) return
        val now = System.currentTimeMillis()
        _tabs.value = noteIds.filter { it > 0 }.distinct().take(MAX_TABS).map { OpenTab(it, now) }
        _selectedIndex.value = selected.coerceIn(0, _tabs.value.lastIndex)
    }

    private fun hasDraft(): Boolean = _tabs.value.any { it.noteId == 0 }

    private fun draftIndex(): Int = _tabs.value.indexOfFirst { it.noteId == 0 }

    private fun appendTab(noteId: Int, now: Long) {
        val grown = _tabs.value + OpenTab(noteId, now)
        _tabs.value = evictIfNeeded(grown, _selectedIndex.value)
        _selectedIndex.value = _tabs.value.indexOfFirst { it.noteId == noteId }
    }

    private fun evictIfNeeded(all: List<OpenTab>, protectedIndex: Int): List<OpenTab> {
        if (all.size <= MAX_TABS) return all
        val victim = all.withIndex()
            .filter { (i, tab) -> i != protectedIndex && tab.noteId != 0 }
            .minByOrNull { (_, tab) -> tab.lastAccess }
            ?: return all.takeLast(MAX_TABS)
        return all.filterIndexed { i, _ -> i != victim.index }
    }

    private fun touch(index: Int, now: Long) {
        _tabs.value = _tabs.value.mapIndexed { i, tab ->
            if (i == index) tab.copy(lastAccess = now) else tab
        }
    }

    companion object {
        const val MAX_TABS = 10
    }
}

val TabsSaver: Saver<OpenNoteTabs, Any> = listSaver(
    save = { manager -> manager.tabs.value.map { it.noteId } + manager.selectedIndex.value },
    restore = { saved ->
        OpenNoteTabs().apply {
            val selected = (saved.lastOrNull() as? Int) ?: 0
            val ids = saved.dropLast(1).mapNotNull { it as? Int }
            restore(ids, selected)
        }
    }
)
