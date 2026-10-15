package com.example.ui.markdown

import android.text.Layout
import android.text.Spanned
import android.text.style.URLSpan
import android.util.Log
import android.view.MotionEvent
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon

/**
 * El cuerpo de un mensaje del chat, pintado con el motor de GFM (spec 017, RF-98, RF-102).
 *
 * ## Las tres decisiones que hacen que esto no rompa la burbuja
 *
 * 1. **El `TextView` no lleva método de movimiento.** Es lo que hace que **no se quede con los toques**: el
 *    menú de la pulsación larga y el deslizamiento los sigue atendiendo la burbuja (RF-102). Y es lo que hace
 *    que el motor **no abra nada por su cuenta**: su resolutor de enlaces solo salta desde el toque de un
 *    enlace, y aquí ese toque lo decide [alPulsar].
 * 2. **La vista no se recrea cuando cambia el texto.** No hay ninguna clave sobre el contenido: el
 *    `AndroidView` sobrevive y solo se le vuelve a entregar el texto. Es justo lo que la librería de Markdown
 *    del changelog hace mal — envuelve su vista en una clave derivada del texto — y por lo que aquí, y no
 *    allí, un mensaje en curso sería viable algún día.
 * 3. **El texto no es seleccionable.** Como hoy: en el chat la pulsación larga abre el menú de la burbuja.
 *
 * ## Qué se entrega al motor
 *
 * Lo llama `markdownForMessage`, y se calcula **una vez por mensaje**, con la línea como clave del
 * `remember` que lo envuelve en la burbuja: sin esa clave se repetiría en cada recomposición, y con una clave
 * que no fuera el texto se quedaría congelado en el primer mensaje que viera la vista.
 *
 * El color **lo pasa la burbuja**, no se lee del tema: cambia con el rol (una burbuja va sobre
 * `primaryContainer` y la otra sobre `surfaceVariant`), y un color único haría ilegible la de la persona.
 */
@Composable
fun ChatMarkdownText(
    crudo: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val markwon: Markwon = ChatMarkwon.LocalChatMarkwon.current
    val markdown = ChatMarkdownSource.markdownForMessage(crudo)

    AndroidView(
        modifier = modifier,
        factory = { contexto -> crearVista(contexto) },
        update = { vista ->
            vista.setTextColor(color.toArgb())
            markwon.setMarkdown(vista, markdown)
        }
    )
}

/**
 * La vista que pinta el motor.
 *
 * Se construye a mano y **no** se le pone método de movimiento ni texto seleccionable, a propósito: es lo que
 * deja los gestos en la burbuja. Es un `TextView` plano y **no** el de appcompat, que es lo que usa la librería
 * del changelog: aquel arrastra cuatro peculiaridades (ajuste de ancho multilínea, reinicio de estado al
 * reciclar, accesibilidad por bloques) que aquí no se usan, y esta app no usa un tema de la plataforma
 * heredada.
 */
private fun crearVista(contexto: android.content.Context): TextView = TextView(contexto).apply {
    setTextIsSelectable(false)
    setOnTouchListener { vista, evento -> alPulsar(vista as TextView, evento) }
}

/**
 * Qué hace un toque en la burbuja (spec 017, RF-100, RNF-51).
 *
 * Devolver `true` significa «yo me lo he quedado»; devolver `false` deja seguir el gesto a la burbuja, que es
 * lo que mantiene vivos el menú y el deslizamiento. Aquí se devuelve `true` en **un solo** caso: un toque
 * que cae sobre un enlace **web** y que se abre de verdad.
 *
 * Que un enlace con esquema raro devuelva `false` es lo que hace que RF-100 y RF-102 no se pisen: un
 * `javascript:` o un `file:` **no** abren nada y **no** se tragan el menú.
 */
private fun alPulsar(vista: TextView, evento: MotionEvent): Boolean {
    if (evento.action != MotionEvent.ACTION_UP) return false
    val destino = enlaceBajoElDedo(vista, evento.x, evento.y) ?: return false
    if (!ChatMarkdownSource.isWebLink(destino)) return false
    return abrir(destino, vista)
}

/**
 * La dirección del enlace que hay **exactamente bajo el dedo**, o `null` si no hay ninguno.
 *
 * Se busca en el `Layout` y no en el texto porque el texto no sabe qué parte está bajo el dedo: un párrafo
 * que se envuelve son cuatro líneas de `Layout` y una de texto. `getOffsetForHorizontal` es lo que traduce
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
 * Abre la dirección, y avisa si no hay nadie a quien abrirla (RNF-51).
 *
 * Un fallo aquí **no** se traga: se anota y se devuelve `false`, con lo que el toque sigue hacia la burbuja.
 * Tragarse la excepción sin más dejaría el mensaje en un estado raro del que no se sabría nada.
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

private const val TAG = "ChatMarkdownText"
