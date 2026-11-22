package com.example.ui.settings

import android.app.Activity
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.AppConstants
import com.example.DarkModeOption
import com.example.R
import com.example.data.ai.AiBackend
import androidx.compose.material3.TopAppBar
import com.example.ui.floating.FloatingPermissionDialog
import com.example.ui.viewmodel.AiViewModel
import com.example.ui.theme.labelRes
import com.example.ui.viewmodel.ThemeViewModel
import com.example.util.FloatingBubbleManager

// ---------------------------------------------------------------------------
// Theme Dialog
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeDialog(
    currentTheme: DarkModeOption,
    onDismiss: () -> Unit,
    onThemeSelected: (DarkModeOption) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 48.dp)
        ) {
            Text(
                text = stringResource(R.string.toggle_dark_theme),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            SettingsCardGroup {
                val options = listOf(
                    Triple(DarkModeOption.SYSTEM, stringResource(R.string.settings_dark_mode_system), Icons.Default.BrightnessAuto),
                    Triple(DarkModeOption.OFF, stringResource(R.string.settings_dark_mode_off), Icons.Default.LightMode),
                    Triple(DarkModeOption.ON, stringResource(R.string.settings_dark_mode_on), Icons.Default.DarkMode)
                )

                options.forEachIndexed { index, (mode, label, icon) ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                    ThemeOptionRow(
                        icon = icon,
                        label = label,
                        isSelected = mode == currentTheme,
                        onClick = {
                            onThemeSelected(mode)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionRow(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIconContainer(icon = icon, isSelected = isSelected)

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier.weight(1f)
        )

        if (isSelected) {
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Language Bottom Sheet
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageBottomSheet(
    currentLanguage: String,
    onDismiss: () -> Unit,
    onLocaleSelected: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 48.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_language),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            LanguageOptionsColumn(currentLanguage) { locale ->
                onLocaleSelected(locale)
                onDismiss()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiBackendBottomSheet(
    currentBackend: AiBackend,
    onDismiss: () -> Unit,
    onBackendSelected: (AiBackend) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 48.dp)
        ) {
            Text(
                text = stringResource(R.string.ai_backend),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            SettingsCardGroup {
                BackendOption(
                    icon = Icons.Default.Wifi,
                    title = stringResource(R.string.ai_ollama),
                    subtitle = stringResource(R.string.ai_ollama_desc),
                    isSelected = currentBackend == AiBackend.OLLAMA,
                    onClick = { onBackendSelected(AiBackend.OLLAMA) }
                )
                if (com.example.BuildConfig.HAS_ON_DEVICE_AI) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    BackendOption(
                        icon = Icons.Default.PhoneAndroid,
                        title = stringResource(R.string.ai_ondevice),
                        subtitle = stringResource(R.string.ai_ondevice_desc),
                        isSelected = currentBackend == AiBackend.ON_DEVICE,
                        onClick = { onBackendSelected(AiBackend.ON_DEVICE) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BackendOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIconContainer(icon = icon, isSelected = isSelected)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isSelected) {
            Spacer(modifier = Modifier.width(12.dp))
            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}
