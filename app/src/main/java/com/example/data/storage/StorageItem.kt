package com.example.data.storage

import com.example.AppConstants

data class StorageItem(
    val path: String,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val category: StorageCategory,
    val isOrphan: Boolean = false
) {
    val isLargeFile: Boolean get() = size >= AppConstants.LARGE_FILE_THRESHOLD_BYTES
}

enum class StorageCategory {
    ATTACHMENT,
    CACHE,
    EXPORT,
    TEMP,
    DRAWING,
    VOICE,
    FILE,
    DATABASE,
    AI_MODEL,
    OTHER
}

data class StorageOverview(
    val attachmentsSize: Long = 0L,
    val cacheSize: Long = 0L,
    val exportsSize: Long = 0L,
    val tempSize: Long = 0L,
    val databaseSize: Long = 0L,
    val drawingsSize: Long = 0L,
    val voiceSize: Long = 0L,
    val filesSize: Long = 0L,
    val aiModelSize: Long = 0L,
    val otherSize: Long = 0L,
    val orphanSize: Long = 0L,
    val totalUsed: Long = 0L,
    val freeSpace: Long = 0L,
    val totalSpace: Long = 0L,
    val orphanCount: Int = 0
) {
    val totalUncategorized: Long
        get() = attachmentsSize + cacheSize + exportsSize + tempSize +
                databaseSize + drawingsSize + voiceSize + filesSize + aiModelSize + otherSize
}
