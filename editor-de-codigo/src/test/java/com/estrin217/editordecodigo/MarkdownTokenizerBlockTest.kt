package com.estrin217.editordecodigo

import com.estrin217.editordecodigo.syntax.MdToken
import com.estrin217.editordecodigo.syntax.MdTokenKind
import com.estrin217.editordecodigo.syntax.MarkdownTokenizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tokenizador de bloques de Markdown (spec 012, T1).
 *
 * Estas pruebas son JUnit puro a propósito: en esta plataforma Robolectric no tiene runtime nativo
 * para `linux/aarch64` y no se ejecuta (RNF-27).
 */
class MarkdownTokenizerBlockTest {

    private fun tokensOf(src: String): List<MdToken> = MarkdownTokenizer.tokenize(src)

    private fun kindsOf(src: String): List<MdTokenKind> = tokensOf(src).map { it.kind }

    private fun tokenAt(src: String, kind: MdTokenKind): List<MdToken> =
        tokensOf(src).filter { it.kind == kind }

    /** Invariante universal: ordenado, sin solapes y dentro del documento. */
    private fun assertInvariant(src: String) {
        var lastEnd = 0
        tokensOf(src).forEach { token ->
            assertTrue(
                "token solapado: $token empieza en ${token.start} y el anterior acaba en $lastEnd",
                token.start >= lastEnd
            )
            assertTrue(
                "token fuera de rango: $token en un texto de ${src.length} caracteres",
                token.start >= 0 && token.end <= src.length && token.end > token.start
            )
            lastEnd = token.end
        }
    }

    private fun textOf(src: String, token: MdToken): String = src.substring(token.start, token.end)

    // ---------------------------------------------------------------- front matter

    @Test
    fun `el front matter con cierre es un solo token`() {
        val md = "---\ntitle: Notes\n---\n\n# Hola\n"
        val front = tokenAt(md, MdTokenKind.FRONT_MATTER)
        assertEquals(1, front.size)
        assertEquals("---\ntitle: Notes\n---", textOf(md, front.first()))
    }

    @Test
    fun `sin linea de cierre el guion no es front matter sino una raya`() {
        val md = "---\ntitle: Notes\n"
        assertTrue(kindsOf(md).contains(MdTokenKind.THEMATIC_BREAK))
        assertTrue(!kindsOf(md).contains(MdTokenKind.FRONT_MATTER))
    }

    @Test
    fun `el front matter solo cuenta en la primera linea`() {
        val md = "# Titulo\n\n---\ntitle: x\n---\n"
        assertTrue(kindsOf(md).none { it == MdTokenKind.FRONT_MATTER })
    }

    // ---------------------------------------------------------------- encabezados

    @Test
    fun `encabezado ATX separa la marca del texto`() {
        val md = "# Titulo\n"
        assertEquals(listOf(MdTokenKind.HEADING_MARK, MdTokenKind.HEADING), kindsOf(md))
        assertEquals("#", textOf(md, tokenAt(md, MdTokenKind.HEADING_MARK).first()))
        assertEquals("Titulo", textOf(md, tokenAt(md, MdTokenKind.HEADING).first()))
    }

    @Test
    fun `el hash de cierre del encabezado no es parte del texto`() {
        val md = "## Titulo ##\n"
        assertEquals("Titulo", textOf(md, tokenAt(md, MdTokenKind.HEADING).first()))
    }

    @Test
    fun `los seis niveles son encabezado y el septimo no`() {
        assertEquals(1, tokensOf("###### Seis\n").count { it.kind == MdTokenKind.HEADING_MARK })
        assertEquals("######", textOf("###### Seis\n", tokenAt("###### Seis\n", MdTokenKind.HEADING_MARK).first()))
        assertTrue(kindsOf("####### Siete\n").none { it == MdTokenKind.HEADING_MARK })
    }

    @Test
    fun `una almohadilla sin espacio no es encabezado`() {
        assertTrue(kindsOf("#NoHeading\n").none { it == MdTokenKind.HEADING_MARK })
    }

    @Test
    fun `encabezado setext con subrayado de igual y de guion`() {
        val mdIgual = "Titulo\n=====\n"
        assertEquals("Titulo", textOf(mdIgual, tokenAt(mdIgual, MdTokenKind.HEADING).first()))
        // El subrayado entero es la marca: quien lo ve comoraya se equivoca, por eso va coloreado.
        assertEquals("=====", textOf(mdIgual, tokenAt(mdIgual, MdTokenKind.HEADING_MARK).first()))

        val mdGuion = "Titulo\n-----\n"
        assertEquals("Titulo", textOf(mdGuion, tokenAt(mdGuion, MdTokenKind.HEADING).first()))
        assertEquals("-----", textOf(mdGuion, tokenAt(mdGuion, MdTokenKind.HEADING_MARK).first()))
    }

