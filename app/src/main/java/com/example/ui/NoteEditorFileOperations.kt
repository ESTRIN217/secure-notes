package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.example.ui.editor.ExternalTarget
import com.example.ui.editor.externalTargetFor
import com.example.ui.editor.isAudioFile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class NoteEditorFileOperations(
    private val context: Context,
    private val noteId: Int,
    private val scope: CoroutineScope,
    private val onAudioAttached: (String) -> Unit,
    private val onFileAttached: (String, String) -> Unit,
    private val onPickerCompleted: () -> Unit,
    private val persistNote: suspend () -> Unit,
    private val onPickedFileError: (Exception) -> Unit,
    private val onOpenPdfTab: (Uri, String) -> Unit,
    private val onOpenTextTab: (Uri, String) -> Unit,
    private val onOpenFileError: () -> Unit
) {
    fun handlePickedFile(selectedUri: Uri) {
        try {
            val imported = importPickedFile(context, selectedUri, noteId)
            if (imported.isAudio) onAudioAttached(imported.file.absolutePath)
            else onFileAttached(imported.file.absolutePath, imported.name)
            onPickerCompleted()
            scope.launch { persistNote() }
        } catch (e: Exception) {
            Log.e(TAG, "handlePickedFile failed", e)
            onPickedFileError(e)
        }
    }

    fun openExternalFile(path: String) {
        try {
            openExistingFile(path)
        } catch (e: Exception) {
            Log.e(TAG, "openExternalFile failed", e)
            onOpenFileError()
        }
    }

    private fun openExistingFile(path: String) {
        val file = File(path)
        if (!file.exists()) {
            onOpenFileError()
            return
        }
        val ext = path.substringAfterLast('.', "").lowercase()
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        dispatchExternalFile(uri, file.name, mime, ext)
    }

    private fun dispatchExternalFile(uri: Uri, fileName: String, mime: String, ext: String) {
        when (externalTargetFor(mime, ext)) {
            ExternalTarget.PDF -> onOpenPdfTab(uri, fileName)
            ExternalTarget.TEXTO -> onOpenTextTab(uri, fileName)
            // Sin tipo no hay Intent que construir.
            ExternalTarget.SIN_TIPO -> return
            ExternalTarget.VER_CON_OTRA_APP -> context.startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mime)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            )
        }
    }

    private companion object {
        const val TAG = "NoteEditorFileOperations"
    }
}

private data class ImportedNoteFile(val file: File, val name: String, val isAudio: Boolean)

private fun importPickedFile(context: Context, uri: Uri, noteId: Int): ImportedNoteFile {
    val name = queryPickedFileName(context, uri)
    val extension = name.substringAfterLast('.', "").takeIf { '.' in name } ?: ""
    val mime = context.contentResolver.getType(uri) ?: ""
    val audio = isAudioFile(mime, extension)
    val prefix = if (audio) "audio" else "file"
    val localFile = File(context.filesDir, "${prefix}_${noteId}_${System.currentTimeMillis()}.$extension")
    copyPickedFile(context, uri, localFile)
    return ImportedNoteFile(localFile, name, audio)
}

private fun queryPickedFileName(context: Context, uri: Uri): String {
    var name = "selected_file"
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && nameIndex >= 0) name = cursor.getString(nameIndex)
    }
    return name
}

private fun copyPickedFile(context: Context, uri: Uri, destination: File) {
    val input = context.contentResolver.openInputStream(uri)
        ?: throw IOException("Unable to read selected file")
    input.use { source ->
        FileOutputStream(destination).use { output -> source.copyTo(output) }
    }
}
