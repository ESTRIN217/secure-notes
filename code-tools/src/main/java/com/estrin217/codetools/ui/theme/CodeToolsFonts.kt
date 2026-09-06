package com.estrin217.codetools.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.estrin217.codetools.R

/**
 * JetBrains Mono (SIL OFL) used by the code editor. Bold weight included
 * for status badges and emphasized UI text.
 */
val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold)
)
