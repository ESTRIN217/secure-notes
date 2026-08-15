package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.model.BlockType
import com.example.data.model.TextSegment

@Composable
fun EditableChecklistBlock(
    segments: List<TextSegment>,
    isChecked: Boolean,
    globalIndex: Int,
    onChange: (List<TextSegment>) -> Unit,
    onToggle: (Int) -> Unit,
    onFocusChange: (Boolean) -> Unit = {},
    onCursorChange: (Int) -> Unit = {},
    onSplit: ((before: List<TextSegment>, after: List<TextSegment>) -> Unit)? = null,
    onConvertToText: () -> Unit = {},
    modifier: Modifier = Modifier,
    requestFocus: Boolean = false,
    onFocusRequested: () -> Unit = {}
) {
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
        EditableTextBlock(
            segments = segments,
            blockType = BlockType.TEXT,
            onChange = onChange,
            onFocusChange = onFocusChange,
            onCursorChange = onCursorChange,
            onSelectionChange = {},
            onSplit = onSplit,
            onConvertToText = onConvertToText,
            onEmptyBackspace = onConvertToText,
            showPrefix = false,
            requestFocus = requestFocus,
            onFocusRequested = onFocusRequested,
            textStyle = if (isChecked) {
                MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textDecoration = TextDecoration.LineThrough
                )
            } else {
                null
            },
            modifier = Modifier.weight(1f)
        )
    }
}
