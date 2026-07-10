package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val isEncrypted: Boolean = false,
    val salt: String = "",
    val iv: String = "",
    val lastModified: Long = System.currentTimeMillis(),
    val tagsJson: String = "[]", // JSON array of tag names, e.g., ["Work", "Personal"]
    val backgroundColor: Int? = null,
    val backgroundImagePath: String? = null,
    val isArchived: Boolean = false,
    val isFavorite: Boolean = false,
    val categoryId: String? = null,
    val isPinned: Boolean = false,
    val isDeleted: Boolean = false
)


fun Note.cleanedTags(): List<String> = tagsJson
    .replace("[", "")
    .replace("]", "")
    .replace("\"", "")
    .split(",")
    .map { it.trim() }
    .filter { it.isNotEmpty() }

@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey val name: String,
    val colorHex: String = "#7E57C2" // Standard light purple/violet accent
)
