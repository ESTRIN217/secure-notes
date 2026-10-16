package com.example

import com.example.crash.CrashIssueUrl
import com.example.crash.CrashReportText
import com.example.crash.CrashStore
import com.example.crash.DatosInforme
import com.example.crash.TrazaRecortada

import java.net.URLDecoder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas puras de la URL de la incidencia de GitHub (spec 020, RF-121 y caso límite 6).
 *
 * La prueba que de verdad importa es la del **viaje redondo**: el cuerpo se codifica por parámetro, así que si
 * un salto de línea, un acento o un `&` no vuelven **exactamente iguales** al descomprimir, la incidencia se
 * abre con el cuerpo cortado o vacío.
 */
class CrashIssueUrlTest {

    @Test
    fun `la url es una incidencia nueva del repositorio`() {
        val url = CrashIssueUrl.url("titulo", "cuerpo")

        assertEquals("https://github.com/ESTRIN217/secure-notes/issues/new?title=titulo&body=cuerpo", url)
    }

    @Test
    fun `el titulo lleva el tipo de excepcion y no el mensaje`() {
        val titulo = CrashIssueUrl.titulo("java.lang.IllegalStateException: el texto de mi nota")

        assertTrue(titulo, titulo.contains("IllegalStateException"))
        // El mensaje puede llevar contenido del usuario (RNF-67): al título no se lleva.
        assertFalse(titulo, titulo.contains("el texto de mi nota"))
    }

    @Test
    fun `el titulo sin tipo de excepcion sigue siendo un titulo`() {
        assertEquals("[Crash] Secure Notes", CrashIssueUrl.titulo(""))
    }

    @Test
    fun `un cuerpo con saltos de linea acentos ampersand almohadilla y porcentaje vuelve IGUAL`() {
        val cuerpo = "primera línea\nsegunda & tercera\n# título\n100% de nada\n+ más"

        val url = CrashIssueUrl.url("t", cuerpo)

        assertEquals(cuerpo, parametroDe(url, "body"))
    }

    @Test
    fun `un cuerpo enorme no se parte en dos parametros`() {
        val cuerpo = "línea\n".repeat(500) + "&final"

        val url = CrashIssueUrl.url("t", cuerpo)

        assertEquals(cuerpo, parametroDe(url, "body"))
        assertEquals(1, Regex("body=").findAll(url).count())
    }

    @Test
    fun `un titulo con espacios sale con %20 y no con mas`() {
        val url = CrashIssueUrl.url("[Crash] IllegalStateException de la app", "c")

        assertEquals("[Crash] IllegalStateException de la app", parametroDe(url, "title"))
        assertTrue(url, "%20" in url)
    }

    @Test
    fun `el cuerpo lleva el informe y la nota de que hay que revisarlo`() {
        val cuerpo = CrashIssueUrl.cuerpo("TRAZA DEL INFORME")

        assertTrue(cuerpo, cuerpo.contains("TRAZA DEL INFORME"))
    }
}

/** Lee un parámetro de la query y lo devuelve **descodificado**: el viaje redondo de RF-121. */
private fun parametroDe(url: String, nombre: String): String {
    val crudo = url.substringAfter("$nombre=").substringBefore("&")
    return URLDecoder.decode(crudo, "UTF-8")
}