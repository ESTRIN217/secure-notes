package com.example.ui

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.model.TableData
import kotlin.math.roundToInt

private data class FocusedCell(val row: Int, val col: Int)

private data class BgOption(val hex: String?, val label: String)

private val BG_OPTIONS = listOf(
    BgOption(null, "None"),
    BgOption("FFF8E1", "Cream"),
    BgOption("E3F2FD", "Blue"),
    BgOption("E8F5E9", "Green"),
    BgOption("FCE4EC", "Pink"),
    BgOption("FFF3E0", "Orange"),
    BgOption("EDE7F6", "Purple")
)

private val TableBorderIdle = Color(0xFFE5E7EB)
private val TableBorderActive = Color(0xFF2563EB)
private val TableHighlightBg = Color(0x142563EB)

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

private fun String.hex(): String = trim().removePrefix("#").toString()


private fun keyMoveDirection(key: Key): Pair<Int, Int>? = when (key) {
    Key.DirectionDown, Key.Enter -> Pair(1, 0)
    Key.DirectionUp -> Pair(-1, 0)
    Key.DirectionRight, Key.Tab -> Pair(0, 1)
    Key.DirectionLeft -> Pair(0, -1)
    else -> null
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
    private var focusedCell by mutableStateOf<FocusedCell?>(null)
    var selectedRow by mutableStateOf<Int?>(null)
    var showRowMenu by mutableStateOf(false)
    var showColMenu by mutableStateOf(false)
    var dragRow by mutableStateOf<Int?>(null)
    var dragRowTarget by mutableStateOf<Int?>(null)
    var dragCol by mutableStateOf<Int?>(null)
    var dragColTarget by mutableStateOf<Int?>(null)
    var rowHeightPx by mutableStateOf(48f)

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
            current.showHeader == data.showHeader
        ) return
        headers = data.headers.toMutableList()
        rows = data.rows.map { it.toMutableList() }.toMutableList()
        weights = data.normalizedWeights().toMutableList()
        bgColorHex = data.bgColorHex?.hex()
        showHeader = data.showHeader
    }

    // --- Derivados (solo lectura) ---
    val columnCount: Int
        get() = headers.size.coerceAtLeast(rows.maxOfOrNull { it.size } ?: 0)
    val focusedRow: Int?
        get() = focusedCell?.takeIf { it.row >= 0 }?.row
    val focusedCol: Int?
        get() = focusedCell?.col
    val hasFocus: Boolean
        get() = focusedCell != null
    val activeCol: Int?
        get() = focusedCol
    val activeRow: Int?
        get() = selectedRow ?: focusedRow

    fun cellWeight(col: Int): Float = weights.getOrElse(col) { 1f }.coerceAtLeast(0.1f)

    private fun toTableData() = TableData(
        headers = headers.toList(),
        rows = rows.map { it.toList() },
        columnWeights = weights.toList(),
        bgColorHex = bgColorHex,
        showHeader = showHeader
    )

    private fun notifyChange() {
        changeListener(toTableData())
    }

    // --- Mutadores ---
    fun setHeader(col: Int, value: String) {
        if (col !in headers.indices) return
        headers[col] = value
        notifyChange()
    }

    fun selectHeaderCell(col: Int, focused: Boolean) {
        if (focused) {
            focusedCell = FocusedCell(-1, col)
            selectedRow = null
        } else if (focusedCell == FocusedCell(-1, col)) {
            focusedCell = null
        }
    }

    fun selectCell(rowIndex: Int, col: Int, focused: Boolean) {
        if (focused) {
            focusedCell = FocusedCell(rowIndex, col)
        } else if (focusedCell == FocusedCell(rowIndex, col)) {
            focusedCell = null
        }
    }

    fun selectRow(rowIndex: Int) {
        selectedRow = rowIndex
        focusedCell = null
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
        focusedCell = null
        notifyChange()
    }

    fun insertRowBelow(rowIndex: Int) {
        rows.add((rowIndex + 1).coerceIn(0, rows.size), MutableList(columnCount) { "" })
        selectedRow = (rowIndex + 1).coerceIn(0, rows.size - 1)
        focusedCell = null
        notifyChange()
    }

    fun deleteRow(rowIndex: Int) {
        if (rows.size > 1) {
            rows.removeAt(rowIndex.coerceIn(0, rows.lastIndex))
            selectedRow = null
            focusedCell = null
            clearFocusRequesters() // Garantiza que no existan referencias corruptas
            notifyChange()
        }
    }

    fun insertColumnLeft(colIndex: Int) {
        headers.add(colIndex.coerceIn(0, headers.size), "")
        rows.forEach { it.add(colIndex.coerceIn(0, it.size), "") }
        if (colIndex in weights.indices) weights.add(colIndex, 1f) else weights.add(1f)
        focusedCell = null
        selectedRow = null
        notifyChange()
    }

    fun insertColumnRight(colIndex: Int) {
        headers.add((colIndex + 1).coerceIn(0, headers.size), "")
        rows.forEach { it.add((colIndex + 1).coerceIn(0, it.size), "") }
        weights.add((colIndex + 1).coerceIn(0, weights.size), 1f)
        focusedCell = null
        selectedRow = null
        notifyChange()
    }

    fun deleteColumn(colIndex: Int) {
        if (columnCount > 1) {
            if (colIndex < headers.size) headers.removeAt(colIndex)
            rows.forEach { if (colIndex < it.size) it.removeAt(colIndex) }
            if (colIndex < weights.size) weights.removeAt(colIndex)
            focusedCell = null
            selectedRow = null
            clearFocusRequesters() // Limpia el mapa para regenerar posiciones
            notifyChange()
        }
    }
    

    private fun clearFocusRequesters() {
        focusRequesters.clear()
    }
    
    fun moveRow(from: Int, to: Int) {
        if (from == to || from !in rows.indices || to !in rows.indices) return
        val item = rows.removeAt(from)
        rows.add(to, item)
        selectedRow = to
        focusedCell = null
        notifyChange()
    }

    fun moveColumn(from: Int, to: Int) {
        if (from == to || headers.isEmpty()) return
        val header = headers.removeAt(from)
        headers.add(to, header)
        rows.forEach { row -> if (from < row.size) { val c = row.removeAt(from); row.add(to, c) } }
        val w = weights.removeAt(from)
        weights.add(to, w)
        focusedCell = null
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

    fun adjustRowWidth(col: Int, delta: Float) {
        if (col !in weights.indices) return
        weights[col] = (cellWeight(col) + delta / 24f / columnCount.coerceAtLeast(1)).coerceIn(0.1f, 6f)
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

    fun setBgColor(hex: String?) {
        bgColorHex = hex?.hex()
        notifyChange()
    }

    fun moveFocus(dRow: Int, dCol: Int) {
        val cur = focusedCell ?: return
        if (columnCount == 0) return
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
    onDragTableStart: () -> Unit = {},
    onDragTableBy: (Float) -> Unit = {},
    onDragTableEnd: () -> Unit = {},
    onDragTableCancel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state = rememberTableBlockState(tableData, onChange)
    val tableBg = parseHex(state.bgColorHex) ?: Color.Transparent
    val outline = MaterialTheme.colorScheme.outlineVariant
    val primary = MaterialTheme.colorScheme.primary

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val handleWidth = 32.dp
        val availableWidthForCols = (maxWidth - handleWidth).coerceAtLeast(0.dp)

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

        Box(modifier = Modifier.fillMaxWidth()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = if (tableBg == Color.Transparent) MaterialTheme.colorScheme.surface else tableBg
                )
            ) {
                Column(
                    modifier = Modifier
                        .background(tableBg)
                        .horizontalScroll(rememberScrollState())
                ) {
                    if (state.showHeader) {
                        TableHeaderRow(
                            headers = state.headers,
                            columnCount = state.columnCount,
                            getColumnWidth = getColumnWidth,
                            outline = outline,
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
                            onMoveFocus = state::moveFocus
                        )
                        }

                    state.rows.forEachIndexed { rowIndex, row ->
                        TableRow(
                            row = row,
                            rowIndex = rowIndex,
                            columnCount = state.columnCount,
                            primary = primary,
                            focusedRow = state.focusedRow,
                            focusedCol = state.focusedCol,
                            selectedRow = state.selectedRow,
                            isDropRow = state.dragRow != null && state.dragRow != rowIndex && state.dragRowTarget == rowIndex,
                            showRowMenu = state.showRowMenu && state.selectedRow == rowIndex,
                            onShowRowMenuChange = { state.showRowMenu = it },
                            getColumnWidth = getColumnWidth,
                            onCellValueChange = { col, value -> state.setCellValue(rowIndex, col, value) },
                            onCellSelect = { col, focused ->
                                state.selectCell(rowIndex, col, focused)
                                onFocusChange(focused)
                            },
                            onRowSelect = { state.selectRow(rowIndex) },
                            onInsertAbove = { state.insertRowAbove(rowIndex) },
                            onInsertBelow = { state.insertRowBelow(rowIndex) },
                            onDeleteRow = { state.deleteRow(rowIndex) },
                            canDeleteRow = state.rows.size > 1,
                            onResize = { col, delta -> state.adjustRowWidth(col, delta) },
                            rowHeightPx = state.rowHeightPx,
                            rowCount = state.rows.size,
                            onDragRowState = { s, t -> state.dragRow = s; state.dragRowTarget = t },
                            onCommitRow = { state.commitRowDrag() },
                            onCancelRow = { state.dragRow = null; state.dragRowTarget = null },
                            onRowHeight = { state.rowHeightPx = it },
                            focusRequesters = state.focusRequesters,
                            onMoveFocus = state::moveFocus
                        )
                    }

                    AddRowFooter(
                        onAddBelow = { state.insertRowBelow(state.rows.lastIndex) },
                        outline = outline,
                        primary = primary
                    )
                }
            }

            if (state.hasFocus) {
                FloatingToolbar(
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp),
                    canDeleteColumn = state.columnCount > 1,
                    canDeleteRow = state.rows.size > 1,
                    hasRow = state.activeRow != null,
                    hasColumn = state.activeCol != null,
                    showHeader = state.showHeader,
                    onToggleHeader = { state.toggleHeader() },
                    onFitToScreen = { state.fitToScreen() },
                    onFitToContent = { state.fitToContent() },
                    onDeleteRow = { state.activeRow?.let { state.deleteRow(it) } },
                    onDeleteColumn = { state.activeCol?.let { state.deleteColumn(it) } },
                    onInsertRowAbove = { state.activeRow?.let { state.insertRowAbove(it) } },
                    onInsertRowBelow = { state.activeRow?.let { state.insertRowBelow(it) } },
                    onInsertColumnBefore = { state.activeCol?.let { state.insertColumnLeft(it) } },
                    onInsertColumnAfter = { state.activeCol?.let { state.insertColumnRight(it) } },
                    onDeleteTable = onDeleteBlock,
                    onSelectBg = { state.setBgColor(it) },
                    onDragTableStart = onDragTableStart,
                    onDragTableBy = onDragTableBy,
                    onDragTableEnd = onDragTableEnd,
                    onDragTableCancel = onDragTableCancel
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
    outline: Color,
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
    onMoveFocus: (Int, Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isDropCol || headers.isNotEmpty())
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDropCol) 0.4f else 0.25f)
                else Color.Transparent
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AddColumnFooter(onClick = { onInsertRight(columnCount - 1) }, outline = outline)

        for (col in 0 until columnCount) {
            val header = headers.getOrElse(col) { "" }
            val selected = focusedCol == col
            val focusRequester = remember { FocusRequester() }
            focusRequesters[Pair(-1, col)] = focusRequester
            val colWidth = getColumnWidth(col)
            Box(
                modifier = Modifier
                    .width(colWidth)
                    .zIndex(if (selected) 1f else 0f)
                    .border(
                        width = if (selected) 1.5.dp else 0.5.dp,
                        color = if (selected) TableBorderActive else TableBorderIdle
                    )
                    .background(
                        if (selected) TableHighlightBg else Color.Transparent
                    )
                    .clickable { onHeaderSelect(col, true) }
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
                    LaunchedEffect(header) {
                        if (headerText.text != header) headerText = TextFieldValue(header)
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
                                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                                val dir = keyMoveDirection(event.key) ?: return@onPreviewKeyEvent false
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
            DropdownMenuItem(text = { Text("Insert column left") }, onClick = { onShowMenuChange(false); onInsertLeft() })
            DropdownMenuItem(text = { Text("Insert column right") }, onClick = { onShowMenuChange(false); onInsertRight() })
            DropdownMenuItem(text = { Text("Delete column") }, onClick = { onShowMenuChange(false); onDeleteColumn() }, enabled = canDeleteColumn)
        }
    }
}

@Composable
private fun TableRow(
    row: List<String>,
    rowIndex: Int,
    columnCount: Int,
    primary: Color,
    focusedRow: Int?,
    focusedCol: Int?,
    selectedRow: Int?,
    isDropRow: Boolean,
    showRowMenu: Boolean,
    onShowRowMenuChange: (Boolean) -> Unit,
    getColumnWidth: (Int) -> Dp,
    onCellValueChange: (Int, String) -> Unit,
    onCellSelect: (Int, Boolean) -> Unit,
    onRowSelect: () -> Unit,
    onInsertAbove: () -> Unit,
    onInsertBelow: () -> Unit,
    onDeleteRow: () -> Unit,
    canDeleteRow: Boolean,
    onResize: (Int, Float) -> Unit,
    rowHeightPx: Float,
    rowCount: Int,
    onDragRowState: (Int, Int) -> Unit,
    onCommitRow: () -> Unit,
    onCancelRow: () -> Unit,
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
                    isDropRow -> primary.copy(alpha = 0.12f)
                    isActiveRow -> primary.copy(alpha = 0.06f)
                    rowIndex % 2 == 1 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    else -> Color.Transparent
                }
            )
            .onGloballyPositioned { onRowHeight(it.size.height.toFloat()) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        RowHandle(
            rowIndex = rowIndex,
            selected = isActiveRow,
            primary = primary,
            showMenu = showRowMenu,
            onShowMenuChange = onShowRowMenuChange,
            onSelect = onRowSelect,
            onInsertAbove = onInsertAbove,
            onInsertBelow = onInsertBelow,
            onDeleteRow = onDeleteRow,
            canDeleteRow = canDeleteRow,
            rowHeightPx = rowHeightPx,
            rowCount = rowCount,
            onDragRowState = onDragRowState,
            onCommitRow = onCommitRow,
            onCancelRow = onCancelRow
        )
        for (col in 0 until columnCount) {
            val cell = row.getOrElse(col) { "" }
            val cellFocused = focusedRow == rowIndex && focusedCol == col
            val inActiveCol = focusedCol == col
            val focusRequester = remember { FocusRequester() }
            focusRequesters[Pair(rowIndex, col)] = focusRequester
            TableCell(
                cell = cell,
                isFocused = cellFocused,
                inActiveCol = inActiveCol,
                primary = primary,
                onValueChange = { onCellValueChange(col, it) },
                onFocusChange = { onCellSelect(col, it) },
                onResize = { onResize(col, it) },
                showResize = cellFocused,
                onMoveFocus = onMoveFocus,
                focusRequester = focusRequester,
                modifier = Modifier.width(getColumnWidth(col))
            )
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
    Box(modifier = Modifier.width(32.dp), contentAlignment = Alignment.Center) {
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
            DropdownMenuItem(text = { Text("Insert row above") }, onClick = { onShowMenuChange(false); onInsertAbove() })
            DropdownMenuItem(text = { Text("Insert row below") }, onClick = { onShowMenuChange(false); onInsertBelow() })
            DropdownMenuItem(text = { Text("Delete row") }, onClick = { onShowMenuChange(false); onDeleteRow() }, enabled = canDeleteRow)
        }
    }
}

