package com.example.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R

@Composable
fun tabIconFor(tab: OpenTab): ImageVector? {
    if (tab is OpenTab.Note) return null
    if (tab is OpenTab.Media && tab.type == "image") return Icons.Default.Image
    if (tab is OpenTab.Media && tab.type == "video") return Icons.Default.Videocam
    if (tab is OpenTab.Media) return Icons.Default.Audiotrack
    if (tab is OpenTab.Pdf) return Icons.Default.PictureAsPdf
    return Icons.Default.Description
}

@Composable
fun OpenNoteTabBar(
    tabs: List<OpenTab>,
    selectedIndex: Int,
    titleFor: @Composable (OpenTab) -> String,
    onSelect: (Int) -> Unit,
    onClose: (Int) -> Unit,
    onCloseOthers: (Int) -> Unit,
    onCloseAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (tabs.size <= 1) return
    PrimaryScrollableTabRow(
        selectedTabIndex = selectedIndex.coerceIn(0, tabs.lastIndex),
        modifier = modifier,
        edgePadding = 8.dp
    ) {
        tabs.forEachIndexed { index, tab ->
            NoteTab(
                title = titleFor(tab),
                icon = tabIconFor(tab),
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
    icon: ImageVector?,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 4.dp)
                    )
                }
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
