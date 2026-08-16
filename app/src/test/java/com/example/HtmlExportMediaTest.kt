package com.example

import com.example.data.model.BlockType
import com.example.data.model.DataBlock
import com.example.data.model.TableData
import com.example.util.RichTextConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlExportMediaTest {

    private val fakeResolver = RichTextConverter.MediaHtmlResolver { block ->
        "data:image/png;base64," + when (block.type) {
            BlockType.IMAGE, BlockType.DRAWING -> "AAAA"
            BlockType.VIDEO -> "BBBB"
            BlockType.AUDIO, BlockType.VOICE -> "CCCC"
            BlockType.FILE -> "DDDD"
            else -> ""
        }
    }

    @Test
    fun `image block embeds data uri and caption figure`() {
        val block = DataBlock(
            type = BlockType.IMAGE,
            content = "/data/img_1.png",
            meta = mapOf("caption" to "Una foto")
        )
        val html = RichTextConverter.blocksToHtml(listOf(block), fakeResolver)

        assertTrue(html.contains("<img src=\"data:image/png;base64,AAAA\" alt=\"Una foto\">"))
        assertTrue(html.contains("<figure>"))
        assertTrue(html.contains("<figcaption>Una foto</figcaption>"))
    }

    @Test
    fun `image without resolver falls back to placeholder`() {
        val block = DataBlock(type = BlockType.IMAGE, content = "/data/img_1.png")

        assertEquals("[Image]", RichTextConverter.blocksToHtml(listOf(block)).trim())
    }

    @Test
    fun `video block embeds controls element`() {
        val block = DataBlock(type = BlockType.VIDEO, content = "/data/vid_1.mp4")

        assertTrue(RichTextConverter.blocksToHtml(listOf(block), fakeResolver).contains(
            "<video controls src=\"data:image/png;base64,BBBB\"></video>"
        ))
    }

    @Test
    fun `youtube video renders as link`() {
        val block = DataBlock(type = BlockType.VIDEO, content = "https://youtu.be/abc123")

        val html = RichTextConverter.blocksToHtml(listOf(block))
        assertTrue(html.contains("""<p class="video-link"><a href="https://youtu.be/abc123">▶ Video</a></p>"""))
    }

    @Test
    fun `audio and voice blocks embed controls element`() {
        val audio = DataBlock(type = BlockType.AUDIO, content = "/data/a.m4a")
        val voice = DataBlock(type = BlockType.VOICE, content = "/data/v.3gp")

        assertTrue(RichTextConverter.blocksToHtml(listOf(audio), fakeResolver).contains("<audio controls"))
        assertTrue(RichTextConverter.blocksToHtml(listOf(voice), fakeResolver).contains("<audio controls"))
    }

    @Test
    fun `file block embeds download link with name`() {
        val block = DataBlock(type = BlockType.FILE, content = "/data/reporte.pdf", meta = mapOf("name" to "reporte.pdf"))

        val html = RichTextConverter.blocksToHtml(listOf(block), fakeResolver)
        assertTrue(html.contains("""<a href="data:image/png;base64,DDDD" download="reporte.pdf">📎 reporte.pdf</a>"""))
    }

    @Test
    fun `file without resolver falls back to placeholder with name`() {
        val block = DataBlock(type = BlockType.FILE, content = "/data/reporte.pdf", meta = mapOf("name" to "reporte.pdf"))

        assertEquals("[File: reporte.pdf]", RichTextConverter.blocksToHtml(listOf(block)).trim())
    }

    @Test
    fun `drawing block embeds rasterized image`() {
        val block = DataBlock(
            type = BlockType.DRAWING,
            content = """[{"color":-16777216,"width":8,"points":[{"x":0,"y":0}]}]""",
            meta = mapOf("wysiwyg" to "true")
        )

        assertTrue(RichTextConverter.blocksToHtml(listOf(block), fakeResolver).contains(
            "<img src=\"data:image/png;base64,AAAA\""
        ))
    }

    @Test
    fun `table cells are html escaped`() {
        val data = TableData(
            headers = listOf("A <b>H</b>"),
            rows = listOf(listOf("x & y", "<script>"))
        )
        val block = DataBlock(type = BlockType.TABLE, content = "", meta = mapOf("table" to data.toJson()))

        val html = RichTextConverter.blocksToHtml(listOf(block))
        assertTrue(html.contains("&lt;b&gt;"))
        assertTrue(html.contains("x &amp; y"))
        assertTrue(html.contains("&lt;script&gt;"))
        assertTrue(!html.contains("<script>"))
    }

    @Test
    fun `legacy item markup migrates to checklist html`() {
        val html = RichTextConverter.contentToHtml("<item checked=\"true\">hecho</item>")

        assertTrue(html.contains("""<li data-checked="checked">hecho</li>"""))
    }
}
