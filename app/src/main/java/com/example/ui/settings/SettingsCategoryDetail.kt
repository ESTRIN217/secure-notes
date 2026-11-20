package com.example.ui.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.estrin217.tema.AppTheme
import com.example.DarkModeOption
import com.example.R
import com.example.ui.floating.FloatingPermissionDialog
import com.example.ui.theme.labelRes
import com.example.ui.viewmodel.AiViewModel
import com.example.ui.viewmodel.ThemeViewModel
import com.example.util.FloatingBubbleManager

/**
 * Spec 044 enmienda, RF-330…RF-333. Capa que solo recoge estado de los ViewModels y delega en
 * [SettingsCategoryContent], que no sabe nada de Android y se puede enseñar en un `@Preview`.
 */
@Composable
fun SettingsCategoryDetail(
    category: SettingsDestination,
    themeViewModel: ThemeViewModel,
    aiViewModel: AiViewModel,
    onNavigate: (SettingsDestination) -> Unit,
    onOpenCodeEditor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appTheme by themeViewModel.appTheme.collectAsStateWithLifecycle()
    val darkMode by themeViewModel.darkModeOption.collectAsStateWithLifecycle()
    val language by themeViewModel.language.collectAsStateWithLifecycle()
    val aiEnabled by aiViewModel.aiEnabled.collectAsStateWithLifecycle()
    val floatingActive by FloatingBubbleManager.isRunningFlow().collectAsStateWithLifecycle()

    SettingsCategoryContent(
        category = category,
        appTheme = appTheme,
        darkMode = darkMode,
        language = language,
        aiEnabled = aiEnabled,
        floatingActive = floatingActive,
        canUseOverlay = FloatingBubbleManager.isOverlayPermissionGranted(context),
        onLanguageSelected = { locale -> aplicarIdioma(themeViewModel, context, locale) },
        onAiEnabled = { aiViewModel.setAiEnabled(it) },
        onFloatingToggle = { enable ->
            if (enable) FloatingBubbleManager.startService(context)
            else FloatingBubbleManager.stopService(context)
        },
        onNavigate = onNavigate,
        onOpenCodeEditor = onOpenCodeEditor,
        onThemeSelected = { themeViewModel.setAppTheme(it) },
        onDarkModeSelected = { themeViewModel.setDarkModeOption(it) },
        onOpenOverlaySettings = { FloatingBubbleManager.openOverlaySettings(context) },
        modifier = modifier
    )
}

private fun aplicarIdioma(themeViewModel: ThemeViewModel, context: Context, locale: String) {
    themeViewModel.setLanguage(locale)
    Toast.makeText(context, context.getString(R.string.toast_language_changed), Toast.LENGTH_SHORT).show()
    (context as? android.app.Activity)?.recreate()
}

/**
 * Contenido puro de cada categoría: recibe estado y lanza eventos. Usado por la pantalla real y
 * por las previews, así que ambas muestran exactamente lo mismo.
 */
