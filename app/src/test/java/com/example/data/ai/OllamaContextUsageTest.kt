package com.example.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Spec 034 (RF-250, RNF-148): el parser del trozo `done` de Ollama. Tests JVM puros.
 */
class OllamaContextUsageTest {

    @Test
    fun parse_doneLineWithCounts_returnsSum() {
        val line = """{"model":"qwen","done":true,"prompt_eval_count":1200,"eval_count":350,"total_duration":123}"""
        assertEquals(OllamaContextUsage(1200, 350), parseOllamaContextUsage(line))
        assertEquals(1550, parseOllamaContextUsage(line)?.sum)
    }

    @Test
    fun parse_contentChunk_returnsNull() {
        val line = """{"message":{"role":"assistant","content":"Hola"},"done":false}"""
        assertNull(parseOllamaContextUsage(line))
    }

    @Test
    fun parse_doneTrueButMissingCounts_returnsNull() {
        assertNull(parseOllamaContextUsage("""{"done":true}"""))
        assertNull(parseOllamaContextUsage("""{"done":true,"prompt_eval_count":100}"""))
        assertNull(parseOllamaContextUsage("""{"done":true,"eval_count":100}"""))
    }

    @Test
    fun parse_withDurations_readsThem() {
        val line = """{"done":true,"prompt_eval_count":30,"eval_count":12,"prompt_eval_duration":3260000000,"eval_duration":2770000000}"""
        val usage = parseOllamaContextUsage(line)!!
        assertEquals(3_260_000_000L, usage.promptEvalDurationNs)
        assertEquals(2_770_000_000L, usage.evalDurationNs)
        assertEquals(42, usage.sum)
    }

    @Test
    fun parse_missingDurations_staysNullButKeepsCounts() {
        val line = """{"done":true,"prompt_eval_count":30,"eval_count":12}"""
        val usage = parseOllamaContextUsage(line)!!
        assertNull(usage.promptEvalDurationNs)
        assertNull(usage.evalDurationNs)
    }

    @Test
    fun parse_invalidDurations_stayNull() {
        val line = """{"done":true,"prompt_eval_count":30,"eval_count":12,"prompt_eval_duration":-5,"eval_duration":"2770000000"}"""
        val usage = parseOllamaContextUsage(line)!!
        assertNull(usage.promptEvalDurationNs)
        assertNull(usage.evalDurationNs)
    }

    @Test
    fun parse_countsNotNumbers_returnsNull() {
        assertNull(parseOllamaContextUsage("""{"done":true,"prompt_eval_count":"1200","eval_count":350}"""))
    }

    @Test
    fun parse_malformedJson_returnsNull() {
        assertNull(parseOllamaContextUsage("not json"))
        assertNull(parseOllamaContextUsage(""))
    }
}
