package com.estrin217.editordecodigo

import com.estrin217.editordecodigo.syntax.MdToken
import com.estrin217.editordecodigo.syntax.MdTokenKind
import com.estrin217.editordecodigo.syntax.MarkdownTokenizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tokenizador de **nivel en línea** de Markdown (spec 012, T2).
 *
 * Es la segunda pasada: se aplica a lo que el recorrido de bloques dejó libre. Un token de bloque
 * gana siempre, así que dentro de una cerca, de un encabezado o de una celda ya emitted no cabe un
 * token en línea encima.
 */
class MarkdownTokenizerInlineTest {

    private fun tokensOf(src: String): List<MdToken> = MarkdownTokenizer.tokenize(src)

    private fun tokenAt(src: String, kind: MdTokenKind): List<MdToken> =
        tokensOf(src).filter { it.kind == kind }

    private fun textOf(src: String, token: MdToken): String = src.substring(token.start, token.end)

    private fun only(src: String, kind: MdTokenKind): String {
        val found = tokenAt(src, kind)
        assertEquals("se esperaba exactamente un token $kind en [$src]", 1, found.size)
        return textOf(src, found.first())
    }

    /** Invariante universal, el mismo de T1: ordenado, sin solapes y dentro del documento. */
    private fun assertInvariant(src: String) {
        var lastEnd = 0
        tokensOf(src).forEach { token ->
            assertTrue(
                "token solapado: $token en [$src]",
                token.start >= lastEnd
            )
            assertTrue(
                "token fuera de rango: $token en un texto de ${src.length}",
                token.start >= 0 && token.end <= src.length && token.end > token.start
            )
            lastEnd = token.end
        }
    }

    // ---------------------------------------------------------------- énfasis

    @Test
    fun `el enfasis fuerte con asteriscos y con guiones bajos`() {
        assertEquals("**fuerte**", only("**fuerte**", MdTokenKind.STRONG))
        assertEquals("__fuerte__", only("__fuerte__", MdTokenKind.STRONG))
    }

    @Test
    fun `el enfasis simple con asteriscos y con guiones bajos`() {
        assertEquals("*cursiva*", only("*cursiva*", MdTokenKind.EM))
        assertEquals("_cursiva_", only("_cursiva_", MdTokenKind.EM))
    }

    @Test
    fun `el enfasis no abre dentro de una palabra`() {
        // `a*b*c` no es enfasis: el delimitador haría de apertura y de cierre a la vez, y CommonMark
        // lo veta. Igual para el guion bajo, que es lo que salva a `snake_case_name`.
        assertTrue(kindsOf("a*b*c").none { it == MdTokenKind.EM })
        assertTrue(kindsOf("snake_case_name").none { it == MdTokenKind.EM })
        assertTrue(kindsOf("a * b").none { it == MdTokenKind.EM })
        // Con espacio alrededor sí emphasizes.
        assertEquals("*b*", textOf("a *b* c\n", tokenAt("a *b* c\n", MdTokenKind.EM).single()))
    }

    @Test
    fun `el enfasis simple no se come al fuerte`() {
        val md = "**fuerte**"
        assertTrue(kindsOf(md).none { it == MdTokenKind.EM })
    }

    @Test
    fun `el enfasis que no cierra se queda como texto`() {
        assertTrue(kindsOf("*sin cerrar").none { it == MdTokenKind.EM })
    }

    @Test
    fun `el delimitador pegado a un espacio no abre enfasis`() {
        assertTrue(kindsOf("a * b").none { it == MdTokenKind.EM })
    }

    // ---------------------------------------------------------------- tachado

    @Test
    fun `el tachado lleva las dos tildes`() {
        assertEquals("~~tachado~~", only("~~tachado~~", MdTokenKind.STRIKETHROUGH))
    }

    @Test
    fun `una sola tilde no es tachado`() {
        assertTrue(kindsOf("~una~").none { it == MdTokenKind.STRIKETHROUGH })
    }

    // ---------------------------------------------------------------- codigo en linea

    @Test
    fun `el codigo en linea lleva sus acentos graves`() {
        assertEquals("`codigo`", only("`codigo`", MdTokenKind.INLINE_CODE))
    }

    @Test
    fun `la doble tilde admite acentos graves dentro`() {
        assertEquals("``a ` b``", only("``a ` b``", MdTokenKind.INLINE_CODE))
    }

    @Test
    fun `un cierre con otro numero de tildes no cierra`() {
        // Una apertura de una tilde no la cierra una de dos.
        assertTrue(kindsOf("`a``b").none { it == MdTokenKind.INLINE_CODE })
    }

    @Test
    fun `los acentos graves sin pareja no son codigo`() {
        assertTrue(kindsOf("`sin cerrar").none { it == MdTokenKind.INLINE_CODE })
    }

