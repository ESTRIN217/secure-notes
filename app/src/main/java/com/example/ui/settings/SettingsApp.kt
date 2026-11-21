package com.example.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.security.CipherService
import com.example.ui.launchCodeEditor
import com.example.ui.viewmodel.AiViewModel
import com.example.ui.viewmodel.BackupViewModel
import com.example.ui.viewmodel.NotesViewModel
import com.example.ui.viewmodel.StorageViewModel
import com.example.ui.viewmodel.ThemeViewModel
import com.example.ui.viewmodel.UpdaterViewModel
import com.example.ui.viewmodel.aiViewModelFactory
import com.example.ui.viewmodel.backupViewModelFactory
import com.example.ui.viewmodel.storageViewModelFactory
import com.example.ui.viewmodel.themeViewModelFactory

/**
 * Vista principal de Ajustes, responsiva para Móvil, Tablet y PC en Material 3 Expressive.
 */
@Composable
fun SettingsApp(
    initialDestination: SettingsDestination,
    notesViewModel: NotesViewModel,
    cipherService: CipherService,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val application = LocalContext.current.applicationContext as android.app.Application
    val themeViewModel: ThemeViewModel = viewModel(factory = themeViewModelFactory(application))
    val aiViewModel: AiViewModel = viewModel(factory = aiViewModelFactory(application, cipherService))
    val backupViewModel: BackupViewModel = viewModel(factory = backupViewModelFactory(application, notesViewModel))
    val storageViewModel: StorageViewModel = viewModel(factory = storageViewModelFactory(application))
    val updaterViewModel: UpdaterViewModel = viewModel()

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedDestination by rememberSaveable {
        mutableStateOf(
            if (initialDestination != SettingsDestination.HUB) initialDestination
            else CATEGORIES.first().destination
        )
    }
    var activeDestination by rememberSaveable {
        mutableStateOf<SettingsDestination?>(
            if (initialDestination != SettingsDestination.HUB) initialDestination
            else null
        )
    }

    LaunchedEffect(initialDestination) {
        if (initialDestination != SettingsDestination.HUB) {
            selectedDestination = initialDestination
            activeDestination = initialDestination
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isTwoPane = maxWidth >= 600.dp

        if (isTwoPane) {
            BackHandler {
                onClose()
            }

            TwoPaneSettingsLayout(
                selectedDestination = selectedDestination,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onSelect = { selectedDestination = it },
                onClose = onClose,
                notesViewModel = notesViewModel,
                themeViewModel = themeViewModel,
                aiViewModel = aiViewModel,
                backupViewModel = backupViewModel,
                storageViewModel = storageViewModel,
                updaterViewModel = updaterViewModel
            )
        } else {
            BackHandler {
                if (activeDestination != null) {
                    activeDestination = null
                } else {
                    onClose()
                }
            }

            AnimatedContent(
                targetState = activeDestination,
                transitionSpec = {
                    if (targetState != null) {
                        (slideInHorizontally { it / 4 } + fadeIn()) togetherWith
                            (slideOutHorizontally { -it / 6 } + fadeOut())
                    } else {
                        (slideInHorizontally { -it / 4 } + fadeIn()) togetherWith
                            (slideOutHorizontally { it / 6 } + fadeOut())
                    }
                },
                label = "mobileSettingsTransition"
            ) { destination ->
                if (destination == null) {
                    MobileSettingsOverview(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onSelect = { activeDestination = it },
                        onClose = onClose
                    )
                } else {
                    MobileSettingsDetail(
                        destination = destination,
                        onBack = { activeDestination = null },
                        notesViewModel = notesViewModel,
                        themeViewModel = themeViewModel,
                        aiViewModel = aiViewModel,
                        backupViewModel = backupViewModel,
                        storageViewModel = storageViewModel,
                        updaterViewModel = updaterViewModel,
                        onNavigate = { activeDestination = it }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TwoPaneSettingsLayout(
    selectedDestination: SettingsDestination,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelect: (SettingsDestination) -> Unit,
    onClose: () -> Unit,
    notesViewModel: NotesViewModel,
    themeViewModel: ThemeViewModel,
    aiViewModel: AiViewModel,
    backupViewModel: BackupViewModel,
    storageViewModel: StorageViewModel,
    updaterViewModel: UpdaterViewModel
) {
    val leftScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(end = 16.dp, bottom = 16.dp)
    ) {
        // Left Pane: Scaffold with MediumTopAppBar + Search + Category List
        Scaffold(
            modifier = Modifier
                .widthIn(min = 290.dp, max = 360.dp)
                .fillMaxHeight()
                .nestedScroll(leftScrollBehavior.nestedScrollConnection),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                MediumTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.nav_settings),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    scrollBehavior = leftScrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        ) { innerPadding ->
            SettingsCategoryListBody(
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                selected = selectedDestination,
                isTwoPane = true,
                onSelect = onSelect,
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 12.dp,
                    top = innerPadding.calculateTopPadding() + 4.dp,
                    bottom = 24.dp
                ),
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Right Pane: Detail Header + Rounded Surface Container
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(top = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = categoryLabel(selectedDestination),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxSize()
            ) {
                AnimatedContent(
                    targetState = selectedDestination,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "twoPaneDetailContent"
                ) { dest ->
                    SettingsDetailContent(
                        destination = dest,
                        notesViewModel = notesViewModel,
                        themeViewModel = themeViewModel,
                        aiViewModel = aiViewModel,
                        backupViewModel = backupViewModel,
                        storageViewModel = storageViewModel,
                        updaterViewModel = updaterViewModel,
                        onNavigate = onSelect,
                        onClose = onClose
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MobileSettingsOverview(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelect: (SettingsDestination) -> Unit,
    onClose: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.nav_settings),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        SettingsCategoryListBody(
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            selected = null,
            isTwoPane = false,
            onSelect = onSelect,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 4.dp,
                bottom = 28.dp
            ),
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MobileSettingsDetail(
    destination: SettingsDestination,
    onBack: () -> Unit,
    notesViewModel: NotesViewModel,
    themeViewModel: ThemeViewModel,
    aiViewModel: AiViewModel,
    backupViewModel: BackupViewModel,
    storageViewModel: StorageViewModel,
    updaterViewModel: UpdaterViewModel,
    onNavigate: (SettingsDestination) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        text = categoryLabel(destination),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
        ) {
            SettingsDetailContent(
                destination = destination,
                notesViewModel = notesViewModel,
                themeViewModel = themeViewModel,
                aiViewModel = aiViewModel,
                backupViewModel = backupViewModel,
                storageViewModel = storageViewModel,
                updaterViewModel = updaterViewModel,
                onNavigate = onNavigate,
                onClose = onBack
            )
        }
    }
}

private data class SearchMatchOption(
    val title: String,
    val destination: SettingsDestination,
    val categoryName: String,
    val icon: ImageVector
)

@Composable
internal fun SettingsCategoryListBody(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selected: SettingsDestination?,
    isTwoPane: Boolean,
    onSelect: (SettingsDestination) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isSearching = searchQuery.isNotBlank()

    val (matchingCategories, matchingOptions) = remember(searchQuery) {
        if (!isSearching) {
            Pair(CATEGORIES, emptyList<SearchMatchOption>())
        } else {
            val q = searchQuery.trim()
            val cats = CATEGORIES.filter { info ->
                val haystack = buildList {
                    add(context.getString(info.labelRes))
                    add(context.getString(info.summaryRes))
                    searchTermsFor(info.destination).forEach { add(context.getString(it)) }
                }
                matchesQuery(haystack, q)
            }
            val options = mutableListOf<SearchMatchOption>()
            CATEGORIES.forEach { info ->
                val terms = searchTermsFor(info.destination)
                terms.forEach { termRes ->
                    val termStr = context.getString(termRes)
                    if (termStr.contains(q, ignoreCase = true)) {
                        options.add(
                            SearchMatchOption(
                                title = termStr,
                                destination = info.destination,
                                categoryName = context.getString(info.labelRes),
                                icon = categoryIcon(info.destination)
                            )
                        )
                    }
                }
            }
            Pair(cats, options.distinctBy { it.title })
        }
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        item(key = "search_bar") {
            SettingsSearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        if (isSearching) {
            if (matchingCategories.isEmpty() && matchingOptions.isEmpty()) {
                item(key = "empty_state") {
                    SettingsEmptySearchState(query = searchQuery)
                }
            } else {
                if (matchingCategories.isNotEmpty()) {
                    item(key = "header_matching_categories") {
                        SettingsSectionHeader(title = stringResource(R.string.settings_group_general))
                    }
                    items(matchingCategories.size, key = { "cat_${matchingCategories[it].destination.name}" }) { index ->
                        val cat = matchingCategories[index]
                        SettingsCategoryCardRow(
                            info = cat,
                            isSelected = selected == cat.destination,
                            isTwoPane = isTwoPane,
                            shape = groupedCardShape(index, matchingCategories.size),
                            onClick = { onSelect(cat.destination) }
                        )
                    }
                }

                if (matchingOptions.isNotEmpty()) {
                    item(key = "header_matching_options") {
                        SettingsSectionHeader(title = stringResource(R.string.settings_search_placeholder))
                    }
                    items(matchingOptions.size, key = { "opt_${it}_${matchingOptions[it].title}" }) { index ->
                        val opt = matchingOptions[index]
                        SettingsSearchOptionRow(
                            title = opt.title,
                            categoryName = opt.categoryName,
                            icon = opt.icon,
                            shape = groupedCardShape(index, matchingOptions.size),
                            onClick = { onSelect(opt.destination) }
                        )
                    }
                }
            }
        } else {
            SettingsGroup.entries.forEach { group ->
                val itemsInGroup = CATEGORIES.filter { it.group == group }
                if (itemsInGroup.isNotEmpty()) {
                    item(key = "header_${group.name}") {
                        SettingsSectionHeader(title = stringResource(group.labelRes))
                    }
                    items(
                        count = itemsInGroup.size,
                        key = { idx -> "item_${itemsInGroup[idx].destination.name}" }
                    ) { idx ->
                        val cat = itemsInGroup[idx]
                        SettingsCategoryCardRow(
                            info = cat,
                            isSelected = selected == cat.destination,
                            isTwoPane = isTwoPane,
                            shape = groupedCardShape(idx, itemsInGroup.size),
                            onClick = { onSelect(cat.destination) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun SettingsCategoryList(
    selected: SettingsDestination?,
    showChevron: Boolean = true,
    onScroll: (Int) -> Unit = {},
    onSelect: (SettingsDestination) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    SettingsCategoryListBody(
        searchQuery = query,
        onSearchQueryChange = { query = it },
        selected = selected,
        isTwoPane = !showChevron,
        onSelect = onSelect,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
internal fun SettingsDetailContent(
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

/** Encabezado del detalle con título grande y cierre. */
@Composable
internal fun SettingsDetailHeader(title: String, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
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

internal fun categoryIcon(destination: SettingsDestination): ImageVector = when (destination) {
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
