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
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.DragIndicator
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.unit.dp
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

private val LINK_REGEX = Regex("(https?|note)://")

private fun isLink(text: String): Boolean = LINK_REGEX.containsMatchIn(text)

private fun parseHex(hex: String?): Color? {
    if (hex.isNullOrBlank()) return null
    return try {
        Color(android.graphics.Color.parseColor("#$hex"))
    } catch (e: Exception) {
        null
    }
}

private fun String.hex(): String = trim().removePrefix("#")

private fun keyMoveDirection(key: Key): Pair<Int, Int>? = when (key) {
    Key.DirectionDown, Key.Enter -> Pair(1, 0)
    Key.DirectionUp -> Pair(-1, 0)
    Key.DirectionRight, Key.Tab -> Pair(0, 1)
    Key.DirectionLeft -> Pair(0, -1)
    else -> null
}

@Composable
fun EditableTableBlock(
    tableData: TableData,
    onChange: (TableData) -> Unit,
    onFocusChange: (Boolean) -> Unit = {},
    onDeleteBlock: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var headers by remember { mutableStateOf(tableData.headers.toMutableList()) }
    var rows by remember { mutableStateOf(tableData.rows.map { it.toMutableList() }.toMutableList()) }
    var weights by remember { mutableStateOf(tableData.normalizedWeights().toMutableList()) }
    var bgColorHex by remember { mutableStateOf(tableData.bgColorHex?.hex()) }
    var showHeader by remember { mutableStateOf(tableData.showHeader) }
    var focusedCell by remember { mutableStateOf<FocusedCell?>(null) }
    var selectedRow by remember { mutableStateOf<Int?>(null) }
    var showRowMenu by remember { mutableStateOf(false) }
    var showColMenu by remember { mutableStateOf(false) }
    var dragRow by remember { mutableStateOf<Int?>(null) }
    var dragRowTarget by remember { mutableStateOf<Int?>(null) }
    var dragCol by remember { mutableStateOf<Int?>(null) }
    var dragColTarget by remember { mutableStateOf<Int?>(null) }
    var rowHeightPx by remember { mutableStateOf(48f) }

    val focusRequesters = remember { mutableMapOf<Pair<Int, Int>, FocusRequester>() }

    LaunchedEffect(tableData) {
        headers = tableData.headers.toMutableList()
        rows = tableData.rows.map { it.toMutableList() }.toMutableList()
        weights = tableData.normalizedWeights().toMutableList()
        bgColorHex = tableData.bgColorHex?.hex()
        showHeader = tableData.showHeader
    }

    fun notifyChange() {
        onChange(
            TableData(
                headers = headers.toList(),
                rows = rows.map { it.toList() },
                columnWeights = weights.toList(),
                bgColorHex = bgColorHex,
                showHeader = showHeader
            )
        )
    }

    val columnCount = headers.size.coerceAtLeast(rows.maxOfOrNull { it.size } ?: 0)
    val focusedRow = focusedCell?.takeIf { it.row >= 0 }?.row
    val focusedCol = focusedCell?.col
    val hasFocus = focusedCell != null
    val activeCol = focusedCol
    val activeRow = selectedRow ?: focusedRow

    fun cellWeight(col: Int): Float = weights.getOrElse(col) { 1f }.coerceAtLeast(0.1f)

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
            notifyChange()
        }
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

    val tableBg = parseHex(bgColorHex) ?: Color.Transparent
    val outline = MaterialTheme.colorScheme.outlineVariant
    val primary = MaterialTheme.colorScheme.primary

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val colUnitPx = with(LocalDensity.current) { maxWidth.toPx() } / columnCount.coerceAtLeast(1)

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
                    if (showHeader) {
                        TableHeaderRow(
                            headers = headers,
                            columnCount = columnCount,
                            cellWeight = ::cellWeight,
                            outline = outline,
                            primary = primary,
                            focusedCol = focusedCol,
                            showColMenu = showColMenu,
                            onShowColMenuChange = { showColMenu = it },
                            isDropCol = dragCol != null && dragCol != dragColTarget && dragColTarget != null,
                            colUnitPx = colUnitPx,
                            onHeaderValueChange = { col, value ->
                                headers[col] = value
                                notifyChange()
                            },
                            onHeaderSelect = { col, focused ->
                                if (focused) {
                                    focusedCell = FocusedCell(-1, col)
                                    selectedRow = null
                                } else if (focusedCell == FocusedCell(-1, col)) {
                                    focusedCell = null
                                }
                                onFocusChange(focused)
                            },
                            onInsertLeft = { insertColumnLeft(it) },
                            onInsertRight = { insertColumnRight(it) },
                            onDeleteColumn = { deleteColumn(it) },
                            canDeleteColumn = columnCount > 1,
                            onResizeColumn = { col, delta ->
                                weights[col] = (cellWeight(col) + delta / colUnitPx.coerceAtLeast(1f)).coerceIn(0.1f, 6f)
                                notifyChange()
                            },
                            onDragColState = { s, t -> dragCol = s; dragColTarget = t },
                            onCommitCol = {
                                val s = dragCol
                                val t = dragColTarget
                                dragCol = null
                                dragColTarget = null
                                if (s != null && t != null) moveColumn(s, t)
                            },
                            onCancelCol = { dragCol = null; dragColTarget = null },
                            focusRequesters = focusRequesters,
                            onMoveFocus = ::moveFocus
                        )
                        HorizontalDivider(thickness = 1.dp, color = outline)
                    }

                    rows.forEachIndexed { rowIndex, row ->
                        TableRow(
                            row = row,
                            rowIndex = rowIndex,
                            columnCount = columnCount,
                            outline = outline,
                            primary = primary,
                            focusedRow = focusedRow,
                            focusedCol = focusedCol,
                            selectedRow = selectedRow,
                            isDropRow = dragRow != null && dragRow != rowIndex && dragRowTarget == rowIndex,
                            showRowMenu = showRowMenu && selectedRow == rowIndex,
                            onShowRowMenuChange = { showRowMenu = it },
                            onCellWeight = ::cellWeight,
                            onCellValueChange = { col, value ->
                                while (row.size <= col) row.add("")
                                row[col] = value
                                notifyChange()
                            },
                            onCellSelect = { col, focused ->
                                if (focused) {
                                    focusedCell = FocusedCell(rowIndex, col)
                                } else if (focusedCell == FocusedCell(rowIndex, col)) {
                                    focusedCell = null
                                }
                                onFocusChange(focused)
                            },
                            onRowSelect = {
                                selectedRow = rowIndex
                                focusedCell = null
                            },
                            onInsertAbove = { insertRowAbove(rowIndex) },
                            onInsertBelow = { insertRowBelow(rowIndex) },
                            onDeleteRow = { deleteRow(rowIndex) },
                            canDeleteRow = rows.size > 1,
                            onResize = { col, delta ->
                                weights[col] = (cellWeight(col) + delta / 24f / columnCount.coerceAtLeast(1)).coerceIn(0.1f, 6f)
                                notifyChange()
                            },
                            rowHeightPx = rowHeightPx,
                            rowCount = rows.size,
                            onDragRowState = { s, t -> dragRow = s; dragRowTarget = t },
                            onCommitRow = {
                                val s = dragRow
                                val t = dragRowTarget
                                dragRow = null
                                dragRowTarget = null
                                if (s != null && t != null) moveRow(s, t)
                            },
                            onCancelRow = { dragRow = null; dragRowTarget = null },
                            onRowHeight = { rowHeightPx = it },
                            focusRequesters = focusRequesters,
                            onMoveFocus = ::moveFocus
                        )
                        if (rowIndex < rows.lastIndex) {
                            HorizontalDivider(thickness = 0.5.dp, color = outline.copy(alpha = 0.5f))
                        }
                    }

                    AddRowFooter(
                        onAddBelow = { insertRowBelow(rows.lastIndex) },
                        outline = outline,
                        primary = primary
                    )
                }
            }

            if (hasFocus) {
                FloatingToolbar(
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp),
                    canDeleteColumn = columnCount > 1,
                    canDeleteRow = rows.size > 1,
                    hasRow = activeRow != null,
                    hasColumn = activeCol != null,
                    showHeader = showHeader,
                    onToggleHeader = { showHeader = !showHeader; notifyChange() },
                    onFitToScreen = { fitToScreen() },
                    onFitToContent = { fitToContent() },
                    onDeleteRow = { activeRow?.let { deleteRow(it) } },
                    onDeleteColumn = { activeCol?.let { deleteColumn(it) } },
                    onInsertRowAbove = { activeRow?.let { insertRowAbove(it) } },
                    onInsertRowBelow = { activeRow?.let { insertRowBelow(it) } },
                    onInsertColumnBefore = { activeCol?.let { insertColumnLeft(it) } },
                    onInsertColumnAfter = { activeCol?.let { insertColumnRight(it) } },
                    onDeleteTable = onDeleteBlock,
                    onSelectBg = { bgColorHex = it?.hex(); notifyChange() }
                )
            }
        }
    }
}

