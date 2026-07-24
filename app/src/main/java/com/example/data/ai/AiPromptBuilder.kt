package com.example.data.ai

import com.example.util.RichTextParser

object AiPromptBuilder {

    val systemPrompts = mapOf(
        AiAction.GENERATE to "Eres un asistente de escritura creativa. Genera texto basado en la solicitud del usuario usando formato markdown cuando corresponda.",
        AiAction.SUMMARIZE to "Resume el siguiente texto de forma concisa conservando la información clave.",
        AiAction.REWRITE to "Eres un asistente de escritura. Reescribe el texto en el estilo indicado.",
        AiAction.TRANSLATE to "Traduce el siguiente texto al idioma especificado. Devuelve solo la traducción sin explicaciones."
    )

    fun resolveSystemPrompt(action: AiAction, customPrompt: String): String {
        return customPrompt.ifBlank { systemPrompts[action] ?: "" }
    }

    fun buildUserPrompt(request: AiRequest): String {
        val cleanContext = RichTextParser.cleanForAI(request.context)
        val cleanSelectedText = RichTextParser.cleanForAI(request.selectedText)
        return when (request.action) {
            AiAction.GENERATE -> {
                val contextPrefix = if (cleanContext.isNotBlank()) {
                    "Contexto de la nota actual:\n${cleanContext}\n\n"
                } else ""
                "${contextPrefix}${request.prompt}"
            }
            AiAction.SUMMARIZE -> {
                "Resume el siguiente texto:\n\n${cleanSelectedText.ifBlank { cleanContext }}"
            }
            AiAction.REWRITE -> {
                val styleDesc = when (request.rewriteStyle) {
                    RewriteStyle.FORMAL -> "formal y profesional"
                    RewriteStyle.CASUAL -> "casual y conversacional"
                    RewriteStyle.POETIC -> "florido, rítmico y expresivo"
                    RewriteStyle.PROFESSIONAL -> "apropiado para negocios y pulido"
                }
                val contextPrefix = if (cleanContext.isNotBlank()) {
                    "Contexto de la nota:\n${cleanContext}\n\n"
                } else ""
                "${contextPrefix}Reescribe el siguiente texto en un estilo $styleDesc. Este es un ejercicio creativo:\n\n${cleanSelectedText}"
            }
            AiAction.TRANSLATE -> {
                "Traduce el siguiente texto a ${request.targetLanguage}:\n\n${cleanSelectedText}"
            }
        }
    }
}
