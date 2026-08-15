package com.example.ui

import androidx.annotation.StringRes
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
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BorderAll
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.BlockType

enum class BlockAction { NONE, URL_DIALOG, TABLE_DIALOG, IMAGE_DIALOG, VIDEO_DIALOG, VOICE_FILE_DIALOG, FILE_DIALOG, INSERT_PAGE, LINK_PAGE, LINK_INLINE, EQUATION_DIALOG, DRAWING_DIALOG }

enum class BlockSection { BASIC, BLOCKS, LINK, MEDIA }

data class BlockCommand(
    @StringRes val labelRes: Int,
    @StringRes val descriptionRes: Int,
    val icon: ImageVector,
    val section: BlockSection,
    val blockType: BlockType? = null,
    val action: BlockAction = BlockAction.NONE,
    val defaultContent: String = "",
    val meta: Map<String, String> = emptyMap()
)

val BLOCK_COMMANDS: List<BlockCommand> = listOf(
    BlockCommand(R.string.block_text, R.string.block_text_desc, Icons.Default.Title, BlockSection.BASIC, BlockType.TEXT),
    BlockCommand(R.string.block_heading_1, R.string.block_heading_1_desc, Icons.Default.Title, BlockSection.BASIC, BlockType.HEADING1),
    BlockCommand(R.string.block_heading_2, R.string.block_heading_2_desc, Icons.Default.Title, BlockSection.BASIC, BlockType.HEADING2),
    BlockCommand(R.string.block_heading_3, R.string.block_heading_3_desc, Icons.Default.Title, BlockSection.BASIC, BlockType.HEADING3),
    BlockCommand(R.string.block_heading_4, R.string.block_heading_4_desc, Icons.Default.Title, BlockSection.BASIC, BlockType.HEADING4),
    BlockCommand(R.string.block_bulleted, R.string.block_bulleted_desc, Icons.AutoMirrored.Filled.FormatListBulleted, BlockSection.BASIC, BlockType.BULLET_LIST),
    BlockCommand(R.string.block_numbered, R.string.block_numbered_desc, Icons.Default.FormatListNumbered, BlockSection.BASIC, BlockType.NUMBERED_LIST),
    BlockCommand(R.string.block_checklist, R.string.block_checklist_desc, Icons.Default.CheckBox, BlockSection.BASIC, BlockType.CHECKLIST_ITEM, meta = mapOf("checked" to "false")),
    BlockCommand(R.string.block_collapsible, R.string.block_collapsible_desc, Icons.Default.KeyboardArrowDown, BlockSection.BASIC, BlockType.COLLAPSIBLE),
    BlockCommand(R.string.block_page, R.string.block_page_desc, Icons.Default.Description, BlockSection.BASIC, action = BlockAction.INSERT_PAGE),
    BlockCommand(R.string.block_highlight, R.string.block_highlight_desc, Icons.Default.Lightbulb, BlockSection.BASIC, BlockType.CALLOUT),
    BlockCommand(R.string.block_quote, R.string.block_quote_desc, Icons.Default.FormatQuote, BlockSection.BASIC, BlockType.QUOTE),
    BlockCommand(R.string.block_table, R.string.block_table_desc, Icons.Default.BorderAll, BlockSection.BASIC, action = BlockAction.TABLE_DIALOG),
    BlockCommand(R.string.block_divider, R.string.block_divider_desc, Icons.Default.HorizontalRule, BlockSection.BASIC, BlockType.HORIZONTAL_RULE),
    BlockCommand(R.string.block_page_link, R.string.block_page_link_desc, Icons.Default.Link, BlockSection.BASIC, action = BlockAction.LINK_PAGE),
    BlockCommand(R.string.block_inline_link, R.string.block_inline_link_desc, Icons.Default.Link, BlockSection.LINK, action = BlockAction.LINK_INLINE),
    BlockCommand(R.string.block_equation, R.string.block_equation_desc, Icons.Default.Functions, BlockSection.LINK, action = BlockAction.EQUATION_DIALOG),
    BlockCommand(R.string.block_link, R.string.block_link_desc, Icons.Default.Link, BlockSection.LINK, action = BlockAction.URL_DIALOG),
    BlockCommand(R.string.block_image, R.string.block_image_desc, Icons.Default.Image, BlockSection.MEDIA, action = BlockAction.IMAGE_DIALOG),
    BlockCommand(R.string.block_video, R.string.block_video_desc, Icons.Default.Videocam, BlockSection.MEDIA, action = BlockAction.VIDEO_DIALOG),
    BlockCommand(R.string.block_audio, R.string.block_audio_desc, Icons.Default.MusicNote, BlockSection.MEDIA, action = BlockAction.VOICE_FILE_DIALOG),
    BlockCommand(R.string.block_code, R.string.block_code_desc, Icons.Default.Code, BlockSection.MEDIA, BlockType.CODE_BLOCK),
    BlockCommand(R.string.block_file, R.string.block_file_desc, Icons.Default.AttachFile, BlockSection.MEDIA, action = BlockAction.FILE_DIALOG),
    BlockCommand(R.string.block_bookmark, R.string.block_bookmark_desc, Icons.Default.Bookmark, BlockSection.MEDIA, action = BlockAction.URL_DIALOG),
    BlockCommand(R.string.block_drawing, R.string.block_drawing_desc, Icons.Default.Brush, BlockSection.MEDIA, action = BlockAction.DRAWING_DIALOG)
)

@Composable
fun SlashCommandMenu(
    filter: String,
    onSelect: (BlockCommand) -> Unit,
    onDismiss: () -> Unit
) {
    val labeled = BLOCK_COMMANDS.map { cmd -> cmd to stringResource(cmd.labelRes) }
    val filtered = remember(filter) {
        labeled.filter { (_, label) -> filter.isBlank() || label.contains(filter, ignoreCase = true) }
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
                text = stringResource(R.string.search_results_empty),
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        } else {
            LazyColumn {
                items(filtered) { (cmd, _) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(cmd) }
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
                                text = stringResource(cmd.labelRes),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(cmd.descriptionRes),
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
