package com.example.service

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.example.util.FloatingBubbleManager

@RequiresApi(Build.VERSION_CODES.N)
class FloatingNoteTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (!FloatingBubbleManager.isOverlayPermissionGranted(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivityAndCollapse(intent)
            return
        }

        if (FloatingBubbleManager.isRunning()) {
            FloatingBubbleManager.stopService(this)
        } else {
            FloatingBubbleManager.startService(this)
        }
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val active = FloatingBubbleManager.isRunning()
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }
}
