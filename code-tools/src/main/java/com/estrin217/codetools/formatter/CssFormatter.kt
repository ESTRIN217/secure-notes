package com.estrin217.codetools.formatter

import com.estrin217.codetools.syntax.SupportedLanguage

/**
 * Formateador para hojas de estilo CSS.
 * Ordena selectores, propiedades y valores con indentación coherente.
 */
class CssFormatter : LanguageFormatter {

    override fun canFormat(language: SupportedLanguage): Boolean = language == SupportedLanguage.CSS

    override fun format(code: String, language: SupportedLanguage, options: FormatOptions): FormattingResult {
        val trimmed = code.trim()
        if (trimmed.isEmpty()) return FormattingResult.Success(code)

        return try {
            val lines = trimmed.lines()
            val result = StringBuilder()
            var indentLevel = 0

            for (rawLine in lines) {
                val line = rawLine.trim()
                if (line.isEmpty()) continue

                if (line.startsWith("}")) {
                    indentLevel = (indentLevel - 1).coerceAtLeast(0)
                    result.append(options.getIndentString(indentLevel)).append("}\n\n")
                    continue
                }

                if (line.endsWith("{")) {
                    val selector = line.removeSuffix("{").trim()
                    result.append(options.getIndentString(indentLevel))
                        .append(selector)
                        .append(" {\n")
                    indentLevel++
                    continue
                }

                if (line.contains("{") && line.contains("}")) {
                    // Regla de una sola línea: selector { prop: val; }
                    val parts = line.split("{")
                    val selector = parts[0].trim()
                    val body = parts[1].removeSuffix("}").trim()

                    result.append(options.getIndentString(indentLevel)).append(selector).append(" {\n")
                    indentLevel++
                    val declarations = body.split(";")
                    for (decl in declarations) {
                        val cleanDecl = decl.trim()
                        if (cleanDecl.isNotEmpty()) {
                            val formattedDecl = formatDeclaration(cleanDecl)
                            result.append(options.getIndentString(indentLevel))
                                .append(formattedDecl)
                                .append(";\n")
                        }
                    }
                    indentLevel--
                    result.append(options.getIndentString(indentLevel)).append("}\n\n")
                    continue
                }

                // Declaración común dentro de un bloque
                val formattedDecl = formatDeclaration(line.removeSuffix(";").trim())
                val isComment = line.startsWith("/*") || line.startsWith("//")

                if (isComment) {
                    result.append(options.getIndentString(indentLevel)).append(line).append("\n")
                } else {
                    result.append(options.getIndentString(indentLevel))
                        .append(formattedDecl)
                        .append(";\n")
                }
            }

            var output = result.toString().trimEnd()
            if (options.insertFinalNewline) {
                output += "\n"
            }

            FormattingResult.Success(output)
        } catch (e: Exception) {
            FormattingResult.Error("Error al estructurar CSS: ${e.message}")
        }
    }

    private fun formatDeclaration(declaration: String): String {
        val colonIndex = declaration.indexOf(':')
        if (colonIndex == -1) return declaration
        val prop = declaration.substring(0, colonIndex).trim()
        val value = declaration.substring(colonIndex + 1).trim()
        return "$prop: $value"
    }
}
