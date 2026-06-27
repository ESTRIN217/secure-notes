package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import org.json.JSONArray

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

fun Note.toListItem(): ListItem {
    val tagsList = try {
        val arr = JSONArray(tagsJson)
        List(arr.length()) { arr.getString(it) }
    } catch (e: Exception) {
        emptyList()
    }
    return ListItem(
        id = id.toString(),
        title = title,
        summary = content,
        lastModified = Instant.ofEpochMilli(lastModified),
        backgroundColor = backgroundColor,
        backgroundImagePath = backgroundImagePath,
        tags = tagsList,
        isArchived = isArchived,
        isFavorite = isFavorite,
        categoryId = categoryId,
        isPinned = isPinned,
        isDeleted = isDeleted
    )
}

fun ListItem.toNote(isEncrypted: Boolean = false, salt: String = "", iv: String = ""): Note {
    return Note(
        id = id.toIntOrNull() ?: 0,
        title = title,
        content = summary,
        isEncrypted = isEncrypted,
        salt = salt,
        iv = iv,
        lastModified = lastModified.toEpochMilli(),
        tagsJson = JSONArray(tags).toString(),
        backgroundColor = backgroundColor,
        backgroundImagePath = backgroundImagePath,
        isArchived = isArchived,
        isFavorite = isFavorite,
        categoryId = categoryId,
        isPinned = isPinned,
        isDeleted = isDeleted
    )
}



@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey val name: String,
    val colorHex: String = "#7E57C2" // Standard light purple/violet accent
)
