package com.example.ui.settings

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.ai.*
import com.example.ui.CustomTopBar
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
    val onDeviceState by aiViewModel.onDeviceModelState.collectAsStateWithLifecycle()
    val downloadState by aiViewModel.downloadState.collectAsStateWithLifecycle()
    val onDeviceLoadedInfo by aiViewModel.onDeviceLoadedModelInfo.collectAsStateWithLifecycle()

    var showAiBackendSheet by remember { mutableStateOf(false) }
    var editingUrl by remember(aiEndpointUrl) { mutableStateOf(aiEndpointUrl) }
    var editingModel by remember(aiModelName) { mutableStateOf(aiModelName) }
    var editingSystemPrompt by remember(systemPrompt) { mutableStateOf(systemPrompt) }

    val selectedModel by aiViewModel.selectedOnDeviceModel.collectAsStateWithLifecycle()
    val deviceInfo = aiViewModel.deviceInfo
    val recommendedModels = aiViewModel.recommendedModels
    val bestModel = aiViewModel.bestModel

    Scaffold(
        topBar = {
            CustomTopBar {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                    Text(
                        text = stringResource(R.string.ai_settings_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
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
                    onDeviceRecommendedSection(bestModel)
                    onDeviceAllModelsSection(
                        models = recommendedModels,
                        selectedModel = selectedModel,
                        deviceInfo = deviceInfo,
                        onSelectModel = { aiViewModel.selectOnDeviceModel(it) }
                    )
                    onDeviceActionsSection(
                        selectedModel = selectedModel,
                        downloadState = downloadState,
                        onDeviceState = onDeviceState,
                        onDownload = { aiViewModel.downloadSelectedModel() },
                        onCancelDownload = { aiViewModel.cancelDownload() },
                        onDeleteModel = { aiViewModel.deleteDownloadedModel() },
                        onLoadModel = { aiViewModel.loadSelectedModel() },
                        onUnloadModel = { aiViewModel.unloadModel() },
                        isModelDownloaded = { aiViewModel.isModelDownloaded(it) }
                    )
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
                    value = "${deviceInfo.availableRamMb} MB available / ${deviceInfo.totalRamMb} MB total"
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
            }
        }
    }
}

private fun LazyListScope.onDeviceRecommendedSection(bestModel: OnDeviceModel?) {
    item {
        SettingsSectionTitle(title = stringResource(R.string.ai_ondevice_recommended))
        SettingsCardGroup {
            if (bestModel != null) {
                RecommendedModelCard(model = bestModel)
            } else {
                Text(
                    text = stringResource(R.string.ai_ondevice_no_compatible),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(20.dp)
                )
            }
        }
    }
}

private fun LazyListScope.onDeviceAllModelsSection(
    models: List<OnDeviceModel>,
    selectedModel: OnDeviceModel?,
    deviceInfo: DeviceInfo,
    onSelectModel: (OnDeviceModel) -> Unit
) {
    if (models.isEmpty()) return
    item {
        SettingsSectionTitle(title = stringResource(R.string.ai_ondevice_all_models))
        SettingsCardGroup {
            Column(modifier = Modifier.padding(8.dp)) {
                models.forEach { model ->
                    ModelSelectionRow(
                        model = model,
                        isSelected = selectedModel?.id == model.id,
                        isCompatibleByRam = model.minRamMb <= deviceInfo.availableRamMb,
                        onSelect = { onSelectModel(model) }
                    )
                    if (model != models.last()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }
}

private fun LazyListScope.onDeviceActionsSection(
    selectedModel: OnDeviceModel?,
    downloadState: DownloadState,
    onDeviceState: ModelState,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onDeleteModel: () -> Unit,
    onLoadModel: () -> Unit,
    onUnloadModel: () -> Unit,
    isModelDownloaded: (OnDeviceModel) -> Boolean
) {
    val selected = selectedModel ?: return
    val downloaded = isModelDownloaded(selected)

    item {
        SettingsCardGroup {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = selected.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${selected.fileSizeMb}MB · Min ${selected.minRamMb}MB RAM · Rec ${selected.recommendedRamMb}MB RAM",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                when (val d = downloadState) {
                    is DownloadState.Downloading -> {
                        LinearProgressIndicator(
                            progress = { d.progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        val speedText = if (d.speedBytesPerSec >= 1_000_000) {
                            String.format("%.1f MB/s", d.speedBytesPerSec / 1_000_000.0)
                        } else {
                            String.format("%.0f KB/s", d.speedBytesPerSec / 1_000.0)
                        }
                        Text(
                            text = "${d.downloadedMb}MB / ${d.totalMb}MB (${(d.progress * 100).toInt()}%) · $speedText",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    is DownloadState.Completed -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.ai_ondevice_download_complete),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    is DownloadState.Failed -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.ai_ondevice_download_failed, d.error),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    else -> {}
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!downloaded) {
                        Button(
                            onClick = onDownload,
                            enabled = downloadState !is DownloadState.Downloading,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (downloadState is DownloadState.Downloading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.ai_ondevice_download))
                        }
                        if (downloadState is DownloadState.Downloading) {
                            OutlinedButton(onClick = onCancelDownload) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    } else {
                        when (onDeviceState) {
                            ModelState.READY -> {
                                Button(
                                    onClick = onUnloadModel,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.ai_ondevice_unload))
                                }
                            }
                            ModelState.LOADING -> {
                                Button(onClick = {}, enabled = false, modifier = Modifier.weight(1f)) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.ai_ondevice_loading))
                                }
                            }
                            ModelState.ERROR -> {
                                Button(onClick = onLoadModel, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.ai_ondevice_retry))
                                }
                            }
                            else -> {
                                Button(onClick = onLoadModel, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.ai_ondevice_load))
                                }
                            }
                        }
                        OutlinedButton(onClick = onDeleteModel) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                when (onDeviceState) {
                    ModelState.READY -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.ai_ondevice_status_ready),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    ModelState.ERROR -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.ai_ondevice_status_error),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    else -> {}
                }
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

@Composable
private fun RecommendedModelCard(model: OnDeviceModel) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 2.dp,
            brush = SolidColor(MaterialTheme.colorScheme.primary)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = model.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${model.fileSizeMb}MB · Needs ${model.minRamMb}MB RAM",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ModelSelectionRow(
    model: OnDeviceModel,
    isSelected: Boolean,
    isCompatibleByRam: Boolean,
    onSelect: () -> Unit
) {
    val statusColor = when {
        !isCompatibleByRam -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.tertiary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = isSelected, onClick = onSelect)
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = model.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
            Text(
                text = "${model.fileSizeMb}MB · Min ${model.minRamMb}MB RAM",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (!isCompatibleByRam) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = stringResource(R.string.ai_ondevice_oom_warning),
                tint = statusColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
