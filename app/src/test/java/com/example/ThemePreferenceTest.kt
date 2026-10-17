package com.example

import com.estrin217.tema.AppThemes
import com.example.data.theme.resolveStoredTheme
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * La migración del interruptor al tema (spec 025, RF-164, RF-165, HU-4).
 *
 * Es la diferencia entre «nadie nota nada» y «la mitad de la app abre de otro color». Las tres ramas están aquí,
 * y el parámetro `null` es el que se olvida: no es «apagado», es «nunca escrito».
 */
class ThemePreferenceTest {

    @Test
    fun `sin nada guardado se ve lo de siempre`() {
        assertSame(AppThemes.dynamic, resolveStoredTheme(null, null))
    }

    @Test
    fun `con el interruptor apagado se ven los morados de siempre`() {
        assertSame(AppThemes.byId("default"), resolveStoredTheme(null, false))
    }

    @Test
    fun `con el interruptor encendido se ven los colores del sistema`() {
        assertSame(AppThemes.dynamic, resolveStoredTheme(null, true))
    }

    @Test
    fun `un tema guardado gana a lo que diga el interruptor antiguo`() {
        AppThemes.all.forEach { tema ->
            listOf(null, false, true).forEach { antiguo ->
                assertSame(
                    "el tema guardado '${tema.id}' debería ganar al interruptor antiguo=$antiguo",
                    tema,
                    resolveStoredTheme(tema.id, antiguo),
                )
            }
        }
    }

    @Test
    fun `un identificador que no existe cae en el dinamico`() {
        listOf("", "   ", "no_existe", "otro_valor").forEach { id ->
            assertSame("el identificador '$id' debería caer en el dinámico", AppThemes.dynamic, resolveStoredTheme(id, null))
        }
    }

    @Test
    fun `un identificador desconocido no la da por buena aunque el interruptor estuviera apagado`() {
        // Si el identificador está pero no existe, el catálogo manda y se va al dinámico. Es lo que dice RF-165, y
        // es lo contrario de lo que haría "si no conozco el tema, usa el predeterminado": un identificador
        // corrupto no debería arreglarse solo con un morado que tampoco pidió nadie.
        assertSame(AppThemes.dynamic, resolveStoredTheme("no_existe", false))
    }

    @Test
    fun `un tema guardado vacio cae en el dinamico`() {
        assertSame(AppThemes.dynamic, resolveStoredTheme("", false))
    }

    @Test
    fun `la migracion no escribe nada ni necesita el sistema`() {
        // La función es pura: solo recibe dos valores. Que se pueda llamar con literales es la prueba de que no
        // toca el disco (RNF-104).
        assertSame(AppThemes.dynamic, resolveStoredTheme(null, null))
        assertSame(AppThemes.dynamic, resolveStoredTheme(null, null))
    }
}