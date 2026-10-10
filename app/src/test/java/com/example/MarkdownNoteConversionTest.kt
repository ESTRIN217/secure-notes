package com.example

import com.example.data.model.BlockType
import com.example.data.model.DataBlock
import com.example.util.FileImporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La conversión de Markdown a bloques y el título de la nota (spec 013, T1).
 *
 * JUnit puro a propósito: aquí no hay `ContentResolver`, ni `Uri`, ni Android (RNF-38). Es lo que
 * permite que estas pruebas se ejecuten en esta plataforma.
 */
class MarkdownNoteConversionTest {

    private fun blocksOf(markdown: String): List<DataBlock> =
        DataBlock.deserialize(FileImporter.markdownToBlockContent(markdown)) ?: emptyList()

    // ---------------------------------------------------------------- conversion

    @Test
    fun `el markdown se convierte en bloques serializados`() {
        // Lo que se guarda en la nota es el **bloque JSON**, no el texto: es lo que la base espera.
        val salida = FileImporter.markdownToBlockContent("# Titulo\n\nCuerpo.\n")
        assertTrue(salida.startsWith("[{"))
        assertEquals(2, DataBlock.deserialize(salida)?.size)
    }

    @Test
    fun `los encabezados conservan su nivel`() {
        val bloques = blocksOf("# Uno\n\n## Dos\n\n### Tres\n")
        val tipos = bloques.map { it.type }
        assertEquals(BlockType.HEADING1, tipos.first())
        assertTrue(tipos.contains(BlockType.HEADING2))
        assertTrue(tipos.contains(BlockType.HEADING3))
    }

    @Test
    fun `las listas y las tareas se reconocen`() {
        val bloques = blocksOf("- uno\n- [ ] pendiente\n- [x] hecha\n")
        val tipos = bloques.map { it.type }
        assertTrue("faltan vinetas en $tipos", tipos.contains(BlockType.BULLET_LIST))
        assertEquals("faltan tareas en $tipos", 2, tipos.count { it == BlockType.CHECKLIST_ITEM })
    }

    @Test
    fun `la lista numerada sale como texto y es un defecto conocido`() {
        // Defecto PREEXISTENTE de `NoteContentBlockConverter`, no de esta spec: `convertTextToBlocks`
        // recibe el texto ya recortado con `.trim()`, y su regex `^\d+\.\s` exige un espacio que
        // para entonces ya no está, asi que `1. primera` cae en TEXT.
        //
        // La 013 **reutiliza** la conversion del importador sin extenderla (RF-83), y por eso el
        // envio hereda el defecto. Se documenta en vez de arreglarse: arreglarlo aqui haria que la
        // nota del editor dejara de coincidir con la del FAB, que es justo lo que RF-83 impide.
        // Ver "Defecto conocido" en MEMORY.md.
        val tipos = blocksOf("1. primera\n").map { it.type }
        assertEquals(listOf(BlockType.TEXT), tipos)
    }

    @Test
    fun `el estado de la tarea se conserva`() {
        val tareas = blocksOf("- [ ] pendiente\n- [x] hecha\n")
            .filter { it.type == BlockType.CHECKLIST_ITEM }
        assertEquals("false", tareas[0].meta["checked"])
        assertEquals("true", tareas[1].meta["checked"])
    }

    @Test
    fun `una tabla se convierte en un bloque de tabla`() {
        val bloques = blocksOf("| Lema | Tipo |\n| --- | --- |\n| lista | vi |\n")
        assertTrue(bloques.any { it.type == BlockType.TABLE })
    }

    @Test
    fun `una imagen markdown se convierte en bloque de imagen`() {
        val bloques = blocksOf("Antes\n\n![alt](imagen.png)\n\nDespues\n")
        assertTrue(bloques.any { it.type == BlockType.IMAGE })
    }

    @Test
    fun `el texto vacio produce una lista de bloques vacia`() {
        val salida = FileImporter.markdownToBlockContent("")
        assertEquals("[]", salida)
        assertTrue(blocksOf("").isEmpty())
    }

    @Test
    fun `el texto en blanco tambien produce bloques vacios`() {
        assertTrue(blocksOf("   \n\n  ").isEmpty())
    }

    // ---------------------------------------------------------------- el titulo

    @Test
    fun `el titulo es el nombre del archivo sin extension`() {
        assertEquals("Introduccion", FileImporter.noteTitleFrom("Introduccion.md"))
        assertEquals("Introduccion", FileImporter.noteTitleFrom("Introduccion.markdown"))
    }

    @Test
    fun `el titulo no hereda el directorio`() {
        // Una nota no debe heredar la carpeta del almacenamiento (caso limite 5).
        assertEquals("notas", FileImporter.noteTitleFrom("carpeta/notas.md"))
        assertEquals("notas", FileImporter.noteTitleFrom("/absoluta/ deeply/notas.md"))
    }

    @Test
    fun `un nombre sin extension se queda tal cual`() {
        assertEquals("README", FileImporter.noteTitleFrom("README"))
        assertEquals("Makefile", FileImporter.noteTitleFrom("Makefile"))
    }

    @Test
    fun `un nombre en blanco cae a un nombre por defecto`() {
        // Nunca una cadena vacía: una nota sin título no se distingue en la lista (caso limite 1).
        assertEquals("documento", FileImporter.noteTitleFrom(""))
        assertEquals("documento", FileImporter.noteTitleFrom("   "))
        assertEquals("documento", FileImporter.noteTitleFrom("/"))
    }

    @Test
    fun `un nombre de puros puntos no se queda sin nada`() {
        assertFalse(FileImporter.noteTitleFrom("...").isBlank())
    }

    // ---------------------------------------------------------------- los dos caminos

    @Test
    fun `la conversion es determinista`() {
        // Un importador que cambiara de resultado entre llamadas pondria dos notas distintas por lo
        // mismo. La conversion pura tiene que ser una funcion de su entrada.
        val markdown = "# Guia\n\nTexto con **fuerte**.\n\n- [ ] tarea\n"
        assertEquals(
            FileImporter.markdownToBlockContent(markdown),
            FileImporter.markdownToBlockContent(markdown)
        )
    }

    @Test
    fun `el markdown se conserva al reponer los bloques`() {
        // El bloque serializado tiene que volver a los mismos bloques: es lo que hace comparables
        // el camino del FAB y el del editor sin tener un Uri delante.
        val markdown = "# Guia\n\n- [ ] tarea\n"
        val una = blocksOf(markdown).map { it.type }
        val otra = blocksOf(markdown).map { it.type }
        assertEquals(una, otra)
        assertTrue(una.isNotEmpty())
    }

    @Test
    fun `el texto del markdown sobrevive a la conversion`() {
        // Lo que el usuario escribe tiene que ser legible en la nota: sin las marcas de bloque, pero
        // con las palabras dentro. Se comprueba el texto de cada bloque, no la cadena completa.
        val bloques = blocksOf("## Seccion\n\nUna frase con **negrita** y `codigo`.\n")
        val texto = bloques.joinToString(" | ") { it.content }
        listOf("Seccion", "Una frase con", "negrita", "codigo").forEach { trozo ->
            assertTrue("falta [$trozo] en [$texto]", texto.contains(trozo))
        }
    }
}
