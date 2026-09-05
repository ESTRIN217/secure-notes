package com.example.ui.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.AppUpdateConfig
import androidx.compose.material3.TopAppBar
import com.example.ui.viewmodel.UpdaterViewModel
import com.example.ui.viewmodel.UpdateDownloadState
import dev.jeziellago.compose.markdowntext.MarkdownText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateInfoScreen(
    viewModel: UpdaterViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showChangelog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val installPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.onInstallPermissionResult()
    }

    var pendingNotificationEnable by rememberSaveable { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onNotificationPermissionResult(granted)
        pendingNotificationEnable = false
        if (granted) {
            viewModel.toggleNotifications(true)
        }
    }

    fun handleNotificationToggle(enabled: Boolean) {
        if (!enabled) {
            pendingNotificationEnable = false
            viewModel.toggleNotifications(false)
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) {
            pendingNotificationEnable = false
            viewModel.toggleNotifications(true)
        } else {
            pendingNotificationEnable = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshNotificationPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val unknownLabel = stringResource(R.string.platform_unknown)
    val deviceArch = remember {
        if (Build.SUPPORTED_ABIS.isNotEmpty()) Build.SUPPORTED_ABIS[0].uppercase() else unknownLabel
    }

    Scaffold(
        topBar = {
          TopAppBar(
            title = {
              Text(
                        text = stringResource(R.string.settings_check_update),
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
            // --- CURRENT VERSION ---
            item {
                SettingsSectionTitle(title = stringResource(R.string.update_current_version))
                Spacer(modifier = Modifier.height(8.dp))
                SettingsCardGroup {
                    SettingsListTile(
                        leadingIcon = Icons.Default.Info,
                        title = stringResource(R.string.update_current_version_label, uiState.currentVersion),
                        subtitle = stringResource(R.string.update_device_arch_foss, deviceArch),
                        onClick = {}
                    )
                }
            }

            // --- UPDATE SETTINGS ---
            item {
                SettingsSectionTitle(title = stringResource(R.string.update_settings_title))
                Spacer(modifier = Modifier.height(8.dp))
                SettingsCardGroup {
                    Column {
                        SettingsSwitchTile(
                            title = stringResource(R.string.update_auto_check),
                            icon = Icons.Default.Refresh,
                            checked = uiState.autoUpdate,
                            onCheckedChange = { viewModel.toggleAutoUpdate(it) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        SettingsSwitchTile(
                            title = stringResource(R.string.update_notifications),
                            icon = Icons.Default.NotificationsNone,
                            subtitle = if (uiState.notifications && !uiState.notificationPermissionGranted) {
                                stringResource(R.string.update_notifications_permission_missing)
                            } else {
                                null
                            },
                            checked = uiState.notifications &&
                                (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || uiState.notificationPermissionGranted),
                            onCheckedChange = { handleNotificationToggle(it) }
                        )
                    }
                }
            }

            // --- CHECK FOR UPDATES ---
            item {
                SettingsSectionTitle(title = stringResource(R.string.update_check_now))
                Spacer(modifier = Modifier.height(8.dp))
                val targetIcon = when {
                    uiState.isChecking -> Icons.Default.HourglassEmpty
                    uiState.hasUpdate -> Icons.Default.Download
                    else -> Icons.Default.Refresh
                }
                val targetTitle = when {
                    uiState.isChecking -> stringResource(R.string.update_checking)
                    uiState.hasUpdate -> stringResource(R.string.update_latest_version, uiState.latestVersion ?: "")
                    else -> stringResource(R.string.update_check_now)
                }
                SettingsCardGroup {
                    SettingsListTile(
                        leadingIcon = targetIcon,
                        title = targetTitle,
                        trailingIcon = Icons.Default.ChevronRight,
                        onClick = {
                            if (uiState.hasUpdate) {
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(AppUpdateConfig.releasesWebUrl)
                                )
                                context.startActivity(intent)
                            } else {
                                viewModel.checkForUpdates()
                            }
                        }
                    )
                }
            }

            // --- DOWNLOAD & INSTALL ---
            if (uiState.hasUpdate) {
                item {
                    SettingsSectionTitle(title = stringResource(R.string.update_download_title))
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsCardGroup {
                        when (val state = uiState.downloadState) {
                            UpdateDownloadState.Idle -> {
                                SettingsListTile(
                                    leadingIcon = Icons.Default.Download,
                                    title = stringResource(R.string.update_download_install),
                                    subtitle = stringResource(R.string.update_download_subtitle, AppUpdateConfig.GITHUB_REPO),
                                    trailingIcon = Icons.Default.ChevronRight,
                                    onClick = { viewModel.downloadAndInstall() }
                                )
                            }
                            is UpdateDownloadState.Downloading -> {
                                SettingsListTile(
                                    leadingIcon = Icons.Default.Sync,
                                    title = stringResource(R.string.update_downloading),
                                    subtitle = if (state.totalMb > 0) {
                                        stringResource(
                                            R.string.update_download_progress,
                                            state.progress,
                                            state.downloadedMb,
                                            state.totalMb
                                        )
                                    } else {
                                        stringResource(R.string.platform_unknown)
                                    },
                                    trailingIcon = Icons.Default.Close,
                                    onClick = { viewModel.cancelDownload() }
                                )
                                LinearProgressIndicator(
                                    progress = { state.progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 8.dp)
                                )
                            }
                            UpdateDownloadState.PreparingInstall -> {
                                SettingsListTile(
                                    leadingIcon = Icons.Default.CheckCircle,
                                    title = stringResource(R.string.update_preparing_install),
                                    onClick = {}
                                )
                            }
                            is UpdateDownloadState.DownloadFailed -> {
                                SettingsListTile(
                                    leadingIcon = Icons.Default.Error,
                                    title = stringResource(R.string.update_download_failed),
                                    subtitle = state.error,
                                    onClick = { viewModel.checkForUpdates() },
                                    trailingIcon = Icons.Default.Refresh
                                )
                            }
                        }
                    }
                }
            }

            // --- INSTALL PERMISSION ---
            if (uiState.needsInstallPermission) {
                item {
                    SettingsCardGroup {
                        SettingsListTile(
                            leadingIcon = Icons.Default.Security,
                            title = stringResource(R.string.update_install_permission_title),
                            subtitle = stringResource(R.string.update_install_permission_subtitle),
                            trailingIcon = Icons.Default.ChevronRight,
                            onClick = {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                    Uri.parse("package:${context.packageName}")
                                )
                                installPermissionLauncher.launch(intent)
                            }
                        )
                    }
                }
            }

            // --- ERROR ---
            if (uiState.updateError) {
                item {
                    SettingsCardGroup {
                        SettingsListTile(
                            leadingIcon = Icons.Default.Error,
                            title = stringResource(R.string.update_check_failed),
                            trailingIcon = Icons.Default.Refresh,
                            onClick = { viewModel.checkForUpdates() }
                        )
                    }
                }
            }

            // --- CHANGELOG ---
            if (uiState.hasUpdate && uiState.latestChangelog != null) {
                item {
                    SettingsCardGroup {
                        Column {
                            TextButton(
                                onClick = { showChangelog = !showChangelog },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (showChangelog) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (showChangelog) {
                                            stringResource(R.string.update_hide_changelog)
                                        } else {
                                            stringResource(R.string.update_show_changelog)
                                        }
                                    )
                                }
                            }

                            AnimatedVisibility(
                                visible = showChangelog,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(bottom = 12.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                    MarkdownText(
                                        markdown = uiState.latestChangelog ?: "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        linkColor = MaterialTheme.colorScheme.primary
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
