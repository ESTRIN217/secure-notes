package com.example.data.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

sealed class DownloadState {
    data object Idle : DownloadState()
    data class Downloading(val progress: Float, val downloadedMb: Long, val totalMb: Long) : DownloadState()
    data class Completed(val file: File) : DownloadState()
    data class Failed(val error: String) : DownloadState()
}

class ModelDownloader(private val context: Context) {

    private val _state = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val state: StateFlow<DownloadState> = _state.asStateFlow()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val modelsDir: File
        get() = File(context.filesDir, "models").also { it.mkdirs() }

    private var currentCall: okhttp3.Call? = null

    fun getModelFile(model: OnDeviceModel): File? {
        val file = File(modelsDir, model.ggufFileName)
        return file.takeIf { it.exists() && it.length() > 0 }
    }

    fun isDownloaded(model: OnDeviceModel): Boolean {
        val file = File(modelsDir, model.ggufFileName)
        return file.exists() && file.length() > 0
    }

    fun getModelPath(model: OnDeviceModel): String? {
        return getModelFile(model)?.absolutePath
    }

    suspend fun download(model: OnDeviceModel) = withContext(Dispatchers.IO) {
        val file = File(modelsDir, model.ggufFileName)
        if (file.exists() && file.length() > 0) {
            _state.value = DownloadState.Completed(file)
            return@withContext
        }

        val downloadUrl = "https://huggingface.co/${model.huggingFaceRepo}/resolve/main/${model.ggufFileName}"

        try {
            _state.value = DownloadState.Downloading(0f, 0, model.fileSizeMb.toLong())

            val request = Request.Builder()
                .url(downloadUrl)
                .build()

            currentCall = client.newCall(request)
            val response = currentCall!!.execute()

            if (!response.isSuccessful) {
                _state.value = DownloadState.Failed("HTTP ${response.code}: ${response.message}")
                return@withContext
            }

            val body = response.body ?: run {
                _state.value = DownloadState.Failed("Empty response body")
                return@withContext
            }

            val contentLength = body.contentLength()
            val totalBytes = if (contentLength > 0) contentLength else model.fileSizeMb * 1024L * 1024L

            FileOutputStream(file).use { output ->
                val buffer = ByteArray(32 * 1024)
                var bytesRead: Long = 0
                val inputStream = body.byteStream()

                while (true) {
                    val read = inputStream.read(buffer)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                    bytesRead += read

                    if (totalBytes > 0) {
                        val progress = (bytesRead.toFloat() / totalBytes).coerceAtMost(1f)
                        _state.value = DownloadState.Downloading(
                            progress = progress,
                            downloadedMb = bytesRead / (1024 * 1024),
                            totalMb = totalBytes / (1024 * 1024)
                        )
                    }
                }
            }

            if (file.exists() && file.length() > 0) {
                _state.value = DownloadState.Completed(file)
                Log.i(TAG, "Model downloaded: ${file.absolutePath} (${file.length() / (1024*1024)}MB)")
            } else {
                _state.value = DownloadState.Failed("Downloaded file is empty")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            _state.value = DownloadState.Failed(e.message ?: "Download failed")
        } finally {
            currentCall = null
        }
    }

    fun cancel() {
        currentCall?.cancel()
        currentCall = null
        _state.value = DownloadState.Idle
    }

    fun deleteModel(model: OnDeviceModel) {
        File(modelsDir, model.ggufFileName).delete()
        _state.value = DownloadState.Idle
    }

    fun resetState() {
        _state.value = DownloadState.Idle
    }

    companion object {
        private const val TAG = "ModelDownloader"
    }
}
