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

    override val isAvailable: Boolean
        get() = _loadedInfo != null

    override val isLoaded: Boolean
        get() = _loadedInfo != null

    override val loadedInfo: LoadedModelInfo?
        get() = _loadedInfo

    override suspend fun load(filePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            unload()
            engine.loadModel(filePath)
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
    }

    override suspend fun execute(request: AiRequest): Result<String> = withContext(Dispatchers.IO) {
        try {
            val prompt = buildUserPrompt(request)
            val response = engine.sendUserPrompt(prompt, request.maxTokens)
                .toList()
                .joinToString("")
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Inference failed", e)
            Result.failure(e)
        }
    }

    override suspend fun executeStreaming(request: AiRequest): Flow<String> {
        val prompt = buildUserPrompt(request)
        return engine.sendUserPrompt(prompt, request.maxTokens)
    }

    private fun buildUserPrompt(request: AiRequest): String {
        val parts = mutableListOf<String>()
        if (request.messages.isNotEmpty()) {
            for (msg in request.messages) {
                parts.add("${msg.role}: ${msg.content}")
            }
        } else if (request.prompt.isNotBlank()) {
            parts.add(request.prompt)
        }
        return parts.joinToString("\n")
    }

    companion object {
        private const val TAG = "LlamaCppEngine"
    }
}
