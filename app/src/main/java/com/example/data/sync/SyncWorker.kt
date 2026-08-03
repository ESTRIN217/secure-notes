package com.example.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.AppConstants
import com.example.data.local.NoteDatabase
import com.example.data.model.Note
import com.example.data.model.toJson
import com.example.data.security.CipherService
import com.example.data.security.EncryptionServiceImpl
import com.example.data.security.SecurePrefsStore
import com.example.util.BackupAttachmentHelper
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val noteDatabase = NoteDatabase.getDatabase(appContext)
    private val noteDao = noteDatabase.noteDao
    private val tagDao = noteDatabase.tagDao
    private val syncService: SyncService = GoogleDriveSyncService()
    private val cipherService: CipherService = EncryptionServiceImpl()

    override suspend fun doWork(): Result {
        val encryptedPrefs = SecurePrefsStore(applicationContext)

        val token = encryptedPrefs.getString(AppConstants.DRIVE_ACCESS_TOKEN_KEY, null)
        if (token.isNullOrEmpty()) return Result.failure()

        val hasPassword = applicationContext.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
            .contains(AppConstants.MASTER_PASSWORD_HASH_KEY)

        try {
            val rawNotes = noteDao.getAllNotesFlow().first()
            val tags = tagDao.getAllTagsFlow().first()
            val sharedPrefs = applicationContext.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)

            val notesArray = JSONArray()
            rawNotes.forEach { note -> notesArray.put(note.toJson()) }
            val tagsArray = JSONArray()
            tags.forEach { tag -> tagsArray.put(tag.toJson()) }

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

            val encryptBackups = sharedPrefs.getBoolean(AppConstants.ENCRYPT_BACKUPS_KEY, hasPassword)
            val cachedPassword = encryptedPrefs.getString(AppConstants.CACHED_MASTER_PASSWORD_KEY, null)
            val includeAttachments = sharedPrefs.getBoolean(AppConstants.INCLUDE_ATTACHMENTS_KEY, false)

            val finalPayload: String
            if (encryptBackups && !cachedPassword.isNullOrEmpty()) {
                val salt = cipherService.generateSalt()
                val iv = cipherService.generateIv()
                val cipherPayload = cipherService.encrypt(syncPayload, cachedPassword, salt, iv).getOrDefault("")
                finalPayload = JSONObject().apply {
                    put("encrypted", true)
                    put("salt", salt)
                    put("iv", iv)
                    put("data", cipherPayload)
                }.toString()
            } else {
                finalPayload = JSONObject().apply {
                    put("encrypted", false)
                    put("data", syncPayload)
                }.toString()
            }

            val existingFileId = syncService.searchBackupFile(token).getOrNull()

            var backupSize = 0L
            val success: Boolean

            if (includeAttachments) {
                val context = applicationContext
                val tempDir = File(context.cacheDir, "backup_attachments_${System.currentTimeMillis()}")
                tempDir.mkdirs()
                val tempAttachmentsDir = File(tempDir, "attachments")
                try {
                    val allPathMaps = mutableMapOf<String, String>()
                    rawNotes.forEach { note ->
                        val pathMap = BackupAttachmentHelper.collectAndCopyAttachments(
                            note.content, note.backgroundImagePath, context, tempAttachmentsDir
                        )
                        allPathMaps.putAll(pathMap)
                    }
                    val zipFile = File(tempDir, "backup.zip")
                    BackupAttachmentHelper.buildBackupZip(finalPayload, allPathMaps, tempAttachmentsDir, zipFile)
                    val zipBytes = zipFile.readBytes()
                    backupSize = zipFile.length()

                    success = if (existingFileId != null) {
                        syncService.uploadFileBytes(token, existingFileId, zipBytes).getOrDefault(false)
                    } else {
                        val newId = syncService.createBackupFile(token, "placeholder").getOrNull()
                        newId != null && syncService.uploadFileBytes(token, newId, zipBytes).getOrDefault(false)
                    }
                } finally {
                    tempDir.deleteRecursively()
                }
            } else {
                backupSize = finalPayload.toByteArray().size.toLong()
                success = if (existingFileId != null) {
                    syncService.uploadFileContent(token, existingFileId, finalPayload).getOrDefault(false)
                } else {
                    syncService.createBackupFile(token, finalPayload).getOrNull() != null
                }
            }

            if (!success) return Result.retry()

            sharedPrefs.edit()
                .putLong(AppConstants.LAST_BACKUP_SIZE_CLOUD_KEY, backupSize)
                .putString(AppConstants.LAST_SYNC_TIME_KEY,
                    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).let { fmt ->
                        applicationContext.getString(com.example.R.string.label_today_at, fmt.format(Date()))
                    })
                .apply()

            return Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Background sync failed", e)
            return Result.retry()
        }
    }
}
