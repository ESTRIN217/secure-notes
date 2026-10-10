package com.example

import com.example.util.MarkdownHandoff
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * El sobre de traspaso entre `CodeEditorActivity` y `MainActivity` (spec 013, T2).
 *
 * Es lo que permite que el editor de código, que vive **fuera** del flujo de bloqueo de la bóveda,
 * llegue a crear una nota sin escribir nunca en la base de notas.
 *
 * JUnit puro: el sobre es un fichero en un directorio, sin `ContentResolver` ni Android (RNF-38).
 */
class NoteHandoffTest {

    /** Un directorio temporal por prueba: los sobres no se mezclan ni se limpian entre pruebas. */
    private fun tempDir(): File =
        File(System.getProperty("java.io.tmpdir"), "handoff-test-${System.nanoTime()}").apply { mkdirs() }

    // ---------------------------------------------------------------- escritura y lectura

    @Test
    fun `el sobre sobrevive a la escritura y la lectura`() {
        val dir = tempDir()
        val ruta = MarkdownHandoff.write(dir, "notas/Introduccion.md", "# Hola\n\nCuerpo.\n")

        assertTrue(File(ruta).exists())
        val leido = MarkdownHandoff.read(ruta)
        assertEquals("notas/Introduccion.md", leido?.first)
        assertEquals("# Hola\n\nCuerpo.\n", leido?.second)
        dir.deleteRecursively()
    }

    @Test
    fun `el sobre conserva el markdown tal cual`() {
        val dir = tempDir()
        val markdown = "## Sección\n\n- [ ] tarea\n- [x] hecha\n\n```kotlin\nval x = 1\n```\n"
        val ruta = MarkdownHandoff.write(dir, "prueba.md", markdown)
        assertEquals(markdown, MarkdownHandoff.read(ruta)?.second)
        dir.deleteRecursively()
    }

    @Test
    fun `el markdown con comillas y saltos de linea sobrevive`() {
        val dir = tempDir()
        val markdown = "Un \"texto\" entre comillas\ny otra línea\n"
        val ruta = MarkdownHandoff.write(dir, "citas.md", markdown)
        assertEquals(markdown, MarkdownHandoff.read(ruta)?.second)
        dir.deleteRecursively()
    }

    @Test
    fun `un nombre con acentos y espacios sobrevive`() {
        val dir = tempDir()
        val ruta = MarkdownHandoff.write(dir, "Introducción a la edición.md", "texto")
        assertEquals("Introducción a la edición.md", MarkdownHandoff.read(ruta)?.first)
        dir.deleteRecursively()
    }

    @Test
    fun `la ruta se escribe dentro del directorio dado`() {
        val dir = tempDir()
        val ruta = MarkdownHandoff.write(dir, "x.md", "y")
        assertTrue("la ruta se salio del directorio: $ruta", File(ruta).canonicalPath.startsWith(dir.canonicalPath))
        dir.deleteRecursively()
    }

    // ---------------------------------------------------------------- el nombre del fichero

    @Test
    fun `el nombre del sobre no lleva nada del usuario`() {
        // RNF-36: la ruta acaba en un log cuando algo falla, y el nombre del archivo del usuario no
        // puede aparecer ahi. El nombre va DENTRO del sobre, no en el nombre del fichero.
        val dir = tempDir()
        val ruta = MarkdownHandoff.write(dir, "Documento Confidencial del Adrian.md", "secreto")
        val nombreFichero = File(ruta).name

        assertFalse("la ruta incluye el nombre del usuario: $nombreFichero", nombreFichero.contains("Adrian"))
        assertFalse(nombreFichero.contains("Confidencial"))
        assertEquals(MarkdownHandoff.FILE_NAME, nombreFichero)
        dir.deleteRecursively()
    }

    @Test
    fun `el nombre del sobre es el mismo siempre`() {
        val dir = tempDir()
        val a = MarkdownHandoff.write(dir, "uno.md", "a")
        val b = MarkdownHandoff.write(dir, "otro.md", "b")
        assertEquals(File(a).name, File(b).name)
        dir.deleteRecursively()
    }

    // ---------------------------------------------------------------- borrado

    @Test
    fun `el sobre se borra tras consumirse`() {
        // RNF-35: cacheDir no lo limpia el sistema de forma fiable, y un markdown sin cifrar que se
        // queda ahi indefinidamente es justo lo que este proyecto evita.
        val dir = tempDir()
        val ruta = MarkdownHandoff.write(dir, "nota.md", "contenido")
        assertTrue(File(ruta).exists())

        MarkdownHandoff.consume(ruta)

        assertFalse("el sobre sobrevivio al consumo", File(ruta).exists())
        dir.deleteRecursively()
    }

    @Test
    fun `consumir devuelve el contenido y borra en el mismo paso`() {
        val dir = tempDir()
        val ruta = MarkdownHandoff.write(dir, "nota.md", "el cuerpo")

        val leido = MarkdownHandoff.consume(ruta)

        assertEquals("nota.md", leido?.first)
        assertEquals("el cuerpo", leido?.second)
        assertFalse(File(ruta).exists())
        dir.deleteRecursively()
    }

    @Test
    fun `consumir un sobre que no existe no revienta`() {
        // Puede pasar: el fichero se limpia solo entre el arranque y el consumo.
        assertNull(MarkdownHandoff.consume("/no/existe/sobrenada.json"))
    }

    @Test
    fun `un sobre corrupto devuelve null en vez de propagar la excepcion`() {
        val dir = tempDir()
        val ruta = File(dir, MarkdownHandoff.FILE_NAME).apply { writeText("esto no es json") }

        assertNull(MarkdownHandoff.read(ruta.absolutePath))

        MarkdownHandoff.consume(ruta.absolutePath)
        dir.deleteRecursively()
    }

    @Test
    fun `un sobre sin los dos campos devuelve null`() {
        val dir = tempDir()
        val ruta = File(dir, MarkdownHandoff.FILE_NAME).apply { writeText("""{"name":"solo el nombre"}""") }
        assertNull(MarkdownHandoff.read(ruta.absolutePath))
        dir.deleteRecursively()
    }

    @Test
    fun `el contenido vuelve intacto tras un viaje largo`() {
        // El sobre pasa por disco: si el JSON no escapara bien, el texto llegaria alterado a la nota
        // y se perderia el trabajo del usuario sin que nada fallara.
        val dir = tempDir()
        val markdown = (1..50).joinToString("\n") { "Línea $it con acentos: áéíóúñ **$it**" }
        val ruta = MarkdownHandoff.write(dir, "largo.md", markdown)
        assertEquals(markdown, MarkdownHandoff.consume(ruta)?.second)
        dir.deleteRecursively()
    }
}
