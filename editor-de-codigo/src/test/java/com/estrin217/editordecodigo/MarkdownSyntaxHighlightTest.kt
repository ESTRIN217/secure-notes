package com.estrin217.editordecodigo

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import com.estrin217.editordecodigo.syntax.MarkdownTokenizer
import com.estrin217.editordecodigo.syntax.SupportedLanguage
import com.estrin217.editordecodigo.syntax.SyntaxHighlighter
import com.estrin217.tema.SyntaxTheme
import com.estrin217.tema.SyntaxThemes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El resaltador de Markdown alimentado por el tokenizador (spec 012, T4).
 *
 * Estas pruebas comprueban el **puente** entre tokens y `SpanStyle`: que el texto vuelve intacto,
 * que cada token_kind recibe el color que el plan le asigna, y que el cuerpo de una cerca **no**
 * lleva el color del código en línea.
 */
class MarkdownSyntaxHighlightTest {

    private val theme: SyntaxTheme = SyntaxThemes.OneDark

    private fun highlight(code: String, query: String = ""): androidx.compose.ui.text.AnnotatedString =
        SyntaxHighlighter.highlight(code, SupportedLanguage.MARKDOWN, theme, query)

    /**
     * Color que el resaltador aplica al tramo [from, to).
     *
     * Se queda con el **último** span que cubre el tramo, no con el más grande ni con el más
     * pequeño. Es como resuelve Compose: gana el que se aplicó después. Descartar el mayor
     * descartaría el token concreto porque el span base cubre el documento entero, y descartar el
     * menor descartaría la búsqueda, cuyo span es más corto que el token que tapa.
     */
    private fun colorAt(result: androidx.compose.ui.text.AnnotatedString, from: Int, to: Int): Color? =
        result.spanStyles.lastOrNull { it.start <= from && it.end >= to }?.item?.color

    // ---------------------------------------------------------------- invariantes

    @Test
    fun `el texto vuelve identico al original`() {
        val md = "# Titulo\n\nTexto **fuerte** con `codigo`.\n\n- [ ] Tarea\n"
        assertEquals(md, highlight(md).text)
    }

    @Test
    fun `los seis niveles y una tabla producen color sin romper el texto`() {
        val md = "# h1\n## h2\n### h3\n#### h4\n##### h5\n###### h6\n\n| a | b |\n| - | - |\n| 1 | 2 |\n"
        val result = highlight(md)
        assertEquals(md, result.text)
        assertTrue(result.spanStyles.isNotEmpty())
    }

    @Test
    fun `cada token recibe el color que le asigna el plan`() {
        val md = "# Titulo\n\n- [ ] Tarea\n\n```kotlin\nval x = 1\n```\n"
        val result = highlight(md)

        // Los indices se sacan con indexOf, no a ojo: escritos a mano se desplazan en cuanto el
        // fixture cambia una linea, y la prueba sigue en verde mirando el color equivocado.
        assertEquals(theme.keyword, colorAt(result, md.indexOf("#"), md.indexOf("#") + 1))
        val tarea = md.indexOf("[ ]")
        assertEquals(theme.booleanNull, colorAt(result, tarea, tarea + 3))
        val cerca = md.indexOf("```")
        assertEquals(theme.string, colorAt(result, cerca, cerca + 3))
        val cuerpo = md.indexOf("val x = 1")
        assertEquals(theme.string, colorAt(result, cuerpo, cuerpo + 9))
    }

    @Test
    fun `el enfasis fuerte es negrita y el simple cursiva`() {
        val md = "**fuerte** y *cursiva*\n"
        val result = highlight(md)

        val fuerte = result.spanStyles.firstOrNull { it.start == 0 && it.end == 10 }
        assertEquals(theme.propertyKey, fuerte?.item?.color)
        assertEquals(FontWeight.Bold, fuerte?.item?.fontWeight)

        val cursiva = result.spanStyles.firstOrNull { it.start == 13 && it.end == 22 }
        assertEquals(theme.propertyKey, cursiva?.item?.color)
        assertEquals(FontStyle.Italic, cursiva?.item?.fontStyle)
    }

