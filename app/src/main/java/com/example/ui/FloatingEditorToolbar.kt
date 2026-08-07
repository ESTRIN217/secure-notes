package com.example.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatIndentDecrease
import androidx.compose.material.icons.automirrored.filled.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatClear
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.FormatShapes
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.example.R
import java.util.Locale

// Enum para controlar la vista activa de la barra flotante (Clean State)
enum class EditorToolbarMode {
    MAIN,
    TEXT_FORMAT,
    SEARCH
}

@Composable
fun EditorToolbarContainer(
    modifier: Modifier = Modifier,
    activeTextStyles: Set<String>,
    isSpeaking: Boolean,
    aiEnabled: Boolean,
    showAiPanel: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleTag: (String) -> Unit,
    onClearFormatting: () -> Unit,
    onOpenMoreFormatting: () -> Unit,
    onOpenPalette: () -> Unit,
    onTtsToggle: () -> Unit,
    onOpenDrawing: () -> Unit,
    onOpenAttachments: () -> Unit,
    onOpenAi: () -> Unit,
    onToggleAiPanel: () -> Unit,
    onToggleKeyboard: () -> Unit,
    onOpenbgFontColor: () -> Unit,
    onOpenInlineLink: () -> Unit,
    onOpenEquation: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    matchCount: Int,
    currentMatchIndex: Int,
    onPreviousMatch: () -> Unit,
    onNextMatch: () -> Unit,
    caseSensitive: Boolean,
    onCaseSensitiveChange: (Boolean) -> Unit,
    fullWord: Boolean,
    onFullWordChange: (Boolean) -> Unit,
    decreaseIndent: () -> Unit,
    pasteFromClipboard: () -> Unit,
    insertCurrentDate: () -> Unit,
    applyTagWithVal: (String, String) -> Unit
) {
    var currentMode by remember { mutableStateOf(EditorToolbarMode.MAIN) }

    OutlinedCard(
        modifier = modifier
            .padding(bottom = 8.dp)
            .fillMaxWidth(0.95f),
        shape = CircleShape,
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        when (currentMode) {
            EditorToolbarMode.MAIN -> FloatingEditorToolbar(
                aiEnabled = aiEnabled,
                showAiPanel = showAiPanel,
                canUndo = canUndo,
                canRedo = canRedo,
                isSpeaking = isSpeaking,
                onUndo = onUndo,
                onRedo = onRedo,
                onClearFormatting = onClearFormatting,
                onOpenMoreFormatting = onOpenMoreFormatting,
                onTexto = { currentMode = EditorToolbarMode.TEXT_FORMAT },
                onOpenSearch = { currentMode = EditorToolbarMode.SEARCH },
                onOpenPalette = onOpenPalette,
                onTtsToggle = onTtsToggle,
                onOpenDrawing = onOpenDrawing,
                onOpenAttachments = onOpenAttachments,
                onOpenAi = onOpenAi,
                onToggleAiPanel = onToggleAiPanel,
                onToggleKeyboard = onToggleKeyboard,
                onToggleTag = onToggleTag,
                decreaseIndent = decreaseIndent,
                pasteFromClipboard = pasteFromClipboard,
                insertCurrentDate = insertCurrentDate
            )

            EditorToolbarMode.TEXT_FORMAT -> TextoToolbar(
                activeTextStyles = activeTextStyles,
                onToggleTag = onToggleTag,
                onBack = { currentMode = EditorToolbarMode.MAIN },
                onOpenbgFontColor = onOpenbgFontColor,
                onOpenInlineLink = onOpenInlineLink,
                onOpenEquation = onOpenEquation,
                applyTagWithVal = applyTagWithVal
            )

            EditorToolbarMode.SEARCH -> InlineSearchBar(
                onClose = { currentMode = EditorToolbarMode.MAIN },
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                matchCount = matchCount,
                currentMatchIndex = currentMatchIndex,
                onPrevious = onPreviousMatch,
                onNext = onNextMatch,
                caseSensitive = caseSensitive,
                onCaseSensitiveChange = onCaseSensitiveChange,
                fullWord = fullWord,
                onFullWordChange = onFullWordChange
            )
        }
    }
}

