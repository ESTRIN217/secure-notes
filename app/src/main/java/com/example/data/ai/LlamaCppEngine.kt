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

class LlamaCppEngine(
    private val context: Context,
    var nCtx: Int = 1024,
    var nGpuLayers: Int = 0,
    var maxTokens: Int = 256,
    var temperature: Float = 0.7f,
    var repetitionPenalty: Float = 1.1f,
    var topK: Int = 40,
    var nThreads: Int = 4
) : InferenceEngine {

    private var llamaModel: NativeLlamaModel? = null
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
            llamaModel = NativeLlamaModel(context, filePath, nCtx, nGpuLayers, nThreads)
            Log.i(TAG, "Engine loaded: $filePath (ctx=$nCtx, gpu=$nGpuLayers, threads=$nThreads)")
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
        val model = llamaModel
        if (model != null) {
            model.close()
        }
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
                model.generateChat(
                    request.messages,
                    maxTokens,
                    temperature,
                    repetitionPenalty,
                    topK
                )
            } else {
                model.generate(
                    request.prompt,
                    maxTokens,
                    temperature,
                    repetitionPenalty,
                    topK
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
            close(Exception("Motor no cargado"))
            return@callbackFlow
        }
        withContext(Dispatchers.IO) {
            try {
                if (request.messages.isNotEmpty()) {
                    model.generateChatStreaming(
                        request.messages,
                        maxTokens,
                        temperature,
                        repetitionPenalty,
                        topK,
                        object : TokenCallback {
                            override fun onToken(token: String) {
                                trySend(token)
                            }
                            override fun onComplete() {
                                close()
                            }
                            override fun onError(error: String) {
                                close(Exception(error))
                            }
                        }
                    )
                } else {
                    model.generateStreaming(
                        request.prompt,
                        maxTokens,
                        temperature,
                        repetitionPenalty,
                        topK,
                        object : TokenCallback {
                            override fun onToken(token: String) {
                                trySend(token)
                            }
                            override fun onComplete() {
                                close()
                            }
                            override fun onError(error: String) {
                                close(Exception(error))
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

internal class NativeLlamaModel(
    context: Context,
    modelPath: String,
    nCtx: Int,
    nGpuLayers: Int,
    nThreads: Int
) {
    private val nativeHandle: Long

    init {
        nativeHandle = nativeCreate(modelPath, nCtx, nGpuLayers, nThreads)
        if (nativeHandle == 0L) {
            throw RuntimeException("nativeCreate devolvió un identificador nulo: $modelPath")
        }
    }

    fun generate(
        prompt: String,
        maxTokens: Int,
        temperature: Float = 0.7f,
        repetitionPenalty: Float = 1.1f,
        topK: Int = 40
    ): String {
        return nativeGenerate(nativeHandle, prompt, maxTokens, temperature, repetitionPenalty, topK)
    }

    fun generateChat(
        messages: List<ChatMessage>,
        maxTokens: Int,
        temperature: Float = 0.7f,
        repetitionPenalty: Float = 1.1f,
        topK: Int = 40
    ): String {
        val roles = messages.map { it.role }.toTypedArray()
        val contents = messages.map { it.content }.toTypedArray()
        return nativeGenerateChat(nativeHandle, roles, contents, maxTokens, temperature, repetitionPenalty, topK)
    }

    fun generateStreaming(
        prompt: String,
        maxTokens: Int,
        temperature: Float,
        repetitionPenalty: Float,
        topK: Int,
        callback: TokenCallback
    ) {
        nativeGenerateStreaming(nativeHandle, prompt, maxTokens, temperature, repetitionPenalty, topK, callback)
    }

    fun generateChatStreaming(
        messages: List<ChatMessage>,
        maxTokens: Int,
        temperature: Float,
        repetitionPenalty: Float,
        topK: Int,
        callback: TokenCallback
    ) {
        val roles = messages.map { it.role }.toTypedArray()
        val contents = messages.map { it.content }.toTypedArray()
        nativeGenerateChatStreaming(nativeHandle, roles, contents, maxTokens, temperature, repetitionPenalty, topK, callback)
    }

    fun close() {
        try {
            nativeDestroy(nativeHandle)
        } catch (e: Exception) {
            Log.e("NativeLlamaModel", "nativeDestroy failed", e)
        }
    }

    private companion object {
        init {
            System.loadLibrary("llamajni")
        }
    }

    private external fun nativeCreate(modelPath: String, nCtx: Int, nGpuLayers: Int, nThreads: Int): Long
    private external fun nativeGenerate(handle: Long, prompt: String, maxTokens: Int, temperature: Float, repetitionPenalty: Float, topK: Int): String
    private external fun nativeGenerateChat(handle: Long, roles: Array<String>, contents: Array<String>, maxTokens: Int, temperature: Float, repetitionPenalty: Float, topK: Int): String
    private external fun nativeGenerateStreaming(handle: Long, prompt: String, maxTokens: Int, temperature: Float, repetitionPenalty: Float, topK: Int, callback: TokenCallback)
    private external fun nativeGenerateChatStreaming(handle: Long, roles: Array<String>, contents: Array<String>, maxTokens: Int, temperature: Float, repetitionPenalty: Float, topK: Int, callback: TokenCallback)
    private external fun nativeDestroy(handle: Long)
}
