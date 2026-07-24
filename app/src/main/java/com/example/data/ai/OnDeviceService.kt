package com.example.data.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    suspend fun load(filePath: String): Result<Unit>
    fun unload()
    val isLoaded: Boolean
    val loadedInfo: LoadedModelInfo?
}

class OnDeviceService(
    private val engine: InferenceEngine
) : AIService {

    private val _modelState = MutableStateFlow(ModelState.NOT_LOADED)
    val modelState: StateFlow<ModelState> = _modelState.asStateFlow()

    private val _loadedModelInfo = MutableStateFlow<LoadedModelInfo?>(null)
    val loadedModelInfo: StateFlow<LoadedModelInfo?> = _loadedModelInfo.asStateFlow()

    override val isAvailable: Boolean
        get() = _modelState.value == ModelState.READY

    suspend fun loadModel(filePath: String, model: OnDeviceModel): Result<Unit> {
        _modelState.value = ModelState.LOADING
        return engine.load(filePath).also { result ->
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
                return@withContext Result.failure(Exception("No hay modelo cargado. Carga un modelo local primero."))
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
                    rewriteStyle = request.rewriteStyle,
                    targetLanguage = request.targetLanguage,
                    customSystemPrompt = systemPrompt,
                    messages = fullMessages
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
        return engine.executeStreaming(AiRequest(
            action = request.action,
            prompt = userPrompt,
            selectedText = request.selectedText,
            context = request.context,
            rewriteStyle = request.rewriteStyle,
            targetLanguage = request.targetLanguage,
            customSystemPrompt = systemPrompt,
            messages = fullMessages
        ))
    }

    companion object {
        private const val TAG = "OnDeviceService"
    }
}
