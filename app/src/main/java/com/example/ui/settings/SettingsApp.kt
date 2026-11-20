package com.example.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.ui.launchCodeEditor
import com.example.ui.viewmodel.AiViewModel
import com.example.ui.viewmodel.BackupViewModel
import com.example.ui.viewmodel.NotesViewModel
import com.example.ui.viewmodel.StorageViewModel
import com.example.ui.viewmodel.ThemeViewModel
import com.example.ui.viewmodel.UpdaterViewModel
import com.example.data.security.CipherService
import com.example.ui.viewmodel.aiViewModelFactory
import com.example.ui.viewmodel.backupViewModelFactory
import com.example.ui.viewmodel.storageViewModelFactory
import com.example.ui.viewmodel.themeViewModelFactory
import kotlinx.coroutines.launch

/**
 * Spec 044, RF-317…RF-320. En pantallas grandes la lista de categorías queda visible junto al
 * detalle; en teléfono el scaffold muestra un panel y atrás vuelve a la lista.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsApp(
    initialDestination: SettingsDestination,
    notesViewModel: NotesViewModel,
    cipherService: CipherService,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val application = LocalContext.current.applicationContext
    val themeViewModel: ThemeViewModel = viewModel(factory = themeViewModelFactory(application))
    val aiViewModel: AiViewModel = viewModel(factory = aiViewModelFactory(application as android.app.Application, cipherService))
    val backupViewModel: BackupViewModel = viewModel(factory = backupViewModelFactory(application as android.app.Application, notesViewModel))
    val storageViewModel: StorageViewModel = viewModel(factory = storageViewModelFactory(application as android.app.Application))
    val updaterViewModel: UpdaterViewModel = viewModel()

    val navigator = rememberListDetailPaneScaffoldNavigator<SettingsDestination>()
    val scope = rememberCoroutineScope()

    // RF-329: en dos paneles la primera categoría queda abierta; en teléfono la lista sigue
    // siendo la entrada al patrón list-detail compacto.
    val hasTwoPanes = LocalContext.current.resources.configuration.screenWidthDp >= 600
    val startDestination = if (initialDestination == SettingsDestination.HUB && hasTwoPanes) {
        CATEGORIES.first().destination
    } else {
        initialDestination
    }
    LaunchedEffect(startDestination) {
        if (startDestination != SettingsDestination.HUB) {
            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, startDestination)
        }
    }

    fun openDetail(destination: SettingsDestination) {
        scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, destination) }
    }

    fun goBack() {
        scope.launch {
            if (navigator.canNavigateBack()) navigator.navigateBack() else onClose()
        }
    }

    BackHandler { goBack() }

    NavigableListDetailPaneScaffold(
        modifier = modifier,
        navigator = navigator,
        listPane = {
            AnimatedPane(modifier = Modifier.widthIn(max = 280.dp)) {
                SettingsCategoryList(
                    selected = navigator.currentDestination?.contentKey
                        ?: CATEGORIES.first().destination,
                    onSelect = { openDetail(it) }
                )
            }
        },
        detailPane = {
            AnimatedPane {
                SettingsDetailPane(
                    destination = navigator.currentDestination?.contentKey,
                    notesViewModel = notesViewModel,
                    themeViewModel = themeViewModel,
                    aiViewModel = aiViewModel,
                    backupViewModel = backupViewModel,
                    storageViewModel = storageViewModel,
                    updaterViewModel = updaterViewModel,
                    onNavigate = { openDetail(it) },
                    onBack = { goBack() }
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsCategoryList(
    selected: SettingsDestination?,
    onSelect: (SettingsDestination) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val searchLabel = stringResource(R.string.settings_search_placeholder)
    val visible = CATEGORIES.filter { info ->
        matchesQuery(stringResource(info.labelRes), stringResource(info.summaryRes), query)
    }
    Column(Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.nav_settings),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .semantics { contentDescription = searchLabel },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            placeholder = { Text(stringResource(R.string.settings_search_placeholder)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
        )
        if (visible.isEmpty()) {
            Text(
                text = stringResource(R.string.settings_search_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
            )
        } else {
            LazyColumn {
                visible.groupBy { it.group }.forEach { (group, items) ->
                    item(key = "header_${group.name}") {
                        Text(
                            text = stringResource(group.labelRes),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                        )
                    }
                    items(items, key = { it.destination.name }) { info ->
                        SettingsCategoryRow(info, selected == info.destination) {
                            onSelect(info.destination)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsCategoryRow(
    info: SettingsCategoryInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val container = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val headline = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                   else MaterialTheme.colorScheme.onSurface
    val supporting = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                     else MaterialTheme.colorScheme.onSurfaceVariant
    ListItem(
        selected = isSelected,
        onClick = onClick,
        supportingContent = { Text(stringResource(info.summaryRes)) },
        leadingContent = {
            Icon(
                categoryIcon(info.destination),
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = container,
            contentColor = headline,
            supportingContentColor = supporting,
            leadingContentColor = headline,
            trailingContentColor = headline
        ),
        modifier = Modifier.padding(horizontal = 12.dp).clip(RoundedCornerShape(16.dp))
    ) {
        Text(stringResource(info.labelRes))
    }
}

@Composable
private fun SettingsDetailPane(
    destination: SettingsDestination?,
    notesViewModel: NotesViewModel,
    themeViewModel: ThemeViewModel,
    aiViewModel: AiViewModel,
    backupViewModel: BackupViewModel,
    storageViewModel: StorageViewModel,
    updaterViewModel: UpdaterViewModel,
    onNavigate: (SettingsDestination) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    if (destination == null || destination == SettingsDestination.HUB) return
    if (destination.isCategory) {
        Column(Modifier.fillMaxSize()) {
            SettingsDetailHeader(title = categoryLabel(destination), onClose = onBack)
            SettingsCategoryDetail(
                category = destination,
                themeViewModel = themeViewModel,
                aiViewModel = aiViewModel,
                onNavigate = onNavigate,
                onOpenCodeEditor = { launchCodeEditor(context) }
            )
        }
    } else {
        when (destination) {
            SettingsDestination.PRIVACY_SETTINGS -> PrivacySettingsScreen(notesViewModel, onBack)
            SettingsDestination.BACKUP -> BackupRestoreScreen(backupViewModel, onBack)
            SettingsDestination.STORAGE_MANAGER -> StorageManagerScreen(storageViewModel, onBack)
            SettingsDestination.AI_SETTINGS -> AiSettingsScreen(aiViewModel, onBack)
            SettingsDestination.LEGAL_INFO -> LegalInfoScreen(onBack)
            SettingsDestination.LICENSES -> LicensesScreen(onBack)
            SettingsDestination.UPDATE -> UpdateInfoScreen(updaterViewModel, onBack)
            SettingsDestination.ABOUT -> AboutScreen(
                onBack = onBack,
                onNavigateToLegalInfo = { onNavigate(SettingsDestination.LEGAL_INFO) },
                onNavigateToLicenses = { onNavigate(SettingsDestination.LICENSES) }
            )
            else -> Unit
        }
    }
}

/** RF-330: encabezado del detalle con título grande y cierre, sin barra superior. */
@Composable
internal fun SettingsDetailHeader(title: String, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.back))
        }
    }
}

