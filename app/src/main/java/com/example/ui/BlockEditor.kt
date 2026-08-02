package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.data.model.Attachment
import com.example.data.model.BlockType
import com.example.data.model.DataBlock
import com.example.data.model.NoteContentBlock
import com.example.data.model.TableData
import com.example.util.RichTextParser

@Composable
fun BlockEditor(
    blocks: List<DataBlock>,
    onBlocksChange: (List<DataBlock>) -> Unit,
    activeBlockIndex: Int,
    onActiveBlockChange: (Int) -> Unit,
    attachments: List<Attachment>,
    noteId: Int,
    onNavigateToMediaViewer: (String, String) -> Unit,
    onNavigateToDrawing: (Int, String?) -> Unit,
    onNavigateToNote: (Int) -> Unit = { _ -> },
    noteTitleById: (Int) -> String = { "" },
    pendingTagInsert: MutableState<String?>,
    pendingInsert: MutableState<String?> = remember { mutableStateOf(null) },
    onActiveCursorChange: (Int) -> Unit = {},
    onParseResult: ((RichTextParser.ParseResult) -> Unit)? = null,
    pendingFocusBlockIndex: Int = -1,
    onFocusHandled: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var dragState by remember { mutableStateOf<DragState?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
        ) {
            var numberedCounter = 0
            blocks.forEachIndexed { index, block ->
                numberedCounter = if (block.type == BlockType.NUMBERED_LIST) numberedCounter + 1 else 0
                val isDragging = dragState?.draggedIndex == index
                val dragOffset = if (isDragging) dragState?.offset ?: 0f else 0f
                val droppedIndex = dragState?.tentativeIndex
                val draggedIdx = dragState?.draggedIndex
                val dropAbove = draggedIdx != null && droppedIndex != null && droppedIndex != draggedIdx && droppedIndex == index
                val dropBelow = draggedIdx != null && droppedIndex != null && droppedIndex != draggedIdx && index - 1 == droppedIndex

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isDragging) Modifier
                                .zIndex(10f)
                                .graphicsLayer {
                                    translationY = dragOffset
                                    alpha = 0.9f
                                    shadowElevation = 8f
                                }
                            else Modifier
                        )
                        .then(
                            if (index != activeBlockIndex && !isDragging) Modifier.pointerInput(index) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
                                    val startTime = System.nanoTime()
                                    var longPressed = false
                                    var lastPosition = down.position

                                    while (true) {
                                        val event = awaitPointerEvent(PointerEventPass.Initial)
                                        val change = event.changes.firstOrNull { it.id == down.id } ?: break

                                        if (!change.pressed) break

                                        if (!longPressed) {
                                            val now = System.nanoTime()
                                            val elapsed = now - startTime
                                            if (elapsed >= viewConfiguration.longPressTimeoutMillis.toLong() * 1_000_000) {
                                                longPressed = true
                                                change.consume()
                                                lastPosition = change.position
                                                dragState = DragState(
                                                    draggedIndex = index,
                                                    startOffset = 0f,
                                                    offset = 0f,
                                                    initialScrollOffset = scrollState.value
                                                )
                                            } else {
                                                val distance = (change.position - down.position).getDistance()
                                                if (distance > viewConfiguration.touchSlop) break
                                            }
                                        }

                                        if (longPressed) {
                                            change.consume()
                                            val delta = change.position.y - lastPosition.y
                                            lastPosition = change.position
                                            val state = dragState
                                            if (state != null) {
                                                val newOffset = state.offset + delta
                                                val moveDelta = (newOffset / 72f).toInt()
                                                val tentativeIndex = (index + moveDelta).coerceIn(0, blocks.size - 1)
                                                dragState = state.copy(offset = newOffset, tentativeIndex = tentativeIndex)
                                            }
                                        }
                                    }

                                    if (longPressed) {
                                        val state = dragState
                                        if (state != null && state.tentativeIndex != null && state.tentativeIndex != index) {
                                            val newBlocks = blocks.toMutableList()
                                            val item = newBlocks.removeAt(index)
                                            newBlocks.add(state.tentativeIndex, item)
                                            onBlocksChange(newBlocks)
                                            onActiveBlockChange(state.tentativeIndex)
                                        }
                                        dragState = null
                                    }
                                }
                            } else Modifier
                        )
                ) {
                    if (dropAbove) {
                        HorizontalDivider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    val prevIndex = index - 1
                    val nextIndex = index + 1

                    BlockRow(
                        block = block,
                        index = index,
                        blockCount = blocks.size,
                        isActive = index == activeBlockIndex,
                        numberIndex = numberedCounter.takeIf { block.type == BlockType.NUMBERED_LIST },
                        onActivate = { onActiveBlockChange(index) },
                        onNavigateToNote = onNavigateToNote,
                        noteTitleById = noteTitleById,
                        onChange = { newBlock ->
                            val newBlocks = blocks.toMutableList()
                            newBlocks[index] = newBlock
                            onBlocksChange(newBlocks)
                        },
                        onDelete = {
                            val newBlocks = blocks.toMutableList()
                            newBlocks.removeAt(index)
                            onBlocksChange(newBlocks)
                            if (activeBlockIndex >= newBlocks.size && newBlocks.isNotEmpty()) {
                                onActiveBlockChange(newBlocks.size - 1)
                            }
                        },
                        onMoveUp = {
                            if (index > 0) {
                                val newBlocks = blocks.toMutableList()
                                val item = newBlocks.removeAt(index)
                                newBlocks.add(index - 1, item)
                                onBlocksChange(newBlocks)
                                onActiveBlockChange(index - 1)
                            }
                        },
                        onMoveDown = {
                            if (index < blocks.size - 1) {
                                val newBlocks = blocks.toMutableList()
                                val item = newBlocks.removeAt(index)
                                newBlocks.add(index + 1, item)
                                onBlocksChange(newBlocks)
                                onActiveBlockChange(index + 1)
                            }
                        },
                        onDuplicate = {
                            val newBlocks = blocks.toMutableList()
                            newBlocks.add(index + 1, block.copy(content = block.content))
                            onBlocksChange(newBlocks)
                            onActiveBlockChange(index + 1)
                        },
                        onInsertAbove = {
                            val newBlocks = blocks.toMutableList()
                            newBlocks.add(index, DataBlock(type = BlockType.TEXT))
                            onBlocksChange(newBlocks)
                            onActiveBlockChange(index)
                        },
                        onInsertBelow = {
                            val newBlocks = blocks.toMutableList()
                            newBlocks.add(index + 1, DataBlock(type = BlockType.TEXT))
                            onBlocksChange(newBlocks)
                            onActiveBlockChange(index + 1)
                        },
                        onSplit = { before, after ->
                            val newBlocks = blocks.toMutableList()
                            if (before.isEmpty() && after.isEmpty() && block.type in exitOnEmptyTypes) {
                                newBlocks[index] = DataBlock(type = BlockType.TEXT)
                                onBlocksChange(newBlocks)
                                onActiveBlockChange(index)
                            } else {
                                newBlocks[index] = block.copy(content = before)
                                val splitType = if (block.type.name.startsWith("HEADING")) BlockType.TEXT else block.type
                                newBlocks.add(index + 1, DataBlock(type = splitType, content = after))
                                onBlocksChange(newBlocks)
                                onActiveBlockChange(index + 1)
                            }
                        },
                        onCursorChange = onActiveCursorChange,
                        pendingTagInsert = pendingTagInsert,
                        requestFocus = index == pendingFocusBlockIndex,
                        onFocusRequested = onFocusHandled,
                        onSplitCollapsibleSummary = { before, after ->
                            val newBlocks = blocks.toMutableList()
                            newBlocks[index] = block.copy(meta = block.meta + ("summary" to before))
                            newBlocks.add(index + 1, DataBlock(type = BlockType.COLLAPSIBLE, content = "", meta = mapOf("summary" to after)))
                            onBlocksChange(newBlocks)
                            onActiveBlockChange(index + 1)
                        },
                        onMoveToPreviousBlock = {
                            if (prevIndex >= 0) onActiveBlockChange(prevIndex)
                        },
                        onMoveToNextBlock = {
                            if (nextIndex < blocks.size) onActiveBlockChange(nextIndex)
                        },
                        onDeleteBlock = {
                            val newBlocks = blocks.toMutableList()
                            if (newBlocks.size > 1) {
                                val focusIdx = (if (index > 0) index - 1 else 0).coerceAtMost(newBlocks.size - 2)
                                newBlocks.removeAt(index)
                                onBlocksChange(newBlocks)
                                onActiveBlockChange(focusIdx)
                            }
                        },
                        onParseResult = onParseResult
                    )

                    if (dropBelow) {
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter),
                            thickness = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            BlockAddButton(onAdd = {
                onBlocksChange(blocks + DataBlock(type = BlockType.TEXT))
                onActiveBlockChange(blocks.size)
            })
        }
    }
}

