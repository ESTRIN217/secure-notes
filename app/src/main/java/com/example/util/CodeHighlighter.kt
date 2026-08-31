package com.example.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle

/** Resaltado de sintaxis por render (display-only): el texto almacenado se mantiene plano. */
object CodeHighlighter {

    private data class LangConfig(
        val keywords: Set<String>,
        val hashComment: Boolean = false,
        val doubleDashComment: Boolean = false,
        val blockComment: Boolean = true,
        val htmlComment: Boolean = false
    )

    private val langConfigs: Map<String, LangConfig> = mapOf(
        "kotlin" to LangConfig(
            keywords = setOf(
                "fun", "val", "var", "if", "else", "when", "for", "while", "do", "return", "class",
                "object", "interface", "package", "import", "private", "public", "internal", "protected",
                "inline", "suspend", "data", "sealed", "enum", "try", "catch", "finally", "throw", "is",
                "in", "as", "null", "true", "false", "this", "super", "override", "abstract", "companion",
                "init", "lateinit", "constructor", "by", "reified", "infix", "tailrec", "operator",
                "open", "annotation", "typealias", "expect", "actual", "const", "vararg", "noinline",
                "crossinline", "dynamic"
            )
        ),
        "java" to LangConfig(
            keywords = setOf(
                "public", "private", "protected", "static", "final", "class", "interface", "extends",
                "implements", "import", "package", "new", "if", "else", "for", "while", "do", "switch",
                "case", "default", "break", "continue", "return", "try", "catch", "finally", "throw",
                "throws", "void", "int", "long", "double", "float", "boolean", "byte", "char", "short",
                "this", "super", "null", "true", "false", "instanceof", "synchronized", "abstract",
                "native", "volatile", "transient", "enum", "assert"
            )
        ),
        "python" to LangConfig(
            keywords = setOf(
                "def", "class", "if", "elif", "else", "for", "while", "in", "not", "and", "or",
                "return", "yield", "lambda", "import", "from", "as", "pass", "break", "continue",
                "try", "except", "finally", "raise", "with", "global", "nonlocal", "assert", "del",
                "is", "None", "True", "False"
            ),
            hashComment = true,
            blockComment = false
        ),
        "javascript" to LangConfig(
            keywords = setOf(
                "function", "var", "let", "const", "if", "else", "for", "while", "do", "switch",
                "case", "default", "break", "continue", "return", "try", "catch", "finally", "throw",
                "new", "class", "extends", "super", "import", "export", "from", "this", "null",
                "undefined", "true", "false", "async", "await", "typeof", "instanceof", "in", "of",
                "delete", "void"
            )
        ),
        "typescript" to LangConfig(
            keywords = setOf(
                "function", "var", "let", "const", "if", "else", "for", "while", "do", "switch",
                "case", "default", "break", "continue", "return", "try", "catch", "finally", "throw",
                "new", "class", "extends", "super", "import", "export", "from", "this", "null",
                "undefined", "true", "false", "async", "await", "typeof", "instanceof", "in", "of",
                "delete", "void", "interface", "type", "enum", "namespace", "declare", "readonly",
                "private", "public", "protected", "static", "abstract", "implements", "keyof",
                "infer", "never", "any", "unknown"
            )
        ),
        "html" to LangConfig(
            keywords = emptySet(),
            blockComment = false,
            htmlComment = true
        ),
        "xml" to LangConfig(
            keywords = emptySet(),
            blockComment = false,
            htmlComment = true
        ),
        "css" to LangConfig(
            keywords = setOf(
                "background", "color", "display", "margin", "padding", "border", "font", "position",
                "width", "height", "flex", "grid", "visibility", "overflow", "z-index", "opacity",
                "transition", "animation", "cursor", "text-align", "align-items", "justify-content"
            )
        ),
        "json" to LangConfig(
            keywords = setOf("true", "false", "null"),
            blockComment = false
        ),
        "sql" to LangConfig(
            keywords = setOf(
                "select", "from", "where", "insert", "into", "values", "update", "set", "delete",
                "create", "table", "alter", "drop", "index", "view", "join", "left", "right", "inner",
                "outer", "on", "as", "and", "or", "not", "null", "primary", "key", "foreign",
                "references", "group", "by", "order", "having", "limit", "distinct", "count", "sum",
                "avg", "min", "max", "union", "all", "exists", "between", "like", "in", "case", "when",
                "then", "else", "end"
            ),
            hashComment = true,
            doubleDashComment = true,
            blockComment = false
        ),
        "c" to LangConfig(
            keywords = setOf(
                "int", "char", "float", "double", "void", "struct", "union", "enum", "typedef",
                "static", "extern", "register", "auto", "const", "volatile", "unsigned", "signed",
                "short", "long", "if", "else", "for", "while", "do", "switch", "case", "default",
                "break", "continue", "return", "goto", "sizeof"
            )
        ),
        "cpp" to LangConfig(
            keywords = setOf(
                "int", "char", "float", "double", "void", "struct", "union", "enum", "typedef",
                "static", "extern", "register", "auto", "const", "volatile", "unsigned", "signed",
                "short", "long", "if", "else", "for", "while", "do", "switch", "case", "default",
                "break", "continue", "return", "goto", "sizeof", "class", "namespace", "template",
                "typename", "virtual", "override", "public", "private", "protected", "friend",
                "inline", "constexpr", "using", "new", "delete", "this", "nullptr", "explicit",
                "noexcept", "operator"
            )
        ),
        "csharp" to LangConfig(
            keywords = setOf(
                "public", "private", "protected", "internal", "class", "struct", "interface", "enum",
                "namespace", "using", "if", "else", "for", "foreach", "while", "do", "switch", "case",
                "default", "break", "continue", "return", "try", "catch", "finally", "throw", "new",
                "this", "base", "null", "true", "false", "async", "await", "readonly", "static",
                "virtual", "override", "sealed", "abstract", "partial", "var", "void", "int", "string",
                "bool", "double", "float", "long", "decimal", "object", "delegate", "event", "is",
                "as", "out", "ref", "in", "const"
            )
        ),
        "go" to LangConfig(
            keywords = setOf(
                "package", "import", "func", "var", "const", "type", "struct", "interface", "if",
                "else", "for", "range", "switch", "case", "default", "fallthrough", "go", "defer",
                "select", "chan", "map", "return", "break", "continue", "goto"
            )
        ),
        "rust" to LangConfig(
            keywords = setOf(
                "fn", "let", "mut", "const", "static", "if", "else", "for", "while", "loop", "match",
                "return", "struct", "enum", "trait", "impl", "mod", "use", "pub", "crate", "self",
                "super", "where", "async", "await", "move", "ref", "in", "as", "unsafe", "extern"
            )
        ),
        "swift" to LangConfig(
            keywords = setOf(
                "func", "var", "let", "if", "else", "for", "while", "repeat", "switch", "case",
                "default", "return", "guard", "defer", "class", "struct", "enum", "protocol",
                "extension", "import", "inout", "public", "private", "internal", "fileprivate",
                "open", "static", "subscript", "init", "deinit", "where", "throw", "throws", "catch",
                "try", "as", "is", "nil", "true", "false", "self", "super"
            )
        ),
        "bash" to LangConfig(
            keywords = setOf(
                "if", "then", "else", "elif", "fi", "for", "while", "do", "done", "case", "esac",
                "function", "return", "echo", "export", "source", "local", "declare", "in", "break",
                "continue"
            ),
            hashComment = true,
            blockComment = false
        )
    )

