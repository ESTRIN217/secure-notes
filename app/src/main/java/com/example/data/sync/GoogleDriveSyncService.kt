package com.example.data.sync

import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

object GoogleDriveSyncService {
    private const val TAG = "GoogleDriveSync"
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private const val FILES_URL = "https://www.googleapis.com/drive/v3/files"
    private const val UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"

    suspend fun searchBackupFile(accessToken: String): String? {
        val request = Request.Builder()
            .url("$FILES_URL?q=name='secure_notes_backup.json' and trashed=false&fields=files(id)")
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        return suspendCancellableCoroutine<String?> { continuation ->
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
        }
    }

    suspend fun createBackupFile(accessToken: String, fileContent: String): String? {
        val metadata = JSONObject().apply {
            put("name", "secure_notes_backup.json")
            put("parents", JSONArray().put("appDataFolder")) // Save securely inside the App Data Folder or root drive
        }

        // 1. Create file metadata in Google Drive
        val body = metadata.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val metadataRequest = Request.Builder()
            .url(FILES_URL)
            .addHeader("Authorization", "Bearer $accessToken")
            .post(body)
            .build()

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
            // 2. Upload actual content
            val uploadSuccess = uploadFileContent(accessToken, fileId, fileContent)
            return if (uploadSuccess) fileId else null
        }
        return null
    }

    suspend fun uploadFileContent(accessToken: String, fileId: String, content: String): Boolean {
        val contentBody = content.toRequestBody("text/plain".toMediaTypeOrNull())
        val uploadRequest = Request.Builder()
            .url("$UPLOAD_URL/$fileId?uploadType=media")
            .addHeader("Authorization", "Bearer $accessToken")
            .patch(contentBody)
            .build()

        return suspendCancellableCoroutine<Boolean> { continuation ->
            client.newCall(uploadRequest).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resume(false)
                }

                override fun onResponse(call: Call, response: Response) {
                    continuation.resume(response.isSuccessful)
                }
            })
        }
    }

    suspend fun downloadBackupFile(accessToken: String, fileId: String): String? {
        val request = Request.Builder()
            .url("$FILES_URL/$fileId?alt=media")
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        return suspendCancellableCoroutine<String?> { continuation ->
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
        }
    }
}
