package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ai.FileAttachment
import com.example.ui.chat.ATTACHMENT_CHIP
import com.example.ui.chat.ChatHint
import com.example.ui.chat.chipKey
import kotlinx.coroutines.delay

/**
 * Campo de entrada flotante del chat (RF-33). **Solo pinta y lanza eventos**: no recibe el
 * `AiViewModel` ni lee ningún `StateFlow` (decisión D-1 de `plan.md`).
 *
 * Lo que decide no está aquí: [canSend] es el veredicto de L4 —ya incluye los adjuntos pendientes y
 * la generación—, [hint] es el identificador que devuelve L4 y se traduce aquí con `stringResource`
 * (RF-33 c), y qué texto se muestra y a qué estado se escribe lo toma L6 en la pantalla (RF-33 e).
 * El tipo de contenido de las fichas lo declara **una sola vez** el archivo de lógica, no este
 * (RF-39).
 */
@Composable
fun FloatingChatInputField(
    attachments: List<FileAttachment>,
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    canSend: Boolean,
    isProcessing: Boolean,
    hint: ChatHint,
    onRemoveAttachment: (Int) -> Unit,
    onAttachClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    HorizontalFloatingToolbar(
        expanded = true,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(35.dp),
        colors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
        scrollBehavior = null
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            if (attachments.isNotEmpty()) {
                AttachmentChips(attachments = attachments, onRemove = onRemoveAttachment)
                Spacer(modifier = Modifier.height(4.dp))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
            ) {
                // Botón de adjuntar (+) a la izquierda; el menú lo abre la pantalla (es T9/T10).
                IconButton(
                    onClick = onAttachClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.attach),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                // El peso se resuelve aquí, dentro de la `Row`: `weight` es extensión de `RowScope`.
                ChatTextField(
                    value = text,
                    onValueChange = onTextChange,
                    enabled = !isProcessing,
                    hint = hint,
                    modifier = Modifier.weight(1f)
                )
                SendOrStopButton(
                    enabled = canSend || isProcessing,
                    isProcessing = isProcessing,
                    onClick = onSend
                )
            }
        }
    }
}

/**
 * Los cuatro textos de ayuda del campo, resueltos desde recursos (RF-33 c).
 *
 * La **decisión** de cuál corresponde la toma L4 ([com.example.ui.chat.hintFor]); aquí solo se
 * traduce el identificador. Ninguna clave se escribe como texto en el código.
 */
@Composable
private fun hintText(hint: ChatHint): String = stringResource(
    when (hint) {
        ChatHint.FOLLOWUP -> R.string.ai_chat_hint_followup
        ChatHint.GENERATE -> R.string.ai_chat_hint_generate
        ChatHint.SUMMARIZE -> R.string.ai_chat_hint_summarize
        ChatHint.FIX_GRAMMAR -> R.string.ai_chat_hint_fix_grammar
    }
)

/**
 * El campo de escritura (RF-33 b): **no acepta escritura mientras el modelo está generando**, que es
 * lo que evita que se pierda texto tecleado durante la respuesta. En ese estado el contenedor y el
 * indicador se ponen transparentes explícitamente: los valores por defecto de deshabilitado
 * pintarían un rectángulo gris dentro de la píldora.
 */
@Composable
private fun ChatTextField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    hint: ChatHint,
    modifier: Modifier = Modifier
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        placeholder = {
            Text(
                text = hintText(hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        },
        modifier = modifier,
        singleLine = false,
        maxLines = 6,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        )
    )
}

/**
 * Botón de enviar o detener, en una sola pieza (RF-33 a, g, j): habilitado con `canSend || isProcessing`
 * —L4 ya metió los adjuntos pendientes en `canSend`—, **color animado**, contenedor de error en modo
 * detener, vibración al enviar y **rebote de escala**.
 */
@Composable
private fun SendOrStopButton(
    enabled: Boolean,
    isProcessing: Boolean,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    // El rebote es estado de la vista, no de la pantalla: baja a 0.9f al pulsar y vuelve a 1f.
    var pulse by remember { mutableStateOf(1f) }
    val scale by animateFloatAsState(targetValue = pulse, label = "sendPulse")
    LaunchedEffect(pulse) {
        if (pulse < 1f) {
            delay(100)
            pulse = 1f
        }
    }
    val containerColor by animateColorAsState(
        targetValue = when {
            isProcessing -> MaterialTheme.colorScheme.errorContainer
            enabled -> MaterialTheme.colorScheme.primary
            else -> Color.Transparent
        },
        animationSpec = tween(200),
        label = "sendContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            isProcessing -> MaterialTheme.colorScheme.onErrorContainer
            enabled -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(200),
        label = "sendContent"
    )
    IconButton(
        onClick = {
            // La vibración es la del **envío**; detener no vibra, que es lo que hacía el campo anterior.
            if (!isProcessing) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            pulse = 0.9f
            onClick()
        },
        enabled = enabled,
        modifier = Modifier
            .size(30.dp)
            // El fondo va después de la capa, así que el rebote escala el círculo entero.
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .background(color = containerColor, shape = CircleShape)
    ) {
        Icon(
            imageVector = if (isProcessing) Icons.Default.Stop else Icons.AutoMirrored.Filled.Send,
            contentDescription = if (isProcessing) stringResource(R.string.ai_stop)
            else stringResource(R.string.ai_send),
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Las fichas de los adjuntos pendientes (RF-33 h): se quitan **una a una por su posición** y se pintan
 * con clave y tipo de contenido explícitos. La clave la compone [chipKey] con **posición y nombre**, que
 * es única por construcción, y [ATTACHMENT_CHIP] es la **única** declaración del repositorio (RF-39).
 */
@Composable
private fun AttachmentChips(
    attachments: List<FileAttachment>,
    onRemove: (Int) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(
            count = attachments.size,
            key = { index -> chipKey(index, attachments[index].name) },
            contentType = { ATTACHMENT_CHIP }
        ) { index ->
            val attachment = attachments[index]
            InputChip(
                selected = false,
                onClick = { onRemove(index) },
                label = {
                    Text(
                        text = attachment.name.take(20),
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                leadingIcon = {
                    // `source` de una nota empieza por "note" seguido del id ("note:12"), así que aquí
                    // no vale la constante de opción de L5, que es el id de la hoja.
                    if (attachment.source.startsWith("note")) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                },
                trailingIcon = {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.remove),
                        modifier = Modifier.size(14.dp)
                    )
                },
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}
