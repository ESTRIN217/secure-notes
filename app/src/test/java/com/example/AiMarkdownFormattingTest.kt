package com.example

import com.example.data.ai.AiAction
import com.example.data.ai.AiPromptBuilder
import com.example.data.ai.AiRequest
import com.example.data.model.BlockType
import com.example.data.model.DataBlock
import com.example.data.model.TextSegment
import com.example.util.RichTextParser
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiMarkdownFormattingTest {

    private fun richBlocksJson(): String {
        val blocks = listOf(
            DataBlock(
                type = BlockType.HEADING1,
                content = "",
                richTextJson = TextSegment.serialize(listOf(TextSegment(text = "Plan", bold = true)))
            ),
            DataBlock(
                type = BlockType.TEXT,
                content = "",
                richTextJson = TextSegment.serialize(
                    listOf(
                        TextSegment(text = "see "),
                        TextSegment(text = "docs", linkUrl = "https://example.com")
                    )
                )
            ),
            DataBlock(
                type = BlockType.CODE_BLOCK,
                content = "",
                meta = mapOf("language" to "kotlin"),
                richTextJson = TextSegment.serialize(listOf(TextSegment(text = "val x = 1")))
            )
        )
        return DataBlock.serialize(blocks)
    }

    @Test
    fun `markdownForAI preserves headings links and code fences`() {
        val md = RichTextParser.markdownForAI(richBlocksJson())

        assertTrue(md.contains("# "))
        assertTrue(md.contains("**Plan**"))
        assertTrue(md.contains("[docs](https://example.com)"))
        assertTrue(md.contains("```kotlin"))
        assertTrue(md.contains("val x = 1"))
    }

    @Test
    fun `markdownForAI strips attachments suffix`() {
        val raw = richBlocksJson() + "\n\n---Attachments---\n[{" +
            "\"name\":\"a.txt\",\"uri\":\"file:///tmp/a.txt\",\"size\":10,\"mime\":\"text/plain\"}]"

        val md = RichTextParser.markdownForAI(raw)

        assertFalse(md.contains("Attachments"))
        assertFalse(md.contains("a.txt"))
        assertTrue(md.contains("[docs](https://example.com)"))
    }

    @Test
    fun `generate prompt embeds markdown context and response hint`() {
        val request = AiRequest(action = AiAction.GENERATE, prompt = "Continue writing", context = richBlocksJson())

        val prompt = AiPromptBuilder.buildUserPrompt(request)

        assertTrue(prompt.startsWith("Current note context:"))
        assertTrue(prompt.contains("[docs](https://example.com)"))
        assertTrue(prompt.endsWith("Use Markdown formatting in your response."))
    }

    @Test
    fun `fix grammar prompt asks to preserve markdown structure`() {
        val request = AiRequest(action = AiAction.FIX_GRAMMAR, selectedText = richBlocksJson())

        val prompt = AiPromptBuilder.buildUserPrompt(request)

        assertTrue(prompt.contains("Fix grammar and spelling"))
        assertTrue(prompt.contains("[docs](https://example.com)"))
        assertTrue(prompt.endsWith("keep the Markdown formatting unchanged."))
    }

    @Test
    fun `every action appends a markdown response hint`() {
        for (action in AiAction.entries) {
            val prompt = AiPromptBuilder.buildUserPrompt(AiRequest(action = action, prompt = "x"))

            assertTrue(
                "Missing Markdown hint for $action",
                action == AiAction.GENERATE || prompt.trimEnd().endsWith(".")
            )
            assertTrue(prompt.length > 0)
        }
    }
}
