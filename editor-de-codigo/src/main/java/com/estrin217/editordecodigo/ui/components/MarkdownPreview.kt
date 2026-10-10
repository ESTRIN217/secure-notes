package com.estrin217.editordecodigo.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.estrin217.editordecodigo.markdown.MarkdownPreview
import com.estrin217.editordecodigo.markdown.PreviewTheme
import com.estrin217.editordecodigo.syntax.SyntaxTheme
import java.io.File

/**
 * La vista previa del Markdown, en un `WebView`.
 *
 * Modelado sobre `InAppYouTubePlayer` de `:visor-media`, que es el otro `WebView` del repositorio y
 * ya resolvió los mismos tres problemas: cargar datos, abrir enlaces fuera y **no filtrar**.
 *
 * ## Los ajustes, y por qué cada uno
 *
 * | Ajuste | Valor | Por qué |
 * |---|---|---|
 * | `javaScriptEnabled` | `false` | Una vista previa no necesita JS, y sin él no hay superficie de XSS |
 * | `domStorageEnabled` | `false` | No se guarda nada entre recargas |
 * | `allowContentAccess` | `false` | El contenido es el Markdown del usuario, no un `content://` |
 * | `allowFileAccess` | `true` | **Necesario**: sin él no salen las imágenes locales relativas (RF-73) |
 * | `allowFileAccessFromFileURLs` | `false` | Sin esto, una página `file://` puede leer otras |
 *
 * `allowFileAccess` y `allowFileAccessFromFileURLs` seemingly se contradicen, y no: el primero
 * deja **leer imágenes del disco**, el segundo impide que el HTML **lea otros ficheros por script**.
 *
 * ## El `WebView` no sobrevive a quien lo crea
 *
 * Se destruye en `onDispose` (RNF-30). Sin eso, cada cambio de modo deja un `WebView` huérfano
 * consuming memoria, y `destroy()` es lo único que lo libera del todo.
 */
/**
 * Pinta un documento HTML ya generado en el `WebView`.
 *
 * ## ⚠️ Este componente NO convierte nada
 *
 * [documentHtml] tiene que ser **el documento ya renderizado**, no el Markdown: convertirlo aquí fue
 * un bug real que salió en dispositivo, donde la vista previa salía con el HTML entero escapado
 * (`<!DOCTYPE html>` y `<h2>` a la vista) porque el documento pasaba por commonmark **dos veces**: la
 * segunda lo escapaba entero. Aquí solo se pinta.
 *
 * Convertir es trabajo de [MarkdownRenderer], que es pura y se prueba sin Android, y de quien lo
 * llama, que lo hace en un hilo aparte para no bloquear la escritura (RF-75). Por eso este
 * componente **no necesita `SyntaxTheme`**: los colores ya vienen dentro del documento.
 */
@Composable
fun MarkdownPreview(
    documentHtml: String,
    basePath: String?,
    modifier: Modifier = Modifier
) {
    // La referencia vive fuera del árbol de composición: `destroy()` tiene que ocurrir **después** de
    // que la vista sale de él, y el `onRelease` de `AndroidView` se dispara con la vista aún viva.
    // Es el mismo orden que usa `InAppYouTubePlayer` en `:visor-media` (RNF-30).
    var webView by remember { mutableStateOf<WebView?>(null) }
    DisposableEffect(Unit) {
        onDispose {
            webView?.dispose()
            webView = null
        }
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                configure()
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                webView = this
            }
        },
        update = { view -> view.loadDocument(MarkdownPreview.documentToLoad(documentHtml), basePath) },
        modifier = modifier
    )
}

/**
 * Ajustes de seguridad. Va aparte para que se lean de un vistazo: es la lista de lo que este
 * `WebView` **no** hace, y es lo que impide que la vista previa sea una vía de salida.
 *
 * Los dos avisos de `Deprecated` que da el compilador son **intencionados**:
 * `allowFileAccessFromFileURLs` y `allowUniversalAccessFromFileURLs` están obsoletos desde que
 * `WebSettings.setAllowFileAccess` es `false` por defecto en API 30+, pero ponerlos a `false`
 * explícitamente sigue siendo la forma de dejar constancia de que la decisión es deliberada y no
 * un olvido de la plataforma: es la línea que evita que un Markdown pueda pedir otros ficheros
 * del disco desde una página `file://`.
 */
@SuppressLint("SetJavaScriptEnabled")
private fun WebView.configure() {
    settings.apply {
        javaScriptEnabled = false
        domStorageEnabled = false
        allowContentAccess = false
        allowFileAccess = true
        allowFileAccessFromFileURLs = false
        allowUniversalAccessFromFileURLs = false
        cacheMode = WebSettings.LOAD_DEFAULT
        setSupportMultipleWindows(false)
        mediaPlaybackRequiresUserGesture = true
        loadsImagesAutomatically = true
    }
    isVerticalScrollBarEnabled = true
    setBackgroundColor(AndroidColor.TRANSPARENT)
    webViewClient = ExternalLinksOnlyClient()
}

/**
 * Los enlaces salen fuera con un `Intent`; cualquier otra cosa se ignora.
 *
 * Se ignoran a propósito: un `javascript:` o un esquema raro dentro del Markdown no debe abrir nada.
 */
private class ExternalLinksOnlyClient : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url ?: return true
        return view != null && openExternally(view.context, url.toString())
    }

    @Deprecated("La firma de String sigue siendo abstracta aunque minSdk sea 33")
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean =
        url != null && view != null && openExternally(view.context, url)
}

/**
 * Abre el enlace fuera si el esquema es de los tres permitidos, y devuelve siempre `true`: el
 * `WebView` no debe navegar nunca, ni siquiera a un destino que se acaba de rechazar.
 */
private fun openExternally(context: Context, url: String): Boolean {
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return true
    val scheme = uri.scheme
    if (scheme != "http" && scheme != "https" && scheme != "mailto") return true
    return runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    }.getOrDefault(true)
}

/**
 * Carga el documento con una base **local**, que es lo que permite resolver las imágenes relativas
 * del Markdown (`imagenes/flujo.png`).
 *
 * Sin archivo en disco no hay carpeta que ofrecer, y se usa una base `https://` inaccesible: el
 * documento se pinta igual y ninguna ruta relativa sale a la red.
 */
private fun WebView.loadDocument(document: String, basePath: String?) {
    val base = basePath?.let { "file://${File(it).parent ?: it}" } ?: "https://preview.invalid/"
    loadDataWithBaseURL(base, document, "text/html", "UTF-8", null)
}

/** `stopLoading` antes de `destroy()`: sin eso, el `WebView` puede volver a pintar al destruirse. */
private fun WebView.dispose() {
    stopLoading()
    loadUrl("about:blank")
    destroy()
}
