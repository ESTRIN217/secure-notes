package com.example

import com.example.util.MarkdownHandoff
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * El contrato de consumo del sobre de traspaso (spec 013, arreglo del camino largo).
 *
 * El bug que motivó este fichero: «enviar a notas» desde `CodeEditorActivity` **no hacía nada**. El
 * sobre llegaba por `onNewIntent`, se guardaba en una `var` estática, y el `LaunchedEffect` que debía
 * consumirlo tenía por clave `isUnlocked` —que ya no cambiaba al llegar el Intent—, así que nunca
 * volvía a correr y el Markdown se quedaba esperando en `cacheDir` para siempre.
 *
 * ## Lo que este fichero SÍ fija, y lo que NO
 *
 * **Sí**: que [MarkdownHandoff.consume] lee y borra **en el mismo paso**, incluso con el contenido
 * corrupto, y que el contenido llega intacto. Eso es lo que hace que el camino corto y el largo
 * produzcan la misma nota, y es lo que permite consumir en el acto sin miedo.
 *
 * **No**: cuándo se llama a `consume`. Eso vive en `MainActivity`, necesita el ViewModel y la
 * `Activity`, y aquí no se puede ejercitar. Lo que se fija de eso es una regla de código, revisada a
 * mano en `MainActivity.deliverNoteHandoff`: si la app está abierta se consume en el acto, y solo se
 * aplaza al desbloqueo si está bloqueada.
 *
 * Es una limitación real de esta plataforma, no un olvido: no hay forma de observar el reloj de un
 * `LaunchedEffect` desde un test puro. Lo que **sí** se puede es comprobar que la operación sobre la
 * que se apoya ese reloj es segura, y eso es lo de aquí.
 */
class NoteHandoffTimingTest {

    private fun tempDir(): File =
        File(System.getProperty("java.io.tmpdir"), "handoff-timing-${System.nanoTime()}").apply { mkdirs() }

    @Test
    fun `consume lee y borra en el mismo paso`() {
        // Esta es la garantía de la que depende poder consumir «en el acto»: si leer y borrar fueran
        // dos pasos, habría una ventana en la que el mismo sobre se podría consumir dos veces.
        val dir = tempDir()
        val ruta = MarkdownHandoff.write(dir, "nota.md", "# Contenido\n")
        assertTrue(File(ruta).exists())

        val par = MarkdownHandoff.consume(ruta)

        assertNotNull(par)
        assertEquals("nota.md", par?.first)
        assertEquals("# Contenido\n", par?.second)
        assertFalse("consume tiene que haber borrado el fichero", File(ruta).exists())
        dir.deleteRecursively()
    }

    @Test
    fun `consumir el mismo sobre dos veces no devuelve contenido la segunda`() {
        // Refuerza que consume es «leer y borrar» de verdad, y no solo «leer».
        val dir = tempDir()
        val ruta = MarkdownHandoff.write(dir, "una.md", "contenido")

        assertNotNull(MarkdownHandoff.consume(ruta))
        assertNull("la segunda vez no deberia devolver nada", MarkdownHandoff.consume(ruta))
        dir.deleteRecursively()
    }

    @Test
    fun `el sobre se borra aunque este corrupto`() {
        // Esto es lo que hace seguro consumir en el acto: si el contenido está corrupto, el
        // borrado ocurre igual. Un sobre a medio consumir en `cacheDir` sería un Markdown sin
        // cifrar que se queda ahí (RNF-35).
        val dir = tempDir()
        val ruta = File(dir, MarkdownHandoff.FILE_NAME).apply { writeText("no es json") }

        assertNull(MarkdownHandoff.consume(ruta.absolutePath))
        assertFalse("el corrupto tambien debe borrarse", ruta.exists())
        dir.deleteRecursively()
    }

    @Test
    fun `el contenido llega intacto aunque el sobre haya esperado`() {
        // El caso bloqueado deja el fichero vivo un rato. Lo que el usuario escribió tiene que
        // llegar igual a la nota, o la nota sería distinta de lo que se ve en el editor.
        val dir = tempDir()
        val markdown = "# Título\n\n- [ ] tarea con **fuerte** y `codigo`\n\n```kotlin\nval x = 1\n```\n"
        val ruta = MarkdownHandoff.write(dir, "intacto.md", markdown)

        // Simula la espera: el fichero sigue ahi, sin leer, durante el tiempo que haga falta.
        assertTrue(File(ruta).exists())

        val par = MarkdownHandoff.consume(ruta)
        assertEquals(markdown, par?.second)
        dir.deleteRecursively()
    }

    @Test
    fun `el nombre del archivo sobrevive intacto para el titulo de la nota`() {
        // El aviso necesita el nombre, y el título sale de ahí: si el sobre lo alterara, la nota se
        // llamaría distinto de lo que el usuario ve en el editor.
        val dir = tempDir()
        val ruta = MarkdownHandoff.write(dir, "Introducción a la edición.md", "x")

        assertEquals("Introducción a la edición.md", MarkdownHandoff.consume(ruta)?.first)
        dir.deleteRecursively()
    }
}