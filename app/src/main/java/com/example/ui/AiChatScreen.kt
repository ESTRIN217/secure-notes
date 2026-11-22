package com.example.ui

import android.content.ClipData
import android.net.Uri
import android.provider.Settings
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.estrin217.markdown.MarkdownEngine
import com.estrin217.markdown.MarkdownText
import com.example.ui.markdown.markdownColors
import com.example.R
import com.example.data.ai.*
import com.example.data.local.ChatSessionWithPreview
import com.example.data.model.DecryptedNote
import com.example.ui.chat.BubbleAction
import com.example.ui.chat.adoptPendingEditing
import com.example.ui.chat.bubbleActions
import com.example.ui.chat.buildContextDetail
import com.example.ui.chat.SystemMessageDialog
import com.example.ui.chat.ToolsSheet
import com.example.ui.chat.AssistantDestination
import com.example.ui.chat.AssistantRail
import com.example.ui.chat.chatLayoutMode
import com.example.ui.chat.shouldShowAssistantRail
import com.example.ui.chat.isLargeScreenWidth
import com.example.ui.chat.TurnMetrics
import com.example.ui.chat.canSend
import com.example.ui.chat.effectiveText
import com.example.ui.chat.greetingIndexFor
import com.example.ui.chat.hintFor
import com.example.ui.chat.shouldDiscardChatSession
import com.example.ui.chat.shouldShowModelLoadingIndicator
import com.example.ui.chat.shouldShowUnavailableError
import com.example.ui.chat.requestFor
import com.example.ui.chat.shouldClearInput
import com.example.ui.chat.shouldSend
import com.example.ui.chat.visibleSuggestions
import com.example.ui.chat.tokensPerSecond
import com.example.ui.chat.writeTarget
import com.example.ui.chat.WriteTarget
import com.example.ui.markdown.ChatMarkdownText
import com.example.ui.viewmodel.AiViewModel
import com.example.ui.viewmodel.ChatHistoryViewModel
import com.example.ui.viewmodel.ConnectionState
import com.example.ui.viewmodel.ConversationTurn
import com.example.ui.viewmodel.MessageStatus
import com.example.util.RichTextParser
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    onNavigateToChatSearch: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToMain: () -> Unit = {}
) {
    var currentAction by remember { mutableStateOf(AiAction.GENERATE) }
    var inputText by remember { mutableStateOf("") }
    var targetLanguage by remember { mutableStateOf("en") }
    var showRenameTitleDialog by remember { mutableStateOf(false) }
    var renameTitleText by remember { mutableStateOf("") }
    var editingMessage by remember { mutableStateOf<String?>(null) }
    var showNotePicker by remember { mutableStateOf(false) }
    var notePickerSearchQuery by remember { mutableStateOf("") }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showModelPanel by remember { mutableStateOf(false) }
    // Enmienda 046: destino del área principal y estado del rail en Media+.
    var assistantDestination by remember { mutableStateOf(AssistantDestination.CHAT) }
    var railExpanded by remember { mutableStateOf(true) }
    // Spec 042 (RF-301, RF-304): las dos superficies nuevas que la hoja de adjuntos abre tras cerrarse.
    var showSystemMessageDialog by remember { mutableStateOf(false) }
    var showToolsSheet by remember { mutableStateOf(false) }
    // Spec 040: datos de la vista de detalles de un modelo on-device (hermano de los overlays).
    var infoModel by remember { mutableStateOf<OnDeviceModel?>(null) }
    var modelSheetDismissTick by remember { mutableIntStateOf(0) }

    val pendingAttachments by viewModel.pendingAttachments.collectAsStateWithLifecycle()

    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val streamingText by viewModel.streamingText.collectAsStateWithLifecycle()
    val processingTimeMs by viewModel.processingTimeMs.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val backend by viewModel.backend.collectAsStateWithLifecycle()
    val modelNameSetting by viewModel.modelName.collectAsStateWithLifecycle()
    val selectedOnDeviceModel by viewModel.selectedOnDeviceModel.collectAsStateWithLifecycle()
    val maxTokens by viewModel.maxTokens.collectAsStateWithLifecycle()
    val reasoningEffort by viewModel.reasoningEffort.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val downloadedModelIds by viewModel.downloadedModelIds.collectAsStateWithLifecycle()
    val favoriteModels by viewModel.favoriteModels.collectAsStateWithLifecycle()
    val downloadingModelId by viewModel.downloadingModelId.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val onDeviceModelState by viewModel.onDeviceModelState.collectAsStateWithLifecycle()
    val hiddenModels by viewModel.hiddenModels.collectAsStateWithLifecycle()
    val onDeviceContextLengths by viewModel.onDeviceContextLengths.collectAsStateWithLifecycle()
    val contextUsage by viewModel.contextUsage.collectAsStateWithLifecycle()
    val contextLengths = remember(connectionState, onDeviceContextLengths) {
        val ollama: Map<String, Long> = (connectionState as? ConnectionState.Connected)?.contextLengths?.mapKeys { "ollama:${it.key}" } ?: emptyMap()
        onDeviceContextLengths.mapKeys { "gguf:${it.key}" } + ollama
    }
    val allHistory by viewModel.conversationHistory.collectAsStateWithLifecycle()
    val sessionTitle by viewModel.sessionTitle.collectAsStateWithLifecycle()
    val activeMemories by viewModel.activeMemories.collectAsStateWithLifecycle()
    // Spec 042: prompt actual, registro de herramientas y conjunto deshabilitado para las dos
    // superficies nuevas de la hoja de adjuntos (RF-301, RF-304).
    val systemPrompt by viewModel.systemPrompt.collectAsStateWithLifecycle()
    val toolNames by viewModel.toolNames.collectAsStateWithLifecycle()
    val disabledTools by viewModel.disabledTools.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val haptic = LocalHapticFeedback.current
    val configuration = LocalConfiguration.current
    val isLargeScreen = remember(configuration.screenWidthDp) { isLargeScreenWidth(configuration.screenWidthDp) }
    val chatLayout = remember(configuration.screenWidthDp) { chatLayoutMode(configuration.screenWidthDp) }
    val reduceMotion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val sessions by chatHistoryViewModel.sessions.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf<ChatSessionWithPreview?>(null) }
    var showRenameDialog by remember { mutableStateOf<ChatSessionWithPreview?>(null) }
    var renameText by remember { mutableStateOf("") }
    var drawerSearchQuery by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val name = context.contentResolver.query(
                uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
            )?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx)?.takeIf { it.isNotBlank() } else null
            } ?: uri.lastPathSegment ?: "file"
            val content = try {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
            } catch (_: Exception) { "" }
            if (content.isNotBlank()) {
                viewModel.addPendingAttachment(FileAttachment(name = name, content = content, source = "external"))
            }
        }
    }

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    val listState = rememberLazyListState()

    val effectiveSessionId = if (sessionId > 0) sessionId else viewModel.currentSessionId.value

    LaunchedEffect(sessionId, noteId) {
        if (sessionId > 0) {
            viewModel.loadSession(sessionId, noteId)
        } else if (noteId > 0 && viewModel.currentSessionId.value <= 0) {
            viewModel.createAndStartSession(noteId)
        } else if (sessionId <= 0 && viewModel.currentSessionId.value <= 0) {
            viewModel.createAndStartSession()
        }
        // L6 decide qué se hace con la edición recién cargada; la guarda es imprescindible porque
        // la lógica pura devuelve el texto vacío y `null` de edición cuando **no** hay edición
        // pendiente, y sin la guarda se borraría `inputText` en cada carga de sesión
        // (RNF-11, RF-33 e, caso límite 7).
        if (editingMessage != null) {
            val (adoptedText, adoptedEditing) = adoptPendingEditing(editingMessage)
            inputText = adoptedText
            editingMessage = adoptedEditing
        }
    }

    val conversationHistory = allHistory[effectiveSessionId] ?: emptyList()

    // Spec 038 (RF-273/RF-275): salir de una sesión sin turnos ni draft ni adjuntos la borra
    // (silenciosamente, cascada incluida). Con draft pendiente o con turnos se conserva.
    BackHandler(enabled = !drawerState.isOpen) {
        val draft = effectiveText(inputText, editingMessage)
        val discard = shouldDiscardChatSession(
            turnsCount = conversationHistory.size,
            hasDraftOrAttachments = draft.isNotBlank() || pendingAttachments.isNotEmpty()
        )
        if (discard && effectiveSessionId > 0) {
            chatHistoryViewModel.deleteSession(effectiveSessionId)
        }
        onBack()
    }

    // Spec 037: detalle derivado del anillo — acumulados del turno (spec 036) + ocupación (spec 034).
    val conversationTokenMetrics = remember(conversationHistory) {
        conversationHistory.map { turn ->
            TurnMetrics(
                promptTokens = turn.promptEvalCount,
                generatedTokens = turn.evalCount,
                evalDurationMs = turn.evalDurationMs,
                cachedTokens = turn.promptCachedTokens
            )
        }
    }
    val contextDetail = remember(contextUsage, conversationTokenMetrics) {
        buildContextDetail(contextUsage, conversationTokenMetrics)
    }
    val displayModelName = if (backend == AiBackend.OLLAMA) modelNameSetting
                           else selectedOnDeviceModel?.displayName ?: ""
    val ollamaModels = remember(connectionState) {
        (connectionState as? ConnectionState.Connected)?.models ?: emptyList()
    }
    val connectionErrorText = (connectionState as? ConnectionState.Failed)?.let {
        stringResource(R.string.ai_connection_fail, it.error)
    }

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

    val showRailForInput = shouldShowAssistantRail(chatLayout)
    @Composable
    fun ConversationPane(showHistoryNav: Boolean) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
              modifier = Modifier
              .fillMaxSize(),
              contentWindowInsets = WindowInsets(0,0,0,0),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = sessionTitle.ifBlank { stringResource(R.string.nav_chats) },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier.statusBarsPadding(),
                    navigationIcon = {
                        if (showHistoryNav) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = stringResource(R.string.chat_history_title))
                            }
                        }
                    },
                    actions = {
                        if (effectiveSessionId > 0) {
                            if (conversationHistory.isNotEmpty()) {
                                IconButton(onClick = {
                                    val path = viewModel.exportConversation(context, effectiveSessionId)
                                    if (path != null) {
                                        scope.launch {
                                            val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(android.content.Intent.EXTRA_STREAM,
                                        androidx.core.content.FileProvider.getUriForFile(
                                                        context, "${context.packageName}.fileprovider", java.io.File(path)
                                                        )
                                                )
                                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(android.content.Intent.createChooser(sendIntent, context.getString(R.string.chat_export)))
                                        }
                                    }
                                }) {
                                    Icon(Icons.Default.Share, contentDescription = stringResource(R.string.chat_export))
                                }
                            }
                        }
                    }
                )
            },
            bottomBar = {
              val bottomInsets = WindowInsets.ime.union(WindowInsets.navigationBars)
                // El campo solo pinta y lanza eventos (RF-33): el texto que muestra y el estado al
                // que escribe los decide L6, y si se puede enviar y el texto de ayuda, L4.
                FloatingChatInputField(
                    // El inset vive UNA vez, en el bottomBar: `ime` gana con el teclado abierto,
                    // `navigationBars` con el teclado cerrado. El `Box` de contenido ya no repite
                    // el `imePadding()`, que además duplicaba el `innerPadding` del Scaffold.
                    modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(bottomInsets),
                    attachments = pendingAttachments,
                    text = effectiveText(inputText, editingMessage),
                    onTextChange = { newText ->
                        // L6: la escritura va al mismo estado del que lee el campo (RF-33 e).
                        when (writeTarget(editingMessage != null)) {
                            WriteTarget.EDITING -> editingMessage = newText
                            WriteTarget.DRAFT -> inputText = newText
                        }
                    },
                    onSend = {
                        if (isProcessing) {
                            viewModel.cancelGeneration()
                            return@FloatingChatInputField
                        }
                        val text = effectiveText(inputText, editingMessage)
                        // L4: el envío vacío se descarta aquí y no dentro del componente (RF-33 a).
                        if (!shouldSend(text, pendingAttachments.isNotEmpty())) {
                            return@FloatingChatInputField
                        }
                        // Editar un mensaje ya enviado vuelve a preguntar con GENERATE (RF-33 d).
                        if (editingMessage != null) {
                            viewModel.execute(
                                AiRequest(
                                    action = AiAction.GENERATE,
                                    prompt = text,
                                    selectedText = selectedText,
                                    context = fullContent
                                ),
                                effectiveSessionId
                            )
                            editingMessage = null
                            return@FloatingChatInputField
                        }
                        // Decisión 11 de la spec: el mapeo acción → petición sigue en el Composable.
                        val prompt = when (currentAction) {
                            AiAction.GENERATE -> text
                            AiAction.SUMMARIZE -> context.getString(R.string.ai_chat_prompt_summarize)
                            AiAction.FIX_GRAMMAR -> context.getString(R.string.ai_chat_prompt_fix_grammar)
                        }
                        val request = AiRequest(
                            action = currentAction,
                            prompt = prompt,
                            selectedText = selectedText,
                            context = fullContent,
                            targetLanguage = targetLanguage
                        )
                        viewModel.execute(request, effectiveSessionId)
                        // L7: solo la acción de generación deja el campo limpio (RF-33 f).
                        if (shouldClearInput(currentAction)) inputText = ""
                    },
                    canSend = canSend(
                        effectiveText(inputText, editingMessage),
                        pendingAttachments.isNotEmpty(),
                        isProcessing
                    ),
                    isProcessing = isProcessing,
                    hint = hintFor(conversationHistory.isNotEmpty(), currentAction),
                    onRemoveAttachment = viewModel::removePendingAttachment,
                    onAttachClick = { showAttachmentSheet = true },
                    reasoningEffort = reasoningEffort,
                    onReasoningEffortChange = viewModel::setReasoningEffort,
                    maxTokensPref = maxTokens,
                    modelSelector = ModelSelectorState(
                        backend = backend,
                        selectedOnDeviceId = selectedOnDeviceModel?.id,
                        downloadedIds = downloadedModelIds,
                        catalogModels = MODEL_CATALOG,
                        modelName = displayModelName,
                        ollamaModels = ollamaModels,
                        isTesting = connectionState is ConnectionState.Testing,
                        connectionError = connectionErrorText,
                        favorites = favoriteModels,
                        downloadingModelId = downloadingModelId,
                        downloadState = downloadState,
                        onDeviceModelState = onDeviceModelState,
                        hiddenModels = hiddenModels,
                        contextLengths = contextLengths
                    ),
                    onSelectOnDevice = { model ->
                        viewModel.selectOnDeviceModel(model)
                        viewModel.setBackend(AiBackend.ON_DEVICE)
                    },
                    onSelectOllama = { name ->
                        if (name.isNotBlank()) {
                            viewModel.setModelName(name)
                            viewModel.setBackend(AiBackend.OLLAMA)
                        }
                    },
                    onTestConnection = viewModel::testConnection,
                    onToggleFavoriteModel = viewModel::toggleFavoriteModel,
                    onDownloadModel = viewModel::downloadOnDeviceModel,
                    onLoadModel = { model ->
                        viewModel.selectOnDeviceModel(model)
                        viewModel.setBackend(AiBackend.ON_DEVICE)
                        viewModel.loadSelectedModel()
                    },
                    onUnloadModel = viewModel::unloadModel,
                    onModelPanelOpen = {
                        if (showRailForInput) assistantDestination = AssistantDestination.MODELS
                        else showModelPanel = true
                    },
                    onShowModelInfo = { infoModel = it },
                    modelSheetDismissTick = modelSheetDismissTick,
                    contextDetail = contextDetail
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                  // Spec 035 (RF-258): carga del motor on-device visible, indeterminada; nada en Ollama.
                  if (shouldShowModelLoadingIndicator(backend, onDeviceModelState)) {
                      LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
                  }
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (conversationHistory.isEmpty() && !isStreamingEmpty && !hasStreamingContent) {
                            // Día local: días desde época en la zona horaria del dispositivo. Se calcula
                            // una sola vez, con `remember` sin claves (decisión D-3), y no va al
                            // ViewModel (RNF-14 lo prohíbe).
                            val todayEpochDay = remember { LocalDate.now().toEpochDay() }
                            EmptyChatWelcome(
                                greeting = stringResource(
                                    when (greetingIndexFor(todayEpochDay, 3)) {
                                        0 -> R.string.ai_chat_greeting_1
                                        1 -> R.string.ai_chat_greeting_2
                                        else -> R.string.ai_chat_greeting_3
                                    }
                                ),
                                // L1 decide cuáles se ven; aquí no se añade ni se quita ninguna.
                                suggestions = visibleSuggestions(
                                    hasSelectedText = selectedText.isNotBlank(),
                                    hasNoteContent = fullContent.isNotBlank(),
                                    hasAttachments = pendingAttachments.isNotEmpty()
                                ),
                                onSuggestion = { suggestion ->
                                    // L2 decide la petición. La etiqueta llega ya resuelta, porque
                                    // `EmptyChatWelcome` rellena la copia que entrega (RF-37).
                                    viewModel.execute(
                                        requestFor(
                                            suggestion = suggestion,
                                            selectedText = selectedText,
                                            noteContent = fullContent,
                                            summarizePrompt = context.getString(R.string.ai_chat_prompt_summarize),
                                            targetLanguage = targetLanguage
                                        ),
                                        effectiveSessionId
                                    )
                                }
                            )
                        } else {
                            ChatMessageList(
                                conversationHistory = conversationHistory,
                                isStreamingEmpty = isStreamingEmpty,
                                hasStreamingContent = hasStreamingContent,
                                streamingText = streamingText,
                                displayModelName = displayModelName,
                                listState = listState,
                                clipboard = clipboard,
                                haptic = haptic,
                                onInsert = onInsert,
                                onResend = { text -> editingMessage = text; inputText = text },
                                onEdit = { text -> editingMessage = text; inputText = text },
                                selectedText = selectedText,
                                fullContent = fullContent,
                                effectiveSessionId = effectiveSessionId,
                                viewModel = viewModel
                            )
                        }
                    }
                }

                // Spec 035 (RF-259): durante LOADING no se pinta el banner; vuelve a NOT_LOADED/ERROR.
                val showError = shouldShowUnavailableError(
                    modelLoading = shouldShowModelLoadingIndicator(backend, onDeviceModelState),
                    available = viewModel.isAvailable(),
                    historyEmpty = conversationHistory.isEmpty()
                )
                if (showError) {
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
    }

    @Composable
    fun ModelInfoContent(model: com.example.data.ai.OnDeviceModel) {
        ModelInfoPanel(
            state = ModelInfoState(
                model = model,
                isDownloaded = model.id in downloadedModelIds,
                isSelected = backend == AiBackend.ON_DEVICE && selectedOnDeviceModel?.id == model.id,
                engineState = onDeviceModelState,
                isDownloadingThis = downloadingModelId == model.id,
                isAnyDownloadActive = downloadingModelId != null,
                downloadProgress = (downloadState as? DownloadState.Downloading)?.progress,
                contextLength = contextLengths["gguf:${model.id}"]
            ),
            isLargeScreen = isLargeScreen,
            onDismiss = { infoModel = null },
            onLoad = {
                viewModel.selectOnDeviceModel(model)
                viewModel.setBackend(AiBackend.ON_DEVICE)
                viewModel.loadSelectedModel()
            },
            onUnload = viewModel::unloadModel,
            onDownload = { viewModel.downloadOnDeviceModel(model) },
            onNewChat = {
                viewModel.selectOnDeviceModel(model)
                viewModel.setBackend(AiBackend.ON_DEVICE)
                chatHistoryViewModel.createSession(backend = AiBackend.ON_DEVICE, modelName = model.displayName)
                showModelPanel = false
                modelSheetDismissTick += 1
                infoModel = null
                assistantDestination = AssistantDestination.CHAT
            },
            onDelete = { viewModel.deleteOnDeviceModel(model) },
            loadChatTemplate = { m -> viewModel.readChatTemplate(m) }
        )
    }
    @Composable
    fun ModelManagerPanelAsDestination(
        onClose: () -> Unit,
        onShowInfo: (com.example.data.ai.OnDeviceModel) -> Unit,
        onSelectOllama: (String) -> Unit
    ) {
        ModelManagerPanel(
            state = ModelSelectorState(
                backend = backend,
                selectedOnDeviceId = selectedOnDeviceModel?.id,
                downloadedIds = downloadedModelIds,
                catalogModels = MODEL_CATALOG,
                modelName = displayModelName,
                ollamaModels = ollamaModels,
                isTesting = connectionState is ConnectionState.Testing,
                connectionError = connectionErrorText,
                favorites = favoriteModels,
                downloadingModelId = downloadingModelId,
                downloadState = downloadState,
                onDeviceModelState = onDeviceModelState,
                hiddenModels = hiddenModels,
                contextLengths = contextLengths
            ),
            onClose = onClose,
            onSelectOnDevice = { model ->
                viewModel.selectOnDeviceModel(model)
                viewModel.setBackend(AiBackend.ON_DEVICE)
            },
            onSelectOllama = onSelectOllama,
            onDownloadModel = viewModel::downloadOnDeviceModel,
            onLoadModel = { model ->
                viewModel.selectOnDeviceModel(model)
                viewModel.setBackend(AiBackend.ON_DEVICE)
                viewModel.loadSelectedModel()
            },
            onUnloadModel = viewModel::unloadModel,
            onDeleteModel = viewModel::deleteOnDeviceModel,
            onToggleFavorite = viewModel::toggleFavoriteModel,
            onToggleHidden = viewModel::toggleHiddenModel,
            onShowInfo = onShowInfo
        )
    }

    val showRail = shouldShowAssistantRail(chatLayout)
    // Al volver a Compacto, el destino se reconduce a Chat (RF-377/RF-379).
    LaunchedEffect(showRail) {
        if (!showRail) assistantDestination = AssistantDestination.CHAT
    }

    if (showRail) {
        Row(modifier = Modifier.fillMaxSize()) {
            AssistantRail(
                destination = assistantDestination,
                expanded = railExpanded,
                sessions = sessions,
                activeSessionId = effectiveSessionId,
                onDestinationChange = { assistantDestination = it },
                onToggleExpanded = { railExpanded = !railExpanded },
                onNewChat = { chatHistoryViewModel.createSession(backend = backend) },
                onSearch = onNavigateToChatSearch,
                onSettings = onNavigateToSettings,
                onNavigateToMain = onNavigateToMain,
                onNavigateToChat = { id -> viewModel.loadSession(id, 0) },
                onTogglePin = { session -> chatHistoryViewModel.togglePin(session.id, session.isPinned) }
            )
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                AnimatedContent(
                    targetState = assistantDestination,
                    transitionSpec = {
                        if (reduceMotion) {
                            fadeIn(animationSpec = tween(0)) togetherWith fadeOut(animationSpec = tween(0))
                        } else {
                            fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(120))
                        }
                    },
                    label = "assistantDestination"
                ) { destination ->
                when (destination) {
                    AssistantDestination.CHAT -> ConversationPane(showHistoryNav = false)
                    AssistantDestination.MODELS -> {
                        if (infoModel != null) {
                            ModelInfoContent(infoModel!!)
                        } else {
                            ModelManagerPanelAsDestination(
                        onClose = { assistantDestination = AssistantDestination.CHAT },
                        onShowInfo = { infoModel = it },
                        onSelectOllama = { name ->
                            if (name.isNotBlank()) {
                                viewModel.setModelName(name)
                                viewModel.setBackend(AiBackend.OLLAMA)
                            }
                            assistantDestination = AssistantDestination.CHAT
                        }
                        )
                        }
                    }
                }
                }
            }
        }
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = false,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.width(320.dp),
                    drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                ) {
                    ChatHistoryDrawerContent(
                        sessions = sessions,
                        drawerSearchQuery = drawerSearchQuery,
                        onSearchQueryChange = { drawerSearchQuery = it },
                        onNavigateToSession = { sessionId ->
                            viewModel.loadSession(sessionId, 0)
                            scope.launch { drawerState.close() }
                        },
                        onCreateSession = {
                            chatHistoryViewModel.createSession(backend = backend)
                            scope.launch { drawerState.close() }
                        },
                        onRename = { session ->
                            renameText = session.title
                            showRenameDialog = session
                        },
                        onDelete = { showDeleteDialog = it },
                        onTogglePin = { session -> chatHistoryViewModel.togglePin(session.id, session.isPinned) },
                        chatHistoryViewModel = chatHistoryViewModel,
                        drawerState = { scope.launch { drawerState.close() } }
                    )
                }
            }
        ) {
            ConversationPane(showHistoryNav = true)
        }
    }

    val latestSessionId by chatHistoryViewModel.latestSessionId.collectAsStateWithLifecycle()
    LaunchedEffect(latestSessionId) {
        latestSessionId?.let { id ->
            chatHistoryViewModel.clearLatestSessionId()
            viewModel.loadSession(id, 0)
        }
    }

    if (showRenameDialog != null) {
        val session = showRenameDialog!!
        AlertDialog(
            onDismissRequest = { showRenameDialog = null },
            title = { Text(stringResource(R.string.chat_rename_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text(stringResource(R.string.chat_title_hint)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (renameText.isNotBlank()) {
                        chatHistoryViewModel.renameSession(session.id, renameText)
                    }
                    showRenameDialog = null
                }) { Text(stringResource(R.string.chat_rename)) }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = null }) { Text(stringResource(R.string.btn_cancel)) }
            }
        )
    }

    if (showDeleteDialog != null) {
        val session = showDeleteDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(stringResource(R.string.chat_delete_confirm)) },
            text = { Text(session.title) },
            confirmButton = {
                TextButton(onClick = {
                    chatHistoryViewModel.deleteSession(session.id)
                    showDeleteDialog = null
                }) { Text(stringResource(R.string.btn_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text(stringResource(R.string.btn_cancel)) }
            }
        )
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

    if (showNotePicker) {
        NoteAttachmentSheet(
            viewModel = viewModel,
            searchQuery = notePickerSearchQuery,
            onSearchQueryChange = { query ->
                notePickerSearchQuery = query
                viewModel.loadAvailableNotes(query)
            },
            onDismiss = { showNotePicker = false },
            onNoteSelected = { note ->
                viewModel.attachNoteById(note.note.id)
                showNotePicker = false
            }
        )
    }

    // La hoja de adjuntos es dueña de su `ModalBottomSheet` (decisión D-6): aquí solo se le pasan
    // los callbacks. El cierre antes de abrir cada superficie ya lo hace ella (RF-35 extendido).
    if (showAttachmentSheet) {
        AttachmentAiSheet(
            onPickNote = {
                showNotePicker = true
                notePickerSearchQuery = ""
                viewModel.loadAvailableNotes()
            },
            onPickFile = {
                filePickerLauncher.launch(arrayOf("text/*", "application/json", "application/xml"))
            },
            onOpenSystemMessage = { showSystemMessageDialog = true },
            onOpenTools = { showToolsSheet = true },
            onDismiss = { showAttachmentSheet = false }
        )
    }

    // Editor del prompt personalizado (spec 042, RF-301…RF-303): guardar no envía ninguna petición;
    // el cambio se aplica a la siguiente por el camino de resolución de siempre.
    if (showSystemMessageDialog) {
        SystemMessageDialog(
            currentPrompt = systemPrompt,
            onSave = { texto ->
                viewModel.setSystemPrompt(texto)
                showSystemMessageDialog = false
            },
            onDismiss = { showSystemMessageDialog = false }
        )
    }

    // Submenú de herramientas (spec 042 RF-304/RF-305; 043 RF-308/RF-309): marcar, desmarcar o
    // accionar el maestro persiste en el acto; la pantalla solo pasa datos y callbacks.
    if (showToolsSheet) {
        ToolsSheet(
            toolNames = toolNames,
            disabledTools = disabledTools,
            onToggle = viewModel::toggleTool,
            onSetAll = viewModel::setAllTools,
            onDismiss = { showToolsSheet = false }
        )
    }

    // Panel de gestión de modelos a pantalla completa (spec 033, RF-238…RF-247): hermano de
    // Scaffold, animado después — se pinta por encima; el atrás también lo cierra (RF-238).
    // Modelos como overlay solo en Compacto; en Media+ es destino del área principal (RF-372/RF-374).
    if (showModelPanel && !showRail) {
        ModelManagerPanelAsDestination(
            onClose = { showModelPanel = false },
            onShowInfo = { infoModel = it },
            onSelectOllama = { name ->
                if (name.isNotBlank()) {
                    viewModel.setModelName(name)
                    viewModel.setBackend(AiBackend.OLLAMA)
                }
                showModelPanel = false
            }
        )
    }


    // Spec 040: en Media+ el detalle se integra en el destino Modelos (RF-372/RF-374);
    // en Compacto sigue como overlay (RF-377).
    if (!showRail) {
        infoModel?.let { model -> ModelInfoContent(model) }
    }

}

