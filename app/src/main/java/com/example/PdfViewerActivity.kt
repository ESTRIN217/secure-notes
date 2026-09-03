package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import com.estrin217.pdfviewer.ui.PdfViewerScreen
import com.estrin217.pdfviewer.ui.PdfViewerViewModel
import com.example.ui.theme.MyApplicationTheme

class PdfViewerActivity : FragmentActivity() {

    private val viewModel: PdfViewerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            MyApplicationTheme {
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
