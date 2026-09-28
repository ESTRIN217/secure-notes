package com.estrin217.editordecodigo.utils

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Log
import com.estrin217.editordecodigo.syntax.SupportedLanguage
import java.io.File

data class ExternalFileInfo(
    val name: String,
    val content: String,
    val language: SupportedLanguage,
    val uriString: String? = null,
    val filePath: String? = null
)

object FileImportExportHelper {

    private const val TAG = "EditorDeCodigo"

    /** Límite de lectura para archivos externos (2 MB en chars UTF-16). */
    const val MAX_EXTERNAL_CHARS = 2 * 1024 * 1024

    /** Tamaño máximo por archivo al importar una carpeta completa (1 MB). */
    private const val MAX_FOLDER_ENTRY_CHARS = 1024 * 1024

    /**
     * True si el tamaño declarado supera el límite, sin leer contenido.
     * Tamaño desconocido (-1) cuenta como no excedido; la lectura acotada protege igual.
     */
    fun isOversize(context: Context, uri: Uri): Boolean {
        if (uri.scheme == "file") return fileLength(uri) > MAX_EXTERNAL_CHARS
        return querySize(context, uri) > MAX_EXTERNAL_CHARS
    }

    private fun fileLength(uri: Uri): Long {
        return runCatching { File(uri.path.orEmpty()).length() }.getOrDefault(-1)
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

    private fun readCappedText(context: Context, uri: Uri, maxChars: Int = MAX_EXTERNAL_CHARS): String? {
        val reader = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)
            ?: return null
        return try {
            reader.use {
                val out = StringBuilder()
                val buf = CharArray(8192)
                while (true) {
                    val n = it.read(buf)
                    if (n < 0) break
                    if (out.length + n > maxChars) return null
                    out.append(buf, 0, n)
                }
                out.toString()
            }
        } catch (e: Exception) {
            Log.e(TAG, "readCappedText failed", e)
            null
        }
    }

    /**
     * Reads the filename and content of a file selected via document picker Uri.
     * @return null when the file cannot be read or exceeds [MAX_EXTERNAL_CHARS].
     */
    fun readExternalFile(context: Context, uri: Uri): ExternalFileInfo? {
        return try {
            val contentResolver = context.contentResolver
            var fileName = "archivo_externo.txt"

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    val queriedName = cursor.getString(nameIndex)
                    if (!queriedName.isNullOrBlank()) {
                        fileName = queriedName
                    }
                }
            }

            if (fileName == "archivo_externo.txt" && uri.lastPathSegment != null) {
                val segment = uri.lastPathSegment.orEmpty().substringAfterLast('/')
                if (segment.isNotBlank()) {
                    fileName = segment
                }
            }

            fileName = fileName.substringAfterLast('/').substringAfterLast('\\').trim()
            if (fileName.isBlank()) {
                fileName = "archivo_externo.txt"
            }

            val content = readCappedText(context, uri) ?: return null

            ExternalFileInfo(
                name = fileName,
                content = content,
                language = SupportedLanguage.fromFileName(fileName),
                uriString = uri.toString(),
                filePath = null
            )
        } catch (e: Exception) {
            Log.e(TAG, "readExternalFile failed", e)
            null
        }
    }

    /**
     * Reads text-based code files inside a selected directory tree Uri (OpenDocumentTree).
     * Import is flat: nested directories are skipped.
     */
    fun readExternalFolder(context: Context, treeUri: Uri): Pair<String, List<ExternalFileInfo>> {
        val folderName = runCatching {
            DocumentsContract.getTreeDocumentId(treeUri)
                .substringAfterLast(':')
                .substringAfterLast('/')
                .ifBlank { "carpeta" }
        }.getOrDefault("carpeta")

        return folderName to readFolderEntries(context, treeUri)
    }

    private fun readFolderEntries(context: Context, treeUri: Uri): List<ExternalFileInfo> {
        val loadedFiles = mutableListOf<ExternalFileInfo>()
        try {
            val contentResolver = context.contentResolver
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                treeUri,
                DocumentsContract.getTreeDocumentId(treeUri)
            )
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE
            )

            contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)

                while (cursor.moveToNext()) {
                    val docId = (if (idIndex >= 0) cursor.getString(idIndex) else null) ?: continue
                    val name = if (nameIndex >= 0) cursor.getString(nameIndex) else "archivo"
                    val mime = if (mimeIndex >= 0) cursor.getString(mimeIndex) else ""
                    val size = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L

                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) continue
                    if (size > MAX_FOLDER_ENTRY_CHARS) continue

                    val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                    val content = readCappedText(context, docUri, MAX_FOLDER_ENTRY_CHARS) ?: continue
                    loadedFiles.add(
                        ExternalFileInfo(
                            name = name,
                            content = content,
                            language = SupportedLanguage.fromFileName(name),
                            uriString = docUri.toString(),
                            filePath = null
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "readExternalFolder failed", e)
        }
        return loadedFiles
    }
}
