package com.estrin217.tema

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Las cuatro paletas del editor de código llegaron a `:tema` **sin cambiar de valor** (spec 025, RF-159,
 * RNF-103).
 *
 * Es una mudanza de código, y una mudanza no puede cambiar lo que se muda. Estas pruebas escriben los 22 valores
 * de cada paleta para que, si alguien «limpia» un color, da por hecho que un tema se puede retocar aquí o
 * arrastra un cambio de la spec 022, se entere en la prueba y no en un dispositivo.
 */
class ThemeCatalogPalettesTest {

    @Test
    fun `la paleta de VS Code Dark Modern sigue siendo la de siempre`() {
        val tema = SyntaxThemes.VSCodeDark

        assertEquals("id", "vscode_dark", tema.id)
        assertEquals("background", 0xFF1F1F1F.toInt(), tema.background.argb())
        assertEquals("gutterBackground", 0xFF181818.toInt(), tema.gutterBackground.argb())
        assertEquals("activeLineBackground", 0xFF282828.toInt(), tema.activeLineBackground.argb())
        assertEquals("text", 0xFFCCCCCC.toInt(), tema.text.argb())
        assertEquals("lineNumber", 0xFF6E7681.toInt(), tema.lineNumber.argb())
        assertEquals("activeLineNumber", 0xFFE0E0E0.toInt(), tema.activeLineNumber.argb())
        assertEquals("keyword", 0xFF569CD6.toInt(), tema.keyword.argb())
        assertEquals("string", 0xFFCE9178.toInt(), tema.string.argb())
        assertEquals("comment", 0xFF6A9955.toInt(), tema.comment.argb())
        assertEquals("number", 0xFFB5CEA8.toInt(), tema.number.argb())
        assertEquals("function", 0xFFDCDCAA.toInt(), tema.function.argb())
        assertEquals("variable", 0xFF9CDCFE.toInt(), tema.variable.argb())
        assertEquals("propertyKey", 0xFF9CDCFE.toInt(), tema.propertyKey.argb())
        assertEquals("operator", 0xFFD4D4D4.toInt(), tema.operator.argb())
        assertEquals("punctuation", 0xFFCCCCCC.toInt(), tema.punctuation.argb())
        assertEquals("booleanNull", 0xFF569CD6.toInt(), tema.booleanNull.argb())
        assertEquals("annotation", 0xFF4EC9B0.toInt(), tema.annotation.argb())
        assertEquals("type", 0xFF4EC9B0.toInt(), tema.type.argb())
        assertEquals("searchMatchBackground", 0x66515C6A.toInt(), tema.searchMatchBackground.argb())
    }

    @Test
    fun `la paleta de One Dark sigue siendo la de siempre`() {
        val tema = SyntaxThemes.OneDark

        assertEquals("id", "one_dark", tema.id)
        assertEquals("background", 0xFF1E1E2E.toInt(), tema.background.argb())
        assertEquals("gutterBackground", 0xFF181825.toInt(), tema.gutterBackground.argb())
        assertEquals("activeLineBackground", 0xFF28283D.toInt(), tema.activeLineBackground.argb())
        assertEquals("text", 0xFFCDD6F4.toInt(), tema.text.argb())
        assertEquals("lineNumber", 0xFF6C7086.toInt(), tema.lineNumber.argb())
        assertEquals("activeLineNumber", 0xFF89B4FA.toInt(), tema.activeLineNumber.argb())
        assertEquals("keyword", 0xFFCBA6F7.toInt(), tema.keyword.argb())
        assertEquals("string", 0xFFA6E3A1.toInt(), tema.string.argb())
        assertEquals("comment", 0xFF7F849C.toInt(), tema.comment.argb())
        assertEquals("number", 0xFFFAB387.toInt(), tema.number.argb())
        assertEquals("function", 0xFF89B4FA.toInt(), tema.function.argb())
        assertEquals("variable", 0xFFF38BA8.toInt(), tema.variable.argb())
        assertEquals("propertyKey", 0xFF89DCEB.toInt(), tema.propertyKey.argb())
        assertEquals("operator", 0xFF94E2D5.toInt(), tema.operator.argb())
        assertEquals("punctuation", 0xFFBAC2DE.toInt(), tema.punctuation.argb())
        assertEquals("booleanNull", 0xFFF9E2AF.toInt(), tema.booleanNull.argb())
        assertEquals("annotation", 0xFFF9E2AF.toInt(), tema.annotation.argb())
        assertEquals("type", 0xFF89DCEB.toInt(), tema.type.argb())
        assertEquals("searchMatchBackground", 0x66F9E2AF.toInt(), tema.searchMatchBackground.argb())
    }

