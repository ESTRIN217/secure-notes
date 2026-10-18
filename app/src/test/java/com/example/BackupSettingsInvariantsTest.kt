package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Invariantes de los ajustes que viajan en la copia de seguridad (spec 028, RF-191, RNF-120).
 *
 * RF-191 **no** se puede comprobar con pruebas de comportamiento, y conviene decir por qué: las trece
 * preferencias que ya se leían siguen leyéndose igual después del refactor, así que volver a escribir la
 * lista a mano en un sexto sitio **no cambia ninguna salida**. Lo que se vigila aquí es que eso no llegue a
 * pasar, y eso solo se ve en el texto.
 *
 * Por eso los recorridos apuntan a **patrones que no se pueden copiar sin que salte**:
 *
 * - Los que **crean** la copia no pueden tener `put(AppConstants.…,` — los ajustes vivos se escriben con
 *   `putString(`, `putBoolean(` y con `setStringPref(`, así que ese patrón solo aparece al montar el bloque
 *   a mano. Y tienen que llamar a la pieza compartida.
 * - Los que **restauran** no pueden tener `settings.get…(AppConstants.` — se lee el bloque del que salen
 *   así, y ese patrón no aparece en ningún otro sitio del fichero.
 *
 * El alcance es **el bloque, no el fichero**: `NotesViewModel` usa esas mismas claves para su estado vivo
 * (`autoUpdateCheck`, `isBiometricEnabled`, `encryptBackups`…) y `SyncWorker` lee `ENCRYPT_BACKUPS_KEY` para
 * decidir si la copia va cifrada. Ponerlo rojo ahí sería un invariante que obliga a romper cosas.
 *
 * Cada recorrido quita primero comentarios y textos (un `//` de una URL o una clave en un KDoc harían que
 * el patrón encontrara algo donde no hay código), comprueba que el fichero existe para que el inventario no
 * se quede viejo en silencio, y lleva **suelos**: si un recorrido se rompe devuelve 0 y la comprobación
 * «no hay ninguna copia de la lista» pasa por vacío, que es como este tipo de prueba se da por buena sin
 * comprobar nada.
 */
class BackupSettingsInvariantsTest {

    private companion object {
        const val RAIZ = "src/main/java/com/example"
        const val PIEZA = "$RAIZ/data/backup/BackupSettings.kt"

        val COPIAS = listOf(
            "$RAIZ/ui/viewmodel/BackupViewModel.kt",
            "$RAIZ/ui/viewmodel/NotesViewModel.kt",
            "$RAIZ/data/sync/SyncWorker.kt"
        )

        val RESTAURADORES = listOf(
            "$RAIZ/ui/viewmodel/BackupViewModel.kt",
            "$RAIZ/ui/viewmodel/NotesViewModel.kt"
        )

        /** Al menos una llamada por sitio que crea la copia: si el recorrido no lee, salta por el suelo. */
        const val MIN_COPIAS = 3

        /** Lo mismo con los dos que restauran. */
        const val MIN_RESTAURADORES = 2

        /** Las dos listas tienen que aparecer **por lo menos** una vez antes de exigir que sea una. */
        const val MIN_DECLARACIONES = 1

        /** Las dos funciones puras tienen que estar en la pieza. */
        const val MIN_FUNCIONES_PURAS = 2

        const val LISTA = "put\\(AppConstants\\."
        const val LEE_BLOQUE = "settings.get(String|Boolean|Long)\\((AppConstants|settings)\\."
        const val COPIA = "backupSettingsJson\\("
        const val APLICA = "applyRestoredSettings\\("

        /** Cuántas declaraciones de lista se esperan, y en cuántas: las dos listas, en la misma pieza. */
        const val DECLARACIONES = 2
    }

    // ── El recorrido y su autocomprobación ─────────────────────────────────────────────────

    /**
     * El recorrido, probado con un trozo de código inventado.
     *
     * Sin esto, un `Regex` mal escrito devuelve 0 coincidencias, la comprobación «no hay ni una copia de la
     * lista» pasa en verde y la prueba no está mirando nada.
     */
    @Test
    fun `el recorrido quita comentarios y textos`() {
        val limpio = sinComentariosNiTextos(
            """
            val a = 1 // esto no
            /* esto
               tampoco */
            val b = "esto tampoco // ni esto"
            val c = put(AppConstants.LANGUAGE_KEY, "es")
            """.trimIndent()
        )
        assertTrue("El comentario de línea tiene que irse", !limpio.contains("esto no"))
        assertTrue("El comentario de bloque tiene que irse", !limpio.contains("tampoco"))
        assertTrue("El texto tiene que irse", !limpio.contains("ni esto"))
        assertTrue(
            "El código de verdad se queda, y es lo que el recorrido tiene que encontrar",
            limpio.contains("put(AppConstants.LANGUAGE_KEY, ")
        )
    }

    // ── RF-191: quien crea la copia no escribe la lista ─────────────────────────────────────

    @Test
    fun `quien crea la copia no escribe la lista a mano`() {
        val copias = COPIAS.map { ruta ->
            val codigo = codigo(ruta)
            assertEquals(
                "$ruta monta el bloque de ajustes a mano: tiene que pasar por la pieza compartida",
                0,
                ocurrencias(codigo, LISTA)
            )
            ruta to ocurrencias(codigo, COPIA)
        }
        val total = copias.sumOf { it.second }
        assertTrue(
            "Cada uno de los ${COPIAS.size} sitios que crean la copia tiene que llamar a la pieza " +
                "compartida, y solo hay $total llamadas (mínimo $MIN_COPIAS)",
            total >= MIN_COPIAS
        )
    }

