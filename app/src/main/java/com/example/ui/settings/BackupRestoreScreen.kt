package com.example.ui.settings

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Note
import com.example.data.model.Tag
import com.example.ui.viewmodel.NotesViewModel
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BackupRestoreScreen(
    viewModel: NotesViewModel,
    onBack: () -> Unit
) {
    val isDriveLinked by viewModel.isDriveLinked.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()
    val syncStatusMessage by viewModel.syncStatusMessage.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(syncStatusMessage) {
        syncStatusMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearStatusMessage()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                val notes = viewModel.rawNotes.value
                val tags = viewModel.availableTags.value
                val backupJson = buildBackupJson(notes, tags)
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(backupJson.toByteArray())
                }
                Toast.makeText(context, context.getString(R.string.toast_export_backup_success), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.toast_export_backup_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
                restoreFromBackup(json, viewModel)
                Toast.makeText(context, context.getString(R.string.toast_import_backup_success), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.toast_import_backup_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .statusBarsPadding()
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                    Text(
                        text = stringResource(R.string.backup_restore_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Local Backup Section
            Text(
                text = stringResource(R.string.backup_local_section),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp)
            )

            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    ActionButton(
                        icon = Icons.Default.Upload,
                        title = stringResource(R.string.backup_export),
                        desc = stringResource(R.string.backup_export_desc),
                        color = Color(0xFF43A047),
                        onClick = { exportLauncher.launch("secure_notes_backup.json") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ActionButton(
                        icon = Icons.Default.Download,
                        title = stringResource(R.string.backup_import),
                        desc = stringResource(R.string.backup_import_desc),
                        color = MaterialTheme.colorScheme.primary,
                        onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }
                    )
                }
            }

            // Cloud Backup Section
            Text(
                text = stringResource(R.string.backup_cloud_section),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )

            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = stringResource(R.string.cloud_icon),
                            tint = if (isDriveLinked) Color(0xFF42A5F5) else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.title_cloud_sync),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = if (isDriveLinked) stringResource(R.string.drive_linked) else stringResource(R.string.drive_unlinked),
                                color = if (isDriveLinked) Color(0xFF42A5F5) else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.last_synced, lastSyncTime),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isDriveLinked) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.forceSyncCloud() },
                                modifier = Modifier.weight(1f).height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.backup_upload), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { viewModel.restoreSyncCloud() },
                                modifier = Modifier.weight(1f).height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.backup_download), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                viewModel.unlinkGoogleDrive()
                            },
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.disconnect), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.linkGoogleDrive("ya29.simulated_access_token") },
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Icon(Icons.Default.CloudQueue, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.btn_link_drive), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String,
    color: Color,
    onClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = desc,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.NavigateNext,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun buildBackupJson(notes: List<com.example.data.model.Note>, tags: List<Tag>): String {
    val notesArray = JSONArray()
    notes.forEach { note ->
        val noteObj = JSONObject().apply {
            put("id", note.id)
            put("title", note.title)
            put("content", note.content)
            put("isEncrypted", note.isEncrypted)
            put("salt", note.salt)
            put("iv", note.iv)
            put("lastModified", note.lastModified)
            put("tagsJson", note.tagsJson)
            put("isArchived", note.isArchived)
            put("isFavorite", note.isFavorite)
            put("isPinned", note.isPinned)
            put("isDeleted", note.isDeleted)
            put("backgroundColor", note.backgroundColor)
            put("backgroundImagePath", note.backgroundImagePath ?: "")
            put("categoryId", note.categoryId)
        }
        notesArray.put(noteObj)
    }

    val tagsArray = JSONArray()
    tags.forEach { tag ->
        val tagObj = JSONObject().apply {
            put("name", tag.name)
            put("colorHex", tag.colorHex)
        }
        tagsArray.put(tagObj)
    }

    return JSONObject().apply {
        put("version", 3)
        put("notes", notesArray)
        put("tags", tagsArray)
        put("timestamp", System.currentTimeMillis())
    }.toString(2)
}

private fun restoreFromBackup(json: String, viewModel: NotesViewModel) {
    val container = JSONObject(json)
    val notesArr = container.getJSONArray("notes")
    val tagsArr = container.getJSONArray("tags")

    for (i in 0 until notesArr.length()) {
        val noteObj = notesArr.getJSONObject(i)

        val tagsJson = noteObj.optString("tagsJson", "[]")
        val tagsList = try {
            val arr = JSONArray(tagsJson)
            List(arr.length()) { arr.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }

        viewModel.saveNote(
            id = 0,
            title = noteObj.getString("title"),
            content = noteObj.getString("content"),
            isEncrypted = noteObj.getBoolean("isEncrypted"),
            tagsList = tagsList,
            backgroundColor = if (noteObj.has("backgroundColor") && !noteObj.isNull("backgroundColor"))
                noteObj.optInt("backgroundColor") else null,
            backgroundImagePath = noteObj.optString("backgroundImagePath", "").ifEmpty { null },
            isPinned = noteObj.optBoolean("isPinned", false),
            isFavorite = noteObj.optBoolean("isFavorite", false),
            isArchived = noteObj.optBoolean("isArchived", false)
        )
    }

    for (i in 0 until tagsArr.length()) {
        val tagObj = tagsArr.getJSONObject(i)
        viewModel.createTag(tagObj.getString("name"), tagObj.getString("colorHex"))
    }
}