    @Test
    fun `la paleta de Monokai sigue siendo la de siempre`() {
        val tema = SyntaxThemes.Monokai

        assertEquals("id", "monokai", tema.id)
        assertEquals("background", 0xFF272822.toInt(), tema.background.argb())
        assertEquals("gutterBackground", 0xFF1E1F1C.toInt(), tema.gutterBackground.argb())
        assertEquals("activeLineBackground", 0xFF3E3D32.toInt(), tema.activeLineBackground.argb())
        assertEquals("text", 0xFFF8F8F2.toInt(), tema.text.argb())
        assertEquals("lineNumber", 0xFF75715E.toInt(), tema.lineNumber.argb())
        assertEquals("activeLineNumber", 0xFFFD971F.toInt(), tema.activeLineNumber.argb())
        assertEquals("keyword", 0xFFF92672.toInt(), tema.keyword.argb())
        assertEquals("string", 0xFFE6DB74.toInt(), tema.string.argb())
        assertEquals("comment", 0xFF75715E.toInt(), tema.comment.argb())
        assertEquals("number", 0xFFAE81FF.toInt(), tema.number.argb())
        assertEquals("function", 0xFFA6E22E.toInt(), tema.function.argb())
        assertEquals("variable", 0xFFFD971F.toInt(), tema.variable.argb())
        assertEquals("propertyKey", 0xFF66D9EF.toInt(), tema.propertyKey.argb())
        assertEquals("operator", 0xFFF92672.toInt(), tema.operator.argb())
        assertEquals("punctuation", 0xFFF8F8F2.toInt(), tema.punctuation.argb())
        assertEquals("booleanNull", 0xFFAE81FF.toInt(), tema.booleanNull.argb())
        assertEquals("annotation", 0xFFE6DB74.toInt(), tema.annotation.argb())
        assertEquals("type", 0xFF66D9EF.toInt(), tema.type.argb())
        assertEquals("searchMatchBackground", 0x66FFE792.toInt(), tema.searchMatchBackground.argb())
    }

    @Test
    fun `la paleta de GitHub Light sigue siendo la de siempre`() {
        val tema = SyntaxThemes.GitHubLight

        assertEquals("id", "github_light", tema.id)
        assertEquals("background", 0xFFF6F8FA.toInt(), tema.background.argb())
        assertEquals("gutterBackground", 0xFFEBEEF2.toInt(), tema.gutterBackground.argb())
        assertEquals("activeLineBackground", 0xFFE1E4E8.toInt(), tema.activeLineBackground.argb())
        assertEquals("text", 0xFF24292E.toInt(), tema.text.argb())
        assertEquals("lineNumber", 0xFF959DA5.toInt(), tema.lineNumber.argb())
        assertEquals("activeLineNumber", 0xFF0366D6.toInt(), tema.activeLineNumber.argb())
        assertEquals("keyword", 0xFFD73A49.toInt(), tema.keyword.argb())
        assertEquals("string", 0xFF032F62.toInt(), tema.string.argb())
        assertEquals("comment", 0xFF6A737D.toInt(), tema.comment.argb())
        assertEquals("number", 0xFF005CC5.toInt(), tema.number.argb())
        assertEquals("function", 0xFF6F42C1.toInt(), tema.function.argb())
        assertEquals("variable", 0xFFE36209.toInt(), tema.variable.argb())
        assertEquals("propertyKey", 0xFF22863A.toInt(), tema.propertyKey.argb())
        assertEquals("operator", 0xFFD73A49.toInt(), tema.operator.argb())
        assertEquals("punctuation", 0xFF24292E.toInt(), tema.punctuation.argb())
        assertEquals("booleanNull", 0xFF005CC5.toInt(), tema.booleanNull.argb())
        assertEquals("annotation", 0xFF6F42C1.toInt(), tema.annotation.argb())
        assertEquals("type", 0xFF005CC5.toInt(), tema.type.argb())
        assertEquals("searchMatchBackground", 0x66FFD33D.toInt(), tema.searchMatchBackground.argb())
    }

    @Test
    fun `el orden y el identificador de cada paleta son los de siempre`() {
        assertEquals(
            "el orden del selector del editor no puede cambiar",
            listOf("vscode_dark", "one_dark", "monokai", "github_light"),
            SyntaxThemes.all.map { it.id },
        )
    }

    @Test
    fun `solo GitHub Light es un tema claro`() {
        val claros = SyntaxThemes.all.filter { !it.isDark }.map { it.id }

        assertEquals("GitHub Light es el único tema claro del editor", listOf("github_light"), claros)
    }

    @Test
    fun `ninguna paleta tiene un color sin especificar`() {
        // La paleta del editor se pinta entera —fondo, canal, línea activa—, así que un `Color.Unspecified` en
        // una de ellas se vería como un agujero en la pantalla. El `Unspecified` es solo para el bloque de nota,
        // y para eso está `forNoteBlock()`.
        val sinEspecificar = SyntaxThemes.all.flatMap { tema ->
            tema.roles().filter { (_, color) -> color == Color.Unspecified }.map { "${tema.id}.${it.first}" }
        }

        assertTrue("paletas con colores sin especificar: $sinEspecificar", sinEspecificar.isEmpty())
    }

    @Test
    fun `el texto de cada paleta no es su fondo`() {
        SyntaxThemes.all.forEach { tema ->
            assertNotEquals("${tema.id} pinta el texto igual que el fondo", tema.background, tema.text)
        }
    }

    /**
     * Los 22 campos de una paleta, con su nombre, para no repetirlos en cada aserción.
     *
     * Es la lista completa a propósito: un recorrido que se olvidara de un campo dejaría ese campo sin comprobar,
     * y este es justo el fichero donde un valor cambiado tiene que notarse.
     */
    private fun SyntaxTheme.roles(): List<Pair<String, Color>> = listOf(
        "background" to background,
        "gutterBackground" to gutterBackground,
        "activeLineBackground" to activeLineBackground,
        "text" to text,
        "lineNumber" to lineNumber,
        "activeLineNumber" to activeLineNumber,
        "keyword" to keyword,
        "string" to string,
        "comment" to comment,
        "number" to number,
        "function" to function,
        "variable" to variable,
        "propertyKey" to propertyKey,
        "operator" to operator,
        "punctuation" to punctuation,
        "booleanNull" to booleanNull,
        "annotation" to annotation,
        "type" to type,
        "searchMatchBackground" to searchMatchBackground,
    )
}