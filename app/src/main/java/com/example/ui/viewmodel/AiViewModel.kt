package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PreferencesRepository
import com.example.data.ai.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    private val _resultText = MutableStateFlow<String?>(null)
    val resultText: StateFlow<String?> = _resultText.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Unknown)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    val onDeviceModelState: StateFlow<ModelState> = onDeviceService.modelState
    val onDeviceLoadedModelInfo: StateFlow<LoadedModelInfo?> = onDeviceService.loadedModelInfo

    val deviceInfo: DeviceInfo = DeviceInfo.detect(getApplication())

    val recommendedModels: List<OnDeviceModel> = MODEL_CATALOG.filterForDevice(deviceInfo)

    val bestModel: OnDeviceModel? = MODEL_CATALOG.bestForDevice(deviceInfo)

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

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

    fun setAiEnabled(enabled: Boolean) {
        _aiEnabled.value = enabled
        prefsRepository.setAiEnabled(enabled)
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
            _errorMessage.value = "Model not downloaded yet"
            return
        }
        viewModelScope.launch {
            val result = onDeviceService.loadModel(path, model)
            if (result.isSuccess) {
                setOnDeviceModelPath(path)
                _errorMessage.value = null
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to load model"
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
                    ConnectionState.Failed(error.message ?: "Unknown error")
                }
            )
        }
    }

    fun execute(request: AiRequest) {
        _resultText.value = null
        _errorMessage.value = null
        _isProcessing.value = true

        val requestWithPrompt = request.copy(customSystemPrompt = _systemPrompt.value)
        viewModelScope.launch {
            val result = currentService.execute(requestWithPrompt)
            _isProcessing.value = false
            result.fold(
                onSuccess = { text ->
                    _resultText.value = text
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Unknown error"
                }
            )
        }
    }

    fun clearResult() {
        _resultText.value = null
        _errorMessage.value = null
    }
}
