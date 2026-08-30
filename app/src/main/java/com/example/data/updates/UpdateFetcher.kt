package com.example.data.updates

import android.os.Build
import com.example.BuildConfig
import com.example.data.AppUpdateConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class GithubAsset(
    val name: String,
    val url: String
)

data class GithubRelease(
    val version: String,
    val title: String,
    val body: String,
    val date: String,
    val assets: List<GithubAsset>
)

class UpdateFetcher {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun fetchLatestRelease(): GithubRelease {
        return withContext(Dispatchers.IO) {
            val json = JSONObject(execute(AppUpdateConfig.latestReleaseUrl))
            parseRelease(json)
        }
    }

    suspend fun fetchAssetText(url: String): String {
        return withContext(Dispatchers.IO) { execute(url) }
    }

    suspend fun fetchReleases(limit: Int = 100): List<GithubRelease> {
        return withContext(Dispatchers.IO) {
            val jsonArray = JSONArray(execute(AppUpdateConfig.releasesListUrl))
            val list = mutableListOf<GithubRelease>()
            for (i in 0 until jsonArray.length().coerceAtMost(limit)) {
                list.add(parseRelease(jsonArray.getJSONObject(i)))
            }
            list
        }
    }

    fun userAgent(): String {
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"
        return "SecureNotes/${BuildConfig.VERSION_NAME} (Android; $abi)"
    }

    private fun execute(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent())
            .header("Accept", "application/vnd.github.v3+json")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw java.io.IOException("GitHub API returned ${response.code}")
            }
            return response.body.string()
        }
    }

    private fun parseRelease(json: JSONObject): GithubRelease {
        val assets = mutableListOf<GithubAsset>()
        json.optJSONArray("assets")?.let { arr ->
            for (i in 0 until arr.length()) {
                val asset = arr.optJSONObject(i)
                val name = asset?.optString("name", "") ?: ""
                val url = asset?.optString("browser_download_url", "") ?: ""
                if (name.isNotEmpty() && url.isNotEmpty()) {
                    assets.add(GithubAsset(name, url))
                }
            }
        }
        val rawDate = json.optString("published_at", "")
        return GithubRelease(
            version = json.optString("tag_name", "").removePrefix("v"),
            title = json.optString("name", ""),
            body = json.optString("body", ""),
            date = if (rawDate.length >= 10) rawDate.substring(0, 10) else rawDate,
            assets = assets
        )
    }
}