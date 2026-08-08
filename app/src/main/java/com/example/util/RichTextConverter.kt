package com.example.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import com.example.data.model.TextBaseline
import com.example.data.model.TextSegment

object RichTextConverter {

    const val URL_ANNOTATION = "URL"
    const val EQ_ANNOTATION = "EQ"

    private val escapeable = setOf('\\', '`', '*', '_', '~', '[', ']', '(', ')', '<', '>', '#', '-', '!', '|', '{', '}')
    private val inlineStyleTokens = setOf("b", "i", "u", "s", "code", "sub", "sup", "color", "bg", "font", "size", "url", "mark", "highlight", "var", "kbd", "samp", "small")
    private val markdownLinkRegex = Regex("^\\[([^\\]]*)\\]\\(([^\\)]+)\\)")
    private val autoLinkRegex = Regex("^https?://[^\\s<>\"'(){}|\\\\^`\\[\\]]+")
    private val markupTagRegex = Regex(
        "</?(b|i|u|s|code|pre|sub|sup|color|bg|font|size|url|eq|mark|highlight|indent)\\b[^>]*>",
        RegexOption.IGNORE_CASE
    )

    // ── Markup (legacy) → segments ─────────────────────────────────────────

    private class StyleToken(val type: String, val value: String? = null)

    fun markupToSegments(markup: String): List<TextSegment> {
        val out = mutableListOf<TextSegment>()
        val stack = ArrayDeque<StyleToken>()
        val plain = StringBuilder()
        var i = 0
        val n = markup.length

        fun flush() {
            if (plain.isEmpty()) return
            out.add(buildSegment(plain.toString(), stack))
            plain.setLength(0)
        }

        while (i < n) {
            val char = markup[i]

            if (char == '\\' && i + 1 < n && markup[i + 1] in escapeable) {
                plain.append(markup[i + 1])
                i += 2
                continue
            }

            if (char == '<') {
                val tag = HtmlTagParser.parseTag(markup, i)
                if (tag != null) {
                    when {
                        !tag.isClosing && tag.tagName == "eq" -> {
                            val closeIdx = markup.indexOf("</eq>", tag.endIndex)
                            val closeEnd = if (closeIdx == -1) tag.endIndex else closeIdx + "</eq>".length
                            val latex = if (closeIdx == -1) "" else markup.substring(tag.endIndex, closeIdx)
                            flush()
                            val rendered = MathRenderer.render(latex).text
                            out.add(TextSegment(text = rendered, equationLatex = latex))
                            i = closeEnd
                            continue
                        }
                        tag.isClosing -> {
                            val idx = stack.indexOfLast { it.type == tag.tagName }
                            if (idx != -1) {
                                flush()
                                stack.removeAt(idx)
                            }
                            i = tag.endIndex
                            continue
                        }
                        tag.tagName in inlineStyleTokens -> {
                            flush()
                            stack.addLast(StyleToken(tag.tagName, tag.value))
                            i = tag.endIndex
                            continue
                        }
                        tag.tagName == "indent" -> {
                            plain.append("    ")
                            i = tag.endIndex
                            continue
                        }
                        else -> {
                            i = tag.endIndex
                            continue
                        }
                    }
                }
                plain.append(char)
                i++
                continue
            }

            val markdownResult = parseMarkdownInline(markup, i, stack, plain, out)
            if (markdownResult != null) {
                i = markdownResult
                continue
            }

            plain.append(char)
            i++
        }
        flush()
        return mergeAdjacent(out)
    }

    private fun parseMarkdownInline(
        markup: String,
        i: Int,
        stack: ArrayDeque<StyleToken>,
        plain: StringBuilder,
        out: MutableList<TextSegment>
    ): Int? {
        fun flush() {
            if (plain.isEmpty()) return
            out.add(buildSegment(plain.toString(), stack))
            plain.setLength(0)
        }

        fun toggle(type: String, value: String? = null): Int {
            flush()
            val idx = stack.indexOfLast { it.type == type }
            if (idx != -1) stack.removeAt(idx) else stack.addLast(StyleToken(type, value))
            return 0
        }

        val linkMatch = markdownLinkRegex.find(markup.substring(i))
        if (linkMatch != null) {
            flush()
            out.add(TextSegment(text = linkMatch.groupValues[1], linkUrl = linkMatch.groupValues[2]))
            return i + linkMatch.value.length
        }

        if (markup.startsWith("**", i) || markup.startsWith("__", i)) {
            toggle("b"); return i + 2
        }
        if (markup.startsWith("~~", i)) {
            toggle("s"); return i + 2
        }
        if (markup[i] == '*' || markup[i] == '_') {
            toggle("i"); return i + 1
        }
        if (markup[i] == '`') {
            toggle("code"); return i + 1
        }

        val autoLinkMatch = autoLinkRegex.find(markup.substring(i))
        if (autoLinkMatch != null) {
            flush()
            val url = autoLinkMatch.value
            out.add(TextSegment(text = url, linkUrl = url))
            return i + url.length
        }

        return null
    }

