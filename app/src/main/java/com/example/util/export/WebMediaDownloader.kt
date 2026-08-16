package com.example.util.export

import android.util.Base64
import com.example.data.model.BlockType
import com.example.util.ImageUrlResolver
import com.example.util.VideoUrlHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Descarga media web (http/https) a una data URI para embeber en HTML. */
object WebMediaDownloader {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val USER_AGENT = "Mozilla/5.0 (Linux; Android) SecureNotes"

    suspend fun downloadToDataUri(src: String, type: BlockType): String? = withContext(Dispatchers.IO) {
        if (!src.startsWith("http://") && !src.startsWith("https://")) return@withContext null
        if (type == BlockType.VIDEO && VideoUrlHelper.isYouTubeUrl(src)) return@withContext null
        val url = if (type == BlockType.IMAGE) ImageUrlResolver.resolveImageUrl(src) else src
        try {
            val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val mime = response.body?.contentType()?.toString()
                    ?: mimeFor(url, type)
                val bytes = response.body?.bytes() ?: return@withContext null
                "data:$mime;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun mimeFor(url: String, type: BlockType): String {
        val ext = url.substringAfterLast('.', "").substringBefore('?').lowercase()
        val fromExt = if (ext.isNotBlank()) {
            android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        } else null
        return fromExt ?: when (type) {
            BlockType.IMAGE -> "image/jpeg"
            BlockType.VIDEO -> "video/mp4"
            BlockType.AUDIO -> "audio/mpeg"
            BlockType.VOICE -> "audio/3gpp"
            BlockType.FILE -> "application/octet-stream"
            else -> "application/octet-stream"
        }
    }
}
