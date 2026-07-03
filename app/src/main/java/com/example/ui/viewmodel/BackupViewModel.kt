package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AppConstants
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class BackupUiState(
    val isDriveLinked: Boolean = false,
    val isLoading: Boolean = false,
    val lastSyncTime: String = "Never",
    val lastLocalBackup: String = "Never"
)

class BackupViewModel(
    application: Application,
    private val notesViewModel: NotesViewModel
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
            notesViewModel.isDriveLinked.collect { linked ->
                _uiState.update { it.copy(isDriveLinked = linked) }
            }
        }
        viewModelScope.launch {
            notesViewModel.lastSyncTime.collect { time ->
                _uiState.update { it.copy(lastSyncTime = time) }
            }
        }
        viewModelScope.launch {
            notesViewModel.syncStatusMessage.collect { msg ->
                msg?.let {
                    _snackbarMessage.value = it
                    notesViewModel.clearStatusMessage()
                }
            }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun linkDrive() {
        notesViewModel.linkGoogleDrive("ya29.simulated_access_token")
    }

    fun unlinkDrive() {
        notesViewModel.unlinkGoogleDrive()
    }

    fun backupToCloud() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                notesViewModel.forceSyncCloud()
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun restoreFromCloud() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                notesViewModel.restoreSyncCloud()
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun buildBackupJson(): String {
        val notes = notesViewModel.rawNotes.value
        val tags = notesViewModel.availableTags.value

        val notesArray = JSONArray()
        notes.forEach { note ->
            val noteObj = JSONObject().apply {
                put("id", note.id)
                put("title", note.title)
                put("content", note.content)
                put("isEncrypted", note.isEncrypted)
                put("salt", note.salt)
                put("iv", note.iv)
                put("lastModified", note.lastModified)
                put("tagsJson", note.tagsJson)
                put("isArchived", note.isArchived)
                put("isFavorite", note.isFavorite)
                put("isPinned", note.isPinned)
                put("isDeleted", note.isDeleted)
                put("backgroundColor", note.backgroundColor)
                put("backgroundImagePath", note.backgroundImagePath ?: "")
                put("categoryId", note.categoryId)
            }
            notesArray.put(noteObj)
        }

        val tagsArray = JSONArray()
        tags.forEach { tag ->
            val tagObj = JSONObject().apply {
                put("name", tag.name)
                put("colorHex", tag.colorHex)
            }
            tagsArray.put(tagObj)
        }

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

            notesViewModel.saveNote(
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
            notesViewModel.createTag(tagObj.getString("name"), tagObj.getString("colorHex"))
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
