package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Invariantes de las superficies de Markdown fuera del chat (spec 018, RNF-53, RNF-54, RNF-55, RNF-56).
 *
 * Son **pruebas puras** que leen el texto de los ficheros y no necesitan emulador ni entorno de pruebas, que
 * es la única forma de que corran en la plataforma de desarrollo. Miden el **texto**, no el comportamiento: por
 * eso se complementan con un `compileDebugKotlin` en verde, y por eso no sustituyen a probar en dispositivo.
 *
 * Cada recorrido lleva una **autocomprobación** de que está leyendo el fichero. Sin ella, un detector roto
 * devuelve una lista vacía y la prueba pasa **por vacío** — que es como se dio por buena una prueba de este
 * tipo en su primer intento en el proyecto: `^` no ancla al principio de línea en Kotlin sin
 * `RegexOption.MULTILINE`, y cuatro de los cinco recorridos no veían nada.
 */
class MarkdownSurfacesInvariantsTest {

    private companion object {
        const val TOML = "../gradle/libs.versions.toml"
        const val BUILD_APP = "build.gradle.kts"
        const val LICENCIAS = "src/main/assets/oss-licenses.json"
        const val RESPUESTAS = "../THIRD-PARTY-NOTICES"

        const val LEGAL = "src/main/java/com/example/ui/settings/LegalInfoScreen.kt"
        const val ACTUALIZACION = "src/main/java/com/example/ui/settings/UpdateInfoScreen.kt"
        const val NOVEDADES = "src/main/java/com/example/ui/settings/ChangelogSheet.kt"
        const val CHAT = "src/main/java/com/example/ui/AiChatScreen.kt"
        const val LISTA = "src/main/java/com/example/ui/MainListScreen.kt"
        const val PLEGABLE = "src/main/java/com/example/ui/NoteContentBlockRenderer.kt"

        /** Piso de cualquier recorrido de grep: si baja de aquí, el recorrido está roto, no el código. */
        const val MINIMO_DE_LINEAS = 5

        /** La librería que esta spec retira (RF-107). */
        const val LIBRERIA_RETIRADA = "compose-markdown"

        /** Los nueve idiomas. Los cinco textos legales tienen que estar en todos (constitución 6). */
        val LOCALES = listOf(
            "values", "values-es-rVE", "values-pt-rBR", "values-fr", "values-it",
            "values-en-rGB", "values-es-rES", "values-pt-rPT", "values-b+es+419"
        )

        val TEXTOS_LEGALES = listOf(
            "legal_section_terms_content",
            "legal_section_ai_content",
            "legal_section_sovereignty_content",
            "legal_section_drive_content",
            "legal_section_security_content"
        )

        /**
         * Etiquetas del markup heredado de las notas. Un texto legal que las llevara **no** podría pintarse con
         * el motor de GFM, que no las entiende: se verían tal cual. Es el otro lado de la línea que decide qué
         * superficies se migran (RF-104).
         */
        val MARCA_PROPIO = listOf("<color=", "<bg=", "<size=", "<font=", "<normal>", "<item", "<url=", "<eq>")

        /** Construcciones de Markdown. Sin al menos una, el texto no es Markdown y no tiene sentido migrarlo. */
        val MARCA_MARKDOWN = listOf("**", "- ", "1. ", "## ", "[")

        /** El composable que pinta Markdown en la app, ya sin la librería de terceros. */
        const val COMPOSABLE = "MarkdownText"
    }

    private fun leer(ruta: String): String = File(ruta).readText()

    /**
     * Lee un fichero y **comprueba que se ha leído de verdad**.
     *
     * Sin esta comprobación, una ruta equivocada hace que `readText` lance y la prueba se vaya al fallo por un
     * motivo que no es el que vigila; y con un recorrido de `grep` vacío, la prueba pasa **por vacío**, que es
     * peor: verde sin comprobar nada.
     */
    private fun leerComprobando(ruta: String, etiqueta: String): String {
        val texto = leer(ruta)
        assertTrue(
            "no se está leyendo $etiqueta ($ruta): el recorrido está roto, no el código",
            texto.lines().size >= MINIMO_DE_LINEAS
        )
        return texto
    }

