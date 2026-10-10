package com.estrin217.editordecodigo

import com.estrin217.editordecodigo.ui.EditorModes
import com.estrin217.editordecodigo.ui.ViewMode
import com.estrin217.editordecodigo.syntax.SupportedLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Los modos del editor y cuáles se ofrecen (spec 012, T6).
 *
 * La regla «Markdown tiene cuatro modos, el resto dos» vive en una función pura para que se pueda
 * comprobar aquí, y no repartida por `if` dentro de un Composable.
 */
class EditorViewModeTest {

    @Test
    fun `markdown ofrece los cuatro modos`() {
        assertEquals(ViewMode.entries, EditorModes.availableFor(SupportedLanguage.MARKDOWN))
        assertEquals(4, EditorModes.availableFor(SupportedLanguage.MARKDOWN).size)
    }

    @Test
    fun `un lenguaje que no es markdown ofrece solo editar y ver`() {
        listOf(
            SupportedLanguage.KOTLIN, SupportedLanguage.JSON, SupportedLanguage.PLAIN_TEXT,
            SupportedLanguage.YAML, SupportedLanguage.BASH, SupportedLanguage.HTML,
            SupportedLanguage.CSS, SupportedLanguage.RUST
        ).forEach { language ->
            assertEquals(
                "el lenguaje ${language.displayName} no debería ofrecer más modos",
                listOf(ViewMode.EDIT, ViewMode.VIEW),
                EditorModes.availableFor(language)
            )
        }
    }

    @Test
    fun `split y preview implican no ser editable`() {
        // En vista previa no se compone el editor, y en mitad y mitad se compone pero sin escribir:
        // si alguno de los dos fuera editable, escribiría en un texto que no se ve.
        assertFalse(ViewMode.PREVIEW.isEditable())
        assertFalse(ViewMode.SPLIT.isEditable())
        assertTrue(ViewMode.EDIT.isEditable())
        assertFalse(ViewMode.VIEW.isEditable())
    }

    @Test
    fun `solo split y preview enseñan la vista previa`() {
        assertTrue(ViewMode.SPLIT.showsPreview())
        assertTrue(ViewMode.PREVIEW.showsPreview())
        assertFalse(ViewMode.EDIT.showsPreview())
        assertFalse(ViewMode.VIEW.showsPreview())
    }

    @Test
    fun `split y preview solo aplican a markdown`() {
        assertTrue(EditorModes.supportsPreview(SupportedLanguage.MARKDOWN))
        assertFalse(EditorModes.supportsPreview(SupportedLanguage.PLAIN_TEXT))
        assertFalse(EditorModes.supportsPreview(SupportedLanguage.KOTLIN))
    }

    @Test
    fun `un modo no disponible se reemplaza por editar`() {
        assertEquals(ViewMode.EDIT, EditorModes.sanitized(ViewMode.PREVIEW, SupportedLanguage.KOTLIN))
        assertEquals(ViewMode.EDIT, EditorModes.sanitized(ViewMode.SPLIT, SupportedLanguage.PLAIN_TEXT))
        assertEquals(ViewMode.PREVIEW, EditorModes.sanitized(ViewMode.PREVIEW, SupportedLanguage.MARKDOWN))
        assertEquals(ViewMode.SPLIT, EditorModes.sanitized(ViewMode.SPLIT, SupportedLanguage.MARKDOWN))
        assertEquals(ViewMode.VIEW, EditorModes.sanitized(ViewMode.VIEW, SupportedLanguage.KOTLIN))
    }

    @Test
    fun `enviar a notas necesita anfitrion, markdown y archivo`() {
        // Las tres condiciones: sin anfitrión la librería no puede cumplir la acción, sin Markdown
        // el envío no tiene sentido, y sin archivo no hay nada que mandar (RF-76).
        assertTrue(EditorModes.canSendToNotes(true, SupportedLanguage.MARKDOWN, true))
        assertFalse(EditorModes.canSendToNotes(false, SupportedLanguage.MARKDOWN, true))
        assertFalse(EditorModes.canSendToNotes(true, SupportedLanguage.KOTLIN, true))
    }

    @Test
    fun `enviar a notas comparte regla con la vista previa`() {
        // Es la misma condición de lenguaje: lo que se manda es el Markdown que se vería en la
        // vista previa, así que un archivo sin vista previa no tiene a dónde ir.
        SupportedLanguage.entries.forEach { language ->
            assertEquals(
                "el lenguaje ${language.displayName} discrepa entre las dos reglas",
                EditorModes.supportsPreview(language),
                EditorModes.canSendToNotes(true, language, true)
            )
        }
    }

    @Test
    fun `el registro por archivo recuerda el modo de cada uno`() {
        val modos = EditorModes.forFiles()
        modos.record(fileId = 7L, ViewMode.PREVIEW)
        modos.record(fileId = 9L, ViewMode.EDIT)

        assertEquals(ViewMode.PREVIEW, modos.modeOf(7L))
        assertEquals(ViewMode.EDIT, modos.modeOf(9L))
        // Un archivo sin registro arranca en EDIT, no hereda el del anterior.
        assertEquals(ViewMode.EDIT, modos.modeOf(11L))
    }

    @Test
    fun `el registro por archivo sobrevive a un cambio de ida y vuelta`() {
        val modos = EditorModes.forFiles()
        modos.record(7L, ViewMode.SPLIT)
        modos.record(9L, ViewMode.EDIT)

        assertEquals(ViewMode.SPLIT, modos.modeOf(7L))
        assertEquals(ViewMode.EDIT, modos.modeOf(9L))
    }

    @Test
    fun `un archivo al que se le quita el registro vuelve al inicio`() {
        val modos = EditorModes.forFiles()
        modos.record(7L, ViewMode.PREVIEW)
        modos.forget(7L)
        assertEquals(ViewMode.EDIT, modos.modeOf(7L))
    }
}