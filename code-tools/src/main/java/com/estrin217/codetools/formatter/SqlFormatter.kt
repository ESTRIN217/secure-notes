package com.estrin217.codetools.formatter

import com.estrin217.codetools.syntax.SupportedLanguage
import java.util.regex.Pattern

/**
 * Formateador de consultas y scripts SQL.
 * Convierte palabras clave a mayúsculas canónicas y estructura cláusulas principales en renglones legibles.
 */
class SqlFormatter : LanguageFormatter {

    override fun canFormat(language: SupportedLanguage): Boolean = language == SupportedLanguage.SQL

    private val majorClauses = listOf(
        "SELECT", "FROM", "WHERE", "GROUP BY", "HAVING", "ORDER BY",
        "LIMIT", "OFFSET", "INSERT INTO", "VALUES", "UPDATE", "SET",
        "DELETE FROM", "UNION ALL", "UNION", "CREATE TABLE", "ALTER TABLE", "DROP TABLE"
    )

    private val subClauses = listOf(
        "LEFT JOIN", "RIGHT JOIN", "INNER JOIN", "FULL JOIN", "CROSS JOIN", "JOIN",
        "AND", "OR", "ON"
    )

    private val sqlKeywords = listOf(
        "SELECT", "FROM", "WHERE", "AND", "OR", "NOT", "IN", "IS", "NULL",
        "LIKE", "BETWEEN", "EXISTS", "AS", "DISTINCT", "JOIN", "INNER", "LEFT",
        "RIGHT", "FULL", "OUTER", "CROSS", "ON", "GROUP", "BY", "ORDER", "HAVING",
        "LIMIT", "OFFSET", "ASC", "DESC", "UNION", "ALL", "INSERT", "INTO",
        "VALUES", "UPDATE", "SET", "DELETE", "CREATE", "TABLE", "VIEW", "INDEX",
        "DROP", "ALTER", "ADD", "COLUMN", "PRIMARY", "KEY", "FOREIGN", "REFERENCES",
        "CONSTRAINT", "DEFAULT", "CHECK", "CASE", "WHEN", "THEN", "ELSE", "END",
        "COUNT", "SUM", "AVG", "MIN", "MAX", "COALESCE"
    )

    override fun format(code: String, language: SupportedLanguage, options: FormatOptions): FormattingResult {
        val trimmed = code.trim()
        if (trimmed.isEmpty()) return FormattingResult.Success(code)

        return try {
            // 1. Normalizar espacios múltiples
            var normalized = trimmed.replace(Regex("\\s+"), " ")

            // 2. Normalizar palabras clave a mayúsculas preservando cadenas entre comillas
            val stringMatcher = Pattern.compile("'[^']*'|\"[^\"]*\"").matcher(normalized)
            val preservedStrings = mutableListOf<String>()
            val placeholder = "___SQL_STR_TOKEN___"

            val noStrings = StringBuilder()
            var lastEnd = 0
            while (stringMatcher.find()) {
                noStrings.append(normalized.substring(lastEnd, stringMatcher.start()))
                preservedStrings.add(stringMatcher.group())
                noStrings.append(placeholder)
                lastEnd = stringMatcher.end()
            }
            noStrings.append(normalized.substring(lastEnd))

            var processed = noStrings.toString()

            // Reemplazar keywords con mayúsculas
            for (kw in sqlKeywords.sortedByDescending { it.length }) {
                processed = processed.replace(
                    Regex("(?i)\\b$kw\\b"),
                    kw
                )
            }

            // Normalizar espaciado alrededor de comas
            processed = processed.replace(Regex("\\s*,\\s*"), ", ")

            // 3. Separar cláusulas principales en nuevas líneas
            for (clause in majorClauses) {
                processed = processed.replace(
                    Regex("(?i)\\b$clause\\b"),
                    "\n$clause"
                )
            }

            // Separar sub-cláusulas (AND, OR, JOIN, etc.) con indentación
            for (sub in subClauses) {
                processed = processed.replace(
                    Regex("(?i)\\b$sub\\b"),
                    "\n${options.getIndentString(1)}$sub"
                )
            }

            // 4. Restaurar cadenas literales
            for (str in preservedStrings) {
                processed = processed.replaceFirst(placeholder, str)
            }

            // 5. Limpieza de líneas vacías excesivas
            val formatted = processed.lineSequence()
                .map { it.trimEnd() }
                .filter { it.isNotBlank() }
                .joinToString("\n") + if (options.insertFinalNewline) "\n" else ""

            FormattingResult.Success(formatted.trimStart())
        } catch (e: Exception) {
            FormattingResult.Error("Error formateando sentencia SQL: ${e.message}")
        }
    }
}
