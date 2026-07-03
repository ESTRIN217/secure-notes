package com.example.ui.settings

import android.os.Build
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
import androidx.compose.material.icons.rounded.*
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
                OutlinedCard(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ListItem(
                        headlineContent = {
                            Text(
                                text = stringResource(R.string.update_current_version_label, uiState.currentVersion),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        },
                        supportingContent = {
                            Text(
                                text = "$deviceArch - FOSS",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                    )
                }
            }

            // --- UPDATE SETTINGS ---
            item {
                SettingsSectionTitle(title = stringResource(R.string.update_settings_title))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedCard(
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        SettingsSwitchTile(
                            title = stringResource(R.string.update_auto_check),
                            icon = Icons.Rounded.Refresh,
                            checked = uiState.autoUpdate,
                            onCheckedChange = { viewModel.toggleAutoUpdate(it) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        SettingsSwitchTile(
                            title = stringResource(R.string.update_notifications),
                            icon = Icons.Rounded.NotificationsNone,
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
                OutlinedCard(
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val targetIcon = when {
                        uiState.isChecking -> Icons.Rounded.HourglassEmpty
                        uiState.hasUpdate -> Icons.Rounded.Download
                        else -> Icons.Rounded.Refresh
                    }

                    val targetTitle = if (uiState.hasUpdate) {
                        stringResource(R.string.update_latest_version, uiState.latestVersion ?: "")
                    } else {
                        stringResource(R.string.update_check_now)
                    }

                    ListItem(
                        modifier = Modifier.clickable(enabled = !uiState.isChecking) {
                            if (uiState.hasUpdate) {
                                // Open browser or download
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse("https://github.com/ESTRIN217/secure-notes/releases/latest")
                                )
                                context.startActivity(intent)
                            } else {
                                viewModel.checkForUpdates()
                            }
                        },
                        leadingContent = {
                            Icon(
                                imageVector = targetIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        headlineContent = {
                            if (uiState.isChecking) {
                                Text(
                                    text = stringResource(R.string.update_checking),
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                                )
                            } else {
                                Text(
                                    text = targetTitle,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = null
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                    )
                }
            }

            // --- CHANGELOG ---
            if (uiState.hasUpdate && uiState.latestChangelog != null) {
                item {
                    OutlinedCard(
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    ) {
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
                                        imageVector = if (showChangelog) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
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
