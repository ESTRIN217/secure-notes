package com.estrin217.editordecodigo.utils

import android.content.Context
import android.net.Uri
import com.estrin217.editordecodigo.data.model.CodeFile
import com.estrin217.editordecodigo.syntax.SupportedLanguage
import java.io.File
import java.io.FileOutputStream

/**
 * Gestor de almacenamiento físico de archivos de código.
 * Los archivos viven en el almacenamiento del dispositivo (disco externo/físico),
 * y la base de datos se reserva exclusivamente para índices, metadatos e historial de recientes.
 */
object FileStorageManager {

    private const val CODE_STORAGE_DIR = "code_files"

    /**
     * Retorna el directorio raíz en el almacenamiento donde residen físicamente los archivos.
     */
    fun getStorageDirectory(context: Context): File {
        val dir = File(context.filesDir, CODE_STORAGE_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Escribe o sobrescribe el contenido de un archivo en el disco físico.
     * Soporta tanto archivos en el directorio local como Uris de Storage Access Framework (SAF).
     */
    fun writeContent(context: Context, file: CodeFile, content: String): Boolean {
        return try {
            // 1. Si el archivo fue abierto o vinculado a través de SAF (content://)
            if (!file.uriString.isNullOrBlank() && file.uriString.startsWith("content://")) {
                val uri = Uri.parse(file.uriString)
                val outputStream = context.contentResolver.openOutputStream(uri, "wt")
                if (outputStream != null) {
                    outputStream.use { os ->
                        os.write(content.toByteArray(Charsets.UTF_8))
                        os.flush()
                    }
                    return true
                }
            }

            // 2. Si tiene una ruta de archivo local específica (file:// o ruta absoluta)
            val targetFile = if (!file.filePath.isNullOrBlank()) {
                val path = if (file.filePath.startsWith("file://")) {
                    Uri.parse(file.filePath).path ?: file.filePath
                } else {
                    file.filePath
                }
                File(path)
            } else {
                // Fallback: guardar en el directorio de almacenamiento local de la app
                File(getStorageDirectory(context), file.name)
            }

            targetFile.parentFile?.mkdirs()
            FileOutputStream(targetFile).use { fos ->
                fos.write(content.toByteArray(Charsets.UTF_8))
                fos.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Lee el contenido desde el archivo físico en el disco o mediante content Uri.
     */
    fun readContent(context: Context, file: CodeFile): String {
        return try {
            // 1. Si tiene URI de SAF (content://)
            if (!file.uriString.isNullOrBlank() && file.uriString.startsWith("content://")) {
                val uri = Uri.parse(file.uriString)
                context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                    it.readText()
                } ?: ""
            } else if (!file.filePath.isNullOrBlank()) {
                val path = if (file.filePath.startsWith("file://")) {
                    Uri.parse(file.filePath).path ?: file.filePath
                } else {
                    file.filePath
                }
                val diskFile = File(path)
                if (diskFile.exists()) {
                    diskFile.readText(Charsets.UTF_8)
                } else {
                    // Si no existe aún en disco, intentamos en el directorio estándar
                    val fallbackFile = File(getStorageDirectory(context), file.name)
                    if (fallbackFile.exists()) fallbackFile.readText(Charsets.UTF_8) else ""
                }
            } else {
                val diskFile = File(getStorageDirectory(context), file.name)
                if (diskFile.exists()) diskFile.readText(Charsets.UTF_8) else ""
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * Crea un archivo físico nuevo en el almacenamiento del dispositivo con el contenido inicial.
     * Retorna la ruta absoluta del archivo creado.
     */
    fun createPhysicalFile(context: Context, fileName: String, initialContent: String): File {
        val dir = getStorageDirectory(context)
        var file = File(dir, fileName)
        if (file.exists()) {
            // Si ya existe, generar un nombre con timestamp para no pisar
            val baseName = fileName.substringBeforeLast('.')
            val extension = fileName.substringAfterLast('.', "")
            val extPart = if (extension.isNotBlank()) ".$extension" else ""
            file = File(dir, "${baseName}_${System.currentTimeMillis() % 10000}$extPart")
        }
        FileOutputStream(file).use { fos ->
            fos.write(initialContent.toByteArray(Charsets.UTF_8))
            fos.flush()
        }
        return file
    }

    /**
     * Elimina el archivo del almacenamiento físico si existe.
     */
    fun deletePhysicalFile(context: Context, file: CodeFile): Boolean {
        return try {
            if (!file.filePath.isNullOrBlank()) {
                val f = File(file.filePath)
                if (f.exists()) f.delete() else true
            } else {
                val f = File(getStorageDirectory(context), file.name)
                if (f.exists()) f.delete() else true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