// ============ 4. CELDA (KISS) ============
@Composable
private fun TableCell(
    cell: String,
    isFocused: Boolean,
    inActiveCol: Boolean,
    primary: Color,
    onValueChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    onResize: (Float) -> Unit,
    showResize: Boolean,
    onMoveFocus: (Int, Int) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val isHighlighted = isFocused || inActiveCol
    val link = isLink(cell)
    var text by remember { mutableStateOf(TextFieldValue(cell)) }
    LaunchedEffect(cell) {
        if (text.text != cell) text = TextFieldValue(cell)
    }
    var base = modifier
        .zIndex(if (isHighlighted) 1f else 0f)
        .border(
            width = if (isHighlighted) 1.5.dp else 0.5.dp,
            color = if (isHighlighted) TableBorderActive else TableBorderIdle
        )
        .background(
            if (isHighlighted) TableHighlightBg else Color.Transparent
        )
    if (link) {
        base = base.shadow(2.dp, RoundedCornerShape(4.dp))
    }
    Box(base.height(44.dp).padding(horizontal = 12.dp, vertical = 4.dp), contentAlignment = Alignment.CenterStart) {
        BasicTextField(
            value = text,
            onValueChange = { text = it; onValueChange(it.text) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { state -> onFocusChange(state.isFocused) }
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val dir = keyMoveDirection(event.key) ?: return@onPreviewKeyEvent false
                    onMoveFocus(dir.first, dir.second)
                    true
                },
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = if (link) primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (link) FontWeight.Medium else FontWeight.Normal
            ),
            singleLine = true
        )
        if (showResize) {
            ResizeHandle(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 4.dp, bottom = 4.dp)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { _, dragAmount -> onResize(dragAmount) }
                    },
                primary = primary
            )
        }
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

