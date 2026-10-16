package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Invariantes de la spec 024: el administrador de almacenamiento respeta los archivos del editor y el
 * editor conserva el permiso del almacenamiento compartido.
 *
 * Son pruebas **puras** que leen el texto de los ficheros, con las dos reglas del proyecto (ver `MEMORY.md`):
 * cada recorrido **quita comentarios antes de buscar** y **se autocomprueba**, para que un KDoc que explique
 * por qué algo **no** está no dé verde, ni un recorrido vacío pase por vacío.
 *
 * Lo que se comprueba aquí es el **cableado**, que es justo lo que las pruebas puras de las decisiones no ven.
 * Los dos comportamientos que dependen del sistema —que el permiso sobreviva al reinicio y que la base del
 * editor pueda no existir— van al guion de dispositivo (`plan.md`).
 */
class StorageEditorFilesInvariantsTest {

    private companion object {
        const val APP_JAVA = "src/main/java/com/example"
        const val EDITOR_JAVA = "../editor-de-codigo/src/main/java/com/estrin217/editordecodigo"

        const val ANALIZADOR = "$APP_JAVA/data/storage/StorageAnalyzer.kt"
        const val INDICE_DE_RUTAS = "$EDITOR_JAVA/utils/CodeFilePathIndex.kt"
        const val PERMISO_PURO = "$EDITOR_JAVA/utils/PersistablePermission.kt"
        const val LECTOR_EXTERNO = "$EDITOR_JAVA/utils/FileImportExportHelper.kt"
        const val FACHADA_EDITOR = "$EDITOR_JAVA/CodeEditor.kt"

        /** Piso de cualquier recorrido: si baja de aquí, el recorrido está roto, no el código. */
        const val MINIMO_DE_LINEAS = 12

        private val COMENTARIOS_KOTLIN = Regex("/\\*.*?\\*/|//[^\\n]*", RegexOption.DOT_MATCHES_ALL)
    }

    /** Lee **quitando comentarios**, para que un KDoc que explica por qué algo **no** está no dé verde. */
    private fun leerCodigo(ruta: String, etiqueta: String): String {
        val fichero = File(ruta)
        assertTrue("no existe $etiqueta ($ruta)", fichero.exists())
        val texto = fichero.readText()
        assertTrue("$etiqueta ($ruta) solo tiene ${texto.lines().size} líneas", texto.lines().size >= MINIMO_DE_LINEAS)
        return texto.replace(COMENTARIOS_KOTLIN, "")
    }

    private fun entre(texto: String, inicio: String, fin: String): String {
        val desde = texto.indexOf(inicio)
        assertTrue("no está «$inicio» en el recorrido", desde >= 0)
        val hasta = texto.indexOf(fin, desde)
        assertTrue("no está «$fin» después de «$inicio»", hasta > desde)
        return texto.substring(desde, hasta)
    }

    @Test
    fun `el administrador suma los recientes del editor a lo referenciado`() {
        val codigo = leerCodigo(ANALIZADOR, "el analizador")

        assertTrue(
            "el escaneo no consulta al editor",
            codigo.contains("CodeEditor.recentLocalFilePaths(context)")
        )
        assertTrue(
            "las rutas del editor no se suman a las referenciadas",
            codigo.contains("collectReferencedPaths(database) + collectEditorPaths(context)")
        )
    }

    @Test
    fun `un fallo leyendo los recientes no tumba el escaneo`() {
        val codigo = leerCodigo(ANALIZADOR, "el analizador")
        val cuerpo = entre(codigo, "fun collectEditorPaths", "private fun extractPathsFromContent")

        assertTrue("el fallo no se registra", cuerpo.contains("registrarFallo("))
        assertTrue("el fallo no se cierra a cero rutas", cuerpo.contains("emptySet()"))
    }

    @Test
    fun `las dos piezas de decision son puras`() {
        for (ruta in listOf(INDICE_DE_RUTAS, PERMISO_PURO)) {
            val codigo = leerCodigo(ruta, ruta)
            assertFalse("$ruta importa android", codigo.contains("import android"))
            assertFalse("$ruta importa androidx", codigo.contains("import androidx"))
        }
    }

    @Test
    fun `el indice de rutas solo protege las locales`() {
        val codigo = leerCodigo(INDICE_DE_RUTAS, "el índice de rutas")

        assertTrue("no se descartan las rutas sin filePath", codigo.contains("filePath?.takeIf { it.isNotBlank() }"))
        assertTrue("no se normaliza el esquema file", codigo.contains("removePrefix(FILE_SCHEME)"))
        assertTrue("no se descarta el almacenamiento compartido", codigo.contains("CONTENT_SCHEME"))
    }

    @Test
    fun `la fachada no crea la base por preguntar`() {
        val codigo = leerCodigo(FACHADA_EDITOR, "la fachada")
        val cuerpo = entre(codigo, "fun recentLocalFilePaths", "\n}")

        assertTrue("la fachada no comprueba si la base existe", cuerpo.contains("databaseExists(appContext)"))
        val guarda = cuerpo.indexOf("databaseExists(appContext)")
        val obtiene = cuerpo.indexOf("getInstance(appContext)")
        assertTrue("se obtiene la base antes de comprobar que existe", obtiene > guarda)
    }

    @Test
    fun `los dos lectores piden el permiso persistente`() {
        val codigo = leerCodigo(LECTOR_EXTERNO, "el lector externo")

        assertTrue(
            "no se pide el permiso persistente",
            codigo.contains("takePersistableUriPermission(uri, flags)")
        )
        assertTrue(
            "el permiso no se pregunta a la pieza pura",
            codigo.contains("PersistablePermission.appliesTo(uri.scheme)")
        )
        assertTrue(
            "abrir un archivo no pide el permiso",
            codigo.contains("persistSharedStoragePermission(context, uri)")
        )
        assertTrue(
            "abrir una carpeta no pide el permiso",
            codigo.contains("persistSharedStoragePermission(context, treeUri)")
        )
    }

    @Test
    fun `el permiso no concedido se registra y no se traga`() {
        val codigo = leerCodigo(LECTOR_EXTERNO, "el lector externo")
        val cuerpo = entre(codigo, "fun persistSharedStoragePermission", "private fun tryPersist")

        assertTrue("el fallo no se registra", cuerpo.contains("registrarFallo("))
        assertTrue("no hay caída a solo lectura", cuerpo.contains("FLAG_GRANT_READ_URI_PERMISSION"))
        // El registro no puede llevar el contenido ni la ruta: solo la clase de la excepción.
        assertFalse("el registro lleva el mensaje de la excepción", cuerpo.contains("e.message"))
        assertEquals(
            1,
            Regex("registrarFallo\\(").findAll(cuerpo).count()
        )
    }
}