    @Test
    fun `el tachado lleva el mismo corte de linea que el resto de enfasis`() {
        val result = highlight("~~tachado~~\n")
        val token = result.spanStyles.firstOrNull { it.start == 0 && it.end == 11 }
        assertEquals(theme.propertyKey, token?.item?.color)
        assertEquals(
            androidx.compose.ui.text.style.TextDecoration.LineThrough,
            token?.item?.textDecoration
        )
    }

    // ---------------------------------------------------------------- la precedencia, en color

    @Test
    fun `el cuerpo de una cerca no lleva el color del codigo en linea`() {
        val md = "```kotlin\n`esto` sigue siendo bloque\n```\n"
        val result = highlight(md)
        val bodyStart = md.indexOf("`esto`")
        // El interior de la cerca es CODE_BLOCK (string), nunca INLINE_CODE (function).
        assertEquals(theme.string, colorAt(result, bodyStart, bodyStart + 6))
        assertNotEquals(theme.function, colorAt(result, bodyStart, bodyStart + 6))
    }

    @Test
    fun `el codigo en linea de un parrafo si lleva su color`() {
        val md = "Texto con `codigo` aqui\n"
        val result = highlight(md)
        val at = md.indexOf("`codigo`")
        assertEquals(theme.function, colorAt(result, at, at + 8))
    }

    @Test
    fun `el tachado de un parrafo no se come el texto`() {
        val md = "Un ~~tachado~~ y nada mas\n"
        val result = highlight(md)
        val at = md.indexOf("~~tachado~~")
        assertEquals(theme.propertyKey, colorAt(result, at, at + 11))
        assertEquals(md, result.text)
    }

    // ---------------------------------------------------------------- busqueda yLanguages

    @Test
    fun `la busqueda se superpone al resaltado`() {
        // El span de busqueda no cambia el color del texto: solo pinta el **fondo**. Pedir el color
        // daria transparente y la comprobacion no significaria nada.
        val md = "# Titulo con buscar\n"
        val result = highlight(md, "buscar")
        val at = md.indexOf("buscar")
        val fondo = result.spanStyles.lastOrNull { it.start <= at && it.end >= at + 6 }?.item?.background
        assertEquals(theme.searchMatchBackground, fondo)
        assertEquals(md, result.text)
    }

    @Test
    fun `markdown entra en la lista de lenguajes con spans`() {
        val result = highlight("# h\n\n- a\n")
        assertTrue(result.spanStyles.size >= 3)
        assertEquals("# h\n\n- a\n", result.text)
    }

    @Test
    fun `el mapa de tokens cubre todos los tipos conocidos`() {
        // Si se anade un MdTokenKind y no su color, ese tramo sale sin pintar y no hay quien lo note.
        // La prueba recorre los tokens y exige que cada uno tenga **al menos un** span que lo cubra.
        val md = """
            ---
            fm: 1
            ---

            # h
            texto \* `c` **b** *i* ~~s~~ [t](u) <http://y> <b>h</b>
            > c
            - [ ] t
            1. n
            | a | b |
            | - | - |
            ---
            [r]: http://x
        """.trimIndent() + "\n"

        // Sin busqueda: si no, el span de coincidencia tapa al del token y la comprobacion no
        // distinguiria un mapa completo de uno a medias.
        val result = highlight(md)
        val sinPintar = MarkdownTokenizer.tokenize(md)
            .filterNot { token ->
                result.spanStyles.any { it.start <= token.start && it.end >= token.end }
            }
            .map { it.kind }
            .toSet()

        assertTrue("tipos sin color en el resaltador: $sinPintar", sinPintar.isEmpty())
    }
}
