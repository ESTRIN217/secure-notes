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
import com.example.data.local.NoteDao
import com.example.data.model.DecryptedNote
import com.example.data.model.Note
import com.example.data.security.CipherService
import com.example.data.security.EncryptionServiceImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
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
    val modelName: String? = null,
    val status: MessageStatus = MessageStatus.COMPLETED,
    val timestamp: Long = System.currentTimeMillis(),
    val errorMessage: String? = null,
    val id: Long = idCounter++,
    val files: List<FileAttachment> = emptyList()
) {
    val formattedTime: String
        get() = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))

    val formattedDuration: String?
        get() = processingTimeMs?.let { "%.1fs".format(it / 1000.0) }

    companion object {
        private var idCounter = 0L

        fun filesToJson(files: List<FileAttachment>): String {
            return JSONArray(files.map { f ->
                JSONObject().apply {
                    put("name", f.name)
                    put("content", f.content)
                    put("source", f.source)
                }
            }).toString()
        }

        fun jsonToFiles(json: String?): List<FileAttachment> {
            if (json.isNullOrBlank()) return emptyList()
            return try {
                val arr = JSONArray(json)
                (0 until arr.length()).map { i ->
                    val obj = arr.getJSONObject(i)
                    FileAttachment(
                        name = obj.optString("name", "file"),
                        content = obj.optString("content", ""),
                        source = obj.optString("source", "external")
                    )
                }
            } catch (_: Exception) { emptyList() }
        }
    }
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
    private val chatSessionDao: ChatSessionDao,
    private val noteDao: NoteDao,
    private val cipherService: CipherService = EncryptionServiceImpl(),
    private val memoryDao: com.example.data.local.MemoryDao? = null
) : AndroidViewModel(application) {

    private val memoryManager = memoryDao?.let { MemoryManager(it) }

    private val toolRegistry = ToolRegistry().apply {
        register(
            com.example.data.ai.tools.SearchNotesTool.spec,
            { args ->
                val query = args["query"]?.toString() ?: return@register ""
                val maxResults = (args["max_results"] as? Number)?.toInt() ?: 5
                val allNotes = kotlinx.coroutines.runBlocking {
                    withContext(Dispatchers.IO) { noteDao.getAllNotes() }
                }
                val matching = allNotes.filter { !it.isDeleted && (it.title.contains(query, ignoreCase = true) || com.example.util.RichTextConverter.contentToPlainText(it.content).contains(query, ignoreCase = true)) }
                matching.take(maxResults).joinToString("\n") { note ->
                    val content = if (note.isEncrypted) "[Encrypted]" else com.example.util.RichTextConverter.contentToPlainText(note.content).take(200)
                    "[${note.id}] ${note.title}: $content"
                }.ifEmpty { "No notes found for: $query" }
            }
        )
        register(
            com.example.data.ai.tools.GetNoteTool.spec,
            { args ->
                val noteId = (args["note_id"] as? Number)?.toInt() ?: return@register "Invalid note_id"
                val note = kotlinx.coroutines.runBlocking {
                    withContext(Dispatchers.IO) { noteDao.getNoteById(noteId) }
                }
                if (note == null) return@register "Note #$noteId not found"
                if (note.isEncrypted) {
                    val password = _masterPassword.value ?: ""
                    if (password.isEmpty()) return@register "Note #$noteId is encrypted. Unlock to read."
                    val decTitle = cipherService.decrypt(note.title, password, note.salt, note.iv).getOrDefault("")
                    val decContent = com.example.util.RichTextConverter.contentToMarkdown(
                        cipherService.decrypt(note.content, password, note.salt, note.iv).getOrDefault("")
                    )
                    "Title: $decTitle\n\n$decContent"
                } else {
                    "Title: ${note.title}\n\n${com.example.util.RichTextConverter.contentToMarkdown(note.content)}"
                }
            }
        )
        register(
            com.example.data.ai.tools.CreateNoteTool.spec,
            { args ->
                val title = args["title"]?.toString() ?: "Untitled"
                val content = args["content"]?.toString() ?: ""
                kotlinx.coroutines.runBlocking {
                    withContext(Dispatchers.IO) {
                        noteDao.insertNote(com.example.data.model.Note(title = title, content = content))
                    }
                }
                "Note '$title' created successfully."
            }
        )
    }

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

    private val _temperature = MutableStateFlow(prefsRepository.getAiTemperature())
    val temperature: StateFlow<Float> = _temperature.asStateFlow()
    private val _topK = MutableStateFlow(prefsRepository.getAiTopK())
    val topK: StateFlow<Int> = _topK.asStateFlow()
    private val _topP = MutableStateFlow(prefsRepository.getAiTopP())
    val topP: StateFlow<Float> = _topP.asStateFlow()
    private val _repetitionPenalty = MutableStateFlow(prefsRepository.getAiRepetitionPenalty())
    val repetitionPenalty: StateFlow<Float> = _repetitionPenalty.asStateFlow()
    private val _maxTokens = MutableStateFlow(prefsRepository.getAiMaxTokens())
    val maxTokens: StateFlow<Int> = _maxTokens.asStateFlow()

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

    private val _pendingInsert = MutableStateFlow<String?>(null)
    val pendingInsert: StateFlow<String?> = _pendingInsert.asStateFlow()

    private val _pendingAttachments = MutableStateFlow<List<FileAttachment>>(emptyList())
    val pendingAttachments: StateFlow<List<FileAttachment>> = _pendingAttachments.asStateFlow()

    private val _inPlaceStreamingText = MutableStateFlow<String?>(null)
    val inPlaceStreamingText: StateFlow<String?> = _inPlaceStreamingText.asStateFlow()

    private val _inPlaceResult = MutableStateFlow<String?>(null)
    val inPlaceResult: StateFlow<String?> = _inPlaceResult.asStateFlow()

    private val _inPlaceProcessing = MutableStateFlow(false)
    val inPlaceProcessing: StateFlow<Boolean> = _inPlaceProcessing.asStateFlow()

    private val _inPlaceAction = MutableStateFlow<AiAction?>(null)
    val inPlaceAction: StateFlow<AiAction?> = _inPlaceAction.asStateFlow()

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

    private val _activeMemories = MutableStateFlow<List<String>>(emptyList())
    val activeMemories: StateFlow<List<String>> = _activeMemories.asStateFlow()

    private val _currentSessionId = MutableStateFlow(0)
    val currentSessionId: StateFlow<Int> = _currentSessionId.asStateFlow()

    private val _sessionTitle = MutableStateFlow("New Chat")
    val sessionTitle: StateFlow<String> = _sessionTitle.asStateFlow()

    private val _currentNoteId = MutableStateFlow(0)
    val currentNoteId: StateFlow<Int> = _currentNoteId.asStateFlow()

    private val _masterPassword = MutableStateFlow<String?>(null)
    val masterPassword: StateFlow<String?> = _masterPassword.asStateFlow()

    private val _availableNotes = MutableStateFlow<List<DecryptedNote>>(emptyList())
    val availableNotes: StateFlow<List<DecryptedNote>> = _availableNotes.asStateFlow()

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

    private fun currentModelName(): String = when (_backend.value) {
        AiBackend.OLLAMA -> _modelName.value
        AiBackend.ON_DEVICE -> _selectedOnDeviceModel.value?.displayName ?: "On-Device"
    }

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

    fun requestInsert(text: String) {
        _pendingInsert.value = text
    }

    fun clearInsertResult() {
        _pendingInsert.value = null
    }

    fun executeInPlace(
        action: AiAction,
        text: String,
        rewriteStyle: RewriteStyle = RewriteStyle.FORMAL,
        targetLanguage: String = "en"
    ) {
        currentJob?.cancel()
        _inPlaceResult.value = null
        _inPlaceStreamingText.value = ""
        _inPlaceProcessing.value = true
        _inPlaceAction.value = action

        val resolvedPrompt = _systemPrompt.value.ifBlank {
            AiPromptBuilder.resolveSystemPromptResource(getApplication(), action, "")
        }
        val request = AiRequest(
            action = action,
            selectedText = text,
            context = text,
            rewriteStyle = rewriteStyle,
            targetLanguage = targetLanguage,
            customSystemPrompt = resolvedPrompt,
            temperature = _temperature.value,
            topK = _topK.value,
            topP = _topP.value,
            repetitionPenalty = _repetitionPenalty.value,
            maxTokens = _maxTokens.value.coerceAtMost(512)
        )

        currentJob = viewModelScope.launch {
            try {
                val fullText = StringBuilder()
                currentService.executeStreaming(request).collect { token ->
                    fullText.append(token)
                    _inPlaceStreamingText.value = fullText.toString()
                }
                _inPlaceResult.value = fullText.toString()
            } catch (e: Exception) {
                _inPlaceResult.value = null
                _errorMessage.value = e.message ?: getApplication<android.app.Application>().getString(com.example.R.string.ai_error_inplace)
            } finally {
                _inPlaceStreamingText.value = null
                _inPlaceProcessing.value = false
            }
        }
    }

    fun clearInPlaceResult() {
        _inPlaceResult.value = null
        _inPlaceStreamingText.value = null
        _inPlaceProcessing.value = false
        _inPlaceAction.value = null
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

    fun setTemperature(value: Float) {
        _temperature.value = value
        prefsRepository.setAiTemperature(value)
    }

    fun setTopK(value: Int) {
        _topK.value = value
        prefsRepository.setAiTopK(value)
    }

    fun setTopP(value: Float) {
        _topP.value = value
        prefsRepository.setAiTopP(value)
    }

    fun setRepetitionPenalty(value: Float) {
        _repetitionPenalty.value = value
        prefsRepository.setAiRepetitionPenalty(value)
    }

    fun setMaxTokens(value: Int) {
        _maxTokens.value = value
        prefsRepository.setAiMaxTokens(value)
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
            _errorMessage.value = getApplication<android.app.Application>().getString(com.example.R.string.ai_ondevice_not_downloaded)
            return
        }
        viewModelScope.launch {
            val result = onDeviceService.loadModel(path, model)
            if (result.isSuccess) {
                setOnDeviceModelPath(path)
                _errorMessage.value = null
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: getApplication<android.app.Application>().getString(com.example.R.string.ai_error_load_model)
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
                onFailure = { error -> ConnectionState.Failed(error.message ?: getApplication<android.app.Application>().getString(com.example.R.string.ai_error_unknown)) }
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
                    session.backend.let { backendStr ->
                        val savedBackend = if (backendStr == "ondevice") AiBackend.ON_DEVICE else AiBackend.OLLAMA
                        if (savedBackend != _backend.value) {
                            _backend.value = savedBackend
                        }
                    }
                session.modelName?.let { name ->
                    val savedBackendEnum = if (session.backend == "ondevice") AiBackend.ON_DEVICE else AiBackend.OLLAMA
                    if (savedBackendEnum == AiBackend.ON_DEVICE) {
                        val matching = MODEL_CATALOG.firstOrNull { it.displayName == name || it.id == name }
                        if (matching != null) _selectedOnDeviceModel.value = matching
                    } else {
                        if (name != _modelName.value) {
                            _modelName.value = name
                            ollamaService.updateConfig(_endpointUrl.value, name)
                        }
                    }
                }
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
                modelName = currentModelName(),
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

    fun addPendingAttachment(attachment: FileAttachment) {
        _pendingAttachments.update { it + attachment }
    }

    fun removePendingAttachment(index: Int) {
        _pendingAttachments.update { it.toMutableList().apply { removeAt(index) } }
    }

    fun clearPendingAttachments() {
        _pendingAttachments.value = emptyList()
    }

    fun attachNoteAsAttachment(noteId: Int, title: String, content: String) {
        val source = "note:$noteId"
        _pendingAttachments.update { list ->
            list.filterNot { it.source == source } + FileAttachment(
                name = title.ifBlank { "note" },
                content = content,
                source = source
            )
        }
    }

    fun setMasterPassword(password: String?) {
        _masterPassword.value = password
    }

    fun loadAvailableNotes(searchQuery: String = "") {
        viewModelScope.launch {
            val notes = withContext(Dispatchers.IO) { noteDao.getAllNotes() }
            val password = _masterPassword.value
            val decrypted = notes
                .filter { !it.isDeleted }
                .map { decryptNoteForAttach(it, password) }
                .filter { searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) }
            _availableNotes.value = decrypted
        }
    }

    fun attachNoteById(noteId: Int) {
        viewModelScope.launch {
            val note = withContext(Dispatchers.IO) { noteDao.getNoteById(noteId) } ?: return@launch
            val password = _masterPassword.value
            val decrypted = decryptNoteForAttach(note, password)
            if (!decrypted.isDecryptionSuccessful) return@launch
            attachNoteAsAttachment(noteId, decrypted.title, decrypted.content)
        }
    }

    private fun decryptNoteForAttach(note: Note, password: String?): DecryptedNote {
        if (!note.isEncrypted) return DecryptedNote(note, note.title, note.content, true)
        val pass = password ?: ""
        if (pass.isEmpty()) return DecryptedNote(note, "[Encrypted]", "[Unlock to read notes]", false)
        val decTitle = cipherService.decrypt(note.title, pass, note.salt, note.iv).getOrDefault("")
        val decContent = cipherService.decrypt(note.content, pass, note.salt, note.iv).getOrDefault("")
        return if (decTitle.isEmpty() && decContent.isEmpty()) {
            DecryptedNote(note, "[Corrupted / Wrong Password]", "[Cannot decrypt]", false)
        } else {
            DecryptedNote(note, decTitle, decContent, true)
        }
    }

    fun loadConversation(sessionId: Int) {
        if (_conversationHistory.value.containsKey(sessionId)) return
        viewModelScope.launch {
            val turns = withContext(Dispatchers.IO) {
                conversationDao.getConversations(sessionId)
            }
            if (turns.isNotEmpty()) {
                _conversationHistory.update { current ->
                    current + (sessionId to turns.map { entity ->
                        ConversationTurn(
                            entity.role, entity.content, entity.processingTimeMs,
                            modelName = entity.modelName, timestamp = entity.timestamp,
                            id = entity.id.toLong(),
                            files = ConversationTurn.jsonToFiles(entity.attachmentsJson)
                        )
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
                _errorMessage.value = getApplication<android.app.Application>().getString(com.example.R.string.ai_error_no_session)
                _isProcessing.value = false
            }
            return
        }

        val resolvedPrompt = _systemPrompt.value.ifBlank {
            AiPromptBuilder.resolveSystemPromptResource(getApplication(), request.action, "")
        }
        var enrichedRequest = request.copy(
            customSystemPrompt = resolvedPrompt,
            temperature = _temperature.value,
            topK = _topK.value,
            topP = _topP.value,
            repetitionPenalty = _repetitionPenalty.value,
            maxTokens = _maxTokens.value
        )

        val currentHistory = _conversationHistory.value[sessionId] ?: emptyList()
        if (currentHistory.isNotEmpty()) {
            val chatMessages = currentHistory.takeLast(20).map { turn ->
                ChatMessage(turn.role, turn.content)
            }
            enrichedRequest = enrichedRequest.copy(messages = chatMessages)
        }

        val app = getApplication<android.app.Application>()
        val pendingFiles = _pendingAttachments.value
        val attachmentsContext = if (pendingFiles.isNotEmpty()) {
            pendingFiles.joinToString("\n\n---\n") { f ->
                if (f.source.startsWith("note")) "[Attached note: ${f.name}]\n${f.content}"
                else "[Attached file: ${f.name}]\n${f.content}"
            } + "\n\n---\n"
        } else ""

        val userPrompt = when (request.action) {
            AiAction.REWRITE -> app.getString(com.example.R.string.ai_user_msg_rewrite, request.rewriteStyle.name.lowercase(), request.selectedText)
            AiAction.SUMMARIZE -> app.getString(com.example.R.string.ai_user_msg_summarize, request.selectedText.ifBlank { request.context })
            AiAction.TRANSLATE -> app.getString(com.example.R.string.ai_user_msg_translate, request.targetLanguage, request.selectedText)
            AiAction.GENERATE -> request.prompt.ifBlank { app.getString(com.example.R.string.ai_user_msg_generate) }
            AiAction.MAKE_SHORTER -> app.getString(com.example.R.string.ai_user_msg_make_shorter, request.selectedText.ifBlank { request.context })
            AiAction.FIX_GRAMMAR -> app.getString(com.example.R.string.ai_user_msg_fix_grammar, request.selectedText.ifBlank { request.context })
            AiAction.EXPLAIN -> app.getString(com.example.R.string.ai_user_msg_explain, request.selectedText.ifBlank { request.context })
        }
        val userMessage = if (attachmentsContext.isNotBlank()) {
            "$attachmentsContext$userPrompt"
        } else userPrompt
        val filesJson = ConversationTurn.filesToJson(pendingFiles)

        val attachmentsContextForAi = if (pendingFiles.isNotEmpty()) {
            pendingFiles.joinToString("\n\n") { f -> "--- ${f.name} ---\n${f.content}" } + "\n\n"
        } else ""
        val memories = if (memoryManager != null && sessionId > 0) {
            val memTexts = kotlinx.coroutines.runBlocking {
                memoryManager.getRelevantMemories(sessionId)
            }
            _activeMemories.value = memTexts
            if (memTexts.isEmpty()) "" else "Relevant memories from previous conversations:\n" + memTexts.joinToString("\n") + "\n\n"
        } else ""
        val toolSpecs = if (toolRegistry.isNotEmpty() && _backend.value == AiBackend.OLLAMA) {
            toolRegistry.getSpecsForApi()
        } else emptyList<Map<String, Any>>()

        enrichedRequest = enrichedRequest.copy(
            attachments = pendingFiles,
            tools = toolSpecs,
            context = if (memories.isNotBlank()) "$memories$attachmentsContextForAi${request.context}"
                      else if (attachmentsContextForAi.isNotBlank()) "$attachmentsContextForAi${request.context}"
                      else request.context
        )

        val isFirstMessage = currentHistory.isEmpty()
        val isNewSession = _conversationHistory.value[sessionId] == null

        _conversationHistory.update { current ->
            val updated = (current[sessionId]?.toMutableList() ?: mutableListOf()).apply {
                add(ConversationTurn("user", userMessage, status = MessageStatus.SENT, modelName = currentModelName(), files = pendingFiles))
            }
            current + (sessionId to updated)
        }

        _pendingAttachments.value = emptyList()

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                conversationDao.insert(
                    ConversationEntity(sessionId = sessionId, noteId = _currentNoteId.value, role = "user", content = userMessage, modelName = currentModelName(), attachmentsJson = filesJson.ifBlank { null })
                )
            }
            if (isFirstMessage || isNewSession) {
                val title = userMessage.take(50).ifBlank { getApplication<android.app.Application>().getString(com.example.R.string.ai_session_title_new) }
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
                var finalResult = fullText.toString()
                val toolCallPrefix = "TOOL_CALLS:"
                if (finalResult.startsWith(toolCallPrefix) && toolRegistry.isNotEmpty()) {
                    val toolCallData = finalResult.removePrefix(toolCallPrefix)
                    val toolResults = toolCallData.split("|||").mapNotNull { tcStr ->
                        val parts = tcStr.split(":::", limit = 3)
                        if (parts.size < 3) return@mapNotNull null
                        val (tcId, name, argsStr) = parts
                        val args = try {
                            org.json.JSONObject(argsStr).keys().asSequence().associateWith { key ->
                                org.json.JSONObject(argsStr).get(key) as Any
                            }
                        } catch (_: Exception) { emptyMap<String, Any>() }
                        val result = toolRegistry.execute(name, args)
                        ToolResult(tcId, name, result)
                    }
                    if (toolResults.isNotEmpty()) {
                        val toolRequest = enrichedRequest.copy(
                            messages = enrichedRequest.messages + listOf(
                                ChatMessage("assistant", finalResult)
                            ) + toolResults.map { ChatMessage("tool", it.result) },
                            toolResults = toolResults,
                            tools = emptyList()
                        )
                        val secondResult = withContext(Dispatchers.IO) {
                            currentService.execute(toolRequest).getOrDefault("")
                        }
                        if (secondResult.isNotBlank()) {
                            finalResult = secondResult
                        }
                    }
                }

                val elapsed = System.currentTimeMillis() - startTime
                _processingTimeMs.value = elapsed
                _resultText.value = finalResult

                _conversationHistory.update { current ->
                    val updated = (current[sessionId]?.toMutableList() ?: mutableListOf()).apply {
                        add(ConversationTurn("assistant", finalResult, elapsed, modelName = currentModelName(), status = MessageStatus.COMPLETED))
                    }
                    current + (sessionId to updated)
                }
                viewModelScope.launch {
                    withContext(Dispatchers.IO) {
                        conversationDao.insert(
                            ConversationEntity(sessionId = sessionId, noteId = _currentNoteId.value, role = "assistant", content = finalResult, processingTimeMs = elapsed, modelName = currentModelName())
                        )
                        val count = conversationDao.countBySessionId(sessionId)
                        chatSessionDao.updateMetadata(sessionId, System.currentTimeMillis(), count)
                    }
                }
                viewModelScope.launch {
                    if (memoryManager != null && memoryManager.shouldSummarize(sessionId)) {
                        val turns = _conversationHistory.value[sessionId] ?: emptyList()
                        val summaryPrompt = memoryManager.buildMemoryPrompt(turns)
                        if (summaryPrompt != null) {
                            try {
                                val summaryRequest = AiRequest(
                                    action = AiAction.SUMMARIZE,
                                    prompt = summaryPrompt,
                                    context = summaryPrompt,
                                    maxTokens = 256
                                )
                                val summary = withContext(Dispatchers.IO) {
                                    currentService.execute(summaryRequest).getOrDefault("")
                                }
                                if (summary.isNotBlank()) {
                                    memoryManager.saveSummary(sessionId, summary)
                                }
                            } catch (_: Exception) {}
                        }
                    }
                }
                _streamingText.value = null
                _isProcessing.value = false
            } catch (e: IOException) {
                addErrorTurn(sessionId, e.message ?: getApplication<android.app.Application>().getString(com.example.R.string.ai_error_connection))
            } catch (e: Throwable) {
                addErrorTurn(sessionId, e.message ?: getApplication<android.app.Application>().getString(com.example.R.string.ai_error_unexpected))
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
                add(ConversationTurn("assistant", "", status = MessageStatus.ERROR, errorMessage = errorMsg, modelName = currentModelName()))
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

    fun savePinnedMemory(sessionId: Int, content: String) {
        if (memoryManager == null) return
        viewModelScope.launch {
            memoryManager.savePinnedMemory(sessionId, content)
        }
    }

    fun clearSessionMemories(sessionId: Int) {
        if (memoryManager == null) return
        _activeMemories.value = emptyList()
        viewModelScope.launch {
            memoryManager.clearSessionMemories(sessionId)
        }
    }

    fun clearResult() {
        _resultText.value = null
        _errorMessage.value = null
        _isProcessing.value = false
        _streamingText.value = null
        _processingTimeMs.value = null
    }

    fun exportConversation(context: android.content.Context, sessionId: Int): String? {
        val turns = _conversationHistory.value[sessionId] ?: return null
        if (turns.isEmpty()) return null
        val sb = StringBuilder()
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        sb.appendLine("=== ${_sessionTitle.value} ===")
        sb.appendLine("Exported: ${dateFormat.format(java.util.Date())}")
        sb.appendLine()
        for (turn in turns) {
            val time = turn.formattedTime
            val role = if (turn.role == "user") "You" else "AI"
            val model = turn.modelName?.let { " ($it)" } ?: ""
            val files = if (turn.files.isNotEmpty()) {
                " [" + turn.files.joinToString(", ") { it.name } + "]"
            } else ""
            sb.appendLine("[$role$model$files · $time]")
            sb.appendLine(turn.content)
            sb.appendLine()
        }
        try {
            val fileName = "chat_${_sessionTitle.value.take(30).replace(" ", "_")}_${System.currentTimeMillis()}.txt"
            val file = java.io.File(context.getExternalFilesDir(null), fileName)
            file.parentFile?.mkdirs()
            file.writeText(sb.toString(), java.nio.charset.StandardCharsets.UTF_8)
            return file.absolutePath
        } catch (e: Exception) {
            Log.e("AiViewModel", "Export failed", e)
            return null
        }
    }
}