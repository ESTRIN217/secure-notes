package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.mapSaver
import com.example.ui.viewmodel.StorageViewModel
import com.example.ui.viewmodel.ChatHistoryViewModel
import kotlinx.coroutines.launch

data class ScreenContext(
    val viewModel: com.example.ui.viewmodel.NotesViewModel,
    val themeViewModel: com.example.ui.viewmodel.ThemeViewModel,
    val backupViewModel: com.example.ui.viewmodel.BackupViewModel,
    val updaterViewModel: com.example.ui.viewmodel.UpdaterViewModel,
    val aiViewModel: com.example.ui.viewmodel.AiViewModel,
    val storageViewModel: StorageViewModel,
    val chatHistoryViewModel: ChatHistoryViewModel,
    val navigator: Navigator,
    val currentScreen: Screen
)

sealed class Screen {
    @Composable abstract fun render(context: ScreenContext)

    object MainList : Screen() {
        @Composable
        override fun render(context: ScreenContext) {
            val scope = rememberCoroutineScope()
            MainListScreen(
                viewModel = context.viewModel,
                aiViewModel = context.aiViewModel,
                onNavigateToEditor = { noteId -> context.navigator.onNavigateTo(Screen.NoteEditor(noteId)) },
                onNavigateToCloud = { context.navigator.onNavigateTo(Screen.BackupRestore) },
                onNavigateToPrivacy = { context.navigator.onNavigateTo(Screen.PrivacySettings) },
                onNavigateToSearch = { context.navigator.onNavigateTo(Screen.Search) },
                onNavigateToDrawing = { id, path -> context.navigator.onNavigateTo(Screen.DrawingCanvas(id, path)) },
                onNavigateToMediaViewer = { type, src -> context.navigator.onNavigateTo(Screen.MediaViewer(type, src, context.currentScreen)) },
                onNavigateToSettingsHub = { context.navigator.onNavigateTo(Screen.SettingsHub) },
                onNavigateToBackupRestore = { context.navigator.onNavigateTo(Screen.BackupRestore) },
                onNavigateToUpdateInfo = { context.navigator.onNavigateTo(Screen.UpdateInfo) },
                onNavigateToAbout = { context.navigator.onNavigateTo(Screen.About) },
                onNavigateToChatHistory = { context.navigator.onNavigateTo(Screen.ChatHistory) },
                onLaunchNewAiChat = {
                  context.chatHistoryViewModel.createSession(backend = context.aiViewModel.backend.value)
                  context.navigator.onNavigateTo(Screen.AiChatStandalone)
                },
                onNavigateToNewDrawing = {
                    scope.launch {
                        val noteId = context.viewModel.saveNoteAndGetId(id = 0, title = "", content = "", isEncrypted = false, tagsList = emptyList())
                        context.navigator.onNavigateTo(Screen.DrawingCanvas(noteId, null))
                    }
                },
            )
        }
    }

    data class NoteEditor(val noteId: Int) : Screen() {
        @Composable
        override fun render(context: ScreenContext) {
            NoteEditorScreen(
                noteId = noteId,
                viewModel = context.viewModel,
                aiViewModel = context.aiViewModel,
                onBack = { context.navigator.onNavigateBack(Screen.MainList) },
                onNavigateToDrawing = { id, path -> context.navigator.onNavigateTo(Screen.DrawingCanvas(id, path)) },
                onNavigateToMediaViewer = { type, src -> context.navigator.onNavigateTo(Screen.MediaViewer(type, src, context.currentScreen)) },
                onNavigateToAiChat = { id -> context.navigator.onNavigateTo(Screen.AiChat(id)) },
                onNavigateToNote = { id -> context.navigator.onNavigateTo(Screen.NoteEditor(id)) }
            )
        }
    }

    data class DrawingCanvas(val noteId: Int, val jsonPath: String? = null) : Screen() {
        @Composable
        override fun render(context: ScreenContext) {
            DrawingCanvasScreen(
                noteId = noteId,
                jsonPath = jsonPath,
                viewModel = context.viewModel,
                onBack = { context.navigator.onNavigateBack(Screen.NoteEditor(noteId)) }
            )
        }
    }

    object PrivacySettings : Screen() {
        @Composable
        override fun render(context: ScreenContext) {
            com.example.ui.settings.PrivacySettingsScreen(
                viewModel = context.viewModel,
                onBack = { context.navigator.onNavigateBack(Screen.SettingsHub) }
            )
        }
    }

