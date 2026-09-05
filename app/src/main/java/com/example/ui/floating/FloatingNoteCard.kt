package com.example.ui.floating

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.example.R
import com.example.data.model.Note
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

enum class FloatingTab { QUICK_NOTE, RECENT_NOTES }

@Composable
fun FloatingNoteCard(
    recentNotes: List<Note>,
    onSaveNote: (title: String, content: String) -> Unit,
    onOpenApp: (noteId: Int?) -> Unit,
    onMinimize: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(FloatingTab.QUICK_NOTE) }

    Surface(
        modifier = modifier
            .widthIn(min = 280.dp, max = 380.dp)
            .heightIn(min = 360.dp, max = 500.dp)
            .shadow(16.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            FloatingCardHeader(
                onOpenApp = { onOpenApp(null) },
                onMinimize = onMinimize,
                onClose = onClose
            )
            Spacer(modifier = Modifier.height(8.dp))
            FloatingCardTabs(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
            Spacer(modifier = Modifier.height(12.dp))
            when (selectedTab) {
                FloatingTab.QUICK_NOTE -> FloatingQuickNoteTab(onSaveNote = onSaveNote)
                FloatingTab.RECENT_NOTES -> FloatingRecentNotesTab(
                    notes = recentNotes,
                    onOpenNote = { onOpenApp(it.id) }
                )
            }
        }
    }
}

@Composable
private fun FloatingCardHeader(
    onOpenApp: () -> Unit,
    onMinimize: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo
            AsyncImage(
                model = R.mipmap.ic_launcher,
                contentDescription = stringResource(id = R.string.cd_secure_notes_logo),
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
      IconButton(onClick = onMinimize, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Minimize, contentDescription = stringResource(R.string.floating_mode_minimize), modifier = Modifier.size(18.dp))
                }
        IconButton(onClick = onOpenApp, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.OpenInNew, contentDescription = stringResource(R.string.floating_mode_open_app), modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.floating_mode_close), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun FloatingCardTabs(
    selectedTab: FloatingTab,
    onTabSelected: (FloatingTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp)
    ) {
        TabButton(
            title = stringResource(R.string.floating_mode_quick_note),
            isSelected = selectedTab == FloatingTab.QUICK_NOTE,
            onClick = { onTabSelected(FloatingTab.QUICK_NOTE) },
            modifier = Modifier.weight(1f)
        )
        TabButton(
            title = stringResource(R.string.floating_mode_recent_notes),
            isSelected = selectedTab == FloatingTab.RECENT_NOTES,
            onClick = { onTabSelected(FloatingTab.RECENT_NOTES) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TabButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (isSelected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FloatingQuickNoteTab(
    onSaveNote: (title: String, content: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var showSavedBanner by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(showSavedBanner) {
        if (showSavedBanner) {
            delay(2000)
            showSavedBanner = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (showSavedBanner) {
            SavedFeedbackBanner()
            Spacer(modifier = Modifier.height(6.dp))
        }
        QuickNoteInputs(
            title = title,
            content = content,
            onTitleChange = { title = it },
            onContentChange = { content = it },
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        FloatingQuickNoteActions(
            canSave = content.isNotBlank() || title.isNotBlank(),
            onSave = {
                onSaveNote(title.trim(), content.trim())
                title = ""
                content = ""
                showSavedBanner = true
            },
            onCopy = { clipboard.setText(AnnotatedString(content)) },
            onClear = { title = ""; content = "" }
        )
    }
}

@Composable
private fun SavedFeedbackBanner() {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.floating_mode_note_saved), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun QuickNoteInputs(
    title: String,
    content: String,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            placeholder = { Text(stringResource(R.string.floating_mode_note_title_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = content,
            onValueChange = onContentChange,
            placeholder = { Text(stringResource(R.string.floating_mode_note_content_hint)) },
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
private fun FloatingQuickNoteActions(
    canSave: Boolean,
    onSave: () -> Unit,
    onCopy: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClear) {
            Icon(Icons.Default.DeleteOutline, contentDescription = stringResource(R.string.floating_mode_clear))
        }
        OutlinedButton(onClick = onCopy, shape = RoundedCornerShape(12.dp)) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.floating_mode_copy_text))
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onSave,
            enabled = canSave,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.floating_mode_save_note))
        }
    }
}

@Composable
private fun FloatingRecentNotesTab(
    notes: List<Note>,
    onOpenNote: (Note) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(notes, searchQuery) {
        if (searchQuery.isBlank()) notes
        else notes.filter { it.title.contains(searchQuery, ignoreCase = true) || it.content.contains(searchQuery, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(stringResource(R.string.search_placeholder)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(filtered, key = { it.id }) { note ->
                FloatingRecentNoteItem(note = note, onClick = { onOpenNote(note) })
            }
        }
    }
}

@Composable
private fun FloatingRecentNoteItem(
    note: Note,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val dateText = remember(note.lastModified) { dateFormat.format(Date(note.lastModified)) }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (note.isEncrypted) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = note.title.ifBlank { stringResource(R.string.btn_new_note) },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(text = dateText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}
