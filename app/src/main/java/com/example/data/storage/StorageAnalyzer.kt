package com.example.data.storage

import android.content.Context
import android.os.StatFs
import android.util.Log
import com.example.data.local.NoteDatabase
import com.example.data.model.Note
import com.example.util.BackupAttachmentHelper
import org.json.JSONArray
import java.io.File

object StorageAnalyzer {
    private const val TAG = "StorageAnalyzer"

    suspend fun scan(context: Context, database: NoteDatabase): Pair<StorageOverview, List<StorageItem>> {
        val filesDir = context.filesDir
        val cacheDir = context.cacheDir
        val dbPath = context.getDatabasePath("secure_notes_database")

        val allItems = mutableListOf<StorageItem>()
        val referencedPaths = collectReferencedPaths(database)

        scanDirectory(filesDir, StorageCategory.OTHER, allItems, referencedPaths, isCache = false)
        scanDirectory(cacheDir, StorageCategory.CACHE, allItems, referencedPaths, isCache = true)

        if (dbPath.exists()) {
            allItems.add(
                StorageItem(
                    path = dbPath.absolutePath,
                    name = dbPath.name,
                    size = dbPath.length(),
                    lastModified = dbPath.lastModified(),
                    category = StorageCategory.DATABASE
                )
            )
        }

        val overview = computeOverview(allItems, context.filesDir.absolutePath)
        return Pair(overview, allItems)
    }

    private suspend fun collectReferencedPaths(database: NoteDatabase): Set<String> {
        val referenced = mutableSetOf<String>()
        try {
            val notes = database.noteDao.getAllNotes()
            for (note in notes) {
                if (!note.backgroundImagePath.isNullOrEmpty()) {
                    referenced.add(note.backgroundImagePath)
                }
                if (!note.isEncrypted) {
                    referenced.addAll(extractPathsFromContent(note.content))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to collect referenced paths", e)
        }
        return referenced
    }

    private fun extractPathsFromContent(content: String): List<String> {
        val paths = mutableListOf<String>()

        val mediaRegex = Regex("""<(?:img|video|audio)\s+src="([^"]+)"\s*/?>""")
        for (match in mediaRegex.findAll(content)) {
            paths.add(match.groupValues[1])
        }

        paths.addAll(BackupAttachmentHelper.collectBlockMediaPaths(content))

        val delimiter = "\n\n---Attachments---\n"
        if (content.contains(delimiter)) {
            try {
                val parts = content.split(delimiter, limit = 2)
                val jsonStr = parts.getOrNull(1) ?: "[]"
                val arr = JSONArray(jsonStr)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val path = obj.optString("path", "")
                    if (path.isNotEmpty()) paths.add(path)
                    val name = obj.optString("name", "")
                    if (name.isNotEmpty() && name != path) paths.add(name)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing attachments JSON", e)
            }
        }

        return paths
    }

    private fun scanDirectory(
        dir: File,
        defaultCategory: StorageCategory,
        items: MutableList<StorageItem>,
        referencedPaths: Set<String>,
        isCache: Boolean
    ) {
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                val category = when {
                    file.name.startsWith("backup_attachments_") -> StorageCategory.TEMP
                    file.name.startsWith("restore_") -> StorageCategory.TEMP
                    file.name == "tmp_attachments" -> StorageCategory.TEMP
                    file.name == "restored_attachments" -> StorageCategory.TEMP
                    file.name == "attachments" -> StorageCategory.ATTACHMENT
                    file.name == "models" -> StorageCategory.AI_MODEL
                    else -> StorageCategory.OTHER
                }
                scanDirectory(file, category, items, referencedPaths, isCache)
            } else {
                val category = classifyFile(file, defaultCategory, isCache)
                val isOrphan = !isCache &&
                        category in listOf(
                            StorageCategory.ATTACHMENT,
                            StorageCategory.AUDIO,
                            StorageCategory.FILE,
                            StorageCategory.VOICE,
                            StorageCategory.DRAWING,
                            StorageCategory.OTHER
                        ) &&
                        !isReferenced(file.absolutePath, referencedPaths)
                items.add(
                    StorageItem(
                        path = file.absolutePath,
                        name = file.name,
                        size = file.length(),
                        lastModified = file.lastModified(),
                        category = category,
                        isOrphan = isOrphan
                    )
                )
            }
        }
    }

    private fun classifyFile(file: File, defaultCategory: StorageCategory, isCache: Boolean): StorageCategory {
        val name = file.name
        return when {
            isCache && (name.startsWith("Exported_Notes") || name.startsWith("Note_")) -> StorageCategory.EXPORT
            isCache && name == "secure_notes_backup.json" -> StorageCategory.CACHE
            isCache -> StorageCategory.CACHE
            name.startsWith("attachment_") -> StorageCategory.ATTACHMENT
            name.startsWith("audio_") -> StorageCategory.AUDIO
            name.startsWith("drawing_") -> StorageCategory.DRAWING
            name.startsWith("voice_") -> StorageCategory.VOICE
            name.startsWith("file_") -> StorageCategory.FILE
            else -> defaultCategory
        }
    }

    private fun isReferenced(absolutePath: String, referencedPaths: Set<String>): Boolean {
        return referencedPaths.contains(absolutePath)
    }

