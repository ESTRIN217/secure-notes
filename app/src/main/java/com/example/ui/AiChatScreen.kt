package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField as FoundationBasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.ai.*
import com.example.ui.viewmodel.AiViewModel
import com.example.ui.viewmodel.ChatHistoryViewModel
import com.example.ui.viewmodel.ConversationTurn
import com.example.util.RichTextParser

@OptIn(ExperimentalMaterial3Api::class)
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
    val listState = rememberLazyListState()

    val effectiveSessionId = if (sessionId > 0) sessionId else viewModel.currentSessionId.value

    LaunchedEffect(sessionId, noteId) {
        if (sessionId > 0) {
            viewModel.loadSession(sessionId, noteId)
        }
    }

    val conversationHistory = allHistory[effectiveSessionId] ?: emptyList()
    val displayModelName = if (backend == AiBackend.OLLAMA) modelNameSetting
                           else selectedOnDeviceModel?.displayName ?: ""

    val hasStreamingContent = isProcessing && !streamingText.isNullOrEmpty()
    val isStreamingEmpty = isProcessing && streamingText.isNullOrEmpty()

    val isNearBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            if (layoutInfo.totalItemsCount == 0) true
            else {
                val lastItem = layoutInfo.visibleItemsInfo.lastOrNull()
                lastItem != null && lastItem.index >= layoutInfo.totalItemsCount - 1
            }
        }
    }

    LaunchedEffect(conversationHistory.size, hasStreamingContent) {
        if (!isNearBottom) return@LaunchedEffect
        val targetIndex = when {
            hasStreamingContent -> conversationHistory.size
            conversationHistory.isNotEmpty() -> conversationHistory.size - 1
            else -> return@LaunchedEffect
        }
        listState.animateScrollToItem(targetIndex)
    }

    LaunchedEffect(isProcessing) {
        if (!isProcessing && errorMessage == null) {
            val history = viewModel.getConversationHistory(effectiveSessionId)
            if (history.isNotEmpty()) {
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
                                modifier = Modifier.clickable {
                                    renameTitleText = sessionTitle
                                    showRenameTitleDialog = true
                                }
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
                            IconButton(onClick = { viewModel.clearConversationHistory(effectiveSessionId) }) {
                                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.ai_new_chat))
                            }
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
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                placeholder = {
                                    Text(
                                        when (currentAction) {
                                            AiAction.GENERATE -> stringResource(R.string.ai_chat_hint_generate)
                                            AiAction.SUMMARIZE -> stringResource(R.string.ai_chat_hint_summarize)
                                            AiAction.REWRITE -> stringResource(R.string.ai_chat_hint_rewrite)
                                            AiAction.TRANSLATE -> stringResource(R.string.ai_chat_hint_translate)
                                        }
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                minLines = 1,
                                maxLines = 4,
                                shape = RoundedCornerShape(24.dp),
                                enabled = !isProcessing
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            FilledIconButton(
                                onClick = {
                                    if (isProcessing) {
                                        viewModel.cancelGeneration()
                                    } else {
                                        val prompt = when (currentAction) {
                                            AiAction.GENERATE -> inputText
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
                                },
                                enabled = !isProcessing || isProcessing,
                                modifier = Modifier.size(48.dp)
                            ) {
                                if (isProcessing) {
                                    Icon(Icons.Default.Stop, contentDescription = stringResource(R.string.ai_stop))
                                } else {
                                    Icon(Icons.Default.Send, contentDescription = stringResource(R.string.ai_send))
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
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Chat,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                stringResource(R.string.ai_chat_empty_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.ai_chat_empty_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            val poemSuggestion = stringResource(R.string.ai_suggestion_poem)
                            val ideasSuggestion = stringResource(R.string.ai_suggestion_ideas)
                            val summarizePrompt = stringResource(R.string.ai_chat_prompt_summarize)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        viewModel.execute(
                                            AiRequest(action = AiAction.GENERATE, prompt = poemSuggestion, selectedText = selectedText, context = fullContent),
                                            effectiveSessionId
                                        )
                                    },
                                    label = { Text(poemSuggestion, style = MaterialTheme.typography.labelSmall) },
                                    leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                )
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        viewModel.execute(
                                            AiRequest(action = AiAction.GENERATE, prompt = ideasSuggestion, selectedText = selectedText, context = fullContent),
                                            effectiveSessionId
                                        )
                                    },
                                    label = { Text(ideasSuggestion, style = MaterialTheme.typography.labelSmall) },
                                    leadingIcon = { Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                )
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        viewModel.execute(
                                            AiRequest(action = AiAction.SUMMARIZE, prompt = summarizePrompt, selectedText = selectedText, context = fullContent),
                                            effectiveSessionId
                                        )
                                    },
                                    label = { Text(stringResource(R.string.ai_summarize), style = MaterialTheme.typography.labelSmall) },
                                    leadingIcon = { Icon(Icons.Default.Summarize, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            items(conversationHistory) { turn ->
                                MessageBubble(
                                    turn = turn,
                                    clipboardManager = clipboardManager,
                                    isLastAssistant = turn == conversationHistory.lastOrNull() && turn.role == "assistant",
                                    showInsert = turn.role == "assistant" && onInsert != null,
                                    processingTimeMs = if (turn == conversationHistory.lastOrNull() && turn.role == "assistant") turn.processingTimeMs else null,
                                    onInsert = { onInsert?.invoke(turn.content) },
                                    onResend = { inputText = it }
                                )
                            }
                            if (isStreamingEmpty) {
                                item(key = "typing") {
                                    TypingIndicator()
                                }
                            }
                            if (hasStreamingContent) {
                                item(key = "streaming") {
                                    StreamingBubble(text = streamingText!!)
                                }
                            }
                    }
                }

                errorMessage?.let { error ->
                    Snackbar(
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.BottomCenter),
                        action = {
                            TextButton(onClick = { viewModel.clearResult() }) {
                                Text(stringResource(R.string.dismiss))
                            }
                        }
                    ) {
                        Text(error)
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

@Composable
fun MessageBubble(
    turn: ConversationTurn,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager,
    isLastAssistant: Boolean,
    showInsert: Boolean,
    processingTimeMs: Long?,
    onInsert: () -> Unit,
    onResend: (String) -> Unit
) {
    val isUser = turn.role == "user"
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = if (isUser) stringResource(R.string.ai_chat_you) else stringResource(R.string.ai_chat_ai),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                FoundationBasicTextField(
                    value = remember(turn.content) {
                        TextFieldValue(RichTextParser.parse(turn.content, hideTags = true))
                    },
                    onValueChange = {},
                    readOnly = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (processingTimeMs != null) {
                        Text(
                            text = "${"%.1f".format(processingTimeMs / 1000.0)}s",
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.5f)
                        )
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                    Row {
                        IconButton(
                            onClick = { clipboardManager.setText(AnnotatedString(turn.content)) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = stringResource(R.string.ai_copy),
                                modifier = Modifier.size(14.dp),
                                tint = textColor.copy(alpha = 0.6f)
                            )
                        }
                        if (isUser) {
                            IconButton(
                                onClick = { onResend(turn.content) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = stringResource(R.string.ai_resend),
                                    modifier = Modifier.size(14.dp),
                                    tint = textColor.copy(alpha = 0.6f)
                                )
                            }
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
    }
}

@Composable
fun StreamingBubble(text: String) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.ai_chat_ai),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = 4.dp,
                bottomEnd = 16.dp
            ),
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                FoundationBasicTextField(
                    value = TextFieldValue(RichTextParser.parse(text, hideTags = true)),
                    onValueChange = {},
                    readOnly = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.ai_generating),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp
                    )
                }
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
            Text(
                text = stringResource(R.string.ai_chat_ai),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(
                    topStart = 16.dp, topEnd = 16.dp,
                    bottomStart = 4.dp, bottomEnd = 16.dp
                ),
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