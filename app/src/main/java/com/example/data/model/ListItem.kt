package com.example.data.model

import java.time.Instant
import java.time.format.DateTimeFormatter
import org.json.JSONArray
import org.json.JSONException

enum class SortMethod {
    CUSTOM, ALPHABETICAL, BY_DATE
}

data class ListItem(
    val id: String,
    val title: String,
    val summary: String,
    val lastModified: Instant,
    val backgroundColor: Int? = null,
    val backgroundImagePath: String? = null,
    val tags: List<String> = emptyList(),
    val isArchived: Boolean = false,
    val isFavorite: Boolean = false,
    val categoryId: String? = null,
    val isPinned: Boolean = false,
    val isDeleted: Boolean = false
) {
    // Método equivalente a copyWith se genera automáticamente en Kotlin usando .copy()

    fun toJson(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "title" to title,
            "summary" to summary,
            "lastModified" to DateTimeFormatter.ISO_INSTANT.format(lastModified),
            "backgroundColor" to backgroundColor,
            "backgroundImagePath" to backgroundImagePath,
            "tags" to tags,
            "isArchived" to isArchived,
            "isFavorite" to isFavorite,
            "categoryId" to categoryId,
            "isPinned" to isPinned,
            "isDeleted" to isDeleted
        )
    }

    companion object {
        fun fromJson(json: Map<String, Any?>): ListItem {
            val tagsRaw = json["tags"] as? List<*>
            val tagsList = tagsRaw?.mapNotNull { it as? String } ?: emptyList()

            return ListItem(
                id = json["id"] as String,
                title = json["title"] as String,
                summary = json["summary"] as String,
                lastModified = Instant.parse(json["lastModified"] as String),
                backgroundColor = json["backgroundColor"] as? Int,
                backgroundImagePath = json["backgroundImagePath"] as? String,
                tags = tagsList,
                isArchived = json["isArchived"] as? Boolean ?: false,
                isFavorite = json["isFavorite"] as? Boolean ?: false,
                categoryId = json["categoryId"] as? String,
                isPinned = json["isPinned"] as? Boolean ?: false,
                isDeleted = json["isDeleted"] as? Boolean ?: false
            )
        }
    }

    // Helper equivalente para verificar si el contenido de summary es un JSON Delta válido
    val isJsonDelta: Boolean
        get() {
            val trimmed = summary.trim()
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                return try {
                    JSONArray(trimmed)
                    true
                } catch (e: JSONException) {
                    false
                }
            }
            return false
        }
}
