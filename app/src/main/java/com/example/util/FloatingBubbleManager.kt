package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.service.FloatingBubbleService
import kotlinx.coroutines.flow.StateFlow

object FloatingBubbleManager {

    fun isOverlayPermissionGranted(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun openOverlaySettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun startService(context: Context): Boolean {
        if (!isOverlayPermissionGranted(context)) return false
        val intent = Intent(context, FloatingBubbleService::class.java)
        ContextCompat.startForegroundService(context, intent)
        return true
    }

    fun stopService(context: Context) {
        FloatingBubbleService.stopService(context)
    }

    fun isRunningFlow(): StateFlow<Boolean> = FloatingBubbleService.isRunning

    fun isRunning(): Boolean = FloatingBubbleService.isRunning.value
}
