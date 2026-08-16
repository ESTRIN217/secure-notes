package com.example.util.export

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.SubscriptSpan
import android.text.style.SuperscriptSpan
import android.text.style.TypefaceSpan
import android.text.style.UnderlineSpan
import android.text.style.URLSpan
import com.example.data.model.BlockType
import com.example.data.model.DataBlock
import com.example.data.model.TableData
import com.example.data.model.TextBaseline
import com.example.data.model.TextSegment
import com.example.util.RichTextConverter

/** Dibuja bloques de nota sobre el canvas de un PdfDocument conservando formato enriquecido y media. */
class PdfBlockRenderer {

    private var numberedCounter = 0
    private var lastMedia: Pair<DataBlock, Bitmap?>? = null

    fun reset() {
        numberedCounter = 0
        lastMedia = null
    }

    fun measureBlock(block: DataBlock, embedder: HtmlMediaEmbedder, maxWidth: Int): Float =
        renderBlock(null, block, embedder, 0f, 0f, maxWidth)

    fun drawBlock(
        canvas: Canvas,
        block: DataBlock,
        embedder: HtmlMediaEmbedder,
        x: Float,
        y: Float,
        maxWidth: Int
    ): Float = renderBlock(canvas, block, embedder, x, y, maxWidth)

    private fun renderBlock(
        canvas: Canvas?,
        block: DataBlock,
        embedder: HtmlMediaEmbedder,
        x: Float,
        y: Float,
        maxWidth: Int
    ): Float {
        return when (block.type) {
            BlockType.TEXT -> drawTextBlock(canvas, block, x, y, maxWidth, textPaint(TEXT_SIZE), TEXT_SIZE, 6f)
            BlockType.HEADING1 -> drawTextBlock(canvas, block, x, y, maxWidth, textPaint(22f, bold = true), 22f, 10f)
            BlockType.HEADING2 -> drawTextBlock(canvas, block, x, y, maxWidth, textPaint(19f, bold = true), 19f, 8f)
            BlockType.HEADING3 -> drawTextBlock(canvas, block, x, y, maxWidth, textPaint(16f, bold = true), 16f, 8f)
            BlockType.HEADING4 -> drawTextBlock(canvas, block, x, y, maxWidth, textPaint(14f, bold = true), 14f, 6f)
            BlockType.BULLET_LIST -> drawListItem(canvas, block, x, y, maxWidth, "• ")
            BlockType.NUMBERED_LIST -> {
                if (canvas != null) numberedCounter++
                drawListItem(canvas, block, x, y, maxWidth, "$numberedCounter. ")
            }
            BlockType.CHECKLIST_ITEM -> {
                val mark = if (block.meta["checked"] == "true") "☑ " else "☐ "
                drawListItem(canvas, block, x, y, maxWidth, mark)
            }
            BlockType.QUOTE -> drawQuote(canvas, block, x, y, maxWidth)
            BlockType.CODE_BLOCK -> drawCodeBlock(canvas, block, x, y, maxWidth)
            BlockType.CALLOUT -> drawCallout(canvas, block, x, y, maxWidth)
            BlockType.HORIZONTAL_RULE -> drawRule(canvas, block, x, y, maxWidth)
            BlockType.BOOKMARK -> drawBookmark(canvas, block, x, y, maxWidth)
            BlockType.COLLAPSIBLE -> drawCollapsible(canvas, block, x, y, maxWidth)
            BlockType.TABLE -> drawTable(canvas, block, x, y, maxWidth)
            BlockType.PAGE, BlockType.PAGE_LINK ->
                drawTextBlock(canvas, block, x, y, maxWidth, textPaint(TEXT_SIZE, color = Color.parseColor("#1565C0")), TEXT_SIZE, 4f)
            BlockType.IMAGE, BlockType.DRAWING -> drawMedia(canvas, block, embedder, x, y, maxWidth)
            else -> 0f
        }
    }

    private fun drawTextBlock(
        canvas: Canvas?,
        block: DataBlock,
        x: Float,
        y: Float,
        maxWidth: Int,
        paint: TextPaint,
        baseSize: Float,
        spacing: Float
    ): Float {
        val segments = block.ensureSegments()
        if (segments.isEmpty()) return 0f
        val layout = buildLayout(segmentsToSpanned(segments, baseSize), paint, maxWidth, spacing = true)
        if (canvas != null) {
            canvas.save()
            canvas.translate(x, y)
            layout.draw(canvas)
            canvas.restore()
        }
        return layout.height + spacing
    }

