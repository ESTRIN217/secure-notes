package com.estrin217.editordecodigo.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.WrapText
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.estrin217.editordecodigo.R
import com.estrin217.editordecodigo.syntax.SupportedLanguage
import com.estrin217.editordecodigo.syntax.SyntaxTheme
import com.estrin217.editordecodigo.ui.components.CodeEditorView
import com.estrin217.editordecodigo.ui.components.EditorTabsBar
import com.estrin217.editordecodigo.ui.components.FilesDrawerSheet
import com.estrin217.editordecodigo.ui.components.FormatOptionsDialog
import com.estrin217.editordecodigo.ui.components.JumpToLineDialog
import com.estrin217.editordecodigo.ui.components.LanguageSelectorDialog
import com.estrin217.editordecodigo.ui.components.NewFileDialog
import com.estrin217.editordecodigo.ui.components.QuickSymbolBar
import com.estrin217.editordecodigo.ui.components.SearchReplaceBar
import com.estrin217.editordecodigo.ui.components.ThemeSelectorDialog
import com.estrin217.editordecodigo.ui.components.VSCodeWelcomeScreen
import com.estrin217.editordecodigo.ui.theme.JetBrainsMono
import com.estrin217.editordecodigo.utils.FileImportExportHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeEditorApp(
    viewModel: CodeEditorViewModel,
    topTabs: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // System file picker launcher for single external files
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            if (FileImportExportHelper.isOversize(context, uri)) {
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.file_too_large))
                }
                return@rememberLauncherForActivityResult
            }
            val fileInfo = FileImportExportHelper.readExternalFile(context, uri)
            if (fileInfo != null) {
                viewModel.importExternalFile(
                    name = fileInfo.name,
                    content = fileInfo.content,
                    detectedLanguage = fileInfo.language,
                    uriString = fileInfo.uriString,
                    filePath = fileInfo.filePath
                )
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.file_read_error))
                }
            }
        }
    }

    // System directory picker launcher for opening a folder (VS Code style "Abrir carpeta")
    val openDirectoryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            val (folderName, filesInFolder) = FileImportExportHelper.readExternalFolder(context, treeUri)
            if (filesInFolder.isNotEmpty()) {
                viewModel.importMultipleFiles(filesInFolder, folderName)
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.snackbar_folder_no_code))
                }
            }
        }
    }

    val files by viewModel.files.collectAsState()
    val openTabs by viewModel.openTabs.collectAsState()
    val dirtyFileIds by viewModel.dirtyFileIds.collectAsState()
    val currentFile by viewModel.currentFile.collectAsState()
    val isWelcomeTabOpen by viewModel.isWelcomeTabOpen.collectAsState()
    val textFieldValue by viewModel.textFieldValue.collectAsState()
    val language by viewModel.language.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()
    val showLineNumbers by viewModel.showLineNumbers.collectAsState()
    val wordWrap by viewModel.wordWrap.collectAsState()
    val fontSizeSp by viewModel.fontSizeSp.collectAsState()
    val theme by viewModel.syntaxTheme.collectAsState()
    val isSearchVisible by viewModel.isSearchVisible.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val replaceQuery by viewModel.replaceQuery.collectAsState()
    val currentMatchIndex by viewModel.currentMatchIndex.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val formatError by viewModel.formatError.collectAsState()
    val formatOptions by viewModel.formatOptions.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()

    val shareSubjectDefault = stringResource(R.string.share_subject_default)

    // Dialog & Sheet States
    var showFilesSheet by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showJumpToLineDialog by remember { mutableStateOf(false) }
    var showLanguageSelector by remember { mutableStateOf(false) }
    var showThemeSelector by remember { mutableStateOf(false) }
    var showFormatOptionsDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    @Suppress("DEPRECATION")
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Handle toast messages
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearToast()
        }
    }

    // Stats calculations
    val (cursorLine, cursorCol) = remember(textFieldValue.text, textFieldValue.selection) {
        viewModel.getCursorPositionInfo()
    }
    val totalLines = remember(textFieldValue.text) {
        textFieldValue.text.split("\n").size
    }
    val matchCount = remember(searchQuery, textFieldValue.text) {
        viewModel.getMatchesCount()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(theme.gutterBackground)
            ) {
                topTabs()

                // Redesigned Top Bar: VS Code Titlebar / Command Center
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Left Brand & Breadcrumb
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { showFilesSheet = true }
                                    .padding(vertical = 4.dp)
                            ) {
                                if (currentFile != null) {
                                    Text(
                                        text = " › ",
                                        color = theme.lineNumber.copy(alpha = 0.5f),
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = currentFile?.name ?: "",
                                        color = theme.lineNumber,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 110.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Center Command Center search pill (VS Code style)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(theme.activeLineBackground.copy(alpha = 0.85f))
                                    .border(
                                        BorderStroke(1.dp, theme.lineNumber.copy(alpha = 0.22f)),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { viewModel.toggleSearch() }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .testTag("topbar_command_center"),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = stringResource(R.string.cd_search_or_replace),
                                        tint = if (isSearchVisible) MaterialTheme.colorScheme.primary else theme.lineNumber,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = when {
                                            isSearchVisible && searchQuery.isNotBlank() -> stringResource(R.string.topbar_search_query, searchQuery)
                                            currentFile != null -> stringResource(R.string.topbar_search_file, currentFile?.name.orEmpty())
                                            else -> stringResource(R.string.topbar_search_command)
                                        },
                                        fontSize = 11.sp,
                                        color = if (isSearchVisible) theme.text else theme.lineNumber,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { showFilesSheet = true },
                            modifier = Modifier.testTag("appbar_files_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = stringResource(R.string.cd_view_files),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    actions = {
                        // Quick Edit / View Mode Toggle Chip
                        FilterChip(
                            selected = isEditMode,
                            onClick = { viewModel.toggleEditMode() },
                            label = {
                                Text(
                                    text = if (isEditMode) stringResource(R.string.mode_edit) else stringResource(R.string.mode_view),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isEditMode) Icons.Default.Edit else Icons.Default.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            modifier = Modifier.testTag("toggle_mode_chip")
                        )

                        Spacer(modifier = Modifier.width(2.dp))

                        // Undo button (Deshacer)
                        IconButton(
                            onClick = { viewModel.undo() },
                            enabled = canUndo,
                            modifier = Modifier.testTag("appbar_undo_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = stringResource(R.string.cd_undo),
                                tint = if (canUndo) theme.text else theme.comment.copy(alpha = 0.4f)
                            )
                        }

                        // Redo button (Rehacer)
                        IconButton(
                            onClick = { viewModel.redo() },
                            enabled = canRedo,
                            modifier = Modifier.testTag("appbar_redo_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Redo,
                                contentDescription = stringResource(R.string.cd_redo),
                                tint = if (canRedo) theme.text else theme.comment.copy(alpha = 0.4f)
                            )
                        }

                        // Save button with unsaved modifications indicator
                        val isCurrentModified = currentFile != null && dirtyFileIds.contains(currentFile?.id)
                        IconButton(
                            onClick = { viewModel.saveCurrentFile() },
                            modifier = Modifier.testTag("appbar_save_btn")
                        ) {
                            Box(contentAlignment = Alignment.TopEnd) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = stringResource(R.string.save),
                                    tint = if (isCurrentModified) MaterialTheme.colorScheme.primary else theme.text
                                )
                                if (isCurrentModified) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    )
                                }
                            }
                        }

                        // Overflow Menu
                        Box {
                            IconButton(
                                onClick = { showOverflowMenu = true },
                                modifier = Modifier.testTag("appbar_overflow_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.more_options),
                                    tint = theme.text
                                )
                            }

                            DropdownMenu(
                                expanded = showOverflowMenu,
                                onDismissRequest = { showOverflowMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_open_external)) },
                                    leadingIcon = { Icon(Icons.Default.FileOpen, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        openDocumentLauncher.launch(arrayOf("*/*"))
                                    },
                                    modifier = Modifier.testTag("menu_open_external_file")
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_open_folder)) },
                                    leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        openDirectoryLauncher.launch(null)
                                    },
                                    modifier = Modifier.testTag("menu_open_folder")
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_jump_line)) },
                                    leadingIcon = { Icon(Icons.Default.FormatListNumbered, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        showJumpToLineDialog = true
                                    },
                                    modifier = Modifier.testTag("menu_jump_line")
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_syntax)) },
                                    leadingIcon = { Icon(Icons.Default.TextFields, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        showLanguageSelector = true
                                    },
                                    modifier = Modifier.testTag("menu_language")
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_theme)) },
                                    leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        showThemeSelector = true
                                    },
                                    modifier = Modifier.testTag("menu_theme")
                                )
                                DropdownMenuItem(
                                    text = { Text(if (showLineNumbers) stringResource(R.string.menu_hide_lines) else stringResource(R.string.menu_show_lines)) },
                                    leadingIcon = { Icon(Icons.Default.FormatListNumbered, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        viewModel.toggleLineNumbers()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (wordWrap) stringResource(R.string.menu_wrap_off) else stringResource(R.string.menu_wrap_on)) },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.WrapText, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        viewModel.toggleWordWrap()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.format_code)) },
                                    leadingIcon = { Icon(Icons.Default.AutoFixHigh, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        viewModel.formatCode()
                                    },
                                    modifier = Modifier.testTag("menu_format_code")
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.menu_format_options)) },
                                    leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        showFormatOptionsDialog = true
                                    },
                                    modifier = Modifier.testTag("menu_format_options")
                                )
                                if (language == SupportedLanguage.JSON) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.menu_minify)) },
                                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.FormatAlignLeft, contentDescription = null) },
                                        onClick = {
                                            showOverflowMenu = false
                                            viewModel.minifyJson()
                                        }
                                    )
                                }
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.copy_code)) },
                                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Code", textFieldValue.text))
                                        scope.launch {
                                            snackbarHostState.showSnackbar(context.getString(R.string.copied_clipboard))
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.share_script)) },
                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, textFieldValue.text)
                                            putExtra(Intent.EXTRA_SUBJECT, currentFile?.name ?: shareSubjectDefault)
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.share_chooser)))
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = theme.gutterBackground,
                        titleContentColor = theme.text,
                        navigationIconContentColor = theme.text,
                        actionIconContentColor = theme.text
                    )
                )

                // VS Code Horizontal Tabs Bar
                EditorTabsBar(
                    tabs = openTabs,
                    activeFile = currentFile,
                    dirtyFileIds = dirtyFileIds,
                    theme = theme,
                    isWelcomeTabOpen = isWelcomeTabOpen,
                    onSelectTab = { viewModel.selectFile(it) },
                    onCloseTab = { viewModel.closeTab(it) },
                    onCloseOtherTabs = { viewModel.closeOtherTabs(it) },
                    onCloseAllTabs = { viewModel.closeAllTabs() },
                    onNewTab = { showNewFileDialog = true },
                    onSelectWelcomeTab = { viewModel.openWelcomeTab() },
                    onCloseWelcomeTab = { viewModel.closeWelcomeTab() }
                )
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(theme.background)
        ) {
            // Format Error Banner
            formatError?.let { err ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearFormatError() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.close),
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Search and Replace Bar
            SearchReplaceBar(
                isVisible = isSearchVisible,
                theme = theme,
                searchQuery = searchQuery,
                replaceQuery = replaceQuery,
                matchCount = matchCount,
                currentMatchIndex = currentMatchIndex,
                onSearchChange = { viewModel.setSearchQuery(it) },
                onReplaceChange = { viewModel.setReplaceQuery(it) },
                onNextMatch = { viewModel.findNextMatch(forward = true) },
                onPrevMatch = { viewModel.findNextMatch(forward = false) },
                onReplaceSingle = { viewModel.replaceSingle() },
                onReplaceAll = { viewModel.replaceAll() },
                onClose = { viewModel.toggleSearch() }
            )

            // Main Editor & Viewer Area
            Box(modifier = Modifier.weight(1f)) {
                if (currentFile != null) {
                    CodeEditorView(
                        textFieldValue = textFieldValue,
                        onValueChange = { viewModel.updateTextFieldValue(it) },
                        language = language,
                        theme = theme,
                        isEditMode = isEditMode,
                        showLineNumbers = showLineNumbers,
                        wordWrap = wordWrap,
                        fontSizeSp = fontSizeSp,
                        searchQuery = searchQuery
                    )
                } else if (isWelcomeTabOpen) {
                    // Pantalla de bienvenida estilo VS Code (Visual Studio Code Start Page)
                    VSCodeWelcomeScreen(
                        theme = theme,
                        recentFiles = files,
                        onNewFile = { showNewFileDialog = true },
                        onOpenFile = { openDocumentLauncher.launch(arrayOf("*/*")) },
                        onOpenFolder = { openDirectoryLauncher.launch(null) },
                        onSelectRecentFile = { viewModel.selectFile(it) }
                    )
                } else {
                    EmptyWorkspaceView(
                        theme = theme,
                        onNewFile = { showNewFileDialog = true },
                        onOpenFile = { openDocumentLauncher.launch(arrayOf("*/*")) }
                    )
                }
            }

            // Quick Symbol Toolbar (Available in Edit Mode when an editor is active)
            if (isEditMode && currentFile != null) {
                QuickSymbolBar(
                    theme = theme,
                    onInsertSymbol = { symbol -> viewModel.insertTextAtCursor(symbol) },
                    onInsertIndent = { viewModel.insertIndentation(formatOptions.indentSize) },
                    onToggleComment = { viewModel.toggleCommentOnCurrentLine() },
                    onDuplicateLine = { viewModel.duplicateCurrentLine() },
                    onDeleteLine = { viewModel.deleteCurrentLine() }
                )
            }

            // Editor Status Bar at bottom (visible when file is active)
            if (currentFile != null) {
                Surface(
                    color = theme.gutterBackground,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Cursor position and stats
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.status_cursor, cursorLine, cursorCol),
                                color = theme.lineNumber,
                                fontSize = 11.sp,
                                fontFamily = JetBrainsMono,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "•",
                                color = theme.lineNumber,
                                fontSize = 11.sp
                            )
                            Text(
                                text = stringResource(R.string.status_lines, totalLines),
                                color = theme.lineNumber,
                                fontSize = 11.sp,
                                fontFamily = JetBrainsMono
                            )
                        }

                        // Right: Language picker badge and zoom controls
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Clickable language badge
                            Surface(
                                onClick = { showLanguageSelector = true },
                                shape = RoundedCornerShape(4.dp),
                                color = theme.background,
                                modifier = Modifier.testTag("status_lang_badge")
                            ) {
                                Text(
                                    text = language.displayName,
                                    color = theme.function,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = JetBrainsMono,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            // Zoom buttons
                            IconButton(
                                onClick = { viewModel.decreaseFontSize() },
                                modifier = Modifier.size(24.dp).testTag("zoom_out_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ZoomOut,
                                    contentDescription = stringResource(R.string.zoom_out),
                                    tint = theme.lineNumber,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Text(
                                text = stringResource(R.string.status_font_size, fontSizeSp.toInt()),
                                color = theme.lineNumber,
                                fontSize = 11.sp,
                                fontFamily = JetBrainsMono
                            )

                            IconButton(
                                onClick = { viewModel.increaseFontSize() },
                                modifier = Modifier.size(24.dp).testTag("zoom_in_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ZoomIn,
                                    contentDescription = stringResource(R.string.zoom_in),
                                    tint = theme.lineNumber,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Files Drawer Sheet
    if (showFilesSheet) {
        FilesDrawerSheet(
            sheetState = sheetState,
            files = files,
            currentFile = currentFile,
            onDismiss = { showFilesSheet = false },
            onSelectFile = { file -> viewModel.selectFile(file) },
            onDeleteFile = { file -> viewModel.deleteFile(file) },
            onOpenNewFileDialog = { showNewFileDialog = true },
            onOpenFilePicker = {
                openDocumentLauncher.launch(arrayOf("*/*"))
            }
        )
    }

    // Dialogs
    if (showNewFileDialog) {
        NewFileDialog(
            onDismiss = { showNewFileDialog = false },
            onCreate = { name, lang -> viewModel.createNewFile(name, lang) }
        )
    }

    if (showJumpToLineDialog) {
        JumpToLineDialog(
            totalLines = totalLines,
            onDismiss = { showJumpToLineDialog = false },
            onJump = { line -> viewModel.jumpToLine(line) }
        )
    }

    if (showLanguageSelector) {
        LanguageSelectorDialog(
            currentLanguage = language,
            onDismiss = { showLanguageSelector = false },
            onSelectLanguage = { viewModel.setLanguage(it) }
        )
    }

    if (showThemeSelector) {
        ThemeSelectorDialog(
            currentTheme = theme,
            onDismiss = { showThemeSelector = false },
            onSelectTheme = { viewModel.setSyntaxTheme(it) }
        )
    }

    if (showFormatOptionsDialog) {
        FormatOptionsDialog(
            currentOptions = formatOptions,
            currentLanguage = language,
            onDismiss = { showFormatOptionsDialog = false },
            onApplyAndFormat = { newOptions ->
                showFormatOptionsDialog = false
                viewModel.updateFormatOptions(newOptions)
                viewModel.formatCode(newOptions)
            }
        )
    }
}

@Composable
private fun EmptyWorkspaceView(
    theme: SyntaxTheme,
    onNewFile: () -> Unit,
    onOpenFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(theme.background)
            .testTag("empty_workspace_view"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Surface(
                color = Color(0xFF0078D4).copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF0078D4).copy(alpha = 0.35f)),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = Color(0xFF0078D4),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Text(
                text = stringResource(R.string.empty_workspace_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = theme.text
            )

            Text(
                text = stringResource(R.string.empty_workspace_desc),
                style = MaterialTheme.typography.bodySmall,
                color = theme.lineNumber,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.widthIn(max = 280.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Button(
                    onClick = onNewFile,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("empty_new_file_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.empty_new_file), fontSize = 12.sp)
                }
            }
        }
    }
}