    object Search : Screen() {
        @Composable
        override fun render(context: ScreenContext) {
            SearchScreen(
                viewModel = context.viewModel,
                onNavigateToEditor = { noteId -> context.navigator.onNavigateTo(Screen.NoteEditor(noteId)) },
                onBack = { context.navigator.onNavigateBack(Screen.MainList) },
                onNavigateToDrawing = { id, path -> context.navigator.onNavigateTo(Screen.DrawingCanvas(id, path)) },
                onNavigateToMediaViewer = { type, src -> context.navigator.onNavigateTo(Screen.MediaViewer(type, src, context.currentScreen)) }
            )
        }
    }

    data class MediaViewer(val type: String, val src: String, val previousScreen: Screen) : Screen() {
        @Composable
        override fun render(context: ScreenContext) {
            com.example.ui.MediaViewerScreen(
                type = type,
                src = src,
                onBack = { context.navigator.onNavigateBack(previousScreen) },
            )
        }
    }

    object SettingsHub : Screen() {
        @Composable
        override fun render(context: ScreenContext) {
            com.example.ui.settings.SettingsScreen(
                themeViewModel = context.themeViewModel,
                aiViewModel = context.aiViewModel,
                onBack = { context.navigator.onNavigateBack(Screen.MainList) },
                onNavigateToBackupRestore = { context.navigator.onNavigateTo(Screen.BackupRestore) },
                onNavigateToStorageManager = { context.navigator.onNavigateTo(Screen.StorageManager) },
                onNavigateToUpdateInfo = { context.navigator.onNavigateTo(Screen.UpdateInfo) },
                onNavigateToAbout = { context.navigator.onNavigateTo(Screen.About) },
                onNavigateToPrivacy = { context.navigator.onNavigateTo(Screen.PrivacySettings) },
                onNavigateToLegalInfo = { context.navigator.onNavigateTo(Screen.LegalInfo) },
                onNavigateToLicenses = { context.navigator.onNavigateTo(Screen.Licenses) },
                onNavigateToAiSettings = { context.navigator.onNavigateTo(Screen.AiSettings) }
            )
        }
    }

    object BackupRestore : Screen() {
        @Composable
        override fun render(context: ScreenContext) {
            com.example.ui.settings.BackupRestoreScreen(
                viewModel = context.backupViewModel,
                onBack = { context.navigator.onNavigateBack(Screen.SettingsHub) }
            )
        }
    }

    object UpdateInfo : Screen() {
        @Composable
        override fun render(context: ScreenContext) {
            com.example.ui.settings.UpdateInfoScreen(
                viewModel = context.updaterViewModel,
                onBack = { context.navigator.onNavigateBack(Screen.SettingsHub) }
            )
        }
    }

    object LegalInfo : Screen() {
        @Composable
        override fun render(context: ScreenContext) {
            com.example.ui.settings.LegalInfoScreen(
                onBack = { context.navigator.onNavigateBack(Screen.SettingsHub) }
            )
        }
    }

    object Licenses : Screen() {
        @Composable
        override fun render(context: ScreenContext) {
            com.example.ui.settings.LicensesScreen(
                onBack = { context.navigator.onNavigateBack(Screen.SettingsHub) }
            )
        }
    }

    object AiSettings : Screen() {
        @Composable
        override fun render(context: ScreenContext) {
            com.example.ui.settings.AiSettingsScreen(
                aiViewModel = context.aiViewModel,
                onBack = { context.navigator.onNavigateBack(Screen.SettingsHub) }
            )
        }
    }

    object About : Screen() {
        @Composable
        override fun render(context: ScreenContext) {
            com.example.ui.settings.AboutScreen(
                onBack = { context.navigator.onNavigateBack(Screen.SettingsHub) },
                onNavigateToLegalInfo = { context.navigator.onNavigateTo(Screen.LegalInfo) },
                onNavigateToLicenses = { context.navigator.onNavigateTo(Screen.Licenses) }
            )
        }
    }

    data class AiChat(val noteId: Int) : Screen() {
        @Composable
        override fun render(context: ScreenContext) {
            AiChatScreen(
                viewModel = context.aiViewModel,
                chatHistoryViewModel = context.chatHistoryViewModel,
                sessionId = 0,
                noteId = noteId,
                fullContent = "",
                selectedText = "",
                onBack = { context.navigator.onNavigateBack(Screen.NoteEditor(noteId)) },
                onInsert = { text ->
                    context.aiViewModel.requestInsert(text)
                    context.navigator.onNavigateBack(Screen.NoteEditor(noteId))
                }
            )
        }
    }

    data class AiChatSession(val sessionId: Int) : Screen() {
        @Composable
        override fun render(context: ScreenContext) {
            AiChatScreen(
                viewModel = context.aiViewModel,
                chatHistoryViewModel = context.chatHistoryViewModel,
                sessionId = sessionId,
                noteId = 0,
                fullContent = "",
                selectedText = "",
                onBack = { context.navigator.onNavigateBack(Screen.ChatHistory) },
                onInsert = null
            )
        }
    }

