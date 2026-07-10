package com.example.ui

import android.accounts.Account
import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.viewmodel.NotesViewModel
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudSyncScreen(
    viewModel: NotesViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val isDriveLinked by viewModel.isDriveLinked.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()
    val syncStatusMessage by viewModel.syncStatusMessage.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val signInClient: GoogleSignInClient = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Scope("https://www.googleapis.com/auth/drive.file"))
            .requestEmail()
            .build()
            .let { GoogleSignIn.getClient(context, it) }
    }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            scope.launch(Dispatchers.IO) {
                try {
                    val signedInAccount = GoogleSignIn.getLastSignedInAccount(context)
                    if (signedInAccount == null) return@launch
                    val accountEmail = signedInAccount.email
                    if (accountEmail == null) return@launch
                    val token = GoogleAuthUtil.getToken(
                        context,
                        Account(accountEmail, "com.google"),
                        "oauth2:https://www.googleapis.com/auth/drive.file"
                    )
                    withContext(Dispatchers.Main) {
                        viewModel.linkGoogleDrive(token)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CloudSync", "Failed to get Drive token", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, context.getString(R.string.toast_auth_error), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    LaunchedEffect(syncStatusMessage) {
        syncStatusMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        topBar = {
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .statusBarsPadding()
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                    Text(
                        text = stringResource(id = R.string.title_cloud_sync),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = stringResource(R.string.cloud_icon),
                            tint = if (isDriveLinked) Color(0xFF42A5F5) else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.cloud_sync_title),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = if (isDriveLinked) stringResource(id = R.string.drive_linked) else stringResource(id = R.string.drive_unlinked),
                                color = if (isDriveLinked) Color(0xFF42A5F5) else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(id = R.string.info_cloud),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(id = R.string.last_synced, lastSyncTime),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!isDriveLinked) {
                Text(
                    text = stringResource(R.string.link_account),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Button(
                    onClick = { signInLauncher.launch(signInClient.signInIntent) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("google_sign_in_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(id = R.string.btn_sign_in_google), fontWeight = FontWeight.Bold)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.forceSyncCloud() },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("push_sync_btn")
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = stringResource(R.string.btn_upload_backup))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.backup_cloud), fontSize = 11.sp)
                    }

                    Button(
                        onClick = { viewModel.restoreSyncCloud() },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("pull_sync_btn")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = stringResource(R.string.btn_download_backup))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.restore_notes), fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { viewModel.unlinkGoogleDrive() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("unlink_drive_btn"),
                    colors = ButtonDefaults.outlinedButtonColors()
                ) {
                    Text(stringResource(id = R.string.btn_unlink_drive), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
