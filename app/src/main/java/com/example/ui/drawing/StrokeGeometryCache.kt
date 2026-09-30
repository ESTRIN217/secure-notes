package com.example.ui.drawing

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.data.model.DrawingStroke

/**
 * Cache de geometría para el lienzo de dibujo.
 *
 * El `Canvas` de Compose se redibuja en cada frame mientras se arrastra el dedo. Construir un
 * `Path` (y su `Stroke`) por trazo y por frame generaba decenas de miles de objetos por segundo
 * en el hilo de render. Aquí los `Path` se reutilizan por índice y solo se reconstruyen cuando el
 * trazo cambia; los `Stroke` se cachean por grosor (hay pocos grosores en uso).
 *
 * Solo se usa dentro del lambda de dibujo: no es estado observable, así que no provoca
 * recomposición.
 */
class StrokeGeometryCache {

    private val paths = ArrayList<Path>()
    private val owners = ArrayList<DrawingStroke?>()
    private val styles = HashMap<Float, Stroke>(8)
    private val live = Path()

    /** Devuelve el `Path` reutilizado del trazo en [index], reconstruyéndolo si ha cambiado. */
    fun pathFor(index: Int, stroke: DrawingStroke): Path {
        grow(index)
        val cached = paths[index]
        if (owners[index] !== stroke) {
            cached.reset()
            buildInto(cached, stroke.points)
            owners[index] = stroke
        }
        return cached
    }

    /** `Stroke` cacheado por grosor: Round/Round es siempre el mismo, solo cambia el width. */
    fun styleFor(width: Float): Stroke = styles.getOrPut(width) {
        Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round)
    }

    /** Path del trazo en curso, reutilizado y reconstruido en cada frame (se dibuja mientras se arrastra). */
    fun livePath(points: List<Offset>): Path {
        live.reset()
        buildInto(live, points)
        return live
    }

    /** Libera los paths que ya no corresponden a ningún trazo visible. */
    fun trimTo(size: Int) {
        while (paths.size > size) {
            paths.removeAt(paths.size - 1)
            owners.removeAt(owners.size - 1)
        }
    }

    private fun grow(index: Int) {
        while (paths.size <= index) {
            paths.add(Path())
            owners.add(null)
        }
    }

    private fun buildInto(target: Path, points: List<Offset>) {
        if (points.isEmpty()) return
        target.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            val p = points[i]
            target.lineTo(p.x, p.y)
        }
    }
}
