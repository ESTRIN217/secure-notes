package com.example.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EditNote
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
 * Hoja modal de adjuntos del chat: **exactamente cuatro** opciones — «Nota», «Archivo», «System
 * Message» y «Tools», en ese orden — y nada más (RF-300, enmienda a RF-34).
 *
 * Es **dueña de su propia hoja modal** (decisión D-6 de `plan.md`): quien la monta solo le pasa los
 * callbacks y no tiene que saber nada de ella, así que se puede descartar por sí sola.
 *
 * Las opciones de adjunto pasan por L5 (`attachmentSourceFor`): el `id` lo pone la propia hoja desde
 * las constantes, y la fuente que devuelve se reparte con un `when` **sin `else`**. Un `id`
 * desconocido devuelve `null` y ese `null` **no abre nada**, que es mejor que capturar una excepción
 * dentro de un `onClick`.
 *
 * **Orden de RF-35 extendido (RF-301, RF-304):** en las cuatro opciones va `onDismiss()` **antes**
 * del callback, para que la hoja desaparezca antes de abrir el selector, el editor del prompt o el
 * submenú de herramientas. Se escribe así a propósito, en el sitio, y no en una función auxiliar:
 * el orden es parte del requisito y tiene que leerse en el diff.
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
    onOpenSystemMessage: () -> Unit,
    onOpenTools: () -> Unit,
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
            NoteTile(onDismiss = onDismiss, onPickNote = onPickNote)
            FileTile(onDismiss = onDismiss, onPickFile = onPickFile)
            SystemMessageTile(onDismiss = onDismiss, onOpen = onOpenSystemMessage)
            ToolsTile(onDismiss = onDismiss, onOpen = onOpenTools)
        }
    }
}

/** Azulejo «Nota» (RF-300): la fuente la decide `attachmentSourceFor` (RF-34, intacto). */
@Composable
private fun NoteTile(onDismiss: () -> Unit, onPickNote: () -> Unit) {
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
}

/** Azulejo «Archivo» (RF-300): la fuente la decide `attachmentSourceFor` (RF-34, intacto). */
@Composable
private fun FileTile(onDismiss: () -> Unit, onPickFile: () -> Unit) {
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

/** Azulejo «System Message» (RF-300, RF-301): cierra la hoja antes de abrir el editor del prompt. */
@Composable
private fun SystemMessageTile(onDismiss: () -> Unit, onOpen: () -> Unit) {
    SettingsListTile(
        leadingIcon = Icons.Default.EditNote,
        title = stringResource(R.string.attach_system_message),
        subtitle = stringResource(R.string.attach_system_message_subtitle),
        trailingIcon = Icons.Default.ChevronRight,
        onClick = {
            onDismiss()
            onOpen()
        }
    )
}

/** Azulejo «Tools» (RF-300, RF-304): cierra la hoja antes de abrir el submenú de herramientas. */
@Composable
private fun ToolsTile(onDismiss: () -> Unit, onOpen: () -> Unit) {
    SettingsListTile(
        leadingIcon = Icons.Default.Build,
        title = stringResource(R.string.attach_tools),
        subtitle = stringResource(R.string.attach_tools_subtitle),
        trailingIcon = Icons.Default.ChevronRight,
        onClick = {
            onDismiss()
            onOpen()
        }
    )
}
