package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.model.DecryptedNote
import com.example.data.model.Note
import com.example.ui.settings.SettingsCardGroup
import com.example.ui.settings.SettingsListTile
import com.example.ui.settings.SettingsSwitchTile
import com.example.ui.viewmodel.NotesViewModel
import com.example.util.exportMultipleToHtmlAsync
import com.example.util.exportMultipleToTxt
import com.example.util.exportToMarkdown
import com.example.util.exportToPdf
import com.example.util.exportSingleNoteToJson
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MoreOptionsDialog(
    noteId: Int,
    originalNote: Note?,
    title: String,
    content: String,
    isEncrypted: Boolean,
    isPinned: Boolean,
    isFavorite: Boolean,
    isArchived: Boolean,
    isPasswordSet: Boolean,
    onEncryptionChange: (Boolean) -> Unit,
    onPinChange: (Boolean) -> Unit,
    onFavoriteChange: (Boolean) -> Unit,
    onArchiveChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
  onOpenPalette: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.more_options),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                val lastModifiedTime = originalNote?.lastModified ?: System.currentTimeMillis()
                val formattedDate = SimpleDateFormat("LLL dd, yyyy HH:mm", Locale.getDefault()).format(Date(lastModifiedTime))

                SuggestionChip(
                    onClick = {},
                    label = {
                        Text(
                            stringResource(id = R.string.label_last_modified, formattedDate),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    icon = {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                SettingsCardGroup {
                    SettingsSwitchTile(
                        icon = Icons.Default.Shield,
                        title = stringResource(id = R.string.label_e2e_encryption),
                        subtitle = stringResource(
                            id = if (isPasswordSet) R.string.desc_e2e_encryption_enabled
                            else R.string.desc_e2e_encryption_disabled
                        ),
                        checked = isEncrypted,
                        onCheckedChange = {
                            if (!isPasswordSet) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.toast_setup_password_first),
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                onEncryptionChange(it)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                SettingsCardGroup {
                    val toggleShapes = IconButtonDefaults.toggleableShapes(
                        shape = IconButtonDefaults.mediumRoundShape,
                        pressedShape = IconButtonDefaults.mediumPressedShape,
                        checkedShape = IconButtonDefaults.mediumSelectedRoundShape
                    )
                    val toggleSize = IconButtonDefaults.mediumContainerSize()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalIconToggleButton(
                            checked = isPinned,
                            onCheckedChange = onPinChange,
                            shapes = toggleShapes,
                            modifier = Modifier.size(toggleSize)
                        ) {
                            Icon(
                                imageVector = if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                contentDescription = stringResource(
                                    if (isPinned) R.string.tooltip_unpin else R.string.tooltip_pin
                                ),
                                modifier = Modifier.size(IconButtonDefaults.mediumIconSize)
                            )
                        }
                        FilledTonalIconToggleButton(
                            checked = isFavorite,
                            onCheckedChange = onFavoriteChange,
                            shapes = toggleShapes,
                            modifier = Modifier.size(toggleSize)
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.Star,
                                contentDescription = stringResource(
                                    if (isFavorite) R.string.tooltip_unfavorite else R.string.tooltip_favorite
                                ),
                                modifier = Modifier.size(IconButtonDefaults.mediumIconSize)
                            )
                        }
                        FilledTonalIconToggleButton(
                            checked = isArchived,
                            onCheckedChange = onArchiveChange,
                            shapes = toggleShapes,
                            modifier = Modifier.size(toggleSize)
                        ) {
                            Icon(
                                imageVector = if (isArchived) Icons.Default.Archive else Icons.Outlined.Archive,
                                contentDescription = stringResource(
                                    if (isArchived) R.string.tooltip_unarchive else R.string.tooltip_archive
                                ),
                                modifier = Modifier.size(IconButtonDefaults.mediumIconSize)
                            )
                        }
                        FilledTonalIconButton(
                          onClick = onOpenPalette,
                          shape = IconButtonDefaults.mediumRoundShape,
                          modifier = Modifier.size(toggleSize)
                        ) {
                          Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Estilo de la nota"
                          )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(id = R.string.option_share),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, bottom = 8.dp)
                )

                val decryptedNoteForShare = DecryptedNote(
                    note = originalNote ?: Note(title = title, content = content),
                    title = title,
                    content = content,
                    isDecryptionSuccessful = true
                )

                val shareFormats = listOf(
                    Triple("TXT", Icons.Default.Description, R.string.share_format_txt),
                    Triple("MD", Icons.Default.Code, R.string.share_format_md),
                    Triple("PDF", Icons.Default.PictureAsPdf, R.string.share_format_pdf),
                    Triple("HTML", Icons.Default.Html, R.string.share_format_html),
                    Triple("JSON", Icons.Default.Code, R.string.share_format_json)
                )

                var exportingHtml by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()

                SettingsCardGroup {
                    shareFormats.forEachIndexed { index, (formatKey, icon, labelResId) ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                        if (formatKey == "HTML" && exportingHtml) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = stringResource(id = labelResId),
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        } else {
                            SettingsListTile(
                                leadingIcon = icon,
                                title = stringResource(id = labelResId),
                                modifier = Modifier.testTag("share_format_${formatKey.lowercase()}_btn"),
                                onClick = {
                                    val notesToShare = listOf(decryptedNoteForShare)
                                    when (formatKey) {
                                        "TXT" -> exportMultipleToTxt(context, notesToShare)
                                        "MD" -> exportToMarkdown(context, decryptedNoteForShare.note, title, content)
                                        "PDF" -> exportToPdf(context, decryptedNoteForShare.note, title, content)
                                        "HTML" -> {
                                            if (!exportingHtml) {
                                                exportingHtml = true
                                                scope.launch {
                                                    try {
                                                        exportMultipleToHtmlAsync(context, notesToShare)
                                                    } finally {
                                                        exportingHtml = false
                                                    }
                                                    onDismiss()
                                                }
                                            }
                                            return@SettingsListTile
                                        }
                                        "JSON" -> exportSingleNoteToJson(context, decryptedNoteForShare.note, title, content)
                                    }
                                    onDismiss()
                                }
                            )
                        }
                    }
                }

                if (noteId != 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delete_note_btn_more"),
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.error),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(id = R.string.option_delete))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun EditorMoreOptions(
    show: Boolean,
    viewModel: NotesViewModel,
    noteId: Int,
    originalNote: Note?,
    title: String,
    content: String,
    isEncrypted: Boolean,
    isPinned: Boolean,
    isFavorite: Boolean,
    isArchived: Boolean,
    isPasswordSet: Boolean,
    onEncryptionChange: (Boolean) -> Unit,
    onPinChange: (Boolean) -> Unit,
    onFavoriteChange: (Boolean) -> Unit,
    onArchiveChange: (Boolean) -> Unit,
    onDeleted: () -> Unit,
    onDismiss: () -> Unit,
    onOpenPalette: () -> Unit = {}
) {
    if (!show) return
    val context = LocalContext.current

    MoreOptionsDialog(
        noteId = noteId,
        originalNote = originalNote,
        title = title,
        content = content,
        isEncrypted = isEncrypted,
        isPinned = isPinned,
        isFavorite = isFavorite,
        isArchived = isArchived,
        isPasswordSet = isPasswordSet,
        onEncryptionChange = onEncryptionChange,
        onPinChange = onPinChange,
        onFavoriteChange = onFavoriteChange,
        onArchiveChange = onArchiveChange,
        onDelete = {
            originalNote?.let { note ->
                viewModel.moveToTrash(note)
                Toast.makeText(context, context.getString(R.string.toast_moved_trash), Toast.LENGTH_SHORT).show()
                onDeleted()
            }
        },
        onDismiss = onDismiss,
        onOpenPalette = onOpenPalette
    )
}

@Preview
@Composable
fun MoreOptionsDialogPreview() {
    
}