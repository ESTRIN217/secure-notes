package com.example.util.export

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.webkit.MimeTypeMap
import com.example.data.model.BlockType
import com.example.data.model.DataBlock
import com.example.data.model.DrawingStrokeCodec
import com.example.util.RichTextConverter
import com.example.util.VideoUrlHelper
import java.io.File
import java.io.FileInputStream

/** Resuelve bloques de media a data URIs (o URLs passthrough) para embeber en HTML. */
class HtmlMediaEmbedder(
    private val context: Context,
    private val webMedia: Map<String, String> = emptyMap()
) : RichTextConverter.MediaHtmlResolver {

    override fun resolveMedia(block: DataBlock): String? {
        return when (block.type) {
            BlockType.DRAWING -> resolveDrawing(block)
            BlockType.IMAGE, BlockType.VIDEO, BlockType.AUDIO, BlockType.VOICE, BlockType.FILE ->
                resolveSource(block.content, block.type)
            else -> null
        }
    }

    private fun resolveDrawing(block: DataBlock): String? {
        return try {
            val png: ByteArray = when {
                block.isWysiwygDrawing -> {
                    DrawingPngRenderer.render(DrawingStrokeCodec.strokesFromJson(block.content)) ?: return null
                }
                else -> {
                    val preview = block.meta["previewPath"]?.takeIf { it.isNotBlank() } ?: return null
                    readBytes(preview) ?: return null
                }
            }
            "data:image/png;base64," + Base64.encodeToString(png, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e("HtmlMediaEmbedder", "Error embedding drawing", e)
            null
        }
    }

    private fun resolveSource(src: String, type: BlockType): String? {
        if (src.isBlank()) return null
        if (src.startsWith("http://") || src.startsWith("https://")) {
            if (type == BlockType.VIDEO && VideoUrlHelper.isYouTubeUrl(src)) return null
            return webMedia[src] ?: src
        }
        val bytes = readBytes(src) ?: return null
        val mime = mimeFor(src, type)
        return "data:$mime;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun readBytes(src: String): ByteArray? {
        return try {
            val uri = Uri.parse(src)
            if (uri.scheme == "content") {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } else {
                val file = File(src)
                if (file.exists() && file.isFile) FileInputStream(file).use { it.readBytes() } else null
            }
        } catch (e: Exception) {
            Log.e("HtmlMediaEmbedder", "Error reading media: $src", e)
            null
        }
    }

    private fun mimeFor(src: String, type: BlockType): String {
        val ext = src.substringAfterLast('.', "").substringBefore('?').lowercase()
        val fromExt = if (ext.isNotBlank()) MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) else null
        return fromExt ?: when (type) {
            BlockType.IMAGE -> "image/jpeg"
            BlockType.VIDEO -> "video/mp4"
            BlockType.AUDIO -> "audio/mpeg"
            BlockType.VOICE -> "audio/3gpp"
            BlockType.FILE -> "application/octet-stream"
            else -> "application/octet-stream"
        }
    }
}
