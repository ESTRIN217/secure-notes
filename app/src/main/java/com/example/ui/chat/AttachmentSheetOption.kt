package com.example.ui.chat

/**
 * L5 — qué acción corresponde a cada opción de la hoja de adjuntos (RF-34, RF-35).
 *
 * La hoja tiene **exactamente dos** opciones, Nota y Archivo, y nada más: los adjuntos llegan al
 * modelo serializados como texto y el motor en el dispositivo es solo de texto, así que no hay foto
 * ni cámara que ofrecer. Cada `id` se corresponde con **una** fuente, y cada fuente con **un** `when`
 * en la hoja (T9): Nota cierra la hoja y abre el selector de notas, Archivo la cierra y abre el
 * selector de archivos.
 *
 * **Los tipos MIME no viven aquí** (decisión D-4 del plan): se quedan escritos en `AiChatScreen.kt`
 * como están hoy, porque el criterio aprobado de RF-35 los busca literalmente en ese archivo y
 * duplicar la constante en dos sitios sería peor que la tensión.
 *
 * Este archivo no importa `androidx.compose`, no accede a `R` y no lee el reloj: los subtítulos son
 * claves de T6 y los resuelve el Composable con `stringResource`.
 */
enum class AttachmentSource { NOTE, FILE }

/** Id estable de la opción «Nota», tal como lo escribe la hoja. */
const val ATTACHMENT_NOTE_ID = "note"

/** Id estable de la opción «Archivo», tal como lo escribe la hoja. */
const val ATTACHMENT_FILE_ID = "file"

/**
 * Resuelve la fuente de una opción por su `id`, o `null` si ese `id` no es una de las dos opciones.
 *
 * Se elige `null` y no una excepción ni una fuente por defecto: `null` es la señal de «esta opción no
 * existe», y quien cablea en T9 lo decide con un `when` —sin abrir selector, sin hoja colgada— en
 * lugar de tener que capturar una excepción en un callback de `onClick`. La comparación es **exacta**
 * porque los `id` son claves escritas en el código por la propia hoja, no texto de persona: `"Note"`
 * o `" note"` no son la opción Nota, son otro `id`.
 */
fun attachmentSourceFor(id: String): AttachmentSource? = when (id) {
    ATTACHMENT_NOTE_ID -> AttachmentSource.NOTE
    ATTACHMENT_FILE_ID -> AttachmentSource.FILE
    else -> null
}