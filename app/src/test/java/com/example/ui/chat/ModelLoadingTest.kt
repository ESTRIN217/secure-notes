package com.example.ui.chat

import com.example.data.ai.AiBackend
import com.example.data.ai.ModelState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Spec 035 (RF-258, RF-259, RNF-153): decisiones de visibilidad de carga del modelo. JVM puro.
 */
class ModelLoadingTest {

    @Test
    fun indicator_onDeviceLoading_shows() {
        assertTrue(shouldShowModelLoadingIndicator(AiBackend.ON_DEVICE, ModelState.LOADING))
    }

    @Test
    fun indicator_ollamaWithLoadingEngine_hides() {
        assertFalse(shouldShowModelLoadingIndicator(AiBackend.OLLAMA, ModelState.LOADING))
    }

    @Test
    fun indicator_onDeviceNotLoading_hides() {
        assertFalse(shouldShowModelLoadingIndicator(AiBackend.ON_DEVICE, ModelState.READY))
        assertFalse(shouldShowModelLoadingIndicator(AiBackend.ON_DEVICE, ModelState.ERROR))
        assertFalse(shouldShowModelLoadingIndicator(AiBackend.ON_DEVICE, ModelState.NOT_LOADED))
    }

    @Test
    fun errorBanner_duringLoading_isSuppressed() {
        assertFalse(shouldShowUnavailableError(modelLoading = true, available = false, historyEmpty = true))
        assertFalse(shouldShowUnavailableError(modelLoading = true, available = false, historyEmpty = false))
    }

    @Test
    fun errorBanner_whenUnavailableAndEmptyHistory_shows() {
        assertTrue(shouldShowUnavailableError(modelLoading = false, available = false, historyEmpty = true))
    }

    @Test
    fun errorBanner_whenAvailableOrHistoryPresent_hides() {
        assertFalse(shouldShowUnavailableError(modelLoading = false, available = true, historyEmpty = true))
        assertFalse(shouldShowUnavailableError(modelLoading = false, available = false, historyEmpty = false))
    }
}
