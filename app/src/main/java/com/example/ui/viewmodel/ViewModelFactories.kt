package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.SecureNotesApplication
import com.example.data.SharedPreferencesRepository
import com.example.data.ai.ModelDownloader
import com.example.data.ai.OllamaService
import com.example.data.local.NoteDatabase
import com.example.data.security.CipherService
import com.example.data.security.EncryptionServiceImpl
import com.example.data.sync.GoogleDriveSyncService

/**
 * Spec 044, RNF-208. Los gráficos de ViewModels son iguales en `MainActivity` y en
 * `SettingsActivity`; tenerlos en un solo sitio evita que el de IA —que es el más largo— se
 * copie y acabe divergiendo. El motor sigue materializándose solo a través de `AiModelHost`.
 */
internal fun notesViewModelFactory(application: Application): ViewModelProvider.Factory =
    viewModelFactory {
        NotesViewModel(
            application,
            NoteDatabase.getDatabase(application),
            EncryptionServiceImpl(),
            GoogleDriveSyncService()
        )
    }

internal fun themeViewModelFactory(context: Context): ViewModelProvider.Factory =
    viewModelFactory { ThemeViewModel(SharedPreferencesRepository(context.applicationContext)) }

internal fun aiViewModelFactory(
    application: Application,
    cipherService: CipherService
): ViewModelProvider.Factory = viewModelFactory {
    val prefsRepo = SharedPreferencesRepository(application)
    val database = NoteDatabase.getDatabase(application)
    AiViewModel(
        application,
        prefsRepo,
        OllamaService(
            context = application,
            endpointUrl = prefsRepo.getAiEndpointUrl(),
            modelName = prefsRepo.getAiModelName()
        ),
        (application as SecureNotesApplication).aiModelHost,
        ModelDownloader(application),
        database.conversationDao,
        database.chatSessionDao,
        database.noteDao,
        cipherService,
        memoryDao = database.memoryDao
    )
}

internal fun backupViewModelFactory(
    application: Application,
    notesViewModel: NotesViewModel
): ViewModelProvider.Factory = viewModelFactory { BackupViewModel(application, notesViewModel) }

internal fun storageViewModelFactory(application: Application): ViewModelProvider.Factory =
    viewModelFactory { StorageViewModel(application, NoteDatabase.getDatabase(application)) }

internal fun chatHistoryViewModelFactory(application: Application): ViewModelProvider.Factory =
    viewModelFactory {
        val database = NoteDatabase.getDatabase(application)
        ChatHistoryViewModel(application, database.chatSessionDao, database.conversationDao)
    }

/** Factoría de un solo ViewModel: el `create` se ejecuta una vez por ViewModel y no por recomposición. */
private inline fun <reified T : ViewModel> viewModelFactory(
    noinline create: () -> T
): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = create() as VM
}
