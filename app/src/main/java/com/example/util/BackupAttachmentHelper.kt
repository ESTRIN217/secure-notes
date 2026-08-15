package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.model.BlockType
import com.example.data.model.DataBlock
import com.example.data.model.parseNoteContentAndAttachments
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupAttachmentHelper {
    private const val TAG = "BackupAttachmentHelper"
    private const val ATTACHMENTS_DIR = "attachments"
    private const val BACKUP_JSON_ENTRY = "backup.json"
    private const val ZIP_MAGIC = "PK"

    /**
     * Scan note content for URIs/image paths and copy all referenced files
     * into the temp directory. Returns a mapping of old path → new relative path.
     */
    fun collectAndCopyAttachments(
        content: String,
        backgroundImagePath: String?,
        context: Context,
        tempAttachmentsDir: File,
        warnings: MutableList<String>? = null
    ): Map<String, String> {
        val pathMap = mutableMapOf<String, String>()
        if (!tempAttachmentsDir.exists()) tempAttachmentsDir.mkdirs()

        // Scan for <img src="...">, <video src="...">, <audio src="...">
        val uriPattern = Regex("""<(?:img|video|audio)\s+src="([^"]+)"\s*/?>""")
        uriPattern.findAll(content).forEach { match ->
            val uriStr = match.groupValues[1]
            if (uriStr.isNotEmpty() && !pathMap.containsKey(uriStr)) {
                copyToAttachmentDir(uriStr, context, tempAttachmentsDir, warnings)?.let { relPath ->
                    pathMap[uriStr] = relPath
                }
            }
        }

        // Scan WYSIWYG block JSON for media file paths. IMAGE/VIDEO/AUDIO/FILE/VOICE
        // store their file path in `content`; legacy DRAWING blocks store a strokes JSON
        // path. Embedded strokes of WYSIWYG drawings are self-contained and skipped.
        for (p in collectBlockMediaPaths(content)) {
            if (p.isNotEmpty() && !pathMap.containsKey(p)) {
                copyToAttachmentDir(p, context, tempAttachmentsDir, warnings)?.let { relPath ->
                    pathMap[p] = relPath
                }
            }
        }

        // Scan for ---Attachments--- section paths
        // Also copy the "name" field (PNG preview for drawings, display file for others)
        val delimiter = "\n\n---Attachments---\n"
        if (content.contains(delimiter)) {
            try {
                val parts = content.split(delimiter, limit = 2)
                val jsonStr = parts.getOrNull(1) ?: "[]"
                val arr = org.json.JSONArray(jsonStr)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val path = obj.optString("path", "")
                    if (path.isNotEmpty() && !pathMap.containsKey(path)) {
                        copyToAttachmentDir(path, context, tempAttachmentsDir, warnings)?.let { relPath ->
                            pathMap[path] = relPath
                        }
                    }
                    val name = obj.optString("name", "")
                    if (name.isNotEmpty() && name != path && !pathMap.containsKey(name)) {
                        copyToAttachmentDir(name, context, tempAttachmentsDir, warnings)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing attachments section", e)
            }
        }

        // Background image
        if (!backgroundImagePath.isNullOrEmpty() && !pathMap.containsKey(backgroundImagePath)) {
            copyToAttachmentDir(backgroundImagePath, context, tempAttachmentsDir, warnings)?.let { relPath ->
                pathMap[backgroundImagePath] = relPath
            }
        }

        return pathMap
    }

    /**
     * Extracts local media file paths referenced by WYSIWYG block JSON in [content].
     * Non-local entries (http/https URLs) and self-contained WYSIWYG drawing strokes
     * are skipped. Returns an empty list when [content] is not block JSON.
     */
    internal fun collectBlockMediaPaths(content: String): List<String> {
        val textPart = parseNoteContentAndAttachments(content).first
        val blocks = DataBlock.deserialize(textPart) ?: return emptyList()
        val paths = mutableListOf<String>()
        for (block in blocks) {
            val candidate = when (block.type) {
                BlockType.IMAGE, BlockType.VIDEO, BlockType.AUDIO, BlockType.FILE, BlockType.VOICE ->
                    block.content.takeIf { it.isNotEmpty() && !it.startsWith("http") }
                BlockType.DRAWING -> block.content.takeIf { block.isLegacyDrawing && it.isNotEmpty() }
                else -> null
            }
            candidate?.let { if (it !in paths) paths.add(it) }
            block.meta["previewPath"]
                ?.takeIf { it.isNotEmpty() && it != candidate && it !in paths }
                ?.let { paths.add(it) }
        }
        return paths
    }

    private fun copyToAttachmentDir(
        uriStr: String,
        context: Context,
        tempAttachmentsDir: File,
        warnings: MutableList<String>? = null
    ): String? {
        return try {
            val uri = Uri.parse(uriStr)
            val fileName = absPathToFileName(uriStr)
            val destFile = File(tempAttachmentsDir, fileName)

            if (uri.scheme == "content") {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: run {
                    warnings?.add("Could not read content URI: $uriStr")
                    return null
                }
            } else {
                // Absolute file path
                val srcFile = File(uriStr)
                if (srcFile.exists()) {
                    FileInputStream(srcFile).use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                } else {
                    Log.w(TAG, "File not found: $uriStr")
                    warnings?.add("File not found: $uriStr")
                    return null
                }
            }
            "$ATTACHMENTS_DIR/$fileName"
        } catch (e: Exception) {
            Log.w(TAG, "Failed to copy attachment: $uriStr", e)
            warnings?.add("Failed to copy: $uriStr (${e.localizedMessage})")
            null
        }
    }

    private fun absPathToFileName(path: String): String {
        val fileName = File(path).name
        // Avoid name collisions by prefixing with a hash
        val hash = path.hashCode().toLong().let { if (it < 0) -it else it }
        return "${hash}_$fileName"
    }

    /**
     * Rewrite content URIs/paths in a note content to use relative attachment paths.
     */
    fun rewriteContentPaths(content: String, pathMap: Map<String, String>): String {
        var result = content
        for ((oldPath, newPath) in pathMap) {
            result = result.replace(oldPath, newPath)
        }
        return result
    }

    /**
     * Build a ZIP file containing backup.json + attachments/
     */
    fun buildBackupZip(
        backupJson: String,
        pathMap: Map<String, String>,
        tempAttachmentsDir: File,
        outputZipFile: File
    ): Boolean {
        return try {
            ZipOutputStream(FileOutputStream(outputZipFile)).use { zos ->
                // Add backup.json
                zos.putNextEntry(ZipEntry(BACKUP_JSON_ENTRY))
                zos.write(backupJson.toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // Add attachments
                val seen = mutableSetOf<String>()
                for ((_, relPath) in pathMap) {
                    if (seen.contains(relPath)) continue
                    seen.add(relPath)
                    val file = File(tempAttachmentsDir, relPath.removePrefix("$ATTACHMENTS_DIR/"))
                    if (file.exists()) {
                        zos.putNextEntry(ZipEntry(relPath))
                        FileInputStream(file).use { input ->
                            input.copyTo(zos)
                        }
                        zos.closeEntry()
                    }
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "buildBackupZip failed", e)
            false
        }
    }

    /**
     * Extract a ZIP backup and return the parsed JSON.
     */
    fun extractBackupZip(
        zipBytes: ByteArray,
        outputDir: File,
        context: Context
    ): JSONObject? {
        return try {
            var backupJson: JSONObject? = null
            ZipInputStream(zipBytes.inputStream()).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val entryName = entry.name
                    if (entryName == BACKUP_JSON_ENTRY) {
                        val jsonStr = zis.readBytes().toString(Charsets.UTF_8)
                        backupJson = JSONObject(jsonStr)
                    } else if (entryName.startsWith("$ATTACHMENTS_DIR/")) {
                        val fileName = entryName.removePrefix("$ATTACHMENTS_DIR/")
                        val destFile = File(outputDir, fileName)
                        destFile.parentFile?.mkdirs()
                        FileOutputStream(destFile).use { output ->
                            zis.copyTo(output)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            backupJson
        } catch (e: Exception) {
            Log.e(TAG, "extractBackupZip failed", e)
            null
        }
    }

    /**
     * Rewrite relative attachment paths back to absolute app file paths.
     */
    fun rewriteRestoredPaths(
        content: String,
        restoreDir: File
    ): String {
        var result = content
        val attachmentPattern = Regex("""$ATTACHMENTS_DIR/(\d+_.+?)(?=["'\s])""")
        val matches = attachmentPattern.findAll(content).map { it.value }.toSet()
        for (match in matches) {
            val fileName = match.removePrefix("$ATTACHMENTS_DIR/")
            val restoredFile = File(restoreDir, fileName)
            if (restoredFile.exists()) {
                result = result.replace(match, restoredFile.absolutePath)
            }
        }
        return result
    }

    /**
     * Check if content is a ZIP (starts with PK magic bytes).
     */
    fun isZipContent(content: String): Boolean {
        return content.startsWith(ZIP_MAGIC)
    }

    /**
     * Check if content is a ZIP (from byte array).
     */
    fun isZipBytes(data: ByteArray): Boolean {
        return data.size > 2 && data[0] == 0x50.toByte() && data[1] == 0x4B.toByte()
    }

    /**
     * Copy all content:// URIs in a note's content to internal storage.
     * Returns the updated content with absolute file paths.
     */
    fun copyUrisToLocalStorage(
        content: String,
        noteId: Int,
        context: Context
    ): String {
        var result = content
        val uriPattern = Regex("""<(?:img|video|audio)\s+src="(content://[^"]+)"\s*/?>""")
        uriPattern.findAll(content).forEach { match ->
            val uriStr = match.groupValues[1]
            try {
                val uri = Uri.parse(uriStr)
                val extension = when {
                    uriStr.contains("images") -> ".jpg"
                    uriStr.contains("video") -> ".mp4"
                    else -> ".bin"
                }
                val destFile = File(context.filesDir, "attachment_${noteId}_${System.currentTimeMillis()}$extension")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                result = result.replace(uriStr, destFile.absolutePath)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to copy URI to local: $uriStr", e)
            }
        }
        return result
    }
}
