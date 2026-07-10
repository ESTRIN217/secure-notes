package com.example.util

import com.example.data.model.Attachment
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

fun parseToContentBlocks(rawText: String): List<NoteContentBlock> {
    val blocks = mutableListOf<NoteContentBlock>()

    val regex = Regex(
        "(?:!\\s*\\[([^\\]]*)\\]\\(([^\\)]+)\\))" +
        "|(?:!video\\s*\\[([^\\]]*)\\]\\(([^\\)]+)\\))" +
        "|(?:!audio\\s*\\[([^\\]]*)\\]\\(([^\\)]+)\\))" +
        "|(?:<item\\s+checked=\"(true|false)\">([\\s\\S]*?)</item>)" +
        "|(?:<(img|video|audio)\\s+src=\"([^\"]+)\"\\s*/>|<(img|video|audio)=([^>]+)>)" +
        "|(<cl>|</cl>)"
    )

    var lastIndex = 0
    val matches = regex.findAll(rawText)
    var checklistItemIndex = 0

    for (match in matches) {
        val preText = rawText.substring(lastIndex, match.range.first)
        if (preText.isNotEmpty()) {
            val parseResult = RichTextParser.parseWithMapping(preText, hideTags = true)
            if (parseResult.text.text.isNotBlank()) {
                blocks.add(NoteContentBlock.TextBlock(parseResult, rawStart = lastIndex))
            }
        }

        val isMdImg = match.groupValues[2].isNotEmpty() && match.groupValues[1].isNotEmpty() || (match.groupValues[2].isNotEmpty() && match.value.startsWith("!"))
        val isMdVideo = match.groupValues[4].isNotEmpty()
        val isMdAudio = match.groupValues[6].isNotEmpty()
        val isItem = match.groupValues[7].isNotEmpty()
        val isHtmlMedia = match.groupValues[9].isNotEmpty()
        val isHtmlShortMedia = match.groupValues[11].isNotEmpty()

        when {
            isMdImg -> {
                val src = match.groupValues[2]
                blocks.add(NoteContentBlock.ImageBlock(src))
            }
            isMdVideo -> {
                val src = match.groupValues[4]
                blocks.add(NoteContentBlock.VideoBlock(src))
            }
            isMdAudio -> {
                val src = match.groupValues[6]
                blocks.add(NoteContentBlock.AudioBlock(src))
            }
            isItem -> {
                val isChecked = match.groupValues[7] == "true"
                val itemText = match.groupValues[8]
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
                val mediaType = match.groupValues[9]
                val src = match.groupValues[10]
                when (mediaType) {
                    "img" -> blocks.add(NoteContentBlock.ImageBlock(src))
                    "video" -> blocks.add(NoteContentBlock.VideoBlock(src))
                    "audio" -> blocks.add(NoteContentBlock.AudioBlock(src))
                }
            }
            isHtmlShortMedia -> {
                val mediaType = match.groupValues[11]
                val src = match.groupValues[12]
                when (mediaType) {
                    "img" -> blocks.add(NoteContentBlock.ImageBlock(src))
                    "video" -> blocks.add(NoteContentBlock.VideoBlock(src))
                    "audio" -> blocks.add(NoteContentBlock.AudioBlock(src))
                }
            }
        }

        lastIndex = match.range.last + 1
    }

    if (lastIndex < rawText.length) {
        val remainingText = rawText.substring(lastIndex)
        if (remainingText.isNotEmpty()) {
            val parseResult = RichTextParser.parseWithMapping(remainingText, hideTags = true)
            if (parseResult.text.text.isNotBlank()) {
                blocks.add(NoteContentBlock.TextBlock(parseResult, rawStart = lastIndex))
            }
        }
    }

    return blocks.ifEmpty {
        val parseResult = RichTextParser.parseWithMapping(rawText, hideTags = true)
        listOf(NoteContentBlock.TextBlock(parseResult, rawStart = 0))
    }
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
