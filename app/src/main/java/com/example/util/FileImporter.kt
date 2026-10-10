package com.example.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.model.DataBlock
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

    /**
 * Convierte Markdown a contenido de bloque, **sin `Context` ni `Uri`**: solo texto.
 *
 * Vive separada de [convertToBlockContent] a propósito (spec 013, RF-83). El importador del FAB y el
 * envío «Enviar a notas» del editor tienen que producir **exactamente la misma nota**: si cada uno
 * tuviera su conversión, divergirían en cuanto una cambiara, y nadie lo notaría hasta comparar dos
 * notas que deberían ser iguales.
 *
 * Pura y sin Android (RNF-38), que es lo que permite probarla en esta plataforma.
 */
fun markdownToBlockContent(raw: String): String {
    if (raw.isBlank()) return DataBlock.serialize(emptyList())
    return DataBlock.serialize(DataBlock.migrateLegacyContent(raw))
}

/**
 * El título de la nota a partir del nombre del archivo: sin directorios y sin extensión.
 *
 * Una nota no debe heredar la carpeta del almacenamiento (caso límite 5), y el título es el nombre,
 * no el nombre de fichero. Nunca devuelve una cadena vacía: una nota sin título no se distingue en la
 * lista (caso límite 1).
 */
fun noteTitleFrom(fileName: String): String {
    val base = fileName.trim().substringAfterLast('/').substringAfterLast('\\')
    val withoutExtension = base.substringBeforeLast('.', base).trim()
    return withoutExtension.ifBlank { base.trim() }.ifBlank { DEFAULT_NOTE_TITLE }
}

private const val DEFAULT_NOTE_TITLE = "documento"

suspend fun convertToBlockContent(context: Context, uri: Uri, type: ImportFileType): String? {
        if (!isTextLike(type)) return null
        val raw = readTextFromUri(context, uri)
        if (raw.isBlank()) return null
        return when (type) {
            ImportFileType.HTML -> com.example.data.model.DataBlock.serialize(
                com.example.data.model.DataBlock.migrateLegacyContent(
                    com.example.util.HtmlConverter.convertHtmlToSecureNotes(raw)
                )
            )
            // Markdown, TXT y JSON pasan por la MISMA conversión que el envío del editor, para que
            // las dos notas salgan idénticas (spec 013, RF-83).
            else -> this.markdownToBlockContent(raw)
        }
    }
}