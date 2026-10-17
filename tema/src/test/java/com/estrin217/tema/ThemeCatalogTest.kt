package com.estrin217.tema

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El catálogo de temas: seis entradas, tres reglas y ninguna a medio declarar (spec 025, RF-160, RF-161).
 *
 * Es la prueba que decide si la app tiene un tema o un montón de colores sueltos. Si mañana alguien añade un
 * tema con esquema claro pero sin el oscuro —que es la forma natural de equivocarse al copiar y pegar— falla
 * aquí, no en la pantalla de alguien.
 */
class ThemeCatalogTest {

    @Test
    fun `el catalogo tiene los seis temas con su identificador`() {
        assertEquals(
            "los seis temas del catálogo, en su orden",
            listOf("dynamic", "default", "vscode_dark", "one_dark", "monokai", "github_light"),
            AppThemes.all.map { it.id },
        )
    }

    @Test
    fun `ningun identificador se repite`() {
        val repetidos = AppThemes.all.groupBy { it.id }.filterValues { it.size > 1 }.keys

        assertEquals("identificadores repetidos: $repetidos", emptySet<String>(), repetidos)
    }

    @Test
    fun `un tema dinamico no declara colores propios`() {
        val dinamico = AppThemes.byId("dynamic")

        assertTrue("el tema dinámico es el único que no trae colores", dinamico.isDynamic)
        assertEquals("un tema dinámico no lleva esquema claro", null, dinamico.light)
        assertEquals("un tema dinámico no lleva esquema oscuro", null, dinamico.dark)
        assertEquals("un tema dinámico no lleva paleta de resaltado", null, dinamico.syntax)
    }

    @Test
    fun `cada tema fijo trae los tres colores que necesita`() {
        AppThemes.all.filter { !it.isDynamic }.forEach { tema ->
            assertTrue("${tema.id} sin esquema claro", tema.light != null)
            assertTrue("${tema.id} sin esquema oscuro", tema.dark != null)
            assertTrue("${tema.id} sin paleta de resaltado", tema.syntax != null)
        }
    }

    @Test
    fun `cada tema fijo se ve distinto en claro y en oscuro`() {
        AppThemes.all.filter { !it.isDynamic }.forEach { tema ->
            val claro = tema.light!!
            val oscuro = tema.dark!!
            assertTrue(
                "${tema.id}: la superficie clara tiene que ser más clara que la oscura",
                Contrast.relativeLuminance(claro.surface) > Contrast.relativeLuminance(oscuro.surface),
            )
        }
    }

    @Test
    fun `un tema a medio declarar no se puede construir`() {
        // El `require` del constructor es lo que hace imposible el error; la prueba comprueba que está.
        assertThrows(IllegalArgumentException::class.java) {
            AppTheme(id = "a_medias", isDynamic = false, light = null, dark = null, syntax = null)
        }
        assertThrows(IllegalArgumentException::class.java) {
            AppTheme(id = "a_medias", isDynamic = false, syntax = SyntaxThemes.OneDark)
        }
        // Y el `require` también salta al copiar: no vale la pena tener un tema bueno y corromperlo con un `copy`.
        assertThrows(IllegalArgumentException::class.java) {
            AppThemes.vscodeDark.copy(dark = null)
        }
        assertThrows(IllegalArgumentException::class.java) {
            AppThemes.dynamic.copy(isDynamic = false)
        }
    }

    @Test
    fun `un identificador guardado devuelve su tema`() {
        AppThemes.all.forEach { tema ->
            assertSame("el identificador ${tema.id} no devolvió su tema", tema, AppThemes.byId(tema.id))
        }
    }

    @Test
    fun `un identificador que no existe cae en el tema dinamico`() {
        // Es lo que evita que un identificador de una versión anterior, o uno corrupto, deje la app sin tema
        // (RF-165). No es una hipótesis: `byId` no lanza.
        listOf(null, "", "   ", "no_existe", "VSCODE_DARK", "default_v2").forEach { id ->
            assertSame("el identificador '$id' debería caer en el dinámico", AppThemes.dynamic, AppThemes.byId(id))
        }
    }

    @Test
    fun `el tema dinamico es el que se usa cuando no se sabe cual es`() {
        assertSame(AppThemes.dynamic, AppThemes.byId(null))
        assertTrue("y es dinámico", AppThemes.byId(null).isDynamic)
    }

    @Test
    fun `cada tema fijo lleva la paleta de su familia`() {
        assertSame(SyntaxThemes.VSCodeDark, AppThemes.byId("vscode_dark").syntax)
        assertSame(SyntaxThemes.OneDark, AppThemes.byId("one_dark").syntax)
        assertSame(SyntaxThemes.Monokai, AppThemes.byId("monokai").syntax)
        assertSame(SyntaxThemes.GitHubLight, AppThemes.byId("github_light").syntax)
        assertSame(SyntaxThemes.Standard, AppThemes.byId("default").syntax)
    }

