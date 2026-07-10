package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.model.Tag
import com.example.ui.viewmodel.NotesViewModel

@Composable
fun ManageTagsDialog(
    viewModel: NotesViewModel,
    onDismiss: () -> Unit
) {
    val tags by viewModel.availableTags.collectAsState()
    var editingTag by remember { mutableStateOf<Tag?>(null) }

    if (editingTag != null) {
        EditTagDialog(
            tag = editingTag!!,
            viewModel = viewModel,
            onDismiss = { editingTag = null }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.dialog_manage_tags),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tags) { tag ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(tag.colorHex)))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = tag.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { editingTag = tag },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = stringResource(id = R.string.cd_edit_tag),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                var showLocalDeleteConfirm by remember { mutableStateOf(false) }
                                val context = LocalContext.current
                                if (showLocalDeleteConfirm) {
                                    AlertDialog(
                                        onDismissRequest = { showLocalDeleteConfirm = false },
                                        title = { Text(stringResource(id = R.string.delete_tag_confirm_title)) },
                                        text = { Text(stringResource(id = R.string.delete_tag_confirm_message, tag.name)) },
                                        confirmButton = {
                                            TextButton(
                                                onClick = {
                                                    viewModel.deleteTag(tag)
                                                    Toast.makeText(context, context.getString(R.string.toast_tag_deleted), Toast.LENGTH_SHORT).show()
                                                    showLocalDeleteConfirm = false
                                                }
                                            ) {
                                                Text(stringResource(id = R.string.btn_delete), color = MaterialTheme.colorScheme.error)
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { showLocalDeleteConfirm = false }) {
                                                Text(stringResource(id = R.string.btn_cancel))
                                            }
                                        }
                                    )
                                }
                                IconButton(
                                    onClick = { showLocalDeleteConfirm = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(id = R.string.cd_delete_tag),
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(stringResource(id = R.string.btn_ok))
                }
            }
        }
    }
}
