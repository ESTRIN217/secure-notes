package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PreferencesRepository
import com.example.data.ai.*
import com.example.data.local.ChatSessionDao
import com.example.data.local.ChatSessionEntity
import com.example.data.local.ConversationDao
import com.example.data.local.ConversationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MessageStatus {
    SENT,
    DELIVERED,
    GENERATING,
    COMPLETED,
    ERROR
}

data class ConversationTurn(
    val role: String,
    val content: String,
    val processingTimeMs: Long? = null,
    val status: MessageStatus = MessageStatus.COMPLETED,
    val timestamp: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
) {
    val formattedTime: String
        get() = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))

    val formattedDuration: String?
        get() = processingTimeMs?.let { "%.1fs".format(it / 1000.0) }
}

sealed class ConnectionState {
    data object Unknown : ConnectionState()
    data object Testing : ConnectionState()
    data class Connected(val models: List<String>) : ConnectionState()
    data class Failed(val error: String) : ConnectionState()
}

class AiViewModel(
    application: Application,
    private val prefsRepository: PreferencesRepository,
    private val ollamaService: OllamaService,
    private val onDeviceService: OnDeviceService,
    private val modelDownloader: ModelDownloader,
    private val conversationDao: ConversationDao,
    private val chatSessionDao: ChatSessionDao
) : AndroidViewModel(application) {

    private val _aiEnabled = MutableStateFlow(prefsRepository.getAiEnabled())
    val aiEnabled: StateFlow<Boolean> = _aiEnabled.asStateFlow()

    private val _backend = MutableStateFlow(
        when (prefsRepository.getAiBackend()) {
            "ondevice" -> AiBackend.ON_DEVICE
            else -> AiBackend.OLLAMA
        }
    )
    val backend: StateFlow<AiBackend> = _backend.asStateFlow()

    private val _endpointUrl = MutableStateFlow(prefsRepository.getAiEndpointUrl())
    val endpointUrl: StateFlow<String> = _endpointUrl.asStateFlow()

    private val _modelName = MutableStateFlow(prefsRepository.getAiModelName())
    val modelName: StateFlow<String> = _modelName.asStateFlow()

    private val _onDeviceModelPath = MutableStateFlow(prefsRepository.getAiOnDeviceModelPath())
    val onDeviceModelPath: StateFlow<String> = _onDeviceModelPath.asStateFlow()

    private val _selectedOnDeviceModel = MutableStateFlow<OnDeviceModel?>(null)
    val selectedOnDeviceModel: StateFlow<OnDeviceModel?> = _selectedOnDeviceModel.asStateFlow()

    private val _systemPrompt = MutableStateFlow(prefsRepository.getAiSystemPrompt())
    val systemPrompt: StateFlow<String> = _systemPrompt.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _streamingText = MutableStateFlow<String?>(null)
    val streamingText: StateFlow<String?> = _streamingText.asStateFlow()

    private val _processingTimeMs = MutableStateFlow<Long?>(null)
    val processingTimeMs: StateFlow<Long?> = _processingTimeMs.asStateFlow()

    private val _resultText = MutableStateFlow<String?>(null)
    val resultText: StateFlow<String?> = _resultText.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _chatNoteContext = MutableStateFlow("")
    val chatNoteContext: StateFlow<String> = _chatNoteContext.asStateFlow()

    private val _chatNoteTitle = MutableStateFlow<String?>(null)
    val chatNoteTitle: StateFlow<String?> = _chatNoteTitle.asStateFlow()

    private val _chatSelectedText = MutableStateFlow("")
    val chatSelectedText: StateFlow<String> = _chatSelectedText.asStateFlow()

    private val _pendingInsert = MutableStateFlow<String?>(null)
    val pendingInsert: StateFlow<String?> = _pendingInsert.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Unknown)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    val onDeviceModelState: StateFlow<ModelState> = onDeviceService.modelState
    val onDeviceLoadedModelInfo: StateFlow<LoadedModelInfo?> = onDeviceService.loadedModelInfo

    val deviceInfo: DeviceInfo = DeviceInfo.detect(getApplication())
    val recommendedModels: List<OnDeviceModel> = MODEL_CATALOG.filterForDevice(deviceInfo)
    val bestModel: OnDeviceModel? = MODEL_CATALOG.bestForDevice(deviceInfo)
    val downloadState: StateFlow<DownloadState> = modelDownloader.state

    private val _conversationHistory = MutableStateFlow<Map<Int, List<ConversationTurn>>>(emptyMap())
    val conversationHistory: StateFlow<Map<Int, List<ConversationTurn>>> = _conversationHistory.asStateFlow()

    private val _currentSessionId = MutableStateFlow(0)
    val currentSessionId: StateFlow<Int> = _currentSessionId.asStateFlow()

    private val _sessionTitle = MutableStateFlow("New Chat")
    val sessionTitle: StateFlow<String> = _sessionTitle.asStateFlow()

    private val _currentNoteId = MutableStateFlow(0)
    val currentNoteId: StateFlow<Int> = _currentNoteId.asStateFlow()

    private var currentJob: Job? = null

    init {
        val savedPath = prefsRepository.getAiOnDeviceModelPath()
        if (savedPath.isNotBlank()) {
            val matching = MODEL_CATALOG.firstOrNull { model ->
                savedPath.endsWith(model.ggufFileName)
            }
            if (matching != null) {
                _selectedOnDeviceModel.value = matching
                if (modelDownloader.isDownloaded(matching)) {
                    viewModelScope.launch {
                        onDeviceService.loadModel(savedPath, matching)
                    }
                }
            }
        }
    }

    private val currentService: AIService
        get() = if (_backend.value == AiBackend.ON_DEVICE) onDeviceService else ollamaService

    fun isAvailable(): Boolean = currentService.isAvailable

    fun getConversationHistory(sessionId: Int): List<ConversationTurn> {
        return _conversationHistory.value[sessionId] ?: emptyList()
    }

    fun clearConversationHistory(sessionId: Int) {
        _conversationHistory.update { current ->
            current.toMutableMap().apply { remove(sessionId) }.toMap()
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) { conversationDao.deleteBySessionId(sessionId) }
        }
    }

    fun setAiEnabled(enabled: Boolean) {
        _aiEnabled.value = enabled
        prefsRepository.setAiEnabled(enabled)
    }

    fun prepareChatForNote(context: String, selected: String, noteTitle: String? = null) {
        _chatNoteContext.value = context
        _chatSelectedText.value = selected
        _chatNoteTitle.value = noteTitle
    }

    fun requestInsert(text: String) {
        _pendingInsert.value = text
    }

    fun clearInsertResult() {
        _pendingInsert.value = null
    }

    fun setBackend(backend: AiBackend) {
        _backend.value = backend
        prefsRepository.setAiBackend(if (backend == AiBackend.ON_DEVICE) "ondevice" else "ollama")
    }

    fun setEndpointUrl(url: String) {
        _endpointUrl.value = url
        prefsRepository.setAiEndpointUrl(url)
        ollamaService.updateConfig(url, _modelName.value)
        _connectionState.value = ConnectionState.Unknown
    }

    fun setModelName(model: String) {
        _modelName.value = model
        prefsRepository.setAiModelName(model)
        ollamaService.updateConfig(_endpointUrl.value, model)
    }

    fun setOnDeviceModelPath(path: String) {
        _onDeviceModelPath.value = path
        prefsRepository.setAiOnDeviceModelPath(path)
    }

    fun setSystemPrompt(prompt: String) {
        _systemPrompt.value = prompt
        prefsRepository.setAiSystemPrompt(prompt)
    }

    fun selectOnDeviceModel(model: OnDeviceModel) {
        _selectedOnDeviceModel.value = model
        val path = modelDownloader.getModelPath(model)
        if (path != null) {
            setOnDeviceModelPath(path)
        }
    }

    fun downloadSelectedModel() {
        val model = _selectedOnDeviceModel.value ?: return
        viewModelScope.launch {
            modelDownloader.download(model)
            if (modelDownloader.state.value is DownloadState.Completed) {
                loadSelectedModel()
            }
        }
    }

    fun cancelDownload() {
        modelDownloader.cancel()
    }

    fun deleteDownloadedModel() {
        val model = _selectedOnDeviceModel.value ?: return
        onDeviceService.unloadModel()
        modelDownloader.deleteModel(model)
        modelDownloader.resetState()
    }

    fun loadSelectedModel() {
        val model = _selectedOnDeviceModel.value ?: return
        val path = modelDownloader.getModelPath(model) ?: run {
            _errorMessage.value = "El modelo aún no se ha descargado"
            return
        }
        viewModelScope.launch {
            val result = onDeviceService.loadModel(path, model)
            if (result.isSuccess) {
                setOnDeviceModelPath(path)
                _errorMessage.value = null
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Error al cargar el modelo"
            }
        }
    }

    fun unloadModel() {
        onDeviceService.unloadModel()
    }

    fun isModelDownloaded(model: OnDeviceModel): Boolean {
        return modelDownloader.isDownloaded(model)
    }

    fun getModelPath(model: OnDeviceModel): String? {
        return modelDownloader.getModelPath(model)
    }

    fun testConnection() {
        _connectionState.value = ConnectionState.Testing
        viewModelScope.launch {
            val result = ollamaService.testConnection()
            _connectionState.value = result.fold(
                onSuccess = { models -> ConnectionState.Connected(models) },
                onFailure = { error -> ConnectionState.Failed(error.message ?: "Error desconocido") }
            )
        }
    }

    fun loadSession(sessionId: Int, noteId: Int = 0) {
        _currentSessionId.value = sessionId
        _currentNoteId.value = noteId
        if (sessionId > 0) {
            loadConversation(sessionId)
            viewModelScope.launch {
                val session = withContext(Dispatchers.IO) { chatSessionDao.getSession(sessionId) }
                if (session != null) {
                    _sessionTitle.value = session.title
                    _currentNoteId.value = session.noteId ?: 0
                }
            }
        }
    }

    fun renameCurrentSession(title: String) {
        val id = _currentSessionId.value
        if (id <= 0) return
        _sessionTitle.value = title
        viewModelScope.launch {
            withContext(Dispatchers.IO) { chatSessionDao.updateTitle(id, title) }
        }
    }

    fun createAndStartSession(noteId: Int = 0, noteTitle: String? = null) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val title = if (noteTitle != null) "Chat - $noteTitle" else "New Chat"
            val session = ChatSessionEntity(
                title = title,
                noteId = noteId.takeIf { it > 0 },
                noteTitle = noteTitle,
                backend = if (_backend.value == AiBackend.ON_DEVICE) "ondevice" else "ollama",
                modelName = _modelName.value,
                createdAt = now,
                updatedAt = now,
                messageCount = 0
            )
            val id = withContext(Dispatchers.IO) { chatSessionDao.insert(session) }.toInt()
            _currentSessionId.value = id
            _currentNoteId.value = noteId
            _sessionTitle.value = title
        }
    }

    fun detachNote() {
        _chatNoteContext.value = ""
        _chatSelectedText.value = ""
        _chatNoteTitle.value = null
        _currentNoteId.value = 0
    }

    fun hasNoteContext(): Boolean = _currentNoteId.value > 0 && _chatNoteContext.value.isNotBlank()

    fun loadConversation(sessionId: Int) {
        if (_conversationHistory.value.containsKey(sessionId)) return
        viewModelScope.launch {
            val turns = withContext(Dispatchers.IO) {
                conversationDao.getConversations(sessionId)
            }
            if (turns.isNotEmpty()) {
                _conversationHistory.update { current ->
                    current + (sessionId to turns.map { entity ->
                        ConversationTurn(entity.role, entity.content, entity.processingTimeMs)
                    })
                }
            }
        }
    }

    fun execute(request: AiRequest, sessionId: Int = 0) {
        currentJob?.cancel()
        _resultText.value = null
        _errorMessage.value = null
        _isProcessing.value = true
        _streamingText.value = ""
        _processingTimeMs.value = null

        if (sessionId <= 0) {
            viewModelScope.launch {
                _errorMessage.value = "No active session"
                _isProcessing.value = false
            }
            return
        }

        var enrichedRequest = request.copy(customSystemPrompt = _systemPrompt.value)

        val currentHistory = _conversationHistory.value[sessionId] ?: emptyList()
        if (currentHistory.isNotEmpty()) {
            val chatMessages = currentHistory.takeLast(10).map { turn ->
                ChatMessage(turn.role, turn.content)
            }
            enrichedRequest = enrichedRequest.copy(messages = chatMessages)
        }

        val userMessage = when (request.action) {
            AiAction.REWRITE -> "Reescribe en estilo ${request.rewriteStyle}: ${request.selectedText}"
            AiAction.SUMMARIZE -> "Resume: ${request.selectedText.ifBlank { request.context }}"
            AiAction.TRANSLATE -> "Traduce a ${request.targetLanguage}: ${request.selectedText}"
            AiAction.GENERATE -> request.prompt.ifBlank { "Generar texto" }
        }

        val isFirstMessage = currentHistory.isEmpty()
        val isNewSession = _conversationHistory.value[sessionId] == null

        _conversationHistory.update { current ->
            val updated = (current[sessionId]?.toMutableList() ?: mutableListOf()).apply {
                add(ConversationTurn("user", userMessage, status = MessageStatus.SENT))
            }
            current + (sessionId to updated)
        }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                conversationDao.insert(
                    ConversationEntity(sessionId = sessionId, noteId = _currentNoteId.value, role = "user", content = userMessage)
                )
            }
            if (isFirstMessage || isNewSession) {
                val title = userMessage.take(50).ifBlank { "New Chat" }
                _sessionTitle.value = title
                withContext(Dispatchers.IO) {
                    chatSessionDao.updateTitle(sessionId, title)
                    val count = conversationDao.countBySessionId(sessionId)
                    chatSessionDao.updateMetadata(sessionId, System.currentTimeMillis(), count)
                }
            }
        }

        val startTime = System.currentTimeMillis()
        currentJob = viewModelScope.launch {
            try {
                var firstToken = true
                val fullText = StringBuilder()
                currentService.executeStreaming(enrichedRequest).collect { token ->
                    if (firstToken) {
                        _conversationHistory.update { current ->
                            val updated = (current[sessionId]?.toMutableList() ?: mutableListOf()).apply {
                                val idx = indexOfLast { it.role == "user" }
                                if (idx >= 0) set(idx, get(idx).copy(status = MessageStatus.DELIVERED))
                            }
                            current + (sessionId to updated)
                        }
                        firstToken = false
                    }
                    fullText.append(token)
                    _streamingText.value = fullText.toString()
                }
                val elapsed = System.currentTimeMillis() - startTime
                _processingTimeMs.value = elapsed
                val result = fullText.toString()
                _resultText.value = result

                _conversationHistory.update { current ->
                    val updated = (current[sessionId]?.toMutableList() ?: mutableListOf()).apply {
                        add(ConversationTurn("assistant", result, elapsed, status = MessageStatus.COMPLETED))
                    }
                    current + (sessionId to updated)
                }
                viewModelScope.launch {
                    withContext(Dispatchers.IO) {
                        conversationDao.insert(
                            ConversationEntity(sessionId = sessionId, noteId = _currentNoteId.value, role = "assistant", content = result, processingTimeMs = elapsed)
                        )
                        val count = conversationDao.countBySessionId(sessionId)
                        chatSessionDao.updateMetadata(sessionId, System.currentTimeMillis(), count)
                    }
                }
                _streamingText.value = null
                _isProcessing.value = false
            } catch (e: IOException) {
                addErrorTurn(sessionId, e.message ?: "Error de conexión")
            } catch (e: Throwable) {
                addErrorTurn(sessionId, e.message ?: "Error inesperado")
            }
        }
    }

    private fun addErrorTurn(sessionId: Int, errorMsg: String) {
        _isProcessing.value = false
        _streamingText.value = null
        _processingTimeMs.value = null
        _errorMessage.value = errorMsg
        _conversationHistory.update { current ->
            val updated = (current[sessionId]?.toMutableList() ?: mutableListOf()).apply {
                add(ConversationTurn("assistant", "", status = MessageStatus.ERROR, errorMessage = errorMsg))
            }
            current + (sessionId to updated)
        }
    }

    fun cancelGeneration() {
        currentJob?.cancel()
        _isProcessing.value = false
        _streamingText.value = null
        _errorMessage.value = null
    }

    fun clearResult() {
        _resultText.value = null
        _errorMessage.value = null
        _isProcessing.value = false
        _streamingText.value = null
        _processingTimeMs.value = null
    }
}