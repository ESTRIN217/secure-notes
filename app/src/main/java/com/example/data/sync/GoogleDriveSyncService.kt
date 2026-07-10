package com.example.data.sync

import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

class GoogleDriveSyncService : SyncService {
    private companion object {
        private const val TAG = "GoogleDriveSync"
        private const val FILES_URL = "https://www.googleapis.com/drive/v3/files"
        private const val UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    override suspend fun searchBackupFile(accessToken: String): Result<String?> {
        val request = Request.Builder()
            .url("$FILES_URL?q=name='secure_notes_backup.json' and trashed=false&fields=files(id)")
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        return try {
            suspendCancellableCoroutine<String?> { continuation ->
                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e(TAG, "Failed searching for backup file", e)
                        continuation.resume(null)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        try {
                            if (response.isSuccessful) {
                                val body = response.body?.string()
                                if (!body.isNullOrEmpty()) {
                                    val json = JSONObject(body)
                                    val filesArray = json.optJSONArray("files")
                                    if (filesArray != null && filesArray.length() > 0) {
                                        val fileId = filesArray.getJSONObject(0).optString("id")
                                        continuation.resume(fileId)
                                        return
                                    }
                                }
                            } else {
                                Log.e(TAG, "Search file failed: Code ${response.code} ${response.message}")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Exception parsing search file response", e)
                        }
                        continuation.resume(null)
                    }
                })
            }.let { Result.success(it) }
        } catch (e: Exception) {
            Log.e(TAG, "searchBackupFile failed", e)
            Result.failure(e)
        }
    }

    override suspend fun createBackupFile(accessToken: String, fileContent: String): Result<String?> {
        val metadata = JSONObject().apply {
            put("name", "secure_notes_backup.json")
            put("parents", JSONArray().put("appDataFolder"))
        }

        val body = metadata.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val metadataRequest = Request.Builder()
            .url(FILES_URL)
            .addHeader("Authorization", "Bearer $accessToken")
            .post(body)
            .build()

        return try {
            val fileId = suspendCancellableCoroutine<String?> { continuation ->
                client.newCall(metadataRequest).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        continuation.resume(null)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        try {
                            if (response.isSuccessful) {
                                val bodyStr = response.body?.string()
                                if (!bodyStr.isNullOrEmpty()) {
                                    val id = JSONObject(bodyStr).optString("id")
                                    continuation.resume(id)
                                    return
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Exception creating file", e)
                        }
                        continuation.resume(null)
                    }
                })
            }

            if (fileId != null) {
                val uploadResult = uploadFileContent(accessToken, fileId, fileContent)
                val uploadSuccess = uploadResult.getOrDefault(false)
                Result.success(if (uploadSuccess) fileId else null)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "createBackupFile failed", e)
            Result.failure(e)
        }
    }

    override suspend fun uploadFileContent(accessToken: String, fileId: String, content: String): Result<Boolean> {
        val contentBody = content.toRequestBody("text/plain".toMediaTypeOrNull())
        val uploadRequest = Request.Builder()
            .url("$UPLOAD_URL/$fileId?uploadType=media")
            .addHeader("Authorization", "Bearer $accessToken")
            .patch(contentBody)
            .build()

        return try {
            suspendCancellableCoroutine<Boolean> { continuation ->
                client.newCall(uploadRequest).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        continuation.resume(false)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        continuation.resume(response.isSuccessful)
                    }
                })
            }.let { Result.success(it) }
        } catch (e: Exception) {
            Log.e(TAG, "uploadFileContent failed", e)
            Result.failure(e)
        }
    }

    override suspend fun downloadBackupFile(accessToken: String, fileId: String): Result<String?> {
        val request = Request.Builder()
            .url("$FILES_URL/$fileId?alt=media")
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        return try {
            suspendCancellableCoroutine<String?> { continuation ->
                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        continuation.resume(null)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        try {
                            if (response.isSuccessful) {
                                continuation.resume(response.body?.string())
                            } else {
                                continuation.resume(null)
                            }
                        } catch (e: Exception) {
                            continuation.resume(null)
                        }
                    }
                })
            }.let { Result.success(it) }
        } catch (e: Exception) {
            Log.e(TAG, "downloadBackupFile failed", e)
            Result.failure(e)
        }
    }
}
