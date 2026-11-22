package com.example.ui.chat

/**
 * Modo de layout adaptativo del asistente de IA por ventana disponible (spec 046, RF-359).
 * Puro: sin Android/Compose, testeable sin Robolectric.
 */
enum class ChatWindowLayout {
    COMPACT,   // < 600 dp
    MEDIUM,    // 600..839 dp
    EXPANDED,  // 840..1199 dp
    WIDE       // >= 1200 dp
}

/** Clasifica el ancho disponible en dp a un modo de layout. */
fun chatLayoutMode(widthDp: Int): ChatWindowLayout = when {
    widthDp >= 1200 -> ChatWindowLayout.WIDE
    widthDp >= 840 -> ChatWindowLayout.EXPANDED
    widthDp >= 600 -> ChatWindowLayout.MEDIUM
    else -> ChatWindowLayout.COMPACT
}

/** Media+: el historial puede accederse sin ocultar completamente la conversación. */
fun showsInlineHistory(layout: ChatWindowLayout): Boolean =
    layout != ChatWindowLayout.COMPACT

/** Expanded+: historial y conversación en list-detail. */
fun showsListDetailHistory(layout: ChatWindowLayout): Boolean =
    layout == ChatWindowLayout.EXPANDED || layout == ChatWindowLayout.WIDE

/** Media+: paneles de modelo como side sheet/panel acotado. */
fun usesSecondarySheetForModels(layout: ChatWindowLayout): Boolean =
    layout != ChatWindowLayout.COMPACT

/**
 * Destino del área principal del asistente en Media+ (spec 046 enmienda, RF-372/RF-373).
 * Puro, sin Android/Compose.
 */
enum class AssistantDestination { CHAT, MODELS }

/** El rail permanente solo existe en Media+ (RF-370/RF-377). */
fun shouldShowAssistantRail(layout: ChatWindowLayout): Boolean =
    layout != ChatWindowLayout.COMPACT

/** Ancho del rail: extendido 260 dp, colapsado 72 dp (RF-371). */
fun assistantRailWidthDp(expanded: Boolean): Int = if (expanded) 260 else 72
