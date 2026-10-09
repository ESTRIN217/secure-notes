package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.data.model.TableData
import com.example.ui.table.CellKey
import com.example.ui.table.SelectionState
import com.example.ui.table.TableMetrics
import com.example.ui.table.TableSelectionOverlay
import com.example.ui.table.columnHandleLeft
import com.example.ui.table.moved
import com.example.ui.table.nextFocusSet
import com.example.ui.table.resized
import com.example.ui.table.rowHandleCenters
import com.example.ui.table.rowHeightOr
import com.example.ui.table.without
import kotlin.math.roundToInt

private data class FocusedCell(val row: Int, val col: Int)

// Solo el hex: la etiqueta no se pintaba en ninguna parte y quedaba hardcodeada
// en inglés. El swatch ya se explica solo con el color.
private val BG_OPTIONS = listOf<String?>(
    null,
    "FFF8E1",
    "E3F2FD",
    "E8F5E9",
    "FCE4EC",
    "FFF3E0",
    "EDE7F6"
)

// Tokens MD3 en vez de literales: los colores fijos rompían el tema oscuro.
private val TableHighlightBgAlpha = 0.08f
private val TableDropAlpha = 0.12f
private val TableHeaderAlpha = 0.25f
private val TableRowActiveAlpha = 0.06f

/**
 * Alto del margen que aloja la barra. Es constante: la tabla debe estar en el mismo
 * sitio tenga la barra o no la tenga, que es lo que impide que salte al enfocarla.
 *
 * Mide más que el mínimo de 56 dp de la barra expresiva más el hueco: por eso no son los
 * 44 dp que se habían estimado, porque la barra de Material 3 expresivo no baja de ese alto.
 */
private val TableToolbarMargin = 60.dp

/** Separación entre el borde inferior de la barra y el borde superior de la tabla. */
private val TableToolbarGap = 4.dp

/** Respiro bajo la tabla, para que no quede pegada al bloque de abajo. */
private val TableBottomMargin = 12.dp

/**
 * Suelo de altura de una celda: el mínimo táctil de Material.
 *
 * Es un **mínimo y nunca un tope**: una celda cuyo texto no cabe en él crece, porque una tabla no
 * puede ocultar texto (spec 009, RF-55 y RF-59).
 */
private val MinCellHeight = 44.dp

/**
 * Tamaño de los manejadores: el de columna va horizontal y el de fila vertical. Es el
 * mismo número a propósito, para que el carril de la izquierda mida igual que el
 * manejador de columna.
 *
 * 48 dp, el objetivo táctil mínimo de Material, porque los tres ya son `IconButton`:
 * antes medían 16×24 dp (o 24×16) y se quedaban en un tercio del mínimo. El carril
 * crece con ellos, así que la tabla pierde 24 dp de ancho útil: es el coste consciente
 * de la decisión 1 de la spec 007.
 */
private val HandleWidth = 48.dp
private val HandleHeight = 48.dp
private val ColumnHandleHeight = 48.dp

/**
 * Carril de los manejadores de fila, a la izquierda de la primera columna.
 *
 * El texto de la primera celda arranca justo donde acaba el carril. El carril se
 * superpone al borde de la tarjeta y no es opcional: con los manejadores encima de la
 * celda tapaban la primera letra de cada fila, porque son más anchos que el relleno
 * horizontal de la celda.
 */
private val TableRowGutter = HandleWidth

private val LINK_REGEX = Regex("(https?|note)://")

private fun isLink(text: String): Boolean = LINK_REGEX.containsMatchIn(text)

private fun parseHex(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    return try {
        val cleanHex = hex.hex()
        val colorInt = cleanHex.toLong(16)
        if (cleanHex.length == 6) {
            Color(colorInt or 0xFF000000)
        } else if (cleanHex.length == 8) {
            Color(colorInt)
        } else null
    } catch (e: Exception) {
        null
    }
}

private fun String.hex(): String = trim().removePrefix("#")


// Colapsa una selección al nuevo largo del texto, preservando el rango.
private fun TextRange.clampedTo(length: Int): TextRange {
    val s = start.coerceIn(0, length)
    return TextRange(s, end.coerceIn(s, length))
}

// Solo se cambia de celda cuando la pulsación no puede tener otro significado
// dentro del campo: con texto seleccionado o el cursor a media celda, las
// flechas las gestiona el propio campo (mover cursor, seleccionar con Shift).
private fun cellMoveFor(event: KeyEvent, selection: TextRange, textLength: Int): Pair<Int, Int>? {
    if (event.type != KeyEventType.KeyDown) return null
    return when (event.key) {
        // En un campo singleLine arriba/abajo no tienen uso nativo: navegación tipo hoja de cálculo.
        Key.DirectionDown -> Pair(1, 0)
        Key.DirectionUp -> Pair(-1, 0)
        Key.Enter -> if (selection.collapsed) Pair(1, 0) else null
        Key.Tab -> Pair(0, if (event.isShiftPressed) -1 else 1)
        Key.DirectionRight -> if (selection.collapsed && selection.start >= textLength) Pair(0, 1) else null
        Key.DirectionLeft -> if (selection.collapsed && selection.start <= 0) Pair(0, -1) else null
        else -> null
    }
}

