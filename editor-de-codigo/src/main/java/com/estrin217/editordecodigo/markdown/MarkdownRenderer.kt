package com.estrin217.editordecodigo.markdown

/**
 * Colores de la vista previa, **en hexadecimal y no en `Color`**.
 *
 * Es la frontera que permite que [MarkdownRenderer] sea lógica pura (RNF-26): si el tema fuera un
 * `Color` de Compose, el renderizador importaría `androidx.compose` y sus pruebas no se podrían
 * ejecutar en esta plataforma, donde no hay runtime nativo para Robolectric.
 *
 * El `Color` → hexadecimal ocurre en el Composable, que sí puede usar Compose. Es el mismo trick que
 * `SyntaxTheme` usa en el otro sentido: el tokenizador es puro y el color llega después.
 */
data class PreviewTheme(
    val background: String,
    val text: String,
    val accent: String,
    val codeBackground: String,
    val quoteBar: String,
    val border: String
)

/**
 * Convierte Markdown en el documento HTML de la vista previa.
 *
 * ## La cadena, en orden
 *
 * 1. commonmark parsea con las extensiones de **tablas** y **tachado** de GFM.
 * 2. `escapeHtml(true)` neutraliza el HTML incrustado: `<script>` sale como texto (RF-71).
 * 3. [neutralizeRemoteSources] quita lo que pueda salir a la red (RF-70).
 * 4. [markTaskItems] convierte `- [ ]` en casilla, porque la 0.13 no trae esa extensión.
 * 5. Se envuelve en un documento con el CSS del tema, **sin fuentes ni hojas externas**.
 *
 * ## Por qué no se sube a commonmark 0.22
 *
 * La 0.22 trae `ext-gfm-task-list-items` y `ext-gfm-footnotes`, pero **subiría la versión transitiva
 * que usa `compose-markdown` 0.7.2** (en `UpdateInfoScreen` y `ChangelogSheet`), y un salto de API
 * en 0.13 → 0.22 no es gratis. Seis líneas de post-proceso sobre HTML propio son más baratas que ese
 * riesgo.
 *
 * Lógica pura: sin `androidx.compose`, sin `Context`, sin `R`, sin reloj (RNF-26).
 */
object MarkdownRenderer {

    /**
     * Las extensiones van **a los dos constructores**, no solo al del parser.
     *
     * Es el error que costó una vuelta entera: registrando las extensiones solo en el parser, la
     * tabla y el tachado se parseaban bien y luego el renderizador, que no conocía esos nodos, los
     * pintaba **vacíos** — `<p></p>` en lugar de `<del>`. Una extensión de commonmark es a la vez
     * `ParserExtension` y `HtmlRendererExtension`, y hay que dársela a los dos.
     */
    private val extensions = listOf(
        org.commonmark.ext.gfm.tables.TablesExtension.create(),
        org.commonmark.ext.gfm.strikethrough.StrikethroughExtension.create()
    )

    private val parser = org.commonmark.parser.Parser.builder()
        .extensions(extensions)
        .build()

    private val htmlRenderer = org.commonmark.renderer.html.HtmlRenderer.builder()
        .escapeHtml(true)
        .extensions(extensions)
        .build()

    /** Solo el `<body>`, sin el documento. Es lo que comprueban las pruebas sin montar el documento. */
    fun toHtmlBody(markdown: String): String =
        markTaskItems(neutralizeRemoteSources(htmlRenderer.render(parser.parse(markdown))))

    /** El documento completo, listo para `loadDataWithBaseURL`. */
    fun toHtmlDocument(markdown: String, theme: PreviewTheme): String {
        val body = toHtmlBody(markdown)
        return buildString {
            append("<!DOCTYPE html><html><head><meta charset=\"utf-8\">")
            append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
            append("<style>").append(css(theme)).append("</style></head><body>")
            append(body)
            append("</body></html>")
        }
    }

    /**
     * Sustituye todo destino que pueda sacar una petición de red.
     *
     * Abrir un Markdown **no debe generar tráfico**: una imagen remota avisaría a un tercero de que
     * ese documento existe, y el usuario no lo pidió (principio 5 de la constitución). Las imágenes
     * **locales** no se tocan, que es para lo que está `allowFileAccess` en el `WebView`.
     *
     * `javascript:` y `data:` desaparecen enteros en lugar de marcarse: no tienen nada que enseñar.
     */
    private fun neutralizeRemoteSources(html: String): String {
        var result = html
        result = SRC_RE.replace(result) { match ->
            // El grupo 1 son los atributos **anteriores** al `src` (aquí, ninguno) y el 2 es la URL.
            // Leer el 1 hacía que `url` saliera vacía y el bloqueo no llegara a aplicarse nunca.
            val url = match.groupValues[2].trim()
            when {
                // ⚠️ La URL **no** se guarda en ningún atributo, ni en un `title` para que se vea al
                // pasar el ratón: seguiría siendo un destino que el WebView podría intentar, y la
                // prueba que exige que no aparezca la URL es la que lo fija.
                url.startsWith("http://") || url.startsWith("https://") || url.startsWith("//") ->
                    "<span class=\"blocked-image\">$BLOCKED_MARKER</span>"
                url.startsWith("javascript:") || url.startsWith("data:") -> ""
                else -> match.value
            }
        }
        return result
    }

