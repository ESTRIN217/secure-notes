package com.example

import androidx.compose.ui.graphics.Color
import com.example.util.CodeSyntax
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Invariantes de la spec 022: el bloque de código **no** tiene resaltador ni catálogo propios; los reutiliza
 * del módulo del editor.
 *
 * Son pruebas **puras** que leen el texto de los ficheros, con las dos reglas del proyecto (ver `MEMORY.md`):
 * cada recorrido **quita comentarios antes de buscar** y **se autocomprueba**, para que un KDoc que explique
 * por qué algo **no** está no dé verde, ni un recorrido vacío pase por vacío.
 *
 * La otra mitad de la spec (que el bloque se vea legible en claro y oscuro) **no** se puede comprobar aquí:
 * va a dispositivo (RF-134).
 */
class CodeSyntaxInvariantsTest {

    private companion object {
        const val APP = "src/main"
        const val JAVA = "$APP/java/com/example"
        const val RES = "$APP/res"

        const val SYNTAX = "$JAVA/util/CodeSyntax.kt"
        const val SHEET = "$JAVA/ui/CodeLanguageSheet.kt"
        const val READ_ONLY = "$JAVA/ui/ReadOnlyTextBlock.kt"
        const val EDITABLE = "$JAVA/ui/EditableTextBlock.kt"

        /** El módulo del editor, tal como se ve desde el directorio de `:app`. */
        const val EDITOR = "../editor-de-codigo/src/main/java"

        /** Lo que la app tenía duplicado y ya no puede volver. */
        val DUPLICADOS_QUE_SE_FUERON = listOf(
            "$JAVA/util/CodeHighlighter.kt",
            "$JAVA/ui/CodeLanguages.kt"
        )

        /** Piso de cualquier recorrido: si baja de aquí, el recorrido está roto, no el código. */
        const val MINIMO_DE_FICHEROS = 40

        private val COMENTARIOS = Regex("/\\*.*?\\*/|//[^\\n]*", RegexOption.DOT_MATCHES_ALL)

        fun leer(ruta: String): String = File(ruta).readText()

        /** Lee el fichero **quitando comentarios**, para que un KDoc no pueda dar verde un invariante. */
        fun leerCodigo(ruta: String): String = leer(ruta).replace(COMENTARIOS, "")

        /** Todos los `.kt` de `:app` bajo `src/main`, comprobando que el recorrido mira algo de verdad. */
        fun fuentesDeLaApp(): List<File> {
            val ficheros = File(JAVA).walkTopDown().filter { it.extension == "kt" }.toList()
            assertTrue("el recorrido de `:app` está roto: solo ve ${ficheros.size} ficheros", ficheros.size >= MINIMO_DE_FICHEROS)
            return ficheros
        }
    }

    private fun leerCodigoComprobando(ruta: String, etiqueta: String): String {
        val texto = leer(ruta)
        assertTrue("no se está leyendo $etiqueta ($ruta): el recorrido está roto", texto.lines().size >= 5)
        val codigo = leerCodigo(ruta)
        assertTrue("el recorrido de $etiqueta ($ruta) se ha quedado sin código", codigo.isNotBlank())
        return codigo
    }

    // ── RNF-77: no quedan copias muertas ────────────────────────────────────────────────────

    @Test
    fun `los ficheros duplicados ya no existen`() {
        DUPLICADOS_QUE_SE_FUERON.forEach { ruta ->
            assertFalse("seguía existiendo el duplicado $ruta", File(ruta).exists())
        }
    }

    @Test
    fun `ninguna fuente de la app menciona el resaltador ni el catalogo retirados`() {
        val supervivientes = fuentesDeLaApp()
            .filter { fichero ->
                val codigo = leerCodigo(fichero.path)
                codigo.contains("CodeHighlighter") || codigo.contains("CodeLanguages")
            }
            .map { it.name }

        assertTrue("quedan referencias al resaltador o al catálogo retirados: $supervivientes", supervivientes.isEmpty())
    }

    // ── RF-131 / RF-132: una sola implementación y un solo catálogo ─────────────────────────

