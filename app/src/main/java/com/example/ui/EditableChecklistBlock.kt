package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.util.RichTextParser

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EditableChecklistBlock(
    itemText: String,
    isChecked: Boolean,
    globalIndex: Int,
    onChange: (String) -> Unit,
    onToggle: (Int) -> Unit,
    onFocusChange: (Boolean) -> Unit = {},
    onCursorChange: (Int) -> Unit = {},
    onSplit: ((before: String, after: String) -> Unit)? = null,
    onConvertToText: () -> Unit = {},
    modifier: Modifier = Modifier,
    requestFocus: Boolean = false,
    onFocusRequested: () -> Unit = {}
) {
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = itemText, selection = TextRange(itemText.length)))
    }
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            focusRequester.requestFocus()
            onFocusRequested()
        }
    }

    if (!isFocused && itemText != textFieldValue.text) {
        textFieldValue = TextFieldValue(text = itemText, selection = TextRange(itemText.length))
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = if (isChecked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
            contentDescription = null,
            tint = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(end = 8.dp)
                .size(24.dp)
                .clickable { onToggle(globalIndex) }
        )
        BasicTextField(
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
                        return@BasicTextField
                    }
                }

                textFieldValue = newValue
                onChange(newText)
                onCursorChange(newValue.selection.start)
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                    onFocusChange(focusState.isFocused)
                    if (focusState.isFocused) {
                        onCursorChange(textFieldValue.selection.start)
                    }
                }
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyUp) {
                        if (event.key == Key.Backspace || event.key == Key.Delete) {
                            if (textFieldValue.text.isEmpty()) {
                                onConvertToText()
                                true
                            } else false
                        } else false
                    } else false
                },
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = if (isChecked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (isChecked) TextDecoration.LineThrough else null
            )
        )
    }
}