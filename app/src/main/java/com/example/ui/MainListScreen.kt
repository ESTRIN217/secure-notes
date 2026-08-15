package com.example.ui

import kotlinx.coroutines.launch
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items as staggeredItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.AppConstants
import com.example.NavigationRailContent
import com.example.R
import com.example.data.model.Note
import com.example.data.model.Tag
import com.example.data.model.parseNoteContentAndAttachments
import com.example.data.model.DecryptedNote
import com.example.data.model.NavigationSection
import com.example.data.model.parseTags
import com.example.ui.viewmodel.NotesViewModel
import com.example.util.MoveDirection
import com.example.util.RichTextParser
import com.example.util.SortOption
import com.example.util.borderStrokeHelper
import com.example.util.getNoteBackgroundColor
import com.example.util.reorderNote
import com.example.util.swapNotes
import kotlin.math.roundToInt
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.animation.core.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MainListScreen(
    viewModel: NotesViewModel,
    aiViewModel: com.example.ui.viewmodel.AiViewModel,
    onNavigateToEditor: (Int) -> Unit,
    onNavigateToCloud: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToDrawing: (Int, String?) -> Unit,
    onNavigateToMediaViewer: (String, String) -> Unit,
    onNavigateToSettingsHub: () -> Unit = {},
    onNavigateToBackupRestore: () -> Unit = {},
    onNavigateToUpdateInfo: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToChatHistory: () -> Unit = {},
    onLaunchNewAiChat: () -> Unit = {},
    onNavigateToNewDrawing: () -> Unit = {},
) {
    val currentSection by viewModel.currentSection.collectAsState()
    val aiEnabled by aiViewModel.aiEnabled.collectAsStateWithLifecycle()
    val notes by viewModel.notesList.collectAsState()
    val tags by viewModel.availableTags.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTagFilter by viewModel.selectedTagFilter.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isDriveLinked = syncState.isDriveLinked
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

    BackHandler(enabled = currentSection != com.example.data.model.NavigationSection.HOME) {
        viewModel.currentSection.value = com.example.data.model.NavigationSection.HOME
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
    var showEmptyTrashAlert by remember { mutableStateOf(false) }
    var isFabExpanded by remember { mutableStateOf(false) }

    var showAudioRecorderSheet by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordedFile by remember { mutableStateOf<File?>(null) }
    var isPlayingRecording by remember { mutableStateOf(false) }
    var draftPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    var showImageOptionsDialog by remember { mutableStateOf(false) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    val startAudioRecording: () -> Unit = {
        try {
            val file = File(context.filesDir, "voice_${System.currentTimeMillis()}.3gp")
            recordedFile = file
            val recorderContext = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.createAttributionContext("microphone")
            } else {
                context
            }
            val recorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                MediaRecorder(recorderContext)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            isRecording = true
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, context.getString(R.string.toast_recording_error) + ": ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    val imageGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(it, takeFlags)
            } catch (_: Exception) {}
            scope.launch {
                viewModel.saveNoteAndGetId(id = 0, title = "", content = "<img src=\"$it\" />", isEncrypted = false, tagsList = emptyList())
                Toast.makeText(context, context.getString(R.string.toast_note_created_with_image), Toast.LENGTH_SHORT).show()
            }
        }
    }

    val imageCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            cameraImageUri?.let { uri ->
                scope.launch {
                    viewModel.saveNoteAndGetId(id = 0, title = "", content = "<img src=\"$uri\" />", isEncrypted = false, tagsList = emptyList())
                    Toast.makeText(context, context.getString(R.string.toast_note_created_with_image), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val launchCameraForImage: () -> Unit = {
        try {
            val tempFile = File(context.cacheDir, "img_${System.currentTimeMillis()}.jpg")
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, tempFile)
            cameraImageUri = uri
            imageCameraLauncher.launch(uri)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, context.getString(R.string.toast_camera_error) + ": ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    var pendingCameraLaunch by remember { mutableStateOf(false) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && pendingCameraLaunch) {
            launchCameraForImage()
        } else if (!isGranted) {
            Toast.makeText(context, context.getString(R.string.camera_permission_required), Toast.LENGTH_SHORT).show()
        }
        pendingCameraLaunch = false
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startAudioRecording()
        } else {
            Toast.makeText(context, context.getString(R.string.mic_permission_required), Toast.LENGTH_SHORT).show()
        }
    }

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
                            if (section == com.example.data.model.NavigationSection.SETTINGS) {
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
                        widthClass = widthClass,
                        onCreateTag = { showCreateTagDialog = true },
                        onManageTags = { showManageTagsDialog = true },
                        onNavigateToChatHistory = {
                            scope.launch { drawerState.close() }
                            onNavigateToChatHistory()
                        },
                        aiEnabled = aiEnabled
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
                        if (section == com.example.data.model.NavigationSection.SETTINGS) {
                            onNavigateToSettingsHub()
                        } else {
                            viewModel.currentSection.value = section
                            selectedNoteIds = emptySet()
                        }
                    },
                    isExtended = isNavExtended,
                    onToggleExtend = { isNavExtended = !isNavExtended },
                    widthClass = widthClass,
                    onCreateTag = { showCreateTagDialog = true },
                    onManageTags = { showManageTagsDialog = true },
                    onNavigateToChatHistory = onNavigateToChatHistory,
                    aiEnabled = aiEnabled
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
                        CustomTopBar {
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
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
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

                                        when (currentSection) {
                                            com.example.data.model.NavigationSection.TRASH -> {
                                                Text(
                                                    text = stringResource(id = R.string.nav_trash_title),
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.weight(1f))
                                                if (notes.isNotEmpty()) {
                                                    OutlinedButton(
                                                        onClick = { showEmptyTrashAlert = true },
                                                        colors = ButtonDefaults.outlinedButtonColors(
                                                            contentColor = MaterialTheme.colorScheme.error
                                                        ),
                                                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error),
                                                        modifier = Modifier.testTag("empty_trash_btn")
                                                    ) {
                                                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(stringResource(id = R.string.action_empty_trash), fontSize = 12.sp)
                                                    }
                                                }
                                            }
                                            com.example.data.model.NavigationSection.ARCHIVED -> {
                                                Text(
                                                    text = stringResource(id = R.string.nav_archived_title),
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.weight(1f))
                                                IconButton(
                                                    onClick = onNavigateToSearch,
                                                    modifier = Modifier.testTag("archived_search_btn")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Search,
                                                        contentDescription = stringResource(R.string.search_icon),
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            else -> {
                                                Surface(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(48.dp)
                                                        .testTag("search_field"),
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                                    border = BorderStroke(3.dp, MaterialTheme.colorScheme.outlineVariant)
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .padding(horizontal = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        IconButton(
                                                            onClick = onNavigateToSearch,
                                                            modifier = Modifier.testTag("search_icon_btn")
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Search,
                                                                contentDescription = stringResource(R.string.search_icon),
                                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                        Text(
                                                            text = stringResource(id = R.string.search_placeholder),
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .clickable { onNavigateToSearch() },
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                        )
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
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))

                                        val accountEmail by viewModel.driveAccountEmail.collectAsState()
                                        val profilePictureUri by viewModel.driveProfilePictureUri.collectAsState()
                                        if (accountEmail != null) {
                                            val email = accountEmail!!
                                            Surface(
                                                onClick = onNavigateToBackupRestore,
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    if (profilePictureUri != null) {
                                                        SubcomposeAsyncImage(
                                                            model = ImageRequest.Builder(LocalContext.current)
                                                                .data(profilePictureUri)
                                                                .crossfade(true)
                                                                .build(),
                                                            contentDescription = stringResource(R.string.cd_account_avatar),
                                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                            contentScale = ContentScale.Crop,
                                                            error = {
                                                                Text(
                                                                    text = email.first().uppercase(),
                                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 16.sp
                                                                )
                                                            }
                                                        )
                                                    } else {
                                                        Text(
                                                            text = email.first().uppercase(),
                                                            color = MaterialTheme.colorScheme.onPrimary,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 16.sp
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            Surface(
                                                onClick = onNavigateToBackupRestore,
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.AccountCircle,
                                                        contentDescription = stringResource(R.string.cd_account_avatar),
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    floatingActionButton = {
                        if (selectedNoteIds.isEmpty() && currentSection != com.example.data.model.NavigationSection.TRASH) {
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AnimatedVisibility(
                                    visible = isFabExpanded,
                                    enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (aiEnabled) {
                                            SmallFloatingActionButton(
                                                onClick = {
                                                    isFabExpanded = false
                                                    onLaunchNewAiChat()
                                                },
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.testTag("fab_ai")
                                            ) {
                                                Icon(Icons.Default.AutoAwesome, contentDescription = stringResource(R.string.ai_assistant))
                                            }
                                        }
                                        SmallFloatingActionButton(
                                            onClick = {
                                                isFabExpanded = false
                                                showImageOptionsDialog = true
                                            },
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.testTag("fab_image")
                                        ) {
                                            Icon(Icons.Default.Image, contentDescription = stringResource(R.string.fab_image))
                                        }
                                        SmallFloatingActionButton(
                                            onClick = {
                                                isFabExpanded = false
                                                onNavigateToNewDrawing()
                                            },
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.testTag("fab_drawing")
                                        ) {
                                            Icon(Icons.Default.Gesture, contentDescription = stringResource(R.string.fab_drawing))
                                        }
                                        SmallFloatingActionButton(
                                            onClick = {
                                                isFabExpanded = false
                                                showAudioRecorderSheet = true
                                            },
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.testTag("fab_audio")
                                        ) {
                                            Icon(Icons.Default.Mic, contentDescription = stringResource(R.string.fab_audio))
                                        }
                                        SmallFloatingActionButton(
                                            onClick = {
                                                isFabExpanded = false
                                                onNavigateToEditor(0)
                                            },
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.testTag("fab_text")
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.fab_text))
                                        }
                                    }
                                }
                                FloatingActionButton(
                                    onClick = { isFabExpanded = !isFabExpanded },
                                    modifier = Modifier.testTag("fab_main"),
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ) {
                                    Icon(
                                        imageVector = if (isFabExpanded) Icons.Default.Close else Icons.Default.Add,
                                        contentDescription = stringResource(R.string.create_note)
                                    )
                                }
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
                        if (currentSection == com.example.data.model.NavigationSection.HOME ||
                            currentSection == com.example.data.model.NavigationSection.FAVORITES) {
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

                            }
                        }



                        // Notes grid/list scrollarea
                        if (isLoading) {
                            LoadingShimmer(modifier = Modifier.weight(1f))
                        } else if (notes.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                when (currentSection) {
                                    com.example.data.model.NavigationSection.FAVORITES -> {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(24.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                                border = BorderStroke(3.dp, MaterialTheme.colorScheme.outlineVariant),
                                                modifier = Modifier.size(180.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.FavoriteBorder,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                        modifier = Modifier.size(80.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(24.dp))
                                            Text(
                                                text = stringResource(id = R.string.status_empty_favorites),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(horizontal = 16.dp),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    }
                                    com.example.data.model.NavigationSection.ARCHIVED -> {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(24.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                                border = BorderStroke(3.dp, MaterialTheme.colorScheme.outlineVariant),
                                                modifier = Modifier.size(180.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Archive,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                        modifier = Modifier.size(80.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(24.dp))
                                            Text(
                                                text = stringResource(id = R.string.status_empty_archived),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(horizontal = 16.dp),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    }
                                    com.example.data.model.NavigationSection.TRASH -> {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(24.dp),
                                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                                border = BorderStroke(3.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                                modifier = Modifier.size(180.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.DeleteSweep,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                                                        modifier = Modifier.size(80.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(24.dp))
                                            Text(
                                                text = stringResource(id = R.string.status_empty_trash),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(horizontal = 16.dp),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = stringResource(id = R.string.status_empty_trash_subtitle),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Normal,
                                                modifier = Modifier.padding(horizontal = 32.dp),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    }
                                    else -> {
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
                                }
                            }
                        } else {
                            if (isGridView) {
                                LazyVerticalStaggeredGrid(
                                    columns = StaggeredGridCells.Fixed(gridColumns),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalItemSpacing = 10.dp,
                                    contentPadding = PaddingValues(bottom = 80.dp)
                                ) {
                                    staggeredItems(sortedNotes) { decryptedNote ->
                                        val isThisDragged = draggedNoteId == decryptedNote.note.id
                                        val isCustomOrderActive = (sortOption == SortOption.CUSTOM && currentSection == com.example.data.model.NavigationSection.HOME)
                                        
                                        val dragModifier = if (isCustomOrderActive) {
                                            Modifier.pointerInput(decryptedNote.note.id) {
                                                fun resetDrag() {
                                                    draggedNoteId = null
                                                    dragOffsetX = 0f
                                                    dragOffsetY = 0f
                                                }

                                                fun handleSwap() {
                                                    val currentIndex = sortedNotes.indexOfFirst { it.note.id == decryptedNote.note.id }
                                                    if (currentIndex == -1) return
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

                                                detectDragGesturesAfterLongPress(
                                                    onDragStart = {
                                                        draggedNoteId = decryptedNote.note.id
                                                        dragOffsetX = 0f
                                                        dragOffsetY = 0f
                                                    },
                                                    onDragEnd = { resetDrag() },
                                                    onDragCancel = { resetDrag() },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        dragOffsetX += dragAmount.x
                                                        dragOffsetY += dragAmount.y
                                                        handleSwap()
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
                                                isInTrash = currentSection == com.example.data.model.NavigationSection.TRASH,
                                                isGrid = true,
                                                onNavigateToDrawing = onNavigateToDrawing,
                                                onNavigateToMediaViewer = onNavigateToMediaViewer,
                                                pageTitleById = { id -> notes.find { it.note.id == id }?.title ?: "" },
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
                                                        if (currentSection != com.example.data.model.NavigationSection.TRASH) {
                                                            onNavigateToEditor(decryptedNote.note.id)
                                                        } else {
                                                            viewModel.restoreFromTrash(decryptedNote.note)
                                                            Toast.makeText(context, context.getString(R.string.toast_restored), Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                },
                                                onLongClick = {
                                                    if (currentSection != com.example.data.model.NavigationSection.TRASH) {
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
                                            isCustomOrderActive = (sortOption == SortOption.CUSTOM && currentSection == com.example.data.model.NavigationSection.HOME),
                                            isInTrash = currentSection == com.example.data.model.NavigationSection.TRASH,
                                            isGrid = false,
                                            onNavigateToDrawing = onNavigateToDrawing,
                                            onNavigateToMediaViewer = onNavigateToMediaViewer,
                                            pageTitleById = { id -> notes.find { it.note.id == id }?.title ?: "" },
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
                                                    if (currentSection != com.example.data.model.NavigationSection.TRASH) {
                                                        onNavigateToEditor(decryptedNote.note.id)
                                                    } else {
                                                        viewModel.restoreFromTrash(decryptedNote.note)
                                                        Toast.makeText(context, context.getString(R.string.toast_restored), Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            onLongClick = {
                                                if (currentSection != com.example.data.model.NavigationSection.TRASH) {
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
                                if (currentSection == com.example.data.model.NavigationSection.TRASH) {
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

    if (showSortBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSortBottomSheet = false },
            sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden),
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

    if (showImageOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showImageOptionsDialog = false },
            title = { Text(stringResource(id = R.string.fab_image)) },
            text = { Text(stringResource(id = R.string.desc_insert_image)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImageOptionsDialog = false
                        imageGalleryLauncher.launch("image/*")
                    }
                ) {
                    Text(stringResource(id = R.string.label_option_gallery))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showImageOptionsDialog = false
                        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.CAMERA
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        if (hasPermission) {
                            launchCameraForImage()
                        } else {
                            pendingCameraLaunch = true
                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                        }
                    }
                ) {
                    Text(stringResource(id = R.string.label_option_camera))
                }
            },
            shape = RoundedCornerShape(12.dp)
        )
    }

    if (showAudioRecorderSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showAudioRecorderSheet = false
                mediaRecorder?.apply {
                    try { stop() } catch (_: Exception) {}
                    release()
                }
                mediaRecorder = null
                isRecording = false
            },
            sheetState = rememberBottomSheetState(
                initialValue = SheetValue.Hidden,
                enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
            ),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.voice_note_recorder),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                if (!isRecording && recordedFile == null) {
                    IconButton(
                        onClick = { recordAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO) },
                        modifier = Modifier
                            .size(64.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            .testTag("start_record_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = stringResource(id = R.string.tap_to_record),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Text(stringResource(id = R.string.tap_to_record), style = MaterialTheme.typography.bodySmall)
                } else if (isRecording) {
                    IconButton(
                        onClick = {
                            try {
                                mediaRecorder?.apply {
                                    stop()
                                    release()
                                }
                                mediaRecorder = null
                                isRecording = false
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .background(MaterialTheme.colorScheme.errorContainer, CircleShape)
                            .testTag("stop_record_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = stringResource(id = R.string.cd_stop_recording),
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Text(
                        stringResource(id = R.string.recording_tap_to_stop),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (recordedFile != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (isPlayingRecording) {
                                    draftPlayer?.stop()
                                    draftPlayer?.release()
                                    draftPlayer = null
                                    isPlayingRecording = false
                                } else {
                                    try {
                                        val player = MediaPlayer().apply {
                                            setDataSource(recordedFile!!.absolutePath)
                                            prepare()
                                            start()
                                            setOnCompletionListener {
                                                isPlayingRecording = false
                                                release()
                                            }
                                        }
                                        draftPlayer = player
                                        isPlayingRecording = true
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        Toast.makeText(context, context.getString(R.string.toast_audio_preview_error), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(64.dp)
                                .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isPlayingRecording) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = stringResource(id = R.string.cd_play_pause_audio),
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                recordedFile = null
                                recordedFile?.delete()
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.errorContainer, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(id = R.string.cd_discard_recording),
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            recordedFile?.let { file ->
                                scope.launch {
                                    viewModel.saveNoteAndGetId(id = 0, title = "", content = "<audio src=\"${file.absolutePath}\" />", isEncrypted = false, tagsList = emptyList())
                                }
                                Toast.makeText(context, context.getString(R.string.toast_note_created_with_audio), Toast.LENGTH_SHORT).show()
                            }
                            showAudioRecorderSheet = false
                            recordedFile = null
                        },
                        modifier = Modifier.fillMaxWidth().testTag("attach_voice_btn")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(id = R.string.create_note))
                    }
                }
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                mediaRecorder?.apply {
                    try { stop() } catch (_: Exception) {}
                    release()
                }
                draftPlayer?.release()
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
    onNavigateToMediaViewer: ((type: String, src: String) -> Unit)? = null,
    pageTitleById: (Int) -> String = { "" }
) {
    val note = decryptedNote.note
    val cleanDateStr = SimpleDateFormat("LLL dd, yyyy HH:mm", Locale.getDefault()).format(Date(note.lastModified))
    val pageBlockLabel = stringResource(R.string.block_page)
    
    val tagsList = remember(note.tagsJson) { note.parseTags() }

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

            val (displayText, visualAttachments) = remember(decryptedNote.content) {
                val content = decryptedNote.content
                val blocks = com.example.util.RichTextConverter.contentToBlocks(content)
                if (blocks != null) {
                    val textParts = mutableListOf<String>()
                    val attachments = mutableListOf<Triple<String, String, String>>()
                    var numberedCounter = 0
                    for (block in blocks) {
                        val blockText = com.example.util.RichTextConverter.segmentsToPlainText(block.ensureSegments())
                        when (block.type) {
                            com.example.data.model.BlockType.TEXT,
                            com.example.data.model.BlockType.CODE_BLOCK -> {
                                numberedCounter = 0
                                textParts.add(blockText)
                            }
                            com.example.data.model.BlockType.BULLET_LIST -> {
                                numberedCounter = 0
                                textParts.add("• $blockText")
                            }
                            com.example.data.model.BlockType.NUMBERED_LIST -> {
                                numberedCounter++
                                textParts.add("$numberedCounter. $blockText")
                            }
                            com.example.data.model.BlockType.QUOTE -> {
                                numberedCounter = 0
                                textParts.add("▎ $blockText")
                            }
                            com.example.data.model.BlockType.CALLOUT -> {
                                numberedCounter = 0
                                textParts.add("💡 $blockText")
                            }
                            com.example.data.model.BlockType.PAGE -> {
                                numberedCounter = 0
                                val icon = if (block.meta["iconType"] == "emoji") block.meta["iconValue"].orEmpty() else "🔗"
                                val linkedId = block.meta["noteId"]?.toIntOrNull()
                                val pageText = linkedId?.let { pageTitleById(it) }
                                    .orEmpty()
                                    .ifBlank { blockText }
                                    .ifBlank { pageBlockLabel }
                                textParts.add("$icon $pageText")
                            }
                            com.example.data.model.BlockType.PAGE_LINK -> {
                                numberedCounter = 0
                                val linkedId = block.meta["noteId"]?.toIntOrNull()
                                val pageLinkText = linkedId?.let { pageTitleById(it) }
                                    .orEmpty()
                                    .ifBlank { blockText }
                                    .ifBlank { pageBlockLabel }
                                textParts.add("🔗 $pageLinkText")
                            }
                            com.example.data.model.BlockType.CHECKLIST_ITEM -> {
                                numberedCounter = 0
                                val prefix = if (block.meta["checked"] == "true") "☑ " else "☐ "
                                textParts.add("$prefix$blockText")
                            }
                            com.example.data.model.BlockType.HEADING1,
                            com.example.data.model.BlockType.HEADING2,
                            com.example.data.model.BlockType.HEADING3 -> {
                                numberedCounter = 0
                                textParts.add(blockText)
                            }
                            com.example.data.model.BlockType.IMAGE -> attachments.add(Triple("image", block.content, ""))
                            com.example.data.model.BlockType.VIDEO -> {
                                numberedCounter = 0
                                attachments.add(Triple("video", block.content, ""))
                            }
                            com.example.data.model.BlockType.AUDIO -> {
                                numberedCounter = 0
                                attachments.add(Triple("audio", block.content, ""))
                            }
                            com.example.data.model.BlockType.DRAWING -> {
                                numberedCounter = 0
                                if (!block.isWysiwygDrawing) {
                                    val previewPath = block.meta["previewPath"] ?: ""
                                    attachments.add(Triple("drawing", block.content, previewPath))
                                }
                            }
                            com.example.data.model.BlockType.BOOKMARK -> {
                                numberedCounter = 0
                                textParts.add(blockText)
                            }
                            else -> numberedCounter = 0
                        }
                    }
                    Pair(textParts.joinToString("\n"), attachments)
                } else {
                    val (cleanNoteText, allAttachments) = com.example.data.model.parseNoteContentAndAttachments(content)
                    val fromLegacy = allAttachments.filter { it.type in listOf("drawing", "image", "video") }
                    val fromMediaTags = mutableListOf<Pair<String, String>>()
                    val regex = Regex("<(img|video)\\s+src=\"([^\"]+)\"\\s*/>")
                    regex.findAll(cleanNoteText).forEach { match ->
                        val tagType = match.groupValues[1]
                        val src = match.groupValues[2]
                        val normType = when (tagType) { "img" -> "image"; else -> tagType }
                        fromMediaTags.add(normType to src)
                    }
                    val attachList = fromLegacy.map { Triple(it.type, it.path, it.name) } +
                        fromMediaTags.map { Triple(it.first, it.second, "") }
                    Pair(cleanNoteText, attachList)
                }
            }

            val formattedContent = remember(displayText) {
                com.example.util.RichTextParser.parse(displayText, hideTags = true)
            }

            Text(
                text = formattedContent,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (isGrid) 3 else 4,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

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
                                            contentScale = ContentScale.Crop
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
                                    "audio" -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MusicNote,
                                                contentDescription = stringResource(R.string.attachment_audio),
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
fun LoadingShimmer(modifier: Modifier = Modifier) {
    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f)
    )
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(6) {
            ShimmerCard(brush)
        }
    }
}

@Composable
private fun ShimmerCard(brush: Brush) {
    Card(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Box(
                modifier = Modifier.fillMaxWidth(0.7f).height(16.dp).background(brush, RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier.fillMaxWidth(0.9f).height(12.dp).background(brush, RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier.fillMaxWidth(0.5f).height(12.dp).background(brush, RoundedCornerShape(4.dp))
            )
        }
    }
}
