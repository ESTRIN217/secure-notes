package com.example.data.model

import com.example.util.RichTextConverter
import org.json.JSONArray

data class DecryptedNote(
    val note: Note,
    val title: String,
    val content: String,
    val isDecryptionSuccessful: Boolean
) {
    val summary: String
        get() = RichTextConverter.contentToPlainText(content)

    val tagsList: List<String>
        get() = try {
            val arr = JSONArray(note.tagsJson)
            (0 until arr.length()).mapNotNull { i ->
                val name = arr.optJSONObject(i)?.optString("name") ?: arr.optString(i)
                name.takeIf { it.isNotEmpty() }
            }
        } catch (_: Exception) {
            emptyList()
        }
}
