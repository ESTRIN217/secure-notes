package com.example.data.ai.tools

import com.example.data.ai.ToolSpec

/**
 * Herramienta del grupo Browser (spec 043, RF-310, RF-312): sin parámetros, devuelve la hora local
 * del dispositivo. La descripción es dato de protocolo en inglés (D6 de la 042), no recurso.
 * El formato de la respuesta vive en `DateTimeToolFormat`, puro y testeable sin Robolectric.
 */
object GetDatetimeTool {
    val spec = ToolSpec(
        name = "get_datetime",
        description = "Get the current date and time in the user's local timezone. Returns an ISO-8601 timestamp with offset and the IANA timezone name.",
        parameters = emptyList()
    )
}
