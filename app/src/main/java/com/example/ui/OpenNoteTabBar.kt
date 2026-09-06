package com.example.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R

@Composable
fun OpenNoteTabBar(
    tabs: List<OpenTab>,
    selectedIndex: Int,
    titleFor: @Composable (Int) -> String,
    onSelect: (Int) -> Unit,
    onClose: (Int) -> Unit,
    onCloseOthers: (Int) -> Unit,
    onCloseAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (tabs.size <= 1) return
    ScrollableTabRow(
        selectedTabIndex = selectedIndex.coerceIn(0, tabs.lastIndex),
        modifier = modifier,
        edgePadding = 8.dp
    ) {
        tabs.forEachIndexed { index, tab ->
            NoteTab(
                title = titleFor(tab.noteId),
                selected = index == selectedIndex,
                onSelect = { onSelect(index) },
                onClose = { onClose(index) },
                onCloseOthers = { onCloseOthers(index) },
                onCloseAll = onCloseAll
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteTab(
    title: String,
    selected: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    onCloseOthers: () -> Unit,
    onCloseAll: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Tab(
        selected = selected,
        onClick = onSelect,
        text = {
            Row {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 4.dp)
                )
                TabCloseButton(onClose)
            }
        },
        modifier = Modifier.combinedClickable(
            onClick = onSelect,
            onLongClick = { menuExpanded = true }
        )
    )
    TabMenu(
        expanded = menuExpanded,
        onDismiss = { menuExpanded = false },
        onCloseOthers = { menuExpanded = false; onCloseOthers() },
        onCloseAll = { menuExpanded = false; onCloseAll() }
    )
}

@Composable
private fun TabCloseButton(onClose: () -> Unit) {
    IconButton(onClick = onClose, modifier = Modifier.size(20.dp)) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = stringResource(R.string.tabs_close),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun TabMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onCloseOthers: () -> Unit,
    onCloseAll: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.tabs_close_others)) },
            onClick = onCloseOthers
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.tabs_close_all)) },
            onClick = onCloseAll
        )
    }
}
