package com.example.ui

import androidx.compose.runtime.saveable.mapSaver

sealed class Screen {
    object MainList : Screen()
    data class NoteEditor(val noteId: Int) : Screen()
    data class DrawingCanvas(val noteId: Int, val jsonPath: String? = null) : Screen()
    object CloudSync : Screen()
    object PrivacySettings : Screen()
    object Search : Screen()
    data class MediaViewer(val type: String, val src: String, val previousScreen: Screen) : Screen()
    object SettingsHub : Screen()
    object BackupRestore : Screen()
    object UpdateInfo : Screen()
    object About : Screen()
}

val ScreenSaver = mapSaver(
    save = { screen: Screen ->
        when (screen) {
            is Screen.MainList -> mapOf("route" to "main_list")
            is Screen.NoteEditor -> mapOf("route" to "note_editor", "noteId" to screen.noteId)
            is Screen.DrawingCanvas -> mapOf("route" to "drawing_canvas", "noteId" to screen.noteId, "jsonPath" to (screen.jsonPath ?: ""))
            is Screen.CloudSync -> mapOf("route" to "cloud_sync")
            is Screen.PrivacySettings -> mapOf("route" to "privacy_settings")
            is Screen.Search -> mapOf("route" to "search")
            is Screen.MediaViewer -> mapOf("route" to "media_viewer", "type" to screen.type, "src" to screen.src)
            is Screen.SettingsHub -> mapOf("route" to "settings_hub")
            is Screen.BackupRestore -> mapOf("route" to "backup_restore")
            is Screen.UpdateInfo -> mapOf("route" to "update_info")
            is Screen.About -> mapOf("route" to "about")
        }
    },
    restore = { map: Map<String, Any?> ->
        when (map["route"] as? String) {
            "main_list" -> Screen.MainList
            "note_editor" -> Screen.NoteEditor((map["noteId"] as? Int) ?: 0)
            "drawing_canvas" -> Screen.DrawingCanvas((map["noteId"] as? Int) ?: 0, (map["jsonPath"] as? String)?.ifEmpty { null })
            "cloud_sync" -> Screen.CloudSync
            "privacy_settings" -> Screen.PrivacySettings
            "search" -> Screen.Search
            "media_viewer" -> Screen.MediaViewer((map["type"] as? String) ?: "", (map["src"] as? String) ?: "", Screen.MainList)
            "settings_hub" -> Screen.SettingsHub
            "backup_restore" -> Screen.BackupRestore
            "update_info" -> Screen.UpdateInfo
            "about" -> Screen.About
            else -> Screen.MainList
        }
    }
)
