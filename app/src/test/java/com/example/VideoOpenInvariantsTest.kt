package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Invariantes de la spec 023: el bloque de vídeo abre **dentro** lo que el reproductor interno reproduce, y
 * las dos superficies deciden con la **misma** regla.
 *
 * Son pruebas **puras** que leen el texto de los ficheros, con las dos reglas del proyecto (ver `MEMORY.md`):
 * cada recorrido **quita comentarios antes de buscar** y **se autocomprueba**, para que un KDoc que explique
 * por qué algo **no** está no dé verde, ni un recorrido vacío pase por vacío.
 *
 * La otra mitad de la spec —que el vídeo se vea dentro— **no** se puede comprobar aquí: va a dispositivo (T7).
 */
class VideoOpenInvariantsTest {

    private companion object {
        const val JAVA = "src/main/java/com/example"
        const val RES = "src/main/res"

        const val BLOQUE_EDITABLE = "$JAVA/ui/EditableVideoBlock.kt"
        const val BLOQUE_SOLO_LECTURA = "$JAVA/ui/NoteContentBlockRenderer.kt"
        const val APERTURA_EXTERNA = "$JAVA/ui/VideoBlockOpen.kt"
        const val BARRA_DE_PESTANAS = "$JAVA/ui/NoteEditorTabsHost.kt"
        const val CLASIFICACION = "../visor-media/src/main/java/com/estrin217/visormedia/util/VideoUrlHelper.kt"

        val GRADLE = listOf(
            "build.gradle.kts",
            "../visor-media/build.gradle.kts",
            "../gradle/libs.versions.toml"
        )

        /**
         * Las claves que muestra el bloque editable **hoy**, literal (RNF-85). Es el texto que se muestra, así
         * que crece cuando la spec cambie lo que el bloque enseña, no cuando cambie el código que lo pinta.
         */
        val CLAVES_DEL_BLOQUE_EDITABLE = listOf(
            "block_align_center", "block_align_left", "block_align_right", "block_convert_title",
            "block_duplicate", "block_insert_below", "block_move_to", "block_video", "block_video_align",
            "block_video_caption_hint", "block_video_description", "block_video_load_error",
            "block_video_open", "block_video_replace", "btn_delete", "cd_block_more", "cd_play_video"
        )

        /** La lista cerrada de RF-141, tal cual. */
        val EXTENSIONES = listOf("mp4", "m4v", "mov", "webm", "mkv", "m3u8", "ts", "flv", "ogv", "ogg")

        /** La librería de terceros que la spec **descartó** (RNF-82). */
        val LIBRERIA_DESCARTADA = listOf("pierfrancesco", "androidyoutubeplayer", "android-youtube-player")

        /** Piso de cualquier recorrido: si baja de aquí, el recorrido está roto, no el código. */
        const val MINIMO_DE_LINEAS = 5

        private val COMENTARIOS_KOTLIN = Regex("/\\*.*?\\*/|//[^\\n]*", RegexOption.DOT_MATCHES_ALL)
    }

    private fun leer(ruta: String, etiqueta: String): String {
        val fichero = File(ruta)
        assertTrue("no existe $etiqueta ($ruta)", fichero.exists())
        val texto = fichero.readText()
        assertTrue("$etiqueta ($ruta) solo tiene ${texto.lines().size} líneas", texto.lines().size >= MINIMO_DE_LINEAS)
        return texto
    }

    /** Lee **quitando comentarios**, para que un KDoc que explica por qué algo **no** está no dé verde. */
    private fun leerCodigo(ruta: String, etiqueta: String): String {
        val codigo = leer(ruta, etiqueta).replace(COMENTARIOS_KOTLIN, "")
        assertTrue("el recorrido de $etiqueta ($ruta) se ha quedado sin código", codigo.isNotBlank())
        return codigo
    }

    // ── RNF-83 y RF-149: una sola regla, dos superficies ─────────────────────────────────────

    @Test
    fun `las dos superficies del bloque deciden con opensInApp`() {
        assertTrue(
            "el bloque editable no usa la regla compartida",
            leerCodigo(BLOQUE_EDITABLE, "el bloque editable").contains("VideoUrlHelper.opensInApp")
        )
        assertTrue(
            "el render de solo lectura no usa la regla compartida",
            leerCodigo(BLOQUE_SOLO_LECTURA, "el render de solo lectura").contains("VideoUrlHelper.opensInApp")
        )
    }

    // ── RNF-84: abrir fuera solo por la puerta única ─────────────────────────────────────────

    @Test
    fun `ninguna de las dos superficies lanza ACTION_VIEW`() {
        for (ruta in listOf(BLOQUE_EDITABLE, BLOQUE_SOLO_LECTURA)) {
            assertFalse(
                "$ruta abre fuera por su cuenta: debe llamar a abrirVideoFuera",
                leerCodigo(ruta, ruta).contains("ACTION_VIEW")
            )
        }
    }

