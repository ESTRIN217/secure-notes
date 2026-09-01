package com.example

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.pdf.viewer.fragment.PdfViewerFragment

/**
 * Standalone PDF viewer activity. Hosts the Jetpack `androidx.pdf` [PdfViewerFragment],
 * which renders the document inside an isolated sandbox service (framework PdfRenderer).
 * Direct target of the "view PDF" action in the file-open dialog and opened from the
 * file picker; lives outside the MainActivity lock flow (view-only, never stored).
 */
class PdfViewerActivity : AppCompatActivity() {

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

        val uri = intent?.data
        val scheme = uri?.scheme
        if (uri == null || (scheme != ContentResolver.SCHEME_CONTENT && scheme != ContentResolver.SCHEME_FILE)) {
            super.finish()
            return
        }

        enableEdgeToEdge()
        val prefs = getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(AppConstants.SCREENSHOT_ENABLED_KEY, false)) {
            window.setFlags(
                android.view.WindowManager.LayoutParams.FLAG_SECURE,
                android.view.WindowManager.LayoutParams.FLAG_SECURE
            )
        }

        setContentView(R.layout.activity_pdf_viewer)
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.pdfToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        var fragment = supportFragmentManager.findFragmentByTag(TAG_PDF_VIEWER) as? AppPdfViewerFragment
        if (fragment == null) {
            fragment = AppPdfViewerFragment().apply {
                arguments = Bundle().apply { putString(AppPdfViewerFragment.ARG_URI, uri.toString()) }
            }
            supportFragmentManager.beginTransaction()
                .add(R.id.pdf_container, fragment, TAG_PDF_VIEWER)
                .commit()
        }
        fragment.onImmersiveChanged = { enterImmersive ->
            toolbar.visibility = if (enterImmersive) View.GONE else View.VISIBLE
        }
    }

    companion object {
        private const val TAG_PDF_VIEWER = "pdf_viewer_fragment"

        /** Builds an explicit [Intent] to open [uri] (a content:// or file:// PDF). */
        fun intentFor(context: Context, uri: Uri): Intent =
            Intent(context, PdfViewerActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
    }
}

/**
 * Fragment subclass that lets the activity react to the viewer's immersive-mode requests
 * (e.g. hiding the toolbar while the user scrolls deeply through the document).
 */
class AppPdfViewerFragment : PdfViewerFragment() {
    var onImmersiveChanged: ((Boolean) -> Unit)? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.getString(ARG_URI)?.let { documentUri = Uri.parse(it) }
    }

    override fun onRequestImmersiveMode(enterImmersive: Boolean) {
        super.onRequestImmersiveMode(enterImmersive)
        onImmersiveChanged?.invoke(enterImmersive)
    }

    companion object {
        const val ARG_URI = "arg_uri"
    }
}