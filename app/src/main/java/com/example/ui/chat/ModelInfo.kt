package com.example.ui.chat

import com.example.data.ai.ModelState

/**
 * Spec 040 (RF-283, RF-284): decisiones puras de la vista de detalles de modelo on-device.
 * Sin `Context`; el estado llega ya recolectado por quien llama.
 */

enum class ModelDotState { LOADED, NOT_LOADED, LOADING }

enum class ModelEngineAction { DOWNLOAD, DOWNLOADING_THIS, DOWNLOAD_BUSY, LOAD, LOADING, RETRY, UNLOAD }

/**
 * Punto de estado de la cabecera: verde ⇔ es el modelo activo ∧ `READY`;
 * spinner ⇔ es el activo ∧ `LOADING`; en cualquier otro caso gris «Not loaded»
 * (el motor en ERROR se pinta como «Not loaded»).
 */
fun modelDotState(isSelected: Boolean, engineState: ModelState): ModelDotState = when {
    isSelected && engineState == ModelState.READY -> ModelDotState.LOADED
    isSelected && engineState == ModelState.LOADING -> ModelDotState.LOADING
    else -> ModelDotState.NOT_LOADED
}

/**
 * Botón de ciclo de vida: sin archivo → descargar (deshabilitado si otro modelo descarga);
 * activo ∧ `READY` → unload; activo ∧ `LOADING` → spinner; activo ∧ `ERROR` → retry; si no, cargar.
 */
fun modelEngineAction(
    isDownloaded: Boolean,
    isDownloadingThis: Boolean,
    isAnyDownloadActive: Boolean,
    isSelected: Boolean,
    engineState: ModelState
): ModelEngineAction = when {
    !isDownloaded && isDownloadingThis -> ModelEngineAction.DOWNLOADING_THIS
    !isDownloaded && isAnyDownloadActive -> ModelEngineAction.DOWNLOAD_BUSY
    !isDownloaded -> ModelEngineAction.DOWNLOAD
    isSelected && engineState == ModelState.LOADING -> ModelEngineAction.LOADING
    isSelected && engineState == ModelState.READY -> ModelEngineAction.UNLOAD
    isSelected && engineState == ModelState.ERROR -> ModelEngineAction.RETRY
    else -> ModelEngineAction.LOAD
}
