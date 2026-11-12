package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ai.AiBackend
import com.example.data.ai.FileAttachment
import com.example.data.ai.MODEL_CATALOG
import com.example.data.ai.OnDeviceModel
import com.example.data.ai.ReasoningEffort
import com.example.ui.chat.ATTACHMENT_CHIP
import com.example.ui.chat.ChatHint
import com.example.ui.chat.chipKey
import com.example.ui.chat.isLargeScreenWidth
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
 *
 * El botón de **esfuerzo de razonamiento** (spec 033, RF-215…RF-217) monta aquí su hoja/menú —estado
 * transitorio de vista, como el pulso del botón de enviar— mientras el valor lo posee `AiChatScreen`
 * y llega por [reasoningEffort] (RF-33). El botón de **modelo** (RF-222) monta aquí el selector
 * ([ModelSelectorState] por parámetros, sin VM): la geometría de `DropdownMenu` exige que la hoja/el
 * menú vivan junto al botón.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
    reasoningEffort: ReasoningEffort,
    onReasoningEffortChange: (ReasoningEffort) -> Unit,
    maxTokensPref: Int,
    modelSelector: ModelSelectorState,
    onSelectOnDevice: (OnDeviceModel) -> Unit,
    onSelectOllama: (String) -> Unit,
    onTestConnection: () -> Unit,
    onToggleFavoriteModel: (String) -> Unit,
    onDownloadModel: (OnDeviceModel) -> Unit,
    onLoadModel: (OnDeviceModel) -> Unit,
    onUnloadModel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLargeScreen = remember(configuration.screenWidthDp) { isLargeScreenWidth(configuration.screenWidthDp) }
    var showEffortSheet by remember { mutableStateOf(false) }
    var showEffortMenu by remember { mutableStateOf(false) }
    var showModelSheet by remember { mutableStateOf(false) }
    var showModelMenu by remember { mutableStateOf(false) }

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
            ChatTextField(
                value = text,
                onValueChange = onTextChange,
                enabled = !isProcessing,
                hint = hint,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón de adjuntar (+) a la izquierda.
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
                ReasoningEffortButton(
                    effort = reasoningEffort,
                    isLargeScreen = isLargeScreen,
                    menuExpanded = showEffortMenu,
                    onExpandMenu = { showEffortMenu = !showEffortMenu },
                    onDismissMenu = { showEffortMenu = false },
                    onShowSheet = { showEffortSheet = true },
                    onSelect = onReasoningEffortChange
                )
                Spacer(modifier = Modifier.weight(1f))
                // El menú/hoja anclado vive aquí dentro (igual que el esfuerzo): la geometría
                // de `DropdownMenu` lo exige, y el campo sigue sin VM ni StateFlow.
                Box {
                    ModelPickerButton(
                        modelName = modelSelector.modelName,
                        onClick = { if (isLargeScreen) showModelMenu = !showModelMenu else showModelSheet = true }
                    )
                    ModelSelectorMenu(
                        expanded = showModelMenu,
                        onDismissRequest = { showModelMenu = false },
                        state = modelSelector,
                        onSelectOnDevice = onSelectOnDevice,
                        onSelectOllama = onSelectOllama,
                        onTestConnection = onTestConnection,
                        onToggleFavorite = onToggleFavoriteModel,
                        onDownloadModel = onDownloadModel,
                        onLoadModel = onLoadModel,
                        onUnloadModel = onUnloadModel
                    )
                }
                SendOrStopButton(
                    enabled = canSend || isProcessing,
                    isProcessing = isProcessing,
                    onClick = onSend
                )
            }
        }
    }

    if (showEffortSheet) {
        ReasoningEffortSheet(
            current = reasoningEffort,
            maxTokensPref = maxTokensPref,
            onSelect = onReasoningEffortChange,
            onDismiss = { showEffortSheet = false }
        )
    }
    if (showModelSheet) {
        ModelSelectorSheet(
            state = modelSelector,
            onSelectOnDevice = onSelectOnDevice,
            onSelectOllama = onSelectOllama,
            onTestConnection = onTestConnection,
            onToggleFavorite = onToggleFavoriteModel,
            onDownloadModel = onDownloadModel,
            onLoadModel = onLoadModel,
            onUnloadModel = onUnloadModel,
            onDismiss = { showModelSheet = false }
        )
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
 * lo que evita que se pierda texto tecleado durante la respuesta. Es un [BasicTextField] sin
 * contenedor ni indicador (nada que pintar transparente); en estado deshabilitado el texto se
 * atenúa explícitamente porque [BasicTextField] no aplica el alfa de deshabilitado por sí solo.
 */
@Composable
private fun ChatTextField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    hint: ChatHint,
    modifier: Modifier = Modifier
) {
    val textColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        modifier = modifier,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = textColor),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        singleLine = false,
        maxLines = 6,
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        text = hintText(hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                innerTextField()
            }
        }
    )
}