    private fun computeOverview(items: List<StorageItem>, filesDirPath: String): StorageOverview {
        var attachmentsSize = 0L
        var audioSize = 0L
        var cacheSize = 0L
        var exportsSize = 0L
        var tempSize = 0L
        var databaseSize = 0L
        var drawingsSize = 0L
        var voiceSize = 0L
        var filesSize = 0L
        var otherSize = 0L
        var aiModelSize = 0L
        var orphanSize = 0L
        var orphanCount = 0

        for (item in items) {
            val size = item.size
            when (item.category) {
                StorageCategory.ATTACHMENT -> attachmentsSize += size
                StorageCategory.AUDIO -> audioSize += size
                StorageCategory.CACHE -> cacheSize += size
                StorageCategory.EXPORT -> exportsSize += size
                StorageCategory.TEMP -> tempSize += size
                StorageCategory.DATABASE -> databaseSize += size
                StorageCategory.DRAWING -> drawingsSize += size
                StorageCategory.VOICE -> voiceSize += size
                StorageCategory.FILE -> filesSize += size
                StorageCategory.AI_MODEL -> aiModelSize += size
                StorageCategory.OTHER -> otherSize += size
            }
            if (item.isOrphan) {
                orphanSize += size
                orphanCount++
            }
        }

        val totalUsed = items.sumOf { it.size }
        val freeSpace = try {
            val stat = StatFs(filesDirPath)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (e: Exception) {
            -1L
        }
        val totalSpace = try {
            val stat = StatFs(filesDirPath)
            stat.blockCountLong * stat.blockSizeLong
        } catch (e: Exception) {
            -1L
        }

        return StorageOverview(
            attachmentsSize = attachmentsSize,
            audioSize = audioSize,
            cacheSize = cacheSize,
            exportsSize = exportsSize,
            tempSize = tempSize,
            databaseSize = databaseSize,
            drawingsSize = drawingsSize,
            voiceSize = voiceSize,
            filesSize = filesSize,
            aiModelSize = aiModelSize,
            otherSize = otherSize,
            orphanSize = orphanSize,
            totalUsed = totalUsed,
            freeSpace = freeSpace,
            totalSpace = totalSpace,
            orphanCount = orphanCount
        )
    }

    fun clearCache(context: Context): Long {
        val cacheDir = context.cacheDir
        return deleteDirectoryContents(cacheDir)
    }

    /** Audios (`voice_*`, `audio_*`) presentes en disco, con su estado de adjunto. */
    suspend fun findAudioFiles(context: Context, database: NoteDatabase): List<AudioFileInfo> {
        val notes = database.noteDao.getAllNotes()
        val existingNoteIds = notes.mapTo(mutableSetOf()) { it.id }
        val noteById = notes.associateBy { it.id }
        val attachedPaths = mutableMapOf<String, Note>()

        for (note in notes) {
            if (note.isEncrypted) continue
            for (path in extractPathsFromContent(note.content)) {
                attachedPaths.putIfAbsent(path, note)
            }
        }

        val audioFiles = mutableListOf<File>()
        collectAudioFiles(context.filesDir, audioFiles)

        return audioFiles.map { file ->
            val path = file.absolutePath
            val note = attachedPaths[path]
            var isAttached = note != null
            var noteId = note?.id
            val noteTitle = note?.title?.takeIf { it.isNotBlank() }

            if (!isAttached) {
                // Notas cifradas no dejan leer su contenido: si el id del nombre
                // coincide con una nota existente, se protege por defecto.
                val idFromName = parseNoteIdFromName(file.name)
                if (idFromName != null && idFromName in existingNoteIds) {
                    val candidate = noteById[idFromName]
                    if (candidate != null && candidate.isEncrypted) {
                        isAttached = true
                        noteId = candidate.id
                    }
                }
            }

            AudioFileInfo(
                item = StorageItem(
                    path = path,
                    name = file.name,
                    size = file.length(),
                    lastModified = file.lastModified(),
                    category = classifyFile(file, StorageCategory.OTHER, false)
                ),
                isAttached = isAttached,
                noteId = noteId,
                noteTitle = noteTitle
            )
        }
    }

    private fun collectAudioFiles(dir: File, out: MutableList<File>) {
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                collectAudioFiles(file, out)
            } else if (file.name.startsWith("voice_") || file.name.startsWith("audio_")) {
                out.add(file)
            }
        }
    }

    private fun parseNoteIdFromName(name: String): Int? {
        return Regex("""^(?:voice|audio)_(\d+)_""").find(name)?.groupValues?.get(1)?.toIntOrNull()
    }

    fun deleteFiles(files: List<StorageItem>): Int {
        var deleted = 0
        for (file in files) {
            try {
                if (File(file.path).delete()) deleted++
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete ${file.path}", e)
            }
        }
        return deleted
    }

    fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    fun formatPercentage(part: Long, total: Long): String {
        if (total <= 0) return "0%"
        return String.format("%d%%", (part * 100 / total))
    }

    private fun deleteDirectoryContents(dir: File): Long {
        var freed = 0L
        val files = dir.listFiles() ?: return 0L
        for (file in files) {
            try {
                if (file.isDirectory) {
                    freed += deleteDirectoryContents(file)
                    file.delete()
                } else {
                    freed += file.length()
                    file.delete()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete ${file.path}", e)
            }
        }
        return freed
    }
}
