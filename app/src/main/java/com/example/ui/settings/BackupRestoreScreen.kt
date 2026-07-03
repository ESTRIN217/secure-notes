package com.example.ui.settings

import android.net.Uri
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
import com.example.R
import com.example.ui.viewmodel.BackupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    viewModel: BackupViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

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
                        onBackupCloud = { viewModel.backupToCloud() },
                        onRestoreCloud = { viewModel.restoreFromCloud() },
                        onLinkDrive = { viewModel.linkDrive() },
                        onUnlinkDrive = { viewModel.unlinkDrive() }
                    )
                }

                // Local Section
                item {
                    SettingsSectionTitle(title = stringResource(R.string.backup_local_section))
                }
                item {
                    LocalSection(
                        onCreateBackup = {
                            try {
                                val json = viewModel.buildBackupJson()
                                val cacheFile = java.io.File(context.cacheDir, "secure_notes_backup.json")
                                cacheFile.writeText(json)
                                val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    putExtra(android.content.Intent.EXTRA_STREAM, androidx.core.content.FileProvider.getUriForFile(
                                        context, "${context.packageName}.fileprovider", cacheFile
                                    ))
                                    type = "application/json"
                                }
                                context.startActivity(android.content.Intent.createChooser(sendIntent, context.getString(R.string.backup_restore_title)))
                            } catch (e: Exception) {
                                // handled by snackbar
                            }
                        },
                        onRestoreBackup = { filePickerLauncher.launch("application/json") }
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Cloud Section
// ---------------------------------------------------------------------------

@Composable
fun CloudSection(
    isDriveLinked: Boolean,
    lastSyncTime: String,
    onBackupCloud: () -> Unit,
    onRestoreCloud: () -> Unit,
    onLinkDrive: () -> Unit,
    onUnlinkDrive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    SettingsCardGroup(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SettingsIconContainer(icon = Icons.Outlined.Cloud)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.title_cloud_sync),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
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

            Spacer(modifier = Modifier.height(16.dp))

            if (isDriveLinked) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBackupCloud,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.backup_upload), fontSize = 13.sp)
                    }
                    OutlinedButton(
                        onClick = onRestoreCloud,
                        modifier = Modifier.weight(1f)
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
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colorScheme.error)
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
// Local Section
// ---------------------------------------------------------------------------

@Composable
fun LocalSection(
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
                Text(
                    text = stringResource(R.string.backup_local_section),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.backup_export_desc),
                fontSize = 12.sp,
                color = colorScheme.onSurfaceVariant
            )

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
