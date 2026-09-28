package com.estrin217.editordecodigo.ui.components

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.estrin217.editordecodigo.R
import com.estrin217.editordecodigo.syntax.SupportedLanguage
import com.estrin217.editordecodigo.syntax.SyntaxTheme
import com.estrin217.editordecodigo.syntax.SyntaxThemes

@Composable
fun JumpToLineDialog(
    totalLines: Int,
    onDismiss: () -> Unit,
    onJump: (Int) -> Unit
) {
    val invalidLineMessage = stringResource(R.string.jump_line_invalid, totalLines)
    var lineInput by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.menu_jump_line)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.jump_line_hint, totalLines),
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
                        error = invalidLineMessage
                    }
                },
                modifier = Modifier.testTag("jump_line_confirm_btn")
            ) {
                Text(stringResource(R.string.go))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun NewFileDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, language: SupportedLanguage) -> Unit
) {
    val invalidNameMessage = stringResource(R.string.new_file_invalid_name)
    var fileName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val trimmed = fileName.trim()
    val illegalChars = listOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
    val hasIllegalChars = trimmed.any { it in illegalChars }

    val detectedLang = remember(trimmed) {
        if (trimmed.isNotBlank() && !hasIllegalChars) {
            SupportedLanguage.fromFileName(trimmed)
        } else {
            null
        }
    }

    val submitFile: () -> Unit = {
        when {
            trimmed.isBlank() -> {
                errorMessage = invalidNameMessage
            }
            hasIllegalChars -> {
                errorMessage = invalidNameMessage
            }
            else -> {
                val finalName = if (!trimmed.contains('.') &&
                    !trimmed.equals("dockerfile", ignoreCase = true) &&
                    !trimmed.equals("gemfile", ignoreCase = true)
                ) {
                    "$trimmed.txt"
                } else {
                    trimmed
                }
                val lang = SupportedLanguage.fromFileName(finalName)
                onCreate(finalName, lang)
                onDismiss()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.NoteAdd,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = stringResource(R.string.menu_new_file),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.new_file_name_helper),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = fileName,
                    onValueChange = {
                        fileName = it
                        errorMessage = null
                    },
                    label = { Text(stringResource(R.string.new_file_name_label)) },
                    placeholder = { Text(stringResource(R.string.new_file_name_hint)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (fileName.isNotBlank()) {
                            IconButton(onClick = {
                                fileName = ""
                                errorMessage = null
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = stringResource(R.string.cd_clear)
                                )
                            }
                        }
                    },
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { error ->
                        {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Text
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { submitFile() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_file_name_input")
                )

                // Feedback visual dinámico del tipo de archivo detectado
                if (trimmed.isNotBlank() && !hasIllegalChars && detectedLang != null) {
                    if (detectedLang != SupportedLanguage.PLAIN_TEXT) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = stringResource(R.string.new_file_detected_type, detectedLang.displayName, detectedLang.fileExtension),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    } else if (!trimmed.contains('.')) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.new_file_extension_tip),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { submitFile() },
                enabled = fileName.isNotBlank(),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("new_file_create_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.create))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(stringResource(R.string.cancel))
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
        title = { Text(stringResource(R.string.syntax_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.syntax_search_hint)) },
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
                                    contentDescription = stringResource(R.string.selected_cd),
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
                Text(stringResource(R.string.close))
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
        title = { Text(stringResource(R.string.theme_title)) },
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
                                    contentDescription = stringResource(R.string.active_cd),
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
                Text(stringResource(R.string.close))
            }
        }
    )
}
