package com.example.ui.settings

import android.os.Build
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.viewmodel.UpdaterViewModel

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

    val deviceArch = remember {
        if (Build.SUPPORTED_ABIS.isNotEmpty()) Build.SUPPORTED_ABIS[0].uppercase() else "UNKNOWN"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_check_update),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
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
                        subtitle = "$deviceArch - FOSS",
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
                            checked = uiState.notifications,
                            onCheckedChange = { viewModel.toggleNotifications(it) }
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
                                    android.net.Uri.parse("https://github.com/ESTRIN217/secure-notes/releases/latest")
                                )
                                context.startActivity(intent)
                            } else {
                                viewModel.checkForUpdates()
                            }
                        }
                    )
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
                                    Text(
                                        text = uiState.latestChangelog ?: "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
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