    @Test
    fun `el codigo gana al enfasis de su contenido`() {
        // `**no**` es código, no enfasis: el código en linea tiene mas prioridad.
        val md = "`**no**`"
        assertEquals(listOf(MdTokenKind.INLINE_CODE), kindsOf(md))
    }

    // ---------------------------------------------------------------- escapes

    @Test
    fun `la barra invertida escapa el asterisco`() {
        val md = "\\*no cursiva\\*"
        assertEquals(2, tokenAt(md, MdTokenKind.ESCAPE).size)
        assertTrue(kindsOf(md).none { it == MdTokenKind.EM })
    }

    @Test
    fun `una barra invertida suelta no es escape`() {
        assertTrue(kindsOf("c:\\ruta\\fichero").none { it == MdTokenKind.ESCAPE })
    }

    // ---------------------------------------------------------------- enlaces

    @Test
    fun `el enlace en linea separa el texto del destino`() {
        val md = "[texto](https://ejemplo.com)"
        assertEquals("[texto]", textOf(md, tokenAt(md, MdTokenKind.LINK_TEXT).first()))
        assertEquals("https://ejemplo.com", textOf(md, tokenAt(md, MdTokenKind.LINK_URL).first()))
    }

    @Test
    fun `la imagen usa el mismo par de tokens que el enlace`() {
        val md = "![alt](imagen.png)"
        assertEquals("![alt]", textOf(md, tokenAt(md, MdTokenKind.LINK_TEXT).first()))
        assertEquals("imagen.png", textOf(md, tokenAt(md, MdTokenKind.LINK_URL).first()))
    }

    @Test
    fun `el enlace por referencia apunta a su definicion`() {
        val md = "[texto][ref]"
        assertEquals("[texto]", textOf(md, tokenAt(md, MdTokenKind.LINK_TEXT).first()))
        assertEquals("[ref]", textOf(md, tokenAt(md, MdTokenKind.LINK_REF).first()))
    }

    @Test
    fun `el enlace colapsado es solo la referencia`() {
        assertEquals("[ref]", only("[ref]", MdTokenKind.LINK_REF))
    }

    @Test
    fun `un corchete suelto es un enlace colapsado y no una definicion`() {
        // En linea `[texto]` SI es un enlace colapsado, y se marca entero. Lo que no llega a hacer es
        // confundirse con la definicion `[ref]: url`, que es cosa de la pasada de bloque.
        assertEquals("[texto]", only("[texto]\n", MdTokenKind.LINK_REF))
    }

    @Test
    fun `un corchete suelto no produce texto de enlace`() {
        // No hay destino ni referencia, asi que no hay LINK_TEXT: el enlace colapsado usa LINK_REF.
        assertTrue(kindsOf("[corchete suelto]").none { it == MdTokenKind.LINK_TEXT })
        assertTrue(kindsOf("[]\n").none { it == MdTokenKind.LINK_REF })
    }

    // ---------------------------------------------------------------- autolinks y HTML

    @Test
    fun `el autolink lleva lasAngles`() {
        val md = "<https://ejemplo.com>"
        assertEquals("<https://ejemplo.com>", only(md, MdTokenKind.AUTOLINK))
    }

    @Test
    fun `el enlace de correo es autolink`() {
        assertEquals("<quien@ejemplo.com>", only("<quien@ejemplo.com>", MdTokenKind.AUTOLINK))
    }

    @Test
    fun `una etiqueta html incrustada se marca como etiqueta`() {
        val md = "<b>negrita</b>"
        assertEquals(2, tokenAt(md, MdTokenKind.HTML_TAG).size)
        assertEquals("<b>", textOf(md, tokenAt(md, MdTokenKind.HTML_TAG).first()))
        assertEquals("</b>", textOf(md, tokenAt(md, MdTokenKind.HTML_TAG).last()))
    }

    @Test
    fun `una etiqueta con atributos cierra en el primer angulo`() {
        assertEquals("""<a href="x">""", only("""<a href="x">""", MdTokenKind.HTML_TAG))
    }

    @Test
    fun `una Mayor que suelta no es etiqueta`() {
        assertTrue(kindsOf("5 > 3").none { it == MdTokenKind.HTML_TAG })
    }

    // ---------------------------------------------------------------- prioridad

    @Test
    fun `el enlace gana al enfasis que lo envuelve`() {
        // `_texto_` emphasis gana porque abre y cierra en la misma pasada; lo que se comprueba es que
        // el **enlace** se tokeniza completo y no se parte en trozos por el énfasis de alrededor.
        val md = "Antes [a](b) despues\n"
        assertEquals("[a]", textOf(md, tokenAt(md, MdTokenKind.LINK_TEXT).first()))
        assertEquals("b", textOf(md, tokenAt(md, MdTokenKind.LINK_URL).first()))
        assertInvariant(md)
    }

