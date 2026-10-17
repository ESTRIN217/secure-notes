package com.example

import com.estrin217.tema.AppThemes
import com.example.ui.theme.labelRes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * El nombre de cada tema está en los nueve idiomas (spec 025, RNF-102, D9).
 *
 * Una clave que falta en un idioma no es un error de compilación: cae al inglés y ya. Así que esta prueba lee los
 * nueve ficheros, que es la única forma de que se note.
 */
class ThemeLabelsTest {

    private companion object {
        const val RES = "src/main/res"

        val LOCALES = listOf(
            "values",
            "values-es-rVE",
            "values-es-rES",
            "values-b+es+419",
            "values-pt-rBR",
            "values-pt-rPT",
            "values-fr",
            "values-it",
            "values-en-rGB",
        )

        val CLAVES = listOf(
            "settings_theme",
            "theme_picker_title",
            "theme_dynamic",
            "theme_default",
            "theme_vscode_dark",
            "theme_one_dark",
            "theme_monokai",
            "theme_github_light",
            "theme_active_cd",
        )
    }

    @Test
    fun `cada tema tiene un nombre propio`() {
        val nombres = AppThemes.all.map { it.id to labelRes(it) }

        assertEquals("cada tema del catálogo tiene su clave", AppThemes.all.size, nombres.map { it.second }.toSet().size)
    }

    @Test
    fun `un tema que no esta en el catalogo no tiene nombre`() {
        // Un `error` y no un nombre por defecto: si algún día el catálogo crece y aquí no, que se note al
        // arrancar y no con un tema sin etiqueta en el selector.
        val inventado = AppThemes.vscodeDark.copy(id = "tema_inventado")
        val fallo = org.junit.Assert.assertThrows(IllegalStateException::class.java) { labelRes(inventado) }

        assertTrue("el fallo tiene que decir qué tema es: ${fallo.message}", fallo.message!!.contains("tema_inventado"))
    }

    @Test
    fun `las nueve claves estan en los nueve idiomas`() {
        LOCALES.forEach { local ->
            val xml = File("$RES/$local/strings.xml").readText()
            CLAVES.forEach { clave ->
                assertTrue("falta '$clave' en $local", xml.contains("name=\"$clave\""))
            }
        }
    }

    @Test
    fun `las claves muertas del interruptor ya no estan`() {
        // Se quitaron junto con el interruptor (RF-170). Una clave sin uso en los nueve idiomas es ruido que
        // alguien va a traducir un día.
        LOCALES.forEach { local ->
            val xml = File("$RES/$local/strings.xml").readText()
            listOf("settings_dynamic_colors", "settings_dynamic_colors_desc").forEach { clave ->
                assertTrue("'$clave' sigue en $local", !xml.contains("name=\"$clave\""))
            }
        }
    }

    @Test
    fun `los apostrofes estan escapados`() {
        // `mergeDebugResources` es donde AAPT ve un apóstrofo sin escapar, que es **después** de que Kotlin
        // compile en verde. Aquí se ve antes.
        LOCALES.forEach { local ->
            val xml = File("$RES/$local/strings.xml").readText()
            val sinEscapar = Regex("<string name=\"[^\"]+\">[^<]*[^\\\\]'[^<]*</string>").findAll(xml)
            assertTrue("$local tiene apóstrofos sin escapar: ${sinEscapar.map { it.value }.toList()}", sinEscapar.none())
        }
    }
}