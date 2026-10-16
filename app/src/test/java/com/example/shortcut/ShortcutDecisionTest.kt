package com.example.shortcut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Qué se hace con un destino que llega del lanzador (spec 021, RF-127, RF-129).
 *
 * Es la pieza que decide entre **ejecutar ahora** y **esperar al candado**, y es pura a propósito (RNF-73): la
 * bóveda entra como un `Boolean` y no como un estado de Android, así que la decisión entera se prueba aquí sin
 * `Robolectric`, que en esta plataforma no se ejecuta.
 *
 * Lo que se vigila sobre todo es la **confusión**: **`null` no puede significar «no hay destino» y «queda
 * pendiente» a la vez**, porque confundirlas deja un atajo esperando para siempre sin que nada avise. Por eso
 * el resultado son tres casos y no un `ShortcutDestination?`.
 */
class ShortcutDecisionTest {

    @Test
    fun `con la boveda abierta se ejecuta en el acto`() {
        val pendiente = ShortcutPending()

        val decision = decidir(ShortcutDestination.SEARCH.accion, abierta = true, pendiente = pendiente)

        assertEquals(ShortcutDecision.Ejecutar(ShortcutDestination.SEARCH), decision)
        assertNull("ejecutar no debe dejar nada pendiente", pendiente.destino)
    }

    @Test
    fun `con la boveda cerrada el destino queda pendiente y no se ejecuta`() {
        val pendiente = ShortcutPending()

        val decision = decidir(ShortcutDestination.NEW_NOTE.accion, abierta = false, pendiente = pendiente)

        assertEquals(ShortcutDecision.Pendiente(ShortcutDestination.NEW_NOTE), decision)
        assertEquals(ShortcutDestination.NEW_NOTE, pendiente.destino)
    }

    @Test
    fun `una clave desconocida se ignora y no deja nada pendiente`() {
        val pendiente = ShortcutPending()

        assertEquals(ShortcutDecision.Ignorar, decidir("com.estrin217.securenotes.shortcut.NUEVO", abierta = true, pendiente = pendiente))
        assertNull(ShortcutPending().destino)

        assertEquals(ShortcutDecision.Ignorar, decidir("com.estrin217.securenotes.shortcut.NUEVO", abierta = false, pendiente = pendiente))
        assertNull("un atajo desconocido no debe esperar al candado", pendiente.destino)
    }

    @Test
    fun `sin clave se ignora`() {
        assertEquals(ShortcutDecision.Ignorar, decidir(null, abierta = true, pendiente = ShortcutPending()))
        assertEquals(ShortcutDecision.Ignorar, decidir("", abierta = false, pendiente = ShortcutPending()))
    }

    /**
     * Los cinco destinos tienen que pasar por los dos caminos, no solo el primero: un brazo que se olvidara de
     * mirar `abierta` se vería verde con las pruebas de arriba.
     */
    @Test
    fun `los cinco destinos se ejecutan con la boveda abierta y esperan con la cerrada`() {
        ShortcutDestination.entries.forEach { destino ->
            val alAbrir = ShortcutPending()
            assertEquals(
                ShortcutDecision.Ejecutar(destino),
                decidir(destino.accion, abierta = true, pendiente = alAbrir)
            )

            val alCerrar = ShortcutPending()
            assertEquals(
                ShortcutDecision.Pendiente(destino),
                decidir(destino.accion, abierta = false, pendiente = alCerrar)
            )
            assertEquals(destino, alCerrar.destino)
        }
    }

    /**
     * El atajo del editor de código **no** es la excepción: la bóveda cerrada también lo retiene (RF-127).
     * La `Activity` del editor no tiene candado propio, y por eso este caso se escribe aparte.
     */
    @Test
    fun `el editor de codigo tambien espera al candado`() {
        val pendiente = ShortcutPending()

        assertEquals(
            ShortcutDecision.Pendiente(ShortcutDestination.CODE_EDITOR),
            decidir(ShortcutDestination.CODE_EDITOR.accion, abierta = false, pendiente = pendiente)
        )
    }
}