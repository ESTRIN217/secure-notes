package com.example

import com.example.data.model.BlockType
import com.example.data.model.DataBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FileBlockMetaTest {

    @Test
    fun `file block serializes and deserializes meta`() {
        val block = DataBlock(
            type = BlockType.FILE,
            content = "/data/user/0/com.example/files/file_1_12345.pdf",
            meta = mapOf(
                "name" to "report.pdf",
                "caption" to "Informe final",
                "color" to "#D32F2F",
                "showCaption" to "true"
            )
        )

        val json = DataBlock.serialize(listOf(block))
        val back = DataBlock.deserialize(json)

        assertNotNull(back)
        assertEquals(1, back!!.size)
        val restored = back[0]
        assertEquals(BlockType.FILE, restored.type)
        assertEquals(block.content, restored.content)
        assertEquals("report.pdf", restored.meta["name"])
        assertEquals("Informe final", restored.meta["caption"])
        assertEquals("#D32F2F", restored.meta["color"])
        assertEquals("true", restored.meta["showCaption"])
    }

    @Test
    fun `file block without name falls back to path basename`() {
        val block = DataBlock(type = BlockType.FILE, content = "/data/data/com.example/files/file_9_999.zip")
        assertEquals("file_9_999.zip", block.meta["name"] ?: block.content.substringAfterLast('/'))
    }

    @Test
    fun `file block with blank path has no render source`() {
        val block = DataBlock(type = BlockType.FILE, content = "")
        assertEquals("", block.content)
        assertNull(block.meta["name"])
    }
}
