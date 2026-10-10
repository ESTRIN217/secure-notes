package com.estrin217.editordecodigo

import com.estrin217.editordecodigo.markdown.MarkdownRenderer
import com.estrin217.editordecodigo.markdown.PreviewTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Renderizador Markdown → HTML de la vista previa (spec 012, T5).
 *
 * JUnit puro a propósito: en esta plataforma Robolectric no tiene runtime nativo para
 * `linux/aarch64` y no se ejecuta (RNF-27).
 */
class MarkdownRendererTest {

    private val theme = PreviewTheme(
        background = "#101010",
        text = "#D0D0D0",
        accent = "#569CD6",
        codeBackground = "#1E1E1E",
        quoteBar = "#4A4A4A",
        border = "#2A2A2A"
    )

    private fun html(md: String): String = MarkdownRenderer.toHtmlDocument(md, theme)

    private fun body(md: String): String = MarkdownRenderer.toHtmlBody(md)

    // ---------------------------------------------------------------- estructura

    @Test
    fun `el documento lleva el fondo y el color del tema`() {
        val doc = html("# Hola\n")
        assertTrue(doc.startsWith("<!DOCTYPE html>"))
        assertTrue(doc.contains(theme.background))
        assertTrue(doc.contains(theme.text))
        assertTrue(doc.contains("<h1>Hola</h1>"))
    }

    @Test
    fun `un tema oscuro y uno claro producen documentos distintos`() {
        val claro = PreviewTheme("#FFFFFF", "#000000", "#005CC5", "#F6F8FA", "#CCCCCC", "#DDDDDD")
        assertTrue(html("# H\n") != MarkdownRenderer.toHtmlDocument("# H\n", claro))
    }

    @Test
    fun `la entrada vacia produce un documento valido`() {
        val doc = html("")
        assertTrue(doc.startsWith("<!DOCTYPE html>"))
        assertTrue(doc.trim().endsWith("</html>"))
    }

    @Test
    fun `el css no trae fuentes ni hojas externas`() {
        val doc = html("texto\n")
        assertFalse(doc.contains("http://") || doc.contains("https://"))
        assertFalse(doc.contains("@import"))
        assertFalse(doc.contains("<link"))
        assertFalse(doc.contains("<script"))
    }

    // ---------------------------------------------------------------- GFM

    @Test
    fun `la tabla sale como tabla de verdad`() {
        val md = "| Lema | Tipo |\n| --- | :-: |\n| lista | vi |\n"
        val out = body(md)
        assertTrue(out.contains("<table>"))
        assertTrue(out.contains("<th>Lema</th>"))
        assertTrue(out.contains("<td>lista</td>"))
        // El separador `:-:` es centrado, y el `align` sale en la celda.
        assertTrue("esperaba align=center, salio: $out", out.contains("align=\"center\""))
    }

    @Test
    fun `el tachado sale con la etiqueta de GFM`() {
        assertTrue(body("~~tachado~~\n").contains("<del>"))
        assertFalse(body("~~tachado~~\n").contains("~~"))
    }

    @Test
    fun `la lista de tarea se marca como tal`() {
        val out = body("- [ ] Pendiente\n- [x] Hecha\n")
        assertTrue("esperaba class=task", out.contains("class=\"task\""))
        assertTrue(out.contains("checkbox"))
    }

    @Test
    fun `la lista de tarea no marca las listas normales`() {
        val out = body("- uno\n- dos\n")
        assertFalse(out.contains("class=\"task\""))
        assertFalse(out.contains("checkbox"))
    }

    @Test
    fun `el encabezado conserva su nivel`() {
        assertTrue(body("### Tres\n").contains("<h3>"))
        assertTrue(body("###### Seis\n").contains("<h6>"))
    }

    // ---------------------------------------------------------------- seguridad

    @Test
    fun `el script incrustado sale escapado y no ejecutable`() {
        val out = body("<script>alert(1)</script>\n")
        assertFalse("el script debe quedar escapado", out.contains("<script>"))
        assertTrue(out.contains("&lt;script&gt;"))
    }

    @Test
    fun `el iframe incrustado sale escapado`() {
        assertFalse(body("<iframe src=\"x\"></iframe>\n").contains("<iframe"))
    }

