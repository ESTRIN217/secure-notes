package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject

fun parseNoteContentAndAttachments(rawContent: String): Pair<String, List<Attachment>> {
    val delimiter = "\n\n---Attachments---\n"
    if (rawContent.contains(delimiter)) {
        val parts = rawContent.split(delimiter, limit = 2)
        val text = parts[0]
        val jsonStr = parts.getOrNull(1) ?: "[]"
        val list = mutableListOf<Attachment>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(Attachment(
                    type = obj.getString("type"),
                    path = obj.getString("path"),
                    name = obj.optString("name", "")
                ))
            }
        } catch (e: Exception) {
            android.util.Log.e("NoteContentUtils", "parseNoteContentAndAttachments failed", e)
        }
        return Pair(text, list)
    }
    return Pair(rawContent, emptyList())
}

fun createRawContent(text: String, attachments: List<Attachment>): String {
    if (attachments.isEmpty()) return text
    val delimiter = "\n\n---Attachments---\n"
    val arr = JSONArray()
    attachments.forEach { att ->
        val obj = JSONObject()
        obj.put("type", att.type)
        obj.put("path", att.path)
        obj.put("name", att.name)
        arr.put(obj)
    }
    return text + delimiter + arr.toString()
}
