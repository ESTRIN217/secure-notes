package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AppConstants
import com.example.R
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
    val lastLocalBackup: String = ""
)

class BackupViewModel(
    application: Application,
    private val cloudSyncManager: CloudSyncManager
) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)

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
                _uiState.update { it.copy(isDriveLinked = state.isDriveLinked, lastSyncTime = state.lastSyncTime) }
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

    fun linkGoogleDrive(token: String) {
        cloudSyncManager.linkGoogleDrive(token)
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

    fun buildBackupJson(): String {
        val notesArray = JSONArray()
        cloudSyncManager.rawNotes.value.forEach { note -> notesArray.put(note.toJson()) }

        val tagsArray = JSONArray()
        cloudSyncManager.availableTags.value.forEach { tag -> tagsArray.put(tag.toJson()) }

        return JSONObject().apply {
            put("version", 3)
            put("notes", notesArray)
            put("tags", tagsArray)
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
                isArchived = noteObj.optBoolean("isArchived", false)
            )
        }

        for (i in 0 until tagsArr.length()) {
            val tagObj = tagsArr.getJSONObject(i)
            cloudSyncManager.createTag(tagObj.getString("name"), tagObj.getString("colorHex"))
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
