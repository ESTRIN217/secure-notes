package com.estrin217.tema

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Los ocho esquemas del catálogo tienen que verse bien, y «verse bien» se mide (spec 025, RF-161, RNF-101).
 *
 * Esta es la prueba que justifica el generador tonal: un tema escrito a mano pasa el contraste en el papel y
 * falla en la pantalla. Aquí no hay dispositivo ni ojo; hay WCAG.
 *
 * Los umbrales son los de AA y **no bajan**: 4.5 para texto normal y 3 para el acento, que a veces va en grande.
 * Si un par falla, lo que se toca es la semilla de ese tema, no el número de aquí.
 */
class TonalSchemeContrastTest {

    @Test
    fun `el texto se lee sobre su superficie, en claro y en oscuro`() {
        porCadaEsquema { tema, modo, esquema ->
            exigir(4.5, esquema.onSurface, esquema.surface, "$tema en $modo: onSurface/surface")
            exigir(4.5, esquema.onBackground, esquema.background, "$tema en $modo: onBackground/background")
            exigir(4.5, esquema.onSurfaceVariant, esquema.surfaceVariant, "$tema en $modo: onSurfaceVariant/surfaceVariant")
            exigir(4.5, esquema.onSurfaceVariant, esquema.surface, "$tema en $modo: onSurfaceVariant/surface")
        }
    }

    @Test
    fun `el acento se ve sobre su superficie`() {
        porCadaEsquema { tema, modo, esquema ->
            exigir(3.0, esquema.primary, esquema.surface, "$tema en $modo: primary/surface")
            exigir(3.0, esquema.secondary, esquema.surface, "$tema en $modo: secondary/surface")
            exigir(3.0, esquema.tertiary, esquema.surface, "$tema en $modo: tertiary/surface")
            exigir(3.0, esquema.primary, esquema.background, "$tema en $modo: primary/background")
        }
    }

    @Test
    fun `el color de acento se lee sobre su propio color`() {
        porCadaEsquema { tema, modo, esquema ->
            exigir(4.5, esquema.onPrimary, esquema.primary, "$tema en $modo: onPrimary/primary")
            exigir(4.5, esquema.onSecondary, esquema.secondary, "$tema en $modo: onSecondary/secondary")
            exigir(4.5, esquema.onTertiary, esquema.tertiary, "$tema en $modo: onTertiary/tertiary")
            exigir(4.5, esquema.onPrimaryContainer, esquema.primaryContainer, "$tema en $modo: onPrimaryContainer")
            exigir(4.5, esquema.onSecondaryContainer, esquema.secondaryContainer, "$tema en $modo: onSecondaryContainer")
            exigir(4.5, esquema.onTertiaryContainer, esquema.tertiaryContainer, "$tema en $modo: onTertiaryContainer")
        }
    }

    @Test
    fun `el error se lee en los dos modos`() {
        porCadaEsquema { tema, modo, esquema ->
            exigir(4.5, esquema.onError, esquema.error, "$tema en $modo: onError/error")
            exigir(4.5, esquema.onErrorContainer, esquema.errorContainer, "$tema en $modo: onErrorContainer")
            exigir(3.0, esquema.error, esquema.surface, "$tema en $modo: error/surface")
        }
    }

    @Test
    fun `los contenedores de superficie se distinguen entre si`() {
        // Un tema con todos los contenedores del mismo color parece plano, y además rompe la lectura de las
        // tarjetas: el ojo no encuentra dónde acaba una y empieza la otra.
        porCadaEsquema { tema, modo, esquema ->
            val distintos = listOf(
                esquema.surfaceContainerLowest,
                esquema.surfaceContainerLow,
                esquema.surfaceContainer,
                esquema.surfaceContainerHigh,
                esquema.surfaceContainerHighest,
            ).toSet().size

            assertTrue("$tema en $modo: solo hay $distintos contenedores distintos", distintos >= 4)
        }
    }

    @Test
    fun `el velo es negro en los dos modos`() {
        porCadaEsquema { tema, modo, esquema ->
            val velo = Contrast.relativeLuminance(esquema.scrim)
            assertTrue("$tema en $modo: el velo debería ser negro (luminancia $velo)", velo < 0.01)
        }
    }

    @Test
    fun `el tema claro es mas claro que el oscuro`() {
        // Una comprobación de que los dos lados no se han intercambiado por el camino.
        AppThemes.all.filter { !it.isDynamic }.forEach { tema ->
            val claro = tema.light!!
            val oscuro = tema.dark!!
            assertTrue(
                "${tema.id}: la superficie clara debería ser más clara",
                Contrast.relativeLuminance(claro.surface) > Contrast.relativeLuminance(oscuro.surface),
            )
            assertTrue(
                "${tema.id}: el acento oscuro debería ser más claro que el de su versión clara",
                Contrast.relativeLuminance(oscuro.primary) > Contrast.relativeLuminance(claro.primary),
            )
        }
    }

    @Test
    fun `ningun acento se ha quedado sin color al ajustar el tono`() {
        // El ajuste de tono mueve el acento para cumplir el contraste; si se pasara de un extremo, el acento
        // acabaría en negro en claro o en blanco en oscuro, que es un color pero no un tema.
        porCadaEsquema { tema, modo, esquema ->
            val luminancia = Contrast.relativeLuminance(esquema.primary)
            assertTrue(
                "$tema en $modo: el acento se ha ido al extremo (luminancia $luminancia)",
                luminancia in 0.02..0.8,
            )
        }
    }

    /** Recorre los ocho esquemas del catálogo: los cinco temas fijos, en claro y en oscuro. */
    private fun porCadaEsquema(comprobacion: (tema: String, modo: String, esquema: ColorScheme) -> Unit) {
        AppThemes.all.filter { !it.isDynamic }.forEach { theme ->
            comprobacion(theme.id, "claro", theme.light!!)
            comprobacion(theme.id, "oscuro", theme.dark!!)
        }
    }

    private fun exigir(minimo: Double, a: Color, b: Color, etiqueta: String) {
        val ratio = Contrast.ratio(a, b)
        assertTrue("$etiqueta = ${"%.2f".format(ratio)}:1, y hace falta $minimo:1", ratio >= minimo)
    }
}