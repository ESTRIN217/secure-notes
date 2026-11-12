package com.example.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ai.ReasoningEffort

/**
 * Hoja «Reasoning effort» del chat (spec 033, RF-215/RF-217): seis opciones en orden
 * Default → Max; la seleccionada lleva una marca y su subtítulo lleva el tope.
 *
 * Es **dueña de su hoja** (patrón `AttachmentAiSheet`): la monta el campo de entrada con
 * todo por parámetros —esfuerzo actual, pref de tokens y callbacks—, sin ViewModel.
 * El modo compacto de presentación se decide fuera (pantalla/menú por ancho), no aquí.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReasoningEffortSheet(
    current: ReasoningEffort,
    maxTokensPref: Int,
    onSelect: (ReasoningEffort) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.chat_effort_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
            ReasoningEffort.entries.forEach { option ->
                EffortOptionRow(
                    option = option,
                    selected = option == current,
                    maxTokensPref = maxTokensPref,
                    onClick = {
                        onSelect(option)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun EffortOptionRow(
    option: ReasoningEffort,
    selected: Boolean,
    maxTokensPref: Int,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(effortLabelKey(option)),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = effortSubtitle(option, maxTokensPref),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

/** Etiqueta corta del esfuerzo (también usada por el botón): clave localizada, no literal. */
internal fun effortLabelKey(effort: ReasoningEffort): Int = when (effort) {
    ReasoningEffort.DEFAULT -> R.string.chat_effort_default
    ReasoningEffort.OFF -> R.string.chat_effort_off
    ReasoningEffort.LOW -> R.string.chat_effort_low
    ReasoningEffort.MEDIUM -> R.string.chat_effort_medium
    ReasoningEffort.HIGH -> R.string.chat_effort_high
    ReasoningEffort.MAX -> R.string.chat_effort_max
}

@Composable
private fun effortSubtitle(option: ReasoningEffort, maxTokensPref: Int): String = when (option) {
    ReasoningEffort.DEFAULT -> stringResource(R.string.chat_effort_app_setting, maxTokensPref)
    ReasoningEffort.OFF -> stringResource(R.string.chat_effort_off_desc, maxTokensPref)
    ReasoningEffort.LOW -> stringResource(R.string.chat_effort_tokens, 512)
    ReasoningEffort.MEDIUM -> stringResource(R.string.chat_effort_tokens, 2048)
    ReasoningEffort.HIGH -> stringResource(R.string.chat_effort_tokens, 8192)
    ReasoningEffort.MAX -> stringResource(R.string.chat_effort_unlimited)
}
