package com.example

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.app.Application
import android.accounts.Account
import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import android.util.Log
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
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
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
import com.example.data.model.DecryptedNote
import com.example.ui.viewmodel.NotesViewModel
import com.example.ui.viewmodel.ThemeViewModel
import com.example.ui.viewmodel.BackupViewModel
import com.example.ui.viewmodel.UpdaterViewModel
import com.example.util.ExportUtils
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.example.ui.Screen
import com.example.ui.ScreenSaver
import com.example.util.MoveDirection
import com.example.util.SortOption
import com.example.util.borderStrokeHelper
import com.example.util.fillPackageNameOrScope
import com.example.util.getColorName
import com.example.util.getNoteBackgroundColor
import com.example.util.reorderNote
import com.example.util.swapNotes
import com.example.ui.CloudSyncScreen
import com.example.ui.LockScreen
import com.example.ui.SearchScreen
import com.example.ui.CreateTagDialog
import com.example.ui.EditTagDialog
import com.example.ui.ManageTagsDialog
import com.example.ui.BatchTagDialog
import com.example.ui.ShareFormatSheet
import com.example.ui.MainListScreen
import com.example.ui.NoteCardItem
import com.example.ui.SortOptionRow

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
                    onNavigateToCloud = { navigateTo(Screen.CloudSync) },
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

@Composable
fun NavigationRailContent(
    currentSection: com.example.data.model.NavigationSection,
    onSectionSelected: (com.example.data.model.NavigationSection) -> Unit,
    isExtended: Boolean,
    onToggleExtend: () -> Unit,
    widthClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact
) {
    val isLargeScreen = widthClass != WindowWidthSizeClass.Compact

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
                contentDescription = stringResource(id = R.string.cd_secure_notes_logo),
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
                Triple(com.example.data.model.NavigationSection.HOME, Icons.Default.Home, R.string.nav_home),
                Triple(com.example.data.model.NavigationSection.FAVORITES, Icons.Default.Favorite, R.string.nav_favorites),
                Triple(com.example.data.model.NavigationSection.ARCHIVED, Icons.Default.Archive, R.string.nav_archived),
                Triple(com.example.data.model.NavigationSection.TRASH, Icons.Default.Delete, R.string.nav_trash),
                Triple(com.example.data.model.NavigationSection.SETTINGS, Icons.Default.Settings, R.string.nav_settings)
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







