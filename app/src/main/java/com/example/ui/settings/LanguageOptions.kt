package com.example.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R

/** Spec 044 enmienda, RF-332. La lista de idiomas es dato compartido: una sola fuente. */
data class LanguageEntry(val locale: String, val labelRes: Int, val flagRes: Int? = null)

val LANGUAGE_ENTRIES: List<LanguageEntry> = listOf(
    LanguageEntry("", R.string.settings_lang_default),
    LanguageEntry("en", R.string.settings_lang_en, R.drawable.flag_us),
    LanguageEntry("en-GB", R.string.settings_lang_en_gb, R.drawable.flag_gb),
    LanguageEntry("es-419", R.string.settings_lang_es_419, R.drawable.flag_la),
    LanguageEntry("es-VE", R.string.settings_lang_es, R.drawable.flag_ve),
    LanguageEntry("es-ES", R.string.settings_lang_es_es, R.drawable.spanish_flag),
    LanguageEntry("pt-BR", R.string.settings_lang_pt, R.drawable.brazil_flag),
    LanguageEntry("pt-PT", R.string.settings_lang_pt_pt, R.drawable.portugal_flag),
    LanguageEntry("fr", R.string.settings_lang_fr, R.drawable.flag_fr),
    LanguageEntry("it", R.string.settings_lang_it, R.drawable.flag_it)
)

/** Contenido del selector, sin el `ModalBottomSheet`, para poder pintarlo también en el detalle. */
@Composable
fun LanguageOptionsColumn(currentLanguage: String, onSelect: (String) -> Unit) {
    SettingsCardGroup {
        LANGUAGE_ENTRIES.forEachIndexed { index, entry ->
            LanguageOptionRow(entry, currentLanguage == entry.locale) { onSelect(entry.locale) }
            if (index < LANGUAGE_ENTRIES.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun LanguageOptionRow(entry: LanguageEntry, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (entry.flagRes == null) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Image(
                    painter = painterResource(id = entry.flagRes),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Text(
            text = stringResource(entry.labelRes),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}
