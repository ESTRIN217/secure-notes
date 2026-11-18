package com.example.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import androidx.compose.material3.*

/**
 * Submenú de herramientas del chat (spec 042, RF-304, RF-305): una segunda hoja modal «tonta» que
 * lista **todas** las herramientas registradas, cada una con su estado y un `Checkbox` para
 * cambiarlo.
 *
 * Es tonta (RNF-193): entran los datos y sale el toggle; ninguna escritura de preferencias ni lógica
 * de envío aquí — quien marca persiste **en el acto** vía `onToggle` (RF-305), sin paso de guardado.
 * El cambio rige a partir de la siguiente petición: la en vuelo no se toca (RF-306).
 *
 * Los nombres se enseñan **como dato** (D6), igual que los nombres de modelo (spec 033): son el
 * identificador de la API y decir exactamente qué se deshabilita es el punto del submenú. Solo el
 * chrome — título, mensaje de lista vacía — sale de recursos.
 *
 * Sin herramientas registradas, muestra `tools_empty` en vez de una hoja en blanco (RF-304).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsSheet(
    toolNames: List<String>,
    disabledTools: Set<String>,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.tools_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            )
            if (toolNames.isEmpty()) {
                ToolsEmptyMessage()
            } else {
                toolNames.forEach { nombre ->
                    ToolsRow(
                        name = nombre,
                        disabled = nombre in disabledTools,
                        onToggle = onToggle
                    )
                }
            }
        }
    }
}

/** Mensaje de lista vacía (RF-304): sin registro no hay hoja en blanco. */
@Composable
private fun ToolsEmptyMessage() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 24.dp)
    ) {
        Text(
            text = stringResource(R.string.tools_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Una fila por herramienta: nombre como dato a la izquierda y `Checkbox` con su estado a la derecha. */
@Composable
private fun ToolsRow(name: String, disabled: Boolean, onToggle: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(name) }
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Checkbox(
            checked = !disabled,
            onCheckedChange = { onToggle(name) }
        )
    }
}
