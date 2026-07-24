package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ConversationDao {

    @Query("SELECT * FROM conversations WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getConversations(sessionId: Int): List<ConversationEntity>

    @Query("SELECT * FROM conversations WHERE noteId = :noteId ORDER BY timestamp ASC")
    suspend fun getConversationsByNote(noteId: Int): List<ConversationEntity>

    @Insert
    suspend fun insert(conversation: ConversationEntity)

    @Query("DELETE FROM conversations WHERE sessionId = :sessionId")
    suspend fun deleteBySessionId(sessionId: Int)

    @Query("DELETE FROM conversations WHERE noteId = :noteId")
    suspend fun deleteByNoteId(noteId: Int)

    @Query("SELECT COUNT(*) FROM conversations WHERE sessionId = :sessionId")
    suspend fun countBySessionId(sessionId: Int): Int

    @Query("SELECT content FROM conversations WHERE sessionId = :sessionId AND role = 'user' ORDER BY timestamp ASC LIMIT 1")
    suspend fun getFirstUserMessage(sessionId: Int): String?
}