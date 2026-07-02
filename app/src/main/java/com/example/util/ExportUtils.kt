package com.example.util

import android.content.Context
import android.content.Intent
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
import com.example.R
import androidx.core.content.FileProvider
import com.example.data.model.Note
import com.example.data.model.cleanedTags
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportUtils {

    fun exportToMarkdown(context: Context, note: Note, decryptedTitle: String, decryptedContent: String) {
        try {
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(note.lastModified))
            val sb = StringBuilder()
            sb.append("# ").append(decryptedTitle).append("\n\n")
            sb.append("**Last Modified:** ").append(dateStr).append("\n")
            
            val cleanedTags = note.cleanedTags()
            if (cleanedTags.isNotEmpty()) {
                sb.append("**Tags:** ").append(cleanedTags.joinToString(", ")).append("\n")
            }
            sb.append("\n---\n\n")
            sb.append(RichTextParser.convertToMarkdown(decryptedContent)).append("\n")

            val fileName = "Note_${note.id}_" + decryptedTitle.replace("[^a-zA-Z0-9]".toRegex(), "_") + ".md"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { out ->
                out.write(sb.toString().toByteArray())
            }

            shareFile(context, file, "text/markdown", "Export Markdown")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, context.getString(R.string.toast_export_error, e.localizedMessage), Toast.LENGTH_LONG).show()
        }
    }

    fun exportToPdf(context: Context, note: Note, decryptedTitle: String, decryptedContent: String) {
        try {
            val pdfDocument = PdfDocument()
            // Standard A4 Size: 595 x 842 points
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val margin = 50f
            var currentY = margin

            // Draw Header Card border using Material Outlined aesthetic
            val outlinePaint = Paint().apply {
                color = Color.DKGRAY
                style = Paint.Style.STROKE
                strokeWidth = 2f
                isAntiAlias = true
            }
            val fillPaint = Paint().apply {
                color = Color.parseColor("#FAFAFA")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(margin - 10, margin - 10, 595f - margin + 10, 180f, 12f, 12f, fillPaint)
            canvas.drawRoundRect(margin - 10, margin - 10, 595f - margin + 10, 180f, 12f, 12f, outlinePaint)

            // Draw Title inside Header
            val titlePaint = TextPaint().apply {
                color = Color.BLACK
                textSize = 24f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            
            val infoPaint = Paint().apply {
                color = Color.GRAY
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                isAntiAlias = true
            }

            canvas.drawText(
                if (decryptedTitle.length > 32) decryptedTitle.take(30) + "..." else decryptedTitle,
                margin + 10,
                margin + 35,
                titlePaint
            )

            // Draw Status / Security indicator
            val securityPaint = Paint().apply {
                color = Color.parseColor("#43A047") // Green color for encryption success
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            if (note.isEncrypted) {
                canvas.drawText("🛡️ AES-256 END-TO-END ENCRYPTED", margin + 10, margin + 60, securityPaint)
            } else {
                canvas.drawText("📝 PLAIN LOCAL COPY", margin + 10, margin + 60, Paint().apply {
                    color = Color.parseColor("#E65100")
                    textSize = 10f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                })
            }

            // Draw date & tags
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(note.lastModified))
            canvas.drawText("Last Modified: $dateStr", margin + 10, margin + 85, infoPaint)

            val cleanedTags = note.cleanedTags()
            if (cleanedTags.isNotEmpty()) {
                canvas.drawText("Tags: " + cleanedTags.joinToString(", "), margin + 10, margin + 105, infoPaint)
            }

            // Draw note Content (supporting multi-line and text wrapping!)
            currentY = 210f
            val contentPaint = TextPaint().apply {
                color = Color.BLACK
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
            }

            val maxWidth = (595 - (margin * 2)).toInt()
            val plainContent = RichTextParser.stripTags(decryptedContent)
            val staticLayout = StaticLayout.Builder.obtain(plainContent, 0, plainContent.length, contentPaint, maxWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(2f, 1.1f)
                .build()

            canvas.save()
            canvas.translate(margin, currentY)
            staticLayout.draw(canvas)
            canvas.restore()

            pdfDocument.finishPage(page)

            val fileName = "Note_${note.id}_" + decryptedTitle.replace("[^a-zA-Z0-9]".toRegex(), "_") + ".pdf"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            shareFile(context, file, "application/pdf", "Export PDF")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, context.getString(R.string.toast_export_error, e.localizedMessage), Toast.LENGTH_LONG).show()
        }
    }

    private fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val authority = "${context.packageName}.fileprovider"
        val uri: Uri = FileProvider.getUriForFile(context, authority, file)
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        val chooser = Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun exportMultipleToTxt(context: Context, notes: List<com.example.ui.viewmodel.DecryptedNote>) {
        try {
            val sb = StringBuilder()
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            notes.forEachIndexed { index, dec ->
                val dateStr = format.format(Date(dec.note.lastModified))
                sb.append("=== ").append(dec.title.uppercase(Locale.getDefault())).append(" ===\n")
                sb.append("Modified: ").append(dateStr).append("\n")
                
                val cleanedTags = dec.note.cleanedTags()
                if (cleanedTags.isNotEmpty()) {
                    sb.append("Tags: ").append(cleanedTags.joinToString(", ")).append("\n")
                }
                sb.append("\n")
                sb.append(RichTextParser.stripTags(dec.content)).append("\n\n")
                if (index < notes.size - 1) {
                    sb.append("----------------------------\n\n")
                }
            }

            val file = File(context.cacheDir, "Exported_Notes.txt")
            FileOutputStream(file).use { out ->
                out.write(sb.toString().toByteArray())
            }
            shareFile(context, file, "text/plain", "Share Notes as Text")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, context.getString(R.string.toast_export_error, e.localizedMessage), Toast.LENGTH_LONG).show()
        }
    }

    fun exportMultipleToMarkdown(context: Context, notes: List<com.example.ui.viewmodel.DecryptedNote>) {
        try {
            val sb = StringBuilder()
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            notes.forEachIndexed { index, dec ->
                val dateStr = format.format(Date(dec.note.lastModified))
                sb.append("# ").append(dec.title).append("\n\n")
                sb.append("**Last Modified:** ").append(dateStr).append("\n")
                
                val cleanedTags = dec.note.cleanedTags()
                if (cleanedTags.isNotEmpty()) {
                    sb.append("**Tags:** ").append(cleanedTags.joinToString(", ")).append("\n")
                }
                sb.append("\n---\n\n")
                sb.append(RichTextParser.convertToMarkdown(dec.content)).append("\n\n")
                if (index < notes.size - 1) {
                    sb.append("\n\n---\n\n")
                }
            }

            val file = File(context.cacheDir, "Exported_Notes.md")
            FileOutputStream(file).use { out ->
                out.write(sb.toString().toByteArray())
            }
            shareFile(context, file, "text/markdown", "Share Notes as Markdown")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, context.getString(R.string.toast_export_error, e.localizedMessage), Toast.LENGTH_LONG).show()
        }
    }

    fun exportMultipleToHtml(context: Context, notes: List<com.example.ui.viewmodel.DecryptedNote>) {
        try {
            val sb = StringBuilder()
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            sb.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\"><style>")
            sb.append("body { font-family: sans-serif; padding: 20px; line-height: 1.6; background-color: #f9f9f9; color: #333; }")
            sb.append(".note { border: 1px solid #ddd; background: #fff; padding: 20px; border-radius: 8px; margin-bottom: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.05); }")
            sb.append("h1 { margin-top: 0; color: #111; }")
            sb.append(".meta { color: #666; font-size: 0.9em; margin-bottom: 15px; }")
            sb.append(".content { white-space: pre-wrap; }")
            sb.append("</style></head><body>")
            
            notes.forEach { dec ->
                val dateStr = format.format(Date(dec.note.lastModified))
                sb.append("<div class=\"note\">")
                sb.append("<h1>").append(dec.title).append("</h1>")
                sb.append("<div class=\"meta\">Last Modified: ").append(dateStr)
                
                val cleanedTags = dec.note.cleanedTags()
                if (cleanedTags.isNotEmpty()) {
                    sb.append(" | Tags: ").append(cleanedTags.joinToString(", "))
                }
                sb.append("</div>")
                sb.append("<div class=\"content\">").append(RichTextParser.convertToHtml(dec.content)).append("</div>")
                sb.append("</div>")
            }
            sb.append("</body></html>")

            val file = File(context.cacheDir, "Exported_Notes.html")
            FileOutputStream(file).use { out ->
                out.write(sb.toString().toByteArray())
            }
            shareFile(context, file, "text/html", "Share Notes as HTML")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, context.getString(R.string.toast_export_error, e.localizedMessage), Toast.LENGTH_LONG).show()
        }
    }

    fun exportMultipleToJson(context: Context, notes: List<com.example.ui.viewmodel.DecryptedNote>) {
        try {
            val arr = org.json.JSONArray()
            notes.forEach { dec ->
                val obj = org.json.JSONObject()
                obj.put("title", dec.title)
                obj.put("content", dec.content)
                obj.put("lastModified", dec.note.lastModified)
                obj.put("isEncrypted", dec.note.isEncrypted)
                
                val cleanedTags = dec.note.cleanedTags()
                val tagsArr = org.json.JSONArray()
                cleanedTags.forEach { tagsArr.put(it) }
                obj.put("tags", tagsArr)
                
                arr.put(obj)
            }

            val file = File(context.cacheDir, "Exported_Notes.json")
            FileOutputStream(file).use { out ->
                out.write(arr.toString(4).toByteArray())
            }
            shareFile(context, file, "application/json", "Share Notes as JSON")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, context.getString(R.string.toast_export_error, e.localizedMessage), Toast.LENGTH_LONG).show()
        }
    }

    fun exportSingleNoteToJson(context: Context, note: Note, decryptedTitle: String, decryptedContent: String) {
        try {
            val obj = org.json.JSONObject()
            obj.put("id", note.id)
            obj.put("title", decryptedTitle)
            obj.put("summary", decryptedContent)
            obj.put("lastModified", note.lastModified)
            obj.put("isEncrypted", note.isEncrypted)
            obj.put("backgroundColor", note.backgroundColor)
            obj.put("backgroundImagePath", note.backgroundImagePath)
            obj.put("isArchived", note.isArchived)
            obj.put("isFavorite", note.isFavorite)
            obj.put("categoryId", note.categoryId)
            obj.put("isPinned", note.isPinned)
            
            val cleanedTags = note.cleanedTags()
            val tagsArr = org.json.JSONArray()
            cleanedTags.forEach { tagsArr.put(it) }
            obj.put("tags", tagsArr)

            val fileName = "Note_${note.id}_" + decryptedTitle.replace("[^a-zA-Z0-9]".toRegex(), "_") + ".json"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { out ->
                out.write(obj.toString(4).toByteArray())
            }
            shareFile(context, file, "application/json", "Export JSON")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, context.getString(R.string.toast_export_error, e.localizedMessage), Toast.LENGTH_LONG).show()
        }
    }

    fun exportMultipleToPdf(context: Context, notes: List<com.example.ui.viewmodel.DecryptedNote>) {
        try {
            val pdfDocument = PdfDocument()
            val paint = Paint().apply {
                color = Color.BLACK
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
            }
            val titlePaint = TextPaint().apply {
                color = Color.BLACK
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val infoPaint = Paint().apply {
                color = Color.GRAY
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                isAntiAlias = true
            }

            val margin = 50f
            val pageWidth = 595
            val pageHeight = 842
            val drawableWidth = pageWidth - (margin * 2).toInt()

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas
            var currentY = margin

            val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

            notes.forEach { dec ->
                if (currentY > pageHeight - 150) {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    currentY = margin
                }

                if (currentY > margin + 10) {
                    val linePaint = Paint().apply {
                        color = Color.LTGRAY
                        strokeWidth = 1f
                    }
                    canvas.drawLine(margin, currentY, pageWidth - margin, currentY, linePaint)
                    currentY += 20f
                }

                val titleLayout = StaticLayout.Builder.obtain(dec.title, 0, dec.title.length, titlePaint, drawableWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .build()
                canvas.save()
                canvas.translate(margin, currentY)
                titleLayout.draw(canvas)
                canvas.restore()
                currentY += titleLayout.height + 8f

                val dateStr = format.format(Date(dec.note.lastModified))
                val cleanedTags = dec.note.cleanedTags()
                val metaText = "Last Modified: $dateStr" + if (cleanedTags.isNotEmpty()) " | Tags: " + cleanedTags.joinToString(", ") else ""
                
                canvas.drawText(metaText, margin, currentY, infoPaint)
                currentY += 20f

                val contentPaint = TextPaint().apply {
                    color = Color.BLACK
                    textSize = 11f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    isAntiAlias = true
                }

                val plainContent = RichTextParser.stripTags(dec.content)
                val contentLayout = StaticLayout.Builder.obtain(plainContent, 0, plainContent.length, contentPaint, drawableWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(2f, 1.1f)
                    .build()

                val totalLines = contentLayout.lineCount
                for (i in 0 until totalLines) {
                    val lineY = contentLayout.getLineBottom(i) - contentLayout.getLineTop(i)
                    if (currentY + lineY > pageHeight - margin) {
                        pdfDocument.finishPage(page)
                        pageNumber++
                        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas
                        currentY = margin
                    }
                    
                    val startIdx = contentLayout.getLineStart(i)
                    val endIdx = contentLayout.getLineEnd(i)
                    val lineStr = plainContent.substring(startIdx, endIdx)
                    canvas.drawText(lineStr, margin, currentY + lineY - 2, contentPaint)
                    currentY += lineY
                }
                currentY += 25f
            }

            pdfDocument.finishPage(page)

            val file = File(context.cacheDir, "Exported_Notes.pdf")
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            shareFile(context, file, "application/pdf", "Share Notes as PDF")
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, context.getString(R.string.toast_export_error, e.localizedMessage), Toast.LENGTH_LONG).show()
        }
    }
}
