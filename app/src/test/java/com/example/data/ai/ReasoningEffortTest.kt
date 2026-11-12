package com.example.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Mapeos esfuerzo de razonamiento → tope de tokens y `think` (spec 033, RF-218/RF-219, RNF-140).
 *
 * Es puro y no importa `android` ni Compose, así que corre en este host donde Robolectric no corre.
 */
class ReasoningEffortTest {

    // ── reasoningMaxTokens (RF-218) ──────────────────────────────────────────

    @Test
    fun `default y off usan la preferencia de la app`() {
        assertEquals(256, reasoningMaxTokens(256, ReasoningEffort.DEFAULT))
        assertEquals(512, reasoningMaxTokens(512, ReasoningEffort.OFF))
    }

    @Test
    fun `low medium high aplican sus topes fijos`() {
        assertEquals(512, reasoningMaxTokens(256, ReasoningEffort.LOW))
        assertEquals(2048, reasoningMaxTokens(256, ReasoningEffort.MEDIUM))
        assertEquals(8192, reasoningMaxTokens(256, ReasoningEffort.HIGH))
    }

    @Test
    fun `max devuelve -1, el convenio de ilimitado de ollama`() {
        assertEquals(-1, reasoningMaxTokens(256, ReasoningEffort.MAX))
    }

    // ── reasoningThink (RF-219) ──────────────────────────────────────────────

    @Test
    fun `default omite el campo think`() {
        assertNull(reasoningThink(ReasoningEffort.DEFAULT))
    }

    @Test
    fun `off pide think false`() {
        assertEquals(false, reasoningThink(ReasoningEffort.OFF))
    }

    @Test
    fun `el resto pide think true`() {
        assertEquals(true, reasoningThink(ReasoningEffort.LOW))
        assertEquals(true, reasoningThink(ReasoningEffort.MEDIUM))
        assertEquals(true, reasoningThink(ReasoningEffort.HIGH))
        assertEquals(true, reasoningThink(ReasoningEffort.MAX))
    }

    // ── invariantes del enum ─────────────────────────────────────────────────

    @Test
    fun `el orden del enum es el de la hoja, default primero y max ultimo`() {
        assertEquals(
            listOf(
                ReasoningEffort.DEFAULT,
                ReasoningEffort.OFF,
                ReasoningEffort.LOW,
                ReasoningEffort.MEDIUM,
                ReasoningEffort.HIGH,
                ReasoningEffort.MAX,
            ),
            ReasoningEffort.entries.toList()
        )
    }
}
