package com.example

import com.example.data.ai.AiAction
import com.example.ui.chat.ChatHint
import com.example.ui.chat.WriteTarget
import com.example.ui.chat.adoptPendingEditing
import com.example.ui.chat.canSend
import com.example.ui.chat.chipKey
import com.example.ui.chat.effectiveText
import com.example.ui.chat.hintFor
import com.example.ui.chat.shouldClearInput
import com.example.ui.chat.shouldSend
import com.example.ui.chat.writeTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas de L4 (habilitación, envío vacío, texto de ayuda y clave de ficha), L6 (qué muestra el
 * campo y adónde escribe) y L7 (vaciado tras enviar).
 *
 * Son pruebas puras a propósito: sin entorno de pruebas ni superficie de dibujo, para que se ejecuten
 * en la plataforma de desarrollo, donde la suite que sí lo necesita está bloqueada. El texto de
 * ayuda que devuelve L4 es un identificador del dominio, no una clave de recurso: el texto lo
 * resuelve el Composable con `stringResource`, así que aquí no hay recursos ni `R`.
 */
class ChatInputStateTest {

    // L4 / RF-33 a: habilitado con texto, con adjuntos pendientes o generando; deshabilitado si no
    // hay ninguna de las tres (caso límite 2).

    @Test
    fun `sending is enabled with text alone`() {
        assertTrue(canSend("hola", hasAttachments = false, isProcessing = false))
    }

    @Test
    fun `sending is enabled with pending attachments alone`() {
        assertTrue(canSend("", hasAttachments = true, isProcessing = false))
    }

    @Test
    fun `sending is enabled while generating with nothing typed`() {
        assertTrue(canSend("", hasAttachments = false, isProcessing = true))
    }

    @Test
    fun `sending is disabled without text attachments or generation`() {
        assertFalse(canSend("", hasAttachments = false, isProcessing = false))
        assertFalse(canSend("   ", hasAttachments = false, isProcessing = false))
    }

    @Test
    fun `every combination of text attachments and generation follows the truth table`() {
        val expected = listOf(
            // texto, adjuntos, generando -> habilitado
            Triple(false, false, false) to false,
            Triple(true, false, false) to true,
            Triple(false, true, false) to true,
            Triple(true, true, false) to true,
            Triple(false, false, true) to true,
            Triple(true, false, true) to true,
            Triple(false, true, true) to true,
            Triple(true, true, true) to true
        )

        for ((state, enabled) in expected) {
            val (hasText, hasAttachments, isProcessing) = state

            assertEquals(
                "text=$hasText attachments=$hasAttachments generating=$isProcessing",
                enabled,
                canSend(if (hasText) "hola" else "", hasAttachments, isProcessing)
            )
        }
    }

    // L4 / RF-33 a: el envío vacío se descarta SOLO sin adjuntos; con adjuntos pendientes se envía,
    // que es el comportamiento actual (caso límite 1 y caso límite 5).

    @Test
    fun `an empty send is discarded when there are no attachments`() {
        assertFalse(shouldSend("", hasAttachments = false))
        assertFalse(shouldSend("   ", hasAttachments = false))
    }

    @Test
    fun `an empty send goes through with pending attachments`() {
        assertTrue(shouldSend("", hasAttachments = true))
    }

    @Test
    fun `a non empty send always goes through`() {
        assertTrue(shouldSend("hola", hasAttachments = false))
        assertTrue(shouldSend("hola", hasAttachments = true))
    }

    // L4 / RF-33 c: con historial manda el texto de seguimiento; sin historial, el de la acción.

    @Test
    fun `with history the hint is always the follow up one`() {
        assertEquals(ChatHint.FOLLOWUP, hintFor(hasHistory = true, action = AiAction.GENERATE))
        assertEquals(ChatHint.FOLLOWUP, hintFor(hasHistory = true, action = AiAction.SUMMARIZE))
        assertEquals(ChatHint.FOLLOWUP, hintFor(hasHistory = true, action = AiAction.FIX_GRAMMAR))
    }

    @Test
    fun `without history the hint is the one of the active action`() {
        assertEquals(ChatHint.GENERATE, hintFor(hasHistory = false, action = AiAction.GENERATE))
        assertEquals(ChatHint.SUMMARIZE, hintFor(hasHistory = false, action = AiAction.SUMMARIZE))
        assertEquals(ChatHint.FIX_GRAMMAR, hintFor(hasHistory = false, action = AiAction.FIX_GRAMMAR))
    }

    // L4 / RF-33 h y decisión 7: la clave es posicional + nombre, única por construcción. Una clave
    // por nombre lanzaría IllegalArgumentException con dos adjuntos del mismo nombre (caso límite 4).

    @Test
    fun `two attachments with the same name get two different keys`() {
        assertNotEquals(chipKey(0, "nota.txt"), chipKey(1, "nota.txt"))
    }

    @Test
    fun `removing the first chip gives the second one a different key`() {
        assertNotEquals(chipKey(1, "nota.txt"), chipKey(0, "nota.txt"))
    }

    @Test
    fun `the key carries the position and the name and is never blank`() {
        val key = chipKey(2, "nota.txt")

        assertTrue("key was $key", key.isNotBlank())
        assertTrue("key was $key", key.contains("2"))
        assertTrue("key was $key", key.contains("nota.txt"))
    }

    @Test
    fun `five attachments with the same name get five different keys`() {
        val keys = (0 until 5).map { chipKey(it, "nota.txt") }

        assertEquals(keys.size, keys.toSet().size)
    }

    // L6 / RF-33 e: el campo muestra la edición si la hay y si no el texto normal (caso límite 7).

    @Test
    fun `without editing the field shows the draft`() {
        assertEquals("borrador", effectiveText("borrador", editing = null))
    }

    @Test
    fun `with editing the field shows the editing instead of the draft`() {
        assertEquals("editando", effectiveText("borrador", editing = "editando"))
    }

    @Test
    fun `an emptied editing shows an empty field and not the draft`() {
        assertEquals("", effectiveText("borrador", editing = ""))
    }

    @Test
    fun `the write target is the draft while nothing is being edited`() {
        assertEquals(WriteTarget.DRAFT, writeTarget(hasEditing = false))
    }

    @Test
    fun `the write target is the editing while a message is being edited`() {
        assertEquals(WriteTarget.EDITING, writeTarget(hasEditing = true))
    }

    // L6 / RF-33 e: al cargar una edición al abrir la sesión se fijan los dos estados, igual que
    // las dos vías de entrar en edición.

    @Test
    fun `a pending editing lands in both states and clears the editing flag`() {
        assertEquals(Pair("editando", null), adoptPendingEditing("editando"))
    }

    @Test
    fun `a null pending editing does not invent an editing`() {
        val (draft, editing) = adoptPendingEditing(pendingEditing = null)

        assertEquals("", draft)
        assertNull(editing)
    }

    // L7 / RF-33 f: el campo solo se limpia con la acción de generación (caso límite 6).

    @Test
    fun `the field is cleared after sending with the generate action`() {
        assertTrue(shouldClearInput(AiAction.GENERATE))
    }

    @Test
    fun `the field is not cleared with the summarize action`() {
        assertFalse(shouldClearInput(AiAction.SUMMARIZE))
    }

    @Test
    fun `the field is not cleared with the fix grammar action`() {
        assertFalse(shouldClearInput(AiAction.FIX_GRAMMAR))
    }
}
