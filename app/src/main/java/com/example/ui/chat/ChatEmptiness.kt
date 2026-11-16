package com.example.ui.chat

/**
 * Predicados de descarte de chats vacíos (spec 038). Piezas puras, sin `android`.
 */

/** Al salir del chat (RF-273): verdadero si la sesión no tiene turnos **y** no hay contenido tecleado ni adjuntos pendientes. */
fun shouldDiscardChatSession(turnsCount: Int, hasDraftOrAttachments: Boolean): Boolean =
    turnsCount <= 0 && !hasDraftOrAttachments

/** ¿Purga al cerrar la app (RF-274)? Verdadero si fue creada en este proceso y nunca tuvo turnos (`messageCount == 0`). */
fun isPurgeableSession(createdAtMs: Long, processStartedAtMs: Long, messageCount: Int): Boolean =
    messageCount == 0 && createdAtMs >= processStartedAtMs