// ============ 1. STATE HOLDER (SRP) ============
// Toda la lógica y el estado de la tabla viven aquí. La vista solo observa
// este estado y delega en él cada mutación. (Mover a un ViewModel si la tabla
// llegara a persistir en la base de datos.)
class TableBlockState(initialData: TableData) {
    // La cabecera es la fila 0: no hay ninguna lista de títulos aparte (spec 008).
    var rows by mutableStateOf(initialData.rows.map { it.toMutableList() }.toMutableList())
    var weights by mutableStateOf(initialData.normalizedWeights().toMutableList())
    var bgColorHex by mutableStateOf(initialData.bgColorHex?.hex())
    var showHeader by mutableStateOf(initialData.showHeader)
    var showHeaderColumn by mutableStateOf(initialData.showHeaderColumn)
    // Conjunto de celdas con foco, no una celda: el desenfoque de una puede llegar
    // después del foco de la siguiente y con un solo valor se borrarían entre ellas.
    var focusedCells by mutableStateOf(emptySet<CellKey>())
    // Última celda tocada. No se limpia nunca: alimenta las acciones contextuales de
    // la hoja, que es la única vía para añadir o quitar filas y columnas.
    private var lastFocused by mutableStateOf<CellKey?>(null)
    var hovered by mutableStateOf(false)
    var selectedRow by mutableStateOf<Int?>(null)
    var showRowMenu by mutableStateOf(false)
    var showColMenu by mutableStateOf(false)
    var dragRow by mutableStateOf<Int?>(null)
    var dragRowTarget by mutableStateOf<Int?>(null)
    var dragCol by mutableStateOf<Int?>(null)
    var dragColTarget by mutableStateOf<Int?>(null)
    // Un alto por fila, no uno para toda la tabla: cada fila mide lo que mide su celda más alta.
    // Con un alto compartido el contorno sumaba alturas que no eran las reales en cuanto una crecía
    // (spec 009, RF-56). Arranca a cero porque el alto real solo se sabe tras medir.
    var rowHeightsPx by mutableStateOf(resized(emptyList(), initialData.rows.size, 0f))

    val focusRequesters = mutableMapOf<Pair<Int, Int>, FocusRequester>()

    private var changeListener: (TableData) -> Unit = {}

    fun registerOnChange(listener: (TableData) -> Unit) {
        changeListener = listener
    }

    fun reloadFrom(data: TableData) {
        val current = toTableData()
        if (current.rows == data.rows &&
            current.columnWeights == data.columnWeights &&
            current.bgColorHex == data.bgColorHex &&
            current.showHeader == data.showHeader &&
            current.showHeaderColumn == data.showHeaderColumn
        ) return
        rows = data.rows.map { it.toMutableList() }.toMutableList()
        weights = data.normalizedWeights().toMutableList()
        rowHeightsPx = resized(rowHeightsPx, rows.size, 0f)
        bgColorHex = data.bgColorHex?.hex()
        showHeader = data.showHeader
        showHeaderColumn = data.showHeaderColumn
    }

    // --- Derivados (solo lectura) ---
    val columnCount: Int
        get() = rows.maxOfOrNull { it.size } ?: 0
    val focusedRow: Int?
        get() = lastFocused?.row
    val focusedCol: Int?
        get() = lastFocused?.col
    /** Celda enfocada para el contorno, o null si ya no existe la fila o la columna. */
    val focusedCellForOutline: CellKey?
        get() = lastFocused?.takeIf { it.col in 0 until columnCount }
            ?.takeIf { it.row in rows.indices }
    val hasFocus: Boolean
        get() = focusedCells.isNotEmpty()
    /** La tabla está señalada con el foco o con un puntero encima. */
    val isSignaled: Boolean
        get() = com.example.ui.table.isSignaled(
            SelectionState(
                focused = lastFocused,
                selectedRow = selectedRow,
                hovered = hovered,
                active = hasFocus,
            )
        )
    // Con los "+" fuera, la hoja de opciones es la única vía para añadir o
    // quitar filas y columnas: siempre cae en un destino válido.
    val activeCol: Int
        get() = (focusedCol ?: 0).coerceIn(0, (columnCount - 1).coerceAtLeast(0))
    val activeRow: Int
        get() = (selectedRow ?: focusedRow ?: 0).coerceIn(0, (rows.size - 1).coerceAtLeast(0))

    fun cellWeight(col: Int): Float = weights.getOrElse(col) { 1f }.coerceAtLeast(0.1f)

    private fun toTableData() = TableData(
        rows = rows.map { it.toList() },
        columnWeights = weights.toList(),
        bgColorHex = bgColorHex,
        showHeader = showHeader,
        showHeaderColumn = showHeaderColumn
    )

    private fun notifyChange() {
        changeListener(toTableData())
    }

    // --- Mutadores ---
    fun onHover(inside: Boolean) {
        hovered = inside
        if (!isSignaled) selectedRow = null
    }

    // El conjunto es lo que decide si la tabla está activa; lastFocused solo
    // recuerda dónde estaba el cursor para las acciones de la hoja.
    fun selectCell(rowIndex: Int, col: Int, focused: Boolean) =
        setCellFocus(CellKey(rowIndex, col), focused)

    private fun setCellFocus(cell: CellKey, focused: Boolean) {
        focusedCells = nextFocusSet(focusedCells, cell, focused)
        if (focused) lastFocused = cell
        if (!isSignaled) selectedRow = null
    }

    fun selectRow(rowIndex: Int) {
        selectedRow = rowIndex
    }

    fun setCellValue(rowIndex: Int, col: Int, value: String) {
        if (rowIndex !in rows.indices) return
        val row = rows[rowIndex]
        while (row.size <= col) row.add("")
        if (row[col] != value) {
            row[col] = value
            notifyChange()
        }
    }

    fun insertRowAbove(rowIndex: Int) {
        val at = rowIndex.coerceIn(0, rows.size)
        rows.add(at, MutableList(columnCount) { "" })
        rowHeightsPx = resized(rowHeightsPx, rows.size, 0f)
        selectedRow = at.coerceIn(0, rows.size - 1)
        notifyChange()
    }

    fun insertRowBelow(rowIndex: Int) {
        val at = (rowIndex + 1).coerceIn(0, rows.size)
        rows.add(at, MutableList(columnCount) { "" })
        rowHeightsPx = resized(rowHeightsPx, rows.size, 0f)
        selectedRow = at.coerceIn(0, rows.size - 1)
        notifyChange()
    }

    fun deleteRow(rowIndex: Int) {
        if (rows.size <= 1) return
        rows.removeAt(rowIndex.coerceIn(0, rows.lastIndex))
        rowHeightsPx = without(rowHeightsPx, rowIndex.coerceIn(0, rows.size))
        selectedRow = null
        notifyChange()
    }

    fun insertColumnLeft(colIndex: Int) {
        rows.forEach { it.add(colIndex.coerceIn(0, it.size), "") }
        if (colIndex in weights.indices) weights.add(colIndex, 1f) else weights.add(1f)
        selectedRow = null
        notifyChange()
    }

