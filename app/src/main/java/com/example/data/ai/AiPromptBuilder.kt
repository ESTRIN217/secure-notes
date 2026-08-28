package com.example.data.ai

import com.example.util.RichTextParser

object AiPromptBuilder {

    val systemPrompts = mapOf(
        AiAction.GENERATE to "You are a creative writing assistant. Follow the user's instructions to produce engaging text that matches the tone and language of the surrounding note. Use Markdown wherever it improves readability.",
        AiAction.SUMMARIZE to "You are a precise summarizer. Condense the provided text while preserving every key fact, number, and conclusion. Always respond in the same language as the source text.",
        AiAction.FIX_GRAMMAR to "You are a careful editor. Correct only grammar, spelling, and punctuation errors. Preserve the original meaning, tone, and structure without rewriting or adding content.",
    )

    fun resolveSystemPrompt(action: AiAction, customPrompt: String): String {
        return customPrompt.ifBlank { systemPrompts[action] ?: "" }
    }

    fun resolveSystemPromptResource(context: android.content.Context, action: AiAction, customPrompt: String): String {
        if (customPrompt.isNotBlank()) return customPrompt
        val resId = when (action) {
            AiAction.GENERATE -> com.example.R.string.ai_prompt_generate
            AiAction.SUMMARIZE -> com.example.R.string.ai_prompt_summarize
            AiAction.FIX_GRAMMAR -> com.example.R.string.ai_prompt_fix_grammar
            else -> return ""
        }
        return context.getString(resId)
    }

    fun buildUserPrompt(request: AiRequest): String {
        val mdContext = RichTextParser.markdownForAI(request.context)
        val mdSelectedText = RichTextParser.markdownForAI(request.selectedText)
        return when (request.action) {
            AiAction.GENERATE -> {
                val contextPrefix = if (mdContext.isNotBlank()) {
                    "Current note context:\n${mdContext}\n\n"
                } else ""
                "${contextPrefix}${request.prompt}"
            }
            AiAction.SUMMARIZE -> {
                "Summarize the following text:\n\n${mdSelectedText.ifBlank { mdContext }}"
            }
            AiAction.FIX_GRAMMAR -> {
                "Fix grammar and spelling in the following text:\n\n${mdSelectedText.ifBlank { mdContext }}"
            }
        }
    }
}
