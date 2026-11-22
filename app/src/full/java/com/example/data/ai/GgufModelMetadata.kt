package com.example.data.ai

import com.arm.aichat.gguf.GgufMetadataReader
import java.io.File
import java.io.FileInputStream

object GgufModelMetadata {
    suspend fun readContextLength(path: String): Long? = runCatching {
        FileInputStream(File(path)).use { stream ->
            GgufMetadataReader.create().readStructuredMetadata(stream).dimensions?.contextLength?.toLong()
        }
    }.getOrNull()

    suspend fun readChatTemplate(path: String): String? = runCatching {
        FileInputStream(File(path)).use { stream ->
            GgufMetadataReader.create(
                skipKeys = setOf(
                    "tokenizer.ggml.scores",
                    "tokenizer.ggml.tokens",
                    "tokenizer.ggml.token_type"
                )
            ).readStructuredMetadata(stream).tokenizer?.chatTemplate?.takeIf { it.isNotBlank() }
        }
    }.getOrNull()
}
