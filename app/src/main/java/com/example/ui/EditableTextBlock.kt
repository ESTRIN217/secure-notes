package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.model.BlockType
import com.example.data.model.TextSegment
import com.example.util.RichTextConverter
import com.example.util.RichTextParser

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EditableTextBlock(
    segments: List<TextSegment>,
    blockType: BlockType = BlockType.TEXT,
    onChange: (List<TextSegment>) -> Unit,
    onFocusChange: (Boolean) -> Unit = {},
    onCursorChange: (Int) -> Unit = {},
    onSelectionChange: (IntRange) -> Unit = {},
    onSplit: ((before: List<TextSegment>, after: List<TextSegment>) -> Unit)? = null,
    onMoveToPreviousBlock: () -> Unit = {},
    onMoveToNextBlock: () -> Unit = {},
    onDeleteBlock: () -> Unit = {},
    onConvertToText: () -> Unit = {},
    onParseResult: ((RichTextParser.ParseResult) -> Unit)? = null,
    modifier: Modifier = Modifier,
    numberIndex: Int? = null,
    requestFocus: Boolean = false,
    onFocusRequested: () -> Unit = {},
    pendingInsert: MutableState<String?> = remember { mutableStateOf(null) },
    initialSelection: Int? = null,
    pendingSelection: MutableState<IntRange?> = remember { mutableStateOf(null) },
    pendingTypingStyle: TextSegment? = null,
    showPrefix: Boolean = true,
    forcePlain: Boolean = false,
    highlightLanguage: String? = null,
    softWrap: Boolean = true
) {
    var annotated by remember {
        mutableStateOf(RichTextConverter.segmentsToAnnotatedString(segments))
    }
    var fieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = annotated.text,
                selection = TextRange((initialSelection ?: annotated.text.length).coerceIn(0, annotated.text.length))
            )
        )
    }
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()

    val parseResult = remember(annotated) { RichTextConverter.parseResultFor(RichTextConverter.annotatedStringToSegments(annotated)) }

    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            focusRequester.requestFocus()
            onFocusRequested()
        }
    }

    val segmentsKey = RichTextConverter.segmentsJson(segments)
    LaunchedEffect(segmentsKey) {
        val incoming = segments
        val current = RichTextConverter.annotatedStringToSegments(annotated)
        if (RichTextConverter.segmentsJson(incoming) != RichTextConverter.segmentsJson(current)) {
            val newAnnotated = RichTextConverter.segmentsToAnnotatedString(incoming)
            annotated = newAnnotated
            fieldValue = TextFieldValue(
                text = newAnnotated.text,
                selection = TextRange((initialSelection ?: newAnnotated.text.length).coerceIn(0, newAnnotated.text.length))
            )
            onCursorChange(fieldValue.selection.start)
        }
    }

    LaunchedEffect(parseResult) {
        onParseResult?.invoke(parseResult)
    }

    LaunchedEffect(pendingSelection.value) {
        val range = pendingSelection.value ?: return@LaunchedEffect
        val text = fieldValue.text
        val start = range.first.coerceIn(0, text.length)
        val end = range.last.coerceIn(0, text.length).coerceAtLeast(start)
        fieldValue = fieldValue.copy(selection = TextRange(start, end))
        onCursorChange(start)
        onSelectionChange(start..end)
        pendingSelection.value = null
    }

    LaunchedEffect(pendingInsert.value) {
        val insert = pendingInsert.value ?: return@LaunchedEffect
        if (!isFocused) return@LaunchedEffect
        val selStart = fieldValue.selection.start.coerceIn(0, fieldValue.text.length)
        val selEnd = fieldValue.selection.end.coerceIn(0, fieldValue.text.length)
        val before = annotated.subSequence(0, selStart)
        val after = annotated.subSequence(selEnd, annotated.text.length)
        val insertAnnotated = RichTextConverter.segmentsToAnnotatedString(RichTextConverter.markupToSegments(insert))
        val newAnnotated = before + insertAnnotated + after
        annotated = newAnnotated
        val newCursor = selStart + insertAnnotated.text.length
        fieldValue = TextFieldValue(text = newAnnotated.text, selection = TextRange(newCursor))
        onChange(RichTextConverter.annotatedStringToSegments(newAnnotated))
        onCursorChange(newCursor)
        pendingInsert.value = null
    }

    val identityMapping = remember {
        object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = offset
            override fun transformedToOriginal(offset: Int): Int = offset
        }
    }

    val displayAnnotated = remember(annotated, highlightLanguage) {
        if (highlightLanguage != null) {
            com.example.util.CodeHighlighter.highlight(annotated, highlightLanguage)
        } else {
            annotated
        }
    }

    val visualTransformation = VisualTransformation { text ->
        TransformedText(if (text.text == displayAnnotated.text) displayAnnotated else text, identityMapping)
    }

    val prefix = if (!showPrefix) {
        ""
    } else {
        when (blockType) {
            BlockType.BULLET_LIST -> "• "
            BlockType.NUMBERED_LIST -> "${numberIndex ?: 1}. "
            BlockType.QUOTE -> "▎ "
            BlockType.CODE_BLOCK -> "  "
            else -> ""
        }
    }

    val textStyle = when (blockType) {
        BlockType.HEADING1 -> MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        BlockType.HEADING2 -> MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        BlockType.HEADING3 -> MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        BlockType.HEADING4 -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        BlockType.QUOTE -> MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Normal
        )
        BlockType.CODE_BLOCK -> MaterialTheme.typography.bodyLarge.copy(
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface
        )
        BlockType.BULLET_LIST, BlockType.NUMBERED_LIST -> MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface
        )
        else -> MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)
    }

    val rowModifier = if (blockType == BlockType.CALLOUT) {
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    } else {
        modifier.fillMaxWidth()
    }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (prefix.isNotEmpty()) {
            Text(
                text = prefix,
                style = textStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(4.dp))
        }

        if (blockType == BlockType.CALLOUT) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
        }

        val blockModifier = if (blockType == BlockType.QUOTE) {
            Modifier
                .weight(1f)
                .padding(start = 8.dp)
        } else {
            Modifier.weight(1f)
        }

        BasicTextField(
            value = fieldValue,
            onValueChange = { newValue ->
                val oldText = fieldValue.text
                val newText = newValue.text

                if (oldText == newText) {
                    fieldValue = newValue.copy(selection = newValue.selection)
                    onSelectionChange(fieldValue.selection.start..fieldValue.selection.end)
                    onCursorChange(fieldValue.selection.start)
                    return@BasicTextField
                }

                if (onSplit != null && blockType != BlockType.CODE_BLOCK && newText.length > oldText.length) {
                    val diffStart = oldText.commonPrefixWith(newText).length
                    if (diffStart < newText.length && newText[diffStart] == '\n') {
                        val beforeSegs = RichTextConverter.annotatedStringToSegments(annotated.subSequence(0, diffStart))
                        val afterSegs = RichTextConverter.annotatedStringToSegments(annotated.subSequence(diffStart, annotated.text.length))
                        fieldValue = TextFieldValue(text = oldText, selection = TextRange(diffStart))
                        onCursorChange(diffStart)
                        onFocusChange(true)
                        onSplit(
                            beforeSegs,
                            afterSegs
                        )
                        return@BasicTextField
                    }
                }

                val newAnnotated = applyPlainEdit(annotated, newText, pendingTypingStyle, forcePlain)
                annotated = newAnnotated
                fieldValue = newValue.copy(selection = newValue.selection)
                onChange(RichTextConverter.annotatedStringToSegments(newAnnotated))
                onCursorChange(fieldValue.selection.start)
                onSelectionChange(fieldValue.selection.start..fieldValue.selection.end)
            },
            visualTransformation = visualTransformation,
            modifier = blockModifier
                .fillMaxWidth()
                .then(if (softWrap) Modifier else Modifier.horizontalScroll(scrollState))
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                    onFocusChange(focusState.isFocused)
                    if (focusState.isFocused) {
                        onCursorChange(fieldValue.selection.start)
                        onSelectionChange(fieldValue.selection.start..fieldValue.selection.end)
                    }
                }
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyUp) {
                        when (event.key) {
                            Key.DirectionUp -> {
                                if (fieldValue.selection.start == 0 && fieldValue.selection.end == 0) {
                                    onMoveToPreviousBlock()
                                    true
                                } else false
                            }
                            Key.DirectionDown -> {
                                val len = fieldValue.text.length
                                if (fieldValue.selection.start == len && fieldValue.selection.end == len) {
                                    onMoveToNextBlock()
                                    true
                                } else false
                            }
                            Key.Backspace -> {
                                if (fieldValue.text.isEmpty()) {
                                    if (blockType in exitOnEmptyTypes) onConvertToText() else onDeleteBlock()
                                    true
                                } else false
                            }
                            else -> false
                        }
                    } else false
                },
            textStyle = textStyle
        )
    }
}

