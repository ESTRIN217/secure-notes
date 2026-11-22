package com.example.data.ai

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Variante independent: sin IA on-device. Mantiene la misma API pública que la implementación full
 * para que el código común compile sin depender de :lib / llama.cpp.
 */
class AiModelHost(private val appContext: Context) {

    private val _modelState = MutableStateFlow(ModelState.NOT_LOADED)
    val modelState: StateFlow<ModelState> = _modelState.asStateFlow()

    private val _loadedModelInfo = MutableStateFlow<LoadedModelInfo?>(null)
    val loadedModelInfo: StateFlow<LoadedModelInfo?> = _loadedModelInfo.asStateFlow()

    private val noOpEngine = object : InferenceEngine {
        override suspend fun load(filePath: String, model: OnDeviceModel): Result<Unit> =
            Result.failure(UnsupportedOperationException("On-device AI not available in this variant"))
        override fun unload() = Unit
        override val isLoaded: Boolean = false
        override val loadedInfo: LoadedModelInfo? = null
        override fun nkvPosition(): Int = -1
        override fun lastRequestMetrics(): String = ""
        override fun clearRequestMetrics() = Unit
        override fun lastPromptEntryPosition(): Int = -1
        override suspend fun execute(request: AiRequest): Result<String> =
            Result.failure(UnsupportedOperationException("On-device AI not available in this variant"))
        override val isAvailable: Boolean = false
    }

    private val noOpService = OnDeviceService(noOpEngine, appContext)

    fun serviceOrNull(): OnDeviceService? = null

    fun installLoader(loader: suspend (OnDeviceService) -> Result<Unit>) = Unit

    fun acquire(): OnDeviceService = noOpService

    fun release() = Unit

    fun releaseAll(reason: String) = Unit

    suspend fun ensureReady(): Result<Unit> =
        Result.failure(UnsupportedOperationException("On-device AI not available in this variant"))

    private companion object {
        const val TAG = "AiModelHost"
    }
}
