package com.estrin217.editordecodigo

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Invariantes de la vista previa sin `WebView` (spec 019, RF-110, RNF-57, RNF-60, RNF-61).
 *
 * Son **pruebas puras** que leen el texto de los ficheros y no necesitan emulador ni entorno de pruebas, que es
 * la única forma de que corran en la plataforma de desarrollo. Miden el **texto**, no el comportamiento: por eso
 * se complementan con un `compileDebugKotlin` en verde, y por eso no sustituyen a probar en dispositivo.
 *
 * Cada recorrido lleva una **autocomprobación** de que está leyendo el fichero. Sin ella, un detector roto
 * devuelve una lista vacía y la prueba pasa **por vacío** — que es como se dio por buena una prueba de este
 * tipo en su primer intento en el proyecto: `^` no ancla al principio de línea en Kotlin sin
 * `RegexOption.MULTILINE`, y cuatro de los cinco recorridos no veían nada.
 */
class MarkdownPreviewInvariantsTest {

    private companion object {
        const val BUILD = "build.gradle.kts"
        const val RAIZ = "src/main/java/com/estrin217/editordecodigo"
        const val MODULO_MARKDOWN = "../markdown/src/main/java/com/estrin217/markdown"

        /** Piso de cualquier recorrido de grep: si baja de aquí, el recorrido está roto, no el código. */
        const val MINIMO_DE_LINEAS = 5
    }

    private fun leer(ruta: String): String = File(ruta).readText()

    /** Lee y **comprueba que se ha leído de verdad**: una ruta equivocada daría verde por vacío. */
    private fun leerComprobando(ruta: String, etiqueta: String): String {
        val texto = leer(ruta)
        assertTrue(
            "no se está leyendo $etiqueta ($ruta): el recorrido está roto, no el código",
            texto.lines().size >= MINIMO_DE_LINEAS
        )
        return texto
    }

    /** Todos los `.kt` de un árbol, como texto, para poder buscar en varios a la vez. */
    private fun leerArbol(raiz: String): Map<String, String> =
        File(raiz).walkTopDown().filter { it.isFile && it.extension == "kt" }.associate {
            it.path to sinComentarios(it.readText())
        }

