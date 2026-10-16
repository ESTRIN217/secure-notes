package com.example.ui.markdown

import android.text.util.Linkify
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import io.noties.markwon.Markwon
import io.noties.markwon.SoftBreakAddsNewLinePlugin
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tables.TableTheme
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.linkify.LinkifyPlugin

/**
 * El motor de GFM del chat, **uno por pantalla** (spec 017, RF-98).
 *
 * ## Por qué uno y no uno por burbuja
 *
 * Una lista perezosa **descarta** lo que sale de pantalla: al volver a una burbuja, su composable se crea de
 * nuevo. Un motor por burbuja se reconstruiría en cada vuelta, y construirlo no es barato (registra
 * analizadores,extensions y factories de spans). Uno por pantalla se construye **una vez** mientras el chat
 * esté abierto y se reparte con [LocalChatMarkwon].
 *
 * ## Qué decide el motor y qué decide la burbuja
 *
 * El motor **no pinta el color del texto**: eso es de la burbuja, que cambia con el rol (una es `primaryContainer`
 * y la otra `surfaceVariant`). Del tema solo se afinan las piezas que sí son del motor: enlaces, fondo de
 * código, citas, rayas y tablas.
 *
 * ## Lo que este motor no hace
 *
 * - **No descarga imágenes.** No se registra ningún cargador de imágenes, así que un `![alt](url)` o un
 *   `<img>` no sale a la red (RNF-50). Es la razón por la que el chat puede seguir la regla de que los datos
 *   no salen del dispositivo, sin tener que vigilarse el contenido de cada mensaje.
 * - **No abre nada por su cuenta.** Su resolutor de enlaces solo se dispara desde el toque de un enlace, y el
 *   `TextView` de la burbuja **no** lleva método de movimiento (ver `ChatMarkdownText`). Quién abre es
 *   `ChatMarkdownText`, y solo direcciones web.
 */
object ChatMarkwon {

    /**
     * Cuánto se hunde un bloque (párrafo, lista, tabla) respecto del anterior, en píxeles.
     *
     * Es el mismo margen que usan las listas y las citas del tema del sistema, y es un entero porque el motor
     * trabaja en píxeles y no conoce `dp`. Con `0` todos los bloques quedan pegados y una respuesta con
     * tres párrafos se lee como un muro.
     */
    private const val MARGEN_DE_BLOQUE_PX = 16

    /** Ancho de la barra de la cita, en píxeles. */
    private const val ANCHO_DE_CITA_PX = 6

    /** El motor de la pantalla actual. */
    val LocalChatMarkwon = staticCompositionLocalOf<Markwon> {
        error("No hay motor de Markdown: hay que envolver la pantalla con ProvidesChatMarkwon.")
    }

    /**
     * Envuelve [contenido] con el motor.
     *
     * Va **aquí arriba** y no dentro de cada burbuja a propósito: es lo que hace que el motor sobreviva a que
     * la lista descarte una burbuja.
     */
    @Composable
    fun ProvidesChatMarkwon(contenido: @Composable () -> Unit) {
        // La clave es el **esquema de color**, no el texto: cambiar de tema claro a oscuro tiene que
        // reconstruir el motor, porque sus colores se copiaron al construirlo. Cambiar el contenido de un
        // mensaje, en cambio, **no** lo reconstruye: de eso se encarga `ChatMarkdownText`.
        val esquema = MaterialTheme.colorScheme
        // El contexto se lee **fuera** del `remember`: dentro solo se evalúa la primera vez, y leer un
        // `Local` de composición dentro de esa lambda no es una llamada a composición válida.
        val contexto = LocalContext.current
        val markwon = remember(esquema) { build(contexto, esquema) }
        CompositionLocalProvider(LocalChatMarkwon provides markwon, content = contenido)
    }

