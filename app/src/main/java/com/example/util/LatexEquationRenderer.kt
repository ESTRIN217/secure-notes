package com.example.util

import android.util.Log
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import com.example.data.model.TextSegment
import com.hrm.latex.parser.LatexParser
import com.hrm.latex.parser.ParseDiagnostic
import com.hrm.latex.renderer.measure.rememberLatexMeasurer
import com.hrm.latex.renderer.model.LatexConfig

private const val TAG = "LatexEquations"

/**
 * Puente WYSIWYG entre [TextSegment] con `equationLatex` y la librería
 * `io.github.huarangmeng:latex-renderer` (render inline real en text blocks).
 *
 * Modelo: el `text` del segmento es la fuente `$latex$` (visible y editable) y
 * el latex se deriva de él ([RichTextConverter.deriveLatex]). Si la medición
 * falla (sintaxis inválida), Compose muestra la propia fuente con estilo código.
 */
object LatexEquationRenderer {

    const val EQ_INLINE_PREFIX = "eq_"

    fun inlineId(index: Int): String = "$EQ_INLINE_PREFIX$index"

    fun fallbackStyle(): SpanStyle = SpanStyle(
        fontFamily = FontFamily.Monospace,
        background = Color(0x1F808080),
        color = Color(0xFFE91E63)
    )

    fun hasEquations(segments: List<TextSegment>): Boolean =
        segments.any { it.equationLatex != null }

    fun isValidLatex(latex: String): Boolean {
        if (latex.isBlank()) return false
        return try {
            LatexParser().parseWithDiagnostics(latex).diagnostics.none {
                it.severity == ParseDiagnostic.Severity.ERROR
            }
        } catch (e: Exception) {
            Log.e(TAG, "isValidLatex failed", e)
            false
        }
    }
}

/**
 * Mapa `id -> InlineTextContent` para los latex dados. Las entradas que midan
 * `null` (vacío o fallo) se omiten: Compose muestra el `alternateText` (`$latex$`).
 * El `LatexMeasurerState` interno ya cachea (128 entradas); el `remember` evita
 * remediar en cada recomposición.
 */
@Composable
fun rememberEquationInlineMap(
    latexList: List<String>,
    config: LatexConfig
): Map<String, InlineTextContent> {
    val measurer = rememberLatexMeasurer()
    return remember(latexList, config) {
        buildMap {
            latexList.forEachIndexed { index, latex ->
                try {
                    measurer.inlineContent(latex, config)?.let {
                        put(LatexEquationRenderer.inlineId(index), it)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "inlineContent failed for: $latex", e)
                }
            }
        }
    }
}
