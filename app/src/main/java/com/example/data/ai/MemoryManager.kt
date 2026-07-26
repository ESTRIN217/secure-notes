package com.example.data.ai

import com.example.data.local.MemoryDao
import com.example.data.local.MemoryEntity
import com.example.ui.viewmodel.ConversationTurn

class MemoryManager(
    private val memoryDao: MemoryDao
) {
    companion object {
        private const val SUMMARY_INTERVAL = 15
        private const val MAX_CONTEXT_MEMORIES = 3
        const val TYPE_SUMMARY = "summary"
        const val TYPE_PINNED = "pinned"
        const val TYPE_USER = "user"
    }

    suspend fun shouldSummarize(sessionId: Int): Boolean {
        val count = memoryDao.countBySessionId(sessionId)
        return count > 0 && count % SUMMARY_INTERVAL == 0
    }

    suspend fun saveSummary(sessionId: Int, summary: String) {
        memoryDao.insert(
            MemoryEntity(
                sessionId = sessionId,
                type = TYPE_SUMMARY,
                content = summary,
                summary = null
            )
        )
    }

    suspend fun getRelevantMemories(sessionId: Int): List<String> {
        val summaries = memoryDao.getMemoriesBySessionAndType(sessionId, TYPE_SUMMARY, MAX_CONTEXT_MEMORIES)
        val pinned = memoryDao.getMemoriesBySessionAndType(sessionId, TYPE_PINNED, MAX_CONTEXT_MEMORIES)
        val result = mutableListOf<String>()
        if (pinned.isNotEmpty()) {
            result.add("Pinned memories:\n" + pinned.joinToString("\n") { "- ${it.content}" })
        }
        if (summaries.isNotEmpty()) {
            result.add("Previous conversation summaries:\n" + summaries.joinToString("\n") { "- ${it.content}" })
        }
        return result
    }

    suspend fun savePinnedMemory(sessionId: Int, content: String) {
        memoryDao.insert(
            MemoryEntity(
                sessionId = sessionId,
                type = TYPE_PINNED,
                content = content
            )
        )
    }

    suspend fun clearSessionMemories(sessionId: Int) {
        memoryDao.deleteBySessionId(sessionId)
    }

    fun buildMemoryPrompt(turns: List<ConversationTurn>): String? {
        if (turns.size < 3) return null
        val relevant = turns.takeLast(10).filter { it.role == "user" || it.role == "assistant" }
        if (relevant.isEmpty()) return null
        return "Summarize the key points from this conversation:\n" +
            relevant.joinToString("\n") { "${it.role}: ${it.content.take(200)}" }
    }
}
