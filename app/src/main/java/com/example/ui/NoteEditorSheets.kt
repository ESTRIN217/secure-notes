package com.example.ui

import android.Manifest
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.R
import com.example.util.getNoteBackgroundColor
import java.io.File

/**
 * Las dos hojas del editor de notas (spec 014, T7, RNF-43).
 *
 * La **paleta** (color e imagen de fondo) y la **hoja de voz y archivo** (grabar una nota de voz,
 * insertar imagen o vídeo, elegir un archivo externo) vivían en el cuerpo del composable.
 *
 * **Por qué la hoja de voz se queda con su `if` en el punto de llamada** (decisión 7 del plan): los
 * `remember` de dentro —el grabador, el reproductor— no sobreviven al cierre de la hoja, y eso es lo
 * que evita que una grabación siga viva con la hoja cerrada. Si esta función se autocondicionara,
 * su composición seguiría existiendo y la grabación también. Quien tiene la bandera decide si existe.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorPaletteSheet(
    selectedColorId: Int?,
    onColorSelected: (Int?) -> Unit,
    backgroundImagePath: String?,
    onPickImage: () -> Unit,
    onClearImage: () -> Unit,
    isDark: Boolean,
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.option_note_styling),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = stringResource(id = R.string.label_note_color),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                (0..6).forEach { colorId ->
                    val isSelected = selectedColorId == colorId || (colorId == 0 && selectedColorId == null)
                    val colorLabel = when (colorId) {
                        0 -> stringResource(id = R.string.label_color_none)
                        1 -> stringResource(id = R.string.label_color_blue)
                        2 -> stringResource(id = R.string.label_color_green)
                        3 -> stringResource(id = R.string.label_color_yellow)
                        4 -> stringResource(id = R.string.label_color_pink)
                        5 -> stringResource(id = R.string.label_color_purple)
                        6 -> stringResource(id = R.string.label_color_orange)
                        else -> ""
                    }
                    val circleColor = if (colorId == 0) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        getNoteBackgroundColor(colorId, isDark)
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(circleColor)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = CircleShape
                            )
                            .clickable {
                                onColorSelected(if (colorId == 0) null else colorId)
                            }
                            .testTag("sheet_color_picker_$colorId"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (colorId == 0) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = colorLabel,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = colorLabel,
                                modifier = Modifier.size(18.dp),
                                tint = if (isDark) Color.White else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(id = R.string.option_select_bg_image),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onPickImage,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(id = R.string.option_select_bg_image), fontSize = 12.sp)
                }

                if (backgroundImagePath != null) {
                    IconButton(
                        onClick = onClearImage,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.errorContainer, CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(id = R.string.option_clear_bg_image),
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            backgroundImagePath?.let { bgPath ->
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedCard(
                    modifier = Modifier
                        .size(120.dp, 80.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    AsyncImage(
                        model = bgPath,
                        contentDescription = stringResource(id = R.string.cd_bg_preview),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

/**
 * La hoja de voz y archivo: imagen, vídeo, nota de voz y archivo externo.
 *
 * **El grabador y el reproductor viven aquí, no en la pantalla.** Es lo que hace que esta hoja tenga
 * frontera propia: son estado de la hoja, y se tira con ella.
 *
 * Lo que la hoja **no** decide es qué pasa con lo que el usuario eligió: eso son eventos que
 * devuelve, y los lleva la pantalla, que es quien tiene los bloques, el historial y el guardado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorVoiceFileSheet(
    noteId: Int,
    /** Abre el diálogo de imagen, en modo «nuevo» si no había bloque de imagen activo. */
    onInsertImage: () -> Unit,
    /** Abre el diálogo de vídeo, en modo «nuevo» si no había bloque de vídeo activo. */
    onInsertVideo: () -> Unit,
    onPickFile: () -> Unit,
    /** Adjunta la grabación y la guarda. Cierra la hoja. */
    onAttachRecording: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordedFile by remember { mutableStateOf<File?>(null) }
    var isPlayingRecording by remember { mutableStateOf(false) }
    var draftPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // Record Audio Permission launcher
    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val file = File(context.filesDir, "voice_${noteId}_${System.currentTimeMillis()}.3gp")
                recordedFile = file

                val recorderContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.createAttributionContext("microphone")
                } else {
                    context
                }
                val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(recorderContext)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                    setOutputFile(file.absolutePath)
                    prepare()
                    start()
                }
                mediaRecorder = recorder
                isRecording = true
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, context.getString(R.string.toast_recording_error) + ": ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, context.getString(R.string.mic_permission_required), Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (e: Exception) {
                    android.util.Log.e("NoteEditor", "media recorder stop failed", e)
                }
                release()
            }
            draftPlayer?.release()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.add_attachment),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            AttachmentOptionCard(
                icon = Icons.Default.Image,
                title = stringResource(id = R.string.rich_insert_image),
                description = stringResource(id = R.string.desc_insert_image),
                onClick = onInsertImage
            )

            AttachmentOptionCard(
                icon = Icons.Default.Videocam,
                title = stringResource(id = R.string.rich_insert_video),
                description = stringResource(id = R.string.desc_insert_video),
                onClick = onInsertVideo
            )

            VoiceRecorderPanel(
                isRecording = isRecording,
                recordedFile = recordedFile,
                isPlaying = isPlayingRecording,
                onRequestPermission = {
                    recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
                onStopRecording = {
                    try {
                        mediaRecorder?.apply {
                            stop()
                            release()
                        }
                        mediaRecorder = null
                        isRecording = false
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                onTogglePlayback = {
                    if (isPlayingRecording) {
                        draftPlayer?.stop()
                        draftPlayer?.release()
                        draftPlayer = null
                        isPlayingRecording = false
                    } else {
                        try {
                            val player = MediaPlayer().apply {
                                setDataSource(recordedFile!!.absolutePath)
                                prepare()
                                start()
                                setOnCompletionListener {
                                    isPlayingRecording = false
                                    release()
                                }
                            }
                            draftPlayer = player
                            isPlayingRecording = true
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, context.getString(R.string.toast_audio_preview_error), Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onAttach = {
                    onAttachRecording(recordedFile!!.absolutePath)
                    Toast.makeText(context, context.getString(R.string.toast_voice_note_attached), Toast.LENGTH_SHORT).show()
                },
                onDiscard = {
                    recordedFile?.delete()
                    recordedFile = null
                }
            )

            AttachmentOptionCard(
                icon = Icons.Default.FolderOpen,
                title = stringResource(R.string.select_external_file),
                description = stringResource(R.string.select_file_desc),
                onClick = onPickFile,
                centerContent = true
            )
        }
    }
}

/**
 * Una de las tarjetas de «insertar imagen» / «insertar vídeo» / «elegir archivo» de la hoja.
 *
 * Las tres tenían el mismo esqueleto y solo cambiaban icono, título y descripción. La de archivo
 * además centraba el contenido, y [centerContent] lo deja como estaba.
 */
@Composable
private fun AttachmentOptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    centerContent: Boolean = false
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (centerContent) Arrangement.Center else Arrangement.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * El panel de grabación: tres estados excluyentes —nada grabado, grabando, y la grabación a la
 * espera de adjuntarse— porque el `if/else if` que los separaba era la parte ilegible. Lo son por
 * construcción: el cuerpo solo pinta el que le toca.
 */
@Composable
private fun VoiceRecorderPanel(
    isRecording: Boolean,
    recordedFile: File?,
    isPlaying: Boolean,
    onRequestPermission: () -> Unit,
    onStopRecording: () -> Unit,
    onTogglePlayback: () -> Unit,
    onAttach: () -> Unit,
    onDiscard: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.voice_note_recorder),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )

            when {
                !isRecording && recordedFile == null -> RecordButton(onClick = onRequestPermission)
                isRecording -> StopButton(onClick = onStopRecording)
                else -> RecordedDraftRow(
                    isPlaying = isPlaying,
                    onTogglePlayback = onTogglePlayback,
                    onAttach = onAttach,
                    onDiscard = onDiscard
                )
            }
        }
    }
}

