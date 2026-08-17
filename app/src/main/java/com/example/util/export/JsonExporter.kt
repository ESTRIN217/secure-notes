package com.example.util.export

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.Attachment
import com.example.data.model.BlockType
import com.example.data.model.DataBlock
import com.example.data.model.DecryptedNote
import com.example.data.model.parseNoteContentAndAttachments
import com.example.util.BackupAttachmentHelper
import com.example.util.Exporter
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class JsonExporter : Exporter {
    override val formatKey = "JSON"

    override fun export(context: Context, notes: List<DecryptedNote>) {
        try {
            val tempDir = File(context.cacheDir, "json_export_temp").apply { mkdirs() }
            val attachmentsDir = File(tempDir, "attachments").apply { mkdirs() }
            val pathMap = mutableMapOf<String, String>()

            val notesArr = JSONArray()
            for (dec in notes) {
                val noteJson = buildNoteJson(dec, context, attachmentsDir, pathMap)
                notesArr.put(noteJson)
            }

            val root = JSONObject()
            root.put("format", "secure-notes-export")
            root.put("version", 2)
            root.put("exportedAt", System.currentTimeMillis())
            root.put("notes", notesArr)

            val jsonString = root.toString(4)
            File(tempDir, "notes.json").writeText(jsonString)

            val zipName = if (notes.size == 1) {
                "Note_${notes[0].note.id}_" +
                        notes[0].title.replace("[^a-zA-Z0-9]".toRegex(), "_") + ".zip"
            } else {
                "Exported_Notes.zip"
            }
            val zipFile = File(context.cacheDir, zipName)

            val success = BackupAttachmentHelper.buildBackupZip(
                backupJson = jsonString,
                pathMap = pathMap,
                tempAttachmentsDir = attachmentsDir,
                outputZipFile = zipFile
            )

            tempDir.deleteRecursively()

            if (!success || !zipFile.exists()) {
                Toast.makeText(context, context.getString(R.string.toast_export_error, "ZIP creation failed"), Toast.LENGTH_LONG).show()
                return
            }

            shareFile(context, zipFile, "application/zip", context.getString(R.string.share_title_json))
        } catch (e: Exception) {
            Log.e(TAG, "JSON export failed", e)
            Toast.makeText(context, context.getString(R.string.toast_export_error, e.localizedMessage), Toast.LENGTH_LONG).show()
        }
    }

    private fun buildNoteJson(
        dec: DecryptedNote,
        context: Context,
        attachmentsDir: File,
        pathMap: MutableMap<String, String>
    ): JSONObject {
        val (textPart, legacyAttachments) = parseNoteContentAndAttachments(dec.content)
        val blocks = DataBlock.deserialize(textPart)

        if (blocks != null) {
            collectAndCopyBlockAttachments(dec, blocks, context, attachmentsDir, pathMap)
        } else {
            collectAndCopyLegacyAttachments(dec, legacyAttachments, context, attachmentsDir, pathMap)
        }

        val tagsArr = JSONArray()
        for (tag in dec.tagsList) tagsArr.put(tag)

        val obj = JSONObject()
        obj.put("id", dec.note.id)
        obj.put("title", dec.title)
        obj.put("summary", dec.summary)
        obj.put("lastModified", dec.note.lastModified)
        obj.put("isEncrypted", dec.note.isEncrypted)
        obj.put("backgroundColor", dec.note.backgroundColor)
        obj.put("isArchived", dec.note.isArchived)
        obj.put("isFavorite", dec.note.isFavorite)
        obj.put("categoryId", dec.note.categoryId)
        obj.put("isPinned", dec.note.isPinned)
        obj.put("tags", tagsArr)

        if (blocks != null) {
            val blocksArr = JSONArray()
            val blocksJsonStr = DataBlock.serialize(blocks)
            val rewritten = BackupAttachmentHelper.rewriteContentPaths(blocksJsonStr, pathMap)
            val rewrittenBlocks = DataBlock.deserialize(rewritten) ?: blocks
            for (block in rewrittenBlocks) {
                blocksArr.put(block.toJson())
            }
            obj.put("contentBlocks", blocksArr)
            obj.put("rawContent", dec.content)
        } else {
            obj.put("rawContent", BackupAttachmentHelper.rewriteContentPaths(dec.content, pathMap))
        }

        if (!dec.note.backgroundImagePath.isNullOrEmpty() && pathMap.containsKey(dec.note.backgroundImagePath)) {
            obj.put("backgroundImagePath", pathMap[dec.note.backgroundImagePath])
        } else if (!dec.note.backgroundImagePath.isNullOrEmpty()) {
            obj.put("backgroundImagePath", dec.note.backgroundImagePath)
        }

        return obj
    }

    private fun collectAndCopyBlockAttachments(
        dec: DecryptedNote,
        blocks: List<DataBlock>,
        context: Context,
        attachmentsDir: File,
        pathMap: MutableMap<String, String>
    ) {
        val blockPaths = mutableListOf<String>()
        for (block in blocks) {
            val candidate = when (block.type) {
                BlockType.IMAGE,
                BlockType.VIDEO,
                BlockType.AUDIO,
                BlockType.FILE,
                BlockType.VOICE ->
                    block.content.takeIf { it.isNotEmpty() && !it.startsWith("http") }

                BlockType.DRAWING ->
                    block.content.takeIf { block.isLegacyDrawing && it.isNotEmpty() }

                else -> null
            }
            candidate?.let { if (it !in blockPaths) blockPaths.add(it) }

            block.meta["previewPath"]
                ?.takeIf { it.isNotEmpty() && it != candidate && it !in blockPaths }
                ?.let { blockPaths.add(it) }
        }

        for (path in blockPaths) {
            copyAttachment(path, context, attachmentsDir)?.let { relPath ->
                pathMap[path] = relPath
            }
        }

        dec.note.backgroundImagePath
            ?.takeIf { it.isNotEmpty() && !it.startsWith("http") && it !in pathMap }
            ?.let { path ->
                copyAttachment(path, context, attachmentsDir)?.let { relPath ->
                    pathMap[path] = relPath
                }
            }
    }

    private fun collectAndCopyLegacyAttachments(
        dec: DecryptedNote,
        legacyAttachments: List<Attachment>,
        context: Context,
        attachmentsDir: File,
        pathMap: MutableMap<String, String>
    ) {
        for (att in legacyAttachments) {
            if (att.path.isNotEmpty() && !pathMap.containsKey(att.path)) {
                copyAttachment(att.path, context, attachmentsDir)?.let { relPath ->
                    pathMap[att.path] = relPath
                }
            }
            if (att.name.isNotEmpty() && att.name != att.path && !pathMap.containsKey(att.name)) {
                copyAttachment(att.name, context, attachmentsDir)
            }
        }

        dec.note.backgroundImagePath
            ?.takeIf { it.isNotEmpty() && !it.startsWith("http") && it !in pathMap }
            ?.let { path ->
                copyAttachment(path, context, attachmentsDir)?.let { relPath ->
                    pathMap[path] = relPath
                }
            }
    }

    private fun copyAttachment(
        path: String,
        context: Context,
        attachmentsDir: File
    ): String? {
        return try {
            val fileName = File(path).name
            val hash = path.hashCode().toLong().let { if (it < 0) -it else it }
            val uniqueName = "${hash}_$fileName"
            val destFile = File(attachmentsDir, uniqueName)

            val uri = Uri.parse(path)
            if (uri.scheme == "content") {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output -> input.copyTo(output) }
                } ?: return null
            } else {
                val srcFile = File(path)
                if (!srcFile.exists()) return null
                FileInputStream(srcFile).use { input ->
                    FileOutputStream(destFile).use { output -> input.copyTo(output) }
                }
            }

            "attachments/$uniqueName"
        } catch (e: Exception) {
            Log.w(TAG, "Failed to copy attachment: $path", e)
            null
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
        private const val TAG = "JsonExporter"
        val instance = JsonExporter()
    }
}
