package com.estrin217.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * La dirección que se abre no es la que la rodeaba (spec 018, RF-106).
 *
 * El texto legal trae su único enlace **metido en paréntesis**, en los nueve idiomas:
 * `(<dirección>)`. Un `URLSpan` es un texto con posición, no una dirección, así que si el detector arrastra el
 * paréntesis de cierre, al pulsarlo se abre una dirección que no existe. Y eso **no se puede comprobar en esta
 * plataforma**: `Linkify` y `Patterns` son framework de Android y aquí no hay runtime nativo de Robolectric.
 *
 * Por eso la recorte es una **función pura**, aquí probada carácter a carácter. Estas pruebas son las que
 * sostienen RF-106; lo único que dejan para un dispositivo es que el detector meta o no el paréntesis dentro.
 */
class DireccionParaAbrirTest {

    // ── El caso que la obliga: el paréntesis de la frase ──────────────────────────────────

    @Test
    fun `un parentesis de cierre sin abrir se quita`() {
        // El texto legal real, en los nueve idiomas.
        assertEquals(
            "https://github.com/ESTRIN217",
            direccionParaAbrir("https://github.com/ESTRIN217)")
        )
    }

    @Test
    fun `varios parentesis sueltos se quitan todos`() {
        assertEquals("https://ejemplo.com", direccionParaAbrir("https://ejemplo.com)))"))
    }

    // ── El caso que NO se puede romper: el paréntesis que sí es de la dirección ───────────

    @Test
    fun `un parentesis balanceado es parte de la direccion y se conserva`() {
        // Caso límite 6 de la spec 017: quitarlo abriría una dirección rota.
        assertEquals("https://ejemplo.com/a_(b)", direccionParaAbrir("https://ejemplo.com/a_(b)"))
    }

    @Test
    fun `el parentesis de una ruta anidada se conserva`() {
        assertEquals(
            "https://ejemplo.com/x_(y_1)/z",
            direccionParaAbrir("https://ejemplo.com/x_(y_1)/z")
        )
    }

    @Test
    fun `la ruta de wikipedia se conserva`() {
        assertEquals(
            "https://es.wikipedia.org/wiki/Anexo:Pel%C3%ADculas_(2010)",
            direccionParaAbrir("https://es.wikipedia.org/wiki/Anexo:Pel%C3%ADculas_(2010)")
        )
    }

    // ── El resto de la puntuación de la frase ─────────────────────────────────────────────

    @Test
    fun `la puntuacion final de la frase se quita`() {
        assertEquals("https://ejemplo.com", direccionParaAbrir("https://ejemplo.com."))
        assertEquals("https://ejemplo.com", direccionParaAbrir("https://ejemplo.com,"))
        assertEquals("https://ejemplo.com", direccionParaAbrir("https://ejemplo.com;"))
        assertEquals("https://ejemplo.com", direccionParaAbrir("https://ejemplo.com!"))
        assertEquals("https://ejemplo.com", direccionParaAbrir("https://ejemplo.com?"))
        assertEquals("https://ejemplo.com", direccionParaAbrir("""https://ejemplo.com" """.trim()))
    }

    @Test
    fun `varias puntuaciones seguidas se quitan todas`() {
        assertEquals("https://ejemplo.com", direccionParaAbrir("https://ejemplo.com.)!"))
    }

    @Test
    fun `los otros tres cierres se quitan cuando estan sueltos`() {
        assertEquals("https://ejemplo.com", direccionParaAbrir("https://ejemplo.com]"))
        assertEquals("https://ejemplo.com", direccionParaAbrir("https://ejemplo.com}"))
        assertEquals("https://ejemplo.com", direccionParaAbrir("https://ejemplo.com>"))
    }

