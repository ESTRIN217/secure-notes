package com.example

import com.example.ui.NoteSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas **puras** de la decisión de selección múltiple (spec 026).
 *
 * No necesitan `Context` ni Robolectric, que no se ejecuta en esta plataforma: la regla vive aparte de la
 * pantalla justamente para poder probarla aquí (RNF-107).
 */
class NoteSelectionTest {

    @Test
    fun `sin notas seleccionadas no hay modo seleccion`() {
        assertFalse(NoteSelection.isActive(emptySet()))
    }

    @Test
    fun `con una nota seleccionada hay modo seleccion`() {
        assertTrue(NoteSelection.isActive(setOf(1)))
    }

    @Test
    fun `alternar sobre la seleccion vacia entra en el modo`() {
        assertEquals(setOf(7), NoteSelection.toggle(emptySet(), 7))
    }

    @Test
    fun `alternar una segunda nota la suma a la seleccion`() {
        assertEquals(setOf(1, 2), NoteSelection.toggle(setOf(1), 2))
    }

    @Test
    fun `alternar una nota ya seleccionada la quita y conserva las demas`() {
        assertEquals(setOf(2), NoteSelection.toggle(setOf(1, 2), 1))
    }

    @Test
    fun `alternar la unica nota seleccionada termina el modo`() {
        val despues = NoteSelection.toggle(setOf(1), 1)

        assertEquals(emptySet<Int>(), despues)
        assertFalse(NoteSelection.isActive(despues))
    }

    @Test
    fun `alternar dos veces el mismo id devuelve el conjunto original`() {
        val original = setOf(1, 2, 3)

        val ida = NoteSelection.toggle(original, 2)
        val vuelta = NoteSelection.toggle(ida, 2)

        assertEquals(original, vuelta)
    }

    @Test
    fun `alternar no modifica la seleccion de entrada`() {
        val original = setOf(1, 2)

        NoteSelection.toggle(original, 3)
        NoteSelection.toggle(original, 1)

        assertEquals(setOf(1, 2), original)
    }

    @Test
    fun `alternar un id ajeno no toca los demas`() {
        assertEquals(setOf(1, 2, 3), NoteSelection.toggle(setOf(1, 2), 3))
    }
}
