package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ai.AiAction

/**
 * La superficie de IA del editor de notas (spec 014, T7, RNF-43).
 *
 * Son tres cosas distintas que el editor pintaba en su propio cuerpo y que no tienen ninguna razón
 * para estar ahí: el **panel flotante** que se abre desde la barra, la **hoja de contexto** con las
 * acciones rápidas, y el **diálogo de progreso** que aparece mientras el modelo responde.
 *
 * **Ninguna declara estado.** Reciben lo que pintan por parámetro con nombre y devuelven lo que el
 * usuario hace como eventos; qué se hace con ellos —qué se le pasa al motor, a qué nota se guarda—
 * es cosa de la pantalla, que es quien tiene el estado.
 *
 * Los tres conservan el `if` de visibilidad **en el punto de llamada**, no aquí dentro. Es lo mismo
 * que se hizo con las hojas y por el mismo motivo: si el hijo se autocondicionara, su composición
 * seguiría viva con la superficie cerrada.
 */
@Composable
fun NoteEditorAiPanel(
    prompt: String,
    onPromptChange: (String) -> Unit,
    /** Acción sobre **toda** la nota: resumen o corrección. Cierra el panel. */
    onRunOverWholeNote: (AiAction) -> Unit,
    /** Abre el chat con la nota adjunta. Cierra el panel. */
    onOpenChat: () -> Unit,
    /** Lo pendiente de escribir, con lo seleccionado como destino. Cierra el panel. */
    onRunPrompt: () -> Unit,
    /** El motor está respondiendo: el botón de generar se apaga. */
    processing: Boolean,
    onDismiss: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 88.dp)
                .fillMaxWidth(0.92f),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("✨ ${stringResource(R.string.ai_assistant)}", style = MaterialTheme.typography.titleSmall)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = prompt,
                    onValueChange = onPromptChange,
                    placeholder = { Text(stringResource(R.string.ai_panel_placeholder), fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    textStyle = MaterialTheme.typography.bodySmall,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SuggestionChip(
                        onClick = { onRunOverWholeNote(AiAction.SUMMARIZE) },
                        label = { Text(stringResource(R.string.ai_summarize), fontSize = 11.sp) }
                    )
                    SuggestionChip(
                        onClick = { onRunOverWholeNote(AiAction.FIX_GRAMMAR) },
                        label = { Text(stringResource(R.string.ai_fix_grammar), fontSize = 11.sp) }
                    )
                    SuggestionChip(
                        onClick = onOpenChat,
                        label = { Text(stringResource(R.string.ai_panel_chat), fontSize = 11.sp) }
                    )
                }
                if (prompt.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onRunPrompt,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !processing
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.ai_generate), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

/**
 * La hoja de contexto: las dos acciones rápidas, resumir y corregir.
 *
 * Solo pinta dos botones. Lo que haya que pasarle al motor es cosa de la pantalla.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorAiContextSheet(
    onRunAction: (AiAction) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "✨ ${stringResource(R.string.ai_assistant)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            Spacer(Modifier.height(16.dp))

            Text(stringResource(R.string.ai_context_quick_actions), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = { onRunAction(AiAction.SUMMARIZE) },
                    label = { Text(stringResource(R.string.ai_chip_summarize), fontSize = 12.sp) },
                    leadingIcon = { }
                )
                AssistChip(
                    onClick = { onRunAction(AiAction.FIX_GRAMMAR) },
                    label = { Text(stringResource(R.string.ai_chip_fix_grammar), fontSize = 12.sp) },
                    leadingIcon = { }
                )
            }
        }
    }
}

/**
 * El diálogo que sale mientras el motor responde: qué está haciendo y lo que lleva dicho hasta ahora.
 *
 * Recibe **la acción**, no el texto: el rótulo de cada acción sale de `strings.xml` y el editor no
 * tiene por qué saber cómo se traduce «corrigiendo la gramática».
 */
@Composable
fun NoteEditorAiProgressDialog(
    action: AiAction,
    streamingText: String?,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.ai_progress_title), style = MaterialTheme.typography.titleSmall) },
        text = {
            Column {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text(
                    text = when (action) {
                        AiAction.SUMMARIZE -> stringResource(R.string.ai_progress_summarizing)
                        AiAction.FIX_GRAMMAR -> stringResource(R.string.ai_progress_fixing_grammar)
                        else -> stringResource(R.string.ai_generating)
                    },
                    style = MaterialTheme.typography.bodySmall
                )
                if (!streamingText.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = streamingText,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp),
                            maxLines = 8,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}