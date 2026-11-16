package com.example.data.ai

/**
 * Ocupación exacta del contexto según Ollama (spec 034, RF-250).
 *
 * El trozo final (`done: true`) de `/api/chat` —tanto en streaming como en la respuesta completa—
 * trae `prompt_eval_count` y `eval_count`. El chat la lee tras la respuesta; nadie la persiste.
 */

data class OllamaContextUsage(
    val promptTokens: Int,
    val evalTokens: Int,
    /** Duración del prompt eval en nanosegundos (tal como la reporta Ollama); `null` si el trozo no la trae. */
    val promptEvalDurationNs: Long? = null,
    /** Duración de la generación en nanosegundos; `null` si el trozo no la trae. */
    val evalDurationNs: Long? = null
) {
    /** Total de tokens que Ollama evaluó para servir la respuesta (prompt + generación). */
    val sum: Int get() = promptTokens + evalTokens
}

/** `null` si la línea no es un trozo `done: true` con ambos contadores (p. ej. trozos de contenido o JSON mal formado). */
fun parseOllamaContextUsage(doneLineJson: String): OllamaContextUsage? = try {
    if (doneLineJson.isBlank()) return null
    val json = org.json.JSONObject(doneLineJson)
    if (!json.optBoolean("done", false)) return null
    val prompt = json.count("prompt_eval_count") ?: return null
    val eval = json.count("eval_count") ?: return null
    OllamaContextUsage(
        promptTokens = prompt,
        evalTokens = eval,
        promptEvalDurationNs = json.positiveLong("prompt_eval_duration"),
        evalDurationNs = json.positiveLong("eval_duration")
    )
} catch (_: Exception) {
    null
}

/** Solo acepta `Number`: ni strings numéricos ni booleanos ni ausencia. Negativos tampoco valen. */
private fun org.json.JSONObject.count(key: String): Int? = when (val value = opt(key)) {
    is Number -> value.toInt().takeIf { it >= 0 }
    else -> null
}

/** Duración opcional: solo vale un `Number` estrictamente positivo; cualquier otra cosa o ausencia → null. */
private fun org.json.JSONObject.positiveLong(key: String): Long? = when (val value = opt(key)) {
    is Number -> value.toLong().takeIf { it > 0 }
    else -> null
}