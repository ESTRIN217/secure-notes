package com.example.data.ai

import android.content.Context
import android.util.Log
import com.arm.aichat.AiChat
import com.arm.aichat.InferenceEngine as ArmInferenceEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext

class LlamaCppEngine(
    private val context: Context
) : InferenceEngine {

    private val engine: ArmInferenceEngine by lazy {
        AiChat.getInferenceEngine(context.applicationContext)
    }

    private var _loadedInfo: LoadedModelInfo? = null
    private var _loadedPath: String? = null
    private var _activeSystemPrompt: String = ""
    private var _systemPromptPending = false

    override val isAvailable: Boolean
        get() = _loadedInfo != null

    override val isLoaded: Boolean
        get() = _loadedInfo != null

    override val loadedInfo: LoadedModelInfo?
        get() = _loadedInfo

    override suspend fun load(filePath: String, model: OnDeviceModel): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                unload()
                engine.loadModel(filePath)
                _loadedInfo = LoadedModelInfo(model, filePath)
                _loadedPath = filePath
                _activeSystemPrompt = ""
                _systemPromptPending = true
                Log.i(TAG, "Model loaded: $filePath")
                Result.success(Unit)
            } catch (e: OutOfMemoryError) {
                Log.e(TAG, "OOM loading model", e)
                Result.failure(Exception("Memoria insuficiente: ${e.message}"))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load engine", e)
                Result.failure(e)
            }
        }

    override fun unload() {
        try {
            engine.cleanUp()
        } catch (e: Exception) {
            Log.e(TAG, "cleanUp failed", e)
        }
        _loadedInfo = null
        _loadedPath = null
        _activeSystemPrompt = ""
        _systemPromptPending = false
    }

    override suspend fun execute(request: AiRequest): Result<String> = withContext(Dispatchers.IO) {
        try {
            val (system, user) = resolvePrompts(request)
            ensureReady(system)
            val response = engine.sendUserPrompt(user, request.maxTokens)
                .toList()
                .joinToString("")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Inference failed", e)
            Result.failure(e)
        }
    }

    override suspend fun executeStreaming(request: AiRequest): Flow<String> {
        val (system, user) = resolvePrompts(request)
        ensureReady(system)
        return engine.sendUserPrompt(user, request.maxTokens)
    }

    private fun resolvePrompts(request: AiRequest): Pair<String, String> {
        val system = request.messages.firstOrNull { it.role == ROLE_SYSTEM }?.content
            ?.takeIf { it.isNotBlank() }
            ?: request.customSystemPrompt.takeIf { it.isNotBlank() }
            ?: ""
        val user = request.messages.lastOrNull { it.role == ROLE_USER }?.content
            ?.takeIf { it.isNotBlank() }
            ?: request.prompt
        return system to user
    }

    private suspend fun ensureReady(systemPrompt: String) {
        if (_loadedPath == null) {
            throw IllegalStateException("No hay modelo cargado. Carga un modelo local primero.")
        }
        if (_systemPromptPending) {
            applySystemPrompt(systemPrompt)
            return
        }
        val isReady = engine.state.value is ArmInferenceEngine.State.ModelReady
        if (!isReady || systemPrompt != _activeSystemPrompt) {
            resetWith(systemPrompt)
        }
    }

    private suspend fun applySystemPrompt(systemPrompt: String) {
        if (systemPrompt.isNotBlank()) {
            engine.setSystemPrompt(systemPrompt)
        }
        _activeSystemPrompt = systemPrompt
        _systemPromptPending = false
    }

    private suspend fun resetWith(systemPrompt: String) {
        Log.i(TAG, "Resetting engine (state changed or system prompt changed)")
        val path = requireNotNull(_loadedPath)
        engine.cleanUp()
        engine.loadModel(path)
        _systemPromptPending = true
        applySystemPrompt(systemPrompt)
    }

    companion object {
        private const val TAG = "LlamaCppEngine"
        private const val ROLE_SYSTEM = "system"
        private const val ROLE_USER = "user"
    }
}