    // ── RF-104: la pantalla legal ───────────────────────────────────────────────────────────

    @Test
    fun `los cinco textos legales existen y traen marcado en los nueve idiomas`() {
        // Constitución 6: los nueve idiomas a la vez. Aquí lo que se vigila es que un idioma no se quede atrás
        // al traducir y pierda el marcado —la negrita que en `values/` sí está— porque el motor de GFM solo
        // pinta lo que hay: una traducción sin `**` se vería sin negrita y **no** se nota en una pantalla
        // comparada con la de al lado.
        val conMarcadoPorLocale = mutableMapOf<String, Set<String>>()

        for (locale in LOCALES) {
            val fuente = leerComprobando("src/main/res/$locale/strings.xml", "los textos de $locale")
            val marcados = mutableSetOf<String>()
            for (nombre in TEXTOS_LEGALES) {
                val valor = valorDe(fuente, nombre)
                    ?: error("$locale no define $nombre: la constitución 6 pide los nueve idiomas a la vez")
                assertTrue("$locale/$nombre está vacío", valor.isNotBlank())
                if (valor.contains("**")) marcados.add(nombre)
            }
            conMarcadoPorLocale[locale] = marcados
        }

        // Autocomprobación: si el recorrido no vio nada, la comparación de abajo daría verde por vacío.
        assertTrue("ningún texto legal trae negrita en ningún idioma: el recorrido está roto",
            conMarcadoPorLocale.values.any { it.isNotEmpty() })

        val referencia = conMarcadoPorLocale.getValue("values")
        conMarcadoPorLocale.forEach { (locale, marcados) ->
            assertEquals(
                "$locale no trae la misma negrita que values/ (${referencia}): una traducción se quedó sin marcar",
                referencia,
                marcados
            )
        }
    }

    @Test
    fun `el texto legal es markdown y no markup heredado`() {
        // Lo que decide qué pantallas se migran (RF-104). Si alguien mete <color=#FF0000> en un texto legal,
        // esta prueba se pone roja y dice por qué: el motor de GFM no lo pinta y saldría a la vista.
        for (locale in LOCALES) {
            val fuente = leerComprobando("src/main/res/$locale/strings.xml", "los textos de $locale")
            for (nombre in TEXTOS_LEGALES) {
                val valor = valorDe(fuente, nombre) ?: error("$locale no define $nombre")
                val heritadas = MARCA_PROPIO.filter { valor.contains(it) }
                assertTrue(
                    "$locale/$nombre lleva markup heredado ($heritadas), que el motor de GFM no sabe pintar",
                    heritadas.isEmpty()
                )
            }
        }

        // Y al menos un texto debe traer construcción de Markdown, o la migración no estaría justificada.
        var conMarkdown = 0
        for (locale in LOCALES) {
            val fuente = leer("src/main/res/$locale/strings.xml")
            for (nombre in TEXTOS_LEGALES) {
                val valor = valorDe(fuente, nombre) ?: error("$locale no define $nombre")
                if (MARCA_MARKDOWN.any { valor.contains(it) }) conMarkdown++
            }
        }
        assertTrue("ningún texto legal trae Markdown: el recorrido está roto", conMarkdown > 0)
    }

    @Test
    fun `la pantalla legal pinta con el motor y no con el analizador`() {
        val fuente = leerComprobando(LEGAL, "la pantalla legal")

        assertTrue(
            "la pantalla legal debe entregar su texto al motor de GFM",
            fuente.contains("$COMPOSABLE(")
        )
        assertFalse(
            "la pantalla legal ya no debe usar el analizador del markup heredado",
            fuente.contains("RichTextParser")
        )
        assertTrue(
            "la pantalla legal debe envolver su contenido con el motor",
            fuente.contains("ProvidesMarkdownEngine")
        )
    }