    fun insertColumnRight(colIndex: Int) {
        rows.forEach { it.add((colIndex + 1).coerceIn(0, it.size), "") }
        weights.add((colIndex + 1).coerceIn(0, weights.size), 1f)
        selectedRow = null
        notifyChange()
    }

    fun deleteColumn(colIndex: Int) {
        if (columnCount > 1) {
            rows.forEach { if (colIndex < it.size) it.removeAt(colIndex) }
            if (colIndex < weights.size) weights.removeAt(colIndex)
            selectedRow = null
            notifyChange()
        }
    }

    fun moveRow(from: Int, to: Int) {
        if (from == to || from !in rows.indices || to !in rows.indices) return
        val item = rows.removeAt(from)
        rows.add(to, item)
        // El alto viaja con la fila: si la alta pasa a ser la última, el resto no se desplaza de
        // sitio y por tanto no puede medirse contra la altura equivocada (RF-56).
        rowHeightsPx = moved(rowHeightsPx, from, to)
        selectedRow = to
        notifyChange()
    }

    // Reordenar columnas no necesita ninguna condición aparte: cada título es la celda de la
// primera fila de su columna y viaja con ella, porque es una fila más (spec 008).
fun moveColumn(from: Int, to: Int) {
        if (from == to || columnCount == 0) return
        if (from !in weights.indices || to !in weights.indices) return
        rows.forEach { row -> if (from < row.size) { val c = row.removeAt(from); row.add(to, c) } }
        val w = weights.removeAt(from)
        weights.add(to, w)
        selectedRow = null
        notifyChange()
    }

    fun commitRowDrag() {
        val from = dragRow
        val to = dragRowTarget
        dragRow = null
        dragRowTarget = null
        if (from != null && to != null) moveRow(from, to)
    }

    fun commitColumnDrag() {
        val from = dragCol
        val to = dragColTarget
        dragCol = null
        dragColTarget = null
        if (from != null && to != null) moveColumn(from, to)
    }

    fun adjustColumnWidth(col: Int, delta: Float, colUnitPx: Float) {
        if (col !in weights.indices) return
        weights[col] = (cellWeight(col) + delta / colUnitPx.coerceAtLeast(1f)).coerceIn(0.1f, 6f)
        notifyChange()
    }

    fun fitToContent() {
        if (columnCount == 0) return
        weights = (0 until columnCount).map { col ->
            // La primera fila solo cuenta para el ancho si hace de cabecera: si la opción está
            // apagada es una fila de datos más y se mide con las demás.
            val header = if (showHeader) rows.firstOrNull()?.getOrNull(col)?.length ?: 0 else 0
            val cell = rows.maxOfOrNull { it.getOrNull(col)?.length ?: 0 } ?: 0
            (maxOf(header, cell) + 3).coerceAtLeast(3).toFloat()
        }.toMutableList()
        notifyChange()
    }

    fun fitToScreen() {
        weights = MutableList(columnCount) { 1f }
        notifyChange()
    }

    fun toggleHeader() {
        showHeader = !showHeader
        notifyChange()
    }

    fun toggleHeaderColumn() {
        showHeaderColumn = !showHeaderColumn
        notifyChange()
    }

    fun setBgColor(hex: String?) {
        bgColorHex = hex?.hex()
        notifyChange()
    }

    fun moveFocus(dRow: Int, dCol: Int) {
        val cur = lastFocused ?: return
        if (columnCount == 0 || rows.isEmpty()) return
        selectedRow = null
        val newRow = (cur.row + dRow).coerceIn(0, rows.lastIndex)
        val newCol = (cur.col + dCol).coerceIn(0, columnCount - 1)
        focusRequesters[Pair(newRow, newCol)]?.requestFocus()
    }
}

@Composable
fun rememberTableBlockState(
    tableData: TableData,
    onChange: (TableData) -> Unit
): TableBlockState {
    val state = remember { TableBlockState(tableData) }
    val currentOnChange by rememberUpdatedState(onChange)
    LaunchedEffect(state) {
        state.registerOnChange { currentOnChange(it) }
    }
    LaunchedEffect(tableData) {
        state.reloadFrom(tableData)
    }
    return state
}

// ============ 2. COMPONENTE ORQUESTADOR (LA TABLA) ============
/**
 * Avisa de si hay un puntero sobre el bloque.
 *
 * Se cubren puntero flotante y contacto porque no son lo mismo: `Enter` y `Exit` solo los produce un
 * dispositivo que flota, y en una pantalla táctil el dedo únicamente da `Press` y `Release`. Con solo
 * los primeros, tocar la tabla con el dedo no la marcaría, que es justo como se usa en la práctica.
 *
 * El `Release` solo desmarca si el contacto es del dedo. Un clic de ratón también produce `Release`,
 * y sin esa comprobación la barra se apagaría con el cursor todavía dentro de la tabla.
 */
