package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.estrin217.tema.AppTheme
import com.estrin217.tema.AppThemes
import com.example.R
import com.example.ui.theme.labelRes

/**
 * El selector de tema de Ajustes (spec 025, RF-169).
 *
 * Tiene la misma forma que tenía el selector del editor —una fila por tema, con dos muestras de color, el nombre
 * y la marca de activo—, y está aquí porque el del editor ya no puede estar: el editor no puede cambiar el tema
 * (RF-166), así que un selector suyo habría guardado algo que el resto de la app no sabe.
 *
 * Cada fila se pinta con los colores del propio tema, no con los del tema activo, que es lo que la convierte en
 * una vista previa. La excepción es el tema dinámico: no tiene colores propios, así que su fila se pinta con el
 * Material de la pantalla —y con un `surfaceContainerHigh` neutro en vez del fondo del tema.
 */
@Composable
fun ThemePickerDialog(
    currentTheme: AppTheme,
    onDismiss: () -> Unit,
    onSelectTheme: (AppTheme) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.theme_picker_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppThemes.all.forEach { theme ->
                    ThemeRow(
                        theme = theme,
                        selected = theme.id == currentTheme.id,
                        onClick = {
                            onSelectTheme(theme)
                            onDismiss()
                        },
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
    )
}

/** Una fila del selector: dos muestras de la paleta, el nombre y la marca de activo. */
@Composable
private fun ThemeRow(
    theme: AppTheme,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val name = stringResource(labelRes(theme))
    val description = stringResource(R.string.theme_active_cd)

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = fondoDelTema(theme),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = if (selected) {
                    Modifier.semantics { contentDescription = "$name, $description" }
                } else {
                    Modifier
                },
            ) {
                Muestra(color = acentoPrimario(theme))
                Spacer(modifier = Modifier.width(8.dp))
                Muestra(color = acentoSecundario(theme))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = name,
                    color = textoDelTema(theme),
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 15.sp,
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = acentoSecundario(theme),
                )
            }
        }
    }
}

/** El fondo de la fila: el del tema, o el de la pantalla si el tema no tiene colores. */
@Composable
private fun fondoDelTema(theme: AppTheme): Color = theme.syntax?.background
    ?: MaterialTheme.colorScheme.surfaceContainerHigh

/** El texto de la fila: el de la paleta, o el del tema Material si el tema no tiene colores. */
@Composable
private fun textoDelTema(theme: AppTheme): Color = theme.syntax?.text
    ?: MaterialTheme.colorScheme.onSurface

/** La primera muestra: la palabra clave de la paleta, o el primario del tema Material. */
@Composable
private fun acentoPrimario(theme: AppTheme): Color = theme.syntax?.keyword
    ?: MaterialTheme.colorScheme.primary

/** La segunda muestra: la cadena, o el secundario. */
@Composable
private fun acentoSecundario(theme: AppTheme): Color = theme.syntax?.string
    ?: MaterialTheme.colorScheme.secondary

@Composable
private fun Muestra(color: Color) {
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(color),
    )
}