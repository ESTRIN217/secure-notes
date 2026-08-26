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
import com.example.data.model.BlockType
import com.example.data.model.DataBlock
import com.example.data.model.TableData
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

    fun blocksToPlainText(blocks: List<DataBlock>): String {
        val sb = StringBuilder()
        var numberedCounter = 0
        for (block in blocks) {
            val text = segmentsToPlainText(block.ensureSegments())
            when (block.type) {
                BlockType.TEXT, BlockType.CODE_BLOCK, BlockType.HEADING1, BlockType.HEADING2,
                BlockType.HEADING3, BlockType.HEADING4, BlockType.CALLOUT -> {
                    numberedCounter = 0
                    sb.append(text)
                }
                BlockType.BULLET_LIST -> {
                    numberedCounter = 0
                    sb.append("• ").append(text)
                }
                BlockType.NUMBERED_LIST -> {
                    numberedCounter++
                    sb.append("$numberedCounter. ").append(text)
                }
                BlockType.CHECKLIST_ITEM -> {
                    numberedCounter = 0
                    sb.append(if (block.meta["checked"] == "true") "☑ " else "☐ ").append(text)
                }
                BlockType.QUOTE -> {
                    numberedCounter = 0
                    sb.append("▎ ").append(text)
                }
                BlockType.HORIZONTAL_RULE -> {
                    numberedCounter = 0
                    sb.append("───")
                }
                BlockType.BOOKMARK -> {
                    numberedCounter = 0
                    sb.append(block.content)
                }
                BlockType.TABLE -> {
                    numberedCounter = 0
                    TableData.fromJson(block.meta["table"])?.let { data ->
                        sb.append(segmentsTableToPlainText(data))
                    }
                }
                BlockType.COLLAPSIBLE -> {
                    numberedCounter = 0
                    val summary = block.meta["summary"]?.takeIf { it.isNotBlank() }
                    if (!summary.isNullOrBlank()) sb.append(summary)
                    if (summary.isNullOrBlank().not() && text.isNotBlank()) sb.append('\n')
                    if (text.isNotBlank()) sb.append(text)
                }
                BlockType.IMAGE, BlockType.VIDEO, BlockType.AUDIO, BlockType.DRAWING,
                BlockType.VOICE, BlockType.FILE, BlockType.PAGE -> {
                    numberedCounter = 0
                    sb.append(mediaBlockToPlainText(block))
                }
                else -> numberedCounter = 0
            }
            sb.append('\n')
        }
        return sb.toString().trimEnd('\n')
    }

    private fun segmentsTableToPlainText(data: TableData): String {
        val sb = StringBuilder()
        if (data.headers.isNotEmpty()) {
            sb.append("| ").append(data.headers.joinToString(" | ")).append(" |\n")
            sb.append("| ").append(List(data.headers.size) { "---" }.joinToString(" | ")).append(" |")
        }
        data.rows.forEach { row ->
            sb.append('\n')
            sb.append("| ").append(row.joinToString(" | ")).append(" |")
        }
        return sb.toString()
    }

    private fun mediaLabel(type: BlockType): String = when (type) {
        BlockType.IMAGE -> "Image"
        BlockType.VIDEO -> "Video"
        BlockType.AUDIO -> "Audio"
        BlockType.DRAWING -> "Drawing"
        BlockType.VOICE -> "Voice"
        BlockType.FILE -> "File"
        else -> ""
    }

    private fun mediaBlockToPlainText(block: DataBlock): String {
        val label = mediaLabel(block.type)
        if (label.isEmpty()) return ""
        val name = block.meta["name"]?.takeIf { it.isNotBlank() }
            ?: block.meta["caption"]?.takeIf { it.isNotBlank() }
        return if (name != null) "[$label: $name]" else "[$label]"
    }

    fun blocksToMarkdown(blocks: List<DataBlock>, media: MediaMarkdownResolver? = null): String {
        val sb = StringBuilder()
        for (block in blocks) {
            val segments = block.ensureSegments()
            when (block.type) {
                BlockType.TEXT, BlockType.CALLOUT -> sb.append(segmentsToMarkdown(segments))
                BlockType.HEADING1 -> sb.append("# ").append(segmentsToMarkdown(segments))
                BlockType.HEADING2 -> sb.append("## ").append(segmentsToMarkdown(segments))
                BlockType.HEADING3 -> sb.append("### ").append(segmentsToMarkdown(segments))
                BlockType.HEADING4 -> sb.append("#### ").append(segmentsToMarkdown(segments))
                BlockType.BULLET_LIST -> sb.append("- ").append(segmentsToMarkdown(segments))
                BlockType.NUMBERED_LIST -> sb.append("1. ").append(segmentsToMarkdown(segments))
                BlockType.CHECKLIST_ITEM -> {
                    val mark = if (block.meta["checked"] == "true") "x" else " "
                    sb.append("- [").append(mark).append("] ").append(segmentsToMarkdown(segments))
                }
                BlockType.QUOTE -> sb.append("> ").append(segmentsToMarkdown(segments))
                BlockType.CODE_BLOCK -> {
                    val lang = block.meta["language"]?.takeIf { it.isNotBlank() }
                    sb.append("```").append(lang ?: "").append('\n')
                        .append(segmentsToPlainText(segments)).append("\n```")
                }
                BlockType.HORIZONTAL_RULE -> sb.append("\n---\n")
                BlockType.BOOKMARK -> {
                    val url = block.content
                    val title = block.meta["title"]?.takeIf { it.isNotBlank() } ?: url
                    sb.append("[").append(title).append("](").append(url).append(")")
                }
                BlockType.TABLE -> {
                    TableData.fromJson(block.meta["table"])?.let { data ->
                        sb.append(segmentsTableToMarkdown(data))
                    }
                }
                BlockType.IMAGE, BlockType.DRAWING -> {
                    val src = media?.resolveMedia(block)
                    if (src != null) {
                        val alt = block.meta["name"]?.takeIf { it.isNotBlank() }
                            ?: block.meta["caption"]?.takeIf { it.isNotBlank() }
                            ?: mediaLabel(block.type)
                        sb.append("![").append(alt.replace("]", "\\]"))
                            .append("](").append(src.replace(")", "\\)")).append(")")
                    } else {
                        sb.append(mediaBlockToPlainText(block))
                    }
                }
                BlockType.VIDEO, BlockType.AUDIO, BlockType.VOICE, BlockType.FILE -> {
                    val src = media?.resolveMedia(block)
                    if (src != null) {
                        val name = block.meta["name"]?.takeIf { it.isNotBlank() }
                            ?: block.meta["caption"]?.takeIf { it.isNotBlank() }
                            ?: mediaLabel(block.type)
                        sb.append("[").append(name.replace("]", "\\]"))
                            .append("](").append(src.replace(")", "\\)")).append(")")
                    } else {
                        sb.append(mediaBlockToPlainText(block))
                    }
                }
                BlockType.PAGE, BlockType.PAGE_LINK -> {
                    val label = block.content.ifBlank { "Page" }
                    sb.append("📄 ").append(label)
                }
                BlockType.COLLAPSIBLE -> {
                    val summary = block.meta["summary"]?.takeIf { it.isNotBlank() } ?: "Details"
                    sb.append("<details><summary>").append(summary).append("</summary>")
                        .append(blocksToMarkdown(listOf(block.copy(type = BlockType.TEXT)), media))
                        .append("</details>")
                }
                else -> {}
            }
            sb.append('\n')
        }
        return sb.toString().trim('\n')
    }

    fun interface MediaHtmlResolver {
        /** Devuelve una data URI (o URL passthrough) para embeber el bloque, o null. */
        fun resolveMedia(block: DataBlock): String?
    }

    fun interface MediaMarkdownResolver {
        /** Devuelve el destino del bloque media en Markdown: data URI, ruta relativa `media/...` o URL. null → placeholder. */
        fun resolveMedia(block: DataBlock): String?
    }

    fun blocksToHtml(blocks: List<DataBlock>, media: MediaHtmlResolver? = null): String {
        val sb = StringBuilder()
        for (block in blocks) {
            val segments = block.ensureSegments()
            when (block.type) {
                BlockType.TEXT -> sb.append("<p>").append(segmentsToHtml(segments)).append("</p>")
                BlockType.HEADING1 -> sb.append("<h1>").append(segmentsToHtml(segments)).append("</h1>")
                BlockType.HEADING2 -> sb.append("<h2>").append(segmentsToHtml(segments)).append("</h2>")
                BlockType.HEADING3 -> sb.append("<h3>").append(segmentsToHtml(segments)).append("</h3>")
                BlockType.HEADING4 -> sb.append("<h4>").append(segmentsToHtml(segments)).append("</h4>")
                BlockType.BULLET_LIST -> sb.append("<ul><li>").append(segmentsToHtml(segments)).append("</li></ul>")
                BlockType.NUMBERED_LIST -> sb.append("<ol><li>").append(segmentsToHtml(segments)).append("</li></ol>")
                BlockType.CHECKLIST_ITEM -> {
                    val isChecked = block.meta["checked"] == "true"
                    val checkAttr = if (isChecked) " checked" else ""
                    sb.append("<ul class=\"checklist\"><li data-checked=\"").append(if (isChecked) "checked" else "").append("\">")
                        .append("<input type=\"checkbox\"").append(checkAttr).append(">")
                        .append(segmentsToHtml(segments)).append("</li></ul>")
                }
                BlockType.QUOTE -> sb.append("<blockquote>").append(segmentsToHtml(segments)).append("</blockquote>")
                BlockType.CALLOUT -> sb.append("<p class=\"callout\">💡 ").append(segmentsToHtml(segments)).append("</p>")
                BlockType.CODE_BLOCK -> sb.append("<pre><code>").append(htmlEscape(segmentsToPlainText(segments))).append("</code></pre>")
                BlockType.HORIZONTAL_RULE -> sb.append("<hr>")
                BlockType.BOOKMARK -> {
                    val url = block.content
                    val title = block.meta["title"]?.takeIf { it.isNotBlank() } ?: url
                    sb.append("<p class=\"bookmark\"><a href=\"").append(htmlEscape(url)).append("\">").append(htmlEscape(title)).append("</a></p>")
                }
                BlockType.TABLE -> {
                    TableData.fromJson(block.meta["table"])?.let { data ->
                        sb.append(data.toHtml())
                    }
                }
                BlockType.COLLAPSIBLE -> {
                    sb.append("<details><summary>").append(htmlEscape(block.meta["summary"] ?: "")).append("</summary>")
                        .append(blocksToHtml(listOf(block.copy(type = BlockType.TEXT)), media)).append("</details>")
                }
                BlockType.IMAGE, BlockType.VIDEO, BlockType.AUDIO, BlockType.DRAWING,
                BlockType.VOICE, BlockType.FILE -> {
                    sb.append(mediaBlockToHtml(block, media))
                }
                BlockType.PAGE, BlockType.PAGE_LINK -> {
                  val label = block.content.ifBlank { "Page" }
                  sb.append("<p><span style=\"font-weight:600;color:#1565c0;\">🔗 ").append(htmlEscape(label)).append("</span></p>")
                }
                else -> {}
            }
            sb.append('\n')
        }
        return sb.toString()
    }

    private fun mediaBlockToHtml(block: DataBlock, media: MediaHtmlResolver?): String {
        val caption = block.meta["caption"]?.takeIf { it.isNotBlank() }
        val label = when (block.type) {
            BlockType.IMAGE -> "Image"
            BlockType.VIDEO -> "Video"
            BlockType.AUDIO -> "Audio"
            BlockType.VOICE -> "Voice"
            BlockType.DRAWING -> "Drawing"
            BlockType.FILE -> "File"
            else -> ""
        }
        val name = block.meta["name"]?.takeIf { it.isNotBlank() }
            ?: caption

        val body: String = when (block.type) {
            BlockType.VIDEO -> {
                if (VideoUrlHelper.isYouTubeUrl(block.content)) {
                    "<p class=\"video-link\"><a href=\"${htmlEscape(block.content)}\">▶ Video</a></p>"
                } else {
                    val src = media?.resolveMedia(block)
                    if (src != null) "<video controls src=\"${htmlEscape(src)}\"></video>"
                    else htmlEscape(name?.let { "[$label: $it]" } ?: "[$label]")
                }
            }
            BlockType.IMAGE, BlockType.DRAWING -> {
                val src = media?.resolveMedia(block)
                if (src != null) {
                    val align = block.meta["align"]
                    val cls = if (align == "left" || align == "right") " class=\"float-$align\"" else ""
                    "<img src=\"${htmlEscape(src)}\" alt=\"${htmlEscape(name ?: label)}\"$cls>"
                } else {
                    htmlEscape(name?.let { "[$label: $it]" } ?: "[$label]")
                }
            }
            BlockType.AUDIO, BlockType.VOICE -> {
                val src = media?.resolveMedia(block)
                if (src != null) "<audio controls src=\"${htmlEscape(src)}\"></audio>"
                else htmlEscape(name?.let { "[$label: $it]" } ?: "[$label]")
            }
            BlockType.FILE -> {
                val src = media?.resolveMedia(block)
                if (src != null) {
                    "<p class=\"file-link\"><a href=\"${htmlEscape(src)}\" download=\"${htmlEscape(name ?: "file")}\">📎 ${htmlEscape(name ?: "File")}</a></p>"
                } else {
                    htmlEscape(name?.let { "[$label: $it]" } ?: "[$label]")
                }
            }
            else -> ""
        }

        return if (!caption.isNullOrBlank() && body.startsWith("<")) {
            "<figure>$body<figcaption>${htmlEscape(caption)}</figcaption></figure>"
        } else {
            body
        }
    }

    fun contentToPlainText(raw: String): String {
        val blocks = contentToBlocks(raw)
        if (blocks != null) return blocksToPlainText(blocks)
        return legacyMarkupToPlainText(raw)
    }

    /** Convierte markup legacy a texto plano conservando checklists, reglas y media. */
    private fun legacyMarkupToPlainText(raw: String): String {
        val summaryRegex = Regex("""<summary>([\s\S]*?)</summary>""", RegexOption.DOT_MATCHES_ALL)
        val text = raw
            .replace(Regex("""<item\s+checked="true">([\s\S]*?)</item>""")) { "☑ ${it.groupValues[1].trim()}" }
            .replace(Regex("""<item\s+checked="false">([\s\S]*?)</item>""")) { "☐ ${it.groupValues[1].trim()}" }
            .replace(Regex("""<item>([\s\S]*?)</item>""")) { "☐ ${it.groupValues[1].trim()}" }
            .replace(Regex("""\n*<hr\s*/?>\n*"""), "\n───\n")
            .replace(Regex("""!audio\s*\[[^\]]*\]\([^\)]+\)"""), "[Audio]")
            .replace(Regex("""!video\s*\[[^\]]*\]\([^\)]+\)"""), "[Video]")
            .replace(Regex("""!\[[^\]]*\]\([^\)]+\)"""), "[Image]")
            .replace(Regex("""<img\s+src="[^"]*"\s*/>|<img=[^>]+>"""), "[Image]")
            .replace(Regex("""<video[^>]*>"""), "[Video]")
            .replace(Regex("""<audio[^>]*>"""), "[Audio]")
            .replace(Regex("""<details>([\s\S]*?)</details>""")) { m ->
                val inner = m.groupValues[1]
                val summary = summaryRegex.find(inner)?.groupValues?.get(1)?.trim() ?: ""
                val body = inner.replace(summaryRegex, "").trim()
                buildString {
                    if (summary.isNotBlank()) append(summary)
                    if (summary.isNotBlank() && body.isNotBlank()) append('\n')
                    if (body.isNotBlank()) append(body)
                }
            }
        return segmentsToPlainText(markupToSegments(text))
    }

    fun contentToMarkdown(raw: String, media: MediaMarkdownResolver? = null): String {
        val blocks = contentToBlocks(raw)
        if (blocks != null) return blocksToMarkdown(blocks, media)
        return try {
            val migrated = DataBlock.migrateLegacyContent(raw)
            val md = blocksToMarkdown(migrated, media)
            if (md.isNotBlank() || raw.isBlank()) md else segmentsToMarkdown(markupToSegments(raw))
        } catch (e: Exception) {
            segmentsToMarkdown(markupToSegments(raw))
        }
    }

    fun contentToHtml(raw: String, media: MediaHtmlResolver? = null): String {
        val blocks = contentToBlocks(raw)
        if (blocks != null) return blocksToHtml(blocks, media)
        return try {
            val migrated = DataBlock.migrateLegacyContent(raw)
            val html = blocksToHtml(migrated, media)
            if (html.isNotBlank() || raw.isBlank()) html else segmentsToHtml(markupToSegments(raw))
        } catch (e: Exception) {
            segmentsToHtml(markupToSegments(raw))
        }
    }

    /** Deserializa bloques ignorando el sufijo `---Attachments---`. */
    fun contentToBlocks(raw: String): List<DataBlock>? {
        val textPart = com.example.data.model.parseNoteContentAndAttachments(raw).first
        return DataBlock.deserialize(textPart)
    }

    private fun segmentsTableToMarkdown(data: TableData): String {
        val sb = StringBuilder()
        val headers = data.headers
        val rows = data.rows
        if (headers.isNotEmpty()) {
            sb.append("| ").append(headers.joinToString(" | ")).append(" |\n")
            sb.append("| ").append(List(headers.size) { "---" }.joinToString(" | ")).append(" |\n")
        }
        rows.forEach { row ->
            sb.append("| ").append(row.joinToString(" | ")).append(" |\n")
        }
        return sb.toString()
    }

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
            if (seg.linkUrl != null) {
              if (seg.isNoteLink) {
                sb.append("<span style=\"font-weight:600;color:#1565c0;\">").append(inner).append("</span>")
              } else {
                sb.append("<a href=\"${htmlEscape(seg.linkUrl)}\">").append(inner).append("</a>")
              }
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
