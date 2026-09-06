package com.estrin217.codetools.formatter

import com.estrin217.codetools.syntax.SupportedLanguage
import java.util.regex.Pattern

/**
 * Formateador jerárquico para documentos XML y HTML.
 * Gestiona apertura y cierre de etiquetas, tags auto-concluidos, comentarios y doctypes.
 */
class XmlHtmlFormatter : LanguageFormatter {

    override fun canFormat(language: SupportedLanguage): Boolean {
        return language == SupportedLanguage.XML || language == SupportedLanguage.HTML
    }

    private val voidHtmlTags = setOf(
        "area", "base", "br", "col", "embed", "hr", "img", "input",
        "link", "meta", "param", "source", "track", "wbr"
    )

    override fun format(code: String, language: SupportedLanguage, options: FormatOptions): FormattingResult {
        val trimmed = code.trim()
        if (trimmed.isEmpty()) return FormattingResult.Success(code)

        val isHtml = language == SupportedLanguage.HTML

        return try {
            val result = StringBuilder()
            var indentLevel = 0

            // Normalizar separadores y dividir en tokens de etiquetas y texto
            val tokenPattern = Pattern.compile("(<[^>]+>|[^<]+)")
            val matcher = tokenPattern.matcher(trimmed)
            val tokens = mutableListOf<String>()

            while (matcher.find()) {
                val token = matcher.group().trim()
                if (token.isNotEmpty()) {
                    tokens.add(token)
                }
            }

            var i = 0
            while (i < tokens.size) {
                val token = tokens[i]

                if (token.startsWith("<!--")) {
                    // Comentario
                    result.append(options.getIndentString(indentLevel)).append(token).append("\n")
                } else if (token.startsWith("<?") || token.startsWith("<!")) {
                    // Declaración XML o DOCTYPE
                    result.append(options.getIndentString(indentLevel)).append(token).append("\n")
                } else if (token.startsWith("</")) {
                    // Etiqueta de cierre: </tag>
                    indentLevel = (indentLevel - 1).coerceAtLeast(0)
                    result.append(options.getIndentString(indentLevel)).append(token).append("\n")
                } else if (token.startsWith("<")) {
                    // Etiqueta de apertura o auto-cerrada: <tag...> o <tag.../>
                    val isSelfClosing = token.endsWith("/>")
                    val tagName = extractTagName(token)
                    val isVoid = isHtml && voidHtmlTags.contains(tagName.lowercase())

                    // Comprobar si es un elemento simple de una línea: <tag>texto</tag>
                    val nextToken = tokens.getOrNull(i + 1)
                    val closingToken = tokens.getOrNull(i + 2)
                    val isSingleLineElement = nextToken != null &&
                            !nextToken.startsWith("<") &&
                            closingToken != null &&
                            closingToken.equals("</$tagName>", ignoreCase = true)

                    if (isSingleLineElement) {
                        result.append(options.getIndentString(indentLevel))
                            .append(token)
                            .append(nextToken)
                            .append(closingToken)
                            .append("\n")
                        i += 2 // Saltar texto y etiqueta de cierre
                    } else {
                        result.append(options.getIndentString(indentLevel)).append(token).append("\n")
                        if (!isSelfClosing && !isVoid) {
                            indentLevel++
                        }
                    }
                } else {
                    // Contenido textual suelto
                    result.append(options.getIndentString(indentLevel)).append(token).append("\n")
                }
                i++
            }

            var formatted = result.toString()
            if (!options.insertFinalNewline && formatted.endsWith("\n")) {
                formatted = formatted.trimEnd('\n')
            }

            FormattingResult.Success(formatted)
        } catch (e: Exception) {
            FormattingResult.Error("Error al estructurar etiquetas XML/HTML: ${e.message}")
        }
    }

    private fun extractTagName(tagToken: String): String {
        val clean = tagToken.removePrefix("<").removeSuffix(">").removeSuffix("/").trim()
        val spaceIndex = clean.indexOfAny(charArrayOf(' ', '\t', '\n'))
        return if (spaceIndex != -1) clean.substring(0, spaceIndex) else clean
    }
}