@Composable
private fun FloatingEditorToolbar(
    aiEnabled: Boolean,
    showAiPanel: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    isSpeaking: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClearFormatting: () -> Unit,
    onOpenMoreFormatting: () -> Unit,
    onTexto: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenPalette: () -> Unit,
    onTtsToggle: () -> Unit,
    onOpenDrawing: () -> Unit,
    onOpenAttachments: () -> Unit,
    onOpenAi: () -> Unit,
    onToggleAiPanel: () -> Unit,
    onToggleKeyboard: () -> Unit,
    onToggleTag: (String) -> Unit,
    decreaseIndent: () -> Unit,
    pasteFromClipboard: () -> Unit,
    insertCurrentDate: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (aiEnabled) {
                ToolbarIconButton(
                    icon = Icons.Default.AutoAwesome,
                    contentDescription = "Asistente IA",
                    tint = MaterialTheme.colorScheme.primary,
                    onClick = onOpenAi
                )
                ToolbarDivider()
            }

            ToolbarIconButton(
                icon = Icons.Default.Add,
                contentDescription = "Bloques",
                onClick = onOpenMoreFormatting
            )

            ToolbarIconButton(
                icon = Icons.Default.FormatShapes,
                contentDescription = "Opciones de texto",
                onClick = onTexto
            )

            ToolbarIconButton(
                icon = Icons.AutoMirrored.Filled.Undo,
                contentDescription = "Deshacer",
                enabled = canUndo,
                onClick = onUndo
            )

            ToolbarIconButton(
                icon = Icons.AutoMirrored.Filled.Redo,
                contentDescription = "Rehacer",
                enabled = canRedo,
                onClick = onRedo
            )

            ToolbarIconButton(
                icon = Icons.Default.FormatClear,
                contentDescription = "Limpiar formato",
                onClick = onClearFormatting
            )

            ToolbarIconButton(
                icon = Icons.AutoMirrored.Filled.FormatIndentIncrease,
                contentDescription = "Aumentar sangría",
                onClick = { onToggleTag("indent")}
            )

            ToolbarIconButton(
                icon = Icons.AutoMirrored.Filled.FormatIndentDecrease,
                contentDescription = "Disminuir sangría",
                onClick = { decreaseIndent()}
            )
            ToolbarIconButton(
                icon = Icons.Default.ArrowDropUp,
                contentDescription = "Subir bloque",
                onClick = { 
                    //funcion debe poner el cursor arriba tecla arrow
                }
            )
            ToolbarIconButton(
                icon = Icons.Default.ArrowDropDown,
                contentDescription = "Bajar bloque",
                onClick = {
                    // funcion debe poner el cursor abajo tecla down
                }
            )

            ToolbarDivider()

            ToolbarIconButton(
                icon = Icons.Default.Palette,
                contentDescription = "Estilo de la nota",
                tint = MaterialTheme.colorScheme.primary,
                onClick = onOpenPalette
            )

            ToolbarIconButton(
                icon = if (isSpeaking) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = if (isSpeaking) "Detener lectura" else "Leer en voz alta",
                tint = if (isSpeaking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                onClick = onTtsToggle
            )

            ToolbarIconButton(
                icon = Icons.Default.Gesture,
                contentDescription = "Añadir dibujo",
                tint = MaterialTheme.colorScheme.primary,
                onClick = onOpenDrawing
            )

            ToolbarIconButton(
                icon = Icons.Default.AttachFile,
                contentDescription = "Adjuntar archivo",
                tint = MaterialTheme.colorScheme.primary,
                onClick = onOpenAttachments
            )

            if (aiEnabled) {
                ToolbarIconButton(
                    icon = if (showAiPanel) Icons.Default.Close else Icons.Default.RateReview,
                    contentDescription = "Panel IA",
                    tint = MaterialTheme.colorScheme.primary,
                    onClick = onToggleAiPanel
                )
            }

            ToolbarDivider()

            ToolbarIconButton(
                icon = Icons.Default.Search,
                contentDescription = "Buscar",
                onClick = onOpenSearch
            )

            ToolbarIconButton(
                icon = Icons.Default.ContentPaste,
                contentDescription = "Pegar con formato",
                onClick = { pasteFromClipboard() }
            )

            ToolbarIconButton(
                icon = Icons.Default.Today,
                contentDescription = "Insertar fecha",
                onClick = { insertCurrentDate() }
            )
        }

        ToolbarDivider()

        ToolbarIconButton(
            icon = Icons.Default.Keyboard,
            contentDescription = "Alternar teclado",
            tint = MaterialTheme.colorScheme.primary,
            onClick = onToggleKeyboard
        )
    }
}

