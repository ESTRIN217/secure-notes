package com.estrin217.pdfviewer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = PdfDarkPrimary,
    onPrimary = PdfDarkOnPrimary,
    primaryContainer = PdfDarkPrimaryContainer,
    onPrimaryContainer = PdfDarkOnPrimaryContainer,
    secondary = PdfDarkSecondary,
    onSecondary = PdfDarkOnSecondary,
    secondaryContainer = PdfDarkSecondaryContainer,
    onSecondaryContainer = PdfDarkOnSecondaryContainer,
    background = PdfDarkBackground,
    onBackground = PdfDarkOnBackground,
    surface = PdfDarkSurface,
    onSurface = PdfDarkOnSurface,
    surfaceVariant = PdfDarkSurfaceVariant,
    onSurfaceVariant = PdfDarkOnSurfaceVariant,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = PdfRedPrimary,
    onPrimary = PdfRedOnPrimary,
    primaryContainer = PdfRedPrimaryContainer,
    onPrimaryContainer = PdfRedOnPrimaryContainer,
    secondary = PdfSecondary,
    onSecondary = PdfOnSecondary,
    secondaryContainer = PdfSecondaryContainer,
    onSecondaryContainer = PdfOnSecondaryContainer,
    tertiary = PdfTertiary,
    onTertiary = PdfOnTertiary,
    tertiaryContainer = PdfTertiaryContainer,
    onTertiaryContainer = PdfOnTertiaryContainer,
    background = PdfBackground,
    onBackground = PdfOnBackground,
    surface = PdfSurface,
    onSurface = PdfOnSurface,
    surfaceVariant = PdfSurfaceVariant,
    onSurfaceVariant = PdfOnSurfaceVariant,
    outline = PdfOutline,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Keep consistent PDF branding by default
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