    @Test
    fun `el manejador en linea sale escapado`() {
        // El HTML crudo lo escapa `escapeHtml(true)`, asi que la etiqueta entera es texto y el
        // manejador no llega a ser atributo. Se comprueba que no hay etiqueta viva ni atributo.
        val out = body("<img src=x onerror=alert(1)>\n")
        assertFalse("no debe quedar una img viva", out.contains("<img"))
        assertTrue(out.contains("&lt;img"))
    }

    @Test
    fun `la imagen remota se sustituye por un marcador`() {
        val out = body("![logo](https://ejemplo.com/logo.png)\n")
        assertFalse("no debe quedar la URL remota", out.contains("ejemplo.com"))
        assertTrue("esperaba el marcador, salio: $out", out.contains("blocked-image"))
    }

    @Test
    fun `el protocolo relativo tambien se bloquea`() {
        val out = body("![x](//ejemplo.com/a.png)\n")
        assertFalse(out.contains("//ejemplo.com"))
    }

    @Test
    fun `la imagen local relativa se conserva`() {
        val out = body("![diagrama](imagenes/flujo.png)\n")
        assertTrue(out.contains("imagenes/flujo.png"))
    }

    @Test
    fun `el javascript en el destino no sobrevive`() {
        // Doble barrera: `escapeHtml` escapa la etiqueta entera, de modo que el `src` deja de ser
        // un atributo; y si aun asi colara una imagen, `neutralizeRemoteSources` lo eliminaria.
        val out = body("<img src=\"javascript:alert(1)\">\n")
        assertFalse("quedo una imagen viva", out.contains("<img"))
        assertTrue("la etiqueta deberia estar escapada", out.contains("&lt;img"))
    }

    @Test
    fun `los enlaces normales no se tocan`() {
        assertTrue(body("Ver [la docs](https://ejemplo.com/docs).\n").contains("https://ejemplo.com/docs"))
    }

    // ---------------------------------------------------------------- robustez

    @Test
    fun `una tabla mal formada no rompe el render`() {
        val doc = html("| a | b\n| --- |\n| 1 |\n\ntexto\n")
        assertTrue(doc.contains("texto"))
    }

    @Test
    fun `los acentos y la puntuacion se conservan`() {
        val out = body("Año 2026, ¿qué tal? ¡Sí! Ábaco\n")
        assertTrue(out.contains("2026"))
        assertTrue(out.contains("Ábaco"))
    }

    @Test
    fun `el documento sale con las etiquetas vivas, no escapadas`() {
        // Este es el test que faltaba cuando el bug del doble render estaba vivo: la vista previa
        // enseñaba el `<!DOCTYPE html>` y el `<h2>` como texto, porque el documento pasaba por
        // commonmark dos veces y la segunda lo escapaba entero.
        //
        // Con doble render aparecen las dos cosas: la etiqueta **viva** (`<h2>`) y la **escapada**
        // (`&lt;h2&gt;`). Comprobar solo una de las dos no distinguiría los casos.
        val doc = html("## Seccion\n\nTexto.\n")
        assertTrue("el h2 deberia salir vivo", doc.contains("<h2>Seccion</h2>"))
        assertFalse("el h2 NO deberia estar escapado", doc.contains("&lt;h2&gt;"))
        assertFalse("el doctype no deberia estar escapado", doc.contains("&lt;!DOCTYPE"))
    }

    @Test
    fun `el documento no lleva html escapado de sus propias etiquetas`() {
        // Barrido general: si el documento se escapo a si mismo, TODAS sus etiquetas estan
        // escapadas. Con el doble render, doc.contains("<h1>") y doc.contains("&lt;h1&gt;") dan true
        // las dos a la vez.
        val doc = html("# Uno\n\n## Dos\n\n### Tres\n\n- [ ] tarea\n\n`codigo`\n")
        listOf("<h1>", "<h2>", "<h3>").forEach { etiqueta ->
            assertTrue("falta la etiqueta viva $etiqueta", doc.contains(etiqueta))
            assertFalse("esta escapada $etiqueta", doc.contains(etiqueta.replace("<", "&lt;")))
        }
    }

    @Test
    fun `el css del documento sale en su forma original`() {
        // El `<style>` embebido es la otra mitad de lo que se ve como texto cuando el documento se
        // escapa a si mismo.
        val doc = html("texto\n")
        assertTrue(doc.contains("<style>"))
        assertFalse(doc.contains("&lt;style&gt;"))
    }

    @Test
    fun `el texto se escapa en el html`() {
        assertTrue(body("5 < 6 y 7 > 6\n").contains("&lt;"))
    }
}