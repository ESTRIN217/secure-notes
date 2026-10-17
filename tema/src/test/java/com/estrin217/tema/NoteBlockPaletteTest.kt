package com.estrin217.tema

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * La paleta del bloque de nota pierde la superficie y conserva los acentos del tema (spec 025, RF-167).
 *
 * Son doce acentos y siete superficies, más `id` e `isDark`. El campo `name` se fue con el selector del editor:
 * los nombres visibles de los temas salen de `strings.xml`, no de aquí. El error fácil es poner `Color.Unspecified` de más —y que el bloque deje
 * de pintarse con el tema— o de menos —y que se vea distinto según por dónde se mire—, así que la prueba compara
 * los dos lados: lo que tiene que seguir y lo que tiene que irse.
 */
class NoteBlockPaletteTest {

    @Test
    fun `las siete superficies se van sin especificar`() {
        SyntaxThemes.all.forEach { original ->
            val bloque = original.forNoteBlock()

            listOf(
                "background" to bloque.background,
                "gutterBackground" to bloque.gutterBackground,
                "activeLineBackground" to bloque.activeLineBackground,
                "text" to bloque.text,
                "lineNumber" to bloque.lineNumber,
                "activeLineNumber" to bloque.activeLineNumber,
                "searchMatchBackground" to bloque.searchMatchBackground,
            ).forEach { (campo, color) ->
                assertEquals("${original.id}.$campo", Color.Unspecified, color)
            }
        }
    }

    @Test
    fun `los doce acentos no se tocan`() {
        SyntaxThemes.all.forEach { original ->
            val bloque = original.forNoteBlock()

            listOf(
                "keyword" to bloque.keyword,
                "string" to bloque.string,
                "comment" to bloque.comment,
                "number" to bloque.number,
                "function" to bloque.function,
                "variable" to bloque.variable,
                "propertyKey" to bloque.propertyKey,
                "operator" to bloque.operator,
                "punctuation" to bloque.punctuation,
                "booleanNull" to bloque.booleanNull,
                "annotation" to bloque.annotation,
                "type" to bloque.type,
            ).forEach { (campo, color) ->
                assertEquals("${original.id}.$campo no debería cambiar", original.acento(campo), color)
            }
        }
    }

    @Test
    fun `la paleta del bloque no pinte su propio fondo`() {
        // Comprobación tonta que ha resultado útil: si `forNoteBlock()` devolviera la paleta tal cual, el bloque
        // empezaría a pintar fondo propio. Comparar la paleta entera lo dice en una línea.
        SyntaxThemes.all.forEach { original ->
            assertNotEquals(
                "${original.id} seguiría pintando su propio fondo",
                original,
                original.forNoteBlock(),
            )
        }
    }

    @Test
    fun `la paleta no ha crecido ni encogido`() {
        // Si alguien añade un campo a [SyntaxTheme], las dos listas de arriba dejan de cubrirla y aquí se ve. Se
        // cuenta por reflexión —`javaClass`, que en una prueba de JVM no necesita `kotlin-reflect`— porque no hay
        // forma de recorrer un `data class` sin ella.
        // El compilador de Compose añade un `\$stable` estático: no es un campo de la paleta.
        val campos = SyntaxTheme::class.java.declaredFields
            .filter { !it.isSynthetic && !it.name.startsWith("\$") }
            .map { it.name }

        assertEquals("la paleta debería seguir teniendo 21 campos: 19 colores, id e isDark", 21, campos.size)
        assertEquals("los doce acentos tienen que existir", emptySet<String>(), ACENTOS - campos.toSet())
        assertEquals("las siete superficies tienen que existir", emptySet<String>(), SUPERFICIES - campos.toSet())
    }

    @Test
    fun `un acento y una superficie no se confunden`() {
        val bloque = SyntaxThemes.VSCodeDark.forNoteBlock()

        assertNotEquals("un acento nunca va sin especificar", Color.Unspecified, bloque.keyword)
        assertEquals("una superficie siempre va sin especificar", Color.Unspecified, bloque.background)
    }

    private companion object {
        val ACENTOS = setOf(
            "keyword", "string", "comment", "number", "function", "variable",
            "propertyKey", "operator", "punctuation", "booleanNull", "annotation", "type",
        )
        val SUPERFICIES = setOf(
            "background", "gutterBackground", "activeLineBackground", "text",
            "lineNumber", "activeLineNumber", "searchMatchBackground",
        )
    }

    private fun SyntaxTheme.acento(campo: String): Color = when (campo) {
        "keyword" -> keyword
        "string" -> string
        "comment" -> comment
        "number" -> number
        "function" -> function
        "variable" -> variable
        "propertyKey" -> propertyKey
        "operator" -> operator
        "punctuation" -> punctuation
        "booleanNull" -> booleanNull
        "annotation" -> annotation
        "type" -> type
        else -> error("'$campo' no es un acento, y aquí solo se comparan acentos")
    }
}