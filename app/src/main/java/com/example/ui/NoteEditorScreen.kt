package com.example.ui

import android.content.ClipData
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.R
import com.example.data.ai.AiAction
import com.example.data.ai.RewriteStyle
import com.example.data.model.Attachment
import com.example.data.model.BlockType
import com.example.data.model.DataBlock
import com.example.data.model.DecryptedNote
import com.example.data.model.Note
import com.example.data.model.NoteContentBlock
import com.example.data.model.TableData
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
import com.example.util.ImageUrlResolver
import com.example.util.MediaBlock
import com.example.util.JsonColorizer
import com.example.util.MathRenderer
import com.example.util.RichTextParser
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
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









@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, FlowPreview::class)
@Composable
fun NoteEditorScreen(
    noteId: Int,
    viewModel: NotesViewModel,
    aiViewModel: AiViewModel,
    onBack: () -> Unit,
    onNavigateToDrawing: (Int, String?) -> Unit = { _, _ -> },
    onNavigateToMediaViewer: (String, String) -> Unit = { _, _ -> },
    onNavigateToAiChat: (Int) -> Unit = {},
    onNavigateToNote: (Int) -> Unit = { _ -> }
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
    var isEncrypted by remember { mutableStateOf(isPasswordSet) }
    var blocks by remember { mutableStateOf<List<DataBlock>>(emptyList()) }
    var contentValue by remember { mutableStateOf(TextFieldValue("")) }
    val content by remember { derivedStateOf { DataBlock.serialize(blocks) } }
    val history = remember { mutableStateListOf<List<DataBlock>>() }
    var historyIndex by remember { mutableStateOf(-1) }
    var showSlashMenu by remember { mutableStateOf(false) }
    var slashFilter by remember { mutableStateOf("") }
    var contentLoaded by remember { mutableStateOf(noteId == 0) }
    val pendingTagInsert = remember { mutableStateOf<String?>(null) }
    val pendingInsert = remember { mutableStateOf<String?>(null) }
    val pendingSelection = remember { mutableStateOf<IntRange?>(null) }

    var toolbarActiveBlockIndex by remember { mutableIntStateOf(0) }
    var toolbarActiveCursorOffset by remember { mutableIntStateOf(0) }
    var activeSelection by remember { mutableStateOf(IntRange(0, 0)) }
    var pendingTypingStyle by remember { mutableStateOf<com.example.data.model.TextSegment?>(null) }
    var pendingFocusBlockIndex by remember { mutableIntStateOf(-1) }

    fun setContentValue(v: TextFieldValue) {
        contentValue = v
        activeSelection = v.selection.start..v.selection.end
        pendingSelection.value = v.selection.start..v.selection.end
    }
    
    var showInsertImageDialog by remember { mutableStateOf(false) }
    var showInsertVideoDialog by remember { mutableStateOf(false) }
    var showInsertUrlDialog by remember { mutableStateOf(false) }
    var showInsertTableDialog by remember { mutableStateOf(false) }
    var tableRows by remember { mutableIntStateOf(3) }
    var tableCols by remember { mutableIntStateOf(3) }
    var tableHasHeader by remember { mutableStateOf(true) }
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
    var urlEditRange by remember { mutableStateOf<IntRange?>(null) }
    
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var matchRanges by remember { mutableStateOf<List<TextRange>>(emptyList()) }
    var currentMatchIndex by remember { mutableStateOf(0) }
    var searchCaseSensitive by remember { mutableStateOf(false) }
    var searchFullWord by remember { mutableStateOf(false) }
    var showSearchMoreOptions by remember { mutableStateOf(false) }
    val editorFocusRequester = remember { FocusRequester() }

    // ── AI Integration State ─────────────────────────────────
    var showAiContextSheet by remember { mutableStateOf(false) }
    var showAiPanel by remember { mutableStateOf(false) }
    var aiPromptInput by remember { mutableStateOf("") }
    var aiSelectionStart by remember { mutableIntStateOf(0) }
    var aiSelectionEnd by remember { mutableIntStateOf(0) }
    var aiPendingRewriteStyle by remember { mutableStateOf(RewriteStyle.FORMAL) }
    var aiPendingTargetLanguage by remember { mutableStateOf("en") }
    var showAiStyleSubmenu by remember { mutableStateOf(false) }
    var showAiLangSubmenu by remember { mutableStateOf(false) }

    val inPlaceResult by aiViewModel.inPlaceResult.collectAsStateWithLifecycle()
    val inPlaceStreamingText by aiViewModel.inPlaceStreamingText.collectAsStateWithLifecycle()
    val inPlaceProcessing by aiViewModel.inPlaceProcessing.collectAsStateWithLifecycle()
    val inPlaceAction by aiViewModel.inPlaceAction.collectAsStateWithLifecycle()

    fun executeAiAction(
        action: AiAction,
        style: RewriteStyle = RewriteStyle.FORMAL,
        language: String = "en"
    ) {
        val allContent = blocks.joinToString("\n") {
            com.example.util.RichTextConverter.segmentsToPlainText(it.ensureSegments())
        }
        val text = contentValue.text.substring(aiSelectionStart, aiSelectionEnd)
            .takeIf { aiSelectionStart != aiSelectionEnd } ?: allContent
        aiViewModel.executeInPlace(action, text, style, language)
        showAiContextSheet = false
        showAiStyleSubmenu = false
        showAiLangSubmenu = false
    }

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
                    setContentValue(contentValue.copy(selection = currentRange))
                }
            }
        } else {
            matchRanges = emptyList()
            currentMatchIndex = 0
        }
    }
    
    fun saveBlocksToHistory() {
        if (historyIndex == -1 || history.getOrNull(historyIndex) != blocks) {
            while (history.size > historyIndex + 1) {
                history.removeAt(history.size - 1)
            }
            history.add(blocks.toList())
            historyIndex = history.size - 1
        }
    }

    fun activeSegments(): List<com.example.data.model.TextSegment> =
        blocks.getOrNull(toolbarActiveBlockIndex)?.ensureSegments() ?: emptyList()

    val commitSegments: (List<com.example.data.model.TextSegment>) -> Unit = { newSegs ->
        if (toolbarActiveBlockIndex in blocks.indices) {
            val updated = blocks[toolbarActiveBlockIndex].copy(
                content = "",
                richTextJson = com.example.data.model.TextSegment.serialize(newSegs)
            )
            blocks = blocks.toMutableList().apply { set(toolbarActiveBlockIndex, updated) }
            saveBlocksToHistory()
        }
    }

    fun commitSegmentsWithSelection(newSegs: List<com.example.data.model.TextSegment>, cursor: Int) {
        commitSegments(newSegs)
        val plain = com.example.util.RichTextConverter.segmentsToPlainText(newSegs)
        val clamped = cursor.coerceIn(0, plain.length)
        contentValue = TextFieldValue(text = plain, selection = TextRange(clamped))
        activeSelection = clamped..clamped
        pendingSelection.value = clamped..clamped
    }

    fun syncActiveBlock() {
        if (toolbarActiveBlockIndex !in blocks.indices) return
        val block = blocks[toolbarActiveBlockIndex]
        val segs = block.ensureSegments()
        val segPlain = com.example.util.RichTextConverter.segmentsToPlainText(segs)
        val text = contentValue.text
        if (segPlain == text) return
        val prefix = segPlain.commonPrefixWith(text).length
        var oldEnd = segPlain.length
        var newEnd = text.length
        while (oldEnd > prefix && newEnd > prefix && segPlain[oldEnd - 1] == text[newEnd - 1]) {
            oldEnd--
            newEnd--
        }
        val newSegs = com.example.util.RichTextConverter.replaceTextRange(segs, prefix, oldEnd, text.substring(prefix, newEnd))
        commitSegments(newSegs)
    }

    fun switchActiveBlock(newIndex: Int) {
        syncActiveBlock()
        toolbarActiveBlockIndex = newIndex
        if (newIndex in blocks.indices) {
            val plain = com.example.util.RichTextConverter.segmentsToPlainText(blocks[newIndex].ensureSegments())
            contentValue = TextFieldValue(text = plain, selection = TextRange(plain.length))
            activeSelection = plain.length..plain.length
        }
    }

    fun applyBlocksChange(newBlocks: List<DataBlock>) {
        val newIdx = toolbarActiveBlockIndex.coerceIn(0, (newBlocks.size - 1).coerceAtLeast(0))
        blocks = newBlocks
        toolbarActiveBlockIndex = newIdx
        val segs = blocks.getOrNull(newIdx)?.ensureSegments() ?: emptyList<com.example.data.model.TextSegment>()
        val blockText = com.example.util.RichTextConverter.segmentsToPlainText(segs)
        contentValue = TextFieldValue(text = blockText, selection = TextRange(blockText.length))
        activeSelection = blockText.length..blockText.length
        saveBlocksToHistory()
    }

    var imageDialogMode by remember { mutableIntStateOf(-1) }

    var videoDialogMode by remember { mutableIntStateOf(-1) }

    fun insertVideoBlock(src: String) {
        showSlashMenu = false
        val currentIdx = toolbarActiveBlockIndex.coerceIn(0, blocks.size)
        val currentBlock = blocks.getOrNull(currentIdx)
        val replaceInPlace = currentBlock
            ?.let { com.example.util.RichTextConverter.segmentsToPlainText(it.ensureSegments()).isBlank() } == true
        val videoBlock = DataBlock(type = BlockType.VIDEO, content = src)
        val newBlocks = blocks.toMutableList()
        val idx = if (replaceInPlace) currentIdx else (currentIdx + 1).coerceAtMost(blocks.size)
        if (replaceInPlace) {
            newBlocks[currentIdx] = videoBlock
        } else {
            newBlocks.add(idx, videoBlock)
        }
        blocks = newBlocks
        toolbarActiveBlockIndex = idx.coerceAtMost(newBlocks.size - 1)
        contentValue = TextFieldValue(text = "", selection = TextRange(0))
        activeSelection = 0..0
        saveBlocksToHistory()
    }

    fun applyVideoSelection(src: String) {
        val replaceIdx = videoDialogMode
        videoDialogMode = -1
        if (replaceIdx >= 0 && replaceIdx in blocks.indices) {
            val newBlocks = blocks.toMutableList()
            newBlocks[replaceIdx] = blocks[replaceIdx].copy(content = src)
            applyBlocksChange(newBlocks)
        } else {
            insertVideoBlock(src)
        }
    }

    fun insertImageBlock(src: String, linkUrl: String? = null) {
        showSlashMenu = false
        val currentIdx = toolbarActiveBlockIndex.coerceIn(0, blocks.size)
        val currentBlock = blocks.getOrNull(currentIdx)
        val replaceInPlace = currentBlock
            ?.let { com.example.util.RichTextConverter.segmentsToPlainText(it.ensureSegments()).isBlank() } == true
        val meta = if (linkUrl != null) mapOf("linkUrl" to linkUrl) else emptyMap()
        val imageBlock = DataBlock(type = BlockType.IMAGE, content = src, meta = meta)
        val newBlocks = blocks.toMutableList()
        val idx = if (replaceInPlace) currentIdx else (currentIdx + 1).coerceAtMost(blocks.size)
        if (replaceInPlace) {
            newBlocks[currentIdx] = imageBlock
        } else {
            newBlocks.add(idx, imageBlock)
        }
        blocks = newBlocks
        toolbarActiveBlockIndex = idx.coerceAtMost(newBlocks.size - 1)
        contentValue = TextFieldValue(text = "", selection = TextRange(0))
        activeSelection = 0..0
        saveBlocksToHistory()
    }

    fun insertDrawingBlock() {
        showSlashMenu = false
        val currentIdx = toolbarActiveBlockIndex.coerceIn(0, blocks.size)
        val currentBlock = blocks.getOrNull(currentIdx)
        val replaceInPlace = currentBlock
            ?.let { com.example.util.RichTextConverter.segmentsToPlainText(it.ensureSegments()).isBlank() } == true
        val drawingBlock = DataBlock(
            type = BlockType.DRAWING,
            content = "",
            meta = mapOf("wysiwyg" to "true")
        )
        val newBlocks = blocks.toMutableList()
        val idx = if (replaceInPlace) currentIdx else (currentIdx + 1).coerceAtMost(blocks.size)
        if (replaceInPlace) {
            newBlocks[currentIdx] = drawingBlock
        } else {
            newBlocks.add(idx, drawingBlock)
        }
        blocks = newBlocks
        toolbarActiveBlockIndex = idx.coerceAtMost(newBlocks.size - 1)
        contentValue = TextFieldValue(text = "", selection = TextRange(0))
        activeSelection = 0..0
        saveBlocksToHistory()
    }

    fun applyImageSelection(src: String) {
        val replaceIdx = imageDialogMode
        imageDialogMode = -1
        scope.launch {
            val resolved = ImageUrlResolver.resolveImageUrl(src)
            if (replaceIdx >= 0 && replaceIdx in blocks.indices) {
                val newBlocks = blocks.toMutableList()
                newBlocks[replaceIdx] = blocks[replaceIdx].copy(content = resolved)
                applyBlocksChange(newBlocks)
            } else {
                insertImageBlock(resolved)
            }
        }
    }

    fun moveActiveBlockUp() {
        val idx = toolbarActiveBlockIndex
        if (idx in 1 until blocks.size) {
            val newBlocks = blocks.toMutableList()
            val item = newBlocks.removeAt(idx)
            newBlocks.add(idx - 1, item)
            toolbarActiveBlockIndex = idx - 1
            applyBlocksChange(newBlocks)
        }
    }

    fun moveActiveBlockDown() {
        val idx = toolbarActiveBlockIndex
        if (idx in 0 until blocks.size - 1) {
            val newBlocks = blocks.toMutableList()
            val item = newBlocks.removeAt(idx)
            newBlocks.add(idx + 1, item)
            toolbarActiveBlockIndex = idx + 1
            applyBlocksChange(newBlocks)
        }
    }

    fun deleteActiveBlock() {
        val idx = toolbarActiveBlockIndex
        if (idx in blocks.indices && blocks.size > 1) {
            val newBlocks = blocks.toMutableList()
            newBlocks.removeAt(idx)
            applyBlocksChange(newBlocks)
        }
    }

    fun convertActiveBlock(type: BlockType, meta: Map<String, String> = emptyMap()) {
        val idx = toolbarActiveBlockIndex
        if (idx in blocks.indices) {
            val block = blocks[idx]
            val newBlocks = blocks.toMutableList().apply {
                set(idx, block.copy(type = type, meta = meta))
            }
            applyBlocksChange(newBlocks)
        }
    }

    val insertAtCursor: (String) -> Unit = { tag ->
        pendingInsert.value = tag
    }

    val handleUrlClick: (String, Int) -> Unit = { url, _ ->
        if (url.startsWith("note://")) {
            url.substringAfter("note://").toIntOrNull()?.let { onNavigateToNote(it) }
        } else if (url.startsWith("http://") || url.startsWith("https://")) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.toast_cannot_open_url), Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    val allTags by viewModel.availableTags.collectAsState()
    val allNotes by viewModel.notesList.collectAsState()

    fun moveBlockToAnotherNote(index: Int, targetNoteId: Int) {
        if (index !in blocks.indices) return
        val target = allNotes.find { it.note.id == targetNoteId } ?: return
        val moved = blocks[index]
        val (textPart, targetAttachments) = parseNoteContentAndAttachments(target.content)
        val targetBlocks = (com.example.util.RichTextConverter.contentToBlocks(textPart)
            ?: DataBlock.migrateLegacyContent(textPart))
            .let { DataBlock.migrateToSegments(it).first }
            .toMutableList()
        targetBlocks.add(moved)
        viewModel.saveNote(
            id = target.note.id,
            title = target.title,
            content = createRawContent(DataBlock.serialize(targetBlocks), targetAttachments),
            isEncrypted = target.note.isEncrypted,
            tagsList = target.note.parseTags(),
            backgroundColor = target.note.backgroundColor,
            backgroundImagePath = target.note.backgroundImagePath,
            isPinned = target.note.isPinned,
            isFavorite = target.note.isFavorite,
            isArchived = target.note.isArchived
        )
        val newBlocks = blocks.toMutableList()
        newBlocks.removeAt(index)
        applyBlocksChange(newBlocks)
        Toast.makeText(context, context.getString(R.string.toast_block_moved, target.title), Toast.LENGTH_SHORT).show()
    }

    var selectedNoteTags by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedBgColorId by remember { mutableStateOf<Int?>(null) }
    var selectedBgImagePath by remember { mutableStateOf<String?>(null) }
    
    var isPinned by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }
    var isArchived by remember { mutableStateOf(false) }
    
    var showPaletteSheet by remember { mutableStateOf(false) }
    var showMoreSheet by remember { mutableStateOf(false) }

    var showNoteLinkPicker by remember { mutableStateOf(false) }
    var pageLinkTargetBlockIndex by remember { mutableIntStateOf(-1) }
    var inlineLinkMode by remember { mutableStateOf(false) }

    var moveBlockIndex by remember { mutableIntStateOf(-1) }
    var showMoveBlockPicker by remember { mutableStateOf(false) }

    var showEquationDialog by remember { mutableStateOf(false) }
    var equationInput by remember { mutableStateOf("") }

    var attachments by remember { mutableStateOf<List<Attachment>>(emptyList()) }
    var showVoiceFileSheet by remember { mutableStateOf(false) }

    // Replace selection with AI in-place result
    LaunchedEffect(inPlaceResult, contentLoaded) {
        val result = inPlaceResult ?: return@LaunchedEffect
        if (!contentLoaded) return@LaunchedEffect
        val selStart = aiSelectionStart
        val selEnd = aiSelectionEnd
        val currentText = contentValue.text
        val newText = currentText.substring(0, selStart) + result + currentText.substring(selEnd)
        val newCursor = selStart + result.length
        setContentValue(TextFieldValue(text = newText, selection = TextRange(newCursor)))
        syncActiveBlock(); saveBlocksToHistory()
        val saveJson = DataBlock.serialize(blocks)
        val saveContent = if (attachments.isEmpty()) saveJson else createRawContent(saveJson, attachments)
        viewModel.saveNote(
            id = noteId,
            title = title.trim(),
            content = saveContent,
            isEncrypted = isEncrypted,
            tagsList = selectedNoteTags,
            backgroundColor = selectedBgColorId,
            backgroundImagePath = selectedBgImagePath,
            isPinned = isPinned,
            isFavorite = isFavorite,
            isArchived = isArchived
        )
        aiViewModel.clearInPlaceResult()
    }

    // Level 3: Streaming insertion — animate segment insertion char by char
    LaunchedEffect(pendingAiInsert, contentLoaded) {
        val text = pendingAiInsert ?: return@LaunchedEffect
        if (!contentLoaded || text.isEmpty()) return@LaunchedEffect
        val segs = activeSegments()
        if (segs.isEmpty()) return@LaunchedEffect
        val plain = com.example.util.RichTextConverter.segmentsToPlainText(segs)
        val insertFrom = contentValue.selection.start.coerceIn(0, plain.length)
        val insertEnd = contentValue.selection.end.coerceIn(0, plain.length).coerceAtLeast(insertFrom)
        var currentSegs = segs
        for (i in text.indices) {
            val chunk = text.substring(0, i + 1)
            currentSegs = com.example.util.RichTextConverter.replaceTextRange(currentSegs, insertFrom, insertFrom + i, chunk)
            if (toolbarActiveBlockIndex in blocks.indices) {
                val updated = blocks[toolbarActiveBlockIndex].copy(
                    content = "",
                    richTextJson = com.example.data.model.TextSegment.serialize(currentSegs)
                )
                blocks = blocks.toMutableList().apply { set(toolbarActiveBlockIndex, updated) }
            }
            kotlinx.coroutines.delay(15)
        }
        commitSegmentsWithSelection(currentSegs, insertFrom + text.length)
        val saveJson = DataBlock.serialize(blocks)
        val saveContent = if (attachments.isEmpty()) saveJson else createRawContent(saveJson, attachments)
        viewModel.saveNote(
            id = noteId,
            title = title.trim(),
            content = saveContent,
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
            cameraImageUri?.let { uri -> applyImageSelection(uri.toString()) }
        }
    }

    val captureVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success: Boolean ->
        if (success) {
            cameraVideoUri?.let { uri -> applyVideoSelection(uri.toString()) }
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
            applyImageSelection(it.toString())
        }
    }

    val insertVideoGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            acquireUriPermission(it)
            applyVideoSelection(it.toString())
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

                val (rawText, parsedAttachments) = parseNoteContentAndAttachments(match.content)
                attachments = parsedAttachments

                val parsedBlocks = DataBlock.deserialize(rawText)
                    ?: DataBlock.migrateLegacyContent(rawText)

                // Migración one-shot: bloques de texto legacy → richTextJson como única fuente.
                val (migratedBlocks, needsMigration) = DataBlock.migrateToSegments(parsedBlocks)
                blocks = migratedBlocks

                if (needsMigration) {
                    val migratedJson = DataBlock.serialize(migratedBlocks)
                    val migratedContent = createRawContent(migratedJson, parsedAttachments)
                    viewModel.saveNoteAndGetId(
                        id = match.note.id,
                        title = match.title,
                        content = migratedContent,
                        isEncrypted = match.note.isEncrypted,
                        tagsList = match.note.parseTags(),
                        backgroundColor = match.note.backgroundColor,
                        backgroundImagePath = match.note.backgroundImagePath,
                        isPinned = match.note.isPinned,
                        isFavorite = match.note.isFavorite,
                        isArchived = match.note.isArchived,
                        categoryId = match.note.categoryId,
                        isDeleted = match.note.isDeleted,
                        deletedAt = match.note.deletedAt,
                        lastModified = match.note.lastModified,
                        salt = match.note.salt,
                        iv = match.note.iv
                    )
                }

                // Self-heal: drawings saved by DrawingCanvas appear as attachments (block-JSON
                // notes). Insert a DRAWING block for any drawing attachment not yet in blocks,
                // at the block the user was editing when they opened the canvas.
                val pendingDrawingIndex = viewModel.pendingDrawingInsertIndex
                viewModel.pendingDrawingInsertIndex = null
                val blockDrawingPaths = blocks
                    .filter { it.type == BlockType.DRAWING }
                    .mapNotNull { it.content }
                    .toSet()
                val newDrawings = attachments.filter { it.type == "drawing" && it.path !in blockDrawingPaths }
                if (newDrawings.isNotEmpty()) {
                    val insertAt = pendingDrawingIndex?.coerceIn(0, blocks.size) ?: blocks.size
                    val list = blocks.toMutableList()
                    var offset = insertAt
                    newDrawings.forEach { drawing ->
                        list.add(offset, DataBlock(type = BlockType.DRAWING, content = drawing.path, meta = mapOf("previewPath" to drawing.name)))
                        offset++
                    }
                    blocks = list
                    val selfHealedContent = createRawContent(DataBlock.serialize(list), attachments)
                    viewModel.saveNoteAndGetId(
                        id = match.note.id,
                        title = match.title,
                        content = selfHealedContent,
                        isEncrypted = match.note.isEncrypted,
                        tagsList = match.note.parseTags(),
                        backgroundColor = match.note.backgroundColor,
                        backgroundImagePath = match.note.backgroundImagePath,
                        isPinned = match.note.isPinned,
                        isFavorite = match.note.isFavorite,
                        isArchived = match.note.isArchived,
                        categoryId = match.note.categoryId,
                        isDeleted = match.note.isDeleted,
                        deletedAt = match.note.deletedAt,
                        lastModified = match.note.lastModified,
                        salt = match.note.salt,
                        iv = match.note.iv
                    )
                }

                if (blocks.isNotEmpty()) {
                    val blockPlain = com.example.util.RichTextConverter.segmentsToPlainText(blocks[0].ensureSegments())
                    contentValue = TextFieldValue(text = blockPlain, selection = TextRange(blockPlain.length))
                    toolbarActiveBlockIndex = 0
                }

                history.clear()
                history.add(blocks.toList())
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

    LaunchedEffect(contentValue.text) {
        if (contentValue.text.isNotEmpty()) {
            kotlinx.coroutines.delay(800)
            syncActiveBlock(); saveBlocksToHistory()
        }
    }

    LaunchedEffect(contentValue.text, contentValue.selection) {
        val text = contentValue.text
        val cursor = contentValue.selection.start
        if (text.startsWith("/") && cursor in 1..text.length && !text.substring(0, cursor).contains(" ")) {
            if (!showSlashMenu) showSlashMenu = true
            slashFilter = text.substring(1, cursor)
        } else {
            if (showSlashMenu) showSlashMenu = false
        }
    }

    fun handleSlashSelect(block: DataBlock) {
        showSlashMenu = false
        val currentIdx = toolbarActiveBlockIndex.coerceIn(0, blocks.size)
        val currentBlock = blocks.getOrNull(currentIdx)
        val replaceInPlace = currentBlock
            ?.ensureSegments()
            ?.let { com.example.util.RichTextConverter.segmentsToPlainText(it) }
            ?.startsWith("/") == true
        val newBlocks = blocks.toMutableList()
        val idx = if (replaceInPlace) currentIdx else (currentIdx + 1).coerceAtMost(blocks.size)
        if (replaceInPlace) {
            newBlocks[currentIdx] = block.copy(content = block.content)
        } else {
            newBlocks.add(idx, block)
            pendingFocusBlockIndex = idx
        }
        blocks = newBlocks
        val blockPlain = com.example.util.RichTextConverter.segmentsToPlainText(block.ensureSegments())
        contentValue = TextFieldValue(text = blockPlain, selection = TextRange(blockPlain.length))
        activeSelection = blockPlain.length..blockPlain.length
        toolbarActiveBlockIndex = idx.coerceAtMost(newBlocks.size - 1)
        saveBlocksToHistory()
    }

    val contentForSave: () -> String = {
        val jsonContent = DataBlock.serialize(blocks)
        if (attachments.isEmpty()) jsonContent
        else createRawContent(jsonContent, attachments)
    }

    fun handleBlockCommand(cmd: BlockCommand) {
        showSlashMenu = false
        fun clearSlashPlaceholder() {
            val currentIdx = toolbarActiveBlockIndex.coerceIn(0, blocks.size)
            val currentBlock = blocks.getOrNull(currentIdx) ?: return
            val plain = com.example.util.RichTextConverter.segmentsToPlainText(currentBlock.ensureSegments())
            if (plain.startsWith("/")) {
                val newBlocks = blocks.toMutableList()
                newBlocks[currentIdx] = currentBlock.copy(
                    content = "",
                    richTextJson = com.example.data.model.TextSegment.serialize(emptyList())
                )
                blocks = newBlocks
                contentValue = TextFieldValue(text = "")
            }
        }
        when (cmd.action) {
            BlockAction.NONE -> cmd.blockType?.let { type ->
                handleSlashSelect(DataBlock(type = type, content = cmd.defaultContent, meta = cmd.meta))
            }
            BlockAction.URL_DIALOG -> {
                clearSlashPlaceholder()
                val ss = contentValue.selection.start
                val se = contentValue.selection.end
                urlInputText = if (ss != se) contentValue.text.substring(ss, se) else ""
                urlInputAddress = ""
                showInsertUrlDialog = true
            }
            BlockAction.TABLE_DIALOG -> {
                clearSlashPlaceholder()
                showInsertTableDialog = true
            }
            BlockAction.IMAGE_DIALOG -> {
                clearSlashPlaceholder()
                imageDialogMode = -1
                showInsertImageDialog = true
            }
            BlockAction.VIDEO_DIALOG -> {
                clearSlashPlaceholder()
                videoDialogMode = -1
                showInsertVideoDialog = true
            }
            BlockAction.VOICE_FILE_DIALOG -> {
                clearSlashPlaceholder()
                showVoiceFileSheet = true
            }
            BlockAction.DRAWING_DIALOG -> {
                clearSlashPlaceholder()
                insertDrawingBlock()
            }
            BlockAction.INSERT_PAGE -> scope.launch {
                syncActiveBlock()
                val newId = viewModel.saveNoteAndGetId(
                    id = 0,
                    title = "",
                    content = "",
                    isEncrypted = isEncrypted,
                    tagsList = selectedNoteTags
                )
                if (newId != 0) {
                    handleSlashSelect(DataBlock(type = BlockType.PAGE, content = "", meta = mapOf("noteId" to newId.toString())))
                    if (noteId != 0) {
                        viewModel.saveNoteAndGetId(
                            id = noteId,
                            title = title.trim(),
                            content = contentForSave(),
                            isEncrypted = isEncrypted,
                            tagsList = selectedNoteTags,
                            backgroundColor = selectedBgColorId,
                            backgroundImagePath = selectedBgImagePath,
                            isPinned = isPinned,
                            isFavorite = isFavorite,
                            isArchived = isArchived
                        )
                    }
                    onNavigateToNote(newId)
                }
            }
            BlockAction.LINK_PAGE -> {
                clearSlashPlaceholder()
                inlineLinkMode = false
                pageLinkTargetBlockIndex = -1
                showNoteLinkPicker = true
            }
            BlockAction.LINK_INLINE -> {
                clearSlashPlaceholder()
                inlineLinkMode = true
                pageLinkTargetBlockIndex = -1
                showNoteLinkPicker = true
            }
            BlockAction.EQUATION_DIALOG -> {
                clearSlashPlaceholder()
                equationInput = ""
                showEquationDialog = true
            }
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow {
            listOf(contentValue.text, content, title, selectedNoteTags.size, selectedBgColorId)
        }
        .debounce(2000)
        .collectLatest {
            syncActiveBlock()
            if ((title.isNotBlank() || content.isNotBlank()) && noteId != 0) {
                viewModel.saveNote(
                    id = noteId,
                    title = title.trim(),
                    content = contentForSave(),
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
                content = contentForSave(),
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
                .imePadding(),
            contentAlignment = Alignment.BottomCenter
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

            var toolbarParseResult by remember { mutableStateOf(com.example.util.RichTextConverter.parseResultFor(emptyList())) }
            var showTextBgColorSheet by remember { mutableStateOf(false) }
            var showMoreFormattingSheet by remember { mutableStateOf(false) }
            var convertBlockMode by remember { mutableStateOf(false) }
            var showFontSizeSheet by remember { mutableStateOf(false) }
            val isKeyboardVisible = WindowInsets.isImeVisible
            val keyboardController = LocalSoftwareKeyboardController.current
            data class ToolbarState(val activeStyles: Set<String>, val activeFontColor: Color?, val activeBgColor: Color?)
            val toolbarState = remember(activeSegments(), activeSelection, toolbarActiveBlockIndex, pendingTypingStyle) {
                val segs = activeSegments()
                val cursor = activeSelection.first
                val activeStyles = mutableSetOf<String>()
                var activeFontColor: Color? = null
                var activeBgColor: Color? = null
                when (blocks.getOrNull(toolbarActiveBlockIndex)?.type) {
                    BlockType.HEADING1 -> activeStyles.add("h1")
                    BlockType.HEADING2 -> activeStyles.add("h2")
                    BlockType.HEADING3 -> activeStyles.add("h3")
                    BlockType.HEADING4 -> activeStyles.add("h4")
                    else -> {}
                }
                var pos = 0
                var target: com.example.data.model.TextSegment? = null
                val totalLen = segs.sumOf { it.text.length }
                if (cursor < totalLen || pendingTypingStyle != null) {
                    for (seg in segs) {
                        val segEnd = pos + seg.text.length
                        if (pos <= cursor && cursor <= segEnd) {
                            target = seg
                            break
                        }
                        pos = segEnd
                    }
                }
                val seg = target
                if (seg != null) {
                    if (seg.bold) activeStyles.add("b")
                    if (seg.italic) activeStyles.add("i")
                    if (seg.underline) activeStyles.add("u")
                    if (seg.strikethrough) activeStyles.add("s")
                    if (seg.code) activeStyles.add("code")
                    when (seg.baseline) {
                        com.example.data.model.TextBaseline.SUBSCRIPT -> activeStyles.add("sub")
                        com.example.data.model.TextBaseline.SUPERSCRIPT -> activeStyles.add("sup")
                        else -> {}
                    }
                    if (seg.colorHex != null) activeFontColor = com.example.util.JsonColorizer.parseColor(seg.colorHex)
                    if (seg.bgColorHex != null) activeBgColor = com.example.util.JsonColorizer.parseColor(seg.bgColorHex)
                }
                if (activeSelection.first == activeSelection.last) {
                    pendingTypingStyle?.let { p ->
                        if (p.bold) activeStyles.add("b")
                        if (p.italic) activeStyles.add("i")
                        if (p.underline) activeStyles.add("u")
                        if (p.strikethrough) activeStyles.add("s")
                        if (p.code) activeStyles.add("code")
                        when (p.baseline) {
                            com.example.data.model.TextBaseline.SUBSCRIPT -> activeStyles.add("sub")
                            com.example.data.model.TextBaseline.SUPERSCRIPT -> activeStyles.add("sup")
                            else -> {}
                        }
                        if (p.colorHex != null) activeFontColor = com.example.util.JsonColorizer.parseColor(p.colorHex)
                        if (p.bgColorHex != null) activeBgColor = com.example.util.JsonColorizer.parseColor(p.bgColorHex)
                    }
                }
                ToolbarState(activeStyles, activeFontColor, activeBgColor)
            }
            val activeTextStyles = toolbarState.activeStyles
            val activeFontColor = toolbarState.activeFontColor
            val activeBgColor = toolbarState.activeBgColor

            fun styleApplies(tag: String, seg: com.example.data.model.TextSegment): Boolean = when (tag) {
                "b" -> seg.bold
                "i" -> seg.italic
                "u" -> seg.underline
                "s" -> seg.strikethrough
                "code" -> seg.code
                "sub" -> seg.baseline == com.example.data.model.TextBaseline.SUBSCRIPT
                "sup" -> seg.baseline == com.example.data.model.TextBaseline.SUPERSCRIPT
                "mark" -> seg.bgColorHex == "FFEB3B"
                "small" -> seg.fontSizeSp != null && seg.fontSizeSp < 14f
                "kbd", "var", "samp" -> seg.code
                "quote" -> seg.italic
                else -> false
            }

            fun applyTransformForTag(tag: String, seg: com.example.data.model.TextSegment): com.example.data.model.TextSegment = when (tag) {
                "b" -> seg.copy(bold = true)
                "i" -> seg.copy(italic = true)
                "u" -> seg.copy(underline = true)
                "s" -> seg.copy(strikethrough = true)
                "code" -> seg.copy(code = true)
                "sub" -> seg.copy(baseline = com.example.data.model.TextBaseline.SUBSCRIPT)
                "sup" -> seg.copy(baseline = com.example.data.model.TextBaseline.SUPERSCRIPT)
                "mark" -> seg.copy(bgColorHex = "FFEB3B")
                "small" -> seg.copy(fontSizeSp = 12f)
                "kbd", "var", "samp" -> seg.copy(code = true)
                "normal" -> seg.copy(bold = false, italic = false, underline = false, strikethrough = false, code = false, colorHex = null, bgColorHex = null, fontSizeSp = null, baseline = com.example.data.model.TextBaseline.NORMAL)
                "quote" -> seg.copy(italic = true)
                else -> seg
            }

            fun removeTransformForTag(tag: String, seg: com.example.data.model.TextSegment): com.example.data.model.TextSegment = when (tag) {
                "b" -> seg.copy(bold = false)
                "i" -> seg.copy(italic = false)
                "u" -> seg.copy(underline = false)
                "s" -> seg.copy(strikethrough = false)
                "code" -> seg.copy(code = false)
                "sub" -> seg.copy(baseline = com.example.data.model.TextBaseline.NORMAL)
                "sup" -> seg.copy(baseline = com.example.data.model.TextBaseline.NORMAL)
                "mark" -> seg.copy(bgColorHex = null)
                "small" -> seg.copy(fontSizeSp = null)
                "kbd", "var", "samp" -> seg.copy(code = false)
                "quote" -> seg.copy(italic = false)
                else -> seg
            }

            fun adjustIndent(delta: Int) {
                if (toolbarActiveBlockIndex !in blocks.indices) return
                val block = blocks[toolbarActiveBlockIndex]
                val current = block.meta["indentLevel"]?.toIntOrNull() ?: 0
                val newLevel = (current + delta).coerceAtLeast(0)
                if (newLevel == current) return
                val updated = if (newLevel == 0) {
                    block.copy(meta = block.meta - "indentLevel")
                } else {
                    block.copy(meta = block.meta + ("indentLevel" to newLevel.toString()))
                }
                blocks = blocks.toMutableList().apply { set(toolbarActiveBlockIndex, updated) }
                saveBlocksToHistory()
            }

            fun convertBlockToHeading(tag: String) {
                if (toolbarActiveBlockIndex !in blocks.indices) return
                val type = when (tag) {
                    "h1" -> BlockType.HEADING1
                    "h2" -> BlockType.HEADING2
                    "h3" -> BlockType.HEADING3
                    "h4" -> BlockType.HEADING4
                    else -> return
                }
                val block = blocks[toolbarActiveBlockIndex]
                if (block.type == type) return
                blocks = blocks.toMutableList().apply { set(toolbarActiveBlockIndex, block.copy(type = type)) }
                saveBlocksToHistory()
            }

            fun applyTag(tag: String) {
                if (tag in setOf("h1", "h2", "h3", "h4", "h5", "h6")) {
                    convertBlockToHeading(tag)
                    return
                }
                if (tag == "indent") {
                    adjustIndent(+1)
                    return
                }
                val styleTags = setOf("b", "i", "u", "s", "code", "sub", "sup", "mark", "small", "kbd", "var", "samp", "normal", "quote")
                if (tag !in styleTags) return
                val segs = activeSegments()
                if (segs.isEmpty()) return
                val plain = com.example.util.RichTextConverter.segmentsToPlainText(segs)
                val selStart = activeSelection.first.coerceIn(0, plain.length)
                val selEnd = activeSelection.last.coerceIn(0, plain.length).coerceAtLeast(selStart)
                if (tag == "normal") {
                    pendingTypingStyle = null
                    val newSegs = if (selStart == selEnd) {
                        if (selStart >= plain.length) return
                        com.example.util.RichTextConverter.applySpanStyle(segs, selStart, selStart + 1) { seg -> removeTransformForTag("normal", seg) }
                    } else {
                        com.example.util.RichTextConverter.applySpanStyle(segs, selStart, selEnd) { seg -> removeTransformForTag("normal", seg) }
                    }
                    commitSegmentsWithSelection(newSegs, selStart)
                    return
                }
                val newSegs = if (selStart == selEnd) {
                    val charSeg = if (selStart < plain.length) com.example.util.RichTextConverter.rangeSegments(segs, selStart, selStart + 1).firstOrNull() else null
                    val base = pendingTypingStyle ?: com.example.data.model.TextSegment()
                    val isActive = (charSeg != null && styleApplies(tag, charSeg)) || styleApplies(tag, base)
                    val nextPending = if (isActive) removeTransformForTag(tag, base) else applyTransformForTag(tag, base)
                    pendingTypingStyle = nextPending.takeIf { it.hasTypingStyle }
                    if (charSeg != null) {
                        com.example.util.RichTextConverter.applySpanStyle(segs, selStart, selStart + 1) { seg ->
                            if (isActive) removeTransformForTag(tag, seg) else applyTransformForTag(tag, seg)
                        }
                    } else {
                        null
                    }
                } else {
                    val inRange = com.example.util.RichTextConverter.rangeSegments(segs, selStart, selEnd)
                    val allStyled = inRange.isNotEmpty() && inRange.all { styleApplies(tag, it) }
                    com.example.util.RichTextConverter.applySpanStyle(segs, selStart, selEnd) { seg ->
                        if (allStyled) removeTransformForTag(tag, seg) else applyTransformForTag(tag, seg)
                    }
                }
                if (newSegs != null) commitSegmentsWithSelection(newSegs, selStart)
            }

            fun applyTagWithVal(tag: String, value: String) {
                val segs = activeSegments()
                if (segs.isEmpty()) return
                val plain = com.example.util.RichTextConverter.segmentsToPlainText(segs)
                val selStart = activeSelection.first.coerceIn(0, plain.length)
                val selEnd = activeSelection.last.coerceIn(0, plain.length).coerceAtLeast(selStart)

                fun setValueOn(seg: com.example.data.model.TextSegment): com.example.data.model.TextSegment = when (tag) {
                    "color" -> seg.copy(colorHex = com.example.util.RichTextConverter.normalizeColorValue(value))
                    "bg" -> seg.copy(bgColorHex = com.example.util.RichTextConverter.normalizeColorValue(value))
                    "font" -> seg.copy(fontFamily = value)
                    "size" -> seg.copy(fontSizeSp = value.toFloatOrNull())
                    "url" -> seg.copy(linkUrl = value)
                    else -> seg
                }

                fun clearValueOn(seg: com.example.data.model.TextSegment): com.example.data.model.TextSegment = when (tag) {
                    "color" -> seg.copy(colorHex = null)
                    "bg" -> seg.copy(bgColorHex = null)
                    "font" -> seg.copy(fontFamily = null)
                    "size" -> seg.copy(fontSizeSp = null)
                    "url" -> seg.copy(linkUrl = null)
                    else -> seg
                }

                fun currentValueOn(seg: com.example.data.model.TextSegment): String? = when (tag) {
                    "color" -> seg.colorHex
                    "bg" -> seg.bgColorHex
                    "font" -> seg.fontFamily
                    "size" -> seg.fontSizeSp?.let { if (it == it.toInt().toFloat()) it.toInt().toString() else it.toString() }
                    "url" -> seg.linkUrl
                    else -> null
                }

                val isDefault = value == "default" || value.isEmpty()
                val newSegs = if (selStart == selEnd) {
                    val charSeg = if (selStart < plain.length) com.example.util.RichTextConverter.rangeSegments(segs, selStart, selStart + 1).firstOrNull() else null
                    val base = pendingTypingStyle ?: com.example.data.model.TextSegment()
                    val alreadyApplied = !isDefault && currentValueOn(base) == value
                    val nextPending = when {
                        isDefault -> clearValueOn(base)
                        alreadyApplied -> clearValueOn(base)
                        else -> setValueOn(base)
                    }
                    pendingTypingStyle = nextPending.takeIf { it.hasTypingStyle }
                    if (charSeg != null) {
                        com.example.util.RichTextConverter.applySpanStyle(segs, selStart, selStart + 1) { seg ->
                            when {
                                isDefault -> clearValueOn(seg)
                                alreadyApplied -> clearValueOn(seg)
                                else -> setValueOn(seg)
                            }
                        }
                    } else {
                        null
                    }
                } else {
                    val inRange = com.example.util.RichTextConverter.rangeSegments(segs, selStart, selEnd)
                    val alreadyApplied = !isDefault && inRange.isNotEmpty() && inRange.all { currentValueOn(it) == value }
                    com.example.util.RichTextConverter.applySpanStyle(segs, selStart, selEnd) { seg ->
                        when {
                            isDefault -> clearValueOn(seg)
                            alreadyApplied -> clearValueOn(seg)
                            else -> setValueOn(seg)
                        }
                    }
                }
                if (newSegs != null) commitSegmentsWithSelection(newSegs, selStart)
            }

            val decreaseIndent: () -> Unit = {
                adjustIndent(-1)
            }

            val clipboard = LocalClipboard.current
            val pasteFromClipboard: () -> Unit = {
                scope.launch {
                val clipText = clipboard.getClipEntry()?.clipData?.getItemAt(0)?.text?.toString() ?: ""
                if (clipText.isNotEmpty()) {
                    if (RichTextParser.isSecureNotesJson(clipText)) {
                        val defaultTitle = context.getString(R.string.title_imported_note)
                        val (importedTitle, importedContent) = RichTextParser.parseSecureNotesJson(clipText, defaultTitle)
                        title = importedTitle
                        val (importedBlocks, _) = DataBlock.migrateToSegments(DataBlock.migrateLegacyContent(importedContent))
                        blocks = importedBlocks
                        if (blocks.isNotEmpty()) {
                            val importedPlain = com.example.util.RichTextConverter.segmentsToPlainText(blocks[0].ensureSegments())
                            contentValue = TextFieldValue(text = importedPlain, selection = TextRange(importedPlain.length))
                            toolbarActiveBlockIndex = 0
                        }
                        saveBlocksToHistory()
                        Toast.makeText(context, context.getString(R.string.toast_imported), Toast.LENGTH_SHORT).show()
                    } else {
                        val converted = if (clipText.trimStart().startsWith("<") || clipText.contains("</")) {
                            try { RichTextParser.convertHtmlToSecureNotes(clipText) } catch (e: Exception) { clipText }
                        } else {
                            clipText
                        }
                        pendingInsert.value = converted
                    }
                }
                }
            }

            val insertCurrentDate: () -> Unit = {
                val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                pendingInsert.value = formattedDate
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp)
                    .padding(bottom = 72.dp)
            ) {
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

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedNoteTags.isNotEmpty()) {
                    // Horizontal Pill tag tagging selectors
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

                
                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    BlockEditor(
                        blocks = blocks,
                        onBlocksChange = { newBlocks ->
                            val removedDrawingPaths = blocks
                                .filter { it.isLegacyDrawing }
                                .mapNotNull { it.content }
                                .filter { path -> newBlocks.none { it.isLegacyDrawing && it.content == path } }
                            if (removedDrawingPaths.isNotEmpty()) {
                                attachments = attachments.filterNot { it.type == "drawing" && it.path in removedDrawingPaths }
                            }
                            val newIdx = toolbarActiveBlockIndex.coerceIn(0, (newBlocks.size - 1).coerceAtLeast(0))
                            blocks = newBlocks
                            toolbarActiveBlockIndex = newIdx
                            val segs = blocks.getOrNull(newIdx)?.ensureSegments() ?: emptyList<com.example.data.model.TextSegment>()
                            val blockText = com.example.util.RichTextConverter.segmentsToPlainText(segs)
                            contentValue = TextFieldValue(text = blockText, selection = TextRange(blockText.length))
                            activeSelection = blockText.length..blockText.length
                            saveBlocksToHistory()
                        },
                        activeBlockIndex = toolbarActiveBlockIndex,
                        onActiveBlockChange = { switchActiveBlock(it) },
                        attachments = attachments,
                        noteId = noteId,
                        onNavigateToMediaViewer = onNavigateToMediaViewer,
                        onNavigateToDrawing = onNavigateToDrawing,
                        onNavigateToNote = onNavigateToNote,
                        noteTitleById = { id -> allNotes.find { it.note.id == id }?.title ?: "" },
                        onMoveBlockTo = { index ->
                            moveBlockIndex = index
                            showMoveBlockPicker = true
                        },
                        onConvertTo = { _ ->
                            convertBlockMode = true
                            showMoreFormattingSheet = true
                        },
                        onEditPageLink = { idx ->
                            pageLinkTargetBlockIndex = idx
                            showNoteLinkPicker = true
                        },
                        onEditImage = { idx ->
                            imageDialogMode = idx
                            showInsertImageDialog = true
                        },
                        onEditVideo = { idx ->
                            videoDialogMode = idx
                            showInsertVideoDialog = true
                        },
                        onUrlClicked = handleUrlClick,
                        pendingTagInsert = pendingTagInsert,
                        pendingInsert = pendingInsert,
                        pendingSelection = pendingSelection,
                        pendingTypingStyle = pendingTypingStyle,
                        onActiveCursorChange = { toolbarActiveCursorOffset = it },
                        onActiveSelectionChange = { activeSelection = it },
                        onParseResult = { toolbarParseResult = it },
                        pendingFocusBlockIndex = pendingFocusBlockIndex,
                        onFocusHandled = { pendingFocusBlockIndex = -1 },
                        modifier = Modifier.fillMaxSize()
                    )

                    if (showSlashMenu) {
                        Box(modifier = Modifier.align(Alignment.TopStart).padding(start = 40.dp, top = 4.dp)) {
                            SlashCommandMenu(
                                filter = slashFilter,
                                onSelect = { cmd -> handleBlockCommand(cmd) },
                                onDismiss = { showSlashMenu = false }
                            )
                        }
                }
                    }

                // Insert Image Dialog
                InsertMediaDialog(
                    show = showInsertImageDialog,
                    onDismiss = {
                        showInsertImageDialog = false
                        imageDialogMode = -1
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
                        if (url.isNotBlank()) applyImageSelection(url)
                        imageInputUrl = ""
                        showInsertImageDialog = false
                        imageDialogMode = -1
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
                        if (url.isNotBlank()) applyVideoSelection(url)
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
                                        val editRange = urlEditRange
                                        val segs = activeSegments()
                                        if (segs.isEmpty()) {
                                            pendingInsert.value = "<url=$urlInputAddress>$display</url>"
                                        } else {
                                            val plain = com.example.util.RichTextConverter.segmentsToPlainText(segs)
                                            val selStart = editRange?.first ?: activeSelection.first.coerceIn(0, plain.length)
                                            val selEnd = editRange?.let { it.last + 1 } ?: activeSelection.last.coerceIn(0, plain.length).coerceAtLeast(selStart)
                                            val insert = listOf(com.example.data.model.TextSegment(text = display, linkUrl = urlInputAddress))
                                            val newSegs = com.example.util.RichTextConverter.insertSegments(segs, selStart, selEnd, insert)
                                            commitSegmentsWithSelection(newSegs, selStart + display.length)
                                        }
                                    }
                                    urlInputAddress = ""
                                    urlInputText = ""
                                    urlEditRange = null
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
                                    urlEditRange = null
                                    showInsertUrlDialog = false
                                }
                            ) {
                                Text(stringResource(id = R.string.btn_cancel))
                            }
                        }
                    )
                }
                
                // Insert Table Dialog
                if (showInsertTableDialog) {
                    AlertDialog(
                        onDismissRequest = { showInsertTableDialog = false },
                        title = { Text("Insert Table") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Rows:", style = MaterialTheme.typography.bodyMedium)
                                    Slider(
                                        value = tableRows.toFloat(),
                                        onValueChange = { tableRows = it.toInt() },
                                        valueRange = 1f..10f,
                                        steps = 8,
                                        modifier = Modifier.width(120.dp)
                                    )
                                    Text("$tableRows", style = MaterialTheme.typography.bodySmall)
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Cols:", style = MaterialTheme.typography.bodyMedium)
                                    Slider(
                                        value = tableCols.toFloat(),
                                        onValueChange = { tableCols = it.toInt() },
                                        valueRange = 1f..8f,
                                        steps = 6,
                                        modifier = Modifier.width(120.dp)
                                    )
                                    Text("$tableCols", style = MaterialTheme.typography.bodySmall)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = tableHasHeader,
                                        onCheckedChange = { tableHasHeader = it }
                                    )
                                    Text("Include header row", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val headers = if (tableHasHeader) {
                                        (1..tableCols).map { "Header $it" }
                                    } else {
                                        List(tableCols) { "" }
                                    }
                                    val rows = (1..tableRows).map { List(tableCols) { "" } }
                                    val tableData = TableData(headers = headers, rows = rows)
                                    handleSlashSelect(DataBlock(type = BlockType.TABLE, content = "", meta = mapOf("table" to tableData.toJson())))
                                    showInsertTableDialog = false
                                }
                            ) {
                                Text(stringResource(id = R.string.btn_insert))
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    showInsertTableDialog = false
                                }
                            ) {
                                Text(stringResource(id = R.string.btn_cancel))
                            }
                        }
                    )
                }

                // URL Click Action Dialog (Open, Copy, Delete)
                if (showUrlDialog) {
                    val clipboard = LocalClipboard.current
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
                                        scope.launch {
                                            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("secure_notes", clickedUrlAddress)))
                                        }
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
                                        val segs = activeSegments()
                                        val offset = clickedUrlAbsoluteOffset.coerceIn(0, com.example.util.RichTextConverter.segmentsToPlainText(segs).length)
                                        var pos = 0
                                        var urlSeg: com.example.data.model.TextSegment? = null
                                        var urlStart = 0
                                        for (seg in segs) {
                                            val segEnd = pos + seg.text.length
                                            if (pos <= offset && offset < segEnd && seg.linkUrl != null) {
                                                urlSeg = seg
                                                urlStart = pos
                                                break
                                            }
                                            pos = segEnd
                                        }
                                        if (urlSeg != null) {
                                            urlInputAddress = urlSeg.linkUrl ?: ""
                                            urlInputText = urlSeg.text
                                            urlEditRange = urlStart..(urlStart + urlSeg.text.length - 1)
                                            showUrlDialog = false
                                            showInsertUrlDialog = true
                                        } else {
                                            showUrlDialog = false
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(id = R.string.url_dialog_edit))
                                }
                                
                                OutlinedButton(
                                    onClick = {
                                        val segs = activeSegments()
                                        val offset = clickedUrlAbsoluteOffset.coerceIn(0, com.example.util.RichTextConverter.segmentsToPlainText(segs).length)
                                        var pos = 0
                                        var urlSeg: com.example.data.model.TextSegment? = null
                                        var urlStart = 0
                                        for (seg in segs) {
                                            val segEnd = pos + seg.text.length
                                            if (pos <= offset && offset < segEnd && seg.linkUrl != null) {
                                                urlSeg = seg
                                                urlStart = pos
                                                break
                                            }
                                            pos = segEnd
                                        }
                                        if (urlSeg != null) {
                                            val newSegs = com.example.util.RichTextConverter.replaceTextRange(segs, urlStart, urlStart + urlSeg.text.length, "")
                                            commitSegmentsWithSelection(newSegs, urlStart)
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

            if (showTextBgColorSheet) {
                TextBgColorSheet(
                    onDismiss = { showTextBgColorSheet = false },
                    onTextColorSelected = { color -> applyTagWithVal("color", color) },
                    onBgColorSelected = { color -> applyTagWithVal("bg", color) }
                )
            }

            if (showFontSizeSheet) {
                FontSizeSheet(
                    onDismiss = { showFontSizeSheet = false },
                    onFontSelected = { font -> applyTagWithVal("font", font) },
                    onSizeSelected = { size -> applyTagWithVal("size", size) }
                )
            }

            if (isKeyboardVisible) {
                Box(Modifier.fillMaxSize()) {
                    EditorToolbarContainer(
                        modifier = Modifier.align(Alignment.BottomCenter),
                    activeTextStyles = activeTextStyles,
                    isSpeaking = isSpeaking,
                    aiEnabled = aiEnabled,
                    showAiPanel = showAiPanel,
                    canUndo = historyIndex > 0,
                    canRedo = historyIndex < history.lastIndex,
                    onUndo = {
                        syncActiveBlock()
                        if (historyIndex > 0) {
                            historyIndex--
                            val prevBlocks = history[historyIndex]
                            blocks = prevBlocks
                            val idx = toolbarActiveBlockIndex.coerceIn(0, (blocks.size - 1).coerceAtLeast(0))
                            toolbarActiveBlockIndex = idx
                            val segs = blocks.getOrNull(idx)?.ensureSegments() ?: emptyList<com.example.data.model.TextSegment>()
                            val text = com.example.util.RichTextConverter.segmentsToPlainText(segs)
                            contentValue = TextFieldValue(text = text, selection = TextRange(text.length))
                            activeSelection = text.length..text.length
                        }
                    },
                    onRedo = {
                        syncActiveBlock()
                        if (historyIndex < history.lastIndex) {
                            historyIndex++
                            val nextBlocks = history[historyIndex]
                            blocks = nextBlocks
                            val idx = toolbarActiveBlockIndex.coerceIn(0, (blocks.size - 1).coerceAtLeast(0))
                            toolbarActiveBlockIndex = idx
                            val segs = blocks.getOrNull(idx)?.ensureSegments() ?: emptyList<com.example.data.model.TextSegment>()
                            val text = com.example.util.RichTextConverter.segmentsToPlainText(segs)
                            contentValue = TextFieldValue(text = text, selection = TextRange(text.length))
                            activeSelection = text.length..text.length
                        }
                    },
                    onToggleTag = { applyTag(it) },
                    decreaseIndent = decreaseIndent,
                    pasteFromClipboard = pasteFromClipboard,
                    insertCurrentDate = insertCurrentDate,
                    applyTagWithVal = { tag, value -> applyTagWithVal(tag, value) },
                    onClearFormatting = {
                        pendingTypingStyle = null
                        val segs = activeSegments()
                        if (segs.isNotEmpty()) {
                            val plain = com.example.util.RichTextConverter.segmentsToPlainText(segs)
                            val selStart = activeSelection.first.coerceIn(0, plain.length)
                            val selEnd = activeSelection.last.coerceIn(0, plain.length).coerceAtLeast(selStart)
                            val start = if (selStart == selEnd) 0 else selStart
                            val end = if (selStart == selEnd) plain.length else selEnd
                            val newSegs = com.example.util.RichTextConverter.applySpanStyle(segs, start, end) {
                                it.copy(
                                    bold = false, italic = false, underline = false, strikethrough = false,
                                    code = false, colorHex = null, bgColorHex = null,
                                    fontFamily = null, fontSizeSp = null,
                                    baseline = com.example.data.model.TextBaseline.NORMAL
                                )
                            }
                            commitSegmentsWithSelection(newSegs, selStart)
                        }
                    },
                    onOpenMoreFormatting = { showMoreFormattingSheet = true },
                    onOpenPalette = { showPaletteSheet = true },
                    onTtsToggle = {
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
                    onOpenDrawing = {
                        insertDrawingBlock()
                    },
                    onOpenAttachments = { showVoiceFileSheet = true },
                    onOpenAi = {
                        aiSelectionStart = activeSelection.first
                        aiSelectionEnd = activeSelection.last
                        if (activeSelection.first != activeSelection.last) {
                            showAiContextSheet = true
                        } else {
                            aiViewModel.prepareChatForNote(content, "", title)
                            onNavigateToAiChat(noteId)
                        }
                    },
                    onToggleAiPanel = { showAiPanel = !showAiPanel },
                    onToggleKeyboard = {
                        if (isKeyboardVisible) {
                            keyboardController?.hide()
                        } else {
                            editorFocusRequester.requestFocus()
                        }
                    },
                    onOpenbgFontColor = { showTextBgColorSheet = true },
                    onOpenInlineLink = {
                        val cursor = toolbarActiveCursorOffset.coerceIn(0, toolbarParseResult.text.length)
                        val urlAtCursor = toolbarParseResult.text.getStringAnnotations("URL", cursor, cursor).firstOrNull()?.item
                        if (urlAtCursor != null) {
                            clickedUrlAddress = urlAtCursor
                            clickedUrlAbsoluteOffset = cursor
                            urlEditRange = null
                            showUrlDialog = true
                        } else {
                            inlineLinkMode = true
                            pageLinkTargetBlockIndex = -1
                            showNoteLinkPicker = true
                        }
                    },
                    onOpenEquation = { showEquationDialog = true },
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    matchCount = matchRanges.size,
                    currentMatchIndex = currentMatchIndex,
                    onPreviousMatch = {
                        if (matchRanges.isNotEmpty()) {
                            currentMatchIndex = (currentMatchIndex - 1 + matchRanges.size) % matchRanges.size
                            setContentValue(contentValue.copy(selection = matchRanges[currentMatchIndex]))
                            editorFocusRequester.requestFocus()
                        }
                    },
                    onNextMatch = {
                        if (matchRanges.isNotEmpty()) {
                            currentMatchIndex = (currentMatchIndex + 1) % matchRanges.size
                            setContentValue(contentValue.copy(selection = matchRanges[currentMatchIndex]))
                            editorFocusRequester.requestFocus()
                        }
                    },
                    caseSensitive = searchCaseSensitive,
                    onCaseSensitiveChange = { searchCaseSensitive = it },
                    fullWord = searchFullWord,
                    onFullWordChange = { searchFullWord = it },
                    onOpenFontSizeSheet = { showFontSizeSheet = true },
                    onConvertBlock = {
                        convertBlockMode = true
                        showMoreFormattingSheet = true
                    },
                    onDeleteBlock = {
                        val deletedBlock = blocks.getOrNull(toolbarActiveBlockIndex)
                        deleteActiveBlock()
                        if (deletedBlock?.isLegacyDrawing == true) {
                            attachments = attachments.filterNot { it.type == "drawing" && it.path == deletedBlock.content }
                        }
                    },
                    onMoveBlockUp = { moveActiveBlockUp() },
                    onMoveBlockDown = { moveActiveBlockDown() }
                )
                }
            }

        if (showMoreFormattingSheet) {
            fun handleBlockSelect(cmd: BlockCommand) {
                if (convertBlockMode) {
                    convertBlockMode = false
                    cmd.blockType?.let { type ->
                        val oldBlock = blocks.getOrNull(toolbarActiveBlockIndex)
                        convertActiveBlock(type, cmd.meta)
                        if (oldBlock?.isLegacyDrawing == true && type != BlockType.DRAWING) {
                            attachments = attachments.filterNot { it.type == "drawing" && it.path == oldBlock.content }
                        }
                        return
                    }
                }
                handleBlockCommand(cmd)
            }

            MoreFormattingSheet(
                convertBlockMode = convertBlockMode,
                onDismiss = {
                    convertBlockMode = false
                    showMoreFormattingSheet = false
                },
                onCommandSelected = { cmd ->
                    handleBlockSelect(cmd)
                    showMoreFormattingSheet = false
                }
            )
        }
        }

        // ── Level 2: Floating AI Panel ──────────────────────────
        if (showAiPanel) {
            Box(Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 88.dp)
                    .fillMaxWidth(0.92f),
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("✨ ${stringResource(R.string.ai_assistant)}", style = MaterialTheme.typography.titleSmall)
                        IconButton(onClick = { showAiPanel = false }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = aiPromptInput,
                        onValueChange = { aiPromptInput = it },
                        placeholder = { Text(stringResource(R.string.ai_panel_placeholder), fontSize = 13.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        textStyle = MaterialTheme.typography.bodySmall,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SuggestionChip(
                            onClick = {
                                aiSelectionStart = 0; aiSelectionEnd = contentValue.text.length
                                executeAiAction(AiAction.SUMMARIZE)
                                showAiPanel = false
                            },
                            label = { Text(stringResource(R.string.ai_summarize), fontSize = 11.sp) }
                        )
                        SuggestionChip(
                            onClick = {
                                aiSelectionStart = 0; aiSelectionEnd = contentValue.text.length
                                executeAiAction(AiAction.FIX_GRAMMAR)
                                showAiPanel = false
                            },
                            label = { Text(stringResource(R.string.ai_fix_grammar), fontSize = 11.sp) }
                        )
                        SuggestionChip(
                            onClick = {
                                aiViewModel.prepareChatForNote(content, "", title)
                                showAiPanel = false
                                onNavigateToAiChat(noteId)
                            },
                            label = { Text(stringResource(R.string.ai_panel_chat), fontSize = 11.sp) }
                        )
                    }
                    if (aiPromptInput.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                aiSelectionStart = activeSelection.first
                                aiSelectionEnd = activeSelection.last
                                val target = contentValue.text.substring(aiSelectionStart, aiSelectionEnd)
                                    .takeIf { aiSelectionStart != aiSelectionEnd } ?: content
                                aiViewModel.executeInPlace(AiAction.GENERATE, "$target\n\n$aiPromptInput")
                                aiPromptInput = ""
                                showAiPanel = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !inPlaceProcessing
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.ai_generate), fontSize = 13.sp)
                        }
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
                }
            }
        }

        EditorMoreOptions(
            show = showMoreSheet,
            viewModel = viewModel,
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
            onDeleted = {
                showMoreSheet = false
                onBack()
            },
            onDismiss = { showMoreSheet = false }
        )

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
                                content = contentForSave(),
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
                                imageDialogMode = -1
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
                                videoDialogMode = -1
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
                                                    content = contentForSave(),
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

        // ── Level 1: AI Context Actions Bottom Sheet ──────────
        if (showAiContextSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showAiContextSheet = false
                    showAiStyleSubmenu = false
                    showAiLangSubmenu = false
                },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "✨ ${stringResource(R.string.ai_assistant)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Spacer(Modifier.height(16.dp))

                    if (showAiStyleSubmenu) {
                        Text(stringResource(R.string.ai_context_rewrite_style), style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            RewriteStyle.entries.forEach { style ->
                                FilterChip(
                                    selected = aiPendingRewriteStyle == style,
                                    onClick = {
                                        aiPendingRewriteStyle = style
                                        executeAiAction(AiAction.REWRITE, style = style)
                                    },
                                    label = { Text(style.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 12.sp) }
                                )
                            }
                        }
                    } else if (showAiLangSubmenu) {
                        Text(stringResource(R.string.ai_context_target_language), style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(8.dp))
                        val languages = listOf("en", "es", "pt", "fr", "de", "it", "ja", "zh", "ru", "ar")
                        val langLabels = mapOf(
                            "en" to stringResource(R.string.ai_lang_en),
                            "es" to stringResource(R.string.ai_lang_es),
                            "pt" to stringResource(R.string.ai_lang_pt),
                            "fr" to stringResource(R.string.ai_lang_fr),
                            "de" to stringResource(R.string.ai_lang_de),
                            "it" to stringResource(R.string.ai_lang_it),
                            "ja" to stringResource(R.string.ai_lang_ja),
                            "zh" to stringResource(R.string.ai_lang_zh),
                            "ru" to stringResource(R.string.ai_lang_ru),
                            "ar" to stringResource(R.string.ai_lang_ar)
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            languages.forEach { code ->
                                FilterChip(
                                    selected = aiPendingTargetLanguage == code,
                                    onClick = {
                                        aiPendingTargetLanguage = code
                                        executeAiAction(AiAction.TRANSLATE, language = code)
                                    },
                                    label = { Text(langLabels[code] ?: code, fontSize = 12.sp) }
                                )
                            }
                        }
                    } else {
                        Text(stringResource(R.string.ai_context_quick_actions), style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AssistChip(
                                onClick = { executeAiAction(AiAction.SUMMARIZE) },
                                label = { Text(stringResource(R.string.ai_chip_summarize), fontSize = 12.sp) },
                                leadingIcon = { }
                            )
                            AssistChip(
                                onClick = { showAiStyleSubmenu = true },
                                label = { Text(stringResource(R.string.ai_chip_rewrite), fontSize = 12.sp) },
                                leadingIcon = { }
                            )
                            AssistChip(
                                onClick = { showAiLangSubmenu = true },
                                label = { Text(stringResource(R.string.ai_chip_translate), fontSize = 12.sp) },
                                leadingIcon = { }
                            )
                            AssistChip(
                                onClick = { executeAiAction(AiAction.MAKE_SHORTER) },
                                label = { Text(stringResource(R.string.ai_chip_make_shorter), fontSize = 12.sp) },
                                leadingIcon = { }
                            )
                            AssistChip(
                                onClick = { executeAiAction(AiAction.FIX_GRAMMAR) },
                                label = { Text(stringResource(R.string.ai_chip_fix_grammar), fontSize = 12.sp) },
                                leadingIcon = { }
                            )
                            AssistChip(
                                onClick = { executeAiAction(AiAction.EXPLAIN) },
                                label = { Text(stringResource(R.string.ai_chip_explain), fontSize = 12.sp) },
                                leadingIcon = { }
                            )
                        }
                    }
                }
            }
        }

        // ── AI Streaming / Result Indicator ───────────────────
        if (inPlaceAction != null && inPlaceProcessing) {
            AlertDialog(
                onDismissRequest = { aiViewModel.cancelGeneration() },
                title = { Text(stringResource(R.string.ai_progress_title), style = MaterialTheme.typography.titleSmall) },
                text = {
                    Column {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = when (inPlaceAction) {
                                AiAction.SUMMARIZE -> stringResource(R.string.ai_progress_summarizing)
                                AiAction.REWRITE -> stringResource(R.string.ai_progress_rewriting)
                                AiAction.TRANSLATE -> stringResource(R.string.ai_progress_translating)
                                AiAction.MAKE_SHORTER -> stringResource(R.string.ai_progress_making_shorter)
                                AiAction.FIX_GRAMMAR -> stringResource(R.string.ai_progress_fixing_grammar)
                                AiAction.EXPLAIN -> stringResource(R.string.ai_progress_explaining)
                                else -> stringResource(R.string.ai_generating)
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (!inPlaceStreamingText.isNullOrBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = inPlaceStreamingText!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(8.dp),
                                    maxLines = 8,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { aiViewModel.cancelGeneration() }) {
                        Text(stringResource(R.string.btn_cancel))
                    }
                }
            )
        }

        if (showNoteLinkPicker) {
            PageLinkNotePickerSheet(
                notes = allNotes,
                currentNoteId = noteId,
                onDismiss = {
                    showNoteLinkPicker = false
                    inlineLinkMode = false
                },
                onNoteSelected = { linkedId ->
                    if (inlineLinkMode) {
                        val segs = activeSegments()
                        val selStart = activeSelection.first.coerceIn(0, com.example.util.RichTextConverter.segmentsToPlainText(segs).length)
                        val selEnd = activeSelection.last.coerceIn(0, com.example.util.RichTextConverter.segmentsToPlainText(segs).length).coerceAtLeast(selStart)
                        if (selStart != selEnd && segs.isNotEmpty()) {
                            val selected = com.example.util.RichTextConverter.segmentsToPlainText(segs).substring(selStart, selEnd)
                            val insert = listOf(com.example.data.model.TextSegment(text = selected.ifBlank { linkedId.toString() }, linkUrl = "note://$linkedId"))
                            val newSegs = com.example.util.RichTextConverter.insertSegments(segs, selStart, selEnd, insert)
                            commitSegmentsWithSelection(newSegs, selStart + insert.sumOf { it.text.length })
                        } else {
                            val title = allNotes.find { it.note.id == linkedId }?.title ?: linkedId.toString()
                            pendingInsert.value = "<url=note://$linkedId>$title</url>"
                        }
                        showNoteLinkPicker = false
                        inlineLinkMode = false
                    } else {
                        val target = pageLinkTargetBlockIndex
                        if (target in blocks.indices) {
                            val newBlocks = blocks.toMutableList()
                            newBlocks[target] = newBlocks[target].copy(meta = mapOf("noteId" to linkedId.toString()))
                            blocks = newBlocks
                            saveBlocksToHistory()
                        } else {
                            handleSlashSelect(DataBlock(type = BlockType.PAGE_LINK, content = "", meta = mapOf("noteId" to linkedId.toString())))
                        }
                        showNoteLinkPicker = false
                    }
                }
            )
        }

        if (showMoveBlockPicker) {
            PageLinkNotePickerSheet(
                notes = allNotes,
                currentNoteId = noteId,
                onDismiss = {
                    showMoveBlockPicker = false
                    moveBlockIndex = -1
                },
                onNoteSelected = { targetId ->
                    val idx = moveBlockIndex
                    showMoveBlockPicker = false
                    moveBlockIndex = -1
                    if (idx >= 0) {
                        val movedBlock = blocks.getOrNull(idx)
                        moveBlockToAnotherNote(idx, targetId)
                        if (movedBlock?.isLegacyDrawing == true) {
                            attachments = attachments.filterNot { it.type == "drawing" && it.path == movedBlock.content }
                        }
                    }
                }
            )
        }

        if (showEquationDialog) {
            AlertDialog(
                onDismissRequest = {
                    showEquationDialog = false
                    equationInput = ""
                },
                title = { Text(stringResource(id = R.string.dialog_insert_equation_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = equationInput,
                            onValueChange = { equationInput = it },
                            label = { Text(stringResource(id = R.string.label_equation_latex)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (equationInput.isNotBlank()) {
                            Text(
                                text = stringResource(id = R.string.rich_live_preview),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = MathRenderer.render(equationInput.trim()),
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(8.dp),
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (equationInput.isNotBlank()) {
                                pendingInsert.value = "<eq>${equationInput.trim()}</eq>"
                            }
                            equationInput = ""
                            showEquationDialog = false
                        }
                    ) {
                        Text(stringResource(id = R.string.btn_insert))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            equationInput = ""
                            showEquationDialog = false
                        }
                    ) {
                        Text(stringResource(id = R.string.btn_cancel))
                    }
                }
            )
        }

    }
}
