package com.example.util

object VideoUrlHelper {

    private val MEDIA_EXTENSIONS = setOf(
        "mp4", "webm", "mkv", "mov", "m4v", "3gp", "avi", "flv", "ts", "m3u8", "ogg", "ogv"
    )

    private val YOUTUBE_ID_REGEX = Regex("[A-Za-z0-9_-]{11}")

    fun youTubeVideoId(url: String): String? {
        if (!url.startsWith("http")) return null
        val withoutScheme = url.substringAfter("//", url)
        val host = withoutScheme.substringBefore('/').substringBefore('?').lowercase()
        val rest = withoutScheme.substringAfter('/', "")
        val path = rest.substringBefore('?')
        val query = rest.substringAfter('?', "")

        val raw: String? = when {
            host.endsWith("youtu.be") -> path
            host.contains("youtube.com") -> {
                when {
                    path.startsWith("shorts/") -> path.removePrefix("shorts/").substringBefore('/')
                    path.startsWith("embed/") -> path.removePrefix("embed/").substringBefore('/')
                    path.startsWith("v/") -> path.removePrefix("v/").substringBefore('/')
                    else -> query.split('&').firstOrNull { it.startsWith("v=") }?.substringAfter("v=")
                }
            }
            else -> null
        }
        return raw?.trim()?.takeIf { it.matches(YOUTUBE_ID_REGEX) }
    }

    fun isYouTubeUrl(url: String): Boolean = youTubeVideoId(url) != null

    fun youTubeThumbnail(url: String): String {
        val id = youTubeVideoId(url) ?: return ""
        return "https://img.youtube.com/vi/$id/hqdefault.jpg"
    }

    fun isWebVideoUrl(url: String): Boolean {
        if (url.startsWith("content://") || url.startsWith("file://")) return false
        if (!url.startsWith("http://") && !url.startsWith("https://")) return false
        val host = url.substringAfter("//").substringBefore('/').substringBefore('?')
        if (host.isBlank()) return false
        val path = url.substringAfter("//").substringAfter('/', "").substringBefore('?')
        val extension = path.substringAfterLast('.', "").lowercase()
        return extension !in MEDIA_EXTENSIONS
    }
}
