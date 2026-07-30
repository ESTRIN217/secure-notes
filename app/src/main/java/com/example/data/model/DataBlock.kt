package com.example.data.model

import com.example.util.NoteContentBlockConverter
import com.example.util.preprocessMarkdownBlocks
import org.json.JSONArray
import org.json.JSONObject

enum class BlockType {
    TEXT, HEADING1, HEADING2, HEADING3, HEADING4,
    BULLET_LIST, NUMBERED_LIST, CHECKLIST_ITEM,
    QUOTE, CODE_BLOCK,
    IMAGE, VIDEO, AUDIO, DRAWING, VOICE, FILE,
    TABLE, HORIZONTAL_RULE, COLLAPSIBLE
}

data class DataBlock(
    val type: BlockType,
    val content: String = "",
    val meta: Map<String, String> = emptyMap()
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("type", type.name)
        obj.put("content", content)
        val metaObj = JSONObject()
        meta.forEach { (k, v) -> metaObj.put(k, v) }
        obj.put("meta", metaObj)
        return obj
    }

    fun toLegacyString(): String {
        return when (type) {
            BlockType.TEXT, BlockType.HEADING1, BlockType.HEADING2, BlockType.HEADING3, BlockType.HEADING4,
            BlockType.BULLET_LIST, BlockType.NUMBERED_LIST,
            BlockType.QUOTE, BlockType.CODE_BLOCK -> content
            BlockType.CHECKLIST_ITEM -> "<item checked=\"${meta["checked"] ?: "false"}\">$content</item>"
            BlockType.IMAGE -> {
                val link = meta["linkUrl"]
                if (link != null) "![${content}]($content)($link)" else "<img src=\"$content\"/>"
            }
            BlockType.VIDEO -> "<video src=\"$content\"/>"
            BlockType.AUDIO -> "<audio src=\"$content\"/>"
            BlockType.DRAWING -> ""
            BlockType.VOICE -> ""
            BlockType.FILE -> ""
            BlockType.TABLE -> "<table>$content</table>"
            BlockType.HORIZONTAL_RULE -> "<hr/>"
            BlockType.COLLAPSIBLE -> "<details><summary>${meta["summary"] ?: ""}</summary>$content</details>"
        }
    }

    companion object {
        fun fromJson(obj: JSONObject): DataBlock {
            val type = try {
                BlockType.valueOf(obj.getString("type"))
            } catch (e: Exception) {
                BlockType.TEXT
            }
            val content = obj.optString("content", "")
            val metaObj = obj.optJSONObject("meta")
            val meta = if (metaObj != null) {
                mutableMapOf<String, String>().apply {
                    metaObj.keys().forEach { k -> put(k, metaObj.optString(k, "")) }
                }
            } else emptyMap()
            return DataBlock(type = type, content = content, meta = meta)
        }

        fun serialize(blocks: List<DataBlock>): String {
            val arr = JSONArray()
            blocks.forEach { arr.put(it.toJson()) }
            return arr.toString()
        }

        fun deserialize(json: String): List<DataBlock>? {
            return try {
                val arr = JSONArray(json)
                (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
            } catch (e: Exception) {
                null
            }
        }

        fun migrateLegacyContent(rawContent: String): List<DataBlock> {
            val (textPart, attachments) = parseNoteContentAndAttachments(rawContent)
            val processed = preprocessMarkdownBlocks(textPart)

            val blocks = mutableListOf<DataBlock>()
            if (processed.isBlank() && attachments.isEmpty()) return blocks

            val regex = Regex(
                "\\[!\\s*\\[([^\\]]*)\\]\\(([^\\)]+)\\)\\]\\(([^\\)]+)\\)" +
                "|!\\s*\\[([^\\]]*)\\]\\(([^\\)]+)\\)" +
                "|!video\\s*\\[[^\\]]*\\]\\(([^\\)]+)\\)" +
                "|!audio\\s*\\[[^\\]]*\\]\\(([^\\)]+)\\)" +
                "|<(img|video|audio)\\s+src=\"([^\"]+)\"\\s*/>" +
                "|<(img|video|audio)=([^>]+)>" +
                "|<item\\s+checked=\"(true|false)\">[\\s\\S]*?</item>" +
                "|<table[^>]*>[\\s\\S]*?</table>" +
                "|<ol>[\\s\\S]*?</ol>" +
                "|<ul>[\\s\\S]*?</ul>" +
                "|<hr\\s*/?>" +
                "|<details>[\\s\\S]*?</details>" +
                "|<cl>[\\s\\S]*?</cl>" +
                "|<align\\s*=\\s*\"?(center|left|right|justify)\"?\\s*>|</align>" +
                "|<split\\s*/?>"
            )

            var lastEnd = 0
            val alignStack = mutableListOf<String>()

            for (match in regex.findAll(processed)) {
                if (lastEnd < match.range.first) {
                    val text = processed.substring(lastEnd, match.range.first).trim()
                    if (text.isNotEmpty()) {
                        blocks.addAll(NoteContentBlockConverter.convertTextToBlocks(text))
                    }
                }

                val matchVal = match.value
                when {
                    matchVal == "</align>" -> alignStack.removeLastOrNull()
                    matchVal.startsWith("<align") -> {
                        val a = Regex("""<align\s*=\s*"?(center|left|right|justify)"?\s*>""").find(matchVal)
                        alignStack.add(a?.groupValues?.get(1) ?: "left")
                    }
                    matchVal.startsWith("<ol>") -> {
                        val content = matchVal.substringAfter(">").substringBeforeLast("</ol>")
                        Regex("<li>([\\s\\S]*?)</li>").findAll(content).forEach { li ->
                            val liContent = li.groupValues[1].trim()
                            if (liContent.isNotEmpty()) blocks.add(DataBlock(type = BlockType.NUMBERED_LIST, content = liContent))
                        }
                    }
                    matchVal.startsWith("<ul>") -> {
                        val content = matchVal.substringAfter(">").substringBeforeLast("</ul>")
                        Regex("<li>([\\s\\S]*?)</li>").findAll(content).forEach { li ->
                            val liContent = li.groupValues[1].trim()
                            if (liContent.isNotEmpty()) blocks.add(DataBlock(type = BlockType.BULLET_LIST, content = liContent))
                        }
                    }
                    matchVal.startsWith("<cl>") -> {
                        val content = matchVal.removePrefix("<cl>").removeSuffix("</cl>")
                        Regex("<item\\s+checked=\"(true|false)\">([\\s\\S]*?)</item>").findAll(content).forEach { itemMatch ->
                            val checked = itemMatch.groupValues[1] == "true"
                            val text = itemMatch.groupValues[2].trim()
                            blocks.add(DataBlock(type = BlockType.CHECKLIST_ITEM, content = text, meta = mapOf("checked" to checked.toString())))
                        }
                    }
                    matchVal.startsWith("<hr") -> blocks.add(DataBlock(type = BlockType.HORIZONTAL_RULE))
                    matchVal.startsWith("<table") -> {
                        val tContent = matchVal.substringAfter(">").substringBeforeLast("</table>")
                        blocks.add(DataBlock(type = BlockType.TABLE, content = tContent))
                    }
                    matchVal.startsWith("<details") -> {
                        val dContent = matchVal.substringAfter(">").substringBeforeLast("</details>")
                        val summaryRegex = Regex("<summary>(.*?)</summary>", RegexOption.DOT_MATCHES_ALL)
                        val summary = summaryRegex.find(dContent)?.groupValues?.get(1)?.trim() ?: ""
                        val body = dContent.replace(summaryRegex, "").trim()
                        blocks.add(DataBlock(type = BlockType.COLLAPSIBLE, content = body, meta = mapOf("summary" to summary)))
                    }
                    matchVal.startsWith("<split") -> { }
                    else -> {
                        val mediaBlock = NoteContentBlockConverter.matchToMediaBlock(match, matchVal)
                        if (mediaBlock != null) blocks.add(mediaBlock)
                    }
                }
                lastEnd = match.range.last + 1
            }

            if (lastEnd < processed.length) {
                val text = processed.substring(lastEnd).trim()
                if (text.isNotEmpty()) {
                    blocks.addAll(NoteContentBlockConverter.convertTextToBlocks(text))
                }
            }

            attachments.forEach { att ->
                when (att.type) {
                    "drawing" -> blocks.add(DataBlock(type = BlockType.DRAWING, content = att.path, meta = mapOf("previewPath" to att.name)))
                    "voice" -> blocks.add(DataBlock(type = BlockType.VOICE, content = att.path))
                    "file" -> blocks.add(DataBlock(type = BlockType.FILE, content = att.path, meta = mapOf("name" to att.name)))
                }
            }

            return blocks.ifEmpty {
                val trimmed = processed.trim()
                if (trimmed.isNotEmpty()) listOf(DataBlock(type = BlockType.TEXT, content = trimmed))
                else emptyList()
            }
        }
    }
}
