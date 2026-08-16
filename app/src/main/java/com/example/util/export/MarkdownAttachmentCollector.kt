package com.example.util.export

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.data.model.BlockType
import com.example.data.model.DataBlock
import com.example.util.RichTextConverter
import java.io.File

/**
 * Resuelve bloques de media a Markdown: imágenes/dibujos se incrustan inline como data URI y
 * los adjuntos no embebibles (video/audio/voz/archivo) se copian a `media/` para el ZIP.
 * Las URLs web se pasan tal cual como enlace.
 */
class MarkdownAttachmentCollector(
    context: Context
) : RichTextConverter.MediaMarkdownResolver {

    /** Prefijo opcional para desambiguar adjuntos entre varias notas (p. ej. `0_`). */
    var prefix: String = ""

    private val embedder = HtmlMediaEmbedder(context)
    private val mediaDir: File = File(context.cacheDir, "markdown_export_media")
        .apply { deleteRecursively(); mkdirs() }
    private val usedNames = mutableSetOf<String>()

    val mediaFiles: MutableList<File> = mutableListOf()

    override fun resolveMedia(block: DataBlock): String? {
        val src = block.content
        if (src.isBlank()) return null
        if (src.startsWith("http://") || src.startsWith("https://")) return src
        return when (block.type) {
            BlockType.IMAGE, BlockType.DRAWING -> embedAsDataUri(block)
            BlockType.VIDEO, BlockType.AUDIO, BlockType.VOICE, BlockType.FILE -> copyToMedia(block)
            else -> null
        }
    }

    private fun embedAsDataUri(block: DataBlock): String? {
        val bytes = embedder.resolveBytes(block) ?: return null
        val mime = if (block.type == BlockType.DRAWING) {
            "image/png"
        } else {
            embedder.mimeFor(block.content, block.type)
        }
        return "data:$mime;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun copyToMedia(block: DataBlock): String? {
        val bytes = embedder.resolveBytes(block) ?: return null
        val name = uniqueName(displayNameFor(block))
        val target = File(mediaDir, name)
        try {
            target.writeBytes(bytes)
        } catch (e: Exception) {
            Log.e("MarkdownAttachmentCollector", "Error copying media to export folder: ${block.content}", e)
            return null
        }
        mediaFiles.add(target)
        return "media/$name"
    }

    private fun displayNameFor(block: DataBlock): String {
        block.meta["name"]?.takeIf { it.isNotBlank() }?.let { return it }
        val base = Uri.parse(block.content).lastPathSegment
            ?.substringBefore('?')
            ?.takeIf { it.isNotBlank() }
            ?: mediaLabel(block.type)
        return if (base.substringAfterLast('.', "").isBlank()) base + extensionFor(block.type) else base
    }

    private fun extensionFor(type: BlockType): String = when (type) {
        BlockType.VIDEO -> ".mp4"
        BlockType.AUDIO -> ".m4a"
        BlockType.VOICE -> ".3gp"
        BlockType.FILE -> ".bin"
        else -> ""
    }

    private fun uniqueName(base: String): String {
        val sanitized = prefix + base.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        var candidate = sanitized
        var i = 1
        while (candidate in usedNames) {
            val dot = sanitized.lastIndexOf('.')
            val stem = if (dot > 0) sanitized.substring(0, dot) else sanitized
            val ext = if (dot > 0) sanitized.substring(dot) else ""
            candidate = "${stem}_$i$ext"
            i++
        }
        usedNames.add(candidate)
        return candidate
    }

    private fun mediaLabel(type: BlockType): String = when (type) {
        BlockType.VIDEO -> "video"
        BlockType.AUDIO -> "audio"
        BlockType.VOICE -> "voice"
        BlockType.FILE -> "file"
        BlockType.IMAGE -> "image"
        BlockType.DRAWING -> "drawing"
        else -> "media"
    }
}
