package com.estrin217.codetools.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.estrin217.codetools.data.model.CodeFile
import com.estrin217.codetools.syntax.SupportedLanguage
import kotlinx.coroutines.flow.Flow

@Dao
interface CodeFileDao {

    @Query("SELECT * FROM code_files ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllFiles(): Flow<List<CodeFile>>

    @Query("SELECT * FROM code_files WHERE id = :id LIMIT 1")
    suspend fun getFileById(id: Long): CodeFile?

  @Query("SELECT * FROM code_files WHERE name = :name LIMIT 1")
  suspend fun getFileByName(name: String): CodeFile?

    @Query("SELECT * FROM code_files ORDER BY isPinned DESC, updatedAt DESC LIMIT 1")
    suspend fun getLatestFile(): CodeFile?
  
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: CodeFile): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<CodeFile>)

    @Update
    suspend fun updateFile(file: CodeFile)

    @Delete
    suspend fun deleteFile(file: CodeFile)

    @Query("DELETE FROM code_files WHERE id = :id")
    suspend fun deleteFileById(id: Long)

    @Query("SELECT COUNT(*) FROM code_files")
    suspend fun countFiles(): Int

    @Query("SELECT COUNT(*) FROM code_files WHERE language = :language")
    suspend fun countFilesByLanguage(language: SupportedLanguage): Int
}