    @Test
    fun `el subrayado setext no se come un elemento de lista`() {
        val md = "- elemento\n------\n"
        assertTrue(kindsOf(md).none { it == MdTokenKind.HEADING })
    }

    // ---------------------------------------------------------------- cercas

    @Test
    fun `la cerca con lenguaje marca apertura cuerpo y cierre`() {
        val md = "```kotlin\nval x = 1\n```\n"
        assertEquals(
            listOf(MdTokenKind.CODE_FENCE, MdTokenKind.CODE_BLOCK, MdTokenKind.CODE_FENCE),
            kindsOf(md)
        )
        assertEquals("val x = 1", textOf(md, tokenAt(md, MdTokenKind.CODE_BLOCK).first()))
    }

    @Test
    fun `la cerca con tildes tambien cierra`() {
        val md = "~~~python\nprint(1)\n~~~\n"
        assertEquals(3, tokensOf(md).size)
        assertEquals("print(1)", textOf(md, tokenAt(md, MdTokenKind.CODE_BLOCK).first()))
    }

    @Test
    fun `una cerca sin cerrar llega hasta el final`() {
        val md = "```\nval x = 1\nval y = 2\n"
        val kinds = kindsOf(md)
        assertEquals(listOf(MdTokenKind.CODE_FENCE, MdTokenKind.CODE_BLOCK), kinds)
        assertEquals("val x = 1\nval y = 2", textOf(md, tokenAt(md, MdTokenKind.CODE_BLOCK).first()))
    }

    @Test
    fun `una cerca vacia no inventa cuerpo`() {
        assertEquals(2, tokensOf("```\n```\n").size)
        assertTrue(kindsOf("```\n```\n").none { it == MdTokenKind.CODE_BLOCK })
    }

    @Test
    fun `dentro de la cerca no sale ningun token de otra construccion`() {
        val md = "```markdown\n# No soy un titulo\n- [ ] No soy una tarea\n***\n```\n"
        val kinds = kindsOf(md)
        assertEquals(
            listOf(MdTokenKind.CODE_FENCE, MdTokenKind.CODE_BLOCK, MdTokenKind.CODE_FENCE),
            kinds
        )
    }

    @Test
    fun `la cadena de lenguaje de la cerca no puede llevar acentos graves`() {
        val md = "```a`b\ntexto\n```\n"
        assertTrue(kindsOf(md).none { it == MdTokenKind.CODE_BLOCK })
    }

    // ---------------------------------------------------------------- tablas

    @Test
    fun `la tabla separa cabecera separador y datos`() {
        val md = "| Lema | Tipo |\n| --- | :-: |\n| lista | vi |\n"
        val kinds = kindsOf(md)
        assertTrue(kinds.contains(MdTokenKind.TABLE_HEADER))
        assertTrue(kinds.contains(MdTokenKind.TABLE_DELIM))
        assertTrue(kinds.contains(MdTokenKind.TABLE_ROW))
        assertEquals("Lema", textOf(md, tokenAt(md, MdTokenKind.TABLE_HEADER).first()))
    }

    @Test
    fun `la tuberia de cada fila es su propio token`() {
        val md = "| a | b |\n| - | - |\n"
        val pipes = tokenAt(md, MdTokenKind.TABLE_DELIM).filter { textOf(md, it) == "|" }
        assertEquals(6, pipes.size)
    }

    @Test
    fun `el guion de la fila separadora no es una raya de separacion`() {
        val md = "| a | b |\n| --- | --- |\n| 1 | 2 |\n"
        assertTrue(kindsOf(md).none { it == MdTokenKind.THEMATIC_BREAK })
    }

    @Test
    fun `una linea con moderacion pero sin fila separadora no abre tabla`() {
        val md = "| a | b |\ntexto normal\n"
        assertTrue(kindsOf(md).none { it == MdTokenKind.TABLE_HEADER })
    }

    @Test
    fun `la tabla se detiene en la linea vacia`() {
        val md = "| a |\n| - |\n| 1 |\n\n| fuera |\n"
        val rows = tokenAt(md, MdTokenKind.TABLE_ROW)
        assertEquals(1, rows.size)
        assertEquals("1", textOf(md, rows.first()))
    }

    @Test
    fun `la alineacion de la fila separadora cuenta como celda delimitadora`() {
        val md = "| a |\n| :-: |\n"
        val delims = tokenAt(md, MdTokenKind.TABLE_DELIM)
        assertTrue(delims.any { textOf(md, it) == ":-:" })
    }

