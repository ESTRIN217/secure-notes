package com.estrin217.visormedia.util

import java.util.regex.Pattern

object VideoUrlHelper {

    fun isWebVideoUrl(src: String): Boolean {
        val trimmed = src.trim().lowercase()
        return trimmed.startsWith("http://") || trimmed.startsWith("https://")
    }

    fun isYouTubeUrl(src: String): Boolean {
        val trimmed = src.trim().lowercase()
        return trimmed.contains("youtube.com") || trimmed.contains("youtu.be")
    }

    fun extractYouTubeId(url: String): String? {
        val patterns = listOf(
            "(?:https?:\\/\\/)?(?:www\\.)?youtu\\.be\\/([a-zA-Z0-9_-]{11})",
            "(?:https?:\\/\\/)?(?:www\\.)?youtube\\.com\\/(?:watch\\?v=|embed\\/|shorts\\/|v\\/)([a-zA-Z0-9_-]{11})"
        )
        for (p in patterns) {
            val matcher = Pattern.compile(p).matcher(url)
            if (matcher.find()) {
                return matcher.group(1)
            }
        }
        return null
    }

    fun youTubeThumbnail(src: String): String {
        val id = extractYouTubeId(src) ?: return ""
        return "https://img.youtube.com/vi/$id/hqdefault.jpg"
    }
}
