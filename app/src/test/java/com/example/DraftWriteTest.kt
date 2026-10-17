package com.example.ui

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Las dos reglas de la escritura de un borrador (spec 027, RF-180, RF-182 y RF-183).
 *
 * Son **puras** y no importan `android`, así que corren en esta plataforma, donde Robolectric no corre (RNF-113).
 * Y no son un detalle: `noteId == 0` parece una verdad sobre la nota y en realidad son **dos** hechos —«no hay
 * nota todavía» y «la nota es esta pero ya tiene fila»—. El defecto reportado salió de leerlo como uno solo.
 */
class DraftWriteTest {

    // ── RF-180: con qué id se escribe ────────────────────────────────────────────────────────

    @Test
    fun `un borrador sin fila se inserta`() {
        assertEquals(0, draftWriteId(noteId = 0, writeState = 0))
    }

    @Test
    fun `un borrador cuya escritura sigue en vuelo se inserta`() {
        // En vuelo no hay fila todavía, pero como el candado serializa, esta escritura va detrás de la otra.
        assertEquals(0, draftWriteId(noteId = 0, writeState = WRITE_IN_FLIGHT))
    }

    @Test
    fun `un borrador con fila escribe sobre esa fila`() {
        assertEquals(7, draftWriteId(noteId = 0, writeState = 7))
    }

    @Test
    fun `una nota que ya existia escribe sobre su id aunque la puerta no sepa nada`() {
        assertEquals(5, draftWriteId(noteId = 5, writeState = 0))
        assertEquals(5, draftWriteId(noteId = 5, writeState = 7))
    }

    // ── RF-182 y RF-183: si hay algo que descartar ───────────────────────────────────────────

    @Test
    fun `un borrador con contenido y sin escribir tiene algo que descartar`() {
        assertTrue(isDraftUnsaved(noteId = 0, writeState = 0, hasContent = true))
    }

    @Test
    fun `un borrador vacio no pregunta`() {
        assertFalse(isDraftUnsaved(noteId = 0, writeState = 0, hasContent = false))
    }

    @Test
    fun `lo que se esta escribiendo no se descarta`() {
        // RF-182: la escritura ya está lanzada, así que el aviso no tiene nada que avisar.
        assertFalse(isDraftUnsaved(noteId = 0, writeState = WRITE_IN_FLIGHT, hasContent = true))
    }

    @Test
    fun `un borrador ya escrito no tiene nada que descartar`() {
        assertFalse(isDraftUnsaved(noteId = 0, writeState = 7, hasContent = true))
    }

    @Test
    fun `una nota que ya existia no es un borrador`() {
        assertFalse(isDraftUnsaved(noteId = 5, writeState = 0, hasContent = true))
    }

    // ── RF-180 y RF-181: la puerta, que recuerda el id y escribe de una en una ─────────────────

    /**
     * **El defecto reportado, con nombre.**
     *
     * Adjuntar un fichero persiste, y un instante después el botón de atrás persiste otra vez. Las dos
     * escrituras leen «este borrador no tiene fila» e **insertan dos notas con el mismo contenido**.
     */
    @Test
    fun `dos escrituras seguidas de un borrador dejan una sola fila`() = runBlocking {
        val insertadas = mutableListOf<Int>()
        val actualizadas = mutableListOf<Int>()
        var siguiente = 40
        val puerta = DraftWriteGate(noteId = 0)

        suspend fun guardar(destino: Int): Int {
            delay(5)
            if (destino == 0) {
                insertadas += destino
                siguiente++
                return siguiente
            }
            actualizadas += destino
            return destino
        }

        val primera = puerta.write { guardar(it) }
        val segunda = puerta.write { guardar(it) }

        assertEquals(41, primera)
        assertEquals(41, segunda)
        assertEquals("una sola inserción", 1, insertadas.size)
        assertEquals("la segunda escribe sobre la fila de la primera", listOf(41), actualizadas)
    }

    @Test
    fun `dos escrituras lanzadas a la vez de un borrador dejan una sola fila`() = runBlocking {
        val destinos = mutableListOf<Int>()
        var siguiente = 40
        var inserciones = 0
        val puerta = DraftWriteGate(noteId = 0)

        suspend fun guardar(destino: Int): Int {
            delay(10)
            destinos += destino
            if (destino != 0) return destino
            inserciones++
            siguiente++
            return siguiente
        }

        awaitAll(
            async { puerta.write { guardar(it) } },
            async { puerta.write { guardar(it) } }
        )

        assertEquals(1, inserciones)
        assertEquals(listOf(0, 41), destinos)
    }

    @Test
    fun `una escritura que no devuelve id deja el borrador como estaba`() = runBlocking {
        val puerta = DraftWriteGate(noteId = 0)

        val fallida = puerta.write { 0 }
        val vuelveAPreguntar = isDraftUnsaved(noteId = 0, writeState = puerta.state, hasContent = true)
        val reintento = puerta.write { 12 }

        assertEquals(0, fallida)
        assertTrue("una escritura fallida no puede decir que ya está guardado", vuelveAPreguntar)
        assertEquals(12, reintento)
        assertEquals(12, puerta.state)
    }

    @Test
    fun `una nota que ya existia nunca inserta`() = runBlocking {
        val destinos = mutableListOf<Int>()
        val puerta = DraftWriteGate(noteId = 5)

        repeat(2) { puerta.write { destinos += it; it } }

        assertEquals(listOf(5, 5), destinos)
        assertFalse(isDraftUnsaved(noteId = 5, writeState = puerta.state, hasContent = true))
    }

    /**
     * RF-182 en el momento en que importa: el aviso se apaga **al lanzar** la escritura, no cuando esta
     * termina. El botón de atrás lee la bandera en la línea siguiente, y un `LaunchedEffect` llega un
     * fotograma más tarde.
     */
    @Test
    fun `lanzar una escritura apaga el aviso antes de que termine`() = runBlocking {
        val pendienteDeRespondar = CompletableDeferred<Int>()
        val puerta = DraftWriteGate(noteId = 0)

        val escritura = async { puerta.write { pendienteDeRespondar.await() } }
        puerta.launch()
        val apagado = isDraftUnsaved(noteId = 0, writeState = puerta.state, hasContent = true)

        pendienteDeRespondar.complete(9)

        assertFalse("con la escritura en marcha no hay nada que descartar", apagado)
        assertEquals(9, escritura.await())
        assertEquals(9, puerta.state)
    }
}