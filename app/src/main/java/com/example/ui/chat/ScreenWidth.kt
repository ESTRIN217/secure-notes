package com.example.ui.chat

/**
 * Corte entre «pantalla grande» y «pantalla compacta» para el chat (spec 033, RNF-140/RNF-144).
 *
 * Es **pura** y sin `android` ni Compose: `true` cuando el ancho en dp es de al menos 600, que es el
 * mismo criterio que usa la hoja principal (`WindowWidthSizeClass` no compacto). Se lee una sola vez
 * por composición en los componentes (RNF-144).
 */
fun isLargeScreenWidth(widthDp: Int): Boolean = widthDp >= 600
