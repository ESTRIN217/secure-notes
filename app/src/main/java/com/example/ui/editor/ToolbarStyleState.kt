package com.example.ui.editor

import com.example.data.model.BlockType
import com.example.data.model.TextBaseline
import com.example.data.model.TextSegment

/**
 * Los estilos que la barra de herramientas tiene que mostrar encendidos (spec 014, RF-87).
 *
 * Es el cálculo que se hace **en cada recomposición** para saber qué botones están pulsados, así
 * que vive aquí como función pura y la pantalla solo la llama. Antes lo calculaba el composable
 * entero, y además calculaba dos colores que **nadie leía**: cuatro conversiones por recomposición
 * para nada (RF-86).
 *
 * Devuelve solo el conjunto de estilos. Lo que la barra pinte con ellos es cosa de la pantalla.
 *
 * **Los estilos se suman, no se pisan.** Con el cursor encima de un tramo en negrita y pendiente de
 * escribir en cursiva, los dos están encendidos: lo pendiente se *añade* a lo que hay debajo. Se
 * documenta porque es contraintuitivo y porque es el comportamiento que había.
 */

/**
 * @param blockType tipo del bloque activo; de él salen las etiquetas de título.
 * @param segments los tramos del bloque activo.
 * @param cursor dónde está el cursor, en caracteres del texto plano.
 * @param collapsed si la selección está vacía. Solo entonces pesa lo pendiente de escribir.
 * @param pending estilo con el que se está escribiendo, o `null` si no hay ninguno.
 */
fun computeToolbarStyle(
    blockType: BlockType?,
    segments: List<TextSegment>,
    cursor: Int,
    collapsed: Boolean,
    pending: TextSegment?
): Set<String> {
    val estilos = mutableSetOf<String>()
    when (blockType) {
        BlockType.HEADING1 -> estilos.add("h1")
        BlockType.HEADING2 -> estilos.add("h2")
        BlockType.HEADING3 -> estilos.add("h3")
        BlockType.HEADING4 -> estilos.add("h4")
        else -> {}
    }
    collectStyles(segmentUnderCursor(segments, cursor, pending), estilos)
    if (collapsed) collectStyles(pending, estilos)
    return estilos
}

/**
 * El tramo que hay bajo el cursor, o `null` si el cursor está fuera del texto.
 *
 * Cuando hay estilo pendiente se busca también con el cursor más allá del final: se está a punto de
 * escribir y lo que importa es el tramo que se va a continuar, no uno que ya no existe.
 */
private fun segmentUnderCursor(
    segments: List<TextSegment>,
    cursor: Int,
    pending: TextSegment?
): TextSegment? {
    val total = segments.sumOf { it.text.length }
    if (cursor >= total && pending == null) return null
    var pos = 0
    for (seg in segments) {
        val fin = pos + seg.text.length
        if (pos <= cursor && cursor <= fin) return seg
        pos = fin
    }
    return null
}

private fun collectStyles(seg: TextSegment?, into: MutableSet<String>) {
    if (seg == null) return
    if (seg.bold) into.add("b")
    if (seg.italic) into.add("i")
    if (seg.underline) into.add("u")
    if (seg.strikethrough) into.add("s")
    if (seg.code) into.add("code")
    when (seg.baseline) {
        TextBaseline.SUBSCRIPT -> into.add("sub")
        TextBaseline.SUPERSCRIPT -> into.add("sup")
        else -> {}
    }
}