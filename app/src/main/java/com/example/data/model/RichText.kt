package com.example.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.example.util.JsonColorizer
import com.example.util.MathRenderer
import com.example.util.RichTextParser
import org.json.JSONArray
import org.json.JSONObject

enum class TextBaseline { NORMAL, SUBSCRIPT, SUPERSCRIPT }

data class TextSegment(
    val text: String = "",
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    val code: Boolean = false,
    val colorHex: String? = null,
    val bgColorHex: String? = null,
    val fontFamily: String? = null,
    val fontSizeSp: Float? = null,
    val baseline: TextBaseline = TextBaseline.NORMAL,
    val linkUrl: String? = null,
    val equationLatex: String? = null
) {
    val isNoteLink: Boolean get() = linkUrl?.startsWith(RichTextParser.NOTE_LINK_PREFIX) == true

    val plainText: String
        get() = if (equationLatex != null) MathRenderer.render(equationLatex).text else text

    fun toSpanStyle(): SpanStyle {
        var style = SpanStyle()
        if (bold) style = style.copy(fontWeight = FontWeight.Bold)
        if (italic) style = style.copy(fontStyle = FontStyle.Italic)
        val decorations = buildList {
            if (underline) add(TextDecoration.Underline)
            if (strikethrough) add(TextDecoration.LineThrough)
        }
        if (decorations.isNotEmpty()) style = style.copy(textDecoration = TextDecoration.combine(decorations))
        if (code) {
            style = style.copy(
                fontFamily = FontFamily.Monospace,
                background = Color(0x1F808080),
                color = Color(0xFFE91E63)
            )
        }
        colorHex?.let { JsonColorizer.parseColor(it)?.let { c -> style = style.copy(color = c) } }
        if (!code) bgColorHex?.let { JsonColorizer.parseColor(it)?.let { c -> style = style.copy(background = c) } }
        fontFamily?.let { name ->
            val family = when (name.lowercase()) {
                "serif" -> FontFamily.Serif
                "monospace" -> FontFamily.Monospace
                "sans-serif" -> FontFamily.SansSerif
                "cursive" -> FontFamily.Cursive
                else -> FontFamily.Default
            }
            style = style.copy(fontFamily = family)
        }
        fontSizeSp?.let { style = style.copy(fontSize = it.sp) }
        when (baseline) {
            TextBaseline.SUBSCRIPT -> style = style.copy(baselineShift = BaselineShift.Subscript, fontSize = (fontSizeSp ?: 11f).sp)
            TextBaseline.SUPERSCRIPT -> style = style.copy(baselineShift = BaselineShift.Superscript, fontSize = (fontSizeSp ?: 11f).sp)
            else -> {}
        }
        if (linkUrl != null) {
            if (isNoteLink) {
                style = style.copy(background = Color(0x1F808080), textDecoration = TextDecoration.None)
            } else {
                style = style.copy(color = Color(0xFF1976D2), textDecoration = TextDecoration.Underline)
            }
        }
        return style
    }

    fun hasSameStyle(other: TextSegment): Boolean =
        bold == other.bold &&
            italic == other.italic &&
            underline == other.underline &&
            strikethrough == other.strikethrough &&
            code == other.code &&
            colorHex == other.colorHex &&
            bgColorHex == other.bgColorHex &&
            fontFamily == other.fontFamily &&
            fontSizeSp == other.fontSizeSp &&
            baseline == other.baseline &&
            linkUrl == other.linkUrl &&
            equationLatex == other.equationLatex

    fun toJson(): JSONObject = JSONObject().apply {
        put("text", text)
        if (bold) put("bold", true)
        if (italic) put("italic", true)
        if (underline) put("underline", true)
        if (strikethrough) put("strikethrough", true)
        if (code) put("code", true)
        colorHex?.let { put("color", it) }
        bgColorHex?.let { put("bg", it) }
        fontFamily?.let { put("font", it) }
        fontSizeSp?.let { put("size", it) }
        if (baseline != TextBaseline.NORMAL) put("baseline", baseline.name)
        linkUrl?.let { put("url", it) }
        equationLatex?.let { put("eq", it) }
    }

    companion object {
        fun fromJson(obj: JSONObject): TextSegment = TextSegment(
            text = obj.optString("text", ""),
            bold = obj.optBoolean("bold", false),
            italic = obj.optBoolean("italic", false),
            underline = obj.optBoolean("underline", false),
            strikethrough = obj.optBoolean("strikethrough", false),
            code = obj.optBoolean("code", false),
            colorHex = obj.optString("color").takeIf { it.isNotEmpty() },
            bgColorHex = obj.optString("bg").takeIf { it.isNotEmpty() },
            fontFamily = obj.optString("font").takeIf { it.isNotEmpty() },
            fontSizeSp = if (obj.has("size") && !obj.isNull("size")) obj.getDouble("size").toFloat() else null,
            baseline = try {
                TextBaseline.valueOf(obj.optString("baseline", TextBaseline.NORMAL.name))
            } catch (e: Exception) {
                TextBaseline.NORMAL
            },
            linkUrl = obj.optString("url").takeIf { it.isNotEmpty() },
            equationLatex = obj.optString("eq").takeIf { it.isNotEmpty() }
        )

        fun serialize(segments: List<TextSegment>): String {
            if (segments.isEmpty()) return ""
            val arr = JSONArray()
            segments.forEach { arr.put(it.toJson()) }
            return arr.toString()
        }

        fun deserialize(json: String?): List<TextSegment>? {
            if (json.isNullOrBlank()) return null
            return try {
                val arr = JSONArray(json)
                (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
            } catch (e: Exception) {
                null
            }
        }

        fun fontFamilyName(family: FontFamily): String? = when (family) {
            FontFamily.Serif -> "serif"
            FontFamily.Monospace -> "monospace"
            FontFamily.SansSerif -> "sans-serif"
            FontFamily.Cursive -> "cursive"
            else -> null
        }
    }
}
