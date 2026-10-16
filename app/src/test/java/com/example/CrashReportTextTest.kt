package com.example

import com.example.crash.CrashIssueUrl
import com.example.crash.CrashReportText
import com.example.crash.CrashStore
import com.example.crash.DatosInforme
import com.example.crash.TrazaRecortada

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas puras del texto del informe (spec 020, RF-118 y caso límite 5).
 *
 * **No llevan `RobolectricTestRunner` a propósito**: en la plataforma de desarrollo no hay runtime nativo de
 * Robolectric para `linux/aarch64`, así que una prueba con ese runner no se ejecuta nunca. Aquí se prueba el
 * texto, que es lo único que hay que decidir de verdad (RNF-64).
 */
class CrashReportTextTest {

    private val datos = DatosInforme(
        versionApp = "3.1.0",
        versionAndroid = "16",
        dispositivo = "Pixel 9 Pro",
        tipoBuild = "RELEASE",
        hilo = "main",
        marcaTiempo = 1_700_000_000_000L,
        traza = "java.lang.IllegalStateException: boom\n\tat com.example.Foo.bar(Foo.kt:42)"
    )

    @Test
    fun `el informe dice la version la de android el dispositivo el build el hilo y la fecha`() {
        val texto = CrashReportText.construir(datos)

        assertTrue(texto, texto.contains("3.1.0"))
        assertTrue(texto, texto.contains("16"))
        assertTrue(texto, texto.contains("Pixel 9 Pro"))
        assertTrue(texto, texto.contains("RELEASE"))
        assertTrue(texto, texto.contains("main"))
        // 1_700_000_000_000 es 2023-11-14T22:13:20Z: la fecha se pone, no se deja un número crudo.
        assertFalse(texto, texto.contains("1700000000000"))
    }

    @Test
    fun `el informe lleva la traza entera`() {
        val texto = CrashReportText.construir(datos)

        assertTrue(texto, texto.contains("IllegalStateException: boom"))
        assertTrue(texto, texto.contains("com.example.Foo.bar(Foo.kt:42)"))
    }

    @Test
    fun `una traza corta no dice que se recorta`() {
        val texto = CrashReportText.construir(datos)

        assertFalse(texto.lowercase(), CrashReportText.MARCA_DE_RECORTE.lowercase() in texto.lowercase())
    }

    @Test
    fun `una traza larguisima se recorta a 200 lineas`() {
        val larga = (1..500).joinToString("\n") { "marco $it" }
        val texto = CrashReportText.construir(datos.copy(traza = larga))

        assertTrue(texto, texto.contains("marco 1"))
        assertFalse(texto, texto.contains("marco 201"))
    }

    @Test
    fun `una traza larguisima DICE que se recorta y cuantas lineas habia`() {
        val larga = (1..500).joinToString("\n") { "marco $it" }
        val texto = CrashReportText.construir(datos.copy(traza = larga))

        // La marca dice cuántos había, para que el recorte no se confunda con un fallo más corto.
        assertEquals(
            "[traza recortada: 200 of 500 lines]",
            texto.lines().first { it.contains(CrashReportText.MARCA_DE_RECORTE) }.trim()
        )
    }

    @Test
    fun `recortar deja la traza corta intacta y sin marca`() {
        val corta = CrashReportText.recortarTraza("uno\ndos\ntres")

        assertEquals("uno\ndos\ntres", corta.texto)
        assertFalse(corta.recortada)
    }

    @Test
    fun `recortar al maximo deja exactamente las primeras lineas`() {
        val larga = (1..500).joinToString("\n") { "marco $it" }
        val corta = CrashReportText.recortarTraza(larga, maximo = 10)

        assertTrue(corta.recortada)
        assertEquals(10, corta.texto.lines().size)
        assertEquals("marco 1", corta.texto.lines().first())
        assertEquals("marco 10", corta.texto.lines().last())
    }

    @Test
    fun `una traza con lineas en blanco al final no cuenta como mas larga de lo que es`() {
        val conCola = CrashReportText.recortarTraza("uno\ndos\n\n\n")

        assertFalse(conCola.recortada)
        assertEquals("uno\ndos", conCola.texto)
    }
}