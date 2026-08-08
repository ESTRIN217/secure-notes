@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R

private val fontOptions = listOf("default", "serif", "monospace", "sans-serif", "cursive")
private val sizeOptions = listOf("default", "12", "14", "16", "18", "20", "24", "28")

@Composable
fun FontSizeSheet(
    onDismiss: () -> Unit,
    onFontSelected: (String) -> Unit,
    onSizeSelected: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(id = R.string.rich_font_family),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            fontOptions.forEach { font ->
                val label = if (font == "default") {
                    stringResource(R.string.text_default)
                } else {
                    font.replaceFirstChar { it.titlecase() }
                }
                Text(
                    text = label,
                    fontFamily = when (font) {
                        "serif" -> FontFamily.Serif
                        "monospace" -> FontFamily.Monospace
                        "sans-serif" -> FontFamily.SansSerif
                        "cursive" -> FontFamily.Cursive
                        else -> FontFamily.Default
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onFontSelected(font)
                            onDismiss()
                        }
                        .padding(vertical = 12.dp)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = stringResource(id = R.string.rich_font_size),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            sizeOptions.forEach { size ->
                val label = if (size == "default") {
                    stringResource(R.string.text_default)
                } else {
                    "${size}sp"
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSizeSelected(size)
                            onDismiss()
                        }
                        .padding(vertical = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
