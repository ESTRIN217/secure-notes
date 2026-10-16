package com.estrin217.editordecodigo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * El cruce entre quien **entrega el texto** y quien **lo pinta** (spec 012, arreglo T2/T3; spec 019, RF-110).
 *
 * Este fichero existe por un bug que **solo se vio en dispositivo**: `MarkdownPreview` volvía a pasar el documento
 * por `MarkdownRenderer`, así que commonmark lo procesaba dos veces y la segunda lo escapaba entero. La pantalla
 * mostraba `<!DOCTYPE html>` y `<h2>Uso rápido</h2>` como texto.
 *
 * ## Por qué ningún test de cada pieza lo cazó
 *
 * `MarkdownRendererTest` comprobaba que el renderizador funciona —y funciona—, y `PreviewLayoutTest` que se componen
 * los paneles correctos —y se componen—. El fallo estaba en **quién llamaba a qué**, un cruce que no existía como
 * código y por tanto no se podía probar.
 *
 * ## Qué queda de ese cruce en la spec 019
 *
 * El generador de HTML **ya no existe**: lo pinta Markwon directamente. El fallo que vigilaba esta prueba no puede
 * volver a darse en esa forma —no hay documento que reprocesar—, así que lo que queda es la mitad que **sí** aplica:
 * **el texto que llega al pintado es el Markdown crudo, sin pasar por nada que lo convierta antes de tiempo.**
 *
 * Y se ha añadido el cruce que la 019 sí tiene, que es nuevo: **la vista previa entrega su texto literal**, sin la
 * adaptación del markup heredado que hace el chat. Pasársela sería el error equivalente: retirar etiquetas que en
 * GitHub son legítimas.
 */
class MarkdownPreviewContractTest {

    private val raizMain = "src/main/java/com/estrin217/editordecodigo"
    private val raizApp = "../app/src/main/java/com/example"

    private fun leer(ruta: String): String = File(ruta).readText()

    private fun leerComprobando(ruta: String, etiqueta: String): String {
        val texto = leer(ruta)
        assertTrue("no se está leyendo $etiqueta ($ruta)", texto.lines().size >= 5)
        return texto
    }

    // ── El texto que llega al pintado ─────────────────────────────────────────────────────

    @Test
    fun `la vista previa entrega el markdown literal`() {
        // Es la mitad del contrato que sobrevive a la 019: lo que llega al motor es lo que escribió la persona,
        // sin una traducción previa. La adaptación del chat (`markdownForMessage`) **no** debe aparecer aquí: en
        // un README el HTML crudo es legítimo y adaptarlo retiraría etiquetas que sí significan algo.
        val pane = leerComprobando("$raizMain/ui/MarkdownPreviewPane.kt", "el panel de la vista previa")

        assertFalse(
            "la vista previa no debe adaptar el markup heredado: es Markdown de GitHub (RF-110)",
            pane.contains("markdownForMessage")
        )
        assertFalse(
            "la vista previa no debe importar la adaptación del chat",
            pane.contains("ChatMarkdownSource")
        )
        assertTrue(
            "el panel debe entregar su texto al motor tal cual",
            pane.contains("MarkdownText(")
        )
    }

    @Test
    fun `el ViewModel entrega el texto y no lo convierte`() {
        val viewModel = leerComprobando("$raizMain/ui/CodeEditorViewModel.kt", "el ViewModel del editor")

        assertFalse(
            "el ViewModel no debe construir un documento: el motor lo pinta (RF-115)",
            viewModel.contains("MarkdownRenderer") || viewModel.contains("toHtml") || viewModel.contains("HtmlDocument")
        )
        assertTrue(
            "el retardo debe seguir: sin él, cada tecla reconstruiría los spans del documento entero",
            viewModel.contains("PREVIEW_DEBOUNCE_MS")
        )
    }

    @Test
    fun `el texto publicado es el mismo que el del editor`() {
        // El cruce, otra vez, pero con el destinatario nuevo: lo que se publica tiene que ser **el texto**, y no
        // una versión transformada de él. Se comprueba que el `StateFlow` se alimenta del texto del editor y no de
        // una llamada a nada.
        val viewModel = leerComprobando("$raizMain/ui/CodeEditorViewModel.kt", "el ViewModel del editor")

        // Ojo al patrón: hay **tres** asignaciones a `_previewMarkdown.value` y dos son `null` —al cerrar la vista
        // previa y al vaciarla—. La que importa es la única que publica un identificador.
        val asignaciones = Regex("""_previewMarkdown\.value\s*=\s*(\w+)""")
            .findAll(viewModel).map { it.groupValues[1] }.toList()
        assertTrue("no se encuentra la asignación del texto de la vista previa", asignaciones.isNotEmpty())
        assertTrue(
            "el texto que se publica tiene que ser el del editor, no un null ni nada transformado",
            asignaciones.contains("text")
        )
    }

    @Test
    fun `el motor del chat no se cuela en el editor de codigo`() {
        // El módulo compartido es de las cinco superficies, pero el **motor** del chat —el que adapta el markup
        // heredado de una nota— no tiene nada que ver aquí. Si algún día se reutilizara, sería la mitad de un bug
        // de la 012 otra vez.
        val arbol = File(raizMain).walkTopDown().filter { it.isFile && it.extension == "kt" }
        val ofensores = arbol.filter { it.readText().contains("ChatMarkdownText") }.toList()
        assertTrue("el editor de código no debe usar el composable del chat", ofensores.isEmpty())
    }

    @Test
    fun `la vista previa no toca el motor del chat`() {
        // Lo mismo, por el otro lado: el editor de código habla con `:markdown`, no con lo que hay en `:app`.
        val arbol = File(raizMain).walkTopDown().filter { it.isFile && it.extension == "kt" }
        val ofensores = arbol.filter { it.readText().contains("com.example.ui.markdown") }.toList()
        assertTrue("el editor de código no debe importar nada del paquete del chat: está en otro módulo", ofensores.isEmpty())
    }

    // ── Lo que la 019 dejó de usar ───────────────────────────────────────────────────────

    @Test
    fun `ya no queda el generador de documento`() {
        listOf("markdown/MarkdownRenderer.kt", "markdown/MarkdownPreview.kt", "ui/PreviewThemeMapping.kt")
            .forEach { assertFalse("debe estar borrado: $it", File("$raizMain/$it").exists()) }
    }

    @Test
    fun `la app tampoco comparta ya nada del motor del chat con el editor`() {
        // Comprobación de que los dos lados del módulo nuevo están donde deben: lo del chat en `:app`, lo
        // compartido en `:markdown`.
        val pane = leerComprobando("$raizMain/ui/MarkdownPreviewPane.kt", "el panel de la vista previa")
        assertTrue(
            "el panel debe usar el motor del módulo compartido",
            pane.contains("com.estrin217.markdown")
        )
        assertTrue(
            "lo que es del chat tiene que seguir en :app",
            File("$raizApp/ui/markdown/ChatMarkdownText.kt").exists()
        )
    }
}