    @Test
    fun `la puerta unica existe y es la que lanza ACTION_VIEW`() {
        // Autocomprobación: si el recorrido no encontrara el `ACTION_VIEW` en ningún sitio, el invariante
        // anterior pasaría por vacío.
        assertTrue(
            "abrirVideoFuera no lanza ACTION_VIEW: el invariante anterior no está midiendo nada",
            leerCodigo(APERTURA_EXTERNA, "la puerta única").contains("Intent.ACTION_VIEW")
        )
    }

    // ── RNF-83 y RF-141: la clasificación es pura y con la lista cerrada ─────────────────────

    @Test
    fun `la clasificacion no depende de android`() {
        val codigo = leerCodigo(CLASIFICACION, "la clasificación")
        assertFalse("VideoUrlHelper importa android", codigo.contains("import android"))
        assertFalse("VideoUrlHelper importa androidx", codigo.contains("import androidx"))
    }

    @Test
    fun `las diez extensiones de la spec estan en la lista cerrada`() {
        val codigo = leerCodigo(CLASIFICACION, "la clasificación")
        for (extension in EXTENSIONES) {
            assertTrue(
                "falta la extensión .$extension en VideoUrlHelper (RF-141): la lista se ha movido sin tocar la spec",
                codigo.contains("\"$extension\"")
            )
        }
    }

    // ── RNF-85: cero textos nuevos ───────────────────────────────────────────────────────────

    /**
     * **No** se comprueba la paridad de claves entre los nueve locales: el repo no la cumple —`values-en-rGB`
     * declara 876 claves frente a las 1183 de `values`, y le faltan casi todas las del bloque de vídeo—. Eso es
     * un defecto **preexistente y ajeno** a esta spec, y un invariante que lo diera por bueno sería mentira;
     * uno que lo denunciara estaría midiendo otra cosa.
     *
     * Lo que sí se puede fijar es lo que pide RNF-85: que el bloque **no estrene** ninguna clave. Si alguien
     * añade un texto al bloque, el conjunto cambia, esta prueba se pone roja y obliga a decidir si el texto va
     * a los nueve locales.
     */
    @Test
    fun `el bloque no estrena ninguna clave de texto`() {
        assertEquals(
            "el bloque editable ha cambiado los textos que muestra: si es un texto nuevo, va a los nueve locales",
            CLAVES_DEL_BLOQUE_EDITABLE.sorted(),
            clavesUsadasEn(BLOQUE_EDITABLE).sorted()
        )
        assertEquals(
            "la apertura externa estrena texto: reutiliza toast_cannot_open_url",
            listOf("toast_cannot_open_url"),
            clavesUsadasEn(APERTURA_EXTERNA).sorted()
        )
    }

    @Test
    fun `el aviso de que no se puede abrir existe en el idioma de referencia`() {
        assertTrue(
            "toast_cannot_open_url ha desaparecido de values",
            leer("$RES/values/strings.xml", "values").contains("name=\"toast_cannot_open_url\"")
        )
    }

    private fun clavesUsadasEn(ruta: String): List<String> {
        val codigo = leerCodigo(ruta, ruta)
        val claves = Regex("R\\.string\\.([a-zA-Z_0-9]+)")
            .findAll(codigo)
            .map { it.groupValues[1] }
            .distinct()
            .toList()
        assertTrue("el recorrido de $ruta no ha encontrado ni una clave", claves.isNotEmpty())
        return claves
    }

    // ── RF-151: el rótulo de la pestaña lo pone la pieza compartida ───────────────────────────

    @Test
    fun `el rotulo de una pestana de video web lo pone la pieza compartida`() {
        // Autocomprobación: la regla tiene que existir donde se dice que está.
        assertTrue(
            "VideoUrlHelper no declara webVideoLabel: RF-151 estaría escrita sin nada detrás",
            leerCodigo(CLASIFICACION, "la clasificación").contains("fun webVideoLabel")
        )
        // `tabTitle` es `@Composable` y usa `stringResource`, así que necesita Robolectric —que aquí no se
        // ejecuta—: esta mitad se comprueba por invariante, y que el rótulo sea el correcto con las pruebas
        // puras de `:visor-media`.
        assertTrue(
            "la barra de pestañas no pregunta por el rótulo web: un enlace de YouTube se leería «watch?v=…»",
            leerCodigo(BARRA_DE_PESTANAS, "la barra de pestañas").contains("VideoUrlHelper.webVideoLabel")
        )
    }

    // ── RNF-82: la librería descartada no entra por la puerta de atrás ───────────────────────

    @Test
    fun `no se ha anadido la libreria de YouTube descartada`() {
        for (ruta in GRADLE) {
            val codigo = leerCodigo(ruta, ruta).lowercase()
            for (marca in LIBRERIA_DESCARTADA) {
                assertFalse(
                    "$ruta declara la librería descartada ($marca): el reproductor interno ya existe",
                    codigo.contains(marca)
                )
            }
        }
    }
}
