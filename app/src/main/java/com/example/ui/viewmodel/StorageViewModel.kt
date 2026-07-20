package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AppConstants
import com.example.data.local.NoteDatabase
import com.example.data.storage.StorageAnalyzer
import com.example.data.storage.StorageItem
import com.example.data.storage.StorageOverview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StorageViewModel(
    application: Application,
    private val database: NoteDatabase
) : AndroidViewModel(application) {

    private val _overview = MutableStateFlow(StorageOverview())
    val overview: StateFlow<StorageOverview> = _overview.asStateFlow()

    private val _allFiles = MutableStateFlow<List<StorageItem>>(emptyList())
    val allFiles: StateFlow<List<StorageItem>> = _allFiles.asStateFlow()

    private val _orphanFiles = MutableStateFlow<List<StorageItem>>(emptyList())
    val orphanFiles: StateFlow<List<StorageItem>> = _orphanFiles.asStateFlow()

    private val _largeFiles = MutableStateFlow<List<StorageItem>>(emptyList())
    val largeFiles: StateFlow<List<StorageItem>> = _largeFiles.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _autoCleanupEnabled = MutableStateFlow(
        application.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(AppConstants.AUTO_CLEANUP_ENABLED_KEY, false)
    )
    val autoCleanupEnabled: StateFlow<Boolean> = _autoCleanupEnabled.asStateFlow()

    private val _lastCleanupMessage = MutableStateFlow<String?>(null)
    val lastCleanupMessage: StateFlow<String?> = _lastCleanupMessage.asStateFlow()

    fun scanStorage() {
        if (_isScanning.value) return
        _isScanning.value = true
        viewModelScope.launch {
            val (overview, items) = withContext(Dispatchers.IO) {
                StorageAnalyzer.scan(getApplication(), database)
            }
            _overview.value = overview
            _allFiles.value = items
            _orphanFiles.value = items.filter { it.isOrphan }
            _largeFiles.value = items.filter { it.isLargeFile }.sortedByDescending { it.size }
            _isScanning.value = false
        }
    }

    fun deleteOrphans() {
        viewModelScope.launch {
            val orphans = _orphanFiles.value.toList()
            if (orphans.isEmpty()) return@launch
            val deleted = withContext(Dispatchers.IO) {
                StorageAnalyzer.deleteFiles(orphans)
            }
            _lastCleanupMessage.value = "Deleted $deleted orphan file(s)"
            scanStorage()
        }
    }

    fun deleteFiles(files: List<StorageItem>) {
        viewModelScope.launch {
            val deleted = withContext(Dispatchers.IO) {
                StorageAnalyzer.deleteFiles(files)
            }
            _lastCleanupMessage.value = "Deleted $deleted file(s)"
            scanStorage()
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            val freed = withContext(Dispatchers.IO) {
                StorageAnalyzer.clearCache(getApplication())
            }
            _lastCleanupMessage.value = "Freed ${StorageAnalyzer.formatSize(freed)}"
            scanStorage()
        }
    }

    fun setAutoCleanup(enabled: Boolean) {
        _autoCleanupEnabled.value = enabled
        getApplication<Application>()
            .getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(AppConstants.AUTO_CLEANUP_ENABLED_KEY, enabled)
            .apply()
    }

    fun runAutoCleanupNow() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val cutoff = System.currentTimeMillis() - java.util.concurrent.TimeUnit.DAYS.toMillis(AppConstants.TRASH_RETENTION_DAYS)
                database.noteDao.deleteOldTrashedNotes(cutoff)
            }
            clearCache()
            deleteOrphans()
        }
    }

    fun dismissCleanupMessage() {
        _lastCleanupMessage.value = null
    }
}
