package com.estrin217.codetools.formatter

import com.estrin217.codetools.syntax.SupportedLanguage

/**
 * Gestor y punto de entrada central para formateo de código en la aplicación.
 *
 * Aplica el patrón Strategy y respeta el principio Abierto/Cerrado (OCP),
 * permitiendo incorporar o sustituir formateadores sin modificar clientes.
 */
object CodeFormattingManager {

    private val jsonFormatter = JsonFormatter()
    private val xmlHtmlFormatter = XmlHtmlFormatter()
    private val sqlFormatter = SqlFormatter()
    private val cssFormatter = CssFormatter()
    private val braceFormatter = BraceLanguageFormatter()
    private val pythonFormatter = PythonFormatter()
    private val genericFormatter = GenericTextFormatter()

    private val formatters: List<LanguageFormatter> = listOf(
        jsonFormatter,
        xmlHtmlFormatter,
        sqlFormatter,
        cssFormatter,
        braceFormatter,
        pythonFormatter,
        genericFormatter // Fallback
    )

    /**
     * Devuelve el tamaño de sangría estándar recomendado para cada lenguaje.
     */
    fun defaultIndentForLanguage(language: SupportedLanguage): Int {
        return when (language) {
            SupportedLanguage.JSON,
            SupportedLanguage.HTML,
            SupportedLanguage.XML,
            SupportedLanguage.CSS,
            SupportedLanguage.JAVASCRIPT,
            SupportedLanguage.TYPESCRIPT,
            SupportedLanguage.YAML,
            SupportedLanguage.BASH -> 2

            SupportedLanguage.KOTLIN,
            SupportedLanguage.JAVA,
            SupportedLanguage.PYTHON,
            SupportedLanguage.C,
            SupportedLanguage.CPP,
            SupportedLanguage.CSHARP,
            SupportedLanguage.GO,
            SupportedLanguage.RUST,
            SupportedLanguage.DART,
            SupportedLanguage.SWIFT,
            SupportedLanguage.PHP,
            SupportedLanguage.SQL,
            SupportedLanguage.LUA,
            SupportedLanguage.RUBY,
            SupportedLanguage.MARKDOWN,
            SupportedLanguage.INI_TOML,
            SupportedLanguage.ENV,
            SupportedLanguage.PLAIN_TEXT -> 4
        }
    }

    /**
     * Formatea el código para el lenguaje especificado aplicando las opciones dadas.
     */
    fun format(
        code: String,
        language: SupportedLanguage,
        options: FormatOptions = FormatOptions(indentSize = defaultIndentForLanguage(language))
    ): FormattingResult {
        if (code.isBlank()) {
            return FormattingResult.Success(code)
        }

        val matchingFormatter = formatters.firstOrNull { it.canFormat(language) } ?: genericFormatter
        return matchingFormatter.format(code, language, options)
    }

    /**
     * Indica si el lenguaje soporta minificación (compactado).
     */
    fun canMinify(language: SupportedLanguage): Boolean = language == SupportedLanguage.JSON

    /**
     * Ejecuta minificación para lenguajes compatibles.
     */
    fun minify(code: String, language: SupportedLanguage): FormattingResult {
        return if (language == SupportedLanguage.JSON) {
            jsonFormatter.minify(code)
        } else {
            FormattingResult.Error("La minificación directa sólo está disponible para JSON.")
        }
    }
}