@Composable
fun MessageBubble(
    turn: ConversationTurn,
    clipboard: Clipboard,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    isLastAssistant: Boolean,
    showInsert: Boolean,
    onInsert: () -> Unit,
    onResend: (String) -> Unit,
    onRetry: () -> Unit,
    onEdit: (String) -> Unit,
    onPinToMemory: (String) -> Unit,
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
    val clipboardScope = rememberCoroutineScope()
    val context = LocalContext.current
    val actions = bubbleActions(
        isUser = isUser,
        isError = isError,
        isLastAssistant = isLastAssistant,
        canInsert = showInsert,
        hasContent = turn.content.isNotBlank()
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
                            text = stringResource(R.string.ai),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = if (isUser) stringResource(R.string.ai_chat_you) else "",
                style = MaterialTheme.typography.labelSmall,
                color = labelColor
            )
        }

        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .widthIn(max = 480.dp)
                .then(if (isError) Modifier.clickable { onRetry() } else Modifier)
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
                    if (turn.files.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        turn.files.forEach { file ->
                            Surface(
                                modifier = Modifier,
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.InsertDriveFile,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = textColor.copy(alpha = 0.6f)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = file.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = textColor.copy(alpha = 0.7f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                    ChatMarkdownText(
                        crudo = turn.content,
                        color = textColor
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
                    val metricTokens = if (isUser) turn.promptEvalCount else turn.evalCount
                    val metricMs = if (isUser) turn.promptEvalDurationMs else turn.evalDurationMs
                    if (metricTokens != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.chat_msg_tokens, metricTokens),
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.5f)
                        )
                    }
                    turn.formattedDuration?.let { dur ->
                        Text(
                            text = stringResource(R.string.chat_msg_duration, dur),
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.5f)
                        )
                    }
                    if (metricTokens != null && metricMs != null && metricMs > 0L) {
                        tokensPerSecond(metricTokens, metricMs * 1_000_000L)?.let { tps ->
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.chat_msg_tok_per_s, String.format(Locale.getDefault(), "%.1f", tps)),
                                style = MaterialTheme.typography.labelSmall,
                                color = textColor.copy(alpha = 0.5f)
                            )
                        }
                    }
                    if (!isUser && modelName.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.chat_msg_model, modelName),
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.35f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (actions.isNotEmpty()) {
                    BubbleActionRow(
                        actions = actions,
                        textColor = textColor,
                        onAction = { action ->
                            when (action) {
                                BubbleAction.COPY -> clipboardScope.launch {
                                    clipboard.setClipEntry(
                                        ClipEntry(ClipData.newPlainText("secure_notes", turn.content))
                                    )
                                }
                                BubbleAction.SHARE -> {
                                    val sendIntent = android.content.Intent().apply {
                                        this.action = android.content.Intent.ACTION_SEND
                                        putExtra(android.content.Intent.EXTRA_TEXT, turn.content)
                                        type = "text/plain"
                                    }
                                    context.startActivity(android.content.Intent.createChooser(sendIntent, null))
                                }
                                BubbleAction.EDIT -> onEdit(turn.content)
                                BubbleAction.RESEND -> onResend(turn.content)
                                BubbleAction.RETRY -> onRetry()
                                BubbleAction.INSERT -> onInsert()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BubbleActionRow(
    actions: List<BubbleAction>,
    textColor: Color,
    onAction: (BubbleAction) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        actions.forEach { action ->
            val (icon, labelRes) = bubbleActionIcon(action)
            IconButton(
                onClick = { onAction(action) },
                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = stringResource(labelRes),
                    modifier = Modifier.size(20.dp),
                    tint = textColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

private fun bubbleActionIcon(action: BubbleAction): Pair<androidx.compose.ui.graphics.vector.ImageVector, Int> = when (action) {
    BubbleAction.EDIT -> Icons.Default.Edit to R.string.chat_action_edit
    BubbleAction.RESEND -> Icons.Default.Refresh to R.string.ai_resend
    BubbleAction.COPY -> Icons.Default.ContentCopy to R.string.chat_action_copy
    BubbleAction.SHARE -> Icons.Default.Share to R.string.chat_action_share
    BubbleAction.RETRY -> Icons.Default.Refresh to R.string.chat_action_retry
    BubbleAction.INSERT -> Icons.Default.ContentPaste to R.string.ai_insert
}

@Composable
fun MessageStatusIcon(status: MessageStatus, tint: Color) {
    when (status) {
        MessageStatus.SENT -> {
            Icon(
                Icons.Default.Check,
                contentDescription = stringResource(R.string.chat_status_sent),
                modifier = Modifier.size(12.dp),
                tint = tint.copy(alpha = 0.4f)
            )
        }
        MessageStatus.DELIVERED -> {
            Icon(
                Icons.Default.DoneAll,
                contentDescription = stringResource(R.string.chat_status_delivered),
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
            val generatingDesc = stringResource(R.string.chat_status_generating)
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .semantics { contentDescription = generatingDesc },
                contentAlignment = Alignment.Center
            ) {
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
                contentDescription = stringResource(R.string.chat_status_completed),
                modifier = Modifier.size(12.dp),
                tint = tint.copy(alpha = 0.6f)
            )
        }
        MessageStatus.ERROR -> {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = stringResource(R.string.chat_status_error),
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.error
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
                    Text(stringResource(R.string.ai), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp),
            modifier = Modifier.widthIn(max = 480.dp)
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
                            text = stringResource(R.string.chat_msg_model, modelName),
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
                        Text(stringResource(R.string.ai), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
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
fun ChatMessageList(
    conversationHistory: List<ConversationTurn>,
    isStreamingEmpty: Boolean,
    hasStreamingContent: Boolean,
    streamingText: String?,
    displayModelName: String,
    listState: androidx.compose.foundation.lazy.LazyListState,
    clipboard: Clipboard,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    onInsert: ((String) -> Unit)?,
    onResend: (String) -> Unit,
    onEdit: (String) -> Unit,
    selectedText: String,
    fullContent: String,
    effectiveSessionId: Int,
    viewModel: AiViewModel
) {
    // El motor de GFM se construye **una vez para toda la lista** y se reparte con un `CompositionLocal`
    // (spec 017, RF-98). Va aquí y no dentro de cada burbuja a propósito: la lista descarta lo que sale de
    // pantalla, así que un motor por burbuja se reconstruiría en cada vuelta al mensaje.
    MarkdownEngine.ProvidesMarkdownEngine(colores = markdownColors()) {
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
            key = { "msg_${it.id}" },
            // contentType por rol: los nodos de mensaje (burbuja de usuario vs de asistente)
            // se reutilizan entre si al recorrer la lista en vez de recomponerse enteros.
            contentType = { if (it.role == "user") USER_TURN else ASSISTANT_TURN }
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
                    clipboard = clipboard,
                    haptic = haptic,
                    isLastAssistant = turn == conversationHistory.lastOrNull() && turn.role == "assistant",
                    showInsert = turn.role == "assistant" && onInsert != null,
                    onInsert = { onInsert?.invoke(turn.content) },
                    onResend = onResend,
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
                    onEdit = onEdit,
                    onPinToMemory = { content -> viewModel.savePinnedMemory(effectiveSessionId, content) },
                    modelName = turn.modelName ?: displayModelName
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
                // La réplica EN CURSO se sigue pintando con el analizador de texto enriquecido, y no con el
                // motor de GFM (spec 017, RF-103). Dos razones, y ninguna es que no se pueda: (1) el motor
                // parsea en el hilo principal, y aquí el texto cambia con cada fragmento; (2) más importante,
                // el texto a medio escribir **cambiaría de motor a mitad**: una tabla que aún no tiene línea
                // separadora se vería como texto y un renglón después como tabla, y ese parpadeo se ve más
                // que la inconsistencia entre la burbuja en curso y la ya guardada.
                StreamingBubble(
                    text = streamingText!!,
                    modelName = displayModelName
                )
            }
        }
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteAttachmentSheet(
    viewModel: AiViewModel,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onNoteSelected: (DecryptedNote) -> Unit
) {
    val context = LocalContext.current
    val notes by viewModel.availableNotes.collectAsStateWithLifecycle()
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = stringResource(R.string.select_note_to_attach),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text(stringResource(R.string.search_notes)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (notes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_notes_found),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(
                        items = notes,
                        key = { "attach_${it.note.id}" },
                        contentType = { ATTACHMENT_ROW }
                    ) { decryptedNote ->
                        val note = decryptedNote.note
                        Surface(
                            onClick = { onNoteSelected(decryptedNote) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (note.isFavorite) Icons.Default.Star else Icons.Default.Description,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (note.isFavorite) MaterialTheme.colorScheme.primary
                                           else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = decryptedNote.title.ifBlank { stringResource(R.string.untitled_note) },
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (decryptedNote.isDecryptionSuccessful && decryptedNote.content.isNotBlank()) {
                                        Text(
                                            text = com.example.util.RichTextConverter.contentToPlainText(decryptedNote.content, context)
                                                .take(80).replace("\n", " "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    } else if (!decryptedNote.isDecryptionSuccessful) {
                                        Text(
                                            text = stringResource(R.string.encrypted_note_locked),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ChatHistoryDrawerContent(
    sessions: List<ChatSessionWithPreview>,
    drawerSearchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onNavigateToSession: (Int) -> Unit,
    onCreateSession: () -> Unit,
    onRename: (ChatSessionWithPreview) -> Unit,
    onDelete: (ChatSessionWithPreview) -> Unit,
    onTogglePin: (ChatSessionWithPreview) -> Unit,
    chatHistoryViewModel: ChatHistoryViewModel,
    drawerState: () -> Unit
) {
    val filteredSessions = remember(sessions, drawerSearchQuery) {
        if (drawerSearchQuery.isBlank()) sessions
        else sessions.filter {
            it.title.contains(drawerSearchQuery, ignoreCase = true) ||
            it.previewText?.contains(drawerSearchQuery, ignoreCase = true) == true
        }
    }
    val pinnedSessions = filteredSessions.filter { it.isPinned }
    val otherSessions = filteredSessions.filter { !it.isPinned }

    Column(modifier = Modifier.fillMaxHeight()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.chat_history_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { drawerState() }) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
            }
        }

        OutlinedTextField(
            value = drawerSearchQuery,
            onValueChange = { query ->
                onSearchQueryChange(query)
                chatHistoryViewModel.setSearchQuery(query)
            },
            placeholder = { Text(stringResource(R.string.chat_search_hint)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            trailingIcon = {
              if (drawerSearchQuery.isNotEmpty()) {
              IconButton(onClick = {
                chatHistoryViewModel.setSearchQuery("")
                onSearchQueryChange("")
            }) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
            } }
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onCreateSession,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.chat_new))
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredSessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.chat_empty_title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (pinnedSessions.isNotEmpty()) {
                    item(key = "pinned_header") {
                        Text(
                            stringResource(R.string.chat_pinned_section),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                        )
                    }
                    items(pinnedSessions, key = { "pin_${it.id}" }) { session ->
                        ChatSessionCard(
                            session = session,
                            onClick = { onNavigateToSession(session.id) },
                            onRename = { onRename(session) },
                            onDelete = { onDelete(session) },
                            onTogglePin = { onTogglePin(session) }
                        )
                    }
                }

                if (otherSessions.isNotEmpty()) {
                    if (pinnedSessions.isNotEmpty()) {
                        item(key = "divider") {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                    items(otherSessions, key = { "sess_${it.id}" }) { session ->
                        ChatSessionCard(
                            session = session,
                            onClick = { onNavigateToSession(session.id) },
                            onRename = { onRename(session) },
                            onDelete = { onDelete(session) },
                            onTogglePin = { onTogglePin(session) }
                        )
                    }
                }

                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

// Claves de `contentType` para los listas del chat. Ints: no asignan por item.
// T4: ATTACHMENT_CHIP vive ya en `ui/chat/ChatInputState.kt`, su única declaración (RF-39).
private const val USER_TURN = 1
private const val ASSISTANT_TURN = 2
private const val ATTACHMENT_ROW = 3
