package com.example.ui

import kotlinx.coroutines.launch
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.AppConstants
import com.example.NavigationRailContent
import com.example.R
import com.example.data.model.Note
import com.example.data.model.Tag
import com.example.data.model.parseNoteContentAndAttachments
import com.example.ui.viewmodel.DecryptedNote
import com.example.ui.viewmodel.NotesViewModel
import com.example.ui.viewmodel.NavigationSection
import com.example.util.MoveDirection
import com.example.util.RichTextParser
import com.example.util.SortOption
import com.example.util.borderStrokeHelper
import com.example.util.getNoteBackgroundColor
import com.example.util.reorderNote
import com.example.util.swapNotes
import kotlin.math.roundToInt
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MainListScreen(
    viewModel: NotesViewModel,
    onNavigateToEditor: (Int) -> Unit,
    onNavigateToCloud: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToDrawing: (Int, String?) -> Unit,
    onNavigateToMediaViewer: (String, String) -> Unit,
    onNavigateToSettingsHub: () -> Unit = {},
    onNavigateToBackupRestore: () -> Unit = {},
    onNavigateToUpdateInfo: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {}
) {
    val currentSection by viewModel.currentSection.collectAsState()
    val notes by viewModel.notesList.collectAsState()
    val tags by viewModel.availableTags.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTagFilter by viewModel.selectedTagFilter.collectAsState()
    val isDriveLinked by viewModel.isDriveLinked.collectAsState()

    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE) }
    
    var isGridView by remember { 
        mutableStateOf(prefs.getBoolean("is_grid_view", false)) 
    }
    
    var sortOption by remember {
        val savedName = prefs.getString("sort_option", SortOption.LAST_MODIFIED.name) ?: SortOption.LAST_MODIFIED.name
        mutableStateOf(try { SortOption.valueOf(savedName) } catch (e: Exception) { SortOption.LAST_MODIFIED })
    }
    
    var customOrderStr by remember {
        mutableStateOf(prefs.getString(AppConstants.CUSTOM_ORDER_KEY, "") ?: "")
    }
    
    var showSortBottomSheet by remember { mutableStateOf(false) }
    var draggedNoteId by remember { mutableStateOf<Int?>(null) }
    var dragOffsetX by remember { mutableStateOf(0f) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    val configuration = LocalConfiguration.current
    val widthClass = when {
        configuration.screenWidthDp < 600 -> WindowWidthSizeClass.Compact
        configuration.screenWidthDp < 840 -> WindowWidthSizeClass.Medium
        else -> WindowWidthSizeClass.Expanded
    }
    val isLargeScreen = widthClass != WindowWidthSizeClass.Compact
    var isNavExtended by remember(isLargeScreen) { mutableStateOf(widthClass == WindowWidthSizeClass.Expanded) }
    val gridColumns = when (widthClass) {
        WindowWidthSizeClass.Compact -> 2
        WindowWidthSizeClass.Medium -> 2
        else -> 3
    }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    BackHandler(enabled = currentSection != com.example.ui.viewmodel.NavigationSection.HOME) {
        viewModel.currentSection.value = com.example.ui.viewmodel.NavigationSection.HOME
    }

    val sortedNotes = remember(notes, sortOption, customOrderStr) {
        val currentIds = if (customOrderStr.isNotEmpty()) {
            customOrderStr.split(",").mapNotNull { it.toIntOrNull() }
        } else {
            emptyList()
        }
        val baseSorted = when (sortOption) {
            SortOption.ALPHABETICAL -> {
                notes.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
            }
            SortOption.LAST_MODIFIED -> {
                notes.sortedByDescending { it.note.lastModified }
            }
            SortOption.CUSTOM -> {
                notes.sortedBy { decryptedNote ->
                    val idx = currentIds.indexOf(decryptedNote.note.id)
                    if (idx != -1) idx else Int.MAX_VALUE
                }
            }
        }
        baseSorted.sortedWith(compareByDescending { it.note.isPinned })
    }
    
    var showCreateTagDialog by remember { mutableStateOf(false) }
    var tagToEdit by remember { mutableStateOf<com.example.data.model.Tag?>(null) }
    var showManageTagsDialog by remember { mutableStateOf(false) }
    var selectedNoteIds by remember { mutableStateOf(emptySet<Int>()) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showBatchTagDialog by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !isLargeScreen,
        drawerContent = {
            if (!isLargeScreen) {
                ModalDrawerSheet(
                    drawerContainerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.width(280.dp),
                    drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                ) {
                    NavigationRailContent(
                        currentSection = currentSection,
                        onSectionSelected = { section ->
                            if (section == com.example.ui.viewmodel.NavigationSection.SETTINGS) {
                                scope.launch { drawerState.close() }
                                onNavigateToSettingsHub()
                            } else {
                                viewModel.currentSection.value = section
                                selectedNoteIds = emptySet()
                                scope.launch { drawerState.close() }
                            }
                        },
                        isExtended = true,
                        onToggleExtend = {},
                        widthClass = widthClass
                    )
                }
            }
        }
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (isLargeScreen) {
                // Left side collapsible navigation rail
                NavigationRailContent(
                    currentSection = currentSection,
                    onSectionSelected = { section ->
                        if (section == com.example.ui.viewmodel.NavigationSection.SETTINGS) {
                            onNavigateToSettingsHub()
                        } else {
                            viewModel.currentSection.value = section
                            selectedNoteIds = emptySet()
                        }
                    },
                    isExtended = isNavExtended,
                    onToggleExtend = { isNavExtended = !isNavExtended },
                    widthClass = widthClass
                )

                // Custom division line
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }

            // Right side main contents pane
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Scaffold(
                    topBar = {
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .statusBarsPadding()
                                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            if (selectedNoteIds.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { selectedNoteIds = emptySet() },
                                            modifier = Modifier.testTag("cancel_selection_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = stringResource(id = R.string.menu_cancel_selection),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = selectedNoteIds.size.toString(),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {

                                        IconButton(
                                            onClick = {
                                                viewModel.batchTogglePin(selectedNoteIds)
                                                selectedNoteIds = emptySet()
                                            },
                                            modifier = Modifier.testTag("batch_pin_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PushPin,
                                                contentDescription = stringResource(id = R.string.menu_batch_pin),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                viewModel.batchToggleFavorite(selectedNoteIds)
                                                selectedNoteIds = emptySet()
                                            },
                                            modifier = Modifier.testTag("batch_favorite_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Favorite,
                                                contentDescription = stringResource(id = R.string.menu_batch_favorite),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                viewModel.batchToggleArchive(selectedNoteIds)
                                                selectedNoteIds = emptySet()
                                            },
                                            modifier = Modifier.testTag("batch_archive_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Archive,
                                                contentDescription = stringResource(id = R.string.menu_batch_archive),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                showBatchTagDialog = true
                                            },
                                            modifier = Modifier.testTag("batch_tag_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Label,
                                                contentDescription = stringResource(id = R.string.menu_batch_tag),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                showShareSheet = true
                                            },
                                            modifier = Modifier.testTag("batch_share_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = stringResource(id = R.string.menu_batch_share),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        IconButton(
                                            onClick = { showDeleteConfirmation = true },
                                            modifier = Modifier.testTag("delete_selected_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = stringResource(id = R.string.menu_delete_selected),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    if (isLargeScreen) {
                                                        isNavExtended = !isNavExtended
                                                    } else {
                                                        scope.launch { drawerState.open() }
                                                    }
                                                },
                                                modifier = Modifier.testTag("toggle_rail_btn")
                                            ) {
                                                Icon(
                                                    imageVector = if (isLargeScreen && isNavExtended) Icons.AutoMirrored.Filled.MenuOpen else Icons.Default.Menu,
                                                    contentDescription = stringResource(R.string.toggle_navigation_rail)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Column {
                                                Text(
                                                    text = when (currentSection) {
                                                        com.example.ui.viewmodel.NavigationSection.HOME -> stringResource(id = R.string.app_name)
                                                        com.example.ui.viewmodel.NavigationSection.FAVORITES -> stringResource(id = R.string.nav_favorites_title)
                                                        com.example.ui.viewmodel.NavigationSection.ARCHIVED -> stringResource(id = R.string.nav_archived_title)
                                                        com.example.ui.viewmodel.NavigationSection.TRASH -> stringResource(id = R.string.nav_trash_title)
                                                        else -> stringResource(id = R.string.app_name)
                                                    },
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.Shield,
                                                        contentDescription = stringResource(R.string.security_active),
                                                        tint = Color(0xFF43A047),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = stringResource(R.string.e2ee_active),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF43A047)
                                                    )
                                                }
                                            }
                                        }
                                        if (currentSection != com.example.ui.viewmodel.NavigationSection.ARCHIVED &&
                                            currentSection != com.example.ui.viewmodel.NavigationSection.TRASH
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    isGridView = !isGridView
                                                    prefs.edit().putBoolean("is_grid_view", isGridView).apply()
                                                },
                                                modifier = Modifier.testTag("toggle_view_mode_btn")
                                            ) {
                                                Icon(
                                                    imageVector = if (isGridView) Icons.AutoMirrored.Filled.FormatListBulleted else Icons.Default.GridView,
                                                    contentDescription = stringResource(id = R.string.menu_toggle_view),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            IconButton(
                                                onClick = { showSortBottomSheet = true },
                                                modifier = Modifier.testTag("sort_options_btn")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                                    contentDescription = stringResource(id = R.string.menu_sort_options),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .testTag("search_field")
                                            .clickable { onNavigateToSearch() },
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Search,
                                                contentDescription = stringResource(R.string.search_icon),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = stringResource(id = R.string.search_placeholder),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    floatingActionButton = {
                        if (selectedNoteIds.isEmpty() && currentSection != com.example.ui.viewmodel.NavigationSection.TRASH) {
                            FloatingActionButton(
                                onClick = { onNavigateToEditor(0) },
                                modifier = Modifier.testTag("new_note_fab"),
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ) {
                                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create_note))
                            }
                        }
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // Tag filters row - only show on HOME or FAVORITES
                        if (currentSection == com.example.ui.viewmodel.NavigationSection.HOME ||
                            currentSection == com.example.ui.viewmodel.NavigationSection.FAVORITES) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { viewModel.selectedTagFilter.value = null },
                                    modifier = Modifier.padding(end = 8.dp),
                                    colors = ButtonDefaults.textButtonColors(
                                        containerColor = if (selectedTagFilter == null) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.all_notes),
                                        fontWeight = if (selectedTagFilter == null) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedTagFilter == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                tags.forEach { tag ->
                                    val isSelected = selectedTagFilter == tag.name
                                    Card(
                                        modifier = Modifier
                                            .padding(end = 8.dp)
                                            .combinedClickable(
                                                onClick = { viewModel.selectedTagFilter.value = tag.name },
                                                onLongClick = { tagToEdit = tag }
                                            ),
                                        border = borderStrokeHelper(isSelected, Color(android.graphics.Color.parseColor(tag.colorHex))),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) Color(android.graphics.Color.parseColor(tag.colorHex)).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(android.graphics.Color.parseColor(tag.colorHex)))
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = tag.name,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }

                                IconButton(onClick = { showCreateTagDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.AddCircle,
                                        contentDescription = stringResource(R.string.create_tag),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                IconButton(onClick = { showManageTagsDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = stringResource(R.string.manage_tags),
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }

                        // Banner to empty trash
                        if (currentSection == com.example.ui.viewmodel.NavigationSection.TRASH && notes.isNotEmpty()) {
                            var showEmptyTrashAlert by remember { mutableStateOf(false) }

                            if (showEmptyTrashAlert) {
                                AlertDialog(
                                    onDismissRequest = { showEmptyTrashAlert = false },
                                    title = { Text(stringResource(id = R.string.action_empty_trash)) },
                                    text = { Text(stringResource(id = R.string.alert_empty_trash_msg)) },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                notes.forEach { viewModel.deletePermanently(it.note) }
                                                showEmptyTrashAlert = false
                                                selectedNoteIds = emptySet()
                                            },
                                            modifier = Modifier.testTag("confirm_empty_trash_ok")
                                        ) {
                                            Text(stringResource(id = R.string.action_delete_perm), color = MaterialTheme.colorScheme.error)
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showEmptyTrashAlert = false }) {
                                            Text(stringResource(id = R.string.btn_cancel))
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            OutlinedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .border(1.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(id = R.string.nav_trash_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = stringResource(R.string.deleted_notes_info),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Button(
                                        onClick = { showEmptyTrashAlert = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("empty_trash_btn")
                                    ) {
                                        Icon(Icons.Default.DeleteSweep, contentDescription = stringResource(R.string.empty_trash), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(stringResource(id = R.string.action_empty_trash), fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        // Notes grid/list scrollarea
                        if (notes.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.img_notes_empty),
                                        contentDescription = stringResource(id = R.string.cd_empty_notes_banner),
                                        modifier = Modifier
                                            .size(180.dp)
                                            .clip(RoundedCornerShape(24.dp))
                                            .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text(
                                        text = stringResource(id = R.string.status_empty),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            if (isGridView) {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(gridColumns),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(bottom = 80.dp)
                                ) {
                                    gridItems(sortedNotes) { decryptedNote ->
                                        val isThisDragged = draggedNoteId == decryptedNote.note.id
                                        val isCustomOrderActive = (sortOption == SortOption.CUSTOM && currentSection == com.example.ui.viewmodel.NavigationSection.HOME)
                                        
                                        val dragModifier = if (isCustomOrderActive) {
                                            Modifier.pointerInput(decryptedNote.note.id) {
                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = { offset ->
                                                        draggedNoteId = decryptedNote.note.id
                                                        dragOffsetX = 0f
                                                        dragOffsetY = 0f
                                                    },
                                                    onDragEnd = {
                                                        draggedNoteId = null
                                                        dragOffsetX = 0f
                                                        dragOffsetY = 0f
                                                    },
                                                    onDragCancel = {
                                                        draggedNoteId = null
                                                        dragOffsetX = 0f
                                                        dragOffsetY = 0f
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        dragOffsetX += dragAmount.x
                                                        dragOffsetY += dragAmount.y
                                                        
                                                        val currentIndex = sortedNotes.indexOfFirst { it.note.id == decryptedNote.note.id }
                                                        if (currentIndex != -1) {
                                                            val density = this.density
                                                            val xThreshold = with(density) { 130.dp.toPx() }
                                                            val yThreshold = with(density) { 150.dp.toPx() }
                                                            
                                                            var swapped = false
                                                            
                                                            if (dragOffsetX > xThreshold && currentIndex % 2 == 0 && currentIndex + 1 < sortedNotes.size) {
                                                                swapNotes(sortedNotes[currentIndex].note.id, sortedNotes[currentIndex + 1].note.id, sortedNotes, context)
                                                                dragOffsetX -= xThreshold
                                                                swapped = true
                                                            } else if (dragOffsetX < -xThreshold && currentIndex % 2 == 1 && currentIndex - 1 >= 0) {
                                                                swapNotes(sortedNotes[currentIndex].note.id, sortedNotes[currentIndex - 1].note.id, sortedNotes, context)
                                                                dragOffsetX += xThreshold
                                                                swapped = true
                                                            }
                                                            
                                                            if (dragOffsetY > yThreshold && currentIndex + 2 < sortedNotes.size) {
                                                                swapNotes(sortedNotes[currentIndex].note.id, sortedNotes[currentIndex + 2].note.id, sortedNotes, context)
                                                                dragOffsetY -= yThreshold
                                                                swapped = true
                                                            } else if (dragOffsetY < -yThreshold && currentIndex - 2 >= 0) {
                                                                swapNotes(sortedNotes[currentIndex].note.id, sortedNotes[currentIndex - 2].note.id, sortedNotes, context)
                                                                dragOffsetY += yThreshold
                                                                swapped = true
                                                            }
                                                            
                                                            if (swapped) {
                                                                customOrderStr = prefs.getString(AppConstants.CUSTOM_ORDER_KEY, "") ?: ""
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                        } else {
                                            Modifier
                                        }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .zIndex(if (isThisDragged) 10f else 1f)
                                                .scale(if (isThisDragged) 1.06f else 1f)
                                                .offset {
                                                    if (isThisDragged) {
                                                        IntOffset(dragOffsetX.roundToInt(), dragOffsetY.roundToInt())
                                                    } else {
                                                        IntOffset(0, 0)
                                                    }
                                                }
                                        ) {
                                            NoteCardItem(
                                                decryptedNote = decryptedNote,
                                                selected = selectedNoteIds.contains(decryptedNote.note.id),
                                                isCustomOrderActive = isCustomOrderActive,
                                                isInTrash = currentSection == com.example.ui.viewmodel.NavigationSection.TRASH,
                                                isGrid = true,
                                                onNavigateToDrawing = onNavigateToDrawing,
                                                onNavigateToMediaViewer = onNavigateToMediaViewer,
                                                onMoveUp = {
                                                    reorderNote(decryptedNote.note.id, MoveDirection.UP, sortedNotes, context)
                                                    customOrderStr = prefs.getString(AppConstants.CUSTOM_ORDER_KEY, "") ?: ""
                                                },
                                                onMoveDown = {
                                                    reorderNote(decryptedNote.note.id, MoveDirection.DOWN, sortedNotes, context)
                                                    customOrderStr = prefs.getString(AppConstants.CUSTOM_ORDER_KEY, "") ?: ""
                                                },
                                                onToggleFavorite = { viewModel.toggleFavorite(decryptedNote.note) },
                                                onToggleArchive = { viewModel.toggleArchive(decryptedNote.note) },
                                                onRestore = {
                                                    viewModel.restoreFromTrash(decryptedNote.note)
                                                    Toast.makeText(context, context.getString(R.string.toast_restored), Toast.LENGTH_SHORT).show()
                                                },
                                                onDeletePermanently = {
                                                    viewModel.deletePermanently(decryptedNote.note)
                                                    Toast.makeText(context, context.getString(R.string.toast_deleted_perm), Toast.LENGTH_SHORT).show()
                                                },
                                                onClick = {
                                                    if (selectedNoteIds.isNotEmpty()) {
                                                        selectedNoteIds = emptySet() // Exit selection mode when tapping any note card
                                                    } else {
                                                        if (currentSection != com.example.ui.viewmodel.NavigationSection.TRASH) {
                                                            onNavigateToEditor(decryptedNote.note.id)
                                                        } else {
                                                            viewModel.restoreFromTrash(decryptedNote.note)
                                                            Toast.makeText(context, context.getString(R.string.toast_restored), Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                },
                                                onLongClick = {
                                                    if (currentSection != com.example.ui.viewmodel.NavigationSection.TRASH) {
                                                        selectedNoteIds = if (selectedNoteIds.contains(decryptedNote.note.id)) {
                                                            selectedNoteIds - decryptedNote.note.id
                                                        } else {
                                                            selectedNoteIds + decryptedNote.note.id
                                                        }
                                                    }
                                                },
                                                dragModifier = dragModifier
                                            )
                                        }
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(bottom = 80.dp)
                                ) {
                                    items(sortedNotes) { decryptedNote ->
                                        NoteCardItem(
                                            decryptedNote = decryptedNote,
                                            selected = selectedNoteIds.contains(decryptedNote.note.id),
                                            isCustomOrderActive = (sortOption == SortOption.CUSTOM && currentSection == com.example.ui.viewmodel.NavigationSection.HOME),
                                            isInTrash = currentSection == com.example.ui.viewmodel.NavigationSection.TRASH,
                                            isGrid = false,
                                            onNavigateToDrawing = onNavigateToDrawing,
                                            onNavigateToMediaViewer = onNavigateToMediaViewer,
                                            onMoveUp = {
                                                reorderNote(decryptedNote.note.id, MoveDirection.UP, sortedNotes, context)
                                                customOrderStr = prefs.getString(AppConstants.CUSTOM_ORDER_KEY, "") ?: ""
                                            },
                                            onMoveDown = {
                                                reorderNote(decryptedNote.note.id, MoveDirection.DOWN, sortedNotes, context)
                                                customOrderStr = prefs.getString(AppConstants.CUSTOM_ORDER_KEY, "") ?: ""
                                            },
                                            onToggleFavorite = { viewModel.toggleFavorite(decryptedNote.note) },
                                            onToggleArchive = { viewModel.toggleArchive(decryptedNote.note) },
                                            onRestore = {
                                                viewModel.restoreFromTrash(decryptedNote.note)
                                                Toast.makeText(context, context.getString(R.string.toast_restored), Toast.LENGTH_SHORT).show()
                                            },
                                            onDeletePermanently = {
                                                viewModel.deletePermanently(decryptedNote.note)
                                                Toast.makeText(context, context.getString(R.string.toast_deleted_perm), Toast.LENGTH_SHORT).show()
                                            },
                                            onClick = {
                                                if (selectedNoteIds.isNotEmpty()) {
                                                    selectedNoteIds = emptySet() // Exit selection mode when tapping any note card
                                                } else {
                                                    if (currentSection != com.example.ui.viewmodel.NavigationSection.TRASH) {
                                                        onNavigateToEditor(decryptedNote.note.id)
                                                    } else {
                                                        viewModel.restoreFromTrash(decryptedNote.note)
                                                        Toast.makeText(context, context.getString(R.string.toast_restored), Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            onLongClick = {
                                                if (currentSection != com.example.ui.viewmodel.NavigationSection.TRASH) {
                                                    selectedNoteIds = if (selectedNoteIds.contains(decryptedNote.note.id)) {
                                                        selectedNoteIds - decryptedNote.note.id
                                                    } else {
                                                        selectedNoteIds + decryptedNote.note.id
                                                    }
                                                }
                                            }
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

    if (showCreateTagDialog) {
        CreateTagDialog(
            viewModel = viewModel,
            onDismiss = { showCreateTagDialog = false }
        )
    }

    if (tagToEdit != null) {
        EditTagDialog(
            tag = tagToEdit!!,
            viewModel = viewModel,
            onDismiss = { tagToEdit = null }
        )
    }

    if (showManageTagsDialog) {
        ManageTagsDialog(
            viewModel = viewModel,
            onDismiss = { showManageTagsDialog = false }
        )
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(text = stringResource(id = R.string.confirm_delete_title)) },
            text = { Text(text = stringResource(id = R.string.confirm_delete_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        notes.forEach { decryptedNote ->
                            if (selectedNoteIds.contains(decryptedNote.note.id)) {
                                if (currentSection == com.example.ui.viewmodel.NavigationSection.TRASH) {
                                    viewModel.deletePermanently(decryptedNote.note)
                                    Toast.makeText(context, context.getString(R.string.toast_deleted_perm), Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.moveToTrash(decryptedNote.note)
                                    Toast.makeText(context, context.getString(R.string.toast_moved_trash), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        selectedNoteIds = emptySet()
                        showDeleteConfirmation = false
                    },
                    modifier = Modifier.testTag("confirm_delete_ok")
                ) {
                    Text(text = stringResource(id = R.string.btn_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmation = false },
                    modifier = Modifier.testTag("confirm_delete_cancel")
                ) {
                    Text(text = stringResource(id = R.string.btn_cancel))
                }
            },
            shape = RoundedCornerShape(12.dp)
        )
    }

    if (showBatchTagDialog) {
        BatchTagDialog(
            selectedNoteIds = selectedNoteIds,
            notes = notes,
            viewModel = viewModel,
            onDismiss = { showBatchTagDialog = false },
            onTagsUpdated = {
                selectedNoteIds = emptySet()
                showBatchTagDialog = false
            }
        )
    }

    if (showShareSheet) {
        val selectedNotesList = remember(notes, selectedNoteIds) {
            notes.filter { it.note.id in selectedNoteIds }
        }
        ShareFormatSheet(
            selectedNotes = selectedNotesList,
            onDismiss = { showShareSheet = false }
        )
    }

    if (showSortBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSortBottomSheet = false },
            sheetState = rememberModalBottomSheetState(),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = 48.dp)
            ) {
                // Header
                Text(
                    text = stringResource(id = R.string.bottom_sheet_sort_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 1. Alphabetical sorting option item
                SortOptionRow(
                    label = stringResource(id = R.string.sort_alphabetical),
                    selected = sortOption == SortOption.ALPHABETICAL,
                    onClick = {
                        sortOption = SortOption.ALPHABETICAL
                        prefs.edit().putString("sort_option", SortOption.ALPHABETICAL.name).apply()
                        showSortBottomSheet = false
                    },
                    icon = Icons.Default.SortByAlpha,
                    testTag = "sort_alphabetical_opt"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Mod Date sorting option item
                SortOptionRow(
                    label = stringResource(id = R.string.sort_last_modified),
                    selected = sortOption == SortOption.LAST_MODIFIED,
                    onClick = {
                        sortOption = SortOption.LAST_MODIFIED
                        prefs.edit().putString("sort_option", SortOption.LAST_MODIFIED.name).apply()
                        showSortBottomSheet = false
                    },
                    icon = Icons.Default.DateRange,
                    testTag = "sort_last_modified_opt"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Custom sorting option item
                SortOptionRow(
                    label = stringResource(id = R.string.sort_custom),
                    selected = sortOption == SortOption.CUSTOM,
                    onClick = {
                        sortOption = SortOption.CUSTOM
                        prefs.edit().putString("sort_option", SortOption.CUSTOM.name).apply()
                        showSortBottomSheet = false
                    },
                    icon = Icons.Default.DragHandle,
                    testTag = "sort_custom_opt"
                )
            }
        }
    }
}

@Composable
fun SortOptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    testTag: String
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.selected_label),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCardItem(
    decryptedNote: DecryptedNote,
    selected: Boolean,
    isCustomOrderActive: Boolean = false,
    isInTrash: Boolean = false,
    isGrid: Boolean = false,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onToggleFavorite: (() -> Unit)? = null,
    onToggleArchive: (() -> Unit)? = null,
    onRestore: (() -> Unit)? = null,
    onDeletePermanently: (() -> Unit)? = null,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    dragModifier: Modifier = Modifier,
    onNavigateToDrawing: ((Int, String?) -> Unit)? = null,
    onNavigateToMediaViewer: ((type: String, src: String) -> Unit)? = null
) {
    val note = decryptedNote.note
    val cleanDateStr = SimpleDateFormat("LLL dd, yyyy HH:mm", Locale.getDefault()).format(Date(note.lastModified))
    
    val tagsList = remember(note.tagsJson) {
        try {
            val arr = JSONArray(note.tagsJson)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                list.add(arr.optString(i))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .border(
                width = if (selected) 2.5.dp else 1.5.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else if (note.isEncrypted) Color(0xFF43A047).copy(alpha = 0.8f) else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            } else if (note.backgroundColor != null && note.backgroundColor != 0) {
                getNoteBackgroundColor(note.backgroundColor)
            } else if (note.isEncrypted) {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = decryptedNote.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = if (isGrid) 1 else 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (note.isPinned) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = stringResource(R.string.pinned_label),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(18.dp)
                                .padding(end = 4.dp)
                        )
                    }
                    if (note.isEncrypted) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = stringResource(R.string.encrypted_label),
                            tint = Color(0xFF43A047),
                            modifier = Modifier
                                .size(18.dp)
                                .padding(end = 4.dp)
                        )
                    }
                    if (selected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                    contentDescription = stringResource(R.string.selected_label),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            val (cleanNoteText, allAttachments) = remember(decryptedNote.content) {
                com.example.data.model.parseNoteContentAndAttachments(decryptedNote.content)
            }

            val formattedContent = remember(cleanNoteText) {
                com.example.util.RichTextParser.parse(cleanNoteText, hideTags = true)
            }

            Text(
                text = formattedContent,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (isGrid) 3 else 4,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            val visualAttachments = remember(allAttachments, cleanNoteText) {
                val fromLegacy = allAttachments.filter { it.type in listOf("drawing", "image", "video") }
                val fromMediaTags = mutableListOf<Pair<String, String>>()
                val regex = Regex("<(img|video)\\s+src=\"([^\"]+)\"\\s*/>")
                regex.findAll(cleanNoteText).forEach { match ->
                    val tagType = match.groupValues[1]
                    val src = match.groupValues[2]
                    val normType = when (tagType) { "img" -> "image"; else -> tagType }
                    fromMediaTags.add(normType to src)
                }
                fromLegacy.map { Triple(it.type, it.path, it.name) } +
                    fromMediaTags.map { Triple(it.first, it.second, "") }
            }

            if (visualAttachments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    visualAttachments.forEach { (type, pathOrSrc, thumbPath) ->
                        OutlinedCard(
                            modifier = Modifier
                                .width(120.dp)
                                .height(90.dp)
                                .clickable {
                                    when (type) {
                                        "drawing" -> onNavigateToDrawing?.invoke(note.id, pathOrSrc)
                                        else -> onNavigateToMediaViewer?.invoke(type, pathOrSrc)
                                    }
                                },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                when (type) {
                                    "drawing" -> {
                                        AsyncImage(
                                            model = thumbPath,
                                            contentDescription = stringResource(R.string.attachment_drawing),
                                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                    "image" -> {
                                        AsyncImage(
                                            model = pathOrSrc,
                                            contentDescription = stringResource(R.string.attachment_image),
                                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    "video" -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = stringResource(id = R.string.cd_play_video),
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isGrid) {
                if (tagsList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tagsList.take(2).forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = tag,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isInTrash) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(
                                onClick = onRestore ?: {},
                                modifier = Modifier.size(32.dp).testTag("note_restore_${note.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Restore,
                                    contentDescription = stringResource(id = R.string.action_restore),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = onDeletePermanently ?: {},
                                modifier = Modifier.size(32.dp).testTag("note_delete_perm_${note.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteForever,
                                    contentDescription = stringResource(id = R.string.action_delete_perm),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    } else {
                        if (isCustomOrderActive && onMoveUp != null && onMoveDown != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!isGrid) {
                                    IconButton(
                                        onClick = onMoveUp,
                                        modifier = Modifier.size(32.dp).testTag("move_up_${note.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = stringResource(id = R.string.action_move_up),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(2.dp))
                                    IconButton(
                                        onClick = onMoveDown,
                                        modifier = Modifier.size(32.dp).testTag("move_down_${note.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDownward,
                                            contentDescription = stringResource(id = R.string.action_move_down),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(2.dp))
                                }
                                IconButton(
                                    onClick = {},
                                    modifier = Modifier
                                        .size(32.dp)
                                        .then(dragModifier)
                                        .testTag("drag_handle_${note.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DragHandle,
                                        contentDescription = stringResource(R.string.drag_to_reorder),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = cleanDateStr,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Light,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (onToggleFavorite != null) {
                                    IconButton(
                                        onClick = onToggleFavorite,
                                        modifier = Modifier.size(32.dp).testTag("note_favorite_${note.id}")
                                    ) {
                                        Icon(
                                            imageVector = if (note.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                            contentDescription = stringResource(id = R.string.nav_favorites),
                                            tint = if (note.isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                if (onToggleArchive != null) {
                                    IconButton(
                                        onClick = onToggleArchive,
                                        modifier = Modifier.size(32.dp).testTag("note_archive_${note.id}")
                                    ) {
                                        Icon(
                                            imageVector = if (note.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                            contentDescription = stringResource(id = R.string.nav_archived),
                                            tint = if (note.isArchived) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // List of tag indicators
                    Row(modifier = Modifier.weight(1f)) {
                        tagsList.take(3).forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(tag, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (isInTrash) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onRestore ?: {},
                                modifier = Modifier.size(32.dp).testTag("note_restore_${note.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Restore,
                                    contentDescription = stringResource(id = R.string.action_restore),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = onDeletePermanently ?: {},
                                modifier = Modifier.size(32.dp).testTag("note_delete_perm_${note.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteForever,
                                    contentDescription = stringResource(id = R.string.action_delete_perm),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (onToggleFavorite != null) {
                                IconButton(
                                    onClick = onToggleFavorite,
                                    modifier = Modifier.size(32.dp).testTag("note_favorite_${note.id}")
                                ) {
                                    Icon(
                                        imageVector = if (note.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = stringResource(id = R.string.nav_favorites),
                                        tint = if (note.isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            if (onToggleArchive != null) {
                                IconButton(
                                    onClick = onToggleArchive,
                                    modifier = Modifier.size(32.dp).testTag("note_archive_${note.id}")
                                ) {
                                    Icon(
                                        imageVector = if (note.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                        contentDescription = stringResource(id = R.string.nav_archived),
                                        tint = if (note.isArchived) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            if (isCustomOrderActive && onMoveUp != null && onMoveDown != null) {
                                IconButton(
                                    onClick = onMoveUp,
                                    modifier = Modifier.size(32.dp).testTag("move_up_${note.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = stringResource(id = R.string.action_move_up),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(2.dp))
                                IconButton(
                                    onClick = onMoveDown,
                                    modifier = Modifier.size(32.dp).testTag("move_down_${note.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = stringResource(id = R.string.action_move_down),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else {
                                Text(
                                    text = cleanDateStr,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Light,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }
                }
            } }
        }
    }
