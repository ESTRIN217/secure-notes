package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crash.CrashIssueUrl
import com.example.data.SharedPreferencesRepository
import com.example.ui.crash.CrashReportScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.CrashReportViewModel
import com.example.ui.viewmodel.ThemeViewModel

/**
 * La pantalla del informe de fallo (spec 020).
 *
 * **`Activity` propia** y no una pantalla más del `Navigator` (decisión 1 del plan): la abre una notificación
 * sin que `MainActivity` esté viva, y el informe no lleva datos del usuario, así que **no** debe quedar
 * bloqueado detrás de la contraseña maestra. Por construcción no toca `NoteDatabase` ni `NotesViewModel`, que es
 * lo que garantiza el caso límite 9.
 */
class CrashReportActivity : ComponentActivity() {

    private val crashViewModel: CrashReportViewModel by viewModels()

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

        setContent {
            val themeViewModel: ThemeViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return ThemeViewModel(SharedPreferencesRepository(applicationContext)) as T
                    }
                }
            )
            val darkModeOption by themeViewModel.darkModeOption.collectAsStateWithLifecycle()
            val isDynamicColor by themeViewModel.isDynamicColor.collectAsStateWithLifecycle()
            val isDark = when (darkModeOption) {
                DarkModeOption.SYSTEM -> isSystemInDarkTheme()
                DarkModeOption.ON -> true
                DarkModeOption.OFF -> false
            }
            val estado by crashViewModel.informe.collectAsStateWithLifecycle()
            var confirmarDescarte by remember { mutableStateOf(false) }

            MyApplicationTheme(darkTheme = isDark, dynamicColor = isDynamicColor) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BackHandler { finish() }
                    CrashReportScreen(
                        estado = estado,
                        onCerrar = { finish() },
                        onCopiar = { copiar(estado.texto) },
                        onCompartir = { compartir(estado.texto) },
                        onEnviar = { enviar(estado.texto) },
                        onDescartar = { confirmarDescarte = true }
                    )
                    if (confirmarDescarte) {
                        DialogoDeDescarte(
                            onConfirmar = {
                                confirmarDescarte = false
                                crashViewModel.consumir { finish() }
                            },
                            onCancelar = { confirmarDescarte = false }
                        )
                    }
                }
            }
        }
    }

    /**
     * Copia **sin consumir**: el paso siguiente del usuario es mandarlo, y consumir aquí lo tiraría por el
     * motivo equivocado (decisión 9 del plan).
     */
    private fun copiar(texto: String) {
        val clipboard = getSystemService(ClipboardManager::class.java) ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.crash_report_title), texto))
        Toast.makeText(this, R.string.crash_copied, Toast.LENGTH_SHORT).show()
    }

    /** Comparte y consume: el informe ya ha salido de la app, así que el aviso de él ya no tiene sentido. */
    private fun compartir(texto: String) {
        crashViewModel.consumir {
            val enviar = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crash_report_title))
                putExtra(Intent.EXTRA_TEXT, texto)
            }
            startActivity(Intent.createChooser(enviar, getString(R.string.crash_share)))
        }
    }

    /** Abre la incidencia de GitHub con el informe dentro y consume el informe local (RF-121, RF-122). */
    private fun enviar(texto: String) {
        crashViewModel.consumir {
            val url = CrashIssueUrl.url(
                titulo = CrashIssueUrl.titulo(texto.lineSequence().firstOrNull().orEmpty()),
                cuerpo = CrashIssueUrl.cuerpo(texto)
            )
            runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                .onFailure { error ->
                    // Sin navegador el informe sigue en pantalla y se puede copiar igual (caso límite 7).
                    Log.e(TAG, "no se pudo abrir GitHub", error)
                    Toast.makeText(this, R.string.crash_no_browser, Toast.LENGTH_SHORT).show()
                }
        }
    }

    @androidx.compose.runtime.Composable
    private fun DialogoDeDescarte(onConfirmar: () -> Unit, onCancelar: () -> Unit) {
        AlertDialog(
            onDismissRequest = onCancelar,
            title = { Text(stringResource(R.string.crash_discard_title)) },
            text = { Text(stringResource(R.string.crash_discard_text)) },
            confirmButton = {
                TextButton(onClick = onConfirmar) { Text(stringResource(R.string.crash_discard)) }
            },
            dismissButton = {
                TextButton(onClick = onCancelar) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    private companion object {
        const val TAG = "CrashReportActivity"
    }
}