package com.example.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatClear
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

@Composable
fun FloatingEditorToolbar(
    modifier: Modifier = Modifier,
    activeTextStyles: Set<String>,
    activeFontColor: Color?,
    activeBgColor: Color?,
    isSpeaking: Boolean,
    aiEnabled: Boolean,
    showAiPanel: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleTag: (String) -> Unit,
    onOpenFontColor: () -> Unit,
    onOpenBgColor: () -> Unit,
    onClearFormatting: () -> Unit,
    onOpenMoreFormatting: () -> Unit,
    onOpenPalette: () -> Unit,
    onTtsToggle: () -> Unit,
    onOpenDrawing: () -> Unit,
    onOpenAttachments: () -> Unit,
    onOpenAi: () -> Unit,
    onToggleAiPanel: () -> Unit
) {
    OutlinedCard(
        modifier = modifier
            .padding(bottom = 16.dp)
            .widthIn(max = 440.dp)
            .fillMaxWidth(0.93f)
            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onUndo,
                    enabled = canUndo,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = stringResource(R.string.rich_undo), modifier = Modifier.size(16.dp))
                }
                IconButton(
                    onClick = onRedo,
                    enabled = canRedo,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = stringResource(R.string.rich_redo), modifier = Modifier.size(16.dp))
                }

                VerticalDivider(modifier = Modifier.height(20.dp))

                FilledTonalIconToggleButton(
                    checked = "b" in activeTextStyles,
                    onCheckedChange = { onToggleTag("b") },
                    modifier = Modifier.size(32.dp)
                ) { Text("B", fontWeight = FontWeight.Bold, fontSize = 12.sp) }

                FilledTonalIconToggleButton(
                    checked = "i" in activeTextStyles,
                    onCheckedChange = { onToggleTag("i") },
                    modifier = Modifier.size(32.dp)
                ) { Text("I", fontStyle = FontStyle.Italic, fontSize = 12.sp) }

                FilledTonalIconToggleButton(
                    checked = "u" in activeTextStyles,
                    onCheckedChange = { onToggleTag("u") },
                    modifier = Modifier.size(32.dp)
                ) { Text("U", style = TextStyle(textDecoration = TextDecoration.Underline), fontSize = 12.sp) }

                FilledTonalIconToggleButton(
                    checked = "s" in activeTextStyles,
                    onCheckedChange = { onToggleTag("s") },
                    modifier = Modifier.size(32.dp)
                ) { Text("S", style = TextStyle(textDecoration = TextDecoration.LineThrough), fontSize = 12.sp) }

                VerticalDivider(modifier = Modifier.height(20.dp))

                FilledTonalIconToggleButton(
                    checked = "h1" in activeTextStyles,
                    onCheckedChange = { onToggleTag("h1") },
                    modifier = Modifier.size(32.dp)
                ) { Text("H1", fontWeight = FontWeight.Bold, fontSize = 10.sp) }

                FilledTonalIconToggleButton(
                    checked = "h2" in activeTextStyles,
                    onCheckedChange = { onToggleTag("h2") },
                    modifier = Modifier.size(32.dp)
                ) { Text("H2", fontWeight = FontWeight.Bold, fontSize = 10.sp) }

                FilledTonalIconToggleButton(
                    checked = "h3" in activeTextStyles,
                    onCheckedChange = { onToggleTag("h3") },
                    modifier = Modifier.size(32.dp)
                ) { Text("H3", fontWeight = FontWeight.Bold, fontSize = 10.sp) }

                VerticalDivider(modifier = Modifier.height(20.dp))

                IconButton(
                    onClick = onOpenFontColor,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.FormatColorText,
                        contentDescription = stringResource(R.string.rich_font_color),
                        modifier = Modifier.size(16.dp),
                        tint = activeFontColor ?: MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onOpenBgColor,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.FormatColorFill,
                        contentDescription = stringResource(R.string.rich_bg_color),
                        modifier = Modifier.size(16.dp),
                        tint = activeBgColor ?: MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onClearFormatting,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.FormatClear, contentDescription = stringResource(R.string.rich_remove_format), modifier = Modifier.size(16.dp))
                }

                VerticalDivider(modifier = Modifier.height(20.dp))

                IconButton(
                    onClick = onOpenMoreFormatting,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = "More", modifier = Modifier.size(16.dp))
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onOpenPalette,
                    modifier = Modifier.testTag("palette_toolbar_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = stringResource(id = R.string.option_note_styling),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onTtsToggle,
                    modifier = Modifier.testTag("tts_toolbar_btn")
                ) {
                    Icon(
                        imageVector = if (isSpeaking) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = if (isSpeaking) stringResource(R.string.stop_speaking) else stringResource(R.string.read_aloud),
                        tint = if (isSpeaking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onOpenDrawing,
                    modifier = Modifier.testTag("drawing_toolbar_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Gesture,
                        contentDescription = stringResource(R.string.add_drawing),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onOpenAttachments,
                    modifier = Modifier.testTag("attachments_toolbar_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = stringResource(R.string.add_attachment),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                if (aiEnabled) {
                    IconButton(
                        onClick = onOpenAi,
                        modifier = Modifier.testTag("ai_toolbar_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = stringResource(R.string.ai_assistant),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (aiEnabled) {
                    IconButton(
                        onClick = onToggleAiPanel
                    ) {
                        Icon(
                            imageVector = if (showAiPanel) Icons.Default.Close else Icons.Default.RateReview,
                            contentDescription = "AI Panel",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
