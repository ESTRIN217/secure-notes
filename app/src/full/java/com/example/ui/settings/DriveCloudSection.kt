package com.example.ui.settings

import android.accounts.Account
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.AppConstants
import com.example.R
import com.example.data.model.SyncStage
import com.example.ui.viewmodel.BackupUiState
import com.example.ui.viewmodel.BackupViewModel
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DriveCloudSection(
    viewModel: BackupViewModel,
    uiState: BackupUiState,
    modifier: Modifier = Modifier
) {
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
                    val pictureUri = googleIdTokenCredential.profilePictureUri?.toString() ?: ""
                    withContext(Dispatchers.IO) {
                        val token = GoogleAuthUtil.getToken(
                            context,
                            Account(accountEmail, "com.google"),
                            "oauth2:https://www.googleapis.com/auth/drive.appdata"
                        )
                        withContext(Dispatchers.Main) {
                            viewModel.linkGoogleDrive(token, accountEmail, pictureUri)
                        }
                    }
                }
            } catch (e: UserRecoverableAuthException) {
                android.util.Log.w("CloudSync", "NeedRemoteConsent — launching recovery intent")
                withContext(Dispatchers.Main) {
                    try {
                        context.startActivity(e.intent)
                    } catch (intentEx: Exception) {
                        Toast.makeText(context, context.getString(R.string.toast_auth_error), Toast.LENGTH_LONG).show()
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

    SettingsSectionTitle(title = stringResource(R.string.backup_cloud_section))
    CloudSection(
        isDriveLinked = uiState.isDriveLinked,
        lastSyncTime = uiState.lastSyncTime,
        syncStage = uiState.syncStage,
        isEncrypted = uiState.isPasswordSet && uiState.encryptBackups,
        backupSize = uiState.lastBackupSizeCloud,
        accountEmail = uiState.driveAccountEmail,
        profilePictureUri = uiState.driveProfilePictureUri,
        onBackupCloud = { viewModel.backupToCloud() },
        onRestoreCloud = { viewModel.checkCloudBeforeManualRestore() },
        onLinkDrive = signIn,
        onUnlinkDrive = { viewModel.unlinkDrive() },
        modifier = modifier
    )
}

@Composable
private fun CloudSection(
    isDriveLinked: Boolean,
    lastSyncTime: String,
    syncStage: SyncStage = SyncStage.IDLE,
    isEncrypted: Boolean = false,
    backupSize: Long = 0L,
    accountEmail: String? = null,
    profilePictureUri: String? = null,
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
                    if (isDriveLinked && accountEmail != null) {
                        Text(
                            text = accountEmail,
                            color = colorScheme.onSurface,
                            fontSize = 13.sp
                        )
                    } else {
                        Text(
                            text = if (isDriveLinked) stringResource(R.string.drive_linked) else stringResource(R.string.drive_unlinked),
                            color = if (isDriveLinked) Color(0xFF42A5F5) else colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
                if (isDriveLinked && accountEmail != null) {
                    val context = LocalContext.current
                    val imageRequest = remember(profilePictureUri) {
                        ImageRequest.Builder(context)
                            .data(profilePictureUri)
                            .crossfade(true)
                            .build()
                    }
                    AsyncImage(
                        model = imageRequest,
                        contentDescription = accountEmail,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
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
                    text = stringResource(R.string.backup_size_label, formatSize(context = LocalContext.current, bytes = backupSize)),
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

private fun formatSize(context: Context, bytes: Long): String {
    return when {
        bytes < 1024 -> context.getString(R.string.size_bytes, bytes)
        bytes < 1024 * 1024 -> context.getString(R.string.size_kb, bytes / 1024.0)
        else -> context.getString(R.string.size_mb, bytes / (1024.0 * 1024.0))
    }
}
