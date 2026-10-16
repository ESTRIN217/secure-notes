package com.example.shortcut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * El destino pendiente de un atajo (spec 021, RF-127, caso límite 4).
 *
 * Es el sitio donde vive la espera: el atajo se pulsó con la bóveda cerrada y no se puede cumplir hasta que el
 * usuario desbloquea. Vive en el `companion` de `MainActivity` porque quien lo escribe —el `IntentRelay`— y
 * quien lo lee —un `LaunchedEffect` que reacciona al desbloqueo— son sitios distintos de la misma clase.
 *
 * **Una clase y no un `object`**: con estado global, una prueba deja un destino pendiente y el fallo sale en la
 * prueba siguiente, no en la que lo introdujo. Aquí cada prueba arranca con una instancia limpia.
 */
class ShortcutPendingTest {

    @Test
    fun `sin nada registrado no hay nada que drenar`() {
        assertNull(ShortcutPending().drena())
    }

    @Test
    fun `drenar devuelve el destino y lo vacia`() {
        val pendiente = ShortcutPending()
        pendiente.registra(ShortcutDestination.SEARCH)

        assertEquals(ShortcutDestination.SEARCH, pendiente.destino)
        assertEquals(ShortcutDestination.SEARCH, pendiente.drena())
        assertNull("drenar tiene que vaciar, o el atajo se cumpliria dos veces", pendiente.destino)
    }

    @Test
    fun `drenar dos veces no repite el destino`() {
        val pendiente = ShortcutPending()
        pendiente.registra(ShortcutDestination.SETTINGS)

        assertEquals(ShortcutDestination.SETTINGS, pendiente.drena())
        assertNull(pendiente.drena())
    }

    @Test
    fun `si llegan dos atajos gana el ultimo`() {
        val pendiente = ShortcutPending()
        pendiente.registra(ShortcutDestination.NEW_NOTE)
        pendiente.registra(ShortcutDestination.AI_CHAT)

        assertEquals(
            "cumplir los dos seria saltar de pantalla sin que nadie lo pidiera",
            ShortcutDestination.AI_CHAT,
            pendiente.drena()
        )
    }
}