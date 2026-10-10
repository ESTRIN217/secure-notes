package com.estrin217.editordecodigo.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.estrin217.editordecodigo.markdown.PreviewTheme
import com.estrin217.editordecodigo.syntax.SyntaxTheme

/**
 * El tema del editor traducido al de la vista previa.
 *
 * Vive **fuera** de `MarkdownRenderer` a propósito: el renderizador es lógica pura y sus pruebas se
 * ejecutan sin Android (RNF-26), mientras que `Color.toArgb()` es lo único que necesita Compose. El
 * HTML se genera con cadenas hexadecimales y el color llega después, ya en la capa de UI.
 *
 * Aquí la usa `CodeEditorViewModel`, que es quien genera el documento en un hilo aparte (RF-75).
 */
fun SyntaxTheme.toPreviewTheme(): PreviewTheme = PreviewTheme(
    background = background.toHex(),
    text = text.toHex(),
    accent = keyword.toHex(),
    codeBackground = gutterBackground.toHex(),
    quoteBar = comment.toHex(),
    border = activeLineBackground.toHex()
)

private fun Color.toHex(): String = String.format("#%06X", 0xFFFFFF and toArgb())