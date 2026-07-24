package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Int = 0,
    val noteId: Int = 0,
    val role: String,
    val content: String,
    val processingTimeMs: Long? = null,
    val timestamp: Long = System.currentTimeMillis()
)