    private fun buildSegment(text: String, stack: List<StyleToken>): TextSegment {
        var bold = false
        var italic = false
        var underline = false
        var strike = false
        var code = false
        var color: String? = null
        var bg: String? = null
        var font: String? = null
        var size: Float? = null
        var baseline = TextBaseline.NORMAL
        var url: String? = null

        for (token in stack) {
            when (token.type) {
                "b" -> bold = true
                "i" -> italic = true
                "u" -> underline = true
                "s" -> strike = true
                "code" -> code = true
                "sub" -> baseline = TextBaseline.SUBSCRIPT
                "sup" -> baseline = TextBaseline.SUPERSCRIPT
                "color" -> color = color ?: normalizeColor(token.value)
                "bg" -> bg = bg ?: normalizeColor(token.value)
                "mark" -> bg = bg ?: "FFEB3B"
                "highlight" -> bg = bg ?: (normalizeColor(token.value) ?: "FFEB3B")
                "font" -> font = font ?: token.value?.trim()?.removeSurrounding("\"")?.removeSurrounding("'")
                "size" -> size = size ?: token.value?.trim()?.removeSurrounding("\"")?.removeSurrounding("'")?.toFloatOrNull()
                "url" -> url = url ?: token.value
                else -> {}
            }
        }
        return TextSegment(
            text = text,
            bold = bold,
            italic = italic,
            underline = underline,
            strikethrough = strike,
            code = code,
            colorHex = color,
            bgColorHex = bg,
            fontFamily = font,
            fontSizeSp = size,
            baseline = baseline,
            linkUrl = url
        )
    }

    fun containsMarkup(text: String): Boolean =
        markupTagRegex.containsMatchIn(text) || "**" in text || "~~" in text || '`' in text

    fun toPlainText(markup: String): String = segmentsToPlainText(markupToSegments(markup))

    // ── Segments → AnnotatedString ─────────────────────────────────────────

    fun segmentsToAnnotatedString(segments: List<TextSegment>): AnnotatedString {
        val builder = AnnotatedString.Builder()
        for (seg in segments) {
            val start = builder.length
            val display = if (seg.equationLatex != null) MathRenderer.render(seg.equationLatex).text else seg.text
            if (display.isEmpty()) continue
            builder.append(display)
            if (seg.equationLatex != null) {
                builder.addStringAnnotation(EQ_ANNOTATION, seg.equationLatex, start, builder.length)
            }
            if (seg.linkUrl != null) {
                builder.addStringAnnotation(URL_ANNOTATION, seg.linkUrl, start, builder.length)
            }
            val style = seg.toSpanStyle()
            builder.addStyle(style, start, builder.length)
        }
        return builder.toAnnotatedString()
    }

    // ── AnnotatedString → segments ─────────────────────────────────────────

    fun annotatedStringToSegments(annotated: AnnotatedString): List<TextSegment> {
        val text = annotated.text
        val boundaries = sortedSetOf(0, text.length)
        for (span in annotated.spanStyles) {
            boundaries.add(span.start)
            boundaries.add(span.end)
        }
        for (ann in annotated.getStringAnnotations(URL_ANNOTATION, 0, text.length)) {
            boundaries.add(ann.start)
            boundaries.add(ann.end)
        }
        for (ann in annotated.getStringAnnotations(EQ_ANNOTATION, 0, text.length)) {
            boundaries.add(ann.start)
            boundaries.add(ann.end)
        }

        val points = boundaries.toList()
        val out = mutableListOf<TextSegment>()
        for (k in 0 until points.size - 1) {
            val start = points[k]
            val end = points[k + 1]
            if (start >= end) continue
            val chunk = text.substring(start, end)
            val styles = annotated.spanStyles.filter { it.start <= start && it.end >= end }.map { it.item }
            val url = annotated.getStringAnnotations(URL_ANNOTATION, start, end)
                .firstOrNull { it.start <= start && it.end >= end }?.item
            val eq = annotated.getStringAnnotations(EQ_ANNOTATION, start, end)
                .firstOrNull { it.start <= start && it.end >= end }?.item
            out.add(spanStylesToSegment(chunk, styles, url, eq))
        }
        return mergeAdjacent(out)
    }