    private fun drawListItem(
        canvas: Canvas?,
        block: DataBlock,
        x: Float,
        y: Float,
        maxWidth: Int,
        prefix: String
    ): Float {
        val segments = block.ensureSegments()
        if (segments.isEmpty()) return 0f
        val indent = 14f
        val layout = buildLayout(segmentsToSpanned(segments, TEXT_SIZE), textPaint(TEXT_SIZE), maxWidth - indent.toInt(), spacing = true)
        if (canvas != null) {
            canvas.drawText(prefix, x, y + layout.getLineBaseline(0), textPaint(TEXT_SIZE))
            canvas.save()
            canvas.translate(x + indent, y)
            layout.draw(canvas)
            canvas.restore()
        }
        return layout.height + 4f
    }

    private fun drawQuote(canvas: Canvas?, block: DataBlock, x: Float, y: Float, maxWidth: Int): Float {
        val segments = block.ensureSegments()
        if (segments.isEmpty()) return 0f
        val indent = 10f
        val layout = buildLayout(
            segmentsToSpanned(segments, TEXT_SIZE),
            textPaint(TEXT_SIZE, italic = true, color = Color.parseColor("#455A64")),
            maxWidth - indent.toInt(),
            spacing = true
        )
        if (canvas != null) {
            canvas.drawRoundRect(x, y, x + 4f, y + layout.height + 2f, 2f, 2f, fillPaint(Color.parseColor("#546E7A")))
            canvas.save()
            canvas.translate(x + indent, y)
            layout.draw(canvas)
            canvas.restore()
        }
        return layout.height + 8f
    }

    private fun drawCodeBlock(canvas: Canvas?, block: DataBlock, x: Float, y: Float, maxWidth: Int): Float {
        val text = RichTextConverter.segmentsToPlainText(block.ensureSegments())
        if (text.isBlank()) return 0f
        val pad = 10f
        val layout = buildLayout(text, textPaint(9.5f).apply { typeface = Typeface.MONOSPACE }, (maxWidth - pad * 2).toInt())
        val height = layout.height + pad * 2 + 6f
        if (canvas != null) {
            canvas.drawRoundRect(x, y, x + maxWidth, y + height - 6f, 6f, 6f, fillPaint(Color.parseColor("#F5F5F5")))
            canvas.save()
            canvas.translate(x + pad, y + pad)
            layout.draw(canvas)
            canvas.restore()
        }
        return height
    }

    private fun drawCallout(canvas: Canvas?, block: DataBlock, x: Float, y: Float, maxWidth: Int): Float {
        val segments = block.ensureSegments()
        if (segments.isEmpty()) return 0f
        val layout = buildLayout(segmentsToSpanned(segments, TEXT_SIZE), textPaint(TEXT_SIZE), maxWidth - 20, spacing = true)
        val height = layout.height + 12f
        if (canvas != null) {
            canvas.drawRoundRect(x, y, x + maxWidth, y + height, 6f, 6f, fillPaint(Color.parseColor("#E8F4FD")))
            canvas.drawRoundRect(x, y, x + 4f, y + height, 2f, 2f, fillPaint(Color.parseColor("#2196F3")))
            canvas.save()
            canvas.translate(x + 12f, y + 6f)
            layout.draw(canvas)
            canvas.restore()
        }
        return height + 6f
    }

    private fun drawRule(canvas: Canvas?, block: DataBlock, x: Float, y: Float, maxWidth: Int): Float {
        if (canvas != null) {
            canvas.drawLine(x, y + 8f, x + maxWidth, y + 8f, strokePaint(Color.LTGRAY))
        }
        return 16f
    }

    private fun drawBookmark(canvas: Canvas?, block: DataBlock, x: Float, y: Float, maxWidth: Int): Float {
        val url = block.content
        val title = block.meta["title"]?.takeIf { it.isNotBlank() } ?: url
        val titleLayout = buildLayout(title, textPaint(TEXT_SIZE, bold = true, color = Color.parseColor("#1565C0")), maxWidth)
        var total = titleLayout.height + 2f
        if (canvas != null) {
            canvas.save()
            canvas.translate(x, y)
            titleLayout.draw(canvas)
            canvas.restore()
        }
        if (title != url) {
            val urlLayout = buildLayout(url, textPaint(9f, color = Color.GRAY), maxWidth)
            if (canvas != null) {
                canvas.save()
                canvas.translate(x, y + titleLayout.height + 2f)
                urlLayout.draw(canvas)
                canvas.restore()
            }
            total += urlLayout.height
        }
        return total + 6f
    }