    @Test
    fun `el texto de un enlace no se parte por un enfasis exterior`() {
        // `[**fuerte**](x)` es un enlace entero: la pasada en línea se salta su interior.
        val md = "[**fuerte**](x)\n"
        assertEquals("[**fuerte**]", textOf(md, tokenAt(md, MdTokenKind.LINK_TEXT).single()))
        assertTrue(kindsOf(md).none { it == MdTokenKind.STRONG })
    }

    @Test
    fun `el texto de un enlace no se vuelve a tokenizar por dentro`() {
        val md = "[**fuerte**](x)"
        assertTrue(kindsOf(md).none { it == MdTokenKind.STRONG })
    }

    @Test
    fun `el contenido de una cerca no recibe nada en linea`() {
        val md = "```\n**fuerte** y [enlace](x)\n```\n"
        assertEquals(
            listOf(MdTokenKind.CODE_FENCE, MdTokenKind.CODE_BLOCK, MdTokenKind.CODE_FENCE),
            kindsOf(md)
        )
    }

    @Test
    fun `el texto de un titulo lo cubre el token de titulo`() {
        // Igual que la celda: el HEADING cubre el texto entero y el énfasis queda dentro sin tocar.
        val md = "# **fuerte**\n"
        assertEquals(1, tokenAt(md, MdTokenKind.HEADING_MARK).size)
        assertEquals("**fuerte**", textOf(md, tokenAt(md, MdTokenKind.HEADING).single()))
        assertTrue(kindsOf(md).none { it == MdTokenKind.STRONG })
    }

    @Test
    fun `el enfasis de un parrafo si se tokeniza`() {
        // El caso contrario: sin token de bloque por medio, la pasada en línea entra.
        assertTrue(kindsOf("Texto con **fuerte** aqui\n").contains(MdTokenKind.STRONG))
    }

    @Test
    fun `el marcador de tarea no se come el texto que viene despues`() {
        val md = "- [ ] **fuerte**\n"
        assertEquals("[ ]", textOf(md, tokenAt(md, MdTokenKind.TASK_MARK).first()))
        assertTrue(kindsOf(md).contains(MdTokenKind.STRONG))
    }

    @Test
    fun `el enfasis de una celda de tabla no pisa el token de celda`() {
        // El token de celda cubre el texto entero, así que dentro no cabe un token en línea: es el
        // bloque ganando, que es justo lo que pide RF-65. Elemphasis queda sin colorear.
        val md = "| **fuerte** |\n| --- |\n"
        val kinds = kindsOf(md)
        assertTrue(kinds.contains(MdTokenKind.TABLE_HEADER))
        assertTrue(kinds.none { it == MdTokenKind.STRONG })
        assertInvariant(md)
    }

    // ---------------------------------------------------------------- combinacion

    private fun kindsOf(src: String): List<MdTokenKind> = tokensOf(src).map { it.kind }

    @Test
    fun `un parrafo con todo se tokeniza en orden`() {
        val md = "Texto con `codigo`, **fuerte**, *cursiva*, ~~tachado~~, " +
            "[enlace](x), ![img](y.png) y <https://a.com>.\n"
        listOf(
            MdTokenKind.INLINE_CODE, MdTokenKind.STRONG, MdTokenKind.EM,
            MdTokenKind.STRIKETHROUGH, MdTokenKind.LINK_TEXT, MdTokenKind.LINK_URL,
            MdTokenKind.AUTOLINK
        ).forEach { kind ->
            assertTrue("falta $kind en [$md]", kindsOf(md).contains(kind))
        }
        assertInvariant(md)
    }

    @Test
    fun `el invariante se respeta en una lluvia de construcciones`() {
        val md = "# Titulo con **fuerte**\n\n" +
            "- [ ] Tarea con `codigo`\n" +
            "  - Anidada con [enlace](x)\n\n" +
            "> Cita con ~~tachado~~\n\n" +
            "| **a** | _b_ |\n| --- | --- |\n| 1 | 2 |\n\n" +
            "```kotlin\nval fuera = **no**\n```\n\n" +
            "Suelto \\* escapado y <https://a.com>.\n"
        assertInvariant(md)
    }

    @Test
    fun `el invariante se respeta en los casos limite sueltos`() {
        listOf(
            "*", "**", "_", "`", "``", "~", "~~", "\\",
            "[", "![", "<", "<>", "< >", "](", "[](x)",
            "a *", "* a", "a ** b", "`a` `b`",
            "- [ ] ", "> *x*", "# `x`"
        ).forEach { assertInvariant(it) }
    }
}