private data class DragState(
    val draggedIndex: Int,
    val startOffset: Float,
    val offset: Float,
    val initialScrollOffset: Int,
    val tentativeIndex: Int? = null
)

@Composable
private fun BlockRow(
    block: DataBlock,
    index: Int,
    blockCount: Int,
    isActive: Boolean,
    numberIndex: Int? = null,
    onActivate: () -> Unit,
    onNavigateToNote: (Int) -> Unit = { _ -> },
    noteTitleById: (Int) -> String = { "" },
    onChange: (DataBlock) -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDuplicate: () -> Unit,
    onInsertAbove: () -> Unit,
    onInsertBelow: () -> Unit,
    onSplit: (String, String) -> Unit,
    onCursorChange: (Int) -> Unit,
    onParseResult: ((RichTextParser.ParseResult) -> Unit)? = null,
    pendingTagInsert: MutableState<String?>,
    requestFocus: Boolean = false,
    onFocusRequested: () -> Unit = {},
    onSplitCollapsibleSummary: (String, String) -> Unit = { _, _ -> },
    onMoveToPreviousBlock: () -> Unit = {},
    onMoveToNextBlock: () -> Unit = {},
    onDeleteBlock: () -> Unit = {}
) {
    var showBlockMenu by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.Top
    ) {

        when (block.type) {
            BlockType.TEXT, BlockType.HEADING1, BlockType.HEADING2, BlockType.HEADING3, BlockType.HEADING4,
            BlockType.BULLET_LIST, BlockType.NUMBERED_LIST,
            BlockType.QUOTE, BlockType.CODE_BLOCK, BlockType.CALLOUT -> {
                EditableTextBlock(
                    rawText = block.content,
                    blockType = block.type,
                    onChange = { onChange(block.copy(content = it)) },
                    onFocusChange = { if (it) onActivate() },
                    onCursorChange = onCursorChange,
                    onSplit = onSplit,
                    onMoveToPreviousBlock = onMoveToPreviousBlock,
                    onMoveToNextBlock = onMoveToNextBlock,
                    onDeleteBlock = onDeleteBlock,
                    onConvertToText = { onChange(block.copy(type = BlockType.TEXT)) },
                    onParseResult = onParseResult,
                    modifier = Modifier.weight(1f),
                    numberIndex = numberIndex,
                    requestFocus = requestFocus,
                    onFocusRequested = onFocusRequested
                )
            }
            BlockType.TABLE -> {
                val tableData = TableData.fromJson(block.meta["table"])
                    ?: TableData.fromLegacyHtml(block.content)
                    ?: TableData.default3x3()
                EditableTableBlock(
                    tableData = tableData,
                    onChange = { newData ->
                        onChange(block.copy(content = "", meta = block.meta + ("table" to newData.toJson())))
                    },
                    onFocusChange = { if (it) onActivate() },
                    modifier = Modifier.weight(1f)
                )
            }
            BlockType.CHECKLIST_ITEM -> {
                val checked = block.meta["checked"] == "true"
                EditableChecklistBlock(
                    itemText = block.content,
                    isChecked = checked,
                    globalIndex = index,
                    onChange = { onChange(block.copy(content = it)) },
                    onToggle = {
                        onChange(block.copy(meta = mapOf("checked" to (!checked).toString())))
                    },
                    onFocusChange = { if (it) onActivate() },
                    onCursorChange = onCursorChange,
                    onSplit = onSplit,
                    onConvertToText = { onChange(block.copy(type = BlockType.TEXT)) },
                    requestFocus = requestFocus,
                    onFocusRequested = onFocusRequested
                )
            }
            BlockType.COLLAPSIBLE -> {
                EditableCollapsibleBlock(
                    summary = block.meta["summary"] ?: "",
                    content = block.content,
                    onChange = { newSummary, newContent ->
                        onChange(block.copy(content = newContent, meta = block.meta + ("summary" to newSummary)))
                    },
                    onSplitSummary = onSplitCollapsibleSummary,
                    onFocusChange = { if (it) onActivate() },
                    onCursorChange = onCursorChange,
                    onMoveToPreviousBlock = onMoveToPreviousBlock,
                    onMoveToNextBlock = onMoveToNextBlock,
                    onConvertToText = { onChange(block.copy(type = BlockType.TEXT)) },
                    modifier = Modifier.weight(1f),
                    requestFocus = requestFocus,
                    onFocusRequested = onFocusRequested
                )
            }
            BlockType.PAGE -> {
                val linkedNoteId = block.meta["noteId"]?.toIntOrNull()
                val resolvedTitle = linkedNoteId?.let { noteTitleById(it) }.orEmpty()
                val pageTitle = resolvedTitle.ifBlank { block.content.ifBlank { stringResource(R.string.block_page) } }
                var showIconSheet by remember { mutableStateOf(false) }
                OutlinedCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                                .clickable { showIconSheet = true },
                            contentAlignment = Alignment.Center
                        ) {
                            PageBlockIconContent(
                                iconType = block.meta["iconType"],
                                iconValue = block.meta["iconValue"].orEmpty(),
                                size = 18.dp,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = pageTitle,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { if (linkedNoteId != null) onNavigateToNote(linkedNoteId) }
                        )
                    }
                }
                if (showIconSheet) {
                    PageBlockOptionsSheet(
                        block = block,
                        onIconSelected = { type, value ->
                            onChange(block.copy(meta = block.meta + mapOf("iconType" to type, "iconValue" to value)))
                        },
                        onDelete = {
                            showIconSheet = false
                            onDelete()
                        },
                        onDismiss = { showIconSheet = false }
                    )
                }
            }
            else -> {
                val renderingBlock = block.toRenderingBlock()
                NoteContentBlockCard(
                    block = renderingBlock,
                    noteId = 0,
                    onDeleteBlock = { onDelete() },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (showBlockMenu) {
            BlockMenuPopup(
                block = block,
                index = index,
                isActive = isActive,
                canMoveUp = index > 0,
                canMoveDown = index < blockCount - 1,
                onDismiss = { showBlockMenu = false },
                onDelete = onDelete,
                onConvert = { newType ->
                    onChange(block.copy(type = newType))
                    showBlockMenu = false
                },
                onActivate = onActivate,
                onMoveUp = onMoveUp,
                onMoveDown = onMoveDown,
                onDuplicate = onDuplicate,
                onInsertAbove = onInsertAbove,
                onInsertBelow = onInsertBelow
            )
        }
    }
}

