package com.example.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.withStyle

class JsonColorizer {

    fun isJson(text: String): Boolean {
        val trimmed = text.trim()
        return (trimmed.startsWith("{") && trimmed.endsWith("}")) || (trimmed.startsWith("[") && trimmed.endsWith("]"))
    }

    fun isSecureNotesJson(text: String): Boolean {
        return try {
            val trimmed = text.trim()
            if (trimmed.startsWith("{")) {
                val json = org.json.JSONObject(trimmed)
                json.has("id") && json.has("title") && json.has("summary")
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun parseSecureNotesJson(text: String, defaultTitle: String = "Imported Note"): Pair<String, String> {
        val json = org.json.JSONObject(text.trim())
        val title = json.optString("title", defaultTitle)
        val summary = json.optString("summary", "")
        return Pair(title, summary)
    }

    fun parseColor(value: String?): Color? {
        if (value == null) return null
        val cleaned = value.trim().removeSurrounding("\"").removeSurrounding("'")
        if (cleaned.startsWith("#")) {
            return try {
                val hex = cleaned.substring(1)
                if (hex.length == 6) {
                    Color((0xFF000000 or hex.toLong(16)).toInt())
                } else if (hex.length == 8) {
                    Color(hex.toLong(16).toInt())
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        } else {
            return when (cleaned.lowercase()) {
                "red" -> Color(0xFFD32F2F)
                "blue" -> Color(0xFF1976D2)
                "green" -> Color(0xFF388E3C)
                "yellow" -> Color(0xFFFBC02D)
                "orange" -> Color(0xFFF57C00)
                "purple" -> Color(0xFF7B1FA2)
                "pink" -> Color(0xFFC2185B)
                "brown" -> Color(0xFF5D4037)
                "black" -> Color(0xFF000000)
                "white" -> Color(0xFFFFFFFF)
                "gray" -> Color(0xFF757575)
                else -> null
            }
        }
    }

    fun highlightJson(text: String, builder: AnnotatedString.Builder) {
        val cleaned = text.trim()
        var i = 0
        var inString = false
        var inKey = false
        var braceDepth = 0
        while (i < cleaned.length) {
            val ch = cleaned[i]
            when {
                ch == '"' && !inString -> {
                    inString = true
                    inKey = !inKey
                    builder.append(ch.toString())
                }
                ch == '"' && inString -> {
                    inString = false
                    builder.append(ch.toString())
                }
                inString -> {
                    builder.withStyle(androidx.compose.ui.text.SpanStyle(color = Color(0xFF6A8759))) {
                        append(ch.toString())
                    }
                }
                ch == '{' || ch == '}' || ch == '[' || ch == ']' -> {
                    if (ch == '{' || ch == '[') braceDepth++ else braceDepth--
                    builder.withStyle(androidx.compose.ui.text.SpanStyle(color = Color(0xFF6897BB), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)) {
                        append(ch.toString())
                    }
                }
                ch == ':' -> {
                    builder.append(ch.toString())
                }
                ch == ',' -> {
                    builder.append(ch.toString())
                }
                ch == 't' && cleaned.substring(i).startsWith("true") -> {
                    builder.withStyle(androidx.compose.ui.text.SpanStyle(color = Color(0xFFCC7832))) {
                        append("true")
                    }
                    i += 3
                }
                ch == 'f' && cleaned.substring(i).startsWith("false") -> {
                    builder.withStyle(androidx.compose.ui.text.SpanStyle(color = Color(0xFFCC7832))) {
                        append("false")
                    }
                    i += 4
                }
                ch == 'n' && cleaned.substring(i).startsWith("null") -> {
                    builder.withStyle(androidx.compose.ui.text.SpanStyle(color = Color(0xFFCC7832))) {
                        append("null")
                    }
                    i += 3
                }
                ch == '-' || ch.isDigit() -> {
                    val start = i
                    while (i < cleaned.length && (cleaned[i].isDigit() || cleaned[i] == '.' || cleaned[i] == '-' || cleaned[i] == 'e' || cleaned[i] == 'E')) {
                        i++
                    }
                    builder.withStyle(androidx.compose.ui.text.SpanStyle(color = Color(0xFF6897BB))) {
                        append(cleaned.substring(start, i))
                    }
                    i--
                }
                else -> {
                    builder.append(ch.toString())
                }
            }
            i++
        }
    }

    companion object {
        private val default = JsonColorizer()

        fun isJson(text: String) = default.isJson(text)
        fun isSecureNotesJson(text: String) = default.isSecureNotesJson(text)
        fun parseSecureNotesJson(text: String, defaultTitle: String = "Imported Note") = default.parseSecureNotesJson(text, defaultTitle)
        fun parseColor(value: String?) = default.parseColor(value)
        fun highlightJson(text: String, builder: AnnotatedString.Builder) = default.highlightJson(text, builder)
    }
}