    // ── RF-191: quien restaura tampoco ─────────────────────────────────────────────────────

    @Test
    fun `quien restaura no escribe la lista a mano`() {
        val restauradores = RESTAURADORES.map { ruta ->
            val codigo = codigo(ruta)
            assertEquals(
                "$ruta aplica los ajustes uno a uno: tiene que pasar por la pieza compartida",
                0,
                ocurrencias(codigo, LEE_BLOQUE)
            )
            ruta to ocurrencias(codigo, APLICA)
        }
        val total = restauradores.sumOf { it.second }
        assertTrue(
            "Cada uno de los ${RESTAURADORES.size} sitios que restauran tiene que llamar a la pieza " +
                "compartida, y solo hay $total llamadas (mínimo $MIN_RESTAURADORES)",
            total >= MIN_RESTAURADORES
        )
    }

    // ── RF-191: la lista vive en un solo fichero ───────────────────────────────────────────

    /**
     * Las dos listas, declaradas **una vez** en `:app`.
     *
     * Aquí se busca `val BACKUP_SETTINGS` y `val DEFAULT_SETTINGS` en todo el árbol, y no «el fichero que
     * menciona el tema junto al interruptor antiguo»: ese invariante era del plan y estaba **mal**, porque
     * `SharedPreferencesRepository.getAppTheme()` menciona los dos legítimamente —es el otro lector de la
     * migración— y `Constants.kt` los declara. Lo que tiene que ser único es la **declaración**.
     */
    @Test
    fun `las listas se declaran en un solo fichero`() {
        val declaraciones = kotlinDe(RAIZ)
            .map { (ruta, codigo) -> ruta to ocurrencias(codigo, "val (BACKUP_SETTINGS|DEFAULT_SETTINGS)") }
            .filter { (_, cuantas) -> cuantas > 0 }
        val total = declaraciones.sumOf { (_, cuantas) -> cuantas }
        assertTrue(
            "El recorrido no ha encontrado ninguna declaración: no está leyendo nada",
            total >= MIN_DECLARACIONES
        )
        assertEquals(
            "Las listas del respaldo solo se declaran en un sitio, y solo son dos: " +
                declaraciones.joinToString { "${it.first} (${it.second})" },
            listOf(PIEZA to DECLARACIONES),
            declaraciones
        )
    }

    // ── RNF-120: la pieza es pura ───────────────────────────────────────────────────────────

    @Test
    fun `la pieza no depende de compose y contiene las dos funciones puras`() {
        val codigo = codigo(PIEZA)
        assertTrue(
            "La pieza de los ajustes no puede importar Compose: su decisión tiene que probarse sin " +
                "Robolectric",
            !codigo.contains("androidx.compose")
        )
        val puras = listOf("fun backupSettingsJson(", "fun restorableSettings(")
            .count { codigo.contains(it) }
        assertTrue(
            "Las dos funciones puras tienen que estar en la pieza y solo hay $puras",
            puras >= MIN_FUNCIONES_PURAS
        )
    }

    // ── Decisión 2 del plan: el tema se resuelve con la regla de la spec 025 ───────────────

    @Test
    fun `la pieza resuelve el tema con la regla de la spec 025`() {
        assertTrue(
            "El tema que viaja tiene que salir de resolveStoredTheme y no de una caída propia",
            codigo(PIEZA).contains("resolveStoredTheme(")
        )
    }

    // ── El recorrido ───────────────────────────────────────────────────────────────────────

    /**
     * El código del fichero, sin comentarios ni textos, y comprobando que el fichero existe: un inventario
     * que se queda viejo en silencio es un invariante que deja de mirar.
     */
    private fun codigo(ruta: String): String {
        val fichero = File(ruta)
        assertTrue("Falta $ruta: el inventario de la copia está desactualizado", fichero.exists())
        return sinComentariosNiTextos(fichero.readText())
    }

    /** Pares de ruta y código de los `.kt` de `:app`. */
    private fun kotlinDe(raiz: String): List<Pair<String, String>> =
        File(raiz).walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .map { it.path to sinComentariosNiTextos(it.readText()) }
            .toList()

    private fun ocurrencias(codigo: String, patron: String): Int =
        Regex(patron).findAll(codigo).count()

    /**
     * Sin comentarios de bloque, sin comentarios de línea y sin textos.
     *
     * Los de bloque primero con un `Regex`: un KDoc de tres líneas es justo lo que escondería un `put(` de
     * ejemplo o un `AppConstants.…` citado. Los de línea y los textos, línea a línea, porque un `//` puede
     * ser una URL dentro de un texto y no un comentario —y en `NotesViewModel` hay más de cuarenta líneas
     * con `//`, casi todas URLs.
     */
    private fun sinComentariosNiTextos(texto: String): String {
        val sinBloques = Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL).replace(texto, " ")
        return sinBloques.split("\n").joinToString("\n") { linea ->
            val limpia = StringBuilder()
            var enTexto = false
            var i = 0
            while (i < linea.length) {
                val c = linea[i]
                when {
                    enTexto && c == '\\' -> i++
                    enTexto && c == '"' -> enTexto = false
                    !enTexto && c == '"' -> enTexto = true
                    !enTexto && c == '/' && i + 1 < linea.length && linea[i + 1] == '/' -> break
                    !enTexto -> limpia.append(c)
                }
                i++
            }
            limpia.toString()
        }
    }
}