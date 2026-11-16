package com.example.data.ai

import android.content.Context
import android.util.Log
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

enum class ModelState {
    NOT_LOADED,
    LOADING,
    READY,
    ERROR
}

data class LoadedModelInfo(
    val model: OnDeviceModel,
    val filePath: String
)

interface InferenceEngine : AIService {
    suspend fun load(filePath: String, model: OnDeviceModel): Result<Unit>
    fun unload()
    val isLoaded: Boolean
    val loadedInfo: LoadedModelInfo?
    /** Spec 039: tokens en el KV cache en este instante (`-1` sin modelo cargado o sin datos). */
    fun nkvPosition(): Int
    /** Spec 039: métricas CSV de la última petición completada (`""` si no hay). */
    fun lastRequestMetrics(): String
    /** Spec 039: limpia las métricas antes de cada petición (evita herencia por cancelación). */
    fun clearRequestMetrics()
}

class OnDeviceService(
    private val engine: InferenceEngine,
    private val context: Context
) : AIService {

    private val _modelState = MutableStateFlow(ModelState.NOT_LOADED)
    val modelState: StateFlow<ModelState> = _modelState.asStateFlow()

    /** Spec 039: métricas exactas de la última respuesta completada (null si se canceló o aún no hay dato). */
    private val _lastContextUsage = MutableStateFlow<OllamaContextUsage?>(null)
    val lastContextUsage: StateFlow<OllamaContextUsage?> = _lastContextUsage.asStateFlow()

    /** Spec 039: tokens residentes en el KV cache del modelo (`null` si no hay modelo cargado). */
    fun currentNkvSize(): Int? = engine.nkvPosition().takeIf { it >= 0 }

    private val _loadedModelInfo = MutableStateFlow<LoadedModelInfo?>(null)
    val loadedModelInfo: StateFlow<LoadedModelInfo?> = _loadedModelInfo.asStateFlow()

    override val isAvailable: Boolean
        get() = _modelState.value == ModelState.READY

    suspend fun loadModel(filePath: String, model: OnDeviceModel): Result<Unit> {
        _modelState.value = ModelState.LOADING
        return engine.load(filePath, model).also { result ->
            if (result.isSuccess) {
                _loadedModelInfo.value = LoadedModelInfo(model, filePath)
                _modelState.value = ModelState.READY
                Log.i(TAG, "Model loaded: ${model.displayName} ($filePath)")
            } else {
                _modelState.value = ModelState.ERROR
                Log.e(TAG, "Failed to load model: ${model.displayName}", result.exceptionOrNull())
            }
        }
    }

    fun unloadModel() {
        engine.unload()
        _loadedModelInfo.value = null
        _modelState.value = ModelState.NOT_LOADED
    }

    override suspend fun execute(request: AiRequest): Result<String> =
        withContext(Dispatchers.IO) {
            if (_modelState.value != ModelState.READY) {
                return@withContext Result.failure(Exception(context.getString(R.string.ai_err_no_model)))
            }
            try {
                val systemPrompt = AiPromptBuilder.resolveSystemPrompt(request.action, request.customSystemPrompt)
                val userPrompt = AiPromptBuilder.buildUserPrompt(request)
                val fullMessages = mutableListOf<ChatMessage>()
                if (systemPrompt.isNotBlank()) {
                    fullMessages.add(ChatMessage("system", systemPrompt))
                }
                fullMessages.addAll(request.messages)
                fullMessages.add(ChatMessage("user", userPrompt))

                val result = engine.execute(AiRequest(
                    action = request.action,
                    prompt = userPrompt,
                    selectedText = request.selectedText,
                    context = request.context,
                    targetLanguage = request.targetLanguage,
                    customSystemPrompt = systemPrompt,
                    messages = fullMessages,
                    temperature = request.temperature,
                    topK = request.topK,
                    topP = request.topP,
                    repetitionPenalty = request.repetitionPenalty,
                    maxTokens = request.maxTokens
                ))
                if (result.isFailure) {
                    Log.e(TAG, "Engine inference failed", result.exceptionOrNull())
                }
                result
            } catch (e: Throwable) {
                Log.e(TAG, "Inference failed", e)
                Result.failure(e)
            }
        }

    override suspend fun executeStreaming(request: AiRequest): Flow<String> {
        val systemPrompt = AiPromptBuilder.resolveSystemPrompt(request.action, request.customSystemPrompt)
        val userPrompt = AiPromptBuilder.buildUserPrompt(request)
        val fullMessages = mutableListOf<ChatMessage>()
        if (systemPrompt.isNotBlank()) {
            fullMessages.add(ChatMessage("system", systemPrompt))
        }
        fullMessages.addAll(request.messages)
        fullMessages.add(ChatMessage("user", userPrompt))
        val predictFlow = engine.executeStreaming(AiRequest(
            action = request.action,
            prompt = userPrompt,
            selectedText = request.selectedText,
            context = request.context,
            targetLanguage = request.targetLanguage,
            customSystemPrompt = systemPrompt,
            messages = fullMessages,
            temperature = request.temperature,
            topK = request.topK,
            topP = request.topP,
            repetitionPenalty = request.repetitionPenalty,
            maxTokens = request.maxTokens
        ))
        return kotlinx.coroutines.flow.flow {
            _lastContextUsage.value = null
            predictFlow.collect { emit(it) }
            // Se completa solo si la respuesta llega a su fin; con cancelación, se queda en null.
            _lastContextUsage.value = parseOllamaEngineCSV(engine.lastRequestMetrics())
        }.flowOn(Dispatchers.IO)
    }

    companion object {
        private const val TAG = "OnDeviceService"
    }
}
