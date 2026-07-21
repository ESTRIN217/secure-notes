package com.example.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.AppConstants
import com.example.R
import com.example.data.storage.StorageAnalyzer
import com.example.data.storage.StorageCategory
import com.example.data.storage.StorageItem
import com.example.data.storage.StorageOverview
import com.example.ui.CustomTopBar
import com.example.ui.viewmodel.StorageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageManagerScreen(
    viewModel: StorageViewModel,
    onBack: () -> Unit
) {
    val overview by viewModel.overview.collectAsStateWithLifecycle()
    val orphanFiles by viewModel.orphanFiles.collectAsStateWithLifecycle()
    val largeFiles by viewModel.largeFiles.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val autoCleanupEnabled by viewModel.autoCleanupEnabled.collectAsStateWithLifecycle()
    val lastCleanupMessage by viewModel.lastCleanupMessage.collectAsStateWithLifecycle()

    BackHandler(onBack = onBack)

    LaunchedEffect(Unit) {
        viewModel.scanStorage()
    }

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
                        text = stringResource(R.string.settings_storage_manager),
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
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            if (lastCleanupMessage != null) {
                item {
                    Snackbar(
                        modifier = Modifier.padding(16.dp),
                        action = {
                            TextButton(onClick = { viewModel.dismissCleanupMessage() }) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    ) {
                        Text(lastCleanupMessage ?: "")
                    }
                }
            }

            item {
                SettingsSectionTitle(title = stringResource(R.string.storage_overview))
            }

            if (isScanning) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                item {
                    StorageOverviewCard(overview)
                }

                if (orphanFiles.isNotEmpty()) {
                    item {
                        SettingsSectionTitle(title = stringResource(R.string.storage_orphan_files))
                    }
                    item {
                        OrphanFilesSection(
                            files = orphanFiles,
                            onDelete = { files -> viewModel.deleteFiles(files) }
                        )
                    }
                }

                if (largeFiles.isNotEmpty()) {
                    item {
                        SettingsSectionTitle(
                            title = stringResource(R.string.storage_large_files, AppConstants.LARGE_FILE_THRESHOLD_MB)
                        )
                    }
                    item {
                        LargeFilesSection(
                            files = largeFiles,
                            onDelete = { file -> viewModel.deleteFiles(listOf(file)) }
                        )
                    }
                }

                item {
                    SettingsSectionTitle(title = stringResource(R.string.settings_storage_tools))
                }
                item {
                    StorageToolsSection(
                        onClearCache = { viewModel.clearCache() },
                        onCleanUpNow = { viewModel.runAutoCleanupNow() },
                        autoCleanupEnabled = autoCleanupEnabled,
                        onAutoCleanupChange = { viewModel.setAutoCleanup(it) }
                    )
                }

                item {
                    SettingsSectionTitle(title = stringResource(R.string.storage_details))
                }
                item {
                    StorageDetailsCard(overview)
                }
            }
        }
    }
}

