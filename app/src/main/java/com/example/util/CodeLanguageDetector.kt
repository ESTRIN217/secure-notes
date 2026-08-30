package com.example.util

object CodeLanguageDetector {

    private val extensionMap = mapOf(
        "kt" to "kotlin",
        "kts" to "kotlin",
        "java" to "java",
        "py" to "python",
        "js" to "javascript",
        "mjs" to "javascript",
        "cjs" to "javascript",
        "ts" to "typescript",
        "tsx" to "typescript",
        "html" to "html",
        "htm" to "html",
        "css" to "css",
        "json" to "json",
        "xml" to "xml",
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
        "zsh" to "bash"
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
