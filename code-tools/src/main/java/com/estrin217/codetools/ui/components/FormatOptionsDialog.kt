package com.estrin217.codetools.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatIndentIncrease
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.estrin217.codetools.R
import com.estrin217.codetools.formatter.FormatOptions
import com.estrin217.codetools.syntax.SupportedLanguage

/**
 * Diálogo de configuración y ejecución del formateador de código
 * según directrices de Material Design 3 Expresivo y localización en español venezolano.
 */
@Composable
fun FormatOptionsDialog(
    currentOptions: FormatOptions,
    currentLanguage: SupportedLanguage,
    onDismiss: () -> Unit,
    onApplyAndFormat: (FormatOptions) -> Unit
) {
    var indentSize by remember { mutableIntStateOf(currentOptions.indentSize) }
    var useSpaces by remember { mutableStateOf(currentOptions.useSpaces) }
    var trimWhitespace by remember { mutableStateOf(currentOptions.trimTrailingWhitespace) }
    var insertFinalNewline by remember { mutableStateOf(currentOptions.insertFinalNewline) }
    var maxEmptyLines by remember { mutableIntStateOf(currentOptions.maxEmptyLines) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("format_options_dialog"),
        icon = {
            Icon(
                imageVector = Icons.Default.AutoFixHigh,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.format_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.format_subtitle, currentLanguage.displayName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Sección: Tamaño de Sangría
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.FormatIndentIncrease,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.format_indent_section),
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Opción 2 espacios
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    useSpaces = true
                                    indentSize = 2
                                }
                                .padding(vertical = 4.dp, horizontal = 6.dp)
                                .testTag("format_indent_2_btn"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = useSpaces && indentSize == 2,
                                onClick = {
                                    useSpaces = true
                                    indentSize = 2
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(stringResource(R.string.format_indent_2), fontWeight = FontWeight.Medium)
                                Text(stringResource(R.string.format_indent_2_desc), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Opción 4 espacios
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    useSpaces = true
                                    indentSize = 4
                                }
                                .padding(vertical = 4.dp, horizontal = 6.dp)
                                .testTag("format_indent_4_btn"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = useSpaces && indentSize == 4,
                                onClick = {
                                    useSpaces = true
                                    indentSize = 4
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(stringResource(R.string.format_indent_4), fontWeight = FontWeight.Medium)
                                Text(stringResource(R.string.format_indent_4_desc), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Opción Tabulaciones
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { useSpaces = false }
                                .padding(vertical = 4.dp, horizontal = 6.dp)
                                .testTag("format_indent_tab_btn"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = !useSpaces,
                                onClick = { useSpaces = false }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(stringResource(R.string.format_indent_tab), fontWeight = FontWeight.Medium)
                                Text(stringResource(R.string.format_indent_tab_desc), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // Sección: Reglas adicionales de limpieza
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = stringResource(R.string.format_cleanup_title),
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelLarge
                        )

                        // Switch: Limpiar espacios al final
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.format_trim_title), fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                Text(stringResource(R.string.format_trim_desc), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = trimWhitespace,
                                onCheckedChange = { trimWhitespace = it },
                                modifier = Modifier.testTag("format_trim_spaces_switch")
                            )
                        }

                        // Switch: Salto de línea final
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.format_eof_title), fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                Text(stringResource(R.string.format_eof_desc), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = insertFinalNewline,
                                onCheckedChange = { insertFinalNewline = it },
                                modifier = Modifier.testTag("format_final_newline_switch")
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newOptions = FormatOptions(
                        indentSize = indentSize,
                        useSpaces = useSpaces,
                        trimTrailingWhitespace = trimWhitespace,
                        insertFinalNewline = insertFinalNewline,
                        maxEmptyLines = maxEmptyLines,
                        isCustom = true
                    )
                    onApplyAndFormat(newOptions)
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("format_apply_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.AutoFixHigh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.format_apply))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("format_cancel_btn")
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
