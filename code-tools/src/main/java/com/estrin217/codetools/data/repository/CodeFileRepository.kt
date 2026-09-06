package com.estrin217.codetools.data.repository

import com.estrin217.codetools.data.db.CodeFileDao
import com.estrin217.codetools.data.model.CodeFile
import com.estrin217.codetools.data.samples.CodeSamples
import kotlinx.coroutines.flow.Flow

class CodeFileRepository(private val dao: CodeFileDao) {

    val allFiles: Flow<List<CodeFile>> = dao.getAllFiles()

    suspend fun getFileById(id: Long): CodeFile? = dao.getFileById(id)

    suspend fun getFileByName(name: String): CodeFile? = dao.getFileByName(name)

    suspend fun getLatestFile(): CodeFile? = dao.getLatestFile()
    
    suspend fun saveFile(file: CodeFile): Long {
        return if (file.id == 0L) {
            dao.insertFile(file)
        } else {
            dao.updateFile(file)
            file.id
        }
    }

    suspend fun deleteFile(file: CodeFile) = dao.deleteFile(file)

    suspend fun deleteFileById(id: Long) = dao.deleteFileById(id)

    suspend fun ensureSamplesLoaded() {
        if (dao.countFiles() == 0) {
            dao.insertFiles(CodeSamples.samples)
        } else {
            val hasKotlin = dao.countFilesByLanguage(com.estrin217.codetools.syntax.SupportedLanguage.KOTLIN) > 0
            if (!hasKotlin) {
                val kotlinSample = CodeSamples.samples.firstOrNull { it.language == com.estrin217.codetools.syntax.SupportedLanguage.KOTLIN }
                if (kotlinSample != null) {
                    dao.insertFile(kotlinSample)
                }
            }
        }
    }
}