private fun Modifier.hoverSignal(onHover: (Boolean) -> Unit): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            val fromTouch = event.changes.any { it.type == PointerType.Touch }
            when (event.type) {
                PointerEventType.Enter -> onHover(true)
                PointerEventType.Press -> onHover(true)
                PointerEventType.Exit -> onHover(false)
                PointerEventType.Release -> if (fromTouch) onHover(false)
                else -> Unit
            }
        }
    }
}
@Composable
fun EditableTableBlock(
    tableData: TableData,
    onChange: (TableData) -> Unit,
    onFocusChange: (Boolean) -> Unit = {},
    onDeleteBlock: () -> Unit = {},
    onInsertBelow: () -> Unit = {},
    onDuplicate: () -> Unit = {},
    onMoveTo: () -> Unit = {},
    onDragTableStart: () -> Unit = {},
    onDragTableBy: (Float) -> Unit = {},
    onDragTableEnd: () -> Unit = {},
    onDragTableCancel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state = rememberTableBlockState(tableData, onChange)
    var showMoreMenu by remember { mutableStateOf(false) }
    val tableBg = parseHex(state.bgColorHex) ?: Color.Transparent
    val primary = MaterialTheme.colorScheme.primary

    BoxWithConstraints(modifier.fillMaxWidth()) {
        // El carril se descuenta del ancho de las columnas: si no, la tabla se saldría
        // de la tarjeta por la izquierda en cuanto hiciera falta una columna más.
        val availableWidthForCols = (maxWidth - TableRowGutter).coerceAtLeast(0.dp)

        val totalWeight = remember(state.weights, state.columnCount) {
            (0 until state.columnCount)
                .sumOf { state.cellWeight(it).toDouble() }
                .toFloat()
                .coerceAtLeast(0.1f)
        }

        val minUnitWidth = 100.dp
        val totalNaturalWidth = minUnitWidth * totalWeight
        val fitsOnScreen = totalNaturalWidth <= availableWidthForCols

        val getColumnWidth: (Int) -> Dp = { col ->
            val weight = state.cellWeight(col)
            if (fitsOnScreen) {
                availableWidthForCols * (weight / totalWeight)
            } else {
                minUnitWidth * weight
            }
        }

        val colUnitPx = with(LocalDensity.current) {
            if (fitsOnScreen) (availableWidthForCols / totalWeight).toPx() else minUnitWidth.toPx()
        }

        // Una sola medida compartida por el contorno y por los manejadores: si la
        // calcularan por separado, cualquier diferencia entre las dos los desincronizaría
        // y el contorno caería donde no es la celda.
        val tableMetrics = with(LocalDensity.current) {
            TableMetrics(
                columnWidths = (0 until state.columnCount).map { getColumnWidth(it).toPx() },
                rowHeights = state.rowHeightsPx,
                gutterWidth = TableRowGutter.toPx(),
            )
        }

        // El contenedor NO pregunta por el foco: onFocusChanged solo se lo avisa al
        // elemento al que está aplicado, y un contenedor que solo agrupa celdas no es
        // destino de foco, así que nunca se entera. El foco se lleva celda a celda,
        // que sí son destinos legítimos, y se agrega en un conjunto.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .hoverSignal(state::onHover)
        ) {
            // Margen que la tabla tiene siempre por encima, para alojar la barra. El
        // editor no pone espacio entre bloques, así que si no lo reservara la barra
        // tendría que tapar el bloque de arriba. Como el alto es constante y solo
        // cambia la visibilidad de la barra dentro, la tabla no se mueve nunca.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TableToolbarMargin)
                .padding(bottom = TableToolbarGap)
        ) {
            // Sin AnimatedVisibility: dentro de un Box su sobrecarga se resuelve a la
            // de ColumnScope y no compila. La aparición y desaparición no cambian la
            // altura del margen, así que la tabla no se mueve igual.
            if (state.isSignaled) {
                FloatingToolbar(
                    // Abajo a la derecha y pegada a la tabla: si la barra llegara a
                    // ser más alta que el margen, el desborde cae hacia arriba y nunca
                    // encima de la primera fila.
                    modifier = Modifier.align(Alignment.BottomEnd),
                    onOpenMoreMenu = { showMoreMenu = true },
                    onDragTableStart = onDragTableStart,
                    onDragTableBy = onDragTableBy,
                    onDragTableEnd = onDragTableEnd,
                    onDragTableCancel = onDragTableCancel
                )
            }
        }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = if (tableBg == Color.Transparent) MaterialTheme.colorScheme.surface else tableBg
                )
            ) {
                // El gutter de los handles se superpone a la esquina izquierda en
                // lugar de reservarse: la tabla aprovecha todo el ancho disponible.
                // La cabecera no lleva gutter porque sus handles cuelgan del borde
                // de cada celda.
                Box {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = TableRowGutter)
                            .background(tableBg)
                            .horizontalScroll(rememberScrollState())
                    ) {
                        // La cabecera es la fila 0, así que no hay una fila aparte que pintar:
                        // todas entran por aquí y la primera se pinta de cabecera si toca.
                        state.rows.forEachIndexed { rowIndex, row ->
                            key(rowIndex) {
                                TableRow(
                                    row = row,
                                    rowIndex = rowIndex,
                                    columnCount = state.columnCount,
                                    primary = primary,
                                    isHeaderRow = state.showHeader && rowIndex == 0,
                                    showHeaderColumn = state.showHeaderColumn,
                                    selectedRow = state.selectedRow,
                                    isDropRow = state.dragRow != null && state.dragRow != rowIndex && state.dragRowTarget == rowIndex,
                                    isDropCol = state.dragCol != null && state.dragCol != state.dragColTarget && state.dragColTarget != null,
                                    getColumnWidth = getColumnWidth,
                                    colUnitPx = colUnitPx,
                                    onCellValueChange = { col, value -> state.setCellValue(rowIndex, col, value) },
                                    onCellSelect = { col, focused ->
                                        state.selectCell(rowIndex, col, focused)
                                        onFocusChange(focused)
                                    },
                                    onResizeColumn = { col, delta -> state.adjustColumnWidth(col, delta, colUnitPx) },
                                    rowHeightPx = rowHeightOr(state.rowHeightsPx, rowIndex, 0f),
                                    onRowHeight = { alto ->
                                        state.rowHeightsPx = state.rowHeightsPx.toMutableList()
                                            .also { lista ->
                                                if (rowIndex in lista.indices) lista[rowIndex] = alto
                                            }
                                    },
                                    focusRequesters = state.focusRequesters,
                                    onMoveFocus = state::moveFocus
                                )
                            }
                        }
                    }

                    // Manejadores superpuestos al borde izquierdo: uno por fila, colocados
                    // con la geometría ya comprobada. No reservan ancho y no cambian el alto
                    // de nada, así que no mueven la tabla.
                    if (state.isSignaled) {
                        TableHandleOverlay(
                            metrics = tableMetrics,
                            focused = state.focusedCellForOutline,
                            selectedRow = state.selectedRow,
                            showColMenu = state.showColMenu,
                            showRowMenu = state.showRowMenu,
                            onShowColMenuChange = { state.showColMenu = it },
                            onShowRowMenuChange = { state.showRowMenu = it },
                            onRowSelect = state::selectRow,
                            onColumnSelect = { col -> state.selectCell(0, col, true) },
                            onInsertRowAbove = state::insertRowAbove,
                            onInsertRowBelow = state::insertRowBelow,
                            onDeleteRow = state::deleteRow,
                            canDeleteRow = state.rows.size > 1,
                            onInsertColumnLeft = state::insertColumnLeft,
                            onInsertColumnRight = state::insertColumnRight,
                            onDeleteColumn = state::deleteColumn,
                            canDeleteColumn = state.columnCount > 1,
                            onDragRowState = { s, t -> state.dragRow = s; state.dragRowTarget = t },
                            onCommitRow = { state.commitRowDrag() },
                            onCancelRow = { state.dragRow = null; state.dragRowTarget = null },
                            onDragColState = { s, t -> state.dragCol = s; state.dragColTarget = t },
                            onCommitCol = { state.commitColumnDrag() },
                            onCancelCol = { state.dragCol = null; state.dragColTarget = null },
                            modifier = Modifier.matchParentSize()
                        )
                    }

                    // Contorno de selección: una sola región por encima de las
                    // celdas. Las medidas salen de lo ya medido, así que no hay
                    // una segunda medición que pueda desincronizarse.
                    TableSelectionOverlay(
                        metrics = tableMetrics,
                        state = SelectionState(
                            focused = state.focusedCellForOutline,
                            selectedRow = state.selectedRow,
                            hovered = state.hovered,
                            active = state.hasFocus,
                        ),
                        modifier = Modifier.matchParentSize()
                    )
                }
            }

            Spacer(Modifier.height(TableBottomMargin))

            if (showMoreMenu) {
                TableBlockOptionsSheet(
                    state = state,
                    canDeleteColumn = state.columnCount > 1,
                    canDeleteRow = state.rows.size > 1,
                    onInsertBelow = {
                        showMoreMenu = false
                        onInsertBelow()
                    },
                    onDuplicate = {
                        showMoreMenu = false
                        onDuplicate()
                    },
                    onMoveTo = {
                        showMoreMenu = false
                        onMoveTo()
                    },
                    onDeleteTable = {
                        showMoreMenu = false
                        onDeleteBlock()
                    },
                    onSelectBg = { state.setBgColor(it) },
                    onDismiss = { showMoreMenu = false }
                )
            }
        }
    }
}

