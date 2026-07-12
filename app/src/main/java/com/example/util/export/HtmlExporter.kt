package com.example.util.export

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.DecryptedNote
import com.example.data.model.cleanedTags
import com.example.util.Exporter
import com.example.util.RichTextParser
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HtmlExporter : Exporter {
    override val formatKey = "HTML"

    override fun export(context: Context, notes: List<DecryptedNote>) {
        try {
            val sb = StringBuilder()
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            sb.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\"><style>")
            sb.append("body { font-family: sans-serif; padding: 20px; line-height: 1.6; background-color: #f9f9f9; color: #333; }")
            sb.append(".note { border: 1px solid #ddd; background: #fff; padding: 20px; border-radius: 8px; margin-bottom: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.05); }")
            sb.append("h1 { margin-top: 0; color: #111; }")
            sb.append(".meta { color: #666; font-size: 0.9em; margin-bottom: 15px; }")
            sb.append(".content { white-space: pre-wrap; }")
            sb.append("</style></head><body>")

            for (dec in notes) {
                val dateStr = format.format(Date(dec.note.lastModified))
                sb.append("<div class=\"note\">")
                sb.append("<h1>").append(dec.title).append("</h1>")
                sb.append("<div class=\"meta\">Last Modified: ").append(dateStr)
                val tags = dec.note.cleanedTags()
                if (tags.isNotEmpty()) {
                    sb.append(" | Tags: ").append(tags.joinToString(", "))
                }
                sb.append("</div>")
                sb.append("<div class=\"content\">").append(RichTextParser.convertToHtml(dec.content)).append("</div>")
                sb.append("</div>")
            }
            sb.append("</body></html>")

            val file = File(context.cacheDir, "Exported_Notes.html")
            FileOutputStream(file).use { out -> out.write(sb.toString().toByteArray()) }
            shareFile(context, file, "text/html", context.getString(R.string.share_title_html))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, context.getString(R.string.toast_export_error, e.localizedMessage), Toast.LENGTH_LONG).show()
        }
    }

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
