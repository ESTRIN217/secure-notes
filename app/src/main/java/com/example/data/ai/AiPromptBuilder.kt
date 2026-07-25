package com.example.data.ai

import com.example.util.RichTextParser

object AiPromptBuilder {

    val systemPrompts = mapOf(
        AiAction.GENERATE to "You are a creative writing assistant. Generate text based on the user's request using markdown formatting when appropriate.",
        AiAction.SUMMARIZE to "Summarize the following text concisely while preserving key information.",
        AiAction.REWRITE to "You are a writing assistant. Rewrite the text in the indicated style.",
        AiAction.TRANSLATE to "Translate the following text to the specified language. Return only the translation without explanations."
    )

    fun resolveSystemPrompt(action: AiAction, customPrompt: String): String {
        return customPrompt.ifBlank { systemPrompts[action] ?: "" }
    }

    fun resolveSystemPromptResource(context: android.content.Context, action: AiAction, customPrompt: String): String {
        if (customPrompt.isNotBlank()) return customPrompt
        val resId = when (action) {
            AiAction.GENERATE -> com.example.R.string.ai_prompt_generate
            AiAction.SUMMARIZE -> com.example.R.string.ai_prompt_summarize
            AiAction.REWRITE -> com.example.R.string.ai_prompt_rewrite
            AiAction.TRANSLATE -> com.example.R.string.ai_prompt_translate
        }
        return context.getString(resId)
    }

    fun buildUserPrompt(request: AiRequest): String {
        val cleanContext = RichTextParser.cleanForAI(request.context)
        val cleanSelectedText = RichTextParser.cleanForAI(request.selectedText)
        return when (request.action) {
            AiAction.GENERATE -> {
                val contextPrefix = if (cleanContext.isNotBlank()) {
                    "Current note context:\n${cleanContext}\n\n"
                } else ""
                "${contextPrefix}${request.prompt}"
            }
            AiAction.SUMMARIZE -> {
                "Summarize the following text:\n\n${cleanSelectedText.ifBlank { cleanContext }}"
            }
            AiAction.REWRITE -> {
                val styleDesc = when (request.rewriteStyle) {
                    RewriteStyle.FORMAL -> "formal and professional"
                    RewriteStyle.CASUAL -> "casual and conversational"
                    RewriteStyle.POETIC -> "florid, rhythmic, and expressive"
                    RewriteStyle.PROFESSIONAL -> "business-appropriate and polished"
                }
                val contextPrefix = if (cleanContext.isNotBlank()) {
                    "Note context:\n${cleanContext}\n\n"
                } else ""
                "${contextPrefix}Rewrite the following text in a $styleDesc style:\n\n${cleanSelectedText}"
            }
            AiAction.TRANSLATE -> {
                "Translate the following text to ${request.targetLanguage}:\n\n${cleanSelectedText}"
            }
        }
    }
}
