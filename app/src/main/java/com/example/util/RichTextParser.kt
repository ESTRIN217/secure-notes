package com.example.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color

object RichTextParser {
    
    data class ParseResult(
        val text: AnnotatedString,
        val sourceToTransformed: IntArray,
        val transformedToSource: IntArray
    )

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

    fun parseSecureNotesJson(text: String): Pair<String, String> {
        val json = org.json.JSONObject(text.trim())
        val title = json.optString("title", "Imported Note")
        val summary = json.optString("summary", "")
        return Pair(title, summary)
    }

    private fun parseColor(value: String?): Color? {
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

    // Single-pass parsing that returns styled AnnotatedString and offset mappings
    fun parseWithMapping(rawText: String, hideTags: Boolean, showTagsGray: Boolean = false): ParseResult {
        val N = rawText.length
        
        // If it's JSON, colorize and keep full identity mapping
        if (isJson(rawText)) {
            val builder = AnnotatedString.Builder()
            builder.append(rawText)
            highlightJson(rawText, builder)
            val identityMapping = IntArray(N + 1) { it }
            return ParseResult(builder.toAnnotatedString(), identityMapping, identityMapping)
        }

        val builder = AnnotatedString.Builder()
        val sourceToTransformed = IntArray(N + 1) { -1 }
        val transformedToSourceList = ArrayList<Int>(N)

        fun skipOrGrayTagChars(start: Int, end: Int) {
            if (showTagsGray) {
                for (k in start until end) {
                    transformedToSourceList.add(k)
                    sourceToTransformed[k] = builder.length
                    builder.append(rawText[k])
                }
                builder.addStyle(SpanStyle(color = Color(0xFF9E9E9E)), builder.length - (end - start), builder.length)
            } else {
                for (k in start until end) {
                    sourceToTransformed[k] = builder.length
                }
            }
        }

        // Stack to track active SpanStyles and their start indices in the transformed text
        data class ActiveStyle(val type: String, val style: SpanStyle, val start: Int, val annotation: String? = null)
        val activeStyles = mutableListOf<ActiveStyle>()

        fun startStyle(type: String, style: SpanStyle, annotation: String? = null) {
            activeStyles.add(ActiveStyle(type, style, builder.length, annotation))
        }

        fun endStyle(type: String) {
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

        val olIndexStack = mutableListOf<Int>()
        val ulStack = mutableListOf<Boolean>()
        val listContainerStack = mutableListOf<String>()

        var i = 0
        var isLineStart = true

        while (i < N) {
            val char = rawText[i]

            // 1. Detect Markdown line-start structures (Headers, Lists, Quotes)
            if (isLineStart && (hideTags || showTagsGray)) {
                // Check for H3: "### "
                if (rawText.startsWith("### ", i)) {
                    val tagEnd = i + 4
                    skipOrGrayTagChars(i, tagEnd)
                    startStyle("h3", SpanStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFFE65100)))
                    i = tagEnd
                    isLineStart = false
                    continue
                }
                // Check for H2: "## "
                if (rawText.startsWith("## ", i)) {
                    val tagEnd = i + 3
                    skipOrGrayTagChars(i, tagEnd)
                    startStyle("h2", SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFFF57C00)))
                    i = tagEnd
                    isLineStart = false
                    continue
                }
                // Check for H1: "# "
                if (rawText.startsWith("# ", i)) {
                    val tagEnd = i + 2
                    skipOrGrayTagChars(i, tagEnd)
                    startStyle("h1", SpanStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color(0xFFFB8C00)))
                    i = tagEnd
                    isLineStart = false
                    continue
                }
                // Check for Blockquote: "> "
                if (rawText.startsWith("> ", i)) {
                    val tagEnd = i + 2
                    skipOrGrayTagChars(i, tagEnd)
                    startStyle("quote", SpanStyle(fontStyle = FontStyle.Italic, color = Color(0xFF546E7A)))
                    i = tagEnd
                    isLineStart = false
                    continue
                }
                // Check for Checklists: "- [ ] " or "- [x] " / "* [ ] " or "* [x] "
                if (rawText.startsWith("- [ ] ", i) || rawText.startsWith("* [ ] ", i)) {
                    val tagEnd = i + 6
                    if (showTagsGray) {
                        skipOrGrayTagChars(i, tagEnd)
                    } else {
                        skipOrGrayTagChars(i, tagEnd)
                        transformedToSourceList.add(i)
                        builder.append("☐ ")
                    }
                    i = tagEnd
                    isLineStart = false
                    continue
                }
                if (rawText.startsWith("- [x] ", i) || rawText.startsWith("* [x] ", i) || rawText.startsWith("- [X] ", i) || rawText.startsWith("* [X] ", i)) {
                    val tagEnd = i + 6
                    if (showTagsGray) {
                        skipOrGrayTagChars(i, tagEnd)
                    } else {
                        skipOrGrayTagChars(i, tagEnd)
                        transformedToSourceList.add(i)
                        builder.append("☑ ")
                    }
                    i = tagEnd
                    isLineStart = false
                    continue
                }
                // Check for bullet list: "- " or "* "
                if (rawText.startsWith("- ", i) || rawText.startsWith("* ", i)) {
                    val tagEnd = i + 2
                    if (showTagsGray) {
                        skipOrGrayTagChars(i, tagEnd)
                    } else {
                        skipOrGrayTagChars(i, tagEnd)
                        transformedToSourceList.add(i)
                        builder.append("• ")
                    }
                    i = tagEnd
                    isLineStart = false
                    continue
                }
                // Check for numbered list: e.g. "1. "
                val numListMatch = Regex("^\\d+\\.\\s+").find(rawText.substring(i))
                if (numListMatch != null) {
                    val tagLen = numListMatch.value.length
                    val tagEnd = i + tagLen
                    if (showTagsGray) {
                        skipOrGrayTagChars(i, tagEnd)
                    } else {
                        skipOrGrayTagChars(i, tagEnd)
                        transformedToSourceList.add(i)
                        builder.append(numListMatch.value)
                    }
                    i = tagEnd
                    isLineStart = false
                    continue
                }
            }

            // 2. Detect inline HTML tags: starting with "<"
            if (char == '<' && (hideTags || showTagsGray)) {
                val closeIdx = rawText.indexOf('>', i)
                if (closeIdx != -1) {
                    val tagContent = rawText.substring(i + 1, closeIdx).trim()
                    val lowerTag = tagContent.lowercase()
                    
                    var isValidHtmlTag = false
                    var tagRendered = false
                    if (lowerTag.startsWith("/")) {
                        val endTagName = lowerTag.substring(1)
                        if (endTagName in listOf("b", "i", "u", "s", "code", "pre", "quote", "color", "bg", "font", "size", "h1", "h2", "h3", "normal", "sub", "sup", "indent", "url", "ol", "ul", "cl", "img", "video", "audio")) {
                            endStyle(endTagName)
                            if (endTagName == "ol") {
                                olIndexStack.removeLastOrNull()
                                val idx = listContainerStack.indexOfLast { it == "ol" }
                                if (idx != -1) {
                                    listContainerStack.removeAt(idx)
                                }
                            }
                            if (endTagName == "ul") {
                                ulStack.removeLastOrNull()
                                val idx = listContainerStack.indexOfLast { it == "ul" }
                                if (idx != -1) {
                                    listContainerStack.removeAt(idx)
                                }
                            }
                            isValidHtmlTag = true
                        } else if (endTagName == "item" || endTagName == "li") {
                            isValidHtmlTag = true
                        }
                    } else {
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
                        } else {
                            null
                        }

                        var style: SpanStyle? = null
                        when (tagName) {
                            "b" -> style = SpanStyle(fontWeight = FontWeight.Bold)
                            "i" -> style = SpanStyle(fontStyle = FontStyle.Italic)
                            "u" -> style = SpanStyle(textDecoration = TextDecoration.Underline)
                            "s" -> style = SpanStyle(textDecoration = TextDecoration.LineThrough)
                            "code" -> style = SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0x1F808080), color = Color(0xFFE91E63))
                            "pre" -> style = SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0x15000000), color = Color(0xFF37474F))
                            "quote" -> style = SpanStyle(fontStyle = FontStyle.Italic, color = Color(0xFF607D8B))
                            "h1" -> style = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp)
                            "h2" -> style = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            "h3" -> style = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            "normal" -> style = SpanStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp)
                            "sub" -> style = SpanStyle(fontSize = 11.sp, baselineShift = BaselineShift.Subscript)
                            "sup" -> style = SpanStyle(fontSize = 11.sp, baselineShift = BaselineShift.Superscript)
                            "color" -> {
                                val c = parseColor(tagValue)
                                if (c != null) style = SpanStyle(color = c)
                            }
                            "bg" -> {
                                val c = parseColor(tagValue)
                                if (c != null) style = SpanStyle(background = c)
                            }
                            "font" -> {
                                val fam = when (tagValue?.lowercase()?.removeSurrounding("\"")?.removeSurrounding("'")) {
                                    "serif" -> FontFamily.Serif
                                    "monospace" -> FontFamily.Monospace
                                    "sans-serif" -> FontFamily.SansSerif
                                    "cursive" -> FontFamily.Cursive
                                    else -> FontFamily.Default
                                }
                                style = SpanStyle(fontFamily = fam)
                            }
                            "size" -> {
                                val sz = tagValue?.removeSurrounding("\"")?.removeSurrounding("'")?.toIntOrNull() ?: 16
                                style = SpanStyle(fontSize = sz.sp)
                            }
                            "url" -> {
                                style = SpanStyle(color = Color(0xFF1976D2), textDecoration = TextDecoration.Underline)
                            }
                            "ol" -> {
                                listContainerStack.add("ol")
                                olIndexStack.add(1)
                                isValidHtmlTag = true
                            }
                            "ul" -> {
                                listContainerStack.add("ul")
                                ulStack.add(true)
                                isValidHtmlTag = true
                            }
                            "cl" -> {
                                isValidHtmlTag = true
                            }
                            "indent" -> {
                                if (!showTagsGray) {
                                    transformedToSourceList.add(i)
                                    builder.append("    ")
                                }
                                isValidHtmlTag = true
                            }
                            "li" -> {
                                if (!showTagsGray) {
                                    transformedToSourceList.add(i)
                                    if (listContainerStack.lastOrNull() == "ol" && olIndexStack.isNotEmpty()) {
                                        val idx = olIndexStack.last()
                                        builder.append("$idx. ")
                                        olIndexStack[olIndexStack.lastIndex] = idx + 1
                                    } else {
                                        builder.append("• ")
                                    }
                                }
                                isValidHtmlTag = true
                            }
                            "item" -> {
                                if (!showTagsGray) {
                                    transformedToSourceList.add(i)
                                    val isChecked = tagValue?.lowercase()?.contains("true") == true
                                    builder.append(if (isChecked) "☑ " else "☐ ")
                                }
                                isValidHtmlTag = true
                            }
                        }

                        if (style != null || tagName in listOf("b", "i", "u", "s", "code", "pre", "quote", "color", "bg", "font", "size", "h1", "h2", "h3", "normal", "sub", "sup", "ol", "ul", "cl", "indent", "li", "item", "url", "img", "video", "audio")) {
                            if (style != null) {
                                if (showTagsGray) {
                                    skipOrGrayTagChars(i, closeIdx + 1)
                                    tagRendered = true
                                }
                                val annot = if (tagName == "url") tagValue else null
                                startStyle(tagName, style, annot)
                            }
                            isValidHtmlTag = true
                        }
                    }

                    if (isValidHtmlTag) {
                        if (!tagRendered) {
                            skipOrGrayTagChars(i, closeIdx + 1)
                        }
                        i = closeIdx + 1
                        continue
                    }
                }
            }

            // 3. Detect inline markdown markers: bold, italic, strikethrough, inline-code, links
            if (hideTags || showTagsGray) {
                // Markdown link: [text](url)
                val linkMatch = Regex("^\\[([^\\]]*)\\]\\(([^\\)]+)\\)").find(rawText.substring(i))
                if (linkMatch != null) {
                    val full = linkMatch.value
                    val display = linkMatch.groupValues[1]
                    val url = linkMatch.groupValues[2]
                    if (showTagsGray) {
                        // Render [ in gray
                        skipOrGrayTagChars(i, i + 1)
                        // Render display text in link style
                        val linkStyle = SpanStyle(color = Color(0xFF1976D2), textDecoration = TextDecoration.Underline)
                        startStyle("url", linkStyle, annotation = url)
                        transformedToSourceList.add(i + 1)
                        builder.append(display)
                        endStyle("url")
                        // Render ](url) in gray
                        skipOrGrayTagChars(i + 1 + display.length, i + full.length)
                    } else {
                        for (k in i until i + full.length) {
                            sourceToTransformed[k] = builder.length
                        }
                        val linkStyle = SpanStyle(color = Color(0xFF1976D2), textDecoration = TextDecoration.Underline)
                        startStyle("url", linkStyle, annotation = url)
                        transformedToSourceList.add(i)
                        builder.append(display)
                        endStyle("url")
                    }
                    i += full.length
                    continue
                }
                // Bold "**" or "__"
                if (rawText.startsWith("**", i) || rawText.startsWith("__", i)) {
                    val marker = rawText.substring(i, i + 2)
                    val isActive = activeStyles.any { it.type == marker }
                    if (showTagsGray) {
                        if (isActive) {
                            endStyle(marker)
                            skipOrGrayTagChars(i, i + 2)
                        } else {
                            skipOrGrayTagChars(i, i + 2)
                            startStyle(marker, SpanStyle(fontWeight = FontWeight.Bold))
                        }
                    } else {
                        skipOrGrayTagChars(i, i + 2)
                        if (isActive) endStyle(marker) else startStyle(marker, SpanStyle(fontWeight = FontWeight.Bold))
                    }
                    i += 2
                    continue
                }
                // Italic "*" or "_"
                if (char == '*' || char == '_') {
                    val marker = char.toString()
                    val isActive = activeStyles.any { it.type == marker }
                    if (showTagsGray) {
                        if (isActive) {
                            endStyle(marker)
                            skipOrGrayTagChars(i, i + 1)
                        } else {
                            skipOrGrayTagChars(i, i + 1)
                            startStyle(marker, SpanStyle(fontStyle = FontStyle.Italic))
                        }
                    } else {
                        skipOrGrayTagChars(i, i + 1)
                        if (isActive) endStyle(marker) else startStyle(marker, SpanStyle(fontStyle = FontStyle.Italic))
                    }
                    i += 1
                    continue
                }
                // Strikethrough "~~"
                if (rawText.startsWith("~~", i)) {
                    val marker = "~~"
                    val isActive = activeStyles.any { it.type == marker }
                    if (showTagsGray) {
                        if (isActive) {
                            endStyle(marker)
                            skipOrGrayTagChars(i, i + 2)
                        } else {
                            skipOrGrayTagChars(i, i + 2)
                            startStyle(marker, SpanStyle(textDecoration = TextDecoration.LineThrough))
                        }
                    } else {
                        skipOrGrayTagChars(i, i + 2)
                        if (isActive) endStyle(marker) else startStyle(marker, SpanStyle(textDecoration = TextDecoration.LineThrough))
                    }
                    i += 2
                    continue
                }
                // Inline Code "`"
                if (char == '`') {
                    val marker = "`"
                    val isActive = activeStyles.any { it.type == marker }
                    if (showTagsGray) {
                        if (isActive) {
                            endStyle(marker)
                            skipOrGrayTagChars(i, i + 1)
                        } else {
                            skipOrGrayTagChars(i, i + 1)
                            startStyle(marker, SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0x1F808080), color = Color(0xFFE91E63)))
                        }
                    } else {
                        skipOrGrayTagChars(i, i + 1)
                        if (isActive) {
                            endStyle(marker)
                        } else {
                            startStyle(marker, SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0x1F808080), color = Color(0xFFE91E63)))
                        }
                    }
                    i += 1
                    continue
                }
            }

            // 4. Default Character Processing
            transformedToSourceList.add(i)
            sourceToTransformed[i] = builder.length
            builder.append(char)

            if (char == '\n') {
                isLineStart = true
                if (hideTags || showTagsGray) {
                    endStyle("h1")
                    endStyle("h2")
                    endStyle("h3")
                    endStyle("quote")
                }
            } else {
                isLineStart = false
            }

            i++
        }

        // Handle end of text mappings
        sourceToTransformed[N] = builder.length
        transformedToSourceList.add(N)

        for (active in activeStyles.reversed()) {
            builder.addStyle(active.style, active.start, builder.length)
        }

        val transformedToSource = transformedToSourceList.toIntArray()

        var lastT = 0
        for (idx in 0..N) {
            if (sourceToTransformed[idx] == -1) {
                sourceToTransformed[idx] = lastT
            } else {
                lastT = sourceToTransformed[idx]
            }
        }

        val M = builder.length
        val filledTransformedToSource = IntArray(M + 1)
        for (idx in 0..M) {
            filledTransformedToSource[idx] = if (idx < transformedToSource.size) {
                transformedToSource[idx].coerceIn(0, N)
            } else {
                N
            }
        }

        return ParseResult(builder.toAnnotatedString(), sourceToTransformed, filledTransformedToSource)
    }

    private fun highlightJson(text: String, builder: AnnotatedString.Builder) {
        val stringRegex = Regex("\"(\\\\.|[^\"])*\"")
        for (match in stringRegex.findAll(text)) {
            var isKey = false
            var idx = match.range.last + 1
            while (idx < text.length) {
                val c = text[idx]
                if (!c.isWhitespace()) {
                    if (c == ':') {
                        isKey = true
                    }
                    break
                }
                idx++
            }
            val color = if (isKey) Color(0xFF1E88E5) else Color(0xFF43A047)
            builder.addStyle(SpanStyle(color = color, fontWeight = if (isKey) FontWeight.Bold else FontWeight.Normal), match.range.first, match.range.last + 1)
        }

        val numberRegex = Regex("\\b\\d+(\\.\\d+)?\\b")
        for (match in numberRegex.findAll(text)) {
            builder.addStyle(SpanStyle(color = Color(0xFFD81B60)), match.range.first, match.range.last + 1)
        }

        val keywordRegex = Regex("\\b(true|false|null)\\b")
        for (match in keywordRegex.findAll(text)) {
            val color = if (match.value == "null") Color(0xFF8E24AA) else Color(0xFFFB8C00)
            builder.addStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1)
        }

        val puncRegex = Regex("[\\{\\}\\[\\]\\,:]")
        for (match in puncRegex.findAll(text)) {
            builder.addStyle(SpanStyle(color = Color(0xFF757575), fontFamily = FontFamily.Monospace), match.range.first, match.range.last + 1)
        }
    }

    // Keep legacy parse method for other files or parts of code that use it, ensuring compatibility
    fun parse(rawText: String, hideTags: Boolean, showTagsGray: Boolean = false): AnnotatedString {
        return parseWithMapping(rawText, hideTags, showTagsGray).text
    }

    // ──────────────────────────────────────────────
    // Conversion utilities for export/import
    // ──────────────────────────────────────────────

    private data class TagTransform(
        val regex: Regex,
        val transform: (MatchResult) -> String
    )

    private fun applyTransforms(input: String, transforms: List<TagTransform>): String {
        var s = input
        for (t in transforms) {
            s = s.replace(t.regex, t.transform)
        }
        return s
    }

    /** Strip all internal custom tags, leaving plain text. */
    fun stripTags(raw: String): String {
        return raw
            .replace(Regex("</?[a-z]+(=(\"[^\"]*\"|'[^']*'|[^>\\s]+))?\\s*/?>"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /** Convert internal custom tags to Markdown. */
    fun convertToMarkdown(raw: String): String {
        return applyTransforms(raw, listOf(
            TagTransform(Regex("<h1>(.*?)</h1>")) { "# ${it.groupValues[1]}" },
            TagTransform(Regex("<h2>(.*?)</h2>")) { "## ${it.groupValues[1]}" },
            TagTransform(Regex("<h3>(.*?)</h3>")) { "### ${it.groupValues[1]}" },
            TagTransform(Regex("<b>(.*?)</b>")) { "**${it.groupValues[1]}**" },
            TagTransform(Regex("<i>(.*?)</i>")) { "*${it.groupValues[1]}*" },
            TagTransform(Regex("<s>(.*?)</s>")) { "~~${it.groupValues[1]}~~" },
            TagTransform(Regex("<u>(.*?)</u>")) { "<ins>${it.groupValues[1]}</ins>" },
            TagTransform(Regex("<code>(.*?)</code>")) { "`${it.groupValues[1]}`" },
            TagTransform(Regex("<pre>(.*?)</pre>", RegexOption.DOT_MATCHES_ALL)) { "\n```\n${it.groupValues[1]}\n```\n" },
            TagTransform(Regex("<quote>(.*?)</quote>", RegexOption.DOT_MATCHES_ALL)) {
                it.groupValues[1].lines().joinToString("\n") { line -> "> $line" }
            },
            TagTransform(Regex("<url=(\"[^\"]*\"|'[^']*'|[^>]+)>(.*?)</url>")) { m ->
                val url = m.groupValues[1].removeSurrounding("\"").removeSurrounding("'")
                val text = m.groupValues[2]
                if (url == text) url else "[$text]($url)"
            },
            TagTransform(Regex("<color=(\"[^\"]*\"|'[^']*'|[^>]+)>(.*?)</color>")) { m ->
                val color = m.groupValues[1].removeSurrounding("\"").removeSurrounding("'")
                "<span style=\"color:$color\">${m.groupValues[2]}</span>"
            },
            TagTransform(Regex("<bg=(\"[^\"]*\"|'[^']*'|[^>]+)>(.*?)</bg>")) { m ->
                val color = m.groupValues[1].removeSurrounding("\"").removeSurrounding("'")
                "<span style=\"background:$color\">${m.groupValues[2]}</span>"
            },
            TagTransform(Regex("<ol>\\s*(.*?)\\s*</ol>", RegexOption.DOT_MATCHES_ALL)) { m ->
                m.groupValues[1].lines().withIndex().joinToString("\n") { (i, line) ->
                    "${i + 1}. ${line.replace(Regex("</?li>"), "").trim()}"
                }
            },
            TagTransform(Regex("<ul>\\s*(.*?)\\s*</ul>", RegexOption.DOT_MATCHES_ALL)) { m ->
                m.groupValues[1].lines().joinToString("\n") { line ->
                    "- ${line.replace(Regex("</?li>"), "").trim()}"
                }
            },
            TagTransform(Regex("<cl>\\s*(.*?)\\s*</cl>", RegexOption.DOT_MATCHES_ALL)) { m ->
                m.groupValues[1].lines().joinToString("\n") { line ->
                    val checked = line.contains("checked=\"true\"")
                    val content = line.replace(Regex("<[^>]+>"), "").trim()
                    "- [${if (checked) "x" else " "}] $content"
                }
            },
            TagTransform(Regex("<img\\s+src=\"([^\"]+)\"\\s*/>")) { "![image](${it.groupValues[1]})" },
            TagTransform(Regex("<video\\s+src=\"([^\"]+)\"\\s*/>")) { "!video[video](${it.groupValues[1]})" },
            TagTransform(Regex("<audio\\s+src=\"([^\"]+)\"\\s*/>")) { "!audio[audio](${it.groupValues[1]})" },
            TagTransform(Regex("<[^>]+>")) { "" }
        ))
    }

    /** Convert internal custom tags to standard HTML. */
    fun convertToHtml(raw: String): String {
        return applyTransforms(raw, listOf(
            TagTransform(Regex("<h1>(.*?)</h1>")) { "<h1>${it.groupValues[1]}</h1>" },
            TagTransform(Regex("<h2>(.*?)</h2>")) { "<h2>${it.groupValues[1]}</h2>" },
            TagTransform(Regex("<h3>(.*?)</h3>")) { "<h3>${it.groupValues[1]}</h3>" },
            TagTransform(Regex("<b>(.*?)</b>")) { "<strong>${it.groupValues[1]}</strong>" },
            TagTransform(Regex("<i>(.*?)</i>")) { "<em>${it.groupValues[1]}</em>" },
            TagTransform(Regex("<s>(.*?)</s>")) { "<del>${it.groupValues[1]}</del>" },
            TagTransform(Regex("<u>(.*?)</u>")) { "<ins>${it.groupValues[1]}</ins>" },
            TagTransform(Regex("<code>(.*?)</code>")) { "<code>${it.groupValues[1]}</code>" },
            TagTransform(Regex("<pre>(.*?)</pre>", RegexOption.DOT_MATCHES_ALL)) { "<pre>${it.groupValues[1]}</pre>" },
            TagTransform(Regex("<quote>(.*?)</quote>", RegexOption.DOT_MATCHES_ALL)) { "<blockquote>${it.groupValues[1]}</blockquote>" },
            TagTransform(Regex("<url=(\"[^\"]*\"|'[^']*'|[^>]+)>(.*?)</url>")) { m ->
                val url = m.groupValues[1].removeSurrounding("\"").removeSurrounding("'")
                "<a href=\"$url\">${m.groupValues[2]}</a>"
            },
            TagTransform(Regex("<color=(\"[^\"]*\"|'[^']*'|[^>]+)>(.*?)</color>")) { m ->
                "<span style=\"color:${m.groupValues[1].removeSurrounding("\"").removeSurrounding("'")}\">${m.groupValues[2]}</span>"
            },
            TagTransform(Regex("<bg=(\"[^\"]*\"|'[^']*'|[^>]+)>(.*?)</bg>")) { m ->
                "<span style=\"background:${m.groupValues[1].removeSurrounding("\"").removeSurrounding("'")}\">${m.groupValues[2]}</span>"
            },
            TagTransform(Regex("<font=(\"[^\"]*\"|'[^']*'|[^>]+)>(.*?)</font>")) { m ->
                "<span style=\"font-family:${m.groupValues[1].removeSurrounding("\"").removeSurrounding("'")}\">${m.groupValues[2]}</span>"
            },
            TagTransform(Regex("<size=(\"[^\"]*\"|'[^']*'|[^>]+)>(.*?)</size>")) { m ->
                "<span style=\"font-size:${m.groupValues[1].removeSurrounding("\"").removeSurrounding("'")}sp\">${m.groupValues[2]}</span>"
            },
            TagTransform(Regex("<ol>(.*?)</ol>", RegexOption.DOT_MATCHES_ALL)) { "<ol>${it.groupValues[1]}</ol>" },
            TagTransform(Regex("<ul>(.*?)</ul>", RegexOption.DOT_MATCHES_ALL)) { "<ul>${it.groupValues[1]}</ul>" },
            TagTransform(Regex("</?li>")) { "" },
            TagTransform(Regex("<cl>(.*?)</cl>", RegexOption.DOT_MATCHES_ALL)) { m ->
                m.groupValues[1].lines().joinToString("\n") { line ->
                    val checked = line.contains("checked=\"true\"")
                    val content = line.replace(Regex("<[^>]+>"), "").trim()
                    "<li>${if (checked) "☑" else "☐"} $content</li>"
                }
            },
            TagTransform(Regex("<img\\s+src=\"([^\"]+)\"\\s*/>")) { "<img src=\"${it.groupValues[1]}\" alt=\"image\" />" },
            TagTransform(Regex("<video\\s+src=\"([^\"]+)\"\\s*/>")) { "<video src=\"${it.groupValues[1]}\" controls>video</video>" },
            TagTransform(Regex("<audio\\s+src=\"([^\"]+)\"\\s*/>")) { "<audio src=\"${it.groupValues[1]}\" controls>audio</audio>" },
            TagTransform(Regex("</?normal>")) { "" },
            TagTransform(Regex("</?sub>")) { "" },
            TagTransform(Regex("</?sup>")) { "" },
            TagTransform(Regex("</?indent>")) { "" },
            TagTransform(Regex("<[^>]+>")) { "" }
        )).trim()
    }

    /** Convert basic standard HTML to internal secure-notes custom tags. */
    fun convertHtmlToSecureNotes(html: String): String {
        var s = html
        // strip doc type / html / head / body wrappers
        s = s.replace(Regex("<!DOCTYPE[^>]*>", RegexOption.IGNORE_CASE), "")
        s = s.replace(Regex("</?(html|head|body|meta|link|script|style)[^>]*>", RegexOption.IGNORE_CASE), "")
        s = s.replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")
        // block elements → spacing
        s = s.replace(Regex("</?(p|div|section|article|nav|header|footer|main|aside)(\\s[^>]*)?>", RegexOption.IGNORE_CASE), "\n")
        s = s.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        // headings
        s = s.replace(Regex("<h1[^>]*>(.*?)</h1>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "<h1>$1</h1>")
        s = s.replace(Regex("<h2[^>]*>(.*?)</h2>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "<h2>$1</h2>")
        s = s.replace(Regex("<h3[^>]*>(.*?)</h3>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "<h3>$1</h3>")
        // inline formatting
        s = s.replace(Regex("<strong[^>]*>(.*?)</strong>", RegexOption.IGNORE_CASE), "<b>$1</b>")
        s = s.replace(Regex("<b[^>]*>(.*?)</b>", RegexOption.IGNORE_CASE), "<b>$1</b>")
        s = s.replace(Regex("<em[^>]*>(.*?)</em>", RegexOption.IGNORE_CASE), "<i>$1</i>")
        s = s.replace(Regex("<i[^>]*>(.*?)</i>", RegexOption.IGNORE_CASE), "<i>$1</i>")
        s = s.replace(Regex("<ins[^>]*>(.*?)</ins>", RegexOption.IGNORE_CASE), "<u>$1</u>")
        s = s.replace(Regex("<u[^>]*>(.*?)</u>", RegexOption.IGNORE_CASE), "<u>$1</u>")
        s = s.replace(Regex("<del[^>]*>(.*?)</del>", RegexOption.IGNORE_CASE), "<s>$1</s>")
        s = s.replace(Regex("<s[^>]*>(.*?)</s>", RegexOption.IGNORE_CASE), "<s>$1</s>")
        s = s.replace(Regex("<code[^>]*>(.*?)</code>", RegexOption.IGNORE_CASE), "<code>$1</code>")
        s = s.replace(Regex("<pre[^>]*>(.*?)</pre>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "<pre>$1</pre>")
        s = s.replace(Regex("<blockquote[^>]*>(.*?)</blockquote>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "<quote>$1</quote>")
        // anchor links
        s = s.replace(Regex("<a\\s+[^>]*href=\"([^\"]+)\"[^>]*>(.*?)</a>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "<url=$1>$2</url>")
        s = s.replace(Regex("<a\\s+[^>]*href='([^']+)'[^>]*>(.*?)</a>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "<url=$1>$2</url>")
        // images
        s = s.replace(Regex("<img\\s+[^>]*src=\"([^\"]+)\"[^>]*>", RegexOption.IGNORE_CASE), "<img src=\"$1\" />")
        s = s.replace(Regex("<img\\s+[^>]*src='([^']+)'[^>]*>", RegexOption.IGNORE_CASE), "<img src=\"$1\" />")
        // span style → parse color, background, font-family, font-size
        s = s.replace(Regex("<span\\s+[^>]*style=\"([^\"]+)\"[^>]*>(.*?)</span>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))) { match ->
            val style = match.groupValues[1]
            val content = match.groupValues[2]
            val parts = style.split(";").map { it.trim() }
            var result = content
            for (part in parts) {
                if (part.startsWith("color:", true)) {
                    val c = part.substringAfter(":").trim()
                    result = "<color=$c>$result</color>"
                } else if (part.startsWith("background", true)) {
                    val c = part.substringAfter(":").trim()
                    result = "<bg=$c>$result</bg>"
                } else if (part.startsWith("font-weight:", true)) {
                    if (part.contains("bold", true)) result = "<b>$result</b>"
                } else if (part.startsWith("font-style:", true)) {
                    if (part.contains("italic", true)) result = "<i>$result</i>"
                } else if (part.startsWith("text-decoration:", true)) {
                    if (part.contains("underline", true)) result = "<u>$result</u>"
                    if (part.contains("line-through", true)) result = "<s>$result</s>"
                } else if (part.startsWith("font-family:", true)) {
                    val fam = part.substringAfter(":").trim().removeSurrounding("\"").removeSurrounding("'")
                    result = "<font=$fam>$result</font>"
                } else if (part.startsWith("font-size:", true)) {
                    val size = part.substringAfter(":").trim().replace("px", "").replace("pt", "").trim()
                    result = "<size=$size>$result</size>"
                }
            }
            result
        }
        // ordered/unordered lists
        s = s.replace(Regex("<ol[^>]*>(.*?)</ol>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))) { match ->
            val items = match.groupValues[1]
            val lis = items.replace(Regex("<li[^>]*>(.*?)</li>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))) { liMatch ->
                liMatch.groupValues[1]
            }
            "<ol>\n${lis.lines().joinToString("\n") { "  <li>$it</li>" }}\n</ol>"
        }
        s = s.replace(Regex("<ul[^>]*>(.*?)</ul>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))) { match ->
            val items = match.groupValues[1]
            val lis = items.replace(Regex("<li[^>]*>(.*?)</li>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))) { liMatch ->
                liMatch.groupValues[1]
            }
            "<ul>\n${lis.lines().joinToString("\n") { "  <li>$it</li>" }}\n</ul>"
        }
        // clean up extra whitespace
        s = s.replace(Regex("\\n{3,}"), "\n\n")
        return s.trim()
    }

    // Parse media elements into structured blocks (for rich components, if needed)
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
}

sealed class MediaBlock {
    data class TextBlock(val text: String) : MediaBlock()
    data class ImageBlock(val src: String) : MediaBlock()
    data class VideoBlock(val src: String) : MediaBlock()
    data class AudioBlock(val src: String) : MediaBlock()
}
