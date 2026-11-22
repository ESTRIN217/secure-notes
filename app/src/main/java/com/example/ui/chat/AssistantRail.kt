package com.example.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.ChatSessionWithPreview

/**
 * Rail lateral del asistente en Media+ (spec 046 enmienda, RF-370/RF-371/RF-378):
 * destinos Nuevo chat, Búsqueda, Modelos, Ajustes y el resto de secciones, más conversaciones
 * recientes/fijadas y un botón para colapsar/expandir.
 *
 * Es tonto: recibe el destino y el estado de conversaciones, y lanza eventos. No lee ViewModels.
 * El orden de destinos replica el de `NavigationRailContent` de la app para mantener identidad.
 */
@Composable
fun AssistantRail(
    destination: AssistantDestination,
    expanded: Boolean,
    sessions: List<ChatSessionWithPreview>,
    activeSessionId: Int,
    onDestinationChange: (AssistantDestination) -> Unit,
    onToggleExpanded: () -> Unit,
    onNewChat: () -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onNavigateToMain: () -> Unit,
    onNavigateToChat: (Int) -> Unit,
    onTogglePin: (ChatSessionWithPreview) -> Unit,
    modifier: Modifier = Modifier
) {
    val widthDp = assistantRailWidthDp(expanded)
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxHeight()
            .width(widthDp.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 12.dp, horizontal = if (expanded) 12.dp else 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RailBrand(expanded = expanded)
            Spacer(modifier = Modifier.height(16.dp))

            RailNavItem(
                icon = Icons.Default.Add,
                label = stringResource(R.string.chat_new),
                selected = false,
                expanded = expanded,
                onClick = onNewChat
            )
            RailNavItem(
                icon = Icons.Default.Search,
                label = stringResource(R.string.chat_search_hint),
                selected = false,
                expanded = expanded,
                onClick = onSearch
            )
            RailNavItem(
                icon = Icons.AutoMirrored.Filled.Chat,
                label = stringResource(R.string.nav_chats),
                selected = destination == AssistantDestination.CHAT,
                expanded = expanded,
                onClick = { onDestinationChange(AssistantDestination.CHAT) }
            )
            RailNavItem(
                icon = Icons.Default.Memory,
                label = stringResource(R.string.models_panel_title),
                selected = destination == AssistantDestination.MODELS,
                expanded = expanded,
                onClick = { onDestinationChange(AssistantDestination.MODELS) }
            )
            RailNavItem(
                icon = Icons.Default.Settings,
                label = stringResource(R.string.nav_settings),
                selected = false,
                expanded = expanded,
                onClick = onSettings
            )

            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (expanded) 1f else 0.8f)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (expanded) {
                Text(
                    text = stringResource(R.string.chat_history_title),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            val pinned = sessions.filter { it.isPinned }
            val others = sessions.filterNot { it.isPinned }
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(pinned + others, key = { "rail_${it.id}" }) { session ->
                    RailSessionRow(
                        session = session,
                        selected = session.id == activeSessionId,
                        expanded = expanded,
                        onClick = { onNavigateToChat(session.id) },
                        onTogglePin = { onTogglePin(session) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            RailNavItem(
                icon = if (expanded) Icons.AutoMirrored.Filled.ArrowBack else Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                label = stringResource(R.string.nav_home),
                selected = false,
                expanded = expanded,
                onClick = onNavigateToMain
            )
            RailNavItem(
                icon = if (expanded) Icons.AutoMirrored.Filled.KeyboardArrowLeft else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                label = stringResource(R.string.back),
                selected = false,
                expanded = expanded,
                onClick = onToggleExpanded
            )
        }
    }
}

@Composable
private fun RailBrand(expanded: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(22.dp)
            )
        }
        if (expanded) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RailNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit
) {
    val targetBg = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    } else Color.Transparent
    val bg by animateColorAsState(targetValue = targetBg, animationSpec = tween(220), label = "railItemBg")
    val border = if (selected) {
        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
    } else Modifier
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(bg, RoundedCornerShape(12.dp))
            .then(border)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        if (expanded) {
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RailSessionRow(
    session: ChatSessionWithPreview,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
    onTogglePin: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(
                if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                RoundedCornerShape(10.dp)
            )
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (session.isPinned) Icons.Default.PushPin else Icons.AutoMirrored.Filled.Chat,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        if (expanded) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = session.title,
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (session.isPinned) Icons.Default.PushPin else Icons.Default.PushPin,
                contentDescription = stringResource(R.string.chat_pin),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(16.dp)
                    .clickable(onClick = onTogglePin)
            )
        }
    }
}
