package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.ScrollState
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
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.data.model.Attachment
import com.example.data.model.BlockType
import com.example.data.model.DataBlock
import com.example.data.model.NoteContentBlock
import com.example.data.model.TableData
import com.example.data.model.TextSegment
import com.example.util.RichTextConverter
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
    onMoveBlockTo: (Int) -> Unit = {},
    onConvertTo: (Int) -> Unit = {},
    onEditPageLink: (Int) -> Unit = {},
    onEditImage: (Int) -> Unit = {},
    onUrlClicked: (String, Int) -> Unit = { _, _ -> },
    pendingTagInsert: MutableState<String?>,
    pendingInsert: MutableState<String?> = remember { mutableStateOf(null) },
    pendingSelection: MutableState<IntRange?> = remember { mutableStateOf(null) },
    onActiveCursorChange: (Int) -> Unit = {},
    onActiveSelectionChange: (IntRange) -> Unit = {},
    onParseResult: ((RichTextParser.ParseResult) -> Unit)? = null,
    pendingFocusBlockIndex: Int = -1,
    onFocusHandled: () -> Unit = {},
    pendingTypingStyle: TextSegment? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val dragState = remember { mutableStateOf<DragState?>(null) }
    var tapFocusIndex by remember { mutableIntStateOf(-1) }
    var tapFocusOffset by remember { mutableIntStateOf(0) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
        ) {
            var numberedCounter = 0
            blocks.forEachIndexed { index, block ->
                numberedCounter = if (block.type == BlockType.NUMBERED_LIST) numberedCounter + 1 else 0
                val isDragging = dragState.value?.draggedIndex == index
                val dragOffset = if (isDragging) dragState.value?.offset ?: 0f else 0f
                val droppedIndex = dragState.value?.tentativeIndex
                val draggedIdx = dragState.value?.draggedIndex
                val dropAbove = draggedIdx != null && droppedIndex != null && droppedIndex != draggedIdx && droppedIndex == index
                val dropBelow = draggedIdx != null && droppedIndex != null && droppedIndex != draggedIdx && index - 1 == droppedIndex
                val dragViaLongPress = block.type.dragViaLongPress(index != activeBlockIndex)

                fun commitBlockDrop(from: Int, tentativeIndex: Int) {
                    if (from == tentativeIndex) return
                    val newBlocks = blocks.toMutableList()
                    val item = newBlocks.removeAt(from)
                    newBlocks.add(tentativeIndex, item)
                    onBlocksChange(newBlocks)
                    onActiveBlockChange(tentativeIndex)
                }

                fun insertBelow(index: Int) {
                    val newBlocks = blocks.toMutableList()
                    newBlocks.add(index + 1, DataBlock(type = BlockType.TEXT))
                    onBlocksChange(newBlocks)
                    onActiveBlockChange(index + 1)
                }

                fun duplicateBlock(index: Int) {
                    val newBlocks = blocks.toMutableList()
                    newBlocks.add(index + 1, blocks[index].copy())
                    onBlocksChange(newBlocks)
                    onActiveBlockChange(index + 1)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(if (isDragging) 10f else 0f)
                        .graphicsLayer {
                            translationY = dragOffset
                            alpha = if (isDragging) 0.9f else 1f
                            shadowElevation = if (isDragging) 8f else 0f
                        }
                        .then(
                            if (dragViaLongPress) Modifier.pointerInput(index) {
                                awaitBlockDragGesture(
                                    index = index,
                                    blockCount = blocks.size,
                                    scrollState = scrollState,
                                    dragState = dragState,
                                    commitDrop = ::commitBlockDrop
                                )
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
                        numberIndex = numberedCounter.takeIf { block.type == BlockType.NUMBERED_LIST },
                        onActivate = { onActiveBlockChange(index) },
                        onNavigateToNote = onNavigateToNote,
                        onNavigateToMediaViewer = onNavigateToMediaViewer,
                        noteTitleById = noteTitleById,
                        onInsertBelow = ::insertBelow,
                        onDuplicate = ::duplicateBlock,
                        onMoveTo = onMoveBlockTo,
                        onConvertTo = onConvertTo,
                        onEditPageLink = onEditPageLink,
                        onEditImage = onEditImage,
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
                        
                        onSplit = { before, after ->
                            val newBlocks = blocks.toMutableList()
                            if (before.isEmpty() && after.isEmpty() && block.type in exitOnEmptyTypes) {
                                newBlocks[index] = DataBlock(type = BlockType.TEXT)
                                onBlocksChange(newBlocks)
                                onActiveBlockChange(index)
                            } else {
                                newBlocks[index] = block.copy(
                                    content = "",
                                    richTextJson = TextSegment.serialize(before)
                                )
                                val splitType = if (block.type.name.startsWith("HEADING")) BlockType.TEXT else block.type
                                newBlocks.add(
                                    index + 1,
                                    DataBlock(
                                        type = splitType,
                                        content = "",
                                        richTextJson = TextSegment.serialize(after)
                                    )
                                )
                                onBlocksChange(newBlocks)
                                onActiveBlockChange(index + 1)
                            }
                        },
                        onCursorChange = onActiveCursorChange,
                        onSelectionChange = onActiveSelectionChange,
                        pendingTagInsert = pendingTagInsert,
                        pendingInsert = pendingInsert,
                        pendingSelection = pendingSelection,
                        pendingTypingStyle = pendingTypingStyle,
                        onTapToEdit = { originalOffset ->
                            tapFocusIndex = index
                            tapFocusOffset = originalOffset
                            onActiveBlockChange(index)
                        },
                        onUrlClicked = onUrlClicked,
                        activeBlockIndex = activeBlockIndex,
                        requestFocus = index == pendingFocusBlockIndex || index == tapFocusIndex,
                        initialSelection = if (index == tapFocusIndex) tapFocusOffset else null,
                        onFocusRequested = {
                            tapFocusIndex = -1
                            onFocusHandled()
                        },
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
                        onParseResult = onParseResult,
                        onDragHandleStart = {
                            dragState.value = DragState(index, 0f, 0f, scrollState.value)
                        },
                        onDragHandleBy = { delta ->
                            val state = dragState.value
                            if (state != null) {
                                dragState.value = updateDragOffset(state, delta, index, blocks.size)
                            }
                        },
                        onDragHandleEnd = {
                            val state = dragState.value
                            if (state != null && state.tentativeIndex != null && state.tentativeIndex != index) {
                                commitBlockDrop(index, state.tentativeIndex)
                            }
                            dragState.value = null
                        },
                        onDragHandleCancel = { dragState.value = null }
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

private suspend fun PointerInputScope.awaitBlockDragGesture(
    index: Int,
    blockCount: Int,
    scrollState: ScrollState,
    dragState: MutableState<DragState?>,
    commitDrop: (Int, Int) -> Unit
) {
    awaitEachGesture {
        var started = false
        try {
            val down = awaitFirstDown(pass = PointerEventPass.Initial, requireUnconsumed = false)
            val startTime = System.nanoTime()
            var lastPosition = down.position

            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break

                if (!started) {
                    val elapsed = System.nanoTime() - startTime
                    if (elapsed >= viewConfiguration.longPressTimeoutMillis * 1_000_000) {
                        started = true
                        change.consume()
                        lastPosition = change.position
                        dragState.value = DragState(
                            draggedIndex = index,
                            startOffset = 0f,
                            offset = 0f,
                            initialScrollOffset = scrollState.value
                        )
                    } else {
                        val distance = (change.position - down.position).getDistance()
                        if (distance > viewConfiguration.touchSlop) break
                    }
                } else {
                    change.consume()
                    val delta = change.position.y - lastPosition.y
                    lastPosition = change.position
                    val state = dragState.value
                    if (state != null) {
                        dragState.value = updateDragOffset(state, delta, index, blockCount)
                    }
                }
            }
        } finally {
            val state = dragState.value
            if (state != null && state.tentativeIndex != null && state.tentativeIndex != index) {
                commitDrop(index, state.tentativeIndex)
            }
            dragState.value = null
        }
    }
}

private fun updateDragOffset(state: DragState, deltaY: Float, index: Int, blockCount: Int): DragState {
    val newOffset = state.offset + deltaY
    val moveDelta = (newOffset / 72f).toInt()
    val tentativeIndex = (index + moveDelta).coerceIn(0, blockCount - 1)
    return state.copy(offset = newOffset, tentativeIndex = tentativeIndex)
}

private fun BlockType.dragViaLongPress(isActive: Boolean): Boolean = when (this) {
    BlockType.TABLE -> false
    BlockType.TEXT, BlockType.HEADING1, BlockType.HEADING2, BlockType.HEADING3, BlockType.HEADING4,
    BlockType.BULLET_LIST, BlockType.NUMBERED_LIST, BlockType.CHECKLIST_ITEM, BlockType.COLLAPSIBLE,
    BlockType.QUOTE, BlockType.CODE_BLOCK -> !isActive
    else -> true
}

@Composable
private fun BlockRow(
    block: DataBlock,
    index: Int,
    numberIndex: Int? = null,
    onActivate: () -> Unit,
    onNavigateToNote: (Int) -> Unit = { _ -> },
    onNavigateToMediaViewer: (String, String) -> Unit = { _, _ -> },
    noteTitleById: (Int) -> String = { "" },
    onInsertBelow: (Int) -> Unit = {},
    onDuplicate: (Int) -> Unit = {},
    onMoveTo: (Int) -> Unit = {},
    onConvertTo: (Int) -> Unit = {},
    onEditPageLink: (Int) -> Unit = {},
    onEditImage: (Int) -> Unit = {},
    onChange: (DataBlock) -> Unit,
    onDelete: () -> Unit,
    onSplit: (List<TextSegment>, List<TextSegment>) -> Unit,
    onCursorChange: (Int) -> Unit,
    onSelectionChange: (IntRange) -> Unit = {},
    onParseResult: ((RichTextParser.ParseResult) -> Unit)? = null,
    pendingTagInsert: MutableState<String?>,
    pendingInsert: MutableState<String?> = remember { mutableStateOf(null) },
    pendingSelection: MutableState<IntRange?> = remember { mutableStateOf(null) },
    pendingTypingStyle: TextSegment? = null,
    onTapToEdit: (Int) -> Unit = { _ -> },
    onUrlClicked: (String, Int) -> Unit = { _, _ -> },
    activeBlockIndex: Int = -1,
    requestFocus: Boolean = false,
    initialSelection: Int? = null,
    onFocusRequested: () -> Unit = {},
    onSplitCollapsibleSummary: (String, String) -> Unit = { _, _ -> },
    onMoveToPreviousBlock: () -> Unit = {},
    onMoveToNextBlock: () -> Unit = {},
    onDeleteBlock: () -> Unit = {},
    onDragHandleStart: () -> Unit = {},
    onDragHandleBy: (Float) -> Unit = {},
    onDragHandleEnd: () -> Unit = {},
    onDragHandleCancel: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val (topMargin, bottomMargin) = block.type.blockVerticalMargins()
    val indentLevel = (block.meta["indentLevel"]?.toIntOrNull() ?: 0).coerceAtLeast(0)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = (indentLevel * 24).dp, top = topMargin, bottom = bottomMargin),
        verticalAlignment = Alignment.Top
    ) {

        when (block.type) {
            BlockType.TEXT, BlockType.HEADING1, BlockType.HEADING2, BlockType.HEADING3, BlockType.HEADING4,
            BlockType.BULLET_LIST, BlockType.NUMBERED_LIST,
            BlockType.QUOTE, BlockType.CODE_BLOCK, BlockType.CALLOUT -> {
                if (index == activeBlockIndex) {
                    val editorSegments = if (block.type == BlockType.CODE_BLOCK) {
                        block.segments() ?: listOf(TextSegment(text = block.content))
                    } else {
                        block.ensureSegments()
                    }
                    EditableTextBlock(
                        segments = editorSegments,
                        blockType = block.type,
                        onChange = { newSegs ->
                            onChange(
                                block.copy(
                                    content = "",
                                    richTextJson = TextSegment.serialize(newSegs)
                                )
                            )
                        },
                        onFocusChange = { if (it) onActivate() },
                        onCursorChange = onCursorChange,
                        onSelectionChange = onSelectionChange,
                        onSplit = onSplit,
                        onMoveToPreviousBlock = onMoveToPreviousBlock,
                        onMoveToNextBlock = onMoveToNextBlock,
                        onDeleteBlock = onDeleteBlock,
                        onConvertToText = { onChange(block.copy(type = BlockType.TEXT)) },
                        onParseResult = onParseResult,
                        modifier = Modifier.weight(1f),
                        numberIndex = numberIndex,
                        requestFocus = requestFocus,
                        onFocusRequested = onFocusRequested,
                        pendingInsert = pendingInsert,
                        initialSelection = initialSelection,
                        pendingSelection = pendingSelection,
                        pendingTypingStyle = pendingTypingStyle
                    )
                } else {
                    ReadOnlyTextBlock(
                        segments = if (block.type == BlockType.CODE_BLOCK) {
                            block.segments() ?: listOf(TextSegment(text = block.content))
                        } else {
                            block.ensureSegments()
                        },
                        blockType = block.type,
                        numberIndex = numberIndex,
                        onActivate = onTapToEdit,
                        onUrlClicked = onUrlClicked,
                        modifier = Modifier.weight(1f)
                    )
                }
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
                    onDeleteBlock = onDeleteBlock,
                    onInsertBelow = { onInsertBelow(index) },
                    onDuplicate = { onDuplicate(index) },
                    onMoveTo = { onMoveTo(index) },
                    onDragTableStart = onDragHandleStart,
                    onDragTableBy = onDragHandleBy,
                    onDragTableEnd = onDragHandleEnd,
                    onDragTableCancel = onDragHandleCancel,
                    modifier = Modifier.weight(1f)
                )
            }
            BlockType.CHECKLIST_ITEM -> {
                val checked = block.meta["checked"] == "true"
                val itemSegments = block.ensureSegments()
                EditableChecklistBlock(
                    itemText = RichTextConverter.segmentsToPlainText(itemSegments),
                    isChecked = checked,
                    globalIndex = index,
                    onChange = { newText ->
                        onChange(
                            block.copy(
                                content = "",
                                richTextJson = TextSegment.serialize(listOf(TextSegment(text = newText)))
                            )
                        )
                    },
                    onToggle = {
                        onChange(block.copy(meta = mapOf("checked" to (!checked).toString())))
                    },
                    onFocusChange = { if (it) onActivate() },
                    onCursorChange = onCursorChange,
                    onSplit = { before, after ->
                        onSplit(listOf(TextSegment(text = before)), listOf(TextSegment(text = after)))
                    },
                    onConvertToText = { onChange(block.copy(type = BlockType.TEXT)) },
                    requestFocus = requestFocus,
                    onFocusRequested = onFocusRequested
                )
            }
            BlockType.COLLAPSIBLE -> {
                val collapsibleContent = RichTextConverter.segmentsToMarkup(block.ensureSegments())
                EditableCollapsibleBlock(
                    summary = block.meta["summary"] ?: "",
                    content = collapsibleContent,
                    onChange = { newSummary, newContent ->
                        onChange(
                            block.copy(
                                content = "",
                                richTextJson = TextSegment.serialize(RichTextConverter.markupToSegments(newContent)),
                                meta = block.meta + ("summary" to newSummary)
                            )
                        )
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
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
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
            BlockType.PAGE_LINK -> {
                val linkedNoteId = block.meta["noteId"]?.toIntOrNull()
                val resolvedTitle = linkedNoteId?.let { noteTitleById(it) }.orEmpty()
                val pageLinkTitle = resolvedTitle.ifBlank { block.content.ifBlank { stringResource(R.string.block_page_link_pick) } }
                OutlinedCard(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = pageLinkTitle,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = if (linkedNoteId == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    if (linkedNoteId != null) onNavigateToNote(linkedNoteId)
                                    else onEditPageLink(index)
                                }
                        )
                        if (linkedNoteId != null) {
                            IconButton(onClick = { onEditPageLink(index) }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.block_page_link_change),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
            BlockType.IMAGE -> {
                val showCaption = block.meta["showCaption"] != "false"
                EditableImageBlock(
                    src = block.content,
                    caption = block.meta["caption"] ?: "",
                    alignment = block.meta["align"] ?: "center",
                    isActive = index == activeBlockIndex,
                    onActivate = onActivate,
                    onOpen = { onNavigateToMediaViewer("image", block.content) },
                    onCaptionChange = { newCaption ->
                        onChange(block.copy(meta = block.meta + ("caption" to newCaption)))
                    },
                    onReplace = { onEditImage(index) },
                    onDelete = onDelete,
                    onAlignmentChange = { newAlign ->
                        onChange(block.copy(meta = block.meta + ("align" to newAlign)))
                    },
                    onConvertTo = { onConvertTo(index) },
                    onInsertBelow = { onInsertBelow(index) },
                    onDuplicate = { onDuplicate(index) },
                    onMoveTo = { onMoveTo(index) },
                    showCaption = showCaption,
                    onShowCaptionChange = { newVal ->
                        onChange(block.copy(meta = block.meta + ("showCaption" to newVal.toString())))
                    },
                    onSrcResolved = { resolved ->
                        onChange(block.copy(content = resolved))
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            else -> {
                val renderingBlock = block.toRenderingBlock()
                var showBlockOptions by remember { mutableStateOf(false) }
                NoteContentBlockCard(
                    block = renderingBlock,
                    noteId = 0,
                    onDeleteBlock = { onDelete() },
                    onUrlClicked = onUrlClicked,
                    onOpenBlockMore = { showBlockOptions = true },
                    modifier = Modifier.weight(1f)
                )
                if (showBlockOptions) {
                    BlockOptionsSheet(
                        title = block.type.displayName,
                        onDismiss = { showBlockOptions = false },
                        actions = listOf(
                            BlockSheetAction(
                                label = stringResource(R.string.block_insert_below),
                                icon = Icons.Default.ArrowDownward,
                                onClick = {
                                    showBlockOptions = false
                                    onInsertBelow(index)
                                }
                            ),
                            BlockSheetAction(
                                label = stringResource(R.string.block_duplicate),
                                icon = Icons.Default.ContentCopy,
                                onClick = {
                                    showBlockOptions = false
                                    onDuplicate(index)
                                }
                            ),
                            BlockSheetAction(
                                label = stringResource(R.string.block_move_to),
                                icon = Icons.AutoMirrored.Filled.DriveFileMove,
                                onClick = {
                                    showBlockOptions = false
                                    onMoveTo(index)
                                }
                            ),
                            BlockSheetAction(
                                label = stringResource(R.string.btn_delete),
                                icon = Icons.Default.Delete,
                                danger = true,
                                onClick = {
                                    showBlockOptions = false
                                    onDelete()
                                }
                            )
                        )
                    )
                }
            }
        }
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
        BlockType.PAGE_LINK -> "Page Link"
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

private val exitOnEmptyTypes = setOf(BlockType.BULLET_LIST, BlockType.NUMBERED_LIST, BlockType.QUOTE, BlockType.CALLOUT)

private fun BlockType.blockVerticalMargins(): Pair<Dp, Dp> = when (this) {
    BlockType.HEADING1, BlockType.HEADING2, BlockType.HEADING3, BlockType.HEADING4 -> 24.dp to 8.dp
    BlockType.PAGE, BlockType.PAGE_LINK, BlockType.TABLE, BlockType.COLLAPSIBLE, BlockType.CALLOUT,
    BlockType.HORIZONTAL_RULE -> 16.dp to 16.dp
    BlockType.TEXT, BlockType.BULLET_LIST, BlockType.NUMBERED_LIST, BlockType.CHECKLIST_ITEM,
    BlockType.QUOTE, BlockType.CODE_BLOCK -> 8.dp to 8.dp
    else -> 0.dp to 0.dp
}

private fun DataBlock.toRenderingBlock(): NoteContentBlock {
    return when (type) {
        BlockType.IMAGE -> {
            val link = meta["linkUrl"]
            val imageBlock = if (link != null) NoteContentBlock.ImageBlock(src = content, linkUrl = link)
            else NoteContentBlock.ImageBlock(src = content)
            imageBlock.copy(
                caption = meta["caption"]?.takeIf { it.isNotBlank() },
                align = meta["align"]?.takeIf { it.isNotBlank() }
            )
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
        BlockType.COLLAPSIBLE -> NoteContentBlock.CollapsibleBlock(
            summary = meta["summary"] ?: "",
            content = com.example.util.RichTextConverter.segmentsToMarkup(ensureSegments())
        )
        else -> {
            val parseResult = com.example.util.RichTextParser.parseWithMapping(
                com.example.util.RichTextConverter.segmentsToMarkup(ensureSegments()),
                hideTags = true
            )
            NoteContentBlock.TextBlock(parseResult = parseResult, rawStart = 0)
        }
    }
}
