package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.TableData

private data class FocusedCell(val row: Int, val col: Int)

@Composable
fun EditableTableBlock(
    tableData: TableData,
    onChange: (TableData) -> Unit,
    onFocusChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var headers by remember { mutableStateOf(tableData.headers.toMutableList()) }
    var rows by remember { mutableStateOf(tableData.rows.map { it.toMutableList() }.toMutableList()) }
    var focusedCell by remember { mutableStateOf<FocusedCell?>(null) }
    var showRowMenu by remember { mutableStateOf(false) }
    var showColMenu by remember { mutableStateOf(false) }

    LaunchedEffect(tableData) {
        headers = tableData.headers.toMutableList()
        rows = tableData.rows.map { it.toMutableList() }.toMutableList()
    }

    fun notifyChange() {
        onChange(TableData(headers.toList(), rows.map { it.toList() }))
    }

    val columnCount = headers.size.coerceAtLeast(rows.maxOfOrNull { it.size } ?: 0)
    val focusedRow = focusedCell?.takeIf { it.row >= 0 }?.row
    val focusedCol = focusedCell?.col
    val hasFocus = focusedCell != null

    fun insertRowAbove(rowIndex: Int) {
        rows.add(rowIndex.coerceIn(0, rows.size), MutableList(columnCount) { "" })
        focusedCell = null
        notifyChange()
    }

    fun insertRowBelow(rowIndex: Int) {
        rows.add((rowIndex + 1).coerceIn(0, rows.size), MutableList(columnCount) { "" })
        focusedCell = null
        notifyChange()
    }

    fun deleteRow(rowIndex: Int) {
        if (rows.size > 1) {
            rows.removeAt(rowIndex.coerceIn(0, rows.lastIndex))
            focusedCell = null
            notifyChange()
        }
    }

    fun insertColumnLeft(colIndex: Int) {
        headers.add(colIndex.coerceIn(0, headers.size), "")
        rows.forEach { it.add(colIndex.coerceIn(0, it.size), "") }
        focusedCell = null
        notifyChange()
    }

    fun insertColumnRight(colIndex: Int) {
        headers.add((colIndex + 1).coerceIn(0, headers.size), "")
        rows.forEach { it.add((colIndex + 1).coerceIn(0, it.size), "") }
        focusedCell = null
        notifyChange()
    }

    fun deleteColumn(colIndex: Int) {
        if (columnCount > 1) {
            if (colIndex < headers.size) headers.removeAt(colIndex)
            rows.forEach { if (colIndex < it.size) it.removeAt(colIndex) }
            focusedCell = null
            notifyChange()
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(Modifier.horizontalScroll(rememberScrollState())) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.width(20.dp))
                headers.forEachIndexed { col, header ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicTextField(
                            value = header,
                            onValueChange = {
                                headers[col] = it
                                notifyChange()
                            },
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .onFocusChanged { state ->
                                    if (state.isFocused) {
                                        focusedCell = FocusedCell(-1, col)
                                    } else if (focusedCell == FocusedCell(-1, col)) {
                                        focusedCell = null
                                    }
                                    onFocusChange(state.isFocused)
                                },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            singleLine = true
                        )
                        if (hasFocus && focusedCol == col) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DragHandle,
                                    contentDescription = "Column handle",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                DropdownMenu(
                                    expanded = showColMenu,
                                    onDismissRequest = { showColMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Insert column left") },
                                        onClick = {
                                            showColMenu = false
                                            insertColumnLeft(col)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Insert column right") },
                                        onClick = {
                                            showColMenu = false
                                            insertColumnRight(col)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete column") },
                                        onClick = {
                                            showColMenu = false
                                            deleteColumn(col)
                                        },
                                        enabled = columnCount > 1
                                    )
                                }
                            }
                        }
                    }
                }
                if (columnCount > headers.size) {
                    repeat(columnCount - headers.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

            rows.forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (rowIndex % 2 == 1)
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                            else Color.Transparent
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.width(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (hasFocus && focusedRow == rowIndex) {
                            Icon(
                                imageVector = Icons.Default.DragIndicator,
                                contentDescription = "Row handle",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            DropdownMenu(
                                expanded = showRowMenu,
                                onDismissRequest = { showRowMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Insert row above") },
                                    onClick = {
                                        showRowMenu = false
                                        insertRowAbove(rowIndex)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Insert row below") },
                                    onClick = {
                                        showRowMenu = false
                                        insertRowBelow(rowIndex)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete row") },
                                    onClick = {
                                        showRowMenu = false
                                        deleteRow(rowIndex)
                                    },
                                    enabled = rows.size > 1
                                )
                            }
                        }
                    }
                    for (col in 0 until columnCount) {
                        val cell = row.getOrElse(col) { "" }
                        BasicTextField(
                            value = cell,
                            onValueChange = {
                                while (row.size <= col) row.add("")
                                row[col] = it
                                notifyChange()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .onFocusChanged { state ->
                                    if (state.isFocused) {
                                        focusedCell = FocusedCell(rowIndex, col)
                                    } else if (focusedCell == FocusedCell(rowIndex, col)) {
                                        focusedCell = null
                                    }
                                    onFocusChange(state.isFocused)
                                },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            singleLine = true
                        )
                    }
                }
            }
        }
    }
}
