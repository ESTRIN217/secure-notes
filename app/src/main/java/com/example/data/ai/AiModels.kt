package com.example.data.ai

enum class AiAction {
    GENERATE,
    SUMMARIZE,
    REWRITE,
    TRANSLATE
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

data class AiRequest(
    val action: AiAction,
    val prompt: String = "",
    val selectedText: String = "",
    val context: String = "",
    val rewriteStyle: RewriteStyle = RewriteStyle.FORMAL,
    val targetLanguage: String = "en"
)

data class AiResult(
    val text: String,
    val action: AiAction
)