    @Test
    fun `el texto legal trae un enlace que se abre entero`() {
        // El único enlace del texto legal va metido en paréntesis en los nueve idiomas. Si la pantalla se
        // migra y el enlace no se abre entero, RF-106 no se cumple en su único caso real (caso límite 5).
        var conEnlace = 0
        for (locale in LOCALES) {
            val fuente = leer("src/main/res/$locale/strings.xml")
            val valor = valorDe(fuente, "legal_section_security_content")
                ?: error("$locale no define legal_section_security_content")
            val direcciones = Regex("""https?://[^\s<>()]+""").findAll(valor).map { it.value }.toList()
            assertTrue("$locale no trae dirección en el aviso de seguridad", direcciones.isNotEmpty())
            direcciones.forEach { conEnlace++ }
        }
        assertTrue("no se ha encontrado ningún enlace: el recorrido está roto", conEnlace > 0)
    }

    // ── RF-105: las dos superficies de novedades ───────────────────────────────────────────

    @Test
    fun `las dos superficies de novedades pintan con el motor`() {
        for ((etiqueta, ruta) in listOf("actualización" to ACTUALIZACION, "novedades" to NOVEDADES)) {
            val fuente = leerComprobando(ruta, "la pantalla de $etiqueta")
            assertTrue(
                "la pantalla de $etiqueta debe usar el composable de Markdown de la app",
                fuente.contains("$COMPOSABLE(")
            )
            assertTrue(
                "la pantalla de $etiqueta debe envolver su contenido con el motor",
                fuente.contains("ProvidesMarkdownEngine")
            )
            assertFalse(
                "la pantalla de $etiqueta no debe importar la librería que esta spec retira (RF-105)",
                fuente.contains("dev.jeziellago")
            )
        }
    }

    @Test
    fun `las notas de una release se entregan literales, sin adaptar`() {
        // Caso límite 2 de la spec: las notas se escriben en GitHub, donde el HTML crudo es legítimo.
        // Adaptarlas retiraría etiquetas que ahí sí significan algo.
        for ((etiqueta, ruta) in listOf("actualización" to ACTUALIZACION, "novedades" to NOVEDADES)) {
            val fuente = leerComprobando(ruta, "la pantalla de $etiqueta")
            assertFalse(
                "la pantalla de $etiqueta no debe adaptar el markup heredado: es Markdown de GitHub",
                fuente.contains("markdownForMessage")
            )
            assertFalse(
                "la pantalla de $etiqueta no debe importar la adaptación del chat",
                fuente.contains("ChatMarkdownSource")
            )
        }
    }

    // ── RF-108: el chat adapta una sola vez ────────────────────────────────────────────────

    @Test
    fun `el texto de un mensaje se adapta una sola vez`() {
        // El doble que hace la 017: la burbuja lo adapta dentro de un `remember` y el composable lo adapta
        // otra vez **sin** `remember`, o sea en cada recomposición. El recorte es: **cero** en la burbuja y
        // **uno** en el composable.
        val composable = leerComprobando(
            "src/main/java/com/example/ui/markdown/ChatMarkdownText.kt", "el composable de la burbuja"
        )
        val chat = leerComprobando(CHAT, "la pantalla del chat")
        val llamada = Regex("""\bmarkdownForMessage\s*\(""")

        assertEquals(
            "la burbuja no debe adaptar el texto: lo hace el composable, una sola vez",
            0, llamada.findAll(chat).count()
        )
        assertEquals(
            "el composable debe adaptar el texto exactamente una vez",
            1, llamada.findAll(composable).count()
        )
    }

    // ── RNF-56: el chat y las superficies de nota no cambian ───────────────────────────────

