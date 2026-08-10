package com.example.data.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

data class DrawingStroke(
    val points: List<Offset>,
    val color: Color,
    val width: Float
)

object DrawingStrokeCodec {
    private val strokeRegex = Regex(
        """\{"color":(-?\d+),"width":([0-9.eE+-]+),"points":\[(.*?)\]\}"""
    )
    private val pointRegex = Regex(
        """\{"x":([0-9.eE+-]+),"y":([0-9.eE+-]+)\}"""
    )

    /** Serializa trazos al mismo formato JSON que el canvas legado. */
    fun strokesToJson(strokes: List<DrawingStroke>): String {
        val sb = StringBuilder()
        sb.append('[')
        strokes.forEachIndexed { idx, stroke ->
            if (idx > 0) sb.append(',')
            sb.append("{\"color\":").append(stroke.color.toArgb())
                .append(",\"width\":").append(stroke.width)
                .append(",\"points\":[")
            stroke.points.forEachIndexed { pIdx, pt ->
                if (pIdx > 0) sb.append(',')
                sb.append("{\"x\":").append(pt.x).append(",\"y\":").append(pt.y).append('}')
            }
            sb.append("]}")
        }
        sb.append(']')
        return sb.toString()
    }

    fun strokesFromJson(json: String?): List<DrawingStroke> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            strokeRegex.findAll(json).map { m ->
                val color = Color(m.groupValues[1].toInt())
                val width = m.groupValues[2].toFloat()
                val points = pointRegex.findAll(m.groupValues[3]).map { pm ->
                    Offset(pm.groupValues[1].toFloat(), pm.groupValues[2].toFloat())
                }.toList()
                DrawingStroke(points, color, width)
            }.toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Convierte coordenadas absolutas (píxeles) a normalizadas 0..1. */
    fun normalize(strokes: List<DrawingStroke>, width: Int, height: Int): List<DrawingStroke> {
        if (width <= 0 || height <= 0) return strokes
        val w = width.toFloat()
        val h = height.toFloat()
        return strokes.map { stroke ->
            stroke.copy(points = stroke.points.map { Offset(it.x / w, it.y / h) })
        }
    }

    /** Convierte coordenadas normalizadas 0..1 a píxeles del canvas objetivo. */
    fun denormalize(strokes: List<DrawingStroke>, width: Int, height: Int): List<DrawingStroke> {
        if (width <= 0 || height <= 0) return strokes
        val w = width.toFloat()
        val h = height.toFloat()
        return strokes.map { stroke ->
            stroke.copy(points = stroke.points.map { Offset(it.x * w, it.y * h) })
        }
    }
}
