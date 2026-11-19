package com.example.ui.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Invariante de fuentes del toque individual de una herramienta (spec 043, enmienda RF-313, RNF-204).
 *
 * `ToolsRow` llegó a tener dos manejadores de pulsación sobre la misma fila: un `.clickable` en el
 * `Row` y el `onCheckedChange` del `Checkbox`. Un toque en la casilla podía disparar los dos y dejar la
 * herramienta en el estado en que ya estaba. El doble handler no cambia ninguna salida observable de
 * las piezas puras —`toggledDisabledTools` sigue siendo «desactivar añade / activar quita»—, así que una
 * prueba de comportamiento sobre la regla no lo notaría: solo se ve en el texto de la fila. Por eso se
 * lee la fuente, como en `BackupSettingsInvariantsTest` y `EditorInvariantsTest`.
 *
 * JUnit4 puro, sin Robolectric: aquí no se infla Compose, solo se lee un `.kt`.
 */
class ToolsSheetInvariantsTest {

    private companion object {
        const val RUTA = "src/main/java/com/example/ui/chat/ToolsSheet.kt"
        const val FUNCION = "fun ToolsRow("
        const val TOGGLE = "onToggle\\(name\\)"
        const val CLICKABLE = "\\.clickable"
    }

    /** Autocomprobación del recorrido: un KDoc que nombre `.clickable` no es código. */
    @Test
    fun `el recorrido quita comentarios y textos`() {
        val limpio = sinComentariosNiTextos(
            """
            /** ojo: .clickable y onToggle(name) en el KDoc no son código */
            val aviso = ".clickable onToggle(name) en un texto"
            Checkbox(onCheckedChange = { onToggle(name) })
            """.trimIndent()
        )
        assertTrue("El comentario de bloque tiene que irse", !limpio.contains("en el KDoc"))
        assertTrue("El texto tiene que irse", !limpio.contains("en un texto"))
        assertTrue(
            "El código de verdad se queda, y es lo que el recorrido tiene que contar",
            limpio.contains("onToggle(name)")
        )
    }

    /** RF-313: un toque produce exactamente un toggle. */
    @Test
    fun `ToolsRow tiene exactamente un onToggle`() {
        val cuerpo = cuerpoDeToolsRow(codigo())
        assertEquals(
            "ToolsRow debe tener un único onToggle(name) — el del Checkbox — y no el de un Row.clickable",
            1,
            ocurrencias(cuerpo, TOGGLE)
        )
    }

    /** RNF-204: ningún segundo manejador de pulsación en la fila. */
    @Test
    fun `ToolsRow no tiene clickable`() {
        val cuerpo = cuerpoDeToolsRow(codigo())
        assertTrue(
            "El recorrido tiene que estar leyendo el cuerpo de ToolsRow, no un trozo vacío",
            cuerpo.contains("Checkbox(")
        )
        assertEquals(
            "ToolsRow no puede tener .clickable: un toque en la casilla dispararía dos toggles",
            0,
            ocurrencias(cuerpo, CLICKABLE)
        )
    }

    /** El código del fichero, sin comentarios ni textos, comprobando que existe. */
    private fun codigo(): String {
        val fichero = File(RUTA)
        assertTrue("Falta $RUTA: el invariante no está leyendo la fuente", fichero.exists())
        return sinComentariosNiTextos(fichero.readText())
    }

    /** El cuerpo de `ToolsRow`, balanceando llaves para no contar otras filas ni la cabecera. */
    private fun cuerpoDeToolsRow(codigo: String): String {
        val inicio = codigo.indexOf(FUNCION)
        assertTrue("No se encontró ToolsRow en $RUTA", inicio >= 0)
        val apertura = codigo.indexOf('{', inicio)
        assertTrue("No se encontró el cuerpo de ToolsRow", apertura >= 0)
        var nivel = 0
        var i = apertura
        while (i < codigo.length) {
            when (codigo[i]) {
                '{' -> nivel++
                '}' -> if (--nivel == 0) return codigo.substring(apertura, i + 1)
            }
            i++
        }
        throw AssertionError("El cuerpo de ToolsRow no cierra: el recorrido está roto")
    }

    private fun ocurrencias(codigo: String, patron: String): Int =
        Regex(patron).findAll(codigo).count()

    /** Sin comentarios de bloque, sin comentarios de línea y sin textos. */
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
