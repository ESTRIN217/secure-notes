package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val isEncrypted: Boolean = false,
    val salt: String = "",
    val iv: String = "",
    val lastModified: Long = System.currentTimeMillis(),
    val tagsJson: String = "[]",
    val backgroundColor: Int? = null,
    val backgroundImagePath: String? = null,
    val isArchived: Boolean = false,
    val isFavorite: Boolean = false,
    val categoryId: String? = null,
    val isPinned: Boolean = false,
    val isDeleted: Boolean = false
)

fun Note.parseTags(): List<String> {
    return try {
        val arr = JSONArray(tagsJson)
        List(arr.length()) { arr.optString(it) }
    } catch (e: Exception) {
        emptyList()
    }
}

fun Note.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("title", title)
    put("content", content)
    put("isEncrypted", isEncrypted)
    put("salt", salt)
    put("iv", iv)
    put("lastModified", lastModified)
    put("tagsJson", tagsJson)
    put("isArchived", isArchived)
    put("isFavorite", isFavorite)
    put("isPinned", isPinned)
    put("isDeleted", isDeleted)
    put("backgroundColor", backgroundColor)
    put("backgroundImagePath", backgroundImagePath ?: "")
    put("categoryId", categoryId)
}

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
    val colorHex: String = "#7E57C2"
)

fun Tag.toJson(): JSONObject = JSONObject().apply {
    put("name", name)
    put("colorHex", colorHex)
}
