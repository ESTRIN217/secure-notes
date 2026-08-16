package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.os.Build
import androidx.compose.ui.geometry.Offset
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.BlockType
import com.example.data.model.DataBlock
import com.example.data.model.DecryptedNote
import com.example.data.model.DrawingStroke
import com.example.data.model.DrawingStrokeCodec
import com.example.data.model.Note
import com.example.data.model.TableData
import com.example.data.model.TextSegment
import com.example.util.export.HtmlMediaEmbedder
import com.example.util.export.PdfBlockRenderer
import com.example.util.export.PdfExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class PdfExporterTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun block(
        type: BlockType,
        content: String = "",
        meta: Map<String, String> = emptyMap(),
        segments: List<TextSegment> = emptyList()
    ): DataBlock {
        val rich = if (segments.isEmpty()) null else TextSegment.serialize(segments)
        return DataBlock(type = type, content = content, meta = meta, richTextJson = rich)
    }

    private fun textBlock(text: String, vararg styleSegs: TextSegment): DataBlock =
        block(BlockType.TEXT, segments = if (styleSegs.isEmpty()) listOf(TextSegment(text = text)) else styleSegs.toList())

    private fun renderPdf(blocks: List<DataBlock>): ByteArray {
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
        val renderer = PdfBlockRenderer()
        val embedder = HtmlMediaEmbedder(context)
        var y = 50f
        for (b in blocks) {
            y += renderer.drawBlock(page.canvas, b, embedder, 50f, y, 495)
        }
        doc.finishPage(page)
        val out = ByteArrayOutputStream()
        doc.writeTo(out)
        doc.close()
        return out.toByteArray()
    }

    private fun assertValidPdf(bytes: ByteArray) {
        assertTrue(bytes.size > 8)
        assertEquals('%'.code.toByte(), bytes[0])
        assertEquals('P'.code.toByte(), bytes[1])
        assertEquals('D'.code.toByte(), bytes[2])
        assertEquals('F'.code.toByte(), bytes[3])
    }

    private fun createPng(): File {
        val bitmap = Bitmap.createBitmap(300, 400, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.BLUE)
        val file = File(context.cacheDir, "pdf_test_${System.nanoTime()}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return file
    }

    @Test
    fun `rich note renders to a valid pdf`() {
        val blocks = listOf(
            textBlock("Título", TextSegment(text = "Título", bold = true)),
            textBlock("Texto con estilo",
                TextSegment(text = "negrita ", bold = true),
                TextSegment(text = "cursiva ", italic = true),
                TextSegment(text = "subrayado ", underline = true),
                TextSegment(text = "color", colorHex = "#E65100")
            ),
            block(BlockType.HEADING1, segments = listOf(TextSegment(text = "Encabezado"))),
            block(BlockType.BULLET_LIST, segments = listOf(TextSegment(text = "item"))),
            block(BlockType.CODE_BLOCK, segments = listOf(TextSegment(text = "val x = 1"))),
            block(BlockType.QUOTE, segments = listOf(TextSegment(text = "cita"))),
            block(BlockType.CHECKLIST_ITEM, meta = mapOf("checked" to "true"), segments = listOf(TextSegment(text = "hecho")))
        )

        assertValidPdf(renderPdf(blocks))
    }

    @Test
    fun `table block renders`() {
        val data = TableData(
            headers = listOf("Col A", "Col B"),
            rows = listOf(listOf("a1", "b1"), listOf("a2", "b2"))
        )
        val block = block(BlockType.TABLE, meta = mapOf("table" to data.toJson()))

        assertTrue(renderPdf(listOf(block)).size > 8)
    }

    @Test
    fun `local image is embedded and measured with its real size`() {
        val png = createPng()
        val block = block(BlockType.IMAGE, content = png.absolutePath)

        val height = PdfBlockRenderer().measureBlock(block, HtmlMediaEmbedder(context), 495)

        assertTrue("expected embedded image height, got $height", height > 200f)
        png.delete()
    }

    @Test
    fun `wysiwyg drawing is rasterized and embedded`() {
        val strokes = listOf(
            DrawingStroke(
                points = listOf(Offset(0.1f, 0.1f), Offset(0.9f, 0.9f)),
                color = androidx.compose.ui.graphics.Color.Black,
                width = 8f
            )
        )
        val block = block(
            BlockType.DRAWING,
            content = DrawingStrokeCodec.strokesToJson(strokes),
            meta = mapOf("wysiwyg" to "true")
        )

        val height = PdfBlockRenderer().measureBlock(block, HtmlMediaEmbedder(context), 495)

        assertTrue("expected drawing height, got $height", height > 300f)
    }

    @Test
    fun `web image url renders as label instead of crashing`() {
        val block = block(BlockType.IMAGE, content = "https://example.com/foto.png")

        val bytes = renderPdf(listOf(block))

        assertValidPdf(bytes)
    }

    @Test
    fun `non embeddable attachments are omitted`() {
        val embedder = HtmlMediaEmbedder(context)
        val renderer = PdfBlockRenderer()
        val video = block(BlockType.VIDEO, content = "/data/v.mp4")
        val audio = block(BlockType.AUDIO, content = "/data/a.m4a")
        val voice = block(BlockType.VOICE, content = "/data/v.3gp")
        val file = block(BlockType.FILE, content = "/data/doc.pdf", meta = mapOf("name" to "doc.pdf"))

        assertEquals(0f, renderer.measureBlock(video, embedder, 495))
        assertEquals(0f, renderer.measureBlock(audio, embedder, 495))
        assertEquals(0f, renderer.measureBlock(voice, embedder, 495))
        assertEquals(0f, renderer.measureBlock(file, embedder, 495))
    }

    @Test
    fun `legacy markup migrates to blocks and renders`() {
        val raw = "<item checked=\"true\">hecho</item>\n<hr/>\n" +
            "foto: <img src=\"/storage/f.png\" />"

        val blocks = PdfBlockRenderer.blocksFor(raw)

        assertTrue(blocks.isNotEmpty())
        assertTrue(blocks.any { it.type == BlockType.CHECKLIST_ITEM })
        assertTrue(blocks.any { it.type == BlockType.HORIZONTAL_RULE })
        assertValidPdf(renderPdf(blocks))
    }

    @Test
    fun `long note splits across pages`() {
        val blocks = List(60) { textBlock("Línea número $it") }
        val doc = PdfDocument()
        var pageNumber = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
        var canvas: Canvas = page.canvas
        val renderer = PdfBlockRenderer()
        val embedder = HtmlMediaEmbedder(context)
        var y = 50f
        for (b in blocks) {
            val needed = renderer.measureBlock(b, embedder, 495)
            if (needed > 0f && y + needed > 792f) {
                doc.finishPage(page)
                pageNumber++
                page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
                canvas = page.canvas
                y = 50f
            }
            y += renderer.drawBlock(canvas, b, embedder, 50f, y, 495)
        }
        doc.finishPage(page)
        doc.close()

        assertTrue("expected multiple pages, got $pageNumber", pageNumber > 1)
    }

    @Test
    fun `exporter writes a shareable pdf file for single note`() {
        val blocks = listOf(
            textBlock("Hola", TextSegment(text = "Hola", bold = true)),
            block(BlockType.NUMBERED_LIST, segments = listOf(TextSegment(text = "uno"))),
            block(BlockType.NUMBERED_LIST, segments = listOf(TextSegment(text = "dos")))
        )
        val note = Note(
            id = 7,
            title = "Nota PDF",
            content = DataBlock.serialize(blocks),
            lastModified = 1_600_000_000_000L,
            tagsJson = """["tag1","tag2"]"""
        )
        val decrypted = DecryptedNote(note, "Nota PDF", DataBlock.serialize(blocks), true)

        PdfExporter().export(context, listOf(decrypted))

        val file = File(context.cacheDir, "Note_7_Nota_PDF.pdf")
        assertTrue(file.exists())
        assertValidPdf(file.readBytes())
        file.delete()
    }
}
