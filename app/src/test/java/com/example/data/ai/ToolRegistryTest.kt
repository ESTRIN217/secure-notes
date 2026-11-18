package com.example.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * La decisión del anuncio de herramientas (spec 042, RF-306, RF-307): [ToolRegistry.getSpecsForApi]
 * recibe el conjunto de deshabilitadas y excluye por **nombre**, manteniendo el orden de registro.
 *
 * - Sin conjunto → comportamiento de hoy, intacto: manda todas.
 * - Una deshabilitada → se excluye y las demás conservan su orden.
 * - Todas deshabilitadas → lista vacía; la petición sale sin herramientas.
 * - Un nombre del conjunto que ya no está registrado (catálogo cambiado, copia restaurada de otra
 *   versión) es inofensivo: no filtra nada ni falla (RF-307).
 *
 * JUnit4 puro, sin Robolectric: el fichero del registro no toca `android`.
 */
class ToolRegistryTest {

    private fun registro(vararg nombres: String): ToolRegistry {
        val registry = ToolRegistry()
        nombres.forEach { nombre ->
            registry.register(
                ToolSpec(name = nombre, description = "desc $nombre", parameters = emptyList()),
                { "ok" }
            )
        }
        return registry
    }

    @Test
    fun `sin conjunto manda todas las registradas`() {
        val specs = registro("search_notes", "get_note").getSpecsForApi()
        assertEquals(2, specs.size)
        assertEquals(
            "search_notes",
            (specs[0]["function"] as Map<*, *>)["name"]
        )
        assertEquals(
            "get_note",
            (specs[1]["function"] as Map<*, *>)["name"]
        )
    }

    @Test
    fun `una deshabilitada se excluye y el orden de las demas se conserva`() {
        val specs = registro("search_notes", "get_note", "create_note")
            .getSpecsForApi(setOf("get_note"))
        assertEquals(2, specs.size)
        val nombres = specs.map { (it["function"] as Map<*, *>)["name"] }
        assertEquals(listOf("search_notes", "create_note"), nombres)
    }

    @Test
    fun `todas deshabilitadas dan una lista vacia`() {
        val specs = registro("search_notes", "get_note")
            .getSpecsForApi(setOf("search_notes", "get_note"))
        assertTrue(specs.isEmpty())
    }

    @Test
    fun `un nombre en el conjunto que ya no esta registrado no falla ni filtra de mas`() {
        val specs = registro("search_notes", "get_note")
            .getSpecsForApi(setOf("tool_borrada", "get_note"))
        assertEquals(1, specs.size)
        assertEquals(
            "search_notes",
            (specs[0]["function"] as Map<*, *>)["name"]
        )
    }

    @Test
    fun `names refleja el registro en orden`() {
        assertEquals(
            listOf("search_notes", "get_note"),
            registro("search_notes", "get_note").names()
        )
    }

    private val cinco = listOf(
        "search_notes", "get_note", "create_note", "get_datetime", "get_info"
    )

    private fun registroCinco(): ToolRegistry = registro(*cinco.toTypedArray())

    @Test
    fun `con cinco registradas names devuelve las cinco en orden`() {
        assertEquals(cinco, registroCinco().names())
    }

    @Test
    fun `con cinco registradas sin conjunto se mandan cinco specs`() {
        val specs = registroCinco().getSpecsForApi()
        assertEquals(5, specs.size)
        assertEquals(cinco, specs.map { (it["function"] as Map<*, *>)["name"] })
    }

    @Test
    fun `todas deshabilitadas dan vacio y un nombre muerto no filtra de mas`() {
        val registry = registroCinco()
        assertTrue(registry.getSpecsForApi(cinco.toSet()).isEmpty())
        assertEquals(4, registry.getSpecsForApi(setOf("get_datetime", "tool_borrada")).size)
    }

    @Test
    fun `el ViewModel registra cinco herramientas y no tres`() {
        val fuente = File("src/main/java/com/example/ui/viewmodel/AiViewModel.kt").readText()
        assertEquals(5, fuente.lineSequence().count { it.trimStart().startsWith("register(") })
        assertTrue(fuente.contains("GetDatetimeTool.spec"))
        assertTrue(fuente.contains("GetInfoTool.spec"))
    }
}
