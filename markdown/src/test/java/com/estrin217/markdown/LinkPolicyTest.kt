package com.estrin217.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Qué direcciones se pueden abrir al pulsarlas (spec 017, RF-100; spec 018, RF-106).
 *
 * Son **pruebas puras** y no llevan `RobolectricTestRunner`, que en esta plataforma no tiene runtime nativo. Por
 * eso `LinkPolicy.kt` no importa nada de `android` ni de `androidx.compose` ni el reloj.
 *
 * Las seis primeras vienen de la spec 017 y se trasladaron aquí tal cual cuando la función se movió al módulo del
 * motor: las usan las **cinco** superficies de Markdown, no solo el chat.
 */
class LinkPolicyTest {

    // ── Qué se abre ───────────────────────────────────────────────────────────────────────

    @Test
    fun `http y https si se abren`() {
        assertTrue(isWebLink("http://ejemplo.com"))
        assertTrue(isWebLink("https://ejemplo.com/a/b?c=d"))
    }

    @Test
    fun `el esquema en mayusculas tambien cuenta`() {
        assertTrue(isWebLink("HTTPS://ejemplo.com"))
        assertTrue(isWebLink("Https://ejemplo.com"))
    }

    @Test
    fun `un dominio sin esquema cuenta si parece un dominio`() {
        assertTrue(isWebLink("www.ejemplo.com"))
        assertTrue(isWebLink("ejemplo.com/a"))
        assertTrue(isWebLink("sub.dominio.ejemplo.co.uk"))
    }

    @Test
    fun `los esquemas que no son web no se abren`() {
        listOf(
            "javascript:alert(1)",
            "intent://scan/#Intent;scheme=zxing;end",
            "file:///etc/passwd",
            "content://media/external/images/1",
            "tel:+34600123456",
            "mailto:correo@ejemplo.com",
            "ftp://ejemplo.com",
            "data:text/html,<b>x</b>"
        ).forEach { url ->
            assertFalse("no debe abrirse $url", isWebLink(url))
        }
    }

    @Test
    fun `nada de texto no es una direccion`() {
        listOf("", "   ", "no tengo punto", "///", "...").forEach { url ->
            assertFalse("no debe abrirse '$url'", isWebLink(url))
        }
    }

    @Test
    fun `los espacios al rededor no estropean la decision`() {
        assertTrue(isWebLink("  https://ejemplo.com  "))
        assertFalse(isWebLink("  javascript:alert(1)  "))
    }

    // ── La dirección que se abre, sin la puntuación de su frase (RF-106) ──────────────────
    // Las pruebas a fondo de `direccionParaAbrir` viven en `DireccionParaAbrirTest`; aquí solo las dos que
    // atan esa función con `isWebLink`, que es la propiedad que de verdad importa para un enlace.




    @Test
    fun `recortar solo recorta`() {
        // La propiedad de seguridad: si solo puede quitar caracteres, no puede convertir una dirección inocua en
        // otra que no lo sea.
        val entradas = listOf("javascript:alert(1);", "file:///etc/passwd", "mailto:a@b.com", "data:text/html,x")
        entradas.forEach { url ->
            assertTrue(
                "recortar '$url' no puede producir algo más largo",
                direccionParaAbrir(url).length <= url.length
            )
            assertFalse(
                "recortar no puede volver web lo que no lo era: '$url'",
                !isWebLink(url) && isWebLink(direccionParaAbrir(url))
            )
        }
    }


    @Test
    fun `abrir una direccion recortada da el mismo esquema que abrir la cruda`() {
        // Laproperty que de verdad importa para el enlace del texto legal: si el detector metió el paréntesis
        // dentro del span, lo que se abre tiene que ser el mismo sitio.
        val cruda = "https://github.com/ESTRIN217)"
        assertEquals(isWebLink(cruda), isWebLink(direccionParaAbrir(cruda)))
    }
}