@Composable
private fun HeaderColumnHandle(
    col: Int,
    primary: Color,
    showMenu: Boolean,
    onShowMenuChange: (Boolean) -> Unit,
    onInsertLeft: () -> Unit,
    onInsertRight: () -> Unit,
    onDeleteColumn: () -> Unit,
    canDeleteColumn: Boolean,
    colUnitPx: Float,
    columnCount: Int,
    onDragCol: (Int, Int) -> Unit,
    onCommitCol: () -> Unit,
    onCancelCol: () -> Unit
) {
    Box {
        NotionSelectionHandle(
            isVertical = false,
            primary = primary,
            contentDescription = stringResource(R.string.table_column_actions),
            onClick = { onShowMenuChange(true) },
            onDragModifier = Modifier.pointerInput(col) {
                var start = col
                var acc = 0f
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        start = col
                        acc = 0f
                        onDragCol(start, start)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        acc += dragAmount.x
                        val target = (start + (acc / colUnitPx.coerceAtLeast(1f)).roundToInt()).coerceIn(0, columnCount - 1)
                        onDragCol(start, target)
                    },
                    onDragEnd = { onCommitCol() },
                    onDragCancel = { onCancelCol() }
                )
            }
        )
        DropdownMenu(expanded = showMenu, onDismissRequest = { onShowMenuChange(false) }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.insert_column_left)) }, onClick = { onShowMenuChange(false); onInsertLeft() })
            DropdownMenuItem(text = { Text(stringResource(R.string.insert_column_right)) }, onClick = { onShowMenuChange(false); onInsertRight() })
            DropdownMenuItem(text = { Text(stringResource(R.string.delete_column)) }, onClick = { onShowMenuChange(false); onDeleteColumn() }, enabled = canDeleteColumn)
        }
    }
}

/**
 * Una fila de la tabla.
 *
 * No hay una fila de cabecera aparte: la primera fila lo es cuando [isHeaderRow] está activo, y es
 * una fila más en todo lo demás —mismo alto mínimo, mismo relleno, mismos enlaces— (spec 008).
 */
@Composable
private fun TableRow(
    row: List<String>,
    rowIndex: Int,
    columnCount: Int,
    primary: Color,
    isHeaderRow: Boolean,
    showHeaderColumn: Boolean,
    selectedRow: Int?,
    isDropRow: Boolean,
    isDropCol: Boolean,
    getColumnWidth: (Int) -> Dp,
    colUnitPx: Float,
    onCellValueChange: (Int, String) -> Unit,
    onCellSelect: (Int, Boolean) -> Unit,
    onResizeColumn: (Int, Float) -> Unit,
    rowHeightPx: Float,
    onRowHeight: (Float) -> Unit,
    focusRequesters: MutableMap<Pair<Int, Int>, FocusRequester>,
    onMoveFocus: (Int, Int) -> Unit
) {
    val isActiveRow = selectedRow == rowIndex
    // Todas las celdas de la fila arrancan con el alto ya medido de esa fila, para que el borde y
    // el tinte de cabecera cubran la fila entera y no solo lo que ocupa su texto (RF-58). Antes de
    // medir vale el suelo táctil; después manda el alto real de la fila.
    val cellMinHeight = with(LocalDensity.current) { maxOf(rowHeightPx.toDp(), MinCellHeight) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                when {
                    // Al arrastrar una columna se señala la fila 0, que es la de los títulos.
                    isDropCol -> primary.copy(alpha = TableDropAlpha)
                    isDropRow -> primary.copy(alpha = TableDropAlpha)
                    isActiveRow -> primary.copy(alpha = TableRowActiveAlpha)
                    rowIndex % 2 == 1 -> MaterialTheme.colorScheme.surfaceContainerLow
                    else -> Color.Transparent
                }
            )
            .onGloballyPositioned { onRowHeight(it.size.height.toFloat()) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (col in 0 until columnCount) {
            key(rowIndex, col) {
                val cell = row.getOrElse(col) { "" }
                val focusRequester = remember { FocusRequester() }
                DisposableEffect(focusRequester) {
                    focusRequesters[Pair(rowIndex, col)] = focusRequester
                    onDispose { focusRequesters.remove(Pair(rowIndex, col)) }
                }
                TableCell(
                    cell = cell,
                    // Cabecera es fila o columna, nunca una cosa u otra: las dos pintan igual.
                    isHeader = isHeaderRow || (showHeaderColumn && col == 0),
                    primary = primary,
                    onValueChange = { onCellValueChange(col, it) },
                    onFocusChange = { onCellSelect(col, it) },
                    onMoveFocus = onMoveFocus,
                    focusRequester = focusRequester,
                    // La barra de ancho es un tirador de columna y vive en la primera fila,
                    // con la opción de cabecera activada o no: si no, no habría forma de
                    // cambiar el ancho con la cabecera apagada.
                    resizeBar = if (rowIndex == 0 && col < columnCount - 1) {
                        {
                            ColumnResizeBar(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .width(6.dp)
                                    .fillMaxHeight()
                                    .pointerInput(col) {
                                        detectHorizontalDragGestures { _, delta ->
                                            onResizeColumn(col, delta)
                                        }
                                    },
                                primary = primary,
                            )
                        }
                    } else null,
                    modifier = Modifier
                        .width(getColumnWidth(col))
                        .heightIn(min = cellMinHeight)
                )
            }
        }
    }
}

