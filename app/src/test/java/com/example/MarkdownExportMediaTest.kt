package com.example

import com.example.data.model.BlockType
import com.example.data.model.DataBlock
import com.example.data.model.TextSegment
import com.example.util.RichTextConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownExportMediaTest {

    private val fakeResolver = RichTextConverter.MediaMarkdownResolver { block ->
        when (block.type) {
            BlockType.IMAGE, BlockType.DRAWING -> "data:image/png;base64,AAAA"
            BlockType.VIDEO, BlockType.AUDIO, BlockType.VOICE, BlockType.FILE -> {
                "media/" + (block.meta["name"] ?: "file.bin")
            }
            else -> null
        }
    }

    @Test
    fun `image block embeds data uri with caption as alt`() {
        val block = DataBlock(
            type = BlockType.IMAGE,
            content = "/data/img_1.png",
            meta = mapOf("caption" to "Una foto")
        )

        val md = RichTextConverter.blocksToMarkdown(listOf(block), fakeResolver)

        assertTrue(md.contains("![Una foto](data:image/png;base64,AAAA)"))
    }

    @Test
    fun `image without resolver falls back to placeholder`() {
        val block = DataBlock(type = BlockType.IMAGE, content = "/data/img_1.png")

        assertEquals("[Image]", RichTextConverter.blocksToMarkdown(listOf(block)).trim())
    }

    @Test
    fun `drawing block embeds as image`() {
        val block = DataBlock(
            type = BlockType.DRAWING,
            content = """[{"color":-16777216,"width":8,"points":[{"x":0,"y":0}]}]""",
            meta = mapOf("wysiwyg" to "true")
        )

        assertTrue(RichTextConverter.blocksToMarkdown(listOf(block), fakeResolver).contains(
            "![Drawing](data:image/png;base64,AAAA)"
        ))
    }

    @Test
    fun `video block links to media file`() {
        val block = DataBlock(
            type = BlockType.VIDEO,
            content = "/data/vid_1.mp4",
            meta = mapOf("name" to "vid_1.mp4")
        )

        assertTrue(RichTextConverter.blocksToMarkdown(listOf(block), fakeResolver).contains(
            "[vid_1.mp4](media/vid_1.mp4)"
        ))
    }

    @Test
    fun `audio voice and file blocks link to media files`() {
        val audio = DataBlock(type = BlockType.AUDIO, content = "/data/a.m4a", meta = mapOf("name" to "a.m4a"))
        val voice = DataBlock(type = BlockType.VOICE, content = "/data/v.3gp", meta = mapOf("name" to "v.3gp"))
        val file = DataBlock(type = BlockType.FILE, content = "/data/reporte.pdf", meta = mapOf("name" to "reporte.pdf"))

        assertTrue(RichTextConverter.blocksToMarkdown(listOf(audio), fakeResolver).contains("[a.m4a](media/a.m4a)"))
        assertTrue(RichTextConverter.blocksToMarkdown(listOf(voice), fakeResolver).contains("[v.3gp](media/v.3gp)"))
        assertTrue(RichTextConverter.blocksToMarkdown(listOf(file), fakeResolver).contains("[reporte.pdf](media/reporte.pdf)"))
    }

    @Test
    fun `file without resolver falls back to placeholder with name`() {
        val block = DataBlock(type = BlockType.FILE, content = "/data/reporte.pdf", meta = mapOf("name" to "reporte.pdf"))

        assertEquals("[File: reporte.pdf]", RichTextConverter.blocksToMarkdown(listOf(block)).trim())
    }

    @Test
    fun `web image url passes through`() {
        val block = DataBlock(type = BlockType.IMAGE, content = "https://example.com/x.png")

        assertTrue(RichTextConverter.blocksToMarkdown(listOf(block), fakeResolver).contains(
            "![Image](https://example.com/x.png)"
        ))
    }

    @Test
    fun `legacy img markup migrates to markdown image`() {
        val md = RichTextConverter.contentToMarkdown("""<img src="/data/img_1.png"/>""", fakeResolver)

        assertTrue(md.contains("![Image](data:image/png;base64,AAAA)"))
    }

    @Test
    fun `legacy audio markup migrates to markdown link`() {
        val md = RichTextConverter.contentToMarkdown("""!audio[Nota de voz](/data/v.3gp)""", fakeResolver)

        assertTrue(md.contains("[Audio](media/file.bin)"))
    }

    @Test
    fun `collapsible renders as html details with inner markdown`() {
        val block = DataBlock(
            type = BlockType.COLLAPSIBLE,
            content = "",
            meta = mapOf("summary" to "Ver más"),
            richTextJson = TextSegment.serialize(listOf(TextSegment(text = "contenido **oculto**")))
        )

        val md = RichTextConverter.blocksToMarkdown(listOf(block), fakeResolver)

        assertTrue(md.contains("<details><summary>Ver más</summary>"))
        assertTrue(md.contains("contenido **oculto**"))
        assertTrue(md.endsWith("</details>"))
    }

    @Test
    fun `page block keeps label`() {
        val block = DataBlock(type = BlockType.PAGE, content = "Mi página")

        assertTrue(RichTextConverter.blocksToMarkdown(listOf(block)).contains("📄 Mi página"))
    }
}
