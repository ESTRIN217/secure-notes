package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.WrapText
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.BlockType
import com.example.data.model.TextSegment
import com.example.util.CodeLanguageDetector
import com.example.util.RichTextConverter

/**
 * Standalone code editor. Opens text/code files (SAF), shows syntax
 * highlighting plus line numbers, and lets the user save back via SAF.
 */
@Composable
fun CodeEditorScreen(
    initialName: String?,
    initialContent: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var fileName by remember { mutableStateOf(initialName ?: "") }
    var content by remember { mutableStateOf(initialContent) }
    var language by remember { mutableStateOf(CodeLanguageDetector.detect(initialName, initialContent)) }
    var showLanguageSheet by remember { mutableStateOf(false) }
    var wrap by remember { mutableStateOf(true) }
    var showMenu by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    fun readUri(uri: Uri, name: String) {
        val resolvedName = context.contentResolver.query(
            uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
        )?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx)?.takeIf { it.isNotBlank() } else null
        } ?: name
        val text = try {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: ""
        } catch (e: Exception) {
            android.util.Log.e("CodeEditor", "readUri failed", e)
            Toast.makeText(context, context.getString(R.string.code_tools_load_failed), Toast.LENGTH_SHORT).show()
            return
        }
        fileName = resolvedName
        content = text
        language = CodeLanguageDetector.detect(resolvedName, text)
    }

    fun writeUri(uri: Uri) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(content.toByteArray())
            }
            Toast.makeText(context, context.getString(R.string.code_tools_saved), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            android.util.Log.e("CodeEditor", "writeUri failed", e)
            Toast.makeText(context, context.getString(R.string.code_tools_save_failed), Toast.LENGTH_SHORT).show()
        }
    }

    val openLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            readUri(uri, uri.lastPathSegment ?: "file")
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            writeUri(uri)
        }
    }

    val openMimeTypes = arrayOf(
        "text/*",
        "application/json",
        "application/xml",
        "application/x-httpd-php",
        "application/sql",
        "application/javascript",
        "application/typescript",
        "application/x-sh",
        "application/x-python-code"
    )

    val segments = remember(content) {
        listOf(TextSegment(text = content))
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
      topBar = {
        TopAppBar(
          title = {                     
            Text(
              text = fileName.ifBlank { stringResource(R.string.code_tools_new_document) },
              style = MaterialTheme.typography.titleMedium,
              maxLines = 1
            )},
          navigationIcon = {
            IconButton(onClick = onBack) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
          },
          actions = {
            IconButton(
              onClick = {
                showMenu = false
                if (fileName.isNotBlank() || content.isNotBlank()) {
                  saveLauncher.launch(fileName.ifBlank { "code.txt" })
                } else {
                  Toast.makeText(context, context.getString(R.string.code_tools_empty), Toast.LENGTH_SHORT).show()
                }
              }
            ) {
              Icon(Icons.Default.Save, contentDescription = stringResource(R.string.code_tools_save))
            }
            IconButton(onClick = { openLauncher.launch(openMimeTypes) }) {
              Icon(Icons.Default.Add, contentDescription = stringResource(R.string.code_tools_open_file))
            }
            IconButton(onClick = { showLanguageSheet = true }) {
              Icon(Icons.Default.Code, contentDescription = stringResource(R.string.block_code_language))
            }
            IconButton(onClick = { showMenu = true }) {
              Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.block_options))
            }
          }
        )
      }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxSize()
            ) {
                EditableTextBlock(
                    segments = segments,
                    blockType = BlockType.CODE_BLOCK,
                    showPrefix = false,
                    forcePlain = true,
                    highlightLanguage = language,
                    softWrap = wrap,
                    showLineNumbers = true,
                    onChange = { newSegs ->
                        content = RichTextConverter.segmentsToPlainText(newSegs)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (showLanguageSheet) {
        CodeLanguageSheet(
            current = language ?: "",
            onSelected = { code ->
                language = code.takeIf { it.isNotBlank() }
                showLanguageSheet = false
            },
            onDismiss = { showLanguageSheet = false }
        )
    }

    if (showMenu) {
        BlockOptionsSheet(
            title = stringResource(R.string.code_tools),
            onDismiss = { showMenu = false },
            actions = listOf(
                BlockSheetAction(
                    label = stringResource(R.string.block_code_copy),
                    icon = Icons.Default.ContentCopy,
                    onClick = {
                        showMenu = false
                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                        if (clipboard != null) {
                            clipboard.setPrimaryClip(ClipData.newPlainText("code", content))
                            Toast.makeText(context, context.getString(R.string.block_code_copied), Toast.LENGTH_SHORT).show()
                        }
                    }
                ),
                BlockSheetAction(
                    label = stringResource(R.string.block_code_adjust),
                    icon = Icons.AutoMirrored.Filled.WrapText,
                    toggle = wrap,
                    onClick = {
                        showMenu = false
                        wrap = !wrap
                    }
                ),
                BlockSheetAction(
                    label = stringResource(R.string.code_tools_share),
                    icon = Icons.Default.Share,
                    onClick = {
                        showMenu = false
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, content)
                        }
                        context.startActivity(Intent.createChooser(intent, null))
                    }
                )
            )
        )
    }
}