    private val keywordStyle = SpanStyle(color = Color(0xFF3B82F6))
    private val stringStyle = SpanStyle(color = Color(0xFF16A34A))
    private val stringInterpolationStyle = SpanStyle(color = Color(0xFFF59E0B))
    private val numberStyle = SpanStyle(color = Color(0xFF0891B2))
    private val commentStyle = SpanStyle(color = Color(0xFF8B93A3))
    private val typeStyle = SpanStyle(color = Color(0xFF9333EA))
    private val annotationStyle = SpanStyle(color = Color(0xFF10B981))
    private val functionStyle = SpanStyle(color = Color(0xFFA855F7))

    private val regexCache = mutableMapOf<String, Regex>()

    fun highlight(text: String, language: String?): AnnotatedString =
        highlight(AnnotatedString(text), language)

    fun highlight(code: AnnotatedString, language: String?): AnnotatedString {
        if (code.text.isEmpty()) return code
        val config = langConfigs[language] ?: return code
        val regex = regexFor(config)
        val text = code.text
        val builder = AnnotatedString.Builder(text)
        val stringRanges = mutableListOf<IntRange>()
        for (match in regex.findAll(text)) {
            val style = when {
                match.groups["comment"] != null -> commentStyle
                match.groups["string"] != null -> {
                    stringRanges.add(match.range)
                    stringStyle
                }
                match.groups["number"] != null -> numberStyle
                config.keywords.isNotEmpty() && match.groups["keyword"] != null -> keywordStyle
                match.groups["annotation"] != null -> annotationStyle
                match.groups["type"] != null -> typeStyle
                match.groups["function"] != null -> functionStyle
                else -> continue
            }
            builder.addStyle(style, match.range.first, match.range.last + 1)
        }
        if (stringRanges.isNotEmpty()) {
            val interpRegex = stringInterpolationRegex()
            for (range in stringRanges) {
                for (m in interpRegex.findAll(text, range.first)) {
                    if (m.range.last > range.last) break
                    if (m.range.first >= range.first) {
                        builder.addStyle(
                            stringInterpolationStyle,
                            m.range.first,
                            m.range.last + 1
                        )
                    }
                }
            }
        }
        return builder.toAnnotatedString()
    }