    @Test
    fun `solo la burbuja adapta el markup heredado`() {
        // RNF-56. La adaptación vive en el composable de la burbuja, no en la pantalla del chat: así se
        // ejecuta una vez por entrega y no en cada recomposición (RF-108). Las superficies de Ajustes no la
        // usan, y eso lo fija su propia prueba.
        val composable = leerComprobando(
            "src/main/java/com/example/ui/markdown/ChatMarkdownText.kt", "el composable de la burbuja"
        )
        val chat = leerComprobando(CHAT, "la pantalla del chat")

        assertTrue(
            "el composable de la burbuja debe adaptar el markup heredado",
            composable.contains("ChatMarkdownSource.markdownForMessage")
        )
        assertFalse(
            "la pantalla del chat no debe adaptar el texto: lo hace el composable",
            chat.contains("markdownForMessage")
        )
    }

    @Test
    fun `la vista previa de la lista y el bloque plegable no cambian de analizador`() {
        // RNF-56. Su contenido es markup heredado, no Markdown: migrarlas perdería <color>, <bg>, <size> y la
        // vista previa además perdería su recorte de 3-4 líneas.
        for ((etiqueta, ruta) in listOf("la vista previa" to LISTA, "el bloque plegable" to PLEGABLE)) {
            val fuente = leerComprobando(ruta, etiqueta)
            assertTrue(
                "$etiqueta debe seguir con el analizador de texto enriquecido",
                fuente.contains("RichTextParser")
            )
            assertFalse(
                "$etiqueta no debe usar el motor de GFM: su contenido no es Markdown",
                fuente.contains("$COMPOSABLE(")
            )
        }
    }

    // ── RF-107: la librería retirada no está en ninguna parte ──────────────────────────────

    @Test
    fun `la libreria retirada no aparece en el catalogo`() {
        val toml = leerComprobando(TOML, "el catálogo")
        assertFalse(
            "$LIBRERIA_RETIRADA no debe seguir en el catálogo (RF-107)",
            toml.contains(LIBRERIA_RETIRADA)
        )
    }

    @Test
    fun `la libreria retirada no aparece en las dependencias de la app`() {
        val build = leerComprobando(BUILD_APP, "el fichero de construcción")
        assertFalse(
            "$LIBRERIA_RETIRADA no debe seguir siendo dependencia de la app (RF-107)",
            build.contains("libs.compose.markdown")
        )
    }

    @Test
    fun `la libreria retirada no aparece ni en las licencias ni en los avisos legales`() {
        for ((etiqueta, ruta) in listOf("el inventario" to LICENCIAS, "los avisos" to RESPUESTAS)) {
            val fuente = leerComprobando(ruta, etiqueta)
            assertFalse(
                "$LIBRERIA_RETIRADA no debe seguir en $etiqueta (RF-107)",
                fuente.contains(LIBRERIA_RETIRADA)
            )
        }
    }

    @Test
    fun `el inventario de licencias sigue listando el motor que si se usa`() {
        // Quitar una entrada es fácil; lo que no debe pasar es quitar de más.
        val inventario = leerComprobando(LICENCIAS, "el inventario")
        assertTrue("el motor debe seguir en el inventario", inventario.contains("Markwon"))
        assertTrue("el motor debe seguir en los avisos legales", leer(RESPUESTAS).contains("Markwon"))
    }

    // ── RNF-53: las versiones que si importan, leyéndolas ────────────────────────────────

