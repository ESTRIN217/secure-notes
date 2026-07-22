package com.example.data.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
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
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class OllamaService(
    private var endpointUrl: String = "http://192.168.1.100:11434",
    private var modelName: String = "llama3.2"
) : AIService {

    private val secureClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val unsafeClient: OkHttpClient by lazy {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAllCerts, SecureRandom())
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .build()
    }

    private val client: OkHttpClient
        get() = if (endpointUrl.startsWith("https://", ignoreCase = true)) unsafeClient else secureClient

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
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun execute(request: AiRequest): Result<String> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = AiPromptBuilder.resolveSystemPrompt(request.action, request.customSystemPrompt)
            val userPrompt = AiPromptBuilder.buildUserPrompt(request)

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
                val errorDetail = try {
                    JSONObject(responseBody).optString("error", response.message)
                } catch (_: Exception) {
                    response.message
                }
                return@withContext Result.failure(IOException(errorDetail))
            }

            val jsonResponse = JSONObject(responseBody)
            val text = jsonResponse.optString("response", "")
            if (text.isBlank()) {
                return@withContext Result.failure(IOException("Respuesta vacía del modelo"))
            }

            Result.success(text.trim())
        } catch (e: SSLHandshakeException) {
            Log.e(TAG, "SSL handshake failed", e)
            Result.failure(SSLHandshakeException(
                "Error de conexión SSL. Si usas un certificado autofirmado, usa una URL 'https://' — la app lo aceptará."
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

    companion object {
        private const val TAG = "OllamaService"
    }
}
