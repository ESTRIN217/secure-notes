package com.example.ui.markdown

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.estrin217.markdown.MarkdownText

/**
 * El cuerpo de un mensaje del chat, pintado con el motor de GFM (spec 017, RF-98, RF-102).
 *
 * ## Las tres decisiones que hacen que esto no rompa la burbuja
 *
 * 1. **El `TextView` no lleva método de movimiento.** Es lo que hace que **no se quede con los toques**: el
 *    menú de la pulsación larga y el deslizamiento los sigue atendiendo la burbuja (RF-102). Y es lo que hace
 *    que el motor **no abra nada por su cuenta**: su resolutor de enlaces solo salta desde el toque de un
 *    enlace, y aquí ese toque lo decide [MarkdownText]. El resto de la política está allí, y es **la misma** que
 *    en las superficies de Ajustes (spec 018).
 * 2. **La vista no se recrea cuando cambia el texto.** No hay ninguna clave sobre el contenido: el `AndroidView`
 *    sobrevive y solo se le vuelve a entregar el texto. Es lo que la librería de Markdown del changelog hace mal
 *    — envuelve su vista en una clave derivada del texto — y lo que dejó el streaming como posible.
 * 3. **El texto no es seleccionable.** Como hoy: en el chat la pulsación larga abre el menú de la burbuja.
 *
 * ## Lo único que hace aquí y no hace [MarkdownText] es adaptar
 *
 * Un mensaje puede llegar con dos dialectos mezclados —el **Markdown** que escribe el modelo, que el motor ya
 * entiende, y el **markup heredado** de las notas, que no— y lo segundo se vería tal cual. Esa adaptación es
 * [ChatMarkdownSource.markdownForMessage], y va **una sola vez**, con su `remember`.
 *
 * El doble que se tenía antes de la spec 018 —la burbuja adaptando dentro de su `remember` y este composable
 * adaptando otra vez **sin** él, o sea en cada recomposición sobre el hilo principal— no era visible porque
 * adaptar un texto ya adaptado **no** lo cambia. Eso está fijado por una prueba, no supuesto: si deja de ser
 * cierto, el arreglo se apoya en una suposición y hay que volver a mirarlo.
 *
 * ## El color lo pasa la burbuja, y el estilo no se toca
 *
 * El **color** no se lee del tema porque cambia con el rol: una burbuja va sobre `primaryContainer` y la otra
 * sobre `surfaceVariant`, y un color único haría ilegible la de la persona.
 *
 * El **estilo** sí podría —`MaterialTheme.typography.bodyMedium` sería lo coherente con el resto de la app— pero
 * **no** se pasa, y es deliberado: la vista conserva el tamaño por defecto de la plataforma, que además respeta
 * el ajuste de fuente del dispositivo, y darle uno fijo **cambiaría la apariencia del chat**. RNF-56 dice que el
 * chat no cambia, así que no cambia. Queda anotado como revisión pendiente, no como descuido de la migración.
 */
@Composable
fun ChatMarkdownText(
    crudo: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val markdown = remember(crudo) { ChatMarkdownSource.markdownForMessage(crudo) }
    MarkdownText(markdown = markdown, modifier = modifier, color = color)
}