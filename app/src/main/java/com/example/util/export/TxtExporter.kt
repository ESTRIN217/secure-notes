package com.example.util.export

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.DecryptedNote
import com.example.data.model.cleanedTags
import com.example.util.Exporter
import com.example.util.RichTextConverter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TxtExporter : Exporter {
    override val formatKey = "TXT"

    override fun export(context: Context, notes: List<DecryptedNote>) {
        try {
            val sb = StringBuilder()
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            notes.forEachIndexed { index, dec ->
                val dateStr = format.format(Date(dec.note.lastModified))
                sb.append("=== ").append(dec.title.uppercase(Locale.getDefault())).append(" ===\n")
                sb.append(context.getString(R.string.export_label_modified, dateStr)).append("\n")
                val tags = dec.note.cleanedTags()
                if (tags.isNotEmpty()) {
                    sb.append(context.getString(R.string.export_label_tags, tags.joinToString(", "))).append("\n")
                }
                sb.append("\n")
                sb.append(RichTextConverter.contentToPlainText(dec.content)).append("\n\n")
                if (index < notes.size - 1) {
                    sb.append("----------------------------\n\n")
                }
            }
            val file = File(context.cacheDir, "Exported_Notes.txt")
            FileOutputStream(file).use { out -> out.write(sb.toString().toByteArray()) }
            shareFile(context, file, "text/plain", context.getString(R.string.share_title_text))
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
        val instance = TxtExporter()
    }
}
