package com.example.util.export

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.BlockType
import com.example.data.model.DataBlock
import com.example.data.model.DecryptedNote
import com.example.data.model.cleanedTags
import com.example.util.Exporter
import com.example.util.RichTextConverter
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HtmlExporter : Exporter {
    override val formatKey = "HTML"

    private val webMediaTypes = setOf(
        BlockType.IMAGE, BlockType.VIDEO, BlockType.AUDIO, BlockType.VOICE, BlockType.FILE
    )

    override fun export(context: Context, notes: List<DecryptedNote>) {
        try {
            val html = buildHtml(context, notes, HtmlMediaEmbedder(context))
            writeAndShare(context, html, "Exported_Notes.html", "text/html", R.string.share_title_html)
        } catch (e: Exception) {
            Log.e("HtmlExporter", "Error exporting notes to HTML", e)
            Toast.makeText(context, context.getString(R.string.toast_export_error, e.localizedMessage), Toast.LENGTH_LONG).show()
        }
    }

    /** Exporta HTML autocontenido descargando e incrustando media web como data URIs. */
    suspend fun exportAsync(context: Context, notes: List<DecryptedNote>) {
        try {
            val webMedia = coroutineScope {
                val sources = notes.flatMap { webMediaBlocks(it) }
                    .map { it to it.content }
                    .distinctBy { (_, src) -> src }
                sources.map { (block, src) ->
                    async { src to WebMediaDownloader.downloadToDataUri(src, block.type) }
                }.awaitAll()
                    .filterNotNull()
                    .mapNotNull { (src, dataUri) -> dataUri?.let { src to it } }
                    .toMap()
            }
            val html = buildHtml(context, notes, HtmlMediaEmbedder(context, webMedia))
            writeAndShare(context, html, "Exported_Notes.html", "text/html", R.string.share_title_html)
        } catch (e: Exception) {
            Log.e("HtmlExporter", "Error exporting notes to HTML", e)
            Toast.makeText(context, context.getString(R.string.toast_export_error, e.localizedMessage), Toast.LENGTH_LONG).show()
        }
    }

    private fun webMediaBlocks(dec: DecryptedNote): List<DataBlock> =
        RichTextConverter.contentToBlocks(dec.content)
            ?.filter { it.type in webMediaTypes && it.content.startsWith("http") }
            ?: emptyList()

    private fun buildHtml(context: Context, notes: List<DecryptedNote>, embedder: HtmlMediaEmbedder): String {
        val sb = StringBuilder()
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        sb.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\"><style>")
        sb.append("body { font-family: sans-serif; padding: 20px; line-height: 1.7; background-color: #f9f9f9; color: #333; }")
        sb.append(".note { border: 1px solid #ddd; background: #fff; padding: 20px; border-radius: 8px; margin-bottom: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.05); }")
        sb.append("h1 { margin-top: 0; color: #111; }")
        sb.append(".meta { color: #666; font-size: 0.9em; margin-bottom: 15px; }")
        sb.append(".content { line-height: 1.7; }")
        sb.append("img, video { max-width: 100%; height: auto; border-radius: 8px; display: block; margin: 8px auto; }")
        sb.append("audio { width: 100%; margin: 8px 0; }")
        sb.append("figure { margin: 12px 0; text-align: center; }")
        sb.append("figcaption { font-size: 0.85em; color: #666; margin-top: 4px; }")
        sb.append(".float-left { float: left; margin-right: 12px; } .float-right { float: right; margin-left: 12px; }")
        sb.append("table { border-collapse: collapse; width: 100%; margin: 12px 0; }")
        sb.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; } th { background: #f2f2f2; }")
        sb.append("blockquote { border-left: 4px solid #ddd; margin: 8px 0; padding: 4px 16px; color: #555; background: #fafafa; }")
        sb.append("pre { background: #f4f4f4; padding: 12px; border-radius: 6px; overflow-x: auto; }")
        sb.append("code { font-family: monospace; }")
        sb.append(".checklist { list-style: none; padding-left: 0; }")
        sb.append(".checklist li::before { content: \"☐ \"; }")
        sb.append(".checklist li[data-checked=\"checked\"]::before { content: \"☑ \"; }")
        sb.append(".callout { background: #e8f4fd; border-left: 4px solid #2196F3; padding: 8px 12px; border-radius: 4px; }")
        sb.append("a { color: #1565c0; } .file-link a { font-weight: 600; } .video-link a { font-weight: 600; }")
        sb.append("details summary { cursor: pointer; font-weight: 600; }")
        sb.append(".page-link { color: #1565c0; font-weight: 600; }")
        sb.append("</style></head><body>")

        for (dec in notes) {
            val dateStr = format.format(Date(dec.note.lastModified))
            sb.append("<div class=\"note\">")
            sb.append("<h1>").append(esc(dec.title)).append("</h1>")
            sb.append("<div class=\"meta\">").append(esc(context.getString(R.string.export_label_modified, dateStr)))
            val tags = dec.note.cleanedTags()
            if (tags.isNotEmpty()) {
                sb.append(" | ").append(esc(context.getString(R.string.export_label_tags, tags.joinToString(", "))))
            }
            sb.append("</div>")
            sb.append("<div class=\"content\">").append(RichTextConverter.contentToHtml(dec.content, embedder)).append("</div>")
            sb.append("</div>")
        }
        sb.append("</body></html>")
        return sb.toString()
    }

    private fun writeAndShare(context: Context, html: String, fileName: String, mimeType: String, titleRes: Int) {
        val file = File(context.cacheDir, fileName)
        FileOutputStream(file).use { out -> out.write(html.toByteArray()) }
        shareFile(context, file, mimeType, context.getString(titleRes))
    }

    private fun esc(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    private fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val authority = "${context.packageName}.fileprovider"
        val uri: Uri = FileProvider.getUriForFile(context, authority, file)
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, file.name)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, title).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    companion object {
        val instance = HtmlExporter()
    }
}
