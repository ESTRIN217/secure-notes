package com.example.ui.chat

import com.example.data.ai.MODEL_CATALOG
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Búsqueda y orden del selector de modelo (spec 033, RF-229/RF-233, RNF-146).
 * Puro y sin `android` ni Compose, así que corre en este host (RNF-140/RNF-146).
 */
class ModelSearchTest {

    private val catalog = MODEL_CATALOG

    // ── favoriteOnDeviceKey / favoriteOllamaKey ──────────────────────────────

    @Test
    fun `las claves de favorito llevan prefijo de origen`() {
        assertEquals("gguf:qwen2.5-0.5b", favoriteOnDeviceKey("qwen2.5-0.5b"))
        assertEquals("ollama:qwen3:0.6b", favoriteOllamaKey("qwen3:0.6b"))
    }

    // ── queryMatches ─────────────────────────────────────────────────────────

    @Test
    fun `consulta en blanco coincide con todo`() {
        assertTrue(queryMatches("", "x"))
        assertTrue(queryMatches("   ", "x"))
    }

    @Test
    fun `la comparación ignora mayúsculas y espacios`() {
        assertTrue(queryMatches("  QWEN ", "qwen2.5-0.5b"))
        assertTrue(queryMatches("q4_k", "qwen2.5-0.5b-instruct-q4_k_m.gguf"))
    }

    @Test
    fun `no hay coincidencia si ninguna parte contiene la consulta`() {
        assertFalse(queryMatches("llama", "qwen2.5-0.5b", "smollm"))
    }

    @Test
    fun `partes nulas no rompen la comparación`() {
        assertFalse(queryMatches("gemma", null, null))
        assertTrue(queryMatches("", null, null))
    }

    // ── filterOnDeviceModels ─────────────────────────────────────────────────

    @Test
    fun `el catálogo completo aparece sin consulta`() {
        assertEquals(catalog.size, filterOnDeviceModels(catalog, "").size)
    }

    @Test
    fun `filtra por nombre visible y por repo`() {
        val porNombre = filterOnDeviceModels(catalog, "qwen")
        assertTrue(porNombre.isNotEmpty())
        assertTrue(porNombre.all { it.displayName.contains("Qwen", ignoreCase = true) || it.huggingFaceRepo.contains("qwen", ignoreCase = true) })
        val porRepo = filterOnDeviceModels(catalog, "Qwen/Qwen2.5-0.5B-Instruct-GGUF")
        assertEquals(1, porRepo.size)
    }

    @Test
    fun `filtra por quantización y por archivo`() {
        assertTrue(filterOnDeviceModels(catalog, "q4_k_m").isNotEmpty())
        assertTrue(filterOnDeviceModels(catalog, ".gguf").isNotEmpty())
        assertTrue(filterOnDeviceModels(catalog, "zzz-no-existe").isEmpty())
    }

    // ── filterOllamaModels ───────────────────────────────────────────────────

    @Test
    fun `filtra los nombres de ollama por la consulta`() {
        val names = listOf("qwen3:0.6b", "llama3.2", "gemma3:270m")
        assertEquals(listOf("qwen3:0.6b"), filterOllamaModels(names, "qwen"))
        assertEquals(3, filterOllamaModels(names, "").size)
        assertEquals(emptyList<String>(), filterOllamaModels(names, "deepseek"))
    }

    // ── favoritesFirst ───────────────────────────────────────────────────────

    @Test
    fun `los favoritos van primero y el orden interno es estable`() {
        val items = listOf("a", "b", "c", "d", "e")
        val result = favoritesFirst(items) { it == "c" || it == "a" }
        assertEquals(listOf("a", "c", "b", "d", "e"), result)
    }

    @Test
    fun `sin favoritos devuelve la lista intacta`() {
        val items = listOf("a", "b", "c")
        assertEquals(items, favoritesFirst(items) { false })
    }

    // ── formatTokens ─────────────────────────────────────────────────────────

    @Test
    fun `formato en espanol usa punto y en ingles usa coma`() {
        assertEquals("8.192 tokens", formatTokens(8192, java.util.Locale("es")))
        assertEquals("8,192 tokens", formatTokens(8192, java.util.Locale.US))
        assertEquals("1 tokens", formatTokens(1, java.util.Locale.US))
    }
}
