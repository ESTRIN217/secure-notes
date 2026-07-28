package com.example.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BlockType
import com.example.util.RichTextParser

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EditableTextBlock(
    rawText: String,
    blockType: BlockType = BlockType.TEXT,
    onChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit = {},
    onCursorChange: (Int) -> Unit = {},
    onSplit: ((before: String, after: String) -> Unit)? = null,
    onMoveToPreviousBlock: () -> Unit = {},
    onMoveToNextBlock: () -> Unit = {},
    onDeleteBlock: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = rawText, selection = TextRange(rawText.length)))
    }
    var isFocused by remember { mutableStateOf(false) }

    if (!isFocused && rawText != textFieldValue.text) {
        textFieldValue = TextFieldValue(text = rawText, selection = TextRange(rawText.length))
    }

    val visualTransformation = remember {
        VisualTransformation { text ->
            val parseResult = RichTextParser.parseWithMapping(text.text, hideTags = true)
            val offsetMapping = object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int =
                    parseResult.originalToTransformed(offset)
                override fun transformedToOriginal(offset: Int): Int =
                    parseResult.transformedToOriginal(offset)
            }
            TransformedText(parseResult.text, offsetMapping)
        }
    }

    val prefix = when (blockType) {
        BlockType.BULLET_LIST -> "• "
        BlockType.NUMBERED_LIST -> "1. "
        BlockType.QUOTE -> "> "
        BlockType.CODE_BLOCK -> "  "
        else -> ""
    }

    val textStyle = when (blockType) {
        BlockType.HEADING1 -> MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        BlockType.HEADING2 -> MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        BlockType.HEADING3 -> MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
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

    val borderColor = when (blockType) {
        BlockType.QUOTE -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        BlockType.CODE_BLOCK -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
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

        val blockModifier = if (blockType == BlockType.QUOTE) {
            Modifier
                .weight(1f)
                .padding(start = 8.dp)
        } else {
            Modifier.weight(1f)
        }

        OutlinedTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                val oldText = textFieldValue.text
                val newText = newValue.text

                if (onSplit != null && newText.length > oldText.length) {
                    val diffStart = oldText.commonPrefixWith(newText).length
                    if (diffStart < newText.length && newText[diffStart] == '\n') {
                        val before = newText.substring(0, diffStart)
                        val after = newText.substring(diffStart + 1)
                        textFieldValue = TextFieldValue(text = before, selection = TextRange(before.length))
                        onCursorChange(before.length)
                        onFocusChange(true)
                        onSplit(before, after)
                        return@OutlinedTextField
                    }
                }

                textFieldValue = newValue
                onChange(newText)
                onCursorChange(newValue.selection.start)
            },
            visualTransformation = visualTransformation,
            modifier = blockModifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                    onFocusChange(focusState.isFocused)
                    if (focusState.isFocused) {
                        onCursorChange(textFieldValue.selection.start)
                    }
                }
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyUp) {
                        when (event.key) {
                            Key.DirectionUp -> {
                                if (textFieldValue.selection.start == 0 && textFieldValue.selection.end == 0) {
                                    onMoveToPreviousBlock()
                                    true
                                } else false
                            }
                            Key.DirectionDown -> {
                                val len = textFieldValue.text.length
                                if (textFieldValue.selection.start == len && textFieldValue.selection.end == len) {
                                    onMoveToNextBlock()
                                    true
                                } else false
                            }
                            Key.Backspace -> {
                                if (textFieldValue.text.isEmpty()) {
                                    onDeleteBlock()
                                    true
                                } else false
                            }
                            else -> false
                        }
                    } else false
                },
            shape = RoundedCornerShape(8.dp),
            textStyle = textStyle,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = borderColor,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
            )
        )
    }
}
