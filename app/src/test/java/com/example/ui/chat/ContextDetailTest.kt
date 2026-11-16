package com.example.ui.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Spec 037 (RF-265…RF-270, RNF-160): detalle de ocupación del contexto. Tests JVM puros.
 */
class ContextDetailTest {

    @Test
    fun formatTokensCompact_smallNumbers() {
        assertEquals("0", formatTokensCompact(0))
        assertEquals("42", formatTokensCompact(42))
        assertEquals("999", formatTokensCompact(999))
    }

    @Test
    fun formatTokensCompact_kRange() {
        assertEquals("1k", formatTokensCompact(1000))
        assertEquals("2.05k", formatTokensCompact(2048))
        assertEquals("1.02k", formatTokensCompact(1024))
        assertEquals("1.5k", formatTokensCompact(1500))
        assertEquals("1.23k", formatTokensCompact(1234))
        assertEquals("10k", formatTokensCompact(10_000))
    }

    @Test
    fun formatTokensCompact_millionRange() {
        assertEquals("1.5m", formatTokensCompact(1_500_000))
        assertEquals("2m", formatTokensCompact(2_000_000))
    }

    @Test
    fun formatPercentUsed() {
        assertEquals(0, formatPercentUsed(0f))
        assertEquals(2, formatPercentUsed(0.023f))
        assertEquals(85, formatPercentUsed(0.85f))
        assertEquals(100, formatPercentUsed(1.5f))
    }

    @Test
    fun accumulateTurnTokens_empty() {
        val totals = accumulateTurnTokens(emptyList())
        assertEquals(0, totals.promptTokens)
        assertEquals(0, totals.generatedTokens)
        assertNull(totals.avgTokensPerSecond)
    }

    @Test
    fun accumulateTurnTokens_ignoresMissingMetrics() {
        val totals = accumulateTurnTokens(listOf(
            TurnMetrics(promptTokens = null, generatedTokens = null, evalDurationMs = null),
            TurnMetrics(promptTokens = 30, generatedTokens = null, evalDurationMs = null),
            TurnMetrics(promptTokens = null, generatedTokens = 12, evalDurationMs = 2770L),
            TurnMetrics(promptTokens = null, generatedTokens = 8, evalDurationMs = 0L)
        ))
        assertEquals(30, totals.promptTokens)
        assertEquals(20, totals.generatedTokens)
        assertEquals(20.0f / 2.77f, totals.avgTokensPerSecond!!, 0.05f)
    }

    @Test
    fun accumulateTurnTokens_weightedAverageOfRates() {
        // 20 tokens en 10s y 80 en 10s → Σcount/Σtime = 100/20 = 5 tok/s, NO media(2, 8)
        val totals = accumulateTurnTokens(listOf(
            TurnMetrics(null, 20, 10_000L),
            TurnMetrics(null, 80, 10_000L)
        ))
        assertEquals(5.0f, totals.avgTokensPerSecond!!, 0.001f)
    }

    @Test
    fun buildContextDetail_nullUsage_returnsNull() {
        assertNull(buildContextDetail(null, listOf(TurnMetrics(1, 2, 3L))))
    }

    @Test
    fun buildContextDetail_emptyConversation_keepsWindowFields() {
        val detail = buildContextDetail(ContextUsage(42, 2048, exact = true), emptyList())!!
        assertEquals(0, detail.totalPromptTokens)
        assertEquals(0, detail.totalGeneratedTokens)
        assertNull(detail.avgTokensPerSecond)
        assertNull(detail.turnPromptTokens)
        assertNull(detail.turnGeneratedTokens)
        assertEquals(2048, detail.windowTokens)
        assertEquals(true, detail.exact)
    }

    @Test
    fun buildContextDetail_thisTurnPair_ignoresErrorTurnsInBetween() {
        val turns = listOf(
            TurnMetrics(promptTokens = 10, generatedTokens = null, evalDurationMs = null),  // usuario turno 1
            TurnMetrics(promptTokens = null, generatedTokens = 5, evalDurationMs = 1000L),  // asistente 1
            TurnMetrics(promptTokens = 20, generatedTokens = null, evalDurationMs = null),  // usuario turno 2
            TurnMetrics(promptTokens = null, generatedTokens = null, evalDurationMs = null),// error sin métricas
            TurnMetrics(promptTokens = null, generatedTokens = 8, evalDurationMs = 2000L)   // asistente 2
        )
        val detail = buildContextDetail(ContextUsage(100, 8192, exact = false), turns)!!
        assertEquals(30, detail.totalPromptTokens)
        assertEquals(13, detail.totalGeneratedTokens)
        assertEquals(20, detail.turnPromptTokens)
        assertEquals(8, detail.turnGeneratedTokens)
        assertEquals(13.0f / 3.0f, detail.avgTokensPerSecond!!, 0.05f)
    }
}