private fun applyPlainEdit(old: AnnotatedString, newText: String, pendingTypingStyle: TextSegment? = null, forcePlain: Boolean = false): AnnotatedString {
    val oldText = old.text
    if (oldText == newText) return old
    val prefix = commonPrefixLen(oldText, newText)
    var oldEnd = oldText.length
    var newEnd = newText.length
    while (oldEnd > prefix && newEnd > prefix && oldText[oldEnd - 1] == newText[newEnd - 1]) {
        oldEnd--
        newEnd--
    }
    val inserted = newText.substring(prefix, newEnd)
    if (inserted.isEmpty()) {
        return old.subSequence(0, prefix) + old.subSequence(oldEnd, old.text.length)
    }
    val inheritIdx = if (prefix > 0) prefix - 1 else 0
    val inherited = inheritedStyleAt(old, inheritIdx)
    val atEnd = prefix == old.text.length
    val base = if (forcePlain) {
        SpanStyle()
    } else {
        pendingTypingStyle?.let { inherited.merge(it.toSpanStyle()) }
            ?: if (atEnd) SpanStyle() else inherited
    }
    return old.subSequence(0, prefix) +
        AnnotatedString(inserted, spanStyle = base) +
        old.subSequence(oldEnd, old.text.length)
}

private fun inheritedStyleAt(annotated: AnnotatedString, index: Int): SpanStyle {
    if (annotated.text.isEmpty()) return SpanStyle()
    val idx = index.coerceIn(0, annotated.text.length - 1)
    val covering = annotated.spanStyles.filter { it.start <= idx && it.end > idx }
    if (covering.isEmpty()) return SpanStyle()
    return covering.fold(SpanStyle()) { acc, range -> acc.merge(range.item) }
}

private fun commonPrefixLen(a: String, b: String): Int {
    val max = minOf(a.length, b.length)
    var i = 0
    while (i < max && a[i] == b[i]) i++
    return i
}

internal fun snapSelection(text: String, selection: TextRange): TextRange = selection

private val exitOnEmptyTypes = setOf(BlockType.BULLET_LIST, BlockType.NUMBERED_LIST, BlockType.QUOTE, BlockType.CALLOUT)
