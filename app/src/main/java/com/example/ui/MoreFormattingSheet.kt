package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.settings.SettingsIconContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreFormattingSheet(
    convertBlockMode: Boolean,
    onDismiss: () -> Unit,
    onCommandSelected: (BlockCommand) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                GridSectionTitle(
                    title = stringResource(
                        if (convertBlockMode) R.string.block_convert_title else R.string.block_section_basic
                    )
                )
            }

            items(
                items = BLOCK_COMMANDS.filter { it.section == BlockSection.BASIC },
                key = { it.labelRes }
            ) { cmd ->
                MoreFormattingGridItem(command = cmd) { onCommandSelected(cmd) }
            }

            val blocks = BLOCK_COMMANDS.filter { it.section == BlockSection.BLOCKS }
            if (blocks.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    GridSectionTitle(title = stringResource(R.string.block_section_blocks))
                }
                items(items = blocks, key = { it.labelRes }) { cmd ->
                    MoreFormattingGridItem(command = cmd) { onCommandSelected(cmd) }
                }
            }

            val links = BLOCK_COMMANDS.filter { it.section == BlockSection.LINK }
            if (links.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    GridSectionTitle(title = stringResource(R.string.block_section_link))
                }
                items(items = links, key = { it.labelRes }) { cmd ->
                    MoreFormattingGridItem(command = cmd) { onCommandSelected(cmd) }
                }
            }

            val media = BLOCK_COMMANDS.filter { it.section == BlockSection.MEDIA }
            if (media.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    GridSectionTitle(title = stringResource(R.string.block_section_media))
                }
                items(items = media, key = { it.labelRes }) { cmd ->
                    MoreFormattingGridItem(command = cmd) { onCommandSelected(cmd) }
                }
            }
        }
    }
}

@Composable
private fun GridSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        ),
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun MoreFormattingGridItem(
    command: BlockCommand,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SettingsIconContainer(icon = command.icon)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(command.labelRes),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
