package com.estrin217.editordecodigo.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.estrin217.editordecodigo.R
import com.estrin217.editordecodigo.data.model.CodeFile
import com.estrin217.editordecodigo.syntax.SyntaxTheme
import com.estrin217.editordecodigo.ui.theme.JetBrainsMono
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
  * Pantalla de bienvenida estilo VS Code (Start / Welcome Page).
  * Incluye:
  * - Solo título ("Visual Studio Code" / "Editor de Código")
  * - Sección "Inicio" con botones: Nuevo archivo, Abrir archivo, Abrir carpeta
  * - Sección "Recientes" con lista de archivos modificados o creados recientemente
  */
@Composable
fun VSCodeWelcomeScreen(
    theme: SyntaxTheme,
    recentFiles: List<CodeFile>,
    onNewFile: () -> Unit,
    onOpenFile: () -> Unit,
    onOpenFolder: () -> Unit,
    onSelectRecentFile: (CodeFile) -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(theme.background)
            .testTag("vscode_welcome_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Título principal estilo VS Code
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = Color(0xFF0078D4).copy(alpha = 0.16f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF0078D4).copy(alpha = 0.4f)),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = null,
                                    tint = Color(0xFF0078D4),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            text = stringResource(R.string.welcome_app_title),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = theme.text,
                            letterSpacing = (-0.5).sp
                        )
                    }
                }
            }

            // Sección Inicio (Start) estilo VS Code
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.welcome_home),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = theme.text
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        VSCodeActionButton(
                            icon = Icons.AutoMirrored.Filled.NoteAdd,
                            title = stringResource(R.string.welcome_new_file),
                            theme = theme,
                            tag = "welcome_new_file_btn",
                            onClick = onNewFile
                        )
                        VSCodeActionButton(
                            icon = Icons.Default.FileOpen,
                            title = stringResource(R.string.welcome_open_file),
                            theme = theme,
                            tag = "welcome_open_file_btn",
                            onClick = onOpenFile
                        )
                        VSCodeActionButton(
                            icon = Icons.Default.FolderOpen,
                            title = stringResource(R.string.welcome_open_folder),
                            theme = theme,
                            tag = "welcome_open_folder_btn",
                            onClick = onOpenFolder
                        )
                    }
                }
            }

            // Sección Recientes (Recent) estilo VS Code
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = theme.lineNumber,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.welcome_recent),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = theme.text
                        )
                    }

                    if (recentFiles.isEmpty()) {
                        Surface(
                            color = theme.gutterBackground,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, theme.lineNumber.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.welcome_no_recent),
                                style = MaterialTheme.typography.bodySmall,
                                color = theme.lineNumber,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }

            // Lista de archivos recientes ordenados por fecha
            if (recentFiles.isNotEmpty()) {
                items(recentFiles.take(12), key = { it.id }) { file ->
                    VSCodeRecentFileItem(
                        file = file,
                        theme = theme,
                        formattedDate = dateFormat.format(Date(file.updatedAt)),
                        onClick = { onSelectRecentFile(file) }
                    )
                }
            }
        }
    }
}

@Composable
private fun VSCodeActionButton(
    icon: ImageVector,
    title: String,
    theme: SyntaxTheme,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = theme.gutterBackground,
        border = BorderStroke(1.dp, theme.lineNumber.copy(alpha = 0.18f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF0078D4),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = theme.text
            )
        }
    }
}

@Composable
private fun VSCodeRecentFileItem(
    file: CodeFile,
    theme: SyntaxTheme,
    formattedDate: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = theme.gutterBackground.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, theme.lineNumber.copy(alpha = 0.14f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recent_file_${file.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LanguageBadge(language = file.language)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = JetBrainsMono,
                    color = theme.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val loc = when {
                    file.isSample -> stringResource(R.string.storage_label_sample)
                    !file.uriString.isNullOrBlank() -> stringResource(R.string.storage_label_external)
                    !file.filePath.isNullOrBlank() -> stringResource(R.string.storage_label_internal)
                    else -> stringResource(R.string.storage_label_internal)
                }
                Text(
                    text = "${file.language.displayName} • $formattedDate • $loc",
                    style = MaterialTheme.typography.labelSmall,
                    color = theme.lineNumber,
                    fontSize = 11.sp
                )
            }
        }
    }
}
