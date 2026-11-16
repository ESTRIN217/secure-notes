package com.example.ui.chat

/**
 * Derivadas de métricas de tokens usadas por la burbuja (spec 036). Piezas puras, sin `android`.
 */

/** Tokens por segundo a partir de un conteo y una duración en **nanosegundos**; `null` si no tiene sentido dividiir. */
fun tokensPerSecond(count: Int, durationNs: Long): Float? {
    if (durationNs <= 0L || count < 0) return null
    if (count == 0) return 0f
    return Math.round(count / (durationNs / 1e9f) * 10f) / 10f
}
