package com.example

import com.example.ui.markdown.ChatMarkdownSource.markdownForMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas de lo que se entrega al motor de GFM del chat (spec 017, RF-99).
 *
 * Lo que decide qué direcciones se abren (RF-100) vive ya en el módulo del motor y se prueba en
 * `LinkPolicyTest`, porque lo usan las cinco superficies de Markdown y no solo el chat.
 *
 * Son **funciones puras** en un fichero que no importa nada de composición ni de la plataforma. No es
 * purismo: es la condición para poderlas ejercitar en esta plataforma, donde la mitad de la suite no corre
 * por falta de runtime nativo de Robolectric. Si mañana alguien le mete un `import androidx.compose` al
 * fichero, estas pruebas dejan de correr **sin que se note**, y por eso hay un invariante que lo comprueba.
 *
 * Aquí no se prueba que una tabla **se vea** como tabla: eso necesita pantalla. Se prueba **qué texto llega
 * al pintado**, que es la mitad del RF-98 que sí se puede comprobar desde aquí.
 */
class ChatMarkdownSourceTest {

    // ── Paridad: sin markup no se toca nada ────────────────────────────────────────────────

    @Test
    fun `un texto sin markup sale identico`() {
        val crudo = "Hola.\nSegunda linea conacentos: ñ, á, ü.\n"

        assertEquals(crudo, markdownForMessage(crudo))
    }

    @Test
    fun `un texto vacio sale vacio`() {
        assertEquals("", markdownForMessage(""))
    }

    @Test
    fun `un markdown corriente no se toca`() {
        val crudo = "# Titulo\n\n- **uno**\n- `dos`\n\n```kotlin\nval x = 1\n```\n"

        assertEquals(crudo, markdownForMessage(crudo))
    }

    // ── Etiqueta de enlace: se traduce, no se retira (RF-99) ───────────────────────────────

    @Test
    fun `una etiqueta url se convierte en enlace markdown`() {
        assertEquals(
            "Ver [la doc](https://ejemplo.com/doc) ahora",
            markdownForMessage("Ver <url=https://ejemplo.com/doc>la doc</url> ahora")
        )
    }

    @Test
    fun `la etiqueta url sobrevive a una direccion con signos`() {
        assertEquals(
            "[a](https://ejemplo.com/a?b=1&c=2#d)",
            markdownForMessage("<url=https://ejemplo.com/a?b=1&c=2#d>a</url>")
        )
    }

    @Test
    fun `una etiqueta url sin cerrar se retira entera`() {
        // Decidido mirando lo que se ve HOY, no de memoria: el analizador actual da "Ver  la doc", es decir
        // que la direccion **tampoco** se ve (va dentro de la etiqueta). Retirarla entera es la paridad.
        assertEquals("Ver  la doc", markdownForMessage("Ver <url=https://ejemplo.com> la doc"))
    }

    @Test
    fun `la etiqueta url de una nota interna tambien se traduce`() {
        assertEquals(
            "[nota](note://42)",
            markdownForMessage("<url=note://42>nota</url>")
        )
    }

    @Test
    fun `una etiqueta url con dos enlaces en la linea se traducen los dos`() {
        assertEquals(
            "[uno](https://a.com) y [dos](https://b.com)",
            markdownForMessage("<url=https://a.com>uno</url> y <url=https://b.com>dos</url>")
        )
    }

    // ── Lista de tarea del markup propio: se traduce a casilla (RF-99) ─────────────────────

    @Test
    fun `una etiqueta item a secas es una casilla sin marcar`() {
        // HOY se ve "☐ Comprar pan" para <item> y para <item checked="false"> por igual. Traducirlo a "- "
        // le quitaria una casilla a quien hoy la ve, y este cambio no va de eso.
        assertEquals("- [ ] Comprar pan", markdownForMessage("<item>Comprar pan</item>"))
    }

    @Test
    fun `el cierre de item no se ve`() {
        // El analizador actual deja el cierre A LA VISTA: "☐ Comprar pan</item>". Es un fallo suyo y la
        // traduccion no lo arrastra.
        assertFalse(markdownForMessage("<item checked=\"true\">Comprar pan</item>").contains("</item>"))
    }

    @Test
    fun `una etiqueta item marcada sale como casilla marcada`() {
        assertEquals("- [x] Comprar pan", markdownForMessage("<item checked=\"true\">Comprar pan</item>"))
    }

    @Test
    fun `una etiqueta item sin marcar sale como casilla sin marcar`() {
        assertEquals("- [ ] Comprar pan", markdownForMessage("<item checked=\"false\">Comprar pan</item>"))
    }

    @Test
    fun `la casilla traduce las tres formas de escribir el estado`() {
        listOf("true", "checked").forEach { valor ->
            assertEquals(
                "- [x] Tarea",
                markdownForMessage("<item checked=\"$valor\">Tarea</item>")
            )
        }
    }

    // ── Las etiquetas que el motor sabe pintar se conservan (RF-99) ────────────────────────

    @Test
    fun `las etiquetas de estilo se conservan intactas`() {
        listOf("b", "i", "u", "s", "sub", "sup", "code").forEach { etiqueta ->
            val crudo = "<$etiqueta>texto</$etiqueta>"
            assertEquals("no debe tocar <$etiqueta>", crudo, markdownForMessage(crudo))
        }
    }

    // ── Las etiquetas que el motor no sabe pintar se retiran (RF-99) ──────────────────────

