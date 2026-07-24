package com.example.data.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface AIService {
    suspend fun execute(request: AiRequest): Result<String>

    suspend fun executeStreaming(request: AiRequest): Flow<String> = flow {
        val result = execute(request)
        val text = result.getOrNull() ?: throw (result.exceptionOrNull() ?: Exception("Unknown error"))
        emit(text)
    }

    val isAvailable: Boolean
}
