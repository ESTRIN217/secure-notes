package com.example.ui.chat

/**
 * Acción visible en la fila de una burbuja del chat (spec 045, RF-355).
 *
 * La fila es el único camino accesible: las burbujas no dependen de gestos de arrastre.
 * Contrato puro, sin `android` ni Compose, para poder probarlo sin Robolectric.
 */
enum class BubbleAction {
    EDIT,
    RESEND,
    COPY,
    SHARE,
    RETRY,
    INSERT
}

/**
 * Devuelve las acciones visibles para una burbuja, en orden estable.
 *
 * @param isUser true si la burbuja es del usuario.
 * @param isError true si la burbuja quedó en estado de error.
 * @param isLastAssistant true si es la última burbuja del asistente en la conversación.
 * @param canInsert true si el contexto permite insertar la respuesta en la nota.
 * @param hasContent true si la burbuja tiene contenido sobre el que actuar.
 */
fun bubbleActions(
    isUser: Boolean,
    isError: Boolean,
    isLastAssistant: Boolean,
    canInsert: Boolean,
    hasContent: Boolean
): List<BubbleAction> {
    if (!hasContent) return emptyList()
    if (isUser) return listOf(BubbleAction.EDIT, BubbleAction.RESEND, BubbleAction.COPY, BubbleAction.SHARE)
    if (isError) return listOf(BubbleAction.RETRY, BubbleAction.COPY, BubbleAction.SHARE)

    return buildList {
        add(BubbleAction.COPY)
        add(BubbleAction.SHARE)
        add(BubbleAction.RESEND)
        if (isLastAssistant && canInsert) add(BubbleAction.INSERT)
    }
}
