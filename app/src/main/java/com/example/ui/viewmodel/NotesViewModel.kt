package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.NoteDatabase
import com.example.data.model.Note
import com.example.data.model.Tag
import com.example.data.model.ListItem
import com.example.data.repository.NoteRepository
import com.example.data.security.EncryptionUtils
import com.example.data.sync.GoogleDriveSyncService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import com.example.AppConstants
import com.example.DarkModeOption
import com.example.R

data class DecryptedNote(
    val note: Note,
    val title: String,
    val content: String,
    val isDecryptionSuccessful: Boolean
) {
    fun toListItem(): ListItem {
        val tagsList = try {
            val arr = JSONArray(note.tagsJson)
            List(arr.length()) { arr.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
        return ListItem(
            id = note.id.toString(),
            title = title,
            summary = content,
            lastModified = Instant.ofEpochMilli(note.lastModified),
            backgroundColor = note.backgroundColor,
            backgroundImagePath = note.backgroundImagePath,
            tags = tagsList,
            isArchived = note.isArchived,
            isFavorite = note.isFavorite,
            categoryId = note.categoryId,
            isPinned = note.isPinned,
            isDeleted = note.isDeleted
        )
    }
}

enum class NavigationSection {
    HOME, FAVORITES, ARCHIVED, TRASH, SETTINGS
}

class NotesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: NoteRepository
    private val sharedPrefs = application.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)

    private fun decryptNote(note: Note, password: String?): DecryptedNote {
        if (!note.isEncrypted) return DecryptedNote(note, note.title, note.content, true)
        val pass = password ?: ""
        if (pass.isEmpty()) return DecryptedNote(note, "[Encrypted]", "[Unlock to read notes]", false)
        val decTitle = EncryptionUtils.decrypt(note.title, pass, note.salt, note.iv)
        val decContent = EncryptionUtils.decrypt(note.content, pass, note.salt, note.iv)
        return if (decTitle.isEmpty() && decContent.isEmpty()) {
            DecryptedNote(note, "[Corrupted / Wrong Password]", "[Cannot decrypt]", false)
        } else {
            DecryptedNote(note, decTitle, decContent, true)
        }
    }

    // Dark Mode state (tri-state: SYSTEM / OFF / ON)
    val darkModeOption = MutableStateFlow(
        try {
            DarkModeOption.valueOf(sharedPrefs.getString(AppConstants.DARK_MODE_OPTION_KEY, DarkModeOption.SYSTEM.name) ?: DarkModeOption.SYSTEM.name)
        } catch (e: Exception) {
            // Migrate from old boolean key
            val oldBool = sharedPrefs.getBoolean(AppConstants.DARK_MODE_KEY, false)
            sharedPrefs.edit().remove(AppConstants.DARK_MODE_KEY).apply()
            if (oldBool) DarkModeOption.ON else DarkModeOption.SYSTEM
        }
    )

    // Dynamic Colors
    val isDynamicColor = MutableStateFlow(sharedPrefs.getBoolean(AppConstants.DYNAMIC_COLORS_KEY, true))

    // Language selection
    val language = MutableStateFlow(sharedPrefs.getString(AppConstants.LANGUAGE_KEY, "") ?: "")

    // Auto update check
    val autoUpdateCheck = MutableStateFlow(sharedPrefs.getBoolean(AppConstants.AUTO_UPDATE_CHECK_KEY, true))

    // Password credentials state
    val isPasswordSet = MutableStateFlow(sharedPrefs.contains(AppConstants.MASTER_PASSWORD_HASH_KEY))
    val isUnlocked = MutableStateFlow(!sharedPrefs.contains(AppConstants.MASTER_PASSWORD_HASH_KEY))
    private val masterPassword = MutableStateFlow<String?>(null)

    // Navigation and Filtering state
    val currentSection = MutableStateFlow(NavigationSection.HOME)

    // Search and Tags state
    val searchQuery = MutableStateFlow("")
    val selectedTagFilter = MutableStateFlow<String?>(null)

    // Google Drive state
    val isDriveLinked = MutableStateFlow(sharedPrefs.getBoolean(AppConstants.DRIVE_LINKED_KEY, false))
    val driveAccessToken = MutableStateFlow(sharedPrefs.getString(AppConstants.DRIVE_ACCESS_TOKEN_KEY, "") ?: "")
    val lastSyncTime = MutableStateFlow(sharedPrefs.getString(AppConstants.LAST_SYNC_TIME_KEY, "Never") ?: "Never")
    val syncStatusMessage = MutableStateFlow<String?>(null)

    // Data lists from Room
    val availableTags: StateFlow<List<Tag>>
    val rawNotes: StateFlow<List<Note>>

    // Main UI Decrypted Notes state
    val notesList: StateFlow<List<DecryptedNote>>
    val searchResults: StateFlow<List<DecryptedNote>>

    init {
        val database = NoteDatabase.getDatabase(application)
        repository = NoteRepository(database.noteDao)

        availableTags = repository.allTagsFlow.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            emptyList()
        )

        rawNotes = repository.allNotesFlow.stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            emptyList()
        )

        // Combine notes processing
        notesList = combine(
            rawNotes,
            masterPassword,
            searchQuery,
            selectedTagFilter,
            currentSection
        ) { notes, password, query, tag, section ->
            val decryptedList = notes.map { decryptNote(it, password) }

            // Filter by search query, tag and section
            decryptedList.filter { decryptedNote ->
                val note = decryptedNote.note

                // Section Filter
                val matchesSection = when (section) {
                    NavigationSection.HOME -> !note.isArchived && !note.isDeleted
                    NavigationSection.FAVORITES -> note.isFavorite && !note.isArchived && !note.isDeleted
                    NavigationSection.ARCHIVED -> note.isArchived && !note.isDeleted
                    NavigationSection.TRASH -> note.isDeleted
                    NavigationSection.SETTINGS -> false
                }

                if (!matchesSection) return@filter false

                val matchesQuery = query.isEmpty() ||
                        decryptedNote.title.contains(query, ignoreCase = true) ||
                        decryptedNote.content.contains(query, ignoreCase = true)

                val matchesTag = tag == null || run {
                    try {
                        val tagsArr = JSONArray(decryptedNote.note.tagsJson)
                        var found = false
                        for (i in 0 until tagsArr.length()) {
                            if (tagsArr.optString(i) == tag) {
                                found = true
                                break
                            }
                        }
                        found
                    } catch (e: Exception) {
                        false
                    }
                }

                matchesQuery && matchesTag
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        searchResults = combine(
            rawNotes,
            masterPassword
        ) { notes, password ->
            notes.map { decryptNote(it, password) }.filter { decryptedNote ->
                !decryptedNote.note.isDeleted
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        // Seed default tags if empty
        viewModelScope.launch {
            repository.allTagsFlow.collect { tags ->
                if (tags.isEmpty()) {
                    repository.insertTag(Tag("Work", "#42A5F5"))
                    repository.insertTag(Tag("Personal", "#66BB6A"))
                    repository.insertTag(Tag("Private", "#EC407A"))
                }
            }
        }

        // Seed default notes if empty
        viewModelScope.launch {
            try {
                val notes = repository.allNotesFlow.first()
                if (notes.isEmpty()) {
                    val welcomeTitle = getApplication<Application>().getString(com.example.R.string.welcome_note_title)
                    val welcomeContent = getApplication<Application>().getString(com.example.R.string.welcome_note_content)
                    val welcomeNote = Note(
                        id = 0,
                        title = welcomeTitle,
                        content = welcomeContent,
                        isEncrypted = false,
                        salt = "",
                        iv = "",
                        tagsJson = "[\"Personal\"]",
                        lastModified = System.currentTimeMillis()
                    )
                    repository.insertNote(welcomeNote)

                    val workoutTitle = getApplication<Application>().getString(com.example.R.string.workout_note_title)
                    val workoutContent = getApplication<Application>().getString(com.example.R.string.workout_note_content)
                    val workoutNote = Note(
                        id = 0,
                        title = workoutTitle,
                        content = workoutContent,
                        isEncrypted = false,
                        salt = "",
                        iv = "",
                        tagsJson = "[\"Personal\"]",
                        lastModified = System.currentTimeMillis()
                    )
                    repository.insertNote(workoutNote)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setDarkModeOption(option: DarkModeOption) {
        darkModeOption.value = option
        sharedPrefs.edit().putString(AppConstants.DARK_MODE_OPTION_KEY, option.name).apply()
    }

    fun setDynamicColor(enabled: Boolean) {
        isDynamicColor.value = enabled
        sharedPrefs.edit().putBoolean(AppConstants.DYNAMIC_COLORS_KEY, enabled).apply()
    }

    fun setLanguage(locale: String) {
        language.value = locale
        sharedPrefs.edit().putString(AppConstants.LANGUAGE_KEY, locale).apply()
    }

    fun setAutoUpdateCheck(enabled: Boolean) {
        autoUpdateCheck.value = enabled
        sharedPrefs.edit().putBoolean(AppConstants.AUTO_UPDATE_CHECK_KEY, enabled).apply()
    }

    fun setMasterPassword(password: String) {
        // Compute pseudo-hash to store in preference for validation
        val salt = EncryptionUtils.generateSalt()
        val iv = EncryptionUtils.generateIv()
        val validationHash = EncryptionUtils.encrypt("VALID", password, salt, iv)
        
        sharedPrefs.edit()
            .putString(AppConstants.MASTER_PASSWORD_HASH_KEY, validationHash)
            .putString(AppConstants.MASTER_PASSWORD_SALT_KEY, salt)
            .putString(AppConstants.MASTER_PASSWORD_IV_KEY, iv)
            .apply()

        masterPassword.value = password
        isPasswordSet.value = true
        isUnlocked.value = true
    }

    fun unlockApp(password: String): Boolean {
        val hash = sharedPrefs.getString(AppConstants.MASTER_PASSWORD_HASH_KEY, "") ?: ""
        val salt = sharedPrefs.getString(AppConstants.MASTER_PASSWORD_SALT_KEY, "") ?: ""
        val iv = sharedPrefs.getString(AppConstants.MASTER_PASSWORD_IV_KEY, "") ?: ""

        val result = EncryptionUtils.decrypt(hash, password, salt, iv)
        return if (result == "VALID") {
            masterPassword.value = password
            isUnlocked.value = true
            true
        } else {
            false
        }
    }

    fun lockApp() {
        if (isPasswordSet.value) {
            masterPassword.value = null
            isUnlocked.value = false
        }
    }

    fun deletePassword() {
        sharedPrefs.edit()
            .remove(AppConstants.MASTER_PASSWORD_HASH_KEY)
            .remove(AppConstants.MASTER_PASSWORD_SALT_KEY)
            .remove(AppConstants.MASTER_PASSWORD_IV_KEY)
            .apply()
        
        // Convert all currently encrypted notes back to plain text if unlocked,
        // or just clean up master pass
        viewModelScope.launch {
            val password = masterPassword.value ?: ""
            if (password.isNotEmpty()) {
                rawNotes.value.forEach { note ->
                    if (note.isEncrypted) {
                        val decTitle = EncryptionUtils.decrypt(note.title, password, note.salt, note.iv)
                        val decContent = EncryptionUtils.decrypt(note.content, password, note.salt, note.iv)
                        if (decTitle.isNotEmpty() || decContent.isNotEmpty()) {
                            repository.updateNote(
                                note.copy(
                                    title = decTitle,
                                    content = decContent,
                                    isEncrypted = false,
                                    salt = "",
                                    iv = ""
                                )
                            )
                        }
                    }
                }
            }
            masterPassword.value = null
            isPasswordSet.value = false
            isUnlocked.value = true
        }
    }

    fun saveNote(
        id: Int,
        title: String,
        content: String,
        isEncrypted: Boolean,
        tagsList: List<String>,
        backgroundColor: Int? = null,
        backgroundImagePath: String? = null,
        isPinned: Boolean = false,
        isFavorite: Boolean = false,
        isArchived: Boolean = false
    ) {
        viewModelScope.launch {
            saveNoteAndGetId(id, title, content, isEncrypted, tagsList, backgroundColor, backgroundImagePath, isPinned, isFavorite, isArchived)
        }
    }

    suspend fun saveNoteAndGetId(
        id: Int,
        title: String,
        content: String,
        isEncrypted: Boolean,
        tagsList: List<String>,
        backgroundColor: Int? = null,
        backgroundImagePath: String? = null,
        isPinned: Boolean = false,
        isFavorite: Boolean = false,
        isArchived: Boolean = false
    ): Int {
        val salt = if (isEncrypted) EncryptionUtils.generateSalt() else ""
        val iv = if (isEncrypted) EncryptionUtils.generateIv() else ""

        val pass = if (isEncrypted) masterPassword.value ?: "" else ""
        val storedTitle = if (isEncrypted) EncryptionUtils.encrypt(title, pass, salt, iv) else title
        val storedContent = if (isEncrypted) EncryptionUtils.encrypt(content, pass, salt, iv) else content

        val tagsJson = JSONArray(tagsList).toString()

        val existing = if (id != 0) {
            repository.allNotesFlow.first().find { it.id == id }
        } else null

        val note = Note(
            id = if (id != 0) id else 0,
            title = storedTitle,
            content = storedContent,
            isEncrypted = isEncrypted,
            salt = salt,
            iv = iv,
            tagsJson = tagsJson,
            lastModified = System.currentTimeMillis(),
            isArchived = isArchived,
            isFavorite = isFavorite,
            isPinned = isPinned,
            isDeleted = existing?.isDeleted ?: false,
            backgroundColor = backgroundColor,
            backgroundImagePath = backgroundImagePath ?: existing?.backgroundImagePath,
            categoryId = existing?.categoryId
        )

        return if (id == 0) {
            repository.insertNote(note).toInt()
        } else {
            repository.updateNote(note)
            id
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    fun toggleFavorite(note: Note) {
        viewModelScope.launch {
            repository.updateNote(note.copy(isFavorite = !note.isFavorite, lastModified = System.currentTimeMillis()))
        }
    }

    fun batchTogglePin(noteIds: Set<Int>) {
        viewModelScope.launch {
            try {
                val allNotes = repository.allNotesFlow.first()
                val selected = allNotes.filter { it.id in noteIds }
                val hasUnpinned = selected.any { !it.isPinned }
                selected.forEach { note ->
                    repository.updateNote(note.copy(isPinned = hasUnpinned, lastModified = System.currentTimeMillis()))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun batchToggleFavorite(noteIds: Set<Int>) {
        viewModelScope.launch {
            try {
                val allNotes = repository.allNotesFlow.first()
                val selected = allNotes.filter { it.id in noteIds }
                val hasUnfav = selected.any { !it.isFavorite }
                selected.forEach { note ->
                    repository.updateNote(note.copy(isFavorite = hasUnfav, lastModified = System.currentTimeMillis()))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun batchToggleArchive(noteIds: Set<Int>) {
        viewModelScope.launch {
            try {
                val allNotes = repository.allNotesFlow.first()
                val selected = allNotes.filter { it.id in noteIds }
                val hasUnarchived = selected.any { !it.isArchived }
                selected.forEach { note ->
                    repository.updateNote(note.copy(isArchived = hasUnarchived, lastModified = System.currentTimeMillis()))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun batchUpdateTags(noteIds: Set<Int>, tagNames: List<String>) {
        viewModelScope.launch {
            try {
                val allNotes = repository.allNotesFlow.first()
                val selected = allNotes.filter { it.id in noteIds }
                val allAvailTags = availableTags.value.map { it.name }
                selected.forEach { note ->
                    val existingTags = mutableListOf<String>()
                    try {
                        val arr = JSONArray(note.tagsJson)
                        for (i in 0 until arr.length()) {
                            val tag = arr.optString(i)
                            if (tag.isNotEmpty()) {
                                existingTags.add(tag)
                            }
                        }
                    } catch (e: Exception) {
                        // ignore
                    }

                    allAvailTags.forEach { availTag ->
                        if (availTag in tagNames) {
                            if (availTag !in existingTags) {
                                existingTags.add(availTag)
                            }
                        } else {
                            existingTags.remove(availTag)
                        }
                    }

                    val updatedTagsJson = JSONArray(existingTags).toString()
                    repository.updateNote(note.copy(tagsJson = updatedTagsJson, lastModified = System.currentTimeMillis()))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleArchive(note: Note) {
        viewModelScope.launch {
            repository.updateNote(note.copy(isArchived = !note.isArchived, lastModified = System.currentTimeMillis()))
        }
    }

    fun moveToTrash(note: Note) {
        viewModelScope.launch {
            repository.updateNote(note.copy(isDeleted = true, lastModified = System.currentTimeMillis()))
        }
    }

    fun restoreFromTrash(note: Note) {
        viewModelScope.launch {
            repository.updateNote(note.copy(isDeleted = false, lastModified = System.currentTimeMillis()))
        }
    }

    fun deletePermanently(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    fun createTag(name: String, colorHex: String) {
        viewModelScope.launch {
            repository.insertTag(Tag(name, colorHex))
        }
    }

    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            repository.deleteTag(tag)
            try {
                val allNotes = repository.allNotesFlow.first()
                allNotes.forEach { note ->
                    try {
                        val arr = JSONArray(note.tagsJson)
                        val existingTags = mutableListOf<String>()
                        var modified = false
                        for (i in 0 until arr.length()) {
                            val tagName = arr.optString(i)
                            if (tagName == tag.name) {
                                modified = true
                            } else if (tagName.isNotEmpty()) {
                                existingTags.add(tagName)
                            }
                        }
                        if (modified) {
                            val updatedTagsJson = JSONArray(existingTags).toString()
                            repository.updateNote(note.copy(tagsJson = updatedTagsJson, lastModified = System.currentTimeMillis()))
                        }
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (selectedTagFilter.value == tag.name) {
                selectedTagFilter.value = null
            }
        }
    }

    fun updateTag(oldTag: Tag, newName: String, newColorHex: String) {
        viewModelScope.launch {
            if (oldTag.name == newName) {
                repository.insertTag(Tag(newName, newColorHex))
            } else {
                repository.insertTag(Tag(newName, newColorHex))
                repository.deleteTag(oldTag)
                try {
                    val allNotes = repository.allNotesFlow.first()
                    allNotes.forEach { note ->
                        try {
                            val arr = JSONArray(note.tagsJson)
                            val existingTags = mutableListOf<String>()
                            var modified = false
                            for (i in 0 until arr.length()) {
                                val tagName = arr.optString(i)
                                if (tagName == oldTag.name) {
                                    existingTags.add(newName)
                                    modified = true
                                } else if (tagName.isNotEmpty()) {
                                    existingTags.add(tagName)
                                }
                            }
                            if (modified) {
                                val updatedTagsJson = JSONArray(existingTags).toString()
                                repository.updateNote(note.copy(tagsJson = updatedTagsJson, lastModified = System.currentTimeMillis()))
                            }
                        } catch (e: Exception) {
                            // ignore
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                if (selectedTagFilter.value == oldTag.name) {
                    selectedTagFilter.value = newName
                }
            }
        }
    }

    // Google Drive Integration
    fun linkGoogleDrive(token: String) {
        sharedPrefs.edit()
            .putBoolean(AppConstants.DRIVE_LINKED_KEY, true)
            .putString(AppConstants.DRIVE_ACCESS_TOKEN_KEY, token)
            .apply()
        
        isDriveLinked.value = true
        driveAccessToken.value = token
        syncStatusMessage.value = getApplication<Application>().getString(R.string.toast_drive_connected)
    }

    fun unlinkGoogleDrive() {
        sharedPrefs.edit()
            .putBoolean(AppConstants.DRIVE_LINKED_KEY, false)
            .remove(AppConstants.DRIVE_ACCESS_TOKEN_KEY)
            .apply()
        
        isDriveLinked.value = false
        driveAccessToken.value = ""
        syncStatusMessage.value = getApplication<Application>().getString(R.string.toast_drive_disconnected)
    }

    fun forceSyncCloud() {
        val token = driveAccessToken.value
        if (token.isEmpty()) {
            syncStatusMessage.value = getApplication<Application>().getString(R.string.toast_drive_auth_first)
            return
        }

        viewModelScope.launch {
            syncStatusMessage.value = getApplication<Application>().getString(R.string.toast_syncing)
            try {
                // 1. Pack all notes & tags into a solid encrypted transport JSON format
                val notesArray = JSONArray()
                rawNotes.value.forEach { note ->
                    val noteObj = JSONObject().apply {
                        put("id", note.id)
                        put("title", note.title)
                        put("content", note.content)
                        put("isEncrypted", note.isEncrypted)
                        put("salt", note.salt)
                        put("iv", note.iv)
                        put("lastModified", note.lastModified)
                        put("tagsJson", note.tagsJson)
                    }
                    notesArray.put(noteObj)
                }

                val tagsArray = JSONArray()
                availableTags.value.forEach { tag ->
                    val tagObj = JSONObject().apply {
                        put("name", tag.name)
                        put("colorHex", tag.colorHex)
                    }
                    tagsArray.put(tagObj)
                }

                val syncPayload = JSONObject().apply {
                    put("notes", notesArray)
                    put("tags", tagsArray)
                    put("timestamp", System.currentTimeMillis())
                }.toString()

                // Encrypt payload if app security features are set up
                val finalPayload: String = if (isPasswordSet.value && masterPassword.value != null) {
                    val pass = masterPassword.value!!
                    val salt = EncryptionUtils.generateSalt()
                    val iv = EncryptionUtils.generateIv()
                    val cipherPayload = EncryptionUtils.encrypt(syncPayload, pass, salt, iv)
                    JSONObject().apply {
                        put("encrypted", true)
                        put("salt", salt)
                        put("iv", iv)
                        put("data", cipherPayload)
                    }.toString()
                } else {
                    JSONObject().apply {
                        put("encrypted", false)
                        put("data", syncPayload)
                    }.toString()
                }

                // 2. Execute backup to Drive API
                val existingFileId = GoogleDriveSyncService.searchBackupFile(token)
                val success: Boolean
                if (existingFileId != null) {
                    success = GoogleDriveSyncService.uploadFileContent(token, existingFileId, finalPayload)
                } else {
                    val createdFileId = GoogleDriveSyncService.createBackupFile(token, finalPayload)
                    success = createdFileId != null
                }

                if (success) {
                    val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    val timeStr = formatter.format(Date())
                    sharedPrefs.edit().putString(AppConstants.LAST_SYNC_TIME_KEY, "Today at $timeStr").apply()
                    lastSyncTime.value = "Today at $timeStr"
                    syncStatusMessage.value = getApplication<Application>().getString(R.string.toast_sync_success)
                } else {
                    syncStatusMessage.value = getApplication<Application>().getString(R.string.toast_sync_auth_expired)
                    unlinkGoogleDrive()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                syncStatusMessage.value = getApplication<Application>().getString(R.string.toast_sync_error, e.localizedMessage)
            }
        }
    }

    fun restoreSyncCloud() {
        val token = driveAccessToken.value
        if (token.isEmpty()) {
            syncStatusMessage.value = getApplication<Application>().getString(R.string.toast_drive_auth_first)
            return
        }

        viewModelScope.launch {
            syncStatusMessage.value = getApplication<Application>().getString(R.string.toast_searching_backup)
            try {
                val fileId = GoogleDriveSyncService.searchBackupFile(token)
                if (fileId == null) {
                    syncStatusMessage.value = getApplication<Application>().getString(R.string.toast_no_backup_found)
                    return@launch
                }

                val backupContent = GoogleDriveSyncService.downloadBackupFile(token, fileId)
                if (backupContent.isNullOrEmpty()) {
                    syncStatusMessage.value = getApplication<Application>().getString(R.string.toast_backup_download_failed)
                    return@launch
                }

                val container = JSONObject(backupContent)
                val isBackupEncrypted = container.optBoolean("encrypted", false)
                
                val decryptedPayload: String
                if (isBackupEncrypted) {
                    val pass = masterPassword.value
                    if (pass.isNullOrEmpty()) {
                        syncStatusMessage.value = getApplication<Application>().getString(R.string.toast_unlock_first)
                        return@launch
                    }
                    val salt = container.getString("salt")
                    val iv = container.getString("iv")
                    val cipherData = container.getString("data")
                    decryptedPayload = EncryptionUtils.decrypt(cipherData, pass, salt, iv)
                    if (decryptedPayload.isEmpty()) {
                        syncStatusMessage.value = getApplication<Application>().getString(R.string.toast_decrypt_failed)
                        return@launch
                    }
                } else {
                    decryptedPayload = container.getString("data")
                }

                // Parse decrypted backup payload and save to Room
                val payloadObj = JSONObject(decryptedPayload)
                val notesArr = payloadObj.getJSONArray("notes")
                for (i in 0 until notesArr.length()) {
                    val noteObj = notesArr.getJSONObject(i)
                    val note = Note(
                        id = noteObj.optInt("id", 0),
                        title = noteObj.getString("title"),
                        content = noteObj.getString("content"),
                        isEncrypted = noteObj.getBoolean("isEncrypted"),
                        salt = noteObj.optString("salt", ""),
                        iv = noteObj.optString("iv", ""),
                        lastModified = noteObj.getLong("lastModified"),
                        tagsJson = noteObj.getString("tagsJson")
                    )
                    repository.insertNote(note)
                }

                val tagsArr = payloadObj.getJSONArray("tags")
                for (i in 0 until tagsArr.length()) {
                    val tagObj = tagsArr.getJSONObject(i)
                    val tag = Tag(
                        name = tagObj.getString("name"),
                        colorHex = tagObj.getString("colorHex")
                    )
                    repository.insertTag(tag)
                }

                val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                val timeStr = formatter.format(Date())
                sharedPrefs.edit().putString(AppConstants.LAST_SYNC_TIME_KEY, "Today at $timeStr").apply()
                lastSyncTime.value = "Today at $timeStr"
                syncStatusMessage.value = getApplication<Application>().getString(R.string.toast_restore_success)

            } catch (e: Exception) {
                e.printStackTrace()
                syncStatusMessage.value = getApplication<Application>().getString(R.string.toast_restore_error, e.localizedMessage)
            }
        }
    }

    fun clearStatusMessage() {
        syncStatusMessage.value = null
    }
}
