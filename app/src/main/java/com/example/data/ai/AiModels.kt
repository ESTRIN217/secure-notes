package com.example.data.ai

enum class AiAction {
    GENERATE,
    SUMMARIZE,
    REWRITE,
    TRANSLATE,
    MAKE_SHORTER,
    FIX_GRAMMAR,
    EXPLAIN
}

enum class AiBackend {
    OLLAMA,
    ON_DEVICE
}

enum class RewriteStyle {
    FORMAL,
    CASUAL,
    POETIC,
    PROFESSIONAL
}

data class ChatMessage(
    val role: String,
    val content: String
)

data class AiRequest(
    val action: AiAction,
    val prompt: String = "",
    val selectedText: String = "",
    val context: String = "",
    val rewriteStyle: RewriteStyle = RewriteStyle.FORMAL,
    val targetLanguage: String = "en",
    val customSystemPrompt: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val temperature: Float = 0.7f,
    val topK: Int = 40,
    val topP: Float = 0.9f,
    val repetitionPenalty: Float = 1.1f,
    val maxTokens: Int = 256
)

data class AiResult(
    val text: String,
    val action: AiAction
)
