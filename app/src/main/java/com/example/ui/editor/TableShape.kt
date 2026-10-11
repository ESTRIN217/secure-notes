package com.example.ui.editor

/**
 * Las celdas de una tabla nueva (spec 014, RF-48).
 *
 * Aquí estaba escondida dentro del botón de «aceptar» del diálogo, mezclada con el `AlertDialog`.
 * Es una decisión —¿la cabecera cuenta como fila de datos?— y las decisiones se prueban; los
 * diálogos no.
 *
 * **La cabecera no es una fila de datos.** Antes se creaban `dataRows` filas y, encima, una fila de
 * cabecera, así que una tabla de 3×3 sin cabecera salía con 4 filas y la primera vacía. Ahora la
 * cabecera solo existe si se ha pedido, y cuenta como la primera fila (spec 009).
 */
fun buildTableRows(
    dataRows: Int,
    columns: Int,
    hasHeader: Boolean,
    headerCell: (Int) -> String
): List<List<String>> {
    val cuerpo = List(dataRows) { List(columns) { "" } }
    if (!hasHeader) return cuerpo
    return listOf(List(columns) { headerCell(it + 1) }) + cuerpo
}