@Composable
private fun StorageOverviewCard(overview: StorageOverview) {
    SettingsCardGroup {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OverviewStat(
                    label = stringResource(R.string.storage_used),
                    value = StorageAnalyzer.formatSize(overview.totalUsed),
                    icon = Icons.Default.Storage,
                    color = MaterialTheme.colorScheme.primary
                )
                OverviewStat(
                    label = stringResource(R.string.storage_free),
                    value = if (overview.freeSpace >= 0) StorageAnalyzer.formatSize(overview.freeSpace) else "N/A",
                    icon = Icons.Default.SdStorage,
                    color = MaterialTheme.colorScheme.tertiary
                )
                if (overview.orphanCount > 0) {
                    OverviewStat(
                        label = stringResource(R.string.storage_orphans_short),
                        value = "${overview.orphanCount}",
                        icon = Icons.Default.Report,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = {
                    if (overview.totalUsed + overview.freeSpace > 0)
                        overview.totalUsed.toFloat() / (overview.totalUsed + overview.freeSpace).toFloat()
                    else 0f
                },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun OverviewStat(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun OrphanFilesSection(
    files: List<StorageItem>,
    onDelete: (List<StorageItem>) -> Unit
) {
    var selectedFiles by remember { mutableStateOf<Set<String>>(emptySet()) }

    SettingsCardGroup {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = stringResource(R.string.storage_orphan_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )

            files.take(50).forEach { file ->
                val isSelected = selectedFiles.contains(file.path)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedFiles = if (isSelected) selectedFiles - file.path
                            else selectedFiles + file.path
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = {
                            selectedFiles = if (isSelected) selectedFiles - file.path
                            else selectedFiles + file.path
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = file.name,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = StorageAnalyzer.formatSize(file.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (files.size > 50) {
                Text(
                    text = stringResource(R.string.storage_and_more, files.size - 50),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = {
                    selectedFiles = if (selectedFiles.size == files.size) emptySet()
                    else files.map { it.path }.toSet()
                }) {
                    Text(
                        if (selectedFiles.size == files.size) stringResource(R.string.storage_deselect_all)
                        else stringResource(R.string.storage_select_all)
                    )
                }
                Button(
                    onClick = {
                        val toDelete = files.filter { selectedFiles.contains(it.path) }
                        if (toDelete.isNotEmpty()) onDelete(toDelete)
                        selectedFiles = emptySet()
                    },
                    enabled = selectedFiles.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.storage_delete_selected, selectedFiles.size))
                }
            }
        }
    }
}

@Composable
private fun LargeFilesSection(
    files: List<StorageItem>,
    onDelete: (StorageItem) -> Unit
) {
    SettingsCardGroup {
        Column(modifier = Modifier.padding(8.dp)) {
            files.take(20).forEach { file ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (file.category) {
                            StorageCategory.ATTACHMENT -> Icons.Default.Image
                            StorageCategory.DRAWING -> Icons.Default.Draw
                            StorageCategory.VOICE -> Icons.Default.Mic
                            StorageCategory.FILE -> Icons.Default.AttachFile
                            StorageCategory.AI_MODEL -> Icons.Default.Memory
                            else -> Icons.AutoMirrored.Filled.InsertDriveFile
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = file.name,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = StorageAnalyzer.formatSize(file.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { onDelete(file) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (file != files.last()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StorageToolsSection(
    onClearCache: () -> Unit,
    onCleanUpNow: () -> Unit,
    autoCleanupEnabled: Boolean,
    onAutoCleanupChange: (Boolean) -> Unit
) {
    SettingsCardGroup {
        SettingsSwitchTile(
            title = stringResource(R.string.storage_auto_cleanup),
            subtitle = stringResource(R.string.storage_auto_cleanup_desc),
            icon = Icons.Default.CleaningServices,
            checked = autoCleanupEnabled,
            onCheckedChange = onAutoCleanupChange
        )

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onClearCache,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.storage_clear_cache))
            }
            Button(
                onClick = onCleanUpNow,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.AutoDelete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.storage_clean_up_now))
            }
        }
    }
}

@Composable
private fun StorageDetailsCard(overview: StorageOverview) {
    SettingsCardGroup {
        Column(modifier = Modifier.padding(16.dp)) {
            DetailRow(
                label = stringResource(R.string.storage_attachments),
                size = overview.attachmentsSize,
                total = overview.totalUsed
            )
            DetailRow(
                label = stringResource(R.string.storage_drawings),
                size = overview.drawingsSize,
                total = overview.totalUsed
            )
            DetailRow(
                label = stringResource(R.string.storage_voice),
                size = overview.voiceSize,
                total = overview.totalUsed
            )
            DetailRow(
                label = stringResource(R.string.storage_files),
                size = overview.filesSize,
                total = overview.totalUsed
            )
            DetailRow(
                label = stringResource(R.string.storage_cache),
                size = overview.cacheSize,
                total = overview.totalUsed
            )
            DetailRow(
                label = stringResource(R.string.storage_exports),
                size = overview.exportsSize,
                total = overview.totalUsed
            )
            DetailRow(
                label = stringResource(R.string.storage_temp),
                size = overview.tempSize,
                total = overview.totalUsed
            )
            DetailRow(
                label = stringResource(R.string.storage_database),
                size = overview.databaseSize,
                total = overview.totalUsed
            )
            if (overview.aiModelSize > 0) {
                DetailRow(
                    label = stringResource(R.string.storage_ai_models),
                    size = overview.aiModelSize,
                    total = overview.totalUsed
                )
            }
            if (overview.orphanSize > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                )
                DetailRow(
                    label = stringResource(R.string.storage_orphan_waste),
                    size = overview.orphanSize,
                    total = overview.totalUsed,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    size: Long,
    total: Long,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
        Text(
            text = "${StorageAnalyzer.formatSize(size)} (${StorageAnalyzer.formatPercentage(size, total)})",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
