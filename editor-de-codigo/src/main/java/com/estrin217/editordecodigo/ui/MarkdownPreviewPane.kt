package com.estrin217.editordecodigo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.estrin217.editordecodigo.syntax.SyntaxTheme
import com.estrin217.markdown.MarkdownColors
import com.estrin217.markdown.MarkdownEngine
import com.estrin217.markdown.MarkdownText

/**
 * El panel de la vista previa del Markdown, con el motor de GFM y **sin `WebView`** (spec 019, RF-110, RF-115,
 * RNF-62).
 *
 * Sustituye a la que generaba un documento HTML con CSS embebido y lo cargaba en un `WebView`. Tres cosas cambian
 * y las tres están razonadas en su sitio:
 *
 * - **El motor, no el HTML.** Lo pinto `MarkdownEngine`, el mismo que el chat y que el texto legal.
 * - **El desplazamiento es de aquí, no del motor** (RNF-62). Un `WebView` se desplazaba solo; un `TextView`
 *   dentro de un panel con peso **no**, y sin esto la vista previa sería inútil para cualquier documento largo.
 * - **El cargador de imágenes es local y confinado** (RF-111, RF-113): enseña las imágenes del archivo y de sus
 *   subcarpetas, y **no** puede abrir nada de fuera ni pedir nada por la red.
 *
 * @param markdown El texto ya adaptado por el ViewModel. `null` es el estado de retardo: se pinta el fondo del
 *   tema y nada más, porque un hueco con el color del tema se lee mejor que un hueco blanco.
 * @param rutaArchivo La ruta del archivo abierto, **no** la de su carpeta: de ella sale la carpeta y es contra la
 *   que se resuelven las imágenes relativas.
 */
@Composable
fun MarkdownPreviewPane(
    markdown: String?,
    theme: SyntaxTheme,
    rutaArchivo: String?,
    modifier: Modifier = Modifier
) {
    if (markdown == null) {
        Box(modifier = modifier.background(theme.background))
        return
    }

    // El estado del desplazamiento vive **aquí** y no dentro del composable del motor: si viviera dentro, cada
    // cambio del texto —y hay uno por pulsación tras el retardo— devolvería la vista previa al principio.
    val desplazamiento = rememberScrollState()

    MarkdownEngine.ProvidesMarkdownEngine(
        colores = theme.toMarkdownColors(),
        // El HTML incrustado se ve **como texto**. Antes lo escapaba entero el `escapeHtml(true)` del generador
        // (RF-114): aquí se retira el plugin que lo interpretaría, y comprobar que se ve literal necesita
        // dispositivo, así que no se confía: se quita el plugin entero.
        interpretandoHtml = false,
        // Se pasa la **ruta del archivo**, no un cargador. Así esta pantalla no nombra ninguna clase de Markwon y
        // **no puede** dar al motor un cargador que salga a la red (RF-111, RF-113, RNF-57).
        imagenesLocalesDe = rutaArchivo
    ) {
        MarkdownText(
            markdown = markdown,
            textStyle = theme.toMarkdownTextStyle(),
            color = theme.text,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(desplazamiento)
                .padding(16.dp)
        )
    }
}

/**
 * El tema del editor traducido al del motor.
 *
 * Los colores son los mismos que `PreviewTheme` llevaba en hexadecimal, que era lo que el `WebView` pintaba: el
 * cambio es de **dónde** salen, no de cuáles son, para que la vista previa siga teniendo el aspecto que tenía.
 */
private fun SyntaxTheme.toMarkdownColors(): MarkdownColors = MarkdownColors(
    enlace = keyword.toArgb(),
    fondoDeCodigo = gutterBackground.toArgb(),
    colorDeCodigo = comment.toArgb(),
    fondoDeBloque = gutterBackground.toArgb(),
    colorDeCita = comment.toArgb(),
    colorDeRaya = activeLineBackground.toArgb(),
    colorBajoEncabezado = activeLineBackground.toArgb()
)

/**
 * El texto de la vista previa.
 *
 * Son los mismos números que pintaba el CSS del `WebView` —`font-size: 15px` y `line-height: 1.6`— y no los del
 * editor, a propósito: la vista previa tiene que leerse a la distancia de un **texto**, no de un **código**. Un
 * bloque de código al tamaño del editor deja de caber en la mitad de la pantalla.
 */
private fun SyntaxTheme.toMarkdownTextStyle(): TextStyle = TextStyle(
    fontSize = PREVIO_FONT_SP.sp,
    lineHeight = (PREVIO_FONT_SP * PREVIO_ESCALA_LINEA).sp
)

/** El `font-size: 15px` del CSS que pintaba el `WebView`. */
private val PREVIO_FONT_SP = 15f

/** Equivale al `line-height: 1.6` de ese mismo CSS. */
private const val PREVIO_ESCALA_LINEA = 1.6f