package com.example.util

import com.example.data.model.BlockType
import com.example.data.model.DataBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class BackupAttachmentBlockMediaTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `collectBlockMediaPaths includes wysiwyg block media paths and skips http`() {
        val img = File("/data/user/0/com.estrin217.securenotes/files/media/img_1_123.jpg")
        val vid = File("/data/user/0/com.estrin217.securenotes/files/media/vid_1_124.mp4")
        val audio = File("/data/user/0/com.estrin217.securenotes/files/media/audio_1_125.m4a")
        val drawing = File("/data/user/0/com.estrin217.securenotes/files/media/strokes_1_126.json")
        val preview = File("/data/user/0/com.estrin217.securenotes/files/media/preview_1_127.png")
        val embeddedStrokes = """[{"type":"pen","points":[[0,0],[1,1]]}]"""

        val blocks = listOf(
            DataBlock(type = BlockType.TEXT, content = "hello"),
            DataBlock(type = BlockType.IMAGE, content = img.absolutePath),
            DataBlock(type = BlockType.VIDEO, content = vid.absolutePath),
            DataBlock(type = BlockType.AUDIO, content = audio.absolutePath),
            DataBlock(type = BlockType.FILE, content = "/data/.../file_1_128.pdf"),
            DataBlock(
                type = BlockType.DRAWING,
                content = drawing.absolutePath,
                meta = mapOf("previewPath" to preview.absolutePath)
            ),
            DataBlock(
                type = BlockType.DRAWING,
                content = embeddedStrokes,
                meta = mapOf("wysiwyg" to "true")
            ),
            DataBlock(type = BlockType.IMAGE, content = "https://example.com/x.png")
        )
        val content = DataBlock.serialize(blocks)

        val paths = BackupAttachmentHelper.collectBlockMediaPaths(content)

        assertTrue(paths.contains(img.absolutePath))
        assertTrue(paths.contains(vid.absolutePath))
        assertTrue(paths.contains(audio.absolutePath))
        assertTrue(paths.contains(drawing.absolutePath))
        assertTrue(paths.contains(preview.absolutePath))
        assertEquals(5, paths.size)
        assertTrue(paths.none { it.startsWith("http") })
        assertFalse(paths.contains(embeddedStrokes))
    }

    @Test
    fun `collectBlockMediaPaths returns empty for non block json`() {
        assertEquals(emptyList<String>(), BackupAttachmentHelper.collectBlockMediaPaths("<b>legacy</b>"))
    }

    @Test
    fun `rewriteContentPaths and rewriteRestoredPaths roundtrip on block json`() {
        val img = File("/data/user/0/com.estrin217.securenotes/files/media/img_1_123.jpg")
        val blocks = listOf(DataBlock(type = BlockType.IMAGE, content = img.absolutePath))
        val content = DataBlock.serialize(blocks)
        val fileName = "${img.absolutePath.hashCode().toLong().let { if (it < 0) -it else it }}_${img.name}"
        val restoreDir = tempFolder.newFolder("restore")
        restoreDir.resolve(fileName).writeText("img")

        val pathMap = mapOf(img.absolutePath to "attachments/$fileName")
        val rewritten = BackupAttachmentHelper.rewriteContentPaths(content, pathMap)
        assertTrue(rewritten.contains("attachments/$fileName"))
        assertFalse(rewritten.contains(img.absolutePath))

        val restored = BackupAttachmentHelper.rewriteRestoredPaths(rewritten, restoreDir)
        assertTrue(restored.contains(img.absolutePath))
    }
}
