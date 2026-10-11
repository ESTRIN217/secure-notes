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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.CodeEditorActivity
import com.example.R
import com.example.data.ai.AiAction
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
import com.example.ui.editor.applyTagToSelection
import com.example.ui.editor.buildTableRows
import com.example.ui.editor.findUrlAt
import com.example.ui.editor.setTagValue
import com.example.ui.editor.clearTagValue
import com.example.ui.editor.tagValueOf
import com.example.ui.editor.applyTransformForTag
import com.example.ui.editor.buildMatchRanges
import com.example.ui.editor.computeToolbarStyle
import com.example.ui.editor.convertBlockAt
import com.example.ui.editor.deleteBlockAt
import com.example.ui.editor.headingTypeForTag
import com.example.ui.editor.insertPlanFor
import com.example.ui.editor.insertionIndex
import com.example.ui.editor.ExternalTarget
import com.example.ui.editor.externalTargetFor
import com.example.ui.editor.isAudioFile
import com.example.ui.editor.moveBlockBy
import com.example.ui.editor.removeTransformForTag
import com.example.ui.editor.styleApplies
import com.example.util.RichTextConverter
import com.example.ui.settings.SettingsCardGroup
import com.example.ui.settings.SettingsSwitchTile
import com.example.util.getNoteBackgroundColor
import com.example.util.exportMultipleToHtml
import com.example.util.exportMultipleToTxt
import com.example.util.exportToMarkdown
import com.example.util.exportToPdf
import com.example.util.exportSingleNoteToJson
import com.example.util.ImageUrlResolver
import com.example.util.BookmarkMetadataFetcher
import com.example.util.MediaBlock
import com.example.util.JsonColorizer
import com.example.util.RichTextParser
import com.example.util.highlightMatches
import com.example.util.parseToContentBlocks
import com.example.util.removeAttachmentFromContent
import com.example.util.removeMediaFromContent
import com.example.util.toggleNthChecklistItem
import com.example.util.MediaStorageHelper
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
    onNavigateToNote: (Int) -> Unit = { _ -> },
    draftGuard: DraftGuard? = null,
    tabBarContent: @Composable () -> Unit = {},
    onOpenPdfTab: (Uri, String) -> Unit,
    onOpenTextTab: (Uri, String) -> Unit
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
    var ttsReady by remember { mutableStateOf(false) }
    var ttsPaused by remember { mutableStateOf(false) }
    var ttsParts by remember { mutableStateOf<List<String>>(emptyList()) }
    var ttsPartIndex by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        var ttsEngineRef: TextToSpeech? = null
        val ttsEngine = TextToSpeech(context) { status ->
            val engine = ttsEngineRef
            if (status == TextToSpeech.SUCCESS && engine != null) {
                val locale = Locale.getDefault()
                engine.language = if (engine.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE) {
                    locale
                } else {
                    Locale.US
                }
                engine.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        ttsPartIndex = ttsPartIndex.coerceAtLeast(utteranceId?.toIntOrNull() ?: 0)
                    }
                    override fun onDone(utteranceId: String?) {
                        if (ttsPaused) return
                        if (utteranceId?.toIntOrNull() == ttsParts.lastIndex) {
                            isSpeaking = false
                            ttsPaused = false
                        }
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        if (ttsPaused) return
                        isSpeaking = false
                    }
                    override fun onError(utteranceId: String?, errorCode: Int) {
                        if (ttsPaused) return
                        isSpeaking = false
                    }
                })
                ttsReady = true
            }
        }
        ttsEngineRef = ttsEngine
        tts = ttsEngine
        onDispose {
            ttsEngine.stop()
            ttsEngine.shutdown()
        }
    }

    fun ttsMissingFeedback() {
        Toast.makeText(context, context.getString(R.string.tts_engine_missing), Toast.LENGTH_LONG).show()
        try {
            context.startActivity(Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE))
        } catch (e: Exception) {
            Log.e("NoteEditorScreen", "open tts engine store failed", e)
        }
    }

    fun speakTts(parts: List<String>, startIndex: Int) {
        val engine = tts
        if (engine == null || !ttsReady) {
            ttsMissingFeedback()
            return
        }
        for (i in startIndex until parts.size) {
            if (engine.speak(parts[i], TextToSpeech.QUEUE_ADD, null, i.toString()) == TextToSpeech.ERROR) {
                isSpeaking = false
                ttsPaused = false
                return
            }
        }
        isSpeaking = true
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
    var matchRanges by remember { mutableStateOf<List<TextRange>>(emptyList()) }
    var currentMatchIndex by remember { mutableStateOf(0) }
    var searchCaseSensitive by remember { mutableStateOf(false) }
    var searchFullWord by remember { mutableStateOf(false) }
    val editorFocusRequester = remember { FocusRequester() }

    // ── AI Integration State ─────────────────────────────────
    var showAiContextSheet by remember { mutableStateOf(false) }
    var showAiPanel by remember { mutableStateOf(false) }
    var aiPromptInput by remember { mutableStateOf("") }
    var aiSelectionStart by remember { mutableIntStateOf(0) }
    var aiSelectionEnd by remember { mutableIntStateOf(0) }
    

    val inPlaceResult by aiViewModel.inPlaceResult.collectAsStateWithLifecycle()
    val inPlaceStreamingText by aiViewModel.inPlaceStreamingText.collectAsStateWithLifecycle()
    val inPlaceProcessing by aiViewModel.inPlaceProcessing.collectAsStateWithLifecycle()
    val inPlaceAction by aiViewModel.inPlaceAction.collectAsStateWithLifecycle()

    val isKeyboardVisible = WindowInsets.isImeVisible
    val keyboardController = LocalSoftwareKeyboardController.current

    fun executeAiAction(
        action: AiAction
    ) {
        val allContent = blocks.joinToString("\n") {
            com.example.util.RichTextConverter.segmentsToMarkdown(it.ensureSegments())
        }
        val text = contentValue.text.substring(aiSelectionStart, aiSelectionEnd)
            .takeIf { aiSelectionStart != aiSelectionEnd } ?: allContent
        aiViewModel.executeInPlace(action, text)
        showAiContextSheet = false
    }

    // El resaltado es lógica pura (`buildMatchRanges`); aquí solo se le pasa el texto limpio y la
    // traducción de índices, y se recogen lo que devuelva. El efecto se relanza a sí mismo cuando mueve
    // el cursor: es el comportamiento que había y no se toca en este refactor.
    LaunchedEffect(searchQuery, searchCaseSensitive, searchFullWord, contentValue.text) {
        if (searchQuery.isNotEmpty()) {
            val parseResult = RichTextParser.parseWithMapping(contentValue.text, hideTags = true)
            val ranges = buildMatchRanges(
                cleanText = parseResult.text.text,
                query = searchQuery,
                fullWord = searchFullWord,
                caseSensitive = searchCaseSensitive,
                toRawIndex = parseResult::transformedToOriginal
            ).map { TextRange(it.first, it.last) }
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

    var convertBlockMode by remember { mutableStateOf(false) }
    var showFontSizeSheet by remember { mutableStateOf(false) }
    val activeTextStyles = remember(activeSegments(), activeSelection, toolbarActiveBlockIndex, pendingTypingStyle) {
        computeToolbarStyle(
            blockType = blocks.getOrNull(toolbarActiveBlockIndex)?.type,
            segments = activeSegments(),
            cursor = activeSelection.first,
            collapsed = activeSelection.first == activeSelection.last,
            pending = pendingTypingStyle
        )
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
        val type = headingTypeForTag(tag) ?: return
        val block = blocks.getOrNull(toolbarActiveBlockIndex) ?: return
        if (block.type == type) return
        val nuevo = convertBlockAt(blocks, toolbarActiveBlockIndex, type, block.meta) ?: return
        // **No** por `applyBlocksChange`: eso repone el cursor al final del bloque y aquí el cursor
        // se está eligiendo justo ahora, que es el punto de convertir a título.
        blocks = nuevo
        saveBlocksToHistory()
    }

    val commitSegments: (List<com.example.data.model.TextSegment>) -> Unit = { newSegs ->
        if (toolbarActiveBlockIndex in blocks.indices && blocks[toolbarActiveBlockIndex].contentIsText) {
            val block = blocks[toolbarActiveBlockIndex]
            val storedSegs = if (block.type == BlockType.CODE_BLOCK) {
                listOf(
                    com.example.data.model.TextSegment(
                        text = com.example.util.RichTextConverter.segmentsToPlainText(newSegs)
                    )
                )
            } else {
                newSegs
            }
            val updated = block.copy(
                content = "",
                richTextJson = com.example.data.model.TextSegment.serialize(storedSegs)
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

    fun applyEquationEdit(blockIndex: Int, offset: Int, latex: String) {
        val block = blocks.getOrNull(blockIndex) ?: return
        if (!block.contentIsText) return
        val segs = block.ensureSegments()
        val updated = com.example.util.RichTextConverter.replaceEquationAt(segs, offset, latex)
        if (updated === segs) return
        blocks = blocks.toMutableList().apply {
            set(
                blockIndex,
                block.copy(
                    content = "",
                    richTextJson = com.example.data.model.TextSegment.serialize(updated)
                )
            )
        }
        saveBlocksToHistory()
    }

    fun syncActiveBlock() {
        if (toolbarActiveBlockIndex !in blocks.indices) return
        val block = blocks[toolbarActiveBlockIndex]
        if (!block.contentIsText) return
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

    var audioDialogMode by remember { mutableIntStateOf(-1) }

    var fileDialogMode by remember { mutableIntStateOf(-1) }

    var showBookmarkUrlDialog by remember { mutableStateOf(false) }
    var bookmarkUrlInput by remember { mutableStateOf("") }
    var bookmarkDialogMode by remember { mutableIntStateOf(-1) }
    val fetchingBookmarkIndices = remember { mutableStateOf(setOf<Int>()) }

    /**
     * Inserta un bloque de medio donde está el cursor (RF-89).
     *
     * Sustituye a **cinco** insertadores que eran byte a byte idénticos: lo único que cambiaba era
     * el bloque que construían. Cada uno repetía la decisión de *dónde* insertar —sustituir el
     * bloque vacío del cursor o empujar hacia abajo— y el normalizado del cursor al terminar. Esa
     * decisión está ahora en [insertPlanFor], que es pura y tiene prueba para los dos desenlaces.
     *
     * Devuelve el índice donde ha quedado el bloque, que el de marcador necesita para ir a buscar
     * sus metadatos después.
     */
    fun insertBlockAtCursor(block: DataBlock): Int {
        showSlashMenu = false
        val currentIdx = toolbarActiveBlockIndex.coerceIn(0, blocks.size)
        val plan = insertPlanFor(
            activeIndex = currentIdx,
            activePlainText = blocks.getOrNull(currentIdx)
                ?.let { com.example.util.RichTextConverter.segmentsToPlainText(it.ensureSegments()) },
            blockCount = blocks.size
        )
        val newBlocks = blocks.toMutableList()
        if (plan.replaceInPlace) newBlocks[plan.index] = block else newBlocks.add(plan.index, block)
        blocks = newBlocks
        toolbarActiveBlockIndex = plan.index.coerceAtMost(newBlocks.size - 1)
        contentValue = TextFieldValue(text = "", selection = TextRange(0))
        activeSelection = 0..0
        saveBlocksToHistory()
        return plan.index
    }

    fun insertAudioBlock(src: String) {
        insertBlockAtCursor(DataBlock(type = BlockType.AUDIO, content = src))
    }

    fun applyAudioSelection(src: String) {
        val replaceIdx = audioDialogMode
        audioDialogMode = -1
        if (replaceIdx >= 0 && replaceIdx in blocks.indices) {
            val newBlocks = blocks.toMutableList()
            newBlocks[replaceIdx] = blocks[replaceIdx].copy(content = src)
            applyBlocksChange(newBlocks)
        } else {
            insertAudioBlock(src)
        }
    }

    fun insertFileBlock(path: String, name: String) {
        insertBlockAtCursor(DataBlock(type = BlockType.FILE, content = path, meta = mapOf("name" to name)))
    }

    fun applyFileSelection(path: String, name: String) {
        val replaceIdx = fileDialogMode
        fileDialogMode = -1
        if (replaceIdx >= 0 && replaceIdx in blocks.indices) {
            val newBlocks = blocks.toMutableList()
            newBlocks[replaceIdx] = blocks[replaceIdx].copy(
                content = path,
                meta = blocks[replaceIdx].meta + ("name" to name)
            )
            applyBlocksChange(newBlocks)
        } else {
            insertFileBlock(path, name)
        }
    }

    fun refreshBookmarkMeta(idx: Int, url: String) {
        if (idx !in blocks.indices || url.isBlank()) return
        fetchingBookmarkIndices.value = fetchingBookmarkIndices.value + idx
        scope.launch {
            val meta = BookmarkMetadataFetcher.fetch(url)
            val current = blocks.getOrNull(idx)
            if (current != null && current.type == BlockType.BOOKMARK && current.content == url) {
                val newBlocks = blocks.toMutableList()
                newBlocks[idx] = current.copy(meta = current.meta + mapOf(
                    "title" to meta.title,
                    "description" to meta.description,
                    "favicon" to meta.favicon
                ))
                blocks = newBlocks
                saveBlocksToHistory()
            }
            fetchingBookmarkIndices.value = fetchingBookmarkIndices.value - idx
        }
    }

    fun insertBookmarkBlock(rawUrl: String) {
        // El menú se cierra **antes** de validar la URL. Es lo que había, y no es un descuido: si la
        // URL sale mal, el menú tampoco debe reaparecer. Por eso esta línea no puede delegarse solo
        // en `insertBlockAtCursor`, que se ejecuta más abajo y no se alcanza con una URL vacía.
        showSlashMenu = false
        val url = normalizeBookmarkUrl(rawUrl)
        if (url.isBlank()) return
        val idx = insertBlockAtCursor(
            DataBlock(
                type = BlockType.BOOKMARK,
                content = url,
                meta = mapOf(
                    "title" to (BookmarkMetadataFetcher.hostOf(url) ?: url),
                    "favicon" to BookmarkMetadataFetcher.faviconFor(url)
                )
            )
        )
        refreshBookmarkMeta(idx, url)
    }

    fun applyBookmarkUrl(rawUrl: String) {
        val url = normalizeBookmarkUrl(rawUrl)
        if (url.isBlank()) return
        val replaceIdx = bookmarkDialogMode
        bookmarkDialogMode = -1
        if (replaceIdx >= 0 && replaceIdx in blocks.indices && blocks[replaceIdx].type == BlockType.BOOKMARK) {
            val newBlocks = blocks.toMutableList()
            newBlocks[replaceIdx] = blocks[replaceIdx].copy(
                content = url,
                meta = blocks[replaceIdx].meta - setOf("title", "description", "favicon")
            )
            blocks = newBlocks
            saveBlocksToHistory()
            refreshBookmarkMeta(replaceIdx, url)
        } else {
            insertBookmarkBlock(url)
        }
    }

    fun insertVideoBlock(src: String) {
        insertBlockAtCursor(DataBlock(type = BlockType.VIDEO, content = src))
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
        val meta = if (linkUrl != null) mapOf("linkUrl" to linkUrl) else emptyMap()
        insertBlockAtCursor(DataBlock(type = BlockType.IMAGE, content = src, meta = meta))
    }

    fun insertDrawingBlock() {
        insertBlockAtCursor(
            DataBlock(type = BlockType.DRAWING, content = "", meta = mapOf("wysiwyg" to "true"))
        )
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
        val nuevos = moveBlockBy(blocks, idx, -1) ?: return
        toolbarActiveBlockIndex = idx - 1
        applyBlocksChange(nuevos)
    }

    fun moveActiveBlockDown() {
        val idx = toolbarActiveBlockIndex
        val nuevos = moveBlockBy(blocks, idx, 1) ?: return
        toolbarActiveBlockIndex = idx + 1
        applyBlocksChange(nuevos)
    }

    fun deleteActiveBlock() {
        val nuevos = deleteBlockAt(blocks, toolbarActiveBlockIndex) ?: return
        applyBlocksChange(nuevos)
    }

    fun convertActiveBlock(type: BlockType, meta: Map<String, String> = emptyMap()) {
        val nuevos = convertBlockAt(blocks, toolbarActiveBlockIndex, type, meta) ?: return
        applyBlocksChange(nuevos)
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
    // Edición de ecuación existente: (índice de bloque, offset del placeholder). Null = insertar.
    var equationEditTarget by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    var attachments by remember { mutableStateOf<List<Attachment>>(emptyList()) }
    var showVoiceFileSheet by remember { mutableStateOf(false) }
    var showMoreFormattingSheet by remember { mutableStateOf(false) }

    // El guardado vive aquí y no más abajo por una razón que no es estética: Kotlin **no admite
    // referencias adelantadas a funciones locales**, y los dos efectos de la IA de abajo lo llaman.
    // Una `var` sí se puede capturar desde una lambda declarada antes (el cuerpo del efecto se
    // evalúa cuando corre), pero una función local se resuelve al compilar, en orden.
    val contentForSave: () -> String = {
        val jsonContent = DataBlock.serialize(blocks)
        if (attachments.isEmpty()) jsonContent
        else createRawContent(jsonContent, attachments)
    }

    /**
     * Guarda la nota que se está editando (RF-85).
     *
     * **El único sitio** por el que se envía, salvo la llamada de `moveBlockToAnotherNote`, que
     * guarda *otra* nota con los campos de aquella. El contenido llega como parámetro porque quien
     * llama puede tener algo ya serializado y volver a hacerlo aquí sería tirar el trabajo.
     *
     * **No lleva guarda de «¿hay algo que guardar?»**: esa vigilancia se queda donde ya estaba, que
     * es [saveCurrentNote] y el guardado al salir. Añadirla aquí cambiaría lo que se guarda en los
     * caminos que hoy guardan siempre (RF-85, RNF-39).
     */
    fun persistNote(content: String) {
        viewModel.saveNote(
            id = noteId,
            title = title.trim(),
            content = content,
            isEncrypted = isEncrypted,
            tagsList = selectedNoteTags,
            backgroundColor = selectedBgColorId,
            backgroundImagePath = selectedBgImagePath,
            isPinned = isPinned,
            isFavorite = isFavorite,
            isArchived = isArchived
        )
    }

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
        persistNote(contentForSave())
        aiViewModel.clearInPlaceResult()
    }

    // Level 3: Streaming insertion — animate segment insertion char by char
    LaunchedEffect(pendingAiInsert, contentLoaded) {
        val text = pendingAiInsert ?: return@LaunchedEffect
        if (!contentLoaded || text.isEmpty()) return@LaunchedEffect
        val segs = activeSegments()
        if (segs.isEmpty()) return@LaunchedEffect
        val plain = com.example.util.RichTextConverter.segmentsToPlainText(segs)
        // La inserción streaming va **desde** el cursor, nunca sustituye una selección: se escribe letra
        // a letra y lo que hubiera seleccionado se queda delante.
        val insertFrom = contentValue.selection.start.coerceIn(0, plain.length)
        var currentSegs = segs
        for (i in text.indices) {
            val chunk = text.substring(0, i + 1)
            currentSegs = com.example.util.RichTextConverter.replaceTextRange(currentSegs, insertFrom, insertFrom + i, chunk)
            if (toolbarActiveBlockIndex in blocks.indices && blocks[toolbarActiveBlockIndex].contentIsText) {
                val updated = blocks[toolbarActiveBlockIndex].copy(
                    content = "",
                    richTextJson = com.example.data.model.TextSegment.serialize(currentSegs)
                )
                blocks = blocks.toMutableList().apply { set(toolbarActiveBlockIndex, updated) }
            }
            kotlinx.coroutines.delay(15)
        }
        commitSegmentsWithSelection(currentSegs, insertFrom + text.length)
        persistNote(contentForSave())
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
            val tempFile = File(
                MediaStorageHelper.mediaDir(context),
                "${prefix}_${System.currentTimeMillis()}.$ext"
            )
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
            scope.launch {
                val imported = MediaStorageHelper.importMedia(context, it, noteId, "img")
                applyImageSelection(imported ?: it.toString())
            }
        }
    }

    val insertVideoGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            acquireUriPermission(it)
            scope.launch {
                val imported = MediaStorageHelper.importMedia(context, it, noteId, "vid")
                applyVideoSelection(imported ?: it.toString())
            }
        }
    }

    var originalNote: Note? by remember { mutableStateOf(null) }
    var showTextBgColorSheet by remember { mutableStateOf(false) }
    var toolbarParseResult by remember { mutableStateOf(com.example.util.RichTextConverter.parseResultFor(emptyList())) }

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
        // Sustituye el bloque cuando **es la barra que se estaba escribiendo**, no cuando está
        // vacío: es la misma forma que al insertar un medio, pero otra regla, y por eso la regla
        // se queda aquí y el índice es lo único que se comparte.
        val esPlaceholder = blocks.getOrNull(currentIdx)
            ?.ensureSegments()
            ?.let { com.example.util.RichTextConverter.segmentsToPlainText(it) }
            ?.startsWith("/") == true
        val idx = insertionIndex(currentIdx, esPlaceholder, blocks.size)
        val newBlocks = blocks.toMutableList()
        if (esPlaceholder) {
            newBlocks[currentIdx] = block
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

    val hasContentToSave: () -> Boolean = {
        title.isNotBlank() || content.isNotBlank() || attachments.isNotEmpty()
    }

    LaunchedEffect(title, content, attachments.size, noteId) {
        draftGuard?.hasUnsaved = noteId == 0 && hasContentToSave()
    }

    draftGuard?.saveAction = { onSaved ->
        scope.launch {
            val newId = viewModel.saveNoteAndGetId(
                id = 0,
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
            onSaved(newId)
        }
    }

    fun saveCurrentNote() {
        if (!hasContentToSave()) return
        persistNote(contentForSave())
    }

    // Persist media/text on background (e.g. process death within the autosave debounce).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, noteId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && noteId != 0) {
                saveCurrentNote()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }


    fun handlePickedFile(selectedUri: Uri) {
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
            val mime = contentResolver.getType(selectedUri) ?: ""
            // Si es un audio o un archivo cualquiera lo decide la lógica pura; aquí solo el I/O.
            val esAudio = isAudioFile(mime, extension)
            val prefix = if (esAudio) "audio" else "file"
            val localFile = File(context.filesDir, "${prefix}_${noteId}_${System.currentTimeMillis()}.${extension}")
            contentResolver.openInputStream(selectedUri)?.use { input ->
                FileOutputStream(localFile).use { output ->
                    input.copyTo(output)
                }
            }

            if (esAudio) {
                applyAudioSelection(localFile.absolutePath)
                Toast.makeText(context, context.getString(R.string.toast_audio_attached), Toast.LENGTH_SHORT).show()
            } else {
                applyFileSelection(localFile.absolutePath, name)
                Toast.makeText(context, context.getString(R.string.toast_file_attached), Toast.LENGTH_SHORT).show()
            }
            showVoiceFileSheet = false
            scope.launch {
                persistNote(contentForSave())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, context.getString(R.string.toast_file_select_error) + ": ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handlePickedFile(it) }
    }
    val decreaseIndent: () -> Unit = {
                adjustIndent(-1)
    }

    fun openExternalFile(path: String) {
        try {
            val file = File(path)
            if (!file.exists()) {
                Toast.makeText(context, context.getString(R.string.toast_file_open_error), Toast.LENGTH_SHORT).show()
                return
            }
            val ext = path.substringAfterLast('.', "").lowercase()
            val mime = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            when (externalTargetFor(mime, ext)) {
                ExternalTarget.PDF -> onOpenPdfTab(uri, file.name)
                ExternalTarget.TEXTO -> onOpenTextTab(uri, file.name)
                // Sin tipo no hay Intent que construir: callar es mejor que abrir un error.
                ExternalTarget.SIN_TIPO -> return
                ExternalTarget.VER_CON_OTRA_APP -> context.startActivity(
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, mime)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                )
            }
        } catch (e: Exception) {
            Log.e("NoteEditorScreen", "openExternalFile failed", e)
            Toast.makeText(context, context.getString(R.string.toast_file_open_error), Toast.LENGTH_SHORT).show()
        }
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
            BlockAction.FILE_DIALOG -> {
                clearSlashPlaceholder()
                fileDialogMode = -1
                filePickerLauncher.launch("*/*")
            }
            BlockAction.BOOKMARK_DIALOG -> {
                clearSlashPlaceholder()
                bookmarkDialogMode = -1
                bookmarkUrlInput = ""
                showBookmarkUrlDialog = true
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
                equationEditTarget = null
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
                persistNote(contentForSave())
            }
        }
    }

    val handleSaveAndExit = {
        // La guarda era la de `hasContentToSave` más un flag que nunca se activaba, así que es
        // exactamente la de `saveCurrentNote` (RF-84).
        if (hasContentToSave()) {
            saveCurrentNote()
            Toast.makeText(context, context.getString(R.string.toast_note_saved), Toast.LENGTH_SHORT).show()
        }
        onBack()
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

    /** Las etiquetas que encienden y apagan un tramo. Las de título y la sangría van aparte. */
    val STYLE_TAGS = setOf(
        "b", "i", "u", "s", "code", "sub", "sup", "mark", "small", "kbd", "var", "samp", "normal", "quote"
    )

    fun applyTag(tag: String) {
        if (blocks.getOrNull(toolbarActiveBlockIndex)?.type == BlockType.CODE_BLOCK) return
        if (tag in setOf("h1", "h2", "h3", "h4", "h5", "h6")) {
            convertBlockToHeading(tag)
            return
        }
        if (tag == "indent") {
            adjustIndent(+1)
            return
        }
        if (tag !in STYLE_TAGS) return
        val segs = activeSegments()
        if (segs.isEmpty()) return
        // «normal» no conmuta: apaga todos los formatos a la vez, se ponga lo que se ponga debajo.
        val quita = tag == "normal"
        if (quita) pendingTypingStyle = null
        val r = applyTagToSelection(
            segments = segs,
            plainLength = RichTextConverter.segmentsToPlainText(segs).length,
            selStart = activeSelection.first,
            selEnd = activeSelection.last,
            currentPending = pendingTypingStyle,
            isOn = { if (quita) true else styleApplies(tag, it) },
            isAlreadyApplied = { charSeg, base ->
                (charSeg != null && !quita && styleApplies(tag, charSeg)) || styleApplies(tag, base)
            },
            transform = { seg, _ -> removeTransformForTag(tag, seg) }
        )
        pendingTypingStyle = r.pending.takeIf { it.hasTypingStyle }
        if (r.segments != null) commitSegmentsWithSelection(r.segments, activeSelection.first)
    }

    fun applyTagWithVal(tag: String, value: String) {
        if (blocks.getOrNull(toolbarActiveBlockIndex)?.type == BlockType.CODE_BLOCK) return
        val segs = activeSegments()
        if (segs.isEmpty()) return
        val esDefecto = value == "default" || value.isEmpty()
        val r = applyTagToSelection(
            segments = segs,
            plainLength = RichTextConverter.segmentsToPlainText(segs).length,
            selStart = activeSelection.first,
            selEnd = activeSelection.last,
            currentPending = pendingTypingStyle,
            isOn = { !esDefecto && tagValueOf(it, tag) == value },
            // **Solo lo pendiente**: la letra que hay debajo no entra en la decisión. Es lo que
            // hacía el editor y se conserva a propósito; ver la nota de `applyTagToSelection`.
            isAlreadyApplied = { _, base -> !esDefecto && tagValueOf(base, tag) == value },
            transform = { seg, yaEstaba ->
                if (esDefecto || yaEstaba) clearTagValue(seg, tag) else setTagValue(seg, tag, value)
            }
        )
        pendingTypingStyle = r.pending.takeIf { it.hasTypingStyle }
        if (r.segments != null) commitSegmentsWithSelection(r.segments, activeSelection.first)
    }

    BackHandler(onBack = handleSaveAndExit)

    Scaffold(
      modifier = Modifier
        .statusBarsPadding()
        .navigationBarsPadding(),
        topBar = {
          Column {
            tabBarContent()
            TopAppBar(
            title = {},
            navigationIcon = {
              IconButton(onClick = handleSaveAndExit) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
              }
            },
            actions = {
              IconButton(
                onClick = { showMoreSheet = true },
                modifier = Modifier.testTag("more_note_btn")
              ) {
                Icon(Icons.Default.MoreVert, contentDescription = stringResource(id = R.string.more_options), tint = MaterialTheme.colorScheme.primary)
              }
            }
            )            
          }
        },
      bottomBar = {
        if (isKeyboardVisible) {
                    EditorToolbarContainer(
                        // El inset vive UNA vez, en el bottomBar: `ime` gana con el teclado abierto,
                        // `navigationBars` con el teclado cerrado. El `Box` de contenido ya no repite
                        // el `imePadding()`, que además duplicaba el `innerPadding` del Scaffold.
                        modifier = Modifier.windowInsetsPadding(
                            WindowInsets.ime.union(WindowInsets.navigationBars)
                        ),
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
                        when {
                            isSpeaking -> {
                                tts?.stop()
                                ttsPaused = true
                                isSpeaking = false
                            }
                            ttsPaused -> {
                                speakTts(ttsParts, ttsPartIndex.coerceIn(0, (ttsParts.size - 1).coerceAtLeast(0)))
                            }
                            else -> {
                                val sel = activeSelection
                                val parts = if (sel.first != sel.last) {
                                    val plain = com.example.util.RichTextConverter.segmentsToPlainText(activeSegments())
                                    listOf(
                                        plain.substring(
                                            sel.first.coerceIn(0, plain.length),
                                            sel.last.coerceIn(sel.first, plain.length)
                                        )
                                    )
                                } else {
                                    com.example.util.buildTtsChunks(title, blocks)
                                }
                                val clean = parts.map { it.trim() }.filter { it.isNotBlank() }
                                if (clean.isEmpty()) {
                                    Toast.makeText(context, context.getString(R.string.nothing_to_read), Toast.LENGTH_SHORT).show()
                                } else {
                                    ttsParts = clean
                                    ttsPartIndex = 0
                                    ttsPaused = false
                                    speakTts(clean, 0)
                                }
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
                            aiViewModel.attachNoteAsAttachment(noteId, title, content)
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
                .padding(innerPadding),
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


            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(id = R.string.label_title)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_title_input"),
                    shape = RoundedCornerShape(15.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

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
                    Spacer(modifier = Modifier.height(8.dp))
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
                        onConvertToMention = { idx ->
                            val block = blocks.getOrNull(idx)
                            if (block != null && block.type == BlockType.BOOKMARK) {
                                val url = block.content
                                val title = block.meta["title"]?.takeIf { it.isNotBlank() } ?: url
                                val newBlocks = blocks.toMutableList()
                                newBlocks[idx] = DataBlock(
                                    type = BlockType.TEXT,
                                    content = "",
                                    richTextJson = com.example.data.model.TextSegment.serialize(
                                        listOf(com.example.data.model.TextSegment(text = title, linkUrl = url))
                                    )
                                )
                                blocks = newBlocks
                                saveBlocksToHistory()
                            }
                        },
                        onEditImage = { idx ->
                            imageDialogMode = idx
                            showInsertImageDialog = true
                        },
                        onEditVideo = { idx ->
                            videoDialogMode = idx
                            showInsertVideoDialog = true
                        },
                        onEditAudio = { idx ->
                            audioDialogMode = idx
                            showVoiceFileSheet = true
                        },
                        onEditFile = { idx ->
                            fileDialogMode = idx
                            filePickerLauncher.launch("*/*")
                        },
                        onEditBookmark = { idx ->
                            bookmarkDialogMode = idx
                            val existing = blocks.getOrNull(idx)
                            bookmarkUrlInput = existing?.content ?: ""
                            showBookmarkUrlDialog = true
                        },
                        onRefreshBookmark = { idx ->
                            blocks.getOrNull(idx)?.let { refreshBookmarkMeta(idx, it.content) }
                        },
                        bookmarkFetchingIndexes = fetchingBookmarkIndices.value,
                        onOpenFile = { path, _ ->
                            openExternalFile(path)
                        },
                        onUrlClicked = handleUrlClick,
                        pendingTagInsert = pendingTagInsert,
                        pendingInsert = pendingInsert,
                        pendingSelection = pendingSelection,
                        pendingTypingStyle = pendingTypingStyle,
                        onActiveCursorChange = { toolbarActiveCursorOffset = it },
                        onActiveSelectionChange = { activeSelection = it },
                        onParseResult = { toolbarParseResult = it },
                        onEquationClicked = { blockIndex, latex, range ->
                            equationInput = latex
                            equationEditTarget = blockIndex to range.first
                            showEquationDialog = true
                        },
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
                    InsertUrlDialog(
                        address = urlInputAddress,
                        text = urlInputText,
                        onAddressChange = { urlInputAddress = it },
                        onTextChange = { urlInputText = it },
                        onConfirm = { address, display ->
                            val segs = activeSegments()
                            val editRange = urlEditRange
                            if (segs.isEmpty()) {
                                pendingInsert.value = "<url=$address>$display</url>"
                            } else {
                                val plain = com.example.util.RichTextConverter.segmentsToPlainText(segs)
                                val selStart = editRange?.first
                                    ?: activeSelection.first.coerceIn(0, plain.length)
                                val selEnd = editRange?.let { it.last + 1 }
                                    ?: activeSelection.last.coerceIn(0, plain.length).coerceAtLeast(selStart)
                                val insert = listOf(
                                    com.example.data.model.TextSegment(text = display, linkUrl = address)
                                )
                                val newSegs = com.example.util.RichTextConverter.insertSegments(segs, selStart, selEnd, insert)
                                commitSegmentsWithSelection(newSegs, selStart + display.length)
                            }
                        },
                        // Confirmar y cancelar hacen **lo mismo** al cerrar: limpiar los campos.
                        // Tocar fuera, en cambio, solo cierra y los deja puestos: es lo que había.
                        onCerrar = {
                            urlInputAddress = ""
                            urlInputText = ""
                            urlEditRange = null
                            showInsertUrlDialog = false
                        },
                        onDismissRequest = { showInsertUrlDialog = false }
                    )
                }
                
                // Insert Bookmark Dialog
                if (showBookmarkUrlDialog) {
                    InsertBookmarkDialog(
                        url = bookmarkUrlInput,
                        onUrlChange = { bookmarkUrlInput = it },
                        // Se entrega la URL **antes** de cerrar y de vaciar el campo, que es como
                        // estaba: al confirmar, `applyBookmarkUrl` necesita leerla.
                        onConfirm = { url -> applyBookmarkUrl(url) },
                        onCerrar = {
                            bookmarkUrlInput = ""
                            showBookmarkUrlDialog = false
                        },
                        onDismissRequest = { showBookmarkUrlDialog = false }
                    )
                }

                // Insert Table Dialog
                if (showInsertTableDialog) {
                    val tableHeaderCellLabel = stringResource(R.string.table_header_cell)
                    InsertTableDialog(
                        rows = tableRows,
                        columns = tableCols,
                        hasHeader = tableHasHeader,
                        onRowsChange = { tableRows = it },
                        onColumnsChange = { tableCols = it },
                        onHasHeaderChange = { tableHasHeader = it },
                        onConfirm = { filas, columnas, conCabecera ->
                            // La cabecera es la primera fila y solo existe si se ha pedido: antes se
                            // creaba siempre, con cadenas vacías cuando no había cabecera, y quedaba
                            // una fila fantasma que no era de nadie (RF-48).
                            val tableData = TableData(
                                rows = buildTableRows(filas, columnas, conCabecera) {
                                    String.format(tableHeaderCellLabel, it)
                                },
                                showHeader = conCabecera
                            )
                            handleSlashSelect(
                                DataBlock(
                                    type = BlockType.TABLE,
                                    content = "",
                                    meta = mapOf("table" to tableData.toJson())
                                )
                            )
                        },
                        onCerrar = { showInsertTableDialog = false }
                    )
                }

                // URL Click Action Dialog (Open, Copy, Delete)
                if (showUrlDialog) {
                    val clipboard = LocalClipboard.current
                    UrlActionDialog(
                        url = clickedUrlAddress,
                        // Cada acción cierra ella misma: el orden entre cerrar este diálogo y abrir
                        // el otro está en el `onEdit`, y por eso no hay un `onCerrar` compartido.
                        onOpen = {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(clickedUrlAddress)))
                            } catch (e: Exception) {
                                Toast.makeText(context, context.getString(R.string.toast_cannot_open_url), Toast.LENGTH_SHORT).show()
                            }
                            showUrlDialog = false
                        },
                        onCopy = {
                            scope.launch {
                                clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("secure_notes", clickedUrlAddress)))
                            }
                            Toast.makeText(context, context.getString(R.string.url_dialog_copy) + ": " + clickedUrlAddress, Toast.LENGTH_SHORT).show()
                            showUrlDialog = false
                        },
                        onEdit = {
                            val segs = activeSegments()
                            val offset = clickedUrlAbsoluteOffset.coerceIn(0,
                                com.example.util.RichTextConverter.segmentsToPlainText(segs).length)
                            val url = findUrlAt(segs, offset)
                            if (url != null) {
                                urlInputAddress = url.segment.linkUrl ?: ""
                                urlInputText = url.segment.text
                                urlEditRange = url.range
                                showUrlDialog = false
                                showInsertUrlDialog = true
                            } else {
                                showUrlDialog = false
                            }
                        },
                        onDelete = {
                            val segs = activeSegments()
                            val offset = clickedUrlAbsoluteOffset.coerceIn(0,
                                com.example.util.RichTextConverter.segmentsToPlainText(segs).length)
                            val url = findUrlAt(segs, offset)
                            if (url != null) {
                                val newSegs = com.example.util.RichTextConverter.replaceTextRange(
                                    segs, url.start, url.start + url.segment.text.length, ""
                                )
                                commitSegmentsWithSelection(newSegs, url.start)
                            }
                            showUrlDialog = false
                        },
                        onCerrar = { showUrlDialog = false }
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
        // El `if` se queda aquí y no dentro del panel: quien tiene la bandera decide si la superficie
        // existe (decisión 7 del plan), igual que con las hojas.
        if (showAiPanel) {
            NoteEditorAiPanel(
                prompt = aiPromptInput,
                onPromptChange = { aiPromptInput = it },
                onRunOverWholeNote = { action ->
                    aiSelectionStart = 0; aiSelectionEnd = contentValue.text.length
                    executeAiAction(action)
                    showAiPanel = false
                },
                onOpenChat = {
                    aiViewModel.attachNoteAsAttachment(noteId, title, content)
                    showAiPanel = false
                    onNavigateToAiChat(noteId)
                },
                onRunPrompt = {
                    aiSelectionStart = activeSelection.first
                    aiSelectionEnd = activeSelection.last
                    val target = contentValue.text.substring(aiSelectionStart, aiSelectionEnd)
                        .takeIf { aiSelectionStart != aiSelectionEnd } ?: content
                    aiViewModel.executeInPlace(AiAction.GENERATE, "$target\n\n$aiPromptInput")
                    aiPromptInput = ""
                    showAiPanel = false
                },
                processing = inPlaceProcessing,
                onDismiss = { showAiPanel = false }
            )
        }

        // Palette Bottom Sheet
        if (showPaletteSheet) {
            NoteEditorPaletteSheet(
                selectedColorId = selectedBgColorId,
                onColorSelected = { selectedBgColorId = it },
                backgroundImagePath = selectedBgImagePath,
                onPickImage = { imagePickerLauncher.launch("image/*") },
                onClearImage = { selectedBgImagePath = null },
                isDark = isDark,
                onDismiss = { showPaletteSheet = false }
            )
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
            onDismiss = { showMoreSheet = false },
          onOpenPalette = { showPaletteSheet = true }
        )

        // Voice and File Attachment Bottom Sheet
        //
        // El `if` NO se mueve dentro de la hoja (decisión 7 del plan): sus `remember` —el grabador,
        // el reproductor, el archivo grabado— se descartan con la hoja, y si el hijo se
        // autocondicionara por su cuenta una grabación seguiría viva con la hoja cerrada.
        if (showVoiceFileSheet) {
            NoteEditorVoiceFileSheet(
                noteId = noteId,
                onInsertImage = {
                    showVoiceFileSheet = false
                    imageDialogMode = -1
                    showInsertImageDialog = true
                },
                onInsertVideo = {
                    showVoiceFileSheet = false
                    videoDialogMode = -1
                    showInsertVideoDialog = true
                },
                onPickFile = { filePickerLauncher.launch("*/*") },
                onAttachRecording = { path ->
                    applyAudioSelection(path)
                    scope.launch {
                        persistNote(contentForSave())
                    }
                    showVoiceFileSheet = false
                },
                onDismiss = {
                    showVoiceFileSheet = false
                    audioDialogMode = -1
                }
            )
        }

        // ── Level 1: AI Context Actions Bottom Sheet ──────────
        if (showAiContextSheet) {
            NoteEditorAiContextSheet(
                onRunAction = { action -> executeAiAction(action) },
                onDismiss = { showAiContextSheet = false }
            )
        }

        // ── AI Streaming / Result Indicator ───────────────────
        // `inPlaceAction` es una propiedad delegada, y Kotlin no hace smart cast sobre las delegadas:
        // se lee una vez a un `val` y se pasa ese.
        val accionEnCurso = inPlaceAction
        if (accionEnCurso != null && inPlaceProcessing) {
            NoteEditorAiProgressDialog(
                action = accionEnCurso,
                streamingText = inPlaceStreamingText,
                onCancel = { aiViewModel.cancelGeneration() }
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
            EquationDialog(
                latex = equationInput,
                isEdit = equationEditTarget != null,
                onLatexChange = { equationInput = it },
                onConfirm = { latex ->
                    val target = equationEditTarget
                    if (target != null) applyEquationEdit(target.first, target.second, latex)
                    else pendingInsert.value = "<eq>$latex</eq>"
                },
                onCerrar = {
                    equationInput = ""
                    equationEditTarget = null
                    showEquationDialog = false
                }
            )
        }

    }
}

private fun normalizeBookmarkUrl(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return ""
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
    if (Uri.parse(trimmed).host != null) return "https://$trimmed"
    return trimmed
}