    @Test
    fun `markwon sigue en la version que ya estaba en el apk`() {
        // RNF-53. Commonmark se comprobaba aquí también, y **ya no**: la spec 019 quitó las tres declaraciones
        // que lo fijaban, así que su versión pasó a ser cosa de `markwon-core` y de nadie más. Una línea de
        // catálogo sin nada detrás no fija nada: solo lo aparenta, y un invariante que la comprueba daría verde
        // sobre un control que ya no existe. Lo que **sí** protege es que markwon no se mueva.
        val toml = leerComprobando(TOML, "el catálogo")

        val markwon = Regex("""^markwon\s*=\s*"([^"]+)"""", RegexOption.MULTILINE).find(toml)
        assertTrue("no se encuentra la asignación de markwon", markwon != null)
        assertEquals("markwon", "4.6.2", markwon!!.groupValues[1])
    }

    @Test
    fun `commonmark no queda declarado en ninguna parte`() {
        // Si volviera a declararse, dejaría de ser cosa de `markwon-core` y volvería a haber un sitio donde
        // mover su versión sin que nadie lo note.
        val toml = leerComprobando(TOML, "el catálogo")
        assertFalse(
            "el catálogo no debe volver a declarar commonmark (RNF-60)",
            Regex("""^commonmark-(core|tables|strikethrough)\s*=""", RegexOption.MULTILINE).containsMatchIn(toml)
        )
        assertFalse(
            "ningún módulo debe declarar commonmark",
            File(BUILD_APP).readText().contains("libs.commonmark") ||
                File("../editor-de-codigo/build.gradle.kts").readText().contains("libs.commonmark")
        )
    }

    @Test
    fun `las seis piezas del motor siguen declaradas`() {
        val toml = leerComprobando(TOML, "el catálogo")
        listOf("core", "ext-tables", "ext-strikethrough", "ext-tasklist", "html", "linkify").forEach { pieza ->
            assertTrue("falta la pieza $pieza del motor", toml.contains("io.noties.markwon\", name = \"$pieza\""))
        }
    }

    // ── RNF-54: pintar no hace red ────────────────────────────────────────────────────────

    @Test
    fun `el modulo del motor no tiene ninguna clase de imagenes de terceros`() {
        // RNF-54 y RNF-57. **Cambiado en la spec 019**: el módulo del motor ya registra imágenes, porque es lo
        // que permite que la vista previa del editor de código enseñe las suyas. La regla dejó de ser «aquí no hay
        // imágenes» y pasó a ser «aquí no hay nada que sepa salir a la red»; la de «el chat no enseña imágenes» la
        // sostiene `MarkdownRenderInvariantsTest`, mirando **quién** pasa el cargador.
        val fuente = leerComprobando(
            "../markdown/src/main/java/com/estrin217/markdown/MarkdownEngine.kt", "el módulo del motor"
        )
        listOf("Coil", "coil3", "okhttp", "HttpURLConnection", "java.net.URL", "Glide").forEach {
            assertFalse("el motor no debe nombrar $it: una imagen podría salir a la red (RNF-54, RNF-57)", fuente.contains(it))
        }
        val plugins = fuente.lines().filter { it.contains(".usePlugin(") }
        assertTrue("no se encuentra la lista de plugins: el recorrido está roto", plugins.size >= 6)
    }

    @Test
    fun `el composable de markdown no crea un cargador de imagenes`() {
        val fuente = leerComprobando(
            "../markdown/src/main/java/com/estrin217/markdown/MarkdownText.kt", "el composable de Markdown"
        )
        assertFalse(
            "el composable no debe construir un cargador de imágenes (RNF-54)",
            Regex("""ImageLoader|AsyncDrawableLoader|coil3|coil\.""").containsMatchIn(fuente)
        )
    }

    // ── Auxiliares ────────────────────────────────────────────────────────────────────────

    /**
     * El valor de un `<string>` por su nombre, con las entidades **sin** resolver.
     *
     * Se queda con el texto tal cual porque lo que se busca son construcciones de Markdown y etiquetas: si se
     * des-escapara el `&amp;` primero, un `<` de una entidad podría pasar por una etiqueta que no está.
     */
    private fun valorDe(fuente: String, nombre: String): String? =
        Regex("""<string name="${Regex.escape(nombre)}"\s*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
            .find(fuente)
            ?.groupValues
            ?.get(1)
}