data class ExpandableMenuState(
    val expanded: Boolean = false,
    val showConvertSubmenu: Boolean = false
)

@Composable
private fun BlockMenuPopup(
    block: DataBlock,
    index: Int,
    isActive: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onConvert: (BlockType) -> Unit,
    onActivate: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDuplicate: () -> Unit,
    onInsertAbove: () -> Unit,
    onInsertBelow: () -> Unit
) {
    val convertExpanded = remember { mutableStateOf(false) }

    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss
    ) {
        if (!isActive) {
            DropdownMenuItem(
                text = { Text("Edit") },
                leadingIcon = { Icon(Icons.Default.Title, null, modifier = Modifier.size(18.dp)) },
                onClick = {
                    onActivate()
                    onDismiss()
                }
            )
        }
        DropdownMenuItem(
            text = { Text("Insert Above") },
            leadingIcon = { Icon(Icons.Default.KeyboardArrowUp, null, modifier = Modifier.size(18.dp)) },
            onClick = {
                onInsertAbove()
                onDismiss()
            }
        )
        DropdownMenuItem(
            text = { Text("Insert Below") },
            leadingIcon = { Icon(Icons.Default.KeyboardArrowDown, null, modifier = Modifier.size(18.dp)) },
            onClick = {
                onInsertBelow()
                onDismiss()
            }
        )
        if (canMoveUp) {
            DropdownMenuItem(
                text = { Text("Move Up") },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(18.dp)) },
                onClick = {
                    onMoveUp()
                    onDismiss()
                }
            )
        }
        if (canMoveDown) {
            DropdownMenuItem(
                text = { Text("Move Down") },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp)) },
                onClick = {
                    onMoveDown()
                    onDismiss()
                }
            )
        }
        DropdownMenuItem(
            text = { Text("Duplicate") },
            leadingIcon = { Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(18.dp)) },
            onClick = {
                onDuplicate()
                onDismiss()
            }
        )
        if (block.type in convertibleTypes) {
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Turn into") },
                onClick = { convertExpanded.value = !convertExpanded.value }
            )
            if (convertExpanded.value) {
                convertibleTypes.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.displayName) },
                        onClick = {
                            onConvert(type)
                            onDismiss()
                        },
                        enabled = type != block.type
                    )
                }
            }
        }
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
            leadingIcon = { Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error) },
            onClick = {
                onDelete()
                onDismiss()
            }
        )
    }
}

