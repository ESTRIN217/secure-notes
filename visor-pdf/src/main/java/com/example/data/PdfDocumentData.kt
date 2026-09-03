package com.estrin217.pdfviewer.data

data class PdfFileInfo(
    val name: String,
    val sizeFormatted: String,
    val pageCount: Int,
    val uriString: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class PageDimension(
    val width: Int,
    val height: Int
)
