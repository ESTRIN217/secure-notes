package com.estrin217.tema

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * El tema activo, para que quien pinta no tenga que saber de dónde sale (spec 025, D5).
 *
 * Lo publica `MyApplicationTheme` de `:app` y lo leen el editor de código y los bloques de nota, que **no** pueden
 * preguntar por la preferencia —no la conocen, y no deben— ni recibirla por parámetro en treinta sitios.
 *
 * Lleva el [AppTheme] entero y no una paleta ya derivada, y por un motivo concreto: cuando el tema es dinámico, el
 * editor y el bloque de nota caen a **paletas distintas** (`editorPalette` y `notePalette`). Si el `CompositionLocal`
 * llevara una sola, habría que elegir una de las dos y perder la otra.
 *
 * Es `staticCompositionLocalOf` y no `compositionLocalOf` porque el tema no cambia durante la vida de una
 * composición: cambia entre Activity, o cuando alguien elige uno, y para entonces hay que repintar de todos modos.
 */
val LocalAppTheme: ProvidableCompositionLocal<AppTheme> = staticCompositionLocalOf { AppThemes.dynamic }