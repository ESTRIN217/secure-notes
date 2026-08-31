package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.example.R

/**
 * JetBrains Mono (SIL OFL) used by the standalone code editor. The "calt"
 * (contextual alternates) OpenType feature turns on the programming ligatures
 * (`!=`, `>=`, `<=`, `->`, `==` ...) which are disabled by default in this font.
 */
val JetBrainsMonoFontFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular)
)

/** Code editor style: JetBrains Mono with coding ligatures enabled. */
@Composable
fun codeEditorTextStyle(): TextStyle =
    MaterialTheme.typography.bodyLarge.copy(
        fontFamily = JetBrainsMonoFontFamily,
        fontFeatureSettings = "calt",
        color = MaterialTheme.colorScheme.onSurface
    )
