package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MemoryDao {

    @Query("SELECT * FROM memories WHERE sessionId = :sessionId ORDER BY createdAt DESC")
    suspend fun getMemories(sessionId: Int): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE type = :type ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getMemoriesByType(type: String, limit: Int = 10): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE sessionId = :sessionId AND type = :type ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getMemoriesBySessionAndType(sessionId: Int, type: String, limit: Int = 5): List<MemoryEntity>

    @Insert
    suspend fun insert(memory: MemoryEntity)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM memories WHERE sessionId = :sessionId")
    suspend fun deleteBySessionId(sessionId: Int)

    @Query("SELECT COUNT(*) FROM memories WHERE sessionId = :sessionId")
    suspend fun countBySessionId(sessionId: Int): Int
}
