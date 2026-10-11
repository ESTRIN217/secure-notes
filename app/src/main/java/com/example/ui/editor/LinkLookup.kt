package com.example.ui.editor

import com.example.data.model.TextSegment

/** Dónde está un enlace dentro de los tramos de un bloque, y cuál es su rango. */
data class UrlAt(
    /** El tramo que lleva el enlace. */
    val segment: TextSegment,
    /** Posición del enlace en el texto plano, para poder sustituirlo. */
    val start: Int
) {
    /** El rango del enlace, como se guarda en `urlEditRange`. */
    val range: IntRange get() = start..(start + segment.text.length - 1)
}

/**
 * El enlace que hay **debajo** de una posición del texto, o `null` si ahí no hay ninguno.
 *
 * Estaba escrito dos veces seguidas en el diálogo del enlace —una para editarlo y otra para
 * borrarlo—, y dos copias de un mismo bucle se pueden desincronizar sin que nada se note. Es
 * exactamente el tipo de cosa que debe estar en una función con nombre y con prueba: qué cuenta
 * como «debajo» es una decisión, y la decisión tiene un caso límite.
 *
 * **El límite es el final del texto.** Un enlace termina en `start + largo - 1`, así que la
 * posición justo **detrás** de su último carácter ya no es suya. Por eso la comparación es
 * `offset < fin` y no `offset <= fin`: con `<=`, un clic al final de «aquí» encontraría el enlace y
 * lo editaría sin haberlo pulsado.
 */
fun findUrlAt(segments: List<TextSegment>, offset: Int): UrlAt? {
    var pos = 0
    for (seg in segments) {
        val fin = pos + seg.text.length
        if (pos <= offset && offset < fin && seg.linkUrl != null) {
            return UrlAt(seg, pos)
        }
        pos = fin
    }
    return null
}