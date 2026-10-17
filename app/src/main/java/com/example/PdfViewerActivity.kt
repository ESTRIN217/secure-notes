package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.remember
import androidx.fragment.app.FragmentActivity
import com.estrin217.pdfviewer.ui.PdfViewerScreen
import com.estrin217.pdfviewer.ui.PdfViewerViewModel
import com.example.data.SharedPreferencesRepository
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.isDark

class PdfViewerActivity : FragmentActivity() {

    private val viewModel: PdfViewerViewModel by viewModels()

    /**
     * El tema y el modo, leídos una vez (RF-162).
     *
     * Esta pantalla no cambia el tema, así que no necesita un `ViewModel` para enterarse: leer la preferencia al
     * abrir es suficiente y evita que el visor de PDF sea la única pantalla con un `ThemeViewModel` que nunca
     * emite nada.
     */
    private val prefsRepository by lazy { SharedPreferencesRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            val appTheme = remember { prefsRepository.getAppTheme() }
            val darkModeOption = remember { prefsRepository.getDarkModeOption() }
            MyApplicationTheme(appTheme = appTheme, darkTheme = darkModeOption.isDark(isSystemInDarkTheme())) {
                PdfViewerScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val uri = extractPdfUri(intent)
        if (uri != null) {
            viewModel.openUri(this, uri)
        }
    }

    private fun extractPdfUri(intent: Intent): Uri? {
        intent.data?.let { return it }

        val streamUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        }
        if (streamUri != null) return streamUri

        intent.clipData?.let { clipData ->
            if (clipData.itemCount > 0) {
                val itemUri = clipData.getItemAt(0).uri
                if (itemUri != null) return itemUri
            }
        }

        return null
    }

    companion object {
        fun intentFor(context: Context, uri: Uri): Intent {
            return Intent(context, PdfViewerActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }
}
