package com.example.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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

    // RF-340/RF-347/RF-349/RF-350: una sola barra en móvil; en tablet/PC no hay barra global y el
    // detalle pone título + X. La barra se contrae/expande con un estado booleano con histéresis
    // (sin contentOffset ni snap/fling, que causaban el vaivén). Sin nestedScroll connection, que
    // rompía el gesto (RF-346).
    val topBarState = rememberTopAppBarState()
    val topBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = topBarState,
        snapAnimationSpec = null,
        flingAnimationSpec = null
    )
    var topBarCollapsed by remember { mutableStateOf(false) }
    var titleAlpha by remember { mutableFloatStateOf(1f) }
    val collapseOffsetPx = with(LocalDensity.current) { 48.dp.toPx() }
    LaunchedEffect(topBarCollapsed) {
        topBarState.heightOffset = if (topBarCollapsed) -collapseOffsetPx else 0f
    }
    val current = navigator.currentDestination?.contentKey
    val appBarTitle = if (current == null || current == SettingsDestination.HUB) {
        stringResource(R.string.nav_settings)
    } else {
        categoryLabel(current)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        if (!hasTwoPanes) {
            MediumTopAppBar(
                title = {
                    Text(
                        text = appBarTitle,
                        style = if (topBarCollapsed) MaterialTheme.typography.titleSmall
                                else MaterialTheme.typography.titleLarge,
                        modifier = Modifier.alpha(titleAlpha)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { goBack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                scrollBehavior = topBarScrollBehavior
            )
        }
        // RF-348: en tablet no hay topbar, así que el Box aplica todo el padding del sistema; en
        // móvil la MediumTopAppBar ya cubre el Top, así que aquí solo se aplican laterales y abajo.
        val contentInsets = if (hasTwoPanes) {
            WindowInsets.systemBars
        } else {
            WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .windowInsetsPadding(contentInsets)
        ) {
            NavigableListDetailPaneScaffold(
                navigator = navigator,
                listPane = {
                    AnimatedPane(modifier = Modifier.fillMaxSize().widthIn(max = 280.dp)) {
                        SettingsCategoryList(
                            // RF-351: en tablet/PC la primera queda activa al abrir; en móvil la
                            // lista es la entrada y no debe haber ninguna fila resaltada.
                            selected = current ?: if (hasTwoPanes) CATEGORIES.first().destination else null,
                            showChevron = !hasTwoPanes,
                            // heightOffset se coerciona entre 0 y heightOffsetLimit (lo fija la
                            // barra); contentOffset alimenta el fade del título al pasar contenido
                            // por debajo. Ambos los llenaba la connection, ahora el scroll pasivo.
                            onScroll = { offset ->
                                // Contracción con histéresis (evita el vaivén) y fade del título
                                // solo cuando ya está contraída. Sin contentOffset ni snap/fling.
                                topBarCollapsed = when {
                                    offset > 64 -> true
                                    offset < 8 -> false
                                    else -> topBarCollapsed
                                }
                                titleAlpha = (1f - ((offset - 64) / 72f)).coerceIn(0f, 1f)
                            },
                            onSelect = { openDetail(it) }
                        )
                    }
                },
                detailPane = {
                    AnimatedPane(modifier = Modifier.fillMaxSize()) {
                        SettingsDetailPane(
                            destination = current,
                            showHeader = hasTwoPanes,
                            notesViewModel = notesViewModel,
                            themeViewModel = themeViewModel,
                            aiViewModel = aiViewModel,
                            backupViewModel = backupViewModel,
                            storageViewModel = storageViewModel,
                            updaterViewModel = updaterViewModel,
                            onNavigate = { openDetail(it) },
                            onClose = { goBack() }
                        )
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsCategoryList(
    selected: SettingsDestination?,
    showChevron: Boolean = true,
    onScroll: (Int) -> Unit = {},
    onSelect: (SettingsDestination) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    androidx.compose.runtime.LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemScrollOffset }
            .collect { onScroll(it) }
    }
    val searchLabel = stringResource(R.string.settings_search_placeholder)
    val visible = CATEGORIES.filter { info ->
        // RF-339: título, subtítulo y términos del contenido de su pantalla de detalle.
        val haystack = buildList {
            add(stringResource(info.labelRes))
            add(stringResource(info.summaryRes))
            searchTermsFor(info.destination).forEach { add(stringResource(it)) }
        }
        matchesQuery(haystack, query)
    }
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .semantics { contentDescription = searchLabel },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            placeholder = { Text(searchLabel) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.clear_search)
                        )
                    }
                }
            }
        )
        if (visible.isEmpty()) {
            Text(
                text = stringResource(R.string.settings_search_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
            )
        } else {
            LazyColumn(modifier = Modifier.weight(1f), state = listState) {
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
                        SettingsCategoryRow(info, selected == info.destination, showChevron) {
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
    showChevron: Boolean,
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
            // RF-344: en móvil la fila navega al detalle; en tablet el detalle ya está visible.
            if (showChevron) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                           else MaterialTheme.colorScheme.onSurfaceVariant
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
    showHeader: Boolean,
    notesViewModel: NotesViewModel,
    themeViewModel: ThemeViewModel,
    aiViewModel: AiViewModel,
    backupViewModel: BackupViewModel,
    storageViewModel: StorageViewModel,
    updaterViewModel: UpdaterViewModel,
    onNavigate: (SettingsDestination) -> Unit,
    onClose: () -> Unit
) {
    if (destination == null || destination == SettingsDestination.HUB) return
    if (showHeader) {
        Column(Modifier.fillMaxSize()) {
            SettingsDetailHeader(title = categoryLabel(destination), onClose = onClose)
            SettingsDetailContent(
                destination, notesViewModel, themeViewModel, aiViewModel,
                backupViewModel, storageViewModel, updaterViewModel, onNavigate, onClose
            )
        }
    } else {
        SettingsDetailContent(
            destination, notesViewModel, themeViewModel, aiViewModel,
            backupViewModel, storageViewModel, updaterViewModel, onNavigate, onClose
        )
    }
}

@Composable
private fun SettingsDetailContent(
    destination: SettingsDestination,
    notesViewModel: NotesViewModel,
    themeViewModel: ThemeViewModel,
    aiViewModel: AiViewModel,
    backupViewModel: BackupViewModel,
    storageViewModel: StorageViewModel,
    updaterViewModel: UpdaterViewModel,
    onNavigate: (SettingsDestination) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    // Las sub-pantallas ya no llevan barra ni back: en móvil los da la topbar global y en tablet
    // el encabezado del panel (RF-341).
    if (destination.isCategory &&
        destination != SettingsDestination.AI &&
        destination != SettingsDestination.PRIVACY
    ) {
        SettingsCategoryDetail(
            category = destination,
            themeViewModel = themeViewModel,
            onOpenCodeEditor = { launchCodeEditor(context) }
        )
    } else {
        when (destination) {
            SettingsDestination.PRIVACY, SettingsDestination.PRIVACY_SETTINGS ->
                PrivacySettingsScreen(notesViewModel, onDone = onClose)
            SettingsDestination.BACKUP -> BackupRestoreScreen(backupViewModel)
            SettingsDestination.STORAGE_MANAGER -> StorageManagerScreen(storageViewModel)
            SettingsDestination.AI, SettingsDestination.AI_SETTINGS -> AiSettingsScreen(aiViewModel)
            SettingsDestination.LEGAL_INFO -> LegalInfoScreen()
            SettingsDestination.LICENSES -> LicensesScreen()
            SettingsDestination.UPDATE -> UpdateInfoScreen(updaterViewModel)
            SettingsDestination.CHANGELOG -> ChangelogScreen()
            SettingsDestination.ABOUT -> AboutScreen(
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
internal fun categoryLabel(destination: SettingsDestination): String = when (destination) {
    SettingsDestination.APPEARANCE -> stringResource(R.string.settings_appearance)
    SettingsDestination.LANGUAGE -> stringResource(R.string.settings_language)
    SettingsDestination.PRIVACY -> stringResource(R.string.settings_privacy_security)
    SettingsDestination.STORAGE -> stringResource(R.string.settings_storage_data)
    SettingsDestination.BACKUP -> stringResource(R.string.settings_backup_restore)
    SettingsDestination.STORAGE_MANAGER -> stringResource(R.string.settings_storage_manager)
    SettingsDestination.AI -> stringResource(R.string.ai_section)
    SettingsDestination.TOOLS -> stringResource(R.string.settings_tools)
    SettingsDestination.LEGAL -> stringResource(R.string.settings_legal)
    SettingsDestination.LEGAL_INFO -> stringResource(R.string.settings_terms_privacy)
    SettingsDestination.LICENSES -> stringResource(R.string.settings_licenses)
    SettingsDestination.INFORMATION -> stringResource(R.string.settings_information)
    SettingsDestination.UPDATE -> stringResource(R.string.settings_check_update)
    SettingsDestination.CHANGELOG -> stringResource(R.string.settings_changelog)
    SettingsDestination.ABOUT -> stringResource(R.string.settings_about)
    else -> stringResource(R.string.nav_settings)
}

private fun categoryIcon(destination: SettingsDestination): ImageVector = when (destination) {
    SettingsDestination.APPEARANCE -> Icons.Default.Palette
    SettingsDestination.LANGUAGE -> Icons.Default.Language
    SettingsDestination.PRIVACY -> Icons.Default.Shield
    SettingsDestination.STORAGE -> Icons.Default.Storage
    SettingsDestination.BACKUP -> Icons.Default.CloudDownload
    SettingsDestination.STORAGE_MANAGER -> Icons.Default.Storage
    SettingsDestination.AI -> Icons.Default.Psychology
    SettingsDestination.TOOLS -> Icons.Default.Layers
    SettingsDestination.LEGAL -> Icons.Default.Description
    SettingsDestination.LEGAL_INFO -> Icons.Default.Description
    SettingsDestination.LICENSES -> Icons.Default.Code
    SettingsDestination.UPDATE -> Icons.Default.Update
    SettingsDestination.CHANGELOG -> Icons.Default.History
    SettingsDestination.ABOUT -> Icons.Default.Info
    else -> Icons.Default.Update
}
