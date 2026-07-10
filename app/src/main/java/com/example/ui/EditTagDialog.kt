package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.model.Tag
import com.example.ui.viewmodel.NotesViewModel

@Composable
fun EditTagDialog(
    tag: Tag,
    viewModel: NotesViewModel,
    onDismiss: () -> Unit
) {
    var tagName by remember { mutableStateOf(tag.name) }
    val colors = listOf("#42A5F5", "#66BB6A", "#EC407A", "#AB47BC", "#FFA726", "#26A69A")
    var selectedColor by remember { mutableStateOf(tag.colorHex) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(id = R.string.delete_tag_confirm_title)) },
            text = { Text(stringResource(id = R.string.delete_tag_confirm_message, tag.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTag(tag)
                        Toast.makeText(context, context.getString(R.string.toast_tag_deleted), Toast.LENGTH_SHORT).show()
                        showDeleteConfirm = false
                        onDismiss()
                    }
                ) {
                    Text(stringResource(id = R.string.btn_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(id = R.string.btn_cancel))
                }
            }
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
                        text = stringResource(id = R.string.dialog_edit_tag),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(
                        onClick = { showDeleteConfirm = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(id = R.string.cd_delete_tag),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = tagName,
                    onValueChange = { tagName = it },
                    label = { Text(stringResource(id = R.string.tag_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_tag_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(id = R.string.label_select_color), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(color)))
                                .border(
                                    width = if (selectedColor.lowercase() == color.lowercase()) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = color }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(id = R.string.btn_cancel))
                    }
                    Button(
                        onClick = {
                            if (tagName.isNotBlank()) {
                                viewModel.updateTag(tag, tagName.trim(), selectedColor)
                                Toast.makeText(context, context.getString(R.string.toast_tag_updated), Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        },
                        modifier = Modifier.testTag("save_tag_btn")
                    ) {
                        Text(stringResource(id = R.string.btn_save))
                    }
                }
            }
        }
    }
}
