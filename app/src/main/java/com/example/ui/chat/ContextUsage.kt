package com.example.ui.chat

/**
 * Ocupación de la ventana de contexto de la conversación actual (spec 034, RF-248…RF-257).
 *
 * Pieza **pura** (sin `android`, sin I/O): la UI pinta estado y lanza eventos, el `AiViewModel`
 * decide. Los tokens usados se obtienen exactos del trozo `done` de Ollama (`prompt_eval_count +
 * eval_count`) o se estiman con ~4 caracteres por token cuando no hay dato del servidor.
 */

/** Presupuesto de contexto que el motor on-device reserva siempre (definido upstream en el binding, no aquí). */
const val LLAMA_CPP_CONTEXT_TOKENS = 8192

/** Fracción a partir de la que el anillo se pinta en color de aviso/error (spec 034, RF-254). */
const val NEAR_LIMIT_FRACTION = 0.85f

/** Una lectura de ocupación: cuántos tokens usa la conversación, contra qué ventana, y si es exacto o estimado. */
data class ContextUsage(val usedTokens: Int, val windowTokens: Int, val exact: Boolean)

/** Estimación tosca: ~4 caracteres por token (spec 034, RF-251). Vacío o en blanco → 0. */
fun estimateTokens(text: String): Int {
    if (text.isBlank()) return 0
    return (text.length + 3) / 4
}

/** Fracción usada, acotada a [0, 1]; una ventana no positiva se trata como «sin dato» y devuelve 0f. */
fun contextUsageFraction(used: Int, window: Int): Float {
    if (window <= 0) return 0f
    return (used.coerceAtLeast(0).toFloat() / window).coerceIn(0f, 1f)
}

/** True si la conversación está a punto de desbordar el contexto (spec 034, RF-254). */
fun isContextNearLimit(fraction: Float): Boolean = fraction >= NEAR_LIMIT_FRACTION

/** `null` si no hay ventana conocida o válida: el indicador no se pinta (spec 034, RF-253). `used` se sanea a ≥ 0. */
fun buildContextUsage(usedTokens: Int, windowTokens: Int?, exact: Boolean): ContextUsage? {
    if (windowTokens == null || windowTokens <= 0) return null
    return ContextUsage(
        usedTokens = usedTokens.coerceAtLeast(0),
        windowTokens = windowTokens,
        exact = exact
    )
}
