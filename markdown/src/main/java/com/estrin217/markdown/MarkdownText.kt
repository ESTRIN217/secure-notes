package com.estrin217.markdown

import android.text.Layout
import android.text.Spanned
import android.text.style.URLSpan
import android.util.Log
import android.util.TypedValue
import android.view.MotionEvent
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon

/**
 * Markdown pintado con el motor de GFM, **fuera del chat** (spec 018, RF-104, RF-105, RF-106).
 *
 * La pinto el texto legal de Ajustes, el changelog de la pantalla de actualización y el de la hoja de
 * novedades. El chat usa [ChatMarkdownText], que es esta con un paso antes: adaptar el markup heredado de la
 * nota.
 *
 * ## Lo que decide quien llama, y lo que decide esto
 *
 * - **El texto entra tal cual.** Aquí no se adapta nada. La adaptación del markup heredado vive en
 *   [ChatMarkdownSource] y solo la necesita el chat; pasársela a las notas de una release **haría daño**, porque
 *   en GitHub el HTML crudo es legítimo y la adaptación retira etiquetas que ahí sí significan algo (caso límite
 *   2 de la spec 018).
 * - **El color lo pasa quien llama**, no se lee del tema: cambia por superficie y, en el chat, por rol.
 * - **[textStyle] es opcional y `null` significa «no toques el estilo de la vista».** No es pereza: el chat
 *   **no** lo pasa, y así sigue viéndose exactamente igual que antes de la 018. Si fuera obligatorio, el chat
 *   tendría que pasar `MaterialTheme.typography.bodyMedium`, y eso cambiaría su apariencia —hoy usa el tamaño
 *   por defecto de la plataforma, que además respeta el ajuste de fuente del dispositivo— y RNF-56 dice que el
 *   chat no cambia.
 *
 * ## La política de toque, y por qué no lleva método de movimiento
 *
 * 1. **El `TextView` no lleva método de movimiento.** Es lo que hace que el motor **no abra nada por su cuenta**:
 *    su resolutor de enlaces solo salta desde el toque de un enlace, y aquí ese toque lo decide [alPulsar].
 * 2. **Solo se abre lo que hay exactamente bajo el dedo**, y solo si es **web**. Un `javascript:`, un `tel:` o
 *    un `mailto:` no abren nada y **no se tragan el gesto**: `alPulsar` devuelve `false` (RF-106). En el chat
 *    ese `false` es además lo que mantiene vivos el menú de la pulsación larga y el deslizamiento; aquí no hay
 *    burbuja detrás, así que solo significa «este toque no abre nada».
 * 3. **La dirección se recorta antes de abrirse.** El texto legal trae su único enlace metido en paréntesis y
 *    un `URLSpan` es un texto con posición, no una dirección (ver [direccionParaAbrir]).
 *
 * ## Lo que este composable NO hace
 *
 * - **No recrea la vista al cambiar el texto.** No hay ninguna clave sobre el contenido: el `AndroidView`
 *   sobrevive y solo se le vuelve a entregar el texto. Es justo lo que hacía mal la librería de Markdown que
 *   esta spec retira —envuelve su vista en un `key(markdown.hashCode())`—, y por eso una misma pantalla puede
 *   animarse sin que el texto se repinte desde cero.
 * - **No carga imágenes.** No hay ningún cargador registrado en el motor (RNF-54).
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color,
    textStyle: TextStyle? = null
) {
    val motor: Markwon = MarkdownEngine.LocalMarkdownEngine.current
    val densidad = LocalDensity.current

    AndroidView(
        modifier = modifier,
        factory = { contexto -> crearVista(contexto) },
        update = { vista ->
            // **El orden importa.** Primero el estilo y después el color: al revés, `setMarkdown` pondría el
            // color del tema por encima del que se acaba de elegir.
            textStyle?.let { aplicarEstilo(vista, it, densidad) }
            vista.setTextColor(color.toArgb())
            motor.setMarkdown(vista, markdown)
        }
    )
}

/**
 * La vista que pinta el motor.
 *
 * Se construye a mano y **no** se le pone método de movimiento ni texto seleccionable, a propósito: es lo que
 * deja los gestos donde estaban y lo que impide que el motor abra cualquier cosa por su cuenta. Es un
 * `TextView` plano y **no** el de appcompat: aquel arrastra cuatro peculiaridades —ajuste de ancho multilínea,
 * reinicio de estado al reciclar, accesibilidad por bloques— que aquí no se usan, y esta app no usa un tema de la
 * plataforma heredada.
 */
private fun crearVista(contexto: android.content.Context): TextView = TextView(contexto).apply {
    setTextIsSelectable(false)
    setOnTouchListener { vista, evento -> alPulsar(vista as TextView, evento) }
}

/**
 * Qué hace un toque (spec 018, RF-106).
 *
 * Devolver `true` significa «yo me lo he quedado»; devolver `false` deja seguir el gesto a quien esté detrás. Se
 * devuelve `true` en **un solo** caso: un toque que cae sobre un enlace **web** y que se abre de verdad.
 */
private fun alPulsar(vista: TextView, evento: MotionEvent): Boolean {
    if (evento.action != MotionEvent.ACTION_UP) return false
    val cruda = enlaceBajoElDedo(vista, evento.x, evento.y) ?: return false
    val destino = direccionParaAbrir(cruda)
    if (!isWebLink(destino)) return false
    return abrir(destino, vista)
}

