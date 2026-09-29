package com.estrin217.editordecodigo.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.estrin217.editordecodigo.R
import com.estrin217.editordecodigo.data.model.CodeFile
import com.estrin217.editordecodigo.syntax.SupportedLanguage
import com.estrin217.editordecodigo.syntax.SyntaxTheme
import com.estrin217.editordecodigo.ui.theme.JetBrainsMono

/**
 * VS Code-style horizontal editor tabs bar.
 * Displays open file tabs with language badges, dirty/modified indicators,
 * close buttons, and active tab indicator line.
 */
@Composable
fun EditorTabsBar(
    tabs: List<CodeFile>,
    activeFile: CodeFile?,
    dirtyFileIds: Set<Long>,
    theme: SyntaxTheme,
    isWelcomeTabOpen: Boolean = true,
    onSelectTab: (CodeFile) -> Unit,
    onCloseTab: (Long) -> Unit,
    onCloseOtherTabs: (Long) -> Unit,
    onCloseAllTabs: () -> Unit,
    onNewTab: () -> Unit,
    onSelectWelcomeTab: (() -> Unit)? = null,
    onCloseWelcomeTab: (() -> Unit)? = null,
    onOpenWelcomeTab: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Scroll to active tab whenever activeFile changes
    LaunchedEffect(activeFile?.id) {
        val activeIndex = tabs.indexOfFirst { it.id == activeFile?.id }
        if (activeIndex >= 0 && tabs.isNotEmpty()) {
            val approxTabWidthPx = 130 * 3
            scrollState.animateScrollTo(activeIndex * approxTabWidthPx / 2)
        }
    }

    Surface(
        color = theme.gutterBackground,
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Welcome / Home Tab (VS Code closable "Welcome" tab)
            if (isWelcomeTabOpen) {
                val isWelcomeSelected = activeFile == null
                VSCodeWelcomeTabItem(
                    isSelected = isWelcomeSelected,
                    theme = theme,
                    onSelect = { onSelectWelcomeTab?.invoke() ?: onOpenWelcomeTab?.invoke() },
                    onClose = { onCloseWelcomeTab?.invoke() }
                )
            }

            tabs.forEach { file ->
                val isSelected = activeFile?.id == file.id
                val isModified = dirtyFileIds.contains(file.id)

                VSCodeTabItem(
                    file = file,
                    isSelected = isSelected,
                    isModified = isModified,
                    theme = theme,
                    onSelect = { onSelectTab(file) },
                    onClose = { onCloseTab(file.id) },
                    onCloseOthers = { onCloseOtherTabs(file.id) },
                    onCloseAll = onCloseAllTabs
                )
            }

            // Quick add new tab button (+) like in VS Code
            IconButton(
                onClick = onNewTab,
                modifier = Modifier
                    .size(38.dp)
                    .testTag("tabs_add_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.cd_new_file),
                    tint = theme.lineNumber,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun VSCodeTabItem(
    file: CodeFile,
    isSelected: Boolean,
    isModified: Boolean,
    theme: SyntaxTheme,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    onCloseOthers: () -> Unit,
    onCloseAll: () -> Unit
) {
    var showContextMenu by remember { mutableStateOf(false) }

    val activeAccentColor = Color(0xFF0078D4) // VS Code blue accent line
    val tabBackground = if (isSelected) theme.background else theme.gutterBackground
    val tabTextColor = if (isSelected) theme.text else theme.lineNumber
    val dividerColor = theme.lineNumber.copy(alpha = 0.2f)

    Box(
        modifier = Modifier
            .height(38.dp)
            .widthIn(min = 100.dp, max = 220.dp)
            .background(tabBackground)
            .clickable(onClick = onSelect)
            .testTag("editor_tab_${file.id}")
    ) {
        // Top accent line for active tab
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(activeAccentColor)
                    .align(Alignment.TopCenter)
            )
        }

        // Right vertical divider separating tabs
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(dividerColor)
                .align(Alignment.CenterEnd)
        )

        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(start = 10.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Language badge / icon
            LanguageBadge(language = file.language)

            // File Name
            Text(
                text = file.name,
                color = tabTextColor,
                fontSize = 12.sp,
                fontFamily = JetBrainsMono,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )

            // Dirty indicator dot (VS Code style ●) or close button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(22.dp)
            ) {
                if (isModified) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(
                                color = if (isSelected) theme.text else activeAccentColor,
                                shape = CircleShape
                            )
                    )
                }

                // Close button overlay
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(20.dp)
                        .testTag("tab_close_${file.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.cd_close_tab),
                        tint = if (isModified) Color.Transparent else if (isSelected) theme.text else theme.lineNumber,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        // Context dropdown for tab
        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_tab_close)) },
                onClick = {
                    showContextMenu = false
                    onClose()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_tab_close_others)) },
                onClick = {
                    showContextMenu = false
                    onCloseOthers()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_tab_close_all)) },
                onClick = {
                    showContextMenu = false
                    onCloseAll()
                }
            )
        }
    }
}

