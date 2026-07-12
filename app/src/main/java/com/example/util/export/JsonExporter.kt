package com.example.util.export

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.DecryptedNote
import com.example.data.model.cleanedTags
import com.example.util.Exporter
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class JsonExporter : Exporter {
    override val formatKey = "JSON"

    override fun export(context: Context, notes: List<DecryptedNote>) {
        try {
            val arr = JSONArray()
            for (dec in notes) {
                val obj = JSONObject()
                obj.put("id", dec.note.id)
                obj.put("title", dec.title)
                obj.put("summary", dec.content)
                obj.put("lastModified", dec.note.lastModified)
                obj.put("isEncrypted", dec.note.isEncrypted)
                obj.put("backgroundColor", dec.note.backgroundColor)
                obj.put("backgroundImagePath", dec.note.backgroundImagePath)
                obj.put("isArchived", dec.note.isArchived)
                obj.put("isFavorite", dec.note.isFavorite)
                obj.put("categoryId", dec.note.categoryId)
                obj.put("isPinned", dec.note.isPinned)
                val tags = dec.note.cleanedTags()
                val tagsArr = JSONArray()
                for (tag in tags) tagsArr.put(tag)
                obj.put("tags", tagsArr)
                arr.put(obj)
            }

            val fileName = if (notes.size == 1) {
                "Note_${notes[0].note.id}_" + notes[0].title.replace("[^a-zA-Z0-9]".toRegex(), "_") + ".json"
            } else {
                "Exported_Notes.json"
            }
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { out -> out.write(arr.toString(4).toByteArray()) }
            shareFile(context, file, "application/json", context.getString(R.string.share_title_json))
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
        val instance = JsonExporter()
    }
}
