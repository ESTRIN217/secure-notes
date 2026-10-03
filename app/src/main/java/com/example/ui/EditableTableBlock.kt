package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.data.model.TableData
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

// Ancho del gutter de los handles de fila. No se resta al ancho de columnas: la
// capa del gutter se superpone al borde izquierdo del Card.
private val TableGutterWidth = 32.dp

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
    var headers by mutableStateOf(initialData.headers.toMutableList())
    var rows by mutableStateOf(initialData.rows.map { it.toMutableList() }.toMutableList())
    var weights by mutableStateOf(initialData.normalizedWeights().toMutableList())
    var bgColorHex by mutableStateOf(initialData.bgColorHex?.hex())
    var showHeader by mutableStateOf(initialData.showHeader)
    var showHeaderColumn by mutableStateOf(initialData.showHeaderColumn)
    private var focusedCell by mutableStateOf<FocusedCell?>(null)
    // El foco se rastrea a nivel de tabla (no de celda): al pasar de una celda a
    // otra, el blur de la anterior y el focus de la nueva llegan en orden
    // indeterminado, y limpiar la celda en el blur borraba el estado de golpe.
    private var tableFocused by mutableStateOf(false)
    var selectedRow by mutableStateOf<Int?>(null)
    var showRowMenu by mutableStateOf(false)
    var showColMenu by mutableStateOf(false)
    var dragRow by mutableStateOf<Int?>(null)
    var dragRowTarget by mutableStateOf<Int?>(null)
    var dragCol by mutableStateOf<Int?>(null)
    var dragColTarget by mutableStateOf<Int?>(null)
    var rowHeightPx by mutableStateOf(48f)
    var headerHeightPx by mutableStateOf(44f)

    val focusRequesters = mutableMapOf<Pair<Int, Int>, FocusRequester>()

    private var changeListener: (TableData) -> Unit = {}

    fun registerOnChange(listener: (TableData) -> Unit) {
        changeListener = listener
    }

    fun reloadFrom(data: TableData) {
        val current = toTableData()
        if (current.headers == data.headers &&
            current.rows == data.rows &&
            current.columnWeights == data.columnWeights &&
            current.bgColorHex == data.bgColorHex &&
            current.showHeader == data.showHeader &&
            current.showHeaderColumn == data.showHeaderColumn
        ) return
        headers = data.headers.toMutableList()
        rows = data.rows.map { it.toMutableList() }.toMutableList()
        weights = data.normalizedWeights().toMutableList()
        bgColorHex = data.bgColorHex?.hex()
        showHeader = data.showHeader
        showHeaderColumn = data.showHeaderColumn
    }

    // --- Derivados (solo lectura) ---
    val columnCount: Int
        get() = headers.size.coerceAtLeast(rows.maxOfOrNull { it.size } ?: 0)
    val focusedRow: Int?
        get() = focusedCell?.takeIf { it.row >= 0 }?.row
    val focusedCol: Int?
        get() = focusedCell?.col
    val hasFocus: Boolean
        get() = tableFocused
    // Con los "+" fuera, la hoja de opciones es la única vía para añadir o
    // quitar filas y columnas: siempre cae en un destino válido.
    val activeCol: Int
        get() = (focusedCol ?: 0).coerceIn(0, (columnCount - 1).coerceAtLeast(0))
    val activeRow: Int
        get() = (selectedRow ?: focusedRow ?: 0).coerceIn(0, (rows.size - 1).coerceAtLeast(0))

    fun cellWeight(col: Int): Float = weights.getOrElse(col) { 1f }.coerceAtLeast(0.1f)

    private fun toTableData() = TableData(
        headers = headers.toList(),
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
    fun setTableFocus(focused: Boolean) {
        tableFocused = focused
        if (!focused) selectedRow = null
    }

    fun setHeader(col: Int, value: String) {
        if (col !in headers.indices) return
        headers[col] = value
        notifyChange()
    }

    // focusedCell es memoria de "última celda tocada": nunca se anula al perder
    // el foco, solo alimenta las acciones contextuales de la hoja.
    fun selectHeaderCell(col: Int, focused: Boolean) {
        if (focused) focusedCell = FocusedCell(-1, col)
    }

    fun selectCell(rowIndex: Int, col: Int, focused: Boolean) {
        if (focused) focusedCell = FocusedCell(rowIndex, col)
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
        rows.add(rowIndex.coerceIn(0, rows.size), MutableList(columnCount) { "" })
        selectedRow = rowIndex.coerceIn(0, rows.size - 1)
        notifyChange()
    }

    fun insertRowBelow(rowIndex: Int) {
        rows.add((rowIndex + 1).coerceIn(0, rows.size), MutableList(columnCount) { "" })
        selectedRow = (rowIndex + 1).coerceIn(0, rows.size - 1)
        notifyChange()
    }

    fun deleteRow(rowIndex: Int) {
        if (rows.size > 1) {
            rows.removeAt(rowIndex.coerceIn(0, rows.lastIndex))
            selectedRow = null
            notifyChange()
        }
    }

    fun insertColumnLeft(colIndex: Int) {
        headers.add(colIndex.coerceIn(0, headers.size), "")
        rows.forEach { it.add(colIndex.coerceIn(0, it.size), "") }
        if (colIndex in weights.indices) weights.add(colIndex, 1f) else weights.add(1f)
        selectedRow = null
        notifyChange()
    }

    fun insertColumnRight(colIndex: Int) {
        headers.add((colIndex + 1).coerceIn(0, headers.size), "")
        rows.forEach { it.add((colIndex + 1).coerceIn(0, it.size), "") }
        weights.add((colIndex + 1).coerceIn(0, weights.size), 1f)
        selectedRow = null
        notifyChange()
    }

    fun deleteColumn(colIndex: Int) {
        if (columnCount > 1) {
            if (colIndex < headers.size) headers.removeAt(colIndex)
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
        selectedRow = to
        notifyChange()
    }

    fun moveColumn(from: Int, to: Int) {
        if (from == to || headers.isEmpty()) return
        val header = headers.removeAt(from)
        headers.add(to, header)
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
            val header = if (showHeader) headers.getOrNull(col)?.length ?: 0 else 0
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
        val cur = focusedCell ?: return
        if (columnCount == 0) return
        selectedRow = null
        val newRow = when {
            cur.row < 0 -> if (dRow < 0) -1 else 0
            else -> (cur.row + dRow).coerceIn(0, rows.lastIndex)
        }
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
        val availableWidthForCols = maxWidth

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

        // onFocusChanged en el contenedor: Compose notifica al ancestro cuando
        // cualquier celda gana el foco, así el estado de tabla no depende del
        // orden en que llegan el blur de una celda y el focus de la siguiente.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { state.setTableFocus(it.isFocused) }
        ) {
            // La barra vive en su propia franja, fuera del Card: nunca tapa
            // una celda ni roba ancho de columnas.
            AnimatedVisibility(
                visible = state.hasFocus,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                // AnimatedVisibility no es un ColumnScope: el anclaje va en un Box.
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    FloatingToolbar(
                        modifier = Modifier.padding(vertical = 4.dp),
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
                            .padding(start = TableGutterWidth)
                            .background(tableBg)
                            .horizontalScroll(rememberScrollState())
                    ) {
                        if (state.showHeader) {
                            TableHeaderRow(
                                headers = state.headers,
                                columnCount = state.columnCount,
                                getColumnWidth = getColumnWidth,
                                primary = primary,
                                focusedCol = state.focusedCol,
                                showColMenu = state.showColMenu,
                                onShowColMenuChange = { state.showColMenu = it },
                                isDropCol = state.dragCol != null && state.dragCol != state.dragColTarget && state.dragColTarget != null,
                                colUnitPx = colUnitPx,
                                onHeaderValueChange = { col, value -> state.setHeader(col, value) },
                                onHeaderSelect = { col, focused ->
                                    state.selectHeaderCell(col, focused)
                                    onFocusChange(focused)
                                },
                                onInsertLeft = state::insertColumnLeft,
                                onInsertRight = state::insertColumnRight,
                                onDeleteColumn = state::deleteColumn,
                                canDeleteColumn = state.columnCount > 1,
                                onResizeColumn = { col, delta -> state.adjustColumnWidth(col, delta, colUnitPx) },
                                onDragColState = { s, t -> state.dragCol = s; state.dragColTarget = t },
                                onCommitCol = { state.commitColumnDrag() },
                                onCancelCol = { state.dragCol = null; state.dragColTarget = null },
                                focusRequesters = state.focusRequesters,
                                onMoveFocus = state::moveFocus,
                                onHeaderHeight = { state.headerHeightPx = it }
                            )
                        }

                        state.rows.forEachIndexed { rowIndex, row ->
                            key(rowIndex) {
                                TableRow(
                                    row = row,
                                    rowIndex = rowIndex,
                                    columnCount = state.columnCount,
                                    primary = primary,
                                    showHeaderColumn = state.showHeaderColumn,
                                    focusedRow = state.focusedRow,
                                    focusedCol = state.focusedCol,
                                    selectedRow = state.selectedRow,
                                    isDropRow = state.dragRow != null && state.dragRow != rowIndex && state.dragRowTarget == rowIndex,
                                    getColumnWidth = getColumnWidth,
                                    onCellValueChange = { col, value -> state.setCellValue(rowIndex, col, value) },
                                    onCellSelect = { col, focused ->
                                        state.selectCell(rowIndex, col, focused)
                                        onFocusChange(focused)
                                    },
                                    onRowHeight = { state.rowHeightPx = it },
                                    focusRequesters = state.focusRequesters,
                                    onMoveFocus = state::moveFocus
                                )
                            }
                        }
                    }

                    // Capa del gutter: los handles de fila se dibujan encima del
                    // borde izquierdo, solo con la tabla activa.
                    if (state.hasFocus) {
                        RowGutter(
                            rowCount = state.rows.size,
                            headerHeight = if (state.showHeader) {
                                with(LocalDensity.current) { state.headerHeightPx.toDp() }
                            } else {
                                0.dp
                            },
                            rowHeightPx = state.rowHeightPx,
                            selectedRow = state.selectedRow,
                            primary = primary,
                            showRowMenu = state.showRowMenu,
                            onShowRowMenuChange = { state.showRowMenu = it },
                            onRowSelect = state::selectRow,
                            onInsertAbove = state::insertRowAbove,
                            onInsertBelow = state::insertRowBelow,
                            onDeleteRow = state::deleteRow,
                            canDeleteRow = state.rows.size > 1,
                            onDragRowState = { s, t -> state.dragRow = s; state.dragRowTarget = t },
                            onCommitRow = { state.commitRowDrag() },
                            onCancelRow = { state.dragRow = null; state.dragRowTarget = null }
                        )
                    }
                }
            }

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
private fun TableHeaderRow(
    headers: List<String>,
    columnCount: Int,
    getColumnWidth: (Int) -> Dp,
    primary: Color,
    focusedCol: Int?,
    showColMenu: Boolean,
    onShowColMenuChange: (Boolean) -> Unit,
    isDropCol: Boolean,
    colUnitPx: Float,
    onHeaderValueChange: (Int, String) -> Unit,
    onHeaderSelect: (Int, Boolean) -> Unit,
    onInsertLeft: (Int) -> Unit,
    onInsertRight: (Int) -> Unit,
    onDeleteColumn: (Int) -> Unit,
    canDeleteColumn: Boolean,
    onResizeColumn: (Int, Float) -> Unit,
    onDragColState: (Int, Int) -> Unit,
    onCommitCol: () -> Unit,
    onCancelCol: () -> Unit,
    focusRequesters: MutableMap<Pair<Int, Int>, FocusRequester>,
    onMoveFocus: (Int, Int) -> Unit,
    onHeaderHeight: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                when {
                    isDropCol -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                    headers.isNotEmpty() -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = TableHeaderAlpha)
                    else -> Color.Transparent
                }
            )
            // La cabecera crece al mostrar el handle de columna: se mide en vez
            // de fijar 44.dp para que el gutter de filas no quede desalineado.
            .onGloballyPositioned { onHeaderHeight(it.size.height.toFloat()) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (col in 0 until columnCount) {
            key(col) {
                val header = headers.getOrElse(col) { "" }
                val selected = focusedCol == col
                val focusRequester = remember { FocusRequester() }
                // El registro va en un efecto, no en la composición: así el mapa
                // se poda solo cuando una celda sale del árbol.
                DisposableEffect(focusRequester) {
                    focusRequesters[Pair(-1, col)] = focusRequester
                    onDispose { focusRequesters.remove(Pair(-1, col)) }
                }
                val colWidth = getColumnWidth(col)
                Box(
                    modifier = Modifier
                        .width(colWidth)
                        .zIndex(if (selected) 1f else 0f)
                        .border(
                            width = if (selected) 1.5.dp else 0.5.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = TableHighlightBgAlpha)
                            else Color.Transparent
                        )
                ) {
                    Column {
                        if (selected) {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                HeaderColumnHandle(
                                    col = col,
                                    primary = primary,
                                    showMenu = showColMenu,
                                    onShowMenuChange = onShowColMenuChange,
                                    onInsertLeft = { onInsertLeft(col) },
                                    onInsertRight = { onInsertRight(col) },
                                    onDeleteColumn = { onDeleteColumn(col) },
                                    canDeleteColumn = canDeleteColumn,
                                    colUnitPx = colUnitPx,
                                    columnCount = columnCount,
                                    onDragCol = onDragColState,
                                    onCommitCol = onCommitCol,
                                    onCancelCol = onCancelCol
                                )
                            }
                        }
                        var headerText by remember { mutableStateOf(TextFieldValue(header)) }
                        // Recarga externa (p. ej. deshacer o edición con IA): se conserva
                        // el rango para no teletransportar el cursor al principio ni
                        // colapsar la selección.
                        LaunchedEffect(header) {
                            if (headerText.text != header) {
                                headerText = TextFieldValue(header, headerText.selection.clampedTo(header.length))
                            }
                        }
                        BasicTextField(
                            value = headerText,
                            onValueChange = { headerText = it; onHeaderValueChange(col, it.text) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .focusRequester(focusRequester)
                                .onFocusChanged { state -> onHeaderSelect(col, state.isFocused) }
                                .onPreviewKeyEvent { event ->
                                    val dir = cellMoveFor(event, headerText.selection, headerText.text.length)
                                        ?: return@onPreviewKeyEvent false
                                    onMoveFocus(dir.first, dir.second)
                                    true
                                },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            singleLine = true
                        )
                    }
                    if (col < columnCount - 1) {
                        ColumnResizeBar(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .width(6.dp)
                                .fillMaxHeight()
                                .pointerInput(col) {
                                    detectHorizontalDragGestures { _, dragAmount -> onResizeColumn(col, dragAmount) }
                                },
                            primary = primary,
                            active = selected
                        )
                    }
                }
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

@Composable
private fun TableRow(
    row: List<String>,
    rowIndex: Int,
    columnCount: Int,
    primary: Color,
    showHeaderColumn: Boolean,
    focusedRow: Int?,
    focusedCol: Int?,
    selectedRow: Int?,
    isDropRow: Boolean,
    getColumnWidth: (Int) -> Dp,
    onCellValueChange: (Int, String) -> Unit,
    onCellSelect: (Int, Boolean) -> Unit,
    onRowHeight: (Float) -> Unit,
    focusRequesters: MutableMap<Pair<Int, Int>, FocusRequester>,
    onMoveFocus: (Int, Int) -> Unit
) {
    val isActiveRow = selectedRow == rowIndex
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                when {
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
                val cellFocused = focusedRow == rowIndex && focusedCol == col
                val inActiveCol = focusedCol == col
                val focusRequester = remember { FocusRequester() }
                DisposableEffect(focusRequester) {
                    focusRequesters[Pair(rowIndex, col)] = focusRequester
                    onDispose { focusRequesters.remove(Pair(rowIndex, col)) }
                }
                TableCell(
                    cell = cell,
                    isFocused = cellFocused,
                    inActiveCol = inActiveCol,
                    isHeaderCol = showHeaderColumn && col == 0,
                    primary = primary,
                    onValueChange = { onCellValueChange(col, it) },
                    onFocusChange = { onCellSelect(col, it) },
                    onMoveFocus = onMoveFocus,
                    focusRequester = focusRequester,
                    modifier = Modifier.width(getColumnWidth(col))
                )
            }
        }
    }
}

// Capa superpuesta del gutter izquierdo. Va aparte de TableRow porque los handles
// cuelgan del borde del Card y no deben ocupar ancho de columna ni alterar el
// alto de la fila que se mide para el arrastre.
@Composable
private fun RowGutter(
    rowCount: Int,
    headerHeight: Dp,
    rowHeightPx: Float,
    selectedRow: Int?,
    primary: Color,
    showRowMenu: Boolean,
    onShowRowMenuChange: (Boolean) -> Unit,
    onRowSelect: (Int) -> Unit,
    onInsertAbove: (Int) -> Unit,
    onInsertBelow: (Int) -> Unit,
    onDeleteRow: (Int) -> Unit,
    canDeleteRow: Boolean,
    onDragRowState: (Int, Int) -> Unit,
    onCommitRow: () -> Unit,
    onCancelRow: () -> Unit
) {
    val rowHeightDp = with(LocalDensity.current) { rowHeightPx.toDp() }
    Column(modifier = Modifier.width(TableGutterWidth)) {
        Spacer(Modifier.height(headerHeight))
        repeat(rowCount) { rowIndex ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeightDp),
                contentAlignment = Alignment.Center
            ) {
                RowHandle(
                    rowIndex = rowIndex,
                    selected = selectedRow == rowIndex,
                    primary = primary,
                    showMenu = showRowMenu && selectedRow == rowIndex,
                    onShowMenuChange = onShowRowMenuChange,
                    onSelect = { onRowSelect(rowIndex) },
                    onInsertAbove = { onInsertAbove(rowIndex) },
                    onInsertBelow = { onInsertBelow(rowIndex) },
                    onDeleteRow = { onDeleteRow(rowIndex) },
                    canDeleteRow = canDeleteRow,
                    rowHeightPx = rowHeightPx,
                    rowCount = rowCount,
                    onDragRowState = onDragRowState,
                    onCommitRow = onCommitRow,
                    onCancelRow = onCancelRow
                )
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
@Composable
private fun TableCell(
    cell: String,
    isFocused: Boolean,
    inActiveCol: Boolean,
    isHeaderCol: Boolean,
    primary: Color,
    onValueChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    onMoveFocus: (Int, Int) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val isHighlighted = isFocused || inActiveCol
    val link = isLink(cell)
    var text by remember { mutableStateOf(TextFieldValue(cell)) }
    // Recarga externa: se conserva el rango para no teletransportar el cursor
    // al principio ni colapsar la selección.
    LaunchedEffect(cell) {
        if (text.text != cell) text = TextFieldValue(cell, text.selection.clampedTo(cell.length))
    }
    var base = modifier
        .zIndex(if (isHighlighted) 1f else 0f)
        .border(
            width = if (isHighlighted) 1.5.dp else 0.5.dp,
            color = if (isHighlighted) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant
        )
        .background(
            when {
                isHeaderCol -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = TableHeaderAlpha)
                isHighlighted -> MaterialTheme.colorScheme.primary.copy(alpha = TableHighlightBgAlpha)
                else -> Color.Transparent
            }
        )
    if (link) {
        base = base.shadow(2.dp, RoundedCornerShape(4.dp))
    }
    Box(
        base.heightIn(min = 44.dp).padding(horizontal = 12.dp, vertical = 4.dp),
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
                    isHeaderCol -> FontWeight.Bold
                    link -> FontWeight.Medium
                    else -> FontWeight.Normal
                }
            ),
            singleLine = true
        )
    }
}

@Composable
private fun ColumnResizeBar(modifier: Modifier = Modifier, primary: Color, active: Boolean) {
    Box(
        modifier = modifier.background(
            if (active) primary.copy(alpha = 0.6f) else Color.Transparent
        )
    )
}

// ============ 3. MANEJADOR ESTILO NOTION (DRY) ============
@Composable
fun NotionSelectionHandle(
    isVertical: Boolean,
    primary: Color,
    selected: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onDragModifier: Modifier = Modifier
) {
    val handleText = if (isVertical) "⋮⋮" else "⋯"

    Box(
        modifier = modifier
            .size(if (isVertical) 16.dp else 24.dp, if (isVertical) 24.dp else 16.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(4.dp))
            .border(
                width = 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(4.dp)
            )
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .then(onDragModifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = handleText,
            color = if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = if (isVertical) 10.sp else 12.sp,
            lineHeight = 10.sp,
            modifier = Modifier.offset(y = if (isVertical) (-2).dp else 0.dp)
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
                onClick = {}
            )
        }
        FilledTonalIconButton(onClick = onOpenMoreMenu) {
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
    headers = listOf("Encabezado 1", "Encabezado 2"),
    rows = listOf(
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