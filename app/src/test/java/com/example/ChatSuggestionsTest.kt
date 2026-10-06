package com.example

import com.example.data.ai.AiAction
import com.example.ui.chat.ChatSuggestion
import com.example.ui.chat.ChatSuggestionAction
import com.example.ui.chat.requestFor
import com.example.ui.chat.visibleSuggestions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas de L1 (qué sugerencias se ven) y L2 (qué petición lanza cada una).
 *
 * Son pruebas puras a propósito: sin entorno de pruebas ni superficie de dibujo, para que se ejecuten
 * en la plataforma de desarrollo, donde la suite que sí lo necesita está bloqueada. Las etiquetas
 * llegan ya resueltas desde el Composable: aquí se inventan, porque aquí no hay recursos.
 */
class ChatSuggestionsTest {

    private val poem = ChatSuggestion("poem", "Write a poem", ChatSuggestionAction.GENERATE)
    private val summarize = ChatSuggestion("summarize", "Summarize", ChatSuggestionAction.SUMMARIZE)

    // L1 / RF-38: las tres condiciones de "hay algo que resumir" son disjuntas y las tres cuentan.

    @Test
    fun `without anything to summarize only the three generation suggestions show`() {
        val visible = visibleSuggestions(false, false, false)

        assertEquals(3, visible.size)
        assertFalse(visible.any { it.action == ChatSuggestionAction.SUMMARIZE })
    }

    @Test
    fun `with selected text the summarize suggestion shows`() {
        assertTrue(visibleSuggestions(true, false, false).hasSummarize())
    }

    @Test
    fun `with note content the summarize suggestion shows`() {
        assertTrue(visibleSuggestions(false, true, false).hasSummarize())
    }

    @Test
    fun `with pending attachments the summarize suggestion shows`() {
        assertTrue(visibleSuggestions(false, false, true).hasSummarize())
    }

    @Test
    fun `each of the three conditions adds the fourth suggestion`() {
        val onlyGeneration = visibleSuggestions(false, false, false).size

        assertEquals(onlyGeneration + 1, visibleSuggestions(true, false, false).size)
        assertEquals(onlyGeneration + 1, visibleSuggestions(false, true, false).size)
        assertEquals(onlyGeneration + 1, visibleSuggestions(false, false, true).size)
    }

    @Test
    fun `the ids are the stable english keys and never repeat`() {
        val ids = visibleSuggestions(true, true, true).map { it.id }

        assertEquals(listOf("poem", "ideas", "explain", "summarize"), ids)
        assertEquals(ids.size, ids.toSet().size)
    }

    // L2 / RF-37: generación con la etiqueta como petición, resumen con la suya.

    @Test
    fun `a generation suggestion sends its own label as the prompt`() {
        val request = requestFor(poem, "", "", "Summarize the selection", "en")

        assertEquals(AiAction.GENERATE, request.action)
        assertEquals("Write a poem", request.prompt)
    }

    @Test
    fun `a generation suggestion keeps the selection the note and the target language`() {
        val request = requestFor(poem, "selected", "note body", "Summarize", "pt")

        assertEquals("selected", request.selectedText)
        assertEquals("note body", request.context)
        assertEquals("pt", request.targetLanguage)
    }

    @Test
    fun `the summarize suggestion launches the summarize action with its own prompt`() {
        val request = requestFor(summarize, "selected", "note body", "Summarize the selection", "es")

        assertEquals(AiAction.SUMMARIZE, request.action)
        assertEquals("Summarize the selection", request.prompt)
    }

    @Test
    fun `the summarize suggestion carries the selection the note and the target language`() {
        val request = requestFor(summarize, "selected", "note body", "Summarize", "es")

        assertEquals("selected", request.selectedText)
        assertEquals("note body", request.context)
        assertEquals("es", request.targetLanguage)
    }

    private fun List<ChatSuggestion>.hasSummarize(): Boolean =
        any { it.action == ChatSuggestionAction.SUMMARIZE }
}
