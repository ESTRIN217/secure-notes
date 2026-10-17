package com.example

import com.example.ui.theme.isDark
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El ajuste de modo y el tema son dos cosas distintas (spec 025, RF-171, D8).
 *
 * Antes de la spec esta decisión estaba copiada en tres Activities y en el servicio de la burbuja flotante, cada una
 * con su propio `when`. Aquí está una vez, y se puede probar sin `Context` porque **recibe** el estado del sistema
 * en vez de preguntarlo.
 */
class DarkModeTest {

    @Test
    fun `encendido es oscuro y apagado es claro, diga lo que diga el sistema`() {
        listOf(false, true).forEach { sistema ->
            assertTrue("con el modo encendido tiene que ser oscuro (sistema=$sistema)", DarkModeOption.ON.isDark(sistema))
            assertFalse("con el modo apagado tiene que ser claro (sistema=$sistema)", DarkModeOption.OFF.isDark(sistema))
        }
    }

    @Test
    fun `sistema sigue al sistema`() {
        assertTrue("con el sistema en oscuro", DarkModeOption.SYSTEM.isDark(systemInDarkTheme = true))
        assertFalse("con el sistema en claro", DarkModeOption.SYSTEM.isDark(systemInDarkTheme = false))
    }

    @Test
    fun `los tres ajustes se distinguen entre si`() {
        val enClaro = DarkModeOption.entries.map { it.isDark(systemInDarkTheme = false) }
        val enOscuro = DarkModeOption.entries.map { it.isDark(systemInDarkTheme = true) }

        assertTrue("con el sistema en claro, apagado y sistema tienen que coincidir: $enClaro", enClaro.count { !it } == 2)
        assertTrue("con el sistema en oscuro, encendido y sistema tienen que coincidir: $enOscuro", enOscuro.count { it } == 2)
    }
}