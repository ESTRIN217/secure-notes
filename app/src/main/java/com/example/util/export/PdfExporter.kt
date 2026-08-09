package com.example.util.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.DecryptedNote
import com.example.data.model.cleanedTags
import com.example.util.Exporter
import com.example.util.RichTextConverter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfExporter : Exporter {
    override val formatKey = "PDF"

    override fun export(context: Context, notes: List<DecryptedNote>) {
        if (notes.size == 1) {
            exportSingle(context, notes[0])
        } else {
            exportMultiple(context, notes)
        }
    }

    private fun exportSingle(context: Context, dec: DecryptedNote) {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas
            val margin = 50f

            val outlinePaint = Paint().apply {
                color = Color.DKGRAY; style = Paint.Style.STROKE; strokeWidth = 2f; isAntiAlias = true
            }
            val fillPaint = Paint().apply {
                color = Color.parseColor("#FAFAFA"); style = Paint.Style.FILL
            }
            canvas.drawRoundRect(margin - 10, margin - 10, 595f - margin + 10, 180f, 12f, 12f, fillPaint)
            canvas.drawRoundRect(margin - 10, margin - 10, 595f - margin + 10, 180f, 12f, 12f, outlinePaint)

            val titlePaint = TextPaint().apply {
                color = Color.BLACK; textSize = 24f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true
            }
            val infoPaint = Paint().apply {
                color = Color.GRAY; textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC); isAntiAlias = true
            }
            canvas.drawText(if (dec.title.length > 32) dec.title.take(30) + "..." else dec.title, margin + 10, margin + 35, titlePaint)

            val securityPaint = Paint().apply {
                color = Color.parseColor("#43A047"); textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true
            }
            if (dec.note.isEncrypted) {
                canvas.drawText(context.getString(R.string.pdf_watermark_encrypted), margin + 10, margin + 60, securityPaint)
            } else {
                canvas.drawText(context.getString(R.string.pdf_watermark_plain), margin + 10, margin + 60, Paint().apply {
                    color = Color.parseColor("#E65100"); textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true
                })
            }

            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(dec.note.lastModified))
            canvas.drawText(context.getString(R.string.export_label_last_modified, dateStr), margin + 10, margin + 85, infoPaint)
                val tags = dec.note.cleanedTags()
                if (tags.isNotEmpty()) {
                    canvas.drawText(context.getString(R.string.export_label_tags, tags.joinToString(", ")), margin + 10, margin + 105, infoPaint)
            }

            val contentPaint = TextPaint().apply {
                color = Color.BLACK; textSize = 13f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL); isAntiAlias = true
            }
            val maxWidth = (595 - (margin * 2)).toInt()
            val plainContent = RichTextConverter.contentToPlainText(dec.content)
            val staticLayout = StaticLayout.Builder.obtain(plainContent, 0, plainContent.length, contentPaint, maxWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(2f, 1.1f).build()
            canvas.save(); canvas.translate(margin, 210f); staticLayout.draw(canvas); canvas.restore()

            pdfDocument.finishPage(page)
            val fileName = "Note_${dec.note.id}_" + dec.title.replace("[^a-zA-Z0-9]".toRegex(), "_") + ".pdf"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { out -> pdfDocument.writeTo(out) }
            pdfDocument.close()
            shareFile(context, file, "application/pdf", context.getString(R.string.export_title_pdf))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, context.getString(R.string.toast_export_error, e.localizedMessage), Toast.LENGTH_LONG).show()
        }
    }

    private fun exportMultiple(context: Context, notes: List<DecryptedNote>) {
        try {
            val pdfDocument = PdfDocument()
            val paint = Paint().apply {
                color = Color.BLACK; textSize = 12f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL); isAntiAlias = true
            }
            val titlePaint = TextPaint().apply {
                color = Color.BLACK; textSize = 18f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true
            }
            val infoPaint = Paint().apply {
                color = Color.GRAY; textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC); isAntiAlias = true
            }
            val margin = 50f; val pageWidth = 595; val pageHeight = 842; val drawableWidth = pageWidth - (margin * 2).toInt()

            var pageNumber = 1; var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo); var canvas = page.canvas; var currentY = margin
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

            for (dec in notes) {
                if (currentY > pageHeight - 150) {
                    pdfDocument.finishPage(page); pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo); canvas = page.canvas; currentY = margin
                }
                if (currentY > margin + 10) {
                    val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }
                    canvas.drawLine(margin, currentY, pageWidth - margin, currentY, linePaint); currentY += 20f
                }
                val titleLayout = StaticLayout.Builder.obtain(dec.title, 0, dec.title.length, titlePaint, drawableWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
                canvas.save(); canvas.translate(margin, currentY); titleLayout.draw(canvas); canvas.restore()
                currentY += titleLayout.height + 8f

                val dateStr = format.format(Date(dec.note.lastModified))
                val pdfTags = dec.note.cleanedTags()
                val metaText = context.getString(R.string.export_label_last_modified, dateStr) +
                        if (pdfTags.isNotEmpty()) " | " + context.getString(R.string.export_label_tags, pdfTags.joinToString(", ")) else ""
                canvas.drawText(metaText, margin, currentY, infoPaint); currentY += 20f

                val contentPaint = TextPaint().apply {
                    color = Color.BLACK; textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL); isAntiAlias = true
                }
                val plainContent = RichTextConverter.contentToPlainText(dec.content)
                val contentLayout = StaticLayout.Builder.obtain(plainContent, 0, plainContent.length, contentPaint, drawableWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(2f, 1.1f).build()
                val totalLines = contentLayout.lineCount
                for (i in 0 until totalLines) {
                    val lineY = contentLayout.getLineBottom(i) - contentLayout.getLineTop(i)
                    if (currentY + lineY > pageHeight - margin) {
                        pdfDocument.finishPage(page); pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                        page = pdfDocument.startPage(pageInfo); canvas = page.canvas; currentY = margin
                    }
                    val startIdx = contentLayout.getLineStart(i); val endIdx = contentLayout.getLineEnd(i)
                    canvas.drawText(plainContent.substring(startIdx, endIdx), margin, currentY + lineY - 2, contentPaint)
                    currentY += lineY
                }
                currentY += 25f
            }
            pdfDocument.finishPage(page)
            val file = File(context.cacheDir, "Exported_Notes.pdf")
            FileOutputStream(file).use { out -> pdfDocument.writeTo(out) }; pdfDocument.close()
            shareFile(context, file, "application/pdf", "Share Notes as PDF")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, context.getString(R.string.toast_export_error, e.localizedMessage), Toast.LENGTH_LONG).show()
        }
    }

    private fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val authority = "${context.packageName}.fileprovider"
        val uri: Uri = FileProvider.getUriForFile(context, authority, file)
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, file.name)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, title).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    companion object {
        val instance = PdfExporter()
    }
}
