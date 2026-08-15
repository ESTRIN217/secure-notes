package com.example

import com.example.data.model.BlockType
import com.example.data.model.DataBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BookmarkBlockMetaTest {

    @Test
    fun `bookmark block serializes and deserializes meta`() {
        val block = DataBlock(
            type = BlockType.BOOKMARK,
            content = "https://example.com/article",
            meta = mapOf(
                "title" to "Ejemplo",
                "description" to "Descripción del artículo",
                "favicon" to "https://www.google.com/s2/favicons?domain=example.com&sz=64",
                "caption" to "Mi marcador",
                "color" to "#D32F2F",
                "showCaption" to "true"
            )
        )

        val json = DataBlock.serialize(listOf(block))
        val back = DataBlock.deserialize(json)

        assertNotNull(back)
        assertEquals(1, back!!.size)
        val restored = back[0]
        assertEquals(BlockType.BOOKMARK, restored.type)
        assertEquals(block.content, restored.content)
        assertEquals("Ejemplo", restored.meta["title"])
        assertEquals("Descripción del artículo", restored.meta["description"])
        assertEquals("https://www.google.com/s2/favicons?domain=example.com&sz=64", restored.meta["favicon"])
        assertEquals("Mi marcador", restored.meta["caption"])
        assertEquals("#D32F2F", restored.meta["color"])
        assertEquals("true", restored.meta["showCaption"])
    }

    @Test
    fun `bookmark block with blank url has no render source`() {
        val block = DataBlock(type = BlockType.BOOKMARK, content = "")
        assertEquals("", block.content)
        assertNull(block.meta["title"])
    }
}
