package com.estrin217.codetools.syntax

enum class SupportedLanguage(
    val displayName: String,
    val fileExtension: String,
    val mimeType: String,
    val extensions: List<String>,
    val commentPrefix: String = "// "
) {
    KOTLIN("Kotlin", "kt", "text/x-kotlin", listOf("kt", "kts"), "// "),
    JAVA("Java", "java", "text/x-java-source", listOf("java"), "// "),
    JAVASCRIPT("JavaScript", "js", "application/javascript", listOf("js", "mjs", "cjs"), "// "),
    TYPESCRIPT("TypeScript", "ts", "application/typescript", listOf("ts", "tsx"), "// "),
    HTML("HTML", "html", "text/html", listOf("html", "htm"), "<!-- "),
    CSS("CSS", "css", "text/css", listOf("css", "scss", "sass", "less"), "/* "),
    XML("XML", "xml", "application/xml", listOf("xml", "svg", "plist", "xsd"), "<!-- "),
    SQL("SQL", "sql", "application/sql", listOf("sql"), "-- "),
    C("C", "c", "text/x-c", listOf("c", "h"), "// "),
    CPP("C++", "cpp", "text/x-c++", listOf("cpp", "cxx", "cc", "hpp", "hxx"), "// "),
    CSHARP("C#", "cs", "text/x-csharp", listOf("cs"), "// "),
    GO("Go", "go", "text/x-go", listOf("go"), "// "),
    RUST("Rust", "rs", "text/x-rust", listOf("rs"), "// "),
    SWIFT("Swift", "swift", "text/x-swift", listOf("swift"), "// "),
    PHP("PHP", "php", "application/x-httpd-php", listOf("php", "phtml"), "// "),
    RUBY("Ruby", "rb", "application/x-ruby", listOf("rb", "gemspec", "rake"), "# "),
    DART("Dart", "dart", "application/dart", listOf("dart"), "// "),
    LUA("Lua", "lua", "text/x-lua", listOf("lua"), "-- "),
    PYTHON("Python", "py", "text/x-python", listOf("py", "pyw"), "# "),
    BASH("Bash / Shell", "sh", "text/x-sh", listOf("sh", "bash", "zsh"), "# "),
    JSON("JSON", "json", "application/json", listOf("json"), "// "),
    YAML("YAML", "yaml", "text/yaml", listOf("yaml", "yml"), "# "),
    ENV("Config / ENV", "env", "text/plain", listOf("env", "properties"), "# "),
    INI_TOML("INI / TOML", "ini", "text/plain", listOf("ini", "toml", "conf", "cfg"), "# "),
    MARKDOWN("Markdown", "md", "text/markdown", listOf("md", "markdown"), "> "),
    PLAIN_TEXT("Texto Plano", "txt", "text/plain", listOf("txt", "log"), "# ");

    companion object {
        fun fromFileName(fileName: String): SupportedLanguage {
            val lower = fileName.trim().lowercase()
            if (lower == ".env" || lower.startsWith(".env.") || lower.endsWith(".env") || lower.endsWith(".properties")) return ENV
            if (lower == "dockerfile" || lower.endsWith(".sh")) return BASH
            if (lower == "gemfile" || lower == "rakefile") return RUBY
            if (lower == "cmakelists.txt") return C
            val ext = lower.substringAfterLast('.', "")
            return entries.firstOrNull { lang -> lang.extensions.contains(ext) } ?: PLAIN_TEXT
        }
    }
}
