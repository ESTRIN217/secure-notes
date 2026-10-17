package com.example.ui.theme

import com.example.DarkModeOption

/**
 * Cuál de los dos lados del tema corresponde a cada ajuste de modo (spec 025, RF-171, D8).
 *
 * Es una decisión **pura**: no mira el sistema, lo recibe. Por eso vive aquí y no dentro de los cuatro `@Composable`
 * y el servicio que la tenían copiada —`MainActivity`, `CrashReportActivity`, `CodeEditorActivity` y
 * `FloatingBubbleService`—, y por eso se puede probar sin `Context`: el que sabe de `isSystemInDarkTheme()` o de
 * `Configuration.UI_MODE_NIGHT_MASK` se lo pasa.
 *
 * El ajuste de modo y el tema son cosas distintas: el tema decide **qué colores** hay y el modo decide cuál de sus
 * dos lados se usa. Un tema nunca cambia el modo por sorpresa.
 */
fun DarkModeOption.isDark(systemInDarkTheme: Boolean): Boolean = when (this) {
    DarkModeOption.SYSTEM -> systemInDarkTheme
    DarkModeOption.ON -> true
    DarkModeOption.OFF -> false
}