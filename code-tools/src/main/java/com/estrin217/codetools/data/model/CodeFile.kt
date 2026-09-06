package com.estrin217.codetools.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.estrin217.codetools.syntax.SupportedLanguage

@Entity(tableName = "code_files")
data class CodeFile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val content: String,
    val language: SupportedLanguage,
    val updatedAt: Long = System.currentTimeMillis(),
    val isSample: Boolean = false,
    val isPinned: Boolean = false
)
