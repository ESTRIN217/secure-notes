package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Int,
    val type: String,
    val content: String,
    val summary: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
