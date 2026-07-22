package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PreferencesRepository
import com.example.data.ai.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

data class ConversationTurn(
    val role: String,
    val content: String,
    val processingTimeMs: Long? = null
)

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
    private val modelDownloader: ModelDownloader
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

    fun getConversationHistory(noteId: Int): List<ConversationTurn> {
        return _conversationHistory.value[noteId] ?: emptyList()
    }

    fun clearConversationHistory(noteId: Int) {
        _conversationHistory.update { current ->
            current.toMutableMap().apply { remove(noteId) }.toMap()
        }
    }

    fun setAiEnabled(enabled: Boolean) {
        _aiEnabled.value = enabled
        prefsRepository.setAiEnabled(enabled)
    }

    fun prepareChatForNote(context: String, selected: String) {
        _chatNoteContext.value = context
        _chatSelectedText.value = selected
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
                onSuccess = { models ->
                    ConnectionState.Connected(models)
                },
                onFailure = { error ->
                    ConnectionState.Failed(error.message ?: "Error desconocido")
                }
            )
        }
    }

    fun execute(request: AiRequest, noteId: Int = 0) {
        currentJob?.cancel()
        _resultText.value = null
        _errorMessage.value = null
        _isProcessing.value = true
        _streamingText.value = ""
        _processingTimeMs.value = null

        var enrichedRequest = request.copy(customSystemPrompt = _systemPrompt.value)

        if (noteId > 0) {
            val currentHistory = _conversationHistory.value[noteId] ?: emptyList()
            if (currentHistory.isNotEmpty()) {
                val chatMessages = currentHistory.takeLast(10).map { turn ->
                    ChatMessage(turn.role, turn.content)
                }
                enrichedRequest = enrichedRequest.copy(
                    messages = chatMessages
                )
            }
            val userMessage = when (request.action) {
                AiAction.REWRITE -> "Reescribe en estilo ${request.rewriteStyle}: ${request.selectedText}"
                AiAction.SUMMARIZE -> "Resume: ${request.selectedText.ifBlank { request.context.take(200) }}"
                AiAction.TRANSLATE -> "Traduce a ${request.targetLanguage}: ${request.selectedText}"
                AiAction.GENERATE -> request.prompt.ifBlank { "Generar texto" }
            }
            _conversationHistory.update { current ->
                val updated = (current[noteId]?.toMutableList() ?: mutableListOf()).apply {
                    add(ConversationTurn("user", userMessage))
                }
                current + (noteId to updated)
            }
        }

        val startTime = System.currentTimeMillis()
        currentJob = viewModelScope.launch {
            try {
                val fullText = StringBuilder()
                currentService.executeStreaming(enrichedRequest).collect { token ->
                    fullText.append(token)
                    _streamingText.value = fullText.toString()
                }
                val elapsed = System.currentTimeMillis() - startTime
                _processingTimeMs.value = elapsed
                val result = fullText.toString()
                _resultText.value = result
                if (noteId > 0) {
                    _conversationHistory.update { current ->
                        val updated = (current[noteId]?.toMutableList() ?: mutableListOf()).apply {
                            add(ConversationTurn("assistant", result, elapsed))
                        }
                        current + (noteId to updated)
                    }
                }
                _streamingText.value = null
                _isProcessing.value = false
            } catch (e: IOException) {
                _isProcessing.value = false
                _streamingText.value = null
                _processingTimeMs.value = null
                _errorMessage.value = e.message ?: "Error de conexión"
            } catch (e: Throwable) {
                _isProcessing.value = false
                _streamingText.value = null
                _processingTimeMs.value = null
                _errorMessage.value = e.message ?: "Error inesperado"
            }
        }
    }

    fun clearResult() {
        _resultText.value = null
        _errorMessage.value = null
        _isProcessing.value = false
        _streamingText.value = null
        _processingTimeMs.value = null
    }
}