    /** Construye el motor con los colores que le pide el tema de la app. */
    private fun build(context: android.content.Context, esquema: androidx.compose.material3.ColorScheme): Markwon {
        val pluginDeTema = PluginDeTema(
            enlace = esquema.primary.toArgb(),
            fondoDeCodigo = esquema.surfaceContainerHighest.toArgb(),
            colorDeCodigo = esquema.onSurfaceVariant.toArgb(),
            fondoDeBloque = esquema.surfaceContainerHigh.toArgb(),
            colorDeCita = esquema.outlineVariant.toArgb(),
            colorDeRaya = esquema.outlineVariant.toArgb(),
            colorBajoEncabezado = esquema.outlineVariant.toArgb()
        )
        // `emptyBuilder()` y **no** el que trae los valores por defecto: los del motor son los de Material
        // heredado, no los de la app, y saldrían con otro color en claro y en oscuro.
        val tabla = TableTheme.emptyBuilder()
            .tableBorderColor(esquema.outlineVariant.toArgb())
            .tableOddRowBackgroundColor(Color.Transparent.toArgb())
            .tableEvenRowBackgroundColor(esquema.surfaceContainerHigh.toArgb())
            .tableHeaderRowBackgroundColor(esquema.surfaceContainerHighest.toArgb())
            .build()

        return Markwon.builder(context)
            // El análisis de bloque normal. **No** el constructor sin núcleo de la librería de Markdown del
            // changelog, que trae su propia versión de él y pinta distinto.
            .usePlugin(TablePlugin.create(tabla))
            .usePlugin(StrikethroughPlugin.create())
            // Dibuja la casilla y su estado. **No** la hace pulsable: eso es RF-101, y no lo hace porque
            // este motor no tiene ningún camino para enterarse de un toque.
            .usePlugin(TaskListPlugin.create(context))
            // Sin este, `<b>` y `<i>` —HTML estándar— se verían como etiquetas en pantalla.
            .usePlugin(HtmlPlugin.create())
            // **Solo URLs web**: con la máscara por defecto, `Linkify.PHONE_NUMBERS` convierte un «600 123 456»
            // en un `tel:` y `EMAIL_ADDRESSES` convierte un correo en un `mailto:`, y el RF-100 dice que
            // ninguno de los dos se abre.
            .usePlugin(LinkifyPlugin.create(Linkify.WEB_URLS))
            // Sin esto, un salto de línea simple se convierte en un espacio y una respuesta con pasos se
            // junta en un párrafo.
            .usePlugin(SoftBreakAddsNewLinePlugin.create())
            .usePlugin(pluginDeTema)
            .build()
    }

    /**
     * El tema del motor, en un plugin propio.
     *
     * Va en su propia clase, y no en una lambda, porque `Markwon.builder` solo acepta **plugins** y el
     * constructor de cada plugin es suyo. Este es el que decide los colores que el motor sí pinta.
     */
    private class PluginDeTema(
        private val enlace: Int,
        private val fondoDeCodigo: Int,
        private val colorDeCodigo: Int,
        private val fondoDeBloque: Int,
        private val colorDeCita: Int,
        private val colorDeRaya: Int,
        private val colorBajoEncabezado: Int
    ) : io.noties.markwon.AbstractMarkwonPlugin() {

        override fun configureTheme(builder: MarkwonTheme.Builder) {
            builder.linkColor(enlace)
            builder.isLinkUnderlined(true)
            builder.codeBackgroundColor(fondoDeCodigo)
            builder.codeTextColor(colorDeCodigo)
            builder.codeBlockBackgroundColor(fondoDeBloque)
            builder.codeBlockTextColor(colorDeCodigo)
            builder.blockQuoteColor(colorDeCita)
            builder.blockQuoteWidth(ANCHO_DE_CITA_PX)
            builder.thematicBreakColor(colorDeRaya)
            builder.headingBreakColor(colorBajoEncabezado)
            builder.blockMargin(MARGEN_DE_BLOQUE_PX)
        }
    }
}
