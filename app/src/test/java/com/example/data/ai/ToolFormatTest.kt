package com.example.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

/**
 * El formato de las dos respuestas nuevas (spec 043, RF-310, RF-311, RNF-197, RNF-198):
 * [getDatetimeResult] y [getInfoResult] son puros — reciben el instante, la zona, la versión y la
 * nota como parámetros — y se prueban sin reloj del sistema ni Robolectric.
 *
 * El wire format reproduce a llama.cpp: `{result, timezone}` con ISO local y zona IANA, y
 * `{note, os}` sin cwd ni identificadores.
 */
class ToolFormatTest {

    private val instant = Instant.parse("2026-08-17T12:05:09Z")

    @Test
    fun `get_datetime con desplazamiento positivo y zona IANA`() {
        assertEquals(
            """{"result":"2026-08-17T14:05:09+02:00","timezone":"Europe/Madrid"}""",
            getDatetimeResult(instant, ZoneId.of("Europe/Madrid"))
        )
    }

    @Test
    fun `get_datetime con desplazamiento negativo`() {
        assertEquals(
            """{"result":"2026-08-17T08:05:09-04:00","timezone":"America/New_York"}""",
            getDatetimeResult(instant, ZoneId.of("America/New_York"))
        )
    }

    @Test
    fun `get_datetime en UTC lleva +00-00 y no Z`() {
        val resultado = getDatetimeResult(instant, ZoneId.of("UTC"))
        assertEquals(
            """{"result":"2026-08-17T12:05:09+00:00","timezone":"UTC"}""",
            resultado
        )
        assertFalse(resultado.contains('Z'))
    }

    @Test
    fun `get_datetime respeta medianoche y cambio de ano`() {
        assertEquals(
            """{"result":"2026-01-01T00:30:00+01:00","timezone":"Europe/Madrid"}""",
            getDatetimeResult(Instant.parse("2025-12-31T23:30:00Z"), ZoneId.of("Europe/Madrid"))
        )
    }

    @Test
    fun `get_datetime usa el desplazamiento vigente en la fecha`() {
        val invierno = getDatetimeResult(
            Instant.parse("2026-01-17T12:05:09Z"), ZoneId.of("Europe/Madrid")
        )
        val verano = getDatetimeResult(instant, ZoneId.of("Europe/Madrid"))
        assertEquals("""{"result":"2026-01-17T13:05:09+01:00","timezone":"Europe/Madrid"}""", invierno)
        assertEquals("""{"result":"2026-08-17T14:05:09+02:00","timezone":"Europe/Madrid"}""", verano)
    }

    @Test
    fun `get_info lleva solo note y os`() {
        val nota = "This app only exposes note tools. There is no shell or local file system access."
        val resultado = getInfoResult("14", 34, nota)
        assertEquals(
            """{"note":"$nota","os":"Android 14 (API 34)"}""",
            resultado
        )
        assertFalse(resultado.contains("cwd"))
        assertFalse(resultado.contains("model"))
        assertFalse(resultado.contains("imei"))
    }

    @Test
    fun `get_info escapa comillas y contrabarras de la nota`() {
        val resultado = getInfoResult("14", 34, """Dice "hola" \ adiós""")
        assertEquals(
            """{"note":"Dice \"hola\" \\ adiós","os":"Android 14 (API 34)"}""",
            resultado
        )
    }
}
