package com.example.data.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
            llamaModel = LlamaInference.create(context, filePath, nCtx = 512, nGpuLayers = 0)
            Log.i(TAG, "Engine loaded: $filePath")
            Result.success(Unit)
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "OOM loading model", e)
            Result.failure(Exception("Out of memory: ${e.message}"))
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
            return@withContext Result.failure(Exception("Engine not loaded"))
        }
        try {
            val response = LlamaInference.generate(model, request.prompt, maxTokens = 256)
            Result.success(response)
        } catch (e: Exception) {
            Log.e(TAG, "Inference failed", e)
            Result.failure(e)
        }
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

    fun generate(model: Any, prompt: String, maxTokens: Int): String {
        return if (model is NativeLlamaModel) {
            model.generate(prompt, maxTokens)
        } else {
            throw IllegalArgumentException("Unknown model type")
        }
    }
}

internal class NativeLlamaModel(context: Context, modelPath: String, nCtx: Int, nGpuLayers: Int) {
    private val nativeHandle: Long

    init {
        nativeHandle = nativeCreate(modelPath, nCtx, nGpuLayers)
        if (nativeHandle == 0L) {
            throw RuntimeException("nativeCreate returned null handle: $modelPath")
        }
    }

    fun generate(prompt: String, maxTokens: Int): String {
        return nativeGenerate(nativeHandle, prompt, maxTokens)
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
    private external fun nativeGenerate(handle: Long, prompt: String, maxTokens: Int): String
    private external fun nativeDestroy(handle: Long)
}
