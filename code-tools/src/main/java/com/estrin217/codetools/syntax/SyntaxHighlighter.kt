package com.estrin217.codetools.syntax

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.util.regex.Pattern

object SyntaxHighlighter {

    fun highlight(
        code: String,
        language: SupportedLanguage,
        theme: SyntaxTheme,
        searchQuery: String = ""
    ): AnnotatedString {
        if (code.isEmpty()) return AnnotatedString("")

        return buildAnnotatedString {
            append(code)

            // Apply base text color
            addStyle(SpanStyle(color = theme.text), 0, code.length)

            when (language) {
                SupportedLanguage.KOTLIN -> highlightKotlin(code, theme)
                SupportedLanguage.JAVA -> highlightJava(code, theme)
                SupportedLanguage.JAVASCRIPT -> highlightJavaScript(code, theme)
                SupportedLanguage.TYPESCRIPT -> highlightTypeScript(code, theme)
                SupportedLanguage.HTML -> highlightHtml(code, theme)
                SupportedLanguage.CSS -> highlightCss(code, theme)
                SupportedLanguage.XML -> highlightXml(code, theme)
                SupportedLanguage.SQL -> highlightSql(code, theme)
                SupportedLanguage.C -> highlightC(code, theme)
                SupportedLanguage.CPP -> highlightCpp(code, theme)
                SupportedLanguage.CSHARP -> highlightCSharp(code, theme)
                SupportedLanguage.GO -> highlightGo(code, theme)
                SupportedLanguage.RUST -> highlightRust(code, theme)
                SupportedLanguage.SWIFT -> highlightSwift(code, theme)
                SupportedLanguage.PHP -> highlightPhp(code, theme)
                SupportedLanguage.RUBY -> highlightRuby(code, theme)
                SupportedLanguage.DART -> highlightDart(code, theme)
                SupportedLanguage.LUA -> highlightLua(code, theme)
                SupportedLanguage.PYTHON -> highlightPython(code, theme)
                SupportedLanguage.BASH -> highlightBash(code, theme)
                SupportedLanguage.JSON -> highlightJson(code, theme)
                SupportedLanguage.YAML -> highlightYaml(code, theme)
                SupportedLanguage.ENV -> highlightEnv(code, theme)
                SupportedLanguage.INI_TOML -> highlightIniToml(code, theme)
                SupportedLanguage.MARKDOWN -> highlightMarkdown(code, theme)
                SupportedLanguage.PLAIN_TEXT -> { /* Plain text keeps base style */ }
            }

            // Search query highlights (on top of syntax)
            if (searchQuery.isNotBlank() && searchQuery.length <= code.length) {
                val queryLower = searchQuery.lowercase()
                val codeLower = code.lowercase()
                var startIndex = 0
                while (startIndex < codeLower.length) {
                    val index = codeLower.indexOf(queryLower, startIndex)
                    if (index == -1) break
                    val endIndex = (index + searchQuery.length).coerceAtMost(code.length)
                    addStyle(
                        SpanStyle(
                            background = theme.searchMatchBackground,
                            fontWeight = FontWeight.Bold
                        ),
                        index,
                        endIndex
                    )
                    startIndex = index + searchQuery.length.coerceAtLeast(1)
                }
            }
        }
    }

    // ==========================================
    // C-Family & Modern Languages Highlighters
    // ==========================================

    private fun AnnotatedString.Builder.highlightKotlin(code: String, theme: SyntaxTheme) {
        KotlinSyntaxHighlighter.highlight(this, code, theme)
    }

