package com.example

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.BlockType
import com.example.data.model.DataBlock
import com.example.data.model.DrawingStroke
import com.example.data.model.DrawingStrokeCodec
import com.example.util.export.MarkdownAttachmentCollector
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
class MarkdownAttachmentCollectorTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `local image is copied to media folder and tracked`() {
        val file = File(context.cacheDir, "img_1.png").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
        val collector = MarkdownAttachmentCollector(context)

        val result = collector.resolveMedia(DataBlock(type = BlockType.IMAGE, content = file.absolutePath))

        assertEquals("media/img_1.png", result)
        assertEquals(1, collector.mediaFiles.size)
        assertTrue(collector.mediaFiles[0].exists())
    }

    @Test
    fun `local video is copied to media folder and tracked`() {
        val file = File(context.cacheDir, "vid_1.mp4").apply { writeBytes(byteArrayOf(9, 9, 9)) }
        val collector = MarkdownAttachmentCollector(context)

        val result = collector.resolveMedia(
            DataBlock(type = BlockType.VIDEO, content = file.absolutePath, meta = mapOf("name" to "video.mp4"))
        )

        assertEquals("media/video.mp4", result)
        assertEquals(1, collector.mediaFiles.size)
        assertTrue(collector.mediaFiles[0].exists())
    }

    @Test
    fun `name derived from source when meta lacks name`() {
        val file = File(context.cacheDir, "voice_7.3gp").apply { writeBytes(byteArrayOf(1)) }
        val collector = MarkdownAttachmentCollector(context)

        val result = collector.resolveMedia(DataBlock(type = BlockType.VOICE, content = file.absolutePath))

        assertEquals("media/voice_7.3gp", result)
    }

    @Test
    fun `duplicate names get unique suffix`() {
        val f1 = File(context.cacheDir, "a.mp4").apply { writeBytes(byteArrayOf(1)) }
        val f2 = File(context.cacheDir, "b.mp4").apply { writeBytes(byteArrayOf(2)) }
        val collector = MarkdownAttachmentCollector(context)

        val first = collector.resolveMedia(DataBlock(type = BlockType.VIDEO, content = f1.absolutePath, meta = mapOf("name" to "same.mp4")))
        val second = collector.resolveMedia(DataBlock(type = BlockType.VIDEO, content = f2.absolutePath, meta = mapOf("name" to "same.mp4")))

        assertEquals("media/same.mp4", first)
        assertEquals("media/same_1.mp4", second)
        assertEquals(2, collector.mediaFiles.size)
    }

    @Test
    fun `prefix disambiguates media across notes`() {
        val file = File(context.cacheDir, "clip.mp4").apply { writeBytes(byteArrayOf(1)) }
        val collector = MarkdownAttachmentCollector(context)
        collector.prefix = "2_"

        val result = collector.resolveMedia(DataBlock(type = BlockType.VIDEO, content = file.absolutePath, meta = mapOf("name" to "clip.mp4")))

        assertEquals("media/2_clip.mp4", result)
    }

    @Test
    fun `missing file resolves to null`() {
        val collector = MarkdownAttachmentCollector(context)

        assertNull(collector.resolveMedia(DataBlock(type = BlockType.IMAGE, content = "/no/such/file.png")))
    }

    @Test
    fun `web url passes through and is not copied`() {
        val collector = MarkdownAttachmentCollector(context)

        assertEquals(
            "https://example.com/x.png",
            collector.resolveMedia(DataBlock(type = BlockType.IMAGE, content = "https://example.com/x.png"))
        )
        assertTrue(collector.mediaFiles.isEmpty())
    }

    @Test
    fun `wysiwyg drawing is copied to media folder as png`() {
        val strokes = listOf(
            DrawingStroke(
                points = listOf(Offset(0.1f, 0.1f), Offset(0.9f, 0.9f)),
                color = Color.Black,
                width = 8f
            )
        )
        val collector = MarkdownAttachmentCollector(context)

        val result = collector.resolveMedia(
            DataBlock(
                type = BlockType.DRAWING,
                content = DrawingStrokeCodec.strokesToJson(strokes),
                meta = mapOf("wysiwyg" to "true")
            )
        )

        assertNotNull(result)
        assertTrue(result!!.startsWith("media/"))
        assertTrue(result.endsWith(".png"))
        assertEquals(1, collector.mediaFiles.size)
        assertTrue(collector.mediaFiles[0].exists())
    }
}
