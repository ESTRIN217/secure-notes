package com.example.data.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

interface TokenCallback {
    fun onToken(token: String)
    fun onComplete()
    fun onError(error: String)
}

class LlamaCppEngine(private val context: Context) : InferenceEngine {

    private var llamaModel: Any? = null
    private var _loadedInfo: LoadedModelInfo? = null

    override val isAvailable: Boolean
        get() = llamaModel != null

    override val isLoaded: Boolean
        get() = llamaModel != null

    override val loadedInfo: LoadedModelInfo?
        get() = _loadedInfo

    override suspend fun load(filePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            unload()
            llamaModel = LlamaInference.create(context, filePath, nCtx = 1024, nGpuLayers = 0)
            Log.i(TAG, "Engine loaded: $filePath")
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
            llamaModel?.let { LlamaInference.destroy(it) }
        } catch (_: Exception) {}
        llamaModel = null
        _loadedInfo = null
    }

    override suspend fun execute(request: AiRequest): Result<String> = withContext(Dispatchers.IO) {
        val model = llamaModel
        if (model == null) {
            return@withContext Result.failure(Exception("Motor no cargado"))
        }
        try {
            val response = if (request.messages.isNotEmpty()) {
                LlamaInference.generateChat(
                    model,
                    request.messages,
                    maxTokens = 256,
                    temperature = 0.7f,
                    repetitionPenalty = 1.1f,
                    topK = 40
                )
            } else {
                LlamaInference.generate(
                    model,
                    request.prompt,
                    maxTokens = 256,
                    temperature = 0.7f,
                    repetitionPenalty = 1.1f,
                    topK = 40
                )
            }
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Inference failed", e)
            Result.failure(e)
        }
    }

    override suspend fun executeStreaming(request: AiRequest): Flow<String> = callbackFlow {
        val model = llamaModel
        if (model == null) {
            close(java.lang.Exception("Motor no cargado"))
            return@callbackFlow
        }
        withContext(Dispatchers.IO) {
            try {
                if (request.messages.isNotEmpty()) {
                    LlamaInference.generateChatStreaming(
                        model,
                        request.messages,
                        maxTokens = 256,
                        temperature = 0.7f,
                        repetitionPenalty = 1.1f,
                        topK = 40,
                        object : TokenCallback {
                            override fun onToken(token: String) {
                                trySend(token)
                            }
                            override fun onComplete() {
                                close()
                            }
                            override fun onError(error: String) {
                                close(java.lang.Exception(error))
                            }
                        }
                    )
                } else {
                    LlamaInference.generateStreaming(
                        model,
                        request.prompt,
                        maxTokens = 256,
                        temperature = 0.7f,
                        repetitionPenalty = 1.1f,
                        topK = 40,
                        object : TokenCallback {
                            override fun onToken(token: String) {
                                trySend(token)
                            }
                            override fun onComplete() {
                                close()
                            }
                            override fun onError(error: String) {
                                close(java.lang.Exception(error))
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Streaming inference failed", e)
                close(e)
            }
        }
        awaitClose { }
    }

    companion object {
        private const val TAG = "LlamaCppEngine"
    }
}

internal object LlamaInference {
    fun create(context: Context, modelPath: String, nCtx: Int, nGpuLayers: Int): Any {
        return NativeLlamaModel(context, modelPath, nCtx, nGpuLayers)
    }

    fun destroy(model: Any) {
        if (model is NativeLlamaModel) {
            model.close()
        }
    }

    fun generate(model: Any, prompt: String, maxTokens: Int, temperature: Float = 0.7f, repetitionPenalty: Float = 1.1f, topK: Int = 40): String {
        return if (model is NativeLlamaModel) {
            model.generate(prompt, maxTokens, temperature, repetitionPenalty, topK)
        } else {
            throw IllegalArgumentException("Tipo de modelo desconocido")
        }
    }

    fun generateChat(model: Any, messages: List<ChatMessage>, maxTokens: Int, temperature: Float = 0.7f, repetitionPenalty: Float = 1.1f, topK: Int = 40): String {
        return if (model is NativeLlamaModel) {
            model.generateChat(messages, maxTokens, temperature, repetitionPenalty, topK)
        } else {
            throw IllegalArgumentException("Tipo de modelo desconocido")
        }
    }

    fun generateStreaming(model: Any, prompt: String, maxTokens: Int, temperature: Float, repetitionPenalty: Float, topK: Int, callback: TokenCallback) {
        if (model is NativeLlamaModel) {
            model.generateStreaming(prompt, maxTokens, temperature, repetitionPenalty, topK, callback)
        } else {
            throw IllegalArgumentException("Tipo de modelo desconocido")
        }
    }

    fun generateChatStreaming(model: Any, messages: List<ChatMessage>, maxTokens: Int, temperature: Float, repetitionPenalty: Float, topK: Int, callback: TokenCallback) {
        if (model is NativeLlamaModel) {
            model.generateChatStreaming(messages, maxTokens, temperature, repetitionPenalty, topK, callback)
        } else {
            throw IllegalArgumentException("Tipo de modelo desconocido")
        }
    }
}

internal class NativeLlamaModel(context: Context, modelPath: String, nCtx: Int, nGpuLayers: Int) {
    private val nativeHandle: Long

    init {
        nativeHandle = nativeCreate(modelPath, nCtx, nGpuLayers)
        if (nativeHandle == 0L) {
            throw RuntimeException("nativeCreate devolvió un identificador nulo: $modelPath")
        }
    }

    fun generate(prompt: String, maxTokens: Int, temperature: Float = 0.7f, repetitionPenalty: Float = 1.1f, topK: Int = 40): String {
        return nativeGenerate(nativeHandle, prompt, maxTokens, temperature, repetitionPenalty, topK)
    }

    fun generateChat(messages: List<ChatMessage>, maxTokens: Int, temperature: Float = 0.7f, repetitionPenalty: Float = 1.1f, topK: Int = 40): String {
        val roles = messages.map { it.role }.toTypedArray()
        val contents = messages.map { it.content }.toTypedArray()
        return nativeGenerateChat(nativeHandle, roles, contents, maxTokens, temperature, repetitionPenalty, topK)
    }

    fun generateStreaming(prompt: String, maxTokens: Int, temperature: Float, repetitionPenalty: Float, topK: Int, callback: TokenCallback) {
        nativeGenerateStreaming(nativeHandle, prompt, maxTokens, temperature, repetitionPenalty, topK, callback)
    }

    fun generateChatStreaming(messages: List<ChatMessage>, maxTokens: Int, temperature: Float, repetitionPenalty: Float, topK: Int, callback: TokenCallback) {
        val roles = messages.map { it.role }.toTypedArray()
        val contents = messages.map { it.content }.toTypedArray()
        nativeGenerateChatStreaming(nativeHandle, roles, contents, maxTokens, temperature, repetitionPenalty, topK, callback)
    }

    fun close() {
        nativeDestroy(nativeHandle)
    }

    private companion object {
        init {
            System.loadLibrary("llamajni")
        }
    }

    private external fun nativeCreate(modelPath: String, nCtx: Int, nGpuLayers: Int): Long
    private external fun nativeGenerate(handle: Long, prompt: String, maxTokens: Int, temperature: Float, repetitionPenalty: Float, topK: Int): String
    private external fun nativeGenerateChat(handle: Long, roles: Array<String>, contents: Array<String>, maxTokens: Int, temperature: Float, repetitionPenalty: Float, topK: Int): String
    private external fun nativeGenerateStreaming(handle: Long, prompt: String, maxTokens: Int, temperature: Float, repetitionPenalty: Float, topK: Int, callback: TokenCallback)
    private external fun nativeGenerateChatStreaming(handle: Long, roles: Array<String>, contents: Array<String>, maxTokens: Int, temperature: Float, repetitionPenalty: Float, topK: Int, callback: TokenCallback)
    private external fun nativeDestroy(handle: Long)
}
