package com.example

import com.example.crash.CrashIssueUrl
import com.example.crash.CrashReportText
import com.example.crash.CrashStore
import com.example.crash.DatosInforme
import com.example.crash.TrazaRecortada

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Pruebas del almacén del informe (spec 020, RF-117, RF-122, RF-123 y casos límite 1 y 2).
 *
 * Corren en un `TemporaryFolder` de verdad y **sin `RobolectricTestRunner`**: por eso `CrashStore` recibe la
 * `File` ya resuelta en lugar de un `Context` (decisión 2 del plan). Escribir, renombrar y borrar son las tres
 * cosas que hay que probar, y aquí se prueban de verdad.
 */
class CrashStoreTest {

    @get:Rule
    val carpeta = TemporaryFolder()

    private fun store(): CrashStore {
        val destino = File(carpeta.root, "crash/last_crash.txt")
        return CrashStore(destino)
    }

    @Test
    fun `guardar y leer devuelve el texto escrito`() {
        val store = store()

        store.guardar("informe del fallo")

        assertEquals("informe del fallo", store.leer())
    }

    @Test
    fun `sin fichero no hay nada pendiente`() {
        val store = store()

        assertFalse(store.hayPendiente())
        assertNull(store.leer())
    }

    @Test
    fun `guardar crea el directorio que falta`() {
        val store = store()

        store.guardar("informe")

        assertTrue(File(carpeta.root, "crash/last_crash.txt").isFile)
    }

    @Test
    fun `un segundo fallo SOBREESCRIBE el primero y solo queda el ultimo`() {
        val store = store()

        store.guardar("primer fallo")
        store.guardar("segundo fallo")

        assertEquals("segundo fallo", store.leer())
    }

    @Test
    fun `guardar dos veces no deja el temporal tirado en el directorio`() {
        val store = store()

        store.guardar("uno")
        store.guardar("dos")

        assertEquals(listOf("last_crash.txt"), File(carpeta.root, "crash").list()!!.toList())
    }

    @Test
    fun `un informe vacio no cuenta como pendiente`() {
        val store = store()

        store.guardar("")

        assertFalse(store.hayPendiente())
        assertNull(store.leer())
    }

    @Test
    fun `consumir borra el informe y dice que habia`() {
        val store = store()
        store.guardar("informe")

        assertTrue(store.consumir())

        assertFalse(File(carpeta.root, "crash/last_crash.txt").exists())
        assertFalse(store.hayPendiente())
    }

    @Test
    fun `consumir sin informe no dice que habia nada`() {
        assertFalse(store().consumir())
    }

    @Test
    fun `leer tras consumir es null`() {
        val store = store()
        store.guardar("informe")
        store.consumir()

        assertNull(store.leer())
    }

    @Test
    fun `un informe con acentos y simbolos vuelve IGUAL`() {
        val store = store()
        val texto = "java.lang.IllegalStateException: ¿qué? — a/b\\c «d» 100% & más\n\tat Foo.bar(Foo.kt:1)"

        store.guardar(texto)

        assertEquals(texto, store.leer())
    }
}