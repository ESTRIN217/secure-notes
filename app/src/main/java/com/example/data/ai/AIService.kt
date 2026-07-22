package com.example.data.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface AIService {
    suspend fun execute(request: AiRequest): Result<String>

    suspend fun executeStreaming(request: AiRequest): Flow<String> = flow {
        val result = execute(request)
        result.getOrThrow().let { emit(it) }
    }

    val isAvailable: Boolean
}
