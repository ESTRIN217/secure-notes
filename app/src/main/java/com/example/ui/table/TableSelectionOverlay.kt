package com.example.ui.table

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** Grosor del contorno de selección. */
private val OutlineStrokeWidth = 2.dp

/**
 * Capa que dibuja el contorno de selección por encima de la tabla.
 *
 * Existe como capa aparte porque el contorno tiene que ser **una sola región** y las celdas no
 * pueden dibujarlo: cada celda solo conoce su propia caja, así que dibujarlo allí produce una
 * escalera de rectángulos que no se unen. Aquí la posición sale de [selectionOutline], que ya está
 * comprobada sin pantalla.
 *
 * No participa en la medición: se dibuja sobre lo que hay, sin ocupar sitio ni desplazar nada. Es
 * lo que hace que enfocar una celda no mueva la tabla.
 *
 * El trazo se desplaza hacia dentro media anchura porque Compose lo dibuja centrado sobre el
 * camino: sin ese ajuste, la mitad del contorno caería fuera de la celda y la línea de la rejilla
 * quedaría tapada por fuera.
 */
@Composable
fun TableSelectionOverlay(
    metrics: TableMetrics,
    state: SelectionState,
    modifier: Modifier = Modifier
) {
    val outline = selectionOutline(metrics, state) ?: return
    val stroke = with(LocalDensity.current) { OutlineStrokeWidth.toPx() }
    val inset = stroke / 2f
    val accent = MaterialTheme.colorScheme.primary
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                color = accent,
                topLeft = Offset(outline.left + inset, outline.top + inset),
                size = Size(outline.width - stroke, outline.height - stroke),
                style = Stroke(width = stroke)
            )
        }
    }
}