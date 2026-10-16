package com.example

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Invariantes del renderizado de Markdown del chat (spec 017, RNF-48, RNF-49, RNF-50).
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
class MarkdownRenderInvariantsTest {

    private companion object {
        const val TOML = "../gradle/libs.versions.toml"
        const val MODULO_RENDER = "../markdown/src/main/java/com/estrin217/markdown/MarkdownEngine.kt"
        const val FUENTE_PURA = "src/main/java/com/example/ui/markdown/ChatMarkdownSource.kt"
        const val PANTALLA_CHAT = "src/main/java/com/example/ui/AiChatScreen.kt"

        /** Piso de cualquier recorrido de grep: si baja de aquí, el recorrido está roto, no el código. */
        const val MINIMO_DE_LINEAS = 5

        /**
         * La versión de `commonmark-java`. **No** es un detalle: `markwon-core` la exige en `compile` y el
         * editor de código la declara en `0.13.0` porque es la que ya arrastraba la librería de Markdown del
         * changelog. Si Graddy la resolviera a otra, se romperían el chat y el editor de código **a la vez**, y
         * ninguna prueba funcional se pondría roja: es el accidente más caro y más silencioso de esta spec.
         */
        const val COMMONMARK_ESPERADA = "0.13.0"

        /** La versión del motor de GFM, que es la que ya estaba en el APK. */
        const val MARKWON_ESPERADA = "4.6.2"
    }

    private fun leer(ruta: String): String = File(ruta).readText()

    /** Lee y **comprueba que se ha leído de verdad**: una ruta equivocada haría `readText` petar por otro motivo. */
    private fun leerComprobando(ruta: String, etiqueta: String): String {
        val texto = leer(ruta)
        assertTrue("no se está leyendo $etiqueta ($ruta)", texto.lines().size >= MINIMO_DE_LINEAS)
        return texto
    }

    // ── RNF-48: cero jars nuevos ───────────────────────────────────────────────────────────

    @Test
    fun `commonmark ya no lo declara nadie`() {
        // RNF-48 y RNF-60. **Invertido por la spec 019.** Esto antes fijaba la versión en 0.13.0, y la razón era
        // que había **tres** caminos que podían moverla: la librería de Markdown de terceros (retirada en la 018)
        // y el generador de HTML de la vista previa (borrado en la 019), aparte del propio motor. Ya no queda
        // ninguno de los dos primeros, así que la versión es cosa de `markwon-core` y de nadie más, y una línea
        // de catálogo sin nada detrás **no fija nada**: solo lo aparenta.
        val toml = leerComprobando(TOML, "el catálogo")
        assertFalse(
            "commonmark no debe volver a declararse: el motor lo arrastra y nada más lo necesita (RNF-60)",
            Regex("""^commonmark-(core|tables|strikethrough)\s*=""", RegexOption.MULTILINE).containsMatchIn(toml)
        )
        assertFalse(
            "commonmark no debe volver a declararse en ningún módulo",
            File("build.gradle.kts").readText().contains("libs.commonmark")
        )
    }

