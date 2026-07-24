package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.ai.*
import com.example.ui.viewmodel.AiViewModel
import com.example.ui.viewmodel.ChatHistoryViewModel
import com.example.ui.viewmodel.ConversationTurn
import com.example.ui.viewmodel.MessageStatus
import com.example.util.RichTextParser
import kotlin.math.roundToInt

private const val SWIPE_THRESHOLD = 120f

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AiChatScreen(
    viewModel: AiViewModel,
    chatHistoryViewModel: ChatHistoryViewModel,
    sessionId: Int,
    noteId: Int,
    fullContent: String,
    selectedText: String,
    onBack: () -> Unit,
    onInsert: ((String) -> Unit)?,
    onNavigateToChatHistory: (() -> Unit)?
) {
    BackHandler(onBack = onBack)

    var currentAction by remember { mutableStateOf(AiAction.GENERATE) }
    var inputText by remember { mutableStateOf("") }
    var rewriteStyle by remember { mutableStateOf(RewriteStyle.FORMAL) }
    var targetLanguage by remember { mutableStateOf("en") }
    var showRenameTitleDialog by remember { mutableStateOf(false) }
    var renameTitleText by remember { mutableStateOf("") }
    var editingMessage by remember { mutableStateOf<String?>(null) }

    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val streamingText by viewModel.streamingText.collectAsStateWithLifecycle()
    val processingTimeMs by viewModel.processingTimeMs.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val backend by viewModel.backend.collectAsStateWithLifecycle()
    val modelNameSetting by viewModel.modelName.collectAsStateWithLifecycle()
    val selectedOnDeviceModel by viewModel.selectedOnDeviceModel.collectAsStateWithLifecycle()
    val allHistory by viewModel.conversationHistory.collectAsStateWithLifecycle()
    val sessionTitle by viewModel.sessionTitle.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val listState = rememberLazyListState()

    val effectiveSessionId = if (sessionId > 0) sessionId else viewModel.currentSessionId.value

    LaunchedEffect(sessionId, noteId) {
        if (sessionId > 0) {
            viewModel.loadSession(sessionId, noteId)
        }
        editingMessage?.let { inputText = it; editingMessage = null }
    }

    val conversationHistory = allHistory[effectiveSessionId] ?: emptyList()
    val displayModelName = if (backend == AiBackend.OLLAMA) modelNameSetting
                           else selectedOnDeviceModel?.displayName ?: ""

    val hasStreamingContent = isProcessing && !streamingText.isNullOrEmpty()
    val isStreamingEmpty = isProcessing && streamingText.isNullOrEmpty()

    var userScrolledUp by remember { mutableStateOf(false) }

    val isNearBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            if (layoutInfo.totalItemsCount == 0) true
            else {
                val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()
                lastVisible != null && lastVisible.index >= layoutInfo.totalItemsCount - 1 &&
                    lastVisible.offset + lastVisible.size <= layoutInfo.viewportEndOffset + 50
            }
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            userScrolledUp = !isNearBottom
        }
    }

    LaunchedEffect(conversationHistory.size, hasStreamingContent, isNearBottom) {
        if (userScrolledUp && !isNearBottom) return@LaunchedEffect
        val targetIndex = when {
            hasStreamingContent -> conversationHistory.size
            conversationHistory.isNotEmpty() -> conversationHistory.size - 1
            else -> return@LaunchedEffect
        }
        if (targetIndex >= 0) {
            listState.animateScrollToItem(targetIndex)
        }
    }

    LaunchedEffect(isProcessing) {
        if (!isProcessing && errorMessage == null) {
            val history = viewModel.getConversationHistory(effectiveSessionId)
            if (history.isNotEmpty()) {
                userScrolledUp = false
                listState.animateScrollToItem(history.size - 1)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = sessionTitle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.combinedClickable(
                                    onClick = {},
                                    onLongClick = {
                                        renameTitleText = sessionTitle
                                        showRenameTitleDialog = true
                                    }
                                )
                            )
                            if (displayModelName.isNotBlank()) {
                                Text(
                                    text = displayModelName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                    actions = {
                        if (onNavigateToChatHistory != null) {
                            IconButton(onClick = onNavigateToChatHistory) {
                                Icon(Icons.Default.History, contentDescription = stringResource(R.string.chat_history_title))
                            }
                        }
                        if (effectiveSessionId > 0) {
                            if (conversationHistory.isNotEmpty()) {
                                IconButton(onClick = { viewModel.clearConversationHistory(effectiveSessionId) }) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = stringResource(R.string.ai_clear_history))
                                }
                            }
                        }
                    }
                )
            },
            bottomBar = {
                Surface(
                    tonalElevation = 3.dp,
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        ActionChipRowMinimal(
                            currentAction = currentAction,
                            onActionSelected = { currentAction = it }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        SuggestionChips(
                            hasHistory = conversationHistory.isNotEmpty(),
                            onSuggestion = { suggestion ->
                                viewModel.execute(
                                    AiRequest(action = AiAction.GENERATE, prompt = suggestion, selectedText = selectedText, context = fullContent),
                                    effectiveSessionId
                                )
                                inputText = ""
                            }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = {},
                                enabled = false,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    Icons.Default.AttachFile,
                                    contentDescription = stringResource(R.string.attach),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            OutlinedTextField(
                                value = if (editingMessage != null) editingMessage!! else inputText,
                                onValueChange = {
                                    if (editingMessage != null) editingMessage = it
                                    else inputText = it
                                },
                                placeholder = {
                                    Text(
                                        when {
                                            conversationHistory.isNotEmpty() -> stringResource(R.string.ai_chat_hint_followup)
                                            else -> when (currentAction) {
                                                AiAction.GENERATE -> stringResource(R.string.ai_chat_hint_generate)
                                                AiAction.SUMMARIZE -> stringResource(R.string.ai_chat_hint_summarize)
                                                AiAction.REWRITE -> stringResource(R.string.ai_chat_hint_rewrite)
                                                AiAction.TRANSLATE -> stringResource(R.string.ai_chat_hint_translate)
                                            }
                                        }
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                minLines = 1,
                                maxLines = 5,
                                shape = RoundedCornerShape(24.dp),
                                enabled = !isProcessing
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val sendEnabled = (if (editingMessage != null) editingMessage!! else inputText).isNotBlank()
                            val sendButtonColor by animateColorAsState(
                                targetValue = if (sendEnabled) MaterialTheme.colorScheme.primary
                                              else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                animationSpec = tween(200)
                            )
                            var buttonScale by remember { mutableStateOf(1f) }
                            FilledIconButton(
                                onClick = {
                                    if (isProcessing) {
                                        viewModel.cancelGeneration()
                                    } else {
                                        val text = editingMessage ?: inputText
                                        if (text.isBlank()) return@FilledIconButton
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        buttonScale = 0.95f
                                        if (editingMessage != null) {
                                            viewModel.execute(
                                                AiRequest(action = AiAction.GENERATE, prompt = text, selectedText = selectedText, context = fullContent),
                                                effectiveSessionId
                                            )
                                            editingMessage = null
                                        } else {
                                            val prompt = when (currentAction) {
                                                AiAction.GENERATE -> text
                                                AiAction.SUMMARIZE -> context.getString(R.string.ai_chat_prompt_summarize)
                                                AiAction.REWRITE -> context.getString(R.string.ai_chat_prompt_rewrite, rewriteStyle.name.lowercase())
                                                AiAction.TRANSLATE -> context.getString(R.string.ai_chat_prompt_translate, targetLanguage)
                                            }
                                            val request = AiRequest(
                                                action = currentAction,
                                                prompt = prompt,
                                                selectedText = selectedText,
                                                context = fullContent,
                                                rewriteStyle = rewriteStyle,
                                                targetLanguage = targetLanguage
                                            )
                                            viewModel.execute(request, effectiveSessionId)
                                            if (currentAction == AiAction.GENERATE) {
                                                inputText = ""
                                            }
                                        }
                                    }
                                },
                                enabled = sendEnabled || isProcessing,
                                modifier = Modifier
                                    .size(48.dp)
                                    .graphicsLayer(scaleX = buttonScale, scaleY = buttonScale),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = if (isProcessing) MaterialTheme.colorScheme.errorContainer
                                                     else sendButtonColor
                                )
                            ) {
                                if (isProcessing) {
                                    Icon(Icons.Default.Stop, contentDescription = stringResource(R.string.ai_stop))
                                } else {
                                    Icon(Icons.Default.Send, contentDescription = stringResource(R.string.ai_send))
                                }
                            }
                            LaunchedEffect(buttonScale) {
                                if (buttonScale < 1f) {
                                    kotlinx.coroutines.delay(100)
                                    buttonScale = 1f
                                }
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (conversationHistory.isEmpty() && !isStreamingEmpty && !hasStreamingContent) {
                    EmptyChatWelcome(
                        selectedText = selectedText,
                        fullContent = fullContent,
                        onSuggestion = { prompt ->
                            viewModel.execute(
                                AiRequest(action = AiAction.GENERATE, prompt = prompt, selectedText = selectedText, context = fullContent),
                                effectiveSessionId
                            )
                        },
                        onSummarize = {
                            viewModel.execute(
                                AiRequest(action = AiAction.SUMMARIZE, prompt = context.getString(R.string.ai_chat_prompt_summarize), selectedText = selectedText, context = fullContent),
                                effectiveSessionId
                            )
                        }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        items(
                            items = conversationHistory,
                            key = { "${it.role}_${it.timestamp}_${it.status}" }
                        ) { turn ->
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(animationSpec = tween(200)) +
                                    slideInVertically(
                                        animationSpec = tween(200),
                                        initialOffsetY = { it / 4 }
                                    )
                            ) {
                                MessageBubble(
                                    turn = turn,
                                    clipboardManager = clipboardManager,
                                    haptic = haptic,
                                    isLastAssistant = turn == conversationHistory.lastOrNull() && turn.role == "assistant",
                                    showInsert = turn.role == "assistant" && onInsert != null,
                                    onInsert = { onInsert?.invoke(turn.content) },
                                    onResend = { text ->
                                        editingMessage = text
                                        inputText = text
                                    },
                                    onRetry = {
                                        if (turn.role == "assistant" && turn.status == MessageStatus.ERROR) {
                                            val lastUserMsg = conversationHistory
                                                .takeWhile { it != turn }
                                                .lastOrNull { it.role == "user" }
                                            viewModel.execute(
                                                AiRequest(action = AiAction.GENERATE, prompt = lastUserMsg?.content ?: "", selectedText = selectedText, context = fullContent),
                                                effectiveSessionId
                                            )
                                        }
                                    },
                                    onEdit = { text ->
                                        editingMessage = text
                                        inputText = text
                                    },
                                    modelName = displayModelName
                                )
                            }
                        }
                        if (isStreamingEmpty) {
                            item(key = "typing") {
                                TypingIndicator()
                            }
                        }
                        if (hasStreamingContent) {
                            item(key = "streaming") {
                                StreamingBubble(
                                    text = streamingText!!,
                                    modelName = displayModelName
                                )
                            }
                        }
                    }
                }

                if (!viewModel.isAvailable() && conversationHistory.isEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = stringResource(R.string.ai_model_not_available),
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }

    if (showRenameTitleDialog) {
        AlertDialog(
            onDismissRequest = { showRenameTitleDialog = false },
            title = { Text(stringResource(R.string.chat_rename_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = renameTitleText,
                    onValueChange = { renameTitleText = it },
                    label = { Text(stringResource(R.string.chat_title_hint)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (renameTitleText.isNotBlank()) {
                        viewModel.renameCurrentSession(renameTitleText)
                    }
                    showRenameTitleDialog = false
                }) {
                    Text(stringResource(R.string.chat_rename))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameTitleDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    turn: ConversationTurn,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    isLastAssistant: Boolean,
    showInsert: Boolean,
    onInsert: () -> Unit,
    onResend: (String) -> Unit,
    onRetry: () -> Unit,
    onEdit: (String) -> Unit,
    modelName: String
) {
    val isUser = turn.role == "user"
    val isError = turn.status == MessageStatus.ERROR
    val bubbleColor = when {
        isError -> MaterialTheme.colorScheme.errorContainer
        isUser -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when {
        isError -> MaterialTheme.colorScheme.onErrorContainer
        isUser -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var swipeOffset by remember { mutableStateOf(0f) }

    val animatedSwipeOffset by androidx.compose.animation.core.animateFloatAsState(
        targetValue = swipeOffset,
        animationSpec = tween(200)
    )

    Column(
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            if (!isUser) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "AI",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = if (isUser) stringResource(R.string.ai_chat_you) else stringResource(R.string.ai_chat_ai),
                style = MaterialTheme.typography.labelSmall,
                color = labelColor
            )
        }

        Box {
            Surface(
                color = bubbleColor,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .offset { IntOffset(animatedSwipeOffset.roundToInt(), 0) }
                    .combinedClickable(
                        onClick = {
                            if (isError) onRetry()
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showMenu = true
                        }
                    )
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (swipeOffset > SWIPE_THRESHOLD && isUser) {
                                    onEdit(turn.content)
                                } else if (swipeOffset < -SWIPE_THRESHOLD && !isUser) {
                                    clipboardManager.setText(AnnotatedString(turn.content))
                                }
                                swipeOffset = 0f
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                val maxSwipe = if (isUser) SWIPE_THRESHOLD * 2 else -SWIPE_THRESHOLD * 2
                                swipeOffset = (swipeOffset + dragAmount).coerceIn(
                                    if (isUser) 0f else maxSwipe,
                                    if (isUser) maxSwipe else 0f
                                )
                            }
                        )
                    }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (isError) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = turn.errorMessage ?: stringResource(R.string.ai_generation_failed),
                                style = MaterialTheme.typography.bodySmall,
                                color = textColor
                            )
                        }
                    } else {
                        Text(
                            text = RichTextParser.parse(turn.content, hideTags = true),
                            style = MaterialTheme.typography.bodyMedium.copy(color = textColor)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MessageStatusIcon(turn.status, textColor)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = turn.formattedTime,
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.5f)
                        )
                        turn.formattedDuration?.let { dur ->
                            Text(
                                text = " · $dur",
                                style = MaterialTheme.typography.labelSmall,
                                color = textColor.copy(alpha = 0.5f)
                            )
                        }
                        if (!isUser && modelName.isNotBlank()) {
                            Text(
                                text = " · $modelName",
                                style = MaterialTheme.typography.labelSmall,
                                color = textColor.copy(alpha = 0.35f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        if (isUser) {
                            IconButton(
                                onClick = { onResend(turn.content) },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = stringResource(R.string.ai_resend),
                                    modifier = Modifier.size(12.dp),
                                    tint = textColor.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                    if (showInsert && isLastAssistant) {
                        TextButton(
                            onClick = onInsert,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentPaste,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.ai_insert), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.ai_copy)) },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    onClick = {
                        clipboardManager.setText(AnnotatedString(turn.content))
                        showMenu = false
                    }
                )
                if (!isUser && turn.status == MessageStatus.ERROR) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.ai_retry)) },
                        leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            onRetry()
                            showMenu = false
                        }
                    )
                }
                if (!isUser && turn.content.isNotBlank()) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.ai_regenerate)) },
                        leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            onResend(turn.content)
                            showMenu = false
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.share)) },
                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    onClick = {
                        val sendIntent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(android.content.Intent.EXTRA_TEXT, turn.content)
                            type = "text/plain"
                        }
                        context.startActivity(android.content.Intent.createChooser(sendIntent, null))
                        showMenu = false
                    }
                )
                if (showInsert && isLastAssistant) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.ai_insert)) },
                        leadingIcon = { Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            onInsert()
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MessageStatusIcon(status: MessageStatus, tint: Color) {
    when (status) {
        MessageStatus.SENT -> {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = tint.copy(alpha = 0.4f)
            )
        }
        MessageStatus.DELIVERED -> {
            Icon(
                Icons.Default.DoneAll,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = tint.copy(alpha = 0.4f)
            )
        }
        MessageStatus.GENERATING -> {
            val infiniteTransition = rememberInfiniteTransition()
            val dotAlpha by infiniteTransition.animateFloat(
                initialValue = 0.3f, targetValue = 1.0f,
                animationSpec = infiniteRepeatable(tween(400), RepeatMode.Reverse)
            )
            Box(modifier = Modifier.size(12.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .alpha(dotAlpha)
                        .background(tint.copy(alpha = 0.5f), CircleShape)
                )
            }
        }
        MessageStatus.COMPLETED -> {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = tint.copy(alpha = 0.6f)
            )
        }
        MessageStatus.ERROR -> {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = Color(0xFFE53935)
            )
        }
    }
}

