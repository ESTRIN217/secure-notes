package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import com.example.util.CodeHighlighter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeHighlighterTest {

    private fun spanTextsOf(code: String, lang: String): List<String> =
        CodeHighlighter.highlight(code, lang).spanStyles.map { code.substring(it.start, it.end) }

    private fun spanStylesOf(code: String, lang: String): List<Pair<String, SpanStyle>> =
        CodeHighlighter.highlight(code, lang).spanStyles.map { code.substring(it.start, it.end) to it.item }

    private fun spansWithStyle(
        code: String, lang: String, color: Color
    ): List<String> =
        spanStylesOf(code, lang)
            .filter { it.second.color == color }
            .map { it.first }

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

    @Test
    fun `camel case identifiers do not split into type spans`() {
        val code = "imageVector\nmyTextView\nonClick\ngetStringRes()"
        val typeSpans = spansWithStyle(code, "kotlin", Color(0xFF9333EA))
        assertTrue("no deberia resaltar Vector como tipo: $typeSpans", typeSpans.none { it == "Vector" })
        assertTrue("no deberia resaltar TextView como tipo: $typeSpans", typeSpans.none { it == "TextView" })
        assertTrue("no deberia resaltar Click como tipo: $typeSpans", typeSpans.none { it == "Click" })
        assertTrue("no deberia resaltar Res como tipo: $typeSpans", typeSpans.none { it == "Res" })
        assertTrue("imageVector deberia quedar sin resaltar como tipo: $typeSpans", typeSpans.isEmpty())
    }

    @Test
    fun `annotations are highlighted`() {
        val code = "@Composable\nfun Greeting() {}"
        val annotationSpans = spansWithStyle(code, "kotlin", Color(0xFF10B981))
        assertTrue("@Composable no resaltado: $annotationSpans", annotationSpans.contains("@Composable"))
    }

    @Test
    fun `function calls are highlighted`() {
        val code = "setContentView(R.layout.activity_main)"
        val functionSpans = spansWithStyle(code, "kotlin", Color(0xFFA855F7))
        assertTrue("setContentView no resaltado: $functionSpans", functionSpans.contains("setContentView"))
    }

    @Test
    fun `keyword fun is not matched as function`() {
        val code = "fun main() { Log.e(TAG, \"error\") }"
        val keywordSpans = spansWithStyle(code, "kotlin", Color(0xFF3B82F6))
        val functionSpans = spansWithStyle(code, "kotlin", Color(0xFFA855F7))
        assertTrue("fun deberia ser keyword: $keywordSpans", keywordSpans.contains("fun"))
        assertTrue("e deberia ser function: $functionSpans", functionSpans.contains("e"))
        assertTrue("fun no deberia ser function: $functionSpans", !functionSpans.contains("fun"))
        assertTrue("Log es un tipo, no function: $functionSpans", !functionSpans.contains("Log"))
    }

    @Test
    fun `string interpolation simple var`() {
        val code = "val msg = \"Hello \$name\""
        val interpSpans = spansWithStyle(code, "kotlin", Color(0xFFF59E0B))
        assertTrue("\$name no resaltado: $interpSpans", interpSpans.contains("\$name"))
        assertTrue("el string padre tambien debe existir: $interpSpans",
            spanStylesOf(code, "kotlin").any { it.first == "\"Hello \$name\"" })
    }

    @Test
    fun `string interpolation expression`() {
        val code = "val msg = \"sum = \${a + b}\""
        val interpSpans = spansWithStyle(code, "kotlin", Color(0xFFF59E0B))
        assertTrue("\${a + b} no resaltado: $interpSpans", interpSpans.contains("\${a + b}"))
    }

    @Test
    fun `interpolation not highlighted outside strings`() {
        val code = "val x = \$name"
        val interpSpans = spansWithStyle(code, "kotlin", Color(0xFFF59E0B))
        assertTrue("\$name no deberia resaltar fuera de string: $interpSpans", interpSpans.isEmpty())
    }

    @Test
    fun `annotation with package separators`() {
        val code = "@Suppress(\"UNCHECKED_CAST\")\nval x = 1"
        val annotationSpans = spansWithStyle(code, "kotlin", Color(0xFF10B981))
        assertTrue("@Suppress no resaltado: $annotationSpans", annotationSpans.contains("@Suppress"))
    }

    @Test
    fun `multiline string with interpolation`() {
        val code = "val s = \"\"\"\n  \${name}'s items:\n  \${items.joinToString(\", \")}\n\"\"\""
        val interpSpans = spansWithStyle(code, "kotlin", Color(0xFFF59E0B))
        assertTrue("\${name} no resaltado: $interpSpans", interpSpans.contains("\${name}"))
        assertTrue("\${items.joinToString(\", \")} no resaltado: $interpSpans",
            interpSpans.contains("\${items.joinToString(\", \")}"))
    }

    @Test
    fun `language without keywords does not crash`() {
        val code = "<div class=\"x\">hola</div>"
        CodeHighlighter.highlight(code, "html")
        CodeHighlighter.highlight(code, "xml")
    }

    @Test
    fun `html still highlights strings`() {
        val code = "<div class=\"container\">hola</div>"
        val stringSpans = spansWithStyle(code, "html", Color(0xFF16A34A))
        assertTrue("\"container\" no resaltado: $stringSpans", stringSpans.contains("\"container\""))
    }

    @Test
    fun `html comment without keywords does not crash`() {
        val code = "<!-- val x = 1 --><div>texto</div>"
        val spans = spanTextsOf(code, "html")
        assertTrue("comentario html no resaltado: $spans", spans.contains("<!-- val x = 1 -->"))
    }
}
