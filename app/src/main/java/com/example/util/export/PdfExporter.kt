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
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas: Canvas = page.canvas

            val renderer = PdfBlockRenderer()
            val embedder = HtmlMediaEmbedder(context)
            val blocks = PdfBlockRenderer.blocksFor(dec.content)
            var y = drawHeader(canvas, context, dec)
            var pageNumber = 1
            val maxHeight = (PAGE_HEIGHT - 2 * MARGIN).toInt()
            for (block in blocks) {
                val needed = renderer.measureBlock(block, embedder, PAGE_WIDTH - (MARGIN * 2).toInt(), maxHeight)
                if (needed > 0f && y + needed > PAGE_HEIGHT - MARGIN) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                    canvas = page.canvas
                    y = MARGIN
                }
                y += renderer.drawBlock(canvas, block, embedder, MARGIN, y, PAGE_WIDTH - (MARGIN * 2).toInt(), maxHeight)
            }
            pdfDocument.finishPage(page)

            val fileName = "Note_${dec.note.id}_" + dec.title.replace("[^a-zA-Z0-9]".toRegex(), "_") + ".pdf"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { out -> pdfDocument.writeTo(out) }
            pdfDocument.close()
            shareFile(context, file, "application/pdf", context.getString(R.string.share_title_pdf))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, context.getString(R.string.toast_export_error, e.localizedMessage), Toast.LENGTH_LONG).show()
        }
    }

    private fun exportMultiple(context: Context, notes: List<DecryptedNote>) {
        try {
            val pdfDocument = PdfDocument()
            var pageNumber = 1
            var page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            var canvas: Canvas = page.canvas
            var y = MARGIN
            val renderer = PdfBlockRenderer()
            val embedder = HtmlMediaEmbedder(context)
            val maxWidth = PAGE_WIDTH - (MARGIN * 2).toInt()
            val maxHeight = (PAGE_HEIGHT - 2 * MARGIN).toInt()
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val contentPaint = TextPaint().apply {
                color = Color.BLACK; textSize = 13f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL); isAntiAlias = true
            }

            for (dec in notes) {
                renderer.reset()
                val titleLayout = StaticLayout.Builder.obtain(dec.title, 0, dec.title.length, contentPaint, maxWidth).build()
                if (y > MARGIN + 10) {
                    canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, Paint().apply { color = Color.LTGRAY; strokeWidth = 1f })
                    y += 20f
                }
                if (y + titleLayout.height + 30f > PAGE_HEIGHT - MARGIN) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                    canvas = page.canvas
                    y = MARGIN
                }
                canvas.save(); canvas.translate(MARGIN, y); titleLayout.draw(canvas); canvas.restore()
                y += titleLayout.height + 8f

                val dateStr = format.format(Date(dec.note.lastModified))
                val tags = dec.note.cleanedTags()
                val metaText = context.getString(R.string.export_label_last_modified, dateStr) +
                        if (tags.isNotEmpty()) " | " + context.getString(R.string.export_label_tags, tags.joinToString(", ")) else ""
                canvas.drawText(metaText, MARGIN, y, Paint().apply {
                    color = Color.GRAY; textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC); isAntiAlias = true
                })
                y += 20f

                val blocks = PdfBlockRenderer.blocksFor(dec.content)
                for (block in blocks) {
                    val needed = renderer.measureBlock(block, embedder, maxWidth, maxHeight)
                    if (needed > 0f && y + needed > PAGE_HEIGHT - MARGIN) {
                        pdfDocument.finishPage(page)
                        pageNumber++
                        page = pdfDocument.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                        canvas = page.canvas
                        y = MARGIN
                    }
                    y += renderer.drawBlock(canvas, block, embedder, MARGIN, y, maxWidth, maxHeight)
                }
                y += 25f
            }
            pdfDocument.finishPage(page)

            val file = File(context.cacheDir, "Exported_Notes.pdf")
            FileOutputStream(file).use { out -> pdfDocument.writeTo(out) }
            pdfDocument.close()
            shareFile(context, file, "application/pdf", context.getString(R.string.share_title_pdf))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, context.getString(R.string.toast_export_error, e.localizedMessage), Toast.LENGTH_LONG).show()
        }
    }

    private fun drawHeader(canvas: Canvas, context: Context, dec: DecryptedNote): Float {
        val margin = MARGIN
        val pad = 14f
        val boxLeft = margin - 10f
        val boxRight = PAGE_WIDTH - margin + 10f
        val boxTop = margin - 10f
        val contentWidth = (boxRight - boxLeft - pad * 2).toInt()

        val titlePaint = TextPaint().apply {
            color = Color.BLACK; textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true
        }
        val titleLayout = StaticLayout.Builder.obtain(
            dec.title, 0, dec.title.length, titlePaint, contentWidth
        ).setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(2f, 1.1f).build()

        val infoPaint = TextPaint().apply {
            color = Color.parseColor("#666666"); textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC); isAntiAlias = true
        }
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(dec.note.lastModified))
        val dateText = context.getString(R.string.export_label_last_modified, dateStr)
        val dateLayout = StaticLayout.Builder.obtain(
            dateText, 0, dateText.length, infoPaint, contentWidth
        ).build()

        val tags = dec.note.cleanedTags()
        var tagsLayout: StaticLayout? = null
        if (tags.isNotEmpty()) {
            val tagsText = context.getString(R.string.export_label_tags, tags.joinToString(", "))
            tagsLayout = StaticLayout.Builder.obtain(
                tagsText, 0, tagsText.length, infoPaint, contentWidth
            ).setAlignment(Layout.Alignment.ALIGN_NORMAL).build()
        }

        var cy = pad
        cy += titleLayout.height.toFloat() + 10f
        cy += dateLayout.height.toFloat() + 4f
        if (tagsLayout != null) cy += tagsLayout.height.toFloat()
        val boxBottom = boxTop + cy + pad

        val fillPaint = Paint().apply {
            color = Color.parseColor("#FAFAFA"); style = Paint.Style.FILL; isAntiAlias = true
        }
        val borderPaint = Paint().apply {
            color = Color.parseColor("#E0E0E0"); style = Paint.Style.STROKE; strokeWidth = 1f; isAntiAlias = true
        }
        canvas.drawRoundRect(boxLeft, boxTop, boxRight, boxBottom, 10f, 10f, fillPaint)
        canvas.drawRoundRect(boxLeft, boxTop, boxRight, boxBottom, 10f, 10f, borderPaint)

        val accentPaint = Paint().apply {
            color = Color.parseColor("#1976D2"); style = Paint.Style.FILL; isAntiAlias = true
        }
        canvas.drawRoundRect(boxLeft, boxTop, boxRight, boxTop + 4f, 10f, 10f, accentPaint)
        canvas.drawRect(boxLeft, boxTop + 4f, boxRight, boxTop + 4f, accentPaint)

        var drawY = boxTop + pad
        canvas.save(); canvas.translate(boxLeft + pad, drawY); titleLayout.draw(canvas); canvas.restore()
        drawY += titleLayout.height.toFloat() + 10f

        canvas.save(); canvas.translate(boxLeft + pad, drawY); dateLayout.draw(canvas); canvas.restore()
        drawY += dateLayout.height.toFloat() + 4f

        if (tagsLayout != null) {
            canvas.save(); canvas.translate(boxLeft + pad, drawY); tagsLayout.draw(canvas); canvas.restore()
        }

        return boxBottom + 10f
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
        private const val PAGE_WIDTH = 595
        private const val PAGE_HEIGHT = 842
        private const val MARGIN = 50f

        val instance = PdfExporter()
    }
}