    // ---------------------------------------------------------------- listas

    @Test
    fun `la lista de tarea separa el vineta del marcador`() {
        val md = "- [ ] Pendiente\n"
        assertEquals(
            listOf(MdTokenKind.BULLET_MARK, MdTokenKind.TASK_MARK),
            kindsOf(md)
        )
        assertEquals("-", textOf(md, tokenAt(md, MdTokenKind.BULLET_MARK).first()))
        assertEquals("[ ]", textOf(md, tokenAt(md, MdTokenKind.TASK_MARK).first()))
    }

    @Test
    fun `la tarea marcada tambien se reconoce`() {
        assertEquals("[x]", textOf("- [x] Hecha\n", tokenAt("- [x] Hecha\n", MdTokenKind.TASK_MARK).first()))
        assertEquals("[X]", textOf("- [X] Hecha\n", tokenAt("- [X] Hecha\n", MdTokenKind.TASK_MARK).first()))
    }

    @Test
    fun `el vineta tiene su propio token con su sangria`() {
        val md = "  - Anidada\n"
        val mark = tokenAt(md, MdTokenKind.BULLET_MARK).first()
        assertEquals(2, mark.start)
    }

    @Test
    fun `la lista numerada se distingue de la de vinetas`() {
        val md = "1. Primero\n"
        assertEquals(listOf(MdTokenKind.ORDERED_MARK), kindsOf(md))
        assertEquals("1.", textOf(md, tokenAt(md, MdTokenKind.ORDERED_MARK).first()))
    }

    @Test
    fun `la estrella es vineta y no raya cuando le sigue texto`() {
        val md = "* elemento\n"
        assertTrue(kindsOf(md).contains(MdTokenKind.BULLET_MARK))
        assertTrue(kindsOf(md).none { it == MdTokenKind.THEMATIC_BREAK })
    }

    // ---------------------------------------------------------------- citas y rayas

    @Test
    fun `la cita anidada marca las dos angulos`() {
        val md = ">> Doble\n"
        val marks = tokenAt(md, MdTokenKind.QUOTE_MARK)
        assertEquals(1, marks.size)
        assertEquals(">>", textOf(md, marks.first()))
    }

    @Test
    fun `las tres rayas de separacion se reconocen`() {
        listOf("---", "***", "___", "- - -", "-----").forEach { sep ->
            assertTrue("no reconocio $sep", kindsOf("$sep\n").contains(MdTokenKind.THEMATIC_BREAK))
        }
    }

    @Test
    fun `dos guiones no son raya de separacion`() {
        assertTrue(kindsOf("--\n").none { it == MdTokenKind.THEMATIC_BREAK })
    }

    // ---------------------------------------------------------------- referencias

    @Test
    fun `la definicion de referencia se marca entera`() {
        val md = "[ref]: https://ejemplo.com \"titulo\"\n"
        val ref = tokenAt(md, MdTokenKind.LINK_REF).single()
        assertEquals(md.trimEnd('\n'), textOf(md, ref))
    }

    @Test
    fun `una definicion lleva dos puntos y destino`() {
        val conDestino = "[ref]: https://ejemplo.com\n"
        assertEquals(
            conDestino.trimEnd('\n'),
            textOf(conDestino, tokenAt(conDestino, MdTokenKind.LINK_REF).single())
        )
    }

    // ---------------------------------------------------------------- invariante

    @Test
    fun `el invariante universal se respeta en un documento con todo mezclado`() {
        val md = """
            ---
            titulo: Mixto
            ---

            # Encabezado

            Texto con `codigo`, **fuerte** y un [enlace][ref].

            - [ ] Tarea
              - Anidada
            1. Primera

            > Cita

            | a | b |
            | - | - |
            | 1 | 2 |

            ```kotlin
            // dentro de la cerca: # no, - [ ] no, | no
            ```

            [ref]: https://ejemplo.com

            ---

            Final.
        """.trimIndent() + "\n"

        assertInvariant(md)
        tokensOf(md).forEach { token ->
            assertTrue(
                "token degenerado: $token",
                token.end > token.start && token.start >= 0 && token.end <= md.length
            )
        }
    }

    @Test
    fun `el invariante se respeta en los casos limite sueltos`() {
        listOf(
            "",
            "\n",
            "texto\n",
            "#",
            "# ",
            "```",
            "|",
            "||",
            "- ",
            "> ",
            "a\n\n\n\nb",
            "```\n",
            "---\n"
        ).forEach { src -> assertInvariant(src) }
    }
}