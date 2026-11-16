package com.example.ui.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Spec 034 (RF-249, RF-251, RF-253, RF-254, RNF-148): las piezas puras de ocupación de contexto.
 * Tests JVM puros: no tocan Robolectric ni Android.
 */
class ContextUsageTest {

    @Test
    fun contextUsageFraction_emptyConversation_isZero() {
        assertEquals(0f, contextUsageFraction(0, LLAMA_CPP_CONTEXT_TOKENS), 0.0001f)
    }

    @Test
    fun contextUsageFraction_isClampedToOne() {
        assertEquals(1f, contextUsageFraction(10_000, 100), 0.0001f)
    }

    @Test
    fun contextUsageFraction_nonPositiveWindow_doesNotThrow() {
        assertEquals(0f, contextUsageFraction(50, 0), 0.0001f)
        assertEquals(0f, contextUsageFraction(50, -5), 0.0001f)
    }

    @Test
    fun contextUsageFraction_negativeUsed_isClampedToZero() {
        assertEquals(0f, contextUsageFraction(-10, LLAMA_CPP_CONTEXT_TOKENS), 0.0001f)
    }

    @Test
    fun isContextNearLimit_thresholdIsInclusive() {
        assertFalse(isContextNearLimit(0.84f))
        assertTrue(isContextNearLimit(0.85f))
        assertTrue(isContextNearLimit(1.0f))
        assertFalse(isContextNearLimit(0.0f))
    }

    @Test
    fun buildContextUsage_nullWindow_returnsNull() {
        assertNull(buildContextUsage(usedTokens = 100, windowTokens = null, exact = false))
    }

    @Test
    fun buildContextUsage_sanitizesUsedAndKeepsExactFlag() {
        val usage = buildContextUsage(usedTokens = -5, windowTokens = 8192, exact = true)
        assertEquals(ContextUsage(usedTokens = 0, windowTokens = 8192, exact = true), usage)
    }

    @Test
    fun llamaCppContextTokens_is8192() {
        // Presupuesto fijo upstream del binding; si cambia, cambia este test y la nota en MEMORY.md.
        assertEquals(8192, LLAMA_CPP_CONTEXT_TOKENS)
    }
}
