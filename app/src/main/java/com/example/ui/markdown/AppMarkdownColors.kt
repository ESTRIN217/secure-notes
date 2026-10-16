package com.example.ui.markdown

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.toArgb
import com.estrin217.markdown.MarkdownColors

/**
 * Los colores de Material para el motor de Markdown (spec 018, RNF-59).
 *
 * ## Por qué no los saca el motor por dentro
 *
 * Porque ya no hay **un** tema. La vista previa del editor de código usa el `SyntaxTheme` de VS Code, que no tiene
 * nada que ver con Material (spec 019). Si el motor leyera `MaterialTheme.colorScheme` por su cuenta, la vista
 * previa se pintaría con los colores de la app, y con un tema fijo cada superficie se vería con los de la otra.
 *
 * Vive en `:app` y no en `:markdown` porque **es la traducción de un tema concreto**: quien pinte con otro tema
 * —el editor de código— hace la suya. El módulo del motor solo recibe `Int`, y así no depende de Material.
 *
 * Los colores son los mismos que mapeaba el motor de la 017, **sin tocar ninguno**: pasarlos por aquí no puede
 * cambiar la apariencia del chat, y RNF-56 dice que no cambia.
 */
@Composable
@ReadOnlyComposable
fun markdownColors(): MarkdownColors = MarkdownColors(
    enlace = MaterialTheme.colorScheme.primary.toArgb(),
    fondoDeCodigo = MaterialTheme.colorScheme.surfaceContainerHighest.toArgb(),
    colorDeCodigo = MaterialTheme.colorScheme.onSurfaceVariant.toArgb(),
    fondoDeBloque = MaterialTheme.colorScheme.surfaceContainerHigh.toArgb(),
    colorDeCita = MaterialTheme.colorScheme.outlineVariant.toArgb(),
    colorDeRaya = MaterialTheme.colorScheme.outlineVariant.toArgb(),
    colorBajoEncabezado = MaterialTheme.colorScheme.outlineVariant.toArgb()
)