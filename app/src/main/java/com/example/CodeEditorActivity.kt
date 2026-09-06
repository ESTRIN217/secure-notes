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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.estrin217.codetools.CodeTools
import com.estrin217.codetools.ui.CodeEditorViewModel
import com.estrin217.codetools.ui.CodeToolsApp
import com.example.data.SharedPreferencesRepository
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ThemeViewModel

/**
 * Thin host for the :code-tools library. Owns the Activity concerns —
 * locale, FLAG_SECURE, app theme and SEND/VIEW intent-filters — and
 * delegates all editor state to the library's [CodeEditorViewModel].
 * Lives outside the MainActivity lock flow; content is transient user
 * text, not the vault.
 */
class CodeEditorActivity : ComponentActivity() {

    private val codeViewModel: CodeEditorViewModel by viewModels {
        CodeTools.viewModelFactory(applicationContext)
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
            val isDynamicColor by themeViewModel.isDynamicColor.collectAsStateWithLifecycle()
            val isDark = when (darkModeOption) {
                DarkModeOption.SYSTEM -> isSystemInDarkTheme()
                DarkModeOption.ON -> true
                DarkModeOption.OFF -> false
            }

            MyApplicationTheme(darkTheme = isDark, dynamicColor = isDynamicColor) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CodeToolsApp(viewModel = codeViewModel)
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
