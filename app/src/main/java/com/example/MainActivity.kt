package com.example

import kotlinx.coroutines.launch
import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.scale
import kotlin.math.roundToInt
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.content.Intent
import coil.compose.AsyncImage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Note
import com.example.data.model.Tag
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.NoteEditorScreen
import com.example.ui.DrawingCanvasScreen
import com.example.ui.MediaViewerScreen
import com.example.ui.settings.AboutScreen
import com.example.ui.settings.BackupRestoreScreen
import com.example.ui.settings.PrivacySettingsScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.UpdateInfoScreen
import com.example.ui.viewmodel.DecryptedNote
import com.example.ui.viewmodel.NotesViewModel
import com.example.ui.viewmodel.ThemeViewModel
import com.example.ui.viewmodel.BackupViewModel
import com.example.ui.viewmodel.UpdaterViewModel
import com.example.util.ExportUtils
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class Screen {
    object MainList : Screen()
    data class NoteEditor(val noteId: Int) : Screen()
    data class DrawingCanvas(val noteId: Int, val jsonPath: String? = null) : Screen()
    object CloudSync : Screen()
    object PrivacySettings : Screen()
    object Search : Screen()
    data class MediaViewer(val type: String, val src: String, val previousScreen: Screen) : Screen()
    object SettingsHub : Screen()
    object BackupRestore : Screen()
    object UpdateInfo : Screen()
    object About : Screen()
}

val ScreenSaver = mapSaver(
    save = { screen: Screen ->
        when (screen) {
            is Screen.MainList -> mapOf("route" to "main_list")
            is Screen.NoteEditor -> mapOf("route" to "note_editor", "noteId" to screen.noteId)
            is Screen.DrawingCanvas -> mapOf("route" to "drawing_canvas", "noteId" to screen.noteId, "jsonPath" to (screen.jsonPath ?: ""))
            is Screen.CloudSync -> mapOf("route" to "cloud_sync")
            is Screen.PrivacySettings -> mapOf("route" to "privacy_settings")
            is Screen.Search -> mapOf("route" to "search")
            is Screen.MediaViewer -> mapOf("route" to "media_viewer", "type" to screen.type, "src" to screen.src)
            is Screen.SettingsHub -> mapOf("route" to "settings_hub")
            is Screen.BackupRestore -> mapOf("route" to "backup_restore")
            is Screen.UpdateInfo -> mapOf("route" to "update_info")
            is Screen.About -> mapOf("route" to "about")
        }
    },
    restore = { map: Map<String, Any?> ->
        when (map["route"] as? String) {
            "main_list" -> Screen.MainList
            "note_editor" -> Screen.NoteEditor((map["noteId"] as? Int) ?: 0)
            "drawing_canvas" -> Screen.DrawingCanvas((map["noteId"] as? Int) ?: 0, (map["jsonPath"] as? String)?.ifEmpty { null })
            "cloud_sync" -> Screen.CloudSync
            "privacy_settings" -> Screen.PrivacySettings
            "search" -> Screen.Search
            "media_viewer" -> Screen.MediaViewer((map["type"] as? String) ?: "", (map["src"] as? String) ?: "", Screen.MainList)
            "settings_hub" -> Screen.SettingsHub
            "backup_restore" -> Screen.BackupRestore
            "update_info" -> Screen.UpdateInfo
            "about" -> Screen.About
            else -> Screen.MainList
        }
    }
)

enum class SortOption {
    ALPHABETICAL,
    LAST_MODIFIED,
    CUSTOM
}

private enum class MoveDirection { UP, DOWN }

