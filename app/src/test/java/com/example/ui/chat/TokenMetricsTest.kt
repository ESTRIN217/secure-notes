package com.example.ui.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Spec 036 (RF-261, RF-262, RNF-156): derivación de tok/s. Tests JVM puros.
 */
class TokenMetricsTest {

    @Test
    fun tokensPerSecond_normalCase() {
        assertEquals(4.0f, tokensPerSecond(120, 30_000_000_000L)!!, 0.05f)
    }

    @Test
    fun tokensPerSecond_zeroDuration_returnsNull() {
        assertNull(tokensPerSecond(50, 0L))
    }

    @Test
    fun tokensPerSecond_negativeDuration_returnsNull() {
        assertNull(tokensPerSecond(50, -1L))
    }

    @Test
    fun tokensPerSecond_zeroCount_returnsZero() {
        assertEquals(0f, tokensPerSecond(0, 5_000_000_000L)!!, 0.0001f)
    }

    @Test
    fun tokensPerSecond_negativeCount_returnsNull() {
        assertNull(tokensPerSecond(-1, 5_000_000_000L))
    }

    @Test
    fun tokensPerSecond_roundsToOneDecimal() {
        // 10 tokens en 1.5e9 ns = 6.666… → 6.7
        assertEquals(6.7f, tokensPerSecond(10, 1_500_000_000L)!!, 0.05f)
    }
}
