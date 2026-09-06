package com.estrin217.codetools.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardTab
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.estrin217.codetools.syntax.SyntaxTheme
import com.estrin217.codetools.ui.theme.JetBrainsMono

@Composable
fun QuickSymbolBar(
    theme: SyntaxTheme,
    onInsertSymbol: (String) -> Unit,
    onInsertIndent: () -> Unit,
    onToggleComment: () -> Unit,
    onDuplicateLine: () -> Unit,
    onDeleteLine: () -> Unit,
    onFormatCode: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val symbols = listOf(
        "{", "}", "[", "]", "(", ")",
        "\"", "'", ":", "=", ";", "$",
        "/", "\\", "_", "-", "#", "!",
        "&", "|", "<", ">", "%", "*",
        "@", "~", "`", "^", "?"
    )

    Surface(
        color = theme.gutterBackground,
        tonalElevation = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indent Tab button
            SymbolActionChip(
                label = "TAB",
                backgroundColor = theme.background,
                textColor = theme.keyword,
                onClick = onInsertIndent,
                testTag = "symbol_tab"
            )

            // Format Code button
            if (onFormatCode != null) {
                SymbolActionChip(
                    label = "✨ Formatear",
                    backgroundColor = theme.background,
                    textColor = theme.type,
                    onClick = onFormatCode,
                    testTag = "action_format_code"
                )
            }

            // Comment line toggle
            SymbolActionChip(
                label = "# Comentar",
                backgroundColor = theme.background,
                textColor = theme.comment,
                onClick = onToggleComment,
                testTag = "action_toggle_comment"
            )

            // Duplicate line button
            SymbolActionChip(
                label = "+ Duplicar",
                backgroundColor = theme.background,
                textColor = theme.function,
                onClick = onDuplicateLine,
                testTag = "action_duplicate_line"
            )

            // Delete line button
            SymbolActionChip(
                label = "- Borrar Lín",
                backgroundColor = theme.background,
                textColor = theme.variable,
                onClick = onDeleteLine,
                testTag = "action_delete_line"
            )

            // Quick common symbols
            symbols.forEach { symbol ->
                SymbolKeyChip(
                    symbol = symbol,
                    theme = theme,
                    onClick = { onInsertSymbol(symbol) }
                )
            }
        }
    }
}

@Composable
private fun SymbolActionChip(
    label: String,
    backgroundColor: Color,
    textColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = backgroundColor,
        modifier = Modifier
            .height(36.dp)
            .testTag(testTag)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 10.dp)
        ) {
            Text(
                text = label,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = JetBrainsMono
            )
        }
    }
}

@Composable
private fun SymbolKeyChip(
    symbol: String,
    theme: SyntaxTheme,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = theme.background,
        modifier = Modifier
            .size(width = 36.dp, height = 36.dp)
            .testTag("symbol_key_$symbol")
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = symbol,
                color = theme.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = JetBrainsMono
            )
        }
    }
}