    private fun drawCollapsible(canvas: Canvas?, block: DataBlock, x: Float, y: Float, maxWidth: Int): Float {
        val summary = block.meta["summary"]?.takeIf { it.isNotBlank() }
        val segments = block.ensureSegments().filter { it.text.isNotBlank() || it.equationLatex != null }
        if (summary.isNullOrBlank() && segments.isEmpty()) return 0f
        var total = 0f
        summary?.let { s ->
            val layout = buildLayout(s, textPaint(TEXT_SIZE, bold = true), maxWidth)
            if (canvas != null) {
                canvas.save()
                canvas.translate(x, y + total)
                layout.draw(canvas)
                canvas.restore()
            }
            total += layout.height + 2f
        }
        if (segments.isNotEmpty()) {
            val layout = buildLayout(segmentsToSpanned(segments, TEXT_SIZE), textPaint(TEXT_SIZE), maxWidth, spacing = true)
            if (canvas != null) {
                canvas.save()
                canvas.translate(x, y + total)
                layout.draw(canvas)
                canvas.restore()
            }
            total += layout.height
        }
        return total + 6f
    }

    private fun drawTable(canvas: Canvas?, block: DataBlock, x: Float, y: Float, maxWidth: Int): Float {
        val data = TableData.fromJson(block.meta["table"]) ?: return 0f
        val colCount = data.columnCount
        if (colCount == 0) return 0f
        val weights = data.normalizedWeights().take(colCount)
        val weightSum = weights.sum().coerceAtLeast(1f)
        val tableWidth = maxWidth.toFloat()
        val colWidths = weights.map { it / weightSum * tableWidth }
        val cellPad = 6f
        val cellPaint = textPaint(9.5f)
        val headerPaint = textPaint(9.5f, bold = true)
        val rowCap = 140f

        var cursorY = y
        if (data.showHeader && data.headers.isNotEmpty()) {
            val cells = data.headers
            val rowH = cells.mapIndexed { i, h ->
                minOf(cellHeight(h, cellPaint, colWidths[i].toInt(), cellPad), rowCap)
            }.maxOrNull() ?: cellPad * 2
            if (canvas != null) {
                canvas.drawRect(x, cursorY, x + tableWidth, cursorY + rowH, fillPaint(Color.parseColor("#F2F2F2")))
                cells.forEachIndexed { i, h ->
                    drawCell(canvas, h, x + colWidths.take(i).sum(), cursorY, colWidths[i], rowH, cellPad, headerPaint)
                }
                drawTableBorders(canvas, x, cursorY, colWidths, rowH)
            }
            cursorY += rowH
        }

        data.rows.forEach { row ->
            val cells = List(colCount) { i -> row.getOrNull(i).orEmpty() }
            val rowH = cells.mapIndexed { i, c ->
                minOf(cellHeight(c, cellPaint, colWidths[i].toInt(), cellPad), rowCap)
            }.maxOrNull() ?: cellPad * 2
            if (canvas != null) {
                cells.forEachIndexed { i, c ->
                    drawCell(canvas, c, x + colWidths.take(i).sum(), cursorY, colWidths[i], rowH, cellPad, cellPaint)
                }
                drawTableBorders(canvas, x, cursorY, colWidths, rowH)
            }
            cursorY += rowH
        }
        return (cursorY - y) + 8f
    }