    private fun spanStylesToSegment(
        chunk: String,
        styles: List<SpanStyle>,
        url: String?,
        eq: String?
    ): TextSegment {
        var bold = false
        var italic = false
        var underline = false
        var strike = false
        var code = false
        var color: String? = null
        var bg: String? = null
        var font: String? = null
        var size: Float? = null
        var baseline = TextBaseline.NORMAL

        for (style in styles) {
            if (style.fontWeight == FontWeight.Bold) bold = true
            if (style.fontStyle == FontStyle.Italic) italic = true
            if (style.textDecoration?.contains(TextDecoration.Underline) == true) underline = true
            if (style.textDecoration?.contains(TextDecoration.LineThrough) == true) strike = true
            if (style.fontFamily != null && style.background == Color(0x1F808080)) code = true
            if (style.color != Color.Unspecified) {
                color = color ?: colorToHex(style.color)
            }
            if (!code &&
                style.background != Color.Unspecified &&
                style.background != Color.Transparent &&
                style.background != Color(0x1F808080)
            ) {
                bg = bg ?: colorToHex(style.background)
            }
            style.fontFamily?.let { family ->
                val name = TextSegment.fontFamilyName(family)
                if (name != null && name != "monospace") font = font ?: name
            }
            if (style.fontSize != TextUnit.Unspecified) size = size ?: style.fontSize.value
            when (style.baselineShift) {
                BaselineShift.Subscript -> baseline = TextBaseline.SUBSCRIPT
                BaselineShift.Superscript -> baseline = TextBaseline.SUPERSCRIPT
                else -> {}
            }
        }

        if (code) {
            color = null
            bg = null
            font = null
            size = null
        }

        return TextSegment(
            text = chunk,
            bold = bold,
            italic = italic,
            underline = underline,
            strikethrough = strike,
            code = code,
            colorHex = color,
            bgColorHex = bg,
            fontFamily = font,
            fontSizeSp = size,
            baseline = baseline,
            linkUrl = url,
            equationLatex = eq
        )
    }

    private fun mergeAdjacent(segments: List<TextSegment>): List<TextSegment> {
        if (segments.size < 2) return segments
        val out = mutableListOf<TextSegment>()
        for (seg in segments) {
            val last = out.lastOrNull()
            if (last != null &&
                seg.equationLatex == null &&
                last.equationLatex == null &&
                last.hasSameStyle(seg)
            ) {
                out[out.lastIndex] = last.copy(text = last.text + seg.text)
            } else {
                out.add(seg)
            }
        }
        return out
    }

    // ── Editing helpers ────────────────────────────────────────────────────

    fun parseResultFor(segments: List<TextSegment>): RichTextParser.ParseResult {
        val annotated = segmentsToAnnotatedString(segments)
        val len = annotated.text.length
        val stt = IntArray(len + 1) { it }
        val tts = IntArray(len + 1) { it }
        return RichTextParser.ParseResult(annotated, stt, tts, emptyList())
    }

    fun segmentsJson(segments: List<TextSegment>): String = TextSegment.serialize(segments)

    fun applySpanStyle(
        segments: List<TextSegment>,
        start: Int,
        end: Int,
        transform: (TextSegment) -> TextSegment
    ): List<TextSegment> {
        if (start >= end || segments.isEmpty()) return segments
        val out = mutableListOf<TextSegment>()
        var cursor = 0
        for (seg in segments) {
            val segStart = cursor
            val segEnd = cursor + seg.text.length
            if (segEnd <= start || segStart >= end) {
                out.add(seg)
            } else if (segStart >= start && segEnd <= end) {
                out.add(transform(seg))
            } else {
                val leftLen = (start - segStart).coerceIn(0, seg.text.length)
                val rightStart = (end - segStart).coerceIn(0, seg.text.length)
                if (leftLen > 0) out.add(seg.copy(text = seg.text.substring(0, leftLen)))
                if (leftLen < rightStart) {
                    out.add(transform(seg.copy(text = seg.text.substring(leftLen, rightStart))))
                }
                if (rightStart < seg.text.length) out.add(seg.copy(text = seg.text.substring(rightStart)))
            }
            cursor = segEnd
        }
        return mergeAdjacent(out)
    }