@Composable
private fun ResizeHandle(modifier: Modifier = Modifier, primary: Color) {
    Box(
        modifier = modifier
            .size(14.dp)
            .background(primary, CircleShape)
            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
    )
}

@Composable
private fun AddColumnFooter(onClick: () -> Unit, outline: Color) {
    Box(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .clickable(onClick = onClick)
            .size(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Add, contentDescription = "Add column", tint = outline)
    }
}

@Composable
private fun AddRowFooter(
    onAddBelow: () -> Unit,
    outline: Color,
    primary: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAddBelow)
            .padding(horizontal = 24.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(32.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Add, contentDescription = "Add row", modifier = Modifier.size(18.dp), tint = outline)
        }
        Text("Add row", style = MaterialTheme.typography.bodySmall, color = outline)
    }
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
            .background(Color.White, RoundedCornerShape(4.dp))
            .border(1.dp, if (selected) TableBorderActive else Color(0xFFE5E7EB), RoundedCornerShape(4.dp))
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .then(onDragModifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = handleText,
            color = if (selected) primary else Color.Gray,
            fontSize = if (isVertical) 10.sp else 12.sp,
            lineHeight = 10.sp,
            modifier = Modifier.offset(y = if (isVertical) (-2).dp else 0.dp)
        )
    }
}

