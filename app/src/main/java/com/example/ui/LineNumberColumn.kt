package com.example.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Renders the line numbers for a piece of plain text. It uses the exact same
 * [TextStyle] as the code editor so that each number stays aligned to its
 * logical line (matching line heights).
 *
 * When [contentWidthPx] is provided (> 0), long logical lines are soft-wrapped
 * so each logical line can occupy several visual lines; the logical number is
 * shown only on the first visual line and blank continuation lines follow, so
 * the column height matches the wrapped editor. Without a width, logical lines
 * map one-to-one to visual lines.
 */
@OptIn(ExperimentalTextApi::class)
@Composable
fun LineNumberColumn(
    text: String,
    contentColor: androidx.compose.ui.graphics.Color,
    textStyle: TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    modifier: Modifier = Modifier,
    contentWidthPx: Int = 0
) {
    val textMeasurer = rememberTextMeasurer()
    val logicalLines = text.split('\n')
    val numbers = buildString {
        logicalLines.forEachIndexed { index, line ->
            val number = (index + 1).toString()
            val visualLines = if (contentWidthPx > 0) {
                textMeasurer.measure(
                    text = line,
                    style = textStyle,
                    overflow = TextOverflow.Clip,
                    constraints = Constraints(maxWidth = contentWidthPx)
                ).lineCount.coerceAtLeast(1)
            } else {
                1
            }
            repeat(visualLines) { visual ->
                if (index > 0 || visual > 0) append('\n')
                if (visual == 0) append(number) else append(" ".repeat(number.length))
            }
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
