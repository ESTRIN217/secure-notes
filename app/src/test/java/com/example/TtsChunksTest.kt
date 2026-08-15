package com.example

import com.example.data.model.BlockType
import com.example.data.model.DataBlock
import com.example.data.model.TextSegment
import com.example.util.buildTtsChunks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TtsChunksTest {

    private fun textBlock(content: String) = DataBlock(type = BlockType.TEXT, content = content)

    @Test
    fun `title comes first then blocks`() {
        val chunks = buildTtsChunks(
            "Mi lista",
            listOf(textBlock("Comprar pan"), textBlock("Comprar leche"))
        )
        assertEquals(listOf("Mi lista", "Comprar pan", "Comprar leche"), chunks)
    }

    @Test
    fun `strips markup and json`() {
        val chunks = buildTtsChunks("", listOf(textBlock("<b>negrita</b> y <url=http://x.com>enlace</url>")))
        assertEquals(listOf("negrita y enlace"), chunks)
        assertTrue(chunks.none { it.contains("<b>") || it.contains("{") || it.contains("richTextJson") })
    }

    @Test
    fun `filters blank blocks`() {
        val chunks = buildTtsChunks(
            "",
            listOf(textBlock("   "), DataBlock(type = BlockType.HORIZONTAL_RULE), textBlock("contenido"))
        )
        assertEquals(listOf("contenido"), chunks)
    }

    @Test
    fun `checklist item is plain text`() {
        val block = DataBlock(
            type = BlockType.CHECKLIST_ITEM,
            meta = mapOf("checked" to "true"),
            content = "Comprar leche"
        )
        assertEquals(listOf("Comprar leche"), buildTtsChunks("", listOf(block)))
    }

    @Test
    fun `uses richTextJson when present`() {
        val block = DataBlock(
            type = BlockType.TEXT,
            content = "",
            richTextJson = TextSegment.serialize(listOf(TextSegment(text = "Hola", bold = true)))
        )
        assertEquals(listOf("Hola"), buildTtsChunks("", listOf(block)))
    }

    @Test
    fun `empty note returns empty list`() {
        assertTrue(buildTtsChunks("", emptyList()).isEmpty())
        assertTrue(buildTtsChunks("", listOf(textBlock(" "), DataBlock(type = BlockType.HORIZONTAL_RULE))).isEmpty())
    }

    @Test
    fun `blank title skipped`() {
        assertEquals(listOf("solo bloque"), buildTtsChunks("  ", listOf(textBlock("solo bloque"))))
    }
}
