package com.example.util

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object ImageUrlResolver {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val WRAPPER_IMAGE_REGEX =
        Regex("""https://encrypted-tbn[0-9]*\.gstatic\.com/images\?q=tbn:[^"'\s]+""")

    fun isWrapperUrl(url: String): Boolean {
        if (!url.startsWith("http")) return false
        val host = Uri.parse(url).host ?: return false
        return host == "share.google" ||
            url.contains("imgres") ||
            (host.endsWith("google.com") && url.contains("/images"))
    }

    suspend fun resolveImageUrl(url: String): String = withContext(Dispatchers.IO) {
        if (!isWrapperUrl(url)) return@withContext url
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext url
                val body = response.body.string() ?: return@withContext url
                WRAPPER_IMAGE_REGEX.find(body)?.value?.replace("&amp;", "&") ?: url
            }
        } catch (e: Exception) {
            url
        }
    }
}
