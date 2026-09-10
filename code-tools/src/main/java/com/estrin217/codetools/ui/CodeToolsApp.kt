package com.estrin217.codetools.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.WrapText
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.estrin217.codetools.syntax.SupportedLanguage
import com.estrin217.codetools.ui.components.CodeEditorView
import com.estrin217.codetools.ui.theme.JetBrainsMono
import com.estrin217.codetools.ui.components.FilesDrawerSheet
import com.estrin217.codetools.ui.components.FormatOptionsDialog
import com.estrin217.codetools.ui.components.JumpToLineDialog
import com.estrin217.codetools.ui.components.LanguageSelectorDialog
import com.estrin217.codetools.ui.components.NewFileDialog
import com.estrin217.codetools.ui.components.QuickSymbolBar
import com.estrin217.codetools.ui.components.SearchReplaceBar
import com.estrin217.codetools.ui.components.ThemeSelectorDialog
import com.estrin217.codetools.utils.FileImportExportHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeToolsApp(viewModel: CodeEditorViewModel, topTabs: @Composable () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // System file picker launcher for external files
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        if (FileImportExportHelper.isOversize(context, uri)) {
            scope.launch { snackbarHostState.showSnackbar("Archivo demasiado grande (máx. 2 MB)") }
            return@rememberLauncherForActivityResult
        }
        val fileInfo = FileImportExportHelper.readExternalFile(context, uri)
        if (fileInfo == null) {
            scope.launch { snackbarHostState.showSnackbar("No se pudo leer el archivo seleccionado") }
            return@rememberLauncherForActivityResult
        }
        viewModel.importExternalFile(fileInfo.name, fileInfo.content, fileInfo.language)
    }

    val files by viewModel.files.collectAsState()
    val currentFile by viewModel.currentFile.collectAsState()
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

    // Dialog & Sheet States
    var showFilesSheet by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showJumpToLineDialog by remember { mutableStateOf(false) }
    var showLanguageSelector by remember { mutableStateOf(false) }
    var showThemeSelector by remember { mutableStateOf(false) }
    var showFormatOptionsDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

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
            Column {
                topTabs()
                TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showFilesSheet = true }
                    ) {
                        Column {
                            Text(
                                text = currentFile?.name ?: "Sin título",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${language.displayName} • $totalLines lín",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                            contentDescription = "Ver archivos",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    // Quick Edit / View Mode Toggle
                    FilterChip(
                        selected = isEditMode,
                        onClick = { viewModel.toggleEditMode() },
                        label = {
                            Text(
                                text = if (isEditMode) "Editar" else "Ver",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isEditMode) Icons.Default.Edit else Icons.Default.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("toggle_mode_chip")
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Search toggle button
                    IconButton(
                        onClick = { viewModel.toggleSearch() },
                        modifier = Modifier.testTag("appbar_search_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar y reemplazar",
                            tint = if (isSearchVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Format Code button (universal for all supported languages)
                    IconButton(
                        onClick = { viewModel.formatCode() },
                        modifier = Modifier.testTag("appbar_format_code_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoFixHigh,
                            contentDescription = "Formatear código",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }

                    // Save button
                    IconButton(
                        onClick = { viewModel.saveCurrentFile() },
                        modifier = Modifier.testTag("appbar_save_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Guardar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Overflow Menu
                    Box {
                        IconButton(
                            onClick = { showOverflowMenu = true },
                            modifier = Modifier.testTag("appbar_overflow_btn")
                        ) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Más opciones")
                        }

                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Abrir archivo externo...") },
                                leadingIcon = { Icon(Icons.Default.FileOpen, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    openDocumentLauncher.launch(arrayOf("*/*"))
                                },
                                modifier = Modifier.testTag("menu_open_external_file")
                            )
                            DropdownMenuItem(
                                text = { Text("Nuevo archivo...") },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    showNewFileDialog = true
                                },
                                modifier = Modifier.testTag("menu_new_file")
                            )
                            DropdownMenuItem(
                                text = { Text("Ir a la línea...") },
                                leadingIcon = { Icon(Icons.Default.FormatListNumbered, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    showJumpToLineDialog = true
                                },
                                modifier = Modifier.testTag("menu_jump_line")
                            )
                            DropdownMenuItem(
                                text = { Text("Resaltado de sintaxis") },
                                leadingIcon = { Icon(Icons.Default.TextFields, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    showLanguageSelector = true
                                },
                                modifier = Modifier.testTag("menu_language")
                            )
                            DropdownMenuItem(
                                text = { Text("Tema del editor") },
                                leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    showThemeSelector = true
                                },
                                modifier = Modifier.testTag("menu_theme")
                            )
                            DropdownMenuItem(
                                text = { Text(if (showLineNumbers) "Ocultar número de líneas" else "Mostrar número de líneas") },
                                leadingIcon = { Icon(Icons.Default.FormatListNumbered, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.toggleLineNumbers()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (wordWrap) "Desactivar ajuste de línea" else "Ajuste de línea (Word Wrap)") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.WrapText, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.toggleWordWrap()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Formatear código") },
                                leadingIcon = { Icon(Icons.Default.AutoFixHigh, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.formatCode()
                                },
                                modifier = Modifier.testTag("menu_format_code")
                            )
                            DropdownMenuItem(
                                text = { Text("Opciones de formateo...") },
                                leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    showFormatOptionsDialog = true
                                },
                                modifier = Modifier.testTag("menu_format_options")
                            )
                            if (language == SupportedLanguage.JSON) {
                                DropdownMenuItem(
                                    text = { Text("Compactar JSON (Minify)") },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.FormatAlignLeft, contentDescription = null) },
                                    onClick = {
                                        showOverflowMenu = false
                                        viewModel.minifyJson()
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Copiar código") },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Code", textFieldValue.text))
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Código copiado al portapapeles")
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Compartir script") },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                onClick = {
                                    showOverflowMenu = false
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, textFieldValue.text)
                                        putExtra(Intent.EXTRA_SUBJECT, currentFile?.name ?: "script")
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Compartir código"))
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
                                contentDescription = "Cerrar",
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
            }

            // Quick Symbol Toolbar (Available in Edit Mode for fast coding on mobile)
            if (isEditMode) {
                QuickSymbolBar(
                    theme = theme,
                    onInsertSymbol = { symbol -> viewModel.insertTextAtCursor(symbol) },
                    onInsertIndent = { viewModel.insertIndentation(formatOptions.indentSize) },
                    onToggleComment = { viewModel.toggleCommentOnCurrentLine() },
                    onDuplicateLine = { viewModel.duplicateCurrentLine() },
                    onDeleteLine = { viewModel.deleteCurrentLine() },
                    onFormatCode = { viewModel.formatCode() }
                )
            }

            // Editor Status Bar at bottom
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
                            text = "Lín $cursorLine, Col $cursorCol",
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
                            text = "$totalLines líns",
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
                                contentDescription = "Reducir tamaño",
                                tint = theme.lineNumber,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Text(
                            text = "${fontSizeSp.toInt()}pt",
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
                                contentDescription = "Aumentar tamaño",
                                tint = theme.lineNumber,
                                modifier = Modifier.size(16.dp)
                            )
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
