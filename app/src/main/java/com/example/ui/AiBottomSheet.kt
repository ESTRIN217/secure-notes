package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.ai.*
import com.example.ui.viewmodel.AiViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiBottomSheet(
    viewModel: AiViewModel,
    selectedText: String,
    fullContent: String,
    onInsert: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var currentAction by remember { mutableStateOf(AiAction.GENERATE) }
    var promptText by remember { mutableStateOf("") }
    var rewriteStyle by remember { mutableStateOf(RewriteStyle.FORMAL) }
    var targetLanguage by remember { mutableStateOf("en") }

    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val resultText by viewModel.resultText.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val hasSelection = selectedText.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.clearResult()
            onDismiss()
        },
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 48.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.ai_assistant),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Action selector
            ActionChipRow(
                currentAction = currentAction,
                onActionSelected = {
                    currentAction = it
                    viewModel.clearResult()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Dynamic input area based on action
            when (currentAction) {
                AiAction.GENERATE -> {
                    OutlinedTextField(
                        value = promptText,
                        onValueChange = { promptText = it },
                        label = { Text(stringResource(R.string.ai_prompt_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6
                    )
                }
                AiAction.SUMMARIZE -> {
                    if (hasSelection) {
                        Text(
                            text = stringResource(R.string.ai_summarize_selection),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.ai_summarize_full),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                AiAction.REWRITE -> {
                    RewriteStyleSelector(
                        currentStyle = rewriteStyle,
                        onStyleSelected = { rewriteStyle = it }
                    )
                }
                AiAction.TRANSLATE -> {
                    LanguageSelector(
                        currentLanguage = targetLanguage,
                        onLanguageSelected = { targetLanguage = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Execute button
            Button(
                onClick = {
                    val request = AiRequest(
                        action = currentAction,
                        prompt = promptText,
                        selectedText = selectedText,
                        context = fullContent,
                        rewriteStyle = rewriteStyle,
                        targetLanguage = targetLanguage
                    )
                    viewModel.execute(request)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing && (currentAction != AiAction.GENERATE || promptText.isNotBlank())
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    when (currentAction) {
                        AiAction.GENERATE -> stringResource(R.string.ai_generate)
                        AiAction.SUMMARIZE -> stringResource(R.string.ai_summarize)
                        AiAction.REWRITE -> stringResource(R.string.ai_rewrite)
                        AiAction.TRANSLATE -> stringResource(R.string.ai_translate)
                    }
                )
            }

            // Result area
            val displayText = resultText ?: errorMessage
            if (displayText != null) {
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val isError = errorMessage != null
                        if (isError) {
                            Text(
                                text = stringResource(R.string.ai_error),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if (!isError) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val request = AiRequest(
                                            action = currentAction,
                                            prompt = promptText,
                                            selectedText = selectedText,
                                            context = fullContent,
                                            rewriteStyle = rewriteStyle,
                                            targetLanguage = targetLanguage
                                        )
                                        viewModel.execute(request)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.ai_regenerate))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        onInsert(displayText)
                                        viewModel.clearResult()
                                        onDismiss()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.ai_insert))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionChipRow(
    currentAction: AiAction,
    onActionSelected: (AiAction) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        FilterChip(
            selected = currentAction == AiAction.GENERATE,
            onClick = { onActionSelected(AiAction.GENERATE) },
            label = { Text("Write") },
            leadingIcon = {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        )
        FilterChip(
            selected = currentAction == AiAction.SUMMARIZE,
            onClick = { onActionSelected(AiAction.SUMMARIZE) },
            label = { Text(stringResource(R.string.ai_summarize)) },
            leadingIcon = {
                Icon(
                    Icons.Default.Summarize,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        )
        FilterChip(
            selected = currentAction == AiAction.REWRITE,
            onClick = { onActionSelected(AiAction.REWRITE) },
            label = { Text(stringResource(R.string.ai_rewrite)) },
            leadingIcon = {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        )
        FilterChip(
            selected = currentAction == AiAction.TRANSLATE,
            onClick = { onActionSelected(AiAction.TRANSLATE) },
            label = { Text(stringResource(R.string.ai_translate)) },
            leadingIcon = {
                Icon(
                    Icons.Default.Translate,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        )
    }
}

@Composable
private fun RewriteStyleSelector(
    currentStyle: RewriteStyle,
    onStyleSelected: (RewriteStyle) -> Unit
) {
    Column {
        Text(
            text = stringResource(R.string.ai_style_label),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            RewriteStyle.entries.forEach { style ->
                FilterChip(
                    selected = currentStyle == style,
                    onClick = { onStyleSelected(style) },
                    label = {
                        Text(
                            when (style) {
                                RewriteStyle.FORMAL -> stringResource(R.string.ai_style_formal)
                                RewriteStyle.CASUAL -> stringResource(R.string.ai_style_casual)
                                RewriteStyle.POETIC -> stringResource(R.string.ai_style_poetic)
                                RewriteStyle.PROFESSIONAL -> stringResource(R.string.ai_style_professional)
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun LanguageSelector(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit
) {
    Column {
        Text(
            text = stringResource(R.string.ai_language_target),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf(
                "en" to "English",
                "es" to "Español",
                "pt" to "Português",
                "fr" to "Français",
                "de" to "Deutsch",
                "it" to "Italiano"
            ).forEach { (code, label) ->
                FilterChip(
                    selected = currentLanguage == code,
                    onClick = { onLanguageSelected(code) },
                    label = { Text(label) }
                )
            }
        }
    }
}