    private fun AnnotatedString.Builder.highlightJava(code: String, theme: SyntaxTheme) {
        applyRegex(code, "\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"|'[^'\\\\]*(?:\\\\.[^'\\\\]*)*'", theme.string)
        applyRegex(code, "@[A-Za-z0-9_]+", theme.annotation)

        val keywords = "\\b(package|import|public|protected|private|static|final|abstract|synchronized|volatile|transient|native|strictfp|class|interface|enum|record|extends|implements|new|this|super|if|else|switch|case|default|while|do|for|break|continue|return|throw|throws|try|catch|finally|instanceof|void|assert)\\b"
        applyRegex(code, keywords, theme.keyword, FontWeight.Bold)

        val types = "\\b(int|long|float|double|boolean|char|byte|short|void|String|Integer|Long|Double|Boolean|Object|List|Map|Set|ArrayList|HashMap|Optional|Thread)\\b"
        applyRegex(code, types, theme.type, FontWeight.SemiBold)

        applyRegex(code, "\\b(true|false|null)\\b", theme.booleanNull, FontWeight.Bold)
        applyRegex(code, "\\b\\d+[L|f|F|d|D]?|0x[0-9a-fA-F]+\\b", theme.number)
        applyRegex(code, "\\b[A-Za-z0-9_]+(?=\\s*\\()", theme.function)
        applyRegex(code, "//.*$|/\\*[\\s\\S]*?\\*/", theme.comment)
    }

    private fun AnnotatedString.Builder.highlightJavaScript(code: String, theme: SyntaxTheme) {
        applyRegex(code, "`[^`\\\\]*(?:\\\\.[^`\\\\]*)*`|\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"|'[^'\\\\]*(?:\\\\.[^'\\\\]*)*'", theme.string)

        val keywords = "\\b(const|let|var|function|return|if|else|for|while|do|switch|case|default|break|continue|try|catch|finally|throw|class|extends|new|this|super|import|export|from|as|default|async|await|yield|typeof|instanceof|in|of|void|delete)\\b"
        applyRegex(code, keywords, theme.keyword, FontWeight.Bold)

        applyRegex(code, "\\b(true|false|null|undefined|NaN|Infinity)\\b", theme.booleanNull, FontWeight.Bold)
        applyRegex(code, "\\b\\d+(\\.\\d+)?\\b", theme.number)
        applyRegex(code, "\\b[A-Za-z0-9_$]+(?=\\s*\\()", theme.function)
        applyRegex(code, "//.*$|/\\*[\\s\\S]*?\\*/", theme.comment)
    }

    private fun AnnotatedString.Builder.highlightTypeScript(code: String, theme: SyntaxTheme) {
        applyRegex(code, "`[^`\\\\]*(?:\\\\.[^`\\\\]*)*`|\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"|'[^'\\\\]*(?:\\\\.[^'\\\\]*)*'", theme.string)

        val keywords = "\\b(const|let|var|function|return|if|else|for|while|do|switch|case|default|break|continue|try|catch|finally|throw|class|extends|implements|interface|type|enum|new|this|super|import|export|from|as|default|async|await|yield|typeof|instanceof|in|of|keyof|readonly|declare|namespace|module|abstract|public|private|protected)\\b"
        applyRegex(code, keywords, theme.keyword, FontWeight.Bold)

        val types = "\\b(string|number|boolean|any|void|unknown|never|object|symbol|bigint|Promise|Array|Record|Partial|Pick|Omit)\\b"
        applyRegex(code, types, theme.type, FontWeight.SemiBold)

        applyRegex(code, "\\b(true|false|null|undefined)\\b", theme.booleanNull, FontWeight.Bold)
        applyRegex(code, "\\b\\d+(\\.\\d+)?\\b", theme.number)
        applyRegex(code, "\\b[A-Za-z0-9_$]+(?=\\s*\\()", theme.function)
        applyRegex(code, "//.*$|/\\*[\\s\\S]*?\\*/", theme.comment)
    }

    private fun AnnotatedString.Builder.highlightHtml(code: String, theme: SyntaxTheme) {
        // Tag brackets & Tag Names <tag> </tag>
        applyRegex(code, "</?[a-zA-Z0-9\\-]+", theme.keyword, FontWeight.Bold)
        applyRegex(code, "/?>", theme.keyword, FontWeight.Bold)

        // Attribute names: attr=
        applyRegex(code, "\\b[a-zA-Z0-9\\-_:]+(?=\\s*=)", theme.propertyKey)

        // Attribute values
        applyRegex(code, "\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"|'[^'\\\\]*(?:\\\\.[^'\\\\]*)*'", theme.string)

        // HTML Comments
        applyRegex(code, "<!--[\\s\\S]*?-->", theme.comment)
    }

    private fun AnnotatedString.Builder.highlightCss(code: String, theme: SyntaxTheme) {
        // Strings
        applyRegex(code, "\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"|'[^'\\\\]*(?:\\\\.[^'\\\\]*)*'", theme.string)

        // Properties (color:, margin:, display:)
        applyRegex(code, "[a-zA-Z\\-]+(?=\\s*:)", theme.propertyKey, FontWeight.SemiBold)

        // Classes, IDs and pseudo elements
        applyRegex(code, "\\.[a-zA-Z0-9\\-_]+", theme.function)
        applyRegex(code, "#[a-zA-Z0-9\\-_]+", theme.variable)
        applyRegex(code, ":(hover|active|focus|visited|before|after|nth-child|first-child)", theme.keyword)

        // Values with units (12px, 1.5rem, 100%, 20deg)
        applyRegex(code, "\\b\\d+(\\.\\d+)?(px|em|rem|%|vh|vw|pt|s|ms|deg)\\b", theme.number)

        // Colors (#fff, #000000)
        applyRegex(code, "#([0-9a-fA-F]{3,8})\\b", theme.number)

        // Important
        applyRegex(code, "!important", theme.keyword, FontWeight.Bold)

        // Comments
        applyRegex(code, "/\\*[\\s\\S]*?\\*/", theme.comment)
    }

    private fun AnnotatedString.Builder.highlightXml(code: String, theme: SyntaxTheme) {
        // XML Tags
        applyRegex(code, "</?[a-zA-Z0-9\\-_:]+", theme.keyword, FontWeight.Bold)
        applyRegex(code, "/?>", theme.keyword, FontWeight.Bold)

        // Attribute names
        applyRegex(code, "\\b[a-zA-Z0-9\\-_:]+(?=\\s*=)", theme.propertyKey)

        // Attribute string values
        applyRegex(code, "\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"|'[^'\\\\]*(?:\\\\.[^'\\\\]*)*'", theme.string)

        // XML declaration <?xml ... ?>
        applyRegex(code, "<\\?[^>]*\\?>", theme.annotation)

        // Comments
        applyRegex(code, "<!--[\\s\\S]*?-->", theme.comment)
    }

    private fun AnnotatedString.Builder.highlightSql(code: String, theme: SyntaxTheme) {
        // Strings
        applyRegex(code, "'[^']*'", theme.string)

        val keywords = "(?i)\\b(SELECT|FROM|WHERE|INSERT|INTO|VALUES|UPDATE|SET|DELETE|CREATE|TABLE|DATABASE|ALTER|DROP|INDEX|VIEW|JOIN|INNER|LEFT|RIGHT|FULL|OUTER|CROSS|ON|GROUP|BY|ORDER|HAVING|ASC|DESC|LIMIT|OFFSET|UNION|ALL|DISTINCT|AS|AND|OR|NOT|IN|BETWEEN|LIKE|IS|NULL|PRIMARY|KEY|FOREIGN|REFERENCES|DEFAULT|CHECK|UNIQUE|AUTO_INCREMENT|CASCADE|EXISTS|CASE|WHEN|THEN|ELSE|END|COUNT|SUM|AVG|MIN|MAX|NOW|COALESCE)\\b"
        applyRegex(code, keywords, theme.keyword, FontWeight.Bold)

        val types = "(?i)\\b(INT|INTEGER|BIGINT|SMALLINT|TINYINT|VARCHAR|CHAR|TEXT|DECIMAL|NUMERIC|FLOAT|DOUBLE|DATE|DATETIME|TIMESTAMP|TIME|BOOLEAN|BLOB)\\b"
        applyRegex(code, types, theme.type, FontWeight.SemiBold)

        applyRegex(code, "\\b\\d+(\\.\\d+)?\\b", theme.number)
        applyRegex(code, "--.*$|/\\*[\\s\\S]*?\\*/", theme.comment)
    }

    private fun AnnotatedString.Builder.highlightC(code: String, theme: SyntaxTheme) {
        applyRegex(code, "\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"|'[^'\\\\]*(?:\\\\.[^'\\\\]*)*'", theme.string)

        // Preprocessor directives
        applyRegex(code, "#\\s*(include|define|undef|ifdef|ifndef|if|else|elif|endif|pragma|error)", theme.annotation, FontWeight.Bold)

        val keywords = "\\b(auto|break|case|char|const|continue|default|do|double|else|enum|extern|float|for|goto|if|inline|int|long|register|restrict|return|short|signed|sizeof|static|struct|switch|typedef|union|unsigned|void|volatile|while|_Bool|_Complex|_Imaginary)\\b"
        applyRegex(code, keywords, theme.keyword, FontWeight.Bold)

        applyRegex(code, "\\b(NULL|true|false)\\b", theme.booleanNull, FontWeight.Bold)
        applyRegex(code, "\\b\\d+(\\.\\d+)?([uU]?[lL]{0,2}|[fF])?\\b|0x[0-9a-fA-F]+\\b", theme.number)
        applyRegex(code, "\\b[A-Za-z0-9_]+(?=\\s*\\()", theme.function)
        applyRegex(code, "//.*$|/\\*[\\s\\S]*?\\*/", theme.comment)
    }

    private fun AnnotatedString.Builder.highlightCpp(code: String, theme: SyntaxTheme) {
        applyRegex(code, "\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"|'[^'\\\\]*(?:\\\\.[^'\\\\]*)*'", theme.string)
        applyRegex(code, "#\\s*(include|define|undef|ifdef|ifndef|if|else|elif|endif|pragma)", theme.annotation, FontWeight.Bold)

        val keywords = "\\b(asm|auto|bool|break|case|catch|char|char8_t|char16_t|char32_t|class|compl|concept|const|consteval|constexpr|const_cast|continue|co_await|co_return|co_yield|decltype|default|delete|do|double|dynamic_cast|else|enum|explicit|export|extern|false|float|for|friend|goto|if|inline|int|long|mutable|namespace|new|noexcept|nullptr|operator|private|protected|public|register|reinterpret_cast|requires|return|short|signed|sizeof|static|static_assert|static_cast|struct|switch|template|this|thread_local|throw|true|try|typedef|typeid|typename|union|unsigned|using|virtual|void|volatile|wchar_t|while)\\b"
        applyRegex(code, keywords, theme.keyword, FontWeight.Bold)

        val types = "\\b(std|string|vector|map|set|unordered_map|pair|unique_ptr|shared_ptr|cout|cin|endl|size_t)\\b"
        applyRegex(code, types, theme.type, FontWeight.SemiBold)

        applyRegex(code, "\\b(nullptr|true|false|NULL)\\b", theme.booleanNull, FontWeight.Bold)
        applyRegex(code, "\\b\\d+(\\.\\d+)?([uU]?[lL]{0,2}|[fF])?\\b|0x[0-9a-fA-F]+\\b", theme.number)
        applyRegex(code, "\\b[A-Za-z0-9_]+(?=\\s*\\()", theme.function)
        applyRegex(code, "//.*$|/\\*[\\s\\S]*?\\*/", theme.comment)
    }

    private fun AnnotatedString.Builder.highlightCSharp(code: String, theme: SyntaxTheme) {
        applyRegex(code, "\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"|'[^'\\\\]*(?:\\\\.[^'\\\\]*)*'", theme.string)
        applyRegex(code, "@[A-Za-z0-9_]+", theme.annotation)

        val keywords = "\\b(abstract|as|base|bool|break|byte|case|catch|char|checked|class|const|continue|decimal|default|delegate|do|double|else|enum|event|explicit|extern|false|finally|fixed|float|for|foreach|goto|if|implicit|in|int|interface|internal|is|lock|long|namespace|new|null|object|operator|out|override|params|private|protected|public|readonly|record|ref|return|sbyte|sealed|short|sizeof|stackalloc|static|string|struct|switch|this|throw|true|try|typeof|uint|ulong|unchecked|unsafe|ushort|using|virtual|void|volatile|while|async|await|var)\\b"
        applyRegex(code, keywords, theme.keyword, FontWeight.Bold)

        val types = "\\b(Console|Task|List|Dictionary|IEnumerable|Action|Func|DateTime|Guid)\\b"
        applyRegex(code, types, theme.type, FontWeight.SemiBold)

        applyRegex(code, "\\b(true|false|null)\\b", theme.booleanNull, FontWeight.Bold)
        applyRegex(code, "\\b\\d+(\\.\\d+)?([fF|dD|mM|uU|lL])?\\b", theme.number)
        applyRegex(code, "\\b[A-Za-z0-9_]+(?=\\s*\\()", theme.function)
        applyRegex(code, "//.*$|/\\*[\\s\\S]*?\\*/", theme.comment)
    }

    private fun AnnotatedString.Builder.highlightGo(code: String, theme: SyntaxTheme) {
        applyRegex(code, "\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"|`[^`]*`|'[^'\\\\]*(?:\\\\.[^'\\\\]*)*'", theme.string)

        val keywords = "\\b(break|case|chan|const|continue|default|defer|else|fallthrough|for|func|go|goto|if|import|interface|map|package|range|return|select|struct|switch|type|var)\\b"
        applyRegex(code, keywords, theme.keyword, FontWeight.Bold)

        val types = "\\b(bool|byte|complex64|complex128|error|float32|float64|int|int8|int16|int32|int64|rune|string|uint|uint8|uint16|uint32|uint64|uintptr)\\b"
        applyRegex(code, types, theme.type, FontWeight.SemiBold)

        applyRegex(code, "\\b(true|false|iota|nil)\\b", theme.booleanNull, FontWeight.Bold)
        applyRegex(code, "\\b\\d+(\\.\\d+)?\\b|0x[0-9a-fA-F]+\\b", theme.number)
        applyRegex(code, "\\b[A-Za-z0-9_]+(?=\\s*\\()", theme.function)
        applyRegex(code, "//.*$|/\\*[\\s\\S]*?\\*/", theme.comment)
    }

    private fun AnnotatedString.Builder.highlightRust(code: String, theme: SyntaxTheme) {
        applyRegex(code, "\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"|r#\"[\\s\\S]*?\"#|'[^'\\\\]*'", theme.string)
        applyRegex(code, "#!\\[[^\\]]+\\]|#\\[[^\\]]+\\]", theme.annotation)

        val keywords = "\\b(as|async|await|break|const|continue|crate|dyn|else|enum|extern|false|fn|for|if|impl|in|let|loop|match|mod|move|mut|pub|ref|return|self|Self|static|struct|super|trait|true|type|unsafe|use|where|while)\\b"
        applyRegex(code, keywords, theme.keyword, FontWeight.Bold)

        val types = "\\b(i8|i16|i32|i64|i128|isize|u8|u16|u32|u64|u128|usize|f32|f64|bool|char|str|String|Vec|Option|Result|Some|None|Ok|Err|Box|Rc|Arc)\\b"
        applyRegex(code, types, theme.type, FontWeight.SemiBold)

        applyRegex(code, "\\b(true|false)\\b", theme.booleanNull, FontWeight.Bold)
        applyRegex(code, "\\b\\d+(_\\d+)*(\\.\\d+)?([ui](8|16|32|64|128|size)|f32|f64)?\\b", theme.number)
        applyRegex(code, "\\b[A-Za-z0-9_]+!(?=\\s*\\(|\\s*\\[|\\s*\\{)", theme.keyword, FontWeight.Bold) // Macros like println!
        applyRegex(code, "\\b[A-Za-z0-9_]+(?=\\s*\\()", theme.function)
        applyRegex(code, "//.*$|/\\*[\\s\\S]*?\\*/", theme.comment)
    }

    private fun AnnotatedString.Builder.highlightSwift(code: String, theme: SyntaxTheme) {
        applyRegex(code, "\"\"\"[\\s\\S]*?\"\"\"|\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"", theme.string)
        applyRegex(code, "@[A-Za-z0-9_]+", theme.annotation)

        val keywords = "\\b(associatedtype|class|deinit|enum|extension|fileprivate|func|import|init|inout|internal|let|open|operator|private|precedencegroup|protocol|public|rethrows|static|struct|subscript|typealias|var|break|case|catch|continue|default|defer|do|else|fallthrough|for|guard|if|in|repeat|return|throw|switch|where|while|as|Any|catch|false|is|nil|rethrows|super|self|Self|throws|true|try)\\b"
        applyRegex(code, keywords, theme.keyword, FontWeight.Bold)

        val types = "\\b(Int|Double|Float|Bool|String|Character|Array|Dictionary|Set|Optional)\\b"
        applyRegex(code, types, theme.type, FontWeight.SemiBold)

        applyRegex(code, "\\b(true|false|nil)\\b", theme.booleanNull, FontWeight.Bold)
        applyRegex(code, "\\b\\d+(\\.\\d+)?\\b", theme.number)
        applyRegex(code, "\\b[A-Za-z0-9_]+(?=\\s*\\()", theme.function)
        applyRegex(code, "//.*$|/\\*[\\s\\S]*?\\*/", theme.comment)
    }

    private fun AnnotatedString.Builder.highlightPhp(code: String, theme: SyntaxTheme) {
        applyRegex(code, "\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"|'[^'\\\\]*(?:\\\\.[^'\\\\]*)*'", theme.string)

        // PHP Variables $variable
        applyRegex(code, "\\$[A-Za-z0-9_]+", theme.variable, FontWeight.SemiBold)

        val keywords = "(?i)\\b(abstract|and|array|as|break|callable|case|catch|class|clone|const|continue|declare|default|die|do|echo|else|elseif|empty|enddeclare|endfor|endforeach|endif|endswitch|endwhile|eval|exit|extends|final|finally|fn|for|foreach|function|global|goto|if|implements|include|include_once|instanceof|insteadof|interface|isset|list|match|namespace|new|or|print|private|protected|public|readonly|require|require_once|return|static|switch|throw|trait|try|use|var|while|xor|yield)\\b"
        applyRegex(code, keywords, theme.keyword, FontWeight.Bold)

        applyRegex(code, "(?i)\\b(true|false|null)\\b", theme.booleanNull, FontWeight.Bold)
        applyRegex(code, "\\b\\d+(\\.\\d+)?\\b", theme.number)
        applyRegex(code, "\\b[A-Za-z0-9_]+(?=\\s*\\()", theme.function)
        applyRegex(code, "//.*$|#.*$|/\\*[\\s\\S]*?\\*/", theme.comment)
    }

    private fun AnnotatedString.Builder.highlightRuby(code: String, theme: SyntaxTheme) {
        applyRegex(code, "\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"|'[^'\\\\]*(?:\\\\.[^'\\\\]*)*'", theme.string)

        // Symbols (:symbol)
        applyRegex(code, ":[a-zA-Z0-9_]+", theme.propertyKey)

        // Instance / Class variables (@var, @@var)
        applyRegex(code, "@@?[A-Za-z0-9_]+", theme.variable, FontWeight.SemiBold)

        val keywords = "\\b(__ENCODING__|__LINE__|__FILE__|BEGIN|END|alias|and|begin|break|case|class|def|defined\\?|do|else|elsif|end|ensure|false|for|if|in|module|next|nil|not|or|redo|rescue|retry|return|self|super|then|true|undef|unless|until|when|while|yield|puts|require|require_relative|attr_accessor|attr_reader|attr_writer)\\b"
        applyRegex(code, keywords, theme.keyword, FontWeight.Bold)

        applyRegex(code, "\\b(true|false|nil)\\b", theme.booleanNull, FontWeight.Bold)
        applyRegex(code, "\\b\\d+(\\.\\d+)?\\b", theme.number)
        applyRegex(code, "\\b[A-Za-z0-9_]+(?=\\s*\\()", theme.function)
        applyRegex(code, "(?m)#.*$", theme.comment)
    }

    private fun AnnotatedString.Builder.highlightDart(code: String, theme: SyntaxTheme) {
        applyRegex(code, "\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"|'[^'\\\\]*(?:\\\\.[^'\\\\]*)*'", theme.string)
        applyRegex(code, "@[A-Za-z0-9_]+", theme.annotation)

        val keywords = "\\b(abstract|as|assert|async|await|break|case|catch|class|const|continue|covariant|default|deferred|do|dynamic|else|enum|export|extends|extension|external|factory|false|final|finally|for|Function|get|hide|if|implements|import|in|interface|is|late|library|mixin|new|null|of|on|operator|part|rethrow|return|set|show|static|super|switch|sync|this|throw|true|try|typedef|var|void|while|with|yield)\\b"
        applyRegex(code, keywords, theme.keyword, FontWeight.Bold)

        val types = "\\b(int|double|num|bool|String|List|Map|Set|Iterable|Future|Stream|Widget|StatelessWidget|StatefulWidget)\\b"
        applyRegex(code, types, theme.type, FontWeight.SemiBold)

        applyRegex(code, "\\b(true|false|null)\\b", theme.booleanNull, FontWeight.Bold)
        applyRegex(code, "\\b\\d+(\\.\\d+)?\\b", theme.number)
        applyRegex(code, "\\b[A-Za-z0-9_]+(?=\\s*\\()", theme.function)
        applyRegex(code, "//.*$|/\\*[\\s\\S]*?\\*/", theme.comment)
    }

    private fun AnnotatedString.Builder.highlightLua(code: String, theme: SyntaxTheme) {
        applyRegex(code, "\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"|'[^'\\\\]*(?:\\\\.[^'\\\\]*)*'|\\[\\[[\\s\\S]*?\\]\\]", theme.string)

        val keywords = "\\b(and|break|do|else|elseif|end|false|for|function|goto|if|in|local|nil|not|or|repeat|return|then|true|until|while)\\b"
        applyRegex(code, keywords, theme.keyword, FontWeight.Bold)

        val globals = "\\b(print|pairs|ipairs|tostring|tonumber|type|require|table|string|math|io|os)\\b"
        applyRegex(code, globals, theme.function, FontWeight.SemiBold)

        applyRegex(code, "\\b(true|false|nil)\\b", theme.booleanNull, FontWeight.Bold)
        applyRegex(code, "\\b\\d+(\\.\\d+)?\\b", theme.number)
        applyRegex(code, "--\\[\\[[\\s\\S]*?\\]\\]|--.*$", theme.comment)
    }

    // ==========================================
    // Scripting & Config Highlighters
    // ==========================================

    private fun AnnotatedString.Builder.highlightBash(code: String, theme: SyntaxTheme) {
        applyRegex(code, "\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"|'[^']*'", theme.string)
        applyRegex(code, "\\$\\{?[A-Za-z0-9_]+\\}?", theme.variable, FontWeight.SemiBold)

        val bashKeywords = "\\b(if|then|else|elif|fi|for|in|do|done|while|until|case|esac|function|return|exit|export|source|local|alias|echo|printf|read|cd|sudo|mkdir|rm|cp|mv|cat|grep|sed|awk|curl|wget|chmod|chown|source|test|sh|bash)\\b"
        applyRegex(code, bashKeywords, theme.keyword, FontWeight.Bold)
        applyRegex(code, "(?<=\\s)--?[A-Za-z0-9\\-]+", theme.propertyKey)
        applyRegex(code, "\\b\\d+\\b", theme.number)
        applyRegex(code, "(\\|{1,2}|&&|>>|>|<|;|\\$)", theme.operator)
        applyRegex(code, "(?m)#.*$", theme.comment)
    }

    private fun AnnotatedString.Builder.highlightJson(code: String, theme: SyntaxTheme) {
        applyRegex(code, "\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"(?=\\s*:)", theme.propertyKey, FontWeight.SemiBold)
        applyRegex(code, "(?<=:\\s{0,5})\".*?\"(?=[,\\s\\]\\}]|$)", theme.string)
        applyRegex(code, "\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"", theme.string)
        applyRegex(code, "\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"(?=\\s*:)", theme.propertyKey, FontWeight.SemiBold)
        applyRegex(code, "-?\\b\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?\\b", theme.number)
        applyRegex(code, "\\b(true|false|null)\\b", theme.booleanNull, FontWeight.Bold)
        applyRegex(code, "[\\[\\]\\{\\}]", theme.keyword, FontWeight.Bold)
    }

    private fun AnnotatedString.Builder.highlightYaml(code: String, theme: SyntaxTheme) {
        applyRegex(code, "(?m)^[ \\t]*[a-zA-Z0-9_\\-\\.\\/]+(?=\\s*:)", theme.propertyKey, FontWeight.SemiBold)
        applyRegex(code, "(?m)^[ \\t]*-[ \\t]+[a-zA-Z0-9_\\-\\.\\/]+(?=\\s*:)", theme.propertyKey, FontWeight.SemiBold)
        applyRegex(code, "\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"|'[^']*'", theme.string)
        applyRegex(code, "(?i)\\b(true|false|yes|no|on|off|null|~)\\b", theme.booleanNull, FontWeight.Bold)
        applyRegex(code, "\\b\\d+(?:\\.\\d+)?\\b", theme.number)
        applyRegex(code, "(?m)^[ \\t]*-[ \\t]", theme.operator, FontWeight.Bold)
        applyRegex(code, "(?m)#.*$", theme.comment)
    }

    private fun AnnotatedString.Builder.highlightPython(code: String, theme: SyntaxTheme) {
        applyRegex(code, "\"\"\"[\\s\\S]*?\"\"\"|'''[\\s\\S]*?'''|\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"|'[^'\\\\]*(?:\\\\.[^'\\\\]*)*'", theme.string)
        applyRegex(code, "@[A-Za-z0-9_]+", theme.annotation)

        val pyKeywords = "\\b(def|class|if|elif|else|while|for|in|return|yield|import|from|as|try|except|finally|raise|with|pass|continue|break|lambda|global|nonlocal|assert|del|async|await|and|or|not|is)\\b"
        applyRegex(code, pyKeywords, theme.keyword, FontWeight.Bold)

        val pyBuiltins = "\\b(self|cls|True|False|None|print|len|range|int|str|float|list|dict|set|tuple|bool|open|type|enumerate|zip|isinstance|map|filter|all|any)\\b"
        applyRegex(code, pyBuiltins, theme.variable, FontWeight.SemiBold)

        applyRegex(code, "\\b\\d+(?:\\.\\d+)?\\b", theme.number)
        applyRegex(code, "\\bdef\\s+([A-Za-z0-9_]+)", theme.function, FontWeight.Bold)
        applyRegex(code, "\\bclass\\s+([A-Za-z0-9_]+)", theme.type, FontWeight.Bold)
        applyRegex(code, "(?m)#.*$", theme.comment)
    }

    private fun AnnotatedString.Builder.highlightEnv(code: String, theme: SyntaxTheme) {
        applyRegex(code, "(?m)^[ \\t]*[A-Za-z0-9_\\-\\.]+(?=\\s*=)", theme.propertyKey, FontWeight.Bold)
        applyRegex(code, "\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"|'[^']*'", theme.string)
        applyRegex(code, "\\$\\{?[A-Za-z0-9_]+\\}?", theme.variable)
        applyRegex(code, "(?i)\\b(true|false|yes|no|on|off)\\b", theme.booleanNull, FontWeight.Bold)
        applyRegex(code, "(?m)#.*$", theme.comment)
    }

    private fun AnnotatedString.Builder.highlightIniToml(code: String, theme: SyntaxTheme) {
        applyRegex(code, "(?m)^\\[[^\\]]+\\]", theme.keyword, FontWeight.Bold)
        applyRegex(code, "(?m)^[ \\t]*[a-zA-Z0-9_\\-\\.]+(?=\\s*=)", theme.propertyKey, FontWeight.SemiBold)
        applyRegex(code, "\"[^\"\\\\]*(?:\\\\.[^\"\\\\]*)*\"|'[^']*'", theme.string)
        applyRegex(code, "(?i)\\b(true|false|yes|no|on|off)\\b", theme.booleanNull, FontWeight.Bold)
        applyRegex(code, "\\b\\d+(?:\\.\\d+)?\\b", theme.number)
        applyRegex(code, "(?m)[#;].*$", theme.comment)
    }

    private fun AnnotatedString.Builder.highlightMarkdown(code: String, theme: SyntaxTheme) {
        applyRegex(code, "(?m)^#{1,6}\\s+.*$", theme.keyword, FontWeight.Bold)
        applyRegex(code, "`[^`\\n]+`", theme.function)
        applyRegex(code, "```[\\s\\S]*?```", theme.string)
        applyRegex(code, "(?m)^[ \\t]*[-*+]\\s+", theme.operator, FontWeight.Bold)
        applyRegex(code, "(?m)^[ \\t]*\\d+\\.\\s+", theme.number)
        applyRegex(code, "\\*\\*[^*]+\\*\\*|__[^_]+__", theme.propertyKey, FontWeight.Bold)
        applyRegex(code, "\\[[^\\]]+\\]\\([^\\)]+\\)", theme.variable)
    }

    private fun AnnotatedString.Builder.applyRegex(
        text: String,
        regexPattern: String,
        color: Color,
        fontWeight: FontWeight? = null
    ) {
        try {
            val pattern = Pattern.compile(regexPattern)
            val matcher = pattern.matcher(text)
            while (matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()
                if (start in text.indices && end <= text.length && start < end) {
                    addStyle(
                        SpanStyle(color = color, fontWeight = fontWeight),
                        start,
                        end
                    )
                }
            }
        } catch (_: Exception) {
            // Graceful regex safety
        }
    }
}

class SyntaxVisualTransformation(
    private val language: SupportedLanguage,
    private val theme: SyntaxTheme,
    private val searchQuery: String = ""
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val highlighted = SyntaxHighlighter.highlight(
            code = text.text,
            language = language,
            theme = theme,
            searchQuery = searchQuery
        )
        return TransformedText(highlighted, OffsetMapping.Identity)
    }
}
