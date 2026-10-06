package com.example.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.chat.ATTACHMENT_FILE_ID
import com.example.ui.chat.ATTACHMENT_NOTE_ID
import com.example.ui.chat.AttachmentSource
import com.example.ui.chat.attachmentSourceFor
import com.example.ui.settings.SettingsListTile

/**
 * Hoja modal de adjuntos del chat: **exactamente dos** opciones, «Nota» y «Archivo» (RF-34).
 *
 * Es **dueña de su propia hoja modal** (decisión D-6 de `plan.md`): quien la monta solo le pasa los
 * tres callbacks y no tiene que saber nada de ella, así que se puede descartar por sí sola.
 *
 * Cada opción pasa por L5 (`attachmentSourceFor`): el `id` lo pone la propia hoja desde las
 * constantes, y la fuente que devuelve se reparte con un `when` **sin `else`**. Un `id` desconocido
 * devuelve `null` y ese `null` **no abre nada** (es lo que dejó anotado T5), que es mejor que
 * capturar una excepción dentro de un `onClick`.
 *
 * **Orden de RF-35:** en las dos opciones va `onDismiss()` **antes** del callback del selector, para
 * que la hoja desaparezca antes de abrir el otro selector. Se escribe así a propósito, en el sitio,
 * y no en una función auxiliar: el orden es parte del requisito y tiene que leerse en el diff.
 *
 * Ni foto ni cámara (decisión D1): los adjuntos llegan al modelo serializados como texto y el motor
 * en el dispositivo es solo de texto, así que ofrecerlas sería ofrecer opciones que no pueden
 * funcionar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentAiSheet(
    onPickNote: () -> Unit,
    onPickFile: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            SettingsListTile(
                leadingIcon = Icons.AutoMirrored.Filled.Note,
                title = stringResource(R.string.attach_note),
                subtitle = stringResource(R.string.attach_note_subtitle),
                trailingIcon = Icons.Default.ChevronRight,
                onClick = {
                    when (attachmentSourceFor(ATTACHMENT_NOTE_ID)) {
                        AttachmentSource.NOTE -> {
                            onDismiss()
                            onPickNote()
                        }
                        // La otra fuente y el `null` de un `id` desconocido no abren nada.
                        AttachmentSource.FILE -> Unit
                        null -> Unit
                    }
                }
            )
            SettingsListTile(
                leadingIcon = Icons.AutoMirrored.Filled.InsertDriveFile,
                title = stringResource(R.string.attach_file),
                subtitle = stringResource(R.string.attach_file_subtitle),
                trailingIcon = Icons.Default.ChevronRight,
                onClick = {
                    when (attachmentSourceFor(ATTACHMENT_FILE_ID)) {
                        AttachmentSource.FILE -> {
                            onDismiss()
                            onPickFile()
                        }
                        AttachmentSource.NOTE -> Unit
                        null -> Unit
                    }
                }
            )
        }
    }
}
