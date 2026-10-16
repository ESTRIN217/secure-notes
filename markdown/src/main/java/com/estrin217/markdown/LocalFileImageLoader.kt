package com.estrin217.markdown

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.Log
import io.noties.markwon.image.AsyncDrawable
import io.noties.markwon.image.AsyncDrawableLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Las imágenes de la vista previa, y **solo** las del archivo que se está previsualizando (spec 019, RF-111,
 * RF-112, RNF-57).
 *
 * ## Por qué no se usa Coil, ni ninguna otra librería
 *
 * - **Cero dependencias nuevas** (RNF-57): ni una.
 * - La regla permanente de `AGENTS.md` —«el `ImageLoader` singular … no crear `ImageLoader` por pantalla»—
 *   **no aplica**, porque aquí no hay ningún `ImageLoader`: hay un cargador de sesenta líneas que **solo sabe**
 *   decodificar un fichero local.
 * - **La seguridad es del tipo, no de la configuración.** Un cargador de Coil con la máscara bien puesta se puede
 *   configurar mal; este **no tiene el método** de pedir nada por la red. La red no es un caso prohibido, es un
 *   caso imposible.
 *
 * ## Las cuatro salidas
 *
 * | Destino | Qué sale |
 * |---|---|
 * | Local y dentro de la carpeta | La imagen |
 * | Local pero **fuera** de la carpeta | El marcador, sin haber tocado el disco |
 * | Remoto, o sin carpeta base | El marcador, sin haber tocado el disco |
 * | Local pero no existe, o no se pudo decodificar | El marcador de «no se pudo leer» |
 *
 * Los dos marcadores son **distintos a propósito**: uno explica una decisión y el otro un fallo, y poner el
 * segundo cuando pasó el primero sería mentir sobre por qué no se ve la imagen.
 *
 * ## Lo que no hace
 *
 * No **cachea**. Cada vez que Markwon vuelve a construir los spans —y lo hace en cada `setMarkdown`, o sea en cada
 * cambio de texto tras el retardo— vuelve a decodificar. Es el mismo trabajo que hacía el `WebView`, y una caché
 * sería una mejora real que esta spec **no** da: está anotada en «Fuera de alcance» con su motivo, en vez de
 * meterse a medias.
 */
class LocalFileImageLoader(
    private val carpetaBase: String?,
    private val recursos: android.content.res.Resources
) : AsyncDrawableLoader() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var trabajo: Job? = null

    /**
     * Lo que se ve **mientras** carga, y lo que se ve si nunca carga.
     *
     * Es un solo método para los dos casos porque para quien mira es lo mismo: hay una imagen ahí y aún no se ve.
     */
    override fun placeholder(drawable: AsyncDrawable): Drawable = MarcadorDrawable(
        texto = when (clasificarImagen(drawable.destination, carpetaBase)) {
            ImagenPermitida.NO_ES_IMAGEN -> "no es una imagen"
            ImagenPermitida.FUERA_DE_CARPETA -> "fuera de la carpeta del archivo"
            ImagenPermitida.BLOQUEADA -> "imagen remota bloqueada"
            ImagenPermitida.PERMITIDA -> "cargando imagen…"
        }
    )

    /**
     * Carga la imagen.
     *
     * `setResult` es **público** y es lo que se llama al terminar: no hace falta el planificador de Markwon, porque
     * este cargador hace su propio trabajo asíncrono y sabe cuándo ha acabado. También es lo que permite
     * **cambiar el marcador** por el de «no se pudo leer» cuando la carga falla.
     */
    override fun load(drawable: AsyncDrawable) {
        val destino = drawable.destination
        if (clasificarImagen(destino, carpetaBase) != ImagenPermitida.PERMITIDA) {
            // El marcador ya está puesto y **no se toca el disco**: una ruta remota o de fuera de la carpeta no
            // llega ni a mirar el sistema de ficheros.
            return
        }

        val ruta = rutaAbsoluta(destino) ?: return
        trabajo?.cancel()
        trabajo = scope.launch {
            val bitmap = withContext(Dispatchers.IO) { decodificar(ruta) }
            if (bitmap == null) {
                Log.w(TAG, "No se pudo leer la imagen local $ruta")
                // Un marcador **distinto** del de «cargando»: aquí sí se intentó leer y falló, que no es lo mismo
                // que «no se te deja». Poner el mismo en los dos casos sería mentir sobre por qué no se ve.
                drawable.setResult(MarcadorDrawable("no se pudo leer la imagen"))
                return@launch
            }
            drawable.setResult(android.graphics.drawable.BitmapDrawable(recursos, bitmap))
        }
    }

    /** Cancela lo que hubiera en marcha. Lo llama Markwon cuando ya no pinta esa imagen. */
    override fun cancel(drawable: AsyncDrawable) {
        trabajo?.cancel()
        trabajo = null
    }

    /** La ruta absoluta a la que se va a leer, o `null` si el destino no resuelve a un fichero. */
    private fun rutaAbsoluta(destino: String): String? {
        val base = carpetaBase?.trim()
        if (base.isNullOrBlank()) return null
        val sinFile = destino.trim().removePrefix("file://")
        return if (sinFile.startsWith("/")) sinFile else "${base.substringBeforeLast('/', "")}/$sinFile"
    }

    private fun decodificar(ruta: String): Bitmap? =
        runCatching { BitmapFactory.decodeFile(ruta) }.getOrNull()

    private companion object {
        const val TAG = "LocalFileImageLoader"
    }
}

/**
 * El recuadro con el motivo, que es lo que ve la persona en lugar de una imagen.
 *
 * Se mantiene el recuadro de `MarkdownRenderer` y su razón: un hueco mudo hace pensar que el Markdown está mal,
 * y un recuadro con el motivo deja claro que la imagen **está ahí** y por qué no se ve.
 *
 * Dibuja en `onBoundsChange` y no en el constructor porque el `Drawable` no tiene medidas hasta que el motor se las
 * da, y medirlas en el constructor daría un tamaño inventado.
 */
private class MarcadorDrawable(private val texto: String) : Drawable() {

    private val borde = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.GRAY
        // Trazo discontinuo, que es lo que distingue «aquí faltaba algo» de un contenido real.
        pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f)
    }
    private val letra = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textSize = 28f
        color = Color.GRAY
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        invalidateSelf()
    }

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.isEmpty) return
        val margen = 4f
        val marco = Rect(
            b.left + margen.toInt(),
            b.top + margen.toInt(),
            b.right - margen.toInt(),
            b.bottom - margen.toInt()
        )
        canvas.drawRect(marco, borde)

        val ancho = letra.measureText(texto)
        val alto = letra.fontMetrics.let { it.descent - it.ascent }
        // Centrado **a ojo**: sin esto el texto se sale por arriba en un hueco de una sola línea, que es el caso
        // más frecuente, y quedaría cortado por la mitad.
        canvas.drawText(texto, marco.exactCenterX() - ancho / 2f, marco.exactCenterY() + alto / 2f, letra)
    }

    override fun setAlpha(alpha: Int) {
        borde.alpha = alpha
        letra.alpha = alpha
    }

    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
        borde.colorFilter = colorFilter
        letra.colorFilter = colorFilter
    }

    @Deprecated("Es abstract en Drawable desde API 23 y no se puede dejar sin implementar")
    override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
}