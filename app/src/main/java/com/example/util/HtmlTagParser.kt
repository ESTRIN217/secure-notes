package com.example.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

class HtmlTagParser {

    data class TagInfo(
        val tagName: String,
        val isClosing: Boolean,
        val value: String?,
        val startIndex: Int,
        val endIndex: Int
    )

    fun parseTag(raw: String, openIdx: Int): TagInfo? {
        val closeIdx = raw.indexOf('>', openIdx)
        if (closeIdx == -1) return null

        val tagContent = raw.substring(openIdx + 1, closeIdx).trim()
        val lowerTag = tagContent.lowercase()

        if (lowerTag.startsWith("/")) {
            val endTagName = lowerTag.substring(1)
            if (endTagName in closingTags) {
                return TagInfo(endTagName, true, null, openIdx, closeIdx + 1)
            }
            return null
        }

        val eqIdx = tagContent.indexOf('=')
        val spIdx = tagContent.indexOf(' ')
        val endIdx = when {
            eqIdx != -1 && spIdx != -1 -> minOf(eqIdx, spIdx)
            eqIdx != -1 -> eqIdx
            spIdx != -1 -> spIdx
            else -> tagContent.length
        }
        val tagName = tagContent.substring(0, endIdx).lowercase()
        val tagValue = if (eqIdx != -1) {
            val valuePart = tagContent.substring(eqIdx + 1).trim()
            val spaceInValue = valuePart.indexOf(' ')
            if (spaceInValue != -1) {
                valuePart.substring(0, spaceInValue).trim().removeSurrounding("\"").removeSurrounding("'")
            } else {
                valuePart.removeSurrounding("\"").removeSurrounding("'")
            }
        } else null

        return TagInfo(tagName, false, tagValue, openIdx, closeIdx + 1)
    }

    fun tagToStyle(tagInfo: TagInfo): SpanStyle? {
        val tagName = tagInfo.tagName
        val tagValue = tagInfo.value
        return when (tagName) {
            "b" -> SpanStyle(fontWeight = FontWeight.Bold)
            "i" -> SpanStyle(fontStyle = FontStyle.Italic)
            "u" -> SpanStyle(textDecoration = TextDecoration.Underline)
            "s" -> SpanStyle(textDecoration = TextDecoration.LineThrough)
            "code" -> SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0x1F808080), color = Color(0xFFE91E63))
            "pre" -> SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0x15000000), color = Color(0xFF37474F))
            "quote" -> SpanStyle(fontStyle = FontStyle.Italic, color = Color(0xFF607D8B))
            "h1" -> SpanStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp)
            "h2" -> SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp)
            "h3" -> SpanStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp)
            "h4" -> SpanStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp)
            "h5" -> SpanStyle(fontWeight = FontWeight.Bold, fontSize = 13.sp)
            "h6" -> SpanStyle(fontWeight = FontWeight.Bold, fontSize = 11.sp)
            "normal" -> SpanStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp)
            "sub" -> SpanStyle(fontSize = 11.sp, baselineShift = BaselineShift.Subscript)
            "sup" -> SpanStyle(fontSize = 11.sp, baselineShift = BaselineShift.Superscript)
            "color" -> {
                val c = JsonColorizer.parseColor(tagValue)
                if (c != null) SpanStyle(color = c) else null
            }
            "bg" -> {
                val c = JsonColorizer.parseColor(tagValue)
                if (c != null) SpanStyle(background = c) else null
            }
            "font" -> {
                val fam = when (tagValue?.lowercase()?.removeSurrounding("\"")?.removeSurrounding("'")) {
                    "serif" -> FontFamily.Serif
                    "monospace" -> FontFamily.Monospace
                    "sans-serif" -> FontFamily.SansSerif
                    "cursive" -> FontFamily.Cursive
                    else -> FontFamily.Default
                }
                SpanStyle(fontFamily = fam)
            }
            "size" -> {
                val sz = tagValue?.removeSurrounding("\"")?.removeSurrounding("'")?.toIntOrNull() ?: 16
                SpanStyle(fontSize = sz.sp)
            }
            "url" -> SpanStyle(color = Color(0xFF1976D2), textDecoration = TextDecoration.Underline)
            "mark" -> SpanStyle(background = Color(0xFFFFEB3B))
            "highlight" -> {
                val c = JsonColorizer.parseColor(tagValue)
                if (c != null) SpanStyle(background = c) else SpanStyle(background = Color(0xFFFFEB3B))
            }
            "small" -> SpanStyle(fontSize = 12.sp)
            "kbd" -> SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0x1F808080), color = Color(0xFF37474F))
            "var" -> SpanStyle(fontStyle = FontStyle.Italic)
            "samp" -> SpanStyle(fontFamily = FontFamily.Monospace)
            else -> null
        }
    }

    fun tagNeedsStyle(tagName: String): Boolean = tagName in listOf(
        "b", "i", "u", "s", "code", "pre", "quote", "color", "bg", "font", "size",
        "h1", "h2", "h3", "h4", "h5", "h6", "normal", "sub", "sup", "url",
        "mark", "highlight", "small", "kbd", "var", "samp"
    )

    fun tagIsStructural(tagName: String): Boolean = tagName in listOf(
        "ol", "ul", "cl", "indent", "li", "item", "img", "video", "audio",
        "table", "tr", "td", "th", "hr",
        "details", "summary"
    )

    fun tagIsListContainer(tagName: String): Boolean = tagName in listOf("ol", "ul")

    companion object {
        val closingTags = listOf(
            "b", "i", "u", "s", "code", "pre", "quote", "color", "bg", "font",
            "size", "h1", "h2", "h3", "h4", "h5", "h6", "normal", "sub", "sup",
            "indent", "url",
            "ol", "ul", "cl", "img", "video", "audio",
            "table", "tr", "td", "th", "hr",
            "mark", "highlight", "small", "kbd", "var", "samp",
            "align", "details", "summary"
        )

        private val default = HtmlTagParser()
        fun parseTag(raw: String, openIdx: Int) = default.parseTag(raw, openIdx)
        fun tagToStyle(tagInfo: TagInfo) = default.tagToStyle(tagInfo)
        fun tagNeedsStyle(tagName: String) = default.tagNeedsStyle(tagName)
        fun tagIsStructural(tagName: String) = default.tagIsStructural(tagName)
        fun tagIsListContainer(tagName: String) = default.tagIsListContainer(tagName)
    }
}
