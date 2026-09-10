package com.example

import com.example.data.model.TextSegment
import com.example.util.LatexEquationRenderer
import com.example.util.RichTextConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LatexEquationsTest {

    private val latex = "\\frac{a}{b}"
    private val source = "\$$latex\$"

    @Test
    fun `markupToSegments crea ecuacion con fuente visible`() {
        val segs = RichTextConverter.markupToSegments("E = <eq>$latex</eq> listo")
        assertEquals(3, segs.size)
        assertEquals("E = ", segs[0].text)
        assertEquals(source, segs[1].text)
        assertEquals(latex, segs[1].equationLatex)
        assertEquals(" listo", segs[2].text)
    }

    @Test
    fun `deriveLatex tolera delimitadores`() {
        assertEquals(latex, RichTextConverter.deriveLatex(source))
        assertEquals("\$\\frac{a}{b}\$", RichTextConverter.deriveLatex("\$\$\\frac{a}{b}\$\$"))
        assertEquals("x", RichTextConverter.deriveLatex("x"))
    }

    @Test
    fun `segmentsToAnnotatedString emite fuente con anotacion EQ`() {
        val segs = listOf(
            TextSegment(text = "E = "),
            TextSegment(text = source, equationLatex = latex),
            TextSegment(text = " listo")
        )
        val annotated = RichTextConverter.segmentsToAnnotatedString(segs)
        assertEquals("E = $source listo", annotated.text)
        val eq = annotated.getStringAnnotations(RichTextConverter.EQ_ANNOTATION, 0, annotated.length)
        assertEquals(1, eq.size)
        assertEquals(latex, eq[0].item)
        assertEquals(listOf(latex), RichTextConverter.equationLatexList(segs))
    }

    @Test
    fun `annotatedStringToSegments deriva latex del texto editado`() {
        val segs = listOf(
            TextSegment(text = "x ", bold = true),
            TextSegment(text = source, equationLatex = latex),
            TextSegment(text = " y")
        )
        val back = RichTextConverter.annotatedStringToSegments(
            RichTextConverter.segmentsToAnnotatedString(segs)
        )
        assertEquals(3, back.size)
        assertEquals(latex, back[1].equationLatex)
        assertEquals(source, back[1].text)
        assertFalse(back[1].bold)
        assertFalse(back[1].code)
        assertTrue(back[0].bold)
    }

    @Test
    fun `annotatedStringToSegments sincroniza fuente editada inline`() {
        // El usuario borra "b" dentro de $...$: el modelo sigue al texto visible.
        val editedText = "E = \$\\frac{a}{}\$ listo"
        val eqStart = 4
        val eqEnd = eqStart + "\$\\frac{a}{}\$".length
        val builder = androidx.compose.ui.text.AnnotatedString.Builder(editedText)
        builder.addStringAnnotation(RichTextConverter.EQ_ANNOTATION, latex, eqStart, eqEnd)
        val back = RichTextConverter.annotatedStringToSegments(builder.toAnnotatedString())
        assertEquals(3, back.size)
        assertEquals("\\frac{a}{}", back[1].equationLatex)
    }

    @Test
    fun `replaceEquationAt reemplaza latex y fuente`() {
        val segs = listOf(
            TextSegment(text = "ab"),
            TextSegment(text = source, equationLatex = latex),
            TextSegment(text = "cd")
        )
        val updated = RichTextConverter.replaceEquationAt(segs, 2, "x^2")
        assertEquals("x^2", updated[1].equationLatex)
        assertEquals("\$x^2\$", updated[1].text)
        assertEquals("ab", updated[0].text)
    }

    @Test
    fun `replaceEquationAt sin ecuacion devuelve misma instancia`() {
        val segs = listOf(TextSegment(text = "hola"))
        assertSame(segs, RichTextConverter.replaceEquationAt(segs, 0, "x^2"))
        assertSame(segs, RichTextConverter.replaceEquationAt(segs, 0, "  "))
    }

    @Test
    fun `canonicalizeEquations normaliza legacy unicode`() {
        val legacy = listOf(
            TextSegment(text = "a⁄b", bold = true, equationLatex = latex),
            TextSegment(text = " ok")
        )
        val out = RichTextConverter.canonicalizeEquations(legacy)
        assertEquals(source, out[0].text)
        assertEquals(latex, out[0].equationLatex)
        assertFalse(out[0].bold)
        assertEquals(" ok", out[1].text)
    }

    @Test
    fun `applySpanStyle no parte ni estiliza ecuaciones`() {
        val segs = listOf(
            TextSegment(text = "ab"),
            TextSegment(text = source, equationLatex = latex),
            TextSegment(text = "cd")
        )
        // Selección que pisa el borde: la ecuación entra entera y sin estilo.
        val styled = RichTextConverter.applySpanStyle(segs, 1, 4) { it.copy(bold = true) }
        val eq = styled.first { it.equationLatex != null }
        assertEquals(source, eq.text)
        assertFalse(eq.bold)
        assertTrue(styled.first { it.text == "b" }.bold)
        assertFalse(styled.first { it.text == "a" }.bold)
    }

    @Test
    fun `export html y markdown usan fuente latex`() {
        val segs = listOf(
            TextSegment(text = "E="),
            TextSegment(text = source, equationLatex = latex)
        )
        assertTrue(RichTextConverter.segmentsToHtml(segs).contains(source))
        assertTrue(RichTextConverter.segmentsToMarkdown(segs).contains(source))
    }

    @Test
    fun `isValidLatex acepta formula y rechaza vacio`() {
        assertTrue(LatexEquationRenderer.isValidLatex(latex))
        assertTrue(LatexEquationRenderer.isValidLatex("E=mc^2"))
        assertFalse(LatexEquationRenderer.isValidLatex(""))
        assertFalse(LatexEquationRenderer.isValidLatex("   "))
    }
}
