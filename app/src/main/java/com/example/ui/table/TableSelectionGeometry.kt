package com.example.ui.table

/**
 * Geometría de la selección del bloque de tabla.
 *
 * Todo lo que hay en este archivo son funciones puras: reciben la situación de la tabla como
 * parámetro y devuelven el resultado. No hay aquí nada que dibuje ni nada que lea estado, y el
 * archivo no importa nada de composición a propósito: esa es la condición que permite ejercitar
 * estas funciones en una prueba sin levantar interfaz ni depender del entorno de pruebas, que en
 * la plataforma de desarrollo no se puede ejecutar.
 */

/**
 * Celda del bloque de tabla.
 *
 * [row] con valor [HEADER_ROW] identifica la fila de cabecera, que no está entre las filas de
 * datos. Un valor mayor o igual que el número de filas ya no corresponde a ninguna celda: quien
 * llama tiene que tolerarlo, porque al quitar filas las celdas que las Apuntaban ya no existen.
 */
data class CellKey(val row: Int, val col: Int)

/** Fila de cabecera dentro de [CellKey.row]. */
const val HEADER_ROW: Int = -1

/**
 * Situación de la tabla en la que se decide qué se dibuja.
 *
 * [focused] y [selectedRow] son null cuando no hay ninguna. [active] indica que alguna celda
 * tiene el foco del sistema de escribir y [hovered] que hay un puntero sobre la tabla: la
 * condición de que la tabla esté señalada es el «o» de las dos, y vive aquí para que la barra, los
 * manejadores de fila y la limpieza al salir el puntero no se desincronicen entre sí.
 */
data class SelectionState(
    val focused: CellKey? = null,
    val selectedRow: Int? = null,
    val hovered: Boolean = false,
    val active: Boolean = false,
)

/**
 * Conjunto de celdas con foco después de que [cell] haya ganado o perdido el foco.
 *
 * Se lleva un conjunto y no una sola celda porque el orden en que llegan las notificaciones de
 * foco es indeterminado: al pasar de una celda a otra puede llegar primero la que dice que la
 * anterior pierde el foco. Con un único valor, esa notificación borraría a la nueva y la tabla
 * quedaría marcada como inactiva. Con un conjunto, los dos órdenes terminan en el mismo conjunto
 * porque las dos notificaciones nombran celdas distintas.
 */
fun nextFocusSet(current: Set<CellKey>, cell: CellKey, focused: Boolean): Set<CellKey> =
    if (focused) current + cell else current - cell

/**
 * Si la tabla está señalada y por tanto le toca mostrar la barra y los manejadores.
 */
fun isSignaled(state: SelectionState): Boolean = state.active || state.hovered

/**
 * Región rectangular en píxeles, con el origen en la esquina superior izquierda de la tabla.
 *
 * Se guarda con sus dos bordes en vez de con origen y tamaño porque el borde es lo que se dibuja, y
 * porque así una región vacía o invertida se ve en la propia lectura.
 */
data class OutlineRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

/**
 * Medidas de la tabla en píxeles.
 *
 * El origen de las coordenadas es el **borde superior de la tabla**, no el de la primera fila de
 * datos: por eso [headerHeight] entra en el cálculo aunque la región sea una celda de datos, y por
 * eso una tabla sin cabecera tiene la fila 0 empezando en cero.
 *
 * [columnWidths] no incluye ningún margen para los manejadores: estos se superponen al borde de la
 * tabla, así que no reservan ancho y no aparecen en estas medidas.
 */
data class TableMetrics(
    val columnWidths: List<Float>,
    val rowHeights: List<Float>,
    val headerHeight: Float = 0f,
    val showHeader: Boolean = false,
)

/** Borde izquierdo de la columna [col]. */
private fun TableMetrics.columnLeft(col: Int): Float {
    var x = 0f
    for (i in 0 until col.coerceAtMost(columnWidths.size)) x += columnWidths[i]
    return x
}