@Composable
private fun TextoToolbar(
    activeTextStyles: Set<String>,
    onToggleTag: (String) -> Unit,
    onBack: () -> Unit,
    onOpenbgFontColor: () -> Unit,
    onOpenInlineLink: () -> Unit,
    onOpenEquation: () -> Unit,
    applyTagWithVal: (String, String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ToolbarIconButton(
            icon = Icons.Default.ArrowBack,
            contentDescription = "Volver",
            tint = MaterialTheme.colorScheme.primary,
            onClick = onBack
        )

        ToolbarIconButton(
            icon = Icons.Default.FormatPaint,
            contentDescription = "Color de texto y fondo",
            onClick = onOpenbgFontColor
        )

        FormattingToggleButton(
            checked = "b" in activeTextStyles,
            onCheckedChange = { onToggleTag("b") }
        ) {
            Text("B", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        FormattingToggleButton(
            checked = "i" in activeTextStyles,
            onCheckedChange = { onToggleTag("i") }
        ) {
            Text("I", fontStyle = FontStyle.Italic, fontSize = 13.sp)
        }

        FormattingToggleButton(
            checked = "u" in activeTextStyles,
            onCheckedChange = { onToggleTag("u") }
        ) {
            Text("U", style = TextStyle(textDecoration = TextDecoration.Underline), fontSize = 13.sp)
        }

        FormattingToggleButton(
            checked = "s" in activeTextStyles,
            onCheckedChange = { onToggleTag("s") }
        ) {
            Text("S", style = TextStyle(textDecoration = TextDecoration.LineThrough), fontSize = 13.sp)
        }

        ToolbarIconButton(icon = Icons.Default.Link, contentDescription = "Enlace", onClick = onOpenInlineLink)
        FormattingToggleButton(
            checked = "code" in activeTextStyles,
            onCheckedChange = { onToggleTag("code") }
        ) {
            Icon(
                imageVector = Icons.Default.Code,
                contentDescription = "Código inline",
                modifier = Modifier.size(18.dp)
            )
        }
        ToolbarIconButton(icon = Icons.Default.Functions, contentDescription = "Ecuaciones", onClick = onOpenEquation)
        FilledTonalIconToggleButton(
            checked = "sub" in activeTextStyles,
            onCheckedChange = { onToggleTag("sub") },
            modifier = Modifier.size(36.dp)
        ) {
            Text("x₂", fontSize = 14.sp)
        }

        FilledTonalIconToggleButton(
            checked = "sup" in activeTextStyles,
            onCheckedChange = { onToggleTag("sup") },
            modifier = Modifier.size(36.dp)
        ) {
            Text("x²", fontSize = 14.sp)
        }

        ToolbarDivider()

        // Dropdown Tipografía
        var showFontDropdown by remember { mutableStateOf(false) }
        val applyFont: (String) -> Unit = { font ->
            applyTagWithVal("font", font)
        }
        
        Box {
            OutlinedButton(
                onClick = { showFontDropdown = true},
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(stringResource(id = R.string.rich_font_family), fontSize = 12.sp)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
            }
            DropdownMenu(
                expanded = showFontDropdown,
                onDismissRequest = { showFontDropdown = false}
            ) {
                listOf("default", "serif", "monospace", "sans-serif", "cursive").forEach { font ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = font.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() },
                                fontFamily = when (font) {
                                    "serif" -> FontFamily.Serif
                                    "monospace" -> FontFamily.Monospace
                                    "sans-serif" -> FontFamily.SansSerif
                                    "cursive" -> FontFamily.Cursive
                                    else -> FontFamily.Default
                                }
                            )
                        },
                        onClick = {
                            applyFont(font)
                            showFontDropdown = false
                        }
                    )
                }
            }
        }
        var showSizeDropdown by remember { mutableStateOf(false) }

        // Dropdown Tamaño
        Box {
            OutlinedButton(
                onClick = {},
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(stringResource(id = R.string.rich_font_size), fontSize = 12.sp)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
            }
            DropdownMenu(
                expanded = showSizeDropdown,
                onDismissRequest = { showSizeDropdown = false}
            ) {
                listOf("default", "12", "14", "16", "18", "20", "24", "28").forEach { size ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (size == "default")
                                stringResource(R.string.text_default)
                                else
                                "${size}sp"
                            )
                        },
                        onClick = {
                            applyTagWithVal("size", size)
                            showSizeDropdown = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun InlineSearchBar(
    onClose: () -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    matchCount: Int,
    currentMatchIndex: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    caseSensitive: Boolean,
    onCaseSensitiveChange: (Boolean) -> Unit,
    fullWord: Boolean,
    onFullWordChange: (Boolean) -> Unit
) {
    var showMoreOptions by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Buscar...", fontSize = 14.sp) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                )
            )

            Text(
                text = if (matchCount == 0) "0/0" else "${currentMatchIndex + 1}/$matchCount",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ToolbarIconButton(
                icon = Icons.Default.ArrowUpward,
                contentDescription = "Anterior",
                enabled = searchQuery.isNotEmpty() && matchCount > 0,
                onClick = onPrevious
            )

            ToolbarIconButton(
                icon = Icons.Default.ArrowDownward,
                contentDescription = "Siguiente",
                enabled = searchQuery.isNotEmpty() && matchCount > 0,
                onClick = onNext
            )

            ToolbarIconButton(
                icon = if (showMoreOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = "Más opciones",
                onClick = { showMoreOptions = !showMoreOptions }
            )

            ToolbarIconButton(
                icon = Icons.Default.Close,
                contentDescription = "Cerrar búsqueda",
                onClick = onClose
            )
        }

        if (showMoreOptions) {
            SearchMoreOptions(
                caseSensitive = caseSensitive,
                onCaseSensitiveChange = onCaseSensitiveChange,
                fullWord = fullWord,
                onFullWordChange = onFullWordChange
            )
        }
    }
}

@Composable
private fun SearchMoreOptions(
    caseSensitive: Boolean,
    onCaseSensitiveChange: (Boolean) -> Unit,
    fullWord: Boolean,
    onFullWordChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = caseSensitive, onCheckedChange = onCaseSensitiveChange)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Coincidir mayúsculas", style = MaterialTheme.typography.bodySmall)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = fullWord, onCheckedChange = onFullWordChange)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Palabra completa", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun ToolbarIconButton(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(36.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp),
            tint = if (enabled) tint else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
    }
}

