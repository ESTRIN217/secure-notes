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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import com.example.data.model.Note
import com.example.getNoteBackgroundColor
import com.example.ui.viewmodel.DecryptedNote
import com.example.ui.viewmodel.NotesViewModel
import com.example.util.ExportUtils
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.speech.tts.TextToSpeech
import android.media.MediaPlayer
import android.media.MediaRecorder
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.launch
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.example.util.RichTextParser
import com.example.util.MediaBlock
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.BorderStroke
import androidx.core.content.FileProvider

sealed interface PreviewItem {
    data class LegacyAttachment(val attachment: Attachment) : PreviewItem
    data class MediaTag(val type: String, val src: String) : PreviewItem
}

sealed interface NoteContentBlock {
    data class TextBlock(
        val parseResult: RichTextParser.ParseResult,
        val rawStart: Int
    ) : NoteContentBlock {
        val annotatedString: androidx.compose.ui.text.AnnotatedString get() = parseResult.text
    }

    data class ChecklistItemBlock(
        val isChecked: Boolean,
        val parseResult: RichTextParser.ParseResult,
        val rawStart: Int,
        val globalIndex: Int
    ) : NoteContentBlock {
        val text: androidx.compose.ui.text.AnnotatedString get() = parseResult.text
    }

    data class ImageBlock(val src: String) : NoteContentBlock
    data class VideoBlock(val src: String) : NoteContentBlock
    data class AudioBlock(val src: String) : NoteContentBlock
}

