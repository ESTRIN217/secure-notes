package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BlockType
import com.example.data.model.DataBlock

data class SlashCommand(
    val label: String,
    val icon: ImageVector,
    val description: String,
    val blockType: BlockType,
    val defaultContent: String = "",
    val meta: Map<String, String> = emptyMap()
)

private val COMMANDS = listOf(
    SlashCommand("Text", Icons.Default.Title, "Plain text", BlockType.TEXT),
    SlashCommand("Heading 1", Icons.Default.Title, "Large heading", BlockType.HEADING1, ""),
    SlashCommand("Heading 2", Icons.Default.Title, "Medium heading", BlockType.HEADING2, ""),
    SlashCommand("Heading 3", Icons.Default.Title, "Small heading", BlockType.HEADING3, ""),
    SlashCommand("Bulleted List", Icons.AutoMirrored.Filled.FormatListBulleted, "Bulleted list item", BlockType.BULLET_LIST, ""),
    SlashCommand("Numbered List", Icons.Default.FormatListNumbered, "Numbered list item", BlockType.NUMBERED_LIST, ""),
    SlashCommand("Checklist", Icons.Default.CheckBox, "Checklist item", BlockType.CHECKLIST_ITEM, "", mapOf("checked" to "false")),
    SlashCommand("Quote", Icons.Default.FormatQuote, "Block quote", BlockType.QUOTE, ""),
    SlashCommand("Code Block", Icons.Default.Code, "Code block", BlockType.CODE_BLOCK, ""),
    SlashCommand("Divider", Icons.Default.HorizontalRule, "Horizontal divider", BlockType.HORIZONTAL_RULE, ""),
    SlashCommand("Image", Icons.Default.Image, "Embed image", BlockType.IMAGE, "")
)

@Composable
fun SlashCommandMenu(
    filter: String,
    onSelect: (DataBlock) -> Unit,
    onDismiss: () -> Unit
) {
    val filtered = remember(filter) {
        if (filter.isBlank()) COMMANDS
        else COMMANDS.filter { it.label.contains(filter, ignoreCase = true) }
    }

    Card(
        modifier = Modifier
            .width(280.dp)
            .padding(4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        if (filtered.isEmpty()) {
            Text(
                text = "No results",
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        } else {
            LazyColumn {
                items(filtered) { cmd ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(DataBlock(
                                    type = cmd.blockType,
                                    content = cmd.defaultContent,
                                    meta = cmd.meta
                                ))
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = cmd.icon,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = cmd.label,
                                fontSize = 14.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = cmd.description,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
