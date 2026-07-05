package com.example.ui.settings

import android.app.Activity
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.DarkModeOption
import com.example.R
import com.example.ui.viewmodel.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeViewModel: ThemeViewModel,
    onBack: () -> Unit,
    onNavigateToBackupRestore: () -> Unit,
    onNavigateToUpdateInfo: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToPrivacy: () -> Unit
) {
    BackHandler(onBack = onBack)
    val darkModeOption by themeViewModel.darkModeOption.collectAsStateWithLifecycle()
    val isDynamicColor by themeViewModel.isDynamicColor.collectAsStateWithLifecycle()
    val language by themeViewModel.language.collectAsStateWithLifecycle()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }
    var showChangelogSheet by remember { mutableStateOf(false) }

    val isDynamicColorSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_settings)) },
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
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // --- APPEARANCE ---
            item {
                SettingsSectionTitle(title = stringResource(R.string.settings_appearance))
            }

                item {
                    SettingsCardGroup {
                        if (isDynamicColorSupported) {
                        SettingsSwitchTile(
                            title = stringResource(R.string.settings_dynamic_colors),
                            icon = Icons.Default.Palette,
                            checked = isDynamicColor,
                            onCheckedChange = { themeViewModel.setDynamicColor(it) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        }
                    
                    val currentThemeLabel = when (darkModeOption) {
                        DarkModeOption.SYSTEM -> stringResource(R.string.settings_dark_mode_system)
                        DarkModeOption.ON -> stringResource(R.string.settings_dark_mode_on)
                        DarkModeOption.OFF -> stringResource(R.string.settings_dark_mode_off)
                    }
                    SettingsListTile(
                        leadingIcon = Icons.Default.DarkMode,
                        title = stringResource(R.string.settings_dark_mode),
                        subtitle = currentThemeLabel,
                        trailingIcon = Icons.Default.ChevronRight,
                        onClick = { showThemeDialog = true }
                    )
                    }
                }

            // --- LANGUAGE ---
            item {
                SettingsSectionTitle(title = stringResource(R.string.settings_language))
            }

            item {
                SettingsCardGroup {
                    val currentLangLabel = when (language) {
                        "" -> stringResource(R.string.settings_lang_default)
                        "en" -> stringResource(R.string.settings_lang_en)
                        "es-VE" -> stringResource(R.string.settings_lang_es)
                        "es-ES" -> stringResource(R.string.settings_lang_es_es)
                        "pt-BR" -> stringResource(R.string.settings_lang_pt)
                        "pt-PT" -> stringResource(R.string.settings_lang_pt_pt)
                        else -> language.uppercase()
                    }
                    SettingsListTile(
                        leadingIcon = Icons.Default.Language,
                        title = stringResource(R.string.settings_language),
                        subtitle = currentLangLabel,
                        trailingIcon = Icons.Default.ChevronRight,
                        onClick = { showLanguageSheet = true }
                    )
                }
            }

            // --- PRIVACY & SECURITY ---
            item {
                SettingsSectionTitle(title = stringResource(R.string.settings_privacy_security))
            }

            item {
                SettingsCardGroup {
                    SettingsListTile(
                        leadingIcon = Icons.Default.Shield,
                        title = stringResource(R.string.settings_privacy_security),
                        subtitle = stringResource(R.string.settings_privacy_desc),
                        trailingIcon = Icons.Default.ChevronRight,
                        onClick = onNavigateToPrivacy
                    )
                }
            }

            // --- STORAGE & DATA ---
            item {
                SettingsSectionTitle(title = stringResource(R.string.settings_storage_data))
            }

            item {
                SettingsCardGroup {
                    SettingsListTile(
                        leadingIcon = Icons.Default.CloudDownload,
                        title = stringResource(R.string.settings_backup_restore),
                        subtitle = stringResource(R.string.settings_backup_restore_desc),
                        trailingIcon = Icons.Default.ChevronRight,
                        onClick = onNavigateToBackupRestore
                    )
                }
            }

            // --- INFORMATION ---
            item {
                SettingsSectionTitle(title = stringResource(R.string.settings_information))
            }

            item {
                SettingsCardGroup {
                    SettingsListTile(
                        leadingIcon = Icons.Default.Update,
                        title = stringResource(R.string.settings_check_update),
                        trailingIcon = Icons.Default.ChevronRight,
                        onClick = onNavigateToUpdateInfo
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    SettingsListTile(
                        leadingIcon = Icons.Default.History,
                        title = stringResource(R.string.settings_changelog),
                        subtitle = stringResource(R.string.settings_changelog_desc),
                        trailingIcon = Icons.Default.ChevronRight,
                        onClick = { showChangelogSheet = true }
                    )
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    SettingsListTile(
                        leadingIcon = Icons.Default.Info,
                        title = stringResource(R.string.settings_about),
                        subtitle = stringResource(R.string.settings_about_desc),
                        trailingIcon = Icons.Default.ChevronRight,
                        onClick = onNavigateToAbout
                    )
                }
            }
        }
    }

    // --- DIALOGS & SHEETS ---

    if (showThemeDialog) {
        ThemeDialog(
            currentTheme = darkModeOption,
            onDismiss = { showThemeDialog = false },
            onThemeSelected = { themeViewModel.setDarkModeOption(it) }
        )
    }

    if (showLanguageSheet) {
        LanguageBottomSheet(
            currentLanguage = language,
            onDismiss = { showLanguageSheet = false },
            onLocaleSelected = { locale ->
                themeViewModel.setLanguage(locale)
                showLanguageSheet = false
                Toast.makeText(context, context.getString(R.string.toast_language_changed), Toast.LENGTH_SHORT).show()
                (context as Activity).recreate()
            }
        )
    }

    if (showChangelogSheet) {
        ChangelogBottomSheet(
            onDismiss = { showChangelogSheet = false }
        )
    }
}

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
                text = stringResource(R.string.settings_dark_mode),
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
    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

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

            SettingsCardGroup {
                LanguageOption(
                    icon = Icons.Default.Language,
                    label = stringResource(R.string.settings_lang_default),
                    isSelected = currentLanguage.isEmpty(),
                    onClick = { onLocaleSelected(""); onDismiss() }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = dividerColor
                )
                LanguageOption(
                    flag = "\uD83C\uDDFA\uD83C\uDDF8",
                    label = stringResource(R.string.settings_lang_en),
                    isSelected = currentLanguage == "en",
                    onClick = { onLocaleSelected("en"); onDismiss() }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = dividerColor
                )
                LanguageOption(
                    flag = "\uD83C\uDDFB\uD83C\uDDEA",
                    label = stringResource(R.string.settings_lang_es),
                    isSelected = currentLanguage == "es-VE",
                    onClick = { onLocaleSelected("es-VE"); onDismiss() }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = dividerColor
                )
                LanguageOption(
                    flag = "\uD83C\uDDEA\uD83C\uDDF8",
                    label = stringResource(R.string.settings_lang_es_es),
                    isSelected = currentLanguage == "es-ES",
                    onClick = { onLocaleSelected("es-ES"); onDismiss() }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = dividerColor
                )
                LanguageOption(
                    flag = "\uD83C\uDDFE\uD83C\uDDF7",
                    label = stringResource(R.string.settings_lang_pt),
                    isSelected = currentLanguage == "pt-BR",
                    onClick = { onLocaleSelected("pt-BR"); onDismiss() }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = dividerColor
                )
                LanguageOption(
                    flag = "\uD83C\uDDF5\uD83C\uDDF9",
                    label = stringResource(R.string.settings_lang_pt_pt),
                    isSelected = currentLanguage == "pt-PT",
                    onClick = { onLocaleSelected("pt-PT"); onDismiss() }
                )
            }
        }
    }
}

@Composable
private fun LanguageOption(
    icon: ImageVector? = null,
    flag: String? = null,
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
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Text(
                    text = flag ?: "",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }

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


