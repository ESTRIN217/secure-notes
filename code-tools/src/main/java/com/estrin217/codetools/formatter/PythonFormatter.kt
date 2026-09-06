package com.estrin217.codetools.formatter

import com.estrin217.codetools.syntax.SupportedLanguage

/**
 * Formateador especializado para Python según convenciones PEP 8.
 * Normaliza tabulaciones a espacios, limpia espacios finales y ajusta espaciado de operadores y comas.
 */
class PythonFormatter : LanguageFormatter {

    override fun canFormat(language: SupportedLanguage): Boolean = language == SupportedLanguage.PYTHON

    override fun format(code: String, language: SupportedLanguage, options: FormatOptions): FormattingResult {
        if (code.isBlank()) return FormattingResult.Success(code)

        return try {
            val lines = code.lines()
            val result = StringBuilder()
            var inMultilineDocstring = false
            var docstringQuote = ""
            var consecutiveEmptyLines = 0

            for (rawLine in lines) {
                // Si la línea está completamente vacía
                if (rawLine.trim().isEmpty()) {
                    if (consecutiveEmptyLines < options.maxEmptyLines) {
                        result.append("\n")
                        consecutiveEmptyLines++
                    }
                    continue
                }
                consecutiveEmptyLines = 0

                // 1. Manejo de Docstrings multilínea (""" o ''')
                val trimmed = rawLine.trim()
                if (inMultilineDocstring) {
                    result.append(rawLine.trimEnd()).append("\n")
                    if (trimmed.contains(docstringQuote)) {
                        inMultilineDocstring = false
                    }
                    continue
                }

                if (trimmed.startsWith("\"\"\"") || trimmed.startsWith("'''")) {
                    val quote = if (trimmed.startsWith("\"\"\"")) "\"\"\"" else "'''"
                    result.append(rawLine.trimEnd()).append("\n")
                    // Si no cierra en la misma línea
                    if (trimmed.length > 3 && !trimmed.substring(3).contains(quote)) {
                        inMultilineDocstring = true
                        docstringQuote = quote
                    }
                    continue
                }

                // 2. Manejo de comentarios #
                if (trimmed.startsWith("#")) {
                    val leadingIndent = extractLeadingWhitespace(rawLine, options)
                    result.append(leadingIndent).append(trimmed).append("\n")
                    continue
                }

                // 3. Normalizar tabulaciones y espaciado inicial
                val leadingSpaces = extractLeadingWhitespace(rawLine, options)

                // 4. Normalizar operadores y comas dentro del código
                var formattedContent = trimmed
                // Espacio tras comas
                formattedContent = formattedContent.replace(Regex(",([A-Za-z0-9_\"'$])"), ", $1")
                // Espacio tras dos puntos en definiciones/estructuras
                formattedContent = formattedContent.replace(Regex("(?<=\\b(def|class|if|elif|else|for|while|try|except|finally|with)\\b[^:]*):(\\s*$)"), ":")

                result.append(leadingSpaces).append(formattedContent).append("\n")
            }

            var output = result.toString()
            if (options.trimTrailingWhitespace) {
                output = output.lines().joinToString("\n") { it.trimEnd() }
            }
            if (options.insertFinalNewline && !output.endsWith("\n")) {
                output += "\n"
            } else if (!options.insertFinalNewline && output.endsWith("\n")) {
                output = output.trimEnd('\n')
            }

            FormattingResult.Success(output)
        } catch (e: Exception) {
            FormattingResult.Error("Error al formatear código Python: ${e.message}")
        }
    }

    private fun extractLeadingWhitespace(line: String, options: FormatOptions): String {
        var count = 0
        for (c in line) {
            if (c == ' ') count++
            else if (c == '\t') count += options.indentSize
            else break
        }
        val normalizedLevels = count / options.indentSize
        val remainder = count % options.indentSize
        return options.getIndentString(normalizedLevels) + " ".repeat(remainder)
    }
}
