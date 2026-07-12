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

            val notesArray = JSONArray()
            rawNotes.forEach { note -> notesArray.put(note.toJson()) }
            val tagsArray = JSONArray()
            tags.forEach { tag -> tagsArray.put(tag.toJson()) }

            val syncPayload = JSONObject().apply {
                put("notes", notesArray)
                put("tags", tagsArray)
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
