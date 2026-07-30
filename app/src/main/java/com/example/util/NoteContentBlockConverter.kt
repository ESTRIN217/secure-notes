package com.example.util

import com.example.data.model.BlockType
import com.example.data.model.DataBlock

object NoteContentBlockConverter {
    fun matchToMediaBlock(match: MatchResult, matchVal: String): DataBlock? {
        val isLinkedImg = match.groupValues[3].isNotEmpty()
        val isMdImg = match.groupValues[5].isNotEmpty()
        val isMdVideo = match.groupValues[7].isNotEmpty()
        val isMdAudio = match.groupValues[9].isNotEmpty()
        val isHtmlMedia = match.groupValues[11].isNotEmpty()
        val isHtmlShortMedia = match.groupValues[13].isNotEmpty()

        return when {
            isLinkedImg -> DataBlock(type = BlockType.IMAGE, content = match.groupValues[2], meta = mapOf("linkUrl" to match.groupValues[3]))
            isMdImg -> DataBlock(type = BlockType.IMAGE, content = match.groupValues[5])
            isMdVideo -> DataBlock(type = BlockType.VIDEO, content = match.groupValues[7])
            isMdAudio -> DataBlock(type = BlockType.AUDIO, content = match.groupValues[9])
            isHtmlMedia -> {
                val type = match.groupValues[11]
                val src = match.groupValues[12]
                when (type) {
                    "img" -> DataBlock(type = BlockType.IMAGE, content = src)
                    "video" -> DataBlock(type = BlockType.VIDEO, content = src)
                    "audio" -> DataBlock(type = BlockType.AUDIO, content = src)
                    else -> null
                }
            }
            isHtmlShortMedia -> {
                val type = match.groupValues[13]
                val src = match.groupValues[14]
                when (type) {
                    "img" -> DataBlock(type = BlockType.IMAGE, content = src)
                    "video" -> DataBlock(type = BlockType.VIDEO, content = src)
                    "audio" -> DataBlock(type = BlockType.AUDIO, content = src)
                    else -> null
                }
            }
            else -> null
        }
    }

    fun convertTextToBlocks(text: String): List<DataBlock> {
        val blocks = mutableListOf<DataBlock>()
        val lines = text.split("\n")
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            val heading = Regex("^(#{1,6})\\s").find(trimmed)
            if (heading != null) {
                val level = heading.groupValues[1].length
                val htype = when (level) {
                    1 -> BlockType.HEADING1
                    2 -> BlockType.HEADING2
                    3 -> BlockType.HEADING3
                    4 -> BlockType.HEADING4
                    else -> BlockType.TEXT
                }
                blocks.add(DataBlock(type = htype, content = trimmed.substring(heading.value.length)))
            } else if (trimmed.startsWith("- [ ] ") || trimmed.startsWith("- [x] ")) {
                val checked = trimmed.startsWith("- [x] ")
                blocks.add(DataBlock(type = BlockType.CHECKLIST_ITEM, content = trimmed.substring(6), meta = mapOf("checked" to checked.toString())))
            } else if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                blocks.add(DataBlock(type = BlockType.BULLET_LIST, content = trimmed.substring(2)))
            } else if (trimmed.matches(Regex("^\\d+\\.\\s"))) {
                blocks.add(DataBlock(type = BlockType.NUMBERED_LIST, content = trimmed.substringAfter(". ")))
            } else {
                blocks.add(DataBlock(type = BlockType.TEXT, content = trimmed))
            }
        }
        return blocks
    }
}
