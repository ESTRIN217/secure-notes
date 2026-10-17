package com.example

import android.content.Context
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
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.estrin217.editordecodigo.CodeEditor
import com.estrin217.editordecodigo.ui.CodeEditorApp
import com.estrin217.editordecodigo.ui.CodeEditorViewModel
import com.example.data.SharedPreferencesRepository
import com.example.util.MarkdownHandoff
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.isDark
import com.example.ui.viewmodel.ThemeViewModel

/**
 * Thin host for the :editor-de-codigo library. Owns the Activity concerns —
 * locale, FLAG_SECURE, app theme and SEND/VIEW intent-filters — and
 * delegates all editor state to the library's [CodeEditorViewModel].
 * Lives outside the MainActivity lock flow; content is transient user
 * text, not the vault.
 */
class CodeEditorActivity : ComponentActivity() {

    private val codeViewModel: CodeEditorViewModel by viewModels {
        CodeEditor.viewModelFactory(applicationContext)
    }

    companion object {
        /** Ruta del sobre de traspaso en `cacheDir`. La ruta viaja en el Intent, el contenido no. */
        const val EXTRA_NOTE_HANDOFF = "com.example.EXTRA_NOTE_HANDOFF"
    }

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        val lang = prefs.getString(AppConstants.LANGUAGE_KEY, "") ?: ""
        val context = if (lang.isNotEmpty()) {
            val locale = java.util.Locale.forLanguageTag(lang)
            val config = Configuration(newBase.resources.configuration)
            config.setLocale(locale)
            newBase.createConfigurationContext(config)
        } else {
            newBase
        }
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val prefs = getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(AppConstants.SCREENSHOT_ENABLED_KEY, false)) {
            window.setFlags(
                android.view.WindowManager.LayoutParams.FLAG_SECURE,
                android.view.WindowManager.LayoutParams.FLAG_SECURE
            )
        }
        if (savedInstanceState == null) {
          codeViewModel.handleIntent(this, intent)
        }

        setContent {
            val themeViewModel: ThemeViewModel = viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return ThemeViewModel(SharedPreferencesRepository(applicationContext)) as T
                    }
                }
            )
            val darkModeOption by themeViewModel.darkModeOption.collectAsStateWithLifecycle()
            val appTheme by themeViewModel.appTheme.collectAsStateWithLifecycle()
            val isDark = darkModeOption.isDark(isSystemInDarkTheme())

            MyApplicationTheme(appTheme = appTheme, darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val scope = rememberCoroutineScope()
                    CodeEditorApp(
                        viewModel = codeViewModel,
                        // Esta Activity vive FUERA del flujo de bloqueo de la bóveda y no tiene el
                        // `NotesViewModel`, así que no puede escribir en la base de notas: saltaría
                        // la contraseña maestra (RF-81). Deja el sobre en `cacheDir` y arranca
                        // `MainActivity`, que sí está dentro del flujo y sí crea la nota.
                        onSendToNotes = { name, markdown ->
                            val ruta = MarkdownHandoff.write(cacheDir, name, markdown)
                            val intent = Intent(this, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                putExtra(EXTRA_NOTE_HANDOFF, ruta)
                            }
                            runCatching { startActivity(intent) }.onFailure {
                                // Solo se registra la clase: el mensaje puede llevar la ruta, y la
                                // ruta lleva parte del nombre del archivo del usuario (RNF-36).
                                Log.e("CodeEditorActivity", "handoff failed", it)
                                // Sin destino, el sobre se queda en cacheDir: se borra aqui para no
                                // dejar un Markdown sin cifrar tirado (RNF-35).
                                MarkdownHandoff.consume(ruta)
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        codeViewModel.handleIntent(this, intent)
    }
}
