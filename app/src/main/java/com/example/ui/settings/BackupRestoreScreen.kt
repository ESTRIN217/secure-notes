package com.example.ui.settings

import android.accounts.Account
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.AppConstants
import com.example.R
import com.example.data.model.SyncStage
import com.example.ui.viewmodel.BackupViewModel
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.example.util.BackupAttachmentHelper
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    viewModel: BackupViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val credentialManager = remember { CredentialManager.create(context) }

    val signIn: () -> Unit = {
        scope.launch {
            try {
                val googleIdOption = GetSignInWithGoogleOption.Builder(
                    AppConstants.WEB_CLIENT_ID
                ).build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                val response = credentialManager.getCredential(context, request)
                val credential = response.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdTokenCredential = GoogleIdTokenCredential
                        .createFrom(credential.data)
                    val accountEmail = googleIdTokenCredential.id
                    withContext(Dispatchers.IO) {
                        val token = GoogleAuthUtil.getToken(
                            context,
                            Account(accountEmail, "com.google"),
                            "oauth2:https://www.googleapis.com/auth/drive.appdata"
                        )
                        withContext(Dispatchers.Main) {
                            viewModel.linkGoogleDrive(token, accountEmail)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("CloudSync", "Credential Manager failed", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.toast_auth_error), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.backup_restore_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
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
                // Cloud Section
                item {
                    SettingsSectionTitle(title = stringResource(R.string.backup_cloud_section))
                }
                item {
                    CloudSection(
                        isDriveLinked = uiState.isDriveLinked,
                        lastSyncTime = uiState.lastSyncTime,
                        syncStage = uiState.syncStage,
                        isEncrypted = uiState.isPasswordSet && uiState.encryptBackups,
                        backupSize = uiState.lastBackupSizeCloud,
                        onBackupCloud = { viewModel.backupToCloud() },
                        onRestoreCloud = { viewModel.restoreFromCloud() },
                        onLinkDrive = signIn,
                        onUnlinkDrive = { viewModel.unlinkDrive() }
                    )
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
                            try {
                                val includeAttachments = viewModel.cloudSyncManagerPublic.includeAttachments.value
                                val encrypt = uiState.encryptBackups && uiState.isPasswordSet
                                if (includeAttachments) {
                                    val json = viewModel.buildBackupJson(encrypt)
                                    val tempDir = File(context.cacheDir, "backup_attachments_${System.currentTimeMillis()}")
                                    tempDir.mkdirs()
                                    val tempAttachmentsDir = File(tempDir, "attachments")
                                    val rawNotes = viewModel.cloudSyncManagerPublic.rawNotes.value
                                    val allPathMaps = mutableMapOf<String, String>()
                                    rawNotes.forEach { note ->
                                        val pathMap = BackupAttachmentHelper.collectAndCopyAttachments(
                                            note.content, note.backgroundImagePath, context, tempAttachmentsDir
                                        )
                                        allPathMaps.putAll(pathMap)
                                    }
                                    val zipFile = File(tempDir, "secure_notes_backup.zip")
                                    BackupAttachmentHelper.buildBackupZip(json, allPathMaps, tempAttachmentsDir, zipFile)
                                    val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        putExtra(android.content.Intent.EXTRA_STREAM, androidx.core.content.FileProvider.getUriForFile(
                                            context, "${context.packageName}.fileprovider", zipFile
                                        ))
                                        type = "application/zip"
                                    }
                                    context.startActivity(android.content.Intent.createChooser(sendIntent, context.getString(R.string.backup_restore_title)))
                                } else {
                                    val json = viewModel.buildBackupJson(encrypt)
                                    val cacheFile = java.io.File(context.cacheDir, "secure_notes_backup.json")
                                    cacheFile.writeText(json)
                                    val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        putExtra(android.content.Intent.EXTRA_STREAM, androidx.core.content.FileProvider.getUriForFile(
                                            context, "${context.packageName}.fileprovider", cacheFile
                                        ))
                                        type = "application/json"
                                    }
                                    context.startActivity(android.content.Intent.createChooser(sendIntent, context.getString(R.string.backup_restore_title)))
                                }
                            } catch (e: Exception) {
                                // handled by snackbar
                            }
                        },
                        onRestoreBackup = { filePickerLauncher.launch("application/json") }
                    )
                }

                // Attachment Settings
                item {
                    SettingsSectionTitle(title = stringResource(R.string.attachment_settings_title))
                }
                item {
                    val includeAttachments by viewModel.cloudSyncManagerPublic.includeAttachments.collectAsState()
                    val copyAttachmentsLocal by viewModel.cloudSyncManagerPublic.copyAttachmentsLocal.collectAsState()
                    SettingsCardGroup {
                        Column(modifier = Modifier.padding(8.dp)) {
                            SettingsSwitchTile(
                                title = stringResource(R.string.include_attachments_backup),
                                subtitle = stringResource(R.string.include_attachments_backup_desc),
                                icon = Icons.Outlined.CloudUpload,
                                checked = includeAttachments,
                                onCheckedChange = { viewModel.cloudSyncManagerPublic.setIncludeAttachments(it) }
                            )
                            SettingsSwitchTile(
                                title = stringResource(R.string.copy_attachments_local),
                                subtitle = stringResource(R.string.copy_attachments_local_desc),
                                icon = Icons.Outlined.CloudDownload,
                                checked = copyAttachmentsLocal,
                                onCheckedChange = { viewModel.cloudSyncManagerPublic.setCopyAttachmentsLocal(it) }
                            )
                        }
                    }
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
}

// ---------------------------------------------------------------------------
// Cloud Section
// ---------------------------------------------------------------------------

@Composable
fun CloudSection(
    isDriveLinked: Boolean,
    lastSyncTime: String,
    syncStage: SyncStage = SyncStage.IDLE,
    isEncrypted: Boolean = false,
    backupSize: Long = 0L,
    onBackupCloud: () -> Unit,
    onRestoreCloud: () -> Unit,
    onLinkDrive: () -> Unit,
    onUnlinkDrive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val isSyncing = syncStage != SyncStage.IDLE

    SettingsCardGroup(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SettingsIconContainer(icon = Icons.Outlined.Cloud)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.title_cloud_sync),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        SettingsBadge(
                            text = if (isEncrypted) stringResource(R.string.backup_e2ee_badge)
                                   else stringResource(R.string.backup_e2ee_disabled_badge)
                        )
                    }
                    Text(
                        text = if (isDriveLinked) stringResource(R.string.drive_linked) else stringResource(R.string.drive_unlinked),
                        color = if (isDriveLinked) Color(0xFF42A5F5) else colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.last_synced, lastSyncTime),
                fontSize = 12.sp,
                color = colorScheme.onSurfaceVariant
            )
            if (backupSize > 0L) {
                Text(
                    text = stringResource(R.string.backup_size_label, formatSize(backupSize)),
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }

            if (isSyncing) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (syncStage) {
                        SyncStage.ENCRYPTING -> stringResource(R.string.sync_encrypting)
                        SyncStage.SEARCHING -> stringResource(R.string.sync_searching)
                        SyncStage.UPLOADING -> stringResource(R.string.sync_uploading)
                        SyncStage.DOWNLOADING -> stringResource(R.string.sync_downloading)
                        SyncStage.RESTORING -> stringResource(R.string.sync_restoring)
                        else -> ""
                    },
                    fontSize = 11.sp,
                    color = colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isDriveLinked) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBackupCloud,
                        modifier = Modifier.weight(1f),
                        enabled = !isSyncing
                    ) {
                        Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.backup_upload), fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = onRestoreCloud,
                        modifier = Modifier.weight(1f),
                        enabled = !isSyncing
                    ) {
                        Icon(Icons.Outlined.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.backup_download), fontSize = 13.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onUnlinkDrive,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.error),
                    enabled = !isSyncing
                ) {
                    Icon(Icons.Outlined.LinkOff, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_unlink_drive), fontSize = 13.sp)
                }
            } else {
                OutlinedButton(
                    onClick = onLinkDrive,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.CloudQueue, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_link_drive), fontSize = 13.sp)
                }
            }
        }
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

            if (isPasswordSet && encryptBackups) {
                SettingsSwitchTile(
                    title = stringResource(R.string.backup_auto_title),
                    subtitle = stringResource(R.string.backup_auto_desc),
                    icon = Icons.Outlined.Schedule,
                    checked = autoBackupEnabled,
                    onCheckedChange = onAutoBackupEnabledChange
                )
            }

            if (isPasswordSet && encryptBackups && autoBackupEnabled) {
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
                                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
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
                    text = stringResource(R.string.backup_size_label, formatSize(backupSize)),
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

private fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
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
