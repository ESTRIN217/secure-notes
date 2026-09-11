package com.example.ui.floating

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.data.model.TextSegment
import com.example.ui.EditableTextBlock
import com.example.ui.FormattingToggleButton
import com.example.util.RichTextConverter
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

enum class FloatingTab { QUICK_NOTE, RECENT_NOTES }

@Composable
fun FloatingNoteCard(
    recentNotes: List<Note>,
    title: String,
    segments: List<TextSegment>,
    selection: IntRange,
    pendingTypingStyle: TextSegment?,
    activeTextStyles: Set<String>,
    onTitleChange: (String) -> Unit,
    onSegmentsChange: (List<TextSegment>) -> Unit,
    onSelectionChange: (IntRange) -> Unit,
    onToggleTag: (String) -> Unit,
    onSaveNote: (title: String, segments: List<TextSegment>) -> Unit,
    onClear: () -> Unit,
    onHeaderDrag: (Offset) -> Unit,
    onResizeCorner: (ResizeCorner, Offset) -> Unit,
    onResetLayout: () -> Unit,
    onOpenApp: (noteId: Int?) -> Unit,
    onMinimize: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(FloatingTab.QUICK_NOTE) }
    var resizeMode by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(min = 280.dp)
            .heightIn(min = 360.dp)
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
                onClose = onClose,
                onDrag = onHeaderDrag
            )
            Spacer(modifier = Modifier.height(8.dp))
            FloatingCardTabs(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
            Spacer(modifier = Modifier.height(12.dp))
            when (selectedTab) {
                FloatingTab.QUICK_NOTE -> FloatingQuickNoteTab(
                    title = title,
                    segments = segments,
                    selection = selection,
                    pendingTypingStyle = pendingTypingStyle,
                    activeTextStyles = activeTextStyles,
                    onTitleChange = onTitleChange,
                    onSegmentsChange = onSegmentsChange,
                    onSelectionChange = onSelectionChange,
                    onToggleTag = onToggleTag,
                    onSaveNote = onSaveNote,
                    onClear = onClear,
                    resizeMode = resizeMode,
                    onToggleResizeMode = { resizeMode = !resizeMode },
                    onResetLayout = {
                        onResetLayout()
                        resizeMode = false
                    }
                )
                FloatingTab.RECENT_NOTES -> FloatingRecentNotesTab(
                    notes = recentNotes,
                    onOpenNote = { onOpenApp(it.id) }
                )
            }
        }
    }
    if (resizeMode) {
        ResizeHandles(onResize = onResizeCorner)
    }
    }
}

@Composable
private fun BoxScope.ResizeHandles(
    onResize: (ResizeCorner, Offset) -> Unit
) {
    val currentOnResize by rememberUpdatedState(onResize)
    ResizeHandle(
        corner = ResizeCorner.TOP_LEFT,
        onResize = currentOnResize,
        modifier = Modifier.align(Alignment.TopStart)
    )
    ResizeHandle(
        corner = ResizeCorner.TOP_RIGHT,
        onResize = currentOnResize,
        modifier = Modifier.align(Alignment.TopEnd)
    )
    ResizeHandle(
        corner = ResizeCorner.BOTTOM_LEFT,
        onResize = currentOnResize,
        modifier = Modifier.align(Alignment.BottomStart)
    )
    ResizeHandle(
        corner = ResizeCorner.BOTTOM_RIGHT,
        onResize = currentOnResize,
        modifier = Modifier.align(Alignment.BottomEnd)
    )
}

@Composable
private fun ResizeHandle(
    corner: ResizeCorner,
    onResize: (ResizeCorner, Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .pointerInput(Unit) {
                detectDragGestures { _, dragAmount -> onResize(corner, dragAmount) }
            },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.size(16.dp)
        ) {}
    }
}

