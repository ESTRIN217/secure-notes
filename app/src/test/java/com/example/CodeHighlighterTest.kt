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

    @Test
    fun `html highlights tag names as keywords`() {
        val code = "<div class=\"x\">hola</div>"
        val keywordSpans =
            spanStylesOf(code, "html").filter { it.second.color == Color(0xFF3B82F6) }.map { it.first }
        assertTrue("div no resaltado como keyword: $keywordSpans", keywordSpans.contains("div"))
    }

    @Test
    fun `html highlights tag brackets`() {
        val code = "<div>hola</div>"
        val bracketSpans =
            spanStylesOf(code, "html").filter { it.second.color == Color(0xFF8B93A3) }.map { it.first }
        assertTrue("apertura no resaltada: $bracketSpans", bracketSpans.contains("<"))
        assertTrue("cierre no resaltado: $bracketSpans", bracketSpans.contains(">"))
    }

    @Test
    fun `html highlights attribute names`() {
        val code = "<a href=\"index.html\" target=\"_blank\">link</a>"
        val attrSpans =
            spanStylesOf(code, "html").filter { it.second.color == Color(0xFFF59E0B) }.map { it.first }
        assertTrue("href no resaltado: $attrSpans", attrSpans.contains("href"))
        assertTrue("target no resaltado: $attrSpans", attrSpans.contains("target"))
    }

    @Test
    fun `html keeps attribute strings as strings`() {
        val code = "<img src=\"logo.png\">"
        val stringSpans = spansWithStyle(code, "html", Color(0xFF16A34A))
        assertTrue("\"logo.png\" no resaltado: $stringSpans", stringSpans.contains("\"logo.png\""))
    }

    @Test
    fun `html handles self closing tags`() {
        val code = "<br/><img src=\"a.png\"/>"
        val keywordSpans =
            spanStylesOf(code, "html").filter { it.second.color == Color(0xFF3B82F6) }.map { it.first }
        assertTrue("br no resaltado: $keywordSpans", keywordSpans.contains("br"))
        assertTrue("img no resaltado: $keywordSpans", keywordSpans.contains("img"))
    }

    @Test
    fun `html comment content is not tag highlighted`() {
        val code = "<!-- <div class=\"x\"> --><div>real</div>"
        val spans = spanTextsOf(code, "html")
        assertTrue("comentario completo no resaltado: $spans", spans.contains("<!-- <div class=\"x\"> -->"))
        assertTrue("keyword dentro de comentario no debe aparecer: $spans", !spans.contains("class"))
    }

    @Test
    fun `html text outside tags is not highlighted as tags`() {
        val code = "<p>hola mundo</p>"
        val spans = spanTextsOf(code, "html").map { it.trim() }.filter { it.isNotBlank() }
        assertTrue("texto plano 'hola mundo' no debe resaltarse: $spans",
            spans.none { it.contains("hola mundo") || it == "hola" || it == "mundo" })
        assertTrue("p deberia resaltarse: $spans", spans.contains("p"))
    }

    @Test
    fun `python highlights builtin functions and types as keywords`() {
        val code = "print(len(range(3)))\nx: int = 5\nitems = list((1, 2))"
        val keywordSpans =
            spanStylesOf(code, "python").filter { it.second.color == Color(0xFF3B82F6) }.map { it.first }
        assertTrue("print no resaltado como keyword: $keywordSpans", keywordSpans.contains("print"))
        assertTrue("len no resaltado: $keywordSpans", keywordSpans.contains("len"))
        assertTrue("range no resaltado: $keywordSpans", keywordSpans.contains("range"))
        assertTrue("int no resaltado: $keywordSpans", keywordSpans.contains("int"))
        assertTrue("list no resaltado: $keywordSpans", keywordSpans.contains("list"))
    }

    @Test
    fun `python keyword priorizado sobre funcion generica`() {
        val code = "print(1)"
        val keywordSpans =
            spanStylesOf(code, "python").filter { it.second.color == Color(0xFF3B82F6) }.map { it.first }
        val functionSpans =
            spanStylesOf(code, "python").filter { it.second.color == Color(0xFFA855F7) }.map { it.first }
        assertTrue("print deberia ser keyword azul: $keywordSpans", keywordSpans.contains("print"))
        assertTrue("print no deberia ser function: $functionSpans", functionSpans.none { it == "print" })
    }

    @Test
    fun `python fstring expression is highlighted`() {
        val code = "f\"sum = {a + b}\""
        val interpSpans = spansWithStyle(code, "python", Color(0xFFF59E0B))
        assertTrue("{a + b} no resaltado: $interpSpans", interpSpans.contains("{a + b}"))
        val stringSpans = spansWithStyle(code, "python", Color(0xFF16A34A))
        assertTrue("string padre no resaltado: $stringSpans", stringSpans.contains("\"sum = {a + b}\""))
    }

    @Test
    fun `python plain string is not fstring interpolated`() {
        val code = "\"valor = {x}\""
        val interpSpans = spansWithStyle(code, "python", Color(0xFFF59E0B))
        assertTrue("string plano no deberia resaltar {x}: $interpSpans", interpSpans.isEmpty())
    }

    @Test
    fun `python fstring does not highlight outside braces`() {
        val code = "f\"hola {name}\""
        val interpSpans = spansWithStyle(code, "python", Color(0xFFF59E0B))
        assertTrue("{name} no resaltado: $interpSpans", interpSpans.contains("{name}"))
    }

    @Test
    fun `java highlights primitive types with distinct style`() {
        val code = "int x = 5;\nboolean ok = true;\nvoid run() {}"
        val primitiveSpans =
            spanStylesOf(code, "java").filter { it.second.color == Color(0xFF0D9488) }.map { it.first }
        assertTrue("int no resaltado como primitivo: $primitiveSpans", primitiveSpans.contains("int"))
        assertTrue("boolean no resaltado como primitivo: $primitiveSpans", primitiveSpans.contains("boolean"))
        assertTrue("void no resaltado como primitivo: $primitiveSpans", primitiveSpans.contains("void"))
        val keywordSpans =
            spanStylesOf(code, "java").filter { it.second.color == Color(0xFF3B82F6) }.map { it.first }
        assertTrue("primitivo no deberia ser keyword azul: $keywordSpans",
            keywordSpans.none { it in setOf("int", "boolean", "void") })
    }

    @Test
    fun `java highlights regular keywords as blue`() {
        val code = "public final String name = null;"
        val keywordSpans =
            spanStylesOf(code, "java").filter { it.second.color == Color(0xFF3B82F6) }.map { it.first }
        assertTrue("public no resaltado: $keywordSpans", keywordSpans.contains("public"))
        assertTrue("final no resaltado: $keywordSpans", keywordSpans.contains("final"))
        assertTrue("null no resaltado: $keywordSpans", keywordSpans.contains("null"))
    }

    @Test
    fun `java highlights new module and record keywords`() {
        val code = "record Point(int x) {\n}\nmodule app { requires java.base; }"
        val keywordSpans =
            spanStylesOf(code, "java").filter { it.second.color == Color(0xFF3B82F6) }.map { it.first }
        assertTrue("record no resaltado: $keywordSpans", keywordSpans.contains("record"))
        assertTrue("module no resaltado: $keywordSpans", keywordSpans.contains("module"))
        assertTrue("requires no resaltado: $keywordSpans", keywordSpans.contains("requires"))
    }

    @Test
    fun `java new keywords yield var sealed permits`() {
        val code = "var x = 0;\nint y = switch (x) { case 0 -> 1; default -> 0; };\nsealed class A permits B {}"
        val keywordSpans =
            spanStylesOf(code, "java").filter { it.second.color == Color(0xFF3B82F6) }.map { it.first }
        assertTrue("var no resaltado: $keywordSpans", keywordSpans.contains("var"))
        assertTrue("sealed no resaltado: $keywordSpans", keywordSpans.contains("sealed"))
        assertTrue("permits no resaltado: $keywordSpans", keywordSpans.contains("permits"))
    }

    @Test
    fun `php highlights keywords comments and dollar interpolation`() {
        val code = "<?php\n// comentario\n\$name = \"Hola\";\necho \"Hola \$name\";"
        val blue = spansWithStyle(code, "php", Color(0xFF3B82F6))
        val gray = spansWithStyle(code, "php", Color(0xFF8B93A3))
        val amber = spansWithStyle(code, "php", Color(0xFFF59E0B))
        assertTrue("php keyword no resaltado: $blue", blue.contains("php"))
        assertTrue("echo keyword no resaltado: $blue", blue.contains("echo"))
        assertTrue("comentario php no resaltado: $gray", gray.contains("// comentario"))
        assertTrue("interpolacion \$name no resaltada: $amber", amber.contains("\$name"))
    }

    @Test
    fun `ruby highlights keywords hash comments`() {
        val code = "# comentario\ndef greet\n  puts \"hola\"\nend"
        val blue = spansWithStyle(code, "ruby", Color(0xFF3B82F6))
        val gray = spansWithStyle(code, "ruby", Color(0xFF8B93A3))
        assertTrue("def no resaltado: $blue", blue.contains("def"))
        assertTrue("end no resaltado: $blue", blue.contains("end"))
        assertTrue("comentario ruby no resaltado: $gray", gray.contains("# comentario"))
    }

    @Test
    fun `yaml and toml highlight hash comments and keywords`() {
        val yaml = "# nota\nenabled: true\n"
        val yblue = spansWithStyle(yaml, "yaml", Color(0xFF3B82F6))
        val ygray = spansWithStyle(yaml, "yaml", Color(0xFF8B93A3))
        assertTrue("yaml true no resaltado: $yblue", yblue.contains("true"))
        assertTrue("yaml comentario no resaltado: $ygray", ygray.contains("# nota"))

        val toml = "# nota\nenabled = false\n"
        val tblue = spansWithStyle(toml, "toml", Color(0xFF3B82F6))
        val tgray = spansWithStyle(toml, "toml", Color(0xFF8B93A3))
        assertTrue("toml false no resaltado: $tblue", tblue.contains("false"))
        assertTrue("toml comentario no resaltado: $tgray", tgray.contains("# nota"))
    }

    @Test
    fun `ini highlights semicolon and hash comments`() {
        val code = "; seccion\n# otra nota\n[db]\nhost=localhost\n"
        val gray = spansWithStyle(code, "ini", Color(0xFF8B93A3))
        assertTrue("; comentario no resaltado: $gray", gray.contains("; seccion"))
        assertTrue("# comentario no resaltado: $gray", gray.contains("# otra nota"))
    }

    @Test
    fun `dart highlights keywords and interpolation`() {
        val code = "void main() { var name = 'x'; print('Hola \$name'); }"
        val blue = spansWithStyle(code, "dart", Color(0xFF3B82F6))
        val amber = spansWithStyle(code, "dart", Color(0xFFF59E0B))
        assertTrue("void keyword no resaltado: $blue", blue.contains("void"))
        assertTrue("var keyword no resaltado: $blue", blue.contains("var"))
        assertTrue("interpolacion \$name no resaltada: $amber", amber.contains("\$name"))
    }

    @Test
    fun `lua highlights keywords and double dash comments`() {
        val code = "-- comentario\nlocal x = 1\nwhile x > 0 do\n  break\nend"
        val blue = spansWithStyle(code, "lua", Color(0xFF3B82F6))
        val gray = spansWithStyle(code, "lua", Color(0xFF8B93A3))
        assertTrue("local no resaltado: $blue", blue.contains("local"))
        assertTrue("while no resaltado: $blue", blue.contains("while"))
        assertTrue("comentario lua no resaltado: $gray", gray.contains("-- comentario"))
    }
}
