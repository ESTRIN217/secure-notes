package com.example.ui.theme

import com.estrin217.tema.AppTheme
import com.example.R

/**
 * El nombre visible de cada tema (spec 025, D9, RNF-102).
 *
 * Vive en `:app` y no en `:tema` porque son palabras de la interfaz, y las palabras de la interfaz salen de los
 * `strings.xml` de `:app` —los nueve idiomas están aquí— mientras que el módulo del catálogo es solo datos. La
 * consecuencia buscada es que el catálogo no tenga ni un recurso: seis claves en el sitio donde ya viven las otras
 * 1.179.
 *
 * Los cuatro nombres de paleta son nombres propios de paletas de terceros —Monokai, One Dark— y su traducción es
 * la misma en los nueve idiomas, pero están aquí igualmente y no como literales en el código.
 *
 * Es una función pura con seis entradas: el selector la puede probar sin montar nada.
 */
fun labelRes(theme: AppTheme): Int = when (theme.id) {
    "dynamic" -> R.string.theme_dynamic
    "default" -> R.string.theme_default
    "vscode_dark" -> R.string.theme_vscode_dark
    "one_dark" -> R.string.theme_one_dark
    "monokai" -> R.string.theme_monokai
    "github_light" -> R.string.theme_github_light
    else -> error(
        "el tema '${theme.id}' no tiene nombre. El catálogo tiene seis temas y este no es uno de ellos: " +
            "o falta la entrada aquí, o sobra en AppThemes.",
    )
}