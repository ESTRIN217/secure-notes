package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.ai.*
import com.example.ui.viewmodel.AiViewModel
import com.example.ui.viewmodel.ConversationTurn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    viewModel: AiViewModel,
    noteId: Int,
    fullContent: String,
    selectedText: String,
    onBack: () -> Unit,
    onInsert: (String) -> Unit
) {
    var currentAction by remember { mutableStateOf(AiAction.GENERATE) }
    var inputText by remember { mutableStateOf("") }
    var rewriteStyle by remember { mutableStateOf(RewriteStyle.FORMAL) }
    var targetLanguage by remember { mutableStateOf("en") }

    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val hasSelection = selectedText.isNotBlank()

    val conversationHistory = remember(noteId) {
        derivedStateOf { viewModel.getConversationHistory(noteId) }
    }.value

    LaunchedEffect(conversationHistory.size) {
        if (conversationHistory.isNotEmpty()) {
            listState.animateScrollToItem(conversationHistory.size - 1)
        }
    }

    LaunchedEffect(isProcessing) {
        if (!isProcessing && errorMessage == null) {
            val history = viewModel.getConversationHistory(noteId)
            if (history.isNotEmpty()) {
                listState.animateScrollToItem(history.size - 1)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ai_chat_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (conversationHistory.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearConversationHistory(noteId) }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = stringResource(R.string.ai_clear_history))
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
                                viewModel.execute(request, noteId)
                                if (currentAction == AiAction.GENERATE) {
                                    inputText = ""
                                }
                            },
                            enabled = !isProcessing && (currentAction != AiAction.GENERATE || inputText.isNotBlank()),
                            modifier = Modifier.size(48.dp)
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
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
            if (conversationHistory.isEmpty()) {
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
                            isLastAssistant = turn == conversationHistory.lastOrNull() && turn.role == "assistant",
                            showInsert = turn.role == "assistant",
                            onInsert = { onInsert(turn.content) }
                        )
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

@Composable
private fun MessageBubble(
    turn: ConversationTurn,
    isLastAssistant: Boolean,
    showInsert: Boolean,
    onInsert: () -> Unit
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
    val alignment = if (isUser) Arrangement.End else Arrangement.Start

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
                Text(
                    text = turn.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
                if (showInsert && isLastAssistant) {
                    Spacer(modifier = Modifier.height(8.dp))
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
private fun ActionChipRowMinimal(
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
