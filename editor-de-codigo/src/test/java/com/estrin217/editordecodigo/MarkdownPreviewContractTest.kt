package com.estrin217.editordecodigo

import com.estrin217.editordecodigo.markdown.MarkdownPreview
import com.estrin217.editordecodigo.markdown.PreviewTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El contrato entre quien **genera** el HTML y quien lo **muestra** (spec 012, arreglo T2/T3).
 *
 * Este fichero existe por un bug que **solo se vio en dispositivo**: `MarkdownPreview` volvía a
 * pasar el documento por `MarkdownRenderer`, así que commonmark lo procesaba dos veces y la segunda
 * lo escapaba entero. La pantalla mostraba `<!DOCTYPE html>` y `<h2>Uso rápido</h2>` como texto.
 *
 * ## Por qué ningún test de cada pieza lo cazó
 *
 * `MarkdownRendererTest` comprobaba que el renderizador funciona —y funciona—, y `PreviewLayoutTest`
 * que se componen los paneles correctos —y se componen—. El fallo estaba en **quién llamaba a qué**,
 * un cruce que no existía como código y por tanto no se podía probar.
 *
 * La solución fue sacar el cruce a [MarkdownPreview], que ahora es el **único** sitio donde se pasa
 * de Markdown a documento, y este test lo ejerce de verdad.
 */
class MarkdownPreviewContractTest {

    private val theme = PreviewTheme("#101010", "#D0D0D0", "#569CD6", "#1E1E1E", "#4A4A4A", "#2A2A2A")

    /** El camino completo, con el código real: generar una vez y cargar lo generado. */
    private fun whatTheScreenShows(markdown: String): String =
        MarkdownPreview.documentToLoad(MarkdownPreview.render(markdown, theme))

    @Test
    fun `el documento que ve la pantalla sale renderizado, no escapado`() {
        val visible = whatTheScreenShows("## Seccion\n\nTexto.\n")

        assertTrue("el h2 deberia salir vivo", visible.contains("<h2>Seccion</h2>"))
        assertFalse("el h2 NO deberia estar escapado", visible.contains("&lt;h2&gt;"))
        assertFalse("el doctype no deberia estar escapado", visible.contains("&lt;!DOCTYPE"))
        assertFalse("el cierre del body no deberia estar escapado", visible.contains("&lt;/body&gt;"))
    }

    @Test
    fun `el css sale vivo y no como texto`() {
        val visible = whatTheScreenShows("texto\n")
        assertTrue(visible.contains("<style>"))
        assertFalse(visible.contains("&lt;style&gt;"))
    }

    @Test
    fun `no hay ni una etiqueta escapada en un documento normal`() {
        val visible = whatTheScreenShows(
            "# Uno\n\n## Dos\n\n- [ ] tarea\n\n`codigo`\n\n| a | b |\n| - | - |\n| 1 | 2 |\n"
        )
        listOf("&lt;h1", "&lt;h2", "&lt;style", "&lt;table", "&lt;!DOCTYPE", "&lt;li").forEach { escapada ->
            assertFalse("el documento se escapo a si mismo: aparece $escapada", visible.contains(escapada))
        }
    }

    @Test
    fun `documentToLoad entrega el documento sin volver a convertirlo`() {
        // Esta es la aserción que ata el contrato: si `documentToLoad` volviera a pasar por el
        // renderizador, el bug volvería y ESTE test se pondría rojo.
        val markdown = "# Titulo\n\nTexto con **fuerte**.\n"
        val renderizado = MarkdownPreview.render(markdown, theme)

        assertEquals(renderizado, MarkdownPreview.documentToLoad(renderizado))
        assertFalse(MarkdownPreview.documentToLoad(renderizado).contains("&lt;h1&gt;"))
    }

    @Test
    fun `reprocesar el documento si lo cambia y por eso no se puede re-renderizar`() {
        // Documenta el por qué de la regla. Reprocesar un documento ya renderizado **sí** lo altera
        // (lo escapa), así que un doble render no es invisible: es justo lo que se vio en pantalla.
        val markdown = "# Titulo\n\nTexto con **fuerte**.\n"
        val primera = MarkdownPreview.render(markdown, theme)
        val segunda = MarkdownPreview.render(primera, theme)

        assertFalse("reprocesar deberia cambiar el documento", primera == segunda)
        assertTrue("la segunda pasada escapa lo que la primera habia renderizado", segunda.contains("&lt;h1&gt;"))
    }

    @Test
    fun `el texto del markdown se ve y las marcas de bloque desaparecen`() {
        val visible = whatTheScreenShows("Titulo\n\n- [ ] pendiente\n\n```kotlin\nval x = 1\n```\n")

        assertTrue(visible.contains("Titulo"))
        assertTrue(visible.contains("pendiente"))
        assertTrue(visible.contains("val x = 1"))
        assertFalse("no deberia quedar el marcador de tarea", visible.contains("- [ ] pendiente"))
        assertFalse("no deberia quedar la cerca", visible.contains("```kotlin"))
    }

    @Test
    fun `el documento tiene la forma completa esperada`() {
        val visible = whatTheScreenShows("# T\n")
        assertTrue(visible.startsWith("<!DOCTYPE html>"))
        assertTrue(visible.trim().endsWith("</html>"))
        assertEquals(1, Regex("<style>").findAll(visible).count())
    }
}