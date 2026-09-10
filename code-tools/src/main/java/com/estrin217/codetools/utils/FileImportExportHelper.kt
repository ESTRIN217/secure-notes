package com.estrin217.codetools.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.estrin217.codetools.syntax.SupportedLanguage

data class ExternalFileInfo(
    val name: String,
    val content: String,
    val language: SupportedLanguage
)

object FileImportExportHelper {

    /** Límite de lectura para archivos externos (2 MB en chars UTF-16). */
    const val MAX_EXTERNAL_CHARS = 2 * 1024 * 1024

    /**
     * True si el tamaño declarado supera el límite, sin leer contenido.
     * Tamaño desconocido (-1) cuenta como no excedido; la lectura acotada protege igual.
     */
    fun isOversize(context: Context, uri: Uri): Boolean {
        if (uri.scheme == "file") return fileLength(uri) > MAX_EXTERNAL_CHARS
        return querySize(context, uri) > MAX_EXTERNAL_CHARS
    }

    private fun fileLength(uri: Uri): Long {
        return runCatching { java.io.File(uri.path.orEmpty()).length() }.getOrDefault(-1)
    }

    private fun querySize(context: Context, uri: Uri): Long {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use -1L
                val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (idx < 0) -1L else cursor.getLong(idx)
            } ?: -1L
        }.getOrDefault(-1L)
    }

    private fun readCappedText(context: Context, uri: Uri): String? {
        val reader = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)
            ?: return null
        return try {
            reader.use {
                val out = StringBuilder()
                val buf = CharArray(8192)
                while (true) {
                    val n = it.read(buf)
                    if (n < 0) break
                    if (out.length + n > MAX_EXTERNAL_CHARS) return null
                    out.append(buf, 0, n)
                }
                out.toString()
            }
        } catch (e: Exception) {
            Log.e("CodeTools", "readExternalFile failed", e)
            null
        }
    }

    /**
     * Reads the filename and content of a file selected via document picker Uri.
     */
    fun readExternalFile(context: Context, uri: Uri): ExternalFileInfo? {
        return try {
            val contentResolver = context.contentResolver
            var fileName = "archivo_externo.txt"

            // Query display name
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    val queriedName = cursor.getString(nameIndex)
                    if (!queriedName.isNullOrBlank()) {
                        fileName = queriedName
                    }
                }
            }

            // If query didn't work, try last path segment
            if (fileName == "archivo_externo.txt" && uri.lastPathSegment != null) {
                val segment = uri.lastPathSegment.orEmpty().substringAfterLast('/')
                if (segment.isNotBlank()) {
                    fileName = segment
                }
            }

                        // Clean up any remaining path separators
            fileName = fileName.substringAfterLast('/').substringAfterLast('\\').trim()
            if (fileName.isBlank()) {
                fileName = "archivo_externo.txt"
            }

            // Read content with hard cap (protects streams with unknown size)
            val content = readCappedText(context, uri) ?: return null

            val language = SupportedLanguage.fromFileName(fileName)

            ExternalFileInfo(
                name = fileName,
                content = content,
                language = language
            )
        } catch (e: Exception) {
            Log.e("CodeTools", "readExternalFile failed", e)
            null
        }
    }
}
