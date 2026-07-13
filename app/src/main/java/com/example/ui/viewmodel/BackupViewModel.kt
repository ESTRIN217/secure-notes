package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AppConstants
import com.example.R
import com.example.data.model.SyncStage
import com.example.data.model.SyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import com.example.data.model.toJson
import com.example.data.sync.CloudSyncManager

data class BackupUiState(
    val isDriveLinked: Boolean = false,
    val isLoading: Boolean = false,
    val lastSyncTime: String = "",
    val lastLocalBackup: String = "",
    val syncStage: SyncStage = SyncStage.IDLE,
    val restorePasswordRequired: Boolean = false
)

class BackupViewModel(
    application: Application,
    private val cloudSyncManager: CloudSyncManager
) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
    val cloudSyncManagerPublic: CloudSyncManager get() = cloudSyncManager

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        observeNotesViewModel()
    }

    private fun observeNotesViewModel() {
        viewModelScope.launch {
            cloudSyncManager.syncState.collect { state ->
                _uiState.update { it.copy(isDriveLinked = state.isDriveLinked, lastSyncTime = state.lastSyncTime, syncStage = state.syncStage, restorePasswordRequired = state.syncStage == SyncStage.PASSWORD_REQUIRED) }
                state.syncStatusMessage?.let { msg ->
                    _snackbarMessage.value = msg
                    cloudSyncManager.clearStatusMessage()
                }
            }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun linkGoogleDrive(token: String, accountEmail: String = "") {
        cloudSyncManager.linkGoogleDrive(token, accountEmail)
    }

    fun unlinkDrive() {
        cloudSyncManager.unlinkGoogleDrive()
    }

    fun backupToCloud() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                cloudSyncManager.forceSyncCloud()
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun restoreFromCloud() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                cloudSyncManager.restoreSyncCloud()
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun provideRestorePassword(password: String) {
        _uiState.update { it.copy(restorePasswordRequired = false) }
        cloudSyncManager.provideRestorePassword(password)
    }

    fun buildBackupJson(): String {
        val notesArray = JSONArray()
        cloudSyncManager.rawNotes.value.forEach { note -> notesArray.put(note.toJson()) }

        val tagsArray = JSONArray()
        cloudSyncManager.availableTags.value.forEach { tag -> tagsArray.put(tag.toJson()) }

        val settings = JSONObject().apply {
            put(AppConstants.DARK_MODE_OPTION_KEY, sharedPrefs.getString(AppConstants.DARK_MODE_OPTION_KEY, "SYSTEM"))
            put(AppConstants.DYNAMIC_COLORS_KEY, sharedPrefs.getBoolean(AppConstants.DYNAMIC_COLORS_KEY, true))
            put(AppConstants.LANGUAGE_KEY, sharedPrefs.getString(AppConstants.LANGUAGE_KEY, "") ?: "")
            put(AppConstants.AUTO_UPDATE_CHECK_KEY, sharedPrefs.getBoolean(AppConstants.AUTO_UPDATE_CHECK_KEY, true))
            put(AppConstants.UPDATE_NOTIFICATIONS_KEY, sharedPrefs.getBoolean(AppConstants.UPDATE_NOTIFICATIONS_KEY, true))
            put(AppConstants.CUSTOM_ORDER_KEY, sharedPrefs.getString(AppConstants.CUSTOM_ORDER_KEY, "") ?: "")
            put(AppConstants.INCLUDE_ATTACHMENTS_KEY, sharedPrefs.getBoolean(AppConstants.INCLUDE_ATTACHMENTS_KEY, false))
            put(AppConstants.COPY_ATTACHMENTS_LOCAL_KEY, sharedPrefs.getBoolean(AppConstants.COPY_ATTACHMENTS_LOCAL_KEY, false))
        }

        return JSONObject().apply {
            put("version", 4)
            put("notes", notesArray)
            put("tags", tagsArray)
            put("settings", settings)
            put("timestamp", System.currentTimeMillis())
        }.toString(2)
    }

    fun restoreFromJson(json: String) {
        val container = JSONObject(json)
        val notesArr = container.getJSONArray("notes")
        val tagsArr = container.getJSONArray("tags")

        for (i in 0 until notesArr.length()) {
            val noteObj = notesArr.getJSONObject(i)
            val tagsJson = noteObj.optString("tagsJson", "[]")
            val tagsList = try {
                val arr = JSONArray(tagsJson)
                List(arr.length()) { arr.getString(it) }
            } catch (e: Exception) {
                emptyList()
            }

            cloudSyncManager.saveNote(
                id = 0,
                title = noteObj.getString("title"),
                content = noteObj.getString("content"),
                isEncrypted = noteObj.getBoolean("isEncrypted"),
                tagsList = tagsList,
                backgroundColor = if (noteObj.has("backgroundColor") && !noteObj.isNull("backgroundColor"))
                    noteObj.optInt("backgroundColor") else null,
                backgroundImagePath = noteObj.optString("backgroundImagePath", "").ifEmpty { null },
                isPinned = noteObj.optBoolean("isPinned", false),
                isFavorite = noteObj.optBoolean("isFavorite", false),
                isArchived = noteObj.optBoolean("isArchived", false),
                categoryId = noteObj.optString("categoryId", "").ifEmpty { null },
                isDeleted = noteObj.optBoolean("isDeleted", false),
                lastModified = noteObj.optLong("lastModified", System.currentTimeMillis()),
                salt = noteObj.optString("salt", ""),
                iv = noteObj.optString("iv", "")
            )
        }

        for (i in 0 until tagsArr.length()) {
            val tagObj = tagsArr.getJSONObject(i)
            cloudSyncManager.createTag(tagObj.getString("name"), tagObj.getString("colorHex"))
        }

        if (container.has("settings")) {
            val settings = container.getJSONObject("settings")
            val editor = sharedPrefs.edit()
            if (settings.has(AppConstants.DARK_MODE_OPTION_KEY))
                editor.putString(AppConstants.DARK_MODE_OPTION_KEY, settings.getString(AppConstants.DARK_MODE_OPTION_KEY))
            if (settings.has(AppConstants.DYNAMIC_COLORS_KEY))
                editor.putBoolean(AppConstants.DYNAMIC_COLORS_KEY, settings.getBoolean(AppConstants.DYNAMIC_COLORS_KEY))
            if (settings.has(AppConstants.LANGUAGE_KEY))
                editor.putString(AppConstants.LANGUAGE_KEY, settings.getString(AppConstants.LANGUAGE_KEY))
            if (settings.has(AppConstants.AUTO_UPDATE_CHECK_KEY))
                editor.putBoolean(AppConstants.AUTO_UPDATE_CHECK_KEY, settings.getBoolean(AppConstants.AUTO_UPDATE_CHECK_KEY))
            if (settings.has(AppConstants.UPDATE_NOTIFICATIONS_KEY))
                editor.putBoolean(AppConstants.UPDATE_NOTIFICATIONS_KEY, settings.getBoolean(AppConstants.UPDATE_NOTIFICATIONS_KEY))
            if (settings.has(AppConstants.CUSTOM_ORDER_KEY))
                editor.putString(AppConstants.CUSTOM_ORDER_KEY, settings.getString(AppConstants.CUSTOM_ORDER_KEY))
            if (settings.has(AppConstants.INCLUDE_ATTACHMENTS_KEY))
                editor.putBoolean(AppConstants.INCLUDE_ATTACHMENTS_KEY, settings.getBoolean(AppConstants.INCLUDE_ATTACHMENTS_KEY))
            if (settings.has(AppConstants.COPY_ATTACHMENTS_LOCAL_KEY))
                editor.putBoolean(AppConstants.COPY_ATTACHMENTS_LOCAL_KEY, settings.getBoolean(AppConstants.COPY_ATTACHMENTS_LOCAL_KEY))
            editor.apply()
        }
    }

    fun restoreFromUri(uri: Uri, context: Context) {
        viewModelScope.launch {
            try {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
                restoreFromJson(json)
                _snackbarMessage.value = context.getString(R.string.toast_import_backup_success)
            } catch (e: Exception) {
                _snackbarMessage.value = context.getString(R.string.toast_import_backup_error)
            }
        }
    }
}
