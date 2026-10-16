package com.example.shortcut

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Los destinos de los atajos del lanzador (spec 021, RF-125, RF-129).
 *
 * JUnit puro a propósito: el destino es **una palabra de un conjunto cerrado**, y por eso su parser se puede
 * probar entero aquí, sin `Robolectric`, que en esta plataforma no se ejecuta (RNF-73).
 *
 * Lo que de verdad importa es el otro lado del contrato: una acción que **no** es de un atajo tiene que dar
 * `null` y no reventar. A `MainActivity` llegan acciones de lo de siempre —`ACTION_MAIN` al tocar el icono,
 * `ACTION_VIEW` de los widgets y del importador de archivos— y cualquiera puede construir un `Intent` con la
 * que quiera, porque la `Activity` es `exported="true"` desde antes de esta spec (caso límite 12).
 */
class ShortcutDestinationTest {

    @Test
    fun `cada accion de atajo devuelve su destino`() {
        assertEquals(ShortcutDestination.NEW_NOTE, ShortcutDestination.desdeAccion(PREFIJO_ACCION + "NEW_NOTE"))
        assertEquals(ShortcutDestination.SEARCH, ShortcutDestination.desdeAccion(PREFIJO_ACCION + "SEARCH"))
        assertEquals(ShortcutDestination.AI_CHAT, ShortcutDestination.desdeAccion(PREFIJO_ACCION + "AI_CHAT"))
        assertEquals(ShortcutDestination.SETTINGS, ShortcutDestination.desdeAccion(PREFIJO_ACCION + "SETTINGS"))
        assertEquals(ShortcutDestination.CODE_EDITOR, ShortcutDestination.desdeAccion(PREFIJO_ACCION + "CODE_EDITOR"))
    }

    @Test
    fun `una clave devuelve su destino`() {
        ShortcutDestination.entries.forEach { destino ->
            assertEquals(destino, ShortcutDestination.desdeClave(destino.clave))
        }
    }

    /**
     * Las tres acciones que llegan de fuera tienen que salir por aquí como «esto no es un atajo», y ninguna
     * puede reventar. Es el caso que la app ejecuta **en cada arranque**, no solo con atajos.
     */
    @Test
    fun `las acciones de la app de siempre no son atajos`() {
        assertNull(ShortcutDestination.desdeAccion("android.intent.action.MAIN"))
        assertNull(ShortcutDestination.desdeAccion("android.intent.action.VIEW"))
        assertNull(ShortcutDestination.desdeAccion("android.intent.action.SEND"))
        assertNull(ShortcutDestination.desdeAccion(null))
        assertNull(ShortcutDestination.desdeAccion(""))
        assertNull(ShortcutDestination.desdeAccion("com.estrin217.securenotes.shortcut.NUEVO"))
    }

    @Test
    fun `las cinco claves son distintas entre si`() {
        val claves = ShortcutDestination.entries.map { it.clave }

        assertEquals(claves.size, claves.distinct().size)
    }

    /**
     * La acción se construye a partir del prefijo y del **nombre del enum**, no a mano. Es lo que impide que
     * el XML y el enum se separen: si mañana alguien renombra `SEARCH`, la acción cambia con él y el
     * invariante que los cruza lo pilla en el acto.
     */
    @Test
    fun `cada accion se deriva del nombre del destino`() {
        ShortcutDestination.entries.forEach { destino ->
            assertEquals(PREFIJO_ACCION + destino.name, destino.accion)
        }
        assertEquals("com.estrin217.securenotes.shortcut.", PREFIJO_ACCION)
    }
}