@Composable
private fun TableHeaderRow(
    headers: List<String>,
    columnCount: Int,
    cellWeight: (Int) -> Float,
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
        for (col in 0 until columnCount) {
            val header = headers.getOrElse(col) { "" }
            val selected = focusedCol == col
            val focusRequester = remember { FocusRequester() }
            focusRequesters[Pair(-1, col)] = focusRequester
            Box(
                modifier = Modifier
                    .weight(cellWeight(col))
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) primary else outline.copy(alpha = 0.7f)
                    )
                    .background(
                        if (selected) primary.copy(alpha = 0.08f) else Color.Transparent
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
                    BasicTextField(
                        value = header,
                        onValueChange = { onHeaderValueChange(col, it) },
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
        AddColumnFooter(onClick = { onInsertRight(columnCount - 1) }, outline = outline)
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
        Icon(
            imageVector = Icons.Default.DragHandle,
            contentDescription = "Column handle",
            modifier = Modifier
                .padding(2.dp)
                .clickable { onShowMenuChange(true) }
                .pointerInput(col) {
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
                },
            tint = primary
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
    outline: Color,
    primary: Color,
    focusedRow: Int?,
    focusedCol: Int?,
    selectedRow: Int?,
    isDropRow: Boolean,
    showRowMenu: Boolean,
    onShowRowMenuChange: (Boolean) -> Unit,
    onCellWeight: (Int) -> Float,
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
                outline = outline,
                primary = primary,
                onValueChange = { onCellValueChange(col, it) },
                onFocusChange = { onCellSelect(col, it) },
                onResize = { onResize(col, it) },
                showResize = cellFocused,
                onMoveFocus = onMoveFocus,
                focusRequester = focusRequester,
                modifier = Modifier.weight(onCellWeight(col))
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
    Box(modifier = Modifier.width(24.dp), contentAlignment = Alignment.Center) {
        Icon(
            imageVector = Icons.Default.DragIndicator,
            contentDescription = "Row handle",
            modifier = Modifier
                .padding(2.dp)
                .clickable {
                    onSelect()
                    onShowMenuChange(true)
                }
                .pointerInput(rowIndex) {
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
                },
            tint = if (selected) primary else MaterialTheme.colorScheme.outline
        )
        DropdownMenu(expanded = showMenu, onDismissRequest = { onShowMenuChange(false) }) {
            DropdownMenuItem(text = { Text("Insert row above") }, onClick = { onShowMenuChange(false); onInsertAbove() })
            DropdownMenuItem(text = { Text("Insert row below") }, onClick = { onShowMenuChange(false); onInsertBelow() })
            DropdownMenuItem(text = { Text("Delete row") }, onClick = { onShowMenuChange(false); onDeleteRow() }, enabled = canDeleteRow)
        }
    }
}

@Composable
private fun TableCell(
    cell: String,
    isFocused: Boolean,
    inActiveCol: Boolean,
    outline: Color,
    primary: Color,
    onValueChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    onResize: (Float) -> Unit,
    showResize: Boolean,
    onMoveFocus: (Int, Int) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val link = isLink(cell)
    var base = modifier
        .border(
            width = if (isFocused) 2.dp else 1.dp,
            color = if (isFocused) primary else outline.copy(alpha = 0.7f)
        )
        .background(
            if (isFocused || inActiveCol) primary.copy(alpha = 0.08f) else Color.Transparent
        )
    if (link) {
        base = base.shadow(2.dp, RoundedCornerShape(4.dp))
    }
    Box(base.padding(horizontal = 12.dp, vertical = 10.dp)) {
        BasicTextField(
            value = cell,
            onValueChange = onValueChange,
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
        Box(Modifier.width(24.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Add, contentDescription = "Add row", modifier = Modifier.size(18.dp), tint = outline)
        }
        Text("Add row", style = MaterialTheme.typography.bodySmall, color = outline)
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
    onSelectBg: (String?) -> Unit
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
            IconButton(onClick = onFitToContent) {
                Icon(Icons.Default.FitScreen, contentDescription = "Fit to content", tint = MaterialTheme.colorScheme.primary)
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