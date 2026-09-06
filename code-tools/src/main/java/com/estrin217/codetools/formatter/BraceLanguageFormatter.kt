package com.estrin217.codetools.formatter

import com.estrin217.codetools.syntax.SupportedLanguage

/**
 * Formateador de código universal para lenguajes basados en bloques y llaves
 * (Kotlin, Java, JavaScript, TypeScript, C, C++, C#, Dart, Swift, Go, Rust, PHP, Lua).
 *
 * Aplica sangría inteligente, normaliza espaciado de operadores, limpia espacios redundantes
 * y respeta escrupulosamente los comentarios y cadenas literales.
 */
class BraceLanguageFormatter : LanguageFormatter {

    private val supportedLanguages = setOf(
        SupportedLanguage.KOTLIN,
        SupportedLanguage.JAVA,
        SupportedLanguage.JAVASCRIPT,
        SupportedLanguage.TYPESCRIPT,
        SupportedLanguage.C,
        SupportedLanguage.CPP,
        SupportedLanguage.CSHARP,
        SupportedLanguage.DART,
        SupportedLanguage.SWIFT,
        SupportedLanguage.GO,
        SupportedLanguage.RUST,
        SupportedLanguage.PHP,
        SupportedLanguage.LUA
    )

    override fun canFormat(language: SupportedLanguage): Boolean = supportedLanguages.contains(language)

    override fun format(code: String, language: SupportedLanguage, options: FormatOptions): FormattingResult {
        if (code.isBlank()) return FormattingResult.Success(code)

        return try {
            val lines = code.lines()
            val result = StringBuilder()
            var currentIndentLevel = 0
            var consecutiveEmptyLines = 0
            var inMultilineComment = false

            for (rawLine in lines) {
                val trimmed = rawLine.trim()

                // 1. Manejo de líneas vacías
                if (trimmed.isEmpty()) {
                    if (consecutiveEmptyLines < options.maxEmptyLines) {
                        result.append("\n")
                        consecutiveEmptyLines++
                    }
                    continue
                }
                consecutiveEmptyLines = 0

                // 2. Manejo de bloques de comentario multilínea /* ... */
                if (inMultilineComment) {
                    val indent = options.getIndentString(currentIndentLevel)
                    result.append(indent).append(trimmed).append("\n")
                    if (trimmed.contains("*/")) {
                        inMultilineComment = false
                    }
                    continue
                }

                if (trimmed.startsWith("/*")) {
                    val indent = options.getIndentString(currentIndentLevel)
                    result.append(indent).append(trimmed).append("\n")
                    if (!trimmed.contains("*/")) {
                        inMultilineComment = true
                    }
                    continue
                }

                // 3. Comentarios de una sola línea //
                if (trimmed.startsWith("//") || trimmed.startsWith("#")) {
                    val indent = options.getIndentString(currentIndentLevel)
                    result.append(indent).append(trimmed).append("\n")
                    continue
                }

                // 4. Calcular delta de llaves al inicio de la línea
                val startsWithClosing = trimmed.startsWith("}") ||
                        trimmed.startsWith("]") ||
                        trimmed.startsWith(")")

                val openCount = countOutsideStrings(trimmed, '{')
                val closeCount = countOutsideStrings(trimmed, '}')

                // Si la línea comienza con cierre (ej: "} else {" o "}"), se desindenta inmediatamente para este renglón
                val lineIndent = if (startsWithClosing && currentIndentLevel > 0) {
                    (currentIndentLevel - 1).coerceAtLeast(0)
                } else {
                    currentIndentLevel
                }

                // 5. Normalizar espaciado en la línea
                val formattedLine = formatCodeLine(trimmed)

                // 6. Escribir línea con indentación
                result.append(options.getIndentString(lineIndent))
                    .append(formattedLine)
                    .append("\n")

                // 7. Actualizar el nivel de indentación para las siguientes líneas
                val netChange = openCount - closeCount
                currentIndentLevel = (currentIndentLevel + netChange).coerceAtLeast(0)
            }

            var output = result.toString()
            if (options.trimTrailingWhitespace) {
                output = output.lines().joinToString("\n") { it.trimEnd() }
            }
            if (!options.insertFinalNewline && output.endsWith("\n")) {
                output = output.trimEnd('\n')
            } else if (options.insertFinalNewline && !output.endsWith("\n")) {
                output += "\n"
            }

            FormattingResult.Success(output)
        } catch (e: Exception) {
            FormattingResult.Error("Error al formatear código de bloques: ${e.message}")
        }
    }

    private fun formatCodeLine(line: String): String {
        // Normalizar espaciado antes de llaves abiertas: "if (x){" -> "if (x) {"
        var res = line
        res = res.replace(Regex("([^\\s{])\\{"), "$1 {")

        // Espacio tras estructuras de control: "if(", "for(", "while(" -> "if (", "for (", "while ("
        res = res.replace(Regex("\\b(if|for|while|when|catch|switch)\\("), "$1 (")

        // Espaciado estándar tras coma (no dentro de cadenas)
        res = res.replace(Regex(",([A-Za-z0-9_\"'$])"), ", $1")

        // Operadores de flecha en Kotlin/JS/Dart/Rust: "->"
        res = res.replace(Regex("([^\\s])->"), "$1 ->")
        res = res.replace(Regex("->([^\\s])"), "-> $1")

        // Operador Elvis "?:"
        res = res.replace(Regex("([^\\s])\\?:"), "$1 ?:")
        res = res.replace(Regex("\\?:([^\\s])"), "?: $1")

        return res
    }

    private fun countOutsideStrings(line: String, target: Char): Int {
        var count = 0
        var inQuotes = false
        var quoteChar = ' '
        var escaped = false

        for (c in line) {
            if (escaped) {
                escaped = false
                continue
            }
            if (c == '\\') {
                escaped = true
                continue
            }
            if (inQuotes) {
                if (c == quoteChar) {
                    inQuotes = false
                }
            } else {
                if (c == '"' || c == '\'') {
                    inQuotes = true
                    quoteChar = c
                } else if (c == target) {
                    count++
                }
            }
        }
        return count
    }
}
