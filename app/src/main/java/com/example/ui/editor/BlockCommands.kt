package com.example.ui.editor

import com.example.data.model.BlockType
import com.example.data.model.DataBlock

/**
 * Mandos sobre la lista de bloques, sin nada de la pantalla (spec 014, RF-87).
 *
 * Los cuatro hacen lo mismo en su forma: **reciben la lista, comprueban si lo que se pide tiene
 * sentido, y devuelven la lista nueva o `null`**. Devolver `null` en vez de la lista sin tocar es
 * la decisión de diseño: quien llama necesita distinguir «no se pudo» de «sí, y aquí está», porque
 * lo primero no debe mover el cursor ni ensuciar el historial.
 *
 * Ninguna toca la lista que reciben: devuelven una copia. Es lo que permite que quien llama se
 * pase el resultado a `applyBlocksChange` sin miedo.
 */

/**
 * La etiqueta de título que corresponde a un bloque, o `null` si no es una etiqueta de título.
 *
 * La barra ofrece `h1` a `h4`; `h5` y `h6` existen en Markdown pero el editor no los tiene, así que
 * se comportan como una etiqueta desconocida en vez de como un error.
 */
fun headingTypeForTag(tag: String): BlockType? = when (tag) {
    "h1" -> BlockType.HEADING1
    "h2" -> BlockType.HEADING2
    "h3" -> BlockType.HEADING3
    "h4" -> BlockType.HEADING4
    else -> null
}

/**
 * Mueve un bloque [delta] posiciones, hacia arriba con `-1` y hacia abajo con `+1`.
 *
 * Devuelve `null` cuando el bloque no existe o cuando no hay sitio: el primero no tiene nada
 * encima ni debajo, y el último no tiene nada debajo. Solo se mueve **un** hueco, que es lo que
 * hacen los botones de la barra.
 */
fun moveBlockBy(blocks: List<DataBlock>, from: Int, delta: Int): List<DataBlock>? {
    val destino = from + delta
    if (from !in blocks.indices || destino !in blocks.indices) return null
    val copia = blocks.toMutableList()
    copia.add(destino, copia.removeAt(from))
    return copia
}

/**
 * Quita un bloque de la lista.
 *
 * Devuelve `null` si el bloque no existe **o si es el último**, porque una nota sin ningún bloque
 * no es una nota: no hay nada que editar ni dónde colocar el cursor. Se deja siempre uno.
 */
fun deleteBlockAt(blocks: List<DataBlock>, index: Int): List<DataBlock>? {
    if (index !in blocks.indices || blocks.size <= 1) return null
    val copia = blocks.toMutableList()
    copia.removeAt(index)
    return copia
}

/** Cambia el tipo y la meta de un bloque, sin tocar su contenido ni el resto de la lista. */
fun convertBlockAt(
    blocks: List<DataBlock>,
    index: Int,
    type: BlockType,
    meta: Map<String, String> = emptyMap()
): List<DataBlock>? {
    if (index !in blocks.indices) return null
    val copia = blocks.toMutableList()
    copia[index] = copia[index].copy(type = type, meta = meta)
    return copia
}