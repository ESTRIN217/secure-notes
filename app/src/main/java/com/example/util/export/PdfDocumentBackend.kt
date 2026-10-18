package com.example.util.export

import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import java.io.OutputStream

internal interface PdfDocumentBackend {
    fun startPage(pageInfo: PdfDocument.PageInfo): Canvas
    fun finishPage()
    fun writeTo(output: OutputStream)
    fun close()
}

internal fun interface PdfDocumentFactory {
    fun create(): PdfDocumentBackend
}

internal class AndroidPdfDocumentBackend : PdfDocumentBackend {
    private val document = PdfDocument()
    private var currentPage: PdfDocument.Page? = null

    override fun startPage(pageInfo: PdfDocument.PageInfo): Canvas {
        check(currentPage == null) { "Finish the current PDF page before starting another" }
        return document.startPage(pageInfo).also { currentPage = it }.canvas
    }

    override fun finishPage() {
        val page = checkNotNull(currentPage) { "There is no active PDF page to finish" }
        document.finishPage(page)
        currentPage = null
    }

    override fun writeTo(output: OutputStream) {
        check(currentPage == null) { "Finish the current PDF page before writing the document" }
        document.writeTo(output)
    }

    override fun close() {
        document.close()
    }
}
