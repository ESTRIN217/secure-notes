package com.example.ui

import android.content.Context
import com.example.data.ai.AiBackend
import com.example.shortcut.ShortcutDestination
import com.example.ui.viewmodel.ChatHistoryViewModel

/**
 * Cumple el destino de un atajo del lanzador (spec 021, RF-125, RF-126).
 *
 * Los cuatro destinos son **exactamente** los mismos `Screen` a los que ya navegan los botones de la app,
 * así que aquí no se abre ningún camino nuevo: la pantalla de búsqueda sigue yendo a la lista al pulsar
 * atrás, y el chat sigue siendo el mismo chat.
 *
 * El único que sale de la app es [ShortcutDestination.CODE_EDITOR], que lanza su `Activity`. **Llega aquí
 * después de desbloquear**, porque quien decide eso es [com.example.shortcut.decidir] (RF-127).
 *
 * El atajo de ajustes **ya no aparece aquí** (Spec 044, RF-352): apunta directamente al alias exportado de
 * la Activity de ajustes, así que el launcher lo abre sin pasar por `MainActivity` ni por esta función.
 */
fun ejecutarDestino(
    destino: ShortcutDestination,
    navigator: Navigator,
    chatHistoryViewModel: ChatHistoryViewModel,
    backend: AiBackend,
    context: Context
) {
    when (destino) {
        ShortcutDestination.NEW_NOTE -> navigator.onNavigateTo(Screen.NoteEditor(0))
        ShortcutDestination.SEARCH -> navigator.onNavigateTo(Screen.Search)
        ShortcutDestination.CODE_EDITOR -> launchCodeEditor(context)
        ShortcutDestination.AI_CHAT -> abrirChatNuevo(chatHistoryViewModel, backend, navigator)
    }
}

/**
 * El atajo de la IA abre **una conversación nueva**, igual que el botón de la lista (RF-126).
 *
 * La sesión se crea **antes** de navegar: si se navegara primero, la pantalla aparecería sin conversación y
 * quedaría esperando a una que aún no existe.
 */
private fun abrirChatNuevo(
    chatHistoryViewModel: ChatHistoryViewModel,
    backend: AiBackend,
    navigator: Navigator
) {
    chatHistoryViewModel.createSession(backend = backend)
    navigator.onNavigateTo(Screen.AiChatStandalone)
}