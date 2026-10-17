package com.estrin217.tema

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Un tema de la app: un identificador, y —si es fijo— los colores que lo pinning (spec 025).
 *
 * Un tema **no** son 48 colores escritos a mano ni una sola paleta: son tres partes que van juntas y que no
 * pueden faltar por separado. De ahí los nulos y el `require` del constructor:
 *
 * - [light] y [dark]: los dos lados de Material. Los dos o ninguno, porque un tema que solo tiene el claro se ve
 *   bien hasta que el sistema se pone oscuro.
 * - [syntax]: la paleta con la que se pinta el código, en el editor y en las notas.
 *
 * El tema [AppThemes.dynamic] es el único sin nada de esto, y [AppThemes.dynamic] no es una coincidencia: es lo
 * que mantiene el aspecto de quien ya usaba la app (RF-164, RF-165, HU-4).
 */
data class AppTheme(
    val id: String,
    val isDynamic: Boolean = false,
    val light: ColorScheme? = null,
    val dark: ColorScheme? = null,
    val syntax: SyntaxTheme? = null,
) {
    init {
        val vacio = light == null && dark == null && syntax == null
        val completo = light != null && dark != null && syntax != null
        require(if (isDynamic) vacio else completo) {
            "el tema '$id' está a medias: un tema fijo lleva los tres colores y uno dinámico ninguno " +
                "(dinámico=$isDynamic, claro=${light != null}, oscuro=${dark != null}, paleta=${syntax != null})"
        }
    }
}

/**
 * Los seis temas de la app (spec 025, RF-160).
 *
 * Tres son nuevos y tres ya existían, que es el punto de la spec: `default` es el morado que la app llevaba, y
 * `vscode_dark`, `one_dark`, `monokai` y `github_light` son las paletas que el editor llevaba y que no se veían
 * fuera de él. El que aparece primero es el que se usa cuando no se sabe cuál, y por eso es `dynamic`: es lo que
 * la app muestra hoy por defecto.
 *
 * Las semillas de los cuatro temas fijos son tres acentos de su propia paleta —el azul de VS Code, el rosa de
 * Monokai— y el resto de los 48 roles sale de [TonalScheme]. Los dos morados de `default` se escriben literales
 * porque son los de siempre y no hay razón para volver a generarlos (RNF-103).
 */
object AppThemes {

    /** El tema del sistema: sin colores propios, se los pide a Android. */
    val dynamic = AppTheme(id = "dynamic", isDynamic = true)

    /** El morado de la app antes de esta spec, con sus dos esquemas literales. */
    val default = AppTheme(
        id = "default",
        light = lightColorScheme(primary = PURPLE_40, secondary = PURPLE_GREY_40, tertiary = PINK_40),
        dark = darkColorScheme(primary = PURPLE_80, secondary = PURPLE_GREY_80, tertiary = PINK_80),
        syntax = SyntaxThemes.Standard,
    )

    val vscodeDark = fixed(
        id = "vscode_dark",
        seeds = ThemeSeeds(primary = Color(0xFF3794FF), secondary = Color(0xFF4EC9B0), tertiary = Color(0xFFCE9178)),
        syntax = SyntaxThemes.VSCodeDark,
    )

    val oneDark = fixed(
        id = "one_dark",
        seeds = ThemeSeeds(primary = Color(0xFF89B4FA), secondary = Color(0xFF94E2D5), tertiary = Color(0xFFF9E2AF)),
        syntax = SyntaxThemes.OneDark,
    )

    val monokai = fixed(
        id = "monokai",
        seeds = ThemeSeeds(primary = Color(0xFFF92672), secondary = Color(0xFFA6E22E), tertiary = Color(0xFFE6DB74)),
        syntax = SyntaxThemes.Monokai,
    )

    val githubLight = fixed(
        id = "github_light",
        seeds = ThemeSeeds(primary = Color(0xFF0969DA), secondary = Color(0xFF8250DF), tertiary = Color(0xFF1A7F37)),
        syntax = SyntaxThemes.GitHubLight,
    )

    /** El catálogo, en el orden en que se pintan en el selector de Ajustes. */
    val all = listOf(dynamic, default, vscodeDark, oneDark, monokai, githubLight)

    /**
     * El tema de un identificador guardado, o el dinámico si no se sabe cuál (RF-165).
     *
     * No lanza ni con `null` ni con basura: un identificador de otra versión o corrupto tiene que dejar la app con
     * el tema de siempre, no con una pantalla en negro.
     */
    fun byId(id: String?): AppTheme = all.firstOrNull { it.id == id } ?: dynamic

    /** El esquema del modo pedido, o `null` si el tema se lo deja al sistema (RF-161). */
    fun schemeFor(theme: AppTheme, dark: Boolean): ColorScheme? =
        if (theme.isDynamic) null else if (dark) theme.dark else theme.light

    /**
     * La paleta con la que se pinta el editor de código (RF-166).
     *
     * El editor se pinta entero con su paleta —fondo, canal, línea activa—, así que necesita una paleta completa.
     * Un tema sin colores propios no puede tenerla, y la del editor de siempre es la que se ve hoy.
     */
    fun editorPalette(theme: AppTheme): SyntaxTheme = theme.syntax ?: SyntaxThemes.VSCodeDark

    /**
     * La paleta con la que se pinta un bloque de código dentro de una nota (RF-167, RF-168).
     *
     * No es la del editor: un bloque no tiene fondo propio ni línea activa, así que suelta las superficies y deja
     * que las ponga el tema Material de la pantalla. Y cuando el tema no trae paleta, cae a la del bloque de
     * siempre, que es distinta de la del editor a propósito: son dos superficies que ya se veían de dos maneras.
     */
    fun notePalette(theme: AppTheme): SyntaxTheme =
        (theme.syntax ?: SyntaxThemes.Standard).forNoteBlock()

    /** Un tema fijo: sus dos esquemas salen de las tres semillas, una vez, al cargarse el catálogo. */
    private fun fixed(id: String, seeds: ThemeSeeds, syntax: SyntaxTheme) = AppTheme(
        id = id,
        light = TonalScheme.scheme(seeds, dark = false),
        dark = TonalScheme.scheme(seeds, dark = true),
        syntax = syntax,
    )
}

// Los morados de siempre, los de `app/ui/theme/Color.kt` antes de esta spec. Moverlos aquí es parte del cambio:
// el catálogo de la app no puede vivir a medias en dos módulos.
private val PURPLE_80 = Color(0xFFD0BCFF)
private val PURPLE_GREY_80 = Color(0xFFCCC2DC)
private val PINK_80 = Color(0xFFEFB8C8)
private val PURPLE_40 = Color(0xFF6650A4)
private val PURPLE_GREY_40 = Color(0xFF625B71)
private val PINK_40 = Color(0xFF7D5260)