package com.example.data.ai.tools

import com.example.data.ai.ToolSpec

/**
 * Herramienta del grupo Browser (spec 043, RF-311, RF-312): sin parámetros, devuelve una nota fija
 * y la versión de Android. La descripción es dato de protocolo en inglés (D6 de la 042) y no menciona
 * cwd para no inducir al modelo a esperarlo (D9). La nota visible sale del recurso `get_info_note`.
 */
object GetInfoTool {
    val spec = ToolSpec(
        name = "get_info",
        description = "Returns runtime info: the Android OS version and a note about the available tools.",
        parameters = emptyList()
    )
}
