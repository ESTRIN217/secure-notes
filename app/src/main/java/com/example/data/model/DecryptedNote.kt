package com.example.data.model

import org.json.JSONArray
import java.time.Instant
import java.util.List

data class DecryptedNote(
    val note: Note,
    val title: String,
    val content: String,
    val isDecryptionSuccessful: Boolean
) {
    fun toListItem(): ListItem {
        val tagsList = try {
            val arr = JSONArray(note.tagsJson)
            List(arr.length()) { arr.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
        return ListItem(
            id = note.id.toString(),
            title = title,
            summary = content,
            lastModified = Instant.ofEpochMilli(note.lastModified),
            backgroundColor = note.backgroundColor,
            backgroundImagePath = note.backgroundImagePath,
            tags = tagsList,
            isArchived = note.isArchived,
            isFavorite = note.isFavorite,
            categoryId = note.categoryId,
            isPinned = note.isPinned,
            isDeleted = note.isDeleted
        )
    }
}
