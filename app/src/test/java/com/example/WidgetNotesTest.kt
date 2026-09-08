package com.example.widget

import com.example.data.model.Note
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetNotesTest {

    private fun note(
        id: Int,
        title: String = "T$id",
        encrypted: Boolean = false,
        deleted: Boolean = false,
        archived: Boolean = false,
        modified: Long = id.toLong()
    ) = Note(
        id = id,
        title = title,
        content = "c$id",
        isEncrypted = encrypted,
        isDeleted = deleted,
        isArchived = archived,
        lastModified = modified
    )

    @Test
    fun `visible notes excludes encrypted deleted archived`() {
        val notes = listOf(
            note(1),
            note(2, encrypted = true),
            note(3, deleted = true),
            note(4, archived = true),
            note(5)
        )

        assertEquals(listOf(5, 1), widgetVisibleNotes(notes).map { it.id })
    }

    @Test
    fun `visible notes sorted by lastModified desc`() {
        val notes = listOf(note(1, modified = 10L), note(2, modified = 30L), note(3, modified = 20L))

        assertEquals(listOf(2, 3, 1), widgetVisibleNotes(notes).map { it.id })
    }

    @Test
    fun `title falls back when blank`() {
        assertEquals("Untitled", widgetTitle(note(1, title = "   ")))
        assertEquals("Hola", widgetTitle(note(1, title = "  Hola  ")))
    }

    @Test
    fun `summary strips markup and caps length`() {
        val summary = widgetSummary("**Hola** mundo " + "x".repeat(500))

        assertTrue(summary.contains("Hola"))
        assertTrue(!summary.contains("**"))
        assertTrue(summary.length <= 140)
    }
}