// Capa superpuesta de los manejadores. Va aparte de las filas porque cuelgan del
// borde del Card: no deben ocupar ancho de columna ni alterar el alto de la fila, y
// por eso aparecer y desaparecer no mueve la tabla.
//
// Las posiciones salen de la geometría comprobada, no de sumar alturas aquí, que es
// lo que garantiza que un manejador siga en su fila cuando las filas cambian de alto.
@Composable
private fun TableHandleOverlay(
    metrics: TableMetrics,
    focused: CellKey?,
    selectedRow: Int?,
    showColMenu: Boolean,
    showRowMenu: Boolean,
    onShowColMenuChange: (Boolean) -> Unit,
    onShowRowMenuChange: (Boolean) -> Unit,
    onRowSelect: (Int) -> Unit,
    onColumnSelect: (Int) -> Unit,
    onInsertRowAbove: (Int) -> Unit,
    onInsertRowBelow: (Int) -> Unit,
    onDeleteRow: (Int) -> Unit,
    canDeleteRow: Boolean,
    onInsertColumnLeft: (Int) -> Unit,
    onInsertColumnRight: (Int) -> Unit,
    onDeleteColumn: (Int) -> Unit,
    canDeleteColumn: Boolean,
    onDragRowState: (Int, Int) -> Unit,
    onCommitRow: () -> Unit,
    onCancelRow: () -> Unit,
    onDragColState: (Int, Int) -> Unit,
    onCommitCol: () -> Unit,
    onCancelCol: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val centers = rowHandleCenters(metrics)
    val handleWidthPx = with(LocalDensity.current) { HandleWidth.toPx() }
    val handleHeightPx = with(LocalDensity.current) { HandleHeight.toPx() }
    // Un solo manejador, el de la fila activa: con uno por fila el carril se llenaba
    // de tiradores y la tabla se leía con ruido. La fila activa es la seleccionada o
    // la que tiene la celda enfocada, con la misma prioridad que el contorno.
    val activeRow = selectedRow ?: focused?.row
    Box(modifier = modifier) {
        centers.forEachIndexed { rowIndex, centerY ->
            if (rowIndex != activeRow) return@forEachIndexed
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .zIndex(2f)
                    .offset { IntOffset(0, (centerY - handleHeightPx / 2f).roundToInt()) }
            ) {
                RowHandle(
                    rowIndex = rowIndex,
                    selected = selectedRow == rowIndex,
                    primary = primary,
                    showMenu = showRowMenu && selectedRow == rowIndex,
                    onShowMenuChange = onShowRowMenuChange,
                    onSelect = { onRowSelect(rowIndex) },
                    onInsertAbove = { onInsertRowAbove(rowIndex) },
                    onInsertBelow = { onInsertRowBelow(rowIndex) },
                    onDeleteRow = { onDeleteRow(rowIndex) },
                    // La fila 0 también se borra: es una fila más, y la que pase a ser la
                    // primera hereda el papel de cabecera (RF-49).
                    canDeleteRow = canDeleteRow,
                    rowHeightPx = metrics.rowHeights.getOrElse(rowIndex) { 0f },
                    rowCount = centers.size,
                    onDragRowState = onDragRowState,
                    onCommitRow = onCommitRow,
                    onCancelRow = onCancelRow
                )
            }
        }

        // Manejador de la columna enfocada, centrado sobre el borde superior de la
        // columna. Aparece al enfocar una celda de la primera fila, con la opción de
        // cabecera activada o no: es la fila que hace de cabecera de columnas, exista
        // o no el pintado (RF-28 enmendado). Superpuesto, así que no empuja la fila.
        val col = focused?.col
        if (focused?.row == 0 && col != null) {
            val leftPx = columnHandleLeft(metrics, col, handleWidthPx)
            val colHeightPx = with(LocalDensity.current) { ColumnHandleHeight.toPx() }
            if (leftPx != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .zIndex(3f)
                        .offset {
                            IntOffset(leftPx.roundToInt(), (-colHeightPx / 2f).roundToInt())
                        }
                ) {
                    HeaderColumnHandle(
                        col = col,
                        primary = primary,
                        showMenu = showColMenu,
                        onShowMenuChange = onShowColMenuChange,
                        onInsertLeft = { onInsertColumnLeft(col) },
                        onInsertRight = { onInsertColumnRight(col) },
                        onDeleteColumn = { onDeleteColumn(col) },
                        canDeleteColumn = canDeleteColumn,
                        colUnitPx = metrics.columnWidths.getOrElse(col) { 1f },
                        columnCount = metrics.columnWidths.size,
                        onDragCol = onDragColState,
                        onCommitCol = onCommitCol,
                        onCancelCol = onCancelCol
                    )
                }
            }
        }
    }
}