    private val interpRegexCache = mutableMapOf<String, Regex>()

    private fun stringInterpolationRegex(): Regex =
        interpRegexCache.getOrPut("") {
            Regex("\\$\\{(?:[^{}]|\\{[^{}]*\\})*\\}|\\$[A-Za-z_][A-Za-z0-9_]*")
        }

    private fun regexFor(config: LangConfig): Regex {
        val keywords = config.keywords.toList().sorted()
        val key = keywords.joinToString("|") +
            "|" + config.hashComment + config.doubleDashComment + config.blockComment + config.htmlComment
        return regexCache.getOrPut(key) {
            val comments = mutableListOf<String>()
            if (config.blockComment) comments += "/\\*[\\s\\S]*?\\*/"
            if (config.htmlComment) comments += "<!--[\\s\\S]*?-->"
            if (config.hashComment) comments += "#[^\\n]*"
            if (config.doubleDashComment) comments += "--[^\\n]*"
            comments += "//[^\\n]*"

            val strings = "\"\"\"(?:\\.|(?!\"\"\")[^\\\\])*?\"\"\"|\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'|`(?:\\\\.|[^`\\\\])*`"
            val numbers = "\\b\\d[\\d_]*(?:\\.\\d+)?(?:[eE][+-]?\\d+)?\\b|0[xX][0-9a-fA-F]+"
            val keywordsPattern = if (keywords.isEmpty()) "" else "|(?<keyword>\\b(?:${keywords.joinToString("|")})\\b)"
            val pattern = buildString {
                append("(?<comment>").append(comments.joinToString("|")).append(')')
                append("|(?<string>").append(strings).append(')')
                append("|(?<number>").append(numbers).append(')')
                append(keywordsPattern)
                append("|(?<annotation>@[A-Za-z_][A-Za-z0-9_]*(?:::[A-Za-z_][A-Za-z0-9_]*)*)")
                append("|(?<type>\\b[A-Z][A-Za-z0-9_]*)")
                append("|(?<function>\\b[A-Za-z_][A-Za-z0-9_]*(?=\\s*\\())")
            }
            Regex(pattern)
        }
    }
}
