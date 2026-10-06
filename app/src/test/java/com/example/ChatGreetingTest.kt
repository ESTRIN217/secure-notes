package com.example

import com.example.ui.chat.greetingIndexFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas de L3 (qué saludo se elige).
 *
 * El número de día **no** lo calcula esta función: lo calcula la pantalla una sola vez, con `remember`
 * sin claves, y se pasa por parámetro, así que aquí los días se inventan. Tampoco hay recursos: lo que
 * se decide es el índice, y el texto lo resuelve el Composable con `stringResource`.
 *
 * Lo que la spec **no** promete es que dos días cualesquiera den saludos distintos: con dos entradas y
 * el recorrido completo eso es imposible. Ningún caso de aquí lo exige.
 */
class ChatGreetingTest {

    // El mismo día produce siempre el mismo saludo (caso límite 12).

    @Test
    fun `the same day always yields the same index`() {
        val first = greetingIndexFor(20_000L, 3)
        val second = greetingIndexFor(20_000L, 3)

        assertEquals(first, second)
    }

    // Días consecutivos recorren el conjunto en orden.

    @Test
    fun `consecutive days walk the set in order`() {
        assertEquals(0, greetingIndexFor(0L, 3))
        assertEquals(1, greetingIndexFor(1L, 3))
        assertEquals(2, greetingIndexFor(2L, 3))
    }

    @Test
    fun `one day past the end of the set starts it again`() {
        assertEquals(greetingIndexFor(0L, 3), greetingIndexFor(3L, 3))
    }

    @Test
    fun `traversing the whole set returns to the first index`() {
        for (day in 0L until 10L) {
            assertEquals(
                "day $day with count 3",
                greetingIndexFor(day, 3),
                greetingIndexFor(day + 3, 3)
            )
        }
    }

    @Test
    fun `every day of a long run advances by exactly one inside the set`() {
        for (day in 0L until 30L) {
            val today = greetingIndexFor(day, 3)
            val tomorrow = greetingIndexFor(day + 1, 3)

            assertEquals("day $day with count 3", (today + 1) % 3, tomorrow)
        }
    }

    // Un idioma con menos entradas que otro: el índice siempre cae dentro del conjunto recibido
    // (caso límite 11). El conjunto mínimo de la spec son dos entradas.

    @Test
    fun `the minimum set of two entries alternates without leaving the range`() {
        assertEquals(0, greetingIndexFor(0L, 2))
        assertEquals(1, greetingIndexFor(1L, 2))
        assertEquals(0, greetingIndexFor(2L, 2))
    }

    @Test
    fun `a set smaller than another language's never returns an index out of range`() {
        for (day in -50L until 50L) {
            val index = greetingIndexFor(day, 2)

            assertTrue("day $day with count 2 gave $index", index in 0..1)
        }
    }

    @Test
    fun `the index never reaches count for any small set`() {
        for (count in 1..5) {
            for (day in -20L until 20L) {
                val index = greetingIndexFor(day, count)

                assertTrue("day $day with count $count gave $index", index in 0 until count)
            }
        }
    }

    // Días anteriores a época: el índice nunca puede ser negativo.

    @Test
    fun `a day before the epoch maps inside the set`() {
        assertEquals(2, greetingIndexFor(-1L, 3))
        assertEquals(0, greetingIndexFor(-3L, 3))
        assertEquals(2, greetingIndexFor(-4L, 3))
    }

    @Test
    fun `real epoch days land inside the set`() {
        for (day in 20_000L until 20_010L) {
            val index = greetingIndexFor(day, 3)

            assertTrue("day $day with count 3 gave $index", index in 0..2)
        }
    }

    // Un idioma sin ninguna entrada no puede romper la pantalla.

    @Test
    fun `a zero sized set returns zero without failing`() {
        assertEquals(0, greetingIndexFor(20_000L, 0))
        assertEquals(0, greetingIndexFor(-1L, 0))
    }

    @Test
    fun `a negative set size returns zero without failing`() {
        assertEquals(0, greetingIndexFor(20_000L, -3))
    }
}