@Composable
private fun RowHandle(
    rowIndex: Int,
    selected: Boolean,
    primary: Color,
    showMenu: Boolean,
    onShowMenuChange: (Boolean) -> Unit,
    onSelect: () -> Unit,
    onInsertAbove: () -> Unit,
    onInsertBelow: () -> Unit,
    onDeleteRow: () -> Unit,
    canDeleteRow: Boolean,
    rowHeightPx: Float,
    rowCount: Int,
    onDragRowState: (Int, Int) -> Unit,
    onCommitRow: () -> Unit,
    onCancelRow: () -> Unit
) {
    Box(contentAlignment = Alignment.Center) {
        NotionSelectionHandle(
            isVertical = true,
            primary = primary,
            contentDescription = stringResource(R.string.table_row_actions),
            selected = selected,
            onClick = {
                onSelect()
                onShowMenuChange(true)
            },
            onDragModifier = Modifier.pointerInput(rowIndex) {
                var start = rowIndex
                var acc = 0f
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        start = rowIndex
                        acc = 0f
                        onDragRowState(start, start)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        acc += dragAmount.y
                        val target = (start + (acc / rowHeightPx.coerceAtLeast(1f)).roundToInt()).coerceIn(0, rowCount - 1)
                        onDragRowState(start, target)
                    },
                    onDragEnd = { onCommitRow() },
                    onDragCancel = { onCancelRow() }
                )
            }
        )
        DropdownMenu(expanded = showMenu, onDismissRequest = { onShowMenuChange(false) }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.insert_row_above)) }, onClick = { onShowMenuChange(false); onInsertAbove() })
            DropdownMenuItem(text = { Text(stringResource(R.string.insert_row_below)) }, onClick = { onShowMenuChange(false); onInsertBelow() })
            DropdownMenuItem(text = { Text(stringResource(R.string.delete_row)) }, onClick = { onShowMenuChange(false); onDeleteRow() }, enabled = canDeleteRow)
        }
    }
}

// ============ 4. CELDA (KISS) ============
/**
 * Una celda de la tabla, la de la fila 0 incluida.
 *
 * Lo único que distingue una celda de cabecera de una de datos es [isHeader]: la negrita y el
 * tinte. Altura mínima, relleno, una sola línea y reconocimiento de enlaces son los mismos en las
 * dos, que es justo lo que se buscaba al unificar la fila de cabecera (spec 008).
 *
 * [resizeBar] es la barra que se arrastra para cambiar el ancho de una columna. Va fuera y la pinta
 * quien la necesita, porque solo la primera fila la lleva.
 */
@Composable
private fun TableCell(
    cell: String,
    isHeader: Boolean,
    primary: Color,
    onValueChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    onMoveFocus: (Int, Int) -> Unit,
    focusRequester: FocusRequester,
    resizeBar: (@Composable BoxScope.() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val link = isLink(cell)
    var text by remember { mutableStateOf(TextFieldValue(cell)) }
    // Recarga externa: se conserva el rango para no teletransportar el cursor
    // al principio ni colapsar la selección.
    LaunchedEffect(cell) {
        if (text.text != cell) text = TextFieldValue(cell, text.selection.clampedTo(cell.length))
    }
    // La celda solo lleva la línea de la rejilla. El estado activo no la pinta: lo
    // pinta el contorno de la capa superpuesta, y así una fila o columna resaltada se
    // lee como una región y no como una tira de cajas.
    var base = modifier
        .border(width = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
        .background(
            if (isHeader) MaterialTheme.colorScheme.primaryContainer.copy(alpha = TableHeaderAlpha)
            else Color.Transparent
        )
    if (link) {
        base = base.shadow(2.dp, RoundedCornerShape(4.dp))
    }
    Box(
        base.padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value = text,
            onValueChange = { text = it; onValueChange(it.text) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { state -> onFocusChange(state.isFocused) }
                .onPreviewKeyEvent { event ->
                    val dir = cellMoveFor(event, text.selection, text.text.length)
                        ?: return@onPreviewKeyEvent false
                    onMoveFocus(dir.first, dir.second)
                    true
                },
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = if (link) primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = when {
                    isHeader -> FontWeight.Bold
                    link -> FontWeight.Medium
                    else -> FontWeight.Normal
                }
            )
        )
        resizeBar?.invoke(this@Box)
    }
}

@Composable
private fun ColumnResizeBar(modifier: Modifier = Modifier, primary: Color, active: Boolean = false) {
    Box(
        modifier = modifier.background(
            if (active) primary.copy(alpha = 0.6f) else Color.Transparent
        )
    )
}

// ============ 3. MANEJADOR ESTILO NOTION (DRY) ============
//
// Los tres tiradores de la tabla (fila, columna y arrastre de la barra) salen de aquí.
// Es un botón de icono de 48×48 dp de verdad, sin trucos de tamaño:
//
// - **Sin `Modifier.size`.** En material3 1.5.0-alpha28 `minimumInteractiveComponentSize()`
//   mide `max(hijo, 48.dp)` desde dentro del `IconButton` e ignora los constraints de
//   entrada, así que un `size` externo solo deformaría el contenido.
// - **Sin anular el mínimo interactivo del tema.** Proveer el `CompositionLocal` que lo
//   deja a cero es justo el rodeo que RF-42 prohíbe: el tirador se quedaría por debajo
//   del objetivo táctil que el propio tema impone.
//
// El icono `DragHandle` es horizontal, así que los tiradores verticales giran 90°.
// `selected` ya no pinta un borde: tiñe el icono, que es como Material señala el estado.
@Composable
fun NotionSelectionHandle(
    isVertical: Boolean,
    primary: Color,
    contentDescription: String,
    selected: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onDragModifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.then(onDragModifier),
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Icon(
            imageVector = Icons.Filled.DragHandle,
            contentDescription = contentDescription,
            modifier = if (isVertical) Modifier.rotate(90f) else Modifier
        )
    }
}

