package com.estrin217.tema

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * El contraste de WCAG 2.1, en dos funciones puras (spec 025, RNF-100, RNF-101).
 *
 * Vive en el catálogo y no en la interfaz porque es una **decisión comprobable sin `Context`**: quién decide si
 * un tema se queda o se va es la prueba de `TonalSchemeContrastTest`, que no necesita Android para nada.
 *
 * Los umbrales que se comprueban (4.5 para texto normal, 3 para texto grande y para lo que no es texto) son los
 * de AA, y no bajan.
 */
object Contrast {

    /**
     * Luminancia relativa de un color sRGB: entre 0 (negro) y 1 (blanco).
     *
     * Es la fórmula de WCAG, con el canal sRGB pasado a lineal primero; saltarse ese paso es el error clásico que
     * hace que un tema pase el contraste en el papel y falle en la pantalla.
     */
    fun relativeLuminance(color: Color): Double {
        val r = srgbALineal(color.red)
        val g = srgbALineal(color.green)
        val b = srgbALineal(color.blue)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    /** Razón de contraste entre dos colores: de 1 (idénticos) a 21 (negro sobre blanco). */
    fun ratio(a: Color, b: Color): Double {
        val la = relativeLuminance(a)
        val lb = relativeLuminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }
}

/** El paso de un canal sRGB (0..1) a lineal, tal y como lo define WCAG. */
internal fun srgbALineal(canal: Float): Double {
    val c = canal.toDouble()
    return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
}