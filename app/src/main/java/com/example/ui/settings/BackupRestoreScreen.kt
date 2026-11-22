package com.example.ui.settings

import androidx.compose.foundation.layout.WindowInsets
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.AppConstants
import com.example.R
import androidx.compose.material3.TopAppBar
import com.example.data.model.SyncStage
import com.example.ui.viewmodel.BackupViewModel

import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.example.util.BackupAttachmentHelper

import java.io.File
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    viewModel: BackupViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()


    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.restoreFromUri(it, context) }
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        if (uiState.isLoading) {
            SkeletonBody(modifier = Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
            ) {
                // Cloud Section (solo flavor full)
                if (com.example.BuildConfig.HAS_GOOGLE_DRIVE) {
                    item {
                        DriveCloudSection(viewModel = viewModel, uiState = uiState)
                    }
                }

                // Encryption & Auto-Backup Section
                item {
                    SettingsSectionTitle(title = stringResource(R.string.backup_encrypt_title))
                }
                item {
                    EncryptionSection(
                        isPasswordSet = uiState.isPasswordSet,
                        encryptBackups = uiState.encryptBackups,
                        onEncryptBackupsChange = { viewModel.cloudSyncManagerPublic.setEncryptBackups(it) },
                        autoBackupEnabled = uiState.autoBackupEnabled,
                        onAutoBackupEnabledChange = { viewModel.cloudSyncManagerPublic.setAutoBackupEnabled(it) },
                        autoBackupInterval = uiState.autoBackupInterval,
                        onAutoBackupIntervalChange = { viewModel.cloudSyncManagerPublic.setAutoBackupInterval(it) }
                    )
                }

                // Local Section
                item {
                    SettingsSectionTitle(title = stringResource(R.string.backup_local_section))
                }
                item {
                    LocalSection(
                        isEncrypted = uiState.isPasswordSet && uiState.encryptBackups,
                        backupSize = uiState.lastBackupSizeLocal,
                        onCreateBackup = {
                            scope.launch {
                                try {
                                    val encrypt = uiState.encryptBackups && uiState.isPasswordSet
                                    val exportsDir = File(context.cacheDir, "exports").also { it.mkdirs() }
                                    val tempDir = File(context.cacheDir, "backup_attachments_${System.currentTimeMillis()}")
                                    tempDir.mkdirs()
                                    val tempAttachmentsDir = File(tempDir, "attachments")
                                    val warnings = mutableListOf<String>()
                                    val allPathMaps: Map<String, String>
                                    try {
                                        val rawNotes = viewModel.cloudSyncManagerPublic.rawNotes.value
                                        val collectedMaps = mutableMapOf<String, String>()
                                        rawNotes.forEach { note ->
                                            val pathMap = BackupAttachmentHelper.collectAndCopyAttachments(
                                                note.content, note.backgroundImagePath, context, tempAttachmentsDir, warnings
                                            )
                                            collectedMaps.putAll(pathMap)
                                        }
                                        allPathMaps = collectedMaps
                                        val json = viewModel.buildBackupJson(encrypt, allPathMaps)
                                        val zipFile = File(exportsDir, "secure_notes_backup.zip")
                                        BackupAttachmentHelper.buildBackupZip(json, allPathMaps, tempAttachmentsDir, zipFile)
                                        val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            putExtra(android.content.Intent.EXTRA_STREAM, androidx.core.content.FileProvider.getUriForFile(
                                                context, "${context.packageName}.fileprovider", zipFile
                                            ))
                                            type = "application/zip"
                                        }
                                        context.startActivity(android.content.Intent.createChooser(sendIntent, context.getString(R.string.backup_restore_title)))
                                        if (warnings.isNotEmpty()) {
                                            viewModel.showSnackbarMessage(context.getString(R.string.backup_attachments_warnings, warnings.size))
                                        }
                                    } finally {
                                        tempDir.deleteRecursively()
                                    }
                                } catch (e: Exception) {
                                    viewModel.showSnackbarMessage(e.message ?: context.getString(R.string.toast_export_backup_error))
                                }
                            }
                        },
                        onRestoreBackup = { filePickerLauncher.launch(arrayOf("application/json", "application/zip")) }
                    )
                }
            }
        }
    }

    var restorePassword by remember { mutableStateOf("") }
    if (uiState.restorePasswordRequired) {
        AlertDialog(
            onDismissRequest = {
                restorePassword = ""
                viewModel.provideRestorePassword("")
            },
            title = { Text(stringResource(R.string.restore_password_dialog_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.restore_password_dialog_message))
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = restorePassword,
                        onValueChange = { restorePassword = it },
                        label = { Text(stringResource(R.string.password_hint)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.provideRestorePassword(restorePassword)
                        restorePassword = ""
                    }
                ) {
                    Text(stringResource(R.string.restore_password_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        restorePassword = ""
                        viewModel.provideRestorePassword("")
                    }
                ) {
                    Text(stringResource(R.string.restore_password_dialog_cancel))
                }
            }
        )
    }

    if (uiState.cloudBackupPromptVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissRestorePrompt() },
            icon = { Icon(Icons.Outlined.CloudDownload, contentDescription = null) },
            title = { Text(stringResource(R.string.cloud_backup_found_title)) },
            text = { Text(stringResource(R.string.cloud_backup_found_message)) },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmRestore() }) {
                    Text(stringResource(R.string.cloud_backup_restore_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissRestorePrompt() }) {
                    Text(stringResource(R.string.cloud_backup_restore_later))
                }
            }
        )
    }

    if (uiState.versionConflictVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissVersionConflict() },
            icon = { Icon(Icons.Outlined.SyncProblem, contentDescription = null) },
            title = { Text(stringResource(R.string.version_conflict_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.version_conflict_message,
                        formatBackupTime(uiState.cloudBackupTime),
                        formatBackupTime(uiState.localBackupTime),
                        formatBackupDiff(LocalContext.current, uiState.cloudBackupTime, uiState.localBackupTime)
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDownload() }) {
                    Text(stringResource(R.string.version_conflict_download))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.confirmOverwrite() }) {
                    Text(stringResource(R.string.version_conflict_overwrite))
                }
            }
        )
    }
}

