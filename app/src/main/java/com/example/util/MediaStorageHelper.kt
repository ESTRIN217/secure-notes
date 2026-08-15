package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileOutputStream

object MediaStorageHelper {
    private const val TAG = "MediaStorageHelper"
    private const val MEDIA_DIR = "media"

    fun mediaDir(context: Context): File =
        File(context.filesDir, MEDIA_DIR).apply { mkdirs() }

    /**
     * Copia el contenido de [uri] a `filesDir/media/<kind>_<noteId>_<ts>.<ext>`
     * y devuelve la ruta absoluta, o `null` si falla.
     */
    fun importMedia(context: Context, uri: Uri, noteId: Int, kind: String): String? {
        return try {
            val extension = extensionFor(context, uri, kind)
            val destFile = File(
                mediaDir(context),
                "${kind}_${noteId}_${System.currentTimeMillis()}.$extension"
            )
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output -> input.copyTo(output) }
            } ?: run {
                Log.e(TAG, "openInputStream failed for $uri")
                return null
            }
            destFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "importMedia failed for $uri", e)
            null
        }
    }

    private fun extensionFor(context: Context, uri: Uri, kind: String): String {
        context.contentResolver.getType(uri)?.let { mime ->
            MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)?.let { ext ->
                if (ext.isNotBlank()) return ext
            }
        }
        return uri.path?.substringAfterLast('.', "")?.takeIf { it.isNotBlank() }
            ?: when (kind) {
                "img" -> "jpg"
                "vid" -> "mp4"
                "audio" -> "m4a"
                else -> "bin"
            }
    }
}