/** Borde superior de la fila de datos [row], contando desde el borde superior de la tabla. */
private fun TableMetrics.rowTop(row: Int): Float {
    var y = if (showHeader) headerHeight else 0f
    for (i in 0 until row.coerceAtMost(rowHeights.size)) y += rowHeights[i]
    return y
}

private fun TableMetrics.cellRect(cell: CellKey): OutlineRect? {
    if (cell.col !in columnWidths.indices) return null
    val left = columnLeft(cell.col)
    val right = left + columnWidths[cell.col]
    if (cell.row == HEADER_ROW) {
        if (!showHeader) return null
        return OutlineRect(left, 0f, right, headerHeight)
    }
    if (cell.row !in rowHeights.indices) return null
    val top = rowTop(cell.row)
    return OutlineRect(left, top, right, top + rowHeights[cell.row])
}

private fun TableMetrics.dataRowRect(row: Int): OutlineRect? {
    if (row !in rowHeights.indices || columnWidths.isEmpty()) return null
    val top = rowTop(row)
    return OutlineRect(columnLeft(0), top, columnWidths.sum(), top + rowHeights[row])
}

/**
 * Región que lleva contorno de selección, o null si no hay ninguna.
 *
 * Devuelve **una sola región o ninguna**, nunca una colección de ellas. No es un detalle de la
 * firma: mientras pueda devolver varias, el dibujado no tiene forma de saber si tiene que unirlas
 * en un contorno continuo o pintarlas sueltas, y pintarlas sueltas es precisamente la escalera que
 * este requisito viene a quitar.
 *
 * La prioridad es de la celda enfocada sobre la fila seleccionada, para que nunca haya dos
 * contornos a la vez. Si la celda enfocada ya no existe —se quitó la fila mientras el foco seguía en
 * ella— se devuelve null en vez de un rectángulo fuera de la tabla: es preferible no dibujar nada a
 * dibujar un contorno flotando donde ya no hay contenido.
 */
fun selectionOutline(metrics: TableMetrics, state: SelectionState): OutlineRect? {
    val focused = state.focused
    if (focused != null) return metrics.cellRect(focused)
    val row = state.selectedRow ?: return null
    return metrics.dataRowRect(row)
}

/**
 * Centro vertical de un manejador por fila, contando la de cabecera si la hay.
 *
 * El orden es el mismo en que se recorren las filas, así que el manejador número `i` es siempre el
 * de la fila `i` y quien lo dibuja no necesita saber qué fila es. Que el total se derive de las
 * medidas y no de un contador guardado es lo que recoloca los manejadores solos cuando se añaden,
 * se quitan o se reordenan filas: no hay posición que pueda quedarse desfasada.
 *
 * Con la cabecera desactivada no deja hueco por encima: el primer centro es la mitad de la primera
 * fila, no la primera fila más el hueco de una cabecera que no está.
 */
fun rowHandleCenters(metrics: TableMetrics): List<Float> {
    val centers = ArrayList<Float>(metrics.rowHeights.size + 1)
    if (metrics.showHeader) centers.add(metrics.headerHeight / 2f)
    metrics.rowHeights.forEachIndexed { row, height ->
        centers.add(metrics.rowTop(row) + height / 2f)
    }
    return centers
}

/**
 * Desplazamiento horizontal del borde izquierdo de un manejador de columna de [handleWidth] de
 * ancho, para que quede centrado sobre la columna [col]. Null si esa columna ya no existe.
 *
 * Devuelve el borde izquierdo y no el centro porque lo que se dibuja es el borde: quien llama lo
 * coloca en pantalla con ese valor y no necesita volver a recentrar nada.
 */
fun columnHandleLeft(metrics: TableMetrics, col: Int, handleWidth: Float): Float? {
    val width = metrics.columnWidths.getOrNull(col) ?: return null
    return metrics.columnLeft(col) + width / 2f - handleWidth / 2f
}