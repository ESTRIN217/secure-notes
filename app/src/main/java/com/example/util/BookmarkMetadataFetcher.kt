package com.example.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class BookmarkMeta(
    val title: String = "",
    val description: String = "",
    val favicon: String = ""
)

object BookmarkMetadataFetcher {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private const val MAX_HTML_BYTES = 1024 * 1024

    suspend fun fetch(url: String): BookmarkMeta = withContext(Dispatchers.IO) {
        val normalized = url.trim()
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            return@withContext BookmarkMeta(
                title = hostOf(normalized) ?: normalized,
                favicon = faviconFor(normalized)
            )
        }
        try {
            val request = Request.Builder().url(normalized).header("User-Agent", "Mozilla/5.0 (Linux; Android) SecureNotes").build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext fallbackMeta(normalized)
                }
                val html = response.body?.byteStream()?.use { stream ->
                    val bytes = stream.readBytes()
                    String(bytes, 0, minOf(bytes.size, MAX_HTML_BYTES), Charsets.UTF_8)
                } ?: ""
                parseMetadata(html, normalized)
            }
        } catch (e: Exception) {
            fallbackMeta(normalized)
        }
    }

    fun parseMetadata(html: String, url: String): BookmarkMeta {
        val title = Regex("""<meta[^>]+property=["']og:title["'][^>]+content=["']([^"']*)["'][^>]*>""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.get(1)
            ?: Regex("""<meta[^>]+content=["']([^"']*)["'][^>]+property=["']og:title["'][^>]*>""", RegexOption.IGNORE_CASE)
                .find(html)?.groupValues?.get(1)
            ?: Regex("""<title[^>]*>([\s\S]*?)</title>""", RegexOption.IGNORE_CASE)
                .find(html)?.groupValues?.get(1)?.trim()
            ?: ""

        val description = Regex("""<meta[^>]+property=["']og:description["'][^>]+content=["']([^"']*)["'][^>]*>""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.get(1)
            ?: Regex("""<meta[^>]+content=["']([^"']*)["'][^>]+property=["']og:description["'][^>]*>""", RegexOption.IGNORE_CASE)
                .find(html)?.groupValues?.get(1)
            ?: Regex("""<meta[^>]+name=["']description["'][^>]+content=["']([^"']*)["'][^>]*>""", RegexOption.IGNORE_CASE)
                .find(html)?.groupValues?.get(1)
            ?: Regex("""<meta[^>]+content=["']([^"']*)["'][^>]+name=["']description["'][^>]*>""", RegexOption.IGNORE_CASE)
                .find(html)?.groupValues?.get(1)
            ?: ""

        val favicon = bestFavicon(html, url)

        return BookmarkMeta(
            title = title.decodeEntities().trim().ifBlank { hostOf(url) ?: url },
            description = description.decodeEntities().trim(),
            favicon = if (favicon.isNotBlank()) absolutize(favicon, url) else faviconFor(url)
        )
    }

    private data class IconLink(val href: String, val type: String)

    private fun bestFavicon(html: String, url: String): String {
        val icons = Regex("""<link\b[^>]*>""", RegexOption.IGNORE_CASE)
            .findAll(html)
            .mapNotNull { tag ->
                val rel = attrOf(tag.value, "rel")?.lowercase() ?: return@mapNotNull null
                if ("icon" !in rel) return@mapNotNull null
                val href = attrOf(tag.value, "href") ?: return@mapNotNull null
                val type = attrOf(tag.value, "type").orEmpty().lowercase()
                IconLink(absolutize(href, url), type)
            }
            .toList()
        return icons.firstOrNull { it.isRaster() }?.href ?: icons.firstOrNull()?.href ?: ""
    }

    private fun attrOf(tag: String, name: String): String? =
        Regex("""[\s]$name\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
            .find(tag)?.groupValues?.get(1)
            ?.trim()

    private fun IconLink.isRaster(): Boolean {
        if (type.contains("svg") || type.contains("xml")) return false
        if (type.contains("png") || type.contains("jpeg") || type.contains("jpg") ||
            type.contains("webp") || type.contains("avif") || type.contains("x-icon") ||
            type.contains("vnd.microsoft.icon")
        ) return true
        return listOf(".png", ".ico", ".jpg", ".jpeg", ".webp", ".avif", ".gif")
            .any { href.substringBefore('?').substringBefore('#').endsWith(it) }
    }

    private fun fallbackMeta(url: String): BookmarkMeta =
        BookmarkMeta(title = hostOf(url) ?: url, favicon = faviconFor(url))

    fun hostOf(url: String): String? {
        if (!url.startsWith("http://") && !url.startsWith("https://")) return null
        val withoutScheme = url.substringAfter("://")
        val host = withoutScheme.substringBefore('/').substringBefore('?').substringBefore('#')
        return host.removePrefix("www.").takeIf { it.isNotBlank() }
    }

    fun faviconFor(url: String): String {
        val host = hostOf(url) ?: return ""
        return "https://www.google.com/s2/favicons?domain=$host&sz=64"
    }

    private fun absolutize(href: String, base: String): String {
        if (href.startsWith("http://") || href.startsWith("https://") || href.startsWith("data:")) return href
        val scheme = base.substringBefore("://").takeIf { it.contains("http") } ?: "https"
        val authority = base.substringAfter("://").substringBefore('/')
        if (href.startsWith("//")) return "$scheme:$href"
        return if (href.startsWith("/")) "$scheme://$authority$href" else "$scheme://$authority/$href"
    }

    private fun String.decodeEntities(): String =
        replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&#x27;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
}
