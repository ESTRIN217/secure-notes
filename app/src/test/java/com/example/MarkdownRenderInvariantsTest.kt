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
        const val MODULO_RENDER = "src/main/java/com/example/ui/markdown/ChatMarkwon.kt"
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

    // ── RNF-48: cero jars nuevos ───────────────────────────────────────────────────────────

    @Test
    fun `commonmark no se mueve`() {
        val toml = leer(TOML)
        assertTrue("no se está leyendo el fichero del catálogo", toml.contains("commonmark"))

        val asignacion = Regex("""^commonmark\s*=\s*"([^"]+)"""", RegexOption.MULTILINE).find(toml)
        assertTrue("no se encuentra la asignación de commonmark", asignacion != null)
        assertTrue(
            "commonmark debería seguir en $COMMONMARK_ESPERADA y está en ${asignacion!!.groupValues[1]}",
            asignacion.groupValues[1] == COMMONMARK_ESPERADA
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
    fun `compose-markdown se queda porque otras dos pantallas lo usan`() {
        // Las otras dos pantallas (`ChangelogSheet`, `UpdateInfoScreen`) siguen con `MarkdownText`. Si alguien
        // quita la dependencia esperando que ya no hace falta, la app deja de compilar —que es la forma
        // barata de enterarse—, pero esta prueba lo dice antes.
        val toml = leer(TOML)
        assertTrue("compose-markdown no debe desaparecer del catálogo", toml.contains("compose-markdown"))
    }

    // ── RNF-50: pintar no hace red ──────────────────────────────────────────────────────────

    @Test
    fun `el modulo de render no menciona ninguna clase de imagenes`() {
        val fuente = leer(MODULO_RENDER)
        assertTrue("no se está leyendo el módulo de render", fuente.length > MINIMO_DE_LINEAS)

        listOf("image.AsyncDrawableLoader", "asyncDrawableLoader", "CoilImagesPlugin", "imageLoader").forEach {
            assertFalse(
                "el render no debe nombrar $it: una imagen de un mensaje podría salir a la red (RNF-50)",
                fuente.contains(it)
            )
        }
    }

    @Test
    fun `el motor no arrastra un cargador de imagenes`() {
        val fuente = leer(MODULO_RENDER)
        val plugins = fuente.lines().filter { it.contains(".usePlugin(") }

        assertTrue("no se encuentra la lista de plugins: el recorrido está roto", plugins.size >= 6)
        assertFalse(
            "no debe haber ningún plugin que cargue imágenes (RNF-50)",
            plugins.any { it.contains("Image") }
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