/**
 * Compact colored language pill / indicator representing file type like in VS Code
 */
@Composable
fun LanguageBadge(language: SupportedLanguage) {
    val (badgeText, badgeColor) = when (language) {
        SupportedLanguage.KOTLIN -> "KT" to Color(0xFFA97BFF)
        SupportedLanguage.JAVA -> "JV" to Color(0xFFF89820)
        SupportedLanguage.JAVASCRIPT -> "JS" to Color(0xFFF7DF1E)
        SupportedLanguage.TYPESCRIPT -> "TS" to Color(0xFF3178C6)
        SupportedLanguage.PYTHON -> "PY" to Color(0xFF3776AB)
        SupportedLanguage.BASH -> "SH" to Color(0xFF4EAA25)
        SupportedLanguage.HTML -> "<>" to Color(0xFFE34F26)
        SupportedLanguage.CSS -> "#" to Color(0xFF264DE4)
        SupportedLanguage.JSON -> "{}" to Color(0xFFFBC02D)
        SupportedLanguage.XML -> "XML" to Color(0xFF009688)
        SupportedLanguage.SQL -> "SQL" to Color(0xFF00B0FF)
        SupportedLanguage.C -> "C" to Color(0xFF555555)
        SupportedLanguage.CPP -> "C++" to Color(0xFF00599C)
        SupportedLanguage.CSHARP -> "C#" to Color(0xFF68217A)
        SupportedLanguage.GO -> "GO" to Color(0xFF00ADD8)
        SupportedLanguage.RUST -> "RS" to Color(0xFFDEA584)
        SupportedLanguage.SWIFT -> "SW" to Color(0xFFFA7343)
        SupportedLanguage.PHP -> "PHP" to Color(0xFF777BB4)
        SupportedLanguage.RUBY -> "RB" to Color(0xFFCC342D)
        SupportedLanguage.DART -> "DT" to Color(0xFF0175C2)
        SupportedLanguage.LUA -> "LUA" to Color(0xFF000080)
        SupportedLanguage.YAML -> "YML" to Color(0xFFCB171E)
        SupportedLanguage.ENV -> "ENV" to Color(0xFF4CAF50)
        SupportedLanguage.INI_TOML -> "CFG" to Color(0xFF9E9E9E)
        SupportedLanguage.MARKDOWN -> "MD" to Color(0xFF083FA1)
        SupportedLanguage.PLAIN_TEXT -> "TXT" to Color(0xFF757575)
    }

    Surface(
        color = badgeColor.copy(alpha = 0.22f),
        shape = RoundedCornerShape(3.dp),
        border = BorderStroke(0.5.dp, badgeColor.copy(alpha = 0.6f)),
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            text = badgeText,
            color = badgeColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = JetBrainsMono,
            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
        )
    }
}

@Composable
private fun VSCodeWelcomeTabItem(
    isSelected: Boolean,
    theme: SyntaxTheme,
    onSelect: () -> Unit,
    onClose: () -> Unit
) {
    val activeAccentColor = Color(0xFF0078D4)
    val tabBackground = if (isSelected) theme.background else theme.gutterBackground
    val tabTextColor = if (isSelected) theme.text else theme.lineNumber
    val dividerColor = theme.lineNumber.copy(alpha = 0.2f)

    Box(
        modifier = Modifier
            .height(38.dp)
            .widthIn(min = 100.dp, max = 160.dp)
            .background(tabBackground)
            .clickable(onClick = onSelect)
            .testTag("welcome_tab_item")
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(activeAccentColor)
                    .align(Alignment.TopCenter)
            )
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(dividerColor)
                .align(Alignment.CenterEnd)
        )

        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(start = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = null,
                tint = if (isSelected) activeAccentColor else theme.lineNumber,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = stringResource(R.string.tabs_welcome),
                color = tabTextColor,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(22.dp)
                    .testTag("welcome_tab_close_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.cd_close_welcome_tab),
                    tint = if (isSelected) theme.text else theme.lineNumber,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}
