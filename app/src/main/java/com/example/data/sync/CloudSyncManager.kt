package com.example.data.sync

import com.example.data.model.Note
import com.example.data.model.Tag
import kotlinx.coroutines.flow.StateFlow

interface CloudSyncManager {
    val isDriveLinked: StateFlow<Boolean>
    val lastSyncTime: StateFlow<String>
    val syncStatusMessage: StateFlow<String?>
    val rawNotes: StateFlow<List<Note>>
    val availableTags: StateFlow<List<Tag>>

    fun linkGoogleDrive(token: String)
    fun unlinkGoogleDrive()
    fun forceSyncCloud()
    fun restoreSyncCloud()
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
        isArchived: Boolean = false
    )
    fun createTag(name: String, colorHex: String)
    fun clearStatusMessage()
}
