package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatSessionDao {

    @Query("""
        SELECT cs.*, 
            (SELECT content FROM conversations WHERE sessionId = cs.id ORDER BY timestamp DESC LIMIT 1) AS previewText
        FROM chat_sessions cs
        ORDER BY cs.isPinned DESC, cs.updatedAt DESC
    """)
    fun getAllSessionsWithPreview(): Flow<List<ChatSessionWithPreview>>

    @Query("SELECT * FROM chat_sessions WHERE id = :id")
    suspend fun getSession(id: Int): ChatSessionEntity?

    @Insert
    suspend fun insert(session: ChatSessionEntity): Long

    @Query("UPDATE chat_sessions SET title = :title WHERE id = :id")
    suspend fun updateTitle(id: Int, title: String)

    @Query("UPDATE chat_sessions SET isPinned = :isPinned WHERE id = :id")
    suspend fun togglePin(id: Int, isPinned: Boolean)

    @Query("UPDATE chat_sessions SET updatedAt = :timestamp, messageCount = :count WHERE id = :id")
    suspend fun updateMetadata(id: Int, timestamp: Long, count: Int)

    @Query("DELETE FROM conversations WHERE sessionId = :sessionId")
    suspend fun deleteConversations(sessionId: Int)

    @Query("DELETE FROM chat_sessions WHERE id = :id")
    suspend fun deleteSession(id: Int)

    @Query("""
        SELECT DISTINCT cs.*, 
            (SELECT content FROM conversations WHERE sessionId = cs.id ORDER BY timestamp DESC LIMIT 1) AS previewText
        FROM chat_sessions cs
        LEFT JOIN conversations c ON c.sessionId = cs.id
        WHERE cs.title LIKE '%' || :query || '%' OR c.content LIKE '%' || :query || '%'
        ORDER BY cs.updatedAt DESC
    """)
    fun searchSessions(query: String): Flow<List<ChatSessionWithPreview>>

    @Query("SELECT * FROM chat_sessions WHERE noteId = :noteId ORDER BY updatedAt DESC")
    suspend fun getSessionsForNote(noteId: Int): List<ChatSessionEntity>
}

data class ChatSessionWithPreview(
    val id: Int,
    val title: String,
    val noteId: Int?,
    val noteTitle: String?,
    val backend: String,
    val modelName: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val messageCount: Int,
    val isPinned: Boolean,
    val previewText: String?
)