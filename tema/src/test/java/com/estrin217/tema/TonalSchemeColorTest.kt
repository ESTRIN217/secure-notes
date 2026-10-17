package com.estrin217.tema

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas de color del generador tonal de `:tema` (spec 025, RF-161, RNF-101).
 *
 * Son **puras**: ni `Context` ni Robolectric, así que corren en cualquier entorno. Lo que se comprueba aquí no es
 * que el tema quede bonito —eso va a dispositivo— sino que el generador es **fiable**: que da lo mismo siempre,
 * que ningún color se sale del gamut de sRGB y que un color sin croma no inventa croma por el camino.
 *
 * El contraste de los ocho esquemas del catálogo no se comprueba aquí: vive en `TonalSchemeContrastTest`, que
 * necesita el catálogo y, por tanto, la tarea de sembrarlos.
 */
class TonalSchemeColorTest {

    @Test
    fun `el generador da lo mismo con las mismas semillas`() {
        listOf(false, true).forEach { oscuro ->
            val uno = TonalScheme.scheme(SEMILLAS_DE_PRUEBA, oscuro)
            val otro = TonalScheme.scheme(SEMILLAS_DE_PRUEBA, oscuro)

            val diferencia = uno.roles().zip(otro.roles()).filter { (a, b) -> a.second != b.second }
            assertTrue("el generador no es determinista en oscuro=$oscuro: ${diferencia.map { it.first }}", diferencia.isEmpty())
        }
    }

    @Test
    fun `semillas distintas dan esquemas distintos`() {
        val uno = TonalScheme.scheme(SEMILLAS_DE_PRUEBA, false)
        val otro = TonalScheme.scheme(
            ThemeSeeds(Color(0xFF0969DA), Color(0xFF8250DF), Color(0xFF1A7F37)),
            false,
        )

        assertTrue("dos temas con semillas distintas no pueden ser el mismo esquema", uno.primary != otro.primary)
    }

    @Test
    fun `ningun color se sale del gamut de sRGB`() {
        listOf(false, true).forEach { oscuro ->
            TonalScheme.scheme(SEMILLAS_DE_PRUEBA, oscuro).roles().forEach { (rol, color) ->
                assertTrue("$rol en oscuro=$oscuro está fuera de sRGB: $color", color.enGamutSrgb())
            }
        }
    }

    @Test
    fun `una croma imposible se recorta en vez de recortarse de color`() {
        // Un magenta puro tiene la máxima croma que puede tener el gamut de sRGB. Aclararlo casi hasta el blanco
        // la saca de ese gamut, así que el generador tiene que bajarle la croma manteniendo el matiz.
        val rampa = RampaTonal.deSemilla(Color(0xFFFF00FF))
        val claro = rampa.color(95)

        assertTrue("un color recortado tiene que seguir dentro de sRGB", claro.enGamutSrgb())
        assertTrue("el recorte no puede inventar croma donde no la hay", TonalScheme.chroma(claro) < 0.2)
    }

    @Test
    fun `una semilla sin croma no inventa croma`() {
        // Los acentos tienen que salir grises: si el generador metiera croma donde la semilla no la tiene, un
        // tema de grises acabaría con un tinte que nadie eligió.
        val grises = ThemeSeeds(Color(0xFF888888), Color(0xFF999999), Color(0xFFAAAAAA))
        val esquema = TonalScheme.scheme(grises, false)
        val acentos = listOf(
            "primary" to esquema.primary,
            "secondary" to esquema.secondary,
            "tertiary" to esquema.tertiary,
        )

        acentos.forEach { (rol, color) ->
            val separacion = maxOf(color.red, color.green, color.blue) - minOf(color.red, color.green, color.blue)
            assertTrue("$rol no es un gris: $color", separacion < 1f / 255f)
        }
    }

    @Test
    fun `los neutros llevan un pelo de matiz y nada mas`() {
        // Al revés que los acentos, los neutros **sí** llevan matiz, y a propósito: es lo que hace Material con
        // los suyos, y una superficie del todo gris deja los acentos sin familia. Pero un pelo: si se notara,
        // el tema parecería otro.
        val grises = ThemeSeeds(Color(0xFF888888), Color(0xFF999999), Color(0xFFAAAAAA))
        val esquema = TonalScheme.scheme(grises, false)
        val neutros = listOf(
            "surface" to esquema.surface,
            "onSurface" to esquema.onSurface,
            "surfaceVariant" to esquema.surfaceVariant,
            "outline" to esquema.outline,
            "surfaceContainerHigh" to esquema.surfaceContainerHigh,
        )

        neutros.forEach { (rol, color) ->
            assertTrue("$rol tiene demasiado matiz: $color", TonalScheme.chroma(color) < 0.02)
        }
    }

    @Test
    fun `el tono de un gris es su tono, no un numero arbitrario`() {
        // El tono de Material es un L* de CIELAB, y para un gris su luminancia es ((40+16)/116)³ = 0.112: no
        // 0.40³, que es lo que saldría de confundir las dos escalas. Si el generador moviera los tonos, estas
        // dos cifras dejarían de cuadrar.
        val grises = ThemeSeeds(Color(0xFF888888), Color(0xFF999999), Color(0xFFAAAAAA))
        val claro = TonalScheme.scheme(grises, false)

        assertEquals(0.112, Contrast.relativeLuminance(claro.primary), 0.02)
        assertEquals(1.0, Contrast.relativeLuminance(claro.onPrimary), 0.02)
    }

    @Test
    fun `el papel es blanco y el velo es negro`() {
        val claro = TonalScheme.scheme(SEMILLAS_DE_PRUEBA, false)

        assertEquals(Color.White, claro.surfaceContainerLowest)
        assertEquals(Color.Black, claro.scrim)
    }

    @Test
    fun `el modo oscuro invierte la superficie`() {
        val claro = TonalScheme.scheme(SEMILLAS_DE_PRUEBA, false)
        val oscuro = TonalScheme.scheme(SEMILLAS_DE_PRUEBA, true)

        assertTrue("la superficie clara tiene que ser más clara que la oscura", luminancia(claro.surface) > luminancia(oscuro.surface))
        assertTrue("el texto oscuro tiene que ser más claro que el de la superficie", luminancia(oscuro.onSurface) > luminancia(oscuro.surface))
    }

    private fun luminancia(color: Color): Double = 0.2126 * color.red + 0.7152 * color.green + 0.0722 * color.blue
}