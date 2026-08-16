package com.example.util.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import com.example.data.model.DrawingStroke
import com.example.data.model.DrawingStrokeCodec
import androidx.compose.ui.graphics.toArgb
import java.io.ByteArrayOutputStream

/** Rasteriza trazos normalizados (0..1) a un PNG para embeber en exportaciones HTML. */
object DrawingPngRenderer {
    private const val WIDTH = 1024
    private const val HEIGHT = 1280

    fun render(strokes: List<DrawingStroke>): ByteArray? {
        if (strokes.isEmpty()) return null
        return try {
            val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)
            val scaled = DrawingStrokeCodec.denormalize(strokes, WIDTH, HEIGHT)
            for (stroke in scaled) {
                val points = stroke.points
                if (points.isEmpty()) continue
                val paint = Paint().apply {
                    isAntiAlias = true
                    color = stroke.color.toArgb()
                    style = if (points.size == 1) Paint.Style.FILL else Paint.Style.STROKE
                    strokeWidth = stroke.width.coerceAtLeast(1f)
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                }
                if (points.size == 1) {
                    canvas.drawCircle(points[0].x, points[0].y, paint.strokeWidth / 2f, paint)
                } else {
                    val path = Path().apply {
                        moveTo(points[0].x, points[0].y)
                        for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
                    }
                    canvas.drawPath(path, paint)
                }
            }
            val out = ByteArrayOutputStream()
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) return null
            bitmap.recycle()
            out.toByteArray()
        } catch (e: Exception) {
            null
        }
    }
}
