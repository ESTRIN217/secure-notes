package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatClear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R

@Composable
fun ColorSelectionDialog(
    title: String,
    onDismiss: () -> Unit,
    onColorSelected: (String) -> Unit
) {
    var hexInput by remember { mutableStateOf("#FFFFFF") }
    var hueValue by remember { mutableStateOf(0f) }

    val pickedColor = remember(hueValue) { Color.hsv(hueValue, 1.0f, 1.0f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.color_option_material),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Triple(stringResource(id = R.string.color_red), "#D32F2F", Color(0xFFD32F2F)),
                        Triple(stringResource(id = R.string.color_blue), "#1976D2", Color(0xFF1976D2)),
                        Triple(stringResource(id = R.string.color_green), "#388E3C", Color(0xFF388E3C))
                    ).forEach { (name, hex, color) ->
                        OutlinedCard(
                            onClick = {
                                onColorSelected(hex)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Text(
                                    text = hex,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                Text(
                    text = stringResource(id = R.string.color_option_picker),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                                )
                            )
                        )
                ) {
                    Slider(
                        value = hueValue,
                        onValueChange = {
                            hueValue = it
                            val col = Color.hsv(it, 1f, 1f)
                            val r = (col.red * 255).toInt()
                            val g = (col.green * 255).toInt()
                            val b = (col.blue * 255).toInt()
                            hexInput = String.format("#%02X%02X%02X", r, g, b)
                        },
                        valueRange = 0f..360f,
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color.Transparent,
                            inactiveTrackColor = Color.Transparent,
                            thumbColor = pickedColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = hexInput,
                        onValueChange = { input ->
                            hexInput = input
                            val cleaned = input.trim().removePrefix("#")
                            if (cleaned.length == 6) {
                                try {
                                    val r = cleaned.substring(0, 2).toInt(16)
                                    val g = cleaned.substring(2, 4).toInt(16)
                                    val b = cleaned.substring(4, 6).toInt(16)
                                    val hsv = FloatArray(3)
                                    android.graphics.Color.RGBToHSV(r, g, b, hsv)
                                    hueValue = hsv[0]
                                } catch (e: Exception) {
                                    // Ignore parse error
                                }
                            }
                        },
                        label = { Text(stringResource(id = R.string.color_hex_label)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    val resolvedPreviewColor = remember(hexInput, pickedColor) {
                        val cleaned = hexInput.trim().removePrefix("#")
                        try {
                            if (cleaned.length == 6) {
                                Color(android.graphics.Color.parseColor("#$cleaned"))
                            } else {
                                pickedColor
                            }
                        } catch (e: Exception) {
                            pickedColor
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(resolvedPreviewColor)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    )
                }

                HorizontalDivider()

                Text(
                    text = stringResource(id = R.string.color_option_default),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                OutlinedCard(
                    onClick = {
                        onColorSelected("default")
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatClear,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.color_default),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalHex = if (hexInput.startsWith("#")) hexInput else "#$hexInput"
                    onColorSelected(finalHex)
                    onDismiss()
                }
            ) {
                Text(stringResource(id = R.string.color_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.color_cancel))
            }
        }
    )
}
