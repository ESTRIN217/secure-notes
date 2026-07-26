package com.example.util

import com.example.data.model.Attachment
import com.example.data.model.ColumnAlignment
import com.example.data.model.NoteContentBlock
import com.example.data.model.parseNoteContentAndAttachments
import com.example.data.model.createRawContent

fun highlightMatches(
    annotatedString: androidx.compose.ui.text.AnnotatedString,
    query: String,
    caseSensitive: Boolean,
    fullWord: Boolean,
    currentIndex: Int,
    highlightColor: androidx.compose.ui.graphics.Color,
    currentHighlightColor: androidx.compose.ui.graphics.Color
): androidx.compose.ui.text.AnnotatedString {
    if (query.isEmpty()) return annotatedString
    val text = annotatedString.text
    val builder = androidx.compose.ui.text.AnnotatedString.Builder(annotatedString)

    val ranges = mutableListOf<IntRange>()
    if (fullWord) {
        val escapedQuery = Regex.escape(query)
        val patternString = "\\b$escapedQuery\\b"
        val options = if (caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
        try {
            val regex = Regex(patternString, options)
            regex.findAll(text).forEach { matchResult ->
                ranges.add(matchResult.range)
            }
        } catch (e: Exception) {
            var idx = text.indexOf(query, 0, ignoreCase = !caseSensitive)
            while (idx != -1) {
                ranges.add(idx until (idx + query.length))
                idx = text.indexOf(query, idx + 1, ignoreCase = !caseSensitive)
            }
        }
    } else {
        var idx = text.indexOf(query, 0, ignoreCase = !caseSensitive)
        while (idx != -1) {
            ranges.add(idx until (idx + query.length))
            idx = text.indexOf(query, idx + 1, ignoreCase = !caseSensitive)
        }
    }

    ranges.forEachIndexed { index, range ->
        val color = if (index == currentIndex) currentHighlightColor else highlightColor
        builder.addStyle(
            style = androidx.compose.ui.text.SpanStyle(
                background = color,
                color = if (index == currentIndex) androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.Unspecified
            ),
            start = range.first,
            end = range.last + 1
        )
    }

    return builder.toAnnotatedString()
}

fun findEnclosingUrlTagRange(rawText: String, offset: Int): IntRange? {
    var startIdx = -1
    var temp = offset
    while (temp >= 0) {
        if (rawText.startsWith("<url", temp)) {
            val closeAngle = rawText.indexOf('>', temp)
            if (closeAngle != -1 && closeAngle < rawText.length) {
                startIdx = temp
                break
            }
        }
        temp--
    }
    if (startIdx == -1) return null

    val endTagStart = rawText.indexOf("</url>", startIdx)
    if (endTagStart == -1) return null
    val endIdx = endTagStart + "</url>".length

    if (offset in startIdx..endIdx) {
        return IntRange(startIdx, endIdx - 1)
    }
    return null
}

fun findEnclosingMarkdownLinkRange(rawText: String, offset: Int): IntRange? {
    var temp = offset
    while (temp >= 0) {
        if (rawText[temp] == '[') {
            val closeBracket = rawText.indexOf(']', temp)
            if (closeBracket != -1 && closeBracket + 1 < rawText.length && rawText[closeBracket + 1] == '(') {
                val closeParen = rawText.indexOf(')', closeBracket + 2)
                if (closeParen != -1 && offset <= closeParen) {
                    return IntRange(temp, closeParen)
                }
            }
        }
        temp--
    }
    return null
}

fun toggleNthChecklistItem(rawText: String, indexToToggle: Int): String {
    val regex = Regex("<item\\s+checked=\"(true|false)\">")
    val matches = regex.findAll(rawText).toList()
    if (indexToToggle in matches.indices) {
        val match = matches[indexToToggle]
        val checkedValue = match.groupValues[1]
        val newCheckedValue = if (checkedValue == "true") "false" else "true"
        val start = match.range.first
        val end = match.range.last + 1
        val updatedTag = "<item checked=\"$newCheckedValue\">"
        return rawText.substring(0, start) + updatedTag + rawText.substring(end)
    }
    return rawText
}

fun preprocessMarkdownBlocks(text: String): String {
    val lines = text.split("\n")
    val result = StringBuilder()
    var i = 0
    while (i < lines.size) {
        val trimmed = lines[i].trim()
        val hrCounts = listOf(
            trimmed.count { it == '-' },
            trimmed.count { it == '*' },
            trimmed.count { it == '_' }
        )
        if (!trimmed.contains('|') && hrCounts.any { it >= 3 } && trimmed.matches(Regex("^[-*_ ]+$"))) {
            result.append("<hr/>")
            i++
            continue
        }
        if (trimmed.startsWith("|") && trimmed.count { it == '|' } >= 2) {
            val tableLines = mutableListOf<String>()
            while (i < lines.size) {
                val line = lines[i].trim()
                if (line.startsWith("|") && line.count { it == '|' } >= 2) {
                    tableLines.add(line)
                    i++
                } else {
                    break
                }
            }
            result.append(parsePipeTableToHtml(tableLines))
            if (result.lastOrNull() != '\n' && i < lines.size) {
                result.append("\n")
            }
        } else {
            result.append(lines[i])
            if (i < lines.size - 1) {
                result.append("\n")
            }
            i++
        }
    }
    return result.toString()
}

private fun parsePipeTableToHtml(tableLines: List<String>): String {
    if (tableLines.isEmpty()) return ""

    fun parseRow(line: String): List<String> {
        return line.trim().removeSurrounding("|").split("|").map { it.trim() }
    }

    val sepIndex = tableLines.indexOfFirst { line ->
        line.replace("\\s".toRegex(), "").let { it.count { c -> c == '-' } >= 3 }
    }

    val alignment = if (sepIndex >= 0) {
        parseRow(tableLines[sepIndex]).map { col ->
            val trimmed = col.trim()
            when {
                trimmed.startsWith(":") && trimmed.endsWith(":") -> ColumnAlignment.Center
                trimmed.endsWith(":") -> ColumnAlignment.End
                else -> ColumnAlignment.Start
            }
        }
    } else emptyList()

    val headers = if (sepIndex >= 0) parseRow(tableLines[0]) else emptyList()
    val bodyLines = if (sepIndex >= 0) tableLines.drop(sepIndex + 1) else tableLines

    val sb = StringBuilder("<table>")
    if (headers.isNotEmpty()) {
        sb.append("<th>")
        sb.append(headers.joinToString("</th><th>") { it })
        sb.append("</th>")
    }
    for (bodyLine in bodyLines) {
        val cells = parseRow(bodyLine)
        sb.append("<tr><td>")
        sb.append(cells.joinToString("</td><td>") { it })
        sb.append("</td></tr>")
    }
    sb.append("</table>")
    return sb.toString()
}

fun parseToContentBlocks(rawText: String): List<NoteContentBlock> {
    val processed = preprocessMarkdownBlocks(rawText)
    val blocks = mutableListOf<NoteContentBlock>()

    val regex = Regex(
        "(?:\\[!\\s*\\[([^\\]]*)\\]\\(([^\\)]+)\\)\\]\\(([^\\)]+)\\))" +
        "|(?:!\\s*\\[([^\\]]*)\\]\\(([^\\)]+)\\))" +
        "|(?:!video\\s*\\[([^\\]]*)\\]\\(([^\\)]+)\\))" +
        "|(?:!audio\\s*\\[([^\\]]*)\\]\\(([^\\)]+)\\))" +
        "|(?:<item\\s+checked=\"(true|false)\">([\\s\\S]*?)</item>)" +
        "|(?:<(img|video|audio)\\s+src=\"([^\"]+)\"\\s*/>|<(img|video|audio)=([^>]+)>)" +
        "|(?:<table[^>]*>([\\s\\S]*?)</table>)" +
        "|(?:<hr\\s*/?>)" +
        "|(?:<details>([\\s\\S]*?)</details>)" +
        "|(?:<align\\s*=\\s*\"?(center|left|right|justify)\"?\\s*>)" +
        "|(</align>)" +
        "|(<cl>|</cl>)"
    )

    var lastIndex = 0
    val matches = regex.findAll(processed)
    var checklistItemIndex = 0
    val alignStack = mutableListOf<androidx.compose.ui.text.style.TextAlign>()

    for (match in matches) {
        val preText = processed.substring(lastIndex, match.range.first)
        if (preText.isNotEmpty()) {
            val currentAlign = alignStack.lastOrNull()
            val parseResult = RichTextParser.parseWithMapping(preText, hideTags = true)
            if (parseResult.text.text.isNotBlank()) {
                blocks.add(NoteContentBlock.TextBlock(parseResult, rawStart = lastIndex, textAlign = currentAlign))
            }
        }

        val isLinkedImg = match.groupValues[3].isNotEmpty()
        val isMdImg = !isLinkedImg && (match.groupValues[5].isNotEmpty() && match.groupValues[4].isNotEmpty() || (match.groupValues[5].isNotEmpty() && match.value.startsWith("!")))
        val isMdVideo = match.groupValues[7].isNotEmpty()
        val isMdAudio = match.groupValues[9].isNotEmpty()
        val isItem = match.groupValues[10].isNotEmpty()
        val isHtmlMedia = match.groupValues[12].isNotEmpty()
        val isHtmlShortMedia = match.groupValues[14].isNotEmpty()
        val isTable = match.groupValues[16].isNotEmpty()
        val isDetails = match.groupValues[17].isNotEmpty()
        val isAlign = match.groupValues[18].isNotEmpty()
        val isAlignClose = match.groupValues[19].isNotEmpty()
        val isHr = !isTable && !isDetails && match.value.startsWith("<hr")

        when {
            isAlign -> {
                val alignValue = match.groupValues[18]
                val align = when (alignValue.lowercase()) {
                    "center" -> androidx.compose.ui.text.style.TextAlign.Center
                    "right" -> androidx.compose.ui.text.style.TextAlign.End
                    "justify" -> androidx.compose.ui.text.style.TextAlign.Justify
                    else -> androidx.compose.ui.text.style.TextAlign.Start
                }
                alignStack.add(align)
            }
            isAlignClose -> {
                alignStack.removeLastOrNull()
            }
            isLinkedImg -> {
                val src = match.groupValues[2]
                val link = match.groupValues[3]
                blocks.add(NoteContentBlock.ImageBlock(src, linkUrl = link))
            }
            isMdImg -> {
                val src = match.groupValues[5]
                blocks.add(NoteContentBlock.ImageBlock(src))
            }
            isMdVideo -> {
                val src = match.groupValues[7]
                blocks.add(NoteContentBlock.VideoBlock(src))
            }
            isMdAudio -> {
                val src = match.groupValues[9]
                blocks.add(NoteContentBlock.AudioBlock(src))
            }
            isItem -> {
                val isChecked = match.groupValues[10] == "true"
                val itemText = match.groupValues[11]
                val parseResult = RichTextParser.parseWithMapping(itemText, hideTags = true)
                val relativeStart = match.value.indexOf(itemText)
                val itemTextStart = match.range.first + relativeStart
                blocks.add(NoteContentBlock.ChecklistItemBlock(
                    isChecked = isChecked,
                    parseResult = parseResult,
                    rawStart = itemTextStart,
                    globalIndex = checklistItemIndex
                ))
                checklistItemIndex++
            }
            isHtmlMedia -> {
                val mediaType = match.groupValues[12]
                val src = match.groupValues[13]
                when (mediaType) {
                    "img" -> blocks.add(NoteContentBlock.ImageBlock(src))
                    "video" -> blocks.add(NoteContentBlock.VideoBlock(src))
                    "audio" -> blocks.add(NoteContentBlock.AudioBlock(src))
                }
            }
            isHtmlShortMedia -> {
                val mediaType = match.groupValues[14]
                val src = match.groupValues[15]
                when (mediaType) {
                    "img" -> blocks.add(NoteContentBlock.ImageBlock(src))
                    "video" -> blocks.add(NoteContentBlock.VideoBlock(src))
                    "audio" -> blocks.add(NoteContentBlock.AudioBlock(src))
                }
            }
            isTable -> {
                val tableContent = match.groupValues[16]
                val tableBlock = parseTableTagToBlock(tableContent)
                blocks.add(tableBlock)
            }
            isDetails -> {
                val detailsContent = match.groupValues[17]
                val summaryRegex = Regex("<summary>(.*?)</summary>", RegexOption.DOT_MATCHES_ALL)
                val summaryMatch = summaryRegex.find(detailsContent)
                val summary = summaryMatch?.groupValues?.get(1)?.trim() ?: ""
                val body = detailsContent.replace(summaryRegex, "").trim()
                blocks.add(NoteContentBlock.CollapsibleBlock(summary = summary, content = body))
            }
            isHr -> {
                blocks.add(NoteContentBlock.HorizontalRuleBlock)
            }
        }

        lastIndex = match.range.last + 1
    }

    if (lastIndex < processed.length) {
        val remainingText = processed.substring(lastIndex)
        if (remainingText.isNotEmpty()) {
            val currentAlign = alignStack.lastOrNull()
            val parseResult = RichTextParser.parseWithMapping(remainingText, hideTags = true)
            if (parseResult.text.text.isNotBlank()) {
                blocks.add(NoteContentBlock.TextBlock(parseResult, rawStart = lastIndex, textAlign = currentAlign))
            }
        }
    }

    return blocks.ifEmpty {
        val parseResult = RichTextParser.parseWithMapping(processed, hideTags = true)
        listOf(NoteContentBlock.TextBlock(parseResult, rawStart = 0))
    }
}

private fun parseAlignAttr(tdTag: String): ColumnAlignment {
    val alignMatch = Regex("""align\s*=\s*"([^"]+)"""").find(tdTag)
        ?: Regex("""align\s*=\s*'([^']+)'""").find(tdTag)
    return when (alignMatch?.groupValues?.get(1)?.lowercase()) {
        "center" -> ColumnAlignment.Center
        "right" -> ColumnAlignment.End
        else -> ColumnAlignment.Start
    }
}

private fun parseTableTagToBlock(tableContent: String): NoteContentBlock.TableBlock {
    val headers = mutableListOf<String>()
    val rows = mutableListOf<List<String>>()
    val alignment = mutableListOf<ColumnAlignment>()
    val cellAlignment = mutableListOf<List<ColumnAlignment>>()

    val thRegex = Regex("<th[^>]*>(.*?)</th>", RegexOption.DOT_MATCHES_ALL)
    headers.addAll(thRegex.findAll(tableContent).map {
        it.groupValues[1].trim()
    })
    val thAligns = thRegex.findAll(tableContent).map {
        val tdTag = it.value.substringBefore(">").substringAfter("<th")
        parseAlignAttr(tdTag)
    }.toList()
    if (thAligns.isNotEmpty()) {
        thAligns.forEachIndexed { col, align ->
            if (alignment.size <= col) alignment.add(align)
            else if (alignment[col] == ColumnAlignment.Start) alignment[col] = align
        }
    }

    val trRegex = Regex("<tr>(.*?)</tr>", RegexOption.DOT_MATCHES_ALL)
    for (trMatch in trRegex.findAll(tableContent)) {
        val tdRegex = Regex("<td([^>]*)>(.*?)</td>", RegexOption.DOT_MATCHES_ALL)
        val cells = mutableListOf<String>()
        val aligns = mutableListOf<ColumnAlignment>()
        for (tdMatch in tdRegex.findAll(trMatch.groupValues[1])) {
            cells.add(tdMatch.groupValues[2].trim())
            aligns.add(parseAlignAttr(tdMatch.groupValues[1]))
        }
        if (cells.isNotEmpty()) {
            rows.add(cells)
            cellAlignment.add(aligns)
        }
    }

    // Infer column-level alignment from cell alignment (majority per column)
    if (cellAlignment.isNotEmpty()) {
        val maxCols = cellAlignment.maxOf { it.size }
        for (col in 0 until maxCols) {
            val counts = mutableMapOf(ColumnAlignment.Start to 0, ColumnAlignment.Center to 0, ColumnAlignment.End to 0)
            for (rowAligns in cellAlignment) {
                if (col < rowAligns.size) {
                    counts[rowAligns[col]] = (counts[rowAligns[col]] ?: 0) + 1
                }
            }
            alignment.add(counts.maxByOrNull { it.value }?.key ?: ColumnAlignment.Start)
        }
    }

    return NoteContentBlock.TableBlock(
        headers = headers,
        rows = rows,
        columnAlignment = alignment,
        cellAlignment = cellAlignment
    )
}

fun removeMediaFromContent(text: String, src: String, type: String): String {
    val tagPattern = when (type) {
        "image" -> Regex("<img\\s+src=\"${Regex.escape(src)}\"\\s*/>")
        "video" -> Regex("<video\\s+src=\"${Regex.escape(src)}\"\\s*/>")
        "audio" -> Regex("<audio\\s+src=\"${Regex.escape(src)}\"\\s*/>")
        else -> return text
    }
    return text.replaceFirst(tagPattern, "")
}

fun removeAttachmentFromContent(content: String, target: Attachment): String {
    val (cleanText, currentAttachments) = parseNoteContentAndAttachments(content)
    val updated = currentAttachments.filter { it != target }
    return createRawContent(cleanText, updated)
}

data class BlockRange(
    val block: NoteContentBlock,
    val range: IntRange
)

fun parseEditorBlockRanges(rawContent: String): List<BlockRange> {
    val results = mutableListOf<BlockRange>()
    val regex = Regex(
        "\\[!\\s*\\[([^\\]]*)\\]\\(([^\\)]+)\\)\\]\\(([^\\)]+)\\)" +
        "|!\\s*\\[([^\\]]*)\\]\\(([^\\)]+)\\)" +
        "|!video\\s*\\[[^\\]]*\\]\\(([^\\)]+)\\)" +
        "|!audio\\s*\\[[^\\]]*\\]\\(([^\\)]+)\\)" +
        "|<(img|video|audio)\\s+src=\"([^\"]+)\"\\s*/>" +
        "|<(img|video|audio)=([^>]+)>" +
        "|<item\\s+checked=\"(true|false)\">[\\s\\S]*?</item>" +
        "|<table[^>]*>[\\s\\S]*?</table>" +
        "|<hr\\s*/?>" +
        "|<details>[\\s\\S]*?</details>" +
        "|<cl>[\\s\\S]*?</cl>" +
        "|<align\\s*=\\s*\"?(center|left|right|justify)\"?\\s*>|</align>" +
        "|<split\\s*/?>"
    )

    var lastEnd = 0
    var checklistIndex = 0
    val alignStack = mutableListOf<androidx.compose.ui.text.style.TextAlign>()

    for (match in regex.findAll(rawContent)) {
        if (lastEnd < match.range.first) {
            val text = rawContent.substring(lastEnd, match.range.first)
            if (text.isNotBlank()) {
                val currentAlign = alignStack.lastOrNull()
                val parseResult = RichTextParser.parseWithMapping(text, hideTags = true)
                if (parseResult.text.text.isNotBlank()) {
                    results.add(BlockRange(
                        NoteContentBlock.TextBlock(parseResult, rawStart = lastEnd, textAlign = currentAlign),
                        lastEnd until match.range.first
                    ))
                }
            }
        }

        val isLinkedImg = match.groupValues[3].isNotEmpty()
        val isMdImg = match.groupValues[5].isNotEmpty()
        val isMdVideo = match.groupValues[6].isNotEmpty()
        val isMdAudio = match.groupValues[7].isNotEmpty()
        val isHtmlMedia = match.groupValues[8].isNotEmpty()
        val isHtmlShortMedia = match.groupValues[10].isNotEmpty()
        val isItem = match.groupValues[12].isNotEmpty()
        val isTable = match.value.startsWith("<table")
        val isHr = match.value.startsWith("<hr")
        val isDetails = match.value.startsWith("<details")
        val isCl = match.value.startsWith("<cl>")
        val isAlign = match.value.startsWith("<align") && match.value != "</align>"
        val isSplit = match.value.startsWith("<split")

        when {
            match.value == "</align>" -> alignStack.removeLastOrNull()
            isAlign -> {
                val alignValue = match.groupValues[13].takeIf { it.isNotEmpty() } ?: run {
                    val nameMatch = Regex("""<align\s*=\s*"?(center|left|right|justify)"?\s*>""").find(match.value)
                    nameMatch?.groupValues?.get(1)
                }
                val align = when (alignValue) {
                    "center" -> androidx.compose.ui.text.style.TextAlign.Center
                    "right" -> androidx.compose.ui.text.style.TextAlign.End
                    "justify" -> androidx.compose.ui.text.style.TextAlign.Justify
                    else -> androidx.compose.ui.text.style.TextAlign.Start
                }
                alignStack.add(align)
            }
            isLinkedImg -> {
                val src = match.groupValues[2]
                val link = match.groupValues[3]
                results.add(BlockRange(
                    NoteContentBlock.ImageBlock(src, linkUrl = link),
                    match.range
                ))
            }
            isMdImg -> {
                val src = match.groupValues[5]
                results.add(BlockRange(
                    NoteContentBlock.ImageBlock(src),
                    match.range
                ))
            }
            isMdVideo -> {
                val src = match.groupValues[6]
                results.add(BlockRange(
                    NoteContentBlock.VideoBlock(src),
                    match.range
                ))
            }
            isMdAudio -> {
                val src = match.groupValues[7]
                results.add(BlockRange(
                    NoteContentBlock.AudioBlock(src),
                    match.range
                ))
            }
            isHtmlMedia -> {
                val mediaType = match.groupValues[8]
                val src = match.groupValues[9]
                when (mediaType) {
                    "img" -> results.add(BlockRange(NoteContentBlock.ImageBlock(src), match.range))
                    "video" -> results.add(BlockRange(NoteContentBlock.VideoBlock(src), match.range))
                    "audio" -> results.add(BlockRange(NoteContentBlock.AudioBlock(src), match.range))
                }
            }
            isHtmlShortMedia -> {
                val mediaType = match.groupValues[10]
                val src = match.groupValues[11]
                when (mediaType) {
                    "img" -> results.add(BlockRange(NoteContentBlock.ImageBlock(src), match.range))
                    "video" -> results.add(BlockRange(NoteContentBlock.VideoBlock(src), match.range))
                    "audio" -> results.add(BlockRange(NoteContentBlock.AudioBlock(src), match.range))
                }
            }
            isItem -> {
                val isChecked = match.groupValues[12] == "true"
                val itemContent = match.value.substringAfter(">").substringBeforeLast("</item>")
                val itemParseResult = RichTextParser.parseWithMapping(itemContent, hideTags = true)
                results.add(BlockRange(
                    NoteContentBlock.ChecklistItemBlock(
                        isChecked = isChecked,
                        parseResult = itemParseResult,
                        rawStart = match.range.first + match.value.indexOf(itemContent),
                        globalIndex = checklistIndex
                    ),
                    match.range
                ))
                checklistIndex++
            }
            isTable -> {
                val tableContent = match.value.substringAfter(">").substringBeforeLast("</table>")
                val tableBlock = parseTableTagToBlock(tableContent)
                results.add(BlockRange(tableBlock, match.range))
            }
            isHr -> {
                results.add(BlockRange(NoteContentBlock.HorizontalRuleBlock, match.range))
            }
            isDetails -> {
                val detailsContent = match.value.substringAfter(">").substringBeforeLast("</details>")
                val summaryRegex = Regex("<summary>(.*?)</summary>", RegexOption.DOT_MATCHES_ALL)
                val summaryMatch = summaryRegex.find(detailsContent)
                val summary = summaryMatch?.groupValues?.get(1)?.trim() ?: ""
                val body = detailsContent.replace(summaryRegex, "").trim()
                results.add(BlockRange(NoteContentBlock.CollapsibleBlock(summary = summary, content = body), match.range))
            }
            isSplit -> { }
            isCl -> {
                val clContent = match.value.removePrefix("<cl>").removeSuffix("</cl>")
                val itemRegex = Regex("<item\\s+checked=\"(true|false)\">([\\s\\S]*?)</item>")
                itemRegex.findAll(clContent).forEach { itemMatch ->
                    val isChecked = itemMatch.groupValues[1] == "true"
                    val itemText = itemMatch.groupValues[2]
                    val itemParseResult = RichTextParser.parseWithMapping(itemText, hideTags = true)
                    results.add(BlockRange(
                        NoteContentBlock.ChecklistItemBlock(
                            isChecked = isChecked,
                            parseResult = itemParseResult,
                            rawStart = match.range.first + itemMatch.range.first,
                            globalIndex = checklistIndex
                        ),
                        match.range
                    ))
                    checklistIndex++
                }
            }
        }
        lastEnd = match.range.last + 1
    }

    if (lastEnd < rawContent.length) {
        val text = rawContent.substring(lastEnd)
        if (text.isNotBlank()) {
            val currentAlign = alignStack.lastOrNull()
            val parseResult = RichTextParser.parseWithMapping(text, hideTags = true)
            if (parseResult.text.text.isNotBlank()) {
                results.add(BlockRange(
                    NoteContentBlock.TextBlock(parseResult, rawStart = lastEnd, textAlign = currentAlign),
                    lastEnd until rawContent.length
                ))
            }
        }
    }

    return results.ifEmpty {
        val parseResult = RichTextParser.parseWithMapping(rawContent, hideTags = true)
        listOf(BlockRange(
            NoteContentBlock.TextBlock(parseResult, rawStart = 0),
            0 until rawContent.length
        ))
    }
}

fun buildPreviewBlocks(content: String): List<NoteContentBlock> {
    val (cleanText, attachments) = parseNoteContentAndAttachments(content)
    val blocks = parseToContentBlocks(cleanText).toMutableList()
    attachments.forEach { att ->
        when (att.type) {
            "drawing" -> blocks.add(NoteContentBlock.DrawingBlock(jsonPath = att.path, previewPath = att.name))
            "voice" -> blocks.add(NoteContentBlock.VoiceBlock(path = att.path))
            else -> blocks.add(NoteContentBlock.FileBlock(name = att.name.ifEmpty { att.path.substringAfterLast('/') }, path = att.path))
        }
    }
    return blocks
}
