package com.example

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.SharedPreferencesRepository
import com.example.data.security.EncryptionServiceImpl
import com.example.perf.DisplayRefreshRate
import com.example.perf.ProvidePerformanceProfile
import com.example.ui.LockScreen
import com.example.ui.settings.EXTRA_SETTINGS_DESTINATION
import com.example.ui.settings.SettingsApp
import com.example.ui.settings.SettingsDestination
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.isDark
import com.example.ui.viewmodel.NotesViewModel
import com.example.ui.viewmodel.notesViewModelFactory
import com.example.ui.viewmodel.themeViewModelFactory

/**
 * Spec 044. Todo el árbol de ajustes vive aquí fuera del `Navigator` de `MainActivity`.
 * No está exportada y no tiene filtros de intents (RNF-206): solo la abre esta app.
 */
class SettingsActivity : ComponentActivity() {

    private val app: SecureNotesApplication get() = application as SecureNotesApplication

    private val notesViewModel: NotesViewModel by viewModels(
        factoryProducer = { notesViewModelFactory(application) }
    )

    private val cipherService = EncryptionServiceImpl()

    private var destination by mutableStateOf(SettingsDestination.HUB)

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        val lang = prefs.getString(AppConstants.LANGUAGE_KEY, "") ?: ""
        val context = if (lang.isNotEmpty()) {
            val config = Configuration(newBase.resources.configuration)
            config.setLocale(java.util.Locale.forLanguageTag(lang))
            newBase.createConfigurationContext(config)
        } else {
            newBase
        }
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        DisplayRefreshRate.requestPeak(this)
        val prefs = getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(AppConstants.SCREENSHOT_ENABLED_KEY, false)) {
            window.setFlags(
                android.view.WindowManager.LayoutParams.FLAG_SECURE,
                android.view.WindowManager.LayoutParams.FLAG_SECURE
            )
        }
        destination = leerDestino(intent)

        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) notesViewModel.onAppBackgrounded()
        })

        setContent {
            val themeViewModel: com.example.ui.viewmodel.ThemeViewModel =
                androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = themeViewModelFactory(applicationContext)
                )
            val darkModeOption by themeViewModel.darkModeOption.collectAsStateWithLifecycle()
            val appTheme by themeViewModel.appTheme.collectAsStateWithLifecycle()
            val isDark = darkModeOption.isDark(isSystemInDarkTheme())
            val isPasswordSet by notesViewModel.isPasswordSet.collectAsStateWithLifecycle()
            val isUnlocked by notesViewModel.isUnlocked.collectAsStateWithLifecycle()

            MyApplicationTheme(appTheme = appTheme, darkTheme = isDark) {
                ProvidePerformanceProfile(profile = app.performanceProfile) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        if (isPasswordSet && !isUnlocked) {
                            LockScreen(notesViewModel)
                        } else {
                            SettingsApp(
                                initialDestination = destination,
                                notesViewModel = notesViewModel,
                                cipherService = cipherService,
                                onClose = { finish() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        DisplayRefreshRate.reapplyPeak(this)
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        notesViewModel.resetAutoLockTimer()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        destination = leerDestino(intent)
    }

    private fun leerDestino(intent: Intent): SettingsDestination =
        SettingsDestination.fromExtra(intent.getStringExtra(EXTRA_SETTINGS_DESTINATION))
}
