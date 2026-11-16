package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ConversationDao {

    @Query("SELECT * FROM conversations WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getConversations(sessionId: Int): List<ConversationEntity>

    @Insert
    suspend fun insert(conversation: ConversationEntity): Long

    /** Spec 036 (RF-264): rellena las métricas del prompt en el turno de usuario ya insertado (y la duración para restore). */
    @Query("UPDATE conversations SET promptEvalCount = :promptCount, promptEvalDurationMs = :promptMs, processingTimeMs = :promptMs WHERE id = :turnId")
    suspend fun updatePromptMetrics(turnId: Long, promptCount: Int, promptMs: Long)

    @Query("DELETE FROM conversations WHERE sessionId = :sessionId")
    suspend fun deleteBySessionId(sessionId: Int)

    @Query("SELECT COUNT(*) FROM conversations WHERE sessionId = :sessionId")
    suspend fun countBySessionId(sessionId: Int): Int
}