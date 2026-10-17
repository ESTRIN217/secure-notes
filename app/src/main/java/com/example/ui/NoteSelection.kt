package com.example.ui

/**
 * Reglas de la selección múltiple de la lista de notas (spec 026).
 *
 * Pieza **pura**: no importa `android` ni `androidx`, para que la decisión se pruebe sin Robolectric, que no se
 * ejecuta en esta plataforma (RNF-107). La condición «hay selección activa» es una **sola** y la comparten la
 * barra de acciones por lotes, el toque, la pulsación larga y el botón de atrás (RF-179): así las cuatro no
 * pueden discrepar, que es justo el defecto que motiva la spec.
 */
object NoteSelection {

    /** RF-179: hay modo selección mientras haya al menos una nota seleccionada. */
    fun isActive(selected: Set<Int>): Boolean = selected.isNotEmpty()

    /**
     * RF-173, RF-175: alterna una nota —la quita si estaba, la añade si no—. El toque y la pulsación larga usan
     * esta misma regla; quitar la última nota deja la selección vacía y cierra el modo (RF-176).
     */
    fun toggle(selected: Set<Int>, noteId: Int): Set<Int> =
        if (noteId in selected) selected - noteId else selected + noteId
}
