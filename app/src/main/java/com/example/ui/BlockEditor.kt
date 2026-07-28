package com.example.ui

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.data.model.Attachment
import com.example.data.model.BlockType
import com.example.data.model.DataBlock
import com.example.data.model.NoteContentBlock

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
    pendingTagInsert: MutableState<String?>,
    pendingInsert: MutableState<String?> = remember { mutableStateOf(null) },
    onActiveCursorChange: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var dragState by remember { mutableStateOf<DragState?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            blocks.forEachIndexed { index, block ->
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
                ) {
                    if (dropAbove) {
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 40.dp),
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
                        isDragging = isDragging,
                        onActivate = { onActiveBlockChange(index) },
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
                            newBlocks[index] = block.copy(content = before)
                            newBlocks.add(index + 1, DataBlock(type = block.type, content = after))
                            onBlocksChange(newBlocks)
                            onActiveBlockChange(index + 1)
                        },
                        onCursorChange = onActiveCursorChange,
                        pendingTagInsert = pendingTagInsert,
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
                        onDragStart = {
                            dragState = DragState(
                                draggedIndex = index,
                                startOffset = 0f,
                                offset = 0f,
                                initialScrollOffset = scrollState.value
                            )
                        },
                        onDrag = { delta ->
                            val state = dragState
                            if (state != null) {
                                val newOffset = state.offset + delta
                                val moveDelta = (newOffset / 72f).toInt()
                                val tentativeIndex = (index + moveDelta).coerceIn(0, blocks.size - 1)
                                dragState = state.copy(offset = newOffset, tentativeIndex = tentativeIndex)
                            }
                        },
                        onDragEnd = {
                            val state = dragState
                            if (state != null && state.tentativeIndex != null && state.tentativeIndex != index) {
                                val newBlocks = blocks.toMutableList()
                                val item = newBlocks.removeAt(index)
                                newBlocks.add(state.tentativeIndex, item)
                                onBlocksChange(newBlocks)
                                onActiveBlockChange(state.tentativeIndex)
                            }
                            dragState = null
                        },
                        onDragCancel = { dragState = null }
                    )

                    if (dropBelow) {
                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 40.dp)
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
    isDragging: Boolean = false,
    onActivate: () -> Unit,
    onChange: (DataBlock) -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDuplicate: () -> Unit,
    onInsertAbove: () -> Unit,
    onInsertBelow: () -> Unit,
    onSplit: (String, String) -> Unit,
    onCursorChange: (Int) -> Unit,
    pendingTagInsert: MutableState<String?>,
    onMoveToPreviousBlock: () -> Unit = {},
    onMoveToNextBlock: () -> Unit = {},
    onDeleteBlock: () -> Unit = {},
    onDragStart: () -> Unit = {},
    onDrag: (Float) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {}
) {
    var showBlockMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        BlockHandle(
            isActive = isActive || isDragging,
            onClick = { showBlockMenu = true },
            onDragStart = onDragStart,
            onDrag = onDrag,
            onDragEnd = onDragEnd,
            onDragCancel = onDragCancel
        )

        when (block.type) {
            BlockType.TEXT, BlockType.HEADING1, BlockType.HEADING2, BlockType.HEADING3,
            BlockType.BULLET_LIST, BlockType.NUMBERED_LIST,
            BlockType.QUOTE, BlockType.CODE_BLOCK -> {
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
                    onCursorChange = onCursorChange
                )
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

@Composable
private fun BlockHandle(
    isActive: Boolean,
    onClick: () -> Unit,
    onDragStart: () -> Unit = {},
    onDrag: (Float) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {}
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .width(40.dp)
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.y)
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragCancel() }
                )
            }
    ) {
        Icon(
            Icons.Default.DragHandle,
            contentDescription = "Block menu",
            tint = if (isActive) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            modifier = Modifier.padding(2.dp)
        )
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
        BlockType.BULLET_LIST -> "Bulleted List"
        BlockType.NUMBERED_LIST -> "Numbered List"
        BlockType.CHECKLIST_ITEM -> "Checklist"
        BlockType.QUOTE -> "Quote"
        BlockType.CODE_BLOCK -> "Code Block"
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
    BlockType.TEXT, BlockType.HEADING1, BlockType.HEADING2, BlockType.HEADING3,
    BlockType.BULLET_LIST, BlockType.NUMBERED_LIST, BlockType.CHECKLIST_ITEM,
    BlockType.QUOTE, BlockType.CODE_BLOCK
)

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
