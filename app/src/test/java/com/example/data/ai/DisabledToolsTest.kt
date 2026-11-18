package com.example.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La decisión del toggle de herramientas (spec 042, RF-305, RNF-191): [toggledDisabledTools] es la
 * regla — desactivar añade, activar quita — y el ViewModel solo la consulta, no la reescribe
 * (igual que `draftWriteId`/`isNoteEmpty` en las specs 027/032).
 *
 * JUnit4 puro, sin Robolectric: la pieza vive en un fichero sin `android` a propósito.
 */
class DisabledToolsTest {

    @Test
    fun `desactivar añade el nombre al conjunto`() {
        val resultado = toggledDisabledTools(emptySet(), "search_notes", enabled = false)
        assertEquals(setOf("search_notes"), resultado)
    }

    @Test
    fun `activar quita el nombre del conjunto`() {
        val resultado = toggledDisabledTools(setOf("search_notes", "get_note"), "search_notes", enabled = true)
        assertEquals(setOf("get_note"), resultado)
    }

    @Test
    fun `desactivar dos veces es idempotente`() {
        val una = toggledDisabledTools(emptySet(), "create_note", enabled = false)
        val dos = toggledDisabledTools(una, "create_note", enabled = false)
        assertEquals(una, dos)
    }

    @Test
    fun `activar un nombre que no estaba no cambia nada`() {
        val resultado = toggledDisabledTools(setOf("get_note"), "search_notes", enabled = true)
        assertEquals(setOf("get_note"), resultado)
    }

    @Test
    fun `un nombre desconocido da un conjunto válido`() {
        val resultado = toggledDisabledTools(setOf("search_notes"), "tool_borrada", enabled = false)
        assertTrue(resultado.contains("tool_borrada"))
        assertFalse(resultado.contains("tool_borrada_hasta_la_fila"))
    }
}