private fun reorderNote(noteId: Int, direction: MoveDirection, notesList: List<DecryptedNote>, context: Context) {
    val prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
    val customOrderStr = prefs.getString(AppConstants.CUSTOM_ORDER_KEY, "") ?: ""
    val currentIds = if (customOrderStr.isNotEmpty()) {
        customOrderStr.split(",").mapNotNull { it.toIntOrNull() }.toMutableList()
    } else {
        notesList.map { it.note.id }.toMutableList()
    }

    if (!currentIds.contains(noteId)) currentIds.add(noteId)

    val index = currentIds.indexOf(noteId)
    val swapIndex = when (direction) {
        MoveDirection.UP -> if (index > 0) index - 1 else return
        MoveDirection.DOWN -> if (index in 0 until currentIds.size - 1) index + 1 else return
    }

    val temp = currentIds[index]
    currentIds[index] = currentIds[swapIndex]
    currentIds[swapIndex] = temp
    prefs.edit().putString(AppConstants.CUSTOM_ORDER_KEY, currentIds.joinToString(",")).apply()
}

private fun swapNotes(id1: Int, id2: Int, notesList: List<DecryptedNote>, context: Context) {
    val prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
    val customOrderStr = prefs.getString(AppConstants.CUSTOM_ORDER_KEY, "") ?: ""
    val currentIds = if (customOrderStr.isNotEmpty()) {
        customOrderStr.split(",").mapNotNull { it.toIntOrNull() }.toMutableList()
    } else {
        notesList.map { it.note.id }.toMutableList()
    }

    if (!currentIds.contains(id1)) currentIds.add(id1)
    if (!currentIds.contains(id2)) currentIds.add(id2)

    val idx1 = currentIds.indexOf(id1)
    val idx2 = currentIds.indexOf(id2)
    if (idx1 != -1 && idx2 != -1) {
        val temp = currentIds[idx1]
        currentIds[idx1] = currentIds[idx2]
        currentIds[idx2] = temp
        prefs.edit().putString(AppConstants.CUSTOM_ORDER_KEY, currentIds.joinToString(",")).apply()
    }
}

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        val lang = prefs.getString(AppConstants.LANGUAGE_KEY, "") ?: ""
        val context = if (lang.isNotEmpty()) {
            val locale = java.util.Locale.forLanguageTag(lang)
            val config = Configuration(newBase.resources.configuration)
            config.setLocale(locale)
            newBase.createConfigurationContext(config)
        } else {
            newBase
        }
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: NotesViewModel = viewModel()
            val themeViewModel: ThemeViewModel = viewModel()
            val darkModeOption by themeViewModel.darkModeOption.collectAsStateWithLifecycle()
            val isDynamicColor by themeViewModel.isDynamicColor.collectAsStateWithLifecycle()

            val isDark = when (darkModeOption) {
                DarkModeOption.SYSTEM -> isSystemInDarkTheme()
                DarkModeOption.ON -> true
                DarkModeOption.OFF -> false
            }

            MyApplicationTheme(darkTheme = isDark, dynamicColor = isDynamicColor) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppMainContent(viewModel, themeViewModel)
                }
            }
        }
    }
}