    fun replaceTextRange(
        segments: List<TextSegment>,
        start: Int,
        end: Int,
        newText: String
    ): List<TextSegment> {
        if (start >= end && newText.isEmpty()) return segments
        val (left, _) = splitSegmentsAt(segments, start)
        val (_, right) = splitSegmentsAt(segments, end)
        val insert = if (newText.isEmpty()) {
            emptyList()
        } else {
            val inherited = left.lastOrNull() ?: right.firstOrNull() ?: TextSegment()
            listOf(
                inherited.copy(
                    text = newText,
                    equationLatex = null,
                    linkUrl = null
                )
            )
        }
        return mergeAdjacent(left + insert + right)
    }

    fun insertSegments(
        segments: List<TextSegment>,
        start: Int,
        end: Int,
        insert: List<TextSegment>
    ): List<TextSegment> {
        if (start >= end && insert.isEmpty()) return segments
        val (left, _) = splitSegmentsAt(segments, start)
        val (_, right) = splitSegmentsAt(segments, end)
        return mergeAdjacent(left + insert + right)
    }

    fun rangeSegments(segments: List<TextSegment>, start: Int, end: Int): List<TextSegment> {
        val out = mutableListOf<TextSegment>()
        var cursor = 0
        for (seg in segments) {
            val segEnd = cursor + seg.text.length
            if (segEnd > start && cursor < end) out.add(seg)
            cursor = segEnd
        }
        return out
    }

    private fun splitSegmentsAt(
        segments: List<TextSegment>,
        offset: Int
    ): Pair<List<TextSegment>, List<TextSegment>> {
        if (offset <= 0) return emptyList<TextSegment>() to segments
        val left = mutableListOf<TextSegment>()
        var cursor = 0
        for (seg in segments) {
            val segEnd = cursor + seg.text.length
            if (segEnd <= offset) {
                left.add(seg)
            } else {
                val mid = (offset - cursor).coerceIn(0, seg.text.length)
                val rightSeg = if (mid < seg.text.length) {
                    listOf(seg.copy(text = seg.text.substring(mid)))
                } else {
                    emptyList()
                }
                if (mid > 0) left.add(seg.copy(text = seg.text.substring(0, mid)))
                return left to rightSeg
            }
            cursor = segEnd
        }
        return left to emptyList()
    }

    // ── Export helpers ─────────────────────────────────────────────────────

    fun segmentsToPlainText(segments: List<TextSegment>): String =
        segments.joinToString("") { it.plainText }

    fun segmentsToMarkdown(segments: List<TextSegment>): String {
        val sb = StringBuilder()
        for (seg in segments) {
            if (seg.equationLatex != null) {
                sb.append("$").append(seg.equationLatex).append("$")
                continue
            }
            var text = markdownEscape(seg.text)
            if (seg.code) text = "`$text`"
            if (seg.bold) text = "**$text**"
            if (seg.italic) text = "*$text*"
            if (seg.strikethrough) text = "~~$text~~"
            if (seg.underline) text = "<ins>$text</ins>"
            when (seg.baseline) {
                TextBaseline.SUBSCRIPT -> text = "~$text~"
                TextBaseline.SUPERSCRIPT -> text = "^$text^"
                else -> {}
            }
            if (seg.linkUrl != null) text = "[$text](${seg.linkUrl})"
            sb.append(text)
        }
        return sb.toString()
    }

    fun segmentsToHtml(segments: List<TextSegment>): String {
        val sb = StringBuilder()
        for (seg in segments) {
            val inner = if (seg.equationLatex != null) {
                htmlEscape(MathRenderer.render(seg.equationLatex).text)
            } else {
                htmlEscape(seg.text)
            }
            val linkHref = seg.linkUrl?.let { " href=\"${htmlEscape(it)}\"" } ?: ""
            if (seg.linkUrl != null) {
                sb.append("<a$linkHref>").append(inner).append("</a>")
            } else {
                val styles = mutableListOf<String>()
                if (!seg.code) {
                    seg.colorHex?.let { JsonColorizer.parseColor(it)?.let { c -> styles.add("color:${toCssColor(c)}") } }
                    seg.bgColorHex?.let { JsonColorizer.parseColor(it)?.let { c -> styles.add("background-color:${toCssColor(c)}") } }
                }
                seg.fontFamily?.let { styles.add("font-family:${cssFont(it)}") }
                seg.fontSizeSp?.let { styles.add("font-size:${it}px") }
                if (seg.bold) styles.add("font-weight:bold")
                if (seg.italic) styles.add("font-style:italic")
                val decorations = buildList {
                    if (seg.underline) add("underline")
                    if (seg.strikethrough) add("line-through")
                }
                if (decorations.isNotEmpty()) styles.add("text-decoration:${decorations.joinToString(" ")}")
                if (seg.code) {
                    styles.add("font-family:monospace")
                    styles.add("background-color:rgb(31 128 128 / 0.12)")
                }
                when (seg.baseline) {
                    TextBaseline.SUBSCRIPT -> styles.add("vertical-align:sub;font-size:smaller")
                    TextBaseline.SUPERSCRIPT -> styles.add("vertical-align:super;font-size:smaller")
                    else -> {}
                }
                if (styles.isEmpty()) {
                    sb.append(inner)
                } else {
                    sb.append("<span style=\"").append(styles.joinToString("; ")).append("\">").append(inner).append("</span>")
                }
            }
        }
        return sb.toString()
    }