@Composable
fun StreamingBubble(text: String, modelName: String) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(20.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("AI", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.ai_chat_ai), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        }
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = RichTextParser.parse(text, hideTags = true),
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MessageStatusIcon(MessageStatus.GENERATING, MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.ai_generating),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    if (modelName.isNotBlank()) {
                        Text(
                            text = " · $modelName",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyChatWelcome(
    selectedText: String,
    fullContent: String,
    onSuggestion: (String) -> Unit,
    onSummarize: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "AI",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.ai_chat_welcome),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                val poemSuggestion = stringResource(R.string.ai_suggestion_poem)
                val ideasSuggestion = stringResource(R.string.ai_suggestion_ideas)
                FilterChip(
                    selected = false,
                    onClick = { onSuggestion(poemSuggestion) },
                    label = { Text(poemSuggestion, style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
                FilterChip(
                    selected = false,
                    onClick = { onSuggestion(ideasSuggestion) },
                    label = { Text(ideasSuggestion, style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = { Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
                if (selectedText.isNotBlank() || fullContent.isNotBlank()) {
                    FilterChip(
                        selected = false,
                        onClick = onSummarize,
                        label = { Text(stringResource(R.string.ai_summarize), style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { Icon(Icons.Default.Summarize, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                }
            }
        }
    }
}

@Composable
fun SuggestionChips(hasHistory: Boolean, onSuggestion: (String) -> Unit) {
    val context = LocalContext.current
    if (hasHistory) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val chips = listOf(
                stringResource(R.string.ai_suggestion_explain_more),
                stringResource(R.string.ai_suggestion_another_example),
                stringResource(R.string.ai_suggestion_shorten)
            )
            chips.take(3).forEach { chip ->
                FilterChip(
                    selected = false,
                    onClick = { onSuggestion(chip) },
                    label = { Text(chip, style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                    leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(12.dp)) }
                )
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition()
    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 0), RepeatMode.Reverse)
    )
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 200), RepeatMode.Reverse)
    )
    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 400), RepeatMode.Reverse)
    )

    AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("AI", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.ai_chat_ai),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp),
                modifier = Modifier.widthIn(max = 320.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(8.dp).alpha(dot1Alpha).background(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), CircleShape
                    ))
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(modifier = Modifier.size(8.dp).alpha(dot2Alpha).background(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), CircleShape
                    ))
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(modifier = Modifier.size(8.dp).alpha(dot3Alpha).background(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), CircleShape
                    ))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.ai_generating),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun ActionChipRowMinimal(
    currentAction: AiAction,
    onActionSelected: (AiAction) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        FilterChip(
            selected = currentAction == AiAction.GENERATE,
            onClick = { onActionSelected(AiAction.GENERATE) },
            label = { Text(stringResource(R.string.ai_generate), style = MaterialTheme.typography.labelSmall) },
            leadingIcon = {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
            }
        )
        FilterChip(
            selected = currentAction == AiAction.SUMMARIZE,
            onClick = { onActionSelected(AiAction.SUMMARIZE) },
            label = { Text(stringResource(R.string.ai_summarize), style = MaterialTheme.typography.labelSmall) },
            leadingIcon = {
                Icon(Icons.Default.Summarize, contentDescription = null, modifier = Modifier.size(14.dp))
            }
        )
        FilterChip(
            selected = currentAction == AiAction.REWRITE,
            onClick = { onActionSelected(AiAction.REWRITE) },
            label = { Text(stringResource(R.string.ai_rewrite), style = MaterialTheme.typography.labelSmall) },
            leadingIcon = {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
            }
        )
        FilterChip(
            selected = currentAction == AiAction.TRANSLATE,
            onClick = { onActionSelected(AiAction.TRANSLATE) },
            label = { Text(stringResource(R.string.ai_translate), style = MaterialTheme.typography.labelSmall) },
            leadingIcon = {
                Icon(Icons.Default.Translate, contentDescription = null, modifier = Modifier.size(14.dp))
            }
        )
    }
}
