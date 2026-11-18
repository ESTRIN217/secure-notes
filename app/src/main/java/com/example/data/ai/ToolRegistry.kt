package com.example.data.ai

/**
 * Conjunto de herramientas deshabilitadas tras (des)activar `name` (spec 042, RF-305).
 *
 * Es la **decisión** del toggle y vive aquí, junto al registro, para ser pura y testeable sin
 * Robolectric (RNF-191): `enabled` significa «hoy queda activa», así que activar **quita** del
 * conjunto y desactivar **añade**. El ViewModel no reescribe la regla, la consulta.
 *
 * Top-level y sin `android`: quien llama le pasa el conjunto actual tal cual.
 */
fun toggledDisabledTools(current: Set<String>, name: String, enabled: Boolean): Set<String> =
    if (enabled) current - name else current + name

/**
 * Conjunto de deshabilitadas tras el checkbox maestro (spec 043, RF-309): «activar todas» vacía el
 * conjunto — así un nombre muerto de una copia antigua desaparece sin efecto (RF-307) — y
 * «desactivar todas» suma las registradas a lo que ya había. Decisión pura: el ViewModel solo la
 * consulta y persiste el resultado.
 */
fun allToolsToggle(
    current: Set<String>,
    registered: List<String>,
    enableAll: Boolean
): Set<String> = if (enableAll) emptySet() else current + registered

/** Numerador del contador `n/m` (spec 043, RF-308): registradas que no están deshabilitadas. */
fun enabledToolCount(registered: List<String>, disabled: Set<String>): Int =
    registered.count { it !in disabled }

/**
 * Estado del checkbox maestro (spec 043, RF-308): marcado solo si todas las registradas están
 * activas. Con lista vacía devuelve cierto, pero la UI deja el control deshabilitado (RF-309).
 */
fun allToolsEnabled(registered: List<String>, disabled: Set<String>): Boolean =
    registered.all { it !in disabled }

class ToolRegistry(
    private val tools: MutableList<ToolWithHandler> = mutableListOf()
) {
    data class ToolWithHandler(
        val spec: ToolSpec,
        val handler: suspend (Map<String, Any>) -> String
    )

    fun register(spec: ToolSpec, handler: suspend (Map<String, Any>) -> String) {
        tools.removeAll { it.spec.name == spec.name }
        tools.add(ToolWithHandler(spec, handler))
    }

    /**
     * Especificaciones que se anuncian al modelo (spec 042, RF-306): las registradas menos las
     * deshabilitadas, en orden de registro. Un nombre del conjunto que ya no corresponde a ninguna
     * herramienta es inofensivo (RF-307): filtrar por nombre no puede fallar por lo que no existe.
     * Sin conjunto, el comportamiento de hoy queda intacto.
     */
    fun getSpecsForApi(
        disabledNames: Set<String> = emptySet()
    ): List<Map<String, Any>> = tools.filter { it.spec.name !in disabledNames }.map { tool ->
        mapOf(
            "type" to "function",
            "function" to mapOf(
                "name" to tool.spec.name,
                "description" to tool.spec.description,
                "parameters" to mapOf(
                    "type" to "object",
                    "properties" to tool.spec.parameters.associate { param ->
                        param.name to mapOf(
                            "type" to param.type,
                            "description" to param.description
                        )
                    },
                    "required" to tool.spec.parameters.filter { it.required }.map { it.name }
                )
            )
        )
    }

    suspend fun execute(name: String, args: Map<String, Any>): String {
        val tool = tools.find { it.spec.name == name }
            ?: return "Error: Tool '$name' not found"
        return try {
            tool.handler(args)
        } catch (e: Exception) {
            "Error executing '$name': ${e.message}"
        }
    }

    fun isNotEmpty(): Boolean = tools.isNotEmpty()

    /** Nombres registrados, en orden de registro: lo que la UI lista (D6: dato, no recurso). */
    fun names(): List<String> = tools.map { it.spec.name }
}
