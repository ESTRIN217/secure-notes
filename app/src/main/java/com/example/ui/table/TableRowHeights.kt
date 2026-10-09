package com.example.ui.table

/**
 * Mantenimiento de la lista de altos por fila del bloque de tabla.
 *
 * La tabla **mide cada fila por separado** (spec 009, RF-56). Antes había un único alto para todas
 * las filas, y como el que se guardaba era el de la última fila medida, el contorno de selección
 * sumaba alturas que no eran las reales en cuanto una fila crecía: el error no se veía porque todas
 * las filas medían lo mismo.
 *
 * Todo lo que hay en este archivo son funciones puras: reciben la lista de altos como parámetro y
 * devuelven la nueva. No hay aquí nada que mida, nada que dibuje y nada que lea estado, y el archivo
 * no importa nada de composición a propósito: es la condición para ejercitarlo en una prueba sin
 * levantar interfaz ni depender del entorno de pruebas.
 */

/**
 * Deja la lista con una entrada por fila, conservando los altos ya medidos.
 *
 * Al añadir una fila al final los anteriores **no se mueven**: si cambiaran, el contorno saltaría
 * cada vez que se inserta una fila. Los huecos que no existían reciben [initial].
 */
fun resized(previous: List<Float>, count: Int, initial: Float): List<Float> =
    List(count.coerceAtLeast(0)) { index -> previous.getOrElse(index) { initial } }

/**
 * Quita la entrada de la fila [index]. Un índice que no existe deja la lista como estaba.
 *
 * Las filas posteriores se desplazan de sitio pero **conservan su alto**: lo que se ha ido es la
 * fila, no su medida.
 */
fun without(previous: List<Float>, index: Int): List<Float> {
    if (index !in previous.indices) return previous
    val copy = previous.toMutableList()
    copy.removeAt(index)
    return copy
}

/** Mueve la entrada de la fila [from] a la posición [to], como la fila que representa. */
fun moved(previous: List<Float>, from: Int, to: Int): List<Float> {
    if (from == to || from !in previous.indices || to !in previous.indices) return previous
    val copy = previous.toMutableList()
    copy.add(to, copy.removeAt(from))
    return copy
}

/**
 * Alto de la fila [index] con un suelo en [initial].
 *
 * Es lo que se le pasa a cada celda de esa fila: un **mínimo**, nunca un alto exacto, para que una
 * celda cuyo texto crece después de la medida pueda crecer y su texto no quede recortado (RF-59).
 */
fun rowHeightOr(previous: List<Float>, index: Int, initial: Float): Float =
    previous.getOrElse(index) { initial }