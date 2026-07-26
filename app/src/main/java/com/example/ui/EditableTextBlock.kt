package com.example.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.util.RichTextParser

@Composable
fun EditableTextBlock(
    rawText: String,
    onChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit = {},
    onCursorChange: (Int) -> Unit = {},
    onSplit: ((before: String, after: String) -> Unit)? = null,
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
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
                onFocusChange(focusState.isFocused)
                if (focusState.isFocused) {
                    onCursorChange(textFieldValue.selection.start)
                }
            },
        shape = RoundedCornerShape(8.dp),
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
        )
    )
}
