package com.estrin217.editordecodigo

import com.estrin217.editordecodigo.data.model.CodeFile
import com.estrin217.editordecodigo.syntax.SupportedLanguage
import com.estrin217.editordecodigo.utils.CodeFilePathIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Qué rutas locales protege el editor de código (spec 024, RF-152/RF-153, RNF-92).
 *
 * Es una prueba **pura**: `CodeFilePathIndex` no toca Android, así que corre en esta plataforma
 * aunque Robolectric no tenga runtime nativo aquí. Protege el archivo del editor de que el
 * administrador de almacenamiento lo tome por huérfano.
 */
class CodeFilePathIndexTest {

    private fun fila(name: String, filePath: String? = null, uriString: String? = null) = CodeFile(
        name = name,
        language = SupportedLanguage.MARKDOWN,
        filePath = filePath,
        uriString = uriString
    )

    @Test
    fun `una lista vacia no protege nada`() {
        assertEquals(emptySet<String>(), CodeFilePathIndex.localPaths(emptyList()))
    }

    @Test
    fun `una ruta absoluta queda protegida`() {
        val ruta = "/data/user/0/com.estrin217.securenotes/files/code_files/notas.md"
        assertEquals(setOf(ruta), CodeFilePathIndex.localPaths(listOf(fila("notas.md", ruta))))
    }

    @Test
    fun `el prefijo file se normaliza a la ruta`() {
        val rutas = CodeFilePathIndex.localPaths(
            listOf(fila("a.kt", "file:///data/user/0/app/files/code_files/a.kt"))
        )
        assertEquals(setOf("/data/user/0/app/files/code_files/a.kt"), rutas)
    }

    @Test
    fun `una uri de contenido no protege ninguna ruta local`() {
        val rutas = CodeFilePathIndex.localPaths(
            listOf(fila("b.kt", uriString = "content://com.android.providers/downloads/document/42"))
        )
        assertTrue(rutas.isEmpty())
    }

    @Test
    fun `una fila cuya ruta es de contenido no protege nada`() {
        val rutas = CodeFilePathIndex.localPaths(
            listOf(fila("b2.kt", filePath = "content://com.android.providers/downloads/document/43"))
        )
        assertTrue(rutas.isEmpty())
    }

    @Test
    fun `una fila sin ruta no protege nada`() {
        assertTrue(CodeFilePathIndex.localPaths(listOf(fila("c.txt"))).isEmpty())
    }

    @Test
    fun `una ruta en blanco se ignora`() {
        assertTrue(CodeFilePathIndex.localPaths(listOf(fila("d.txt", "   "))).isEmpty())
    }

    @Test
    fun `dos filas con la misma ruta cuentan una vez`() {
        val ruta = "/data/user/0/app/files/code_files/e.md"
        assertEquals(
            setOf(ruta),
            CodeFilePathIndex.localPaths(listOf(fila("e.md", ruta), fila("e.md", ruta)))
        )
    }

    @Test
    fun `una ruta con espacios internos se conserva tal cual`() {
        val ruta = "/data/user/0/app/files/code_files/mi archivo.kt"
        assertEquals(setOf(ruta), CodeFilePathIndex.localPaths(listOf(fila("mi archivo.kt", ruta))))
    }
}
