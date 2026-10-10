package com.estrin217.editordecodigo

import com.estrin217.editordecodigo.ui.PreviewLayout
import com.estrin217.editordecodigo.ui.ViewMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Qué se compone a cada lado del área de edición (spec 012, T7).
 *
 * La decisión de qué panes existen se prueba aquí; el `WebView` en sí no se puede ejecutar sin
 * dispositivo, así que esto es lo que queda comprobado de la vista previa.
 */
class PreviewLayoutTest {

    @Test
    fun `en editar solo hay editor`() {
        val layout = PreviewLayout.forMode(ViewMode.EDIT)
        assertTrue(layout.showsEditor)
        assertFalse(layout.showsPreview)
        assertFalse(layout.showsDivider)
        assertTrue(layout.isEditable)
    }

    @Test
    fun `en ver solo hay editor y no se puede escribir`() {
        val layout = PreviewLayout.forMode(ViewMode.VIEW)
        assertTrue(layout.showsEditor)
        assertFalse(layout.showsPreview)
        assertFalse(layout.isEditable)
    }

    @Test
    fun `en mitad y mitad hay los tres y el editor no se escribe`() {
        val layout = PreviewLayout.forMode(ViewMode.SPLIT)
        assertTrue(layout.showsEditor)
        assertTrue(layout.showsPreview)
        assertTrue(layout.showsDivider)
        // Escribir en el lado izquierdo no tiene sentido cuando el que importa es el derecho.
        assertFalse(layout.isEditable)
    }

    @Test
    fun `en vista previa no se compone el editor`() {
        val layout = PreviewLayout.forMode(ViewMode.PREVIEW)
        assertFalse(layout.showsEditor)
        assertTrue(layout.showsPreview)
        assertFalse(layout.showsDivider)
        assertFalse(layout.isEditable)
    }

    @Test
    fun `las dos mitades pesan lo mismo`() {
        // Mitades fijas: los dos lados reciben el mismo peso y no hay proporción guardada.
        assertEquals(PreviewLayout.EDITOR_WEIGHT, PreviewLayout.PREVIEW_WEIGHT, 0f)
        assertEquals(1f, PreviewLayout.EDITOR_WEIGHT, 0f)
        assertEquals(1f, PreviewLayout.PREVIEW_WEIGHT, 0f)
    }

    @Test
    fun `la altura de linea del editor y la del css coinciden`() {
        // El `CodeEditorView` usa fontSize * 1.55 para su lineHeight; si el CSS de la vista previa
        // usara otra proporción, las dos mitades se desalinearían verticalmente al desplazar.
        assertEquals(1.55f, PreviewLayout.LINE_HEIGHT_RATIO, 0.001f)
    }

    @Test
    fun `el layout recuerda de que modo viene`() {
        // El modo viaja en el layout para que el Composable no tenga que volver a mirar el enum:
        // decide componer con `showsEditor`/`showsPreview`, no comparando modos a mano.
        ViewMode.entries.forEach { mode ->
            assertEquals(mode, PreviewLayout.forMode(mode).mode)
        }
    }
}
