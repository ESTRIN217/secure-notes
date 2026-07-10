package com.example.data.sync

interface SyncService {
    suspend fun searchBackupFile(accessToken: String): Result<String?>
    suspend fun createBackupFile(accessToken: String, fileContent: String): Result<String?>
    suspend fun uploadFileContent(accessToken: String, fileId: String, content: String): Result<Boolean>
    suspend fun downloadBackupFile(accessToken: String, fileId: String): Result<String?>
}
