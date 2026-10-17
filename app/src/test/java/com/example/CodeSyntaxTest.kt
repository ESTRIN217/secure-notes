package com.example

import com.estrin217.editordecodigo.syntax.SupportedLanguage
import com.estrin217.tema.AppThemes
import com.example.util.CodeSyntax
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas **puras** (sin Robolectric) del pegamento entre el bloque de código y el catálogo del editor
 * (spec 022, RF-131…RF-139).
 *
 * Aquí solo se comprueba la parte que es de `:app`: que todo identificador que la app guarda hoy se resuelva
 * a un idioma, que sin idioma no se resalte, que el texto no cambie al resaltar y que las opciones del selector
 * sigan siendo las de hoy. El resaltado en sí es del módulo `:editor-de-codigo`.
 */
class CodeSyntaxTest {

    /**
     * La paleta con la que se resaltan las pruebas: la del bloque de nota, sin superficie.
     *
     * Da igual cuál sea —aquí se comprueba el resaltado, no el color—, y por eso se usa la del tema por defecto
     * en vez de inventarse una: si algún día las pruebas fallaran por un cambio de paleta, el sitio donde mirar
     * sería el catálogo, no el test.
     */
    private val paleta = AppThemes.notePalette(AppThemes.byId(null))

    /** Todo identificador que la app guarda hoy (los del selector) o detecta por nombre de fichero. */
    private val storedIdsTheAppWrites = listOf(
        "kotlin", "java", "python", "javascript", "typescript", "html", "css", "json", "xml", "sql",
        "c", "cpp", "csharp", "go", "rust", "swift", "bash", "php", "ruby", "yaml",
        "toml", "ini", "dart", "lua", "markdown"
    )

    // ---- RF-133: compatibilidad con lo ya guardado -------------------------------------------------

    @Test
    fun `every identifier the app writes resolves to a language`() {
        for (id in storedIdsTheAppWrites) {
            assertNotNull("identificador sin idioma: $id", CodeSyntax.fromStoredId(id))
        }
    }

    @Test
    fun `identifier resolution ignores case and spaces`() {
        assertEquals(SupportedLanguage.KOTLIN, CodeSyntax.fromStoredId("  Kotlin "))
        assertEquals(SupportedLanguage.BASH, CodeSyntax.fromStoredId("SHELL"))
    }

    @Test
    fun `toml and ini resolve to the same language`() {
        assertEquals(SupportedLanguage.INI_TOML, CodeSyntax.fromStoredId("toml"))
        assertEquals(SupportedLanguage.INI_TOML, CodeSyntax.fromStoredId("ini"))
    }

    // ---- RF-135 / RF-137: sin idioma o desconocido ⇒ texto plano ----------------------------------

    @Test
    fun `no language means no highlighting`() {
        assertNull(CodeSyntax.fromStoredId(null))
        assertNull(CodeSyntax.fromStoredId(""))
        assertNull(CodeSyntax.fromStoredId("   "))
        assertTrue(CodeSyntax.highlight("val x = 1", null, paleta).spanStyles.isEmpty())
        assertTrue(CodeSyntax.highlight("val x = 1", "", paleta).spanStyles.isEmpty())
    }

    @Test
    fun `unknown language highlights nothing and does not fail`() {
        assertNull(CodeSyntax.fromStoredId("rust2020"))
        val result = CodeSyntax.highlight("val x = 1", "rust2020", paleta)
        assertTrue(result.spanStyles.isEmpty())
        assertEquals("val x = 1", result.text)
    }

    // ---- RF-131 / RF-138: resalta de verdad y nunca toca el texto ---------------------------------

    @Test
    fun `highlighting never changes the text`() {
        val code = "fun main() {\n    val s = \"hola\"\n}\n"
        for (id in storedIdsTheAppWrites) {
            assertEquals("el texto cambió con $id", code, CodeSyntax.highlight(code, id, paleta).text)
        }
    }

    @Test
    fun `a known language actually highlights`() {
        assertTrue(
            "kotlin no resalta nada",
            CodeSyntax.highlight("val x = 1", "kotlin", paleta).spanStyles.isNotEmpty()
        )
    }

    // ---- RF-136 / RF-139: las opciones del selector son las de hoy --------------------------------

    @Test
    fun `every selectable option resolves to a language`() {
        val selectable = CodeSyntax.options().filter { it.storedId.isNotBlank() }
        assertEquals("el selector cambió de tamaño", 25, selectable.size)
        for (option in selectable) {
            assertEquals(
                "opción que no resuelve: ${option.storedId}",
                option.language,
                CodeSyntax.fromStoredId(option.storedId)
            )
        }
    }

    @Test
    fun `the plain option has no language`() {
        val plain = CodeSyntax.options().first { it.storedId.isBlank() }
        assertEquals(SupportedLanguage.PLAIN_TEXT, plain.language)
        assertNull(CodeSyntax.fromStoredId(plain.storedId))
    }

    @Test
    fun `options keep today's languages and order`() {
        assertEquals(
            listOf(
                "", "kotlin", "java", "python", "javascript", "typescript", "html", "css", "json", "xml",
                "sql", "c", "cpp", "csharp", "go", "rust", "swift", "bash", "php", "ruby", "yaml",
                "toml", "ini", "dart", "lua", "markdown"
            ),
            CodeSyntax.options().map { it.storedId }
        )
    }

    @Test
    fun `options keep today's labels`() {
        val literals = CodeSyntax.options()
            .associate { it.storedId to it.labelLiteral }
            .filterValues { it.isNotEmpty() }
        assertEquals(
            mapOf(
                "kotlin" to "Kotlin", "java" to "Java", "python" to "Python", "javascript" to "JavaScript",
                "typescript" to "TypeScript", "html" to "HTML", "css" to "CSS", "json" to "JSON",
                "xml" to "XML", "sql" to "SQL", "c" to "C", "cpp" to "C++", "csharp" to "C#",
                "go" to "Go", "rust" to "Rust", "swift" to "Swift", "php" to "PHP", "ruby" to "Ruby",
                "yaml" to "YAML", "toml" to "TOML", "ini" to "INI", "dart" to "Dart", "lua" to "Lua",
                "markdown" to "Markdown"
            ),
            literals
        )
    }

    @Test
    fun `translated labels keep their string resource`() {
        val options = CodeSyntax.options()
        assertEquals(R.string.code_lang_plain, options.first { it.storedId.isEmpty() }.labelRes)
        assertEquals(R.string.code_lang_shell, options.first { it.storedId == "bash" }.labelRes)
    }
}
