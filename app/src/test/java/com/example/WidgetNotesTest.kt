package com.example.widget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.R
import com.example.data.model.Note
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WidgetNotesTest {

    // `widgetTitle` y `widgetSummary` leen recursos vía `RichTextConverter`: sin contexto de
    // aplicación no hay forma de ejercitarlas.
    private val context: Context = ApplicationProvider.getApplicationContext()

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
        // El valor esperado sale del recurso, no de un literal: un literal ataría la suite a un
        // idioma y rompería en cuanto el texto se traduzca (RF-18, principio 6).
        val untitled = context.getString(R.string.widget_note_untitled)

        assertEquals(untitled, widgetTitle(context, note(1, title = "   ")))
        assertEquals("Hola", widgetTitle(context, note(1, title = "  Hola  ")))
    }

    @Test
    fun `summary strips markup and caps length`() {
        val summary = widgetSummary("**Hola** mundo " + "x".repeat(500), context)

        assertTrue(summary.contains("Hola"))
        assertTrue(!summary.contains("**"))
        assertTrue(summary.length <= 140)
    }
}
