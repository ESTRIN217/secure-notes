package com.example.data.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OllamaService(
    private var endpointUrl: String = "http://192.168.1.100:11434",
    private var modelName: String = "llama3.2"
) : AIService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    override val isAvailable: Boolean = true

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val systemPrompts = mapOf(
        AiAction.GENERATE to "You are a helpful writing assistant. Generate text based on the user's request. Use markdown formatting where appropriate.",
        AiAction.SUMMARIZE to "Summarize the following text concisely while preserving key information.",
        AiAction.REWRITE to "Rewrite the following text in the specified style while preserving the original meaning.",
        AiAction.TRANSLATE to "Translate the following text to the specified language. Return only the translation without explanations."
    )

    fun updateConfig(url: String, model: String) {
        endpointUrl = url.trimEnd('/')
        modelName = model
    }

    override suspend fun execute(request: AiRequest): Result<String> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = systemPrompts[request.action] ?: ""
            val userPrompt = buildPrompt(request)

            val jsonBody = JSONObject().apply {
                put("model", modelName)
                put("prompt", userPrompt)
                put("system", systemPrompt)
                put("stream", false)
            }

            val body = jsonBody.toString().toRequestBody(JSON_MEDIA_TYPE)

            val httpRequest = Request.Builder()
                .url("$endpointUrl/api/generate")
                .post(body)
                .build()

            val response = client.newCall(httpRequest).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Ollama API error: ${response.code} $responseBody")
                return@withContext Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
            }

            val jsonResponse = JSONObject(responseBody)
            val text = jsonResponse.optString("response", "")
            if (text.isBlank()) {
                return@withContext Result.failure(Exception("Empty response from model"))
            }

            Result.success(text.trim())
        } catch (e: Exception) {
            Log.e(TAG, "Ollama request failed", e)
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
        private const val TAG = "OllamaService"
    }
}