@Composable
internal fun SettingsCategoryContent(
    category: SettingsDestination,
    appTheme: AppTheme,
    darkMode: DarkModeOption,
    language: String,
    aiEnabled: Boolean,
    floatingActive: Boolean,
    canUseOverlay: Boolean,
    onLanguageSelected: (String) -> Unit,
    onAiEnabled: (Boolean) -> Unit,
    onFloatingToggle: (Boolean) -> Unit,
    onNavigate: (SettingsDestination) -> Unit,
    onOpenCodeEditor: () -> Unit,
    onThemeSelected: (AppTheme) -> Unit,
    onDarkModeSelected: (DarkModeOption) -> Unit,
    onOpenOverlaySettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showThemePicker by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showChangelogSheet by remember { mutableStateOf(false) }
    var showFloatingPermissionDialog by remember { mutableStateOf(false) }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            when (category) {
                SettingsDestination.APPEARANCE -> AppearanceSections(appTheme, darkMode) {
                    if (it == 0) showThemePicker = true else showThemeDialog = true
                }
                SettingsDestination.LANGUAGE -> LanguageSection(language, onLanguageSelected)
                SettingsDestination.PRIVACY -> Section(stringResource(R.string.settings_privacy_security)) {
                    SettingsListTile(
                        leadingIcon = Icons.Default.Shield,
                        title = stringResource(R.string.settings_privacy_security),
                        subtitle = stringResource(R.string.settings_privacy_desc),
                        onClick = { onNavigate(SettingsDestination.PRIVACY_SETTINGS) }
                    )
                }
                SettingsDestination.STORAGE -> StorageSections(onNavigate)
                SettingsDestination.AI -> AiSections(
                    aiEnabled = aiEnabled,
                    onAiEnabled = onAiEnabled,
                    onAiSettings = { onNavigate(SettingsDestination.AI_SETTINGS) }
                )
                SettingsDestination.TOOLS -> ToolsSections(
                    floatingActive,
                    canUseOverlay,
                    { showFloatingPermissionDialog = true },
                    onFloatingToggle,
                    onOpenCodeEditor
                )
                SettingsDestination.LEGAL -> LegalSections(onNavigate)
                SettingsDestination.INFORMATION -> InformationSections(onNavigate) {
                    showChangelogSheet = true
                }
                else -> Unit
            }
        }
    }

    CategoryDialogs(
        appTheme, darkMode,
        showThemePicker, { showThemePicker = false }, onThemeSelected,
        showThemeDialog, { showThemeDialog = false }, onDarkModeSelected,
        showChangelogSheet, { showChangelogSheet = false },
        showFloatingPermissionDialog, { showFloatingPermissionDialog = false },
        onOpenOverlaySettings
    )
}

/** RF-331: cabecera de sección en color de acento y tarjeta separada. */
@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    )
    SettingsCardGroup(content = content)
}

@Composable
private fun AppearanceSections(appTheme: AppTheme, darkMode: DarkModeOption, onOpen: (Int) -> Unit) {
    val currentThemeLabel = when (darkMode) {
        DarkModeOption.SYSTEM -> stringResource(R.string.settings_dark_mode_system)
        DarkModeOption.ON -> stringResource(R.string.settings_dark_mode_on)
        DarkModeOption.OFF -> stringResource(R.string.settings_dark_mode_off)
    }
    Section(stringResource(R.string.settings_theme)) {
        SettingsListTile(
            leadingIcon = Icons.Default.Palette,
            title = stringResource(R.string.settings_theme),
            subtitle = stringResource(labelRes(appTheme)),
            onClick = { onOpen(0) }
        )
        RowDivider()
        SettingsListTile(
            leadingIcon = Icons.Default.DarkMode,
            title = stringResource(R.string.toggle_dark_theme),
            subtitle = currentThemeLabel,
            onClick = { onOpen(1) }
        )
    }
}

/** RF-332: el contenido del bottom sheet de idioma se pinta en el detalle, sin modal. */
@Composable
private fun LanguageSection(language: String, onSelect: (String) -> Unit) {
    Section(stringResource(R.string.settings_language)) {
        LanguageOptionsColumn(language, onSelect)
    }
}

@Composable
private fun StorageSections(onNavigate: (SettingsDestination) -> Unit) {
    Section(stringResource(R.string.settings_backup_restore)) {
        SettingsListTile(
            leadingIcon = Icons.Default.CloudDownload,
            title = stringResource(R.string.settings_backup_restore),
            subtitle = stringResource(R.string.settings_backup_restore_desc),
            onClick = { onNavigate(SettingsDestination.BACKUP) }
        )
    }
    Section(stringResource(R.string.settings_storage_manager)) {
        SettingsListTile(
            leadingIcon = Icons.Default.Storage,
            title = stringResource(R.string.settings_storage_manager),
            subtitle = stringResource(R.string.settings_storage_manager_desc),
            onClick = { onNavigate(SettingsDestination.STORAGE_MANAGER) }
        )
    }
}

@Composable
private fun AiSections(
    aiEnabled: Boolean,
    onAiEnabled: (Boolean) -> Unit,
    onAiSettings: () -> Unit
) {
    Section(stringResource(R.string.ai_enabled)) {
        SettingsSwitchTile(
            title = stringResource(R.string.ai_enabled),
            subtitle = stringResource(R.string.ai_enabled_desc),
            icon = Icons.Default.Psychology,
            checked = aiEnabled,
            onCheckedChange = onAiEnabled
        )
    }
    if (aiEnabled) {
        Section(stringResource(R.string.ai_settings_title)) {
            SettingsListTile(
                leadingIcon = Icons.Default.Tune,
                title = stringResource(R.string.ai_settings_title),
                subtitle = stringResource(R.string.ai_settings_desc),
                onClick = onAiSettings
            )
        }
    }
}

