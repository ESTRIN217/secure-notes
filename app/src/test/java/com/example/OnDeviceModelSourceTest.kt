package com.example

import com.example.data.ai.MODEL_CATALOG
import com.example.data.ai.OnDeviceModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OnDeviceModelSourceTest {

    private fun model(name: String) = OnDeviceModel(
        id = "test",
        displayName = "Test",
        minRamMb = 512,
        recommendedRamMb = 1024,
        fileSizeMb = 100,
        huggingFaceRepo = "org/repo",
        ggufFileName = name
    )

    @Test
    fun `download url points at the resolve endpoint`() {
        assertEquals(
            "https://huggingface.co/org/repo/resolve/main/smollm2-360m-instruct-q4_k_m.gguf",
            model("smollm2-360m-instruct-q4_k_m.gguf").downloadUrl
        )
    }

    @Test
    fun `source page url points at the blob endpoint`() {
        assertEquals(
            "https://huggingface.co/org/repo/blob/main/model.gguf",
            model("model.gguf").sourcePageUrl
        )
    }

    @Test
    fun `source label drops the scheme and the file`() {
        assertEquals("huggingface.co/org/repo", model("model.gguf").sourceLabel)
    }

    @Test
    fun `quant label is parsed from the file name`() {
        assertEquals("Q4_K_M", model("qwen2.5-0.5b-instruct-q4_k_m.gguf").quantLabel)
        assertEquals("Q4_K_M", model("Qwen_Qwen3-0.6B-Q4_K_M.gguf").quantLabel)
        assertEquals("Q4", model("phi-3-mini-4k-instruct-q4.gguf").quantLabel)
        assertEquals("Q8_0", model("llama-3.2-1b-instruct-q8_0.gguf").quantLabel)
    }

    @Test
    fun `quant label is empty when the file name carries no quantization`() {
        assertEquals("", model("model.gguf").quantLabel)
        assertEquals("", model("gpt2.gguf").quantLabel)
    }

    @Test
    fun `every catalog model exposes a hugging face source`() {
        assertTrue(MODEL_CATALOG.isNotEmpty())
        MODEL_CATALOG.forEach { m ->
            assertTrue(m.huggingFaceRepo.contains('/'))
            assertTrue(m.downloadUrl.startsWith("https://huggingface.co/"))
            assertTrue(m.downloadUrl.endsWith(m.ggufFileName))
            assertTrue(m.sourceLabel.startsWith("huggingface.co/"))
            assertTrue("missing quant label for ${m.id}", m.quantLabel.isNotEmpty())
        }
    }

    @Test
    fun `catalog ids and file names are unique`() {
        assertEquals(MODEL_CATALOG.size, MODEL_CATALOG.map { it.id }.toSet().size)
        assertEquals(MODEL_CATALOG.size, MODEL_CATALOG.map { it.ggufFileName }.toSet().size)
    }
}