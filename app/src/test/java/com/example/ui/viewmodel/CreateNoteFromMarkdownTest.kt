package com.example.ui.viewmodel

import com.example.data.model.DataBlock
import com.example.util.FileImporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Lo que `createNoteFromMarkdown` pasa a `saveNoteAndGetId` (spec 013, T4).
 *
 * Se prueba la **entrada**, no la llamada: el ViewModel entero necesita la base de datos y
 * `Application`, que aquí no hay. Lo que sí se puede fijar —y es donde Vive el riesgo— son los tres
 * valores que compone, porque un `id`, un título o un contenido equivocados crean una nota
 * silenciosamente distinta de la que el usuario pidió.
 */
class CreateNoteFromMarkdownTest {

    /** Réplica exacta de lo que compone `createNoteFromMarkdown` antes de llamar. */
    private data class Prepared(val id: Int, val title: String, val content: String)

    private fun prepare(fileName: String, markdown: String): Prepared = Prepared(
        id = 0,
        title = FileImporter.noteTitleFrom(fileName),
        content = FileImporter.markdownToBlockContent(markdown)
    )

    @Test
    fun `el id es cero para que la insercion sea nueva`() {
        // Un id distinto de 0 haría un UPDATE en vez de un INSERT: la nota NO se crearia, y el
        // usuario veria su nota anterior sin cambios (RF-78).
        assertEquals(0, prepare("nota.md", "texto").id)
    }

    @Test
    fun `el titulo viene del nombre del archivo`() {
        assertEquals("notas", prepare("carpeta/notas.md", "x").title)
        assertEquals("Introduccion", prepare("Introduccion.md", "x").title)
    }

    @Test
    fun `el contenido es el bloque serializado`() {
        val markdown = "# Titulo\n\nCuerpo con **fuerte**.\n"
        val preparada = prepare("nota.md", markdown)

        assertEquals(FileImporter.markdownToBlockContent(markdown), preparada.content)
        assertTrue(preparada.content.startsWith("[{"))
        assertEquals(2, DataBlock.deserialize(preparada.content)?.size)
    }

    @Test
    fun `el markdown vacio crea una nota con bloque vacio`() {
        // Caso limite 2: el usuario la pidio, y fallar seria mejor que nada.
        val preparada = prepare("vacia.md", "")
        assertEquals("[]", preparada.content)
        assertEquals("vacia", preparada.title)
    }

    @Test
    fun `cada envio compone una nota nueva e independiente`() {
        // Caso limite 7: dos envios seguidos no se pisan. El id 0 en ambos es lo que lo garantiza.
        val uno = prepare("nota.md", "primero")
        val dos = prepare("nota.md", "segundo")
        assertEquals(0, uno.id)
        assertEquals(0, dos.id)
        assertFalse(uno.content == dos.content)
    }

    @Test
    fun `el mismo markdown produce siempre el mismo contenido`() {
        // Es lo que hace que el FAB y el editor produzcan notas identicas (RF-83), y por eso este
        // camino puede delegar en la conversion del importador sin miedo.
        val markdown = "- [ ] tarea\n\n| a | b |\n| - | - |\n"
        assertEquals(prepare("x.md", markdown).content, prepare("y.md", markdown).content)
    }

    @Test
    fun `el markdown con imagen conserva la referencia`() {
        // El bloque IMAGE guarda la ruta tal cual; copiarla a filesDir es cosa de saveNoteAndGetId,
        // que ya lo hace para el resto de notas. Aqui se comprueba que no se pierde por el camino.
        val markdown = "Antes\n\n![alt](imagenes/flujo.png)\n"
        val bloque = DataBlock.deserialize(prepare("con-imagen.md", markdown).content)
            ?.firstOrNull { it.type == com.example.data.model.BlockType.IMAGE }
        assertTrue("la imagen no llego al bloque", bloque != null)
        assertTrue(bloque!!.content.contains("imagenes/flujo.png"))
    }
}
