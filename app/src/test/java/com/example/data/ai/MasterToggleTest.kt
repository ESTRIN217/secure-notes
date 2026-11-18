package com.example.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Las decisiones del checkbox maestro (spec 043, RF-308, RF-309, RNF-198): viven en piezas puras
 * del fichero del registro, sin `android`, y el ViewModel/ToolsSheet solo las consultan.
 *
 * JUnit4 puro, sin Robolectric: aquí no hace falta reloj, zona ni `Context`.
 */
class MasterToggleTest {

    private val cinco = listOf(
        "search_notes", "get_note", "create_note", "get_datetime", "get_info"
    )

    @Test
    fun `desactivar todas anade las registradas al conjunto`() {
        assertEquals(
            cinco.toSet(),
            allToolsToggle(emptySet(), cinco, enableAll = false)
        )
    }

    @Test
    fun `desactivar todas conserva las ya deshabilitadas`() {
        assertEquals(
            cinco.toSet() + "tool_borrada",
            allToolsToggle(setOf("get_note", "tool_borrada"), cinco, enableAll = false)
        )
    }

    @Test
    fun `activar todas vacia el conjunto y limpia nombres muertos`() {
        val resultado = allToolsToggle(
            setOf("get_note", "tool_borrada"), cinco, enableAll = true
        )
        assertTrue(resultado.isEmpty())
    }

    @Test
    fun `el maestro es idempotente`() {
        val desactivadas = allToolsToggle(emptySet(), cinco, enableAll = false)
        assertEquals(desactivadas, allToolsToggle(desactivadas, cinco, enableAll = false))
        val activadas = allToolsToggle(desactivadas, cinco, enableAll = true)
        assertEquals(activadas, allToolsToggle(activadas, cinco, enableAll = true))
    }

    @Test
    fun `contador de cinco sobre cinco`() {
        assertEquals(5, enabledToolCount(cinco, emptySet()))
    }

    @Test
    fun `contador de cuatro sobre cinco`() {
        assertEquals(4, enabledToolCount(cinco, setOf("get_datetime")))
    }

    @Test
    fun `contador de cero sobre cinco`() {
        assertEquals(0, enabledToolCount(cinco, cinco.toSet()))
    }

    @Test
    fun `contador de cero sobre cero con registro vacio`() {
        assertEquals(0, enabledToolCount(emptyList(), emptySet()))
    }

    @Test
    fun `marcado solo cuando todas estan activas`() {
        assertTrue(allToolsEnabled(cinco, emptySet()))
        assertFalse(allToolsEnabled(cinco, setOf("get_info")))
    }

    @Test
    fun `con lista vacia el maestro esta marcado pero la UI lo deshabilita`() {
        assertTrue(allToolsEnabled(emptyList(), emptySet()))
    }
}
