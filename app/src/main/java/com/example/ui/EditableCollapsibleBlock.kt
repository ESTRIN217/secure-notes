package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.util.RichTextParser

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EditableCollapsibleBlock(
    summary: String,
    content: String,
    onChange: (summary: String, content: String) -> Unit,
    onSplitSummary: ((before: String, after: String) -> Unit)? = null,
    onFocusChange: (Boolean) -> Unit = {},
    onCursorChange: (Int) -> Unit = {},
    onMoveToPreviousBlock: () -> Unit = {},
    onMoveToNextBlock: () -> Unit = {},
    onConvertToText: () -> Unit = {},
    modifier: Modifier = Modifier,
    requestFocus: Boolean = false,
    onFocusRequested: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    var summaryField by remember { mutableStateOf(TextFieldValue(text = summary, selection = TextRange(summary.length))) }
    var contentField by remember { mutableStateOf(TextFieldValue(text = content, selection = TextRange(content.length))) }
    var isSummaryFocused by remember { mutableStateOf(false) }
    var isContentFocused by remember { mutableStateOf(false) }
    val summaryFocusRequester = remember { FocusRequester() }

    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            summaryFocusRequester.requestFocus()
            onFocusRequested()
        }
    }

    if (!isSummaryFocused && summary != summaryField.text) {
        summaryField = TextFieldValue(text = summary, selection = TextRange(summary.length))
    }
    if (!isContentFocused && content != contentField.text) {
        contentField = TextFieldValue(text = content, selection = TextRange(content.length))
    }

    val contentVisualTransformation = remember {
        VisualTransformation { text ->
            val parseResult = RichTextParser.parseWithMapping(text.text, hideTags = true)
            val offsetMapping = object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = parseResult.originalToTransformed(offset)
                override fun transformedToOriginal(offset: Int): Int = parseResult.transformedToOriginal(offset)
            }
            TransformedText(parseResult.text, offsetMapping)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(if (expanded) 180f else 0f)
                        .clickable { expanded = !expanded },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = summaryField,
                    onValueChange = { newValue ->
                        val oldText = summaryField.text
                        val newText = newValue.text

                        if (onSplitSummary != null && newText.length > oldText.length) {
                            val diffStart = oldText.commonPrefixWith(newText).length
                            if (diffStart < newText.length && newText[diffStart] == '\n') {
                                val before = newText.substring(0, diffStart)
                                val after = newText.substring(diffStart + 1)
                                summaryField = TextFieldValue(text = before, selection = TextRange(before.length))
                                onCursorChange(before.length)
                                onFocusChange(true)
                                onSplitSummary(before, after)
                                return@BasicTextField
                            }
                        }

                        summaryField = newValue
                        onChange(newValue.text, contentField.text)
                        onCursorChange(newValue.selection.start)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(summaryFocusRequester)
                        .onFocusChanged { focusState ->
                            isSummaryFocused = focusState.isFocused
                            onFocusChange(isSummaryFocused || isContentFocused)
                            if (focusState.isFocused) onCursorChange(summaryField.selection.start)
                        }
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyUp) {
                                when (event.key) {
                                    Key.Backspace, Key.Delete -> {
                                        if (summaryField.text.isEmpty()) {
                                            onConvertToText()
                                            true
                                        } else false
                                    }
                                    Key.Enter -> {
                                        if (onSplitSummary != null) {
                                            val sel = summaryField.selection.start
                                            val before = summaryField.text.substring(0, sel)
                                            val after = summaryField.text.substring(sel)
                                            summaryField = TextFieldValue(text = before, selection = TextRange(before.length))
                                            onSplitSummary(before, after)
                                            true
                                        } else false
                                    }
                                    Key.DirectionUp -> {
                                        if (summaryField.selection.start == 0 && summaryField.selection.end == 0) {
                                            onMoveToPreviousBlock()
                                            true
                                        } else false
                                    }
                                    Key.DirectionDown -> {
                                        val len = summaryField.text.length
                                        if (summaryField.selection.start == len && summaryField.selection.end == len) {
                                            onMoveToNextBlock()
                                            true
                                        } else false
                                    }
                                    else -> false
                                }
                            } else false
                        },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                BasicTextField(
                    value = contentField,
                    onValueChange = { newValue ->
                        contentField = newValue.copy(selection = snapSelection(newValue.text, newValue.selection))
                        onChange(summaryField.text, newValue.text)
                        onCursorChange(contentField.selection.start)
                    },
                    visualTransformation = contentVisualTransformation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 32.dp, end = 12.dp, top = 2.dp, bottom = 10.dp)
                        .onFocusChanged { focusState ->
                            isContentFocused = focusState.isFocused
                            onFocusChange(isSummaryFocused || isContentFocused)
                            if (focusState.isFocused) onCursorChange(contentField.selection.start)
                        }
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyUp) {
                                val sel = contentField.selection
                                if (!sel.collapsed) return@onPreviewKeyEvent false
                                val p = sel.start
                                val len = contentField.text.length
                                when (event.key) {
                                    Key.DirectionLeft -> {
                                        if (p > 0) {
                                            val target = RichTextParser.parseWithMapping(contentField.text, hideTags = true).previousVisibleOffset(p)
                                            if (target != p - 1) {
                                                contentField = contentField.copy(selection = TextRange(target))
                                                onCursorChange(target)
                                                true
                                            } else false
                                        } else false
                                    }
                                    Key.DirectionRight -> {
                                        if (p < len) {
                                            val target = RichTextParser.parseWithMapping(contentField.text, hideTags = true).nextVisibleOffset(p)
                                            if (target != p + 1) {
                                                contentField = contentField.copy(selection = TextRange(target))
                                                onCursorChange(target)
                                                true
                                            } else false
                                        } else false
                                    }
                                    Key.Backspace -> {
                                        if (p > 0) {
                                            val target = RichTextParser.parseWithMapping(contentField.text, hideTags = true).previousVisibleOffset(p)
                                            if (target != p - 1) {
                                                contentField = contentField.copy(selection = TextRange(target))
                                                onCursorChange(target)
                                                true
                                            } else false
                                        } else false
                                    }
                                    Key.Delete -> {
                                        if (p < len) {
                                            val target = RichTextParser.parseWithMapping(contentField.text, hideTags = true).nextVisibleOffset(p)
                                            if (target != p + 1) {
                                                contentField = contentField.copy(selection = TextRange(target))
                                                onCursorChange(target)
                                                true
                                            } else false
                                        } else false
                                    }
                                    else -> false
                                }
                            } else false
                        },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    }
}