@Composable
fun FormattingToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    FilledTonalIconToggleButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = Modifier.size(36.dp)
    ) {
        content()
    }
}

@Composable
fun ToolbarDivider() {
    VerticalDivider(
        modifier = Modifier
            .height(20.dp)
            .padding(horizontal = 4.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
fun TextoPreview() {
    MaterialTheme {
        TextoToolbar(
            activeTextStyles = setOf("b", "i"),
            onToggleTag = {},
            onBack = {},
            onOpenbgFontColor = {},
            onOpenInlineLink = {},
            onOpenEquation = {},
            applyTagWithVal = { _, _ -> }
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F5F5)
@Composable
fun FloatingToolbarContainerPreview() {
    MaterialTheme {
        EditorToolbarContainer(
            activeTextStyles = setOf("b", "i"),
            isSpeaking = false,
            aiEnabled = true,
            showAiPanel = false,
            canUndo = true,
            canRedo = false,
            onUndo = {},
            onRedo = {},
            onToggleTag = {},
            onClearFormatting = {},
            onOpenMoreFormatting = {},
            onOpenPalette = {},
            onTtsToggle = {},
            onOpenDrawing = {},
            onOpenAttachments = {},
            onOpenAi = {},
            onToggleAiPanel = {},
            onToggleKeyboard = {},
            onOpenbgFontColor = {},
            onOpenInlineLink = {},
            onOpenEquation = {},
            searchQuery = "",
            onSearchQueryChange = {},
            matchCount = 0,
            currentMatchIndex = 0,
            onPreviousMatch = {},
            onNextMatch = {},
            caseSensitive = false,
            onCaseSensitiveChange = {},
            fullWord = false,
            onFullWordChange = {},
            decreaseIndent = {},
            pasteFromClipboard = {},
            insertCurrentDate = {},
            applyTagWithVal = { _, _ -> }
        )
    }
}

@Preview
@Composable
fun SearchInlinePreview() {
    InlineSearchBar(
        searchQuery = "nota",
        onSearchQueryChange = {},
        matchCount = 3,
        currentMatchIndex = 0,
        onPrevious = {},
        onNext = {},
        onClose = {},
        caseSensitive = false,
        onCaseSensitiveChange = {},
        fullWord = false,
        onFullWordChange = {}
    )
}

@Preview
@Composable
fun SearchMorePreview() {
    MaterialTheme {
        SearchMoreOptions(
            caseSensitive = false,
            onCaseSensitiveChange = {},
            fullWord = false,
            onFullWordChange = {}
        )
    }
}