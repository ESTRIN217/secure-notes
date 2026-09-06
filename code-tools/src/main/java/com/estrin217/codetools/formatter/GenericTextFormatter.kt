package com.estrin217.codetools.formatter

import com.estrin217.codetools.syntax.SupportedLanguage

/**
 * Formateador genérico de respaldo para formatos de configuración, scripts y texto plano
 * (YAML, Bash, Ruby, Markdown, ENV, INI/TOML, Plain Text).
 *
 * Normaliza tabulaciones, limpia espacios residuales al final de línea y regula líneas vacías.
 */
class GenericTextFormatter : LanguageFormatter {

    override fun canFormat(language: SupportedLanguage): Boolean = true // Soporte como fallback universal

    override fun format(code: String, language: SupportedLanguage, options: FormatOptions): FormattingResult {
        if (code.isBlank()) return FormattingResult.Success(code)

        return try {
            val lines = code.lines()
            val result = StringBuilder()
            var consecutiveEmptyLines = 0

            for (rawLine in lines) {
                val trimmedRight = if (options.trimTrailingWhitespace) rawLine.trimEnd() else rawLine

                if (trimmedRight.isBlank()) {
                    if (consecutiveEmptyLines < options.maxEmptyLines) {
                        result.append("\n")
                        consecutiveEmptyLines++
                    }
                    continue
                }
                consecutiveEmptyLines = 0

                // Normalizar tabulaciones iniciales si corresponde
                var line = trimmedRight
                if (options.useSpaces && line.startsWith("\t")) {
                    var tabCount = 0
                    while (tabCount < line.length && line[tabCount] == '\t') {
                        tabCount++
                    }
                    val rest = line.substring(tabCount)
                    line = options.getIndentString(tabCount) + rest
                }

                result.append(line).append("\n")
            }

            var output = result.toString()
            if (!options.insertFinalNewline && output.endsWith("\n")) {
                output = output.trimEnd('\n')
            } else if (options.insertFinalNewline && !output.endsWith("\n")) {
                output += "\n"
            }

            FormattingResult.Success(output)
        } catch (e: Exception) {
            FormattingResult.Error("Error en formato genérico: ${e.message}")
        }
    }
}
