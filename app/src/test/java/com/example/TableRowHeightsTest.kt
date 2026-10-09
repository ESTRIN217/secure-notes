package com.example

import com.example.ui.table.moved
import com.example.ui.table.resized
import com.example.ui.table.without
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pruebas del mantenimiento de la lista de altos por fila.
 *
 * Son pruebas puras a propósito, como las de la geometría: sin entorno de pruebas ni pantalla, que
 * es lo que permite ejercitarlas en la plataforma de desarrollo.
 *
 * La lista tiene una entrada por fila. Antes había un único alto para todas, y el contorno
 * sumaba alturas que no eran las reales en cuanto una fila crecía (spec 009, RF-56).
 */
class TableRowHeightsTest {

    private val altoInicial = 44f

    private val tres = resized(emptyList(), 3, altoInicial)
    private val dos = resized(emptyList(), 2, altoInicial)
    private val uno = resized(emptyList(), 1, altoInicial)

    // RF-56: añadir una fila no mueve los altos de las que ya estaban.

    @Test
    fun `a new list is as tall as the initial height`() {
        assertEquals(listOf(44f, 44f, 44f), tres)
    }

    @Test
    fun `an empty list stays empty`() {
        assertEquals(emptyList<Float>(), resized(emptyList(), 0, altoInicial))
    }

    @Test
    fun `growing keeps the heights already measured`() {
        val conAlta = listOf(120f, 44f, 44f)
        assertEquals(listOf(120f, 44f, 44f, 44f), resized(conAlta, 4, altoInicial))
    }

    @Test
    fun `shrinking drops the extra heights`() {
        assertEquals(listOf(120f, 44f), resized(listOf(120f, 44f, 44f, 90f), 2, altoInicial))
    }

    @Test
    fun `a shorter previous list fills the new rows with the initial height`() {
        assertEquals(listOf(120f, 44f, 44f), resized(listOf(120f), 3, altoInicial))
    }

    // RF-56: quitar una fila quita su entrada y deja las demás con su valor.

    @Test
    fun `removing the last row keeps the others`() {
        assertEquals(listOf(120f, 44f), without(listOf(120f, 44f, 44f), 2))
    }

    @Test
    fun `removing from the middle shifts the following heights`() {
        assertEquals(listOf(120f, 90f), without(listOf(120f, 44f, 90f), 1))
    }

    @Test
    fun `removing the only row empties the list`() {
        assertEquals(emptyList<Float>(), without(listOf(120f), 0))
    }

    @Test
    fun `removing a row that is not there changes nothing`() {
        val heights = listOf(120f, 44f)
        assertEquals(heights, without(heights, 5))
        assertEquals(heights, without(heights, -1))
    }

    // RF-56: el alto viaja con la fila.

    @Test
    fun `moving a row takes its height along`() {
        assertEquals(listOf(44f, 120f, 44f), moved(listOf(120f, 44f, 44f), 0, 1))
    }

    @Test
    fun `moving a row to the end takes its height along`() {
        assertEquals(listOf(44f, 44f, 120f), moved(listOf(120f, 44f, 44f), 0, 2))
    }

    @Test
    fun `moving backwards takes the height along`() {
        assertEquals(listOf(90f, 120f, 44f), moved(listOf(120f, 44f, 90f), 2, 0))
    }

    @Test
    fun `moving a row to itself changes nothing`() {
        assertEquals(listOf(120f, 44f), moved(listOf(120f, 44f), 1, 1))
    }

    @Test
    fun `moving a row that is not there changes nothing`() {
        val heights = listOf(120f, 44f)
        assertEquals(heights, moved(heights, 5, 0))
        assertEquals(heights, moved(heights, 0, 9))
    }

    @Test
    fun `moving the only row cannot go anywhere`() {
        assertEquals(uno, moved(uno, 0, 1))
    }

    // Los tres operaciones encadenadas dejan la lista con una entrada por fila siempre.

    @Test
    fun `the list keeps one height per row through a full round trip`() {
        val conAlta = resized(emptyList(), 3, altoInicial).let { it.toMutableList() }
        conAlta[0] = 120f
        conAlta[2] = 90f
        // Quitar la fila del medio deja [120, 90], y mover la primera a la segunda deja [90, 120]:
        // los dos altos viajan con su fila y no con su posición.
        val quitada = without(conAlta, 1)
        val movida = moved(quitada, 0, 1)
        val vueltaAResized = resized(movida, 3, altoInicial)
        assertEquals(3, vueltaAResized.size)
        assertEquals(listOf(90f, 120f, altoInicial), vueltaAResized)
    }

    @Test
    fun `two rows give two heights`() {
        assertEquals(2, dos.size)
    }
}