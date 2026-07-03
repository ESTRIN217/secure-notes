package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.example.AppConstants
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject

data class UpdaterUiState(
    val currentVersion: String = BuildConfig.VERSION_NAME,
    val autoUpdate: Boolean = true,
    val notifications: Boolean = true,
    val isChecking: Boolean = false,
    val hasUpdate: Boolean = false,
    val latestVersion: String? = null,
    val latestChangelog: String? = null
)

class UpdaterViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        UpdaterUiState(
            autoUpdate = sharedPrefs.getBoolean(AppConstants.AUTO_UPDATE_CHECK_KEY, true),
            notifications = sharedPrefs.getBoolean(AppConstants.UPDATE_NOTIFICATIONS_KEY, true)
        )
    )
    val uiState: StateFlow<UpdaterUiState> = _uiState.asStateFlow()

    fun toggleAutoUpdate(enabled: Boolean) {
        _uiState.update { it.copy(autoUpdate = enabled) }
        sharedPrefs.edit().putBoolean(AppConstants.AUTO_UPDATE_CHECK_KEY, enabled).apply()
    }

    fun toggleNotifications(enabled: Boolean) {
        _uiState.update { it.copy(notifications = enabled) }
        sharedPrefs.edit().putBoolean(AppConstants.UPDATE_NOTIFICATIONS_KEY, enabled).apply()
    }

    fun checkForUpdates() {
        _uiState.update { it.copy(isChecking = true) }
        val currentVersion = _uiState.value.currentVersion

        Thread {
            try {
                val url = java.net.URL("https://api.github.com/repos/ESTRIN217/secure-notes/releases/latest")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)
                val latestTag = json.optString("tag_name", "")
                val latestVersion = latestTag.removePrefix("v")
                val changelog = json.optString("body", "")

                if (latestVersion.isNotEmpty()) {
                    val current = currentVersion.split(".").map { it.toIntOrNull() ?: 0 }
                    val latest = latestVersion.split(".").map { it.toIntOrNull() ?: 0 }
                    val isNewer = compareVersions(latest, current) > 0

                    _uiState.update {
                        it.copy(
                            isChecking = false,
                            hasUpdate = isNewer,
                            latestVersion = if (isNewer) latestVersion else null,
                            latestChangelog = if (isNewer && changelog.isNotEmpty()) changelog else null
                        )
                    }
                } else {
                    _uiState.update { it.copy(isChecking = false, hasUpdate = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isChecking = false, hasUpdate = false) }
            }
        }.start()
    }

    private fun compareVersions(a: List<Int>, b: List<Int>): Int {
        for (i in 0 until maxOf(a.size, b.size)) {
            val va = a.getOrElse(i) { 0 }
            val vb = b.getOrElse(i) { 0 }
            if (va != vb) return va - vb
        }
        return 0
    }
}
