package com.example.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.AppConstants
import com.example.ui.NoteCardItem
import com.example.R
import com.example.data.model.DecryptedNote
import com.example.ui.viewmodel.NotesViewModel
import com.example.util.getColorName
import com.example.util.getNoteBackgroundColor
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    viewModel: NotesViewModel,
    onNavigateToEditor: (Int) -> Unit,
    onBack: () -> Unit,
    onNavigateToDrawing: (Int, String?) -> Unit,
    onNavigateToMediaViewer: (String, String) -> Unit
) {
    BackHandler(onBack = onBack)
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE) }

    val isGridView = remember { prefs.getBoolean("is_grid_view", false) }

    var recentSearches by remember {
        mutableStateOf(
            prefs.getString("recent_searches", "")?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        )
    }

    var filterFavorite by remember { mutableStateOf(false) }
    var filterArchived by remember { mutableStateOf(false) }
    var filterTag by remember { mutableStateOf<String?>(null) }
    var filterColorId by remember { mutableStateOf<Int?>(null) }

    val allTags by viewModel.availableTags.collectAsState()

    var showTagDropdown by remember { mutableStateOf(false) }
    var showColorDropdown by remember { mutableStateOf(false) }

    val filteredResults = remember(searchResults, searchQuery, filterFavorite, filterArchived, filterTag, filterColorId) {
        searchResults.filter { decryptedNote ->
            val note = decryptedNote.note

            val matchesQuery = searchQuery.isBlank() ||
                decryptedNote.title.contains(searchQuery, ignoreCase = true) ||
                decryptedNote.content.contains(searchQuery, ignoreCase = true)

            val matchesFavorite = !filterFavorite || note.isFavorite

            val matchesArchived = !filterArchived || note.isArchived

            val matchesTag = filterTag == null || run {
                try {
                    val arr = JSONArray(note.tagsJson)
                    var found = false
                    for (i in 0 until arr.length()) {
                        if (arr.optString(i) == filterTag) {
                            found = true
                            break
                        }
                    }
                    found
                } catch (e: Exception) {
                    false
                }
            }

            val matchesColor = filterColorId == null || (note.backgroundColor == filterColorId)

            matchesQuery && matchesFavorite && matchesArchived && matchesTag && matchesColor
        }
    }

    val isFilteringActive = searchQuery.isNotBlank() || filterFavorite || filterArchived || filterTag != null || filterColorId != null

    val focusRequester = remember { FocusRequester() }

    fun addRecentSearch(query: String) {
        if (query.isBlank()) return
        val trimmed = query.trim()
        val updated = (listOf(trimmed) + recentSearches.filter { it != trimmed }).take(6)
        recentSearches = updated
        prefs.edit().putString("recent_searches", updated.joinToString(",")).apply()
    }

    fun removeRecentSearch(query: String) {
        val updated = recentSearches.filter { it != query }
        recentSearches = updated
        prefs.edit().putString("recent_searches", updated.joinToString(",")).apply()
    }

    fun clearAllRecentSearches() {
        recentSearches = emptyList()
        prefs.edit().remove("recent_searches").apply()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.searchQuery.value = ""
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .statusBarsPadding()
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchQuery.value = it },
                        placeholder = { Text(stringResource(id = R.string.search_placeholder)) },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .testTag("search_input_field"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                addRecentSearch(searchQuery)
                            }
                        ),
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = stringResource(R.string.clear_search),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.label_filter_by),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FilterChip(
                    selected = filterFavorite,
                    onClick = { filterFavorite = !filterFavorite },
                    label = { Text(stringResource(id = R.string.label_favorites)) },
                    leadingIcon = {
                        Icon(
                            imageVector = if (filterFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = stringResource(R.string.favorite_filter),
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("filter_chip_favorite")
                )

                FilterChip(
                    selected = filterArchived,
                    onClick = { filterArchived = !filterArchived },
                    label = { Text(stringResource(id = R.string.label_archived)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = stringResource(R.string.archive_filter),
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("filter_chip_archived")
                )

                Box {
                    FilterChip(
                        selected = filterTag != null,
                        onClick = { showTagDropdown = true },
                        label = { Text(if (filterTag == null) stringResource(id = R.string.label_tags_filter) else "${stringResource(id = R.string.label_tags_filter)}: $filterTag") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.LocalOffer,
                                contentDescription = stringResource(R.string.tag_filter),
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        trailingIcon = {
                            if (filterTag != null) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.clear_tag_filter),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { filterTag = null }
                                )
                            }
                        },
                        modifier = Modifier.testTag("filter_chip_tag")
                    )

                    DropdownMenu(
                        expanded = showTagDropdown,
                        onDismissRequest = { showTagDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.label_all)) },
                            onClick = {
                                filterTag = null
                                showTagDropdown = false
                            }
                        )
                        allTags.forEach { tag ->
                            DropdownMenuItem(
                                text = { Text(tag.name) },
                                onClick = {
                                    filterTag = tag.name
                                    showTagDropdown = false
                                }
                            )
                        }
                    }
                }

                Box {
                    FilterChip(
                        selected = filterColorId != null,
                        onClick = { showColorDropdown = true },
                        label = { Text(if (filterColorId == null) stringResource(id = R.string.label_color_filter) else "${stringResource(id = R.string.label_color_filter)}: ${getColorName(filterColorId)}") },
                        leadingIcon = {
                            if (filterColorId != null && filterColorId != 0) {
                                val isDark = isSystemInDarkTheme()
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(getNoteBackgroundColor(filterColorId, isDark))
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = stringResource(R.string.color_filter),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        },
                        trailingIcon = {
                            if (filterColorId != null) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.clear_color_filter),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { filterColorId = null }
                                )
                            }
                        },
                        modifier = Modifier.testTag("filter_chip_color")
                    )

                    DropdownMenu(
                        expanded = showColorDropdown,
                        onDismissRequest = { showColorDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(id = R.string.label_all)) },
                            onClick = {
                                filterColorId = null
                                showColorDropdown = false
                            }
                        )
                        (1..6).forEach { colorId ->
                            val colorLabel = when (colorId) {
                                1 -> stringResource(id = R.string.label_color_blue)
                                2 -> stringResource(id = R.string.label_color_green)
                                3 -> stringResource(id = R.string.label_color_yellow)
                                4 -> stringResource(id = R.string.label_color_pink)
                                5 -> stringResource(id = R.string.label_color_purple)
                                6 -> stringResource(id = R.string.label_color_orange)
                                else -> ""
                            }
                            val isDark = isSystemInDarkTheme()
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(getNoteBackgroundColor(colorId, isDark))
                                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                        )
                                        Text(colorLabel)
                                    }
                                },
                                onClick = {
                                    filterColorId = colorId
                                    showColorDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            if (!isFilteringActive) {
                if (recentSearches.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(id = R.string.search_recent),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(onClick = { clearAllRecentSearches() }) {
                            Text(
                                text = stringResource(R.string.clear_all),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        recentSearches.forEach { search ->
                            SearchSuggestionChip(
                                text = search,
                                onClick = {
                                    viewModel.searchQuery.value = search
                                    addRecentSearch(search)
                                },
                                onDelete = { removeRecentSearch(search) },
                                modifier = Modifier.testTag("recent_search_chip_$search")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.search_icon_placeholder),
                            modifier = Modifier
                                .size(80.dp)
                                .padding(8.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(id = R.string.search_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                if (filteredResults.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = stringResource(R.string.no_results_icon),
                                modifier = Modifier
                                    .size(80.dp)
                                    .padding(8.dp),
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(id = R.string.search_no_results),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                    if (isGridView) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            gridItems(filteredResults) { decryptedNote ->
                                NoteCardItem(
                                    decryptedNote = decryptedNote,
                                    selected = false,
                                    isGrid = true,
                                    onNavigateToDrawing = onNavigateToDrawing,
                                    onNavigateToMediaViewer = onNavigateToMediaViewer,
                                    onClick = {
                                        addRecentSearch(searchQuery)
                                        onNavigateToEditor(decryptedNote.note.id)
                                    },
                                    onLongClick = {}
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(filteredResults) { decryptedNote ->
                                NoteCardItem(
                                    decryptedNote = decryptedNote,
                                    selected = false,
                                    isGrid = false,
                                    onNavigateToDrawing = onNavigateToDrawing,
                                    onNavigateToMediaViewer = onNavigateToMediaViewer,
                                    onClick = {
                                        addRecentSearch(searchQuery)
                                        onNavigateToEditor(decryptedNote.note.id)
                                    },
                                    onLongClick = {}
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchSuggestionChip(
    text: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.remove_recent_search),
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onDelete() },
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
