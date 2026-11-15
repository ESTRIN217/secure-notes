package com.example.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Caché de `context_length` por modelo on-device derivada de GGUF (spec 033, RF-241).
 * Es puro y no importa android.
 */
class ModelContextCacheTest {

    @Test
    fun `ida y vuelta de un mapa valido`() {
        val map = mapOf(
            "qwen2.5-0.5b" to 32768L,
            "smollm2-360m" to 8192L,
            "qwen3-0.6b" to 32768L
        )
        assertEquals(map, parseContextLengths(serializeContextLengths(map)))
    }

    @Test
    fun `formato legible por humanos, una linea por modelo`() {
        assertEquals("a=1\nb=2", serializeContextLengths(mapOf("a" to 1, "b" to 2)))
    }

    @Test
    fun `las lineas basura se ignoran, las validas entran`() {
        val text = """
            qwen2.5-0.5b=32768
            sinSeparador
            =8192
            gamma= no-un-numero
            delta=-5
            epsilon=0
            zeta=131072
        """.trimIndent()
        assertEquals(mapOf("qwen2.5-0.5b" to 32768L, "zeta" to 131072L), parseContextLengths(text))
    }

    @Test
    fun `mapa vacio serializa a texto vacio y viceversa`() {
        assertEquals("", serializeContextLengths(emptyMap()))
        assertTrue(parseContextLengths("").isEmpty())
    }

    @Test
    fun `las claves con espacios se recortan y los duplicados ganan la ultima`() {
        assertEquals(9L, parseContextLengths("qwen=9").getValue("qwen"))
        assertEquals(2L, parseContextLengths("dup=1\ndup=2")["dup"])
    }
}