    @Test
    fun `la version del motor es la que ya estaba en el apk`() {
        val toml = leer(TOML)
        assertTrue("no se está leyendo el fichero del catálogo", toml.contains("markwon"))

        val asignacion = Regex("""^markwon\s*=\s*"([^"]+)"""", RegexOption.MULTILINE).find(toml)
        assertTrue("no se encuentra la asignación de markwon", asignacion != null)
        assertTrue(
            "el motor debería seguir en $MARKWON_ESPERADA y está en ${asignacion!!.groupValues[1]}",
            asignacion.groupValues[1] == MARKWON_ESPERADA
        )
    }

    @Test
    fun `las seis piezas del motor estan declaradas`() {
        val toml = leer(TOML)
        listOf("core", "ext-tables", "ext-strikethrough", "ext-tasklist", "html", "linkify").forEach { pieza ->
            assertTrue("falta la pieza $pieza del motor", toml.contains("io.noties.markwon\", name = \"$pieza\""))
        }
    }

    @Test
    fun `compose-markdown se retira porque ya no la usa ninguna pantalla`() {
        // Invertido por la spec 018. Cuando se escribió, las otras dos pantallas (`ChangelogSheet`,
        // `UpdateInfoScreen`) seguían con su `MarkdownText` y quitarla rompía la compilación —que es la forma
        // barata de enterarse—. Con la 018 las cuatro superficies de Markdown pintan con el motor de la app
        // y la dependencia no tiene a nadie detrás. Si algún día vuelve a hacer falta, esta prueba lo dice
        // antes de que alguien la declare «porque sí».
        val toml = leer(TOML)
        assertFalse(
            "compose-markdown no debe volver al catálogo: el motor de la app pinta las cuatro superficies",
            toml.contains("compose-markdown")
        )
    }

    // ── RNF-50: pintar no hace red ──────────────────────────────────────────────────────────

    @Test
    fun `el modulo de render no tiene ninguna clase de imagenes de terceros`() {
        // RNF-50 y RNF-57. El módulo del motor **sí** puede registrar imágenes desde la spec 019 —es lo que
        // permite que la vista previa del editor enseñe las suyas—, así que la regla dejó de ser «aquí no hay
        // imágenes» y pasó a ser «aquí no hay nada que sepa salir a la red». Las imágenes del chat las prohibit
        // el invariante de abajo, que mira **quién pasa el cargador**, no qué sabe el motor.
        val fuente = leer(MODULO_RENDER)
        assertTrue("no se está leyendo el módulo de render", fuente.length > MINIMO_DE_LINEAS)

        listOf("Coil", "coil3", "okhttp", "HttpURLConnection", "java.net.URL", "Glide").forEach {
            assertFalse(
                "el módulo del motor no debe nombrar $it: una imagen podría salir a la red (RNF-50, RNF-57)",
                fuente.contains(it)
            )
        }
    }

    @Test
    fun `ninguna superficie de la app pasa un cargador de imagenes`() {
        // RNF-50. Esta es **la** regla: el motor puede con imágenes, pero las cuatro superficies de `:app` pasan
        // `null`, que es lo que impide que un mensaje del chat saque una imagen remota. Es un recorrido por
        // **todos** los ficheros de `src/main`, no solo los cuatro que ya sabemos: un cargador pasado en otro
        // sitio no se vería en un recorrido parcial.
        val arbol = File("src/main").walkTopDown().filter { it.isFile && it.extension == "kt" }
            .associate { it.path to it.readText() }
        assertTrue("el recorrido está roto: no ha leído ningún Kotlin", arbol.size >= 30)

        val ofensores = arbol.filter { (_, texto) ->
            texto.contains("imagenesLocalesDe") || texto.contains("LocalFileImageLoader")
        }.keys
        assertTrue(
            "ninguna superficie de la app debe pasar un cargador de imágenes: el chat y el texto legal no tienen " +
                "imágenes y abrirlas sería una vía de salida (RNF-50). Está en: $ofensores",
            ofensores.isEmpty()
        )
    }

    // ── RNF-49: el fichero puro sigue siendo puro ───────────────────────────────────────────

    @Test
    fun `la logica comprobable del render no importa nada de composicion`() {
        val fuente = leer(FUENTE_PURA)
        assertTrue("no se está leyendo el fichero puro", fuente.length > MINIMO_DE_LINEAS)

        val importa = fuente.lines().filter {
            it.startsWith("import androidx.compose") || it.startsWith("import android.")
        }
        assertTrue(
            "ChatMarkdownSource.kt no puede importar nada de composición o de la plataforma: sin eso sus " +
                "pruebas no corren en la plataforma de desarrollo. Importa: $importa",
            importa.isEmpty()
        )
    }

    // ── RNF-52: lo que ya funciona sigue funcionando ────────────────────────────────────────

    @Test
    fun `la pantalla del chat sigue teniendo sus gestos`() {
        val fuente = leer(PANTALLA_CHAT)
        assertTrue("no se está leyendo la pantalla del chat", fuente.length > MINIMO_DE_LINEAS)

        listOf("combinedClickable", "detectHorizontalDragGestures", "SWIPE_THRESHOLD").forEach { gesto ->
            assertTrue("el gesto $gesto no debe desaparecer de la burbuja", fuente.contains(gesto))
        }
    }

    @Test
    fun `el streaming sigue con el analizador y no con el motor`() {
        // RF-103. Es una decisión que se puede deshacer por accidente sin que nada se rompa: basta con
        // cambiar un `Text` por un `ChatMarkdownText` y la app sigue compilando y pareciendo correcta.
        val fuente = leer(PANTALLA_CHAT)
        assertTrue("no se está leyendo la pantalla del chat", fuente.length > MINIMO_DE_LINEAS)

        val inicio = fuente.indexOf("fun StreamingBubble")
        assertTrue("no se encuentra la burbuja de la réplica en curso", inicio > 0)

        val cuerpo = fuente.substring(inicio, (inicio + 2500).coerceAtMost(fuente.length))
        assertTrue("la réplica en curso debe seguir con el analizador", cuerpo.contains("RichTextParser.parse"))
        assertFalse("la réplica en curso no debe usar el motor de GFM (RF-103)", cuerpo.contains("ChatMarkdownText"))
    }
}
