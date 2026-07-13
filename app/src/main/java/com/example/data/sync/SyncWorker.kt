package com.example.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.AppConstants
import com.example.data.local.NoteDatabase
import com.example.data.model.toJson
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val noteDatabase = NoteDatabase.getDatabase(appContext)
    private val noteDao = noteDatabase.noteDao
    private val tagDao = noteDatabase.tagDao
    private val syncService: SyncService = GoogleDriveSyncService()

    override suspend fun doWork(): Result {
        val encryptedPrefs = run {
            val masterKey = MasterKey.Builder(applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                applicationContext,
                "secure_notes_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }

        val token = encryptedPrefs.getString(AppConstants.DRIVE_ACCESS_TOKEN_KEY, null)
        if (token.isNullOrEmpty()) return Result.failure()

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
            }

            val syncPayload = JSONObject().apply {
                put("notes", notesArray)
                put("tags", tagsArray)
                put("settings", settingsObj)
                put("timestamp", System.currentTimeMillis())
            }.toString()

            val finalPayload = JSONObject().apply {
                put("encrypted", false)
                put("data", syncPayload)
            }.toString()

            val existingFileId = syncService.searchBackupFile(token).getOrNull()
            val success = if (existingFileId != null) {
                syncService.uploadFileContent(token, existingFileId, finalPayload).getOrDefault(false)
            } else {
                syncService.createBackupFile(token, finalPayload).getOrNull() != null
            }

            return if (success) Result.success() else Result.retry()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Background sync failed", e)
            return Result.retry()
        }
    }
}
