package com.estrin217.pdfviewer.ui

import android.app.Application
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.ext.SdkExtensions
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.estrin217.pdfviewer.data.PageDimension
import com.estrin217.pdfviewer.data.PdfFileInfo
import com.estrin217.pdfviewer.data.PdfRendererEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed interface PdfViewerUiState {
    data object Empty : PdfViewerUiState
    data class Loading(val message: String = "Cargando documento...") : PdfViewerUiState
    data class Loaded(
        val uri: Uri,
        val fileInfo: PdfFileInfo,
        val pageCount: Int,
        val dimensions: List<PageDimension>
    ) : PdfViewerUiState
    data class Error(val message: String) : PdfViewerUiState
}

class PdfViewerViewModel(application: Application) : AndroidViewModel(application) {

    private val engine = PdfRendererEngine(application.applicationContext)

    private val _uiState = MutableStateFlow<PdfViewerUiState>(PdfViewerUiState.Empty)
    val uiState: StateFlow<PdfViewerUiState> = _uiState.asStateFlow()

    val isAndroidXPdfSupported: Boolean = try {
        if (Build.VERSION.SDK_INT >= 35) {
            SdkExtensions.getExtensionVersion(31) >= 13
        } else if (Build.VERSION.SDK_INT >= 31) {
            SdkExtensions.getExtensionVersion(31) >= 13
        } else {
            false
        }
    } catch (_: Throwable) {
        false
    }

    private val _useAndroidXPdf = MutableStateFlow(isAndroidXPdfSupported)
    val useAndroidXPdf: StateFlow<Boolean> = _useAndroidXPdf.asStateFlow()

    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _zoomScale = MutableStateFlow(1f)
    val zoomScale: StateFlow<Float> = _zoomScale.asStateFlow()

    private val _isNightMode = MutableStateFlow(false)
    val isNightMode: StateFlow<Boolean> = _isNightMode.asStateFlow()

    private val _controlsVisible = MutableStateFlow(true)
    val controlsVisible: StateFlow<Boolean> = _controlsVisible.asStateFlow()

    private val _showJumpDialog = MutableStateFlow(false)
    val showJumpDialog: StateFlow<Boolean> = _showJumpDialog.asStateFlow()

    private val _showInfoDialog = MutableStateFlow(false)
    val showInfoDialog: StateFlow<Boolean> = _showInfoDialog.asStateFlow()


    private var currentUri: Uri? = null

    fun openUri(context: Context, uri: Uri) {
        currentUri = uri
        _uiState.value = PdfViewerUiState.Loading("Abriendo PDF...")
        _currentPage.value = 0
        _zoomScale.value = 1f

        viewModelScope.launch {
            try {
              val fileInfo = engine.open(uri)
                val cacheDoc = File(getApplication<Application>().cacheDir, "current_view_doc.pdf")
                val safeUri = if (cacheDoc.exists() && cacheDoc.length() > 0) {
                    try {
                        FileProvider.getUriForFile(
                            getApplication<Application>(),
                            "${getApplication<Application>().packageName}.fileprovider",
                            cacheDoc
                        )
                    } catch (_: Exception) {
                        uri
                    }
                } else {
                    uri
                }
                _uiState.value = PdfViewerUiState.Loaded(
                    uri = safeUri,
                    fileInfo = fileInfo,
                    pageCount = engine.pageCount,
                    dimensions = engine.pageDimensions
                )
            } catch (e: Exception) {
                _uiState.value = PdfViewerUiState.Error(
                    e.message ?: "No se pudo leer el archivo PDF seleccionado"
                )
            }
        }
    }

    fun toggleViewerEngine(context: Context? = null) {
        if (!isAndroidXPdfSupported && !_useAndroidXPdf.value) {
            context?.let {
                Toast.makeText(
                    it,
                    it.getString(com.estrin217.pdfviewer.R.string.engine_not_supported_notice),
                    Toast.LENGTH_LONG
                ).show()
            }
            return
        }
        _useAndroidXPdf.value = !_useAndroidXPdf.value
    }

    fun fallbackToClassicEngine(context: Context? = null) {
        if (_useAndroidXPdf.value) {
            _useAndroidXPdf.value = false
            context?.let {
                Toast.makeText(
                    it,
                    it.getString(com.estrin217.pdfviewer.R.string.engine_not_supported_notice),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun setUseAndroidXPdf(enabled: Boolean) {
        if (enabled && !isAndroidXPdfSupported) return
        _useAndroidXPdf.value = enabled
    }

    suspend fun renderPage(pageIndex: Int, targetWidthPx: Int): Bitmap? {
        return engine.renderPage(pageIndex, targetWidthPx)
    }

    fun getCachedPage(pageIndex: Int): Bitmap? {
        return engine.getCachedPage(pageIndex)
    }

    fun setCurrentPage(index: Int) {
        _currentPage.value = index
    }

    fun setZoom(scale: Float) {
        _zoomScale.value = scale.coerceIn(1f, 4f)
    }

    fun zoomIn() {
        _zoomScale.value = (_zoomScale.value + 0.5f).coerceAtMost(4f)
    }

    fun zoomOut() {
        _zoomScale.value = (_zoomScale.value - 0.5f).coerceAtLeast(1f)
    }

    fun resetZoom() {
        _zoomScale.value = 1f
    }

    fun toggleNightMode() {
        _isNightMode.value = !_isNightMode.value
    }

    fun toggleControls() {
        _controlsVisible.value = !_controlsVisible.value
    }

    fun setControlsVisible(visible: Boolean) {
        _controlsVisible.value = visible
    }

    fun showJumpToPageDialog(show: Boolean) {
        _showJumpDialog.value = show
    }

    fun showFileInfoDialog(show: Boolean) {
        _showInfoDialog.value = show
    }

    fun shareCurrentPdf(context: Context) {
        if (currentUri == null) return
        try {
            val cacheDoc = File(context.cacheDir, "current_view_doc.pdf")
            val sampleDoc = File(context.cacheDir, "manual_visor_pdf.pdf")
            val targetFile = when {
                cacheDoc.exists() && cacheDoc.length() > 0 -> cacheDoc
                sampleDoc.exists() && sampleDoc.length() > 0 -> sampleDoc
                else -> null
            }

            val shareUri: Uri = if (targetFile != null) {
                // Ensure friendly file name when sharing if available
                val safeName = (engine.fileInfo?.name ?: "documento.pdf")
                    .replace("[^a-zA-Z0-9._-]".toRegex(), "_")
                val exportFile = File(
                    context.cacheDir,
                    if (safeName.endsWith(".pdf", ignoreCase = true)) safeName else "$safeName.pdf"
                )
                if (exportFile.absolutePath != targetFile.absolutePath) {
                    targetFile.copyTo(exportFile, overwrite = true)
                }
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    exportFile
                )
            } else {
                val uri = currentUri ?: return
                if (uri.scheme == "file") {
                    val file = uri.path?.let { File(it) }
                    if (file != null && file.exists()) {
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                    } else uri
                } else uri
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, shareUri)
                clipData = ClipData.newRawUri("PDF", shareUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Compartir PDF").apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "No fue posible compartir el archivo: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun closeDocument() {
        engine.close()
        currentUri = null
        _uiState.value = PdfViewerUiState.Empty
        _currentPage.value = 0
        _zoomScale.value = 1f
    }

    override fun onCleared() {
        super.onCleared()
        engine.close()
    }
}
