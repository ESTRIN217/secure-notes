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

            // Read content safely
            val content = contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                it.readText()
            } ?: return null

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
