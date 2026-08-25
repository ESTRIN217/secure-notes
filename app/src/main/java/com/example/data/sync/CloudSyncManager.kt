package com.example.data.sync

import com.example.data.model.Note
import com.example.data.model.SyncState
import com.example.data.model.Tag
import kotlinx.coroutines.flow.StateFlow

interface CloudSyncManager {
    val syncState: StateFlow<SyncState>
    val rawNotes: StateFlow<List<Note>>
    val availableTags: StateFlow<List<Tag>>

    val driveAccountEmail: StateFlow<String?>
    val driveProfilePictureUri: StateFlow<String?>

    fun linkGoogleDrive(token: String, accountEmail: String = "", pictureUri: String = "")
    fun unlinkGoogleDrive()
    fun forceSyncCloud()
    fun restoreSyncCloud()
    fun provideRestorePassword(password: String)
    fun saveNote(
        id: Int = 0,
        title: String = "",
        content: String = "",
        isEncrypted: Boolean = false,
        tagsList: List<String> = emptyList(),
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
    )
    fun createTag(name: String, colorHex: String)
    fun clearStatusMessage()
    val isPasswordSet: StateFlow<Boolean>

    val encryptBackups: StateFlow<Boolean>
    val autoBackupEnabled: StateFlow<Boolean>
    val autoBackupInterval: StateFlow<String>
    val lastBackupSizeCloud: StateFlow<Long>
    val lastBackupSizeLocal: StateFlow<Long>
    fun setEncryptBackups(enabled: Boolean)
    fun setAutoBackupEnabled(enabled: Boolean)
    fun setAutoBackupInterval(interval: String)
    fun clearCachedPassword()
}