    object AiChatStandalone : Screen() {
        @Composable
        override fun render(context: ScreenContext) {
            AiChatScreen(
                viewModel = context.aiViewModel,
                chatHistoryViewModel = context.chatHistoryViewModel,
                sessionId = 0,
                noteId = 0,
                fullContent = "",
                selectedText = "",
                onBack = { context.navigator.onNavigateBack(Screen.MainList) },
                onInsert = null
            )
        }
    }

    object StorageManager : Screen() {
        @Composable
        override fun render(context: ScreenContext) {
            com.example.ui.settings.StorageManagerScreen(
                viewModel = context.storageViewModel,
                onBack = { context.navigator.onNavigateBack(Screen.SettingsHub) }
            )
        }
    }

    object ChatHistory : Screen() {
        @Composable
        override fun render(context: ScreenContext) {
            ChatHistoryScreen(
                viewModel = context.chatHistoryViewModel,
                aiViewModel = context.aiViewModel,
                onNavigateToChat = { sessionId -> context.navigator.onNavigateTo(Screen.AiChatSession(sessionId)) },
                onBack = { context.navigator.onNavigateBack(Screen.MainList) }
            )
        }
    }

    object ChatSearch : Screen() {
        @Composable
        override fun render(context: ScreenContext) {
            ChatSearchScreen(
                viewModel = context.chatHistoryViewModel,
                onNavigateToChat = { sessionId -> context.navigator.onNavigateTo(Screen.AiChatSession(sessionId)) },
                onBack = { context.navigator.onNavigateBack(Screen.ChatHistory) }
            )
        }
    }
}

val ScreenSaver = mapSaver(
    save = { screen: Screen ->
        when (screen) {
            is Screen.MainList -> mapOf("route" to "main_list")
            is Screen.NoteEditor -> mapOf("route" to "note_editor", "noteId" to screen.noteId)
            is Screen.DrawingCanvas -> mapOf("route" to "drawing_canvas", "noteId" to screen.noteId, "jsonPath" to (screen.jsonPath ?: ""))
            is Screen.PrivacySettings -> mapOf("route" to "privacy_settings")
            is Screen.Search -> mapOf("route" to "search")
            is Screen.MediaViewer -> mapOf("route" to "media_viewer", "type" to screen.type, "src" to screen.src)
            is Screen.SettingsHub -> mapOf("route" to "settings_hub")
            is Screen.BackupRestore -> mapOf("route" to "backup_restore")
            is Screen.UpdateInfo -> mapOf("route" to "update_info")
            is Screen.LegalInfo -> mapOf("route" to "legal_info")
            is Screen.Licenses -> mapOf("route" to "licenses")
            is Screen.AiSettings -> mapOf("route" to "ai_settings")
            is Screen.AiChat -> mapOf("route" to "ai_chat", "noteId" to screen.noteId)
            is Screen.AiChatSession -> mapOf("route" to "ai_chat_session", "sessionId" to screen.sessionId)
            is Screen.About -> mapOf("route" to "about")
            is Screen.StorageManager -> mapOf("route" to "storage_manager")
            is Screen.ChatHistory -> mapOf("route" to "chat_history")
            is Screen.ChatSearch -> mapOf("route" to "chat_search")
            is Screen.AiChatStandalone -> mapOf("route" to "ai_chat_standalone")
        }
    },
    restore = { map: Map<String, Any?> ->
        when (map["route"] as? String) {
            "main_list" -> Screen.MainList
            "note_editor" -> Screen.NoteEditor((map["noteId"] as? Int) ?: 0)
            "drawing_canvas" -> Screen.DrawingCanvas((map["noteId"] as? Int) ?: 0, (map["jsonPath"] as? String)?.ifEmpty { null })
            "privacy_settings" -> Screen.PrivacySettings
            "search" -> Screen.Search
            "media_viewer" -> Screen.MediaViewer((map["type"] as? String) ?: "", (map["src"] as? String) ?: "", Screen.MainList)
            "settings_hub" -> Screen.SettingsHub
            "backup_restore" -> Screen.BackupRestore
            "update_info" -> Screen.UpdateInfo
            "legal_info" -> Screen.LegalInfo
            "licenses" -> Screen.Licenses
            "ai_settings" -> Screen.AiSettings
            "ai_chat" -> Screen.AiChat((map["noteId"] as? Int) ?: 0)
            "ai_chat_session" -> Screen.AiChatSession((map["sessionId"] as? Int) ?: 0)
            "about" -> Screen.About
            "storage_manager" -> Screen.StorageManager
            "chat_history" -> Screen.ChatHistory
            "chat_search" -> Screen.ChatSearch
            "ai_chat_standalone" -> Screen.AiChatStandalone
            else -> Screen.MainList
        }
    }
)