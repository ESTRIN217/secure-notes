package com.example.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.ai.*
import com.example.ui.viewmodel.AiViewModel
import com.example.ui.viewmodel.ConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(
    aiViewModel: AiViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val aiEnabled by aiViewModel.aiEnabled.collectAsStateWithLifecycle()
    val aiBackend by aiViewModel.backend.collectAsStateWithLifecycle()
    val aiEndpointUrl by aiViewModel.endpointUrl.collectAsStateWithLifecycle()
    val aiModelName by aiViewModel.modelName.collectAsStateWithLifecycle()
    val aiConnectionState by aiViewModel.connectionState.collectAsStateWithLifecycle()
    val systemPrompt by aiViewModel.systemPrompt.collectAsStateWithLifecycle()

    var showAiBackendSheet by remember { mutableStateOf(false) }
    var editingUrl by remember(aiEndpointUrl) { mutableStateOf(aiEndpointUrl) }
    var editingModel by remember(aiModelName) { mutableStateOf(aiModelName) }
    var editingSystemPrompt by remember(systemPrompt) { mutableStateOf(systemPrompt) }

    val deviceInfo = aiViewModel.deviceInfo
    val recommendedModels = aiViewModel.recommendedModels
    val bestModel = aiViewModel.bestModel

    Scaffold(
        topBar = {
          TopAppBar(
            title = {
              Text(
                        text = stringResource(R.string.ai_settings_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
            },
            navigationIcon = {
              IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
              }
            },
            actions = {}
          )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsSectionTitle(title = stringResource(R.string.ai_section))
                SettingsCardGroup {
                    SettingsSwitchTile(
                        title = stringResource(R.string.ai_enabled),
                        subtitle = stringResource(R.string.ai_enabled_desc),
                        icon = Icons.Default.Psychology,
                        checked = aiEnabled,
                        onCheckedChange = { aiViewModel.setAiEnabled(it) }
                    )
                }
            }

            if (aiEnabled) {
                item {
                    SettingsCardGroup {
                        val backendLabel = when (aiBackend) {
                            AiBackend.OLLAMA -> stringResource(R.string.ai_ollama)
                            AiBackend.ON_DEVICE -> stringResource(R.string.ai_ondevice)
                        }
                        SettingsListTile(
                            leadingIcon = Icons.Default.Settings,
                            title = stringResource(R.string.ai_backend),
                            subtitle = backendLabel,
                            trailingIcon = Icons.Default.ChevronRight,
                            onClick = { showAiBackendSheet = true }
                        )
                    }
                }

                if (aiBackend == AiBackend.OLLAMA) {
                    ollamaConfigSection(
                        editingUrl = editingUrl,
                        onEditingUrlChange = { editingUrl = it },
                        aiViewModel = aiViewModel,
                        editingModel = editingModel,
                        onEditingModelChange = { editingModel = it },
                        connState = aiConnectionState
                    )
                }

                if (aiBackend == AiBackend.ON_DEVICE) {
                    onDeviceDeviceInfoSection(deviceInfo)
                    onDeviceModelsSection(
                        aiViewModel = aiViewModel,
                        models = recommendedModels,
                        bestModelId = bestModel?.id,
                        deviceInfo = deviceInfo
                    )
                }

                item {
                    SettingsSectionTitle(title = stringResource(R.string.ai_sampling_params))
                    SettingsCardGroup {
                        Column(modifier = Modifier.padding(20.dp)) {
                            val temp by aiViewModel.temperature.collectAsStateWithLifecycle()
                            val tk by aiViewModel.topK.collectAsStateWithLifecycle()
                            val tp by aiViewModel.topP.collectAsStateWithLifecycle()
                            val rp by aiViewModel.repetitionPenalty.collectAsStateWithLifecycle()
                            val mt by aiViewModel.maxTokens.collectAsStateWithLifecycle()

                            ParamSlider(
                                label = stringResource(R.string.ai_temperature),
                                desc = stringResource(R.string.ai_temperature_desc),
                                value = temp, range = 0f..2f, steps = 20,
                                onValueChange = { aiViewModel.setTemperature(it) }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            ParamSlider(
                                label = stringResource(R.string.ai_top_k),
                                desc = stringResource(R.string.ai_top_k_desc),
                                value = tk.toFloat(), range = 0f..100f, steps = 20,
                                onValueChange = { aiViewModel.setTopK(it.toInt()) }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            ParamSlider(
                                label = stringResource(R.string.ai_top_p),
                                desc = stringResource(R.string.ai_top_p_desc),
                                value = tp, range = 0f..1f, steps = 20,
                                onValueChange = { aiViewModel.setTopP(it) }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            ParamSlider(
                                label = stringResource(R.string.ai_repetition_penalty),
                                desc = stringResource(R.string.ai_repetition_penalty_desc),
                                value = rp, range = 1f..2f, steps = 20,
                                onValueChange = { aiViewModel.setRepetitionPenalty(it) }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            ParamSlider(
                                label = stringResource(R.string.ai_max_tokens),
                                desc = stringResource(R.string.ai_max_tokens_desc),
                                value = mt.toFloat(), range = 64f..4096f, steps = 63,
                                onValueChange = { aiViewModel.setMaxTokens(it.toInt()) }
                            )
                        }
                    }
                }

                item {
                    SettingsSectionTitle(title = stringResource(R.string.ai_system_prompt))
                    SettingsCardGroup {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.ai_system_prompt_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = editingSystemPrompt,
                                onValueChange = { editingSystemPrompt = it },
                                label = { Text(stringResource(R.string.ai_system_prompt)) },
                                placeholder = { Text(stringResource(R.string.ai_system_prompt_hint)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 120.dp),
                                minLines = 4,
                                maxLines = 8
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        editingSystemPrompt = ""
                                        aiViewModel.setSystemPrompt("")
                                    }
                                ) {
                                    Text(stringResource(R.string.ai_system_prompt_clear))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { aiViewModel.setSystemPrompt(editingSystemPrompt) }
                                ) {
                                    Text(stringResource(R.string.btn_save))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAiBackendSheet) {
        AiBackendBottomSheet(
            currentBackend = aiBackend,
            onDismiss = { showAiBackendSheet = false },
            onBackendSelected = { backend ->
                aiViewModel.setBackend(backend)
                showAiBackendSheet = false
            }
        )
    }
}

private fun LazyListScope.ollamaConfigSection(
    editingUrl: String,
    onEditingUrlChange: (String) -> Unit,
    aiViewModel: AiViewModel,
    editingModel: String,
    onEditingModelChange: (String) -> Unit,
    connState: ConnectionState
) {
    item {
        SettingsCardGroup {
            OutlinedTextField(
                value = editingUrl,
                onValueChange = onEditingUrlChange,
                label = { Text(stringResource(R.string.ai_endpoint_url)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            )
            Button(
                onClick = { aiViewModel.setEndpointUrl(editingUrl) },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                Text(stringResource(R.string.btn_save))
            }

            if (editingUrl.startsWith("https://", ignoreCase = true)) {
                Text(
                    text = stringResource(R.string.ai_https_self_signed_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            OutlinedTextField(
                value = editingModel,
                onValueChange = onEditingModelChange,
                label = { Text(stringResource(R.string.ai_model_name)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            )
            Button(
                onClick = { aiViewModel.setModelName(editingModel) },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                Text(stringResource(R.string.btn_save))
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { aiViewModel.testConnection() },
                    enabled = connState !is ConnectionState.Testing
                ) {
                    if (connState is ConnectionState.Testing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Default.NetworkCheck,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(stringResource(R.string.ai_test_connection))
                }
            }

            when (val state = connState) {
                is ConnectionState.Connected -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.ai_connection_ok, state.models.take(3).joinToString(", ")),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                is ConnectionState.Failed -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.ai_connection_fail, state.error),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                else -> {}
            }

            Spacer(modifier = Modifier.height(4.dp))
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.ai_ollama_instructions_title),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.ai_ollama_instructions_step1),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.ai_ollama_instructions_step2),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.ai_ollama_instructions_step3),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

private fun LazyListScope.onDeviceDeviceInfoSection(deviceInfo: DeviceInfo) {
    item {
        SettingsSectionTitle(title = stringResource(R.string.ai_ondevice_device_info))
        SettingsCardGroup {
            Column(modifier = Modifier.padding(20.dp)) {
                DeviceInfoRow(
                    icon = Icons.Default.Memory,
                    label = stringResource(R.string.ai_ondevice_ram),
                    value = stringResource(R.string.ai_device_ram, deviceInfo.availableRamMb, deviceInfo.totalRamMb)
                )
                Spacer(modifier = Modifier.height(8.dp))
                DeviceInfoRow(
                    icon = Icons.Default.PhoneAndroid,
                    label = stringResource(R.string.ai_ondevice_architecture),
                    value = deviceInfo.supportedAbis.joinToString(", ")
                )
                Spacer(modifier = Modifier.height(8.dp))
                DeviceInfoRow(
                    icon = Icons.Default.Speed,
                    label = stringResource(R.string.ai_ondevice_class),
                    value = deviceInfo.deviceClass.name
                )
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.ai_ondevice_engine_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.ai_ondevice_source_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DeviceInfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun LazyListScope.onDeviceModelsSection(
    aiViewModel: AiViewModel,
    models: List<OnDeviceModel>,
    bestModelId: String?,
    deviceInfo: DeviceInfo
) {
    item {
        val selected by aiViewModel.selectedOnDeviceModel.collectAsStateWithLifecycle()
        val downloadState by aiViewModel.downloadState.collectAsStateWithLifecycle()
        val modelState by aiViewModel.onDeviceModelState.collectAsStateWithLifecycle()
        val loadedInfo by aiViewModel.onDeviceLoadedModelInfo.collectAsStateWithLifecycle()
        // Estado descargado reactivo: evita `File.exists()` por modelo y por recomposición.
        val downloadedIds by aiViewModel.downloadedModelIds.collectAsStateWithLifecycle()

        SettingsSectionTitle(title = stringResource(R.string.ai_ondevice_models))
        SettingsCardGroup {
            Column(modifier = Modifier.padding(8.dp)) {
                selected?.let { model ->
                    SelectedModelPanel(
                        model = model,
                        modelState = modelState,
                        downloadState = downloadState,
                        loadedInfo = loadedInfo,
                        isDownloaded = model.id in downloadedIds,
                        onDownload = { aiViewModel.downloadSelectedModel() },
                        onCancelDownload = { aiViewModel.cancelDownload() },
                        onDeleteModel = { aiViewModel.deleteDownloadedModel() },
                        onLoadModel = { aiViewModel.loadSelectedModel() },
                        onUnloadModel = { aiViewModel.unloadModel() }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
                ModelOptionsList(
                    models = models,
                    bestModelId = bestModelId,
                    selectedModelId = selected?.id,
                    availableRamMb = deviceInfo.availableRamMb,
                    onSelect = { aiViewModel.selectOnDeviceModel(it) },
                    isDownloaded = { it.id in downloadedIds }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectedModelPanel(
    model: OnDeviceModel,
    modelState: ModelState,
    downloadState: DownloadState,
    loadedInfo: LoadedModelInfo?,
    isDownloaded: Boolean,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onDeleteModel: () -> Unit,
    onLoadModel: () -> Unit,
    onUnloadModel: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ModelHeader(model)
            ModelSourceRow(model)
            Text(
                text = stringResource(R.string.ai_ondevice_lazy_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            DownloadStatusBlock(downloadState)
            ModelActionButtons(
                modelState = modelState,
                downloadState = downloadState,
                isDownloaded = isDownloaded,
                onDownload = onDownload,
                onCancelDownload = onCancelDownload,
                onDeleteModel = onDeleteModel,
                onLoadModel = onLoadModel,
                onUnloadModel = onUnloadModel
            )
            ModelStateStatus(model = model, modelState = modelState, loadedInfo = loadedInfo)
        }
    }
}

@Composable
private fun ModelHeader(model: OnDeviceModel) {
    Column {
        Text(
            text = model.displayName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Storage,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(
                    R.string.ai_model_meta,
                    model.fileSizeMb,
                    model.minRamMb,
                    model.recommendedRamMb
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (model.quantLabel.isNotEmpty()) {
            Text(
                text = stringResource(R.string.ai_model_format, model.quantLabel),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelSourceRow(model: OnDeviceModel) {
    val context = LocalContext.current
    Surface(
        onClick = { openExternalUrl(context, model.sourcePageUrl) },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CloudDownload,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.ai_model_source),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = model.sourceLabel,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = { copyToClipboard(context, model.sourcePageUrl) }) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = stringResource(R.string.ai_ondevice_copy_source),
                    modifier = Modifier.size(18.dp)
                )
            }
            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = stringResource(R.string.ai_ondevice_open_source),
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadStatusBlock(downloadState: DownloadState) {
    when (downloadState) {
        is DownloadState.Downloading -> {
            LinearWavyProgressIndicator(
                progress = { downloadState.progress },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = stringResource(
                    R.string.ai_download_progress,
                    downloadState.downloadedMb,
                    downloadState.totalMb,
                    (downloadState.progress * 100).toInt(),
                    downloadSpeedLabel(downloadState.speedBytesPerSec)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        is DownloadState.Completed -> StatusHint(
            icon = Icons.Default.CheckCircle,
            text = stringResource(R.string.ai_ondevice_download_complete),
            tint = MaterialTheme.colorScheme.primary
        )
        is DownloadState.Failed -> StatusHint(
            icon = Icons.Default.Error,
            text = stringResource(R.string.ai_ondevice_download_failed, downloadState.error),
            tint = MaterialTheme.colorScheme.error
        )
        DownloadState.Idle -> Unit
    }
}

@Composable
private fun downloadSpeedLabel(speedBytesPerSec: Long): String {
    return if (speedBytesPerSec >= 1_000_000) {
        stringResource(R.string.ai_download_speed_mb, speedBytesPerSec / 1_000_000.0)
    } else {
        stringResource(R.string.ai_download_speed_kb, speedBytesPerSec / 1_000.0)
    }
}

@Composable
private fun StatusHint(icon: ImageVector, text: String, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = tint)
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = tint)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelActionButtons(
    modelState: ModelState,
    downloadState: DownloadState,
    isDownloaded: Boolean,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onDeleteModel: () -> Unit,
    onLoadModel: () -> Unit,
    onUnloadModel: () -> Unit
) {
    if (!isDownloaded) {
        DownloadButtons(downloadState = downloadState, onDownload = onDownload, onCancel = onCancelDownload)
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ModelLifecycleButton(
            modelState = modelState,
            onLoadModel = onLoadModel,
            onUnloadModel = onUnloadModel,
            modifier = Modifier.weight(1f)
        )
        FilledTonalButton(
            onClick = onDeleteModel,
            colors = ButtonDefaults.filledTonalButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.ai_ondevice_delete))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DownloadButtons(downloadState: DownloadState, onDownload: () -> Unit, onCancel: () -> Unit) {
    val downloading = downloadState is DownloadState.Downloading
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = onDownload, enabled = !downloading, modifier = Modifier.weight(1f)) {
            if (downloading) {
                LoadingIndicator(modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(stringResource(R.string.ai_ondevice_download))
        }
        if (downloading) {
            OutlinedButton(onClick = onCancel) {
                Text(stringResource(R.string.cancel))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelLifecycleButton(
    modelState: ModelState,
    onLoadModel: () -> Unit,
    onUnloadModel: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (modelState) {
        ModelState.READY -> UnloadButton(onUnloadModel, modifier)
        ModelState.LOADING -> LoadingButton(modifier)
        ModelState.ERROR -> RetryButton(onLoadModel, modifier)
        else -> LoadButton(onLoadModel, modifier)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnloadButton(onUnloadModel: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onUnloadModel,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        modifier = modifier
    ) {
        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(stringResource(R.string.ai_ondevice_unload))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LoadingButton(modifier: Modifier = Modifier) {
    Button(onClick = {}, enabled = false, modifier = modifier) {
        LoadingIndicator(modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(R.string.ai_ondevice_loading))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RetryButton(onLoadModel: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onLoadModel, modifier = modifier) {
        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(stringResource(R.string.ai_ondevice_retry))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoadButton(onLoadModel: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onLoadModel, modifier = modifier) {
        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(stringResource(R.string.ai_ondevice_load))
    }
}

@Composable
private fun ModelStateStatus(model: OnDeviceModel, modelState: ModelState, loadedInfo: LoadedModelInfo?) {
    when (modelState) {
        ModelState.READY -> StatusHint(
            icon = Icons.Default.CheckCircle,
            text = stringResource(R.string.ai_ondevice_status_ready),
            tint = MaterialTheme.colorScheme.primary
        )
        ModelState.ERROR -> StatusHint(
            icon = Icons.Default.Error,
            text = stringResource(R.string.ai_ondevice_status_error),
            tint = MaterialTheme.colorScheme.error
        )
        else -> Unit
    }
    val loaded = loadedInfo?.model ?: return
    if (loaded.id == model.id) return
    Text(
        text = stringResource(R.string.ai_ondevice_loaded_other, loaded.displayName),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ModelOptionsList(
    models: List<OnDeviceModel>,
    bestModelId: String?,
    selectedModelId: String?,
    availableRamMb: Long,
    onSelect: (OnDeviceModel) -> Unit,
    isDownloaded: (OnDeviceModel) -> Boolean
) {
    if (models.isEmpty()) {
        Text(
            text = stringResource(R.string.ai_ondevice_no_compatible),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(16.dp)
        )
        return
    }
    models.forEach { model ->
        ModelOptionRow(
            model = model,
            isSelected = selectedModelId == model.id,
            isRecommended = bestModelId == model.id,
            isDownloaded = isDownloaded(model),
            isRamRisk = model.minRamMb > availableRamMb,
            onSelect = { onSelect(model) }
        )
        if (model != models.last()) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

@Composable
private fun ModelOptionRow(
    model: OnDeviceModel,
    isSelected: Boolean,
    isRecommended: Boolean,
    isDownloaded: Boolean,
    isRamRisk: Boolean,
    onSelect: () -> Unit
) {
    val highlight = isSelected || isRecommended
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onSelect)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = isSelected, onClick = onSelect)
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = model.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (highlight) FontWeight.SemiBold else FontWeight.Normal
            )
            Text(
                text = stringResource(R.string.ai_model_min_ram, model.fileSizeMb, model.minRamMb),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isRecommended) {
                SettingsBadge(
                    text = stringResource(R.string.ai_ondevice_recommended),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        if (isDownloaded) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = stringResource(R.string.ai_ondevice_download_complete),
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        if (isRamRisk) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = stringResource(R.string.ai_ondevice_oom_warning),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun openExternalUrl(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        .onFailure { Log.e("AiSettingsScreen", "openUrl failed: $url", it) }
}

private fun copyToClipboard(context: Context, url: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("model_source", url))
    Toast.makeText(context, context.getString(R.string.ai_ondevice_source_copied), Toast.LENGTH_SHORT).show()
}

@Composable
private fun ParamSlider(
    label: String,
    desc: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
            Text(
                text = String.format("%.2f", value),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        @Suppress("DEPRECATION")
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
