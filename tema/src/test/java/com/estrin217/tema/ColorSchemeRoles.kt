package com.estrin217.tema

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Los **48** roles de [ColorScheme], en el orden en que los declara Material 3, con su nombre.
 *
 * Existe para que las pruebas puedan recorrer el esquema entero sin tener que repetir la lista, y sobre todo
 * para que una comprobación de contraste se aplique a **todos** los roles y no solo a los que alguien recuerda.
 * `ColorScheme` no implementa `equals`, así que comparar dos esquemas es comparar sus roles uno a uno.
 */
internal fun ColorScheme.roles(): List<Pair<String, Color>> = listOf(
    "primary" to primary,
    "onPrimary" to onPrimary,
    "primaryContainer" to primaryContainer,
    "onPrimaryContainer" to onPrimaryContainer,
    "inversePrimary" to inversePrimary,
    "secondary" to secondary,
    "onSecondary" to onSecondary,
    "secondaryContainer" to secondaryContainer,
    "onSecondaryContainer" to onSecondaryContainer,
    "tertiary" to tertiary,
    "onTertiary" to onTertiary,
    "tertiaryContainer" to tertiaryContainer,
    "onTertiaryContainer" to onTertiaryContainer,
    "background" to background,
    "onBackground" to onBackground,
    "surface" to surface,
    "onSurface" to onSurface,
    "surfaceVariant" to surfaceVariant,
    "onSurfaceVariant" to onSurfaceVariant,
    "surfaceTint" to surfaceTint,
    "inverseSurface" to inverseSurface,
    "inverseOnSurface" to inverseOnSurface,
    "error" to error,
    "onError" to onError,
    "errorContainer" to errorContainer,
    "onErrorContainer" to onErrorContainer,
    "outline" to outline,
    "outlineVariant" to outlineVariant,
    "scrim" to scrim,
    "surfaceBright" to surfaceBright,
    "surfaceDim" to surfaceDim,
    "surfaceContainer" to surfaceContainer,
    "surfaceContainerHigh" to surfaceContainerHigh,
    "surfaceContainerHighest" to surfaceContainerHighest,
    "surfaceContainerLow" to surfaceContainerLow,
    "surfaceContainerLowest" to surfaceContainerLowest,
    "primaryFixed" to primaryFixed,
    "primaryFixedDim" to primaryFixedDim,
    "onPrimaryFixed" to onPrimaryFixed,
    "onPrimaryFixedVariant" to onPrimaryFixedVariant,
    "secondaryFixed" to secondaryFixed,
    "secondaryFixedDim" to secondaryFixedDim,
    "onSecondaryFixed" to onSecondaryFixed,
    "onSecondaryFixedVariant" to onSecondaryFixedVariant,
    "tertiaryFixed" to tertiaryFixed,
    "tertiaryFixedDim" to tertiaryFixedDim,
    "onTertiaryFixed" to onTertiaryFixed,
    "onTertiaryFixedVariant" to onTertiaryFixedVariant,
)

/** Las semillas de un tema que solo se distingue por sus tres acentos, para las pruebas de color. */
internal val SEMILLAS_DE_PRUEBA = ThemeSeeds(
    primary = Color(0xFF3794FF),
    secondary = Color(0xFF4EC9B0),
    tertiary = Color(0xFFF9E2AF),
)
/** El color como entero ARGB, para compararlo con un literal en las aserciones. */
internal fun Color.argb(): Int = toArgb()
