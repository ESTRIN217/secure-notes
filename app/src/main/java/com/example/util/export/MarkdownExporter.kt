package com.example.util.export

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.DecryptedNote
import com.example.data.model.cleanedTags
import com.example.util.Exporter
import com.example.util.RichTextConverter
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class MarkdownExporter : Exporter {
    override val formatKey = "MD"

    override fun export(context: Context, notes: List<DecryptedNote>) {
        try {
            val collector = MarkdownAttachmentCollector(context)
            val sb = StringBuilder()
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            notes.forEachIndexed { index, dec ->
                collector.prefix = if (notes.size > 1) "${index}_" else ""
                val dateStr = format.format(Date(dec.note.lastModified))
                sb.append("# ").append(dec.title).append("\n\n")
                sb.append("**Last Modified:** ").append(dateStr).append("\n")
                val tags = dec.note.cleanedTags()
                if (tags.isNotEmpty()) {
                    sb.append("**Tags:** ").append(tags.joinToString(", ")).append("\n")
                }
                sb.append("\n---\n\n")
                sb.append(RichTextConverter.contentToMarkdown(dec.content, collector)).append("\n\n")
                if (index < notes.size - 1) {
                    sb.append("\n\n---\n\n")
                }
            }
            val mdName = if (notes.size == 1) {
                "Note_${notes[0].note.id}_" + notes[0].title.replace("[^a-zA-Z0-9]".toRegex(), "_") + ".md"
            } else {
                "Exported_Notes.md"
            }
            val mdFile = File(context.cacheDir, mdName)
            FileOutputStream(mdFile).use { out -> out.write(sb.toString().toByteArray()) }
            val zipFile = createZip(context, mdName.removeSuffix(".md") + ".zip", mdName, mdFile, collector.mediaFiles)
            shareFile(context, zipFile, "application/zip", context.getString(R.string.export_title_markdown))
        } catch (e: Exception) {
            Log.e("MarkdownExporter", "Error exporting notes to Markdown", e)
            Toast.makeText(context, context.getString(R.string.toast_export_error, e.localizedMessage), Toast.LENGTH_LONG).show()
        }
    }

    private fun createZip(
        context: Context,
        zipName: String,
        mdName: String,
        mdFile: File,
        mediaFiles: List<File>
    ): File {
        val zip = File(context.cacheDir, zipName)
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zip))).use { zos ->
            zos.putNextEntry(ZipEntry(mdName))
            zos.write(mdFile.readBytes())
            zos.closeEntry()
            mediaFiles.forEach { media ->
                zos.putNextEntry(ZipEntry("media/${media.name}"))
                zos.write(media.readBytes())
                zos.closeEntry()
            }
        }
        return zip
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
        val instance = MarkdownExporter()
    }
}
