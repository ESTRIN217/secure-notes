package com.example.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class ImportFileType {
    TXT, MARKDOWN, HTML, JSON, PDF, OTHER
}

object FileImporter {
    private val EXTENSION_MAP = mapOf(
        "txt" to ImportFileType.TXT,
        "md" to ImportFileType.MARKDOWN,
        "markdown" to ImportFileType.MARKDOWN,
        "mdown" to ImportFileType.MARKDOWN,
        "mkd" to ImportFileType.MARKDOWN,
        "html" to ImportFileType.HTML,
        "htm" to ImportFileType.HTML,
        "xhtml" to ImportFileType.HTML,
        "json" to ImportFileType.JSON,
        "pdf" to ImportFileType.PDF
    )

    const val MAX_TEXT_SIZE = 5 * 1024 * 1024

    fun detectFileType(displayName: String?, mimeType: String?): ImportFileType {
        val extension = displayName?.substringAfterLast('.', "")?.lowercase().orEmpty()
        EXTENSION_MAP[extension]?.let { return it }
        return when {
            mimeType == "application/xhtml+xml" -> ImportFileType.HTML
            mimeType?.startsWith("text/") == true -> when (mimeType) {
                "text/markdown", "text/x-markdown" -> ImportFileType.MARKDOWN
                "text/html" -> ImportFileType.HTML
                else -> ImportFileType.TXT
            }
            mimeType == "application/json" -> ImportFileType.JSON
            mimeType == "application/pdf" -> ImportFileType.PDF
            else -> ImportFileType.OTHER
        }
    }

    fun isTextLike(type: ImportFileType): Boolean = type != ImportFileType.PDF && type != ImportFileType.OTHER

    fun isImportable(type: ImportFileType): Boolean = type != ImportFileType.OTHER

    suspend fun queryDisplayName(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx)?.takeIf { it.isNotBlank() } else null
                }
        }.getOrNull() ?: uri.lastPathSegment
    }

    suspend fun readTextFromUri(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        runCatching {
            val size = runCatching {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
                    ?.use { cursor ->
                        val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (idx >= 0 && cursor.moveToFirst()) cursor.getLong(idx).takeIf { it > 0 } else null
                    }
            }.getOrNull()
            if (size != null && size > MAX_TEXT_SIZE) throw IllegalStateException("File too large ($size bytes)")
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
        }.getOrElse { "" }
    }

    suspend fun convertToBlockContent(context: Context, uri: Uri, type: ImportFileType): String? {
        if (!isTextLike(type)) return null
        val raw = readTextFromUri(context, uri)
        if (raw.isBlank()) return null
        val migrated = when (type) {
            ImportFileType.HTML -> com.example.data.model.DataBlock.migrateLegacyContent(
                com.example.util.HtmlConverter.convertHtmlToSecureNotes(raw)
            )
            else -> com.example.data.model.DataBlock.migrateLegacyContent(raw)
        }
        return com.example.data.model.DataBlock.serialize(migrated)
    }
}