@Composable
private fun FloatingCardHeader(
    onOpenApp: () -> Unit,
    onMinimize: () -> Unit,
    onClose: () -> Unit,
    onDrag: (Offset) -> Unit
) {
    val currentOnDrag by rememberUpdatedState(onDrag)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectDragGestures { _, dragAmount -> currentOnDrag(dragAmount) }
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo
            AsyncImage(
                model = R.mipmap.ic_launcher_round,
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
    title: String,
    segments: List<TextSegment>,
    selection: IntRange,
    pendingTypingStyle: TextSegment?,
    activeTextStyles: Set<String>,
    onTitleChange: (String) -> Unit,
    onSegmentsChange: (List<TextSegment>) -> Unit,
    onSelectionChange: (IntRange) -> Unit,
    onToggleTag: (String) -> Unit,
    onSaveNote: (title: String, segments: List<TextSegment>) -> Unit,
    onClear: () -> Unit,
    resizeMode: Boolean,
    onToggleResizeMode: () -> Unit,
    onResetLayout: () -> Unit
) {
    var showSavedBanner by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val plainText = remember(segments) { RichTextConverter.segmentsToPlainText(segments) }

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
            segments = segments,
            selection = selection,
            pendingTypingStyle = pendingTypingStyle,
            onTitleChange = onTitleChange,
            onSegmentsChange = onSegmentsChange,
            onSelectionChange = onSelectionChange,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        FloatingQuickNoteActions(
            canSave = plainText.isNotBlank() || title.isNotBlank(),
            onSave = {
                onSaveNote(title.trim(), segments)
                onClear()
                showSavedBanner = true
            },
            onCopy = { clipboard.setText(AnnotatedString(plainText)) },
            onClear = onClear,
            activeTextStyles = activeTextStyles,
            onToggleTag = onToggleTag,
            resizeMode = resizeMode,
            onToggleResizeMode = onToggleResizeMode,
            onResetLayout = onResetLayout
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
    segments: List<TextSegment>,
    selection: IntRange,
    pendingTypingStyle: TextSegment?,
    onTitleChange: (String) -> Unit,
    onSegmentsChange: (List<TextSegment>) -> Unit,
    onSelectionChange: (IntRange) -> Unit,
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
        QuickNoteSegmentField(
            segments = segments,
            selection = selection,
            pendingTypingStyle = pendingTypingStyle,
            onSegmentsChange = onSegmentsChange,
            onSelectionChange = onSelectionChange,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
}

@Composable
private fun QuickNoteSegmentField(
    segments: List<TextSegment>,
    selection: IntRange,
    pendingTypingStyle: TextSegment?,
    onSegmentsChange: (List<TextSegment>) -> Unit,
    onSelectionChange: (IntRange) -> Unit,
    modifier: Modifier = Modifier
) {
    val hint = stringResource(R.string.floating_mode_note_content_hint)
    val isEmpty = remember(segments) {
        RichTextConverter.segmentsToPlainText(segments).isEmpty()
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline
        ),
        modifier = modifier
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            EditableTextBlock(
                segments = segments,
                onChange = onSegmentsChange,
                onSelectionChange = onSelectionChange,
                pendingTypingStyle = pendingTypingStyle,
                initialSelection = selection.first,
                showPrefix = false
            )
            if (isEmpty) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FloatingQuickNoteActions(
    canSave: Boolean,
    onSave: () -> Unit,
    onCopy: () -> Unit,
    onClear: () -> Unit,
  activeTextStyles: Set<String>,
  onToggleTag: (String) -> Unit,
  resizeMode: Boolean,
  onToggleResizeMode: () -> Unit,
  onResetLayout: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
      FormattingToggleButton(
        checked = "b" in activeTextStyles,
        onCheckedChange = { onToggleTag("b") }
      ) {
        Icon(Icons.Default.FormatBold, contentDescription = stringResource(R.string.negrita))
      }
                FormattingToggleButton(
                    checked = "i" in activeTextStyles,
                    onCheckedChange = { onToggleTag("i") }
                ) {
                    Icon(Icons.Default.FormatItalic, contentDescription = stringResource(R.string.italica))
                }
                FormattingToggleButton(
                    checked = "u" in activeTextStyles,
                    onCheckedChange = { onToggleTag("u") }
                ) {
                    Icon(Icons.Default.FormatUnderlined, contentDescription = stringResource(R.string.subrayado))
                }
                FormattingToggleButton(
                    checked = "s" in activeTextStyles,
                    onCheckedChange = { onToggleTag("s") }
                ) {
                    Icon(Icons.Default.FormatStrikethrough, contentDescription = stringResource(R.string.tachado))
                }
        IconButton(onClick = onClear) {
            Icon(Icons.Default.DeleteOutline, contentDescription = stringResource(R.string.floating_mode_clear))
        }
        OutlinedButton(onClick = onCopy, shape = RoundedCornerShape(12.dp)) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            //Spacer(modifier = Modifier.width(4.dp))
            //Text(stringResource(R.string.floating_mode_copy_text))
        }
        Button(
            onClick = onSave,
            enabled = canSave,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
            //Spacer(modifier = Modifier.width(4.dp))
            //Text(stringResource(R.string.floating_mode_save_note))
        }
        IconButton(onClick = onToggleResizeMode) {
            Icon(
                Icons.Default.OpenInFull,
                contentDescription = stringResource(R.string.redimensionar),
                tint = if (resizeMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        if (resizeMode) {
            IconButton(onClick = onResetLayout) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.restablecer_tamano),
                    modifier = Modifier.size(18.dp)
                )
            }
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
