package com.example

import android.content.Context
import android.util.Base64
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.BlockType
import com.example.data.model.DataBlock
import com.example.data.model.DrawingStroke
import com.example.data.model.DrawingStrokeCodec
import com.example.util.export.DrawingPngRenderer
import com.example.util.export.HtmlMediaEmbedder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class HtmlMediaEmbedderTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `image file path resolves to data uri`() {
        val file = File(context.cacheDir, "img_1.png").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
        val block = DataBlock(type = BlockType.IMAGE, content = file.absolutePath)

        val result = HtmlMediaEmbedder(context).resolveMedia(block)

        assertNotNull(result)
        assertTrue(result!!.startsWith("data:image/png;base64,"))
        assertEquals(byteArrayOf(1, 2, 3, 4).toList(), Base64.decode(result.substringAfter(","), Base64.DEFAULT).toList())
    }

    @Test
    fun `missing file resolves to null`() {
        val block = DataBlock(type = BlockType.IMAGE, content = "/no/such/file.png")

        assertNull(HtmlMediaEmbedder(context).resolveMedia(block))
    }

    @Test
    fun `web image url passes through when not downloaded`() {
        val block = DataBlock(type = BlockType.IMAGE, content = "https://example.com/x.png")

        assertEquals("https://example.com/x.png", HtmlMediaEmbedder(context).resolveMedia(block))
    }

    @Test
    fun `web media uses downloaded data uri from map`() {
        val block = DataBlock(type = BlockType.IMAGE, content = "https://example.com/x.png")
        val webMedia = mapOf("https://example.com/x.png" to "data:image/png;base64,AAAA")

        val result = HtmlMediaEmbedder(context, webMedia).resolveMedia(block)

        assertEquals("data:image/png;base64,AAAA", result)
    }

    @Test
    fun `youtube video url is not embedded`() {
        val block = DataBlock(type = BlockType.VIDEO, content = "https://youtu.be/abc123")

        assertNull(HtmlMediaEmbedder(context).resolveMedia(block))
    }

    @Test
    fun `large local file is embedded without size cap`() {
        val file = File(context.cacheDir, "big.mp4").apply {
            writeBytes(ByteArray(6 * 1024 * 1024 + 16) { 7 })
        }
        val block = DataBlock(type = BlockType.VIDEO, content = file.absolutePath)

        val result = HtmlMediaEmbedder(context).resolveMedia(block)

        assertNotNull(result)
        assertTrue(result!!.startsWith("data:video/mp4;base64,"))
        assertEquals(file.length(), Base64.decode(result.substringAfter(","), Base64.DEFAULT).size.toLong())
    }

    @Test
    fun `wysiwyg drawing renders to png data uri`() {
        val strokes = listOf(
            DrawingStroke(
                points = listOf(Offset(0.1f, 0.1f), Offset(0.9f, 0.9f)),
                color = Color.Black,
                width = 8f
            )
        )
        val block = DataBlock(
            type = BlockType.DRAWING,
            content = DrawingStrokeCodec.strokesToJson(strokes),
            meta = mapOf("wysiwyg" to "true")
        )

        val result = HtmlMediaEmbedder(context).resolveMedia(block)

        assertNotNull(result)
        assertTrue(result!!.startsWith("data:image/png;base64,"))
        val png = Base64.decode(result.substringAfter(","), Base64.DEFAULT)
        assertTrue(png.size > 8)
    }

    @Test
    fun `drawing png renderer produces valid png bytes`() {
        val strokes = listOf(
            DrawingStroke(points = listOf(Offset(0f, 0f), Offset(1f, 1f)), color = Color.Black, width = 8f)
        )

        val png = DrawingPngRenderer.render(strokes)

        assertNotNull(png)
        assertTrue(png!!.size > 8)
        assertEquals(0x89, png[0].toInt() and 0xFF)
        assertEquals('P'.code, png[1].toInt())
        assertEquals('N'.code, png[2].toInt())
        assertEquals('G'.code, png[3].toInt())
    }
}
