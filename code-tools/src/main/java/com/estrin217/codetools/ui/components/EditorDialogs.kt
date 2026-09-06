package com.estrin217.codetools.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.estrin217.codetools.syntax.SupportedLanguage
import com.estrin217.codetools.syntax.SyntaxTheme
import com.estrin217.codetools.syntax.SyntaxThemes

@Composable
fun JumpToLineDialog(
    totalLines: Int,
    onDismiss: () -> Unit,
    onJump: (Int) -> Unit
) {
    var lineInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ir a la Línea") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Introduce el número de línea (1 - $totalLines):",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = lineInput,
                    onValueChange = {
                        lineInput = it.filter { char -> char.isDigit() }
                        error = null
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("jump_line_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val num = lineInput.toIntOrNull()
                    if (num != null && num in 1..totalLines) {
                        onJump(num)
                        onDismiss()
                    } else {
                        error = "Línea inválida (1 - $totalLines)"
                    }
                },
                modifier = Modifier.testTag("jump_line_confirm_btn")
            ) {
                Text("Ir")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun NewFileDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, language: SupportedLanguage) -> Unit
) {
    var fileName by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf(SupportedLanguage.BASH) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo Archivo de Código") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = fileName,
                    onValueChange = {
                        fileName = it
                        // Auto-detect language if typed extension
                        val detected = SupportedLanguage.fromFileName(it)
                        if (detected != SupportedLanguage.PLAIN_TEXT) {
                            selectedLanguage = detected
                        }
                    },
                    label = { Text("Nombre del archivo") },
                    placeholder = { Text("ej. script.sh, config.yaml") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_file_name_input")
                )

                Text(
                    text = "Sintaxis / Tipo de archivo:",
                    style = MaterialTheme.typography.labelLarge
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    items(SupportedLanguage.entries) { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedLanguage = lang
                                    if (fileName.contains('.')) {
                                        fileName = fileName.substringBeforeLast('.') + "." + lang.fileExtension
                                    }
                                }
                                .padding(vertical = 6.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedLanguage == lang,
                                onClick = {
                                    selectedLanguage = lang
                                    if (fileName.contains('.')) {
                                        fileName = fileName.substringBeforeLast('.') + "." + lang.fileExtension
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = lang.displayName, fontSize = 14.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = if (fileName.isBlank()) {
                        "nuevo_script.${selectedLanguage.fileExtension}"
                    } else if (!fileName.contains('.')) {
                        "$fileName.${selectedLanguage.fileExtension}"
                    } else {
                        fileName
                    }
                    onCreate(finalName, selectedLanguage)
                    onDismiss()
                },
                modifier = Modifier.testTag("new_file_create_btn")
            ) {
                Text("Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun LanguageSelectorDialog(
    currentLanguage: SupportedLanguage,
    onDismiss: () -> Unit,
    onSelectLanguage: (SupportedLanguage) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredLanguages = remember(searchQuery) {
        if (searchQuery.isBlank()) SupportedLanguage.entries
        else SupportedLanguage.entries.filter {
            it.displayName.contains(searchQuery, ignoreCase = true) ||
                    it.fileExtension.contains(searchQuery, ignoreCase = true) ||
                    it.extensions.any { ext -> ext.contains(searchQuery, ignoreCase = true) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Resaltado de Sintaxis") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar lenguaje (ej. Rust, SQL)...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_language_input")
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                ) {
                    items(filteredLanguages) { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onSelectLanguage(lang)
                                    onDismiss()
                                }
                                .padding(vertical = 10.dp, horizontal = 12.dp)
                                .testTag("lang_item_${lang.fileExtension}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = lang.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (lang == currentLanguage) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = ".${lang.fileExtension} (${lang.extensions.joinToString(", ") { ".$it" }})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (lang == currentLanguage) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Seleccionado",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

@Composable
fun ThemeSelectorDialog(
    currentTheme: SyntaxTheme,
    onDismiss: () -> Unit,
    onSelectTheme: (SyntaxTheme) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tema del Editor") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SyntaxThemes.all.forEach { theme ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = theme.background,
                        onClick = {
                            onSelectTheme(theme)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(theme.keyword)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(theme.string)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = theme.name,
                                    color = theme.text,
                                    fontWeight = if (theme.id == currentTheme.id) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 15.sp
                                )
                            }
                            if (theme.id == currentTheme.id) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Activo",
                                    tint = theme.function
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}
