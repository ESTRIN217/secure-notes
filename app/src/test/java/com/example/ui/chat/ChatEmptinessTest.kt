package com.example.ui.chat

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Spec 038 (RF-273, RF-274, RF-275, RNF-165): predicados de descarte de chats vacíos. JVM puro.
 */
class ChatEmptinessTest {

    @Test
    fun shouldDiscard_noTurns_noDraft_discards() {
        assertTrue(shouldDiscardChatSession(0, hasDraftOrAttachments = false))
        assertTrue(shouldDiscardChatSession(-1, hasDraftOrAttachments = false))
    }

    @Test
    fun shouldDiscard_noTurns_withDraftOrAttachments_keeps() {
        assertFalse(shouldDiscardChatSession(0, hasDraftOrAttachments = true))
    }

    @Test
    fun shouldDiscard_withTurns_keeps() {
        assertFalse(shouldDiscardChatSession(1, hasDraftOrAttachments = false))
        assertFalse(shouldDiscardChatSession(10, hasDraftOrAttachments = true))
    }

    @Test
    fun isPurgeable_createdThisProcessAndEmpty_purges() {
        assertTrue(isPurgeableSession(createdAtMs = 5000L, processStartedAtMs = 4000L, messageCount = 0))
        assertTrue(isPurgeableSession(createdAtMs = 4000L, processStartedAtMs = 4000L, messageCount = 0))
    }

    @Test
    fun isPurgeable_beforeProcessStart_keeps() {
        assertFalse(isPurgeableSession(createdAtMs = 3999L, processStartedAtMs = 4000L, messageCount = 0))
    }

    @Test
    fun isPurgeable_withTurns_keeps() {
        assertFalse(isPurgeableSession(createdAtMs = 5000L, processStartedAtMs = 4000L, messageCount = 1))
    }
}
