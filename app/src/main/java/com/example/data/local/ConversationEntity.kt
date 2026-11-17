package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Una fila del histórico. Las columnas `*Eval*` las añade la spec 036 (migración 9→10):
 * turno de usuario → métricas del **prompt**; turno de asistente → métricas de **generación**.
 * Todas anulables: filas anteriores a la spec 036 no tienen métricas y la burbuja no las pinta.
 * `promptCachedTokens` la añade la spec 041 (migración 10→11): posición de entrada en la KV-cache.
 */
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Int = 0,
    val noteId: Int = 0,
    val role: String,
    val content: String,
    val processingTimeMs: Long? = null,
    val modelName: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val attachmentsJson: String? = null,
    val promptEvalCount: Int? = null,
    val promptEvalDurationMs: Long? = null,
    val evalCount: Int? = null,
    val evalDurationMs: Long? = null,
    /** Spec 041: tokens residentes en el KV cache al entrar el turno; null = sin dato. */
    val promptCachedTokens: Int? = null
)
