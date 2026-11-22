package com.example.ui.chat

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Spec 045 (RF-355, RNF-215): contrato puro de acciones de burbuja.
 *
 * Las burbujas no dependen de gestos de arrastre; la fila de acciones es el único camino.
 * JVM puro, sin Robolectric.
 */
class BubbleActionsTest {

    @Test
    fun user_withContent_exposesEditResendCopyShare() {
        val actions = bubbleActions(
            isUser = true,
            isError = false,
            isLastAssistant = false,
            canInsert = false,
            hasContent = true
        )
        assertEquals(
            listOf(BubbleAction.EDIT, BubbleAction.RESEND, BubbleAction.COPY, BubbleAction.SHARE),
            actions
        )
    }

    @Test
    fun assistant_error_exposesRetryCopyShare() {
        val actions = bubbleActions(
            isUser = false,
            isError = true,
            isLastAssistant = true,
            canInsert = false,
            hasContent = true
        )
        assertEquals(
            listOf(BubbleAction.RETRY, BubbleAction.COPY, BubbleAction.SHARE),
            actions
        )
    }

    @Test
    fun assistant_ok_last_canInsert_exposesCopyShareResendInsert() {
        val actions = bubbleActions(
            isUser = false,
            isError = false,
            isLastAssistant = true,
            canInsert = true,
            hasContent = true
        )
        assertEquals(
            listOf(BubbleAction.COPY, BubbleAction.SHARE, BubbleAction.RESEND, BubbleAction.INSERT),
            actions
        )
    }

    @Test
    fun assistant_ok_notLast_cannotInsert_exposesCopyShareResend() {
        val actions = bubbleActions(
            isUser = false,
            isError = false,
            isLastAssistant = false,
            canInsert = false,
            hasContent = true
        )
        assertEquals(
            listOf(BubbleAction.COPY, BubbleAction.SHARE, BubbleAction.RESEND),
            actions
        )
    }

    @Test
    fun emptyContent_exposesNoActions() {
        assertEquals(
            emptyList<BubbleAction>(),
            bubbleActions(isUser = true, isError = false, isLastAssistant = false, canInsert = false, hasContent = false)
        )
        assertEquals(
            emptyList<BubbleAction>(),
            bubbleActions(isUser = false, isError = false, isLastAssistant = false, canInsert = true, hasContent = false)
        )
    }
}