/**
 * Botón de esfuerzo de razonamiento (spec 033, RF-215…RF-217, RF-226). En pantallas grandes el
 * mismo botón despliega un menú anclado; en compacto abre una hoja. Sin nivel visible (DEFAULT)
 * pinta solo la bombilla; con nivel, icono + etiqueta corta en una línea.
 *
 * Sigue habilitado con `isProcessing`: el cambio aplica a la siguiente petición (RF-226).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReasoningEffortButton(
    effort: ReasoningEffort,
    isLargeScreen: Boolean,
    menuExpanded: Boolean,
    onExpandMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onShowSheet: () -> Unit,
    onSelect: (ReasoningEffort) -> Unit
) {
    val label = stringResource(effortLabelKey(effort))
    val isDefault = effort == ReasoningEffort.DEFAULT
    val effortDesc = stringResource(R.string.chat_effort_content_desc, label)
    Box {
        Surface(
            onClick = { if (isLargeScreen) onExpandMenu() else onShowSheet() },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.semantics {
                contentDescription = effortDesc
            }.height(32.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                if (!isDefault) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
        DropdownMenu(expanded = menuExpanded, onDismissRequest = onDismissMenu) {
            ReasoningEffort.entries.forEach { option ->
                val selected = option == effort
                DropdownMenuItem(
                    text = { Text(stringResource(effortLabelKey(option))) },
                    leadingIcon = {
                        if (selected) {
                            Icon(Icons.Default.Check, contentDescription = null)
                        }
                    },
                    onClick = {
                        onDismissMenu()
                        onSelect(option)
                    }
                )
            }
        }
    }
}

/**
 * Botón del modelo activo (spec 033, RF-222): nombre en una línea con elipsis (o «Sin modelo»).
 * El selector lo monta la pantalla (necesita el ViewModel); este botón solo lanza el evento.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelPickerButton(
    modelName: String,
    onClick: () -> Unit
) {
    val label = modelName.ifBlank { stringResource(R.string.chat_model_none) }
    val pickerDesc = stringResource(R.string.chat_model_picker_desc, label)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.semantics {
            contentDescription = pickerDesc
        }.height(32.dp).widthIn(max = 140.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Memory,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false).widthIn(max = 108.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
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

/**
 * Vista previa en Android Studio. Estado estático y plausible: texto tecleado, un adjunto,
 * esfuerzo MEDIUM y un modelo on-device seleccionado. Los callbacks no-op; ninguna decisión.
 */
@Preview(showBackground = true)
@Composable
fun FloatingChatInputFieldPreview() {
    MaterialTheme {
        FloatingChatInputField(
            attachments = listOf(
                FileAttachment(name = "resumen.md", content = "contenido de ejemplo", source = "external")
            ),
            text = "Resume este documento en tres puntos",
            onTextChange = {},
            onSend = {},
            canSend = true,
            isProcessing = false,
            hint = ChatHint.GENERATE,
            onRemoveAttachment = {},
            onAttachClick = {},
            reasoningEffort = ReasoningEffort.MEDIUM,
            onReasoningEffortChange = {},
            maxTokensPref = 2048,
            modelSelector = ModelSelectorState(
                backend = AiBackend.ON_DEVICE,
                selectedOnDeviceId = MODEL_CATALOG.first().id,
                downloadedIds = setOf(MODEL_CATALOG.first().id),
                catalogModels = MODEL_CATALOG,
                modelName = MODEL_CATALOG.first().displayName,
                ollamaModels = emptyList(),
                isTesting = false,
                connectionError = null,
                favorites = emptySet(),
                downloadingModelId = null
            ),
            onSelectOnDevice = {},
            onSelectOllama = {},
            onTestConnection = {},
            onToggleFavoriteModel = {},
            onDownloadModel = {},
            onLoadModel = {},
            onUnloadModel = {}
        )
    }
}
