package com.example.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Attachment
import com.example.data.model.DecryptedNote
import com.example.data.model.Note
import com.example.data.model.NoteContentBlock
import com.example.ui.viewmodel.AiViewModel
import com.example.data.model.createRawContent
import com.example.data.model.parseNoteContentAndAttachments
import com.example.data.model.parseTags
import com.example.ui.viewmodel.NotesViewModel
import com.example.ui.settings.SettingsCardGroup
import com.example.ui.settings.SettingsSwitchTile
import com.example.util.getNoteBackgroundColor
import com.example.util.exportMultipleToHtml
import com.example.util.exportMultipleToTxt
import com.example.util.exportToMarkdown
import com.example.util.exportToPdf
import com.example.util.exportSingleNoteToJson
import com.example.util.MediaBlock
import com.example.util.RichTextParser
import com.example.util.buildPreviewBlocks
import com.example.util.findEnclosingMarkdownLinkRange
import com.example.util.findEnclosingUrlTagRange
import com.example.util.highlightMatches
import com.example.util.parseToContentBlocks
import com.example.util.removeAttachmentFromContent
import com.example.util.removeMediaFromContent
import com.example.util.toggleNthChecklistItem
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.core.content.FileProvider
import androidx.lifecycle.viewModelScope
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch









@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun NoteEditorScreen(
    noteId: Int,
    viewModel: NotesViewModel,
    aiViewModel: AiViewModel,
    onBack: () -> Unit,
    onNavigateToDrawing: (Int, String?) -> Unit = { _, _ -> },
    onNavigateToMediaViewer: (String, String) -> Unit = { _, _ -> },
    onNavigateToAiChat: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isPasswordSet by viewModel.isPasswordSet.collectAsState()
    val acquireUriPermission: (Uri) -> Unit = { uri ->
        try {
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        } catch (e: Exception) {
            Log.e("NoteEditorScreen", "acquireUriPermission failed", e)
        }
    }
    
    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    var isSpeaking by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val ttsEngine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.let { engine ->
                    val locale = Locale.getDefault()
                    if (engine.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE) {
                        engine.language = locale
                    } else {
                        engine.language = Locale.US
                    }
                    engine.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            isSpeaking = true
                        }
                        override fun onDone(utteranceId: String?) {
                            isSpeaking = false
                        }
                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            isSpeaking = false
                        }
                        override fun onError(utteranceId: String?, errorCode: Int) {
                            isSpeaking = false
                        }
                    })
                }
            }
        }
        tts = ttsEngine
        onDispose {
            ttsEngine.stop()
            ttsEngine.shutdown()
        }
    }
    
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var isEncrypted by remember { mutableStateOf(isPasswordSet) }
    var isPreviewMode by remember { mutableStateOf(noteId != 0) }
    
    var contentValue by remember { mutableStateOf(TextFieldValue("")) }
    val history = remember { mutableStateListOf<String>() }
    var historyIndex by remember { mutableStateOf(-1) }
    var contentLoaded by remember { mutableStateOf(noteId == 0) }
    
    var showInsertImageDialog by remember { mutableStateOf(false) }
    var showInsertVideoDialog by remember { mutableStateOf(false) }
    var showInsertUrlDialog by remember { mutableStateOf(false) }
    var isImageLinkExpanded by remember { mutableStateOf(false) }
    var isVideoLinkExpanded by remember { mutableStateOf(false) }
    
    var imageInputUrl by remember { mutableStateOf("") }
    var videoInputUrl by remember { mutableStateOf("") }
    var urlInputAddress by remember { mutableStateOf("") }
    var urlInputText by remember { mutableStateOf("") }
    
    var showUrlDialog by remember { mutableStateOf(false) }
    var clickedUrlAddress by remember { mutableStateOf("") }
    val aiEnabled by aiViewModel.aiEnabled.collectAsStateWithLifecycle()
    val pendingAiInsert by aiViewModel.pendingInsert.collectAsStateWithLifecycle()
    var clickedUrlAbsoluteOffset by remember { mutableStateOf(-1) }
    
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var matchRanges by remember { mutableStateOf<List<TextRange>>(emptyList()) }
    var currentMatchIndex by remember { mutableStateOf(0) }
    var searchCaseSensitive by remember { mutableStateOf(false) }
    var searchFullWord by remember { mutableStateOf(false) }
    var showSearchMoreOptions by remember { mutableStateOf(false) }
    val editorFocusRequester = remember { FocusRequester() }

    LaunchedEffect(searchQuery, searchCaseSensitive, searchFullWord, contentValue.text) {
        if (searchQuery.isNotEmpty()) {
            val parseResult = RichTextParser.parseWithMapping(contentValue.text, hideTags = true)
            val cleanText = parseResult.text.text
            val ranges = mutableListOf<TextRange>()
            if (searchFullWord) {
                val escapedQuery = Regex.escape(searchQuery)
                val patternString = "\\b$escapedQuery\\b"
                val options = if (searchCaseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
                try {
                    val regex = Regex(patternString, options)
                    regex.findAll(cleanText).forEach { matchResult ->
                        val cleanStart = matchResult.range.first
                        val cleanEnd = matchResult.range.last + 1
                        val rawStart = parseResult.transformedToOriginal(cleanStart)
                        val rawEnd = parseResult.transformedToOriginal(cleanEnd)
                        ranges.add(TextRange(rawStart, rawEnd))
                    }
                } catch (e: Exception) {
                    var idx = cleanText.indexOf(searchQuery, 0, ignoreCase = !searchCaseSensitive)
                    while (idx != -1) {
                        val cleanStart = idx
                        val cleanEnd = idx + searchQuery.length
                        val rawStart = parseResult.transformedToOriginal(cleanStart)
                        val rawEnd = parseResult.transformedToOriginal(cleanEnd)
                        ranges.add(TextRange(rawStart, rawEnd))
                        idx = cleanText.indexOf(searchQuery, idx + 1, ignoreCase = !searchCaseSensitive)
                    }
                }
            } else {
                var idx = cleanText.indexOf(searchQuery, 0, ignoreCase = !searchCaseSensitive)
                while (idx != -1) {
                    val cleanStart = idx
                    val cleanEnd = idx + searchQuery.length
                        val rawStart = parseResult.transformedToOriginal(cleanStart)
                        val rawEnd = parseResult.transformedToOriginal(cleanEnd)
                        ranges.add(TextRange(rawStart, rawEnd))
                        idx = cleanText.indexOf(searchQuery, idx + 1, ignoreCase = !searchCaseSensitive)
                }
            }
            matchRanges = ranges
            if (currentMatchIndex >= ranges.size) {
                currentMatchIndex = 0
            }
            if (ranges.isNotEmpty()) {
                val currentRange = ranges[currentMatchIndex]
                if (contentValue.selection != currentRange) {
                    contentValue = contentValue.copy(selection = currentRange)
                }
            }
        } else {
            matchRanges = emptyList()
            currentMatchIndex = 0
        }
    }
    
    val saveToHistory: (String) -> Unit = { text ->
        if (historyIndex == -1 || history.getOrNull(historyIndex) != text) {
            while (history.size > historyIndex + 1) {
                history.removeAt(history.size - 1)
            }
            history.add(text)
            historyIndex = history.size - 1
        }
    }

    val insertAtCursor: (String) -> Unit = { tag ->
        val selStart = contentValue.selection.start
        val selEnd = contentValue.selection.end
        val text = contentValue.text
        val newText = text.substring(0, selStart) + tag + text.substring(selEnd)
        val newCursor = selStart + tag.length
        contentValue = TextFieldValue(text = newText, selection = TextRange(newCursor))
        content = newText
        saveToHistory(newText)
    }
    
    val allTags by viewModel.availableTags.collectAsState()
    var selectedNoteTags by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedBgColorId by remember { mutableStateOf<Int?>(null) }
    var selectedBgImagePath by remember { mutableStateOf<String?>(null) }
    
    var isPinned by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }
    var isArchived by remember { mutableStateOf(false) }
    
    var showPaletteSheet by remember { mutableStateOf(false) }
    var showMoreSheet by remember { mutableStateOf(false) }

    var attachments by remember { mutableStateOf<List<Attachment>>(emptyList()) }
    var showVoiceFileSheet by remember { mutableStateOf(false) }

    LaunchedEffect(pendingAiInsert, contentLoaded) {
        val text = pendingAiInsert ?: return@LaunchedEffect
        if (!contentLoaded) return@LaunchedEffect
        val selStart = contentValue.selection.start
        val selEnd = contentValue.selection.end
        val currentText = contentValue.text
        val newText = currentText.substring(0, selStart) + text + currentText.substring(selEnd)
        val newCursor = selStart + text.length
        contentValue = TextFieldValue(text = newText, selection = TextRange(newCursor))
        content = newText
        saveToHistory(newText)
        viewModel.saveNote(
            id = noteId,
            title = title.trim(),
            content = createRawContent(newText.trim(), attachments),
            isEncrypted = isEncrypted,
            tagsList = selectedNoteTags,
            backgroundColor = selectedBgColorId,
            backgroundImagePath = selectedBgImagePath,
            isPinned = isPinned,
            isFavorite = isFavorite,
            isArchived = isArchived
        )
        aiViewModel.clearInsertResult()
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            acquireUriPermission(it)
            selectedBgImagePath = it.toString()
        }
    }

    var pendingCameraType by remember { mutableStateOf<String?>(null) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var cameraVideoUri by remember { mutableStateOf<Uri?>(null) }

    val insertCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            cameraImageUri?.let { uri -> insertAtCursor("<img src=\"$uri\" />") }
        }
    }

    val captureVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success: Boolean ->
        if (success) {
            cameraVideoUri?.let { uri -> insertAtCursor("<video src=\"$uri\" />") }
        }
    }

    val launchCamera: (String) -> Unit = { type ->
        try {
            val ext = if (type == "image") "jpg" else "mp4"
            val prefix = if (type == "image") "img" else "vid"
            val tempFile = File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}.$ext")
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, tempFile)
            if (type == "image") {
                cameraImageUri = uri
                insertCameraLauncher.launch(uri)
            } else {
                cameraVideoUri = uri
                captureVideoLauncher.launch(uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, context.getString(R.string.toast_camera_error) + ": ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingCameraType?.let { launchCamera(it) }
        } else {
            Toast.makeText(context, context.getString(R.string.camera_permission_required), Toast.LENGTH_SHORT).show()
        }
        pendingCameraType = null
    }

    val checkCameraPermissionAndLaunch: (String) -> Unit = { type ->
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        
        if (hasPermission) {
            launchCamera(type)
        } else {
            pendingCameraType = type
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    val insertGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            acquireUriPermission(it)
            insertAtCursor("<img src=\"$it\" />")
        }
    }

    val insertVideoGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            acquireUriPermission(it)
            insertAtCursor("<video src=\"$it\" />")
        }
    }

    var originalNote: Note? by remember { mutableStateOf(null) }
    var hasNavigatingToDrawing by remember { mutableStateOf(false) }

    // Load note data initially, and reactively reload when externally updated
    LaunchedEffect(noteId) {
        if (noteId == 0) return@LaunchedEffect

        suspend fun loadNoteFromList() {
            val match = viewModel.notesList.value.find { it.note.id == noteId }
            if (match != null) {
                originalNote = match.note
                title = match.title

                val (cleanText, parsedAttachments) = parseNoteContentAndAttachments(match.content)
                content = cleanText
                contentValue = TextFieldValue(text = cleanText, selection = TextRange(cleanText.length))
                attachments = parsedAttachments

                history.clear()
                history.add(cleanText)
                historyIndex = 0

                isEncrypted = match.note.isEncrypted
                selectedBgColorId = match.note.backgroundColor
                selectedBgImagePath = match.note.backgroundImagePath
                isPinned = match.note.isPinned
                isFavorite = match.note.isFavorite
                isArchived = match.note.isArchived

                selectedNoteTags = match.note.parseTags()
            }
        }

        // If note not immediately available (race with Room Flow), wait for it
        var match = viewModel.notesList.value.find { it.note.id == noteId }
        if (match == null) {
            match = viewModel.notesList.first { list ->
                list.any { it.note.id == noteId }
            }.find { it.note.id == noteId }
        }
        if (match != null) {
            loadNoteFromList()
            contentLoaded = true
        }

        // React to external modifications (DrawingCanvas, etc.)
        viewModel.noteExternallyUpdated
            .filter { it == noteId }
            .collect {
                loadNoteFromList()
            }
    }

    LaunchedEffect(content) {
        if (content.isNotEmpty()) {
            kotlinx.coroutines.delay(800)
            saveToHistory(content)
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow {
            listOf(content, title, selectedNoteTags.size, selectedBgColorId)
        }
        .debounce(2000)
        .collectLatest {
            if ((title.isNotBlank() || content.isNotBlank()) && noteId != 0) {
                viewModel.saveNote(
                    id = noteId,
                    title = title.trim(),
                    content = createRawContent(content.trim(), attachments),
                    isEncrypted = isEncrypted,
                    tagsList = selectedNoteTags,
                    backgroundColor = selectedBgColorId,
                    backgroundImagePath = selectedBgImagePath,
                    isPinned = isPinned,
                    isFavorite = isFavorite,
                    isArchived = isArchived
                )
            }
        }
    }

    val handleSaveAndExit = {
        if (!hasNavigatingToDrawing && (title.isNotBlank() || content.isNotBlank() || attachments.isNotEmpty())) {
            viewModel.saveNote(
                id = noteId,
                title = title.trim(),
                content = createRawContent(content.trim(), attachments),
                isEncrypted = isEncrypted,
                tagsList = selectedNoteTags,
                backgroundColor = selectedBgColorId,
                backgroundImagePath = selectedBgImagePath,
                isPinned = isPinned,
                isFavorite = isFavorite,
                isArchived = isArchived
            )
            Toast.makeText(context, context.getString(R.string.toast_note_saved), Toast.LENGTH_SHORT).show()
        }
        onBack()
    }

    BackHandler(onBack = handleSaveAndExit)

    Scaffold(
        topBar = {
            CustomTopBar {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = handleSaveAndExit) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                    IconButton(
                        onClick = { isPreviewMode = !isPreviewMode },
                        modifier = Modifier.testTag("toggle_preview_btn")
                    ) {
                        Icon(
                            imageVector = if (isPreviewMode) Icons.Default.Edit else Icons.Default.Visibility,
                            contentDescription = if (isPreviewMode) stringResource(R.string.desc_switch_edit) else stringResource(R.string.desc_switch_preview),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { showMoreSheet = true },
                        modifier = Modifier.testTag("more_note_btn")
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(id = R.string.more_options), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    ) { innerPadding ->
        val isDark = isSystemInDarkTheme()
        val currentBgColor = if (selectedBgColorId != null && selectedBgColorId != 0) {
            getNoteBackgroundColor(selectedBgColorId!!, isDark)
        } else {
            MaterialTheme.colorScheme.background
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(currentBgColor)
                .padding(innerPadding)
                .imePadding()
        ) {
            selectedBgImagePath?.let { bgPath ->
                AsyncImage(
                    model = bgPath,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.22f
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp)
                    .padding(bottom = 72.dp)
            ) {
                if (isPreviewMode) {
                    Text(
                        text = title.ifEmpty { stringResource(R.string.untitled_note) },
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                } else {
                    // Title Outlined State
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(stringResource(id = R.string.label_title)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("note_title_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedNoteTags.isNotEmpty()) {
                    // Horizontal Pill tag tagging selectors
                    Text(stringResource(id = R.string.label_tags) + ":", fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        allTags.filter { selectedNoteTags.contains(it.name) }.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .background(
                                        color = Color(android.graphics.Color.parseColor(tag.colorHex)).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = 1.5.dp,
                                        color = Color(android.graphics.Color.parseColor(tag.colorHex)),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        selectedNoteTags = selectedNoteTags - tag.name
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(tag.name, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(id = R.string.cd_remove_tag),
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (!isPreviewMode) {
                    // Rich Text Formatter Toolbar
                    Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Undo/Redo
                    IconButton(
                        onClick = {
                            if (historyIndex > 0) {
                                historyIndex--
                                val prev = history[historyIndex]
                                contentValue = contentValue.copy(
                                    text = prev,
                                    selection = TextRange(prev.length)
                                )
                                content = prev
                            }
                        },
                        enabled = historyIndex > 0
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = stringResource(id = R.string.rich_undo),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = {
                            if (historyIndex < history.lastIndex) {
                                historyIndex++
                                val next = history[historyIndex]
                                contentValue = contentValue.copy(
                                    text = next,
                                    selection = TextRange(next.length)
                                )
                                content = next
                            }
                        },
                        enabled = historyIndex < history.lastIndex
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = stringResource(id = R.string.rich_redo),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    VerticalDivider(modifier = Modifier.height(24.dp))
                    
                    // Debounced toolbar parse — avoids re-parsing on every keystroke
                    var toolbarParseResult by remember { mutableStateOf(RichTextParser.parseWithMapping(contentValue.text, hideTags = true)) }
                    LaunchedEffect(Unit) {
                        snapshotFlow { contentValue.text }
                            .debounce(80)
                            .collectLatest { text ->
                                toolbarParseResult = RichTextParser.parseWithMapping(text, hideTags = true)
                            }
                    }
                    data class ToolbarState(val activeStyles: Set<String>, val activeFontColor: Color?, val activeBgColor: Color?)
                    val toolbarState = remember(toolbarParseResult, contentValue.selection) {
                        val parsed = toolbarParseResult
                        val cursorIndex = contentValue.selection.start
                        val transformedIndex = parsed.originalToTransformed(cursorIndex)
                        val targetIndex = if (transformedIndex < parsed.text.length) transformedIndex else (transformedIndex - 1).coerceAtLeast(0)
                        val activeStyles = buildSet {
                            for (range in parsed.text.spanStyles) {
                                if (range.start <= targetIndex && targetIndex < range.end) {
                                    if (range.item.fontWeight == FontWeight.Bold) add("b")
                                    if (range.item.fontStyle == FontStyle.Italic) add("i")
                                    if (range.item.textDecoration?.contains(TextDecoration.Underline) == true) add("u")
                                    if (range.item.textDecoration?.contains(TextDecoration.LineThrough) == true) add("s")
                                }
                            }
                        }
                        val activeFontColor = parsed.text.spanStyles.lastOrNull { range ->
                            range.start <= targetIndex && targetIndex < range.end && range.item.color != Color.Unspecified
                        }?.item?.color
                        val activeBgColor = parsed.text.spanStyles.lastOrNull { range ->
                            range.start <= targetIndex && targetIndex < range.end && range.item.background != Color.Unspecified && range.item.background != Color.Transparent
                        }?.item?.background
                        ToolbarState(activeStyles, activeFontColor, activeBgColor)
                    }
                    val activeTextStyles = toolbarState.activeStyles
                    val activeFontColor = toolbarState.activeFontColor
                    val activeBgColor = toolbarState.activeBgColor
                    
                    // Bold, Italic, Underline, Strikethrough Helpers
                    val applyTag: (String) -> Unit = { tag ->
                        val selStart = contentValue.selection.start
                        val selEnd = contentValue.selection.end
                        val text = contentValue.text
                        val newText = if (selStart != selEnd) {
                            val selectedText = text.substring(selStart, selEnd)
                            text.substring(0, selStart) + "<$tag>" + selectedText + "</$tag>" + text.substring(selEnd)
                        } else {
                            text.substring(0, selStart) + "<$tag></$tag>" + text.substring(selEnd)
                        }
                        val newCursor = if (selStart != selEnd) {
                            selEnd + tag.length * 2 + 5
                        } else {
                            selStart + tag.length + 2
                        }
                        contentValue = TextFieldValue(text = newText, selection = TextRange(newCursor))
                        content = newText
                        saveToHistory(newText)
                    }

                    val applyTagWithVal: (String, String) -> Unit = { tag, value ->
                        val selStart = contentValue.selection.start
                        val selEnd = contentValue.selection.end
                        val text = contentValue.text
                        val newText = if (selStart != selEnd) {
                            val selectedText = text.substring(selStart, selEnd)
                            text.substring(0, selStart) + "<$tag=$value>" + selectedText + "</$tag>" + text.substring(selEnd)
                        } else {
                            text.substring(0, selStart) + "<$tag=$value></$tag>" + text.substring(selEnd)
                        }
                        val newCursor = if (selStart != selEnd) {
                            selEnd + tag.length * 2 + value.length + 8
                        } else {
                            selStart + tag.length + value.length + 3
                        }
                        contentValue = TextFieldValue(text = newText, selection = TextRange(newCursor))
                        content = newText
                        saveToHistory(newText)
                    }
                    
                    FilledTonalIconToggleButton(
                        checked = "b" in activeTextStyles,
                        onCheckedChange = { applyTag("b") },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text("B", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    
                    FilledTonalIconToggleButton(
                        checked = "i" in activeTextStyles,
                        onCheckedChange = { applyTag("i") },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text("I", fontStyle = FontStyle.Italic, fontSize = 14.sp)
                    }
                    
                    FilledTonalIconToggleButton(
                        checked = "u" in activeTextStyles,
                        onCheckedChange = { applyTag("u") },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text("U", style = TextStyle(textDecoration = TextDecoration.Underline), fontSize = 14.sp)
                    }
                    
                    FilledTonalIconToggleButton(
                        checked = "s" in activeTextStyles,
                        onCheckedChange = { applyTag("s") },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text("S", style = TextStyle(textDecoration = TextDecoration.LineThrough), fontSize = 14.sp)
                    }

                    // Inline Code Tag Button
                    FilledTonalIconToggleButton(
                        checked = false,
                        onCheckedChange = { applyTag("code") },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = stringResource(id = R.string.rich_inline_code),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Subscript Button
                    FilledTonalIconToggleButton(
                        checked = false,
                        onCheckedChange = { applyTag("sub") },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text("x₂", fontSize = 14.sp)
                    }

                    // Superscript Button
                    FilledTonalIconToggleButton(
                        checked = false,
                        onCheckedChange = { applyTag("sup") },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text("x²", fontSize = 14.sp)
                    }
                    
                    VerticalDivider(modifier = Modifier.height(24.dp))

                    // Heading Selection Dropdown
                    var showHeadingDropdown by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { showHeadingDropdown = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(stringResource(id = R.string.rich_heading), fontSize = 12.sp)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        DropdownMenu(
                            expanded = showHeadingDropdown,
                            onDismissRequest = { showHeadingDropdown = false }
                        ) {
                            listOf("normal", "h1", "h2", "h3").forEach { heading ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                                    text = when (heading) {
                                                "normal" -> stringResource(R.string.normal_text)
                                                "h1" -> stringResource(R.string.heading_1)
                                                "h2" -> stringResource(R.string.heading_2)
                                                "h3" -> stringResource(R.string.heading_3)
                                                else -> heading
                                            },
                                            fontWeight = if (heading == "normal") FontWeight.Normal else FontWeight.Bold,
                                            fontSize = when (heading) {
                                                "normal" -> 15.sp
                                                "h1" -> 18.sp
                                                "h2" -> 16.sp
                                                "h3" -> 14.sp
                                                else -> 14.sp
                                            }
                                        ) 
                                    },
                                    onClick = {
                                        applyTag(heading)
                                        showHeadingDropdown = false
                                    }
                                )
                            }
                        }
                    }
                    
                    // Font Selection Dropdown
                    var showFontDropdown by remember { mutableStateOf(false) }
                    val applyFont: (String) -> Unit = { font ->
                        applyTagWithVal("font", font)
                    }
                    
                    Box {
                        OutlinedButton(
                            onClick = { showFontDropdown = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(stringResource(id = R.string.rich_font_family), fontSize = 12.sp)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        DropdownMenu(
                            expanded = showFontDropdown,
                            onDismissRequest = { showFontDropdown = false }
                        ) {
                            listOf("default", "serif", "monospace", "sans-serif", "cursive").forEach { font ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            text = font.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }, 
                                            fontFamily = when (font) {
                                                "serif" -> FontFamily.Serif
                                                "monospace" -> FontFamily.Monospace
                                                "sans-serif" -> FontFamily.SansSerif
                                                "cursive" -> FontFamily.Cursive
                                                else -> FontFamily.Default
                                            }
                                        ) 
                                    },
                                    onClick = {
                                        applyFont(font)
                                        showFontDropdown = false
                                    }
                                )
                            }
                        }
                    }
                    
                    // Font Size Dropdown
                    var showSizeDropdown by remember { mutableStateOf(false) }
                    
                    Box {
                        OutlinedButton(
                            onClick = { showSizeDropdown = true },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(stringResource(id = R.string.rich_font_size), fontSize = 12.sp)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        DropdownMenu(
                            expanded = showSizeDropdown,
                            onDismissRequest = { showSizeDropdown = false }
                        ) {
                            listOf("default", "12", "14", "16", "18", "20", "24", "28").forEach { size ->
                                DropdownMenuItem(
                                    text = { Text(if (size == "default") stringResource(R.string.text_default) else "${size}sp") },
                                    onClick = {
                                        applyTagWithVal("size", size)
                                        showSizeDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    var showFontColorDialog by remember { mutableStateOf(false) }
                    IconButton(onClick = { showFontColorDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.FormatColorText,
                            contentDescription = stringResource(id = R.string.rich_font_color),
                            modifier = Modifier.size(20.dp),
                            tint = activeFontColor ?: MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (showFontColorDialog) {
                        ColorSelectionDialog(
                            title = stringResource(id = R.string.dialog_font_color_title),
                            onDismiss = { showFontColorDialog = false },
                            onColorSelected = { color ->
                                applyTagWithVal("color", color)
                            }
                        )
                    }

                    // Background Color Picker Trigger Button
                    var showBgColorDialog by remember { mutableStateOf(false) }
                    IconButton(onClick = { showBgColorDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.FormatColorFill,
                            contentDescription = stringResource(id = R.string.rich_bg_color),
                            modifier = Modifier.size(20.dp),
                            tint = activeBgColor ?: MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (showBgColorDialog) {
                        ColorSelectionDialog(
                            title = stringResource(id = R.string.dialog_bg_color_title),
                            onDismiss = { showBgColorDialog = false },
                            onColorSelected = { color ->
                                applyTagWithVal("bg", color)
                            }
                        )
                    }

                    VerticalDivider(modifier = Modifier.height(24.dp))

                    // Remove Formatting Action Button
                    IconButton(
                        onClick = {
                            val selStart = contentValue.selection.start
                            val selEnd = contentValue.selection.end
                            val text = contentValue.text
                            if (selStart == selEnd) {
                                val cleaned = text.replace(Regex("<[^>]+>"), "")
                                contentValue = TextFieldValue(text = cleaned, selection = TextRange(cleaned.length))
                                content = cleaned
                                saveToHistory(cleaned)
                            } else {
                                val before = text.substring(0, selStart)
                                val selected = text.substring(selStart, selEnd)
                                val after = text.substring(selEnd)
                                val cleanedSelected = selected.replace(Regex("<[^>]+>"), "")
                                val newText = before + cleanedSelected + after
                                contentValue = TextFieldValue(text = newText, selection = TextRange(selStart + cleanedSelected.length))
                                content = newText
                                saveToHistory(newText)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatClear,
                            contentDescription = stringResource(id = R.string.rich_remove_format),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    VerticalDivider(modifier = Modifier.height(24.dp))

                    // Apply List helper lambda
                    val applyListTag: (String) -> Unit = { listType ->
                        val selStart = contentValue.selection.start
                        val selEnd = contentValue.selection.end
                        val text = contentValue.text
                        if (selStart != selEnd) {
                            val selectedText = text.substring(selStart, selEnd)
                            val lines = selectedText.split("\n")
                            val formattedLines = lines.map { line ->
                                if (listType == "cl") {
                                    "<item checked=\"false\">$line</item>"
                                } else {
                                    "<li>$line</li>"
                                }
                            }.joinToString("\n")
                            val newText = text.substring(0, selStart) + "<$listType>\n$formattedLines\n</$listType>" + text.substring(selEnd)
                            val newCursor = selStart + newText.length - text.length + (selEnd - selStart)
                            contentValue = TextFieldValue(text = newText, selection = TextRange(newCursor))
                            content = newText
                            saveToHistory(newText)
                        } else {
                            val emptyTag = if (listType == "cl") {
                                "<cl>\n  <item checked=\"false\"></item>\n</cl>"
                            } else {
                                "<$listType>\n  <li></li>\n</$listType>"
                            }
                            val newText = text.substring(0, selStart) + emptyTag + text.substring(selEnd)
                            val newCursor = selStart + emptyTag.indexOf("</")
                            contentValue = TextFieldValue(text = newText, selection = TextRange(newCursor))
                            content = newText
                            saveToHistory(newText)
                        }
                    }

                    // Decrease Indent helper lambda
                    val decreaseIndent: () -> Unit = {
                        val selStart = contentValue.selection.start
                        val selEnd = contentValue.selection.end
                        val text = contentValue.text
                        if (selStart != selEnd) {
                            val selectedText = text.substring(selStart, selEnd)
                            var cleaned = selectedText
                            if (cleaned.startsWith("<indent>") && cleaned.endsWith("</indent>")) {
                                cleaned = cleaned.substring(8, cleaned.length - 9)
                            } else {
                                cleaned = cleaned.replaceFirst("<indent>", "").replaceFirst("</indent>", "")
                            }
                            val newText = text.substring(0, selStart) + cleaned + text.substring(selEnd)
                            contentValue = TextFieldValue(text = newText, selection = TextRange(selStart + cleaned.length))
                            content = newText
                            saveToHistory(newText)
                        } else {
                            val beforeCursor = text.substring(0, selStart)
                            val afterCursor = text.substring(selStart)
                            val lastIndentIdx = beforeCursor.lastIndexOf("<indent>")
                            val lastCloseIndentIdx = beforeCursor.lastIndexOf("</indent>")
                            if (lastIndentIdx != -1 && lastIndentIdx > lastCloseIndentIdx) {
                                val newBefore = beforeCursor.removeRange(lastIndentIdx, lastIndentIdx + 8)
                                val firstCloseIdx = afterCursor.indexOf("</indent>")
                                val newAfter = if (firstCloseIdx != -1) {
                                    afterCursor.removeRange(firstCloseIdx, firstCloseIdx + 9)
                                } else {
                                    afterCursor
                                }
                                val newText = newBefore + newAfter
                                contentValue = TextFieldValue(text = newText, selection = TextRange(newBefore.length))
                                content = newText
                                saveToHistory(newText)
                            }
                        }
                    }

                    // Paste clipboard helper lambda
                    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                    val pasteFromClipboard: () -> Unit = {
                        val selStart = contentValue.selection.start
                        val selEnd = contentValue.selection.end
                        val text = contentValue.text
                        val clipText = clipboardManager.getText()?.text ?: ""
                        if (clipText.isNotEmpty()) {
                            if (RichTextParser.isSecureNotesJson(clipText)) {
                                val defaultTitle = context.getString(R.string.title_imported_note)
                                val (importedTitle, importedContent) = RichTextParser.parseSecureNotesJson(clipText, defaultTitle)
                                title = importedTitle
                                content = importedContent
                                contentValue = TextFieldValue(text = importedContent, selection = TextRange(importedContent.length))
                                saveToHistory(importedContent)
                                Toast.makeText(context, context.getString(R.string.toast_imported), Toast.LENGTH_SHORT).show()
                            } else {
                                val converted = if (clipText.trimStart().startsWith("<") || clipText.contains("</")) {
                                    try { RichTextParser.convertHtmlToSecureNotes(clipText) } catch (e: Exception) { clipText }
                                } else {
                                    clipText
                                }
                                val newText = text.substring(0, selStart) + converted + text.substring(selEnd)
                                val newCursor = selStart + converted.length
                                contentValue = TextFieldValue(text = newText, selection = TextRange(newCursor))
                                content = newText
                                saveToHistory(newText)
                            }
                        }
                    }

                    // Date inserter helper lambda
                    val insertCurrentDate: () -> Unit = {
                        val selStart = contentValue.selection.start
                        val selEnd = contentValue.selection.end
                        val text = contentValue.text
                        val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                        val newText = text.substring(0, selStart) + formattedDate + text.substring(selEnd)
                        val newCursor = selStart + formattedDate.length
                        contentValue = TextFieldValue(text = newText, selection = TextRange(newCursor))
                        content = newText
                        saveToHistory(newText)
                    }

                    // Numbered List Button
                    IconButton(onClick = { applyListTag("ol") }) {
                        Icon(
                            imageVector = Icons.Default.FormatListNumbered,
                            contentDescription = stringResource(id = R.string.rich_numbered_list),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Bulleted List Button
                    IconButton(onClick = { applyListTag("ul") }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                            contentDescription = stringResource(id = R.string.rich_bulleted_list),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Checklist Button
                    IconButton(onClick = { applyListTag("cl") }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.FactCheck,
                            contentDescription = stringResource(id = R.string.rich_checklist),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Code Block Button
                    IconButton(onClick = { applyTag("pre") }) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = stringResource(id = R.string.rich_code_block),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Quote Button
                    IconButton(onClick = { applyTag("quote") }) {
                        Icon(
                            imageVector = Icons.Default.FormatQuote,
                            contentDescription = stringResource(id = R.string.rich_quote),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Increase Indent Button
                    IconButton(onClick = { applyTag("indent") }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.FormatIndentIncrease,
                            contentDescription = stringResource(id = R.string.rich_increase_indent),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Decrease Indent Button
                    IconButton(onClick = { decreaseIndent() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.FormatIndentDecrease,
                            contentDescription = stringResource(id = R.string.rich_decrease_indent),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Insert URL Button
                    IconButton(onClick = {
                        val selStart = contentValue.selection.start
                        val selEnd = contentValue.selection.end
                        val text = contentValue.text
                        if (selStart != selEnd) {
                            urlInputText = text.substring(selStart, selEnd)
                        } else {
                            urlInputText = ""
                        }
                        urlInputAddress = ""
                        showInsertUrlDialog = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = stringResource(id = R.string.rich_insert_url),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Search Button
                    IconButton(onClick = { isSearchActive = !isSearchActive }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(id = R.string.rich_search),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // AI Assistant Button
                    if (aiEnabled) {
                        VerticalDivider(modifier = Modifier.height(24.dp))
                        IconButton(onClick = {
                            aiViewModel.prepareChatForNote(
                                content,
                                contentValue.text.substring(contentValue.selection.start, contentValue.selection.end)
                                    .takeIf { contentValue.selection.start != contentValue.selection.end } ?: "",
                                title
                            )
                            onNavigateToAiChat(noteId)
                        }) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = stringResource(id = R.string.ai_assistant),
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Paste Button
                    IconButton(onClick = { pasteFromClipboard() }) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = stringResource(id = R.string.rich_paste),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Insert Date Button
                    IconButton(onClick = { insertCurrentDate() }) {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = stringResource(id = R.string.rich_insert_date),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Inline Search Bar
                if (isSearchActive) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text(stringResource(id = R.string.rich_search), fontSize = 14.sp) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent
                                    )
                                )
                                
                                if (matchRanges.isNotEmpty()) {
                                    Text(
                                        text = stringResource(id = R.string.search_match_counter, currentMatchIndex + 1, matchRanges.size),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                // 1. Up Button
                                IconButton(
                                    onClick = {
                                        if (matchRanges.isNotEmpty()) {
                                            currentMatchIndex = (currentMatchIndex - 1 + matchRanges.size) % matchRanges.size
                                            contentValue = contentValue.copy(selection = matchRanges[currentMatchIndex])
                                            try {
                                                editorFocusRequester.requestFocus()
                                            } catch (e: Exception) {
                                                Log.e("NoteEditor", "focus request up failed", e)
                                            }
                                        }
                                    },
                                    enabled = matchRanges.isNotEmpty(),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = stringResource(id = R.string.search_up),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // 2. Down Button
                                IconButton(
                                    onClick = {
                                        if (matchRanges.isNotEmpty()) {
                                            currentMatchIndex = (currentMatchIndex + 1) % matchRanges.size
                                            contentValue = contentValue.copy(selection = matchRanges[currentMatchIndex])
                                            try {
                                                editorFocusRequester.requestFocus()
                                            } catch (e: Exception) {
                                                Log.e("NoteEditor", "focus request down failed", e)
                                            }
                                        }
                                    },
                                    enabled = matchRanges.isNotEmpty(),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = stringResource(id = R.string.search_down),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                
                                // 3. More Button
                                IconButton(
                                    onClick = { showSearchMoreOptions = !showSearchMoreOptions },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (showSearchMoreOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = stringResource(id = R.string.search_more),
                                        modifier = Modifier.size(18.dp),
                                        tint = if (showSearchMoreOptions) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                IconButton(
                                    onClick = {
                                        searchQuery = ""
                                        matchRanges = emptyList()
                                        isSearchActive = false
                                        showSearchMoreOptions = false
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.close),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            
                            // Dynamic count status display
                            if (searchQuery.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val statusText = if (matchRanges.isEmpty()) {
                                        stringResource(id = R.string.search_results_empty)
                                    } else {
                                        val isSingleLetter = searchQuery.trim().length == 1
                                        if (isSingleLetter) {
                                            if (matchRanges.size == 1) {
                                                stringResource(id = R.string.search_letter_found_one)
                                            } else {
                                                stringResource(id = R.string.search_letter_found_many, matchRanges.size)
                                            }
                                        } else {
                                            if (matchRanges.size == 1) {
                                                stringResource(id = R.string.search_word_found_one)
                                            } else {
                                                stringResource(id = R.string.search_word_found_many, matchRanges.size)
                                            }
                                        }
                                    }
                                    Text(
                                        text = statusText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (matchRanges.isEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            
                            // More options row: Two checks (Case sensitive, Full word)
                            if (showSearchMoreOptions) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { searchCaseSensitive = !searchCaseSensitive }
                                    ) {
                                        Checkbox(
                                            checked = searchCaseSensitive,
                                            onCheckedChange = { searchCaseSensitive = it }
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = stringResource(id = R.string.search_case_sensitive),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { searchFullWord = !searchFullWord }
                                    ) {
                                        Checkbox(
                                            checked = searchFullWord,
                                            onCheckedChange = { searchFullWord = it }
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = stringResource(id = R.string.search_full_word),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                              // Live dual-pane view (Source Editor on top, parsed live render below)
                if (isPreviewMode) {
                    val currentContentForPreview = remember(content, attachments) {
                        createRawContent(content.trim(), attachments)
                    }
                    val blocks = remember(currentContentForPreview) { buildPreviewBlocks(currentContentForPreview) }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("note_preview_area"),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        SelectionContainer {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (blocks.isEmpty() || (blocks.size == 1 && blocks[0] is NoteContentBlock.TextBlock && (blocks[0] as NoteContentBlock.TextBlock).annotatedString.text.isBlank())) {
                                    Text(
                                        text = stringResource(R.string.label_empty_preview),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                } else {
                                    blocks.forEach { block ->
                                        NoteContentBlockCard(
                                            block = block,
                                            content = currentContentForPreview,
                                            noteId = noteId,
                                            onDeleteBlock = { deletedBlock ->
                                                val onSavedBlock: (List<Attachment>) -> Unit = { updatedAttachments ->
                                                    val raw = createRawContent(content.trim(), updatedAttachments)
                                                    scope.launch { viewModel.saveNote(id = noteId, title = title.trim(), content = raw, isEncrypted = isEncrypted, tagsList = selectedNoteTags, backgroundColor = selectedBgColorId, backgroundImagePath = selectedBgImagePath, isPinned = isPinned, isFavorite = isFavorite, isArchived = isArchived) }
                                                }
                                                when (deletedBlock) {
                                                    is NoteContentBlock.ImageBlock -> {
                                                        val newText = removeMediaFromContent(content, deletedBlock.src, "image")
                                                        content = newText
                                                        contentValue = TextFieldValue(text = newText, selection = TextRange(newText.length))
                                                        saveToHistory(newText)
                                                    }
                                                    is NoteContentBlock.VideoBlock -> {
                                                        val newText = removeMediaFromContent(content, deletedBlock.src, "video")
                                                        content = newText
                                                        contentValue = TextFieldValue(text = newText, selection = TextRange(newText.length))
                                                        saveToHistory(newText)
                                                    }
                                                    is NoteContentBlock.AudioBlock -> {
                                                        val newText = removeMediaFromContent(content, deletedBlock.src, "audio")
                                                        content = newText
                                                        contentValue = TextFieldValue(text = newText, selection = TextRange(newText.length))
                                                        saveToHistory(newText)
                                                    }
                                                    is NoteContentBlock.DrawingBlock -> {
                                                        val target = attachments.find { it.type == "drawing" && it.path == deletedBlock.jsonPath }
                                                        if (target != null) {
                                                            attachments = attachments - target
                                                            onSavedBlock(attachments - target)
                                                        }
                                                    }
                                                    is NoteContentBlock.VoiceBlock -> {
                                                        val target = attachments.find { it.type == "voice" && it.path == deletedBlock.path }
                                                        if (target != null) {
                                                            attachments = attachments - target
                                                            onSavedBlock(attachments - target)
                                                        }
                                                    }
                                                    is NoteContentBlock.FileBlock -> {
                                                        val target = attachments.find { it.type != "drawing" && it.type != "voice" && it.name == deletedBlock.name }
                                                        if (target != null) {
                                                            attachments = attachments - target
                                                            onSavedBlock(attachments - target)
                                                        }
                                                    }
                                                    is NoteContentBlock.TextBlock, is NoteContentBlock.ChecklistItemBlock -> { }
                                                }
                                            },
                                            onNavigateToMediaViewer = onNavigateToMediaViewer,
                                            onNavigateToDrawing = onNavigateToDrawing,
                                            onUrlClicked = { url, rawOffset ->
                                                clickedUrlAddress = url
                                                clickedUrlAbsoluteOffset = rawOffset
                                                showUrlDialog = true
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val visualTransformation = remember(searchQuery, searchCaseSensitive, searchFullWord, currentMatchIndex, isSearchActive) {
                        var cachedText: String? = null
                        var cachedResult: RichTextParser.ParseResult? = null
                        VisualTransformation { text ->
                            if (cachedText != text.text) {
                                cachedText = text.text
                                cachedResult = RichTextParser.parseWithMapping(text.text, hideTags = false, showTagsGray = true)
                            }
                            val parseResult = cachedResult!!
                            val annotated = if (isSearchActive && searchQuery.isNotEmpty()) {
                                highlightMatches(
                                    annotatedString = parseResult.text,
                                    query = searchQuery,
                                    caseSensitive = searchCaseSensitive,
                                    fullWord = searchFullWord,
                                    currentIndex = currentMatchIndex,
                                    highlightColor = Color(0xFFFFF59D), // Light yellow highlight
                                    currentHighlightColor = Color(0xFFFFCC80) // Soft orange for selected
                                )
                            } else {
                                parseResult.text
                            }
                            val offsetMapping = object : OffsetMapping {
                                override fun originalToTransformed(offset: Int): Int {
                                    return parseResult.originalToTransformed(offset)
                                }

                                override fun transformedToOriginal(offset: Int): Int {
                                    return parseResult.transformedToOriginal(offset)
                                }
                            }
                            TransformedText(annotated, offsetMapping)
                        }
                    }
                    OutlinedTextField(
                        value = contentValue,
                        onValueChange = { newValue ->
                            contentValue = newValue
                            content = newValue.text
                        },
                        label = { Text(stringResource(id = R.string.label_content)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .focusRequester(editorFocusRequester)
                            .testTag("note_content_input"),
                        shape = RoundedCornerShape(10.dp),
                        visualTransformation = visualTransformation,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                        )
                    )
                }

                // Insert Image Dialog
                InsertMediaDialog(
                    show = showInsertImageDialog,
                    onDismiss = {
                        showInsertImageDialog = false
                        isImageLinkExpanded = false
                    },
                    titleResId = R.string.dialog_insert_image_title,
                    mediaType = "image",
                    galleryMime = "image/*",
                    cameraIcon = Icons.Default.PhotoCamera,
                    linkExpanded = isImageLinkExpanded,
                    onLinkExpandedChange = { isImageLinkExpanded = it },
                    inputUrl = imageInputUrl,
                    onInputUrlChange = { imageInputUrl = it },
                    onGalleryClick = {
                        insertGalleryLauncher.launch("image/*")
                        showInsertImageDialog = false
                        isImageLinkExpanded = false
                    },
                    onCameraClick = {
                        checkCameraPermissionAndLaunch("image")
                        showInsertImageDialog = false
                        isImageLinkExpanded = false
                    },
                    onInsertLink = { url ->
                        if (url.isNotBlank()) insertAtCursor("<img src=\"$url\" />")
                        imageInputUrl = ""
                        showInsertImageDialog = false
                        isImageLinkExpanded = false
                    }
                )

                // Insert Video Dialog
                InsertMediaDialog(
                    show = showInsertVideoDialog,
                    onDismiss = {
                        showInsertVideoDialog = false
                        isVideoLinkExpanded = false
                    },
                    titleResId = R.string.dialog_insert_video_title,
                    mediaType = "video",
                    galleryMime = "video/*",
                    cameraIcon = Icons.Default.Videocam,
                    linkExpanded = isVideoLinkExpanded,
                    onLinkExpandedChange = { isVideoLinkExpanded = it },
                    inputUrl = videoInputUrl,
                    onInputUrlChange = { videoInputUrl = it },
                    onGalleryClick = {
                        insertVideoGalleryLauncher.launch("video/*")
                        showInsertVideoDialog = false
                        isVideoLinkExpanded = false
                    },
                    onCameraClick = {
                        checkCameraPermissionAndLaunch("video")
                        showInsertVideoDialog = false
                        isVideoLinkExpanded = false
                    },
                    onInsertLink = { url ->
                        if (url.isNotBlank()) insertAtCursor("<video src=\"$url\" />")
                        videoInputUrl = ""
                        showInsertVideoDialog = false
                        isVideoLinkExpanded = false
                    }
                )
                
                // Insert URL Dialog
                if (showInsertUrlDialog) {
                    AlertDialog(
                        onDismissRequest = { showInsertUrlDialog = false },
                        title = { Text(stringResource(id = R.string.dialog_insert_url_title)) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = urlInputAddress,
                                    onValueChange = { urlInputAddress = it },
                                    label = { Text(stringResource(id = R.string.label_url_address)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = urlInputText,
                                    onValueChange = { urlInputText = it },
                                    label = { Text(stringResource(id = R.string.label_url_text)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (urlInputAddress.isNotBlank()) {
                                        val display = urlInputText.ifEmpty { urlInputAddress }
                                        insertAtCursor("<url=$urlInputAddress>$display</url>")
                                    }
                                    urlInputAddress = ""
                                    urlInputText = ""
                                    showInsertUrlDialog = false
                                }
                            ) {
                                Text(stringResource(id = R.string.btn_insert))
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    urlInputAddress = ""
                                    urlInputText = ""
                                    showInsertUrlDialog = false
                                }
                            ) {
                                Text(stringResource(id = R.string.btn_cancel))
                            }
                        }
                    )
                }
                
                // URL Click Action Dialog (Open, Copy, Delete)
                if (showUrlDialog) {
                    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                    AlertDialog(
                        onDismissRequest = { showUrlDialog = false },
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(stringResource(id = R.string.url_dialog_title))
                            }
                        },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = clickedUrlAddress,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(clickedUrlAddress))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, context.getString(R.string.toast_cannot_open_url), Toast.LENGTH_SHORT).show()
                                        }
                                        showUrlDialog = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(id = R.string.url_dialog_open))
                                }
                                
                                OutlinedButton(
                                    onClick = {
                                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(clickedUrlAddress))
                                        Toast.makeText(context, context.getString(R.string.url_dialog_copy) + ": " + clickedUrlAddress, Toast.LENGTH_SHORT).show()
                                        showUrlDialog = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(id = R.string.url_dialog_copy))
                                }
                                
                                OutlinedButton(
                                    onClick = {
                                        val rangeToDelete = findEnclosingUrlTagRange(content, clickedUrlAbsoluteOffset) ?: findEnclosingMarkdownLinkRange(content, clickedUrlAbsoluteOffset)
                                        if (rangeToDelete != null) {
                                            val newContent = content.removeRange(rangeToDelete)
                                            content = newContent
                                            contentValue = TextFieldValue(text = newContent, selection = TextRange(newContent.length))
                                            saveToHistory(newContent)
                                        }
                                        showUrlDialog = false
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(id = R.string.url_dialog_delete),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        },
                        confirmButton = {},
                        dismissButton = {
                            TextButton(onClick = { showUrlDialog = false }) {
                                Text(stringResource(id = R.string.btn_cancel))
                            }
                        }
                    )
                }
                

            }

            // Bottom floating toolbar
            OutlinedCard(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .widthIn(max = 400.dp)
                    .fillMaxWidth(0.9f)
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showPaletteSheet = true },
                        modifier = Modifier.testTag("palette_toolbar_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = stringResource(id = R.string.option_note_styling),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = {
                            if (isSpeaking) {
                                tts?.stop()
                                isSpeaking = false
                            } else {
                                val textToRead = "$title. $content"
                                if (textToRead.isNotBlank()) {
                                    val params = android.os.Bundle().apply {
                                        putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "NoteTTS")
                                    }
                                    tts?.speak(textToRead, TextToSpeech.QUEUE_FLUSH, params, "NoteTTS")
                                    isSpeaking = true
                                } else {
                                    Toast.makeText(context, context.getString(R.string.nothing_to_read), Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.testTag("tts_toolbar_btn")
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isSpeaking) stringResource(R.string.stop_speaking) else stringResource(R.string.read_aloud),
                            tint = if (isSpeaking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = {
                            hasNavigatingToDrawing = true
                            scope.launch {
                                val savedId = viewModel.saveNoteAndGetId(
                                    id = noteId,
                                    title = title.trim(),
                                    content = createRawContent(content.trim(), attachments),
                                    isEncrypted = isEncrypted,
                                    tagsList = selectedNoteTags,
                                    backgroundColor = selectedBgColorId,
                                    backgroundImagePath = selectedBgImagePath,
                                    isPinned = isPinned,
                                    isFavorite = isFavorite,
                                    isArchived = isArchived
                                )
                                onNavigateToDrawing(savedId, null)
                            }
                        },
                        modifier = Modifier.testTag("drawing_toolbar_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gesture,
                            contentDescription = stringResource(R.string.add_drawing),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = { showVoiceFileSheet = true },
                        modifier = Modifier.testTag("attachments_toolbar_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = stringResource(R.string.add_attachment),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (aiEnabled) {
                        IconButton(
                            onClick = {
                                aiViewModel.prepareChatForNote(
                                    content,
                                    contentValue.text.substring(contentValue.selection.start, contentValue.selection.end)
                                        .takeIf { contentValue.selection.start != contentValue.selection.end } ?: "",
                                    title
                                )
                                onNavigateToAiChat(noteId)
                            },
                            modifier = Modifier.testTag("ai_toolbar_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = stringResource(R.string.ai_assistant),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Palette Bottom Sheet
        if (showPaletteSheet) {
            ModalBottomSheet(
                onDismissRequest = { showPaletteSheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(id = R.string.option_note_styling),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Colors section
                    Text(
                        text = stringResource(id = R.string.label_note_color),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        (0..6).forEach { colorId ->
                            val isSelected = selectedBgColorId == colorId || (colorId == 0 && selectedBgColorId == null)
                            val colorLabel = when (colorId) {
                                0 -> stringResource(id = R.string.label_color_none)
                                1 -> stringResource(id = R.string.label_color_blue)
                                2 -> stringResource(id = R.string.label_color_green)
                                3 -> stringResource(id = R.string.label_color_yellow)
                                4 -> stringResource(id = R.string.label_color_pink)
                                5 -> stringResource(id = R.string.label_color_purple)
                                6 -> stringResource(id = R.string.label_color_orange)
                                else -> ""
                            }
                            val circleColor = if (colorId == 0) {
                                MaterialTheme.colorScheme.surfaceVariant
                            } else {
                                getNoteBackgroundColor(colorId, isDark)
                            }

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(circleColor)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        selectedBgColorId = if (colorId == 0) null else colorId
                                    }
                                    .testTag("sheet_color_picker_$colorId"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (colorId == 0) {
                                    Icon(
                                        imageVector = Icons.Default.Block,
                                        contentDescription = colorLabel,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = colorLabel,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (isDark) Color.White else MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Background Image Section
                    Text(
                        text = stringResource(id = R.string.option_select_bg_image),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                imagePickerLauncher.launch("image/*")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(id = R.string.option_select_bg_image), fontSize = 12.sp)
                        }
                        
                        if (selectedBgImagePath != null) {
                            IconButton(
                                onClick = { selectedBgImagePath = null },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.errorContainer, CircleShape)
                                    .size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(id = R.string.option_clear_bg_image),
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                    
                    selectedBgImagePath?.let { bgPath ->
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedCard(
                            modifier = Modifier
                                .size(120.dp, 80.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            AsyncImage(
                                model = bgPath,
                                contentDescription = stringResource(id = R.string.cd_bg_preview),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Tags manager section
                    Text(
                        text = stringResource(id = R.string.label_tags),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        allTags.forEach { tag ->
                            val isTagged = selectedNoteTags.contains(tag.name)
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isTagged) Color(android.graphics.Color.parseColor(tag.colorHex)).copy(alpha = 0.2f) else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = if (isTagged) 2.dp else 1.dp,
                                        color = if (isTagged) Color(android.graphics.Color.parseColor(tag.colorHex)) else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        selectedNoteTags = if (isTagged) {
                                            selectedNoteTags - tag.name
                                        } else {
                                            selectedNoteTags + tag.name
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(tag.name, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }

        if (showMoreSheet) {
            MoreOptionsDialog(
                noteId = noteId,
                originalNote = originalNote,
                title = title,
                content = content,
                isEncrypted = isEncrypted,
                isPinned = isPinned,
                isFavorite = isFavorite,
                isArchived = isArchived,
                isPasswordSet = isPasswordSet,
                onEncryptionChange = { isEncrypted = it },
                onPinChange = { isPinned = it },
                onFavoriteChange = { isFavorite = it },
                onArchiveChange = { isArchived = it },
                onDelete = {
                    originalNote?.let { note ->
                        viewModel.moveToTrash(note)
                        Toast.makeText(context, context.getString(R.string.toast_moved_trash), Toast.LENGTH_SHORT).show()
                        showMoreSheet = false
                        onBack()
                    }
                },
                onDismiss = { showMoreSheet = false }
            )
        }

        // Voice and File Attachment Bottom Sheet
        if (showVoiceFileSheet) {
            var isRecording by remember { mutableStateOf(false) }
            var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
            var recordedFile by remember { mutableStateOf<File?>(null) }
            var isPlayingRecording by remember { mutableStateOf(false) }
            var draftPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
            
            // File picker
            val filePickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                uri?.let { selectedUri ->
                    try {
                        val contentResolver = context.contentResolver
                        var name = "selected_file"
                        contentResolver.query(selectedUri, null, null, null, null)?.use { cursor ->
                            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (cursor.moveToFirst() && nameIndex >= 0) {
                                name = cursor.getString(nameIndex)
                            }
                        }
                        
                        val extension = if (name.contains(".")) name.substringAfterLast(".") else ""
                        val localFile = File(context.filesDir, "file_${noteId}_${System.currentTimeMillis()}.${extension}")
                        contentResolver.openInputStream(selectedUri)?.use { input ->
                            FileOutputStream(localFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        
                        // Attach file
                        attachments = attachments + Attachment(type = "file", path = localFile.absolutePath, name = name)
                        scope.launch {
                            viewModel.saveNote(
                                id = noteId,
                                title = title.trim(),
                                content = createRawContent(content.trim(), attachments),
                                isEncrypted = isEncrypted,
                                tagsList = selectedNoteTags,
                                backgroundColor = selectedBgColorId,
                                backgroundImagePath = selectedBgImagePath,
                                isPinned = isPinned,
                                isFavorite = isFavorite,
                                isArchived = isArchived
                            )
                        }
                        showVoiceFileSheet = false
                        Toast.makeText(context, context.getString(R.string.toast_file_attached), Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, context.getString(R.string.toast_file_select_error) + ": ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // Record Audio Permission launcher
            val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    try {
                        val file = File(context.filesDir, "voice_${noteId}_${System.currentTimeMillis()}.3gp")
                        recordedFile = file
                        
                        val recorderContext = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            context.createAttributionContext("microphone")
                        } else {
                            context
                        }
                        val recorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            MediaRecorder(recorderContext)
                        } else {
                            @Suppress("DEPRECATION")
                            MediaRecorder()
                        }.apply {
                            setAudioSource(MediaRecorder.AudioSource.MIC)
                            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                            setOutputFile(file.absolutePath)
                            prepare()
                            start()
                        }
                        mediaRecorder = recorder
                        isRecording = true
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, context.getString(R.string.toast_recording_error) + ": ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, context.getString(R.string.mic_permission_required), Toast.LENGTH_SHORT).show()
                }
            }

            DisposableEffect(Unit) {
                onDispose {
                    mediaRecorder?.apply {
                        try {
                            stop()
                        } catch (e: Exception) {
                            Log.e("NoteEditor", "media recorder stop failed", e)
                        }
                        release()
                    }
                    draftPlayer?.release()
                }
            }

            ModalBottomSheet(
                onDismissRequest = { showVoiceFileSheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.add_attachment),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Option: Insert Image
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showVoiceFileSheet = false
                                showInsertImageDialog = true
                            },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = stringResource(id = R.string.rich_insert_image),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = stringResource(id = R.string.rich_insert_image),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stringResource(id = R.string.desc_insert_image),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Option: Insert Video
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showVoiceFileSheet = false
                                showInsertVideoDialog = true
                            },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = stringResource(id = R.string.rich_insert_video),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = stringResource(id = R.string.rich_insert_video),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stringResource(id = R.string.desc_insert_video),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Option A: Voice Recording panel
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.voice_note_recorder),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            
                            if (!isRecording && recordedFile == null) {
                                // Default State: Tap to record
                                IconButton(
                                    onClick = {
                                        recordAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                    },
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                        .testTag("start_record_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = stringResource(R.string.tap_to_record),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Text(stringResource(R.string.tap_to_record), style = MaterialTheme.typography.bodySmall)
                            } else if (isRecording) {
                                // Recording State
                                IconButton(
                                    onClick = {
                                        try {
                                            mediaRecorder?.apply {
                                                stop()
                                                release()
                                            }
                                            mediaRecorder = null
                                            isRecording = false
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    },
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(MaterialTheme.colorScheme.errorContainer, CircleShape)
                                        .testTag("stop_record_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = stringResource(id = R.string.cd_stop_recording),
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Text(stringResource(R.string.recording_tap_to_stop), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            } else if (recordedFile != null) {
                                // Review / Draft State
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (isPlayingRecording) {
                                                draftPlayer?.stop()
                                                draftPlayer?.release()
                                                draftPlayer = null
                                                isPlayingRecording = false
                                            } else {
                                                try {
                                                    val player = MediaPlayer().apply {
                                                        setDataSource(recordedFile!!.absolutePath)
                                                        prepare()
                                                        start()
                                                        setOnCompletionListener {
                                                            isPlayingRecording = false
                                                            release()
                                                        }
                                                    }
                                                    draftPlayer = player
                                                    isPlayingRecording = true
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                    Toast.makeText(context, context.getString(R.string.toast_audio_preview_error), Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                                            .testTag("play_preview_btn")
                                    ) {
                                        Icon(
                                            imageVector = if (isPlayingRecording) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = stringResource(id = R.string.cd_preview_voice_note),
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            insertAtCursor("<audio src=\"${recordedFile!!.absolutePath}\" />")

                                            scope.launch {
                                                viewModel.saveNote(
                                                    id = noteId,
                                                    title = title.trim(),
                                                    content = createRawContent(content.trim(), attachments),
                                                    isEncrypted = isEncrypted,
                                                    tagsList = selectedNoteTags,
                                                    backgroundColor = selectedBgColorId,
                                                    backgroundImagePath = selectedBgImagePath,
                                                    isPinned = isPinned,
                                                    isFavorite = isFavorite,
                                                    isArchived = isArchived
                                                )
                                            }
                                            showVoiceFileSheet = false
                                            Toast.makeText(context, context.getString(R.string.toast_voice_note_attached), Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.testTag("attach_voice_btn")
                                    ) {
                                        Text(stringResource(R.string.attach_voice))
                                    }

                                    IconButton(
                                        onClick = {
                                            recordedFile?.delete()
                                            recordedFile = null
                                        },
                                        modifier = Modifier.testTag("delete_recording_btn")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = stringResource(id = R.string.cd_discard_recording), tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }

                    // Option B: File Selection panel
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { filePickerLauncher.launch("*/*") },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = stringResource(R.string.select_external_file), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(stringResource(R.string.select_external_file), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                Text(stringResource(R.string.select_file_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }


    }
}


