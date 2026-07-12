package com.example.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color

class RichTextParser {

    private data class ActiveStyle(val type: String, val style: SpanStyle, val start: Int, val annotation: String? = null)
    
    class ParseResult(
        val text: AnnotatedString,
        sourceToTransformed: IntArray,
        transformedToSource: IntArray
    ) {
        private val stt = sourceToTransformed
        private val tts = transformedToSource

        fun originalToTransformed(originalIndex: Int): Int {
            return stt.getOrElse(originalIndex) { originalIndex.coerceIn(0, stt.lastIndex) }
        }

        fun transformedToOriginal(transformedIndex: Int): Int {
            return tts.getOrElse(transformedIndex) { transformedIndex.coerceIn(0, tts.lastIndex) }
        }
    }

    fun parseWithMapping(rawText: String, hideTags: Boolean, showTagsGray: Boolean = false): ParseResult {
        val N = rawText.length
        
        if (JsonColorizer.isJson(rawText)) {
            val builder = AnnotatedString.Builder()
            builder.append(rawText)
            JsonColorizer.highlightJson(rawText, builder)
            val identityMapping = IntArray(N + 1) { it }
            return ParseResult(builder.toAnnotatedString(), identityMapping, identityMapping)
        }

        val builder = AnnotatedString.Builder()
        val offsetMapper = OffsetMapper()
        val mapping = offsetMapper.createMapping(N)
        val activeStyles = mutableListOf<ActiveStyle>()
        val olIndexStack = mutableListOf<Int>()
        val ulStack = mutableListOf<Boolean>()
        val listContainerStack = mutableListOf<String>()

        var i = 0
        var isLineStart = true

        while (i < N) {
            val char = rawText[i]

            if (isLineStart && (hideTags || showTagsGray)) {
                val result = parseLineStartMarkers(rawText, i, builder, mapping,
                    hideTags, showTagsGray, activeStyles,
                    olIndexStack, ulStack, listContainerStack)
                if (result != null) {
                    i = result
                    isLineStart = false
                    continue
                }
            }

            if (char == '<' && (hideTags || showTagsGray)) {
                val tagInfo = HtmlTagParser.parseTag(rawText, i)
                if (tagInfo != null) {
                    i = handleHtmlTag(tagInfo, builder, mapping, rawText, hideTags, showTagsGray, activeStyles,
                        olIndexStack, ulStack, listContainerStack)
                    continue
                }
            }

            if (hideTags || showTagsGray) {
                val markdownResult = parseMarkdownSyntax(rawText, i, builder, mapping, hideTags, showTagsGray, activeStyles)
                if (markdownResult != null) {
                    i = markdownResult
                    continue
                }
            }

            appendChar(rawText[i], builder, mapping, i)

            if (char == '\n') {
                isLineStart = true
                if (hideTags || showTagsGray) {
                    endStyle(activeStyles, "h1", builder)
                    endStyle(activeStyles, "h2", builder)
                    endStyle(activeStyles, "h3", builder)
                    endStyle(activeStyles, "quote", builder)
                }
            } else {
                isLineStart = false
            }
            i++
        }

        val finalMapping = offsetMapper.finalize(mapping, builder.length, N)

        for (active in activeStyles.reversed()) {
            builder.addStyle(active.style, active.start, builder.length)
        }

        return ParseResult(builder.toAnnotatedString(), finalMapping.sourceToTransformed, finalMapping.transformedToSource)
    }

