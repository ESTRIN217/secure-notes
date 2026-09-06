package com.estrin217.codetools.formatter

import com.estrin217.codetools.syntax.SupportedLanguage

/**
 * Formateador y validador de sintaxis para JSON implementado en Kotlin puro.
 * No depende de stubs del framework de Android, garantizando paridad total
 * entre entornos de ejecución y pruebas unitarias JVM.
 *
 * Provee diagnóstico preciso de errores en español venezolano.
 */
class JsonFormatter : LanguageFormatter {

    override fun canFormat(language: SupportedLanguage): Boolean = language == SupportedLanguage.JSON

    override fun format(code: String, language: SupportedLanguage, options: FormatOptions): FormattingResult {
        val trimmed = code.trim()
        if (trimmed.isEmpty()) {
            return FormattingResult.Success(code)
        }

        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return FormattingResult.Error("¡Chamo, el contenido debe empezar con '{' o '[' para ser un JSON válido!")
        }

        return try {
            val formatted = formatJsonString(trimmed, options)
            val finalResult = if (options.insertFinalNewline && !formatted.endsWith("\n")) {
                "$formatted\n"
            } else if (!options.insertFinalNewline && formatted.endsWith("\n")) {
                formatted.trimEnd('\n')
            } else {
                formatted
            }
            FormattingResult.Success(finalResult)
        } catch (e: JsonSyntaxException) {
            FormattingResult.Error("¡Epa chamo, hay una pifia en la sintaxis del JSON: ${e.message}!")
        } catch (e: Exception) {
            FormattingResult.Error("Error procesando JSON: ${e.message}")
        }
    }

    /**
     * Compacta el contenido JSON eliminando espacios y saltos de línea fuera de cadenas de texto.
     */
    fun minify(code: String): FormattingResult {
        val trimmed = code.trim()
        if (trimmed.isEmpty()) {
            return FormattingResult.Success(code)
        }

        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return FormattingResult.Error("¡Pana, el contenido no tiene estructura de objeto ni arreglo JSON válido!")
        }

        return try {
            val sb = StringBuilder()
            var inString = false
            var escaped = false

            for (char in trimmed) {
                if (escaped) {
                    sb.append(char)
                    escaped = false
                    continue
                }

                if (char == '\\' && inString) {
                    sb.append(char)
                    escaped = true
                    continue
                }

                if (char == '"') {
                    inString = !inString
                    sb.append(char)
                    continue
                }

                if (inString) {
                    sb.append(char)
                } else if (!char.isWhitespace()) {
                    sb.append(char)
                }
            }

            if (inString) {
                return FormattingResult.Error("¡Pana, dejaste una cadena de texto sin cerrar en el JSON!")
            }

            FormattingResult.Success(sb.toString())
        } catch (e: Exception) {
            FormattingResult.Error("Error al compactar JSON: ${e.message}")
        }
    }

    private fun formatJsonString(json: String, options: FormatOptions): String {
        val sb = StringBuilder()
        var indentLevel = 0
        var inString = false
        var escaped = false
        val delimiterStack = ArrayDeque<Char>()
        var i = 0

        while (i < json.length) {
            val char = json[i]

            if (escaped) {
                sb.append(char)
                escaped = false
                i++
                continue
            }

            if (char == '\\' && inString) {
                sb.append(char)
                escaped = true
                i++
                continue
            }

            if (char == '"') {
                inString = !inString
                sb.append(char)
                i++
                continue
            }

            if (inString) {
                sb.append(char)
                i++
                continue
            }

            // Fuera de cadenas
            when (char) {
                ' ', '\t', '\r', '\n' -> {
                    i++
                }
                '{' -> {
                    delimiterStack.addLast('{')
                    sb.append(char)
                    val nextNonWhitespace = findNextNonWhitespace(json, i + 1)
                    if (nextNonWhitespace.second == '}') {
                        sb.append('}')
                        delimiterStack.removeLastOrNull()
                        i = nextNonWhitespace.first + 1
                    } else {
                        indentLevel++
                        sb.append("\n")
                        sb.append(options.getIndentString(indentLevel))
                        i++
                    }
                }
                '[' -> {
                    delimiterStack.addLast('[')
                    sb.append(char)
                    val nextNonWhitespace = findNextNonWhitespace(json, i + 1)
                    if (nextNonWhitespace.second == ']') {
                        sb.append(']')
                        delimiterStack.removeLastOrNull()
                        i = nextNonWhitespace.first + 1
                    } else {
                        indentLevel++
                        sb.append("\n")
                        sb.append(options.getIndentString(indentLevel))
                        i++
                    }
                }
                '}' -> {
                    val last = delimiterStack.removeLastOrNull()
                    if (last != '{') {
                        throw JsonSyntaxException("Llave de cierre '}' inesperada o mal ubicada")
                    }
                    indentLevel = (indentLevel - 1).coerceAtLeast(0)
                    sb.append("\n")
                    sb.append(options.getIndentString(indentLevel))
                    sb.append(char)
                    i++
                }
                ']' -> {
                    val last = delimiterStack.removeLastOrNull()
                    if (last != '[') {
                        throw JsonSyntaxException("Corchete de cierre ']' inesperado o mal ubicado")
                    }
                    indentLevel = (indentLevel - 1).coerceAtLeast(0)
                    sb.append("\n")
                    sb.append(options.getIndentString(indentLevel))
                    sb.append(char)
                    i++
                }
                ',' -> {
                    sb.append(char)
                    sb.append("\n")
                    sb.append(options.getIndentString(indentLevel))
                    i++
                }
                ':' -> {
                    sb.append(": ")
                    i++
                }
                else -> {
                    val tokenStart = i
                    while (i < json.length && json[i] != ',' && json[i] != '}' && json[i] != ']' && json[i] != ':' && !json[i].isWhitespace()) {
                        i++
                    }
                    val token = json.substring(tokenStart, i).trim()
                    val isValidLiteral = token == "true" || token == "false" || token == "null" || token.toDoubleOrNull() != null
                    if (!isValidLiteral) {
                        throw JsonSyntaxException("Token o valor inválido '$token' fuera de comillas")
                    }
                    sb.append(token)
                }
            }
        }

        if (inString) {
            throw JsonSyntaxException("Cadena literal sin cerrar al final del archivo")
        }

        if (delimiterStack.isNotEmpty()) {
            val expected = if (delimiterStack.last() == '{') "'}'" else "']'"
            throw JsonSyntaxException("Faltó cerrar la estructura con $expected al final")
        }

        return sb.toString()
    }

    private fun findNextNonWhitespace(str: String, startIndex: Int): Pair<Int, Char?> {
        for (idx in startIndex until str.length) {
            val c = str[idx]
            if (!c.isWhitespace()) {
                return Pair(idx, c)
            }
        }
        return Pair(-1, null)
    }

    private class JsonSyntaxException(message: String) : Exception(message)
}
