package com.example.ui

import com.example.data.model.Attachment
import com.example.data.model.BlockType
import com.example.data.model.DataBlock
import com.example.data.model.TableData
import com.example.data.model.TextSegment
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La regla «una nota nueva está vacía» (spec 032, RF-207…RF-210).
 *
 * Es **pura** —título, bloques y adjuntos— y no importa `android`, así que corre en esta plataforma donde
 * Robolectric no corre (RNF-135). El defecto que arregla: `DataBlock.serialize(emptyList())` produce `"[]"`,
 * que no es blank, así que el editor consideraba «con contenido» una nota nueva sin nada dentro y
 * la insertaba en la base de datos al pulsar atrás.
 */
class DraftEmptinessTest {

    private fun textBlock(text: String, styledOnly: Boolean = false) = DataBlock(
        type = BlockType.TEXT,
        content = "",
        richTextJson = TextSegment.serialize(
            listOf(TextSegment(text = text, bold = styledOnly))
        )
    )

    // ── RF-210: la nota nueva de fábrica está vacía (la regresión de esta spec) ─────────────

    @Test
    fun `una nota nueva sin titulo sin bloques y sin adjuntos esta vacia`() {
        assertTrue(isNoteEmpty(title = "", blocks = emptyList(), attachments = emptyList()))
    }

    @Test
    fun `un titulo con solo espacios esta vacio`() {
        assertTrue(isNoteEmpty(title = "   ", blocks = emptyList(), attachments = emptyList()))
    }

    @Test
    fun `un titulo con contenido no esta vacio`() {
        assertFalse(isNoteEmpty(title = "Comprar pan", blocks = emptyList(), attachments = emptyList()))
    }

    @Test
    fun `un adjunto cualquiera no esta vacio`() {
        assertFalse(
            isNoteEmpty(
                title = "",
                blocks = emptyList(),
                attachments = listOf(Attachment(type = "file", path = "/files/nota.pdf", name = "nota.pdf"))
            )
        )
    }

    // ── Bloques de texto ──────────────────────────────────────────────────────────────────

    @Test
    fun `un bloque de texto con solo formato y sin texto esta vacio`() {
        assertTrue(isNoteEmpty(title = "", blocks = listOf(textBlock("", styledOnly = true)), attachments = emptyList()))
    }

    @Test
    fun `un bloque de texto con texto no esta vacio`() {
        assertFalse(isNoteEmpty(title = "", blocks = listOf(textBlock("hola")), attachments = emptyList()))
    }

    @Test
    fun `un bloque de texto con solo una ecuacion no esta vacio`() {
        val block = DataBlock(
            type = BlockType.TEXT,
            richTextJson = TextSegment.serialize(listOf(TextSegment(text = "", equationLatex = "x^2")))
        )
        assertFalse(isNoteEmpty(title = "", blocks = listOf(block), attachments = emptyList()))
    }

    @Test
    fun `un bloque legacy con markup sin texto esta vacio`() {
        val block = DataBlock(type = BlockType.TEXT, content = "<b></b>")
        assertTrue(isNoteEmpty(title = "", blocks = listOf(block), attachments = emptyList()))
    }

    @Test
    fun `un bloque legacy con texto esta no vacio`() {
        val block = DataBlock(type = BlockType.TEXT, content = "<b>hola</b>")
        assertFalse(isNoteEmpty(title = "", blocks = listOf(block), attachments = emptyList()))
    }

    // ── Bloques sin texto ────────────────────────────────────────────────────────────────

    @Test
    fun `una regla horizontal sola esta vacia`() {
        val hr = DataBlock(type = BlockType.HORIZONTAL_RULE)
        assertTrue(isNoteEmpty(title = "", blocks = listOf(hr), attachments = emptyList()))
    }

    @Test
    fun `una tabla con celdas vacias esta vacia`() {
        val table = DataBlock(type = BlockType.TABLE, meta = mapOf("table" to TableData(rows = listOf(listOf("", ""), listOf("  ", ""))).toJson()))
        assertTrue(isNoteEmpty(title = "", blocks = listOf(table), attachments = emptyList()))
    }

    @Test
    fun `una tabla con una celda con texto no esta vacia`() {
        val table = DataBlock(type = BlockType.TABLE, meta = mapOf("table" to TableData(rows = listOf(listOf("", "precio"), listOf("", ""))).toJson()))
        assertFalse(isNoteEmpty(title = "", blocks = listOf(table), attachments = emptyList()))
    }

    @Test
    fun `un dibujo wysiwyg sin trazos esta vacio`() {
        val drawing = DataBlock(type = BlockType.DRAWING, content = "[]", meta = mapOf("wysiwyg" to "true"))
        assertTrue(isNoteEmpty(title = "", blocks = listOf(drawing), attachments = emptyList()))
    }

    @Test
    fun `un dibujo wysiwyg con trazos no esta vacio`() {
        val drawing = DataBlock(type = BlockType.DRAWING, content = """[{"t":1}]""", meta = mapOf("wysiwyg" to "true"))
        assertFalse(isNoteEmpty(title = "", blocks = listOf(drawing), attachments = emptyList()))
    }

    @Test
    fun `un dibujo legacy con ruta no esta vacio`() {
        val drawing = DataBlock(type = BlockType.DRAWING, content = "/files/drawings/d1.json")
        assertFalse(isNoteEmpty(title = "", blocks = listOf(drawing), attachments = emptyList()))
    }

    @Test
    fun `una imagen con ruta no esta vacia`() {
        val image = DataBlock(type = BlockType.IMAGE, content = "/files/img.png")
        assertFalse(isNoteEmpty(title = "", blocks = listOf(image), attachments = emptyList()))
    }

    @Test
    fun `un PAGE con destino no esta vacio`() {
        val page = DataBlock(type = BlockType.PAGE, content = "", meta = mapOf("noteId" to "42"))
        assertFalse(isNoteEmpty(title = "", blocks = listOf(page), attachments = emptyList()))
    }

    @Test
    fun `un PAGE sin destino esta vacio`() {
        val page = DataBlock(type = BlockType.PAGE, content = "", meta = emptyMap())
        assertTrue(isNoteEmpty(title = "", blocks = listOf(page), attachments = emptyList()))
    }

    @Test
    fun `un elemento con contenido entre otros vacios hace a la nota no vacia`() {
        val blocks = listOf(
            textBlock(""),
            DataBlock(type = BlockType.HORIZONTAL_RULE),
            textBlock("  "),
            DataBlock(type = BlockType.IMAGE, content = "/files/a.png")
        )
        assertFalse(isNoteEmpty(title = "", blocks = blocks, attachments = emptyList()))
    }

    @Test
    fun `solo bloques vacios no es contenido`() {
        val hr = DataBlock(type = BlockType.HORIZONTAL_RULE)
        val blocks = listOf(textBlock(""), hr, textBlock(""))
        assertTrue(isNoteEmpty(title = "", blocks = blocks, attachments = emptyList()))
    }
}
