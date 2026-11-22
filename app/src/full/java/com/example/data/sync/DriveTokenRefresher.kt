package com.example.data.sync

import android.accounts.Account
import android.content.Context
import com.google.android.gms.auth.GoogleAuthUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DriveTokenRefresher {
    suspend fun refresh(context: Context, email: String): String? = try {
        withContext(Dispatchers.IO) {
            GoogleAuthUtil.getToken(
                context,
                Account(email, "com.google"),
                "oauth2:https://www.googleapis.com/auth/drive.appdata"
            )
        }
    } catch (_: Exception) {
        null
    }
}
