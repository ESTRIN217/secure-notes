package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.WrapText
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.BlockType
import com.example.data.model.DataBlock
import com.example.data.model.TextSegment
import com.example.util.RichTextConverter
import com.example.util.RichTextParser

@Composable
fun WysiwygCodeBlock(
    block: DataBlock,
    isActive: Boolean,
    onChange: (DataBlock) -> Unit,
    onActivate: () -> Unit,
    onTapToEdit: (Int) -> Unit,
    onCursorChange: (Int) -> Unit,
    onSelectionChange: (IntRange) -> Unit,
    onParseResult: ((RichTextParser.ParseResult) -> Unit)?,
    onMoveToPreviousBlock: () -> Unit,
    onMoveToNextBlock: () -> Unit,
    onDeleteBlock: () -> Unit,
    onConvertToText: () -> Unit,
    onConvertTo: () -> Unit,
    onInsertBelow: () -> Unit,
    onDuplicate: () -> Unit,
    onMoveTo: () -> Unit,
    onDelete: () -> Unit,
    onUrlClicked: (String, Int) -> Unit,
    requestFocus: Boolean,
    initialSelection: Int?,
    onFocusRequested: () -> Unit,
    pendingInsert: MutableState<String?>,
    pendingSelection: MutableState<IntRange?>,
    modifier: Modifier = Modifier
) {
    val segments = block.segments() ?: listOf(TextSegment(text = block.content))
    val language = block.meta["language"] ?: ""
    val highlightLanguage = language.takeIf { it.isNotBlank() }
    val showCaption = block.meta["showCaption"] == "true"
    val wrap = block.meta["wrap"] != "false"
    val showLineNumbers = block.meta["lineNumbers"] != "false"
    var showLanguageSheet by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { showLanguageSheet = true }) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = CodeLanguages.labelFor(language),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = stringResource(R.string.block_code_language)
                    )
                }
                IconButton(onClick = { showMoreMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.block_options)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            if (isActive) {
                EditableTextBlock(
                    segments = segments,
                    blockType = BlockType.CODE_BLOCK,
                    showPrefix = false,
                    forcePlain = true,
                    highlightLanguage = highlightLanguage,
                    softWrap = wrap,
                    showLineNumbers = showLineNumbers && highlightLanguage != null,
                    onChange = { newSegs ->
                        onChange(
                            block.copy(
                                content = "",
                                richTextJson = TextSegment.serialize(
                                    listOf(TextSegment(text = RichTextConverter.segmentsToPlainText(newSegs)))
                                )
                            )
                        )
                    },
                    onFocusChange = { if (it) onActivate() },
                    onCursorChange = onCursorChange,
                    onSelectionChange = onSelectionChange,
                    onMoveToPreviousBlock = onMoveToPreviousBlock,
                    onMoveToNextBlock = onMoveToNextBlock,
                    onDeleteBlock = onDeleteBlock,
                    onConvertToText = onConvertToText,
                    onParseResult = onParseResult,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    requestFocus = requestFocus,
                    onFocusRequested = onFocusRequested,
                    pendingInsert = pendingInsert,
                    initialSelection = initialSelection,
                    pendingSelection = pendingSelection
                )
            } else {
                ReadOnlyTextBlock(
                    segments = segments,
                    blockType = BlockType.CODE_BLOCK,
                    showPrefix = false,
                    highlightLanguage = highlightLanguage,
                    softWrap = wrap,
                    showLineNumbers = showLineNumbers && highlightLanguage != null,
                    onActivate = onTapToEdit,
                    onUrlClicked = onUrlClicked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            if (showCaption) {
                EditableCodeCaption(
                    caption = block.meta["caption"] ?: "",
                    isActive = isActive,
                    onActivate = onActivate,
                    onCaptionChange = { newCaption ->
                        onChange(block.copy(meta = block.meta + ("caption" to newCaption)))
                    },
                    onDelete = onDelete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 4.dp)
                )
            }
        }
    }

    if (showLanguageSheet) {
        CodeLanguageSheet(
            current = language,
            onSelected = { code ->
                onChange(block.copy(meta = block.meta + ("language" to code)))
                showLanguageSheet = false
            },
            onDismiss = { showLanguageSheet = false }
        )
    }

    if (showMoreMenu) {
        BlockOptionsSheet(
            title = stringResource(R.string.block_code),
            onDismiss = { showMoreMenu = false },
            actions = listOf(
                BlockSheetAction(
                    label = stringResource(R.string.block_convert_title),
                    icon = Icons.Default.SwapHoriz,
                    onClick = {
                        showMoreMenu = false
                        onConvertTo()
                    }
                ),
                BlockSheetAction(
                    label = stringResource(R.string.block_insert_below),
                    icon = Icons.Default.ArrowDownward,
                    onClick = {
                        showMoreMenu = false
                        onInsertBelow()
                    }
                ),
                BlockSheetAction(
                    label = stringResource(R.string.block_image_description),
                    icon = Icons.Default.Description,
                    toggle = showCaption,
                    onClick = {
                        showMoreMenu = false
                        onChange(block.copy(meta = block.meta + ("showCaption" to (!showCaption).toString())))
                    }
                ),
                BlockSheetAction(
                    label = stringResource(R.string.block_code_copy),
                    icon = Icons.Default.ContentCopy,
                    onClick = {
                        showMoreMenu = false
                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                        if (clipboard != null) {
                            clipboard.setPrimaryClip(
                                ClipData.newPlainText("code", RichTextConverter.segmentsToPlainText(segments))
                            )
                            Toast.makeText(context, context.getString(R.string.block_code_copied), Toast.LENGTH_SHORT).show()
                        }
                    }
                ),
                BlockSheetAction(
                    label = stringResource(R.string.block_code_adjust),
                    icon = Icons.AutoMirrored.Filled.WrapText,
                    toggle = wrap,
                    onClick = {
                        showMoreMenu = false
                        onChange(block.copy(meta = block.meta + ("wrap" to (!wrap).toString())))
                    }
                ),
                BlockSheetAction(
                    label = stringResource(R.string.block_code_line_numbers),
                    icon = Icons.AutoMirrored.Filled.List,
                    toggle = showLineNumbers,
                    onClick = {
                        showMoreMenu = false
                        onChange(block.copy(meta = block.meta + ("lineNumbers" to (!showLineNumbers).toString())))
                    }
                ),
                BlockSheetAction(
                    label = stringResource(R.string.block_duplicate),
                    icon = Icons.Default.ContentCopy,
                    onClick = {
                        showMoreMenu = false
                        onDuplicate()
                    }
                ),
                BlockSheetAction(
                    label = stringResource(R.string.block_move_to),
                    icon = Icons.AutoMirrored.Filled.DriveFileMove,
                    onClick = {
                        showMoreMenu = false
                        onMoveTo()
                    }
                ),
                BlockSheetAction(
                    label = stringResource(R.string.btn_delete),
                    icon = Icons.Default.Delete,
                    danger = true,
                    onClick = {
                        showMoreMenu = false
                        onDelete()
                    }
                )
            )
        )
    }
}

@Composable
private fun EditableCodeCaption(
    caption: String,
    isActive: Boolean,
    onActivate: () -> Unit,
    onCaptionChange: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var fieldValue by remember {
        mutableStateOf(TextFieldValue(text = caption, selection = TextRange(caption.length)))
    }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(isActive) {
        if (!isActive) isFocused = false
    }

    if (!isFocused && caption != fieldValue.text) {
        fieldValue = TextFieldValue(text = caption, selection = TextRange(caption.length))
    }

    val hintColor = if (isActive) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0f)
    }

    Box(modifier = modifier) {
        BasicTextField(
            value = fieldValue,
            onValueChange = { newValue ->
                fieldValue = newValue
                onCaptionChange(newValue.text)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                    if (focusState.isFocused) onActivate()
                }
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyUp && event.key == Key.Backspace) {
                        if (fieldValue.text.isEmpty()) {
                            onDelete()
                            true
                        } else false
                    } else false
                },
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = if (isActive) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            ),
            singleLine = false
        )
        if (fieldValue.text.isEmpty()) {
            Text(
                text = stringResource(R.string.block_image_caption_hint),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = hintColor,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
        }
    }
}