/** Estado inicial: un botón grande que pide el micrófono. */
@Composable
private fun RecordButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(64.dp)
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
            .testTag("start_record_btn")
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = stringResource(R.string.tap_to_record),
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(32.dp)
        )
    }
    Text(stringResource(R.string.tap_to_record), style = MaterialTheme.typography.bodySmall)
}

/** Grabando: el mismo botón, en rojo, y ahora para parar. */
@Composable
private fun StopButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(64.dp)
            .background(MaterialTheme.colorScheme.errorContainer, CircleShape)
            .testTag("stop_record_btn")
    ) {
        Icon(
            imageVector = Icons.Default.Stop,
            contentDescription = stringResource(R.string.cd_stop_recording),
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(32.dp)
        )
    }
    Text(stringResource(R.string.recording_tap_to_stop), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
}

/** Grabado y pendiente: reproducir, adjuntar o descartar. */
@Composable
private fun RecordedDraftRow(
    isPlaying: Boolean,
    onTogglePlayback: () -> Unit,
    onAttach: () -> Unit,
    onDiscard: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onTogglePlayback,
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                .testTag("play_preview_btn")
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = stringResource(R.string.cd_preview_voice_note),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }

        Button(
            onClick = onAttach,
            modifier = Modifier.testTag("attach_voice_btn")
        ) {
            Text(stringResource(R.string.attach_voice))
        }

        IconButton(
            onClick = onDiscard,
            modifier = Modifier.testTag("delete_recording_btn")
        ) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.cd_discard_recording), tint = MaterialTheme.colorScheme.error)
        }
    }
}