package com.example.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.FileNotFoundException

/**
 * Cubre solo `failureMessage`: es la función pura que compone el mensaje de reemplazo y la
 * única a la que la Decisión 22 de la spec 001 abre la excepción de RF-19.
 *
 * Sin `@RunWith(RobolectricTestRunner::class)`, sin `@Config` y sin `ApplicationProvider` a
 * propósito: la función no toca Android y las suites Robolectric no tienen runtime nativo para
 * `linux/aarch64`.
 */
class OpaqueTraceTest {

    private val idOpaque = "op7/i2"

    @Test
    fun `message contains kind and opaque id with and without exception`() {
        val conExcepcion = OpaqueTrace.failureMessage(
            "copy media to export", "IMAGE", idOpaque, FileNotFoundException::class.java
        )
        val sinExcepcion = OpaqueTrace.failureMessage("openInputStream", "image", idOpaque, null)

        listOf(conExcepcion, sinExcepcion).forEach { mensaje ->
            assertTrue(mensaje, mensaje.contains("kind="))
            assertTrue(mensaje, mensaje.contains(idOpaque))
        }
        assertTrue(conExcepcion.contains("IMAGE"))
        assertTrue(sinExcepcion.contains("image"))
    }

    @Test
    fun `message does not contain the exception text`() {
        val rutaSecreta = "/storage/emulated/0/DCIM/private-note.jpg"
        val falla = IllegalStateException("no se pudo leer $rutaSecreta")
        val texto = requireNotNull(falla.message)

        val mensaje = OpaqueTrace.failureMessage(
            "copy media to export", "IMAGE", idOpaque, falla.javaClass
        )

        assertFalse(mensaje, mensaje.contains(rutaSecreta))
        assertFalse(mensaje, mensaje.contains(texto))
        // Lo único que se toma de la excepción es su clase, nunca su texto.
        assertTrue(mensaje, mensaje.contains("exc=IllegalStateException"))
    }
}