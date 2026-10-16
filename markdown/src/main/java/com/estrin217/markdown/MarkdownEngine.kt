package com.estrin217.markdown

import android.text.util.Linkify
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.MarkwonSpansFactory
import io.noties.markwon.SoftBreakAddsNewLinePlugin
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tables.TableTheme
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.AsyncDrawableLoader
import io.noties.markwon.image.ImageSpanFactory
import io.noties.markwon.linkify.LinkifyPlugin

/**
 * Los colores que el motor pinta, en `Int` y **no** en `Color` (spec 019, RNF-59).
 *
 * La frontera es lo que permite que el módulo del motor no dependa de cómo pinte cada superficie. El chat con IA
 * usa los colores de Material; la vista previa del editor de código usa los de su `SyntaxTheme`, que es un tema
 * distinto por completo. Con un solo tema fijo, una de las dos se vería con los colores de la otra.
 *
 * Van en `Int` y no en `Color` por el mismo motivo que `PreviewTheme` los llevaba en hexadecimal: el motor no debe
 * importar nada de la interfaz, y la conversión la hace quien lo usa, que sí puede usar Compose.
 */
data class MarkdownColors(
    val enlace: Int,
    val fondoDeCodigo: Int,
    val colorDeCodigo: Int,
    val fondoDeBloque: Int,
    val colorDeCita: Int,
    val colorDeRaya: Int,
    val colorBajoEncabezado: Int
)

/**
 * El motor de GFM de **toda** superficie de Markdown de la app, **uno por pantalla** (spec 017, RF-98;
 * spec 018, RF-104, RF-105; spec 019, RF-110).
 *
 * Lo pinto el chat con IA, la pantalla legal, el changelog, la pantalla de actualización y la vista previa del
 * editor de código. Vive en su propio módulo porque lo necesitan **dos**, y entre ellos la dependencia va en un
 * solo sentido.
 *
 * ## Por qué uno y no uno por elemento
 *
 * Una lista perezosa **descarta** lo que sale de pantalla: al volver a una burbuja —o a una release de la hoja de
 * novedades—, su composable se crea de nuevo. Un motor por elemento se reconstruiría en cada vuelta, y
 * construirlo no es barato (registra analizadores, extensiones y factories de spans). Uno por pantalla se
 * construye **una vez** mientras esté abierta y se reparte con [LocalMarkdownEngine].
 *
 * ## Qué decide el motor y qué decide quien lo pinta
 *
 * El motor **no pinta el color del texto**: eso es de quien lo usa, y cambia —una burbuja va sobre
 * `primaryContainer` y otra sobre `surfaceVariant`, y en la vista previa es el color del tema del editor—. Del
 * tema solo se afinan las piezas que sí son del motor: enlaces, fondo de código, citas, rayas y tablas.
 *
 * ## Lo que este motor no hace
 *
 * - **No descarga imágenes, salvo que se le pase un cargador.** Las cuatro superficies de `:app` pasan `null` y no
 *   pueden mostrar ninguna: no hay imágenes en un mensaje del chat ni en un texto legal, y abrirlas convertiría el
 *   chat en una vía de salida (RNF-50, RNF-54). La vista previa del editor **sí** las enseña, y para eso pasa un
 *   cargador propio que solo sabe leer ficheros locales (RF-111).
 * - **No abre nada por su cuenta.** Su resolutor de enlaces solo se dispara desde el toque de un enlace, y el
 *   `TextView` **no** lleva método de movimiento (ver `MarkdownText`). Quién abre es `MarkdownText`, y solo
 *   direcciones web.
 */
object MarkdownEngine {

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
    val LocalMarkdownEngine = staticCompositionLocalOf<Markwon> {
        error("No hay motor de Markdown: hay que envolver la pantalla con ProvidesMarkdownEngine.")
    }

    /**
     * Envuelve [contenido] con el motor.
     *
     * Va **aquí arriba** y no dentro de cada elemento a propósito: es lo que hace que el motor sobreviva a que
     * la lista descarte lo que sale de pantalla. Cada superficie que pinte Markdown envuelve **su** contenido, y
     * como no hay dos abiertas a la vez, los motores nunca coexisten.
     *
     * @param colores Los de la superficie, que son distintos en cada una.
     * @param interpretandoHtml Si es `false`, el HTML incrustado se ve **como texto**. Lo usa la vista previa del
     *   editor de código, que antes lo escapaba entero con `escapeHtml(true)` (RF-114); en las otras superficies es
     *   `true` y `<b>`, `<i>` y `<details>` se pintan con su estilo.
     * @param imagenesLocalesDe La ruta del **archivo** cuyas imágenes locales se pueden leer, o `null` —que es lo
     *   que hacen las cuatro superficies de `:app`, y por tanto **ninguna imagen** se carga—. Se pone la **ruta
     *   del archivo** y no la de un cargador a propósito, por dos razones: quien lo usa no tiene que conocer
     *   ninguna clase de Markwon, y **no puede** pasar un cargador que salga a la red aunque quiera, porque el tipo
     *   no cruza el módulo (RNF-57).
     */
    @Composable
    fun ProvidesMarkdownEngine(
        colores: MarkdownColors,
        interpretandoHtml: Boolean = true,
        imagenesLocalesDe: String? = null,
        contenido: @Composable () -> Unit
    ) {
        // La clave es el **esquema de color**, no el texto: cambiar de tema claro a oscuro tiene que
        // reconstruir el motor, porque sus colores se copiaron al construirlo. Cambiar el contenido de un
        // mensaje, en cambio, **no** lo reconstruye: de eso se encarga `MarkdownText`.
        val contexto = LocalContext.current
        val recursos = contexto.resources
        val motor = remember(colores, interpretandoHtml, imagenesLocalesDe, recursos) {
            val cargador = imagenesLocalesDe?.let { LocalFileImageLoader(it, recursos) }
            build(contexto, colores, interpretandoHtml, cargador)
        }
        CompositionLocalProvider(LocalMarkdownEngine provides motor, content = contenido)
    }

