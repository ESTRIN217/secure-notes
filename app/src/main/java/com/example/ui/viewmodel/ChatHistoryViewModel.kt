package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ChatSessionDao
import com.example.data.ai.AiBackend
import com.example.data.local.ChatSessionEntity
import com.example.data.local.ChatSessionWithPreview
import com.example.data.local.ConversationDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.ui.viewmodel.AiViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class ChatHistoryViewModel(
    application: Application,
    private val sessionDao: ChatSessionDao,
    private val conversationDao: ConversationDao
) : AndroidViewModel(application) {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val sessions: StateFlow<List<ChatSessionWithPreview>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                sessionDao.getAllSessionsWithPreview()
            } else {
                sessionDao.searchSessions(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _latestSessionId = MutableStateFlow<Int?>(null)
    val latestSessionId: StateFlow<Int?> = _latestSessionId.asStateFlow()

    fun createSession(
        noteId: Int? = null,
        noteTitle: String? = null,
        backend: AiBackend,
        modelName: String? = null
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val session = ChatSessionEntity(
                title = if (noteTitle != null) "Chat - $noteTitle" else "Chat ${SimpleDateFormat("dd/MM/yy hh:mm a", Locale.getDefault()).format(Date(now))}",
                noteId = noteId,
                noteTitle = noteTitle,
                backend = if (backend == AiBackend.ON_DEVICE) "ondevice" else "ollama",
                modelName = modelName,
                createdAt = now,
                updatedAt = now,
                messageCount = 0
            )
            val id = withContext(Dispatchers.IO) { sessionDao.insert(session) }
            _latestSessionId.value = id.toInt()
        }
    }

    fun clearLatestSessionId() {
        _latestSessionId.value = null
    }

    fun renameSession(id: Int, title: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { sessionDao.updateTitle(id, title) }
        }
    }

    fun deleteSession(id: Int) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                sessionDao.deleteConversations(id)
                sessionDao.deleteSession(id)
            }
        }
    }

    fun togglePin(id: Int, currentPinned: Boolean) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { sessionDao.togglePin(id, !currentPinned) }
        }
    }

    fun updateMetadata(id: Int) {
        viewModelScope.launch {
            val count = withContext(Dispatchers.IO) { conversationDao.countBySessionId(id) }
            sessionDao.updateMetadata(id, System.currentTimeMillis(), count)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun getSession(id: Int, onResult: (ChatSessionEntity?) -> Unit) {
        viewModelScope.launch {
            val session = withContext(Dispatchers.IO) { sessionDao.getSession(id) }
            onResult(session)
        }
    }
}