@Composable
fun AppMainContent(viewModel: NotesViewModel, themeViewModel: ThemeViewModel) {
    val isUnlocked by viewModel.isUnlocked.collectAsState()
    val isPasswordSet by viewModel.isPasswordSet.collectAsState()
    var currentScreen by rememberSaveable(stateSaver = ScreenSaver) { mutableStateOf<Screen>(Screen.MainList) }
    var isBackNavigation by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val backupViewModel: BackupViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return BackupViewModel(context.applicationContext as android.app.Application, viewModel) as T
            }
        }
    )
    val updaterViewModel: UpdaterViewModel = viewModel()

    fun navigateTo(screen: Screen) {
        isBackNavigation = false
        currentScreen = screen
    }

    fun navigateBack(to: Screen) {
        isBackNavigation = true
        currentScreen = to
    }

    // Lock Screen integration
    if (isPasswordSet && !isUnlocked) {
        LockScreen(viewModel)
    } else {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                if (isBackNavigation) {
                    slideInHorizontally { -it } + fadeIn() togetherWith
                            slideOutHorizontally { it } + fadeOut()
                } else {
                    slideInHorizontally { it } + fadeIn() togetherWith
                            slideOutHorizontally { -it } + fadeOut()
                }
            },
            label = "ScreenTransition"
        ) { screen ->
            when (screen) {
                is Screen.MainList -> MainListScreen(
                    viewModel = viewModel,
                    onNavigateToEditor = { noteId -> navigateTo(Screen.NoteEditor(noteId)) },
                    onNavigateToCloud = { navigateTo(Screen.CloudSync) },
                    onNavigateToPrivacy = { navigateTo(Screen.PrivacySettings) },
                    onNavigateToSearch = { navigateTo(Screen.Search) },
                    onNavigateToDrawing = { id, path -> navigateTo(Screen.DrawingCanvas(id, path)) },
                    onNavigateToMediaViewer = { type, src -> navigateTo(Screen.MediaViewer(type, src, currentScreen)) },
                    onNavigateToSettingsHub = { navigateTo(Screen.SettingsHub) },
                    onNavigateToBackupRestore = { navigateTo(Screen.BackupRestore) },
                    onNavigateToUpdateInfo = { navigateTo(Screen.UpdateInfo) },
                    onNavigateToAbout = { navigateTo(Screen.About) }
                )
                is Screen.Search -> SearchScreen(
                    viewModel = viewModel,
                    onNavigateToEditor = { noteId -> navigateTo(Screen.NoteEditor(noteId)) },
                    onBack = { navigateBack(Screen.MainList) },
                    onNavigateToDrawing = { id, path -> navigateTo(Screen.DrawingCanvas(id, path)) },
                    onNavigateToMediaViewer = { type, src -> navigateTo(Screen.MediaViewer(type, src, currentScreen)) }
                )
                is Screen.NoteEditor -> NoteEditorScreen(
                    noteId = screen.noteId,
                    viewModel = viewModel,
                    onBack = { navigateBack(Screen.MainList) },
                    onNavigateToDrawing = { id, path -> navigateTo(Screen.DrawingCanvas(id, path)) },
                    onNavigateToMediaViewer = { type, src -> navigateTo(Screen.MediaViewer(type, src, currentScreen)) }
                )
                is Screen.DrawingCanvas -> DrawingCanvasScreen(
                    noteId = screen.noteId,
                    jsonPath = screen.jsonPath,
                    viewModel = viewModel,
                    onBack = { navigateBack(Screen.NoteEditor(screen.noteId)) }
                )
                is Screen.CloudSync -> CloudSyncScreen(
                    viewModel = viewModel,
                    onBack = { navigateBack(Screen.MainList) }
                )
                is Screen.PrivacySettings -> PrivacySettingsScreen(
                    viewModel = viewModel,
                    onBack = { navigateBack(Screen.SettingsHub) }
                )
                is Screen.MediaViewer -> MediaViewerScreen(
                    type = screen.type,
                    src = screen.src,
                    onBack = { navigateBack(screen.previousScreen) },
                )
                is Screen.SettingsHub -> SettingsScreen(
                    themeViewModel = themeViewModel,
                    onBack = { navigateBack(Screen.MainList) },
                    onNavigateToBackupRestore = { navigateTo(Screen.BackupRestore) },
                    onNavigateToUpdateInfo = { navigateTo(Screen.UpdateInfo) },
                    onNavigateToAbout = { navigateTo(Screen.About) },
                    onNavigateToPrivacy = { navigateTo(Screen.PrivacySettings) }
                )
                is Screen.BackupRestore -> BackupRestoreScreen(
                    viewModel = backupViewModel,
                    onBack = { navigateBack(Screen.SettingsHub) }
                )
                is Screen.UpdateInfo -> UpdateInfoScreen(
                    viewModel = updaterViewModel,
                    onBack = { navigateBack(Screen.SettingsHub) }
                )
                is Screen.About -> AboutScreen(
                    onBack = { navigateBack(Screen.SettingsHub) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockScreen(viewModel: NotesViewModel) {
    var password by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = stringResource(R.string.lock_icon),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(id = R.string.app_name),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(id = R.string.status_encrypted),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF43A047),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            hasError = false
                        },
                        label = { Text(stringResource(id = R.string.label_enter_password)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input"),
                        singleLine = true,
                        isError = hasError,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    if (hasError) {
                        Text(
                            text = stringResource(id = R.string.error_wrong_password),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .align(Alignment.Start)
                                .padding(top = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            if (viewModel.unlockApp(password)) {
                                Toast.makeText(context, context.getString(R.string.toast_unlocked), Toast.LENGTH_SHORT).show()
                            } else {
                                hasError = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("unlock_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = stringResource(id = R.string.label_unlock),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NavigationRailContent(
    currentSection: com.example.ui.viewmodel.NavigationSection,
    onSectionSelected: (com.example.ui.viewmodel.NavigationSection) -> Unit,
    isExtended: Boolean,
    onToggleExtend: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLargeScreen = configuration.screenWidthDp >= 600

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        modifier = Modifier
            .fillMaxHeight()
            .width(if (isLargeScreen) (if (isExtended) 220.dp else 72.dp) else 280.dp),
        border = if (isLargeScreen) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Toggle button (only on large screens)
            if (isLargeScreen) {
                IconButton(
                    onClick = onToggleExtend,
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .testTag("toggle_rail_btn_rail")
                ) {
                    Icon(
                        imageVector = if (isExtended) Icons.AutoMirrored.Filled.MenuOpen else Icons.Default.Menu,
                        contentDescription = stringResource(R.string.toggle_navigation_rail)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Logo
            Image(
                painter = painterResource(id = R.drawable.img_app_icon),
                contentDescription = "Secure Notes Logo",
                modifier = Modifier
                    .size(if (isExtended) 84.dp else 44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            if (isExtended) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.app_name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Navigation Items
            val navItems = listOf(
                Triple(com.example.ui.viewmodel.NavigationSection.HOME, Icons.Default.Home, R.string.nav_home),
                Triple(com.example.ui.viewmodel.NavigationSection.FAVORITES, Icons.Default.Favorite, R.string.nav_favorites),
                Triple(com.example.ui.viewmodel.NavigationSection.ARCHIVED, Icons.Default.Archive, R.string.nav_archived),
                Triple(com.example.ui.viewmodel.NavigationSection.TRASH, Icons.Default.Delete, R.string.nav_trash),
                Triple(com.example.ui.viewmodel.NavigationSection.SETTINGS, Icons.Default.Settings, R.string.nav_settings)
            )

            Column(
                modifier = Modifier
                    .fillPackageNameOrScope()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                navItems.forEach { (section, icon, labelResId) ->
                    val isSelected = currentSection == section
                    val label = stringResource(id = labelResId)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .clickable { onSectionSelected(section) }
                            .testTag("nav_rail_item_${section.name.lowercase()}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            } else {
                                Color.Transparent
                            }
                        ),
                        border = if (isSelected) {
                            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                        } else {
                            null
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = if (isExtended) Arrangement.Start else Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )

                            if (isExtended) {
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = label,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
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

private fun Modifier.fillPackageNameOrScope(): Modifier = this.fillMaxWidth()

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
    val isLargeScreen = configuration.screenWidthDp >= 600
    var isNavExtended by remember(isLargeScreen) { mutableStateOf(isLargeScreen) }
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
                        onToggleExtend = {}
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
                    onToggleExtend = { isNavExtended = !isNavExtended }
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
                                        contentDescription = "Empty notes background banner",
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
                                    columns = GridCells.Fixed(2),
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
                com.example.ui.parseNoteContentAndAttachments(decryptedNote.content)
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
                                                contentDescription = "Play Video",
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

@Composable
fun CreateTagDialog(viewModel: NotesViewModel, onDismiss: () -> Unit) {
    var tagName by remember { mutableStateOf("") }
    val colors = listOf("#42A5F5", "#66BB6A", "#EC407A", "#AB47BC", "#FFA726", "#26A69A")
    var selectedColor by remember { mutableStateOf(colors[0]) }

    Dialog(onDismissRequest = onDismiss) {
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.dialog_create_tag),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = tagName,
                    onValueChange = { tagName = it },
                    label = { Text(stringResource(id = R.string.tag_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.label_select_color), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(color)))
                                .border(
                                    width = if (selectedColor == color) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = color }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(id = R.string.btn_cancel))
                    }
                    Button(
                        onClick = {
                            if (tagName.isNotBlank()) {
                                viewModel.createTag(tagName.trim(), selectedColor)
                                onDismiss()
                            }
                        }
                    ) {
                        Text(stringResource(id = R.string.btn_add))
                    }
                }
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudSyncScreen(
    viewModel: NotesViewModel,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val isDriveLinked by viewModel.isDriveLinked.collectAsState()
    val driveAccessToken by viewModel.driveAccessToken.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()
    val syncStatusMessage by viewModel.syncStatusMessage.collectAsState()

    var tokenInput by remember { mutableStateOf(driveAccessToken) }
    val context = LocalContext.current

    // Launch action notifier
    LaunchedEffect(syncStatusMessage) {
        syncStatusMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearStatusMessage()
        }
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
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                    Text(
                        text = stringResource(id = R.string.title_cloud_sync),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = stringResource(R.string.cloud_icon),
                            tint = if (isDriveLinked) Color(0xFF42A5F5) else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.cloud_sync_title),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = if (isDriveLinked) stringResource(id = R.string.drive_linked) else stringResource(id = R.string.drive_unlinked),
                                color = if (isDriveLinked) Color(0xFF42A5F5) else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(id = R.string.info_cloud),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(id = R.string.last_synced, lastSyncTime),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!isDriveLinked) {
                Text(
                    text = stringResource(R.string.link_account),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Input field for OAuth API token
                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it },
                    label = { Text("Paste Google API Access Token") },
                    placeholder = { Text("ya29.a0Ac...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("token_input_field"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Your Access Token is handled on-device securely. It grants temporary folder authorization directly to cloud storage without middleman storage.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (tokenInput.isBlank()) {
                            // Seed simulation / test token for smooth UI demo
                            viewModel.linkGoogleDrive("ya29.simulated_access_token")
                            Toast.makeText(context, context.getString(R.string.toast_simulation_token_seeded), Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.linkGoogleDrive(tokenInput.trim())
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("link_drive_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(id = R.string.btn_link_drive), fontWeight = FontWeight.Bold)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.forceSyncCloud() },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("push_sync_btn")
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = stringResource(R.string.btn_upload_backup))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.backup_cloud), fontSize = 11.sp)
                    }

                    Button(
                        onClick = { viewModel.restoreSyncCloud() },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("pull_sync_btn")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = stringResource(R.string.btn_download_backup))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.restore_notes), fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { viewModel.unlinkGoogleDrive() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("unlink_drive_btn"),
                    colors = ButtonDefaults.outlinedButtonColors()
                ) {
                    Text(stringResource(id = R.string.btn_unlink_drive), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

fun borderStrokeHelper(isSelected: Boolean, activeColor: Color): BorderStroke {
    return if (isSelected) {
        BorderStroke(2.dp, activeColor)
    } else {
        BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f))
    }
}

@Composable
fun BatchTagDialog(
    selectedNoteIds: Set<Int>,
    notes: List<DecryptedNote>,
    viewModel: NotesViewModel,
    onDismiss: () -> Unit,
    onTagsUpdated: () -> Unit
) {
    val availableTags by viewModel.availableTags.collectAsState()
    val initialSelectedTags: Set<String> = remember(selectedNoteIds, notes) {
        val selectedNotes = notes.filter { it.note.id in selectedNoteIds }
        val tags = mutableSetOf<String>()
        selectedNotes.forEach { decNote ->
            try {
                val arr = org.json.JSONArray(decNote.note.tagsJson)
                for (i in 0 until arr.length()) {
                    tags.add(arr.optString(i))
                }
            } catch (e: Exception) {
                // ignore
            }
        }
        tags
    }
    var selectedTags by remember(initialSelectedTags) { mutableStateOf(initialSelectedTags) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.batch_tags_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                availableTags.forEach { tag ->
                    val isTagSelected = tag.name in selectedTags
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedTags = if (isTagSelected) {
                                    selectedTags - tag.name
                                } else {
                                    selectedTags + tag.name
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isTagSelected,
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(android.graphics.Color.parseColor(tag.colorHex)))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = tag.name,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.batchUpdateTags(selectedNoteIds, selectedTags.toList())
                    android.widget.Toast.makeText(context, context.getString(R.string.toast_batch_tags_applied), android.widget.Toast.LENGTH_SHORT).show()
                    onTagsUpdated()
                },
                modifier = Modifier.testTag("apply_batch_tags_btn")
            ) {
                Text(stringResource(id = R.string.btn_ok))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.btn_cancel))
            }
        },
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 6.dp
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareFormatSheet(
    selectedNotes: List<com.example.ui.viewmodel.DecryptedNote>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(id = R.string.share_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedCard(
                onClick = {
                    ExportUtils.exportMultipleToTxt(context, selectedNotes)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("share_format_txt_btn"),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Description, contentDescription = "TXT Icon", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(id = R.string.share_format_txt),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            OutlinedCard(
                onClick = {
                    ExportUtils.exportMultipleToMarkdown(context, selectedNotes)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("share_format_md_btn"),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Code, contentDescription = "Markdown Icon", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(id = R.string.share_format_md),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            OutlinedCard(
                onClick = {
                    ExportUtils.exportMultipleToPdf(context, selectedNotes)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("share_format_pdf_btn"),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    @Suppress("DEPRECATION")
                    val articleIcon = Icons.Default.Article
                    Icon(articleIcon, contentDescription = "PDF Icon", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(id = R.string.share_format_pdf),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            OutlinedCard(
                onClick = {
                    ExportUtils.exportMultipleToHtml(context, selectedNotes)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("share_format_html_btn"),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Web, contentDescription = "HTML Icon", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(id = R.string.share_format_html),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            OutlinedCard(
                onClick = {
                    ExportUtils.exportMultipleToJson(context, selectedNotes)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("share_format_json_btn"),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "JSON Icon", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(id = R.string.share_format_json),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun EditTagDialog(
    tag: Tag,
    viewModel: NotesViewModel,
    onDismiss: () -> Unit
) {
    var tagName by remember { mutableStateOf(tag.name) }
    val colors = listOf("#42A5F5", "#66BB6A", "#EC407A", "#AB47BC", "#FFA726", "#26A69A")
    var selectedColor by remember { mutableStateOf(tag.colorHex) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(id = R.string.delete_tag_confirm_title)) },
            text = { Text(stringResource(id = R.string.delete_tag_confirm_message, tag.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTag(tag)
                        Toast.makeText(context, context.getString(R.string.toast_tag_deleted), Toast.LENGTH_SHORT).show()
                        showDeleteConfirm = false
                        onDismiss()
                    }
                ) {
                    Text(stringResource(id = R.string.btn_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(id = R.string.btn_cancel))
                }
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.dialog_edit_tag),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(
                        onClick = { showDeleteConfirm = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Tag",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = tagName,
                    onValueChange = { tagName = it },
                    label = { Text(stringResource(id = R.string.tag_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_tag_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(id = R.string.label_select_color), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(color)))
                                .border(
                                    width = if (selectedColor.lowercase() == color.lowercase()) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = color }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(id = R.string.btn_cancel))
                    }
                    Button(
                        onClick = {
                            if (tagName.isNotBlank()) {
                                viewModel.updateTag(tag, tagName.trim(), selectedColor)
                                Toast.makeText(context, context.getString(R.string.toast_tag_updated), Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        },
                        modifier = Modifier.testTag("save_tag_btn")
                    ) {
                        Text(stringResource(id = R.string.btn_save))
                    }
                }
            }
        }
    }
}

@Composable
fun ManageTagsDialog(
    viewModel: NotesViewModel,
    onDismiss: () -> Unit
) {
    val tags by viewModel.availableTags.collectAsState()
    var editingTag by remember { mutableStateOf<Tag?>(null) }

    if (editingTag != null) {
        EditTagDialog(
            tag = editingTag!!,
            viewModel = viewModel,
            onDismiss = { editingTag = null }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.dialog_manage_tags),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tags) { tag ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(tag.colorHex)))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = tag.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { editingTag = tag },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Tag",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                var showLocalDeleteConfirm by remember { mutableStateOf(false) }
                                val context = LocalContext.current
                                if (showLocalDeleteConfirm) {
                                    AlertDialog(
                                        onDismissRequest = { showLocalDeleteConfirm = false },
                                        title = { Text(stringResource(id = R.string.delete_tag_confirm_title)) },
                                        text = { Text(stringResource(id = R.string.delete_tag_confirm_message, tag.name)) },
                                        confirmButton = {
                                            TextButton(
                                                onClick = {
                                                    viewModel.deleteTag(tag)
                                                    Toast.makeText(context, context.getString(R.string.toast_tag_deleted), Toast.LENGTH_SHORT).show()
                                                    showLocalDeleteConfirm = false
                                                }
                                            ) {
                                                Text(stringResource(id = R.string.btn_delete), color = MaterialTheme.colorScheme.error)
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { showLocalDeleteConfirm = false }) {
                                                Text(stringResource(id = R.string.btn_cancel))
                                            }
                                        }
                                    )
                                }
                                IconButton(
                                    onClick = { showLocalDeleteConfirm = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Tag",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(id = R.string.btn_ok))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
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

    // Clear search on leave
    DisposableEffect(Unit) {
        onDispose {
            viewModel.searchQuery.value = ""
        }
    }

    // Auto-focus on entry
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
            // Horizontal Pill / Filter Chips Row
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

                // 1. Favorites Filter Chip
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

                // 2. Archived Filter Chip
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

                // 3. Tag Filter Chip
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

                // 4. Color Filter Chip
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
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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

@Composable
fun getNoteBackgroundColor(colorId: Int?, isDark: Boolean = isSystemInDarkTheme()): Color {
    if (colorId == null || colorId == 0) return MaterialTheme.colorScheme.surface
    return when (colorId) {
        1 -> if (isDark) Color(0xFF0D47A1).copy(alpha = 0.25f) else Color(0xFFE3F2FD)
        2 -> if (isDark) Color(0xFF1B5E20).copy(alpha = 0.25f) else Color(0xFFE8F5E9)
        3 -> if (isDark) Color(0xFFE65100).copy(alpha = 0.2f) else Color(0xFFFFFDE7)
        4 -> if (isDark) Color(0xFF880E4F).copy(alpha = 0.25f) else Color(0xFFFCE4EC)
        5 -> if (isDark) Color(0xFF4A148C).copy(alpha = 0.25f) else Color(0xFFF3E5F5)
        6 -> if (isDark) Color(0xFF311B92).copy(alpha = 0.25f) else Color(0xFFEDE7F6)
        else -> MaterialTheme.colorScheme.surface
    }
}

@Composable
fun getColorName(colorId: Int?): String {
    return when (colorId) {
        1 -> stringResource(id = R.string.label_color_blue)
        2 -> stringResource(id = R.string.label_color_green)
        3 -> stringResource(id = R.string.label_color_yellow)
        4 -> stringResource(id = R.string.label_color_pink)
        5 -> stringResource(id = R.string.label_color_purple)
        6 -> stringResource(id = R.string.label_color_orange)
        else -> stringResource(id = R.string.label_color_none)
    }
}