    @Test
    fun `las etiquetas de color y tamano se retiran dejando su texto`() {
        val salientes = listOf(
            "<color=#FF0000>rojo</color>",
            "<bg=#00FF00>fondo</bg>",
            "<size=14>grande</size>",
            "<font=mono>cursiva tipografica</font>",
            "<normal>normal</normal>",
            "<h1>Titulo</h1>",
            "<h2>Titulo</h2>",
            "<h3>Titulo</h3>"
        )
        salientes.forEach { crudo ->
            val salida = markdownForMessage(crudo)
            assertFalse("no debe quedar etiqueta en $crudo -> $salida", salida.contains("<"))
            assertTrue("el texto debe quedarse en $crudo -> $salida", salida.isNotBlank())
        }
    }

    @Test
    fun `las etiquetas de lista y de medios se retiran dejando su texto`() {
        val salientes = listOf(
            "<ol>uno</ol>",
            "<ul>uno</ul>",
            "<li>uno</li>",
            "<hr>",
            "<img src=\"medios/a.png\" />",
            "<video src=\"medios/a.mp4\" />",
            "<audio src=\"medios/a.mp3\" />"
        )
        salientes.forEach { crudo ->
            val salida = markdownForMessage(crudo)
            assertFalse("no debe quedar etiqueta en $crudo -> $salida", salida.contains("<"))
        }
    }

    @Test
    fun `details se conserva porque el motor lo sabe pintar`() {
        val crudo = "<details>oculto</details>"

        assertEquals(crudo, markdownForMessage(crudo))
    }

    @Test
    fun `el prefijo comun entre h1 y hr no muerde`() {
        // `<hr>` empieza por `h`, igual que `<h1>`: si la alternancia de nombres no va de los largos a los
        // cortos, `<hr>` se casa con `<h` y deja una `r` colgando en la linea.
        assertFalse(markdownForMessage("a <hr> b").contains("<"))
        assertFalse(markdownForMessage("a <h1>t</h1> b").contains("<"))
        assertEquals("a  b", markdownForMessage("a <hr> b"))
    }

    @Test
    fun `el texto de una imagen retirada se queda pero su direccion no`() {
        val salida = markdownForMessage("Mira <img src=\"https://ejemplo.com/a.png\" /> esta foto")

        assertTrue(salida.contains("Mira"))
        assertTrue(salida.contains("esta foto"))
        assertFalse("ninguna imagen puede quedar con su direccion (RNF-50)", salida.contains("ejemplo.com/a.png"))
    }

    // ── Un angulo que no es etiqueta no se toca (RF-99, caso limite 3) ─────────────────────

    @Test
    fun `una comparacion con menor no se toca`() {
        listOf("a < b", "2 < 3", "x<-1", "if (a<b) {}").forEach { crudo ->
            assertEquals(crudo, markdownForMessage(crudo))
        }
    }

    @Test
    fun `unos angulos vacios no se comen el texto de alrededor`() {
        val salida = markdownForMessage("a <> b")

        assertTrue("el texto de alrededor tiene que quedarse: $salida", salida.contains("a ") && salida.contains(" b"))
    }

    @Test
    fun `una etiqueta que no cierra no se come el texto que sigue`() {
        val salida = markdownForMessage("antes <color=#FF0000 despues")

        assertTrue("el texto posterior tiene que seguir ahi", salida.contains("despues"))
    }

    // ── La cercada manda: dentro es codigo (caso limite 1) ────────────────────────────────

    @Test
    fun `dentro de una cercada no se toca nada`() {
        val crudo = "Antes\n```html\n<color=#FF0000>esto es codigo</color>\n<url=x>y</url>\n```\nDespues"

        assertEquals(crudo, markdownForMessage(crudo))
    }

    @Test
    fun `una cercada de tildes cuenta igual que una de backticks`() {
        val crudo = "~~~xml\n<color=#FF0000>codigo</color>\n~~~"

        assertEquals(crudo, markdownForMessage(crudo))
    }

    @Test
    fun `fuera de la cercada si se limpia`() {
        val salida = markdownForMessage("```\n<color=#FF0000>codigo</color>\n```\n<color=#00FF00>texto</color>")

        assertTrue(salida.contains("<color=#FF0000>codigo</color>"))
        assertFalse(salida.contains("#00FF00"))
    }

    @Test
    fun `una cercada sin cerrar protege hasta el final`() {
        val crudo = "```kotlin\n<color=#FF0000>a medio escribir</color>"

        assertEquals(crudo, markdownForMessage(crudo))
    }

    @Test
    fun `una linea que empieza con backticks pero no es una cerca no protege`() {
        // El nombre de lenguaje no lleva espacios, asi que esto no es una cercada y hay que limpiarla.
        val salida = markdownForMessage("```kotlinly <color=#FF0000>x</color>")

        assertFalse("no es una cerca: hay que limpiar", salida.contains("#FF0000"))
    }

    @Test
    fun `una cercada con nombre de lenguaje protege igual`() {
        // ```kotlin es una cercada igual que ```: si el patron exigiera una linea de solo la marca, el
        // bloque de codigo mas comun de todos se limpiaria y su contenido se veria roto.
        val crudo = "```kotlin\n<color=#FF0000>val x = \"#FF0000\"\n```"

        assertEquals(crudo, markdownForMessage(crudo))
    }

    // ── Varias lineas a la vez (RF-98) ─────────────────────────────────────────────────────

    @Test
    fun `la limpieza respeta los saltos de linea`() {
        val crudo = "<color=#FF0000>uno</color>\n<color=#00FF00>dos</color>\n<color=#0000FF>tres</color>"

        assertEquals("uno\ndos\ntres", markdownForMessage(crudo))
    }

    @Test
    fun `una lista de tarea propia seguida de una markdown se pintan las dos`() {
        val salida = markdownForMessage("<item checked=\"true\">hecho</item>\n- [ ] pendiente")

        assertTrue(salida.contains("- [x] hecho"))
        assertTrue(salida.contains("- [ ] pendiente"))
    }
}
