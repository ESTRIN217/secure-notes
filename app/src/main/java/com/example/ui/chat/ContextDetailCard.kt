package com.example.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import java.util.Locale

/**
 * Tarjeta de detalle de la ocupación del contexto (spec 037, RF-266…RF-270). **Solo pinta** y recibe
 * todo derivado: no decide estado (lo decide el campo al tocar el anillo) ni lee el ViewModel.
 *
 * Diseño: `OutlinedCard` 200×245 dp, esquinas 28, borde `outlineVariant` (mock del usuario): contenido
 * con scroll interno para no exigir más altura, y métricas marcadas con «~» cuando el detalle no es
 * exacto (RF-270). Spec 041: sub-líneas de caché condicionales («reused from KV cache», «fresh + cached»).
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ContextDetailCard(detail: ContextDetail, modifier: Modifier = Modifier) {
    val fraction = contextUsageFraction(detail.usedTokens, detail.windowTokens)
    val pct = formatPercentUsed(fraction)
    val remaining = (detail.windowTokens - detail.usedTokens).coerceAtLeast(0)
    val avgSpeed = detail.avgTokensPerSecond?.let { tps ->
        String.format(Locale.US, "%.1f", tps).let { formatted ->
            stringResource(R.string.chat_msg_tok_per_s, formatted)
        }
    }

    OutlinedCard(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        border = BorderStroke(
            width = 1.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier.size(width = 200.dp, height = 245.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            DetailHeader(title = stringResource(R.string.chat_context_details_title),
                         subtitle = stringResource(R.string.chat_context_of, formatTokensCompact(detail.usedTokens), formatTokensCompact(detail.windowTokens)))
            LinearWavyProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth(),
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.chat_context_pct_used, pct),
                     style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.weight(1f))
                Text(text = stringResource(R.string.chat_context_remaining, formatTokensCompact(remaining)),
                     style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
            HorizontalDivider()
            Text(text = stringResource(R.string.chat_context_usage_details),
                 style = MaterialTheme.typography.labelSmall,
                 color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            Text(text = stringResource(R.string.chat_context_across_all_turns),
                 style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 0.5.sp),
                 color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            DetailRow(stringResource(R.string.chat_context_prompt_evaluated), TokAmount(detail.totalPromptTokens))
            if (detail.totalCachedTokens != null) {
                Text(text = stringResource(R.string.chat_context_reused_kv, detail.totalCachedTokens),
                     style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                     color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
            DetailRow(stringResource(R.string.chat_context_tokens_generated), TokAmount(detail.totalGeneratedTokens))
            Text(text = stringResource(R.string.chat_context_this_turn_kv),
                 style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 0.5.sp),
                 color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            DetailRow(stringResource(R.string.chat_context_prompt), TokAmount(detail.turnPromptRowTokens ?: detail.turnPromptTokens ?: 0))
            if (detail.turnPairFreshTokens != null && detail.turnCachedTokens != null) {
                Text(text = stringResource(R.string.chat_context_fresh_cached, detail.turnPairFreshTokens, detail.turnCachedTokens),
                     style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                     color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
            DetailRow(stringResource(R.string.chat_context_generated), TokAmount(detail.turnGeneratedTokens ?: 0))
            HorizontalDivider()
            DetailRow(stringResource(R.string.chat_context_kv_total), TokAmount(detail.usedTokens))
            if (avgSpeed != null) {
                HorizontalDivider()
                DetailRow(stringResource(R.string.chat_context_avg_speed), avgSpeed)
            }
        }
    }
}

@Composable
private fun DetailHeader(title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = title, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), maxLines = 1)
    }
}

@Composable
private fun TokAmount(value: Int): String =
    stringResource(R.string.chat_context_tok, value)

@Composable
private fun DetailRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.weight(1f))
        Text(text = value, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
    }
}
