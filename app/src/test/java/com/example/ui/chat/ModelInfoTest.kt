package com.example.ui.chat

import com.example.data.ai.ModelState
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Spec 040 (RF-283, RF-284): decisiones puras de la vista de detalles de modelo on-device. JVM puro.
 */
class ModelInfoTest {

    // ── Punto de estado (RF-283) ────────────────────────────────

    @Test
    fun dot_activeReady_isLoaded() {
        assertEquals(ModelDotState.LOADED, modelDotState(isSelected = true, engineState = ModelState.READY))
    }

    @Test
    fun dot_activeLoading_isLoading() {
        assertEquals(ModelDotState.LOADING, modelDotState(isSelected = true, engineState = ModelState.LOADING))
    }

    @Test
    fun dot_activeError_isNotLoaded() {
        assertEquals(ModelDotState.NOT_LOADED, modelDotState(isSelected = true, engineState = ModelState.ERROR))
    }

    @Test
    fun dot_activeNotLoaded_isNotLoaded() {
        assertEquals(ModelDotState.NOT_LOADED, modelDotState(isSelected = true, engineState = ModelState.NOT_LOADED))
    }

    @Test
    fun dot_notSelectedReady_isNotLoaded() {
        assertEquals(ModelDotState.NOT_LOADED, modelDotState(isSelected = false, engineState = ModelState.READY))
    }

    @Test
    fun dot_notSelectedLoading_isNotLoaded() {
        assertEquals(ModelDotState.NOT_LOADED, modelDotState(isSelected = false, engineState = ModelState.LOADING))
    }

    // ── Botón de ciclo de vida (RF-284) ───────────────────────────

    @Test
    fun action_notDownloaded_noDownloads_isDownload() {
        assertEquals(
            ModelEngineAction.DOWNLOAD,
            modelEngineAction(isDownloaded = false, isDownloadingThis = false, isAnyDownloadActive = false, isSelected = false, engineState = ModelState.NOT_LOADED)
        )
    }

    @Test
    fun action_downloadingThis_isDownloadingThis() {
        assertEquals(
            ModelEngineAction.DOWNLOADING_THIS,
            modelEngineAction(isDownloaded = false, isDownloadingThis = true, isAnyDownloadActive = true, isSelected = false, engineState = ModelState.NOT_LOADED)
        )
    }

    @Test
    fun action_anotherDownloadActive_isDownloadBusy() {
        assertEquals(
            ModelEngineAction.DOWNLOAD_BUSY,
            modelEngineAction(isDownloaded = false, isDownloadingThis = false, isAnyDownloadActive = true, isSelected = false, engineState = ModelState.NOT_LOADED)
        )
    }

    @Test
    fun action_downloadedNotSelected_notLoaded_isLoad() {
        assertEquals(
            ModelEngineAction.LOAD,
            modelEngineAction(isDownloaded = true, isDownloadingThis = false, isAnyDownloadActive = false, isSelected = false, engineState = ModelState.NOT_LOADED)
        )
    }

    @Test
    fun action_downloadedNotSelected_anotherModelReady_isLoad() {
        assertEquals(
            ModelEngineAction.LOAD,
            modelEngineAction(isDownloaded = true, isDownloadingThis = false, isAnyDownloadActive = false, isSelected = false, engineState = ModelState.READY)
        )
    }

    @Test
    fun action_downloadedSelectedReady_isUnload() {
        assertEquals(
            ModelEngineAction.UNLOAD,
            modelEngineAction(isDownloaded = true, isDownloadingThis = false, isAnyDownloadActive = false, isSelected = true, engineState = ModelState.READY)
        )
    }

    @Test
    fun action_downloadedSelectedLoading_isLoading() {
        assertEquals(
            ModelEngineAction.LOADING,
            modelEngineAction(isDownloaded = true, isDownloadingThis = false, isAnyDownloadActive = false, isSelected = true, engineState = ModelState.LOADING)
        )
    }

    @Test
    fun action_downloadedSelectedError_isRetry() {
        assertEquals(
            ModelEngineAction.RETRY,
            modelEngineAction(isDownloaded = true, isDownloadingThis = false, isAnyDownloadActive = false, isSelected = true, engineState = ModelState.ERROR)
        )
    }

    @Test
    fun action_downloadedSelectedNotLoaded_isLoad() {
        assertEquals(
            ModelEngineAction.LOAD,
            modelEngineAction(isDownloaded = true, isDownloadingThis = false, isAnyDownloadActive = false, isSelected = true, engineState = ModelState.NOT_LOADED)
        )
    }
}
