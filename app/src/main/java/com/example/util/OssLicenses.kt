package com.example.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

data class OssLibrary(
    val name: String,
    val version: String,
    val license: String,
    val url: String
)

private const val ASSETS_FILE = "oss-licenses.json"

suspend fun loadOssLicenses(context: Context): List<OssLibrary> {
    return withContext(Dispatchers.IO) {
        runCatching { readFromAssets(context) }
            .getOrElse { e ->
                Log.e("OssLicenses", "load failed, using fallback", e)
                fallbackLicenses()
            }
    }
}

private fun readFromAssets(context: Context): List<OssLibrary> {
    context.assets.open(ASSETS_FILE).bufferedReader().use { reader ->
        val root = org.json.JSONObject(reader.readText())
        return parseLibraries(root.optJSONArray("libraries") ?: JSONArray())
    }
}

private fun parseLibraries(array: JSONArray): List<OssLibrary> {
    val out = ArrayList<OssLibrary>(array.length())
    for (i in 0 until array.length()) {
        val o = array.optJSONObject(i) ?: continue
        val name = o.optString("name").trim()
        if (name.isEmpty()) continue
        out.add(
            OssLibrary(
                name = name,
                version = o.optString("version").trim(),
                license = o.optString("license").trim(),
                url = o.optString("url").trim()
            )
        )
    }
    return out
}

private fun fallbackLicenses(): List<OssLibrary> = listOf(
    OssLibrary("Kotlin", "2.4.10", "Apache-2.0", "https://github.com/JetBrains/kotlin/blob/master/license/LICENSE"),
    OssLibrary("Jetpack Compose", "BOM 2026.06.01", "Apache-2.0", "https://github.com/androidx/androidx/blob/androidx-main/LICENSE.txt"),
    OssLibrary("Material 3", "1.5.0-alpha25", "Apache-2.0", "https://github.com/material-components/material-components-android/blob/master/LICENSE"),
    OssLibrary("Room", "2.8.4", "Apache-2.0", "https://developer.android.com/jetpack/androidx/releases/room"),
    OssLibrary("OkHttp", "5.4.0", "Apache-2.0", "https://github.com/square/okhttp/blob/master/LICENSE.txt"),
    OssLibrary("Retrofit", "3.0.0", "Apache-2.0", "https://github.com/square/retrofit/blob/master/LICENSE.txt"),
    OssLibrary("Moshi", "1.15.2", "Apache-2.0", "https://github.com/square/moshi/blob/master/LICENSE.txt"),
    OssLibrary("Coil 3", "3.5.0", "Apache-2.0", "https://github.com/coil-kt/coil/blob/main/LICENSE.txt"),
    OssLibrary("Kotlinx Coroutines", "1.11.0", "Apache-2.0", "https://github.com/Kotlin/kotlinx.coroutines/blob/master/LICENSE.txt"),
    OssLibrary("WorkManager", "2.11.2", "Apache-2.0", "https://developer.android.com/jetpack/androidx/releases/work"),
    OssLibrary("Biometric", "1.4.0-alpha07", "Apache-2.0", "https://developer.android.com/jetpack/androidx/releases/biometric"),
    OssLibrary("Media3", "1.7.1", "Apache-2.0", "https://github.com/androidx/media/blob/release/LICENSE"),
    OssLibrary("PDF Viewer", "1.0.0-alpha19", "Apache-2.0", "https://github.com/androidx/androidx/blob/androidx-main/pdf/pdf-viewer/build.gradle"),
    OssLibrary("compose-markdown", "0.7.2", "MIT", "https://github.com/jeziellago/compose-markdown/blob/main/LICENSE"),
    OssLibrary("LaTeX", "1.5.4", "MIT", "https://github.com/huarangmeng/latex/blob/master/LICENSE"),
    OssLibrary("llama.cpp", "commit 3dc7285b4", "MIT", "https://github.com/ggml-org/llama.cpp/blob/master/LICENSE")
)
