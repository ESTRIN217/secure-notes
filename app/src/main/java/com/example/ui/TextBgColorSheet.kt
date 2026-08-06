@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatClear
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R

private val materialPalette = listOf(
    "#D32F2F", "#E64A19", "#F57C00", "#FBC02D",
    "#7CB342", "#388E3C", "#0097A7", "#1976D2",
    "#303F9F", "#7B1FA2", "#C2185B", "#5D4037",
    "#9E9E9E", "#607D8B", "#000000", "#FFFFFF"
)

@Composable
fun TextBgColorSheet(
    onDismiss: () -> Unit,
    onTextColorSelected: (String) -> Unit,
    onBgColorSelected: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = stringResource(id = R.string.dialog_font_color_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            ColorPickerSection(onColorSelected = onTextColorSelected)

            HorizontalDivider()

            Text(
                text = stringResource(id = R.string.dialog_bg_color_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            ColorPickerSection(onColorSelected = onBgColorSelected)

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ColorPickerSection(onColorSelected: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(id = R.string.color_option_material),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            materialPalette.forEach { hex ->
                ColorSwatch(hex = hex, onClick = { onColorSelected(hex) })
            }
        }

        var showCustom by remember { mutableStateOf(false) }
        OutlinedButton(
            onClick = { showCustom = !showCustom },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(id = R.string.color_option_picker))
        }
        if (showCustom) {
            HsvPicker(onColorSelected = onColorSelected)
        }

        OutlinedButton(
            onClick = { onColorSelected("default") },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.FormatClear,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(id = R.string.color_default))
        }
    }
}

@Composable
private fun ColorSwatch(hex: String, onClick: () -> Unit) {
    val color = remember(hex) {
        runCatching { Color(("FF" + hex.removePrefix("#")).toLong(16).toInt()) }.getOrNull()
    } ?: return
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .clickable(onClick = onClick)
    )
}

@Composable
private fun HsvPicker(onColorSelected: (String) -> Unit) {
    var hue by remember { mutableFloatStateOf(0f) }
    var sat by remember { mutableFloatStateOf(1f) }
    var value by remember { mutableFloatStateOf(1f) }
    val preview = Color.hsv(hue, sat, value)
    val hex = remember(preview) { String.format("#%06X", preview.toArgb() and 0xFFFFFF) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(preview)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            )
            Column {
                Text(
                    text = hex,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = stringResource(id = R.string.color_hex_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HsvSliderRow(label = "H", valueRange = 0f..360f, value = hue, onValueChange = { hue = it })
        HsvSliderRow(label = "S", valueRange = 0f..1f, value = sat, onValueChange = { sat = it })
        HsvSliderRow(label = "V", valueRange = 0f..1f, value = value, onValueChange = { value = it })

        Button(
            onClick = { onColorSelected(hex) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(id = R.string.color_apply))
        }
    }
}

@Composable
private fun HsvSliderRow(
    label: String,
    valueRange: ClosedFloatingPointRange<Float>,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(16.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.weight(1f)
        )
    }
}
