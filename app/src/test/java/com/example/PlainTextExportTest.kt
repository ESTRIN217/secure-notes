package com.example

import com.example.data.model.BlockType
import com.example.data.model.DataBlock
import com.example.data.model.TableData
import com.example.data.model.TextSegment
import com.example.util.RichTextConverter
import org.junit.Assert.assertEquals
import org.junit.Test

class PlainTextExportTest {

    @Test
    fun `table block exports as plain text table`() {
        val data = TableData(
            headers = listOf("A", "B"),
            rows = listOf(listOf("1", "2"))
        )
        val block = DataBlock(type = BlockType.TABLE, content = "", meta = mapOf("table" to data.toJson()))

        assertEquals(
            "| A | B |\n| --- | --- |\n| 1 | 2 |",
            RichTextConverter.blocksToPlainText(listOf(block))
        )
    }

    @Test
    fun `collapsible block exports summary and body`() {
        val block = DataBlock(
            type = BlockType.COLLAPSIBLE,
            content = "",
            meta = mapOf("summary" to "Ver más"),
            richTextJson = TextSegment.serialize(listOf(TextSegment(text = "cuerpo oculto")))
        )

        assertEquals(
            "Ver más\ncuerpo oculto",
            RichTextConverter.blocksToPlainText(listOf(block))
        )
    }

    @Test
    fun `horizontal rule keeps following text on its own line`() {
        fun textBlock(text: String) = DataBlock(
            type = BlockType.TEXT,
            richTextJson = TextSegment.serialize(listOf(TextSegment(text = text)))
        )
        val blocks = listOf(
            textBlock("arriba"),
            DataBlock(type = BlockType.HORIZONTAL_RULE),
            textBlock("abajo")
        )

        assertEquals(
            "arriba\n───\nabajo",
            RichTextConverter.blocksToPlainText(blocks)
        )
    }

    @Test
    fun `media blocks export as bracketed placeholders`() {
        val fileBlock = DataBlock(type = BlockType.FILE, content = "/storage/reporte.pdf", meta = mapOf("name" to "reporte.pdf"))
        val imgBlock = DataBlock(type = BlockType.IMAGE, content = "/storage/img.png")

        assertEquals(
            "[File: reporte.pdf]\n[Image]",
            RichTextConverter.blocksToPlainText(listOf(fileBlock, imgBlock))
        )
    }

    @Test
    fun `legacy item and hr markup keep checklist and rule markers`() {
        val raw = "<item checked=\"true\">hecho</item>\n" +
            "<item checked=\"false\">pendiente</item>\n" +
            "<item>sin marca</item>\n" +
            "<hr/>\ntexto"

        assertEquals(
            "☑ hecho\n☐ pendiente\n☐ sin marca\n───\ntexto",
            RichTextConverter.contentToPlainText(raw)
        )
    }

    @Test
    fun `legacy details exports summary and body`() {
        val raw = "<details><summary>Resumen</summary>cuerpo de detalle</details>"

        assertEquals(
            "Resumen\ncuerpo de detalle",
            RichTextConverter.contentToPlainText(raw)
        )
    }

    @Test
    fun `legacy media tags export as placeholders`() {
        val raw = "foto: <img src=\"/storage/f.png\" />\n" +
            "video: <video src=\"/storage/v.mp4\"></video>"

        assertEquals(
            "foto: [Image]\nvideo: [Video]",
            RichTextConverter.contentToPlainText(raw)
        )
    }
}