@Composable
private fun ToolsSections(
    floatingActive: Boolean,
    canUseOverlay: Boolean,
    onPermission: () -> Unit,
    onFloatingToggle: (Boolean) -> Unit,
    onOpenCodeEditor: () -> Unit
) {
    Section(stringResource(R.string.floating_mode_title)) {
        SettingsSwitchTile(
            icon = Icons.Default.Layers,
            title = stringResource(R.string.floating_mode_title),
            subtitle = stringResource(R.string.floating_mode_desc),
            checked = floatingActive,
            onCheckedChange = { enable ->
                if (enable && !canUseOverlay) onPermission() else onFloatingToggle(enable)
            }
        )
    }
    Section(stringResource(R.string.code_editor)) {
        SettingsListTile(
            leadingIcon = Icons.Default.Code,
            title = stringResource(R.string.code_editor),
            subtitle = stringResource(R.string.code_editor_desc),
            onClick = onOpenCodeEditor
        )
    }
}

@Composable
private fun LegalSections(onNavigate: (SettingsDestination) -> Unit) {
    Section(stringResource(R.string.settings_terms_privacy)) {
        SettingsListTile(
            leadingIcon = Icons.Default.Description,
            title = stringResource(R.string.settings_terms_privacy),
            subtitle = stringResource(R.string.settings_legal_desc),
            onClick = { onNavigate(SettingsDestination.LEGAL_INFO) }
        )
    }
    Section(stringResource(R.string.settings_licenses)) {
        SettingsListTile(
            leadingIcon = Icons.Default.Code,
            title = stringResource(R.string.settings_licenses),
            onClick = { onNavigate(SettingsDestination.LICENSES) }
        )
    }
}

@Composable
private fun InformationSections(onNavigate: (SettingsDestination) -> Unit, onChangelog: () -> Unit) {
    Section(stringResource(R.string.settings_check_update)) {
        SettingsListTile(
            leadingIcon = Icons.Default.Update,
            title = stringResource(R.string.settings_check_update),
            onClick = { onNavigate(SettingsDestination.UPDATE) }
        )
    }
    Section(stringResource(R.string.settings_changelog)) {
        SettingsListTile(
            leadingIcon = Icons.Default.History,
            title = stringResource(R.string.settings_changelog),
            subtitle = stringResource(R.string.settings_changelog_desc),
            onClick = onChangelog
        )
    }
    Section(stringResource(R.string.settings_about)) {
        SettingsListTile(
            leadingIcon = Icons.Default.Info,
            title = stringResource(R.string.settings_about),
            subtitle = stringResource(R.string.settings_about_desc),
            onClick = { onNavigate(SettingsDestination.ABOUT) }
        )
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun CategoryDialogs(
    appTheme: AppTheme,
    darkMode: DarkModeOption,
    showThemePicker: Boolean,
    onDismissThemePicker: () -> Unit,
    onThemeSelected: (AppTheme) -> Unit,
    showThemeDialog: Boolean,
    onDismissThemeDialog: () -> Unit,
    onDarkModeSelected: (DarkModeOption) -> Unit,
    showChangelogSheet: Boolean,
    onDismissChangelog: () -> Unit,
    showFloatingPermissionDialog: Boolean,
    onDismissFloatingPermission: () -> Unit,
    onOpenOverlaySettings: () -> Unit
) {
    if (showThemePicker) {
        ThemePickerDialog(
            currentTheme = appTheme,
            onDismiss = onDismissThemePicker,
            onSelectTheme = onThemeSelected
        )
    }
    if (showThemeDialog) {
        ThemeDialog(
            currentTheme = darkMode,
            onDismiss = onDismissThemeDialog,
            onThemeSelected = onDarkModeSelected
        )
    }
    if (showChangelogSheet) ChangelogBottomSheet(onDismiss = onDismissChangelog)
    if (showFloatingPermissionDialog) {
        FloatingPermissionDialog(
            onConfirm = {
                onDismissFloatingPermission()
                onOpenOverlaySettings()
            },
            onDismiss = onDismissFloatingPermission
        )
    }
}
