package com.example

import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.TextUnit
import com.example.data.model.TextBaseline
import com.example.data.model.TextSegment
import com.example.util.RichTextConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RichTextConverterTest {

    private fun assertSameSegment(expected: TextSegment, actual: TextSegment) {
        assertEquals(expected.text, actual.text)
        assertTrue("segmento no coincide: expected=$expected actual=$actual", expected.hasSameStyle(actual))
    }

    @Test
    fun `roundtrip sub and sup`() {
        val segs = listOf(
            TextSegment(text = "H"),
            TextSegment(text = "2", baseline = TextBaseline.SUBSCRIPT),
            TextSegment(text = "O mc"),
            TextSegment(text = "2", baseline = TextBaseline.SUPERSCRIPT)
        )
        val markup = RichTextConverter.segmentsToMarkup(segs)
        val back = RichTextConverter.markupToSegments(markup)
        assertEquals(segs.size, back.size)
        segs.zip(back).forEach { (a, b) -> assertSameSegment(a, b) }
    }

    @Test
    fun `roundtrip font and size`() {
        val segs = listOf(
            TextSegment(text = "Hola ", fontFamily = "serif"),
            TextSegment(text = "grande", fontSizeSp = 20f, bold = true)
        )
        val markup = RichTextConverter.segmentsToMarkup(segs)
        val back = RichTextConverter.markupToSegments(markup)
        assertEquals(segs.size, back.size)
        segs.zip(back).forEach { (a, b) -> assertSameSegment(a, b) }
    }

    @Test
    fun `roundtrip styles b i u s code color bg`() {
        val segs = listOf(
            TextSegment(text = "negrita", bold = true),
            TextSegment(text = "italica", italic = true),
            TextSegment(text = "subrayado", underline = true),
            TextSegment(text = "tachado", strikethrough = true),
            TextSegment(text = "codigo", code = true),
            TextSegment(text = "rojo", colorHex = "#FF0000"),
            TextSegment(text = "fondo", bgColorHex = "#00FF00")
        )
        val markup = RichTextConverter.segmentsToMarkup(segs)
        val back = RichTextConverter.markupToSegments(markup)
        assertEquals(segs.size, back.size)
        segs.zip(back).forEach { (a, b) -> assertSameSegment(a, b) }
    }

    @Test
    fun `markdown special chars are escaped and preserved`() {
        val segs = listOf(TextSegment(text = "*a_b_ `c` ~d~ [e](f)"))
        val markup = RichTextConverter.segmentsToMarkup(segs)
        val back = RichTextConverter.markupToSegments(markup)
        assertEquals(segs.size, back.size)
        assertSameSegment(segs[0], back[0])
    }

    @Test
    fun `applySpanStyle applies sub to selection`() {
        val segs = listOf(TextSegment(text = "H2O"))
        val out = RichTextConverter.applySpanStyle(segs, 1, 2) {
            it.copy(baseline = TextBaseline.SUBSCRIPT)
        }
        assertEquals(3, out.size)
        assertSameSegment(TextSegment("H"), out[0])
        assertSameSegment(TextSegment("2", baseline = TextBaseline.SUBSCRIPT), out[1])
        assertSameSegment(TextSegment("O"), out[2])
    }

    @Test
    fun `applySpanStyle sets font and size on range`() {
        val segs = listOf(TextSegment(text = "abc"))
        val out = RichTextConverter.applySpanStyle(segs, 1, 3) {
            it.copy(fontFamily = "monospace", fontSizeSp = 14f)
        }
        assertEquals(2, out.size)
        assertSameSegment(TextSegment("a"), out[0])
        assertSameSegment(TextSegment("bc", fontFamily = "monospace", fontSizeSp = 14f), out[1])
    }

    @Test
    fun `segmentsToAnnotatedString carries baseline font and size spans`() {
        val segs = listOf(
            TextSegment(text = "H"),
            TextSegment(text = "2", baseline = TextBaseline.SUBSCRIPT),
            TextSegment(text = "x", fontFamily = "serif", fontSizeSp = 18f)
        )
        val annotated = RichTextConverter.segmentsToAnnotatedString(segs)
        assertEquals("H2x", annotated.text)
        assertNotNull(annotated.spanStyles.firstOrNull { it.item.baselineShift == BaselineShift.Subscript })
        assertNotNull(
            annotated.spanStyles.firstOrNull { it.item.fontSize != TextUnit.Unspecified }
        )
    }

    @Test
    fun `hasTypingStyle reflects pending template`() {
        assertFalse(TextSegment().hasTypingStyle)
        assertTrue(TextSegment(baseline = TextBaseline.SUPERSCRIPT).hasTypingStyle)
        assertTrue(TextSegment(fontSizeSp = 14f).hasTypingStyle)
        assertTrue(TextSegment(fontFamily = "serif").hasTypingStyle)
    }
}
