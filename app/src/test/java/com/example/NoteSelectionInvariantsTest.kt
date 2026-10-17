package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Invariantes de la spec 026: la lista de notas usa **una sola** regla de selección y el atrás cierra el modo.
 *
 * Son pruebas **puras** que leen el texto de los ficheros, con las dos reglas del proyecto (ver `MEMORY.md`):
 * cada recorrido **quita comentarios antes de buscar** —para que el propio comentario que explica la regla no
 * dé verde— y **se autocomprueba**, para que un recorrido vacío no pase por vacío.
 *
 * Lo que se comprueba aquí es el **cableado**, que es justo lo que las pruebas puras de la decisión
 * (`NoteSelectionTest`) no ven. El gesto real en pantalla no se puede probar sin Robolectric, que no se ejecuta
 * en esta plataforma: va al guion de dispositivo (`plan.md`).
 */
class NoteSelectionInvariantsTest {

    private companion object {
        const val PIEZA_PURA = "src/main/java/com/example/ui/NoteSelection.kt"
        const val PANTALLA = "src/main/java/com/example/ui/MainListScreen.kt"

        /** Piso de cualquier recorrido: si baja de aquí, el recorrido está roto, no el código. */
        const val MINIMO_DE_LINEAS = 12

        private val COMENTARIOS_KOTLIN = Regex("/\\*.*?\\*/|//[^\\n]*", RegexOption.DOT_MATCHES_ALL)
    }

    /** Lee **quitando comentarios**, para que un KDoc que explica por qué algo **no** está no dé verde. */
    private fun leerCodigo(ruta: String, etiqueta: String): String {
        val fichero = File(ruta)
        assertTrue("no existe $etiqueta ($ruta)", fichero.exists())
        val texto = fichero.readText()
        assertTrue("$etiqueta ($ruta) solo tiene ${texto.lines().size} líneas", texto.lines().size >= MINIMO_DE_LINEAS)
        return texto.replace(COMENTARIOS_KOTLIN, "")
    }

    private fun entre(texto: String, inicio: String, fin: String): String {
        val desde = texto.indexOf(inicio)
        assertTrue("no está «$inicio» en el recorrido", desde >= 0)
        val hasta = texto.indexOf(fin, desde)
        assertTrue("no está «$fin» después de «$inicio»", hasta > desde)
        return texto.substring(desde, hasta)
    }

    private fun veces(texto: String, patron: String): Int = Regex(patron).findAll(texto).count()

    @Test
    fun `la pieza de seleccion es pura`() {
        val codigo = leerCodigo(PIEZA_PURA, "la pieza de selección")

        assertFalse("la pieza importa android", codigo.contains("import android"))
        assertFalse("la pieza importa androidx", codigo.contains("import androidx"))
        assertTrue("no está la condición de selección activa", codigo.contains("fun isActive(selected: Set<Int>)"))
        assertTrue("no está la alternancia", codigo.contains("fun toggle(selected: Set<Int>, noteId: Int)"))
    }

    @Test
    fun `la pantalla no reescribe la condicion de seleccion`() {
        val codigo = leerCodigo(PANTALLA, "la pantalla")

        assertFalse(
            "la pantalla vuelve a comprobar si hay selección",
            codigo.contains("selectedNoteIds.isNotEmpty()")
        )
        assertFalse(
            "la pantalla vuelve a comprobar si no hay selección",
            codigo.contains("selectedNoteIds.isEmpty()")
        )
    }

    @Test
    fun `el toque y la pulsacion larga alternan en las dos listas`() {
        val codigo = leerCodigo(PANTALLA, "la pantalla")
        // El recorte va de la primera llamada a NoteCardItem hasta la primera línea que ya no es de las
        // listas, para no incluir las acciones por lotes (que sí limpian la selección al terminar y deben
        // seguir haciéndolo).
        val tarjetas = entre(codigo, "NoteCardItem(", "if (showCreateTagDialog) {")

        // Dos llamadas a NoteCardItem (cuadrícula y lista) × dos gestos = cuatro alternancias.
        assertEquals("no se alterna en los cuatro gestos", 4, veces(tarjetas, "NoteSelection\\.toggle\\("))
        // Solo los dos toques preguntan si hay modo selección.
        assertEquals("el toque no pregunta por el modo", 2, veces(tarjetas, "NoteSelection\\.isActive\\("))
        assertFalse(
            "algún gesto de la tarjeta sigue vaciando la selección",
            tarjetas.contains("selectedNoteIds = emptySet()")
        )
    }

    @Test
    fun `atras cierra el modo antes de navegar`() {
        val codigo = leerCodigo(PANTALLA, "la pantalla")
        val atras = entre(codigo, "BackHandler(", "val sortedNotes")

        assertTrue("el atrás no se habilita con la selección activa", atras.contains("enabled = NoteSelection.isActive(selectedNoteIds)"))
        assertTrue("el atrás no pregunta por el modo", atras.contains("if (NoteSelection.isActive(selectedNoteIds))"))

        val limpia = atras.indexOf("selectedNoteIds = emptySet()")
        val navega = atras.indexOf("viewModel.currentSection.value = com.example.data.model.NavigationSection.HOME")
        assertTrue("el atrás no cierra el modo", limpia >= 0)
        assertTrue("el atrás no conserva la navegación", navega > limpia)
    }
}
