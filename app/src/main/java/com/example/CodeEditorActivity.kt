package com.example

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.SharedPreferencesRepository
import com.example.ui.CodeEditorScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ThemeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Standalone code editor activity. Direct target of the SEND/VIEW text
 * intent-filters and openable from the app's FAB / Settings. Lives outside
 * the MainActivity lock flow; content is transient user text, not the vault.
 */
class CodeEditorActivity : ComponentActivity() {

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

            val context = LocalContext.current
            var doc by remember { mutableStateOf(syncDoc(intent)) }
            var requestKey by remember { mutableIntStateOf(0) }

            LaunchedEffect(Unit) {
                intentRelay = { incoming ->
                    doc = syncDoc(incoming)
                    requestKey++
                }
            }

            LaunchedEffect(requestKey) {
                doc = loadUriDoc(context, intent) ?: doc
            }

            MyApplicationTheme(darkTheme = isDark, dynamicColor = isDynamicColor) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    androidx.compose.runtime.key(doc) {
                        CodeEditorScreen(
                            initialName = doc.name,
                            initialContent = doc.content,
                            onBack = { finish() }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intentRelay?.invoke(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        intentRelay = null
    }

    companion object {
        private var intentRelay: ((Intent) -> Unit)? = null
    }
}

private data class Doc(val name: String?, val content: String)

private fun syncDoc(intent: Intent?): Doc {
    if (intent == null) return Doc(null, "")
    return when (intent.action) {
        Intent.ACTION_SEND -> {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (text.isNullOrBlank()) {
                Doc(null, "")
            } else {
                Doc(
                    name = intent.getStringExtra(Intent.EXTRA_SUBJECT),
                    content = text
                )
            }
        }
        else -> Doc(null, "")
    }
}

private suspend fun loadUriDoc(context: Context, intent: Intent?): Doc? {
    val uri = when (intent?.action) {
        Intent.ACTION_SEND -> intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        Intent.ACTION_VIEW -> intent.data
        else -> return null
    } ?: return null

    return withContext(Dispatchers.IO) {
        val name = runCatching {
            context.contentResolver.query(
                uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
            )?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx)?.takeIf { it.isNotBlank() } else null
            }
        }.getOrNull() ?: uri.lastPathSegment ?: "file"
        val text = runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
        }.getOrElse { "" }
        Doc(name = name, content = text)
    }
}
