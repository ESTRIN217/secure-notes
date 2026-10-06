package com.example

import com.example.ui.chat.AttachmentSource
import com.example.ui.chat.attachmentSourceFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pruebas de L5 (qué acción corresponde a cada opción de la hoja de adjuntos).
 *
 * La hoja tiene **exactamente dos** opciones —Nota y Archivo— porque los adjuntos llegan al modelo
 * serializados como texto y el motor en el dispositivo es solo texto: no hay foto ni cámara que
 * ofrecer. Aquí solo se decide el **par option→acción**; los subtítulos son claves de T6 y los **tipos
 * MIME se quedan escritos en `AiChatScreen.kt`** (decisión D-4 del plan), así que en este archivo no
 * hay ni recursos ni `R`.
 *
 * Los `id` son claves estables escritas en el código por la propia hoja, **no** texto de persona: por
 * eso la comparación es exacta y un `id` desconocido devuelve `null` en vez de lanzar excepción. Un
 * `null` es la señal de «esta opción no existe», y quien cablea en T9 decide con un `when` qué hacer:
 * si aparece un `id` que la hoja no ofrece, no hay selector que abrir.
 */
class ChatAttachmentSheetOptionTest {

    @Test
    fun `the note option maps to the note source`() {
        assertEquals(AttachmentSource.NOTE, attachmentSourceFor("note"))
    }

    @Test
    fun `the file option maps to the file source`() {
        assertEquals(AttachmentSource.FILE, attachmentSourceFor("file"))
    }

    // RF-34: la hoja ofrece exactamente dos opciones y nada más.

    @Test
    fun `the sheet offers exactly the two declared sources`() {
        assertEquals(listOf(AttachmentSource.NOTE, AttachmentSource.FILE), AttachmentSource.values().toList())
    }

    // Un id desconocido no rompe la pantalla: se devuelve null en lugar de lanzar excepción, porque
    // null es la señal de que la opción no existe. No hay una tercera vía ni una fuente por defecto.

    @Test
    fun `an unknown id returns null without failing`() {
        assertNull(attachmentSourceFor("camera"))
    }

    @Test
    fun `an empty id returns null without failing`() {
        assertNull(attachmentSourceFor(""))
    }

    // Los ids son claves exactas del código, no texto de persona: mayúsculas, espacios y acentos no
    // se «arreglan», se rechazan. Una opción que llegue con espacios es una opción que no existe.

    @Test
    fun `an id that only differs in case or spacing is not an option`() {
        assertNull(attachmentSourceFor("Note"))
        assertNull(attachmentSourceFor("FILE"))
        assertNull(attachmentSourceFor(" note"))
        assertNull(attachmentSourceFor("note "))
    }

    // Repetir el mapeo muchas veces no puede dar resultados distintos: es una función pura.

    @Test
    fun `the mapping is stable across repetitions`() {
        for (attempt in 0 until 50) {
            assertEquals("note on attempt $attempt", AttachmentSource.NOTE, attachmentSourceFor("note"))
            assertEquals("file on attempt $attempt", AttachmentSource.FILE, attachmentSourceFor("file"))
        }
    }
}