    @Test
    fun `un parentesis de una llave de consulta tambien cuenta como pareja`() {
        // El `(` de la consulta abre, así que su `)` es de la dirección. La primera versión de esta prueba
        // decía lo contrario —«se quita»— con el mismo nombre, y la implementación tenía razón: quitarlo
        // abriría una consulta distinta de la que escribió quien la escribió.
        assertEquals(
            "https://ejemplo.com/buscar?q=(a)",
            direccionParaAbrir("https://ejemplo.com/buscar?q=(a)")
        )
    }

    @Test
    fun `un parentesis de sobra en una ruta con otro bien puesto se quita solo`() {
        // El de la ruta se conserva y el de sobra se va: es el caso que un «compara dos totales» no
        // distinguiría.
        assertEquals(
            "https://ejemplo.com/x_(y)/z",
            direccionParaAbrir("https://ejemplo.com/x_(y)/z)")
        )
    }

    // ── Lo que no hay que tocar ────────────────────────────────────────────────────────────

    @Test
    fun `una direccion sin nada que quitar no cambia`() {
        listOf(
            "https://ejemplo.com",
            "https://ejemplo.com/a/b/c",
            "https://ejemplo.com/a?b=c&d=e#f",
            "http://192.168.1.1:8080/x"
        ).forEach { direccion ->
            assertEquals(direccion, direccionParaAbrir(direccion))
        }
    }

    @Test
    fun `quitar puntuacion nunca deja nada que se parezca a una direccion`() {
        // Una cadena que es solo esquema y puntuación se recorta **del todo**, no a medias: dejar `https:` o
        // `https` daría algo que parece una dirección y no lo es, y una prueba que solo mirase «ha cambiado»
        // no lo vería.
        assertEquals("https", direccionParaAbrir("https:..."))
        assertEquals("https", direccionParaAbrir("https:)"))
        assertFalse(isWebLink(direccionParaAbrir("https:...")))
    }

    @Test
    fun `es idempotente`() {
        // Importa porque el recorte se aplica antes de `isWebLink` y el mismo texto puede pasar dos veces por
        // el camino. Una operación que no lo es dejaría `https://ejemplo.com` → `https://ejemplo.com` en la
        // primera y algo distinto en la segunda, y la segunda es la que se abriría.
        listOf(
            "https://github.com/ESTRIN217)",
            "https://ejemplo.com/a_(b)",
            "https://ejemplo.com.)!",
            "https://ejemplo.com"
        ).forEach { direccion ->
            val una = direccionParaAbrir(direccion)
            assertEquals(direccion, una, direccionParaAbrir(una))
        }
    }

    @Test
    fun `nunca alarga la direccion`() {
        // La propiedad de seguridad: si solo puede quitar, no puede convertir una dirección inocua en otra que
        // no lo sea. Un `javascript:` recortado sigue siendo un `javascript:`, no se vuelve web.
        val entradas = listOf(
            "javascript:alert(1)", "javascript:alert(1);", "intent:#Intent;end", "file:///sdcard/x",
            "content://media/1", "tel:600123456", "mailto:alguien@ejemplo.com", "data:text/html,<b>x"
        )
        entradas.forEach { direccion ->
            val recortada = direccionParaAbrir(direccion)
            assertTrue(
                "recortar '$direccion' no puede producir algo más largo: '$recortada'",
                recortada.length <= direccion.length
            )
            assertFalse(
                "recortar no puede volver web una dirección que no lo era: '$direccion' -> '$recortada'",
                !isWebLink(direccion) && isWebLink(recortada)
            )
        }
    }

    @Test
    fun `un esquema raro recortado sigue sin ser web`() {
        assertEquals("javascript:alert(1)", direccionParaAbrir("javascript:alert(1);"))
        assertFalse(isWebLink(direccionParaAbrir("javascript:alert(1);")))
    }

    @Test
    fun `una cadena vacia o solo puntuacion no lanza`() {
        assertEquals("", direccionParaAbrir(""))
        assertEquals("", direccionParaAbrir("..."))
        assertEquals("", direccionParaAbrir(")"))
    }
}