    /**
     * El fichero **sin comentarios**.
     *
     * Sin esto, un invariante que busca un nombre acaba comprobando el KDoc que explica **por qué** ese nombre no
     * está, y se pone rojo justo cuando el código está bien. Pasó aquí con `WebView` —que solo quedaba en
     * comentarios explicando que ya no se usa— y con `ChatMarkdownText`. Buscar en el código, no en el texto: un
     * invariante que lee comentarios no está midiendo lo que dice medir.
     */
    private fun sinComentarios(texto: String): String {
        val sinBloques = texto.replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), " ")
        return sinBloques.lines().joinToString("\n") { linea ->
            linea.substringBefore("//")
        }
    }

    // ── RF-110: ni rastro de WebView ───────────────────────────────────────────────────────

    @Test
    fun `el editor de codigo no menciona WebView en ningun sitio`() {
        // Es el requisito entero de la spec. Se recorre **todo** el árbol de `src/main`, no solo los ficheros que
        // ya sabemos: un `WebView` nuevo en un fichero que nadie miraría pasaría si el recorrido fuera parcial.
        val arbol = leerArbol("src/main")
        assertTrue("el recorrido está roto: no ha leído ningún Kotlin", arbol.size >= 10)

        val ofensores = arbol.filter { (_, texto) ->
            texto.contains("WebView") || texto.contains("webView") || texto.contains("loadDataWithBaseURL")
        }.keys
        assertTrue(
            "no debe quedar ninguna referencia a WebView en el editor de código (RF-110). Está en: $ofensores",
            ofensores.isEmpty()
        )
    }

    @Test
    fun `la vista previa se pinta con el motor del modulo markdown`() {
        val arbol = leerArbol("src/main")
        val quien = arbol.filter { (_, t) -> t.contains("MarkdownText(") }.keys
        assertTrue("no se encuentra el composable del motor en el editor de código", quien.isNotEmpty())
    }

    // ── RNF-60: fuera el generador de HTML y commonmark ────────────────────────────────────

    @Test
    fun `el generador de html ya no existe`() {
        listOf(
            "markdown/MarkdownRenderer.kt",
            "markdown/MarkdownPreview.kt",
            "ui/PreviewThemeMapping.kt",
            "ui/components/MarkdownPreview.kt"
        ).forEach { ruta ->
            assertFalse("este fichero debe estar borrado: $ruta", File("$RAIZ/$ruta").exists())
        }
    }

    @Test
    fun `sus pruebas tampoco`() {
        listOf("MarkdownRendererTest.kt").forEach { nombre ->
            assertFalse(
                "esta prueba debe estar borrada con su código: $nombre",
                File("src/test/java/com/estrin217/editordecodigo/$nombre").exists()
            )
        }
    }

    @Test
    fun `la libreria ya no declara commonmark`() {
        // Sigue en el APK, pero **por `markwon-core`**, que lo exige en `compile`. Declararlo aquí era para el
        // generador de HTML, que ya no existe.
        val build = leerComprobando(BUILD, "el fichero de construcción")
        assertFalse(
            "commonmark no debe seguir declarándose en la librería (RNF-60)",
            build.contains("libs.commonmark")
        )
    }

    // ── RNF-61: el motor es del módulo compartido ─────────────────────────────────────────

    @Test
    fun `la libreria depende del modulo markdown`() {
        val build = leerComprobando(BUILD, "el fichero de construcción")
        assertTrue(
            "la librería debe declarar el módulo del motor",
            build.contains("project(\":markdown\")")
        )
    }

    @Test
    fun `el modulo del motor es compartido y no menciona ni la IA ni el editor`() {
        // El módulo es lo que **comparten** las cinco superficies. Si su código hablara del chat o del editor
        // de código, la dependencia iría en un solo sentido y volvería a ser un problema.
        val arbol = leerArbol(MODULO_MARKDOWN)
        assertTrue("no se ha leído el módulo del motor", arbol.size >= 3)

        listOf("AiChat", "ChatMarkdown", "editordecodigo", "CodeEditor").forEach { rastro ->
            val ofensores = arbol.filter { (_, t) -> t.contains(rastro) }.keys
            assertTrue(
                "el módulo compartido no debe mencionar $rastro. Está en: $ofensores",
                ofensores.isEmpty()
            )
        }
    }

    @Test
    fun `el modulo del motor no puede sacar nada a la red`() {
        // RNF-57. El cargador de imágenes **no** puede tener una ruta de red, ni por error ni por descuido.
        val arbol = leerArbol(MODULO_MARKDOWN)
        val lista = arbol.filter { (_, t) -> t.contains("AsyncDrawableLoader") }.keys
        assertTrue("no se encuentra el cargador de imágenes", lista.isNotEmpty())

        arbol.forEach { (ruta, texto) ->
            listOf("coil", "okhttp", "HttpURLConnection", "java.net.URL").forEach { red ->
                assertFalse(
                    "el cargador no puede tener una ruta de red ($red). Está en: $ruta (RNF-57)",
                    texto.contains(red)
                )
            }
        }
    }

    @Test
    fun `la vista previa pide que el HTML NO se interprete`() {
        // RF-114. El plugin de HTML de Markwon **interpretaría** el HTML incrustado, y la spec 012 lo quería
        // **escapado**. No se puede comprobar aquí si el plugin lo interpreta o lo deja como texto —hace falta
        // Android—, así que no se confía: la vista previa lo apaga explícitamente en su llamada.
        //
        // La comprobación va en el **sitio de llamada** y no en el módulo del motor, y a propósito: el módulo lo
        // registra aconditionally porque el chat **sí** lo quiere. Buscar «no hay HtmlPlugin en el módulo» sería una
        // regla falsa que además impediría al chat pintar `<b>` y `<details>`.
        val pane = leerArbol("$RAIZ/ui").getValue("$RAIZ/ui/MarkdownPreviewPane.kt")
        assertTrue(
            "la vista previa debe apagar la interpretación del HTML incrustado (RF-114)",
            pane.contains("interpretandoHtml = false")
        )
    }

    @Test
    fun `el modulo del motor no tiene nada que sepa salir a la red`() {
        // RNF-57. Ni Coil, ni cliente HTTP, ni una clase de imágenes de terceros: solo `BitmapFactory`.
        val arbol = leerArbol(MODULO_MARKDOWN)
        assertTrue("no se ha leído el módulo del motor", arbol.size >= 4)

        arbol.forEach { (ruta, codigo) ->
            listOf("Coil", "coil3", "okhttp", "HttpURLConnection", "java.net.URL", "Glide").forEach { red ->
                assertFalse(
                    "el módulo no debe nombrar $red: una imagen podría salir a la red (RNF-57). Está en: $ruta",
                    codigo.contains(red)
                )
            }
        }
    }

    // ── RF-115 y RNF-62: la vista previa se desplaza y no bloquea ──────────────────────────

    @Test
    fun `la vista previa se puede desplazar`() {
        // El riesgo número uno de la spec. Un `WebView` se desplazaba solo; un `TextView` dentro de un panel con
        // peso **no**, y sin esto la vista previa es inútil para cualquier documento largo.
        val arbol = leerArbol("src/main")
        val quienDesplaza = arbol.filter { (_, t) -> t.contains("verticalScroll") }.keys
        assertTrue(
            "la vista previa tiene que poder desplazarse (RNF-62)",
            quienDesplaza.isNotEmpty()
        )
        assertTrue(
            "el desplazamiento tiene que estar en el panel, no en el motor compartido: el motor se usa en " +
                "superficies que ya se desplazan solas",
            quienDesplaza.any { it.contains("Preview") }
        )
    }

    @Test
    fun `el texto del preview no se genera en el ViewModel`() {
        // RF-115. Antes el ViewModel generaba el HTML en un hilo aparte; ahora ya no hay documento que generar,
        // y lo que no puede volver es a pintarse desde ahí.
        val viewModel = leerComprobando(
            "$RAIZ/ui/CodeEditorViewModel.kt", "el ViewModel del editor"
        )
        assertFalse(
            "el ViewModel no debe construir HTML ni Markdown: lo pinta el motor (RF-115)",
            viewModel.contains("MarkdownRenderer") || viewModel.contains("toHtmlDocument")
        )
        assertTrue(
            "el retardo de la vista previa debe seguir en el ViewModel (RF-115)",
            viewModel.contains("PREVIEW_DEBOUNCE_MS")
        )
    }

        // ── RNF-61: lo que no se toca ──────────────────────────────────────────────────────────

    @Test
    fun `el resaltado del panel de texto no se toca`() {
        // RNF-61. `MarkdownTokenizer` y `MarkdownInline` son el resaltado del **editor**, no de la vista previa,
        // y no tienen nada que ver con este cambio. Esta prueba no lee `git diff`: fija que los ficheros siguen
        // existiendo y con su gramática dentro, que es lo que se rompería si se hubieran vaciado por error.
        listOf("syntax/MarkdownTokenizer.kt", "syntax/MarkdownInline.kt", "syntax/MarkdownTokens.kt").forEach { ruta ->
            val texto = leerComprobando("$RAIZ/$ruta", "el resaltado del editor")
            assertTrue("$ruta parece vacío", texto.lines().size >= 20)
        }
    }

    @Test
    fun `los modos de vista previa siguen siendo los mismos`() {
        val texto = leerComprobando("$RAIZ/ui/EditorViewMode.kt", "los modos de vista")
        assertTrue("deben seguir los modos que ya había", texto.contains("PREVIEW") && texto.contains("SPLIT"))
    }
}