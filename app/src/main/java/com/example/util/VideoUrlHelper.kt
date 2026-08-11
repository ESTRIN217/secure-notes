package com.example.util

import android.net.Uri

object VideoUrlHelper {

    private val MEDIA_EXTENSIONS = setOf(
        "mp4", "webm", "mkv", "mov", "m4v", "3gp", "avi", "flv", "ts", "m3u8", "ogg", "ogv"
    )

    fun youTubeVideoId(url: String): String? {
        if (!url.startsWith("http")) return null
        val uri = Uri.parse(url)
        val host = uri.host?.lowercase() ?: return null
        val raw = when {
            host.endsWith("youtu.be") -> uri.path?.trimStart('/')
            host.contains("youtube.com") -> {
                val path = uri.path ?: ""
                when {
                    path.startsWith("/shorts/") -> path.trimStart('/').substringAfter('/')
                    path.startsWith("/embed/") -> path.trimStart('/').substringAfter('/')
                    path.startsWith("/v/") -> path.trimStart('/').substringAfter('/')
                    else -> uri.getQueryParameter("v")
                }
            }
            else -> null
        }
        return raw?.trim()?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{11}")) }
    }

    fun isYouTubeUrl(url: String): Boolean = youTubeVideoId(url) != null

    fun youTubeThumbnail(url: String): String {
        val id = youTubeVideoId(url) ?: return ""
        return "https://img.youtube.com/vi/$id/hqdefault.jpg"
    }

    fun isWebVideoUrl(url: String): Boolean {
        if (url.startsWith("content://") || url.startsWith("file://")) return false
        if (!url.startsWith("http://") && !url.startsWith("https://")) return false
        val uri = Uri.parse(url)
        if (uri.host.isNullOrBlank()) return false
        val path = uri.path.orEmpty()
        val extension = path.substringAfterLast('.', "").lowercase()
        return extension !in MEDIA_EXTENSIONS
    }
}
