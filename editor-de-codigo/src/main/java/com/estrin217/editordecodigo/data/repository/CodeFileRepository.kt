package com.estrin217.editordecodigo.data.repository

import android.content.Context
import com.estrin217.editordecodigo.data.db.CodeFileDao
import com.estrin217.editordecodigo.data.model.CodeFile
import com.estrin217.editordecodigo.utils.FileStorageManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CodeFileRepository(
    private val dao: CodeFileDao,
    private val context: Context? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    val allFiles: Flow<List<CodeFile>> = dao.getAllFiles()

    suspend fun getFileById(id: Long): CodeFile? {
        val meta = dao.getFileById(id) ?: return null
        return if (context != null) {
            val diskContent = withContext(ioDispatcher) {
                FileStorageManager.readContent(context, meta)
            }
            meta.copy(content = diskContent)
        } else {
            meta
        }
    }

    suspend fun getFileByName(name: String): CodeFile? {
        val meta = dao.getFileByName(name) ?: return null
        return if (context != null) {
            val diskContent = withContext(ioDispatcher) {
                FileStorageManager.readContent(context, meta)
            }
            meta.copy(content = diskContent)
        } else {
            meta
        }
    }

    suspend fun getLatestFile(): CodeFile? {
        val meta = dao.getLatestFile() ?: return null
        return if (context != null) {
            val diskContent = withContext(ioDispatcher) {
                FileStorageManager.readContent(context, meta)
            }
            meta.copy(content = diskContent)
        } else {
            meta
        }
    }

    /**
     * Guarda el archivo en almacenamiento del dispositivo/URI externa,
     * y guarda únicamente los metadatos e historial de reciente en la base de datos.
     */
    suspend fun saveFile(file: CodeFile, contentToSave: String): Long {
        if (context == null) {
            val updated = file.copy(
                content = contentToSave,
                sizeBytes = contentToSave.toByteArray(Charsets.UTF_8).size.toLong(),
                updatedAt = System.currentTimeMillis()
            )
            return if (updated.id == 0L) {
                dao.insertFile(updated)
            } else {
                dao.updateFile(updated)
                updated.id
            }
        }

        // Aseguramos que el archivo físico exista en disco si no tiene URI ni filePath
        val targetPath = if (file.filePath.isNullOrBlank() && file.uriString.isNullOrBlank()) {
            val created = withContext(ioDispatcher) {
                FileStorageManager.createPhysicalFile(context, file.name, contentToSave)
            }
            created.absolutePath
        } else {
            file.filePath
        }

        val updatedMeta = file.copy(
            filePath = targetPath,
            sizeBytes = contentToSave.toByteArray(Charsets.UTF_8).size.toLong(),
            updatedAt = System.currentTimeMillis()
        )

        // Escribir el contenido en disco/SAF
        withContext(ioDispatcher) {
            FileStorageManager.writeContent(context, updatedMeta, contentToSave)
        }

        // Guardar en base de datos Room solo metadatos (content vacío en BD para no almacenar contenido allí)
        val metaForDb = updatedMeta.copy(content = "")
        return if (updatedMeta.id == 0L) {
            dao.insertFile(metaForDb)
        } else {
            dao.updateFile(metaForDb)
            updatedMeta.id
        }
    }

    /**
     * Sobrecarga de compatibilidad
     */
    suspend fun saveFile(file: CodeFile): Long = saveFile(file, file.content)

    /**
     * Lee el contenido fresco desde el almacenamiento exterior.
     */
    suspend fun readFileContent(file: CodeFile): String {
        return if (context != null) {
            withContext(ioDispatcher) {
                FileStorageManager.readContent(context, file)
            }
        } else {
            file.content
        }
    }

    suspend fun deleteFile(file: CodeFile) {
        if (context != null) {
            withContext(ioDispatcher) {
                FileStorageManager.deletePhysicalFile(context, file)
            }
        }
        dao.deleteFile(file)
    }

    suspend fun deleteFileById(id: Long) {
        val file = dao.getFileById(id)
        if (file != null) {
            if (context != null) {
                withContext(ioDispatcher) {
                    FileStorageManager.deletePhysicalFile(context, file)
                }
            }
            dao.deleteFile(file)
        }
    }

}