    /** Construye el motor con los colores que le pide la superficie que lo usa. */
    private fun build(
        context: android.content.Context,
        colores: MarkdownColors,
        interpretandoHtml: Boolean,
        cargadorDeImagenes: AsyncDrawableLoader?
    ): Markwon {
        val pluginDeTema = PluginDeTema(
            enlace = colores.enlace,
            fondoDeCodigo = colores.fondoDeCodigo,
            colorDeCodigo = colores.colorDeCodigo,
            fondoDeBloque = colores.fondoDeBloque,
            colorDeCita = colores.colorDeCita,
            colorDeRaya = colores.colorDeRaya,
            colorBajoEncabezado = colores.colorBajoEncabezado
        )
        // `emptyBuilder()` y **no** el que trae los valores por defecto: los del motor son los de Material
        // heredado, no los de la app, y saldrían con otro color en claro y en oscuro.
        val tabla = TableTheme.emptyBuilder()
            .tableBorderColor(colores.colorDeRaya)
            .tableOddRowBackgroundColor(Color.Transparent.toArgb())
            .tableEvenRowBackgroundColor(colores.fondoDeBloque)
            .tableHeaderRowBackgroundColor(colores.fondoDeCodigo)
            .build()

        val constructor = Markwon.builder(context)
            // El análisis de bloque normal.
            .usePlugin(TablePlugin.create(tabla))
            .usePlugin(StrikethroughPlugin.create())
            // Dibuja la casilla y su estado. **No** la hace pulsable: eso es RF-101 del chat, y no lo hace porque
            // este motor no tiene ningún camino para enterarse de un toque.
            .usePlugin(TaskListPlugin.create(context))
            // Sin esto, `<b>` y `<i>` —HTML estándar— se verían como etiquetas en pantalla. Solo se registra
            // donde tiene sentido; ver [ProvidesMarkdownEngine].
            .apply { if (interpretandoHtml) usePlugin(HtmlPlugin.create()) }
            // **Solo URLs web**: con la máscara por defecto, `Linkify.PHONE_NUMBERS` convierte un «600 123 456»
            // en un `tel:` y `EMAIL_ADDRESSES` convierte un correo en un `mailto:`, y RF-100 dice que
            // ninguno de los dos se abre.
            .usePlugin(LinkifyPlugin.create(Linkify.WEB_URLS))
            // Sin esto, un salto de línea simple se convierte en un espacio y una respuesta con pasos se
            // junta en un párrafo.
            .usePlugin(SoftBreakAddsNewLinePlugin.create())
            .usePlugin(pluginDeTema)

        // El cargador va **al final**, después del tema: es el que sustituye el nodo de imagen cuando Markwon
        // construye los spans, y el color de su marcador lo pone el tema.
        if (cargadorDeImagenes != null) {
            constructor.usePlugin(LocalImagesPlugin(cargadorDeImagenes))
        }
        return constructor.build()
    }

    /**
     * Las imágenes, con **un cargador propio** y no con el plugin de la librería.
     *
     * ## Por qué no se usa `io.noties.markwon.image.ImagesPlugin`
     *
     * Eso que parece un paquete es una clase de **`markwon-core`**, que ya está en el APK. El plugin equivalente
     * vive en un artefacto aparte, `markwon-image`, que **no está** —la librería de Markdown que la 018 retiró se
     * traía su propio *fork* de Markwon, y era de ahí de donde salía—. Declararlo añadiría un artefacto nuevo al
     * APK por unas diez líneas que se pueden escribir aquí (RNF-57).
     *
     * Lo que hace son esas diez: registrar el cargador en la configuración y decir qué fábrica de spans se usa
     * para un nodo de imagen. Todo lo demás —`AsyncDrawable`, su planificador, su hueco mientras carga— ya está
     * dentro del núcleo y se usa igual.
     */
    private class LocalImagesPlugin(private val cargador: AsyncDrawableLoader) : AbstractMarkwonPlugin() {

        override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
            builder.asyncDrawableLoader(cargador)
        }

        override fun configureSpansFactory(builder: MarkwonSpansFactory.Builder) {
            builder.setFactory(org.commonmark.node.Image::class.java, ImageSpanFactory())
        }
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
    ) : AbstractMarkwonPlugin() {

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