private val BlockType.displayName: String
    get() = when (this) {
        BlockType.TEXT -> "Text"
        BlockType.HEADING1 -> "Heading 1"
        BlockType.HEADING2 -> "Heading 2"
        BlockType.HEADING3 -> "Heading 3"
        BlockType.HEADING4 -> "Heading 4"
        BlockType.BULLET_LIST -> "Bulleted List"
        BlockType.NUMBERED_LIST -> "Numbered List"
        BlockType.CHECKLIST_ITEM -> "Checklist"
        BlockType.QUOTE -> "Quote"
        BlockType.CODE_BLOCK -> "Code Block"
        BlockType.CALLOUT -> "Highlight"
        BlockType.PAGE -> "Page"
        BlockType.HORIZONTAL_RULE -> "Divider"
        BlockType.IMAGE -> "Image"
        BlockType.VIDEO -> "Video"
        BlockType.AUDIO -> "Audio"
        BlockType.DRAWING -> "Drawing"
        BlockType.VOICE -> "Voice Note"
        BlockType.FILE -> "File"
        BlockType.TABLE -> "Table"
        BlockType.COLLAPSIBLE -> "Collapsible"
    }

@Composable
private fun BlockAddButton(onAdd: () -> Unit) {
    IconButton(
        onClick = onAdd,
        modifier = Modifier.padding(start = 4.dp)
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = "Add block",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

private val convertibleTypes = setOf(
    BlockType.TEXT, BlockType.HEADING1, BlockType.HEADING2, BlockType.HEADING3, BlockType.HEADING4,
    BlockType.BULLET_LIST, BlockType.NUMBERED_LIST, BlockType.CHECKLIST_ITEM,
    BlockType.QUOTE, BlockType.CODE_BLOCK
)

private val exitOnEmptyTypes = setOf(BlockType.BULLET_LIST, BlockType.NUMBERED_LIST, BlockType.QUOTE, BlockType.CALLOUT)

private fun DataBlock.toRenderingBlock(): NoteContentBlock {
    return when (type) {
        BlockType.IMAGE -> {
            val link = meta["linkUrl"]
            if (link != null) NoteContentBlock.ImageBlock(src = content, linkUrl = link)
            else NoteContentBlock.ImageBlock(src = content)
        }
        BlockType.VIDEO -> NoteContentBlock.VideoBlock(src = content)
        BlockType.AUDIO -> NoteContentBlock.AudioBlock(src = content)
        BlockType.DRAWING -> NoteContentBlock.DrawingBlock(jsonPath = content, previewPath = meta["previewPath"] ?: "")
        BlockType.VOICE -> NoteContentBlock.VoiceBlock(path = content)
        BlockType.FILE -> NoteContentBlock.FileBlock(name = meta["name"] ?: content.substringAfterLast('/'), path = content)
        BlockType.TABLE -> NoteContentBlock.TableBlock(
            headers = emptyList(),
            rows = listOf(listOf(content)),
            columnAlignment = emptyList(),
            cellAlignment = emptyList()
        )
        BlockType.HORIZONTAL_RULE -> NoteContentBlock.HorizontalRuleBlock
        BlockType.COLLAPSIBLE -> NoteContentBlock.CollapsibleBlock(summary = meta["summary"] ?: "", content = content)
        else -> {
            val parseResult = com.example.util.RichTextParser.parseWithMapping(content, hideTags = true)
            NoteContentBlock.TextBlock(parseResult = parseResult, rawStart = 0)
        }
    }
}
