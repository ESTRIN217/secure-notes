package com.example.util

object CodeLanguageDetector {

    private val extensionMap = mapOf(
        "kt" to "kotlin",
        "kts" to "kotlin",
        "ktm" to "kotlin",
        "java" to "java",
        "py" to "python",
        "pyw" to "python",
        "pyi" to "python",
        "js" to "javascript",
        "mjs" to "javascript",
        "cjs" to "javascript",
        "jsx" to "javascript",
        "ts" to "typescript",
        "tsx" to "typescript",
        "html" to "html",
        "htm" to "html",
        "xhtml" to "html",
        "css" to "css",
        "json" to "json",
        "jsonc" to "json",
        "xml" to "xml",
        "xsd" to "xml",
        "xsl" to "xml",
        "xslt" to "xml",
        "svg" to "xml",
        "sql" to "sql",
        "c" to "c",
        "h" to "c",
        "cpp" to "cpp",
        "cxx" to "cpp",
        "hpp" to "cpp",
        "hxx" to "cpp",
        "cs" to "csharp",
        "go" to "go",
        "rs" to "rust",
        "swift" to "swift",
        "sh" to "bash",
        "bash" to "bash",
        "zsh" to "bash",
        "ksh" to "bash",
        "fish" to "bash",
        "php" to "php",
        "phtml" to "php",
        "inc" to "php",
        "rb" to "ruby",
        "rake" to "ruby",
        "gemspec" to "ruby",
        "yaml" to "yaml",
        "yml" to "yaml",
        "toml" to "toml",
        "ini" to "ini",
        "cfg" to "ini",
        "properties" to "ini",
        "dart" to "dart",
        "lua" to "lua",
        "md" to "markdown",
        "markdown" to "markdown"
    )

    fun fromFileName(name: String?): String? {
        if (name.isNullOrBlank()) return null
        val ext = name.substringAfterLast('.', "").lowercase()
        if (ext.isEmpty() || ext == name) return null
        return extensionMap[ext]
    }

    fun detect(name: String?, text: String): String? {
        fromFileName(name)?.let { return it }
        val trimmed = text.trim()
        val looksLikeJson = (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
            (trimmed.startsWith("[") && trimmed.endsWith("]"))
        return if (looksLikeJson) "json" else null
    }
}