/**
 * La dirección del enlace que hay **exactamente bajo el dedo**, o `null` si no hay ninguno.
 *
 * Se busca en el `Layout` y no en el texto porque el texto no sabe qué parte está bajo el dedo: un párrafo que
 * se envuelve son cuatro líneas de `Layout` y una de texto. `getOffsetForHorizontal` es lo que traduce
 * «x en esta línea» a «posición en el texto», que es donde viven los `URLSpan`.
 *
 * El desplazamiento se corrige por el relleno de la vista: sin eso, el primer renglón saldría desplazado y
 * tocar la primera línea buscaría en la segunda.
 */
private fun enlaceBajoElDedo(vista: TextView, x: Float, y: Float): String? {
    val disposicion = vista.layout ?: return null
    val texto = vista.text as? Spanned ?: return null
    val fila = filaEnLaDisposicion(disposicion, vista, y) ?: return null
    val izquierda = vista.totalPaddingLeft + vista.scrollX
    val arriba = vista.totalPaddingTop + vista.scrollY
    val offset = disposicion.getOffsetForHorizontal(fila, (x - izquierda).coerceAtLeast(0f))
    return texto.getSpans(offset, offset, URLSpan::class.java)
        .firstOrNull { it.url != null }
        ?.url
}

/** La fila de la disposición que está bajo el dedo, o `null` si el dedo cayó fuera del texto. */
private fun filaEnLaDisposicion(disposicion: Layout, vista: TextView, y: Float): Int? {
    val local = (y - vista.totalPaddingTop - vista.scrollY).toInt()
    if (local < 0 || local >= disposicion.height) return null
    val fila = disposicion.getLineForVertical(local)
    return if (fila in 0 until disposicion.lineCount) fila else null
}

/**
 * Abre la dirección, y avisa si no hay nadie a quien abrirla (spec 017, RNF-51).
 *
 * Un fallo aquí **no** se traga: se anota y se devuelve `false`, con lo que el toque sigue su camino. Tragarse
 * la excepción sin más dejaría el texto en un estado raro del que no se sabría nada.
 */
private fun abrir(destino: String, vista: TextView): Boolean {
    val intencion = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(destino))
    return try {
        vista.context.startActivity(intencion)
        true
    } catch (e: android.content.ActivityNotFoundException) {
        Log.e(TAG, "Ninguna aplicacion atiende $destino", e)
        false
    }
}

/**
 * Pasa al `TextView` lo que de un `TextStyle` se puede pasar sin inventar nada.
 *
 * ## Lo que sí
 *
 * Tamaño, grosor, interletraje y altura de línea.
 *
 * ## Lo que no, y por qué
 *
 * - **`fontFamily` no se pasa.** No es que se haya olvidado: Compose **no** tiene API pública para traducir un
 *   `FontFamily` a un `Typeface` de Android —el `fontFamilyName` que lo haría es interno— y las tres superficies
 *   de Ajustes usan la tipografía de Material, cuyo `fontFamily` es `FontFamily.Default`, que **ya es** el tipo
 *   de letra por defecto de Android. Mapearlo por nombre sería una traducción a medias que hoy no cambia nada y
 *   que un día daría un `Typeface` equivocado en silencio.
 * - **El alineado no se pasa.** Las tres superficies son de texto a la izquierda, y aplicarlo a medias sería peor
 *   que no aplicarlo.
 *
 * ## El orden de las dos últimas
 *
 * La altura de línea se mide contra el alto **natural** de la fuente, y ese depende del tamaño y del grosor: por
 * eso `setLineSpacing` va **al final**. Y el cálculo es la fórmula que usa Compose —el extra es lo que sobra de la
 * altura pedida— y no un múltiplo del tamaño.
 *
 * Cada conversión va detrás de su `isSpecified`: un `TextUnit` sin especificar **lanza** al convertirlo a
 * píxeles, y un `TextStyle` parcial —sin tamaño, o sin altura de línea— es lo normal.
 */
private fun aplicarEstilo(vista: TextView, estilo: TextStyle, densidad: Density) = with(densidad) {
    // El tamaño va primero: es lo que produce la fuente contra la que se mide el alto natural de más abajo.
    if (estilo.fontSize.isSpecified) {
        vista.setTextSize(TypedValue.COMPLEX_UNIT_PX, estilo.fontSize.toPx())
    }

    if (estilo.fontWeight != null) {
        vista.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, estiloDeGrueso(estilo.fontWeight))
    }

    if (estilo.letterSpacing.isSpecified) vista.letterSpacing = estilo.letterSpacing.value

    if (estilo.lineHeight.isSpecified) {
        val pedido = estilo.lineHeight.toPx()
        val natural = vista.paint.fontMetricsInt.let { it.descent - it.ascent }
        vista.setLineSpacing(pedido - natural, 1f)
    }
}

/**
 * El `int` de grosor que entiende `Typeface`.
 *
 * Solo hay dos: normal y negrita. `Typeface` no sabe de 600, así que se decide por el lado en el que está: 600 y
 * más es negrita, menos es normal.
 */
private fun estiloDeGrueso(grosor: FontWeight?): Int =
    if (grosor != null && grosor.weight >= 600) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL

private const val TAG = "MarkdownText"