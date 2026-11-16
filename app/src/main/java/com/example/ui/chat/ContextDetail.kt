package com.example.ui.chat

import java.util.Locale

/**
 * Detalle de ocupación del contexto de la conversación para el card bajo el anillo (spec 037).
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

/** Métricas por turno tal como las guarda spec 036; `null` = el turno no trae la métrica. */
data class TurnMetrics(val promptTokens: Int?, val generatedTokens: Int?, val evalDurationMs: Long?)

/** Acumulado del histórico: totales de prompt/generación y velocidad media ponderada (Σ/Σ, nunca media de tok/s). */
data class TurnTotals(val promptTokens: Int, val generatedTokens: Int, val avgTokensPerSecond: Float?)

/** Acumula ignorando métricas ausentes (turnos de error o heredados). Sin datos de duración → avg null. */
fun accumulateTurnTokens(turns: List<TurnMetrics>): TurnTotals {
    var prompt = 0
    var generated = 0
    var evalNs = 0L
    for (turn in turns) {
        prompt += turn.promptTokens ?: 0
        generated += turn.generatedTokens ?: 0
        evalNs += (turn.evalDurationMs ?: 0L) * 1_000_000L
    }
    return TurnTotals(
        promptTokens = prompt,
        generatedTokens = generated,
        avgTokensPerSecond = tokensPerSecond(generated, evalNs)
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
    val turnGeneratedTokens: Int?
)

/** `null` iff [usage] == null (la tarjeta ni siquiera se pinta). */
fun buildContextDetail(usage: ContextUsage?, turns: List<TurnMetrics>): ContextDetail? {
    if (usage == null) return null
    val totals = accumulateTurnTokens(turns)
    return ContextDetail(
        usedTokens = usage.usedTokens,
        windowTokens = usage.windowTokens,
        exact = usage.exact,
        totalPromptTokens = totals.promptTokens,
        totalGeneratedTokens = totals.generatedTokens,
        avgTokensPerSecond = totals.avgTokensPerSecond,
        turnPromptTokens = turns.lastOrNull { it.promptTokens != null }?.promptTokens,
        turnGeneratedTokens = turns.lastOrNull { it.generatedTokens != null }?.generatedTokens
    )
}
