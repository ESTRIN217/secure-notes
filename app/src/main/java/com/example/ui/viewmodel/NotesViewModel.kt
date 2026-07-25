package com.example.ui.viewmodel

import android.accounts.Account
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.local.NoteDatabase
import com.example.data.local.TagDao
import com.example.data.model.Note
import com.example.data.model.Tag
import com.example.data.model.parseTags
import com.example.data.model.toJson
import com.example.data.model.sectionFilters

import com.example.data.local.NoteDao
import com.example.data.security.CipherService
import com.example.data.security.EncryptionServiceImpl
import com.example.data.security.KeyDerivation
import com.example.data.sync.CloudSyncManager
import com.example.data.sync.SyncService
import com.example.data.sync.SyncWorker
import com.google.android.gms.auth.GoogleAuthUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
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
import java.util.concurrent.TimeUnit
import com.example.AppConstants
import com.example.PasswordType
import com.example.util.BiometricAuthManager
import com.example.util.BackupAttachmentHelper

import com.example.R
import com.example.data.model.DecryptedNote
import com.example.data.model.NavigationSection
import com.example.data.model.SyncStage
import com.example.data.model.SyncState

class NotesViewModel(
    application: Application,
    private val noteDatabase: NoteDatabase,
    private val cipherService: CipherService = EncryptionServiceImpl(),
    private val syncService: SyncService = com.example.data.sync.GoogleDriveSyncService()
) : AndroidViewModel(application), CloudSyncManager {
    private val noteDao: NoteDao = noteDatabase.noteDao
    private val tagDao: TagDao = noteDatabase.tagDao
    private val sharedPrefs = application.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
    private var pendingRestoreContainer: JSONObject? = null
    private var pendingAttachmentRestoreDir: java.io.File? = null
    private val encryptedPrefs = run {
        val masterKey = MasterKey.Builder(application)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            application,
            "secure_notes_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun decryptNote(note: Note, password: String?): DecryptedNote {
        if (!note.isEncrypted) return DecryptedNote(note, note.title, note.content, true)
        val pass = password ?: ""
        if (pass.isEmpty()) return DecryptedNote(note, "[Encrypted]", "[Unlock to read notes]", false)
        val decTitle = cipherService.decrypt(note.title, pass, note.salt, note.iv).getOrDefault("")
        val decContent = cipherService.decrypt(note.content, pass, note.salt, note.iv).getOrDefault("")
        return if (decTitle.isEmpty() && decContent.isEmpty()) {
            DecryptedNote(note, "[Corrupted / Wrong Password]", "[Cannot decrypt]", false)
        } else {
            DecryptedNote(note, decTitle, decContent, true)
        }
    }

    // Auto update check
    val autoUpdateCheck = MutableStateFlow(sharedPrefs.getBoolean(AppConstants.AUTO_UPDATE_CHECK_KEY, true))

    // Loading state for initial DB load
    val isLoading = MutableStateFlow(true)

    // Password credentials state
    private val hasPasswordInPrefs: Boolean
        get() = encryptedPrefs.contains(AppConstants.MASTER_PASSWORD_HASH_KEY) || sharedPrefs.contains(AppConstants.MASTER_PASSWORD_HASH_KEY)

    override val isPasswordSet = MutableStateFlow(hasPasswordInPrefs)
    val isUnlocked = MutableStateFlow(!hasPasswordInPrefs)
    private val masterPassword = MutableStateFlow<String?>(null)
    val masterPasswordState: StateFlow<String?> = masterPassword.asStateFlow()
    val passwordType = MutableStateFlow(
        try { PasswordType.valueOf(sharedPrefs.getString(AppConstants.PASSWORD_TYPE_KEY, PasswordType.PASSWORD.name) ?: PasswordType.PASSWORD.name) }
        catch (e: Exception) { PasswordType.PASSWORD }
    )
    val isBiometricEnabled = MutableStateFlow(sharedPrefs.getBoolean(AppConstants.BIOMETRIC_ENABLED_KEY, false))
    private val biometricAuthManager = BiometricAuthManager(getApplication())
    override val includeAttachments = MutableStateFlow(sharedPrefs.getBoolean(AppConstants.INCLUDE_ATTACHMENTS_KEY, false))
    override val copyAttachmentsLocal = MutableStateFlow(sharedPrefs.getBoolean(AppConstants.COPY_ATTACHMENTS_LOCAL_KEY, false))

    override val encryptBackups = MutableStateFlow(sharedPrefs.getBoolean(AppConstants.ENCRYPT_BACKUPS_KEY, hasPasswordInPrefs))

    // Screenshot / Recents (default = blocked)
    val screenshotEnabled = MutableStateFlow(sharedPrefs.getBoolean(AppConstants.SCREENSHOT_ENABLED_KEY, false))

    // Auto-lock
    val autoLockTimeout = MutableStateFlow(
        sharedPrefs.getLong(AppConstants.AUTO_LOCK_TIMEOUT_KEY, AppConstants.AUTO_LOCK_TIMEOUT_DEFAULT)
    )
    private var autoLockJob: Job? = null

    // Rate limiting
    private var failedAttempts = 0
    private var lastFailedAttemptTime = 0L
    val isRateLimited = MutableStateFlow(false)
    val rateLimitRemainingSeconds = MutableStateFlow(0L)
    override val autoBackupEnabled = MutableStateFlow(sharedPrefs.getBoolean(AppConstants.AUTO_BACKUP_ENABLED_KEY, false))
    override val autoBackupInterval = MutableStateFlow(sharedPrefs.getString(AppConstants.AUTO_BACKUP_INTERVAL_KEY, "6h") ?: "6h")
    override val lastBackupSizeCloud = MutableStateFlow(sharedPrefs.getLong(AppConstants.LAST_BACKUP_SIZE_CLOUD_KEY, 0L))
    override val lastBackupSizeLocal = MutableStateFlow(sharedPrefs.getLong(AppConstants.LAST_BACKUP_SIZE_LOCAL_KEY, 0L))

    // Navigation and Filtering state
    val currentSection = MutableStateFlow(NavigationSection.HOME)

    // Search and Tags state
    val searchQuery = MutableStateFlow("")
    val selectedTagFilter = MutableStateFlow<String?>(null)

    // Google Drive state
    override val syncState = MutableStateFlow(
        SyncState(
            isDriveLinked = sharedPrefs.getBoolean(AppConstants.DRIVE_LINKED_KEY, false),
            lastSyncTime = sharedPrefs.getString(AppConstants.LAST_SYNC_TIME_KEY, getApplication<Application>().getString(R.string.label_never)) ?: getApplication<Application>().getString(R.string.label_never)
        )
    )
    val driveAccessToken = MutableStateFlow(encryptedPrefs.getString(AppConstants.DRIVE_ACCESS_TOKEN_KEY, "") ?: "")
    override val driveAccountEmail = MutableStateFlow(encryptedPrefs.getString(AppConstants.DRIVE_ACCOUNT_EMAIL_KEY, null))
    override val driveProfilePictureUri = MutableStateFlow(encryptedPrefs.getString(AppConstants.DRIVE_PROFILE_PICTURE_KEY, null))

    // Data lists from Room
    override val availableTags: StateFlow<List<Tag>>
    override val rawNotes: StateFlow<List<Note>>

    // Main UI Decrypted Notes state
    val notesList: StateFlow<List<DecryptedNote>>
    val searchResults: StateFlow<List<DecryptedNote>>

    init {
        migrateToEncryptedPrefs()
        scheduleAutoLock()

        availableTags = tagDao.getAllTagsFlow().stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            emptyList()
        )

        rawNotes = noteDao.getAllNotesFlow().stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            emptyList()
        )

        // Mark loading done after first Room emission
        viewModelScope.launch {
            noteDao.getAllNotesFlow().first()
            isLoading.value = false
        }

        // Clean up old trashed notes on startup
        viewModelScope.launch(Dispatchers.IO) {
            val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(AppConstants.TRASH_RETENTION_DAYS)
            noteDao.deleteOldTrashedNotes(cutoff)

            if (getApplication<Application>().getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
                    .getBoolean(AppConstants.AUTO_CLEANUP_ENABLED_KEY, false)) {
                kotlinx.coroutines.withContext(Dispatchers.IO) {
                    val (_, items) = com.example.data.storage.StorageAnalyzer.scan(getApplication(), noteDatabase)
                    val orphans = items.filter { it.isOrphan }
                    if (orphans.isNotEmpty()) {
                        com.example.data.storage.StorageAnalyzer.deleteFiles(orphans)
                    }
                }
            }
        }

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
                val sectionFilter = sectionFilters[section] ?: return@filter false
                if (!sectionFilter.matches(note)) return@filter false

                val matchesQuery = query.isEmpty() ||
                        decryptedNote.title.contains(query, ignoreCase = true) ||
                        decryptedNote.content.contains(query, ignoreCase = true)

                val matchesTag = tag == null || decryptedNote.note.parseTags().contains(tag)

                matchesQuery && matchesTag
            }
        }.flowOn(Dispatchers.IO).stateIn(
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
        }.flowOn(Dispatchers.IO).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        // Seed default tags if empty
        viewModelScope.launch {
            tagDao.getAllTagsFlow().collect { tags ->
                if (tags.isEmpty()) {
                    tagDao.insertTag(Tag(getApplication<Application>().getString(R.string.tag_default_work), "#42A5F5"))
                    tagDao.insertTag(Tag(getApplication<Application>().getString(R.string.tag_default_personal), "#66BB6A"))
                    tagDao.insertTag(Tag(getApplication<Application>().getString(R.string.tag_default_private), "#EC407A"))
                }
            }
        }

        // Seed default notes if empty
        viewModelScope.launch {
            try {
                val notes = noteDao.getAllNotesFlow().first()
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
                    noteDao.insertNote(welcomeNote)

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
                    noteDao.insertNote(workoutNote)
                }
            } catch (e: Exception) {
                Log.e("NotesViewModel", "operation failed", e)
            }
        }
    }

    private fun setBooleanPref(flow: MutableStateFlow<Boolean>, key: String, value: Boolean) {
        flow.value = value
        sharedPrefs.edit().putBoolean(key, value).apply()
    }

    private fun setStringPref(flow: MutableStateFlow<String>, key: String, value: String) {
        flow.value = value
        sharedPrefs.edit().putString(key, value).apply()
    }

    private fun setLongPref(flow: MutableStateFlow<Long>, key: String, value: Long) {
        flow.value = value
        sharedPrefs.edit().putLong(key, value).apply()
    }

    // Signal emitted when a note is modified from a sub-screen (DrawingCanvas, etc.)
    // NoteEditorScreen observes this to reload its state with fresh data.
    private val _noteExternallyUpdated = MutableSharedFlow<Int>(extraBufferCapacity = 8)
    val noteExternallyUpdated: SharedFlow<Int> = _noteExternallyUpdated.asSharedFlow()

    fun notifyNoteExternallyUpdated(noteId: Int) {
        _noteExternallyUpdated.tryEmit(noteId)
    }

    fun setAutoUpdateCheck(enabled: Boolean) {
        setBooleanPref(autoUpdateCheck, AppConstants.AUTO_UPDATE_CHECK_KEY, enabled)
    }

    fun setMasterPassword(password: String) {
        val salt = cipherService.generateSalt()
        val iv = cipherService.generateIv()
        val validationResult = cipherService.encrypt("VALID", password, salt, iv)
        if (validationResult.isFailure) {
            Log.e("NotesViewModel", "setMasterPassword: encryption failed", validationResult.exceptionOrNull())
            return
        }
        val validationHash = validationResult.getOrDefault("")

        encryptedPrefs.edit()
            .putString(AppConstants.MASTER_PASSWORD_HASH_KEY, validationHash)
            .putString(AppConstants.MASTER_PASSWORD_SALT_KEY, salt)
            .putString(AppConstants.MASTER_PASSWORD_IV_KEY, iv)
            .apply()

        masterPassword.value = password
        isPasswordSet.value = true
        isUnlocked.value = true
        if (encryptBackups.value) cacheMasterPassword()
        setBiometricEnabled(false)
    }

    fun unlockApp(password: String): Boolean {
        if (isRateLimited.value) return false

        val hash = encryptedPrefs.getString(AppConstants.MASTER_PASSWORD_HASH_KEY, "") ?: ""
        val salt = encryptedPrefs.getString(AppConstants.MASTER_PASSWORD_SALT_KEY, "") ?: ""
        val iv = encryptedPrefs.getString(AppConstants.MASTER_PASSWORD_IV_KEY, "") ?: ""

        val result = cipherService.decrypt(hash, password, salt, iv).getOrDefault("")
        if (result == "VALID") {
            failedAttempts = 0
            isRateLimited.value = false
            rateLimitRemainingSeconds.value = 0L
            masterPassword.value = password
            isUnlocked.value = true
            resetAutoLockTimer()
            if (encryptBackups.value) cacheMasterPassword()
            return true
        }
        failedAttempts++
        lastFailedAttemptTime = System.currentTimeMillis()
        applyRateLimit()
        return false
    }

    fun lockApp() {
        if (isPasswordSet.value) {
            masterPassword.value = null
            isUnlocked.value = false
            clearCachedPassword()
            KeyDerivation.clearCache()
            autoLockJob?.cancel()
            autoLockJob = null
        }
    }

    private fun applyRateLimit() {
        val delays = listOf(1L, 2L, 4L, 8L, 15L, 30L)
        val index = (failedAttempts - 1).coerceAtMost(delays.size - 1)
        val delaySeconds = delays[index]
        isRateLimited.value = true
        rateLimitRemainingSeconds.value = delaySeconds
        viewModelScope.launch {
            for (remaining in delaySeconds downTo 0) {
                rateLimitRemainingSeconds.value = remaining
                delay(1000L)
            }
            isRateLimited.value = false
        }
    }

    fun deletePassword() {
        encryptedPrefs.edit()
            .remove(AppConstants.MASTER_PASSWORD_HASH_KEY)
            .remove(AppConstants.MASTER_PASSWORD_SALT_KEY)
            .remove(AppConstants.MASTER_PASSWORD_IV_KEY)
            .apply()
        setBiometricEnabled(false)
        
        // Convert all currently encrypted notes back to plain text if unlocked,
        // or just clean up master pass
        viewModelScope.launch(Dispatchers.IO) {
            val password = masterPassword.value ?: ""
            if (password.isNotEmpty()) {
                rawNotes.value.forEach { note ->
                    if (note.isEncrypted) {
                        val decTitle = cipherService.decrypt(note.title, password, note.salt, note.iv).getOrDefault("")
                        val decContent = cipherService.decrypt(note.content, password, note.salt, note.iv).getOrDefault("")
                        if (decTitle.isNotEmpty() || decContent.isNotEmpty()) {
                            noteDao.updateNote(
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
            clearCachedPassword()
        }
    }

    // ── Migration from insecure SharedPrefs to EncryptedSharedPrefs ──
    private fun migrateToEncryptedPrefs() {
        if (sharedPrefs.getBoolean(AppConstants.MIGRATED_ENCRYPTED_PREFS, false)) return

        val keys = listOf(
            AppConstants.MASTER_PASSWORD_HASH_KEY,
            AppConstants.MASTER_PASSWORD_SALT_KEY,
            AppConstants.MASTER_PASSWORD_IV_KEY,
            AppConstants.BIOMETRIC_ENCRYPTED_PASSWORD_KEY,
            AppConstants.BIOMETRIC_IV_KEY
        )
        var migrated = false
        keys.forEach { key ->
            sharedPrefs.getString(key, null)?.let { value ->
                encryptedPrefs.edit().putString(key, value).apply()
                sharedPrefs.edit().remove(key).apply()
                migrated = true
            }
        }
        if (migrated) {
            sharedPrefs.edit().putBoolean(AppConstants.MIGRATED_ENCRYPTED_PREFS, true).apply()
            // Refresh state that depends on password presence
            isPasswordSet.value = hasPasswordInPrefs
            isUnlocked.value = !hasPasswordInPrefs
        }
    }

    // ── Auto-lock ──
    fun setAutoLockTimeout(minutes: Long) {
        setLongPref(autoLockTimeout, AppConstants.AUTO_LOCK_TIMEOUT_KEY, minutes)
        if (isUnlocked.value) resetAutoLockTimer()
    }

    fun resetAutoLockTimer() {
        autoLockJob?.cancel()
        val timeoutMinutes = autoLockTimeout.value
        if (timeoutMinutes < 0 || !isPasswordSet.value) return
        if (timeoutMinutes == 0L) return
        autoLockJob = viewModelScope.launch {
            delay(timeoutMinutes * 60 * 1000L)
            if (isUnlocked.value && isPasswordSet.value) {
                lockApp()
            }
        }
    }

    private fun scheduleAutoLock() {
        if (isUnlocked.value) resetAutoLockTimer()
    }

    fun onAppBackgrounded() {
        if (autoLockTimeout.value == -1L && isPasswordSet.value && isUnlocked.value) {
            lockApp()
        }
    }

    fun setPasswordType(type: PasswordType) {
        passwordType.value = type
        sharedPrefs.edit().putString(AppConstants.PASSWORD_TYPE_KEY, type.name).apply()
    }

    fun setScreenshotEnabled(enabled: Boolean) {
        setBooleanPref(screenshotEnabled, AppConstants.SCREENSHOT_ENABLED_KEY, enabled)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        setBooleanPref(isBiometricEnabled, AppConstants.BIOMETRIC_ENABLED_KEY, enabled)
        if (!enabled) {
            biometricAuthManager.deleteKey(AppConstants.BIOMETRIC_KEY_ALIAS)
            encryptedPrefs.edit()
                .remove(AppConstants.BIOMETRIC_ENCRYPTED_PASSWORD_KEY)
                .remove(AppConstants.BIOMETRIC_IV_KEY)
                .apply()
        }
    }

    override fun setIncludeAttachments(enabled: Boolean) {
        setBooleanPref(includeAttachments, AppConstants.INCLUDE_ATTACHMENTS_KEY, enabled)
    }

    override fun setCopyAttachmentsLocal(enabled: Boolean) {
        setBooleanPref(copyAttachmentsLocal, AppConstants.COPY_ATTACHMENTS_LOCAL_KEY, enabled)
    }

    override fun setEncryptBackups(enabled: Boolean) {
        setBooleanPref(encryptBackups, AppConstants.ENCRYPT_BACKUPS_KEY, enabled)
        if (enabled) cacheMasterPassword() else clearCachedPassword()
    }

    override fun setAutoBackupEnabled(enabled: Boolean) {
        setBooleanPref(autoBackupEnabled, AppConstants.AUTO_BACKUP_ENABLED_KEY, enabled)
        if (enabled && syncState.value.isDriveLinked) schedulePeriodicSync() else cancelPeriodicSync()
    }

    override fun setAutoBackupInterval(interval: String) {
        setStringPref(autoBackupInterval, AppConstants.AUTO_BACKUP_INTERVAL_KEY, interval)
        if (autoBackupEnabled.value && syncState.value.isDriveLinked) {
            cancelPeriodicSync()
            schedulePeriodicSync()
        }
    }

    override fun clearCachedPassword() {
        encryptedPrefs.edit().remove(AppConstants.CACHED_MASTER_PASSWORD_KEY).apply()
    }

    private fun cacheMasterPassword() {
        val pass = masterPassword.value
        if (!pass.isNullOrEmpty()) {
            encryptedPrefs.edit().putString(AppConstants.CACHED_MASTER_PASSWORD_KEY, pass).apply()
        }
    }

    fun saveBiometricEncryptedPassword(cipher: javax.crypto.Cipher) {
        val password = masterPassword.value ?: return
        try {
            val encrypted = cipher.doFinal(password.toByteArray(Charsets.UTF_8))
            val iv = cipher.iv
            encryptedPrefs.edit()
                .putString(AppConstants.BIOMETRIC_ENCRYPTED_PASSWORD_KEY, android.util.Base64.encodeToString(encrypted, android.util.Base64.NO_WRAP))
                .putString(AppConstants.BIOMETRIC_IV_KEY, android.util.Base64.encodeToString(iv, android.util.Base64.NO_WRAP))
                .apply()
        } catch (e: Exception) {
            android.util.Log.e("NotesViewModel", "saveBiometricEncryptedPassword failed", e)
        }
    }

    fun unlockWithBiometricCipher(cipher: javax.crypto.Cipher): Boolean {
        return try {
            val encB64 = encryptedPrefs.getString(AppConstants.BIOMETRIC_ENCRYPTED_PASSWORD_KEY, "") ?: ""
            val ivB64 = encryptedPrefs.getString(AppConstants.BIOMETRIC_IV_KEY, "") ?: ""
            if (encB64.isEmpty() || ivB64.isEmpty()) return false
            val encrypted = android.util.Base64.decode(encB64, android.util.Base64.NO_WRAP)
            val decrypted = cipher.doFinal(encrypted)
            val password = String(decrypted, Charsets.UTF_8)
            unlockApp(password)
        } catch (e: Exception) {
            android.util.Log.e("NotesViewModel", "unlockWithBiometricCipher failed", e)
            false
        }
    }

    fun getBiometricIv(): String {
        return encryptedPrefs.getString(AppConstants.BIOMETRIC_IV_KEY, "") ?: ""
    }

    fun changePassword(oldPassword: String, newPassword: String): Boolean {
        if (!unlockApp(oldPassword)) return false
        viewModelScope.launch(Dispatchers.IO) {
            try {
                rawNotes.value.forEach { note ->
                    if (note.isEncrypted) {
                        val decTitle = cipherService.decrypt(note.title, oldPassword, note.salt, note.iv)
                            .getOrDefault("")
                        val decContent = cipherService.decrypt(note.content, oldPassword, note.salt, note.iv)
                            .getOrDefault("")
                        if (decTitle.isEmpty() && decContent.isEmpty()) return@launch
                        val newSalt = cipherService.generateSalt()
                        val newIv = cipherService.generateIv()
                        cipherService.encrypt(decTitle, newPassword, newSalt, newIv)
                            .getOrDefault("")
                            .takeIf { it.isNotEmpty() }
                            ?.let { encTitle ->
                                cipherService.encrypt(decContent, newPassword, newSalt, newIv)
                                    .getOrDefault("")
                                    .takeIf { it.isNotEmpty() }
                                    ?.let { encContent ->
                                        noteDao.updateNote(note.copy(
                                            title = encTitle,
                                            content = encContent,
                                            salt = newSalt,
                                            iv = newIv
                                        ))
                                    }
                            }
                    }
                }
                val salt = cipherService.generateSalt()
                val iv = cipherService.generateIv()
                cipherService.encrypt("VALID", newPassword, salt, iv)
                    .getOrDefault("")
                    .takeIf { it.isNotEmpty() }
                    ?.let { validationHash ->
                        encryptedPrefs.edit()
                            .putString(AppConstants.MASTER_PASSWORD_HASH_KEY, validationHash)
                            .putString(AppConstants.MASTER_PASSWORD_SALT_KEY, salt)
                            .putString(AppConstants.MASTER_PASSWORD_IV_KEY, iv)
                            .apply()
                    }
                masterPassword.value = newPassword
                if (isBiometricEnabled.value && biometricAuthManager.hasKey(AppConstants.BIOMETRIC_KEY_ALIAS)) {
                    val cipher = biometricAuthManager.getEncryptCipher(AppConstants.BIOMETRIC_KEY_ALIAS)
                    saveBiometricEncryptedPassword(cipher)
                }
            } catch (e: Exception) {
                android.util.Log.e("NotesViewModel", "changePassword failed", e)
            }
        }
        return true
    }

    override fun saveNote(
        id: Int,
        title: String,
        content: String,
        isEncrypted: Boolean,
        tagsList: List<String>,
        backgroundColor: Int?,
        backgroundImagePath: String?,
        isPinned: Boolean,
        isFavorite: Boolean,
        isArchived: Boolean,
        categoryId: String?,
        isDeleted: Boolean,
        deletedAt: Long,
        lastModified: Long,
        salt: String,
        iv: String
    ) {
        viewModelScope.launch {
            saveNoteAndGetId(id, title, content, isEncrypted, tagsList, backgroundColor, backgroundImagePath, isPinned, isFavorite, isArchived, categoryId, isDeleted, deletedAt, lastModified, salt, iv)
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
        isArchived: Boolean = false,
        categoryId: String? = null,
        isDeleted: Boolean = false,
        deletedAt: Long = 0,
        lastModified: Long = System.currentTimeMillis(),
        salt: String = "",
        iv: String = ""
    ): Int = withContext(Dispatchers.IO) {
        try {
            // Copy content:// URIs to local storage if setting is enabled
            val finalContent = if (copyAttachmentsLocal.value) {
                val context = getApplication<Application>().applicationContext
                val nextId = if (id != 0) id else (rawNotes.value.maxOfOrNull { it.id } ?: 0) + 1
                var updatedContent = content
                var updatedBgPath = backgroundImagePath
                if (updatedContent.contains("content://")) {
                    updatedContent = com.example.util.BackupAttachmentHelper.copyUrisToLocalStorage(updatedContent, nextId, context)
                }
                if (updatedBgPath?.startsWith("content://") == true) {
                    val bgMap = com.example.util.BackupAttachmentHelper.collectAndCopyAttachments("", updatedBgPath, context, java.io.File(context.cacheDir, "tmp_attachments").also { it.mkdirs() })
                    bgMap[updatedBgPath]?.let { relPath ->
                        updatedBgPath = java.io.File(context.filesDir, relPath).absolutePath
                    }
                }
                updatedContent
            } else {
                content
            }

            val effectiveSalt = if (salt.isNotEmpty()) salt else if (isEncrypted) cipherService.generateSalt() else ""
            val effectiveIv = if (iv.isNotEmpty()) iv else if (isEncrypted) cipherService.generateIv() else ""

            val pass = if (isEncrypted) masterPassword.value ?: "" else ""
            val storedTitle = if (isEncrypted) cipherService.encrypt(title, pass, effectiveSalt, effectiveIv).getOrDefault("") else title
            val storedContent = if (isEncrypted) cipherService.encrypt(finalContent, pass, effectiveSalt, effectiveIv).getOrDefault("") else finalContent

            val tagsJson = JSONArray(tagsList).toString()

            val existing = if (id != 0) {
                noteDao.getAllNotesFlow().first().find { it.id == id }
            } else null

            val note = Note(
                id = if (id != 0) id else 0,
                title = storedTitle,
                content = storedContent,
                isEncrypted = isEncrypted,
                salt = effectiveSalt,
                iv = effectiveIv,
                tagsJson = tagsJson,
                lastModified = lastModified,
                isArchived = isArchived,
                isFavorite = isFavorite,
                isPinned = isPinned,
                isDeleted = if (id != 0) existing?.isDeleted ?: isDeleted else isDeleted,
                deletedAt = if (id != 0) (existing?.deletedAt ?: deletedAt) else deletedAt,
                backgroundColor = backgroundColor,
                backgroundImagePath = backgroundImagePath ?: existing?.backgroundImagePath,
                categoryId = categoryId ?: existing?.categoryId
            )

            val result = if (id == 0) {
                noteDao.insertNote(note).toInt()
            } else {
                noteDao.updateNote(note)
                id
            }
            result
        } catch (e: Exception) {
            Log.e("NotesViewModel", "saveNoteAndGetId failed", e)
            id
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            noteDao.deleteNote(note)
        }
    }

    fun toggleFavorite(note: Note) {
        viewModelScope.launch {
            noteDao.updateNote(note.copy(isFavorite = !note.isFavorite, lastModified = System.currentTimeMillis()))
        }
    }

    fun batchTogglePin(noteIds: Set<Int>) {
        viewModelScope.launch {
            try {
                val allNotes = noteDao.getAllNotesFlow().first()
                val selected = allNotes.filter { it.id in noteIds }
                val hasUnpinned = selected.any { !it.isPinned }
                selected.forEach { note ->
                    noteDao.updateNote(note.copy(isPinned = hasUnpinned, lastModified = System.currentTimeMillis()))
                }
            } catch (e: Exception) {
                Log.e("NotesViewModel", "operation failed", e)
            }
        }
    }

    fun batchToggleFavorite(noteIds: Set<Int>) {
        viewModelScope.launch {
            try {
                val allNotes = noteDao.getAllNotesFlow().first()
                val selected = allNotes.filter { it.id in noteIds }
                val hasUnfav = selected.any { !it.isFavorite }
                selected.forEach { note ->
                    noteDao.updateNote(note.copy(isFavorite = hasUnfav, lastModified = System.currentTimeMillis()))
                }
            } catch (e: Exception) {
                Log.e("NotesViewModel", "operation failed", e)
            }
        }
    }

    fun batchToggleArchive(noteIds: Set<Int>) {
        viewModelScope.launch {
            try {
                val allNotes = noteDao.getAllNotesFlow().first()
                val selected = allNotes.filter { it.id in noteIds }
                val hasUnarchived = selected.any { !it.isArchived }
                selected.forEach { note ->
                    noteDao.updateNote(note.copy(isArchived = hasUnarchived, lastModified = System.currentTimeMillis()))
                }
            } catch (e: Exception) {
                Log.e("NotesViewModel", "operation failed", e)
            }
        }
    }

    fun batchUpdateTags(noteIds: Set<Int>, tagNames: List<String>) {
        viewModelScope.launch {
            try {
                val allNotes = noteDao.getAllNotesFlow().first()
                val selected = allNotes.filter { it.id in noteIds }
                val allAvailTags = availableTags.value.map { it.name }
                selected.forEach { note ->
                    val existingTags = mutableListOf<String>()
                    existingTags.addAll(note.parseTags())

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
                    noteDao.updateNote(note.copy(tagsJson = updatedTagsJson, lastModified = System.currentTimeMillis()))
                }
            } catch (e: Exception) {
                Log.e("NotesViewModel", "operation failed", e)
            }
        }
    }

    fun toggleArchive(note: Note) {
        viewModelScope.launch {
            noteDao.updateNote(note.copy(isArchived = !note.isArchived, lastModified = System.currentTimeMillis()))
        }
    }

    fun moveToTrash(note: Note) {
        viewModelScope.launch {
            noteDao.updateNote(note.copy(isDeleted = true, deletedAt = System.currentTimeMillis(), lastModified = System.currentTimeMillis()))
        }
    }

    fun restoreFromTrash(note: Note) {
        viewModelScope.launch {
            noteDao.updateNote(note.copy(isDeleted = false, lastModified = System.currentTimeMillis()))
        }
    }

    fun deletePermanently(note: Note) {
        viewModelScope.launch {
            noteDao.deleteNote(note)
        }
    }

    override fun createTag(name: String, colorHex: String) {
        viewModelScope.launch {
            val existing = tagDao.getAllTagsFlow().first()
            if (!existing.any { it.name == name }) {
                tagDao.insertTag(Tag(name, colorHex))
            }
        }
    }

    fun deleteTag(tag: Tag) {
        viewModelScope.launch {
            tagDao.deleteTag(tag)
            try {
                val allNotes = noteDao.getAllNotesFlow().first()
                allNotes.forEach { note ->
                    val filtered = note.parseTags().filter { it != tag.name }
                    if (filtered.size != note.parseTags().size) {
                        noteDao.updateNote(note.copy(tagsJson = JSONArray(filtered).toString(), lastModified = System.currentTimeMillis()))
                    }
                }
            } catch (e: Exception) {
                Log.e("NotesViewModel", "operation failed", e)
            }
            if (selectedTagFilter.value == tag.name) {
                selectedTagFilter.value = null
            }
        }
    }

    fun updateTag(oldTag: Tag, newName: String, newColorHex: String) {
        viewModelScope.launch {
            if (oldTag.name == newName) {
                tagDao.insertTag(Tag(newName, newColorHex))
            } else {
                tagDao.insertTag(Tag(newName, newColorHex))
                tagDao.deleteTag(oldTag)
                try {
                    val allNotes = noteDao.getAllNotesFlow().first()
                    allNotes.forEach { note ->
                        val updated = note.parseTags().map { if (it == oldTag.name) newName else it }
                        if (updated != note.parseTags()) {
                            noteDao.updateNote(note.copy(tagsJson = JSONArray(updated).toString(), lastModified = System.currentTimeMillis()))
                        }
                    }
                } catch (e: Exception) {
                    Log.e("NotesViewModel", "operation failed", e)
                }
                if (selectedTagFilter.value == oldTag.name) {
                    selectedTagFilter.value = newName
                }
            }
        }
    }

    // Periodic background sync
    private fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val hours = when (autoBackupInterval.value) {
            "6h" -> 6L
            "12h" -> 12L
            "24h" -> 24L
            "weekly" -> 168L
            else -> 6L
        }
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(hours, java.util.concurrent.TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(getApplication())
            .enqueueUniquePeriodicWork(
                "secure_notes_cloud_sync",
                ExistingPeriodicWorkPolicy.UPDATE,
                syncRequest
            )
    }

    private fun cancelPeriodicSync() {
        WorkManager.getInstance(getApplication())
            .cancelUniqueWork("secure_notes_cloud_sync")
    }

    // Google Drive Integration
    override fun linkGoogleDrive(token: String, accountEmail: String, pictureUri: String) {
        val uri = pictureUri.ifEmpty { "https://www.google.com/s2/photos/profile/$accountEmail" }
        encryptedPrefs.edit()
            .putString(AppConstants.DRIVE_ACCESS_TOKEN_KEY, token)
            .putString(AppConstants.DRIVE_ACCOUNT_EMAIL_KEY, accountEmail)
            .putString(AppConstants.DRIVE_PROFILE_PICTURE_KEY, uri)
            .apply()
        sharedPrefs.edit()
            .putBoolean(AppConstants.DRIVE_LINKED_KEY, true)
            .apply()
        
        syncState.update { it.copy(isDriveLinked = true, syncStatusMessage = getApplication<Application>().getString(R.string.toast_drive_connected)) }
        driveAccessToken.value = token
        driveAccountEmail.value = accountEmail
        driveProfilePictureUri.value = uri
        if (autoBackupEnabled.value) schedulePeriodicSync()
    }

    override fun unlinkGoogleDrive() {
        encryptedPrefs.edit()
            .remove(AppConstants.DRIVE_ACCESS_TOKEN_KEY)
            .remove(AppConstants.DRIVE_ACCOUNT_EMAIL_KEY)
            .remove(AppConstants.DRIVE_PROFILE_PICTURE_KEY)
            .apply()
        sharedPrefs.edit()
            .putBoolean(AppConstants.DRIVE_LINKED_KEY, false)
            .apply()
        
        syncState.update { it.copy(isDriveLinked = false, syncStatusMessage = getApplication<Application>().getString(R.string.toast_drive_disconnected)) }
        driveAccessToken.value = ""
        driveAccountEmail.value = null
        driveProfilePictureUri.value = null
        cancelPeriodicSync()
    }

    private suspend fun refreshAccessToken(): String? {
        val email = encryptedPrefs.getString(AppConstants.DRIVE_ACCOUNT_EMAIL_KEY, null) ?: return null
        return try {
            withContext(Dispatchers.IO) {
                GoogleAuthUtil.getToken(
                    getApplication<Application>(),
                    Account(email, "com.google"),
                    "oauth2:https://www.googleapis.com/auth/drive.appdata"
                )
            }
        } catch (e: Exception) {
            Log.e("NotesViewModel", "Token refresh failed", e)
            null
        }
    }

    private suspend fun performSyncWithToken(currentToken: String, finalPayload: String): Boolean {
        val existingFileId = syncService.searchBackupFile(currentToken).getOrNull()
        return if (existingFileId != null) {
            syncService.uploadFileContent(currentToken, existingFileId, finalPayload).getOrDefault(false)
        } else {
            val createdFileId = syncService.createBackupFile(currentToken, finalPayload).getOrNull()
            createdFileId != null
        }
    }

    private suspend fun performSyncWithTokenBytes(currentToken: String, data: ByteArray): Boolean {
        val existingFileId = syncService.searchBackupFile(currentToken).getOrNull()
        return if (existingFileId != null) {
            syncService.uploadFileBytes(currentToken, existingFileId, data).getOrDefault(false)
        } else {
            val createdFileId = syncService.createBackupFile(currentToken, "placeholder").getOrNull()
            if (createdFileId != null) {
                syncService.uploadFileBytes(currentToken, createdFileId, data).getOrDefault(false)
            } else false
        }
    }

    override fun forceSyncCloud() {
        var token = driveAccessToken.value
        if (token.isEmpty()) {
            syncState.update { it.copy(syncStatusMessage = getApplication<Application>().getString(R.string.toast_drive_auth_first)) }
            return
        }

        viewModelScope.launch {
            syncState.update { it.copy(syncStage = SyncStage.ENCRYPTING, syncStatusMessage = getApplication<Application>().getString(R.string.toast_syncing)) }
            try {
                // Refresh token if expired before starting
                val newToken = refreshAccessToken()
                if (newToken != null) {
                    token = newToken
                    driveAccessToken.value = newToken
                    encryptedPrefs.edit().putString(AppConstants.DRIVE_ACCESS_TOKEN_KEY, newToken).apply()
                }

                val notesArray = JSONArray()
                rawNotes.value.forEach { note -> notesArray.put(note.toJson()) }

                val tagsArray = JSONArray()
                availableTags.value.forEach { tag -> tagsArray.put(tag.toJson()) }

                val settingsObj = JSONObject().apply {
                    put(AppConstants.DARK_MODE_OPTION_KEY, sharedPrefs.getString(AppConstants.DARK_MODE_OPTION_KEY, "SYSTEM"))
                    put(AppConstants.DYNAMIC_COLORS_KEY, sharedPrefs.getBoolean(AppConstants.DYNAMIC_COLORS_KEY, true))
                    put(AppConstants.LANGUAGE_KEY, sharedPrefs.getString(AppConstants.LANGUAGE_KEY, "") ?: "")
                    put(AppConstants.AUTO_UPDATE_CHECK_KEY, sharedPrefs.getBoolean(AppConstants.AUTO_UPDATE_CHECK_KEY, true))
                    put(AppConstants.UPDATE_NOTIFICATIONS_KEY, sharedPrefs.getBoolean(AppConstants.UPDATE_NOTIFICATIONS_KEY, true))
                    put(AppConstants.CUSTOM_ORDER_KEY, sharedPrefs.getString(AppConstants.CUSTOM_ORDER_KEY, "") ?: "")
                    put(AppConstants.INCLUDE_ATTACHMENTS_KEY, sharedPrefs.getBoolean(AppConstants.INCLUDE_ATTACHMENTS_KEY, false))
                    put(AppConstants.COPY_ATTACHMENTS_LOCAL_KEY, sharedPrefs.getBoolean(AppConstants.COPY_ATTACHMENTS_LOCAL_KEY, false))
                    put(AppConstants.PASSWORD_TYPE_KEY, sharedPrefs.getString(AppConstants.PASSWORD_TYPE_KEY, com.example.PasswordType.PASSWORD.name))
                    put(AppConstants.BIOMETRIC_ENABLED_KEY, sharedPrefs.getBoolean(AppConstants.BIOMETRIC_ENABLED_KEY, false))
                    put(AppConstants.SCREENSHOT_ENABLED_KEY, sharedPrefs.getBoolean(AppConstants.SCREENSHOT_ENABLED_KEY, false))
                    put(AppConstants.AUTO_LOCK_TIMEOUT_KEY, sharedPrefs.getLong(AppConstants.AUTO_LOCK_TIMEOUT_KEY, AppConstants.AUTO_LOCK_TIMEOUT_DEFAULT))
                    put(AppConstants.ENCRYPT_BACKUPS_KEY, sharedPrefs.getBoolean(AppConstants.ENCRYPT_BACKUPS_KEY, false))
                    put(AppConstants.AUTO_BACKUP_ENABLED_KEY, sharedPrefs.getBoolean(AppConstants.AUTO_BACKUP_ENABLED_KEY, false))
                    put(AppConstants.AUTO_BACKUP_INTERVAL_KEY, sharedPrefs.getString(AppConstants.AUTO_BACKUP_INTERVAL_KEY, "6h") ?: "6h")
                }

                val syncPayload = JSONObject().apply {
                    put("version", 4)
                    put("notes", notesArray)
                    put("tags", tagsArray)
                    put("settings", settingsObj)
                    put("timestamp", System.currentTimeMillis())
                }.toString()

                // Collect attachments and rewrite paths BEFORE encryption/ZIP
                var allPathMaps: Map<String, String> = emptyMap()
                var attachmentTempDir: File? = null
                val finalSyncPayload: String

                if (includeAttachments.value) {
                    val app = getApplication<Application>()
                    val context = app.applicationContext
                    val tempDir = File(context.cacheDir, "backup_attachments_${System.currentTimeMillis()}")
                    tempDir.mkdirs()
                    val tempAttachmentsDir = File(tempDir, "attachments")
                    attachmentTempDir = tempDir

                    val collectedMaps = mutableMapOf<String, String>()
                    rawNotes.value.forEach { note ->
                        val pathMap = BackupAttachmentHelper.collectAndCopyAttachments(
                            note.content, note.backgroundImagePath, context, tempAttachmentsDir
                        )
                        collectedMaps.putAll(pathMap)
                    }
                    allPathMaps = collectedMaps

                    if (allPathMaps.isNotEmpty()) {
                        val payloadObj = JSONObject(syncPayload)
                        val notesArr = payloadObj.getJSONArray("notes")
                        for (i in 0 until notesArr.length()) {
                            val noteObj = notesArr.getJSONObject(i)
                            val content = noteObj.optString("content", "")
                            if (content.isNotEmpty()) {
                                noteObj.put("content", BackupAttachmentHelper.rewriteContentPaths(content, allPathMaps))
                            }
                            val bgPath = noteObj.optString("backgroundImagePath", "")
                            if (bgPath.isNotEmpty()) {
                                noteObj.put("backgroundImagePath", BackupAttachmentHelper.rewriteContentPaths(bgPath, allPathMaps))
                            }
                        }
                        finalSyncPayload = payloadObj.toString()
                    } else {
                        finalSyncPayload = syncPayload
                    }
                } else {
                    finalSyncPayload = syncPayload
                }

                val shouldEncrypt = encryptBackups.value && isPasswordSet.value && masterPassword.value != null
                val finalPayload: String = if (shouldEncrypt) {
                    val pass = masterPassword.value ?: return@launch
                    val salt = cipherService.generateSalt()
                    val iv = cipherService.generateIv()
                    val cipherPayload = cipherService.encrypt(finalSyncPayload, pass, salt, iv).getOrDefault("")
                    JSONObject().apply {
                        put("encrypted", true)
                        put("salt", salt)
                        put("iv", iv)
                        put("data", cipherPayload)
                    }.toString()
                } else {
                    JSONObject().apply {
                        put("encrypted", false)
                        put("data", finalSyncPayload)
                    }.toString()
                }

                var backupSize = 0L
                val success = if (includeAttachments.value && attachmentTempDir != null) {
                    syncState.update { it.copy(syncStage = SyncStage.UPLOADING) }
                    try {
                        val zipFile = File(attachmentTempDir, "backup.zip")
                        BackupAttachmentHelper.buildBackupZip(finalPayload, allPathMaps, File(attachmentTempDir, "attachments"), zipFile)
                        val zipBytes = zipFile.readBytes()
                        backupSize = zipFile.length()
                        performSyncWithTokenBytes(token, zipBytes)
                    } finally {
                        attachmentTempDir.deleteRecursively()
                    }
                } else {
                    syncState.update { it.copy(syncStage = SyncStage.UPLOADING) }
                    backupSize = finalPayload.toByteArray().size.toLong()
                    performSyncWithToken(token, finalPayload)
                }

                if (success) {
                    lastBackupSizeCloud.value = backupSize
                    sharedPrefs.edit().putLong(AppConstants.LAST_BACKUP_SIZE_CLOUD_KEY, backupSize).apply()

                    val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    val timeStr = formatter.format(Date())
                    sharedPrefs.edit().putString(AppConstants.LAST_SYNC_TIME_KEY, getApplication<Application>().getString(R.string.label_today_at, timeStr)).apply()
                    syncState.update { it.copy(syncStage = SyncStage.IDLE, lastSyncTime = getApplication<Application>().getString(R.string.label_today_at, timeStr), syncStatusMessage = getApplication<Application>().getString(R.string.toast_sync_success)) }
                } else {
                    syncState.update { it.copy(syncStage = SyncStage.IDLE, syncStatusMessage = getApplication<Application>().getString(R.string.toast_sync_auth_expired)) }
                    unlinkGoogleDrive()
                }
            } catch (e: Exception) {
                Log.e("NotesViewModel", "operation failed", e)
                syncState.update { it.copy(syncStage = SyncStage.IDLE, syncStatusMessage = getApplication<Application>().getString(R.string.toast_sync_error, e.localizedMessage)) }
            }
        }
    }

    override fun restoreSyncCloud() {
        var token = driveAccessToken.value
        if (token.isEmpty()) {
            syncState.update { it.copy(syncStatusMessage = getApplication<Application>().getString(R.string.toast_drive_auth_first)) }
            return
        }

        viewModelScope.launch {
            syncState.update { it.copy(syncStage = SyncStage.SEARCHING, syncStatusMessage = getApplication<Application>().getString(R.string.toast_searching_backup)) }
            try {
                var fileId = syncService.searchBackupFile(token).getOrNull()

                if (fileId == null) {
                    val newToken = refreshAccessToken()
                    if (newToken != null) {
                        token = newToken
                        driveAccessToken.value = newToken
                        encryptedPrefs.edit().putString(AppConstants.DRIVE_ACCESS_TOKEN_KEY, newToken).apply()
                        fileId = syncService.searchBackupFile(token).getOrNull()
                    }
                }

                if (fileId == null) {
                    syncState.update { it.copy(syncStage = SyncStage.IDLE, syncStatusMessage = getApplication<Application>().getString(R.string.toast_no_backup_found)) }
                    return@launch
                }

                syncState.update { it.copy(syncStage = SyncStage.DOWNLOADING) }

                // Try bytes first (ZIP format), fall back to string (JSON)
                var backupBytes = syncService.downloadFileBytes(token, fileId).getOrNull()
                if (backupBytes == null || backupBytes.isEmpty()) {
                    val newToken = refreshAccessToken()
                    if (newToken != null) {
                        token = newToken
                        driveAccessToken.value = newToken
                        encryptedPrefs.edit().putString(AppConstants.DRIVE_ACCESS_TOKEN_KEY, newToken).apply()
                        backupBytes = syncService.downloadFileBytes(token, fileId).getOrNull()
                    }
                }

                if (backupBytes == null || backupBytes.isEmpty()) {
                    syncState.update { it.copy(syncStage = SyncStage.IDLE, syncStatusMessage = getApplication<Application>().getString(R.string.toast_backup_download_failed)) }
                    return@launch
                }

                val app = getApplication<Application>()
                val context = app.applicationContext
                var backupContent: String
                var attachmentRestoreDir: File? = null

                if (BackupAttachmentHelper.isZipBytes(backupBytes)) {
                    val tempDir = File(context.cacheDir, "restore_attachments_${System.currentTimeMillis()}")
                    tempDir.mkdirs()
                    attachmentRestoreDir = File(tempDir, "attachments")
                    val json = BackupAttachmentHelper.extractBackupZip(backupBytes, tempDir, context)
                    if (json == null) {
                        syncState.update { it.copy(syncStage = SyncStage.IDLE, syncStatusMessage = getApplication<Application>().getString(R.string.toast_backup_download_failed)) }
                        tempDir.deleteRecursively()
                        return@launch
                    }
                    backupContent = json.toString()
                } else {
                    // Try as plain JSON string
                    backupContent = backupBytes.toString(Charsets.UTF_8)
                }

                val container = JSONObject(backupContent)
                val isBackupEncrypted = container.optBoolean("encrypted", false)

                val finalPayload: String
                if (isBackupEncrypted) {
                    val pass = masterPassword.value
                    if (pass.isNullOrEmpty()) {
                        pendingRestoreContainer = container
                        pendingAttachmentRestoreDir = attachmentRestoreDir
                        syncState.update { it.copy(syncStage = SyncStage.PASSWORD_REQUIRED, syncStatusMessage = getApplication<Application>().getString(R.string.restore_password_required)) }
                        return@launch
                    }
                    val decrypted = decryptRestoreContainer(container, pass)
                    if (decrypted == null) {
                        syncState.update { it.copy(syncStage = SyncStage.IDLE, syncStatusMessage = getApplication<Application>().getString(R.string.toast_decrypt_failed)) }
                        attachmentRestoreDir?.deleteRecursively()
                        return@launch
                    }
                    finalPayload = decrypted
                } else {
                    finalPayload = container.getString("data")
                }

                finalizeRestore(finalPayload)

                // Restore attachment files
                if (attachmentRestoreDir != null && attachmentRestoreDir.exists()) {
                    val restoreDir = File(context.filesDir, "restored_attachments")
                    restoreDir.mkdirs()
                    attachmentRestoreDir.copyRecursively(restoreDir, overwrite = true)

                    // Rewrite paths in restored notes
                    val allNotes = noteDao.getAllNotesFlow().first()
                    allNotes.forEach { note ->
                        val rewritten = BackupAttachmentHelper.rewriteRestoredPaths(note.content, restoreDir)
                        if (rewritten != note.content) {
                            val bgRewritten = note.backgroundImagePath?.let {
                                BackupAttachmentHelper.rewriteRestoredPaths(it, restoreDir)
                            }
                            noteDao.updateNote(note.copy(content = rewritten, backgroundImagePath = bgRewritten))
                        }
                    }
                    attachmentRestoreDir.deleteRecursively()
                }

            } catch (e: Exception) {
                Log.e("NotesViewModel", "operation failed", e)
                syncState.update { it.copy(syncStage = SyncStage.IDLE, syncStatusMessage = getApplication<Application>().getString(R.string.toast_restore_error, e.localizedMessage)) }
            }
        }
    }

    private fun decryptRestoreContainer(container: JSONObject, password: String): String? {
        return try {
            val salt = container.getString("salt")
            val iv = container.getString("iv")
            val cipherData = container.getString("data")
            val decrypted = cipherService.decrypt(cipherData, password, salt, iv).getOrDefault("")
            if (decrypted.isEmpty()) null else decrypted
        } catch (e: Exception) {
            Log.e("NotesViewModel", "Decrypt failed", e)
            null
        }
    }

    private suspend fun finalizeRestore(decryptedPayload: String) {
        syncState.update { it.copy(syncStage = SyncStage.RESTORING) }
        val payloadObj = JSONObject(decryptedPayload)

        val localNotes = noteDao.getAllNotesFlow().first().associateBy { it.id }
        val notesArr = payloadObj.getJSONArray("notes")
        for (i in 0 until notesArr.length()) {
            val noteObj = notesArr.getJSONObject(i)
            val backupId = noteObj.optInt("id", 0)
            val backupModified = noteObj.getLong("lastModified")
            val localNote = localNotes[backupId]

            if (localNote == null || backupModified > localNote.lastModified) {
                val note = Note(
                    id = backupId,
                    title = noteObj.getString("title"),
                    content = noteObj.getString("content"),
                    isEncrypted = noteObj.getBoolean("isEncrypted"),
                    salt = noteObj.optString("salt", ""),
                    iv = noteObj.optString("iv", ""),
                    lastModified = backupModified,
                    tagsJson = noteObj.getString("tagsJson"),
                    isArchived = noteObj.optBoolean("isArchived", false),
                    isFavorite = noteObj.optBoolean("isFavorite", false),
                    isPinned = noteObj.optBoolean("isPinned", false),
                    isDeleted = noteObj.optBoolean("isDeleted", false),
                    deletedAt = noteObj.optLong("deletedAt", 0),
                    backgroundColor = if (noteObj.has("backgroundColor") && !noteObj.isNull("backgroundColor")) noteObj.optInt("backgroundColor") else null,
                    backgroundImagePath = noteObj.optString("backgroundImagePath", "").ifEmpty { null },
                    categoryId = noteObj.optString("categoryId", "").ifEmpty { null }
                )
                noteDao.insertNote(note)
            }
        }

        val localTags = tagDao.getAllTagsFlow().first().associateBy { it.name }
        val tagsArr = payloadObj.getJSONArray("tags")
        for (i in 0 until tagsArr.length()) {
            val tagObj = tagsArr.getJSONObject(i)
            val tagName = tagObj.getString("name")
            if (!localTags.containsKey(tagName)) {
                val tag = Tag(
                    name = tagName,
                    colorHex = tagObj.getString("colorHex")
                )
                tagDao.insertTag(tag)
            }
        }

        if (payloadObj.has("settings")) {
            val settings = payloadObj.getJSONObject("settings")
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
            if (settings.has(AppConstants.PASSWORD_TYPE_KEY))
                editor.putString(AppConstants.PASSWORD_TYPE_KEY, settings.getString(AppConstants.PASSWORD_TYPE_KEY))
            if (settings.has(AppConstants.BIOMETRIC_ENABLED_KEY))
                editor.putBoolean(AppConstants.BIOMETRIC_ENABLED_KEY, settings.getBoolean(AppConstants.BIOMETRIC_ENABLED_KEY))
            if (settings.has(AppConstants.SCREENSHOT_ENABLED_KEY))
                editor.putBoolean(AppConstants.SCREENSHOT_ENABLED_KEY, settings.getBoolean(AppConstants.SCREENSHOT_ENABLED_KEY))
            if (settings.has(AppConstants.AUTO_LOCK_TIMEOUT_KEY))
                editor.putLong(AppConstants.AUTO_LOCK_TIMEOUT_KEY, settings.getLong(AppConstants.AUTO_LOCK_TIMEOUT_KEY))
            if (settings.has(AppConstants.ENCRYPT_BACKUPS_KEY))
                editor.putBoolean(AppConstants.ENCRYPT_BACKUPS_KEY, settings.getBoolean(AppConstants.ENCRYPT_BACKUPS_KEY))
            if (settings.has(AppConstants.AUTO_BACKUP_ENABLED_KEY))
                editor.putBoolean(AppConstants.AUTO_BACKUP_ENABLED_KEY, settings.getBoolean(AppConstants.AUTO_BACKUP_ENABLED_KEY))
            if (settings.has(AppConstants.AUTO_BACKUP_INTERVAL_KEY))
                editor.putString(AppConstants.AUTO_BACKUP_INTERVAL_KEY, settings.getString(AppConstants.AUTO_BACKUP_INTERVAL_KEY))
            editor.apply()
        }

        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val timeStr = formatter.format(Date())
        sharedPrefs.edit().putString(AppConstants.LAST_SYNC_TIME_KEY, getApplication<Application>().getString(R.string.label_today_at, timeStr)).apply()
        syncState.update { it.copy(syncStage = SyncStage.IDLE, lastSyncTime = getApplication<Application>().getString(R.string.label_today_at, timeStr), syncStatusMessage = getApplication<Application>().getString(R.string.toast_restore_success)) }
    }

    override fun provideRestorePassword(password: String) {
        val container = pendingRestoreContainer ?: return
        pendingRestoreContainer = null
        val attachmentDir = pendingAttachmentRestoreDir
        pendingAttachmentRestoreDir = null

        // If user has no master password yet, set it from the restore password
        if (!isPasswordSet.value && password.isNotEmpty()) {
            setMasterPassword(password)
        }

        viewModelScope.launch {
            try {
                val decrypted = decryptRestoreContainer(container, password)
                if (decrypted == null) {
                    syncState.update { it.copy(syncStage = SyncStage.IDLE, syncStatusMessage = getApplication<Application>().getString(R.string.toast_decrypt_failed)) }
                    attachmentDir?.deleteRecursively()
                    return@launch
                }
                finalizeRestore(decrypted)

                // Restore attachment files after password decryption
                if (attachmentDir != null && attachmentDir.exists()) {
                    val context = getApplication<Application>().applicationContext
                    val restoreDir = java.io.File(context.filesDir, "restored_attachments")
                    restoreDir.mkdirs()
                    attachmentDir.copyRecursively(restoreDir, overwrite = true)
                    val allNotes = noteDao.getAllNotesFlow().first()
                    allNotes.forEach { note ->
                        val rewritten = com.example.util.BackupAttachmentHelper.rewriteRestoredPaths(note.content, restoreDir)
                        if (rewritten != note.content) {
                            val bgRewritten = note.backgroundImagePath?.let {
                                com.example.util.BackupAttachmentHelper.rewriteRestoredPaths(it, restoreDir)
                            }
                            noteDao.updateNote(note.copy(content = rewritten, backgroundImagePath = bgRewritten))
                        }
                    }
                    attachmentDir.deleteRecursively()
                }
            } catch (e: Exception) {
                Log.e("NotesViewModel", "Restore with password failed", e)
                syncState.update { it.copy(syncStage = SyncStage.IDLE, syncStatusMessage = getApplication<Application>().getString(R.string.toast_restore_error, e.localizedMessage)) }
            }
        }
    }

    override fun clearStatusMessage() {
        syncState.update { it.copy(syncStatusMessage = null) }
    }
}