    @Test
    fun `solo CodeSyntax conoce el catalogo del editor`() {
        val conCatalogo = fuentesDeLaApp()
            .filter { leerCodigo(it.path).contains("SupportedLanguage") }
            .map { it.name }

        assertEquals("el catálogo del editor debe conocerse SOLO en CodeSyntax", listOf("CodeSyntax.kt"), conCatalogo)
    }

    @Test
    fun `ninguna fuente de la app define su propia tabla de palabras clave`() {
        val conTabla = fuentesDeLaApp()
            .filter { fichero ->
                val codigo = leerCodigo(fichero.path)
                codigo.contains("keywords = setOf(") || codigo.contains("LangConfig")
            }
            .map { it.name }

        assertTrue("hay una tabla de resaltado propia en: $conTabla", conTabla.isEmpty())
    }

    @Test
    fun `el selector se alimenta del catalogo compartido`() {
        val codigo = leerCodigoComprobando(SHEET, "la hoja de idiomas")

        assertTrue("la hoja no usa CodeSyntax.options()", codigo.contains("CodeSyntax.options()"))
        assertFalse("la hoja enumera idiomas por su cuenta", codigo.contains("SupportedLanguage"))
    }

    @Test
    fun `los dos bloques resaltan con la implementacion compartida`() {
        listOf(READ_ONLY to "el bloque de lectura", EDITABLE to "el bloque de edición").forEach { (ruta, etiqueta) ->
            val codigo = leerCodigoComprobando(ruta, etiqueta)
            assertTrue("$etiqueta no llama al resaltador compartido", codigo.contains("CodeSyntax.highlight("))
        }
    }

    // ── RNF-78: el resaltado va dentro de un remember ───────────────────────────────────────

    @Test
    fun `el resaltado se memoriza por texto e idioma`() {
        val memorizado = Regex("""remember\(\s*annotated\s*,\s*highlightLanguage\s*\)\s*\{[\s\S]{0,400}?CodeSyntax\.highlight\(""")

        listOf(READ_ONLY, EDITABLE).forEach { ruta ->
            val codigo = leerCodigoComprobando(ruta, ruta)
            assertTrue("${ruta} resalta sin memorizar: se recalcularía en cada fotograma", memorizado.containsMatchIn(codigo))
        }
    }

    // ── RNF-76: la dependencia va en un solo sentido ────────────────────────────────────────

    @Test
    fun `el modulo del editor no conoce a la app`() {
        val ficheros = File(EDITOR).walkTopDown().filter { it.extension == "kt" }.toList()
        assertTrue("el recorrido del editor está roto: solo ve ${ficheros.size} ficheros", ficheros.size >= 30)

        val infractores = ficheros
            .filter { leerCodigo(it.path).contains("com.example.") }
            .map { it.name }
        assertTrue("el editor menciona a la app: ${infractores}", infractores.isEmpty())
    }

    // ── RF-134: la paleta del bloque se conserva y no pinta fondo propio ────────────────────

    @Test
    fun `la paleta del bloque son los colores de siempre y sin fondo propio`() {
        val tema = CodeSyntax.blockTheme

        assertEquals("el color base lo pone el tema de la app, no el editor", Color.Unspecified, tema.text)
        assertEquals("el bloque no pinta fondo propio", Color.Unspecified, tema.background)
        assertEquals("el bloque no pinta canal propio", Color.Unspecified, tema.gutterBackground)

        assertEquals("keyword", Color(0xFF3B82F6), tema.keyword)
        assertEquals("string", Color(0xFF16A34A), tema.string)
        assertEquals("comment", Color(0xFF8B93A3), tema.comment)
        assertEquals("number", Color(0xFF0891B2), tema.number)
        assertEquals("function", Color(0xFFA855F7), tema.function)
        assertEquals("type", Color(0xFF9333EA), tema.type)
        assertEquals("annotation", Color(0xFF10B981), tema.annotation)
    }

    // ── RNF-80: cero cadenas nuevas ─────────────────────────────────────────────────────────

    @Test
    fun `las etiquetas traducidas siguen existiendo en values`() {
        val strings = leer("$RES/values/strings.xml")
        assertTrue("falta code_lang_plain", strings.contains("name=\"code_lang_plain\""))
        assertTrue("falta code_lang_shell", strings.contains("name=\"code_lang_shell\""))
    }
}