// MD3 Expressive. Se ubica en una franja propia sobre el Card (ver
// EditableTableBlock), nunca superpuesta a una celda.
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FloatingToolbar(
    modifier: Modifier = Modifier,
    onOpenMoreMenu: () -> Unit,
    onDragTableStart: () -> Unit,
    onDragTableBy: (Float) -> Unit,
    onDragTableEnd: () -> Unit,
    onDragTableCancel: () -> Unit
) {
    // Sin `shape` explícito: `FloatingToolbarDefaults.containerShape` es internal
    // en material3 1.5.0-alpha28 y el propio composable ya aplica la forma
    // expresiva por defecto.
    HorizontalFloatingToolbar(
        expanded = true,
        modifier = modifier,
        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
        scrollBehavior = null
    ) {
        Box(
            modifier = Modifier.pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragTableStart() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDragTableBy(dragAmount.y)
                    },
                    onDragEnd = { onDragTableEnd() },
                    onDragCancel = { onDragTableCancel() }
                )
            },
            contentAlignment = Alignment.Center
        ) {
            NotionSelectionHandle(
                isVertical = true,
                primary = MaterialTheme.colorScheme.primary,
                contentDescription = stringResource(R.string.table_move_handle),
                onClick = {}
            )
        }
        IconButton(onClick = onOpenMoreMenu) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.more),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun TableBlockOptionsSheet(
    state: TableBlockState,
    canDeleteColumn: Boolean,
    canDeleteRow: Boolean,
    onInsertBelow: () -> Unit,
    onDuplicate: () -> Unit,
    onMoveTo: () -> Unit,
    onDeleteTable: () -> Unit,
    onSelectBg: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    // Sin los "+" al pie, esta hoja es la única vía para crear o quitar filas y
    // columnas: activeRow/activeCol nunca son null y las acciones no salen muertas.
    val targetRow = state.activeRow
    val targetCol = state.activeCol
    val rowActions = listOf(
        BlockSheetAction(
            label = context.getString(R.string.block_insert_row_above),
            icon = Icons.Default.Add,
            dividerBefore = true,
            onClick = { state.insertRowAbove(targetRow) }
        ),
        BlockSheetAction(
            label = context.getString(R.string.block_insert_row_below),
            icon = Icons.Default.Add,
            onClick = { state.insertRowBelow(targetRow) }
        ),
        BlockSheetAction(
            label = context.getString(R.string.block_delete_row),
            icon = Icons.Default.Delete,
            enabled = canDeleteRow,
            onClick = { state.deleteRow(targetRow) }
        )
    )
    val columnActions = listOf(
        BlockSheetAction(
            label = context.getString(R.string.block_insert_column_before),
            icon = Icons.Default.Add,
            dividerBefore = true,
            onClick = { state.insertColumnLeft(targetCol) }
        ),
        BlockSheetAction(
            label = context.getString(R.string.block_insert_column_after),
            icon = Icons.Default.Add,
            onClick = { state.insertColumnRight(targetCol) }
        ),
        BlockSheetAction(
            label = context.getString(R.string.block_delete_column),
            icon = Icons.Default.Delete,
            enabled = canDeleteColumn,
            onClick = { state.deleteColumn(targetCol) }
        )
    )

    BlockOptionsSheet(
        title = context.getString(R.string.block_table),
        onDismiss = onDismiss,
        actions = listOf(
            BlockSheetAction(
                label = context.getString(R.string.block_fit_to_width),
                icon = Icons.Default.FitScreen,
                onClick = { state.fitToScreen() }
            ),
            BlockSheetAction(
                label = context.getString(R.string.block_fit_to_content),
                icon = Icons.Default.AspectRatio,
                onClick = { state.fitToContent() }
            ),
            BlockSheetAction(
                label = context.getString(R.string.block_header_row),
                icon = Icons.Default.ViewDay,
                toggle = state.showHeader,
                dividerBefore = true,
                onClick = { state.toggleHeader() }
            ),
            BlockSheetAction(
                label = context.getString(R.string.block_header_column),
                icon = Icons.Default.ViewColumn,
                toggle = state.showHeaderColumn,
                onClick = { state.toggleHeaderColumn() }
            )
        ) + rowActions + columnActions + listOf(
            BlockSheetAction(
                label = context.getString(R.string.block_insert_below),
                icon = Icons.Default.ArrowDownward,
                dividerBefore = true,
                onClick = onInsertBelow
            ),
            BlockSheetAction(
                label = context.getString(R.string.block_duplicate),
                icon = Icons.Default.ContentCopy,
                onClick = onDuplicate
            ),
            BlockSheetAction(
                label = context.getString(R.string.block_move_to),
                icon = Icons.AutoMirrored.Filled.DriveFileMove,
                onClick = onMoveTo
            ),
            BlockSheetAction(
                label = context.getString(R.string.btn_delete),
                icon = Icons.Default.Delete,
                danger = true,
                onClick = onDeleteTable
            )
        )
    ) {
        BlockOptionsSheetDivider()
        Text(
            text = context.getString(R.string.block_table_color),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 4.dp)
        )
        BackgroundColorRow(onSelect = onSelectBg)
    }
}

@Composable
private fun BackgroundColorRow(onSelect: (String?) -> Unit) {
    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
        BG_OPTIONS.forEach { hex ->
            val swatch = parseHex(hex) ?: MaterialTheme.colorScheme.surface
            Box(
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(22.dp)
                    .background(swatch, CircleShape)
                    .then(
                        if (hex == null) Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        else Modifier
                    )
                    .clickable { onSelect(hex) }
            )
        }
    }
}

private val PreviewTableData = TableData(
    rows = listOf(
        listOf("Encabezado 1", "Encabezado 2"),
        listOf("Dato 1", "Dato 2"),
        listOf("Dato 3", "Dato 4")
    ),
    columnWeights = listOf(1f, 1f),
    bgColorHex = null,
    showHeader = true
)

@Preview(showBackground = true)
@Composable
private fun EditableTableBlockPreview() {
    MaterialTheme {
        EditableTableBlock(
            tableData = PreviewTableData,
            onChange = {}
        )
    }
}

// La tabla con la barra flotante solo se ve con el foco en una celda, que un
// Preview no puede activar: esta variante la fuerza para poder revisarla.
@Preview(showBackground = true)
@Composable
private fun EditableTableBlockGutterPreview() {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(16.dp)) {
                // Replica la franja de la barra: así se comprueba que no invade
                // la tabla.
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        tonalElevation = 3.dp,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            NotionSelectionHandle(
                                isVertical = true,
                                primary = MaterialTheme.colorScheme.primary,
                                contentDescription = stringResource(R.string.table_move_handle),
                                onClick = {}
                            )
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                EditableTableBlock(
                    tableData = PreviewTableData,
                    onChange = {}
                )
            }
        }
    }
}
