package com.example.ui.chat

import java.util.Locale

/**
 * Detalle de ocupación del contexto de la conversación para el card bajo el anillo (spec 037, extendido por spec 041).
 * Piezas puras (sin `android`): agregan lo que la conversación sabe y nada más.
 */

/** `[0,1]` → entero de porcentaje (`0.023 → 2`). */
fun formatPercentUsed(fraction: Float): Int = (fraction.coerceIn(0f, 1f) * 100).toInt()

private fun trimZeros(s: String): String = s.trimEnd('0').trimEnd('.')

/** Compacto estilo mock: `42 → "42"`, `2048 → "2.05k"`, `1_500_000 → "1.5m"`. Locale.US, 2 decimales con recorte de ceros. */
fun formatTokensCompact(n: Int): String {
    if (n < 1_000) return n.toString()
    if (n < 1_000_000) return trimZeros(String.format(Locale.US, "%.2f", n / 1000.0)) + "k"
    return trimZeros(String.format(Locale.US, "%.2f", n / 1_000_000.0)) + "m"
}

/** Métricas por turno tal como las guarda spec 036 (+ caché de spec 041); `null` = el turno no trae la métrica. */
data class TurnMetrics(
    val promptTokens: Int?,
    val generatedTokens: Int?,
    val evalDurationMs: Long?,
    /** Spec 041: posición de entrada en la KV-cache persistida del turno; null = sin dato. */
    val cachedTokens: Int? = null
)

/** Acumulado del histórico: totales de prompt/generación y velocidad media ponderada (Σ/Σ, nunca media de tok/s). */
data class TurnTotals(val promptTokens: Int, val generatedTokens: Int, val avgTokensPerSecond: Float?, val cachedTokens: Int?)

/** Acumula ignorando métricas ausentes (turnos de error o heredados). Sin datos de duración → avg null. */
fun accumulateTurnTokens(turns: List<TurnMetrics>): TurnTotals {
    var prompt = 0
    var generated = 0
    var evalNs = 0L
    var cachedSum = 0
    var cachedSeen = false
    for (turn in turns) {
        prompt += turn.promptTokens ?: 0
        generated += turn.generatedTokens ?: 0
        evalNs += (turn.evalDurationMs ?: 0L) * 1_000_000L
        // Spec 041: Σ cached solo de los turnos con dato; ausencia total ⇒ null (RF-296).
        turn.cachedTokens?.let { cachedSum += it; cachedSeen = true }
    }
    return TurnTotals(
        promptTokens = prompt,
        generatedTokens = generated,
        avgTokensPerSecond = tokensPerSecond(generated, evalNs),
        cachedTokens = if (cachedSeen) cachedSum else null
    )
}

/** Lo que pinta la tarjeta: usado/ventana + totales + último par. `null` iff `usage` == null (RF-265). */
data class ContextDetail(
    val usedTokens: Int,
    val windowTokens: Int,
    val exact: Boolean,
    val totalPromptTokens: Int,
    val totalGeneratedTokens: Int,
    val avgTokensPerSecond: Float?,
    val turnPromptTokens: Int?,
    val turnGeneratedTokens: Int?,
    /** Spec 041 (RF-297/RF-298): cached del PAR del último turno con métricas; null iff ese turno no trae cached (o no hay métricas). */
    val turnCachedTokens: Int?,
    /** Spec 041 (RF-297/RF-298): Σ cached de los turnos con dato (doble conteo entre turnos, ver spec 041). Sub-línea ACROSS ALL TURNS. */
    val totalCachedTokens: Int?,
    /** Spec 041 (RF-298): fila «Prompt»: cached+fresh si el último turno con métricas trae ambos; fresh si no trae cached; null si no hay fresh. */
    val turnPromptRowTokens: Int?,
    /** Spec 041 (RF-299): fresh del MISMO turno que [turnCachedTokens] (el par); null iff no hay par. */
    val turnPairFreshTokens: Int?
)

/**
 * `null` iff [usage] == null (la tarjeta ni siquiera se pinta).
 * Spec 041 (RF-298): el emparejamiento cached+fresh se hace sobre el ÚNICO anclaje «último turno con métricas»:
 * `pair = lastMetrics iff lastMetrics.cachedTokens != null`. Si ese turno no trae cached (histórico/Ollama/mixto),
 * no hay par: la fila pinta solo fresh y la sub-línea de turno se omite. Nunca se suman dato de turnos distintos.
 */
fun buildContextDetail(usage: ContextUsage?, turns: List<TurnMetrics>): ContextDetail? {
    if (usage == null) return null
    val totals = accumulateTurnTokens(turns)
    val lastMetrics = turns.lastOrNull { it.promptTokens != null }
    val pair = lastMetrics?.takeIf { it.cachedTokens != null }
    val turnPrompt = lastMetrics?.promptTokens
    val turnCached = pair?.cachedTokens
    return ContextDetail(
        usedTokens = usage.usedTokens,
        windowTokens = usage.windowTokens,
        exact = usage.exact,
        totalPromptTokens = totals.promptTokens,
        totalGeneratedTokens = totals.generatedTokens,
        avgTokensPerSecond = totals.avgTokensPerSecond,
        turnPromptTokens = turnPrompt,
        turnGeneratedTokens = turns.lastOrNull { it.generatedTokens != null }?.generatedTokens,
        turnCachedTokens = turnCached,
        totalCachedTokens = totals.cachedTokens,
        turnPromptRowTokens = turnPromptRowTokens(turnCached, turnPrompt),
        turnPairFreshTokens = pair?.promptTokens
    )
}

/** Fila «Prompt»: cached+fresh con ambos; solo fresh sin cached; null sin fresh (nunca inventada). */
private fun turnPromptRowTokens(cached: Int?, fresh: Int?): Int? = when {
    fresh != null && cached != null -> cached + fresh
    fresh != null -> fresh
    else -> null
}
