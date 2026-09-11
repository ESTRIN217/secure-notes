package com.example.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Subscript
import androidx.compose.material.icons.filled.Superscript
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

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
    applyTagWithVal: (String, String) -> Unit,
    onOpenFontSizeSheet: () -> Unit,
    onConvertBlock: () -> Unit,
    onDeleteBlock: () -> Unit,
    onMoveBlockUp: () -> Unit,
    onMoveBlockDown: () -> Unit
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
                insertCurrentDate = insertCurrentDate,
                onConvertBlock = onConvertBlock,
                onDeleteBlock = onDeleteBlock,
                onMoveBlockUp = onMoveBlockUp,
                onMoveBlockDown = onMoveBlockDown
            )

            EditorToolbarMode.TEXT_FORMAT -> TextoToolbar(
                activeTextStyles = activeTextStyles,
                onToggleTag = onToggleTag,
                onBack = { currentMode = EditorToolbarMode.MAIN },
                onOpenbgFontColor = onOpenbgFontColor,
                onOpenInlineLink = onOpenInlineLink,
                onOpenEquation = onOpenEquation,
                applyTagWithVal = applyTagWithVal,
                onOpenFontSizeSheet = onOpenFontSizeSheet
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
    insertCurrentDate: () -> Unit,
    onConvertBlock: () -> Unit,
    onDeleteBlock: () -> Unit,
    onMoveBlockUp: () -> Unit,
    onMoveBlockDown: () -> Unit
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
                    contentDescription = stringResource(R.string.asistente_ia),
                    tint = MaterialTheme.colorScheme.primary,
                    onClick = onOpenAi
                )
                ToolbarDivider()
            }

            ToolbarIconButton(
                icon = Icons.Default.Add,
                contentDescription = stringResource(R.string.bloques),
                onClick = onOpenMoreFormatting
            )

            ToolbarIconButton(
                icon = Icons.Default.FormatShapes,
                contentDescription = stringResource(R.string.opciones_de_texto),
                onClick = onTexto
            )

            ToolbarIconButton(
                icon = Icons.AutoMirrored.Filled.Undo,
                contentDescription = stringResource(R.string.deshacer),
                enabled = canUndo,
                onClick = onUndo
            )

            ToolbarIconButton(
                icon = Icons.AutoMirrored.Filled.Redo,
                contentDescription = stringResource(R.string.rehacer),
                enabled = canRedo,
                onClick = onRedo
            )

            ToolbarIconButton(
                icon = Icons.Default.FormatClear,
                contentDescription = stringResource(R.string.limpiar_formato),
                onClick = onClearFormatting
            )
            ToolbarIconButton(
                icon = Icons.Default.SwapHoriz,
                contentDescription = stringResource(R.string.convertir_bloque),
                onClick = onConvertBlock
            )
            ToolbarIconButton(
                icon = Icons.Default.Delete,
                contentDescription = stringResource(R.string.eliminar_bloque),
                onClick = onDeleteBlock
            )

            ToolbarIconButton(
                icon = Icons.AutoMirrored.Filled.FormatIndentIncrease,
                contentDescription = stringResource(R.string.aumentar_sangria),
                onClick = { onToggleTag("indent")}
            )

            ToolbarIconButton(
                icon = Icons.AutoMirrored.Filled.FormatIndentDecrease,
                contentDescription = stringResource(R.string.disminuir_sangria),
                onClick = { decreaseIndent()}
            )
            ToolbarIconButton(
                icon = Icons.Default.ArrowDropUp,
                contentDescription = stringResource(R.string.subir_bloque),
                onClick = onMoveBlockUp
            )
            ToolbarIconButton(
                icon = Icons.Default.ArrowDropDown,
                contentDescription = stringResource(R.string.bajar_bloque),
                onClick = onMoveBlockDown
            )

            ToolbarDivider()

            ToolbarIconButton(
                icon = Icons.Default.Palette,
                contentDescription = stringResource(R.string.estilo_de_la_nota),
                tint = MaterialTheme.colorScheme.primary,
                onClick = onOpenPalette
            )

            ToolbarIconButton(
                icon = if (isSpeaking) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = if (isSpeaking) stringResource(R.string.detener_lectura) else stringResource(R.string.leer_en_voz_alta),
                tint = if (isSpeaking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                onClick = onTtsToggle
            )

            ToolbarIconButton(
                icon = Icons.Default.Gesture,
                contentDescription = stringResource(R.string.anadir_dibujo),
                tint = MaterialTheme.colorScheme.primary,
                onClick = onOpenDrawing
            )

            ToolbarIconButton(
                icon = Icons.Default.AttachFile,
                contentDescription = stringResource(R.string.adjuntar_archivo),
                tint = MaterialTheme.colorScheme.primary,
                onClick = onOpenAttachments
            )

            if (aiEnabled) {
                ToolbarIconButton(
                    icon = if (showAiPanel) Icons.Default.Close else Icons.Default.RateReview,
                    contentDescription = stringResource(R.string.panel_ia),
                    tint = MaterialTheme.colorScheme.primary,
                    onClick = onToggleAiPanel
                )
            }

            ToolbarDivider()

            ToolbarIconButton(
                icon = Icons.Default.Search,
                contentDescription = stringResource(R.string.buscar),
                onClick = onOpenSearch
            )

            ToolbarIconButton(
                icon = Icons.Default.ContentPaste,
                contentDescription = stringResource(R.string.pegar_con_formato),
                onClick = { pasteFromClipboard() }
            )

            ToolbarIconButton(
                icon = Icons.Default.Today,
                contentDescription = stringResource(R.string.insertar_fecha),
                onClick = { insertCurrentDate() }
            )
        }

        ToolbarDivider()

        ToolbarIconButton(
            icon = Icons.Default.Keyboard,
            contentDescription = stringResource(R.string.alternar_teclado),
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
    applyTagWithVal: (String, String) -> Unit,
    onOpenFontSizeSheet: () -> Unit
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
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.volver),
            tint = MaterialTheme.colorScheme.primary,
            onClick = onBack
        )

        ToolbarIconButton(
            icon = Icons.Default.FormatPaint,
            contentDescription = stringResource(R.string.color_de_texto_y_fondo),
            onClick = onOpenbgFontColor
        )

        FormattingToggleButton(
          checked = "b" in activeTextStyles,
          onCheckedChange = { onToggleTag("b") }
        ) {
          Icon(Icons.Default.FormatBold, contentDescription = stringResource(R.string.negrita))
        }
                FormattingToggleButton(
                    checked = "i" in activeTextStyles,
                    onCheckedChange = { onToggleTag("i") }
                ) {
                    Icon(Icons.Default.FormatItalic, contentDescription = stringResource(R.string.italica))
                }
                FormattingToggleButton(
                    checked = "u" in activeTextStyles,
                    onCheckedChange = { onToggleTag("u") }
                ) {
                    Icon(Icons.Default.FormatUnderlined, contentDescription = stringResource(R.string.subrayado))
                }
                FormattingToggleButton(
                    checked = "s" in activeTextStyles,
                    onCheckedChange = { onToggleTag("s") }
                ) {
                    Icon(Icons.Default.FormatStrikethrough, contentDescription = stringResource(R.string.tachado))
                }

        ToolbarIconButton(
            icon = Icons.Default.Link, 
            contentDescription = stringResource(R.string.enlace), 
            onClick = onOpenInlineLink
        )
        FormattingToggleButton(
            checked = "code" in activeTextStyles,
            onCheckedChange = { onToggleTag("code") }
        ) {
            Icon(
                imageVector = Icons.Default.Code,
                contentDescription = stringResource(R.string.codigo_inline),
                modifier = Modifier.size(18.dp)
            )
        }
        ToolbarIconButton(
            icon = Icons.Default.Functions, 
            contentDescription = stringResource(R.string.ecuaciones), 
            onClick = onOpenEquation
        )
        FilledTonalIconToggleButton(
            checked = "sub" in activeTextStyles,
            onCheckedChange = { onToggleTag("sub") },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Default.Subscript, contentDescription = "")
        }

        FilledTonalIconToggleButton(
            checked = "sup" in activeTextStyles,
            onCheckedChange = { onToggleTag("sup") },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Default.Superscript, contentDescription = "")
        }

        ToolbarDivider()

        OutlinedButton(
            onClick = onOpenFontSizeSheet,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Icon(Icons.Default.TextFields, contentDescription = "")
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
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
                placeholder = { Text(stringResource(R.string.label_search), fontSize = 14.sp) },
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
                text = stringResource(R.string.search_match_counter, if (matchCount == 0) 0 else currentMatchIndex + 1, matchCount),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ToolbarIconButton(
                icon = Icons.Default.ArrowUpward,
                contentDescription = stringResource(R.string.anterior),
                enabled = searchQuery.isNotEmpty() && matchCount > 0,
                onClick = onPrevious
            )

            ToolbarIconButton(
                icon = Icons.Default.ArrowDownward,
                contentDescription = stringResource(R.string.siguiente),
                enabled = searchQuery.isNotEmpty() && matchCount > 0,
                onClick = onNext
            )

            ToolbarIconButton(
                icon = if (showMoreOptions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = stringResource(R.string.mas_opciones),
                onClick = { showMoreOptions = !showMoreOptions }
            )

            ToolbarIconButton(
                icon = Icons.Default.Close,
                contentDescription = stringResource(R.string.cerrar_busqueda),
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
            Text(stringResource(R.string.coincidir_mayusculas), style = MaterialTheme.typography.bodySmall)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = fullWord, onCheckedChange = onFullWordChange)
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.palabra_completa), style = MaterialTheme.typography.bodySmall)
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
            applyTagWithVal = { _, _ -> },
            onOpenFontSizeSheet = {}
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
            applyTagWithVal = { _, _ -> },
            onOpenFontSizeSheet = {},
            onConvertBlock = {},
            onDeleteBlock = {},
            onMoveBlockUp = {},
            onMoveBlockDown = {}
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