    private fun parseLineStartMarkers(
        rawText: String, i: Int, builder: AnnotatedString.Builder, mapping: OffsetMapper.MappingArrays,
        hideTags: Boolean, showTagsGray: Boolean, activeStyles: MutableList<ActiveStyle>,
        olIndexStack: MutableList<Int>, ulStack: MutableList<Boolean>, listContainerStack: MutableList<String>
    ): Int? {
        data class LineMarker(val prefix: String, val style: SpanStyle?, val type: String, val replacement: String? = null)
        val lineMarkers = listOf(
            LineMarker("### ", SpanStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFFE65100)), "h3"),
            LineMarker("## ", SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFFF57C00)), "h2"),
            LineMarker("# ", SpanStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color(0xFFFB8C00)), "h1"),
            LineMarker("> ", SpanStyle(fontStyle = FontStyle.Italic, color = Color(0xFF546E7A)), "quote"),
            LineMarker("- [ ] ", null, "unchecked", if (showTagsGray) null else "☐ "),
            LineMarker("* [ ] ", null, "unchecked", if (showTagsGray) null else "☐ "),
            LineMarker("- [x] ", null, "checked", if (showTagsGray) null else "☑ "),
            LineMarker("* [x] ", null, "checked", if (showTagsGray) null else "☑ "),
            LineMarker("- [X] ", null, "checked", if (showTagsGray) null else "☑ "),
            LineMarker("* [X] ", null, "checked", if (showTagsGray) null else "☑ "),
            LineMarker("- ", null, "list", if (showTagsGray) null else "• "),
            LineMarker("* ", null, "list", if (showTagsGray) null else "• ")
        )

        for (marker in lineMarkers) {
            if (rawText.startsWith(marker.prefix, i)) {
                val tagEnd = i + marker.prefix.length
                if (marker.replacement != null) {
                    skipOrGrayTagChars(rawText, builder, mapping, i, tagEnd, showTagsGray)
                    OffsetMapper.addChar(mapping, i, builder.length)
                    builder.append(marker.replacement)
                } else {
                    skipOrGrayTagChars(rawText, builder, mapping, i, tagEnd, showTagsGray)
                }
                if (marker.style != null) {
                    startStyle(activeStyles, marker.type, marker.style, builder)
                }
                return tagEnd
            }
        }

        val numListMatch = Regex("^\\d+\\.\\s+").find(rawText.substring(i))
        if (numListMatch != null) {
            val tagLen = numListMatch.value.length
            val tagEnd = i + tagLen
            if (showTagsGray) {
                skipOrGrayTagChars(rawText, builder, mapping, i, tagEnd, showTagsGray)
            } else {
                skipOrGrayTagChars(rawText, builder, mapping, i, tagEnd, showTagsGray)
                OffsetMapper.addChar(mapping, i, builder.length)
                builder.append(numListMatch.value)
            }
            return tagEnd
        }

        return null
    }

    private fun handleHtmlTag(
        tagInfo: HtmlTagParser.TagInfo,
        builder: AnnotatedString.Builder,
        mapping: OffsetMapper.MappingArrays,
        rawText: String,
        hideTags: Boolean,
        showTagsGray: Boolean,
        activeStyles: MutableList<ActiveStyle>,
        olIndexStack: MutableList<Int>,
        ulStack: MutableList<Boolean>,
        listContainerStack: MutableList<String>
    ): Int {
        val tagName = tagInfo.tagName
        val tagValue = tagInfo.value
        val tagEnd = tagInfo.endIndex

        if (tagInfo.isClosing) {
            endStyle(activeStyles, tagName, builder)
            if (tagName == "ol") {
                olIndexStack.removeLastOrNull()
                val idx = listContainerStack.indexOfLast { it == "ol" }
                if (idx != -1) listContainerStack.removeAt(idx)
            }
            if (tagName == "ul") {
                ulStack.removeLastOrNull()
                val idx = listContainerStack.indexOfLast { it == "ul" }
                if (idx != -1) listContainerStack.removeAt(idx)
            }
            skipOrGrayTagChars(rawText, builder, mapping, tagInfo.startIndex, tagEnd, showTagsGray)
            return tagEnd
        }

        val style = HtmlTagParser.tagToStyle(tagInfo)
        val needsStyle = style != null || tagName in listOf("url")

        when {
            HtmlTagParser.tagNeedsStyle(tagName) && needsStyle -> {
                if (showTagsGray) {
                    skipOrGrayTagChars(rawText, builder, mapping, tagInfo.startIndex, tagEnd, showTagsGray)
                }
                val annot = if (tagName == "url") tagValue else null
                val resolvedStyle = style ?: SpanStyle(color = Color(0xFF1976D2), textDecoration = TextDecoration.Underline)
                startStyle(activeStyles, tagName, resolvedStyle, builder, annot)
                skipOrGrayTagChars(rawText, builder, mapping, tagInfo.startIndex, tagEnd, showTagsGray)
            }
            tagName == "indent" -> {
                if (!showTagsGray) {
                    OffsetMapper.addChar(mapping, tagInfo.startIndex, builder.length)
                    builder.append("    ")
                }
                skipOrGrayTagChars(rawText, builder, mapping, tagInfo.startIndex, tagEnd, showTagsGray)
            }
            tagName == "li" -> {
                if (!showTagsGray) {
                    OffsetMapper.addChar(mapping, tagInfo.startIndex, builder.length)
                    if (listContainerStack.lastOrNull() == "ol" && olIndexStack.isNotEmpty()) {
                        val idx = olIndexStack.last()
                        builder.append("$idx. ")
                        olIndexStack[olIndexStack.lastIndex] = idx + 1
                    } else {
                        builder.append("• ")
                    }
                }
                skipOrGrayTagChars(rawText, builder, mapping, tagInfo.startIndex, tagEnd, showTagsGray)
            }
            tagName == "item" -> {
                if (!showTagsGray) {
                    OffsetMapper.addChar(mapping, tagInfo.startIndex, builder.length)
                    val isChecked = tagValue?.lowercase()?.contains("true") == true
                    builder.append(if (isChecked) "☑ " else "☐ ")
                }
                skipOrGrayTagChars(rawText, builder, mapping, tagInfo.startIndex, tagEnd, showTagsGray)
            }
            tagName == "ol" -> {
                listContainerStack.add("ol")
                olIndexStack.add(1)
                skipOrGrayTagChars(rawText, builder, mapping, tagInfo.startIndex, tagEnd, showTagsGray)
            }
            tagName == "ul" -> {
                listContainerStack.add("ul")
                ulStack.add(true)
                skipOrGrayTagChars(rawText, builder, mapping, tagInfo.startIndex, tagEnd, showTagsGray)
            }
            tagName == "cl" -> {
                skipOrGrayTagChars(rawText, builder, mapping, tagInfo.startIndex, tagEnd, showTagsGray)
            }
            tagName in listOf("img", "video", "audio") -> {
                skipOrGrayTagChars(rawText, builder, mapping, tagInfo.startIndex, tagEnd, showTagsGray)
            }
            else -> {
                skipOrGrayTagChars(rawText, builder, mapping, tagInfo.startIndex, tagEnd, showTagsGray)
            }
        }
        return tagEnd
    }

    private fun parseMarkdownSyntax(
        rawText: String, i: Int, builder: AnnotatedString.Builder, mapping: OffsetMapper.MappingArrays,
        hideTags: Boolean, showTagsGray: Boolean, activeStyles: MutableList<ActiveStyle>
    ): Int? {
        val linkMatch = Regex("^\\[([^\\]]*)\\]\\(([^\\)]+)\\)").find(rawText.substring(i))
        if (linkMatch != null) {
            val full = linkMatch.value
            val display = linkMatch.groupValues[1]
            val url = linkMatch.groupValues[2]
            if (showTagsGray) {
                skipOrGrayTagChars(rawText, builder, mapping, i, i + 1, true)
                val linkStyle = SpanStyle(color = Color(0xFF1976D2), textDecoration = TextDecoration.Underline)
                startStyle(activeStyles, "url", linkStyle, builder, annotation = url)
                OffsetMapper.addChar(mapping, i + 1, builder.length)
                builder.append(display)
                endStyle(activeStyles, "url", builder)
                skipOrGrayTagChars(rawText, builder, mapping, i + 1 + display.length, i + full.length, true)
            } else {
                for (k in i until i + full.length) {
                    OffsetMapper.skipChar(mapping, k)
                }
                val linkStyle = SpanStyle(color = Color(0xFF1976D2), textDecoration = TextDecoration.Underline)
                startStyle(activeStyles, "url", linkStyle, builder, annotation = url)
                OffsetMapper.addChar(mapping, i, builder.length)
                builder.append(display)
                endStyle(activeStyles, "url", builder)
            }
            return i + full.length
        }

        val boldMarkers = listOf("**", "__")
        for (marker in boldMarkers) {
            if (rawText.startsWith(marker, i)) {
                val isActive = activeStyles.any { it.type == marker }
                if (showTagsGray) {
                    if (isActive) { endStyle(activeStyles, marker, builder); skipOrGrayTagChars(rawText, builder, mapping, i, i + 2, true) }
                    else { skipOrGrayTagChars(rawText, builder, mapping, i, i + 2, true); startStyle(activeStyles, marker, SpanStyle(fontWeight = FontWeight.Bold), builder) }
                } else {
                    skipOrGrayTagChars(rawText, builder, mapping, i, i + 2, showTagsGray)
                    if (isActive) endStyle(activeStyles, marker, builder) else startStyle(activeStyles, marker, SpanStyle(fontWeight = FontWeight.Bold), builder)
                }
                return i + 2
            }
        }

        if (rawText[i] == '*' || rawText[i] == '_') {
            val marker = rawText[i].toString()
            val isActive = activeStyles.any { it.type == marker }
            if (showTagsGray) {
                if (isActive) { endStyle(activeStyles, marker, builder); skipOrGrayTagChars(rawText, builder, mapping, i, i + 1, true) }
                else { skipOrGrayTagChars(rawText, builder, mapping, i, i + 1, true); startStyle(activeStyles, marker, SpanStyle(fontStyle = FontStyle.Italic), builder) }
            } else {
                skipOrGrayTagChars(rawText, builder, mapping, i, i + 1, showTagsGray)
                if (isActive) endStyle(activeStyles, marker, builder) else startStyle(activeStyles, marker, SpanStyle(fontStyle = FontStyle.Italic), builder)
            }
            return i + 1
        }

        if (rawText.startsWith("~~", i)) {
            val marker = "~~"
            val isActive = activeStyles.any { it.type == marker }
            if (showTagsGray) {
                if (isActive) { endStyle(activeStyles, marker, builder); skipOrGrayTagChars(rawText, builder, mapping, i, i + 2, true) }
                else { skipOrGrayTagChars(rawText, builder, mapping, i, i + 2, true); startStyle(activeStyles, marker, SpanStyle(textDecoration = TextDecoration.LineThrough), builder) }
            } else {
                skipOrGrayTagChars(rawText, builder, mapping, i, i + 2, showTagsGray)
                if (isActive) endStyle(activeStyles, marker, builder) else startStyle(activeStyles, marker, SpanStyle(textDecoration = TextDecoration.LineThrough), builder)
            }
            return i + 2
        }

        if (rawText[i] == '`') {
            val marker = "`"
            val isActive = activeStyles.any { it.type == marker }
            val codeStyle = SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0x1F808080), color = Color(0xFFE91E63))
            if (showTagsGray) {
                if (isActive) { endStyle(activeStyles, marker, builder); skipOrGrayTagChars(rawText, builder, mapping, i, i + 1, true) }
                else { skipOrGrayTagChars(rawText, builder, mapping, i, i + 1, true); startStyle(activeStyles, marker, codeStyle, builder) }
            } else {
                skipOrGrayTagChars(rawText, builder, mapping, i, i + 1, showTagsGray)
                if (isActive) endStyle(activeStyles, marker, builder) else startStyle(activeStyles, marker, codeStyle, builder)
            }
            return i + 1
        }

        return null
    }

    private fun appendChar(char: Char, builder: AnnotatedString.Builder, mapping: OffsetMapper.MappingArrays, index: Int) {
        OffsetMapper.addChar(mapping, index, builder.length)
        builder.append(char)
    }

    private fun skipOrGrayTagChars(rawText: String, builder: AnnotatedString.Builder, mapping: OffsetMapper.MappingArrays, start: Int, end: Int, showTagsGray: Boolean) {
        if (showTagsGray) {
            for (k in start until end) {
                OffsetMapper.addChar(mapping, k, builder.length)
                builder.append(rawText[k])
            }
            builder.addStyle(SpanStyle(color = Color(0xFF9E9E9E)), builder.length - (end - start), builder.length)
        } else {
            for (k in start until end) {
                OffsetMapper.skipChar(mapping, k)
            }
        }
    }

    private fun startStyle(activeStyles: MutableList<ActiveStyle>, type: String, style: SpanStyle, builder: AnnotatedString.Builder, annotation: String? = null) {
        activeStyles.add(ActiveStyle(type, style, builder.length, annotation))
    }

    private fun endStyle(activeStyles: MutableList<ActiveStyle>, type: String, builder: AnnotatedString.Builder) {
        val idx = activeStyles.indexOfLast { it.type == type }
        if (idx != -1) {
            val active = activeStyles[idx]
            builder.addStyle(active.style, active.start, builder.length)
            if (active.annotation != null) {
                builder.addStringAnnotation("URL", active.annotation, active.start, builder.length)
            }
            activeStyles.removeAt(idx)
        }
    }

    fun parse(rawText: String, hideTags: Boolean, showTagsGray: Boolean = false): AnnotatedString {
        return parseWithMapping(rawText, hideTags, showTagsGray).text
    }

    fun parseMediaBlocks(rawText: String): List<MediaBlock> {
        var preprocessed = rawText
        preprocessed = preprocessed.replace(Regex("!\\s*\\[([^\\]]*)\\]\\(([^\\)]+)\\)"), "<img src=\"$2\" />")
        preprocessed = preprocessed.replace(Regex("!video\\s*\\[([^\\]]*)\\]\\(([^\\)]+)\\)"), "<video src=\"$2\" />")
        preprocessed = preprocessed.replace(Regex("!audio\\s*\\[([^\\]]*)\\]\\(([^\\)]+)\\)"), "<audio src=\"$2\" />")

        val blocks = mutableListOf<MediaBlock>()
        var currentStart = 0
        val regex = Regex("<(img|video|audio)\\s+src=\"([^\"]+)\"\\s*/>|<(img|video|audio)=([^>]+)>")

        val matches = regex.findAll(preprocessed)
        for (match in matches) {
            val preText = preprocessed.substring(currentStart, match.range.first)
            if (preText.isNotEmpty()) {
                blocks.add(MediaBlock.TextBlock(preText))
            }

            val tagType = match.groupValues.getOrNull(1)?.takeIf { it.isNotEmpty() }
                ?: match.groupValues.getOrNull(3)?.takeIf { it.isNotEmpty() }
                ?: ""
            val src = match.groupValues.getOrNull(2)?.takeIf { it.isNotEmpty() }
                ?: match.groupValues.getOrNull(4)?.takeIf { it.isNotEmpty() }
                ?: ""

            when (tagType) {
                "img" -> blocks.add(MediaBlock.ImageBlock(src))
                "video" -> blocks.add(MediaBlock.VideoBlock(src))
                "audio" -> blocks.add(MediaBlock.AudioBlock(src))
                else -> blocks.add(MediaBlock.TextBlock(match.value))
            }
            currentStart = match.range.last + 1
        }

        if (currentStart < preprocessed.length) {
            blocks.add(MediaBlock.TextBlock(preprocessed.substring(currentStart)))
        }

        return blocks.ifEmpty { listOf(MediaBlock.TextBlock(preprocessed)) }
    }

    companion object {
        private val default = RichTextParser()

        fun parseWithMapping(rawText: String, hideTags: Boolean, showTagsGray: Boolean = false) = default.parseWithMapping(rawText, hideTags, showTagsGray)
        fun parse(rawText: String, hideTags: Boolean, showTagsGray: Boolean = false) = default.parse(rawText, hideTags, showTagsGray)
        fun parseMediaBlocks(rawText: String) = default.parseMediaBlocks(rawText)

        fun isJson(text: String) = JsonColorizer.isJson(text)
        fun isSecureNotesJson(text: String) = JsonColorizer.isSecureNotesJson(text)
        fun parseSecureNotesJson(text: String, defaultTitle: String = "Imported Note") = JsonColorizer.parseSecureNotesJson(text, defaultTitle)
        fun stripTags(raw: String) = MarkdownConverter.stripTags(raw)
        fun convertToMarkdown(raw: String) = MarkdownConverter.convertToMarkdown(raw)
        fun convertToHtml(raw: String) = HtmlConverter.convertToHtml(raw)
        fun convertHtmlToSecureNotes(html: String) = HtmlConverter.convertHtmlToSecureNotes(html)
    }
}

sealed class MediaBlock {
    data class TextBlock(val text: String) : MediaBlock()
    data class ImageBlock(val src: String) : MediaBlock()
    data class VideoBlock(val src: String) : MediaBlock()
    data class AudioBlock(val src: String) : MediaBlock()
}
