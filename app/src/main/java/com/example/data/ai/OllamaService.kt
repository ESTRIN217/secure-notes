package com.example.data.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.json.JSONArray
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLHandshakeException

class OllamaService(
    private var endpointUrl: String = "http://localhost:11434",
    private var modelName: String = "llama3.2"
) : AIService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    override val isAvailable: Boolean = true

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    fun updateConfig(url: String, model: String) {
        endpointUrl = url.trimEnd('/')
        modelName = model
    }

    suspend fun testConnection(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$endpointUrl/api/tags")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("HTTP ${response.code}: ${response.message}"))
            }

            val json = JSONObject(body)
            val models = json.optJSONArray("models") ?: JSONArray()
            val modelNames = mutableListOf<String>()
            for (i in 0 until models.length()) {
                val m = models.optJSONObject(i)
                m?.let { modelNames.add(it.optString("name", "unknown")) }
            }
            Result.success(modelNames)
        } catch (e: SSLHandshakeException) {
            Result.failure(SSLHandshakeException(
                "Error de conexión SSL. Para servidores locales usa 'http://' en lugar de 'https://', o instala un certificado válido."
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun execute(request: AiRequest): Result<String> = withContext(Dispatchers.IO) {
        try {
            val httpRequest = buildChatRequest(request, stream = false)
            val response = client.newCall(httpRequest).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Ollama API error: ${response.code} $responseBody")
                val errorDetail = try {
                    JSONObject(responseBody).optString("error", response.message)
                } catch (_: Exception) {
                    response.message
                }
                return@withContext Result.failure(IOException(errorDetail))
            }

            val jsonResponse = JSONObject(responseBody)
            val message = jsonResponse.optJSONObject("message")
            val text = message?.optString("content", "") ?: ""
            val toolCalls = parseToolCalls(message)
            if (text.isBlank() && toolCalls.isEmpty()) {
                return@withContext Result.failure(IOException("Respuesta vacía del modelo"))
            }

            val resultText = if (toolCalls.isNotEmpty()) {
                "TOOL_CALLS:" + toolCalls.joinToString("|||") { (tcId, tcName, tcArgs) ->
                    "$tcId:::$tcName:::${org.json.JSONObject(tcArgs)}"
                }
            } else text.trim()

            Result.success(resultText)
        } catch (e: SSLHandshakeException) {
            Log.e(TAG, "SSL handshake failed", e)
            Result.failure(SSLHandshakeException(
                "Error de conexión SSL. Para servidores locales usa 'http://' en lugar de 'https://', o instala un certificado válido."
            ))
        } catch (e: ConnectException) {
            Log.e(TAG, "Connection refused", e)
            Result.failure(ConnectException(
                "No se puede conectar al servidor. ¿Está Ollama ejecutándose y accesible en $endpointUrl?"
            ))
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Connection timed out", e)
            Result.failure(SocketTimeoutException(
                "La conexión expiró. Verifica la URL y la conectividad de red."
            ))
        } catch (e: UnknownHostException) {
            Log.e(TAG, "Unknown host", e)
            Result.failure(UnknownHostException(
                "No se puede resolver el host. Verifica que la URL sea correcta."
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Ollama request failed", e)
            Result.failure(e)
        }
    }

    override suspend fun executeStreaming(request: AiRequest): Flow<String> = flow {
        val httpRequest = buildChatRequest(request, stream = true)
        val response = client.newCall(httpRequest).execute()

        if (!response.isSuccessful) {
            val errorBody = try {
                response.body?.string()?.let { JSONObject(it).optString("error", response.message) }
            } catch (_: Exception) { response.message }
            response.close()
            throw IOException(errorBody)
        }

        val body = response.body ?: throw IOException("Empty response body")
        val source = body.source()
        try {
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (line.isBlank()) continue
                val json = JSONObject(line)
                val message = json.optJSONObject("message")
                val token = message?.optString("content", "") ?: ""
                if (token.isNotEmpty()) {
                    emit(token)
                }
                if (json.optBoolean("done", false)) break
            }
        } finally {
            response.close()
        }
    }.flowOn(Dispatchers.IO)

    private fun buildChatRequest(request: AiRequest, stream: Boolean): okhttp3.Request {
        val systemPrompt = AiPromptBuilder.resolveSystemPrompt(request.action, request.customSystemPrompt)
        val userPrompt = AiPromptBuilder.buildUserPrompt(request)

        val messages = JSONArray()
        if (systemPrompt.isNotBlank()) {
            messages.put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            })
        }
        for (msg in request.messages) {
            messages.put(JSONObject().apply {
                put("role", msg.role)
                put("content", msg.content)
            })
        }
        messages.put(JSONObject().apply {
            put("role", "user")
            put("content", userPrompt)
        })

        val jsonBody = JSONObject().apply {
            put("model", modelName)
            put("messages", messages)
            put("stream", stream)
            put("options", JSONObject().apply {
                put("temperature", request.temperature)
                put("top_k", request.topK)
                put("top_p", request.topP)
                put("repeat_penalty", request.repetitionPenalty)
                put("num_predict", request.maxTokens)
            })
        }

        if (request.tools.isNotEmpty()) {
            val toolsArray = JSONArray()
            request.tools.forEach { toolMap ->
                toolsArray.put(JSONObject(toolMap))
            }
            jsonBody.put("tools", toolsArray)
        }

        if (request.toolResults.isNotEmpty()) {
            request.toolResults.forEach { result ->
                messages.put(JSONObject().apply {
                    put("role", "tool")
                    put("content", result.result)
                })
            }
        }

        val body = jsonBody.toString().toRequestBody(JSON_MEDIA_TYPE)

        return Request.Builder()
            .url("$endpointUrl/api/chat")
            .post(body)
            .build()
    }

    private fun parseToolCalls(message: JSONObject?): List<Triple<String, String, Map<String, Any>>> {
        val result = mutableListOf<Triple<String, String, Map<String, Any>>>()
        if (message == null) return result
        val toolCalls = message.optJSONArray("tool_calls") ?: return result
        for (i in 0 until toolCalls.length()) {
            val tc = toolCalls.optJSONObject(i) ?: continue
            val fn = tc.optJSONObject("function") ?: continue
            val name = fn.optString("name", "")
            val rawArgs = fn.optString("arguments", "{}")
            val args = try {
                val jsonArgs = JSONObject(rawArgs)
                jsonArgs.keys().asSequence().associateWith { key ->
                    jsonArgs.get(key)
                }
            } catch (_: Exception) { emptyMap() }
            result.add(Triple("tc_${i}", name, args))
        }
        return result
    }

    companion object {
        private const val TAG = "OllamaService"
    }
}
