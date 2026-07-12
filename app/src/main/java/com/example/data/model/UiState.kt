package com.example.data.model

data class AuthState(
    val isPasswordSet: Boolean = false,
    val isUnlocked: Boolean = false
)

data class ListState(
    val searchQuery: String = "",
    val selectedTagFilter: String? = null,
    val currentSection: NavigationSection = NavigationSection.HOME
)

enum class SyncStage { IDLE, ENCRYPTING, SEARCHING, UPLOADING, DOWNLOADING, RESTORING, PASSWORD_REQUIRED }

data class SyncState(
    val isDriveLinked: Boolean = false,
    val lastSyncTime: String = "",
    val syncStatusMessage: String? = null,
    val syncStage: SyncStage = SyncStage.IDLE
)
