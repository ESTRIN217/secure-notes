package com.example.ui.chat

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R

/**
 * Editor del prompt personalizado desde la hoja de adjuntos (spec 042, RF-301…RF-303).
 *
 * Es **tonto** (RNF-193): entra el prompt actual y salen guardar/cancelar; cero lógica de envío y
 * cero escritura de preferencias aquí. Guardar **no envía ninguna petición** (RF-302): el cambio se
 * aplica a la siguiente petición por el camino de resolución de siempre — `_systemPrompt.value
 * .ifBlank { prompt por defecto de la acción }` — y con el motor on-device la recarga (releer pesos)
 * ocurre al **enviar**, por `ensureReady`, nunca al guardar (decisión D5).
 *
 * El aviso del siguiente mensaje y de la posible recarga es el texto de ayuda fijo del diálogo
 * (RF-303) y sale de recursos, como todo el chrome.
 *
 * Confirmar con el texto en **blanco** borra el prompt personalizado y devuelve el prompt por
 * defecto (RF-302): el caller decide con lo que recibe, aquí no se interpreta.
 */
@Composable
fun SystemMessageDialog(
    currentPrompt: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var texto by rememberSaveable { mutableStateOf(currentPrompt) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.system_message_title)) },
        text = {
            SystemMessageFields(
                texto = texto,
                onTextoChange = { texto = it }
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(texto) }) {
                Text(stringResource(R.string.system_message_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/** El campo multilínea y el aviso fijo de RF-303, para que el diálogo se lea de una pasada. */
@Composable
private fun SystemMessageFields(texto: String, onTextoChange: (String) -> Unit) {
    OutlinedTextField(
        value = texto,
        onValueChange = onTextoChange,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp),
        placeholder = { Text(stringResource(R.string.ai_system_prompt_hint)) },
        supportingText = {
            Text(
                text = stringResource(R.string.system_message_hint),
                style = MaterialTheme.typography.bodySmall
            )
        }
    )
}
