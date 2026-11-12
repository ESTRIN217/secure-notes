package com.example.data.ai

/**
 * Esfuerzo de razonamiento del chat (spec 033): decide el tope de tokens y el `think`
 * de la **siguiente** petición.
 *
 * Es **puro** —no importa `android` ni Compose— para que corra en tests JVM donde
 * Robolectric no corre (RNF-140). El estado (`DEFAULT` al arrancar el proceso) vive en
 * `AiViewModel`; este archivo solo expone el tipo y los dos mapeos (RF-218, RF-219).
 *
 * «Ilimitado» no existe de forma segura en llama.cpp: `stop_generation_position` es
 * `posición + prompt + n_predict` en entero de 32 bits, y `Int.MAX_VALUE` desbordaría;
 * por eso [reasoningMaxTokens] devuelve `-1` para `MAX` y es el motor de Ollama quien
 * interpreta `num_predict = -1` como ilimitado, mientras llama.cpp aplica un tope
 * grande documentado (1 000 000) en `LlamaCppEngine` (RNF-143).
 */

enum class ReasoningEffort {
    /** Usa `ai_max_tokens` y no envía `think` (el modelo decide su default). */
    DEFAULT,

    /** Usa `ai_max_tokens` y pide `think = false`: sin pensamiento en Ollama. */
    OFF,

    /** Tope 512, `think = true`. */
    LOW,

    /** Tope 2048, `think = true`. */
    MEDIUM,

    /** Tope 8192, `think = true`. */
    HIGH,

    /** Tope ilimitado en Ollama (`-1`), tope grande en llama.cpp; `think = true`. */
    MAX,
}

/**
 * Tope de tokens según el esfuerzo (RF-218).
 *
 * `DEFAULT` y `OFF` usan la preferencia de la app (`ai_max_tokens`), el resto sus límites
 * fijos; `MAX` devuelve `-1`, el convenio de Ollama para «ilimitado» (llama.cpp lo traduce
 * a un tope grande propio, ver cabecera).
 */
fun reasoningMaxTokens(pref: Int, effort: ReasoningEffort): Int = when (effort) {
    ReasoningEffort.DEFAULT, ReasoningEffort.OFF -> pref
    ReasoningEffort.LOW -> 512
    ReasoningEffort.MEDIUM -> 2048
    ReasoningEffort.HIGH -> 8192
    ReasoningEffort.MAX -> -1
}

/**
 * `think` **top-level** para `/api/chat` (RF-219): `null` = campo ausente; `OFF` = `false`;
 * el resto `true`. En on-device (llama.cpp) el campo se ignora y solo aplica el tope de tokens.
 */
fun reasoningThink(effort: ReasoningEffort): Boolean? = when (effort) {
    ReasoningEffort.DEFAULT -> null
    ReasoningEffort.OFF -> false
    ReasoningEffort.LOW, ReasoningEffort.MEDIUM, ReasoningEffort.HIGH, ReasoningEffort.MAX -> true
}