    @Test
    fun `el tema predeterminado conserva sus morados`() {
        // RNF-103: `default` es el morado de siempre. Sus roles sin especificar son los de la línea base de
        // Material, también como entonces, y por eso se comparan algunos y no todos.
        val claro = AppThemes.byId("default").light!!
        val oscuro = AppThemes.byId("default").dark!!

        assertEquals(0xFF6650A4.toInt(), claro.primary.argb())
        assertEquals(0xFF625B71.toInt(), claro.secondary.argb())
        assertEquals(0xFF7D5260.toInt(), claro.tertiary.argb())
        assertEquals(0xFFD0BCFF.toInt(), oscuro.primary.argb())
        assertEquals(0xFFCCC2DC.toInt(), oscuro.secondary.argb())
        assertEquals(0xFFEFB8C8.toInt(), oscuro.tertiary.argb())
    }

    @Test
    fun `un tema generado no se parece a los morados de la linea base`() {
        // Si el generador se olvidara de algún rol, ese rol se quedaría en el morado de la línea base y el tema
        // sería unFrankenstein. Basta mirar uno para verlo.
        AppThemes.all.filter { !it.isDynamic && it.id != "default" }.forEach { tema ->
            listOf("claro" to tema.light!!, "oscuro" to tema.dark!!).forEach { (modo, esquema) ->
                assertNotEquals(
                    "${tema.id} en $modo sigue siendo el morado de la línea base",
                    0xFF6650A4,
                    esquema.primary.argb(),
                )
                assertNotEquals(
                    "${tema.id} en $modo tiene una superficie morada de la línea base",
                    0xFFFFFBFE,
                    esquema.surface.argb(),
                )
            }
        }
    }

    @Test
    fun `ningun tema genera un color fuera de sRGB`() {
        AppThemes.all.flatMap { tema ->
            listOfNotNull(tema.light, tema.dark).flatMap { esquema ->
                esquema.roles().mapNotNull { (rol, color) ->
                    if (color.enGamutSrgb()) null else "${tema.id}.$rol"
                }
            }
        }.let { fuera ->
            assertTrue("roles fuera de sRGB: $fuera", fuera.isEmpty())
        }
    }

    @Test
    fun `el generador es el mismo para el mismo tema, se llame las veces que se llame`() {
        // El catálogo construye los esquemas una vez, al cargarse el objeto. Si alguien los volviera a calcular
        // dentro de `schemeFor`, cada recomposición pintaría un objeto nuevo y la app perdería el skip.
        assertSame("el catálogo debe guardar el esquema, no recalcularlo", AppThemes.default.light, AppThemes.default.light)
        assertSame(AppThemes.oneDark.dark, AppThemes.oneDark.dark)
    }

    @Test
    fun `los acentos de la paleta estandar son los del bloque de nota de siempre`() {
        // La paleta del bloque de nota no es una invención: son estos catorce colores. Es lo que mantiene el
        // aspecto de las notas de quien no cambia de tema (RF-168).
        val estandar = SyntaxThemes.Standard

        assertEquals(0xFF3B82F6.toInt(), estandar.keyword.argb())
        assertEquals(0xFF16A34A.toInt(), estandar.string.argb())
        assertEquals(0xFF8B93A3.toInt(), estandar.comment.argb())
        assertEquals(0xFF0891B2.toInt(), estandar.number.argb())
        assertEquals(0xFFA855F7.toInt(), estandar.function.argb())
        assertEquals(0xFFF59E0B.toInt(), estandar.variable.argb())
        assertEquals(0xFF0D9488.toInt(), estandar.operator.argb())
        assertEquals(0xFF3B82F6.toInt(), estandar.booleanNull.argb())
        assertEquals(0xFF10B981.toInt(), estandar.annotation.argb())
        assertEquals(0xFF9333EA.toInt(), estandar.type.argb())
        assertEquals(0xFF8B93A3.toInt(), estandar.punctuation.argb())
        assertEquals(0xFFF59E0B.toInt(), estandar.propertyKey.argb())
    }

    @Test
    fun `la paleta estandar se puede pintar entera, a diferencia de la del bloque`() {
        val estandar = SyntaxThemes.Standard

        listOf(
            "background" to estandar.background,
            "text" to estandar.text,
            "gutterBackground" to estandar.gutterBackground,
            "lineNumber" to estandar.lineNumber,
            "searchMatchBackground" to estandar.searchMatchBackground,
        ).forEach { (rol, color) ->
            assertNotEquals("la paleta estándar no puede tener $rol sin especificar", Color.Unspecified, color)
        }
    }
}