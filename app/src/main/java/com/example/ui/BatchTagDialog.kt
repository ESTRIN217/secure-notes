package com.example.ui

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DecryptedNote
import com.example.data.model.parseTags
import com.example.ui.viewmodel.NotesViewModel
import org.json.JSONArray

@Composable
fun BatchTagDialog(
    selectedNoteIds: Set<Int>,
    notes: List<DecryptedNote>,
    viewModel: NotesViewModel,
    onDismiss: () -> Unit,
    onTagsUpdated: () -> Unit
) {
    val availableTags by viewModel.availableTags.collectAsState()
    val initialSelectedTags: Set<String> = remember(selectedNoteIds, notes) {
        val selectedNotes = notes.filter { it.note.id in selectedNoteIds }
        val tags = mutableSetOf<String>()
        selectedNotes.forEach { decNote -> tags.addAll(decNote.note.parseTags()) }
        tags
    }
    var selectedTags by remember(initialSelectedTags) { mutableStateOf(initialSelectedTags) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.batch_tags_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                availableTags.forEach { tag ->
                    val isTagSelected = tag.name in selectedTags
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedTags = if (isTagSelected) {
                                    selectedTags - tag.name
                                } else {
                                    selectedTags + tag.name
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isTagSelected,
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(android.graphics.Color.parseColor(tag.colorHex)))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = tag.name,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.batchUpdateTags(selectedNoteIds, selectedTags.toList())
                    Toast.makeText(context, context.getString(R.string.toast_batch_tags_applied), Toast.LENGTH_SHORT).show()
                    onTagsUpdated()
                },
                modifier = Modifier.testTag("apply_batch_tags_btn")
            ) {
                Text(stringResource(id = R.string.btn_ok))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.btn_cancel))
            }
        },
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 6.dp
    )
}
