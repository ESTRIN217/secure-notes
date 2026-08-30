package com.example.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Renders the line numbers for a piece of plain text. It uses the exact same
 * [TextStyle] as the code editor so that each number stays aligned to its
 * logical line (matching line heights). Intended to sit to the left of the
 * editor, outside its horizontal scroll area (the gutter stays fixed while a
 * long, non-wrapped line scrolls).
 */
@Composable
fun LineNumberColumn(
    text: String,
    contentColor: androidx.compose.ui.graphics.Color,
    textStyle: TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    modifier: Modifier = Modifier
) {
    val lineCount = text.count { it == '\n' } + 1
    val numbers = buildString {
        for (line in 1..lineCount) {
            if (line > 1) append('\n')
            append(line)
        }
    }
    androidx.compose.material3.Text(
        text = numbers,
        style = textStyle,
        color = contentColor,
        textAlign = TextAlign.End,
        softWrap = false,
        modifier = modifier.padding(end = 8.dp)
    )
}