@Composable
private fun FloatingToolbar(
    modifier: Modifier = Modifier,
    canDeleteColumn: Boolean,
    canDeleteRow: Boolean,
    hasRow: Boolean,
    hasColumn: Boolean,
    showHeader: Boolean,
    onToggleHeader: () -> Unit,
    onFitToScreen: () -> Unit,
    onFitToContent: () -> Unit,
    onDeleteRow: () -> Unit,
    onDeleteColumn: () -> Unit,
    onInsertRowAbove: () -> Unit,
    onInsertRowBelow: () -> Unit,
    onInsertColumnBefore: () -> Unit,
    onInsertColumnAfter: () -> Unit,
    onDeleteTable: () -> Unit,
    onSelectBg: (String?) -> Unit,
    onDragTableStart: () -> Unit,
    onDragTableBy: (Float) -> Unit,
    onDragTableEnd: () -> Unit,
    onDragTableCancel: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .pointerInput(Unit) {
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
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More", tint = MaterialTheme.colorScheme.primary)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text("Insert row above") }, onClick = { expanded = false; onInsertRowAbove() }, enabled = hasRow)
                    DropdownMenuItem(text = { Text("Insert row below") }, onClick = { expanded = false; onInsertRowBelow() }, enabled = hasRow)
                    DropdownMenuItem(text = { Text("Delete row") }, onClick = { expanded = false; onDeleteRow() }, enabled = hasRow && canDeleteRow)
                    DropdownMenuItem(text = { Text("Insert column before") }, onClick = { expanded = false; onInsertColumnBefore() }, enabled = hasColumn)
                    DropdownMenuItem(text = { Text("Insert column after") }, onClick = { expanded = false; onInsertColumnAfter() }, enabled = hasColumn)
                    DropdownMenuItem(text = { Text("Delete column") }, onClick = { expanded = false; onDeleteColumn() }, enabled = hasColumn && canDeleteColumn)
                    HorizontalDivider()
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                        text = { Text("Fit to screen") },
                        onClick = { expanded = false; onFitToScreen() }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.Default.FitScreen, null) },
                        text = { Text("Fit to content") },
                        onClick = { expanded = false; onFitToContent() }
                    )
                    DropdownMenuItem(
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                        text = { Text(if (showHeader) "Header row: on" else "Header row: off") },
                        onClick = { expanded = false; onToggleHeader() }
                    )
                    DropdownMenuItem(
                        text = { Text("Table color") },
                        onClick = { },
                        enabled = false
                    )
                    BackgroundColorRow(onSelect = onSelectBg)
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("Delete table") }, onClick = { expanded = false; onDeleteTable() })
                }
            }
        }
    }
}

@Composable
private fun BackgroundColorRow(onSelect: (String?) -> Unit) {
    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
        BG_OPTIONS.forEach { option ->
            val swatch = parseHex(option.hex) ?: Color.White
            Box(
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(22.dp)
                    .background(swatch, CircleShape)
                    .then(
                        if (option.hex == null) Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        else Modifier
                    )
                    .clickable { onSelect(option.hex) }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EditableTableBlockPreview() {
    MaterialTheme {
        val sampleData = TableData(
            headers = listOf("Encabezado 1", "Encabezado 2"),
            rows = listOf(
                listOf("Dato 1", "Dato 2"),
                listOf("Dato 3", "Dato 4")
            ),
            columnWeights = listOf(1f, 1f),
            bgColorHex = null,
            showHeader = true
        )
        EditableTableBlock(
            tableData = sampleData,
            onChange = {}
        )
    }
}