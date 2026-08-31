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
        val semicolonComment: Boolean = false,
        val blockComment: Boolean = true,
        val htmlComment: Boolean = false,
        val htmlTags: Boolean = false,
        val fstring: Boolean = false,
        val primitives: Set<String> = emptySet()
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
                "throws", "this", "super", "null", "true", "false", "instanceof", "synchronized",
                "abstract", "native", "volatile", "transient", "enum", "assert", "record", "sealed",
                "permits", "yield", "var", "module", "requires", "transitive", "exports", "opens",
                "uses", "provides", "with", "to", "open"
            ),
            primitives = setOf(
                "void", "int", "long", "double", "float", "boolean", "byte", "char", "short"
            )
        ),
        "python" to LangConfig(
            keywords = setOf(
                "def", "class", "if", "elif", "else", "for", "while", "in", "not", "and", "or",
                "return", "yield", "lambda", "import", "from", "as", "pass", "break", "continue",
                "try", "except", "finally", "raise", "with", "global", "nonlocal", "assert", "del",
                "is", "None", "True", "False",
                "str", "int", "float", "bool", "list", "dict", "tuple", "set", "bytes", "object",
                "type", "print", "input", "len", "range", "enumerate", "zip", "map", "filter",
                "sorted", "reversed", "sum", "min", "max", "abs", "round", "open", "isinstance",
                "issubclass", "getattr", "setattr", "hasattr", "super", "id", "repr", "all", "any"
            ),
            hashComment = true,
            blockComment = false,
            fstring = true
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
            keywords = setOf(
                "doctype", "html", "head", "body", "title", "meta", "link", "style", "script",
                "div", "span", "p", "a", "img", "br", "hr", "ul", "ol", "li", "table", "thead",
                "tbody", "tr", "td", "th", "form", "input", "button", "textarea", "select",
                "option", "label", "nav", "header", "footer", "section", "article", "aside",
                "main", "h1", "h2", "h3", "h4", "h5", "h6", "video", "audio", "canvas",
                "iframe", "em", "strong", "small", "code", "pre", "blockquote"
            ),
            blockComment = false,
            htmlComment = true,
            htmlTags = true
        ),
        "xml" to LangConfig(
            keywords = setOf(
                "xml", "version", "encoding", "doctype", "cdata"
            ),
            blockComment = false,
            htmlComment = true,
            htmlTags = true
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
        ),
        "php" to LangConfig(
            keywords = setOf(
                "php", "echo", "print", "if", "else", "elseif", "for", "foreach", "while", "do",
                "switch", "case", "default", "break", "continue", "return", "function", "class",
                "interface", "trait", "extends", "implements", "public", "private", "protected",
                "static", "final", "abstract", "const", "var", "new", "this", "use", "namespace",
                "try", "catch", "finally", "throw", "instanceof", "null", "true", "false", "global",
                "include", "require", "include_once", "require_once", "isset", "unset", "empty",
                "list", "array", "as", "and", "or", "xor", "not", "match", "fn", "readonly", "enum"
            ),
            hashComment = true,
            blockComment = true
        ),
        "ruby" to LangConfig(
            keywords = setOf(
                "def", "class", "module", "if", "elsif", "else", "unless", "case", "when", "then",
                "for", "while", "until", "do", "return", "yield", "begin", "rescue", "ensure", "end",
                "require", "include", "extend", "attr_accessor", "attr_reader", "attr_writer",
                "new", "self", "super", "nil", "true", "false", "and", "or", "not", "break", "next",
                "redo", "retry", "raise", "catch", "throw", "lambda", "proc", "defined", "alias"
            ),
            hashComment = true,
            blockComment = false
        ),
        "yaml" to LangConfig(
            keywords = setOf(
                "true", "false", "null", "yes", "no", "on", "off", "~", "none", "True", "False",
                "Null", "Yes", "No", "On", "Off"
            ),
            hashComment = true,
            blockComment = false
        ),
        "toml" to LangConfig(
            keywords = setOf("true", "false"),
            hashComment = true,
            blockComment = false
        ),
        "ini" to LangConfig(
            keywords = setOf("true", "false", "yes", "no", "on", "off", "null"),
            hashComment = true,
            semicolonComment = true,
            blockComment = false
        ),
        "dart" to LangConfig(
            keywords = setOf(
                "void", "var", "final", "const", "class", "extends", "implements", "with", "mixin",
                "abstract", "async", "await", "yield", "sync", "if", "else", "for", "while", "do",
                "switch", "case", "default", "break", "continue", "return", "new", "this", "super",
                "null", "true", "false", "late", "required", "factory", "typedef", "part", "import",
                "export", "library", "operator", "get", "set", "rethrow", "throw", "try", "catch",
                "finally", "is", "as", "in", "covariant", "dynamic"
            )
        ),
        "lua" to LangConfig(
            keywords = setOf(
                "and", "break", "do", "else", "elseif", "end", "false", "for", "function", "goto",
                "if", "in", "local", "nil", "not", "or", "repeat", "return", "then", "true", "until",
                "while", "self"
            ),
            doubleDashComment = true,
            blockComment = false
        ),
        "markdown" to LangConfig(
            keywords = setOf(
                "true", "false", "null"
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
    private val attributeStyle = SpanStyle(color = Color(0xFFF59E0B))
    private val bracketStyle = SpanStyle(color = Color(0xFF8B93A3))
    private val primitiveStyle = SpanStyle(color = Color(0xFF0D9488))

    private val regexCache = mutableMapOf<String, Regex>()

    fun highlight(text: String, language: String?): AnnotatedString =
        highlight(AnnotatedString(text), language)

    fun highlight(code: AnnotatedString, language: String?): AnnotatedString {
        if (code.text.isEmpty()) return code
        val config = langConfigs[language] ?: return code
        val regex = regexFor(config)
        val text = code.text
        val builder = AnnotatedString.Builder(text)
        val stringRanges = mutableListOf<Pair<IntRange, Boolean>>()
        for (match in regex.findAll(text)) {
            val style = when {
                match.groups["comment"] != null -> commentStyle
                match.groups["string"] != null -> {
                    stringRanges.add(match.range to isFString(text, match.range, config))
                    stringStyle
                }
                match.groups["number"] != null -> numberStyle
                config.primitives.isNotEmpty() && match.groups["keyword"] != null &&
                    match.groups["keyword"]!!.value in config.primitives -> primitiveStyle
                config.keywords.isNotEmpty() && match.groups["keyword"] != null -> keywordStyle
                config.htmlTags && match.groups["attrName"] != null -> attributeStyle
                config.htmlTags && match.groups["tagName"] != null -> keywordStyle
                config.htmlTags && match.groups["tagBracket"] != null -> bracketStyle
                match.groups["annotation"] != null -> annotationStyle
                match.groups["type"] != null -> typeStyle
                match.groups["function"] != null -> functionStyle
                else -> continue
            }
            builder.addStyle(style, match.range.first, match.range.last + 1)
        }
        if (stringRanges.isNotEmpty()) {
            applyInterpolations(builder, text, stringRanges)
        }
        return builder.toAnnotatedString()
    }

    private fun isFString(text: String, range: IntRange, config: LangConfig): Boolean {
        if (!config.fstring) return false
        val start = range.first
        if (start <= 0) return false
        var i = start - 1
        while (i >= 0 && (text[i] == 'r' || text[i] == 'R')) i--
        return i >= 0 && (text[i] == 'f' || text[i] == 'F')
    }

    private fun applyInterpolations(
        builder: AnnotatedString.Builder,
        text: String,
        stringRanges: List<Pair<IntRange, Boolean>>
    ) {
        for ((range, isF) in stringRanges) {
            val interpRegex = if (isF) fStringInterpolationRegex() else stringInterpolationRegex()
            for (m in interpRegex.findAll(text, range.first)) {
                if (m.range.last > range.last) break
                if (m.range.first >= range.first) {
                    builder.addStyle(stringInterpolationStyle, m.range.first, m.range.last + 1)
                }
            }
        }
    }

    private val interpRegexCache = mutableMapOf<String, Regex>()

    private fun stringInterpolationRegex(): Regex =
        interpRegexCache.getOrPut("") {
            Regex("\\$\\{(?:[^{}]|\\{[^{}]*\\})*\\}|\\$[A-Za-z_][A-Za-z0-9_]*")
        }

    private val fStringRegexCache = mutableMapOf<String, Regex>()

    private fun fStringInterpolationRegex(): Regex =
        fStringRegexCache.getOrPut("f") {
            Regex("\\{(?:[^{}]|\\{[^{}]*\\})*\\}")
        }

    private fun regexFor(config: LangConfig): Regex {
        val allKeywords = (config.keywords + config.primitives).sorted()
        val key = allKeywords.joinToString("|") +
            "|" + config.hashComment + config.doubleDashComment + config.semicolonComment +
            config.blockComment +
            config.htmlComment + config.htmlTags + config.fstring
        return regexCache.getOrPut(key) {
            val comments = mutableListOf<String>()
            if (config.blockComment) comments += "/\\*[\\s\\S]*?\\*/"
            if (config.htmlComment) comments += "<!--[\\s\\S]*?-->"
            if (config.hashComment) comments += "#[^\\n]*"
            if (config.doubleDashComment) comments += "--[^\\n]*"
            if (config.semicolonComment) comments += ";[^\\n]*"
            if (!config.htmlTags) comments += "//[^\\n]*"

            val strings = "\"\"\"(?:\\.|(?!\"\"\")[^\\\\])*?\"\"\"|\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'|`(?:\\\\.|[^`\\\\])*`"
            val numbers = "\\b\\d[\\d_]*(?:\\.\\d+)?(?:[eE][+-]?\\d+)?\\b|0[xX][0-9a-fA-F]+"
            val keywordsPattern = if (allKeywords.isEmpty()) "" else "|(?<keyword>\\b(?:${allKeywords.joinToString("|")})\\b)"
            val pattern = buildString {
                append("(?<comment>").append(comments.joinToString("|")).append(')')
                append("|(?<string>").append(strings).append(')')
                append("|(?<number>").append(numbers).append(')')
                append(keywordsPattern)
                if (config.htmlTags) {
                    append("|(?<tagBracket></|/>|>|<)")
                    append("|(?<attrName>\\b[A-Za-z_][\\w.:-]*(?=\\s*=))")
                    append("|(?<tagName>(?<=<|</)[A-Za-z][\\w:-]*)")
                } else {
                    append("|(?<annotation>@[A-Za-z_][A-Za-z0-9_]*(?:::[A-Za-z_][A-Za-z0-9_]*)*)")
                    append("|(?<type>\\b[A-Z][A-Za-z0-9_]*)")
                    append("|(?<function>\\b[A-Za-z_][A-Za-z0-9_]*(?=\\s*\\())")
                }
            }
            Regex(pattern)
        }
    }
}