@Composable
private fun categoryLabel(destination: SettingsDestination): String = when (destination) {
    SettingsDestination.APPEARANCE -> stringResource(R.string.settings_appearance)
    SettingsDestination.LANGUAGE -> stringResource(R.string.settings_language)
    SettingsDestination.PRIVACY -> stringResource(R.string.settings_privacy_security)
    SettingsDestination.STORAGE -> stringResource(R.string.settings_storage_data)
    SettingsDestination.AI -> stringResource(R.string.ai_section)
    SettingsDestination.TOOLS -> stringResource(R.string.settings_tools)
    SettingsDestination.LEGAL -> stringResource(R.string.settings_legal)
    SettingsDestination.INFORMATION -> stringResource(R.string.settings_information)
    else -> stringResource(R.string.nav_settings)
}

private fun categoryIcon(destination: SettingsDestination): ImageVector = when (destination) {
    SettingsDestination.APPEARANCE -> Icons.Default.Palette
    SettingsDestination.LANGUAGE -> Icons.Default.Language
    SettingsDestination.PRIVACY -> Icons.Default.Shield
    SettingsDestination.STORAGE -> Icons.Default.Storage
    SettingsDestination.AI -> Icons.Default.Psychology
    SettingsDestination.TOOLS -> Icons.Default.Layers
    SettingsDestination.LEGAL -> Icons.Default.Description
    else -> Icons.Default.Update
}
