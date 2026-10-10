package com.estrin217.editordecodigo.ui

import com.estrin217.editordecodigo.syntax.SupportedLanguage

/**
 * Cómo se presenta el archivo activo.
 *
 * ## Por qué un `enum` y no dos booleanos
 *
 * Ya existía `isEditMode`, y funciona. Lo que **no** encaja en un booleano es que `SPLIT` y
 * `PREVIEW` no son «editar o no editar»: son **dos formas de presentar los dos lados a la vez**. Con
 * `isEditMode` + `isSplit` + `isPreview` habría que mantener un invariante que nadie va a
 * mantener: ¿qué pasa si `isSplit` e `isPreview` son verdad a la vez? La pregunta se responde sola
 * con un `enum`.
 */
enum class ViewMode {
    EDIT,
    VIEW,
    SPLIT,
    PREVIEW;

    /** ¿Se puede escribir en el editor? En `SPLIT` se ve pero no se escribe: el lado útil es el otro. */
    fun isEditable(): Boolean = this == EDIT

    /** ¿Hay una vista previa renderizada en pantalla? */
    fun showsPreview(): Boolean = this == SPLIT || this == PREVIEW
}

/**
 * Qué modos se ofrecen y cuál está activo, según el archivo.
 *
 * Toda la lógica de la spec 012 que decide modos vive aquí, **fuera del Composable** (RF-66): la
 * regla «Markdown tiene cuatro modos, el resto dos» queda en una función pura que se prueba sin
 * Android, en vez de repartida por `if` en un `Scaffold`.
 */
object EditorModes {

    /**
     * Los modos disponibles para un lenguaje.
     *
     * `SPLIT` y `PREVIEW` son exclusivos de Markdown porque los dos necesitan el renderizador. Cualquier
     * otro lenguaje los ofrecería con una vista previa que es el texto sin colorear dentro de un
     * HTML, que no es una vista previa: es ruido con un botón.
     */
    fun availableFor(language: SupportedLanguage): List<ViewMode> =
        if (supportsPreview(language)) ViewMode.entries
        else listOf(ViewMode.EDIT, ViewMode.VIEW)

    fun supportsPreview(language: SupportedLanguage): Boolean = language == SupportedLanguage.MARKDOWN

    /**
     * Deja el modo en uno que el lenguaje admite. Sin esto, un archivo Markdown en `PREVIEW` que
     * pasa a ser `.kt` dejaría al usuario mirando una vista previa de un archivo que no la tiene
     * (caso límite 7 de la spec).
     */
    fun sanitized(mode: ViewMode, language: SupportedLanguage): ViewMode =
        if (mode in availableFor(language)) mode else ViewMode.EDIT

    fun forFiles(): ViewModeRegistry = ViewModeRegistry()

    /**
     * Si la acción «Enviar a notas» debe aparecer (RF-76).
     *
     * La condición vive aquí y no dentro del `if` del menú para que sea comprobable: son tres
     * condiciones —hay anfitrión, el lenguaje es Markdown, el archivo existe— y la última se
     * desvía de la regla de la vista previa, que es lo que hace que la condición no sea obvia.
     *
     * Comparte regla con [supportsPreview] a propósito: si un archivo no tiene vista previa, tampoco
     * tiene sentido mandarlo a notas, porque lo que se manda es el mismo Markdown que se vería ahí.
     */
    fun canSendToNotes(hasHost: Boolean, language: SupportedLanguage, hasFile: Boolean): Boolean =
        hasHost && hasFile && supportsPreview(language)
}

/**
 * Qué panes componen el área de edición, ya resuelto desde el modo.
 *
 * Existe para que el `WebView` —que aquí no se puede probar— no arrastre la decisión: **qué hay que
 * componer sí queda bajo prueba**, aunque el pintarlo no. La parte no comprobable de la vista previa
 * es el `WebView` en sí; este objeto es la decisión que lo rodea, y esa se prueba entera.
 *
 * @property showsEditor si el editor se compone siquiera. En `PREVIEW` **no**: no está oculto, no
 *   está painted con alfa cero, es que no existe, y así el usuario no puede escribir en un texto
 *   que no ve (RF-68).
 */
data class PreviewLayout(
    val mode: ViewMode,
    val showsEditor: Boolean,
    val showsPreview: Boolean,
    val showsDivider: Boolean,
    val isEditable: Boolean
) {
    companion object {
        /** Peso de cada mitad. Iguales a propósito: el divisor no se arrastra (RF-67). */
        const val EDITOR_WEIGHT = 1f
        const val PREVIEW_WEIGHT = 1f

        /**
         * La misma proporción que usa `CodeEditorView` para su `lineHeight`. Si el CSS de la vista
         * previa no coincide, las dos mitades se desalinean al desplazar: la línea 3 del Markdown
         * queda a distinta altura que su equivalente en el HTML.
         */
        const val LINE_HEIGHT_RATIO = 1.55f

        fun forMode(mode: ViewMode): PreviewLayout = when (mode) {
            ViewMode.EDIT -> PreviewLayout(mode, showsEditor = true, showsPreview = false, showsDivider = false, isEditable = true)
            ViewMode.VIEW -> PreviewLayout(mode, showsEditor = true, showsPreview = false, showsDivider = false, isEditable = false)
            ViewMode.SPLIT -> PreviewLayout(mode, showsEditor = true, showsPreview = true, showsDivider = true, isEditable = false)
            ViewMode.PREVIEW -> PreviewLayout(mode, showsEditor = false, showsPreview = true, showsDivider = false, isEditable = false)
        }
    }
}

/**
 * El modo que tenía cada archivo.
 *
 * Vive en memoria y **no** se persiste: al reabrir la app todo vuelve a `EDIT`. Es una decisión
 * deliberada —persistirlo tocaría preferencias por una comodidad pequeña—, pero hacer que el modo
 * viaje **con su archivo** dentro de la sesión evita la molestia de perderlo al cambiar de pestaña.
 */
class ViewModeRegistry {
    private val modes = mutableMapOf<Long, ViewMode>()

    fun record(fileId: Long, mode: ViewMode) {
        modes[fileId] = mode
    }

    fun modeOf(fileId: Long): ViewMode = modes[fileId] ?: ViewMode.EDIT

    fun forget(fileId: Long) {
        modes.remove(fileId)
    }

    fun clear() {
        modes.clear()
    }
}