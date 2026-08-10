package com.example

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.data.model.BlockType
import com.example.data.model.DataBlock
import com.example.data.model.DrawingStroke
import com.example.data.model.DrawingStrokeCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawingStrokeCodecTest {

    @Test
    fun `stroke roundtrip json`() {
        val strokes = listOf(
            DrawingStroke(listOf(Offset(0.1f, 0.2f), Offset(0.3f, 0.4f)), Color(0xFFE53935), 8f),
            DrawingStroke(listOf(Offset(0.5f, 0.5f)), Color.Black, 2f)
        )
        val json = DrawingStrokeCodec.strokesToJson(strokes)
        val back = DrawingStrokeCodec.strokesFromJson(json)
        assertEquals(strokes.size, back.size)
        assertEquals(strokes[0].points, back[0].points)
        assertEquals(strokes[0].color.toArgb(), back[0].color.toArgb())
        assertEquals(strokes[0].width, back[0].width, 0.001f)
        assertEquals(strokes[1].points, back[1].points)
        assertEquals(strokes[1].color.toArgb(), back[1].color.toArgb())
    }

    @Test
    fun `parses legacy org-json payload with integer numbers`() {
        val legacy = """[{"color":-16776961,"width":8,"points":[{"x":10,"y":20},{"x":30,"y":40}]}]"""
        val strokes = DrawingStrokeCodec.strokesFromJson(legacy)
        assertEquals(1, strokes.size)
        assertEquals(2, strokes[0].points.size)
        assertEquals(10f, strokes[0].points[0].x, 0.001f)
        assertEquals(20f, strokes[0].points[0].y, 0.001f)
        assertEquals(8f, strokes[0].width, 0.001f)
    }

    @Test
    fun `normalize and denormalize roundtrip`() {
        val absolute = listOf(
            DrawingStroke(listOf(Offset(50f, 100f), Offset(200f, 400f)), Color.Red, 8f)
        )
        val norm = DrawingStrokeCodec.normalize(absolute, 400, 800)
        assertEquals(0.125f, norm[0].points[0].x, 0.0001f)
        assertEquals(0.125f, norm[0].points[0].y, 0.0001f)
        val back = DrawingStrokeCodec.denormalize(norm, 400, 800)
        assertEquals(50f, back[0].points[0].x, 0.001f)
        assertEquals(100f, back[0].points[0].y, 0.001f)
    }

    @Test
    fun `empty json yields empty strokes`() {
        assertTrue(DrawingStrokeCodec.strokesFromJson(null).isEmpty())
        assertTrue(DrawingStrokeCodec.strokesFromJson("").isEmpty())
        assertTrue(DrawingStrokeCodec.strokesFromJson("not json").isEmpty())
        assertTrue(DrawingStrokeCodec.strokesFromJson("[]").isEmpty())
    }

    @Test
    fun `wysiwyg and legacy drawing flags`() {
        val wysiwyg = DataBlock(type = BlockType.DRAWING, content = "[]", meta = mapOf("wysiwyg" to "true"))
        val legacy = DataBlock(
            type = BlockType.DRAWING,
            content = "/path/drawing.json",
            meta = mapOf("previewPath" to "/path/preview.png")
        )
        val text = DataBlock(type = BlockType.TEXT, content = "hola")
        assertTrue(wysiwyg.isWysiwygDrawing)
        assertFalse(wysiwyg.isLegacyDrawing)
        assertTrue(legacy.isLegacyDrawing)
        assertFalse(legacy.isWysiwygDrawing)
        assertFalse(text.isWysiwygDrawing)
        assertFalse(text.isLegacyDrawing)
    }
}
