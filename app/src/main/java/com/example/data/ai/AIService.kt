package com.example.data.ai

interface AIService {
    suspend fun execute(request: AiRequest): Result<String>

    val isAvailable: Boolean
}
