package com.example

import com.example.util.CodeHighlighter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeHighlighterTest {

    private fun spanTextsOf(code: String, lang: String): List<String> =
        CodeHighlighter.highlight(code, lang).spanStyles.map { code.substring(it.start, it.end) }

    @Test
    fun `empty or unknown language does not highlight`() {
        assertTrue(CodeHighlighter.highlight("val x = 1", "").spanStyles.isEmpty())
        assertTrue(CodeHighlighter.highlight("val x = 1", null).spanStyles.isEmpty())
        assertTrue(CodeHighlighter.highlight("val x = 1", "unknown").spanStyles.isEmpty())
    }

    @Test
    fun `plain text is preserved`() {
        val code = "fun main() {\n    val s = \"hola\"\n}"
        val res = CodeHighlighter.highlight(code, "kotlin")
        assertEquals(code, res.text)
    }

    @Test
    fun `kotlin highlights keywords and strings`() {
        val code = "fun main() { val s = \"hola\" }"
        val spans = spanTextsOf(code, "kotlin")
        assertTrue("keyword fun no resaltado: $spans", spans.contains("fun"))
        assertTrue("keyword val no resaltado: $spans", spans.contains("val"))
        assertTrue("string no resaltado: $spans", spans.contains("\"hola\""))
    }

    @Test
    fun `comments and numbers are highlighted`() {
        val code = "// comentario\nint x = 42;"
        val spans = spanTextsOf(code, "c")
        assertTrue("comentario no resaltado: $spans", spans.contains("// comentario"))
        assertTrue("numero no resaltado: $spans", spans.contains("42"))
    }

    @Test
    fun `keywords inside comments are not double highlighted`() {
        val code = "// val x = 1\nval y = 2"
        val spans = spanTextsOf(code, "kotlin")
        assertEquals("solo la segunda val deberia ser keyword: $spans", 1, spans.count { it == "val" })
        assertTrue(spans.contains("// val x = 1"))
    }

    @Test
    fun `capitalized identifiers are highlighted as types`() {
        val code = "val list: List<String> = emptyList()"
        val spans = spanTextsOf(code, "kotlin")
        assertTrue(spans.contains("List"))
        assertTrue(spans.contains("String"))
    }
}
