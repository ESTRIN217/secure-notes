package com.example.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import com.estrin217.editordecodigo.syntax.SupportedLanguage
import com.estrin217.editordecodigo.syntax.SyntaxHighlighter
import com.estrin217.editordecodigo.syntax.SyntaxTheme
import com.example.R

/** Una opción del selector de idioma de un bloque de código: el identificador que se guarda y su etiqueta. */
data class CodeLanguageOption(
    val storedId: String,
    val language: SupportedLanguage,
    val labelRes: Int? = null,
    val labelLiteral: String = ""
)

/**
 * Pegamento entre el bloque de código de una nota y el catálogo del editor de código (spec 022).
 *
 * El bloque **no** tiene resaltador propio: [highlight] delega en el módulo `:editor-de-codigo`. Lo único que
 * vive aquí es el mapeo entre el identificador que la nota guarda y el idioma del catálogo compartido, la
 * paleta del bloque y las etiquetas del selector (RF-132).
 *
 * Es **puro**: no importa `android` ni `androidx.compose.runtime`, para poder probarse sin Robolectric.
 */
object CodeSyntax {

    /**
     * Paleta del bloque de código **dentro de la nota** (RF-134).
     *
     * Son los mismos colores que usaba la implementación retirada. `text` va **sin especificar** a propósito:
     * el color base lo sigue poniendo el tema Material del bloque (`onSurface`), no un tema de editor. Las
     * entradas de fondo y de números de línea también van sin especificar, porque el bloque no las usa y no
     * debe pintar fondo propio.
     */
    val blockTheme = SyntaxTheme(
        id = "app_block",
        name = "Bloque de nota",
        isDark = false,
        background = Color.Unspecified,
        gutterBackground = Color.Unspecified,
        activeLineBackground = Color.Unspecified,
        text = Color.Unspecified,
        lineNumber = Color.Unspecified,
        activeLineNumber = Color.Unspecified,
        keyword = Color(0xFF3B82F6),
        string = Color(0xFF16A34A),
        comment = Color(0xFF8B93A3),
        number = Color(0xFF0891B2),
        function = Color(0xFFA855F7),
        variable = Color(0xFFF59E0B),
        propertyKey = Color(0xFFF59E0B),
        operator = Color(0xFF0D9488),
        punctuation = Color(0xFF8B93A3),
        booleanNull = Color(0xFF3B82F6),
        annotation = Color(0xFF10B981),
        type = Color(0xFF9333EA),
        searchMatchBackground = Color.Unspecified
    )

    /**
     * Identificador guardado en la nota → idioma del catálogo compartido.
     *
     * Son **solo** los identificadores que la app escribe hoy (o detecta por nombre de fichero), no una
     * definición de lenguajes: las reglas de resaltado viven en el módulo (RF-132). `"toml"` e `"ini"` son dos
     * identificadores que resuelven al mismo idioma.
     */
    private val languageByStoredId: Map<String, SupportedLanguage> = mapOf(
        "kotlin" to SupportedLanguage.KOTLIN,
        "java" to SupportedLanguage.JAVA,
        "python" to SupportedLanguage.PYTHON,
        "javascript" to SupportedLanguage.JAVASCRIPT,
        "typescript" to SupportedLanguage.TYPESCRIPT,
        "html" to SupportedLanguage.HTML,
        "css" to SupportedLanguage.CSS,
        "json" to SupportedLanguage.JSON,
        "xml" to SupportedLanguage.XML,
        "sql" to SupportedLanguage.SQL,
        "c" to SupportedLanguage.C,
        "cpp" to SupportedLanguage.CPP,
        "csharp" to SupportedLanguage.CSHARP,
        "go" to SupportedLanguage.GO,
        "rust" to SupportedLanguage.RUST,
        "swift" to SupportedLanguage.SWIFT,
        "bash" to SupportedLanguage.BASH,
        "shell" to SupportedLanguage.BASH,
        "php" to SupportedLanguage.PHP,
        "ruby" to SupportedLanguage.RUBY,
        "yaml" to SupportedLanguage.YAML,
        "toml" to SupportedLanguage.INI_TOML,
        "ini" to SupportedLanguage.INI_TOML,
        "dart" to SupportedLanguage.DART,
        "lua" to SupportedLanguage.LUA,
        "markdown" to SupportedLanguage.MARKDOWN
    )

    /** Las opciones del selector, en el orden y con las etiquetas de siempre (RF-139). */
    private val languageOptions: List<CodeLanguageOption> = listOf(
        CodeLanguageOption("", SupportedLanguage.PLAIN_TEXT, labelRes = R.string.code_lang_plain),
        literal("kotlin", SupportedLanguage.KOTLIN, "Kotlin"),
        literal("java", SupportedLanguage.JAVA, "Java"),
        literal("python", SupportedLanguage.PYTHON, "Python"),
        literal("javascript", SupportedLanguage.JAVASCRIPT, "JavaScript"),
        literal("typescript", SupportedLanguage.TYPESCRIPT, "TypeScript"),
        literal("html", SupportedLanguage.HTML, "HTML"),
        literal("css", SupportedLanguage.CSS, "CSS"),
        literal("json", SupportedLanguage.JSON, "JSON"),
        literal("xml", SupportedLanguage.XML, "XML"),
        literal("sql", SupportedLanguage.SQL, "SQL"),
        literal("c", SupportedLanguage.C, "C"),
        literal("cpp", SupportedLanguage.CPP, "C++"),
        literal("csharp", SupportedLanguage.CSHARP, "C#"),
        literal("go", SupportedLanguage.GO, "Go"),
        literal("rust", SupportedLanguage.RUST, "Rust"),
        literal("swift", SupportedLanguage.SWIFT, "Swift"),
        CodeLanguageOption("bash", SupportedLanguage.BASH, labelRes = R.string.code_lang_shell),
        literal("php", SupportedLanguage.PHP, "PHP"),
        literal("ruby", SupportedLanguage.RUBY, "Ruby"),
        literal("yaml", SupportedLanguage.YAML, "YAML"),
        literal("toml", SupportedLanguage.INI_TOML, "TOML"),
        literal("ini", SupportedLanguage.INI_TOML, "INI"),
        literal("dart", SupportedLanguage.DART, "Dart"),
        literal("lua", SupportedLanguage.LUA, "Lua"),
        literal("markdown", SupportedLanguage.MARKDOWN, "Markdown")
    )

    fun options(): List<CodeLanguageOption> = languageOptions

    /**
     * CUANDO un bloque guarda un identificador de idioma, EL SISTEMA devuelve su idioma; `null` si no hay
     * idioma (vacío o solo espacios) o si el identificador no existe en el catálogo (RF-133, RF-135, RF-137).
     */
    fun fromStoredId(id: String?): SupportedLanguage? {
        val key = id?.trim()?.lowercase()
        if (key.isNullOrEmpty()) return null
        return languageByStoredId[key]
    }

    /**
     * Resalta [code] según el identificador guardado. Sin idioma (o desconocido) devuelve el texto **plano**.
     * El texto nunca cambia: el resaltado es solo visualización (RF-138).
     */
    fun highlight(code: String, storedId: String?): AnnotatedString {
        val language = fromStoredId(storedId) ?: return AnnotatedString(code)
        return SyntaxHighlighter.highlight(code, language, blockTheme)
    }

    private fun literal(storedId: String, language: SupportedLanguage, label: String) =
        CodeLanguageOption(storedId, language, labelLiteral = label)
}