    fun segmentsToMarkup(segments: List<TextSegment>): String {
        val sb = StringBuilder()
        for (seg in segments) {
            if (seg.equationLatex != null) {
                sb.append("<eq>").append(seg.equationLatex).append("</eq>")
                continue
            }
            val open = StringBuilder()
            val close = StringBuilder()
            fun wrap(tag: String) {
                open.append("<$tag>")
                close.insert(0, "</$tag>")
            }
            fun wrapVal(tag: String, value: String) {
                open.append("<$tag=$value>")
                close.insert(0, "</$tag>")
            }
            seg.linkUrl?.let { wrapVal("url", it) }
            if (seg.baseline == TextBaseline.SUBSCRIPT) wrap("sub")
            if (seg.baseline == TextBaseline.SUPERSCRIPT) wrap("sup")
            seg.colorHex?.let { wrapVal("color", it) }
            seg.bgColorHex?.let { wrapVal("bg", it) }
            seg.fontFamily?.let { wrapVal("font", it) }
            seg.fontSizeSp?.let { wrapVal("size", it.toInt().toString()) }
            if (seg.bold) wrap("b")
            if (seg.italic) wrap("i")
            if (seg.underline) wrap("u")
            if (seg.strikethrough) wrap("s")
            if (seg.code) wrap("code")
            sb.append(open).append(markupEscape(seg.text)).append(close)
        }
        return sb.toString()
    }

    private fun markupEscape(text: String): String = buildString {
        for (c in text) {
            if (c in markupEscapeSet) append('\\')
            append(c)
        }
    }

    private val markupEscapeSet = setOf('\\', '`', '*', '_', '~', '[', ']', '(', ')', '<', '>')

    // ── Color / escaping helpers ───────────────────────────────────────────

    fun colorToHex(color: Color): String {
        val r = (color.red * 255).toInt().coerceIn(0, 255)
        val g = (color.green * 255).toInt().coerceIn(0, 255)
        val b = (color.blue * 255).toInt().coerceIn(0, 255)
        return String.format("#%02X%02X%02X", r, g, b)
    }

    fun normalizeColorValue(value: String?): String? {
        val cleaned = value?.trim()?.removeSurrounding("\"")?.removeSurrounding("'") ?: return null
        if (cleaned.startsWith("#") && cleaned.length in 7..9) return cleaned
        val color = JsonColorizer.parseColor(cleaned) ?: return null
        return colorToHex(color)
    }

    private fun normalizeColor(value: String?): String? {
        val cleaned = value?.trim()?.removeSurrounding("\"")?.removeSurrounding("'") ?: return null
        if (cleaned.startsWith("#") && cleaned.length in 7..9) return cleaned
        val color = JsonColorizer.parseColor(cleaned) ?: return null
        return colorToHex(color)
    }

    private fun toCssColor(color: Color): String {
        val r = (color.red * 255).toInt().coerceIn(0, 255)
        val g = (color.green * 255).toInt().coerceIn(0, 255)
        val b = (color.blue * 255).toInt().coerceIn(0, 255)
        return "rgb($r, $g, $b)"
    }

    private fun cssFont(name: String): String = when (name.lowercase()) {
        "serif" -> "serif"
        "monospace" -> "monospace"
        "sans-serif" -> "sans-serif"
        "cursive" -> "cursive"
        else -> "sans-serif"
    }

    private fun htmlEscape(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    private fun markdownEscape(text: String): String = text
        .replace("\\", "\\\\")
        .replace("_", "\\_")
        .replace("*", "\\*")
        .replace("`", "\\`")
}
