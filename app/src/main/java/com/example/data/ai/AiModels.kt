package com.example.data.ai

enum class AiAction {
    GENERATE,
    SUMMARIZE,
    FIX_GRAMMAR
}

enum class AiBackend {
    OLLAMA,
    ON_DEVICE
}

data class FileAttachment(
    val name: String,
    val content: String,
    val source: String = "external"
)

data class ChatMessage(
    val role: String,
    val content: String
)

data class AiRequest(
    val action: AiAction,
    val prompt: String = "",
    val selectedText: String = "",
    val context: String = "",
    val targetLanguage: String = "en",
    val customSystemPrompt: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val attachments: List<FileAttachment> = emptyList(),
    val tools: List<Map<String, Any>> = emptyList(),
    val toolResults: List<ToolResult> = emptyList(),
    val temperature: Float = 0.7f,
    val topK: Int = 40,
    val topP: Float = 0.9f,
    val repetitionPenalty: Float = 1.1f,
    val maxTokens: Int = 256
)

data class ToolResult(
    val toolCallId: String,
    val name: String,
    val result: String
)

data class ToolParam(
    val name: String,
    val type: String,
    val description: String = "",
    val required: Boolean = true
)

data class ToolSpec(
    val name: String,
    val description: String,
    val parameters: List<ToolParam>
)

data class ToolCall(
    val id: String,
    val name: String,
    val arguments: Map<String, Any>
)

data class AiResult(
    val text: String,
    val action: AiAction
)
