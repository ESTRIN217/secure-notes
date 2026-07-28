package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.util.RichTextParser

@Composable
fun EditableChecklistBlock(
    itemText: String,
    isChecked: Boolean,
    globalIndex: Int,
    onChange: (String) -> Unit,
    onToggle: (Int) -> Unit,
    onFocusChange: (Boolean) -> Unit = {},
    onCursorChange: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = itemText, selection = TextRange(itemText.length)))
    }
    var isFocused by remember { mutableStateOf(false) }

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
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue
                onChange(newValue.text)
                onCursorChange(newValue.selection.start)
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                    onFocusChange(focusState.isFocused)
                    if (focusState.isFocused) {
                        onCursorChange(textFieldValue.selection.start)
                    }
                },
            shape = RoundedCornerShape(8.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = if (isChecked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (isChecked) TextDecoration.LineThrough else null
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
            ),
            singleLine = true
        )
    }
}