package com.example.data.model

enum class SyncStage { IDLE, ENCRYPTING, SEARCHING, UPLOADING, DOWNLOADING, RESTORING, PASSWORD_REQUIRED }

data class SyncState(
    val isDriveLinked: Boolean = false,
    val lastSyncTime: String = "",
    val syncStatusMessage: String? = null,
    val syncStage: SyncStage = SyncStage.IDLE
)
