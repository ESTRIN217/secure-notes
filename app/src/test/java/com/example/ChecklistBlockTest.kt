package com.example

import com.example.data.model.BlockType
import com.example.data.model.DataBlock
import com.example.data.model.TextSegment
import com.example.util.RichTextConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChecklistBlockTest {

    private fun formattedChecklist(): DataBlock {
        val segments = listOf(
            TextSegment(text = "Comprar ", bold = true),
            TextSegment(text = "leche", italic = true, colorHex = "#FF0000"),
            TextSegment(text = " y pan", underline = true)
        )
        return DataBlock(
            type = BlockType.CHECKLIST_ITEM,
            content = "",
            meta = mapOf("checked" to "false"),
            richTextJson = TextSegment.serialize(segments)
        )
    }

    @Test
    fun `checklist block roundtrips formatted segments`() {
        val original = formattedChecklist()
        val json = DataBlock.serialize(listOf(original))
        val restored = DataBlock.deserialize(json)!!

        assertEquals(1, restored.size)
        assertEquals(BlockType.CHECKLIST_ITEM, restored[0].type)
        assertEquals("false", restored[0].meta["checked"])
        assertEquals(original.ensureSegments().size, restored[0].ensureSegments().size)
        original.ensureSegments().zip(restored[0].ensureSegments()).forEach { (a, b) ->
            assertTrue("no preserva estilo: $a vs $b", a.hasSameStyle(b))
        }
    }

    @Test
    fun `checklist ensureSegments keeps rich text`() {
        val segs = formattedChecklist().ensureSegments()
        val bold = segs.first { "Comprar" in it.text }
        val colored = segs.first { "leche" in it.text }
        val underlined = segs.first { "pan" in it.text }

        assertTrue(bold.bold)
        assertTrue(colored.italic)
        assertEquals("#FF0000", colored.colorHex)
        assertTrue(underlined.underline)
    }

    @Test
    fun `checklist markdown export preserves formatting`() {
        val md = RichTextConverter.blocksToMarkdown(listOf(formattedChecklist()))

        assertTrue(md.startsWith("- [ ] "))
        assertTrue(md.contains("**Comprar **"))
        assertTrue(md.contains("*leche*"))
        assertTrue(md.contains("<ins> y pan</ins>"))
    }

    @Test
    fun `checklist html export preserves formatting`() {
        val html = RichTextConverter.blocksToHtml(listOf(formattedChecklist()))

        assertTrue(html.contains("data-checked=\"\""))
        assertTrue(html.contains("font-weight:bold"))
        assertTrue(html.contains("font-style:italic"))
        assertTrue(html.contains("color:rgb(255, 0, 0)"))
        assertTrue(html.contains("text-decoration:underline"))
    }

    @Test
    fun `toggle keeps indent meta`() {
        val block = formattedChecklist().copy(meta = mapOf("checked" to "true", "indentLevel" to "2"))
        val toggled = block.copy(meta = block.meta + ("checked" to "false"))

        assertEquals("false", toggled.meta["checked"])
        assertEquals("2", toggled.meta["indentLevel"])
    }
}
