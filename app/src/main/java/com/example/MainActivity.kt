package com.example

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.app.Application
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
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
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
import com.example.ui.viewmodel.AiViewModel
import com.example.ui.viewmodel.StorageViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.ai.OllamaService
import com.example.data.ai.OnDeviceService
import com.example.data.ai.ModelDownloader
import com.example.data.ai.LlamaCppEngine

import com.google.android.gms.common.api.Scope
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.example.ui.Screen
import com.example.ui.ScreenSaver
import com.example.ui.Navigator
import com.example.ui.ScreenContext
import com.example.util.MoveDirection
import com.example.util.SortOption
import com.example.util.borderStrokeHelper
import com.example.util.fillPackageNameOrScope
import com.example.util.getColorName
import com.example.util.getNoteBackgroundColor
import com.example.data.SharedPreferencesRepository
import com.example.data.local.NoteDatabase
import com.example.util.reorderNote
import com.example.util.swapNotes
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
    private var notesViewModel: NotesViewModel? = null

    override fun onUserInteraction() {
        super.onUserInteraction()
        notesViewModel?.resetAutoLockTimer()
    }

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
        val prefs = getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(AppConstants.SCREENSHOT_ENABLED_KEY, false)) {
            window.setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE, android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
        setContent {
            val cipherService = com.example.data.security.EncryptionServiceImpl()
            val syncService = com.example.data.sync.GoogleDriveSyncService()
            val viewModel: NotesViewModel = viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return NotesViewModel(
                            this@MainActivity.applicationContext as android.app.Application,
                            NoteDatabase.getDatabase(this@MainActivity.applicationContext),
                            cipherService,
                            syncService
                        ) as T
                    }
                }
            )
            notesViewModel = viewModel
            val themeViewModel: ThemeViewModel = viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return ThemeViewModel(SharedPreferencesRepository(this@MainActivity.applicationContext)) as T
                    }
                }
            )
            val appContext = this@MainActivity.applicationContext
            val prefsRepo = SharedPreferencesRepository(appContext)
            val ollamaService = OllamaService()
            val modelDownloader = ModelDownloader(appContext)
            val llamaEngine = LlamaCppEngine(appContext)
            val onDeviceService = OnDeviceService(llamaEngine)
            val aiViewModel: AiViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return AiViewModel(
                            appContext as android.app.Application,
                            prefsRepo,
                            ollamaService,
                            onDeviceService,
                            modelDownloader
                        ) as T
                    }
                }
            )
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
                    AppMainContent(viewModel, themeViewModel, aiViewModel)
                }
            }
        }
    }
}

@Composable
fun AppMainContent(viewModel: NotesViewModel, themeViewModel: ThemeViewModel, aiViewModel: AiViewModel) {
    val isUnlocked by viewModel.isUnlocked.collectAsState()
    val isPasswordSet by viewModel.isPasswordSet.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.onAppBackgrounded()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var currentScreen by rememberSaveable(stateSaver = ScreenSaver) { mutableStateOf<Screen>(Screen.MainList) }
    var isBackNavigation by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val activity = context as? Activity
    val screenshotEnabled by viewModel.screenshotEnabled.collectAsState()
    LaunchedEffect(screenshotEnabled) {
        activity?.window?.let { win ->
            if (screenshotEnabled) {
                win.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
            } else {
                win.setFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE, android.view.WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }
    val backupViewModel: BackupViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return BackupViewModel(context.applicationContext as android.app.Application, viewModel as com.example.data.sync.CloudSyncManager) as T
            }
        }
    )
    val updaterViewModel: UpdaterViewModel = viewModel()
    val storageViewModel: StorageViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return StorageViewModel(
                    context.applicationContext as android.app.Application,
                    com.example.data.local.NoteDatabase.getDatabase(context.applicationContext)
                ) as T
            }
        }
    )

    val navigator = remember {
        Navigator(
            onNavigateTo = { screen ->
                isBackNavigation = false
                currentScreen = screen
            },
            onNavigateBack = { to ->
                isBackNavigation = true
                currentScreen = to
            }
        )
    }

    val screenContext = remember(viewModel, themeViewModel, backupViewModel, updaterViewModel, aiViewModel, storageViewModel, navigator, currentScreen) {
        ScreenContext(
            viewModel = viewModel,
            themeViewModel = themeViewModel,
            backupViewModel = backupViewModel,
            updaterViewModel = updaterViewModel,
            aiViewModel = aiViewModel,
            storageViewModel = storageViewModel,
            navigator = navigator,
            currentScreen = currentScreen
        )
    }

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
            screen.render(screenContext)
        }
    }
}

@Composable
fun NavigationRailContent(
    currentSection: com.example.data.model.NavigationSection,
    onSectionSelected: (com.example.data.model.NavigationSection) -> Unit,
    isExtended: Boolean,
    onToggleExtend: () -> Unit,
    widthClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    onCreateTag: () -> Unit = {},
    onManageTags: () -> Unit = {}
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
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
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
                            BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
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

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            if (isExtended) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCreateTag,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = stringResource(R.string.create_tag),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.create_tag), fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = onManageTags,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.manage_tags),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.manage_tags), fontSize = 11.sp)
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onCreateTag) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = stringResource(R.string.create_tag),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onManageTags) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.manage_tags),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}







