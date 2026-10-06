package com.example.ui.chat

import com.example.data.ai.AiAction
import com.example.data.ai.AiRequest

/**
 * L1 — qué sugerencias se ven, incluida la visibilidad condicional de la de resumen.
 *
 * Las etiquetas **no** se resuelven aquí: llegan vacías y quien llama (el Composable) las rellena
 * con `stringResource`, porque este archivo no sabe de recursos ni de `R` (RF-37, RF-40).
 * Los `id` son claves estables en inglés: en T8 son la `key` de la fila de sugerencias (RNF-15).
 */
enum class ChatSuggestionAction { GENERATE, SUMMARIZE }

/** Sugerencia del welcome: par de datos con etiqueta (lo que se ve) y acción (lo que ejecuta). */
data class ChatSuggestion(
    val id: String,
    val label: String,
    val action: ChatSuggestionAction
)

/**
 * Devuelve las sugerencias visibles: las tres de generación, más la de resumen **solo** si hay algo
 * que resumir —texto seleccionado, contenido de nota o adjuntos pendientes (RF-38).
 */
fun visibleSuggestions(
    hasSelectedText: Boolean,
    hasNoteContent: Boolean,
    hasAttachments: Boolean
): List<ChatSuggestion> {
    val generation = listOf(
        ChatSuggestion(id = "poem", label = "", action = ChatSuggestionAction.GENERATE),
        ChatSuggestion(id = "ideas", label = "", action = ChatSuggestionAction.GENERATE),
        ChatSuggestion(id = "explain", label = "", action = ChatSuggestionAction.GENERATE)
    )
    if (!hasSelectedText && !hasNoteContent && !hasAttachments) return generation
    return generation + SUMMARIZE_SUGGESTION
}

/**
 * L2 — qué petición lanza cada sugerencia (RF-37): las de generación mandan **su etiqueta** como
 * petición con la acción de generación; la de resumen lanza la **acción de resumen** con la petición
 * de resumen establecida. Las dos viajan con el texto seleccionado, el contexto de la nota y el
 * idioma objetivo, que de otro modo se perderían en silencio.
 */
fun requestFor(
    suggestion: ChatSuggestion,
    selectedText: String,
    noteContent: String,
    summarizePrompt: String,
    targetLanguage: String
): AiRequest {
    val summarize = suggestion.action == ChatSuggestionAction.SUMMARIZE
    return AiRequest(
        action = if (summarize) AiAction.SUMMARIZE else AiAction.GENERATE,
        prompt = if (summarize) summarizePrompt else suggestion.label,
        selectedText = selectedText,
        context = noteContent,
        targetLanguage = targetLanguage
    )
}

private val SUMMARIZE_SUGGESTION =
    ChatSuggestion(id = "summarize", label = "", action = ChatSuggestionAction.SUMMARIZE)