fun highlightMatches(
    annotatedString: androidx.compose.ui.text.AnnotatedString,
    query: String,
    caseSensitive: Boolean,
    fullWord: Boolean,
    currentIndex: Int,
    highlightColor: androidx.compose.ui.graphics.Color,
    currentHighlightColor: androidx.compose.ui.graphics.Color
): androidx.compose.ui.text.AnnotatedString {
    if (query.isEmpty()) return annotatedString
    val text = annotatedString.text
    val builder = androidx.compose.ui.text.AnnotatedString.Builder(annotatedString)
    
    val ranges = mutableListOf<IntRange>()
    if (fullWord) {
        val escapedQuery = Regex.escape(query)
        val patternString = "\\b$escapedQuery\\b"
        val options = if (caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
        try {
            val regex = Regex(patternString, options)
            regex.findAll(text).forEach { matchResult ->
                ranges.add(matchResult.range)
            }
        } catch (e: Exception) {
            var idx = text.indexOf(query, 0, ignoreCase = !caseSensitive)
            while (idx != -1) {
                ranges.add(idx until (idx + query.length))
                idx = text.indexOf(query, idx + 1, ignoreCase = !caseSensitive)
            }
        }
    } else {
        var idx = text.indexOf(query, 0, ignoreCase = !caseSensitive)
        while (idx != -1) {
            ranges.add(idx until (idx + query.length))
            idx = text.indexOf(query, idx + 1, ignoreCase = !caseSensitive)
        }
    }
    
    ranges.forEachIndexed { index, range ->
        val color = if (index == currentIndex) currentHighlightColor else highlightColor
        builder.addStyle(
            style = androidx.compose.ui.text.SpanStyle(
                background = color,
                color = if (index == currentIndex) androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.Unspecified
            ),
            start = range.first,
            end = range.last + 1
        )
    }
    
    return builder.toAnnotatedString()
}

fun findEnclosingUrlTagRange(rawText: String, offset: Int): IntRange? {
    var startIdx = -1
    var temp = offset
    while (temp >= 0) {
        if (rawText.startsWith("<url", temp)) {
            val closeAngle = rawText.indexOf('>', temp)
            if (closeAngle != -1 && closeAngle < rawText.length) {
                startIdx = temp
                break
            }
        }
        temp--
    }
    if (startIdx == -1) return null
    
    val endTagStart = rawText.indexOf("</url>", startIdx)
    if (endTagStart == -1) return null
    val endIdx = endTagStart + "</url>".length
    
    if (offset in startIdx..endIdx) {
        return IntRange(startIdx, endIdx - 1)
    }
    return null
}

fun toggleNthChecklistItem(rawText: String, indexToToggle: Int): String {
    val regex = Regex("<item\\s+checked=\"(true|false)\">")
    val matches = regex.findAll(rawText).toList()
    if (indexToToggle in matches.indices) {
        val match = matches[indexToToggle]
        val checkedValue = match.groupValues[1]
        val newCheckedValue = if (checkedValue == "true") "false" else "true"
        val start = match.range.first
        val end = match.range.last + 1
        val updatedTag = "<item checked=\"$newCheckedValue\">"
        return rawText.substring(0, start) + updatedTag + rawText.substring(end)
    }
    return rawText
}

fun parseToContentBlocks(rawText: String): List<NoteContentBlock> {
    val blocks = mutableListOf<NoteContentBlock>()
    
    val regex = Regex(
        "(?:!\\s*\\[([^\\]]*)\\]\\(([^\\)]+)\\))" +
        "|(?:!video\\s*\\[([^\\]]*)\\]\\(([^\\)]+)\\))" +
        "|(?:!audio\\s*\\[([^\\]]*)\\]\\(([^\\)]+)\\))" +
        "|(?:<item\\s+checked=\"(true|false)\">([\\s\\S]*?)</item>)" +
        "|(?:<(img|video|audio)\\s+src=\"([^\"]+)\"\\s*/>|<(img|video|audio)=([^>]+)>)" +
        "|(<cl>|</cl>)"
    )
    
    var lastIndex = 0
    val matches = regex.findAll(rawText)
    var checklistItemIndex = 0
    
    for (match in matches) {
        val preText = rawText.substring(lastIndex, match.range.first)
        if (preText.isNotEmpty()) {
            val parseResult = RichTextParser.parseWithMapping(preText, hideTags = true)
            if (parseResult.text.text.isNotBlank()) {
                blocks.add(NoteContentBlock.TextBlock(parseResult, rawStart = lastIndex))
            }
        }
        
        val isMdImg = match.groupValues[2].isNotEmpty() && match.groupValues[1].isNotEmpty() || (match.groupValues[2].isNotEmpty() && match.value.startsWith("!"))
        val isMdVideo = match.groupValues[4].isNotEmpty()
        val isMdAudio = match.groupValues[6].isNotEmpty()
        val isItem = match.groupValues[7].isNotEmpty()
        val isHtmlMedia = match.groupValues[9].isNotEmpty()
        val isHtmlShortMedia = match.groupValues[11].isNotEmpty()
        
        when {
            isMdImg -> {
                val src = match.groupValues[2]
                blocks.add(NoteContentBlock.ImageBlock(src))
            }
            isMdVideo -> {
                val src = match.groupValues[4]
                blocks.add(NoteContentBlock.VideoBlock(src))
            }
            isMdAudio -> {
                val src = match.groupValues[6]
                blocks.add(NoteContentBlock.AudioBlock(src))
            }
            isItem -> {
                val isChecked = match.groupValues[7] == "true"
                val itemText = match.groupValues[8]
                val parseResult = RichTextParser.parseWithMapping(itemText, hideTags = true)
                val relativeStart = match.value.indexOf(itemText)
                val itemTextStart = match.range.first + relativeStart
                blocks.add(NoteContentBlock.ChecklistItemBlock(
                    isChecked = isChecked,
                    parseResult = parseResult,
                    rawStart = itemTextStart,
                    globalIndex = checklistItemIndex
                ))
                checklistItemIndex++
            }
            isHtmlMedia -> {
                val mediaType = match.groupValues[9]
                val src = match.groupValues[10]
                when (mediaType) {
                    "img" -> blocks.add(NoteContentBlock.ImageBlock(src))
                    "video" -> blocks.add(NoteContentBlock.VideoBlock(src))
                    "audio" -> blocks.add(NoteContentBlock.AudioBlock(src))
                }
            }
            isHtmlShortMedia -> {
                val mediaType = match.groupValues[11]
                val src = match.groupValues[12]
                when (mediaType) {
                    "img" -> blocks.add(NoteContentBlock.ImageBlock(src))
                    "video" -> blocks.add(NoteContentBlock.VideoBlock(src))
                    "audio" -> blocks.add(NoteContentBlock.AudioBlock(src))
                }
            }
        }
        
        lastIndex = match.range.last + 1
    }
    
    if (lastIndex < rawText.length) {
        val remainingText = rawText.substring(lastIndex)
        if (remainingText.isNotEmpty()) {
            val parseResult = RichTextParser.parseWithMapping(remainingText, hideTags = true)
            if (parseResult.text.text.isNotBlank()) {
                blocks.add(NoteContentBlock.TextBlock(parseResult, rawStart = lastIndex))
            }
        }
    }
    
    return blocks.ifEmpty { 
        val parseResult = RichTextParser.parseWithMapping(rawText, hideTags = true)
        listOf(NoteContentBlock.TextBlock(parseResult, rawStart = 0))
    }
}

@Composable
fun AudioPlayerWidget(path: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var isPlayingAudio by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    
    DisposableEffect(path) {
        onDispose {
            mediaPlayer?.let { mp ->
                try {
                    if (mp.isPlaying) {
                        mp.stop()
                    }
                } catch (e: Exception) {
                    // ignore
                }
                try {
                    mp.release()
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
    }
    
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = {
                if (isPlayingAudio) {
                    mediaPlayer?.let { mp ->
                        try {
                            if (mp.isPlaying) {
                                mp.stop()
                            }
                        } catch (e: Exception) {
                            // ignore
                        }
                        try {
                            mp.release()
                        } catch (e: Exception) {
                            // ignore
                        }
                    }
                    mediaPlayer = null
                    isPlayingAudio = false
                } else {
                    try {
                        val mp = MediaPlayer().apply {
                            setDataSource(context, Uri.parse(path))
                            prepare()
                            start()
                            setOnCompletionListener {
                                isPlayingAudio = false
                                try {
                                    release()
                                } catch (e: Exception) {
                                    // ignore
                                }
                                mediaPlayer = null
                            }
                        }
                        mediaPlayer = mp
                        isPlayingAudio = true
                    } catch (e: Exception) {
                        try {
                            val mp = MediaPlayer().apply {
                                setDataSource(path)
                                prepare()
                                start()
                                setOnCompletionListener {
                                    isPlayingAudio = false
                                    try {
                                        release()
                                    } catch (e: Exception) {
                                        // ignore
                                    }
                                    mediaPlayer = null
                                }
                            }
                            mediaPlayer = mp
                            isPlayingAudio = true
                        } catch (e2: Exception) {
                            Toast.makeText(context, "Error playing audio", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = if (isPlayingAudio) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Play/Pause Audio",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    noteId: Int,
    viewModel: NotesViewModel,
    onBack: () -> Unit,
    onNavigateToDrawing: (Int, String?) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isPasswordSet by viewModel.isPasswordSet.collectAsState()
    
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
                        val rawStart = parseResult.transformedToSource.getOrElse(cleanStart) { cleanStart }
                        val rawEnd = parseResult.transformedToSource.getOrElse(cleanEnd) { cleanEnd }
                        ranges.add(TextRange(rawStart, rawEnd))
                    }
                } catch (e: Exception) {
                    var idx = cleanText.indexOf(searchQuery, 0, ignoreCase = !searchCaseSensitive)
                    while (idx != -1) {
                        val cleanStart = idx
                        val cleanEnd = idx + searchQuery.length
                        val rawStart = parseResult.transformedToSource.getOrElse(cleanStart) { cleanStart }
                        val rawEnd = parseResult.transformedToSource.getOrElse(cleanEnd) { cleanEnd }
                        ranges.add(TextRange(rawStart, rawEnd))
                        idx = cleanText.indexOf(searchQuery, idx + 1, ignoreCase = !searchCaseSensitive)
                    }
                }
            } else {
                var idx = cleanText.indexOf(searchQuery, 0, ignoreCase = !searchCaseSensitive)
                while (idx != -1) {
                    val cleanStart = idx
                    val cleanEnd = idx + searchQuery.length
                    val rawStart = parseResult.transformedToSource.getOrElse(cleanStart) { cleanStart }
                    val rawEnd = parseResult.transformedToSource.getOrElse(cleanEnd) { cleanEnd }
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
    val previewItems = remember(attachments, content) {
        val items = mutableListOf<PreviewItem>()
        attachments.forEach { items.add(PreviewItem.LegacyAttachment(it)) }
        val regex = Regex("<(img|video|audio)\\s+src=\"([^\"]+)\"\\s*/>")
        regex.findAll(content).forEach { match ->
            val tagType = match.groupValues[1]
            val src = match.groupValues[2]
            val normType = when (tagType) {
                "img" -> "image"
                "video" -> "video"
                "audio" -> "audio"
                else -> tagType
            }
            items.add(PreviewItem.MediaTag(normType, src))
        }
        items
    }
    var showVoiceFileSheet by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(it, takeFlags)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            selectedBgImagePath = it.toString()
        }
    }

    var pendingCameraType by remember { mutableStateOf<String?>(null) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var cameraVideoUri by remember { mutableStateOf<Uri?>(null) }

    val insertImageCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            cameraImageUri?.let { uri ->
                val selStart = contentValue.selection.start
                val selEnd = contentValue.selection.end
                val text = contentValue.text
                val mediaTag = "<img src=\"$uri\" />"
                val newText = text.substring(0, selStart) + mediaTag + text.substring(selEnd)
                val newCursor = selStart + mediaTag.length
                contentValue = TextFieldValue(text = newText, selection = TextRange(newCursor))
                content = newText
                saveToHistory(newText)
            }
        }
    }

    val insertVideoCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success: Boolean ->
        if (success) {
            cameraVideoUri?.let { uri ->
                val selStart = contentValue.selection.start
                val selEnd = contentValue.selection.end
                val text = contentValue.text
                val mediaTag = "<video src=\"$uri\" />"
                val newText = text.substring(0, selStart) + mediaTag + text.substring(selEnd)
                val newCursor = selStart + mediaTag.length
                contentValue = TextFieldValue(text = newText, selection = TextRange(newCursor))
                content = newText
                saveToHistory(newText)
            }
        }
    }

    val launchCameraImage = {
        try {
            val tempFile = File(context.cacheDir, "img_${System.currentTimeMillis()}.jpg")
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, tempFile)
            cameraImageUri = uri
            insertImageCameraLauncher.launch(uri)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error starting camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    val launchCameraVideo = {
        try {
            val tempFile = File(context.cacheDir, "vid_${System.currentTimeMillis()}.mp4")
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, tempFile)
            cameraVideoUri = uri
            insertVideoCameraLauncher.launch(uri)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error starting camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (pendingCameraType == "image") {
                launchCameraImage()
            } else if (pendingCameraType == "video") {
                launchCameraVideo()
            }
        } else {
            Toast.makeText(context, "Camera permission is required to take photos/videos", Toast.LENGTH_SHORT).show()
        }
        pendingCameraType = null
    }

    val checkCameraPermissionAndLaunch: (String) -> Unit = { type ->
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        
        if (hasPermission) {
            if (type == "image") {
                launchCameraImage()
            } else {
                launchCameraVideo()
            }
        } else {
            pendingCameraType = type
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    val insertImageGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(it, takeFlags)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val selStart = contentValue.selection.start
            val selEnd = contentValue.selection.end
            val text = contentValue.text
            val mediaTag = "<img src=\"$it\" />"
            val newText = text.substring(0, selStart) + mediaTag + text.substring(selEnd)
            val newCursor = selStart + mediaTag.length
            contentValue = TextFieldValue(text = newText, selection = TextRange(newCursor))
            content = newText
            saveToHistory(newText)
        }
    }

    val insertVideoGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(it, takeFlags)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val selStart = contentValue.selection.start
            val selEnd = contentValue.selection.end
            val text = contentValue.text
            val mediaTag = "<video src=\"$it\" />"
            val newText = text.substring(0, selStart) + mediaTag + text.substring(selEnd)
            val newCursor = selStart + mediaTag.length
            contentValue = TextFieldValue(text = newText, selection = TextRange(newCursor))
            content = newText
            saveToHistory(newText)
        }
    }

    var originalNote: Note? by remember { mutableStateOf(null) }

    // Recover note details if editing
    LaunchedEffect(noteId) {
        if (noteId != 0) {
            val list = viewModel.notesList.value
            val match = list.find { it.note.id == noteId }
            if (match != null) {
                originalNote = match.note
                title = match.title
                
                // Parse content and attachments
                val (cleanText, parsedAttachments) = parseNoteContentAndAttachments(match.content)
                content = cleanText
                contentValue = TextFieldValue(text = cleanText, selection = TextRange(cleanText.length))
                attachments = parsedAttachments
                
                // Initialize history
                history.clear()
                history.add(cleanText)
                historyIndex = 0
                
                isEncrypted = match.note.isEncrypted
                selectedBgColorId = match.note.backgroundColor
                selectedBgImagePath = match.note.backgroundImagePath
                isPinned = match.note.isPinned
                isFavorite = match.note.isFavorite
                isArchived = match.note.isArchived
                
                // Read original note tags
                try {
                    val arr = JSONArray(match.note.tagsJson)
                    val out = mutableListOf<String>()
                    for (i in 0 until arr.length()) {
                        out.add(arr.optString(i))
                    }
                    selectedNoteTags = out
                } catch (e: Exception) {
                    selectedNoteTags = emptyList()
                }
            }
        }
    }

    LaunchedEffect(content) {
        if (content.isNotEmpty()) {
            kotlinx.coroutines.delay(800)
            saveToHistory(content)
        }
    }

    val handleSaveAndExit = {
        if (title.isNotBlank() || content.isNotBlank() || attachments.isNotEmpty()) {
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
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .statusBarsPadding()
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = handleSaveAndExit) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = if (noteId == 0) stringResource(id = R.string.btn_new_note) else stringResource(id = R.string.btn_edit),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                        IconButton(
                            onClick = { isPinned = !isPinned },
                            modifier = Modifier.testTag("pin_note_btn")
                        ) {
                            Icon(
                                imageVector = if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                contentDescription = if (isPinned) stringResource(R.string.tooltip_unpin) else stringResource(R.string.tooltip_pin),
                                tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { isFavorite = !isFavorite },
                            modifier = Modifier.testTag("favorite_note_btn")
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.Star,
                                contentDescription = if (isFavorite) stringResource(R.string.tooltip_unfavorite) else stringResource(R.string.tooltip_favorite),
                                tint = if (isFavorite) Color(0xFFFBC02D) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { isArchived = !isArchived },
                            modifier = Modifier.testTag("archive_note_btn")
                        ) {
                            Icon(
                                imageVector = if (isArchived) Icons.Default.Archive else Icons.Outlined.Archive,
                                contentDescription = if (isArchived) stringResource(R.string.tooltip_unarchive) else stringResource(R.string.tooltip_archive),
                                tint = if (isArchived) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { showMoreSheet = true },
                            modifier = Modifier.testTag("more_note_btn")
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(id = R.string.more_options), tint = MaterialTheme.colorScheme.primary)
                        }
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
                    .padding(top = 16.dp, bottom = 80.dp)
            ) {
                if (isPreviewMode) {
                    Text(
                        text = title.ifEmpty { "Untitled Note" },
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
                                        contentDescription = "Remove tag",
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
                        checked = false,
                        onCheckedChange = { applyTag("b") },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text("B", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    
                    FilledTonalIconToggleButton(
                        checked = false,
                        onCheckedChange = { applyTag("i") },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text("I", fontStyle = FontStyle.Italic, fontSize = 14.sp)
                    }
                    
                    FilledTonalIconToggleButton(
                        checked = false,
                        onCheckedChange = { applyTag("u") },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text("U", style = TextStyle(textDecoration = TextDecoration.Underline), fontSize = 14.sp)
                    }
                    
                    FilledTonalIconToggleButton(
                        checked = false,
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
                                                "normal" -> "Normal Text"
                                                "h1" -> "Heading 1"
                                                "h2" -> "Heading 2"
                                                "h3" -> "Heading 3"
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
                                    text = { Text(if (size == "default") "Default" else "${size}sp") },
                                    onClick = {
                                        applyTagWithVal("size", size)
                                        showSizeDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    VerticalDivider(modifier = Modifier.height(24.dp))

                    val activeFontColor = remember(contentValue) {
                        val parsed = RichTextParser.parseWithMapping(contentValue.text, hideTags = true)
                        val selection = contentValue.selection
                        val cursorIndex = selection.start
                        val transformedIndex = parsed.sourceToTransformed.getOrNull(cursorIndex) ?: 0
                        
                        val targetIndex = if (transformedIndex < parsed.text.length) transformedIndex else (transformedIndex - 1).coerceAtLeast(0)
                        
                        parsed.text.spanStyles.lastOrNull { range ->
                            range.start <= targetIndex && targetIndex < range.end && range.item.color != Color.Unspecified
                        }?.item?.color
                    }

                    val activeBgColor = remember(contentValue) {
                        val parsed = RichTextParser.parseWithMapping(contentValue.text, hideTags = true)
                        val selection = contentValue.selection
                        val cursorIndex = selection.start
                        val transformedIndex = parsed.sourceToTransformed.getOrNull(cursorIndex) ?: 0
                        
                        val targetIndex = if (transformedIndex < parsed.text.length) transformedIndex else (transformedIndex - 1).coerceAtLeast(0)
                        
                        parsed.text.spanStyles.lastOrNull { range ->
                            range.start <= targetIndex && targetIndex < range.end && range.item.background != Color.Unspecified && range.item.background != Color.Transparent
                        }?.item?.background
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
                                val (importedTitle, importedContent) = RichTextParser.parseSecureNotesJson(clipText)
                                title = importedTitle
                                content = importedContent
                                contentValue = TextFieldValue(text = importedContent, selection = TextRange(importedContent.length))
                                saveToHistory(importedContent)
                                Toast.makeText(context, "Note imported!", Toast.LENGTH_SHORT).show()
                            } else {
                                val newText = text.substring(0, selStart) + clipText + text.substring(selEnd)
                                val newCursor = selStart + clipText.length
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
                            imageVector = Icons.Default.FormatListBulleted,
                            contentDescription = stringResource(id = R.string.rich_bulleted_list),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Checklist Button
                    IconButton(onClick = { applyListTag("cl") }) {
                        Icon(
                            imageVector = Icons.Default.FactCheck,
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
                            imageVector = Icons.Default.FormatIndentIncrease,
                            contentDescription = stringResource(id = R.string.rich_increase_indent),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Decrease Indent Button
                    IconButton(onClick = { decreaseIndent() }) {
                        Icon(
                            imageVector = Icons.Default.FormatIndentDecrease,
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
                                        text = "${currentMatchIndex + 1}/${matchRanges.size}",
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
                                            } catch (e: Exception) {}
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
                                            } catch (e: Exception) {}
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
                                        contentDescription = "Close Search",
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
                    val blocks = remember(content) { parseToContentBlocks(content) }
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
                                        when (block) {
                                            is NoteContentBlock.TextBlock -> {
                                                if (block.annotatedString.isNotEmpty()) {
                                                    ClickableText(
                                                        text = block.annotatedString,
                                                        style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                                                        onClick = { offset ->
                                                            block.annotatedString.getStringAnnotations("URL", offset, offset)
                                                                .firstOrNull()?.let { annotation ->
                                                                    val url = annotation.item
                                                                    clickedUrlAddress = url
                                                                    clickedUrlAbsoluteOffset = block.rawStart + (block.parseResult.transformedToSource.getOrNull(offset) ?: offset)
                                                                    showUrlDialog = true
                                                                }
                                                        }
                                                    )
                                                }
                                            }
                                            is NoteContentBlock.ChecklistItemBlock -> {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            val newContent = toggleNthChecklistItem(content, block.globalIndex)
                                                            content = newContent
                                                            contentValue = TextFieldValue(text = newContent, selection = TextRange(newContent.length))
                                                            saveToHistory(newContent)
                                                        }
                                                        .padding(vertical = 4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (block.isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                                        contentDescription = if (block.isChecked) "Checked" else "Unchecked",
                                                        tint = if (block.isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier
                                                            .padding(end = 8.dp)
                                                            .size(24.dp)
                                                    )
                                                    ClickableText(
                                                        text = block.text,
                                                        style = MaterialTheme.typography.bodyLarge.copy(
                                                            color = if (block.isChecked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                                                            textDecoration = if (block.isChecked) TextDecoration.LineThrough else null
                                                        ),
                                                        onClick = { offset ->
                                                            block.text.getStringAnnotations("URL", offset, offset)
                                                                .firstOrNull()?.let { annotation ->
                                                                    val url = annotation.item
                                                                    clickedUrlAddress = url
                                                                    clickedUrlAbsoluteOffset = block.rawStart + (block.parseResult.transformedToSource.getOrNull(offset) ?: offset)
                                                                    showUrlDialog = true
                                                                } ?: run {
                                                                    val newContent = toggleNthChecklistItem(content, block.globalIndex)
                                                                    content = newContent
                                                                    contentValue = TextFieldValue(text = newContent, selection = TextRange(newContent.length))
                                                                    saveToHistory(newContent)
                                                                }
                                                        }
                                                    )
                                                }
                                            }
                                            is NoteContentBlock.ImageBlock -> {
                                                Card(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .wrapContentHeight(),
                                                    shape = RoundedCornerShape(12.dp),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                                ) {
                                                    AsyncImage(
                                                        model = block.src,
                                                        contentDescription = "Inline Image",
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(12.dp)),
                                                        contentScale = ContentScale.FillWidth
                                                    )
                                                }
                                            }
                                            is NoteContentBlock.VideoBlock -> {
                                                Card(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(200.dp),
                                                    shape = RoundedCornerShape(12.dp),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                                                            .clickable {
                                                                try {
                                                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                                                        setDataAndType(Uri.parse(block.src), "video/*")
                                                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                                    }
                                                                    context.startActivity(intent)
                                                                } catch (e: Exception) {
                                                                    Toast.makeText(context, "No app to play video", Toast.LENGTH_SHORT).show()
                                                                }
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.PlayArrow,
                                                            contentDescription = "Play Video",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(48.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            is NoteContentBlock.AudioBlock -> {
                                                Card(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .wrapContentHeight(),
                                                    shape = RoundedCornerShape(12.dp),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                                ) {
                                                    AudioPlayerWidget(
                                                        path = block.src,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(12.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val visualTransformation = remember(searchQuery, searchCaseSensitive, searchFullWord, currentMatchIndex, isSearchActive) {
                        VisualTransformation { text ->
                            val parseResult = RichTextParser.parseWithMapping(text.text, hideTags = true)
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
                                    val N = text.text.length
                                    val clamped = offset.coerceIn(0, N)
                                    return parseResult.sourceToTransformed[clamped]
                                }

                                override fun transformedToOriginal(offset: Int): Int {
                                    val M = parseResult.text.length
                                    val clamped = offset.coerceIn(0, M)
                                    return parseResult.transformedToSource[clamped]
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
                if (showInsertImageDialog) {
                    AlertDialog(
                        onDismissRequest = { 
                            showInsertImageDialog = false 
                            isImageLinkExpanded = false
                        },
                        title = { Text(stringResource(id = R.string.dialog_insert_image_title)) },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Gallery Card
                                OutlinedCard(
                                    onClick = {
                                        insertImageGalleryLauncher.launch("image/*")
                                        showInsertImageDialog = false
                                        isImageLinkExpanded = false
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Collections,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(
                                            text = stringResource(id = R.string.label_option_gallery),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Camera Card
                                OutlinedCard(
                                    onClick = {
                                        checkCameraPermissionAndLaunch("image")
                                        showInsertImageDialog = false
                                        isImageLinkExpanded = false
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PhotoCamera,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(
                                            text = stringResource(id = R.string.label_option_camera),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Link Card
                                OutlinedCard(
                                    onClick = {
                                        isImageLinkExpanded = !isImageLinkExpanded
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Link,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(
                                            text = stringResource(id = R.string.label_option_link),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (isImageLinkExpanded) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = imageInputUrl,
                                        onValueChange = { imageInputUrl = it },
                                        label = { Text(stringResource(id = R.string.label_media_url)) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            if (isImageLinkExpanded) {
                                Button(
                                    onClick = {
                                        if (imageInputUrl.isNotBlank()) {
                                            val selStart = contentValue.selection.start
                                            val selEnd = contentValue.selection.end
                                            val text = contentValue.text
                                            val mediaTag = "<img src=\"$imageInputUrl\" />"
                                            val newText = text.substring(0, selStart) + mediaTag + text.substring(selEnd)
                                            val newCursor = selStart + mediaTag.length
                                            contentValue = TextFieldValue(text = newText, selection = TextRange(newCursor))
                                            content = newText
                                            saveToHistory(newText)
                                        }
                                        imageInputUrl = ""
                                        showInsertImageDialog = false
                                        isImageLinkExpanded = false
                                    }
                                ) {
                                    Text(stringResource(id = R.string.btn_insert))
                                }
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    imageInputUrl = ""
                                    showInsertImageDialog = false
                                    isImageLinkExpanded = false
                                }
                            ) {
                                Text(stringResource(id = R.string.btn_cancel))
                            }
                        }
                    )
                }

                // Insert Video Dialog
                if (showInsertVideoDialog) {
                    AlertDialog(
                        onDismissRequest = { 
                            showInsertVideoDialog = false 
                            isVideoLinkExpanded = false
                        },
                        title = { Text(stringResource(id = R.string.dialog_insert_video_title)) },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Gallery Card
                                OutlinedCard(
                                    onClick = {
                                        insertVideoGalleryLauncher.launch("video/*")
                                        showInsertVideoDialog = false
                                        isVideoLinkExpanded = false
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Collections,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(
                                            text = stringResource(id = R.string.label_option_gallery),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Camera Card
                                OutlinedCard(
                                    onClick = {
                                        checkCameraPermissionAndLaunch("video")
                                        showInsertVideoDialog = false
                                        isVideoLinkExpanded = false
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Videocam,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(
                                            text = stringResource(id = R.string.label_option_camera),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Link Card
                                OutlinedCard(
                                    onClick = {
                                        isVideoLinkExpanded = !isVideoLinkExpanded
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Link,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text(
                                            text = stringResource(id = R.string.label_option_link),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (isVideoLinkExpanded) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = videoInputUrl,
                                        onValueChange = { videoInputUrl = it },
                                        label = { Text(stringResource(id = R.string.label_media_url)) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            if (isVideoLinkExpanded) {
                                Button(
                                    onClick = {
                                        if (videoInputUrl.isNotBlank()) {
                                            val selStart = contentValue.selection.start
                                            val selEnd = contentValue.selection.end
                                            val text = contentValue.text
                                            val mediaTag = "<video src=\"$videoInputUrl\" />"
                                            val newText = text.substring(0, selStart) + mediaTag + text.substring(selEnd)
                                            val newCursor = selStart + mediaTag.length
                                            contentValue = TextFieldValue(text = newText, selection = TextRange(newCursor))
                                            content = newText
                                            saveToHistory(newText)
                                        }
                                        videoInputUrl = ""
                                        showInsertVideoDialog = false
                                        isVideoLinkExpanded = false
                                    }
                                ) {
                                    Text(stringResource(id = R.string.btn_insert))
                                }
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    videoInputUrl = ""
                                    showInsertVideoDialog = false
                                    isVideoLinkExpanded = false
                                }
                            ) {
                                Text(stringResource(id = R.string.btn_cancel))
                            }
                        }
                    )
                }
                
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
                                        val selStart = contentValue.selection.start
                                        val selEnd = contentValue.selection.end
                                        val text = contentValue.text
                                        val display = urlInputText.ifEmpty { urlInputAddress }
                                        val urlTag = "<url=$urlInputAddress>$display</url>"
                                        val newText = text.substring(0, selStart) + urlTag + text.substring(selEnd)
                                        val newCursor = selStart + urlTag.length
                                        contentValue = TextFieldValue(text = newText, selection = TextRange(newCursor))
                                        content = newText
                                        saveToHistory(newText)
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
                                            Toast.makeText(context, "Cannot open URL", Toast.LENGTH_SHORT).show()
                                        }
                                        showUrlDialog = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.OpenInNew,
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
                                        val rangeToDelete = findEnclosingUrlTagRange(content, clickedUrlAbsoluteOffset)
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
                
                if (previewItems.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Attachments",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        previewItems.forEach { item ->
                            OutlinedCard(
                                modifier = Modifier
                                    .width(160.dp)
                                    .height(130.dp)
                                    .clickable {
                                        if (item is PreviewItem.LegacyAttachment && item.attachment.type == "drawing") {
                                            onNavigateToDrawing(noteId, item.attachment.path)
                                        }
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                )
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(4.dp),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        when (item) {
                                            is PreviewItem.LegacyAttachment -> {
                                                val att = item.attachment
                                                when (att.type) {
                                                    "drawing" -> {
                                                        AsyncImage(
                                                            model = att.name,
                                                            contentDescription = "Drawing Attachment",
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(Color.White)
                                                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                                                            contentScale = ContentScale.Fit
                                                        )
                                                    }
                                                    "voice" -> {
                                                        AudioPlayerWidget(path = att.path, modifier = Modifier.fillMaxSize())
                                                    }
                                                    else -> {
                                                        Box(
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Description,
                                                                contentDescription = "File Attachment",
                                                                tint = MaterialTheme.colorScheme.secondary,
                                                                modifier = Modifier.size(36.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            is PreviewItem.MediaTag -> {
                                                when (item.type) {
                                                    "image" -> {
                                                        AsyncImage(
                                                            model = item.src,
                                                            contentDescription = "Image Attachment",
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(Color.White)
                                                                .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp)),
                                                            contentScale = ContentScale.Crop
                                                        )
                                                    }
                                                    "video" -> {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                                                                .clickable {
                                                                    try {
                                                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                                                            setDataAndType(Uri.parse(item.src), "video/*")
                                                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                                        }
                                                                        context.startActivity(intent)
                                                                    } catch (e: Exception) {
                                                                        Toast.makeText(context, "No app to play video", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                },
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.PlayArrow,
                                                                contentDescription = "Play Video",
                                                                tint = MaterialTheme.colorScheme.primary,
                                                                modifier = Modifier.size(36.dp)
                                                            )
                                                        }
                                                    }
                                                    "audio" -> {
                                                        AudioPlayerWidget(path = item.src, modifier = Modifier.fillMaxSize())
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    
                                    IconButton(
                                        onClick = {
                                            when (item) {
                                                is PreviewItem.LegacyAttachment -> {
                                                    attachments = attachments - item.attachment
                                                    scope.launch {
                                                        viewModel.saveNote(
                                                            id = noteId,
                                                            title = title.trim(),
                                                            content = createRawContent(contentValue.text.trim(), attachments),
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
                                                is PreviewItem.MediaTag -> {
                                                    val oldText = contentValue.text
                                                    val tagPattern = when (item.type) {
                                                        "image" -> "<img\\s+src=\"${Regex.escape(item.src)}\"\\s*/>"
                                                        "video" -> "<video\\s+src=\"${Regex.escape(item.src)}\"\\s*/>"
                                                        "audio" -> "<audio\\s+src=\"${Regex.escape(item.src)}\"\\s*/>"
                                                        else -> ""
                                                    }
                                                    if (tagPattern.isNotEmpty()) {
                                                        val newText = oldText.replaceFirst(Regex(tagPattern), "")
                                                        contentValue = TextFieldValue(text = newText, selection = TextRange(newText.length))
                                                        content = newText
                                                        saveToHistory(newText)
                                                    }
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .size(24.dp)
                                            .align(Alignment.TopEnd)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove Attachment",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
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
                                    Toast.makeText(context, "Nothing to read aloud", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.testTag("tts_toolbar_btn")
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = if (isSpeaking) "Stop Speaking" else "Read Note Aloud",
                            tint = if (isSpeaking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = {
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
                            contentDescription = "Add Drawing",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = { showVoiceFileSheet = true },
                        modifier = Modifier.testTag("attachments_toolbar_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Add Attachment",
                            tint = MaterialTheme.colorScheme.primary
                        )
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
                        .padding(16.dp)
                        .navigationBarsPadding(),
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
                                contentDescription = "Background preview",
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

        // More Options Bottom Sheet
        if (showMoreSheet) {
            ModalBottomSheet(
                onDismissRequest = { showMoreSheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    Text(
                        text = stringResource(id = R.string.more_options),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp).align(Alignment.CenterHorizontally)
                    )
                    
                    val lastModifiedTime = originalNote?.lastModified ?: System.currentTimeMillis()
                    val formattedDate = SimpleDateFormat("LLL dd, yyyy HH:mm", Locale.getDefault()).format(Date(lastModifiedTime))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text(stringResource(id = R.string.label_last_modified, formattedDate), fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            icon = { Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                            )
                        )
                    }

                    // Encryption properties Card
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                            .border(
                                width = 1.dp,
                                color = if (isEncrypted) Color(0xFF43A047) else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(10.dp)
                            ),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (isEncrypted) Color(0xFF43A047).copy(alpha = 0.05f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(id = R.string.label_e2e_encryption),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isEncrypted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(id = if (isPasswordSet) R.string.desc_e2e_encryption_enabled else R.string.desc_e2e_encryption_disabled),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isEncrypted,
                                onCheckedChange = {
                                    if (!isPasswordSet) {
                                        Toast.makeText(context, context.getString(R.string.toast_setup_password_first), Toast.LENGTH_LONG).show()
                                    } else {
                                        isEncrypted = it
                                    }
                                },
                                enabled = isPasswordSet,
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF43A047))
                            )
                        }
                    }
                    
                    // SHARE section
                    Text(
                        text = stringResource(id = R.string.option_share),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    val formats = listOf(
                        Triple("TXT", Icons.Default.Description, R.string.share_format_txt),
                        Triple("MD", Icons.Default.Share, R.string.share_format_md),
                        Triple("PDF", Icons.Default.PictureAsPdf, R.string.share_format_pdf),
                        Triple("HTML", Icons.Default.Html, R.string.share_format_html),
                        Triple("JSON", Icons.Default.Code, R.string.share_format_json)
                    )
                    
                    val decryptedNoteForShare = DecryptedNote(
                        note = originalNote ?: Note(title = title, content = content),
                        title = title,
                        content = content,
                        isDecryptionSuccessful = true
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        formats.forEach { (formatKey, icon, labelResId) ->
                            OutlinedButton(
                                onClick = {
                                    val notesToShare = listOf(decryptedNoteForShare)
                                    when (formatKey) {
                                        "TXT" -> ExportUtils.exportMultipleToTxt(context, notesToShare)
                                        "MD" -> ExportUtils.exportToMarkdown(context, decryptedNoteForShare.note, title, content)
                                        "PDF" -> ExportUtils.exportToPdf(context, decryptedNoteForShare.note, title, content)
                                        "HTML" -> ExportUtils.exportMultipleToHtml(context, notesToShare)
                                        "JSON" -> ExportUtils.exportSingleNoteToJson(context, decryptedNoteForShare.note, title, content)
                                    }
                                    showMoreSheet = false
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(id = labelResId), fontSize = 11.sp)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // DELETE option
                    if (noteId != 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = {
                                originalNote?.let {
                                    viewModel.deleteNote(it)
                                    Toast.makeText(context, context.getString(R.string.toast_note_deleted), Toast.LENGTH_SHORT).show()
                                    showMoreSheet = false
                                    onBack()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("delete_note_btn_more"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(id = R.string.option_delete))
                        }
                    }
                }
            }
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
                        Toast.makeText(context, "File attached successfully", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "Error selecting file: ${e.message}", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(context, "Failed to start recording: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Microphone permission is required to record voice notes", Toast.LENGTH_SHORT).show()
                }
            }

            DisposableEffect(Unit) {
                onDispose {
                    mediaRecorder?.apply {
                        try {
                            stop()
                        } catch (e: Exception) {}
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
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Add Attachment",
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
                                text = "Voice Note Recorder",
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
                                        contentDescription = "Record voice note",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Text("Tap to start recording", style = MaterialTheme.typography.bodySmall)
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
                                        contentDescription = "Stop recording",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Text("Recording... Tap to stop", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
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
                                                    Toast.makeText(context, "Error playing preview", Toast.LENGTH_SHORT).show()
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
                                            contentDescription = "Preview Voice Note",
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            // Insert audio tag in the content
                                            val selStart = contentValue.selection.start
                                            val selEnd = contentValue.selection.end
                                            val text = contentValue.text
                                            val mediaTag = "<audio src=\"${recordedFile!!.absolutePath}\" />"
                                            val newText = text.substring(0, selStart) + mediaTag + text.substring(selEnd)
                                            val newCursor = selStart + mediaTag.length
                                            contentValue = TextFieldValue(text = newText, selection = TextRange(newCursor))
                                            content = newText
                                            saveToHistory(newText)

                                            scope.launch {
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
                                            }
                                            showVoiceFileSheet = false
                                            Toast.makeText(context, "Voice note attached", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.testTag("attach_voice_btn")
                                    ) {
                                        Text("Attach Voice")
                                    }

                                    IconButton(
                                        onClick = {
                                            recordedFile?.delete()
                                            recordedFile = null
                                        },
                                        modifier = Modifier.testTag("delete_recording_btn")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Discard Recording", tint = MaterialTheme.colorScheme.error)
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
                            Icon(Icons.Default.FolderOpen, contentDescription = "Select File", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Select External File", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                Text("Choose documents, audio, or other files to link", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ColorSelectionDialog(
    title: String,
    onDismiss: () -> Unit,
    onColorSelected: (String) -> Unit
) {
    var hexInput by remember { mutableStateOf("#FFFFFF") }
    var hueValue by remember { mutableStateOf(0f) }
    
    val pickedColor = remember(hueValue) { Color.hsv(hueValue, 1.0f, 1.0f) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Predefined Material Colors
                Text(
                    text = stringResource(id = R.string.color_option_material),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Triple(stringResource(id = R.string.color_red), "#D32F2F", Color(0xFFD32F2F)),
                        Triple(stringResource(id = R.string.color_blue), "#1976D2", Color(0xFF1976D2)),
                        Triple(stringResource(id = R.string.color_green), "#388E3C", Color(0xFF388E3C))
                    ).forEach { (name, hex, color) ->
                        OutlinedCard(
                            onClick = {
                                onColorSelected(hex)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = hex,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Section 2: Color Picker
                Text(
                    text = stringResource(id = R.string.color_option_picker),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                // Rainbow Gradient Track for Hue Slider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                                )
                            )
                        )
                ) {
                    Slider(
                        value = hueValue,
                        onValueChange = {
                            hueValue = it
                            val col = Color.hsv(it, 1f, 1f)
                            val r = (col.red * 255).toInt()
                            val g = (col.green * 255).toInt()
                            val b = (col.blue * 255).toInt()
                            hexInput = String.format("#%02X%02X%02X", r, g, b)
                        },
                        valueRange = 0f..360f,
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color.Transparent,
                            inactiveTrackColor = Color.Transparent,
                            thumbColor = pickedColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = hexInput,
                        onValueChange = { input ->
                            hexInput = input
                            val cleaned = input.trim().removePrefix("#")
                            if (cleaned.length == 6) {
                                try {
                                    val r = cleaned.substring(0, 2).toInt(16)
                                    val g = cleaned.substring(2, 4).toInt(16)
                                    val b = cleaned.substring(4, 6).toInt(16)
                                    val hsv = FloatArray(3)
                                    android.graphics.Color.RGBToHSV(r, g, b, hsv)
                                    hueValue = hsv[0]
                                } catch (e: Exception) {
                                    // Ignore parse error
                                }
                            }
                        },
                        label = { Text(stringResource(id = R.string.color_hex_label)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    val resolvedPreviewColor = remember(hexInput, pickedColor) {
                        val cleaned = hexInput.trim().removePrefix("#")
                        try {
                            if (cleaned.length == 6) {
                                Color(android.graphics.Color.parseColor("#$cleaned"))
                            } else {
                                pickedColor
                            }
                        } catch (e: Exception) {
                            pickedColor
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(resolvedPreviewColor)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    )
                }

                HorizontalDivider()

                // Section 3: Default Option
                Text(
                    text = stringResource(id = R.string.color_option_default),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                OutlinedCard(
                    onClick = {
                        onColorSelected("default")
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatClear,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.color_default),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalHex = if (hexInput.startsWith("#")) hexInput else "#$hexInput"
                    onColorSelected(finalHex)
                    onDismiss()
                }
            ) {
                Text(stringResource(id = R.string.color_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.color_cancel))
            }
        }
    )
}
