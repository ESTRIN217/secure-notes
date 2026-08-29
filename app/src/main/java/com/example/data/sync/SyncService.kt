package com.example.data.sync

interface SyncService {
    suspend fun searchBackupFile(accessToken: String): Result<BackupFileInfo?>
    suspend fun createBackupFile(accessToken: String, fileContent: String): Result<String?>
    suspend fun uploadFileContent(accessToken: String, fileId: String, content: String): Result<Boolean>
    suspend fun downloadBackupFile(accessToken: String, fileId: String): Result<String?>
    suspend fun uploadFileBytes(accessToken: String, fileId: String, data: ByteArray): Result<Boolean>
    suspend fun downloadFileBytes(accessToken: String, fileId: String): Result<ByteArray?>
}

data class BackupFileInfo(
    val id: String,
    val modifiedTime: Long?
)
