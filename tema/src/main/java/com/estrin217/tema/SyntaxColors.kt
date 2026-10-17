package com.estrin217.tema

import androidx.compose.ui.graphics.Color

/**
 * Los 22 colores con los que el editor de código y el bloque de código de una nota pintan el código (spec 025,
 * RF-159).
 *
 * Este tipo **no se inventó aquí**: vivía en `:editor-de-codigo`, en `syntax/SyntaxColors.kt`, y solo se mudó de
 * módulo para que la app pudiera leerlo. Los 22 campos, sus nombres y sus valores son los de siempre, y
 * `ThemeCatalogPalettesTest` lo comprueba.
 *
 * Un «tema» de editor no es lo mismo que un «tema» de la app, aunque el catálogo los junte ([AppTheme]): aquí solo
 * hay colores de sintaxis, sin superficies de Material. De ahí que `background` sea el fondo del editor y no el
 * `surface` de la app.
 */
data class SyntaxTheme(
    val id: String,
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
    val searchMatchBackground: Color,
) {

    /**
     * CUANDO se pinta código dentro de una nota, EL SISTEMA suelta el fondo y el color base (RF-167).
     *
     * Es la pieza que hace que el bloque de nota no traiga su propia superficie: los catorce **acentos** se
     * quedan tal cual —son los del tema, que es justo lo que se pidió— y los siete campos de superficie pasan a
     * `Color.Unspecified` para que los ponga el tema Material de la pantalla que contiene el bloque.
     *
     * Poner `Unspecified` a mano, campo a campo, en cada llamante, es el error que este método evita: son
     * catorce acentos y siete superficies, y basta con olvidar uno para que el bloque se vea distinto según por
     * dónde se mire.
     */
    fun forNoteBlock(): SyntaxTheme = copy(
        background = Color.Unspecified,
        gutterBackground = Color.Unspecified,
        activeLineBackground = Color.Unspecified,
        text = Color.Unspecified,
        lineNumber = Color.Unspecified,
        activeLineNumber = Color.Unspecified,
        searchMatchBackground = Color.Unspecified,
    )
}

/**
 * Las paletas del editor de código, sin cambios de valor (spec 025, RF-159, RNF-103).
 *
 * Las cuatro son las que el editor tenía, con el mismo orden y el mismo identificador: el orden importa porque es
 * el que se pintaba en el selector del editor, y el identificador es el que se guarda en el catálogo de la app.
 */
object SyntaxThemes {

    val VSCodeDark = SyntaxTheme(
        id = "vscode_dark",
        isDark = true,
        background = Color(0xFF1F1F1F),
        gutterBackground = Color(0xFF181818),
        activeLineBackground = Color(0xFF282828),
        text = Color(0xFFCCCCCC),
        lineNumber = Color(0xFF6E7681),
        activeLineNumber = Color(0xFFE0E0E0),
        keyword = Color(0xFF569CD6),
        string = Color(0xFFCE9178),
        comment = Color(0xFF6A9955),
        number = Color(0xFFB5CEA8),
        function = Color(0xFFDCDCAA),
        variable = Color(0xFF9CDCFE),
        propertyKey = Color(0xFF9CDCFE),
        operator = Color(0xFFD4D4D4),
        punctuation = Color(0xFFCCCCCC),
        booleanNull = Color(0xFF569CD6),
        annotation = Color(0xFF4EC9B0),
        type = Color(0xFF4EC9B0),
        searchMatchBackground = Color(0x66515C6A)
    )

    val OneDark = SyntaxTheme(
        id = "one_dark",
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

    /**
     * La paleta que comparten el tema `default` y el bloque de nota de siempre (spec 025, RF-167, RF-168).
     *
     * No es una quinta paleta del editor: es **la del bloque de nota con superficie**, que es decir los doce
     * acentos de `CodeSyntax.blockTheme` —los que la app lleva usando desde antes de esta spec— sobre una
     * superficie oscura neutra.
     *
     * Por qué está aquí y no en `:app`: es la paleta de un tema del catálogo, y el catálogo es de `:tema`. Y por
     * qué **no** hay dos paletas iguales: si el bloque de nota y el tema `default` tuvieran cada uno su copia de
     * estos doce colores, cambiar el color de una palabra clave sería cambiarlo en dos sitios.
     *
     * Sus superficies son las de un editor oscuro y neutro, no las del bloque: el bloque de nota no pinta fondo
     * (eso lo pone [forNoteBlock]), pero el editor sí, y cuando el tema es `default` el editor necesita una.
     */
    val Standard = SyntaxTheme(
        id = "standard",
        isDark = true,
        background = Color(0xFF1F1F1F),
        gutterBackground = Color(0xFF181818),
        activeLineBackground = Color(0xFF282828),
        text = Color(0xFFCCCCCC),
        lineNumber = Color(0xFF6E7681),
        activeLineNumber = Color(0xFFE0E0E0),
        keyword = Color(0xFF3B82F6),
        string = Color(0xFF16A34A),
        comment = Color(0xFF8B93A3),
        number = Color(0xFF0891B2),
        function = Color(0xFFA855F7),
        variable = Color(0xFFF59E0B),
        propertyKey = Color(0xFFF59E0B),
        operator = Color(0xFF0D9488),
        punctuation = Color(0xFF8B93A3),
        booleanNull = Color(0xFF3B82F6),
        annotation = Color(0xFF10B981),
        type = Color(0xFF9333EA),
        searchMatchBackground = Color(0x66C586C0)
    )

    val all = listOf(VSCodeDark, OneDark, Monokai, GitHubLight)
}