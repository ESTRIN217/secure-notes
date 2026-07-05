package com.example.ui.settings

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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

            if (isDynamicColorSupported) {
                item {
                    SettingsCardGroup {
                        SettingsSwitchTile(
                            title = stringResource(R.string.settings_dynamic_colors),
                            icon = Icons.Default.Palette,
                            checked = isDynamicColor,
                            onCheckedChange = { themeViewModel.setDynamicColor(it) }
                        )
                    }
                }
            }

            item {
                SettingsCardGroup {
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
                }
            }

            item {
                SettingsCardGroup {
                    SettingsListTile(
                        leadingIcon = Icons.Default.History,
                        title = stringResource(R.string.settings_changelog),
                        subtitle = stringResource(R.string.settings_changelog_desc),
                        trailingIcon = Icons.Default.ChevronRight,
                        onClick = { showChangelogSheet = true }
                    )
                }
            }

            item {
                SettingsCardGroup {
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

@Composable
fun ThemeDialog(
    currentTheme: DarkModeOption,
    onDismiss: () -> Unit,
    onThemeSelected: (DarkModeOption) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_dark_mode)) },
        text = {
            Column(Modifier.selectableGroup()) {
                val options = listOf(
                    Triple(DarkModeOption.SYSTEM, stringResource(R.string.settings_dark_mode_system), Icons.Default.BrightnessAuto),
                    Triple(DarkModeOption.OFF, stringResource(R.string.settings_dark_mode_off), Icons.Default.LightMode),
                    Triple(DarkModeOption.ON, stringResource(R.string.settings_dark_mode_on), Icons.Default.DarkMode)
                )
                options.forEach { (mode, label, icon) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .selectable(
                                selected = (mode == currentTheme),
                                onClick = {
                                    onThemeSelected(mode)
                                    onDismiss()
                                },
                                role = Role.RadioButton
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = (mode == currentTheme), onClick = null)
                        Spacer(Modifier.width(16.dp))
                        Icon(icon, contentDescription = null)
                        Spacer(Modifier.width(16.dp))
                        Text(label)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
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
        Column(modifier = Modifier.fillMaxWidth()) {
            ListItem(
                modifier = Modifier.clickable { onLocaleSelected(""); onDismiss() },
                headlineContent = { Text(stringResource(R.string.settings_lang_default)) },
                leadingContent = { Icon(Icons.Default.Language, contentDescription = null) },
                trailingContent = {
                    if (currentLanguage == "") {
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
            HorizontalDivider()

            ListItem(
                modifier = Modifier.clickable { onLocaleSelected("en"); onDismiss() },
                headlineContent = { Text(stringResource(R.string.settings_lang_en)) },
                leadingContent = { Text("\uD83C\uDDFA\uD83C\uDDF8", style = MaterialTheme.typography.titleLarge) },
                trailingContent = {
                    if (currentLanguage == "en") {
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
            HorizontalDivider()

            ListItem(
                modifier = Modifier.clickable { onLocaleSelected("es-VE"); onDismiss() },
                headlineContent = { Text(stringResource(R.string.settings_lang_es)) },
                leadingContent = { Text("\uD83C\uDDFB\uD83C\uDDEA", style = MaterialTheme.typography.titleLarge) },
                trailingContent = {
                    if (currentLanguage == "es-VE") {
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
            HorizontalDivider()

            ListItem(
                modifier = Modifier.clickable { onLocaleSelected("es-ES"); onDismiss() },
                headlineContent = { Text(stringResource(R.string.settings_lang_es_es)) },
                leadingContent = { Text("\uD83C\uDDEA\uD83C\uDDF8", style = MaterialTheme.typography.titleLarge) },
                trailingContent = {
                    if (currentLanguage == "es-ES") {
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
            HorizontalDivider()

            ListItem(
                modifier = Modifier.clickable { onLocaleSelected("pt-BR"); onDismiss() },
                headlineContent = { Text(stringResource(R.string.settings_lang_pt)) },
                leadingContent = { Text("\uD83C\uDDFE\uD83C\uDDF7", style = MaterialTheme.typography.titleLarge) },
                trailingContent = {
                    if (currentLanguage == "pt-BR") {
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
            HorizontalDivider()

            ListItem(
                modifier = Modifier.clickable { onLocaleSelected("pt-PT"); onDismiss() },
                headlineContent = { Text(stringResource(R.string.settings_lang_pt_pt)) },
                leadingContent = { Text("\uD83C\uDDF5\uD83C\uDDF9", style = MaterialTheme.typography.titleLarge) },
                trailingContent = {
                    if (currentLanguage == "pt-PT") {
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}


