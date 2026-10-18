package com.example.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.AppConstants
import com.example.data.backup.backupSettingsJson
import com.example.data.backup.readBackupSettings
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

            val settingsObj = backupSettingsJson(readBackupSettings(sharedPrefs))

            val syncPayload = JSONObject().apply {
                put("version", 4)
                put("notes", notesArray)
                put("tags", tagsArray)
                put("settings", settingsObj)
                put("timestamp", System.currentTimeMillis())
            }.toString()

            val encryptBackups = sharedPrefs.getBoolean(AppConstants.ENCRYPT_BACKUPS_KEY, hasPassword)
            val cachedPassword = encryptedPrefs.getString(AppConstants.CACHED_MASTER_PASSWORD_KEY, null)

            val context = applicationContext
            val tempDir = File(context.cacheDir, "backup_attachments_${System.currentTimeMillis()}")
            tempDir.mkdirs()
            var backupSize = 0L
            var success = false
            try {
                val allPathMaps = mutableMapOf<String, String>()
                rawNotes.forEach { note ->
                    val pathMap = BackupAttachmentHelper.collectAndCopyAttachments(
                        note.content, note.backgroundImagePath, context, tempDir
                    )
                    allPathMaps.putAll(pathMap)
                }

                val rewrittenPayload: String = if (allPathMaps.isNotEmpty()) {
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
                    payloadObj.toString()
                } else {
                    syncPayload
                }

                val finalPayload: String
                if (encryptBackups && !cachedPassword.isNullOrEmpty()) {
                    val salt = cipherService.generateSalt()
                    val iv = cipherService.generateIv()
                    val cipherPayload = cipherService.encrypt(rewrittenPayload, cachedPassword, salt, iv).getOrDefault("")
                    finalPayload = JSONObject().apply {
                        put("encrypted", true)
                        put("salt", salt)
                        put("iv", iv)
                        put("data", cipherPayload)
                    }.toString()
                } else {
                    finalPayload = JSONObject().apply {
                        put("encrypted", false)
                        put("data", rewrittenPayload)
                    }.toString()
                }

                val existingFileId = syncService.searchBackupFile(token).getOrNull()?.id

                val zipFile = File(tempDir, "backup.zip")
                BackupAttachmentHelper.buildBackupZip(finalPayload, allPathMaps, tempDir, zipFile)
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

            if (!success) return Result.retry()

            sharedPrefs.edit()
                .putLong(AppConstants.LAST_BACKUP_SIZE_CLOUD_KEY, backupSize)
                .putString(AppConstants.LAST_SYNC_TIME_KEY,SimpleDateFormat("dd/MM/yy hh:mm a", Locale.getDefault()).format(Date()))
                .apply()

            return Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Background sync failed", e)
            return Result.retry()
        }
    }
}
