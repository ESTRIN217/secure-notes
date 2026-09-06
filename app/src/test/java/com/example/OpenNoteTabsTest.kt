package com.example.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class OpenNoteTabsTest {

    private lateinit var tabs: OpenNoteTabs

    @Before
    fun setUp() {
        tabs = OpenNoteTabs()
    }

    @Test
    fun `openNote adds tab and selects it`() {
        tabs.openNote(1, now = 1000L)

        assertEquals(listOf(OpenTab(1, 1000L)), tabs.tabs.value)
        assertEquals(0, tabs.selectedIndex.value)
    }

    @Test
    fun `openNote dedupes existing note`() {
        tabs.openNote(1, now = 1000L)
        tabs.openNote(2, now = 2000L)
        tabs.openNote(1, now = 3000L)

        assertEquals(2, tabs.tabs.value.size)
        assertEquals(1, tabs.selectedTab?.noteId)
    }

    @Test
    fun `only one draft tab allowed`() {
        tabs.openNote(0, now = 1000L)
        tabs.openNote(0, now = 2000L)

        assertEquals(1, tabs.tabs.value.size)
        assertEquals(0, tabs.selectedTab?.noteId)
    }

    @Test
    fun `evicts least recently used beyond max`() {
        for (id in 1..OpenNoteTabs.MAX_TABS) {
            tabs.openNote(id, now = id.toLong())
        }
        tabs.openNote(99, now = 9999L)

        assertEquals(OpenNoteTabs.MAX_TABS, tabs.tabs.value.size)
        assertEquals(null, tabs.tabs.value.find { it.noteId == 1 })
        assertEquals(99, tabs.selectedTab?.noteId)
    }

    @Test
    fun `eviction never removes draft`() {
        tabs.openNote(0, now = 1L)
        for (id in 1..OpenNoteTabs.MAX_TABS) {
            tabs.openNote(id, now = 100L + id)
        }

        assertEquals(null, tabs.tabs.value.find { it.noteId == 1 })
        assertEquals(0, tabs.tabs.value.find { it.noteId == 0 }?.noteId)
    }

    @Test
    fun `closeTab selects left neighbor`() {
        tabs.openNote(1, now = 1L)
        tabs.openNote(2, now = 2L)
        tabs.openNote(3, now = 3L)

        val next = tabs.closeTab(2)

        assertEquals(2, next)
        assertEquals(2, tabs.tabs.value.size)
    }

    @Test
    fun `closeTab last returns null`() {
        tabs.openNote(1, now = 1L)

        val next = tabs.closeTab(0)

        assertNull(next)
        assertEquals(0, tabs.tabs.value.size)
    }

    @Test
    fun `closeTab out of bounds keeps state`() {
        tabs.openNote(1, now = 1L)

        tabs.closeTab(5)

        assertEquals(1, tabs.tabs.value.size)
    }

    @Test
    fun `closeOthers keeps selected`() {
        tabs.openNote(1, now = 1L)
        tabs.openNote(2, now = 2L)
        tabs.openNote(3, now = 3L)

        val next = tabs.closeOthers(1)

        assertEquals(2, next)
        assertEquals(1, tabs.tabs.value.size)
    }

    @Test
    fun `promoteDraft swaps draft id`() {
        tabs.openNote(0, now = 1L)
        tabs.openNote(5, now = 2L)

        tabs.promoteDraft(7, now = 3L)

        assertEquals(null, tabs.tabs.value.find { it.noteId == 0 })
        assertEquals(7, tabs.tabs.value.find { it.noteId == 7 }?.noteId)
    }

    @Test
    fun `pruneMissing drops deleted notes but keeps draft`() {
        tabs.openNote(0, now = 1L)
        tabs.openNote(1, now = 2L)
        tabs.openNote(2, now = 3L)

        tabs.pruneMissing(setOf(2))

        assertEquals(listOf(0, 2), tabs.tabs.value.map { it.noteId })
    }

    @Test
    fun `restore filters drafts and caps max`() {
        val ids = listOf(0, 1, 2, 3)

        tabs.restore(ids, selected = 2)

        assertEquals(listOf(1, 2, 3), tabs.tabs.value.map { it.noteId })
        assertEquals(2, tabs.selectedIndex.value)
    }
}
