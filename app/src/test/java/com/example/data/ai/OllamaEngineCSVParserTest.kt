package com.example.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Spec 039 (RF-277, RNF-172): parser del CSV nativo. Tests JVM puros.
 */
class OllamaEngineCSVParserTest {

    @Test
    fun parse_fullCsv_returnsMetrics() {
        val usage = parseOllamaEngineCSV("1200,3260000000,350,2770000000")!!
        assertEquals(1200, usage.promptTokens)
        assertEquals(3_260_000_000L, usage.promptEvalDurationNs)
        assertEquals(350, usage.evalTokens)
        assertEquals(2_770_000_000L, usage.evalDurationNs)
        assertEquals(1550, usage.sum)
    }

    @Test
    fun parse_zeroDurationsAreValid() {
        val usage = parseOllamaEngineCSV("10,0,5,0")!!
        assertEquals(10, usage.promptTokens)
        assertEquals(0L, usage.promptEvalDurationNs)
        assertEquals(5, usage.evalTokens)
        assertEquals(0L, usage.evalDurationNs)
    }

    @Test
    fun parse_zeroesCountsAreValid() {
        val usage = parseOllamaEngineCSV("0,0,0,0")!!
        assertEquals(0, usage.sum)
    }

    @Test
    fun parse_malformedCsv_returnsNull() {
        assertNull(parseOllamaEngineCSV(""))
        assertNull(parseOllamaEngineCSV("   "))
        assertNull(parseOllamaEngineCSV("1,2"))
        assertNull(parseOllamaEngineCSV("1,2,3,4,5"))
        assertNull(parseOllamaEngineCSV("abc,def,ghi,jkl"))
        assertNull(parseOllamaEngineCSV("-1,1,1,1"))
        assertNull(parseOllamaEngineCSV("1,-5,1,1"))
    }

    @Test
    fun parse_negativeNsAllowedZeroOnly() {
        assertNull(parseOllamaEngineCSV("1,-1,1,1"))
    }
}
