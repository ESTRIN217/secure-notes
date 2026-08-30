package com.example.ui.viewmodel

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AppConstants
import com.example.BuildConfig
import com.example.MainActivity
import com.example.R
import com.example.data.updates.GithubAsset
import com.example.data.updates.GithubRelease
import com.example.data.updates.UpdateFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class UpdaterUiState(
    val currentVersion: String = BuildConfig.VERSION_NAME,
    val autoUpdate: Boolean = true,
    val notifications: Boolean = true,
    val notificationPermissionGranted: Boolean = true,
    val isChecking: Boolean = false,
    val hasUpdate: Boolean = false,
    val latestVersion: String? = null,
    val latestChangelog: String? = null,
    val downloadUrl: String? = null,
    val checksum: String? = null,
    val updateError: Boolean = false,
    val needsInstallPermission: Boolean = false,
    val downloadState: UpdateDownloadState = UpdateDownloadState.Idle
)

sealed class UpdateDownloadState {
    data object Idle : UpdateDownloadState()
    data class Downloading(val progress: Float, val downloadedMb: Long, val totalMb: Long) : UpdateDownloadState()
    data object PreparingInstall : UpdateDownloadState()
    data class DownloadFailed(val error: String) : UpdateDownloadState()
}

private data class CheckResult(
    val latestVersion: String?,
    val changelog: String?,
    val downloadUrl: String?,
    val checksum: String?,
    val isNewer: Boolean
)

class UpdaterViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application

    private val sharedPrefs = application.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)

    private val fetcher = UpdateFetcher()

    private val downloadClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    @Volatile
    private var downloadCancelled = false
    private var currentCall: okhttp3.Call? = null

    private val _uiState = MutableStateFlow(
        UpdaterUiState(
            autoUpdate = sharedPrefs.getBoolean(AppConstants.AUTO_UPDATE_CHECK_KEY, true),
            notifications = sharedPrefs.getBoolean(AppConstants.UPDATE_NOTIFICATIONS_KEY, true),
            notificationPermissionGranted = canPostNotifications()
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

    fun refreshNotificationPermission() {
        _uiState.update { it.copy(notificationPermissionGranted = canPostNotifications()) }
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(notificationPermissionGranted = granted) }
    }

    fun checkForUpdates(force: Boolean = true) {
        if (!force && !shouldCheck(lastUpdateCheckTime(), System.currentTimeMillis(), UPDATE_CHECK_INTERVAL_MS)) {
            return
        }
        _uiState.update { it.copy(isChecking = true, updateError = false) }
        viewModelScope.launch(Dispatchers.IO) {
            val result = try {
                performCheck()
            } catch (e: Exception) {
                Log.e(TAG, "check for updates failed", e)
                null
            }
            persistUpdateCheck()
            if (result == null) {
                _uiState.update { it.copy(isChecking = false, hasUpdate = false, updateError = true, checksum = null) }
            } else {
                _uiState.update { applyCheckResult(it, result) }
            }
        }
    }

    fun checkForUpdatesSilently() {
        if (!_uiState.value.autoUpdate) return
        val lastCheck = lastUpdateCheckTime()
        viewModelScope.launch(Dispatchers.IO) {
            if (!shouldCheck(lastCheck, System.currentTimeMillis(), UPDATE_CHECK_INTERVAL_MS)) return@launch
            val result = try {
                performCheck()
            } catch (e: Exception) {
                Log.e(TAG, "silent check for updates failed", e)
                null
            }
            persistUpdateCheck()
            result?.let { r ->
                if (r.isNewer) {
                    _uiState.update { applyCheckResult(it, r) }
                    if (_uiState.value.notifications && canPostNotifications()) {
                        notifyUpdateAvailable(r.latestVersion ?: "", r.changelog)
                    }
                }
            }
        }
    }

    private suspend fun performCheck(): CheckResult {
        val release = fetcher.fetchLatestRelease()
        if (release.version.isEmpty()) {
            return CheckResult(null, null, null, null, isNewer = false)
        }
        val currentVersion = _uiState.value.currentVersion
        val isNewer = compareVersionsInternal(release.version, currentVersion) > 0
        val apk = if (isNewer) selectApkAsset(release.assets, preferredAbi()) else null
        return CheckResult(
            latestVersion = release.version,
            changelog = release.body,
            downloadUrl = apk?.url,
            checksum = if (isNewer) resolveChecksum(release, apk) else null,
            isNewer = isNewer
        )
    }

    private suspend fun resolveChecksum(release: GithubRelease, apk: GithubAsset?): String? {
        extractSha256(release.body)?.let { return it }
        if (apk == null) return null
        val checksumAsset = release.assets.firstOrNull {
            it.name.equals("${apk.name}.sha256", ignoreCase = true) ||
                it.name.contains("sha256", ignoreCase = true)
        } ?: return null
        val text = try {
            fetcher.fetchAssetText(checksumAsset.url)
        } catch (e: Exception) {
            Log.e(TAG, "failed to fetch checksum asset", e)
            null
        } ?: return null
        return extractSha256(text)
    }

    private fun applyCheckResult(state: UpdaterUiState, result: CheckResult): UpdaterUiState {
        return if (result.isNewer) {
            state.copy(
                isChecking = false,
                hasUpdate = true,
                updateError = false,
                latestVersion = result.latestVersion,
                latestChangelog = result.changelog,
                downloadUrl = result.downloadUrl,
                checksum = result.checksum
            )
        } else {
            state.copy(
                isChecking = false,
                hasUpdate = false,
                updateError = false,
                latestVersion = null,
                latestChangelog = null,
                downloadUrl = null,
                checksum = null
            )
        }
    }

    fun downloadAndInstall() {
        if (_uiState.value.downloadState is UpdateDownloadState.Downloading) return
        val downloadUrl = _uiState.value.downloadUrl
        if (downloadUrl == null) {
            _uiState.update { it.copy(downloadState = UpdateDownloadState.DownloadFailed("No download URL")) }
            return
        }
        val expectedChecksum = _uiState.value.checksum
        _uiState.update { it.copy(downloadState = UpdateDownloadState.Downloading(0f, 0, 0), needsInstallPermission = false) }
        val targetFile = File(appContext.cacheDir, APK_FILE_NAME)
        downloadCancelled = false

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(downloadUrl).header("User-Agent", fetcher.userAgent()).build()
                val call = downloadClient.newCall(request)
                currentCall = call
                call.execute().use { response ->
                    if (!response.isSuccessful) {
                        throw java.io.IOException("Download returned ${response.code}")
                    }
                    val body = response.body
                    val totalBytes = if (body.contentLength() > 0) body.contentLength() else 0L
                    var bytesRead = 0L
                    targetFile.outputStream().use { output ->
                        val buffer = ByteArray(32 * 1024)
                        val inputStream = body.byteStream()
                        while (true) {
                            val read = inputStream.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            bytesRead += read
                            if (totalBytes > 0) {
                                _uiState.update {
                                    it.copy(
                                        downloadState = UpdateDownloadState.Downloading(
                                            progress = (bytesRead.toFloat() / totalBytes).coerceAtMost(1f),
                                            downloadedMb = bytesRead / (1024 * 1024),
                                            totalMb = totalBytes / (1024 * 1024)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                currentCall = null
                if (!expectedChecksum.isNullOrEmpty()) {
                    val actual = sha256Of(targetFile)
                    if (actual == null || !actual.equals(expectedChecksum, ignoreCase = true)) {
                        Log.e(TAG, "checksum mismatch: expected=$expectedChecksum actual=$actual")
                        targetFile.delete()
                        _uiState.update { it.copy(downloadState = UpdateDownloadState.DownloadFailed("Checksum verification failed")) }
                        return@launch
                    }
                } else {
                    Log.w(TAG, "No checksum available; skipping integrity verification")
                }
                _uiState.update { it.copy(downloadState = UpdateDownloadState.PreparingInstall) }
                installApk(targetFile)
            } catch (e: Exception) {
                currentCall = null
                if (downloadCancelled) {
                    targetFile.delete()
                    _uiState.update { it.copy(downloadState = UpdateDownloadState.Idle) }
                } else {
                    Log.e(TAG, "download failed", e)
                    targetFile.delete()
                    _uiState.update { it.copy(downloadState = UpdateDownloadState.DownloadFailed(e.message ?: "Download failed")) }
                }
            }
        }
    }

    fun cancelDownload() {
        downloadCancelled = true
        currentCall?.cancel()
        currentCall = null
        _uiState.update { it.copy(downloadState = UpdateDownloadState.Idle) }
    }

    fun requestInstallPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${appContext.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            appContext.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "install permission intent failed", e)
            _uiState.update { it.copy(needsInstallPermission = false, downloadState = UpdateDownloadState.Idle) }
        }
    }

    fun onInstallPermissionResult() {
        if (canRequestPackageInstalls()) {
            _uiState.update { it.copy(needsInstallPermission = false) }
            val file = File(appContext.cacheDir, APK_FILE_NAME)
            if (file.exists()) {
                installApk(file)
            } else {
                _uiState.update { it.copy(downloadState = UpdateDownloadState.Idle) }
            }
        } else {
            _uiState.update { it.copy(needsInstallPermission = true) }
        }
    }

    private fun installApk(file: File) {
        if (!canRequestPackageInstalls()) {
            _uiState.update { it.copy(needsInstallPermission = true, downloadState = UpdateDownloadState.Idle) }
            return
        }
        _uiState.update { it.copy(needsInstallPermission = false) }
        val uri: Uri = FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            appContext.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "install intent failed", e)
            _uiState.update { it.copy(downloadState = UpdateDownloadState.DownloadFailed(e.message ?: "Install failed")) }
        }
    }

    private fun canRequestPackageInstalls(): Boolean {
        return appContext.packageManager.canRequestPackageInstalls()
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    fun emitUpdateNotificationIfGranted() {
        if (!canPostNotifications() || !_uiState.value.hasUpdate) return
        notifyUpdateAvailable(_uiState.value.latestVersion ?: "", _uiState.value.latestChangelog)
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                appContext.getString(R.string.update_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = appContext.getString(R.string.update_channel_description)
            }
            val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun notifyUpdateAvailable(version: String, changelog: String?) {
        val contentIntent = PendingIntent.getActivity(
            appContext,
            0,
            Intent(appContext, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(EXTRA_OPEN_UPDATE, true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(appContext, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(appContext.getString(R.string.update_notification_title, version))
            .setContentText(changelog?.take(120) ?: appContext.getString(R.string.update_notification_text))
            .setStyle(NotificationCompat.BigTextStyle().bigText(changelog ?: appContext.getString(R.string.update_notification_text)))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            manager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            Log.e(TAG, "notification permission missing", e)
        }
    }

    private fun preferredAbi(): String {
        return Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"
    }

    private fun lastUpdateCheckTime(): Long {
        return sharedPrefs.getLong(AppConstants.LAST_UPDATE_CHECK_KEY, 0L)
    }

    private fun persistUpdateCheck() {
        sharedPrefs.edit().putLong(AppConstants.LAST_UPDATE_CHECK_KEY, System.currentTimeMillis()).apply()
    }

    internal companion object {
        const val TAG = "UpdaterViewModel"
        const val APK_FILE_NAME = "secure-notes.apk"
        const val NOTIFICATION_CHANNEL_ID = "updates"
        const val NOTIFICATION_ID = 1001
        const val EXTRA_OPEN_UPDATE = "extra_open_update"
        const val UPDATE_CHECK_INTERVAL_MS = 24L * 60 * 60 * 1000

        private val SHA256_REGEX = Regex("[0-9a-fA-F]{64}")

        fun shouldCheck(lastCheck: Long, now: Long, intervalMs: Long): Boolean {
            return now - lastCheck >= intervalMs
        }

        fun cleanVersion(version: String): List<String> {
            val core = version.substringBefore("+").substringBefore("-")
            return core.split(".")
        }

        fun compareVersionsInternal(a: String, b: String): Int {
            val pa = cleanVersion(a).map { it.toIntOrNull() ?: 0 }
            val pb = cleanVersion(b).map { it.toIntOrNull() ?: 0 }
            for (i in 0 until maxOf(pa.size, pb.size)) {
                val va = pa.getOrElse(i) { 0 }
                val vb = pb.getOrElse(i) { 0 }
                if (va != vb) return va - vb
            }
            return 0
        }

        fun extractSha256(text: String): String? {
            return SHA256_REGEX.find(text)?.value?.lowercase()
        }

        fun sha256Of(file: File): String? {
            return try {
                val digest = MessageDigest.getInstance("SHA-256")
                file.inputStream().use { input ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        digest.update(buffer, 0, read)
                    }
                }
                digest.digest().joinToString("") { "%02x".format(it) }
            } catch (e: Exception) {
                null
            }
        }

        fun selectApkAsset(assets: List<GithubAsset>, abi: String?): GithubAsset? {
            val apks = assets.filter { it.name.endsWith(".apk") }
            if (apks.isEmpty()) return null
            abi?.let { preferred ->
                apks.firstOrNull { it.name.contains(preferred, ignoreCase = true) }?.let { return it }
            }
            apks.firstOrNull { it.name.contains("universal", ignoreCase = true) }?.let { return it }
            return apks.firstOrNull()
        }

        fun parseApkUrlInternal(json: JSONObject): String? {
            return parseApkUrlForAbiInternal(json, null)
        }

        fun parseApkUrlForAbiInternal(json: JSONObject, abi: String?): String? {
            val assets = mutableListOf<GithubAsset>()
            json.optJSONArray("assets")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i)
                    val name = obj?.optString("name", "") ?: ""
                    val url = obj?.optString("browser_download_url", "") ?: ""
                    assets.add(GithubAsset(name, url))
                }
            }
            return selectApkAsset(assets, abi)?.url
        }
    }
}