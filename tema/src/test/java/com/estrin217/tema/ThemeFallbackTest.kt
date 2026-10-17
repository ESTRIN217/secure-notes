package com.estrin217.tema

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Qué esquema y qué paleta usa cada tema (spec 025, RF-161, RF-166, RF-167, RF-168).
 *
 * Aquí está el corazón de la decisión de diseño D4: **el tema dinámico no tiene paleta propia**, y sus dos
 * caídas son distintas a propósito. El editor cae a la paleta del editor de siempre y el bloque de nota cae a la
 * del bloque de siempre, porque son dos superficies que ya se veían de dos maneras diferentes y esta spec no
 * tenía que arreglarlo.
 *
 * Y por eso el `CompositionLocal` lleva el `AppTheme` entero en vez de una paleta: si llevara una, habría que
 * elegir una de las dos y perder la otra.
 */
class ThemeFallbackTest {

    @Test
    fun `el tema dinamico no tiene esquema propio`() {
        assertNull(AppThemes.schemeFor(AppThemes.dynamic, dark = false))
        assertNull(AppThemes.schemeFor(AppThemes.dynamic, dark = true))
    }

    @Test
    fun `un tema fijo devuelve el esquema del modo pedido`() {
        AppThemes.all.filter { !it.isDynamic }.forEach { tema ->
            assertSame("${tema.id} en claro", tema.light, AppThemes.schemeFor(tema, dark = false))
            assertSame("${tema.id} en oscuro", tema.dark, AppThemes.schemeFor(tema, dark = true))
        }
    }

    @Test
    fun `el editor usa la paleta del tema`() {
        assertSame(SyntaxThemes.VSCodeDark, AppThemes.editorPalette(AppThemes.byId("vscode_dark")))
        assertSame(SyntaxThemes.OneDark, AppThemes.editorPalette(AppThemes.byId("one_dark")))
        assertSame(SyntaxThemes.Monokai, AppThemes.editorPalette(AppThemes.byId("monokai")))
        assertSame(SyntaxThemes.GitHubLight, AppThemes.editorPalette(AppThemes.byId("github_light")))
    }

    @Test
    fun `el editor del tema dinamico usa la paleta del editor de siempre`() {
        // Sin esto, quien usa Material You —que es el tema por defecto— abriría el editor con otros colores
        // respecto a como lo ve hoy.
        assertSame(SyntaxThemes.VSCodeDark, AppThemes.editorPalette(AppThemes.dynamic))
    }

    @Test
    fun `el bloque de nota usa la paleta del tema sin su superficie`() {
        val bloque = AppThemes.notePalette(AppThemes.byId("monokai"))
        val monokai = SyntaxThemes.Monokai

        assertEquals("el keyword del bloque es el del tema", monokai.keyword, bloque.keyword)
        assertEquals("el fondo lo pone el tema Material, no la paleta", Color.Unspecified, bloque.background)
        assertEquals("el texto base lo pone el tema Material", Color.Unspecified, bloque.text)
    }

    @Test
    fun `el bloque de nota del tema dinamico usa los acentos de siempre`() {
        val bloque = AppThemes.notePalette(AppThemes.dynamic)
        val estandar = SyntaxThemes.Standard

        assertEquals(estandar.keyword, bloque.keyword)
        assertEquals(estandar.string, bloque.string)
        assertEquals(estandar.comment, bloque.comment)
        assertEquals(Color.Unspecified, bloque.background)
    }

    @Test
    fun `el editor y el bloque del tema dinamico caen a paletas distintas`() {
        // La comprobación que justifica el `CompositionLocal` con el tema entero: las dos caídas son
        // deliberadamente diferentes, así que no puede llevar una sola paleta.
        assertSame(
            "el editor y el bloque no pueden caer a la misma paleta",
            false,
            AppThemes.editorPalette(AppThemes.dynamic) == AppThemes.notePalette(AppThemes.dynamic),
        )
    }
}