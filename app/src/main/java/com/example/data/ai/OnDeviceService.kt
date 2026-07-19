package com.example.data.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
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

class OnDeviceService : AIService {

    private val _modelState = MutableStateFlow(ModelState.NOT_LOADED)
    val modelState: StateFlow<ModelState> = _modelState.asStateFlow()

    override val isAvailable: Boolean
        get() = _modelState.value == ModelState.READY

    private var modelPath: String = ""

    private val systemPrompts = mapOf(
        AiAction.GENERATE to "You are a helpful writing assistant. Generate text based on the user's request. Use markdown formatting where appropriate.",
        AiAction.SUMMARIZE to "Summarize the following text concisely while preserving key information.",
        AiAction.REWRITE to "Rewrite the following text in the specified style while preserving the original meaning.",
        AiAction.TRANSLATE to "Translate the following text to the specified language. Return only the translation without explanations."
    )

    fun setModelPath(path: String) {
        modelPath = path
        _modelState.value = ModelState.NOT_LOADED
    }

    suspend fun loadModel(): Result<Unit> = withContext(Dispatchers.IO) {
        if (modelPath.isBlank()) {
            _modelState.value = ModelState.ERROR
            return@withContext Result.failure(Exception("Model path not set"))
        }
        _modelState.value = ModelState.LOADING
        try {
            // TODO: Initialize MediaPipe LlmInference once library is available
            // val options = LlmInference.LlmInferenceOptions.builder()
            //     .setModelPath(modelPath)
            //     .build()
            // llmInference = LlmInference.createFromOptions(this@OnDeviceService, options)
            _modelState.value = ModelState.READY
            Log.d(TAG, "On-device model loaded from: $modelPath")
            Result.success(Unit)
        } catch (e: Exception) {
            _modelState.value = ModelState.ERROR
            Log.e(TAG, "Failed to load on-device model", e)
            Result.failure(e)
        }
    }

    fun unloadModel() {
        _modelState.value = ModelState.NOT_LOADED
    }

    override suspend fun execute(request: AiRequest): Result<String> = withContext(Dispatchers.IO) {
        if (_modelState.value != ModelState.READY) {
            val loadResult = loadModel()
            if (loadResult.isFailure) {
                return@withContext Result.failure(Exception("On-device model not available"))
            }
        }
        try {
            val systemPrompt = systemPrompts[request.action] ?: ""
            val userPrompt = buildPrompt(request)
            val fullPrompt = if (systemPrompt.isNotBlank()) {
                "<|system|>\n$systemPrompt\n<|user|>\n$userPrompt\n<|assistant|>"
            } else {
                "<|user|>\n$userPrompt\n<|assistant|>"
            }

            // TODO: Call MediaPipe LlmInference.generateResponse() once library is available
            // val response = llmInference.generateResponse(fullPrompt)
            // Result.success(response)

            // Placeholder until MediaPipe is integrated
            Log.w(TAG, "On-device inference not yet implemented - requires mediapipe-tasks-genai")
            Result.failure(Exception("On-device inference not yet available. Please use Ollama backend."))
        } catch (e: Exception) {
            Log.e(TAG, "On-device inference failed", e)
            Result.failure(e)
        }
    }

    private fun buildPrompt(request: AiRequest): String {
        return when (request.action) {
            AiAction.GENERATE -> {
                val contextPrefix = if (request.context.isNotBlank()) {
                    "Context from current note:\n${request.context}\n\n"
                } else ""
                "${contextPrefix}${request.prompt}"
            }
            AiAction.SUMMARIZE -> {
                "Summarize the following text:\n\n${request.selectedText.ifBlank { request.context }}"
            }
            AiAction.REWRITE -> {
                val styleDesc = when (request.rewriteStyle) {
                    RewriteStyle.FORMAL -> "formal and professional"
                    RewriteStyle.CASUAL -> "casual and conversational"
                    RewriteStyle.POETIC -> "poetic and creative"
                    RewriteStyle.PROFESSIONAL -> "business-appropriate and polished"
                }
                "Rewrite the following text in a $styleDesc style:\n\n${request.selectedText}"
            }
            AiAction.TRANSLATE -> {
                "Translate the following text to ${request.targetLanguage}:\n\n${request.selectedText}"
            }
        }
    }

    companion object {
        private const val TAG = "OnDeviceService"
    }
}
