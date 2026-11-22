package com.example.ui

import android.content.ClipData
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ai.DownloadState
import com.example.data.ai.ModelState
import com.example.data.ai.OnDeviceModel
import com.example.ui.chat.ModelDotState
import com.example.ui.chat.ModelEngineAction
import com.example.ui.chat.formatTokens
import com.example.ui.chat.modelDotState
import com.example.ui.chat.modelEngineAction
import kotlinx.coroutines.launch

/**
 * Vista de detalles de un modelo on-device (spec 040, RF-281…RF-292). Sin VM ni disco: recibe el
 * plano [ModelInfoState] y callbacks del llamador; la decisión de cargar/descargar viaja por
 * eventos. El chat template se lee (si hace falta) al expandir la sección, nunca al abrir.
 */
data class ModelInfoState(
    val model: OnDeviceModel,
    /** Archivo en disco según el StateFlow recolectado (`downloadedIds`); nunca una comprobación de disco. */
    val isDownloaded: Boolean,
    /** Es el modelo seleccionado ∧ backend ON_DEVICE. */
    val isSelected: Boolean,
    /** Estado del motor on-device (`onDeviceModelState`). */
    val engineState: ModelState,
    /** La descarga en curso es de este modelo. */
    val isDownloadingThis: Boolean,
    /** Hay una descarga en curso (de cualquiera). */
    val isAnyDownloadActive: Boolean,
    /** Progreso de la descarga de este modelo (0…1) o `null`. */
    val downloadProgress: Float?,
    /** `context_length` del GGUF desde caché (spec 033) o `null` → «—». */
    val contextLength: Long?
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ModelInfoPanel(
    state: ModelInfoState,
    isLargeScreen: Boolean,
    onDismiss: () -> Unit,
    onLoad: () -> Unit,
    onUnload: () -> Unit,
    onDownload: () -> Unit,
    onNewChat: () -> Unit,
    /** Devuelve `true` si el archivo ya no existe; cierra la vista el llamador. */
    onDelete: () -> Boolean,
    loadChatTemplate: suspend (OnDeviceModel) -> String?,
    modifier: Modifier = Modifier
) {
    if (isLargeScreen) {
        SidePanel(
            state = state,
            onDismiss = onDismiss,
            onLoad = onLoad,
            onUnload = onUnload,
            onDownload = onDownload,
            onNewChat = onNewChat,
            onDelete = onDelete,
            loadChatTemplate = loadChatTemplate,
            modifier = modifier
        )
    } else {
        InfoSheet(
            state = state,
            onDismiss = onDismiss,
            onLoad = onLoad,
            onUnload = onUnload,
            onDownload = onDownload,
            onNewChat = onNewChat,
            onDelete = onDelete,
            loadChatTemplate = loadChatTemplate,
            modifier = modifier
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SidePanel(
    state: ModelInfoState,
    onDismiss: () -> Unit,
    onLoad: () -> Unit,
    onUnload: () -> Unit,
    onDownload: () -> Unit,
    onNewChat: () -> Unit,
    onDelete: () -> Boolean,
    loadChatTemplate: suspend (OnDeviceModel) -> String?,
    modifier: Modifier = Modifier
) {
    BackHandler { onDismiss() }
    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(onClick = onDismiss)
        )
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(420.dp)
                .fillMaxHeight()
        ) {
            ModelInfoContent(
                state = state,
                onClose = onDismiss,
                onLoad = onLoad,
                onUnload = onUnload,
                onDownload = onDownload,
                onNewChat = onNewChat,
                onDelete = onDelete,
                loadChatTemplate = loadChatTemplate
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun InfoSheet(
    state: ModelInfoState,
    onDismiss: () -> Unit,
    onLoad: () -> Unit,
    onUnload: () -> Unit,
    onDownload: () -> Unit,
    onNewChat: () -> Unit,
    onDelete: () -> Boolean,
    loadChatTemplate: suspend (OnDeviceModel) -> String?,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        ModelInfoContent(
            state = state,
            onClose = onDismiss,
            onLoad = onLoad,
            onUnload = onUnload,
            onDownload = onDownload,
            onNewChat = onNewChat,
            onDelete = onDelete,
            loadChatTemplate = loadChatTemplate,
            modifier = modifier
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ModelInfoContent(
    state: ModelInfoState,
    onClose: () -> Unit,
    onLoad: () -> Unit,
    onUnload: () -> Unit,
    onDownload: () -> Unit,
    onNewChat: () -> Unit,
    onDelete: () -> Boolean,
    loadChatTemplate: suspend (OnDeviceModel) -> String?,
    modifier: Modifier = Modifier
) {
    val model = state.model
    val clipboard = LocalClipboard.current
    val copyScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0] ?: java.util.Locale.getDefault()
    val dot = modelDotState(state.isSelected, state.engineState)
    val action = modelEngineAction(
        isDownloaded = state.isDownloaded,
        isDownloadingThis = state.isDownloadingThis,
        isAnyDownloadActive = state.isAnyDownloadActive,
        isSelected = state.isSelected,
        engineState = state.engineState
    )

    var templateExpanded by remember(model.id) { mutableStateOf(false) }
    var templateState by remember(model.id) { mutableStateOf<TemplateState>(TemplateState.Idle) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleteFailed by remember(model.id) { mutableStateOf(false) }

    LaunchedEffect(templateExpanded) {
        if (!templateExpanded || templateState != TemplateState.Idle) return@LaunchedEffect
        templateState = TemplateState.Loading
        templateState = try {
            loadChatTemplate(model)?.let { TemplateState.Loaded(it) } ?: TemplateState.Unavailable
        } catch (_: Exception) {
            TemplateState.Unavailable
        }
    }

    val visibleModel = buildString {
        append(model.huggingFaceRepo)
        if (model.quantLabel.isNotBlank()) append(" : ").append(model.quantLabel)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Header(model = model, onClose = onClose)
        StatusRow(dot = dot)
        Spacer(modifier = Modifier.height(16.dp))
        ButtonRow(
            action = action,
            isDownloaded = state.isDownloaded,
            isDownloadingThis = state.isDownloadingThis,
            downloadProgress = state.downloadProgress,
            onLoad = onLoad,
            onUnload = onUnload,
            onDownload = onDownload,
            onNewChat = onNewChat
        )
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        ModelRow(
            visibleText = visibleModel,
            onCopy = {
                copyScope.launch {
                    clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("secure_notes", visibleModel)))
                }
            }
        )
        HorizontalDivider()
        InfoRow(
            label = stringResource(R.string.chat_model_info_context_size),
            value = state.contextLength?.let { formatTokens(it, locale) } ?: "—"
        )
        HorizontalDivider()
        InfoRow(
            label = stringResource(R.string.chat_model_info_parameters),
            value = model.paramsLabel
        )
        if (model.quantLabel.isNotBlank()) {
            HorizontalDivider()
            QuantRow(quant = model.quantLabel)
        }
        Spacer(modifier = Modifier.height(8.dp))
        ChatTemplateSection(
            expanded = templateExpanded,
            onToggle = { templateExpanded = !templateExpanded },
            templateState = templateState
        )
        if (state.isDownloaded) {
            Spacer(modifier = Modifier.height(16.dp))
            DeleteDiskRow(onClick = { showDeleteConfirm = true })
        }
        if (deleteFailed) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.ai_ondevice_delete_failed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.models_delete_title)) },
            text = { Text(stringResource(R.string.models_delete_body, model.displayName)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    if (onDelete()) onClose() else deleteFailed = true
                }) {
                    Text(stringResource(R.string.models_delete_disk))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun Header(model: OnDeviceModel, onClose: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = model.displayName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 6.dp)
            ) {
                Chip(model.paramsLabel)
                if (model.quantLabel.isNotBlank()) Chip(model.quantLabel)
            }
        }
        IconButton(onClick = onClose) {
            Icon(
                Icons.Default.Close,
                contentDescription = stringResource(R.string.close),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Chip(label: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun StatusRow(dot: ModelDotState) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 16.dp)
    ) {
        if (dot == ModelDotState.LOADING) {
            CircularProgressIndicator(
                modifier = Modifier.size(10.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        if (dot == ModelDotState.LOADED) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(
                when (dot) {
                    ModelDotState.LOADED -> R.string.models_loaded
                    ModelDotState.LOADING -> R.string.ai_ondevice_loading
                    ModelDotState.NOT_LOADED -> R.string.models_not_loaded
                }
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ButtonRow(
    action: ModelEngineAction,
    isDownloaded: Boolean,
    isDownloadingThis: Boolean,
    downloadProgress: Float?,
    onLoad: () -> Unit,
    onUnload: () -> Unit,
    onDownload: () -> Unit,
    onNewChat: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Button(
            onClick = onNewChat,
            enabled = isDownloaded,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.filledTonalButtonColors()
        ) {
            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.chat_new))
        }
        Spacer(modifier = Modifier.width(8.dp))
        LifecycleButton(
            action = action,
            isDownloadingThis = isDownloadingThis,
            downloadProgress = downloadProgress,
            onLoad = onLoad,
            onUnload = onUnload,
            onDownload = onDownload,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LifecycleButton(
    action: ModelEngineAction,
    isDownloadingThis: Boolean,
    downloadProgress: Float?,
    onLoad: () -> Unit,
    onUnload: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (action) {
        ModelEngineAction.DOWNLOAD -> Button(onClick = onDownload, modifier = modifier) {
            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.ai_ondevice_download))
        }
        ModelEngineAction.DOWNLOAD_BUSY,
        ModelEngineAction.DOWNLOADING_THIS -> Button(onClick = {}, enabled = false, modifier = modifier) {
            if (isDownloadingThis && downloadProgress != null) {
                LinearWavyProgressIndicator(progress = { downloadProgress }, modifier = Modifier.size(22.dp))
            } else {
                LoadingIndicator(modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.ai_ondevice_download))
        }
        ModelEngineAction.LOAD -> Button(onClick = onLoad, modifier = modifier) {
            Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.ai_ondevice_load))
        }
        ModelEngineAction.RETRY -> Button(onClick = onLoad, modifier = modifier) {
            Text(stringResource(R.string.ai_ondevice_retry))
        }
        ModelEngineAction.LOADING -> Button(onClick = {}, enabled = false, modifier = modifier) {
            LoadingIndicator(modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.ai_ondevice_loading))
        }
        ModelEngineAction.UNLOAD -> Button(onClick = onUnload, modifier = modifier) {
            Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.ai_ondevice_unload))
        }
    }
}

@Composable
private fun ModelRow(visibleText: String, onCopy: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
    ) {
        Text(
            text = stringResource(R.string.models_col_model),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = visibleText,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onCopy) {
            Icon(
                Icons.Default.ContentCopy,
                contentDescription = stringResource(R.string.chat_model_info_copy_desc),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

@Composable
private fun QuantRow(quant: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp)
    ) {
        Text(
            text = stringResource(R.string.chat_model_info_quantization),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp)
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            Chip(quant)
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ChatTemplateSection(
    expanded: Boolean,
    onToggle: () -> Unit,
    templateState: TemplateState
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(vertical = 14.dp)
        ) {
            Text(
                text = stringResource(R.string.chat_model_info_chat_template),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AnimatedVisibility(visible = expanded) {
            when (templateState) {
                TemplateState.Idle, TemplateState.Loading -> Box(
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator(modifier = Modifier.size(28.dp))
                }
                TemplateState.Unavailable -> Text(
                    text = stringResource(R.string.chat_model_info_template_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                is TemplateState.Loaded -> Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = templateState.text,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DeleteDiskRow(onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp, vertical = 6.dp)) {
        Icon(
            Icons.Default.Delete,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.models_delete_disk),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private sealed interface TemplateState {
    data object Idle : TemplateState
    data object Loading : TemplateState
    data class Loaded(val text: String) : TemplateState
    data object Unavailable : TemplateState
}