    private fun cellHeight(text: String, paint: TextPaint, width: Int, pad: Float): Float {
        if (text.isBlank()) return pad * 2
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, (width - pad * 2).toInt().coerceAtLeast(1)).build().height + pad * 2
    }

    private fun drawCell(
        canvas: Canvas,
        text: String,
        cellX: Float,
        cellY: Float,
        width: Float,
        rowH: Float,
        pad: Float,
        paint: TextPaint
    ) {
        if (text.isBlank()) return
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, (width - pad * 2).toInt().coerceAtLeast(1))
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.1f)
            .build()
        canvas.save()
        canvas.clipRect(cellX, cellY, cellX + width, cellY + rowH)
        canvas.translate(cellX + pad, cellY + pad)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun drawTableBorders(canvas: Canvas, x: Float, rowY: Float, colWidths: List<Float>, rowH: Float) {
        val border = strokePaint(Color.parseColor("#DDDDDD"))
        canvas.drawLine(x, rowY, x + colWidths.sum(), rowY, border)
        canvas.drawLine(x, rowY + rowH, x + colWidths.sum(), rowY + rowH, border)
        var colX = x
        for (w in colWidths) {
            canvas.drawLine(colX, rowY, colX, rowY + rowH, border)
            colX += w
        }
    }

    private fun drawMedia(canvas: Canvas?, block: DataBlock, embedder: HtmlMediaEmbedder, x: Float, y: Float, maxWidth: Int): Float {
        if (block.content.isBlank()) return 0f
        val label = if (block.type == BlockType.IMAGE) "Image" else "Drawing"
        val bitmap = decodeMedia(block, embedder)
        if (bitmap == null) return drawMediaFallback(canvas, block, x, y, maxWidth, label)

        val scale = if (bitmap.width > maxWidth) maxWidth.toFloat() / bitmap.width else 1f
        val w = bitmap.width * scale
        val h = bitmap.height * scale
        if (canvas != null) {
            canvas.drawBitmap(bitmap, x + (maxWidth - w) / 2f, y, null)
        }
        var total = h + 6f
        val caption = block.meta["caption"]?.takeIf { it.isNotBlank() }
        if (caption != null) {
            val capLayout = buildLayout(caption, textPaint(9f, italic = true, color = Color.GRAY), maxWidth)
            if (canvas != null) {
                canvas.save()
                canvas.translate(x, y + h + 2f)
                capLayout.draw(canvas)
                canvas.restore()
            }
            total += capLayout.height
        }
        return total
    }

    private fun drawMediaFallback(canvas: Canvas?, block: DataBlock, x: Float, y: Float, maxWidth: Int, label: String): Float {
        val isUrl = block.content.startsWith("http://") || block.content.startsWith("https://")
        val text = if (isUrl) block.content else "[$label]"
        val paint = if (isUrl) textPaint(TEXT_SIZE, color = Color.parseColor("#1565C0")) else textPaint(TEXT_SIZE, color = Color.GRAY)
        val layout = buildLayout(text, paint, maxWidth)
        if (canvas != null) {
            canvas.save()
            canvas.translate(x, y)
            layout.draw(canvas)
            canvas.restore()
        }
        return layout.height + 4f
    }

    private fun decodeMedia(block: DataBlock, embedder: HtmlMediaEmbedder): Bitmap? {
        val cached = lastMedia
        if (cached?.first == block) return cached.second
        val bytes = embedder.resolveBytes(block)
        val bitmap = try {
            if (bytes != null) BitmapFactory.decodeByteArray(bytes, 0, bytes.size) else null
        } catch (e: Exception) {
            null
        }
        lastMedia = block to bitmap
        return bitmap
    }

    private fun buildLayout(source: CharSequence, paint: TextPaint, width: Int, spacing: Boolean = false): StaticLayout {
        val builder = StaticLayout.Builder.obtain(source, 0, source.length, paint, width)
        builder.setAlignment(Layout.Alignment.ALIGN_NORMAL)
        if (spacing) builder.setLineSpacing(2f, 1.1f)
        return builder.build()
    }

    private fun segmentsToSpanned(segments: List<TextSegment>, baseSize: Float): Spanned {
        val sb = SpannableStringBuilder()
        for (seg in segments) {
            val start = sb.length
            sb.append(seg.plainText)
            val end = sb.length
            if (end == start) continue
            if (seg.bold) sb.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (seg.italic) sb.setSpan(StyleSpan(Typeface.ITALIC), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (seg.underline) sb.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (seg.strikethrough) sb.setSpan(StrikethroughSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            if (seg.code) {
                sb.setSpan(TypefaceSpan("monospace"), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                sb.setSpan(BackgroundColorSpan(0x1F808080), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                sb.setSpan(ForegroundColorSpan(0xFFE91E63.toInt()), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else {
                seg.colorHex?.let { parseColorInt(it) }?.let {
                    sb.setSpan(ForegroundColorSpan(it), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                seg.bgColorHex?.let { parseColorInt(it) }?.let {
                    sb.setSpan(BackgroundColorSpan(it), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            }
            seg.fontFamily?.let {
                sb.setSpan(TypefaceSpan(fontName(it)), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            seg.fontSizeSp?.let {
                sb.setSpan(RelativeSizeSpan(it / baseSize), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            when (seg.baseline) {
                TextBaseline.SUBSCRIPT -> sb.setSpan(SubscriptSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                TextBaseline.SUPERSCRIPT -> sb.setSpan(SuperscriptSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                else -> {}
            }
            if (seg.linkUrl != null && !seg.isNoteLink) {
                sb.setSpan(URLSpan(seg.linkUrl), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        return sb
    }

    private fun fontName(name: String): String = when (name.lowercase()) {
        "serif" -> "serif"
        "monospace" -> "monospace"
        "sans-serif" -> "sans-serif"
        "cursive" -> "cursive"
        else -> "sans-serif"
    }

    private fun parseColorInt(hex: String): Int? = try {
        Color.parseColor(hex)
    } catch (e: Exception) {
        null
    }

    private fun textPaint(
        size: Float,
        bold: Boolean = false,
        italic: Boolean = false,
        color: Int = Color.BLACK
    ): TextPaint = TextPaint().apply {
        textSize = size
        isAntiAlias = true
        this.color = color
        typeface = when {
            bold && italic -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
            bold -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            italic -> Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            else -> Typeface.DEFAULT
        }
    }

    private fun fillPaint(color: Int): Paint = Paint().apply {
        this.color = color
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private fun strokePaint(color: Int): Paint = Paint().apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = 1f
        isAntiAlias = true
    }

    companion object {
        const val TEXT_SIZE = 11f

        fun blocksFor(content: String): List<DataBlock> =
            RichTextConverter.contentToBlocks(content) ?: DataBlock.migrateLegacyContent(content)
    }
}