// ---------------------------------------------------------------------------
// Encryption & Auto-Backup Section
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncryptionSection(
    isPasswordSet: Boolean,
    encryptBackups: Boolean,
    onEncryptBackupsChange: (Boolean) -> Unit,
    autoBackupEnabled: Boolean,
    onAutoBackupEnabledChange: (Boolean) -> Unit,
    autoBackupInterval: String,
    onAutoBackupIntervalChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    SettingsCardGroup(modifier = modifier) {
        Column(modifier = Modifier.padding(8.dp)) {
            if (isPasswordSet) {
                SettingsSwitchTile(
                    title = stringResource(R.string.backup_encrypt_title),
                    subtitle = stringResource(R.string.backup_encrypt_desc),
                    icon = Icons.Outlined.Lock,
                    checked = encryptBackups,
                    onCheckedChange = onEncryptBackupsChange
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsIconContainer(icon = Icons.Outlined.Lock)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(R.string.desc_e2e_encryption_disabled),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }

            if (com.example.BuildConfig.HAS_GOOGLE_DRIVE) {
                SettingsSwitchTile(
                    title = stringResource(R.string.backup_auto_title),
                    subtitle = stringResource(R.string.backup_auto_desc),
                    icon = Icons.Outlined.Schedule,
                    checked = autoBackupEnabled,
                    onCheckedChange = onAutoBackupEnabledChange
                )

                if (autoBackupEnabled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.backup_interval_label),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        val intervals = listOf("6h", "12h", "24h", "weekly")
                        val labels = mapOf(
                            "6h" to stringResource(R.string.backup_interval_6h),
                            "12h" to stringResource(R.string.backup_interval_12h),
                            "24h" to stringResource(R.string.backup_interval_24h),
                            "weekly" to stringResource(R.string.backup_interval_weekly)
                        )
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it }
                        ) {
                            OutlinedTextField(
                                value = labels[autoBackupInterval] ?: autoBackupInterval,
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier
                                    .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                                    .width(160.dp),
                                textStyle = MaterialTheme.typography.bodySmall,
                                singleLine = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                intervals.forEach { interval ->
                                    DropdownMenuItem(
                                        text = { Text(labels[interval] ?: interval) },
                                        onClick = {
                                            onAutoBackupIntervalChange(interval)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Local Section
// ---------------------------------------------------------------------------

@Composable
fun LocalSection(
    isEncrypted: Boolean = false,
    backupSize: Long = 0L,
    onCreateBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    SettingsCardGroup(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SettingsIconContainer(icon = Icons.Outlined.PhoneAndroid)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.backup_local_section),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        SettingsBadge(
                            text = if (isEncrypted) stringResource(R.string.backup_e2ee_badge)
                                   else stringResource(R.string.backup_e2ee_disabled_badge)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.backup_export_desc),
                fontSize = 12.sp,
                color = colorScheme.onSurfaceVariant
            )
            if (backupSize > 0L) {
                Text(
                    text = stringResource(R.string.backup_size_label, formatSize(LocalContext.current, backupSize)),
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCreateBackup,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.backup_export), fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = onRestoreBackup,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.backup_import), fontSize = 13.sp)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Skeleton Loading (Shimmer)
// ---------------------------------------------------------------------------

@Composable
fun SkeletonBody(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alphaAnim by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .alpha(alphaAnim)
    ) {
        SkeletonCard()
        Spacer(modifier = Modifier.height(16.dp))
        SkeletonCard()
    }
}

private fun formatSize(context: Context, bytes: Long): String {
    return when {
        bytes < 1024 -> context.getString(R.string.size_bytes, bytes)
        bytes < 1024 * 1024 -> context.getString(R.string.size_kb, bytes / 1024.0)
        else -> context.getString(R.string.size_mb, bytes / (1024.0 * 1024.0))
    }
}

private fun formatBackupTime(epochMillis: Long): String {
    if (epochMillis <= 0L) return ""
    val sdf = java.text.SimpleDateFormat("dd/MM/yy HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(epochMillis))
}

private fun formatBackupDiff(context: Context, cloudTime: Long, localTime: Long): String {
    val diff = Math.abs(cloudTime - localTime)
    val minutes = diff / 60000L
    return when {
        minutes < 60 -> context.getString(R.string.diff_minutes, minutes)
        minutes < 24 * 60 -> context.getString(R.string.diff_hours, minutes / 60.0)
        else -> context.getString(R.string.diff_days, minutes / (24.0 * 60.0))
    }
}

@Composable
fun SkeletonCard() {
    val placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)

    SettingsCardGroup {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(28.dp).background(placeholderColor, CircleShape))
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.size(140.dp, 20.dp).background(placeholderColor, RoundedCornerShape(4.dp)))
            }
            Spacer(modifier = Modifier.height(18.dp))
            Box(modifier = Modifier.fillMaxWidth().height(14.dp).background(placeholderColor, RoundedCornerShape(4.dp)))
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.size(200.dp, 12.dp).background(placeholderColor, RoundedCornerShape(4.dp)))
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f).height(40.dp).background(placeholderColor, RoundedCornerShape(20.dp)))
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.weight(1f).height(40.dp).background(placeholderColor, RoundedCornerShape(20.dp)))
            }
        }
    }
}
