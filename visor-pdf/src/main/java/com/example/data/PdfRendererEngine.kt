package com.estrin217.pdfviewer.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

class PdfRendererEngine(private val context: Context) {

    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
    private var tempFile: File? = null
    private val renderMutex = Mutex()

    // Cache up to 16 rendered pages in memory
    private val bitmapCache = object : LruCache<Int, Bitmap>(16) {
        override fun entryRemoved(evicted: Boolean, key: Int?, oldValue: Bitmap?, newValue: Bitmap?) {
            // Let the GC recycle bitmaps naturally to avoid recycling while drawing
        }
    }

    var pageDimensions: List<PageDimension> = emptyList()
        private set

    var pageCount: Int = 0
        private set

    var fileInfo: PdfFileInfo? = null
        private set

    suspend fun open(uri: Uri): PdfFileInfo = withContext(Dispatchers.IO) {
        close()

        val displayName = resolveDisplayName(uri)
        var sizeBytes = 0L

        // Attempt direct open or fallback to copying stream into a local cache file.
        // Copying guarantees a seekable file on local disk which PdfRenderer strictly requires.
        val cacheFile = File(context.cacheDir, "current_view_doc.pdf")
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(cacheFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                        sizeBytes += read
                    }
                    outputStream.flush()
                }
            } ?: throw IllegalStateException("No se pudo leer el archivo seleccionado")

            tempFile = cacheFile
            val pfd = ParcelFileDescriptor.open(cacheFile, ParcelFileDescriptor.MODE_READ_ONLY)
            fileDescriptor = pfd
            val renderer = PdfRenderer(pfd)
            pdfRenderer = renderer
            pageCount = renderer.pageCount

            val dimensions = ArrayList<PageDimension>(pageCount)
            for (i in 0 until pageCount) {
                renderMutex.withLock {
                    val page = renderer.openPage(i)
                    dimensions.add(PageDimension(page.width, page.height))
                    page.close()
                }
            }
            pageDimensions = dimensions

            val sizeFormatted = formatFileSize(max(sizeBytes, cacheFile.length()))
            val info = PdfFileInfo(
                name = displayName,
                sizeFormatted = sizeFormatted,
                pageCount = pageCount,
                uriString = uri.toString()
            )
            fileInfo = info
            info
        } catch (e: Exception) {
            close()
            throw e
        }
    }

    suspend fun renderPage(pageIndex: Int, targetWidthPx: Int): Bitmap? = withContext(Dispatchers.IO) {
        if (pageIndex !in 0 until pageCount) return@withContext null

        val cached = bitmapCache.get(pageIndex)
        if (cached != null && !cached.isRecycled && cached.width >= targetWidthPx * 0.8f) {
            return@withContext cached
        }

        renderMutex.withLock {
            val renderer = pdfRenderer ?: return@withContext null
            val page = renderer.openPage(pageIndex)
            val origWidth = page.width
            val origHeight = page.height

            val safeTargetWidth = max(targetWidthPx, 100)
            val scale = safeTargetWidth.toFloat() / origWidth.toFloat()
            val targetHeightPx = (origHeight * scale).toInt().coerceAtLeast(100)

            // Clamp max dimension to 3000px to avoid OutOfMemoryError
            val clampedWidth = min(safeTargetWidth, 2400)
            val clampedHeight = min(targetHeightPx, 3500)

            val bitmap = Bitmap.createBitmap(clampedWidth, clampedHeight, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            bitmapCache.put(pageIndex, bitmap)
            bitmap
        }
    }

    fun getCachedPage(pageIndex: Int): Bitmap? {
        return bitmapCache.get(pageIndex)
    }

    fun close() {
        bitmapCache.evictAll()
        try {
            pdfRenderer?.close()
        } catch (_: Exception) {}
        pdfRenderer = null

        try {
            fileDescriptor?.close()
        } catch (_: Exception) {}
        fileDescriptor = null

        tempFile?.let {
            if (it.exists()) it.delete()
        }
        tempFile = null
        pageDimensions = emptyList()
        pageCount = 0
        fileInfo = null
    }

    private fun resolveDisplayName(uri: Uri): String {
        var name = uri.lastPathSegment ?: "documento.pdf"
        if (uri.scheme == "content") {
            val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
            try {
                context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val colIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (colIndex != -1) {
                            val resolved = cursor.getString(colIndex)
                            if (!resolved.isNullOrBlank()) {
                                name = resolved
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        if (!name.endsWith(".pdf", ignoreCase = true)) {
            name = "$name.pdf"
        }
        return name
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 KB"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB", mb)
            else -> String.format(java.util.Locale.US, "%.0f KB", kb)
        }
    }
}
