package com.estrin217.codetools.syntax

import androidx.compose.ui.graphics.Color

data class SyntaxTheme(
    val id: String,
    val name: String,
    val isDark: Boolean,
    val background: Color,
    val gutterBackground: Color,
    val activeLineBackground: Color,
    val text: Color,
    val lineNumber: Color,
    val activeLineNumber: Color,
    val keyword: Color,
    val string: Color,
    val comment: Color,
    val number: Color,
    val function: Color,
    val variable: Color,
    val propertyKey: Color,
    val operator: Color,
    val punctuation: Color,
    val booleanNull: Color,
    val annotation: Color,
    val type: Color,
    val searchMatchBackground: Color
)

object SyntaxThemes {
    val OneDark = SyntaxTheme(
        id = "one_dark",
        name = "One Dark (IDE)",
        isDark = true,
        background = Color(0xFF1E1E2E),
        gutterBackground = Color(0xFF181825),
        activeLineBackground = Color(0xFF28283D),
        text = Color(0xFFCDD6F4),
        lineNumber = Color(0xFF6C7086),
        activeLineNumber = Color(0xFF89B4FA),
        keyword = Color(0xFFCBA6F7), // purple
        string = Color(0xFFA6E3A1),  // green
        comment = Color(0xFF7F849C), // muted gray/blue
        number = Color(0xFFFAB387),  // peach/orange
        function = Color(0xFF89B4FA),// blue
        variable = Color(0xFFF38BA8),// red/rose
        propertyKey = Color(0xFF89DCEB), // sky cyan
        operator = Color(0xFF94E2D5), // teal
        punctuation = Color(0xFFBAC2DE),
        booleanNull = Color(0xFFF9E2AF), // yellow
        annotation = Color(0xFFF9E2AF), // golden yellow
        type = Color(0xFF89DCEB),       // cyan
        searchMatchBackground = Color(0x66F9E2AF)
    )

    val Monokai = SyntaxTheme(
        id = "monokai",
        name = "Monokai Pro",
        isDark = true,
        background = Color(0xFF272822),
        gutterBackground = Color(0xFF1E1F1C),
        activeLineBackground = Color(0xFF3E3D32),
        text = Color(0xFFF8F8F2),
        lineNumber = Color(0xFF75715E),
        activeLineNumber = Color(0xFFFD971F),
        keyword = Color(0xFFF92672),
        string = Color(0xFFE6DB74),
        comment = Color(0xFF75715E),
        number = Color(0xFFAE81FF),
        function = Color(0xFFA6E22E),
        variable = Color(0xFFFD971F),
        propertyKey = Color(0xFF66D9EF),
        operator = Color(0xFFF92672),
        punctuation = Color(0xFFF8F8F2),
        booleanNull = Color(0xFFAE81FF),
        annotation = Color(0xFFE6DB74),
        type = Color(0xFF66D9EF),
        searchMatchBackground = Color(0x66FFE792)
    )

    val GitHubLight = SyntaxTheme(
        id = "github_light",
        name = "GitHub Light",
        isDark = false,
        background = Color(0xFFF6F8FA),
        gutterBackground = Color(0xFFEBEEF2),
        activeLineBackground = Color(0xFFE1E4E8),
        text = Color(0xFF24292E),
        lineNumber = Color(0xFF959DA5),
        activeLineNumber = Color(0xFF0366D6),
        keyword = Color(0xFFD73A49),
        string = Color(0xFF032F62),
        comment = Color(0xFF6A737D),
        number = Color(0xFF005CC5),
        function = Color(0xFF6F42C1),
        variable = Color(0xFFE36209),
        propertyKey = Color(0xFF22863A),
        operator = Color(0xFFD73A49),
        punctuation = Color(0xFF24292E),
        booleanNull = Color(0xFF005CC5),
        annotation = Color(0xFF6F42C1),
        type = Color(0xFF005CC5),
        searchMatchBackground = Color(0x66FFD33D)
    )

    val all = listOf(OneDark, Monokai, GitHubLight)
}
