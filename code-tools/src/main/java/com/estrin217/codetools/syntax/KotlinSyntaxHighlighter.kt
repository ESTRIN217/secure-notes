package com.estrin217.codetools.syntax

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import java.util.regex.Pattern

/**
 * Resaltador de sintaxis especializado y de alto rendimiento para el lenguaje Kotlin.
 *
 * Implementa SRP (Single Responsibility Principle) aislando la lógica léxica
 * y semántica de Kotlin. Protege comentarios y cadenas para evitar colisiones
 * de palabras clave y resalta interpolación de cadenas ($variable y ${expr}),
 * anotaciones, tipos estándar y PascalCase, KDoc, operadores seguros y corutinas.
 */
object KotlinSyntaxHighlighter {

    // Comentarios (Línea simple y multilínea / KDoc)
    private val SINGLE_LINE_COMMENT = Pattern.compile("(?m)//.*$")
    private val MULTI_LINE_COMMENT = Pattern.compile("/\\*[\\s\\S]*?\\*/")
    private val KDOC_TAGS = Pattern.compile("@(param|return|throws|see|author|since|property|constructor|sample|suppress)\\b")

    // Cadenas y literales
    private val RAW_STRING = Pattern.compile("\"\"\"[\\s\\S]*?\"\"\"")
    private val REGULAR_STRING = Pattern.compile("\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"")
    private val CHAR_LITERAL = Pattern.compile("'[^'\\\\]*(?:\\\\.[^'\\\\]*)'")

    // Interpolación en cadenas de texto ($variable y ${expresión})
    private val STRING_TEMPLATE_VAR = Pattern.compile("\\\$[a-zA-Z_][a-zA-Z0-9_]*")
    private val STRING_TEMPLATE_EXPR = Pattern.compile("\\\$\\{([^}]+)\\}")

    // Anotaciones Kotlin (@Composable, @OptIn, @file:JvmName, etc.)
    private val ANNOTATIONS = Pattern.compile("@[A-Za-z0-9_.]+(?:::[A-Za-z0-9_]+)?|@(file|field|get|set|param|property|receiver|setparam|delegate):[A-Za-z0-9_.]+(?:::[A-Za-z0-9_]+)?")

    // Palabras clave de Kotlin (Declaración, modificadores y control de flujo)
    private val KEYWORDS = Pattern.compile(
        "\\b(" +
            "package|import|class|interface|object|fun|val|var|data|sealed|enum|" +
            "open|override|abstract|private|protected|public|internal|inline|noinline|" +
            "crossinline|reified|companion|init|constructor|if|else|when|for|while|do|" +
            "return|break|continue|throw|try|catch|finally|is|!is|as|as\\?|in|!in|" +
            "by|lazy|suspend|typealias|super|this|operator|infix|external|const|" +
            "lateinit|vararg|tailrec|actual|expect|value|annotation|inner|out|" +
            "where|get|set|field|it|dynamic" +
        ")\\b"
    )

    // Literales booleanos y nulo
    private val BOOLEANS_NULL = Pattern.compile("\\b(true|false|null)\\b")

    // Tipos primitivos y estándar de Kotlin
    private val STANDARD_TYPES = Pattern.compile(
        "\\b(" +
            "Int|Long|Float|Double|Boolean|String|Char|Byte|Short|" +
            "UByte|UShort|UInt|ULong|Unit|Nothing|Any|" +
            "List|MutableList|Set|MutableSet|Map|MutableMap|Array|" +
            "IntArray|LongArray|FloatArray|DoubleArray|BooleanArray|ByteArray|CharArray|ShortArray|" +
            "Sequence|Pair|Triple|Result|" +
            "Flow|StateFlow|SharedFlow|CoroutineScope|Job|Deferred|Channel|" +
            "Modifier|Color|TextStyle|Dp|Sp|State|MutableState|ViewModel" +
        ")\\b"
    )

    // Tipos personalizados y nombres de clases en PascalCase
    private val PASCAL_CASE_TYPES = Pattern.compile("\\b[A-Z][A-Za-z0-9_]*\\b")

    // Declaraciones de funciones (fun <nombre>)
    private val FUN_DECLARATION = Pattern.compile("\\bfun\\s+(?:<[^>]+>\\s+)?(?:[A-Za-z0-9_.]+\\.)?([A-Za-z0-9_]+)")

    // Funciones estándar de orden superior y utilitarias de Kotlin
    private val STDLIB_FUNCTIONS = Pattern.compile(
        "\\b(" +
            "apply|let|also|run|with|takeIf|takeUnless|repeat|lazy|" +
            "listOf|setOf|mapOf|mutableListOf|mutableMapOf|mutableSetOf|arrayOf|" +
            "println|print|require|requireNotNull|check|checkNotNull|error" +
        ")\\b"
    )

    // Llamadas a funciones generales (nombre seguido de paréntesis o lambda trailing)
    private val FUNCTION_CALLS = Pattern.compile("\\b([a-zA-Z_][a-zA-Z0-9_]*)(?=\\s*\\()")

    // Nombres de variables en declaraciones (val/var nombre)
    private val VAR_DECLARATION = Pattern.compile("\\b(?:val|var)\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\b")

    // Parámetros y propiedades con tipo (nombre: Tipo)
    private val PARAMETER_NAME = Pattern.compile("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s*:\\s*(?=[A-Z])")

    // Números (Hex, binarios, flotantes, enteros con sufijos y guiones bajos)
    private val NUMBERS = Pattern.compile(
        "\\b(" +
            "0x[0-9a-fA-F_]+[L]?|" +
            "0b[01_]+[L]?|" +
            "\\d[0-9_]*\\.[0-9_]+([eE][+-]?[0-9_]+)?[fFL]?|" +
            "\\d[0-9_]*[L|f|F]?" +
        ")\\b"
    )

    // Operadores característicos de Kotlin (Safe call, Elvis, Not-null, Range, Lambda arrow, etc.)
    private val OPERATORS = Pattern.compile(
        "(\\?\\.|\\?:|!!|::|->|\\.\\.<|\\.\\.|===|!==|==|!=|<=|>=|\\+=|-=|\\*=|/=|%=|=)"
    )

    fun highlight(
        builder: AnnotatedString.Builder,
        code: String,
        theme: SyntaxTheme
    ) {
        if (code.isEmpty()) return

        val codeLength = code.length
        val masked = BooleanArray(codeLength)

        // 1. Comentarios de una sola línea (// ...)
        val singleLineMatcher = SINGLE_LINE_COMMENT.matcher(code)
        while (singleLineMatcher.find()) {
            val start = singleLineMatcher.start()
            val end = singleLineMatcher.end()
            maskRange(masked, start, end)
            builder.addStyle(SpanStyle(color = theme.comment), start, end)
        }

        // 2. Comentarios multilínea y KDoc (/* ... */)
        val multiLineMatcher = MULTI_LINE_COMMENT.matcher(code)
        while (multiLineMatcher.find()) {
            val start = multiLineMatcher.start()
            val end = multiLineMatcher.end()
            maskRange(masked, start, end)
            builder.addStyle(SpanStyle(color = theme.comment), start, end)

            // Resaltar tags KDoc (@param, @return, etc.) dentro del bloque de comentario
            val commentText = code.substring(start, end)
            val kdocMatcher = KDOC_TAGS.matcher(commentText)
            while (kdocMatcher.find()) {
                val tagStart = start + kdocMatcher.start()
                val tagEnd = start + kdocMatcher.end()
                builder.addStyle(
                    SpanStyle(color = theme.annotation, fontWeight = FontWeight.SemiBold),
                    tagStart,
                    tagEnd
                )
            }
        }

        // 3. Cadenas triples (""" ... """)
        val rawStringMatcher = RAW_STRING.matcher(code)
        while (rawStringMatcher.find()) {
            val start = rawStringMatcher.start()
            val end = rawStringMatcher.end()
            if (!masked[start]) {
                maskRange(masked, start, end)
                builder.addStyle(SpanStyle(color = theme.string), start, end)
                highlightStringTemplates(builder, code, start, end, theme)
            }
        }

        // 4. Cadenas regulares (" ... ") y caracteres (' ... ')
        val regStringMatcher = REGULAR_STRING.matcher(code)
        while (regStringMatcher.find()) {
            val start = regStringMatcher.start()
            val end = regStringMatcher.end()
            if (!masked[start]) {
                maskRange(masked, start, end)
                builder.addStyle(SpanStyle(color = theme.string), start, end)
                highlightStringTemplates(builder, code, start, end, theme)
            }
        }

        val charMatcher = CHAR_LITERAL.matcher(code)
        while (charMatcher.find()) {
            val start = charMatcher.start()
            val end = charMatcher.end()
            if (!masked[start]) {
                maskRange(masked, start, end)
                builder.addStyle(SpanStyle(color = theme.string), start, end)
            }
        }

        // 5. Anotaciones (@Composable, @OptIn, @Suppress, etc.)
        val annotMatcher = ANNOTATIONS.matcher(code)
        while (annotMatcher.find()) {
            val start = annotMatcher.start()
            val end = annotMatcher.end()
            if (!masked[start]) {
                maskRange(masked, start, end)
                builder.addStyle(
                    SpanStyle(color = theme.annotation, fontWeight = FontWeight.Medium),
                    start,
                    end
                )
            }
        }

        // 6. Palabras clave (fun, val, class, when, etc.)
        val keyMatcher = KEYWORDS.matcher(code)
        while (keyMatcher.find()) {
            val start = keyMatcher.start()
            val end = keyMatcher.end()
            if (!masked[start]) {
                maskRange(masked, start, end)
                builder.addStyle(
                    SpanStyle(color = theme.keyword, fontWeight = FontWeight.Bold),
                    start,
                    end
                )
            }
        }

        // 7. Literales booleanos y nulo (true, false, null)
        val boolMatcher = BOOLEANS_NULL.matcher(code)
        while (boolMatcher.find()) {
            val start = boolMatcher.start()
            val end = boolMatcher.end()
            if (!masked[start]) {
                maskRange(masked, start, end)
                builder.addStyle(
                    SpanStyle(color = theme.booleanNull, fontWeight = FontWeight.Bold),
                    start,
                    end
                )
            }
        }

        // 8. Tipos estándar y colecciones de Kotlin
        val stdTypesMatcher = STANDARD_TYPES.matcher(code)
        while (stdTypesMatcher.find()) {
            val start = stdTypesMatcher.start()
            val end = stdTypesMatcher.end()
            if (!masked[start]) {
                maskRange(masked, start, end)
                builder.addStyle(
                    SpanStyle(color = theme.type, fontWeight = FontWeight.Bold),
                    start,
                    end
                )
            }
        }

        // 9. Nombres de Clases e Interfaces en PascalCase
        val pascalMatcher = PASCAL_CASE_TYPES.matcher(code)
        while (pascalMatcher.find()) {
            val start = pascalMatcher.start()
            val end = pascalMatcher.end()
            if (!masked[start]) {
                maskRange(masked, start, end)
                builder.addStyle(
                    SpanStyle(color = theme.type, fontWeight = FontWeight.SemiBold),
                    start,
                    end
                )
            }
        }

        // 10. Declaraciones de funciones (fun <nombre>)
        val funDeclMatcher = FUN_DECLARATION.matcher(code)
        while (funDeclMatcher.find()) {
            val groupStart = funDeclMatcher.start(1)
            val groupEnd = funDeclMatcher.end(1)
            if (groupStart >= 0 && groupEnd <= codeLength && !masked[groupStart]) {
                maskRange(masked, groupStart, groupEnd)
                builder.addStyle(
                    SpanStyle(color = theme.function, fontWeight = FontWeight.Bold),
                    groupStart,
                    groupEnd
                )
            }
        }

        // 11. Funciones de biblioteca estándar (apply, let, println, etc.)
        val stdlibMatcher = STDLIB_FUNCTIONS.matcher(code)
        while (stdlibMatcher.find()) {
            val start = stdlibMatcher.start()
            val end = stdlibMatcher.end()
            if (!masked[start]) {
                maskRange(masked, start, end)
                builder.addStyle(
                    SpanStyle(color = theme.function, fontWeight = FontWeight.SemiBold),
                    start,
                    end
                )
            }
        }

        // 12. Llamadas a funciones generales (identificador seguido de '(')
        val fnCallMatcher = FUNCTION_CALLS.matcher(code)
        while (fnCallMatcher.find()) {
            val groupStart = fnCallMatcher.start(1)
            val groupEnd = fnCallMatcher.end(1)
            if (groupStart >= 0 && groupEnd <= codeLength && !masked[groupStart]) {
                // No sobreescribir si empieza con mayúscula (ya clasificado como constructor / tipo)
                val firstChar = code[groupStart]
                if (!firstChar.isUpperCase()) {
                    maskRange(masked, groupStart, groupEnd)
                    builder.addStyle(
                        SpanStyle(color = theme.function),
                        groupStart,
                        groupEnd
                    )
                }
            }
        }

        // 13. Variables declaradas con val o var
        val varDeclMatcher = VAR_DECLARATION.matcher(code)
        while (varDeclMatcher.find()) {
            val groupStart = varDeclMatcher.start(1)
            val groupEnd = varDeclMatcher.end(1)
            if (groupStart >= 0 && groupEnd <= codeLength && !masked[groupStart]) {
                maskRange(masked, groupStart, groupEnd)
                builder.addStyle(
                    SpanStyle(color = theme.variable),
                    groupStart,
                    groupEnd
                )
            }
        }

        // 14. Parámetros nombrados (nombre: Tipo)
        val paramMatcher = PARAMETER_NAME.matcher(code)
        while (paramMatcher.find()) {
            val groupStart = paramMatcher.start(1)
            val groupEnd = paramMatcher.end(1)
            if (groupStart >= 0 && groupEnd <= codeLength && !masked[groupStart]) {
                maskRange(masked, groupStart, groupEnd)
                builder.addStyle(
                    SpanStyle(color = theme.variable),
                    groupStart,
                    groupEnd
                )
            }
        }

        // 15. Números (hex, binary, float, int)
        val numMatcher = NUMBERS.matcher(code)
        while (numMatcher.find()) {
            val start = numMatcher.start()
            val end = numMatcher.end()
            if (!masked[start]) {
                maskRange(masked, start, end)
                builder.addStyle(SpanStyle(color = theme.number), start, end)
            }
        }

        // 16. Operadores característicos de Kotlin (?. ?: !! :: -> .. === !== etc.)
        val opMatcher = OPERATORS.matcher(code)
        while (opMatcher.find()) {
            val start = opMatcher.start()
            val end = opMatcher.end()
            if (!masked[start]) {
                maskRange(masked, start, end)
                builder.addStyle(
                    SpanStyle(color = theme.operator, fontWeight = FontWeight.SemiBold),
                    start,
                    end
                )
            }
        }
    }

    /**
     * Resalta variables e interpolaciones dentro de cadenas ($variable y ${expresión}).
     */
    private fun highlightStringTemplates(
        builder: AnnotatedString.Builder,
        code: String,
        stringStart: Int,
        stringEnd: Int,
        theme: SyntaxTheme
    ) {
        val stringContent = code.substring(stringStart, stringEnd)

        // Interpolación simple: $nombre
        val varMatcher = STRING_TEMPLATE_VAR.matcher(stringContent)
        while (varMatcher.find()) {
            val start = stringStart + varMatcher.start()
            val end = stringStart + varMatcher.end()
            builder.addStyle(
                SpanStyle(color = theme.propertyKey, fontWeight = FontWeight.SemiBold),
                start,
                end
            )
        }

        // Interpolación compleja: ${usuario.nombre}
        val exprMatcher = STRING_TEMPLATE_EXPR.matcher(stringContent)
        while (exprMatcher.find()) {
            val exprStart = stringStart + exprMatcher.start()
            val exprEnd = stringStart + exprMatcher.end()
            // Resaltar delimitadores ${ y }
            builder.addStyle(
                SpanStyle(color = theme.operator, fontWeight = FontWeight.Bold),
                exprStart,
                exprStart + 2
            )
            builder.addStyle(
                SpanStyle(color = theme.operator, fontWeight = FontWeight.Bold),
                exprEnd - 1,
                exprEnd
            )
            // Resaltar cuerpo de la expresión
            if (exprEnd - 1 > exprStart + 2) {
                builder.addStyle(
                    SpanStyle(color = theme.variable),
                    exprStart + 2,
                    exprEnd - 1
                )
            }
        }
    }

    private fun maskRange(masked: BooleanArray, start: Int, end: Int) {
        val safeStart = start.coerceIn(0, masked.size)
        val safeEnd = end.coerceIn(0, masked.size)
        for (i in safeStart until safeEnd) {
            masked[i] = true
        }
    }
}