    /**
     * commonmark cierra las imágenes como `<img ... />`, con barra. El patrón acepta las dos formas
     * porque un `>` a secas no aparece en su salida, y un patrón que no case se traduce en una
     * imagen remota **sin bloquear**, que es justo el fallo que esto previene.
     *
     * La cadena va con escapes normales y **no** cruda (`"""…"""`): en una cruda, `\s` es una barra
     * seguida de una `s` y no un espacio en blanco, así que el patrón no casa con nada. Pasó aquí.
     */
    private val SRC_RE = Regex("<img([^>]*?)\\ssrc=\"([^\"]*)\"([^>]*?)/?>")
    /**
     * Marcador de una imagen bloqueada.
     *
     * Se usa un `<span>`, no un `data:` URI dentro del `src`: un SVG en línea **no** se renderiza
     * como imagen ahí, y un `data:` se descartaría a sí mismo por la misma regla que bloquea las
     * remotas. Un recuadro con el nombre del host deja claro que la imagen está ahí y por qué no se
     * ve, en vez de dejar un hueco mudo.
     */
    private val BLOCKED_MARKER = "imagen remota bloqueada"

    /**
     * `- [ ]` → casilla. La 0.13 no trae la extensión de listas de tarea, así que el texto llega tal
     * cual dentro del `<li>` y se convierte aquí, sobre el HTML que ya es nuestro.
     *
     * El `</li>` lo pone el propio patrón ([TASK_ITEM_RE]), porque el que inserta aquí lo dejaría
     * duplicado y el `<ul>` quedaría con una etiqueta de más.
     */
    private fun markTaskItems(html: String): String =
        html.replace(TASK_ITEM_RE) { match ->
            val checked = match.groupValues[1].lowercase() == "x"
            "<li class=\"task\"><input type=\"checkbox\" disabled${if (checked) " checked" else ""}> " +
                match.groupValues[2].trim() +
                "</li>"
        }

    /** Solo al principio del `<li>`: un `[x]` dentro de un párrafo no es una casilla. */
    private val TASK_ITEM_RE = Regex("<li>\\s*(?:<p>)?\\[([ xX])\\]\\s*([\\s\\S]*?)</li>")

    /**
     * CSS embebido. Sin `@import`, sin `<link>` y sin fuentes remotas: nada de lo que se pinta puede
     * salir a la red, y eso también lo comprueba una prueba.
     */
    private fun css(theme: PreviewTheme): String = """
        html,body{margin:0;padding:0;background:${theme.background};color:${theme.text};}
        body{padding:16px;overflow-wrap:break-word;word-wrap:break-word;
             font-family:sans-serif;font-size:15px;line-height:1.6;}
        h1,h2,h3,h4,h5,h6{margin:1.2em 0 .5em;line-height:1.25;}
        h1{font-size:1.9em;border-bottom:1px solid ${theme.border};padding-bottom:.3em;}
        h2{font-size:1.5em;border-bottom:1px solid ${theme.border};padding-bottom:.3em;}
        h3{font-size:1.25em;} h4{font-size:1.05em;} h5{font-size:1em;} h6{font-size:.9em;color:${theme.accent};}
        p{margin:.6em 0;}
        a{color:${theme.accent};}
        code{background:${theme.codeBackground};padding:.15em .35em;border-radius:4px;
             font-family:monospace;font-size:.9em;}
        pre{background:${theme.codeBackground};border:1px solid ${theme.border};border-radius:6px;
            padding:12px;overflow-x:auto;}
        pre code{background:none;padding:0;}
        blockquote{margin:.8em 0;padding:.2em 1em;border-left:4px solid ${theme.quoteBar};color:${theme.text};}
        table{border-collapse:collapse;margin:1em 0;display:block;overflow-x:auto;max-width:100%;}
        th,td{border:1px solid ${theme.border};padding:6px 12px;text-align:left;}
        th{background:${theme.codeBackground};}
        hr{border:none;border-top:1px solid ${theme.border};margin:1.6em 0;}
        img{max-width:100%;height:auto;}
        .blocked-image{display:inline-block;padding:2px 8px;border:1px dashed ${theme.quoteBar};
                       border-radius:4px;color:${theme.quoteBar};font-size:.85em;}
        li.task{list-style:none;margin-left:-1.2em;}
        li.task input{margin-right:.5em;}
        ul,ol{padding-left:1.6em;}
    """.trimIndent()
}