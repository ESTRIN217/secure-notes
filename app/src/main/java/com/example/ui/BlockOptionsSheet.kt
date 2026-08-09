package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

data class BlockSheetAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val danger: Boolean = false,
    val toggle: Boolean? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockOptionsSheet(
    title: String,
    actions: List<BlockSheetAction>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    contentAfterActions: @Composable ColumnScope.() -> Unit = {}
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp, start = 24.dp, end = 24.dp)
            )
            Spacer(Modifier.height(4.dp))

            actions.forEach { action ->
                BlockSheetActionRow(action)
            }

            contentAfterActions()
        }
    }
}

@Composable
fun BlockOptionsSheetDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun BlockSheetActionRow(action: BlockSheetAction) {
    val contentColor = when {
        action.danger -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
    val iconTint = when {
        action.danger -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = action.enabled) { action.onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = action.icon,
            contentDescription = null,
            tint = if (action.enabled) iconTint else iconTint.copy(alpha = 0.4f),
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = action.label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (action.enabled) contentColor else contentColor.copy(alpha = 0.4f),
            modifier = Modifier
                .weight(1f)
                .padding(start = 20.dp, end = 8.dp)
        )
        if (action.toggle != null) {
            Spacer(Modifier.width(4.dp))
            Switch(
                checked = action.toggle,
                onCheckedChange = { action.onClick() },
                enabled = action.enabled
            )
        }
    }
}
