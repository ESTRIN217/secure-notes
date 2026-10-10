package com.estrin217.editordecodigo.markdown

/**
 * El cruce entre el texto del editor y lo que se ve en pantalla, en **un solo sitio**.
 *
 * Existe porque hubo un bug que solo se vio en dispositivo: la vista previa enseñaba el
 * `<!DOCTYPE html>` y el `<h2>` como texto, porque el documento pasaba por commonmark **dos veces**
 * —la primera al generarlo y la segunda al mostrarlo—, y la segunda lo escapaba entero.
 *
 * ## Por qué está en un fichero aparte y no dentro de `MarkdownPreview`
 *
 * Porque **ningún test por pieza podía verlo**. `MarkdownRendererTest` comprobaba que el
 * renderizador funciona, y funcionaba; el fallo estaba en *quién lo llamaba*. Para que un test pueda
 * cubrir el cruce had que que el cruce **existiera** como código.
 *
 * ## La regla que este fichero hace cumplir
 *
 * Se genera **una vez** y se muestra lo generado. Quien llama, no vuelve a convertir: si vuelve a
 * pasar el documento por [MarkdownRenderer], el test `MarkdownPreviewContractTest` se pone rojo.
 */
object MarkdownPreview {

    /**
     * Markdown → documento HTML. Se llama **una sola vez por cambio de texto**, en un hilo aparte.
     */
    fun render(markdown: String, theme: PreviewTheme): String =
        MarkdownRenderer.toHtmlDocument(markdown, theme)

    /**
     * Lo que se carga en el `WebView`: el documento **tal cual**, sin volver a convertirlo.
     *
     * Es un `identity` a propósito. Existe para que el punto de paso sea explícito y comprobable: si
     * algún día alguien mete una conversión aquí, el nombre y el test lo delatan.
     */
    fun documentToLoad(renderedHtml: String): String = renderedHtml
}